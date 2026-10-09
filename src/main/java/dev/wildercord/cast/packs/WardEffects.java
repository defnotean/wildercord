package dev.wildercord.cast.packs;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.FxSupportAccess;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Targets;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WardRules;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The support pack (see docs/DESIGN.md, "Support pack"): healing, guarding, party help, harmless crowd control and the
 * wards that keep a place, a pet or a village safe. Each rune's numbers are in {@link WardRules}; what they leave behind
 * lives in {@link WardState}. Place effects act once per cast (Kindred's share lands at the same point) and a monster
 * casting one only gets the look of it.
 */
public final class WardEffects {
	private WardEffects() {}

	private static final Identifier SHIELDWALL = Identifier.fromNamespaceAndPath("wildercord", "fxs_shieldwall");

	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		double radius = SpellNumbers.effectRadius(node);
		String path = node.effect.path();
		switch (path) {
			case "worst_first" -> {
				if (!cast.once("fxs:worst_first")) return;
				LivingEntity worst = around(cast, hit.point(), 8.0 * radius, true).stream()
					.filter(e -> e.getHealth() < e.getMaxHealth())
					.min(Comparator.comparingDouble(e -> e.getHealth() / e.getMaxHealth())).orElse(null);
				if (worst == null) {
					WardVfx.fizzle(level, hit.point());
					return;
				}
				worst.heal((float) (6.0 * power));
				WardVfx.heal(level, worst, 0xF5E6A8);
			}
			case "salve" -> helped.forEach(t -> {
				t.heal((float) (2.0 * power));
				t.clearFire();
				t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, FxSupportAccess.ticks(8, duration), 0, false, true));
				WardVfx.heal(level, t, 0xA8E4FF);
			});
			case "mending_mist" -> placeZone(cast, "mending_mist", hit.point(), 4.0 * radius, FxSupportAccess.ticks(8, duration), power, 0xA8E4FF, 0xE6F7FF);
			case "hearthglow" -> placeZone(cast, "hearthglow", hit.point(), 4.0 * radius, FxSupportAccess.ticks(12, duration), power, 0xFFB45A, 0xFFE2B0);
			case "aftercare" -> helped.forEach(t -> {
				WardState.Mark m = WardState.mark(WardState.AFTERCARE, t, FxSupportAccess.ticks(15, duration));
				m.left = WardRules.aftercareHeals(power);
				m.value = 1.0;
				WardVfx.buff(level, t, 0x9BD36A);
			});
			case "hearthsong" -> {
				if (!cast.once("fxs:hearthsong")) return;
				List<LivingEntity> allies = around(cast, hit.point(), 6.0 * radius, true);
				double heal = WardRules.hearthsong(allies.size(), power);
				for (LivingEntity a : allies) {
					a.heal((float) heal);
					WardVfx.heal(level, a, 0xFFC27A);
				}
			}
			case "grace" -> helped.stream().filter(t -> !Spirits.isBoss(t)).forEach(t -> {
				WardState.mark(WardState.GRACE, t, FxSupportAccess.ticks(60, duration));
				WardVfx.guard(level, t, 0xFFE7A0);
			});
			case "managift" -> {
				if (!(caster instanceof Player giver)) return;
				for (LivingEntity t : helped) {
					if (t == caster || !(t instanceof ServerPlayer taker) || Spellbooks.tier(taker) == null) continue;
					double lacks = Mana.max(taker) - Spellbooks.mana(taker);
					double given = WardRules.managiftGiven(Spellbooks.mana(giver), lacks, power);
					if (given <= 0) continue;
					if (!giver.isCreative()) {
						Spellbooks.setMana(giver, (float) (Spellbooks.mana(giver) - given));
					}
					Mana.restore(taker, (float) WardRules.managiftReceived(given));
					WardVfx.tether(level, caster, taker, 0x8FB8FF);
					WardVfx.mend(level, taker, 0x8FB8FF, 4);
				}
			}
			case "manawell" -> placeZone(cast, "manawell", hit.point(), 3.0 * radius, FxSupportAccess.ticks(10, duration), power, 0x8FB8FF, 0xD0E0FF);
			case "guardlink" -> {
				for (LivingEntity t : helped) {
					if (t == caster) continue;
					WardState.Mark m = WardState.mark(WardState.GUARDLINK, t, FxSupportAccess.ticks(15, duration));
					m.other = caster;
					WardVfx.tether(level, caster, t, 0xC9A36B);
					WardVfx.guard(level, t, 0xC9A36B);
				}
			}
			case "rally" -> {
				if (!cast.once("fxs:rally")) return;
				for (LivingEntity a : around(cast, hit.point(), 8.0 * radius, true)) {
					a.addEffect(new MobEffectInstance(MobEffects.SPEED, FxSupportAccess.ticks(12, duration), 0, false, true));
					a.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, FxSupportAccess.ticks(12, duration), 0, false, true));
					WardVfx.buff(level, a, 0xCFF2E0);
				}
			}
			case "morale" -> {
				if (!cast.once("fxs:morale")) return;
				List<LivingEntity> allies = around(cast, hit.point(), 6.0 * radius, true);
				int level0 = WardRules.moraleHearts(allies.size()) - 1;
				for (LivingEntity a : allies) {
					a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, FxSupportAccess.ticks(20, duration), level0, false, true));
					WardVfx.buff(level, a, 0xFFC27A);
				}
			}
			case "shrug_off" -> helped.forEach(t -> {
				MobEffectInstance worst = t.getActiveEffects().stream()
					.filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
					.max(Comparator.comparingInt(e -> e.isInfiniteDuration() ? Integer.MAX_VALUE : e.getDuration())).orElse(null);
				if (worst != null) {
					t.removeEffect(worst.getEffect());
					WardVfx.buff(level, t, 0xCFF2E0);
				}
			});
			case "hexguard" -> helped.forEach(t -> {
				WardState.mark(WardState.HEXGUARD, t, FxSupportAccess.ticks(20, duration));
				WardVfx.guard(level, t, 0x9E7BFF);
			});
			case "stoutheart" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, FxSupportAccess.ticks(10, duration), Math.min(1, amplify), false, true));
				WardVfx.guard(level, t, 0xC9A36B);
			});
			case "ironhold" -> helped.forEach(t -> {
				WardState.Mark m = WardState.mark(WardState.IRONHOLD, t, FxSupportAccess.ticks(6, duration));
				m.value = power;
				WardVfx.guard(level, t, 0xB0B6BE);
			});
			case "evade" -> helped.forEach(t -> {
				WardState.mark(WardState.EVADE, t, FxSupportAccess.ticks(10, duration));
				WardVfx.buff(level, t, 0xCFF2E0);
			});
			case "emberguard" -> helped.forEach(t -> {
				t.clearFire();
				t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FxSupportAccess.ticks(30, duration), 0, false, true));
				WardVfx.guard(level, t, 0xFF8A4A);
			});
			case "beastguard" -> keptCreatures(cast, hit, true).forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, FxSupportAccess.ticks(60, duration), 1, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FxSupportAccess.ticks(60, duration), 0, false, true));
				WardVfx.guard(level, t, 0xFFB3D9);
			});
			case "hearthguard" -> {
				if (!(caster instanceof Player)) return;
				for (Entity e : hit.entities()) {
					if (e instanceof LivingEntity t && t.isAlive() && (t instanceof AbstractVillager || t instanceof AbstractGolem) && cast.admitsConsequence(t)) {
						WardState.mark(WardState.HEARTHGUARD, t, FxSupportAccess.ticks(300, duration));
						WardVfx.guard(level, t, 0xC9A36B);
					}
				}
			}
			case "heel" -> {
				if (!(caster instanceof Player) || !cast.once("fxs:heel")) return;
				AABB box = caster.getBoundingBox().inflate(32.0);
				for (Entity e : level.getEntities(caster, box, x -> x instanceof TamableAnimal pet && pet.isAlive() && pet.getRootOwner() == caster && !pet.isOrderedToSit())) {
					if (e.distanceTo(caster) > 3.0) {
						Vec3 side = FxSupportAccess.horizontal(caster.getLookAngle(), new Vec3(1, 0, 0));
						Vec3 to = caster.position().add(-side.z * 1.2, 0, side.x * 1.2);
						if (!level.noCollision(e, e.getBoundingBox().move(to.subtract(e.position())))) {
							to = caster.position();
						}
						e.teleportTo(to.x, to.y, to.z);
						((Mob) e).getNavigation().stop();
						WardVfx.calm(level, e, 0xCFF2E0);
					}
				}
				dev.wildercord.cast.Fx.sound(level, caster.position(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE, 0.8F, 1.6F);
			}
			case "bellward" -> placeZone(cast, "bellward", hit.point(), 12.0 * radius, FxSupportAccess.ticks(120, duration), power, 0xE8C66B, 0xFFF0C0);
			case "sanctuary" -> placeZone(cast, "sanctuary", hit.point(), 6.0 * radius, FxSupportAccess.ticks(30, duration), power, 0xE8C6FF, 0xFFF0FF);
			case "arrowveil" -> placeZone(cast, "arrowveil", hit.point(), 4.0 * radius, FxSupportAccess.ticks(10, duration), power, 0xD8F4FF, 0xFFFFFF);
			case "blastward" -> placeZone(cast, "blastward", hit.point(), 6.0 * radius, FxSupportAccess.ticks(60, duration), power, 0xC9A36B, 0xE8D8B0);
			case "citadel" -> placeZone(cast, "citadel", hit.point(), 8.0 * radius, FxSupportAccess.ticks(60, duration), power, 0xC9A36B, 0xFFE7A0);
			case "firebreak" -> {
				if (!cast.once("fxs:firebreak")) return;
				double r = 5.0 * radius;
				if (caster instanceof Player) {
					BlockPos centre = BlockPos.containing(hit.point());
					int n = (int) Math.ceil(r);
					for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-n, -n, -n), centre.offset(n, n, n))) {
						if (pos.distToCenterSqr(hit.point()) <= r * r && level.getBlockState(pos).getBlock() instanceof BaseFireBlock
								&& Casters.mayEdit(caster, level, pos.immutable()) && cast.admitsBlock(pos)) {
							level.removeBlock(pos.immutable(), false);
						}
					}
				}
				for (LivingEntity a : around(cast, hit.point(), r, true)) {
					a.clearFire();
				}
				WardVfx.mist(level, hit.point(), r, 0xA8E4FF);
				dev.wildercord.cast.Fx.sound(level, hit.point(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, 0.7F, 1.2F);
			}
			case "pacify" -> harmed.forEach(t -> pacify(level, t, FxSupportAccess.ticks(6, duration), duration));
			case "truce" -> {
				if (!cast.once("fxs:truce")) return;
				for (LivingEntity t : foes(cast, hit.point(), 8.0 * radius)) {
					pacify(level, t, FxSupportAccess.ticks(4, duration), duration);
				}
				WardVfx.place(level, hit.point(), 8.0 * radius, 0xE8C6FF, 0xFFF0FF, 20);
			}
			case "accord" -> {
				if (!placeZone(cast, "accord", hit.point(), 16.0 * radius, FxSupportAccess.ticks(10, duration), power, 0xFFF4C8, 0xFFFFFF)) return;
				for (LivingEntity t : foes(cast, hit.point(), 16.0 * radius)) {
					pacify(level, t, FxSupportAccess.ticks(10, duration), duration);
				}
			}
			case "lure" -> harmed.forEach(t -> {
				if (t instanceof Mob mob && !Spirits.isBoss(mob)) {
					WardState.calm(mob, FxSupportAccess.ticks(4, duration), hit.point());
					WardVfx.calm(level, mob, 0x9E7BFF);
				}
			});
			case "stillbind" -> harmed.forEach(t -> {
				Spirits.hold(t, FxSupportAccess.ticks(3, duration));
				WardVfx.bind(level, t, 0x9E7BFF);
			});
			case "taunt" -> {
				boolean any = false;
				for (LivingEntity t : harmed) {
					if (t instanceof Mob mob) {
						WardState.Mark m = new WardState.Mark(level.getGameTime() + FxSupportAccess.ticks(6, duration));
						m.other = caster;
						WardState.CALMED.remove(mob);
						WardState.TAUNTED.put(mob, m);
						mob.setTarget(caster);
						WardVfx.tether(level, caster, mob, 0xD8424A);
						any = true;
					}
				}
				if (any && cast.once("fxs:taunt")) {
					caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, FxSupportAccess.ticks(6, duration), 0, false, true));
					WardVfx.guard(level, caster, 0xD8424A);
				}
			}
			case "nudge" -> harmed.forEach(t -> {
				Vec3 away = FxSupportAccess.horizontal(t.position().subtract(caster.position()), hit.dir());
				FxSupportAccess.push(t, away.scale(0.9 * Math.min(2.0, power)).add(0, 0.25, 0));
				dev.wildercord.cast.Fx.send(level, net.minecraft.core.particles.ParticleTypes.CLOUD, t.position().add(0, 0.5, 0), 6, 0.3, 0.02);
			});
			case "hobble" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FxSupportAccess.ticks(4, duration), 2, false, true));
				WardVfx.bind(level, t, 0x7A6AA8);
			});
			case "corral" -> {
				if (!cast.once("fxs:corral")) return;
				WardState.Zone zone = new WardState.Zone("corral", level, hit.point(), 5.0 * radius, level.getGameTime() + FxSupportAccess.ticks(6, duration), caster, power);
				for (LivingEntity t : foes(cast, hit.point(), 5.0 * radius)) {
					if (t instanceof Mob mob && !Spirits.isBoss(mob)) {
						zone.held.add(mob);
						WardVfx.bind(level, mob, 0xBFE8D0);
					}
				}
				WardState.zone(zone);
				WardVfx.place(level, hit.point(), 5.0 * radius, 0xBFE8D0, 0xFFFFFF, 30);
			}
			case "spook" -> harmed.forEach(t -> {
				if (t instanceof Mob mob && !Spirits.isBoss(mob)) {
					Vec3 away = FxSupportAccess.horizontal(mob.position().subtract(caster.position()), hit.dir());
					WardState.calm(mob, FxSupportAccess.ticks(3, duration), mob.position().add(away.scale(10.0)));
					WardState.CALMED.get(mob).value = 1.4;
				} else {
					t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FxSupportAccess.ticks(3, duration), 1, false, true));
				}
				WardVfx.calm(level, t, 0x5A4A7A);
			});
			case "soothe" -> harmed.forEach(t -> {
				if (t instanceof Mob mob && mob instanceof NeutralMob) {
					WardState.forget(mob);
					WardVfx.calm(level, mob, 0xE8C6FF);
				} else {
					WardVfx.fizzle(level, t.position().add(0, t.getBbHeight() * 0.6, 0));
				}
			});
			case "aegis" -> {
				if (!cast.once("fxs:aegis")) return;
				Long spent = WardState.AEGIS_SPENT.get(caster.getUUID());
				long now = level.getGameTime();
				if (spent != null && WardRules.resting(spent, now, WardRules.AEGIS_REST)) {
					WardVfx.fizzle(level, caster.position().add(0, 1, 0));
					return;
				}
				WardState.AEGIS_SPENT.put(caster.getUUID(), now);
				List<LivingEntity> allies = around(cast, caster.position(), 6.0 * radius, true);
				if (!allies.contains(caster)) allies.add(caster);
				for (LivingEntity a : allies) {
					WardState.mark(WardState.AEGIS, a, FxSupportAccess.ticks(6, duration));
					WardVfx.guard(level, a, 0xFFE7A0);
				}
				WardVfx.place(level, caster.position(), 6.0 * radius, 0xFFE7A0, 0xFFFFFF, 30);
			}
			case "shieldwall" -> {
				if (!cast.once("fxs:shieldwall")) return;
				int ticks = FxSupportAccess.ticks(8, duration);
				for (LivingEntity a : around(cast, hit.point(), 5.0 * radius, true)) {
					a.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0, false, true));
					AttributeInstance resist = a.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
					if (resist != null) {
						resist.addOrUpdateTransientModifier(new AttributeModifier(SHIELDWALL, 1.0, AttributeModifier.Operation.ADD_VALUE));
						WardState.Mark m = WardState.mark(WardState.SHIELDWALL, a, ticks);
						dev.wildercord.cast.Scheduler.later(ticks, () -> {
							if (a.level().getGameTime() >= m.until && WardState.SHIELDWALL.get(a.getUUID()) == m) {
								WardState.SHIELDWALL.remove(a.getUUID());
								AttributeInstance again = a.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
								if (again != null) again.removeModifier(SHIELDWALL);
							}
						});
					}
					WardVfx.guard(level, a, 0xC9A36B);
				}
			}
			case "staunch" -> helped.forEach(t -> {
				t.removeEffect(MobEffects.POISON);
				t.removeEffect(MobEffects.WITHER);
				WardState.mark(WardState.STAUNCH, t, FxSupportAccess.ticks(10, duration));
				WardVfx.heal(level, t, 0xA8E4FF);
			});
			case "sentry" -> {
				if (!(caster instanceof Player)) return;
				for (LivingEntity t : helped) {
					if (!cast.once("fxs:sentry:" + t.getUUID())) continue;
					double r = 16.0 * radius;
					for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(r), x -> x instanceof Enemy && x instanceof LivingEntity && x.isAlive())) {
						if (e.distanceTo(t) <= r) {
							((LivingEntity) e).addEffect(new MobEffectInstance(MobEffects.GLOWING, FxSupportAccess.ticks(10, duration), 0, false, false));
						}
					}
					WardVfx.buff(level, t, 0xFFF4C8);
				}
			}
			case "tend" -> keptCreatures(cast, hit, false).forEach(t -> {
				t.heal((float) ((t instanceof IronGolem ? 16.0 : 8.0) * power));
				WardVfx.heal(level, t, 0x9BD36A);
			});
			case "withdraw" -> helped.forEach(t -> {
				if (t.getHealth() >= t.getMaxHealth() / 2.0F) {
					WardVfx.fizzle(level, t.position().add(0, 1, 0));
					return;
				}
				int ticks = FxSupportAccess.ticks(4, duration);
				t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));
				t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1, false, true));
				WardState.mark(WardState.WITHDRAWN, t, ticks);
				for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(32.0), x -> x instanceof Mob)) {
					Mob mob = (Mob) e;
					if (mob.getTarget() == t) {
						WardState.forget(mob);
					}
				}
				WardVfx.evade(level, t);
			});
			case "keepsafe" -> {
				if (!(caster instanceof Player) || hit.block() == null || !cast.once("fxs:keepsafe")) return;
				BlockPos pos = hit.block().immutable();
				if (level.getBlockState(pos).isAir() || !Casters.mayEdit(caster, level, pos)) {
					WardVfx.fizzle(level, hit.point());
					return;
				}
				WardState.KEEPSAFE.put(new WardState.Kept(level.dimension(), pos),
					new WardState.Keeper(level.getGameTime() + FxSupportAccess.ticks(600, duration), caster.getUUID()));
				WardVfx.refuse(level, Vec3.atCenterOf(pos), 0xC9A36B);
				dev.wildercord.cast.Fx.sound(level, Vec3.atCenterOf(pos), net.minecraft.sounds.SoundEvents.CHAIN_PLACE, 0.8F, 0.8F);
			}
			case "faithful" -> {
				if (!(caster instanceof Player)) return;
				for (Entity e : hit.entities()) {
					if (e instanceof LivingEntity pet && pet instanceof OwnableEntity owned && pet.isAlive() && owned.getRootOwner() == caster) {
						WardState.Mark m = WardState.mark(WardState.FAITHFUL, pet, FxSupportAccess.ticks(300, duration));
						m.other = caster;
						WardVfx.guard(level, pet, 0xFFB3D9);
					}
				}
			}
			default -> {}
		}
	}

	/** Raises a place ward once per cast; a monster's only shows. False when nothing was raised. */
	private static boolean placeZone(Cast cast, String kind, Vec3 at, double radius, int ticks, double power, int color, int secondary) {
		if (!cast.once("fxs:" + kind)) return false;
		ServerLevel level = cast.level;
		WardVfx.place(level, at, radius, color, secondary, ticks);
		if (!(cast.caster instanceof Player)) return false;
		WardState.zone(new WardState.Zone(kind, level, at, radius, level.getGameTime() + ticks, cast.caster, power));
		return true;
	}

	private static void pacify(ServerLevel level, LivingEntity t, int ticks, double duration) {
		if (Spirits.isBoss(t)) {
			return;
		}
		if (t instanceof Mob mob) {
			WardState.TAUNTED.remove(mob);
			WardState.calm(mob, ticks, null);
		} else {
			t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true));
		}
		WardVfx.calm(level, t, 0xE8C6FF);
	}

	/** The caster's allies within {@code radius} of {@code at} (the caster too, when near and {@code withCaster}). */
	private static List<LivingEntity> around(Cast cast, Vec3 at, double radius, boolean withCaster) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(at, at).inflate(radius),
				x -> x instanceof LivingEntity && Targets.canHelp(cast.caster, x))) {
			if ((withCaster || e != cast.caster) && e.position().distanceTo(at) <= radius) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	/** Creatures the caster may harm within {@code radius} of {@code at}. */
	private static List<LivingEntity> foes(Cast cast, Vec3 at, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(at, at).inflate(radius),
				x -> x instanceof LivingEntity && Targets.canHarm(cast.caster, x) && cast.admits(x))) {
			if (e.position().distanceTo(at) <= radius) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	/**
	 * The creatures Beastguard and Tend look after: the caster's (or an ally's) pets and mounts, farm animals that are no one
	 * else's, and (for Tend) villagers and golems. Only a player's spell looks after them.
	 */
	private static List<LivingEntity> keptCreatures(Cast cast, Cast.Hit hit, boolean petsOnly) {
		List<LivingEntity> out = new ArrayList<>();
		if (!(cast.caster instanceof Player)) {
			return out;
		}
		for (Entity e : hit.entities()) {
			if (!(e instanceof LivingEntity t) || !t.isAlive() || t instanceof Player || t instanceof Enemy || !cast.admitsConsequence(t)) continue;
			boolean kept;
			if (t instanceof OwnableEntity owned && owned.getOwnerReference() != null) {
				kept = Targets.isAlly(cast.caster, t);
			} else {
				kept = t instanceof Animal || !petsOnly && (t instanceof AbstractVillager || t instanceof AbstractGolem);
			}
			if (kept) {
				out.add(t);
			}
		}
		return out;
	}
}
