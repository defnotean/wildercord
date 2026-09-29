package dev.wildercord.cast;

import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The fused effects, made only at the Fusion Altar (see {@code spell.Fusions}). Each is two
 * elements at once, and looks it: their visuals, in {@link FusionVfx}, draw on both elements' languages.
 * The first twelve live here; the rest are in five classes by theme ({@link FusedFlame}, {@link FusedFrost},
 * {@link FusedStorm}, {@link FusedLife} and {@link FusedVoid}), and the signature fusions (two particular runes
 * each) in {@link SignatureFusions}. Numbers match the rune descriptions in {@code Runes}.
 */
public final class FusedEffects {
	private FusedEffects() {}

	/** Registers what the fused effects listen for: called once at startup. */
	public static void init() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> LANDED.clear());
		FusedFlame.init();
		FusedFrost.init();
		FusedStorm.init();
		FusedLife.init();
		FusedVoid.init();
		SignatureFusions.init();
	}

	/** At most this many targets get a lingering or spreading part of their own, so one hit can't flood the server. */
	private static final int MAX_TARGETS = 8;

	/** One enemy takes a caster's Magma once a second, however many of their pools it stands in. */
	private static final int MAGMA_EVERY = 20;
	/** One enemy takes a caster's Tempest once a tick, however many of their strikes land round it. */
	private static final int TEMPEST_EVERY = 1;

	/** What last landed on a creature from one caster's rune: when, and how hard. */
	private record Landed(long at, double damage) {}

	/** By rune, caster and creature: see {@link #unstacked}. */
	private static final java.util.Map<String, Landed> LANDED = new java.util.HashMap<>();

	/**
	 * How much of a hit of {@code damage} from this caster's {@code rune} still lands on {@code t}: all of it the
	 * first time in {@code every} ticks; after that, in the same window, only what it has over the strongest that
	 * already landed. Magma lays a pool under each of a crowd and Tempest a strike on each, so without this an
	 * enemy bunched with others took every overlapping one (up to four pools, eight strikes) at once.
	 */
	private static double unstacked(Cast cast, LivingEntity t, String rune, int every, double damage) {
		long now = cast.level.getGameTime();
		String key = rune + ":" + cast.caster.getUUID() + ":" + t.getUUID();
		Landed last = LANDED.get(key);
		if (last == null || now < last.at() || now - last.at() >= every) {
			if (LANDED.size() > 512) {
				LANDED.values().removeIf(landed -> now - landed.at() > MAGMA_EVERY || landed.at() > now);
			}
			LANDED.put(key, new Landed(now, damage));
			return damage;
		}
		if (damage <= last.damage()) {
			return 0;
		}
		LANDED.put(key, new Landed(last.at(), damage));
		return damage - last.damage();
	}

	static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		switch (Effects.builtIn(node.effect) ? node.effect.path() : "") {
			case "firestorm" -> {
				double radius = 2.0 * SpellNumbers.effectRadius(node);
				int burning = 0;
				for (LivingEntity t : harmed) {
					t.igniteForSeconds((float) (6 * duration));
					Effects.hurt(cast, t, level.damageSources().source(DamageTypes.IN_FIRE, caster), 4 * power * Reactions.fire(cast, t));
					// Every target burns; only the first few spread it (and show it), so a crowd can't flood the server.
					if (burning++ >= MAX_TARGETS) {
						continue;
					}
					FusionVfx.firestorm(level, t, radius);
					// The fire leaps to everyone near it (but never back onto the caster's side).
					int spread = 0;
					for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(radius), e -> Targets.canHarm(caster, e))) {
						if (spread++ >= MAX_TARGETS) {
							break;
						}
						LivingEntity near = (LivingEntity) e;
						near.igniteForSeconds((float) (6 * duration));
						FusionVfx.fireLeap(level, t, near);
					}
				}
			}
			case "steam" -> harmed.forEach(t -> {
				Effects.hurt(cast, t, level.damageSources().source(DamageTypes.HOT_FLOOR, caster), 4 * power);
				t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Effects.ticks(3, duration), 0, false, true));
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					mob.setTarget(null);
				}
				FusionVfx.steam(level, t);
			});
			case "magma" -> {
				List<Vec3> pools = new ArrayList<>();
				first(harmed, 4).forEach(t -> pools.add(t.position()));
				if (pools.isEmpty()) {
					pools.add(hit.point());
				}
				double radius = 1.6 * SpellNumbers.effectRadius(node);
				for (Vec3 at : pools) {
					magma(cast, CastEngine.ground(level, at.add(0, 0.5, 0)), radius, power, (int) Math.max(1, Math.round(4 * duration)));
				}
			}
			case "tempest" -> {
				List<Vec3> strikes = new ArrayList<>();
				first(harmed).forEach(t -> strikes.add(t.position()));
				if (strikes.isEmpty()) {
					strikes.add(hit.point());
				}
				for (Vec3 at : strikes) {
					tempest(cast, at, hit, power);
				}
			}
			case "plasma" -> harmed.forEach(t -> {
				// Half of it through the armour as usual, half straight past it.
				double storm = Reactions.storm(cast, t);
				Effects.hurt(cast, t, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), 4.5 * power * storm);
				Effects.hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 4.5 * power * storm);
				FusionVfx.plasma(level, hit.origin(), t);
			});
			case "hail" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Effects.ticks(4, duration), 1, false, true));
				Reactions.mark(t, Reactions.Mark.FROZEN, 40);
				for (int i = 0; i < 3; i++) {
					int stone = i;
					Scheduler.later(1 + i * 5, Effects.carryContext(() -> {
						if (!cast.alive() || !t.isAlive()) {
							return;
						}
						FusionVfx.hailstone(level, t, stone);
						Effects.hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 2 * power);
					}));
				}
			});
			case "glacier" -> harmed.forEach(t -> {
				int ticks = Effects.ticks(t instanceof Player ? 1 : 2, duration);
				Spirits.freeze(t, ticks);
				t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
				t.needsSync = true;
				FusionVfx.glacier(level, t, ticks);
			});
			case "lifesteal" -> harmed.forEach(t -> {
				float before = t.getHealth();
				Effects.hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 5 * power);
				float taken = Math.max(0.0F, before - t.getHealth());
				if (taken > 0 && caster.isAlive()) {
					caster.heal(taken);
				}
				FusionVfx.lifesteal(level, t, caster, taken);
			});
			case "warp" -> warp(cast, hit, duration);
			case "bloom" -> {
				int level2 = 1 + boost(power, amplify);
				for (LivingEntity t : helped) {
					t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Effects.ticks(6, duration), Math.min(3, level2), false, true));
					FusionVfx.bloom(level, t);
				}
				first(helped, 3).forEach(t -> blossom(cast, t.blockPosition()));
				// Pollen: every ally within 4 blocks of a touched one that it missed gets Regeneration I (one hop, six at most).
				Set<LivingEntity> pollen = new java.util.LinkedHashSet<>();
				for (LivingEntity t : first(helped, 3)) {
					for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(4.0), x -> x instanceof LivingEntity && Targets.canHelp(caster, x))) {
						if (pollen.size() < 6 && e.distanceTo(t) <= 4.0 && !helped.contains(e)) {
							pollen.add((LivingEntity) e);
						}
					}
				}
				for (LivingEntity a : pollen) {
					a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Effects.ticks(5, duration), 0, false, true));
					FusionVfx.bloom(level, a);
				}
			}
			case "surge" -> helped.forEach(t -> {
				int extra = boost(power, amplify);
				t.addEffect(new MobEffectInstance(MobEffects.SPEED, Effects.ticks(8, duration), Math.min(3, extra), false, true));
				t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, Effects.ticks(8, duration), Math.min(1, extra), false, true));
				FusionVfx.surge(level, t);
			});
			case "nullify" -> {
				harmed.forEach(t -> {
					// Magic-made creatures unravel: vexes, and spirits and shades that aren't the caster's.
					if (t.getType() == EntityTypes.VEX || t.hasAttached(dev.wildercord.player.WildercordAttachments.SPIRIT_UNTIL)) {
						FusionVfx.nullify(level, t, false);
						t.discard();
						return;
					}
					strip(t, MobEffectCategory.BENEFICIAL);
					FusionVfx.nullify(level, t, false);
				});
				helped.forEach(t -> {
					strip(t, MobEffectCategory.HARMFUL);
					FusionVfx.nullify(level, t, true);
				});
			}
			default -> {
				boolean done = FusedFlame.apply(cast, node, hit, helped, harmed, power, duration, amplify)
					|| FusedFrost.apply(cast, node, hit, helped, harmed, power, duration, amplify)
					|| FusedStorm.apply(cast, node, hit, helped, harmed, power, duration, amplify)
					|| FusedLife.apply(cast, node, hit, helped, harmed, power, duration, amplify)
					|| FusedVoid.apply(cast, node, hit, helped, harmed, power, duration, amplify)
					|| SignatureFusions.apply(cast, node, hit, helped, harmed, power, duration, amplify);
			}
		}
	}

	private static List<LivingEntity> first(List<LivingEntity> targets) {
		return first(targets, MAX_TARGETS);
	}

	private static List<LivingEntity> first(List<LivingEntity> targets, int n) {
		return targets.size() <= n ? targets : targets.subList(0, n);
	}

	/**
	 * Levels a buff gains beyond its base: one per Amplify (rank III counts as one), like Swift and
	 * Haste. Charge, gear and circles make it last longer, never stronger, so it can't climb to IV.
	 */
	private static int boost(double power, int amplify) {
		return Math.max(0, amplify);
	}

	private static void strip(LivingEntity t, MobEffectCategory category) {
		List<Holder<MobEffect>> gone = new ArrayList<>();
		for (MobEffectInstance effect : t.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == category) {
				gone.add(effect.getEffect());
			}
		}
		gone.forEach(t::removeEffect);
		if (category == MobEffectCategory.HARMFUL) {
			t.clearFire();
			t.setTicksFrozen(0);
			Reactions.clear(t, Reactions.Mark.FROZEN);
		}
	}

	/** Magma: the ground at {@code at} burns every enemy standing on it once a second. */
	private static void magma(Cast cast, Vec3 at, double radius, double power, int seconds) {
		ServerLevel level = cast.level;
		FusionVfx.magmaOpen(level, at, radius, 4 + seconds * 20);
		for (int i = 0; i < seconds; i++) {
			boolean last = i == seconds - 1;
			boolean later = i > 0;
			Scheduler.later(4 + i * 20, Effects.carryContext(() -> {
				if (!cast.alive()) {
					return;
				}
				FusionVfx.magmaPulse(level, at, radius, last);
				// After the first, the pulses linger: a Shield blocks them but can't parry them.
				Runnable pulse = () -> {
					AABB box = new AABB(at, at).inflate(radius, 0.8, radius).move(0, 0.4, 0);
					for (Entity e : level.getEntities((Entity) null, box, e -> Targets.canHarm(cast.caster, e))) {
						LivingEntity t = (LivingEntity) e;
						// Pools overlapping (one under each of a crowd) burn an enemy standing in several once. On it means on
						// the ground or just above it: Magma is earth too, and its own heave throws what it lands on into a hop.
						boolean onIt = t.onGround() || t.getY() - at.y < 1.0;
						double burn = onIt && horizontal(t.position(), at) <= radius ? unstacked(cast, t, "magma", MAGMA_EVERY, 2 * power) : 0;
						if (burn > 0) {
							t.igniteForSeconds(2);
							Effects.hurt(cast, t, level.damageSources().source(DamageTypes.HOT_FLOOR, cast.caster), burn);
						}
					}
				};
				if (later) {
					Effects.lingering(pulse);
				} else {
					pulse.run();
				}
			}));
		}
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Tempest: lightning on the spot, then a gale that throws everything struck far away (once, however many strikes land round it). */
	private static void tempest(Cast cast, Vec3 at, Cast.Hit hit, double power) {
		ServerLevel level = cast.level;
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(at.x, at.y, at.z);
			level.addFreshEntity(bolt);
		}
		FusionVfx.tempest(level, at);
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(1.5, 2.5, 1.5), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			double strike = unstacked(cast, t, "tempest", TEMPEST_EVERY, 8 * power);
			if (strike <= 0) {
				continue;
			}
			Effects.hurt(cast, t, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), strike * Reactions.storm(cast, t));
			if (!Spirits.isBoss(t)) {
				Vec3 away = Effects.horizontal(t.position().subtract(hit.origin()), hit.dir());
				Effects.push(t, away.scale(2.8 * Math.sqrt(power)).add(0, 0.9, 0));
			}
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
	}

	/** Warp: a Swap through the void, and an enemy on the other end is left reeling. */
	private static void warp(Cast cast, Cast.Hit hit, double duration) {
		LivingEntity partner = null;
		for (Entity e : hit.entities()) {
			if (e instanceof LivingEntity living && e != cast.caster && living.isAlive() && !Spirits.isBoss(e)
					&& (Targets.canHarm(cast.caster, e) || Targets.isAlly(cast.caster, e))) {
				partner = living;
				break;
			}
		}
		if (partner == null) {
			return;
		}
		Vec3 from = cast.caster.position();
		Vec3 to = partner.position();
		Techniques.swap(cast, hit);
		if (cast.caster.position().distanceToSqr(from) < 1.0E-4) {
			// The swap was blocked (no room): nothing more happens.
			return;
		}
		FusionVfx.warp(cast.level, from, to);
		if (Targets.canHarm(cast.caster, partner)) {
			partner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Effects.ticks(2, duration), 1, false, true));
			partner.addEffect(new MobEffectInstance(MobEffects.NAUSEA, Effects.ticks(2, duration), 0, false, false));
		}
	}

	/** Bloom's garden: grass, flowers and crops around an ally's feet grow, where the caster may build. */
	private static void blossom(Cast cast, BlockPos feet) {
		ServerLevel level = cast.level;
		if (!Casters.mayBuild(cast.caster)) {
			return;
		}
		int grown = 0;
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 0, 2))) {
			if (grown >= 6) {
				break;
			}
			BlockPos p = pos.immutable();
			if ((p.getX() + p.getZ()) % 2 != 0 || !Casters.mayEdit(cast.caster, level, p)) {
				continue;
			}
			if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, p)) {
				level.levelEvent(null, 1505, p, 15);
				grown++;
			}
		}
	}
}
