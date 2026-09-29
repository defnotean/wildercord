package dev.wildercord.cast;

import dev.wildercord.player.WildercordAttachments;
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
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfVariants;
import net.minecraft.world.phys.Vec3;

/**
 * Temporary magic on creatures: summoned spirit wolves, and mobs frozen solid by Freeze. Both
 * carry a saved end time, so reloading the world can never make a wolf permanent or leave a
 * mob frozen forever.
 */
public final class Spirits {
	private Spirits() {}

	public static void init() {
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

	public static void summonWolves(Cast cast, Vec3 around, int count, double power, double duration) {
		spawnSpirits(cast, around, count, power, duration, false);
		Fx.sound(cast.level, around, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.2F);
	}

	/** Shades: black shadow hounds that trail darkness instead of glowing. */
	public static void summonShades(Cast cast, Vec3 around, int count, double power, double duration) {
		spawnSpirits(cast, around, count, power, duration, true);
		Fx.sound(cast.level, around, SoundEvents.WOLF_GROWL_BABY, 1.0F, 0.5F);
		Fx.sound(cast.level, around, SoundEvents.SOUL_ESCAPE, 1.0F, 0.6F);
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
			if (!shadow) {
				wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, lifetime, (int) Math.max(0, Math.round(power) - 1), false, false));
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

	/** Bosses are only ever slowed: stopping their AI or moving them could break their fight logic. */
	public static boolean isBoss(Entity target) {
		return target.getType() == EntityTypes.ENDER_DRAGON || target.getType() == EntityTypes.WITHER
			|| target.getType() == EntityTypes.WARDEN || target.getType() == EntityTypes.ELDER_GUARDIAN
			|| target.getType() == WildercordEntities.ARCHIVIST || target instanceof DungeonBoss;
	}

	/** Freeze: {@link #hold} plus ice, which sets up Shatter. */
	public static void freeze(LivingEntity target, int ticks) {
		hold(target, ticks);
		target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
		Reactions.mark(target, Reactions.Mark.FROZEN, ticks + 20);
	}

	/** Ends a hold early (Stasis and Bubble end on their own schedule). */
	public static void thawNow(LivingEntity target) {
		if (target instanceof Mob mob) {
			thaw(mob);
		}
	}

	/** Stops a mob's AI for {@code ticks}; players and bosses are slowed to a crawl instead. */
	public static void hold(LivingEntity target, int ticks) {
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 4, false, false));
		if (!isBoss(target) && target instanceof Mob mob && (!mob.isNoAi() || mob.hasAttached(WildercordAttachments.FROZEN_UNTIL))) {
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

	private static void thaw(Mob mob) {
		if (mob.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
			mob.removeAttached(WildercordAttachments.FROZEN_UNTIL);
			mob.setNoAi(false);
		}
	}
}
