package dev.wildercord.cast;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The batch 4 effects that need more than a line or two: slashes and delayed impacts,
 * bindings, bombs, black flames, the Rampart's temporary wall, and the movement tricks.
 * Protections and time magic that react to incoming damage live in {@link Wards}.
 */
final class Techniques {
	private Techniques() {}

	private static final Identifier WEIGH_ID = Identifier.fromNamespaceAndPath("wildercord", "weigh");
	private static final Map<UUID, Long> WEIGHED = new HashMap<>();
	private static final Map<UUID, Long> BLACKFLAME = new HashMap<>();
	private static final Map<UUID, Integer> BIRDS = new HashMap<>();
	private static final int MAX_BIRDS = 3;
	/** Rampart blocks still standing, and what they replaced. */
	private static final Map<GlobalPos, BlockState> RAMPART = new HashMap<>();
	private static final BlockState RAMPART_BLOCK = Blocks.PACKED_MUD.defaultBlockState();

	static void init() {
		// A Rampart block broken by hand crumbles away without dropping anything.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(level instanceof ServerLevel server)) {
				return true;
			}
			BlockState replaced = RAMPART.remove(GlobalPos.of(server.dimension(), pos.immutable()));
			if (replaced == null || !state.is(RAMPART_BLOCK.getBlock())) {
				return true;
			}
			server.levelEvent(2001, pos, Block.getId(state));
			server.setBlockAndUpdate(pos, replaced);
			TemporaryBlocks.remove(server, pos);
			return false;
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(Techniques::crumbleAll);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			WEIGHED.clear();
			BLACKFLAME.clear();
			OVERDRIVE.clear();
			BIRDS.clear();
		});
	}

	private static DamageSource magic(Cast cast) {
		return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
	}

	private static DamageSource strike(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.PLAYER_ATTACK, cast.caster);
	}

	private static void setMotion(LivingEntity target, Vec3 motion) {
		target.setDeltaMovement(motion);
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	private static boolean fits(ServerLevel level, Entity entity, Vec3 feet) {
		return level.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(feet));
	}

	private static void teleport(Entity entity, ServerLevel level, Vec3 to, float yRot, float xRot) {
		entity.teleportTo(level, to.x, to.y, to.z, Set.<Relative>of(), yRot, xRot, false);
		entity.resetFallDistance();
	}

	// ------------------------------------------------------------------ damage

	/** Cleave: damage in proportion to the target's size. */
	static void cleave(Cast cast, LivingEntity t, double power) {
		double amount = (4 + Math.min(30, 0.12 * t.getMaxHealth())) * power;
		TechniqueVfx.cleave(cast.level, t, cast.caster.getLookAngle());
		Effects.hurt(cast, t, strike(cast), amount);
		// A deep cut: bleeding for a few seconds, so wind damage sets off Rupture.
		Reactions.mark(t, Reactions.Mark.BLEEDING, 60);
	}

	/** Dismantle: three slashes a tenth of a second apart, through armour. */
	static void dismantle(Cast cast, LivingEntity t, double power) {
		TechniqueVfx.dismantle(cast.level, t, 0);
		Effects.hurt(cast, t, magic(cast), 3 * power);
		// Cut three times over: bleeding until a little after the last, so wind damage sets off Rupture.
		Reactions.mark(t, Reactions.Mark.BLEEDING, 64);
		for (int i = 1; i < 3; i++) {
			int slash = i;
			Scheduler.later(i * 2, () -> {
				// Not after a player who stepped through a portal meanwhile.
				if (cast.alive() && t.isAlive() && t.level() == cast.level) {
					TechniqueVfx.dismantle(cast.level, t, slash);
					Effects.hurt(cast, t, magic(cast), 3 * power);
				}
			});
		}
	}

	/** Blackspark: one hit in four lands true, for 2.5x damage and a moment in the zone. */
	static void blackspark(Cast cast, LivingEntity t, double power) {
		boolean spark = cast.level.getRandom().nextFloat() < 0.25F;
		TechniqueVfx.blackspark(cast.level, t, spark);
		Effects.hurt(cast, t, magic(cast), 8 * power * (spark ? 2.5 : 1.0));
		if (spark) {
			cast.caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0, false, true));
			cast.caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0, false, true));
			Reactions.callout(cast, "blackspark", 0xD2283C);
		}
	}

	/** Aftershock: a hit now, and a second impact half a second later. */
	static void aftershock(Cast cast, LivingEntity t, double power) {
		TechniqueVfx.aftershock(cast.level, t, false);
		Effects.hurt(cast, t, strike(cast), 5 * power);
		Scheduler.later(10, () -> {
			if (cast.alive() && t.isAlive() && t.level() == cast.level) {
				TechniqueVfx.aftershock(cast.level, t, true);
				Effects.hurt(cast, t, strike(cast), 5 * power);
				Effects.push(t, new Vec3(0, 0.35, 0));
			}
		});
	}

	/** Resonance: marks the target; every other marked enemy nearby feels half the hit. */
	static void resonance(Cast cast, LivingEntity t, double power, int markTicks) {
		double amount = 4 * power;
		List<LivingEntity> linked = new ArrayList<>();
		for (Entity e : cast.level.getEntities(t, t.getBoundingBox().inflate(16.0),
				e -> Targets.canHarm(cast.caster, e) && Reactions.has(e, Reactions.Mark.RESONANT))) {
			if (linked.size() < 8) {
				linked.add((LivingEntity) e);
			}
		}
		TechniqueVfx.resonance(cast.level, t, linked);
		Effects.hurt(cast, t, magic(cast), amount);
		Reactions.mark(t, Reactions.Mark.RESONANT, markTicks);
		for (LivingEntity other : linked) {
			Effects.hurt(cast, other, magic(cast), amount * 0.5);
		}
	}

	/** Ripple: sunlight damage, tripled on undead, that heals the caster for a third of it. */
	static void ripple(Cast cast, LivingEntity t, double power) {
		double amount = 6 * power * (t.isInvertedHealAndHarm() ? 3.0 : 1.0) * Reactions.storm(cast, t);
		TechniqueVfx.ripple(cast.level, t);
		Effects.hurt(cast, t, magic(cast), amount);
		cast.caster.heal((float) Math.min(10.0, amount / 3.0));
	}

	/** Primer: the target becomes a bomb that goes off two seconds later. */
	static void primer(Cast cast, LivingEntity t, double radius, double power) {
		Vec3[] last = {t.getBoundingBox().getCenter()};
		TechniqueVfx.primed(cast.level, t);
		Fx.sound(cast.level, t.position(), SoundEvents.TNT_PRIMED, 0.8F, 1.4F);
		for (int i = 6; i < 40; i += 6) {
			Scheduler.later(i, () -> {
				if (t.isAlive() && t.level() == cast.level) {
					last[0] = t.getBoundingBox().getCenter();
					TechniqueVfx.primerTick(cast.level, t);
				}
			});
		}
		Scheduler.later(40, () -> {
			if (!cast.alive()) {
				return;
			}
			Vec3 at = t.isAlive() && t.level() == cast.level ? t.getBoundingBox().getCenter() : last[0];
			Effects.explode(cast, at, radius, power * 10.0 / 12.0);
		});
	}

	/**
	 * Blackflame: black fire that water can't reach, one hit a second. If the target dies while
	 * burning, the flames leap to the nearest enemy with the time they had left.
	 */
	static void blackflame(Cast cast, LivingEntity t, double power, int seconds, boolean spread) {
		long until = cast.level.getGameTime() + seconds * 20L;
		// Shadowed while the black flames burn: life damage on it sets off Blight.
		Reactions.mark(t, Reactions.Mark.SHADOWED, seconds * 20);
		Long burning = BLACKFLAME.get(t.getUUID());
		if (burning != null && burning >= cast.level.getGameTime()) {
			BLACKFLAME.put(t.getUUID(), Math.max(burning, until));
			TechniqueVfx.blackflame(cast.level, t);
			return;
		}
		BLACKFLAME.put(t.getUUID(), until);
		TechniqueVfx.blackflame(cast.level, t);
		Fx.sound(cast.level, t.position(), SoundEvents.SOUL_ESCAPE, 0.8F, 0.6F);
		boolean[] spent = {false};
		burnTick(cast, t, power, spread, spent);
	}

	private static void burnTick(Cast cast, LivingEntity t, double power, boolean spread, boolean[] spent) {
		Scheduler.later(20, () -> {
			if (!cast.alive()) {
				BLACKFLAME.remove(t.getUUID());
				return;
			}
			Long until = BLACKFLAME.get(t.getUUID());
			long now = cast.level.getGameTime();
			if (!t.isAlive() || t.level() != cast.level) {
				BLACKFLAME.remove(t.getUUID());
				int left = until == null ? 0 : (int) ((until - now) / 20);
				// Only a death spreads it: not a creature that just went (unloaded, despawned, or through a portal).
				if (spread && !spent[0] && left >= 1 && t.isDeadOrDying()) {
					spent[0] = true;
					LivingEntity next = ShapeRunners.nearestEnemy(cast, t.getBoundingBox().getCenter(), 6.0, null);
					if (next != null) {
						Vfx.stream(cast.level, t.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), Vfx.theme("void"), 8);
						blackflame(cast, next, power, Math.max(2, left), true);
					}
				}
				return;
			}
			if (until == null || now > until) {
				BLACKFLAME.remove(t.getUUID());
				return;
			}
			TechniqueVfx.blackflame(cast.level, t);
			Effects.hurt(cast, t, magic(cast), 3 * power);
			burnTick(cast, t, power, spread, spent);
		});
	}

	/**
	 * Hollow: heavy damage to what was hit, and everything around it is dragged into the gap.
	 * Up to three centres per application, so a crowd hit by a Burst still reads clearly.
	 */
	static void hollow(Cast cast, Cast.Hit hit, List<LivingEntity> harmed, double radius, double power) {
		List<LivingEntity> centres = harmed.isEmpty() ? new ArrayList<>() : new ArrayList<>(harmed.subList(0, Math.min(3, harmed.size())));
		List<Vec3> points = new ArrayList<>();
		centres.forEach(t -> points.add(t.getBoundingBox().getCenter()));
		if (points.isEmpty()) {
			points.add(hit.point());
		}
		for (int i = 0; i < points.size(); i++) {
			Vec3 c = points.get(i);
			LivingEntity direct = i < centres.size() ? centres.get(i) : null;
			TechniqueVfx.hollow(cast.level, c, radius);
			Scheduler.later(6, () -> {
				if (!cast.alive()) {
					return;
				}
				if (direct != null && direct.isAlive()) {
					Effects.hurt(cast, direct, cast.level.damageSources().sonicBoom(cast.caster), 20 * power);
				}
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(c, c).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity other = (LivingEntity) e;
					double d = other.getBoundingBox().getCenter().distanceTo(c);
					if (other == direct || d > radius) {
						continue;
					}
					Vec3 towards = c.subtract(other.position());
					Effects.push(other, towards.normalize().scale(Math.min(1.6, 0.4 + d * 0.25)));
					Reactions.mark(other, Reactions.Mark.PULLED);
					Effects.hurt(cast, other, magic(cast), 8 * power);
				}
			});
		}
	}

	/** Repel: a violent outward blast from the point. Enemies just pulled in set off Collapse. */
	static void repel(Cast cast, Cast.Hit hit, double radius, double power) {
		Vec3 c = hit.self() ? cast.caster.position().add(0, 1, 0) : hit.point();
		TechniqueVfx.repel(cast.level, c, radius);
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(c, c).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			double d = t.getBoundingBox().getCenter().distanceTo(c);
			if (d > radius + t.getBbWidth() / 2) {
				continue;
			}
			double react = Reactions.collapse(cast, t);
			Effects.hurt(cast, t, magic(cast), 4 * power * react);
			Vec3 away = Effects.horizontal(t.position().subtract(c), cast.caster.getLookAngle());
			double falloff = 1.0 - 0.4 * Math.min(1.0, d / Math.max(0.5, radius));
			Statuses.windPush(t, away.scale(2.4 * power * falloff).add(0, 0.5, 0));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
	}

	// ------------------------------------------------------------------ control

	/** Decree: everything hit is stunned; speaking it costs the caster 2 health, once per cast. */
	static void decree(Cast cast, List<LivingEntity> harmed, int ticks) {
		if (harmed.isEmpty()) {
			return;
		}
		LivingEntity caster = cast.caster;
		if (cast.once("decree")) {
			TechniqueVfx.decreeSpoken(cast.level, caster);
			if (!Casters.creative(caster)) {
				caster.setHealth(Math.max(1.0F, caster.getHealth() - 2.0F));
				Fx.sound(cast.level, caster.position(), SoundEvents.PLAYER_HURT, 0.6F, 0.8F);
			}
		}
		for (LivingEntity t : harmed) {
			Spirits.hold(t, ticks);
			if (t instanceof Mob mob) {
				mob.setTarget(null);
			}
			TechniqueVfx.decree(cast.level, caster.getEyePosition(), t);
		}
	}

	/** Weigh: triple gravity, slow legs and weak jumps for a while; fliers are dragged down. */
	static void weigh(Cast cast, LivingEntity t, int ticks) {
		modifier(t, Attributes.GRAVITY, 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(t, Attributes.MOVEMENT_SPEED, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifier(t, Attributes.JUMP_STRENGTH, -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		long until = cast.level.getGameTime() + ticks;
		WEIGHED.merge(t.getUUID(), until, Math::max);
		TechniqueVfx.weigh(cast.level, t, true);
		for (int i = 5; i < ticks; i += 5) {
			Scheduler.later(i, () -> {
				if (t.isAlive() && WEIGHED.containsKey(t.getUUID())) {
					if (!t.onGround()) {
						Effects.push(t, new Vec3(0, -0.35, 0));
					}
					TechniqueVfx.weigh(cast.level, t, false);
				}
			});
		}
		Scheduler.later(ticks, () -> {
			Long end = WEIGHED.get(t.getUUID());
			if (end != null && end <= t.level().getGameTime()) {
				WEIGHED.remove(t.getUUID());
				for (var attribute : List.of(Attributes.GRAVITY, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH)) {
					AttributeInstance instance = t.getAttribute(attribute);
					if (instance != null) {
						instance.removeModifier(WEIGH_ID);
					}
				}
			}
		});
	}

	private static void modifier(LivingEntity t, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = t.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(WEIGH_ID, amount, operation));
		}
	}

	/** Shackle: the target is chained to where it stood, and yanked back if it strays. */
	static void shackle(Cast cast, LivingEntity t, int ticks) {
		if (Spirits.isBoss(t)) {
			// Bosses can't be held in place, only slowed.
			t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, ticks, 1, false, true));
			TechniqueVfx.chain(cast.level, t.position(), t, true);
			return;
		}
		Vec3 anchor = t.position();
		TechniqueVfx.chain(cast.level, anchor, t, true);
		for (int i = 2; i <= ticks; i += 2) {
			int tick = i;
			Scheduler.later(i, () -> {
				if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
					return;
				}
				double d = t.position().distanceTo(anchor);
				if (d > 5.0) {
					teleport(t, cast.level, anchor, t.getYRot(), t.getXRot());
				} else if (d > 2.0) {
					Vec3 back = anchor.subtract(t.position()).normalize().scale(Math.min(1.2, 0.3 + (d - 2.0) * 0.4));
					setMotion(t, new Vec3(back.x, Math.max(t.getDeltaMovement().y, back.y), back.z));
					if (tick % 6 == 0) {
						Fx.sound(cast.level, t.position(), SoundEvents.CHAIN_HIT, 0.7F, 0.8F);
					}
				}
				if (tick % 4 == 0) {
					TechniqueVfx.chain(cast.level, anchor, t, false);
				}
			});
		}
	}

	/** Bubble: floats the target helplessly, then pops for damage and leaves it soaked. */
	static void bubble(Cast cast, LivingEntity t, int ticks, double power) {
		// One bubble at a time on a creature: a Zone's next pulse or a Linger doesn't stack pops.
		if (!Statuses.claim(t, "bubble", ticks + 10)) {
			return;
		}
		double lift = 2.5 / Math.max(1, ticks / 2);
		if (t instanceof Mob) {
			Spirits.hold(t, ticks);
		} else {
			t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0, false, false));
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 3, false, false));
		}
		Fx.sound(cast.level, t.position(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.8F, 0.6F);
		for (int i = 0; i < ticks; i += 2) {
			int tick = i;
			Scheduler.later(i + 1, () -> {
				if (!t.isAlive() || t.level() != cast.level) {
					return;
				}
				if (t instanceof Mob && !Spirits.isBoss(t) && fits(cast.level, t, t.position().add(0, lift, 0))) {
					teleport(t, cast.level, t.position().add(0, lift, 0), t.getYRot(), t.getXRot());
				}
				if (tick % 4 == 0) {
					TechniqueVfx.bubble(cast.level, t);
				}
			});
		}
		Scheduler.later(ticks, () -> {
			if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
				return;
			}
			t.removeEffect(MobEffects.LEVITATION);
			// The bubble's own hold lets go now by itself (Spirits.hold); forcing a thaw would also end a longer
			// Freeze on the same creature.
			TechniqueVfx.bubblePop(cast.level, t);
			Effects.hurt(cast, t, magic(cast), 4 * power);
			Reactions.mark(t, Reactions.Mark.SOAKED);
		});
	}

	// ------------------------------------------------------------------ support

	/** Overdrive: strength, speed and haste, paid for with a little health every two seconds. */
	static void overdrive(Cast cast, LivingEntity t, int ticks, int amplify) {
		t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, Math.min(3, 1 + amplify), false, true));
		t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1, false, true));
		t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, 1, false, true));
		TechniqueVfx.overdrive(cast.level, t, true);
		// One drain per target, however often Overdrive is renewed (a passive renews it every 2 s).
		long until = cast.level.getGameTime() + ticks;
		Long running = OVERDRIVE.put(t.getUUID(), Math.max(until, OVERDRIVE.getOrDefault(t.getUUID(), 0L)));
		if (running == null) {
			overdriveDrain(cast.level, t);
		}
	}

	private static final Map<UUID, Long> OVERDRIVE = new HashMap<>();

	private static void overdriveDrain(ServerLevel level, LivingEntity t) {
		Scheduler.later(40, () -> {
			Long until = OVERDRIVE.get(t.getUUID());
			if (until == null || !t.isAlive() || level.getGameTime() > until) {
				OVERDRIVE.remove(t.getUUID());
				return;
			}
			if (t.getHealth() > 2.0F && !(t instanceof ServerPlayer player && player.isCreative())) {
				t.setHealth(t.getHealth() - 1.0F);
				TechniqueVfx.overdrive(level, t, false);
			}
			overdriveDrain(level, t);
		});
	}

	/** Restore: heals, puts out fire and mends worn and held gear a little. */
	static void restore(Cast cast, LivingEntity t, double power) {
		t.heal((float) (6 * power));
		t.clearFire();
		boolean mended = false;
		for (EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			ItemStack stack = t.getItemBySlot(slot);
			if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > 0) {
				int fix = (int) Math.ceil(stack.getMaxDamage() * 0.05 * power);
				stack.setDamageValue(Math.max(0, stack.getDamageValue() - fix));
				mended = true;
			}
		}
		TechniqueVfx.restore(cast.level, t, mended);
	}

	/** Accelerate: time runs faster for the target. */
	static void accelerate(Cast cast, LivingEntity t, int ticks, int amplify) {
		t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, Math.min(4, 2 + amplify), false, true));
		t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, Math.min(4, 2 + amplify), false, true));
		t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, 1, false, true));
		t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true));
		TechniqueVfx.accelerate(cast.level, t);
	}

	// ------------------------------------------------------------------ movement

	/**
	 * The first creature in a hit that a movement trick may act on: not you, not a boss, and friend or fair game
	 * (an enemy whose Shield stopped this spell is neither: the spell ended at its circles).
	 */
	private static LivingEntity partner(Cast cast, Cast.Hit hit) {
		for (Entity e : hit.entities()) {
			if (e instanceof LivingEntity living && e != cast.caster && living.isAlive() && !Spirits.isBoss(e)
					&& (Targets.canHarm(cast.caster, e) && !Shields.blocked(cast, living) || Targets.isAlly(cast.caster, e))) {
				return living;
			}
		}
		return null;
	}

	/** Swap: you and the target trade places. */
	static void swap(Cast cast, Cast.Hit hit) {
		LivingEntity caster = cast.caster;
		LivingEntity target = partner(cast, hit);
		if (target == null) {
			return;
		}
		Vec3 a = caster.position();
		Vec3 b = target.position();
		if (!fits(cast.level, caster, b) || !fits(cast.level, target, a)) {
			Casters.tell(caster, Component.translatable("message.wildercord.swap_blocked"));
			return;
		}
		teleport(caster, cast.level, b, caster.getYRot(), caster.getXRot());
		teleport(target, cast.level, a, target.getYRot(), target.getXRot());
		TechniqueVfx.swap(cast.level, a, b);
	}

	/** Zipper: steps you through the wall you're facing (up to 6 blocks thick). */
	static void zipper(Cast cast) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 eye = caster.getEyePosition();
		Vec3 dir = caster.getLookAngle();
		double wall = -1;
		for (double d = 0.3; d <= 2.5; d += 0.1) {
			BlockPos p = BlockPos.containing(eye.add(dir.scale(d)));
			BlockState state = level.getBlockState(p);
			if (!state.getCollisionShape(level, p).isEmpty()) {
				if (state.getDestroySpeed(level, p) < 0) {
					Casters.tell(caster, Component.translatable("message.wildercord.zipper_unbreakable"));
					return;
				}
				wall = d;
				break;
			}
		}
		if (wall < 0) {
			Casters.tell(caster, Component.translatable("message.wildercord.zipper_no_wall"));
			return;
		}
		Vec3 entry = eye.add(dir.scale(wall));
		double eyeHeight = caster.getEyeHeight();
		for (double d = wall + 0.5; d <= wall + 6.5; d += 0.25) {
			Vec3 p = eye.add(dir.scale(d));
			BlockPos bp = BlockPos.containing(p);
			BlockState state = level.getBlockState(bp);
			if (!state.getCollisionShape(level, bp).isEmpty() && state.getDestroySpeed(level, bp) < 0) {
				Casters.tell(caster, Component.translatable("message.wildercord.zipper_unbreakable"));
				return;
			}
			for (double drop : new double[] {eyeHeight, eyeHeight * 0.5, 0.1}) {
				Vec3 feet = p.subtract(0, drop, 0);
				if (feet.y < level.getMinY() + 1) {
					continue;
				}
				// Out the far side: never into lava or fire (or onto it, a short drop below), nor past the world border.
				AABB landing = caster.getDimensions(caster.getPose()).makeBoundingBox(feet);
				if (fits(level, caster, feet) && !Effects.scorching(level, landing.expandTowards(0, -3, 0))
						&& level.getWorldBorder().isWithinBounds(feet.x, feet.z)) {
					teleport(caster, level, feet, caster.getYRot(), caster.getXRot());
					TechniqueVfx.zipper(level, entry, feet.add(0, eyeHeight * 0.6, 0), dir);
					return;
				}
			}
		}
		Casters.tell(caster, Component.translatable("message.wildercord.zipper_thick"));
	}

	/** Shadowstep: you reappear right behind the target, facing its back. */
	static void shadowstep(Cast cast, Cast.Hit hit) {
		LivingEntity caster = cast.caster;
		LivingEntity target = partner(cast, hit);
		if (target == null) {
			return;
		}
		Vec3 facing = Effects.horizontal(target.getLookAngle(), caster.getLookAngle());
		Vec3 side = new Vec3(-facing.z, 0, facing.x);
		double back = target.getBbWidth() / 2 + 0.9;
		Vec3 base = target.position();
		for (Vec3 offset : List.of(facing.scale(-back), side.scale(back), side.scale(-back))) {
			Vec3 spot = CastEngine.ground(cast.level, base.add(offset).add(0, 0.5, 0));
			if (Math.abs(spot.y - base.y) > 2.5 || !fits(cast.level, caster, spot)) {
				continue;
			}
			Vec3 from = caster.position();
			Vec3 look = target.position().subtract(spot);
			float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
			teleport(caster, cast.level, spot, yaw, 15.0F);
			caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 15, 0, false, false));
			TechniqueVfx.shadowstep(cast.level, from, spot);
			return;
		}
	}

	/** Time Skip: you vanish, reappear up to 8 blocks ahead, and nearby monsters lose you. */
	static void timeSkip(Cast cast) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 dir = Effects.horizontal(caster.getLookAngle(), caster.getLookAngle());
		Vec3 start = caster.position();
		List<Vec3> path = new ArrayList<>();
		for (double d = 0.5; d <= 8.0; d += 0.5) {
			Vec3 spot = path.isEmpty() ? start.add(dir.scale(d)) : path.getLast().add(dir.scale(0.5));
			if (!fits(level, caster, spot)) {
				if (fits(level, caster, spot.add(0, 1.05, 0))) {
					spot = spot.add(0, 1.05, 0);
				} else {
					break;
				}
			}
			path.add(spot);
		}
		// The farthest spot along the way with safe ground under it: never into lava, never over a drop or the void.
		Vec3 end = null;
		for (int i = path.size() - 1; i >= 0 && end == null; i--) {
			Vec3 grounded = CastEngine.ground(level, path.get(i).add(0, 0.2, 0));
			if (Effects.safeSpot(level, caster, grounded)) {
				end = grounded;
			}
		}
		if (end == null) {
			Casters.tell(caster, Component.translatableWithFallback("message.wildercord.time_skip_nowhere", "Nowhere safe ahead to skip to"));
			return;
		}
		teleport(caster, level, end, caster.getYRot(), caster.getXRot());
		caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
		for (Entity e : level.getEntities(caster, caster.getBoundingBox().inflate(16.0), e -> e instanceof Mob)) {
			Mob mob = (Mob) e;
			if (mob.getTarget() == caster) {
				mob.setTarget(null);
			}
		}
		TechniqueVfx.timeSkip(level, start, end);
	}

	// ------------------------------------------------------------------ world

	/** Whether {@code pos} is part of a Rampart's temporary wall. */
	static boolean isRampart(ServerLevel level, BlockPos pos) {
		return !RAMPART.isEmpty() && RAMPART.containsKey(GlobalPos.of(level.dimension(), pos));
	}

	/** Rampart: a temporary earth wall across your aim at the point. */
	static void rampart(Cast cast, Cast.Hit hit, double radiusScale, int ticks) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		Vec3 base = hit.block() != null && hit.face() != null
			? Vec3.atBottomCenterOf(hit.block().relative(hit.face()))
			: CastEngine.ground(level, hit.point().add(0, 0.5, 0));
		Vec3 look = Effects.horizontal(caster.getLookAngle(), caster.getLookAngle());
		Vec3 side = new Vec3(-look.z, 0, look.x);
		int half = Math.max(1, (int) Math.round(2 * radiusScale));
		Set<BlockPos> columns = new LinkedHashSet<>();
		for (int i = -half; i <= half; i++) {
			Vec3 c = base.add(side.scale(i));
			columns.add(BlockPos.containing(c.x, base.y + 0.01, c.z));
		}
		Fx.sound(level, base, SoundEvents.MACE_SMASH_GROUND, 0.8F, 0.7F);
		long due = level.getGameTime() + ticks;
		for (int row = 0; row < 3; row++) {
			int r = row;
			Scheduler.later(1 + row * 2, () -> {
				if (!cast.alive()) {
					return;
				}
				for (BlockPos column : columns) {
					BlockPos p = column.above(r);
					BlockState state = level.getBlockState(p);
					// Never over a Light spell's light: put back when the wall crumbled, it would stay lit for good.
					if (!state.canBeReplaced() || state.is(Blocks.LIGHT)
							|| !level.getEntities((Entity) null, new AABB(p), e -> e instanceof LivingEntity).isEmpty()) {
						continue;
					}
					if (!Casters.mayEdit(caster, level, p) || !cast.takeBlock()) {
						continue;
					}
					RAMPART.put(GlobalPos.of(level.dimension(), p.immutable()), state);
					TemporaryBlocks.put(level, p, RAMPART_BLOCK, state, due);
					level.setBlockAndUpdate(p, RAMPART_BLOCK);
					level.levelEvent(2001, p, Block.getId(RAMPART_BLOCK));
				}
				Fx.sound(level, base.add(0, r, 0), SoundEvents.PACKED_MUD_PLACE, 1.0F, 0.8F + r * 0.1F);
			});
		}
		Scheduler.later(ticks, () -> {
			for (BlockPos column : columns) {
				for (int r = 0; r < 3; r++) {
					crumble(level, column.above(r));
				}
			}
		});
	}

	/** A Rampart block crumbles. One whose chunk isn't loaded now is put back as it loads ({@link TemporaryBlocks}), never loaded just for this. */
	private static void crumble(ServerLevel level, BlockPos pos) {
		BlockState replaced = RAMPART.remove(GlobalPos.of(level.dimension(), pos.immutable()));
		if (replaced == null || !level.isLoaded(pos)) {
			return;
		}
		if (level.getBlockState(pos).is(RAMPART_BLOCK.getBlock())) {
			level.levelEvent(2001, pos, Block.getId(RAMPART_BLOCK));
			level.setBlockAndUpdate(pos, replaced);
		}
		TemporaryBlocks.remove(level, pos);
	}

	/**
	 * On shutdown every Rampart still standing in loaded ground crumbles, so none can outlive its
	 * spell; the rest are saved in {@link TemporaryBlocks}, and crumble as their chunks load.
	 */
	private static void crumbleAll(MinecraftServer server) {
		for (Map.Entry<GlobalPos, BlockState> entry : new ArrayList<>(RAMPART.entrySet())) {
			ServerLevel level = server.getLevel(entry.getKey().dimension());
			BlockPos pos = entry.getKey().pos();
			if (level == null || !level.isLoaded(pos)) {
				continue;
			}
			if (level.getBlockState(pos).is(RAMPART_BLOCK.getBlock())) {
				level.setBlockAndUpdate(pos, entry.getValue());
			}
			TemporaryBlocks.remove(level, pos);
		}
		RAMPART.clear();
	}

	// ------------------------------------------------------------------ summons

	/** Thunderbird: a storm bird circles overhead and strikes the nearest enemy every 1.5 seconds. */
	static void thunderbird(Cast cast, double power, int ticks) {
		LivingEntity caster = cast.caster;
		UUID id = caster.getUUID();
		if (BIRDS.getOrDefault(id, 0) >= MAX_BIRDS) {
			Casters.tell(caster, Component.translatable("message.wildercord.too_many_birds", MAX_BIRDS));
			return;
		}
		BIRDS.merge(id, 1, Integer::sum);
		double phase = cast.level.getRandom().nextDouble() * Math.PI * 2;
		Vec3[] bird = {caster.position().add(0, 3.3, 0)};
		Fx.sound(cast.level, caster.position(), SoundEvents.PHANTOM_FLAP, 1.0F, 1.4F);
		for (int t = 0; t <= ticks; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				double a = phase + tick * 0.12;
				bird[0] = caster.position().add(Math.cos(a) * 2.6, 2.7 + Math.sin(tick * 0.25) * 0.15, Math.sin(a) * 2.6);
				TechniqueVfx.thunderbird(cast.level, bird[0], a, tick);
				if (tick == 0 || tick % 30 != 0) {
					return;
				}
				LivingEntity target = ShapeRunners.nearestEnemy(cast, caster.position().add(0, 1, 0), 12.0, caster.getLastHurtMob());
				if (target != null) {
					TechniqueVfx.birdStrike(cast.level, bird[0], target.getBoundingBox().getCenter());
					Effects.hurt(cast, target, cast.level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), 5 * power * Reactions.storm(cast, target));
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2, false, false));
				}
			});
		}
		Scheduler.later(ticks + 3, () -> {
			BIRDS.computeIfPresent(id, (k, n) -> n <= 1 ? null : n - 1);
			if (caster.level() == cast.level) {
				TechniqueVfx.birdFade(cast.level, bird[0]);
			}
		});
	}
}
