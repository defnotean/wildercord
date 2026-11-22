package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.monster.EliteMonster;
import dev.wildercord.monster.Giants;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.wildlife.RootmoltContent;
import dev.wildercord.wildlife.ShrineCat;
import dev.wildercord.wildlife.ShrineCatRules;
import dev.wildercord.world.sites.oathhold.OathholdLaw;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfVariants;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary magic on creatures: summoned spirit wolves, and mobs frozen solid by Freeze. Both
 * carry a saved end time, so reloading the world can never make a wolf permanent or leave a
 * mob frozen forever.
 */
public final class Spirits {
	private Spirits() {}

	private static final Map<UUID, BossRules.HoldWindow> BOSS_HOLDS = new ConcurrentHashMap<>();

	public static void init() {
		// The packs hold the wolves themselves; drop them with the world they lived in.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PACKS.clear();
			BOSS_HOLDS.clear();
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			long now = level.getGameTime();
			Long spiritUntil = entity.getAttached(WildercordAttachments.SPIRIT_UNTIL);
			if (spiritUntil != null && spiritUntil <= now) {
				entity.discard();
				return;
			}
			if (spiritUntil != null) {
				Scheduler.later((int) (spiritUntil - now), () -> fade(entity));
			}
			Long frozenUntil = entity.getAttached(WildercordAttachments.FROZEN_UNTIL);
			if (frozenUntil != null && entity instanceof Mob mob) {
				if (frozenUntil <= now) {
					thaw(mob);
				} else {
					Scheduler.later((int) (frozenUntil - now), () -> thaw(mob));
				}
			}
		});
	}

	// ------------------------------------------------------------------ Summon

	/** Spirits one player may have at once, across Summon and Shades. */
	public static final int MAX_SPIRITS = 6;

	/** Every player's living spirit wolves, for the upkeep. */
	private static final java.util.Map<java.util.UUID, java.util.List<Wolf>> PACKS = new java.util.HashMap<>();
	/** Mana regeneration while a pack lives: sustaining spirits costs a quarter of it. */
	public static final double PACK_UPKEEP = 0.75;

	/** The share of its mana regeneration a player keeps: {@link #PACK_UPKEEP} while a spirit wolf of theirs lives, else 1. */
	public static double upkeep(net.minecraft.world.entity.player.Player player) {
		java.util.List<Wolf> pack = PACKS.get(player.getUUID());
		if (pack == null) {
			return 1.0;
		}
		pack.removeIf(w -> w.isRemoved() || !w.isAlive());
		if (pack.isEmpty()) {
			PACKS.remove(player.getUUID());
			return 1.0;
		}
		return PACK_UPKEEP;
	}

	public static void summonWolves(Cast cast, Vec3 around, int count, double power, double duration) {
		spawnSpirits(cast, around, count, power, duration, false);
		LifeArcaneFx.summonCircle(cast.level, around);
	}

	/** Shades: black shadow hounds that trail darkness instead of glowing. */
	public static void summonShades(Cast cast, Vec3 around, int count, double power, double duration) {
		spawnSpirits(cast, around, count, power, duration, true);
		// The hounds growl in shadeRise (once each, from the kit); nothing borrowed from the baby wolf.
	}

	private static void spawnSpirits(Cast cast, Vec3 around, int wanted, double power, double duration, boolean shadow) {
		if (!(cast.caster instanceof ServerPlayer caster)) {
			return;
		}
		ServerLevel level = cast.level;
		int existing = level.getEntities(EntityTypes.WOLF, caster.getBoundingBox().inflate(64.0),
			w -> w.hasAttached(WildercordAttachments.SPIRIT_UNTIL) && w.getOwner() == caster).size();
		int count = Math.min(wanted, MAX_SPIRITS - existing);
		if (count <= 0) {
			Vfx.emit(level, ParticleTypes.POOF, around.add(0, 1, 0), 8, 0.3, 0.02);
			return;
		}
		int lifetime = (int) Math.round((shadow ? 15 : 20) * 20 * duration);
		LivingEntity target = caster.getLastHurtMob() != null && caster.getLastHurtMob().isAlive() ? caster.getLastHurtMob() : null;
		for (int i = 0; i < count; i++) {
			Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (wolf == null) {
				continue;
			}
			double a = Math.PI * 2 * i / count;
			Vec3 spot = CastEngine.ground(level, around.add(Math.cos(a) * 1.6, 1.0, Math.sin(a) * 1.6));
			wolf.snapTo(spot.x, spot.y, spot.z, caster.getYRot(), 0);
			wolf.tame(caster);
			if (shadow) {
				level.registryAccess().lookupOrThrow(Registries.WOLF_VARIANT).get(WolfVariants.BLACK)
					.ifPresent(variant -> wolf.setComponent(DataComponents.WOLF_VARIANT, variant));
				wolf.setCustomName(Component.translatable("entity.wildercord.shadow_hound").withColor(0x9A6AD0));
				// Frail and only strong in the dark: 20 health, and Strength only where the light is 7 or less (see the aura below).
				wolf.addTag(VoidTime.SHADE_TAG);
				wolf.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20.0);
				wolf.setHealth(20.0F);
			} else {
				wolf.setCustomName(Component.translatable("entity.wildercord.spirit_wolf").withColor(0xE678DC));
				wolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, lifetime, 0, false, false));
			}
			wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, lifetime, 1, false, false));
			// Shades are frail: no Strength by default, only in dim light (the aura loop below). A spirit wolf bites for a wolf's 4 unless
			// the spell is Amplified (Strength I from one Amplify).
			if (!shadow) {
				int strength = (int) Math.round(power) - 2;
				if (strength >= 0) {
					wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, lifetime, strength, false, false));
				}
				PACKS.computeIfAbsent(caster.getUUID(), k -> new java.util.ArrayList<>()).add(wolf);
			}
			wolf.setAttached(WildercordAttachments.SPIRIT_UNTIL, level.getGameTime() + lifetime);
			if (target != null && Targets.canHarm(caster, target)) {
				wolf.setTarget(target);
			}
			level.addFreshEntity(wolf);
			if (shadow) {
				TechniqueVfx.shadeRise(level, wolf.position());
				for (int t = 10; t < lifetime; t += 10) {
					Scheduler.later(t, () -> {
						if (!wolf.isRemoved()) {
							TechniqueVfx.shadeAura(level, wolf);
							if (level.getMaxLocalRawBrightness(wolf.blockPosition()) <= 7 || VoidTime.darkened(wolf)) {
								wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 25, (int) Math.max(0, Math.round(power) - 1), false, false));
							}
						}
					});
				}
			} else {
				Vfx.summon(level, wolf.position());
			}
			Scheduler.later(lifetime, () -> fade(wolf));
		}
	}

	private static void fade(Entity spirit) {
		if (spirit.isRemoved() || !(spirit.level() instanceof ServerLevel level)) {
			return;
		}
		Vfx.emit(level, ParticleTypes.SOUL, spirit.getBoundingBox().getCenter(), 12, 0.3, 0.03);
		Vfx.emit(level, ParticleTypes.POOF, spirit.getBoundingBox().getCenter(), 8, 0.2, 0.02);
		spirit.discard();
	}

	// ------------------------------------------------------------------ Freeze

	/** Bosses only tolerate brief 1–1.75s holds before breaking out with a 5s immunity window. */
	public static boolean isBoss(Entity target) {
		if (target == null) {
			return false;
		}
		return target.getType() == EntityTypes.ENDER_DRAGON || target.getType() == EntityTypes.WITHER
			|| target.getType() == EntityTypes.WARDEN || target.getType() == EntityTypes.ELDER_GUARDIAN
			|| target.getType() == WildercordEntities.ARCHIVIST || target instanceof DungeonBoss
			|| target instanceof dev.wildercord.aura.world.Gravekeeper
			|| target instanceof dev.wildercord.aura.world.SwordMaster
			|| target instanceof dev.wildercord.world.ferryman.ReedCrownedFerryman
			|| target instanceof dev.wildercord.world.cartographer.GlassCartographer
			|| target instanceof dev.wildercord.world.hollowbell.HollowBell
			|| target instanceof dev.wildercord.world.kilnwidow.KilnWidow
			|| BossDamage.boss(target)
			|| target instanceof EliteMonster
			|| target instanceof dev.wildercord.ocean.ThalassorBoss
			|| target.entityTags().contains(Giants.TAG)
			|| OathholdLaw.isKnightEntity(target)
			|| (target instanceof ShrineCat cat && !cat.isTame() && cat.getRole() == ShrineCatRules.ROLE_GUARDIAN);
	}

	/** Whether a boss is currently inside its 1–1.75s temporary hold window. */
	public static boolean bossHeld(LivingEntity target) {
		return target != null && isBoss(target)
			&& BossRules.isBossHeld(BOSS_HOLDS.get(target.getUUID()), target.level().getGameTime());
	}

	/** Whether a boss recently broke free and is temporarily unstoppable against follow-up holds. */
	public static boolean bossHoldImmune(LivingEntity target) {
		return target != null && isBoss(target)
			&& BossRules.isBossHoldImmune(BOSS_HOLDS.get(target.getUUID()), target.level().getGameTime());
	}

	/** Whether {@code target} may be temporarily held right now (always true for non-bosses). */
	public static boolean canHoldBoss(LivingEntity target) {
		return target != null
			&& (!isBoss(target) || BossRules.canHoldBoss(BOSS_HOLDS.get(target.getUUID()), target.level().getGameTime()));
	}

	/**
	 * Claims a brief 1–1.75s hold window on a boss and schedules active breakout and 5s immunity.
	 * Returns {@code false} for non-bosses or if the boss is already held or unstoppable.
	 */
	public static boolean tryClaimBossHold(LivingEntity target, int ticks, @Nullable LivingEntity attacker) {
		if (!isBoss(target) || target.level().isClientSide() || dev.wildercord.party.Parties.blocksCurrentHarm(target)) {
			return false;
		}
		int capped = BossRules.bossHoldTicks(ticks);
		if (capped <= 0) {
			return false;
		}
		long now = target.level().getGameTime();
		if (!BossRules.canHoldBoss(BOSS_HOLDS.get(target.getUUID()), now)) {
			return false;
		}
		BOSS_HOLDS.put(target.getUUID(), BossRules.beginBossHold(now, capped));
		UUID attackerId = attacker != null ? attacker.getUUID() : null;
		Scheduler.later(capped, () -> breakOutBoss(target, attackerId));
		return true;
	}

	/** Freeze: {@link #hold} plus ice, which sets up Shatter. */
	public static void freeze(LivingEntity target, int ticks) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(target)) return;
		// A Frostward makes a frost hold last a second at most.
		if (Effects.warded(target, "frostward")) {
			ticks = Math.min(ticks, FROSTWARD_CAP);
		}
		if (isBoss(target)) {
			if (!canHoldBoss(target)) {
				return;
			}
			ticks = BossRules.bossHoldTicks(ticks);
		}
		hold(target, ticks);
		target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
		Reactions.mark(target, Reactions.Mark.FROZEN, ticks + 20);
		if (ticks >= 16 && target.level() instanceof ServerLevel level) {
			// The tell: in its last half second the ice shows hairline cracks (only if it is still the same hold).
			Scheduler.later(ticks - 10, () -> {
				MobEffectInstance slow = target.getEffect(MobEffects.SLOWNESS);
				if (target.isAlive() && slow != null && slow.getAmplifier() >= 6 && slow.getDuration() <= 14) {
					Vfx.iceCracking(level, target);
				}
			});
		}
	}

	/** The longest a frost hold lasts on a creature under Frostward: one second. */
	public static final int FROSTWARD_CAP = 20;

	/**
	 * Ends a hold early (Stasis and Bubble end on their own schedule): a mob's AI comes back, and what holds a player
	 * or a boss (Slowness VII and Weakness V) is taken off.
	 */
	public static void thawNow(LivingEntity target) {
		if (target instanceof Mob mob) {
			thaw(mob);
		}
		MobEffectInstance slow = target.getEffect(MobEffects.SLOWNESS);
		if (slow != null && slow.getAmplifier() >= 6) {
			target.removeEffect(MobEffects.SLOWNESS);
		}
		MobEffectInstance weak = target.getEffect(MobEffects.WEAKNESS);
		if (weak != null && weak.getAmplifier() >= 4) {
			target.removeEffect(MobEffects.WEAKNESS);
		}
	}

	/** Stops a mob's AI for {@code ticks}; players and bosses are only held briefly before breaking out. */
	public static void hold(LivingEntity target, int ticks) {
		hold(target, ticks, null);
	}

	/** Stops a mob's AI for {@code ticks}; bosses are capped to 1–1.75s, break free, and counterattack. */
	public static void hold(LivingEntity target, int ticks, @Nullable LivingEntity attacker) {
		if (dev.wildercord.party.Parties.blocksCurrentHarm(target)) return;
		if (target instanceof net.minecraft.server.level.ServerPlayer player) dev.wildercord.aura.MasterForms.cancel(player);
		if (isBoss(target)) {
			if (!tryClaimBossHold(target, ticks, attacker)) {
				return;
			}
			int capped = BossRules.bossHoldTicks(ticks);
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, capped, 6, false, false));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, capped, 4, false, false));
			return;
		}
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 4, false, false));
		if (target instanceof Mob mob && (!mob.isNoAi() || mob.hasAttached(WildercordAttachments.FROZEN_UNTIL))) {
			long until = mob.level().getGameTime() + ticks;
			mob.setNoAi(true);
			mob.setAttached(WildercordAttachments.FROZEN_UNTIL, Math.max(until, mob.getAttachedOrElse(WildercordAttachments.FROZEN_UNTIL, 0L)));
			Scheduler.later(ticks, () -> {
				Long frozenUntil = mob.getAttached(WildercordAttachments.FROZEN_UNTIL);
				if (frozenUntil != null && frozenUntil <= mob.level().getGameTime()) {
					thaw(mob);
				}
			});
		}
	}

	private static void breakOutBoss(LivingEntity target, @Nullable UUID attackerId) {
		if (!target.isAlive() || target.isRemoved() || !(target.level() instanceof ServerLevel level)) {
			return;
		}
		BossRules.HoldWindow window = BOSS_HOLDS.get(target.getUUID());
		if (window != null && level.getGameTime() < window.heldUntil()) {
			return;
		}
		target.removeEffect(MobEffects.SLOWNESS);
		MobEffectInstance weakness = target.getEffect(MobEffects.WEAKNESS);
		if (weakness != null && weakness.getAmplifier() >= 3) {
			target.removeEffect(MobEffects.WEAKNESS);
		}
		target.removeEffect(MobEffects.LEVITATION);
		target.removeEffect(RootmoltContent.TETHER);
		target.setTicksFrozen(0);
		Reactions.clear(target, Reactions.Mark.FROZEN);
		Techniques.clearWeigh(target.getUUID());
		var modifierIds = List.of(Wildercord.id("sinkhole"), Wildercord.id("mire"), Wildercord.id("weigh"));
		for (var attrHolder : List.of(Attributes.JUMP_STRENGTH, Attributes.MOVEMENT_SPEED, Attributes.GRAVITY)) {
			AttributeInstance attr = target.getAttribute(attrHolder);
			if (attr != null) {
				for (var id : modifierIds) {
					attr.removeModifier(id);
				}
			}
		}
		TemporaryBlocks.shatterAround(level, target.getBoundingBox().inflate(1.5, 1.0, 1.5));
		Vec3 center = target.getBoundingBox().getCenter();
		level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.2, 0.2, 0.2, 0.0);
		level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 14, 0.6, 0.6, 0.6, 0.15);
		Fx.sound(level, center, SoundEvents.SHIELD_BREAK, 1.0F, 0.75F);
		if (target instanceof Mob mob) {
			thaw(mob);
			if (!mob.isNoAi()) {
				LivingEntity currentTarget = mob.getTarget();
				if (currentTarget == null || !currentTarget.isAlive()) {
					LivingEntity candidate = null;
					if (attackerId != null && level.getEntity(attackerId) instanceof LivingEntity attacker
							&& attacker.isAlive() && mob.canAttack(attacker)) {
						candidate = attacker;
					} else if (mob.getLastHurtByMob() != null && mob.getLastHurtByMob().isAlive()
							&& mob.canAttack(mob.getLastHurtByMob())) {
						candidate = mob.getLastHurtByMob();
					} else {
						candidate = level.getNearestPlayer(mob, 24.0);
					}
					if (candidate != null) {
						mob.setTarget(candidate);
					}
				}
				LivingEntity prey = mob.getTarget();
				if (prey != null && prey.isAlive()) {
					mob.setAggressive(true);
					mob.getNavigation().moveTo(prey, 1.15);
				}
			}
		}
	}

	private static void thaw(Mob mob) {
		if (mob.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
			mob.removeAttached(WildercordAttachments.FROZEN_UNTIL);
			mob.setNoAi(false);
		}
	}
}
