package dev.wildercord.aura.world;

import dev.wildercord.cast.Light;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.compat.McEntities;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared attack executor for every Oathbound technique. The four knights keep their own hand-built signature moves, but the
 * fifty-plus extra attacks all route through here, so one geometry contract, one damage path, and one effect table cover
 * them all.
 */
public final class OathboundCombat {
	private static final Vec3 UP = new Vec3(0, 1, 0);
	private OathboundCombat() {}

	/** Every living target a knight is allowed to hurt: players in survival, iron golems, and whatever it is hunting. */
	public static List<LivingEntity> targetsIn(ServerLevel level, LivingEntity self, Vec3 center, double radius) {
		AABB box = new AABB(center, center).inflate(radius, 3.0, radius);
		LivingEntity current = self instanceof Mob mob ? mob.getTarget() : null;
		return level.getEntitiesOfClass(LivingEntity.class, box, e ->
				e.isAlive() && e != self && !isOathbound(e)
						&& (e instanceof Player p && !p.isCreative() && !p.isSpectator()
						|| e instanceof IronGolem
						|| (current != null && e == current)));
	}

	private static boolean isOathbound(LivingEntity e) {
		return e instanceof OathboundBastion
				|| e instanceof OathboundBladeChampion
				|| e instanceof OathboundHalberdier
				|| e instanceof OathboundBannerMarshal;
	}

	/** Whether {@code foe} stands inside the technique's swept volume when the knight faces {@code yaw}. */
	public static boolean covers(OathboundTechnique t, float yaw, Vec3 origin, LivingEntity foe) {
		return inVolume(t, yaw, foe.getX() - origin.x, foe.getZ() - origin.z);
	}

	/** Pure geometry behind {@link #covers}: offsets in blocks from the knight, facing yaw in degrees. */
	public static boolean inVolume(OathboundTechnique t, float yaw, double dx, double dz) {
		double dist = Math.hypot(dx, dz);
		double rad = Math.toRadians(yaw);
		double fx = -Math.sin(rad);
		double fz = Math.cos(rad);
		double along = dx * fx + dz * fz;
		double across = Math.abs(dx * (-fz) + dz * fx);
		double angle = dist < 1.0e-5 ? 0.0 : Math.toDegrees(Math.acos(Mth.clamp((dx * fx + dz * fz) / dist, -1.0, 1.0)));
		return switch (t.shape()) {
			case CONE, MULTI_HIT -> dist <= t.reach() && angle <= t.half();
			case WIDE_ARC -> dist <= t.reach() && angle <= t.half();
			case LINE -> along >= 0.0 && along <= t.reach() && across <= t.half();
			case RADIUS -> dist <= t.reach();
			case RING -> dist <= t.reach() && dist >= t.inner();
			case CROSS -> dist <= t.inner()
					|| (Math.abs(along) <= t.reach() && across <= t.half())
					|| (Math.abs(across) <= t.reach() && Math.abs(along) <= t.half());
			case TWIN_LINE -> Math.abs(along) <= t.reach() && across <= t.half();
			case PULL_RADIUS -> dist <= t.reach();
			case PULL_LINE -> along >= 0.0 && along <= t.reach() && across <= t.half();
			case ORBITAL -> dist <= t.reach() && dist >= t.inner();
		};
	}

	/**
	 * Applies one hit pulse of {@code t}. {@code struck} keeps a target from being hit twice by one pulse; multi-hit
	 * techniques pass a fresh set for each pulse so a flurry really lands several times.
	 */
	public static boolean strike(ServerLevel level, LivingEntity self, OathboundTechnique t, float yaw,
			Set<UUID> struck, boolean empowered, double pierce) {
		double mult = empowered ? 1.15 : 1.0;
		Vec3 origin = self.position();
		boolean any = false;
		for (LivingEntity foe : targetsIn(level, self, origin, t.reach() + 1.0)) {
			if (!covers(t, yaw, origin, foe)) {
				continue;
			}
			if (t.shape() != OathboundTechnique.Shape.MULTI_HIT && !struck.add(foe.getUUID())) {
				continue;
			}
			if (!land(level, self, foe, t.damage() * mult, pierce, t.knockback(), origin, t.lift())) {
				continue;
			}
			any = true;
			applyEffect(self, foe, t);
			if (t.shape() == OathboundTechnique.Shape.PULL_LINE || t.shape() == OathboundTechnique.Shape.PULL_RADIUS) {
				pull(level, self, foe, t.speed() > 0.0 ? t.speed() : 0.9);
			}
		}
		return any;
	}

	/** The shared damage path, matching the knights' signature attacks. */
	public static boolean land(ServerLevel level, LivingEntity self, LivingEntity foe, double damage, double pierce,
			double knockback, Vec3 from, double lift) {
		if (!GuardianBite.land(level, self, foe, (float) damage, pierce)) {
			return false;
		}
		if (knockback > 0.0) {
			foe.knockback(knockback, from.x - foe.getX(), from.z - foe.getZ());
		}
		if (lift > 0.0) {
			foe.setDeltaMovement(foe.getDeltaMovement().add(0.0, lift, 0.0));
		}
		McEntities.syncVelocity(foe);
		return true;
	}

	public static void applyEffect(LivingEntity self, LivingEntity foe, OathboundTechnique t) {
		if (t.effect() == OathboundTechnique.Effect.NONE) {
			return;
		}
		switch (t.effect()) {
			case SLOW -> foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, t.effectTicks(), 1, false, true), self);
			case WEAKNESS -> foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t.effectTicks(), 0, false, true), self);
			case BLIND -> foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, t.effectTicks(), 0, false, true), self);
			case WITHER -> foe.addEffect(new MobEffectInstance(MobEffects.WITHER, t.effectTicks(), 0, false, true), self);
			case FEAR -> {
				foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t.effectTicks(), 0, false, true), self);
				foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, t.effectTicks(), 0, false, true), self);
			}
			case LAUNCH -> foe.setDeltaMovement(foe.getDeltaMovement().add(0.0, t.lift(), 0.0));
			default -> {}
		}
		McEntities.syncVelocity(foe);
	}

	public static void pull(ServerLevel level, LivingEntity self, LivingEntity foe, double speed) {
		Vec3 toward = self.position().subtract(foe.position());
		if (toward.lengthSqr() < 1.0e-6) {
			return;
		}
		Vec3 vel = toward.normalize().scale(speed);
		foe.setDeltaMovement(vel.x, Math.max(0.12, foe.getDeltaMovement().y), vel.z);
		McEntities.syncVelocity(foe);
	}

	/** Moves the knight's body for dash, leap, and backstep techniques. */
	public static void motion(LivingEntity self, OathboundTechnique t, float yaw, long active) {
		Vec3 forward = ahead(yaw);
		switch (t.motion()) {
			case DASH -> {
				if (active <= t.committedTicks()) {
					double speed = t.speed() > 0.0 ? t.speed() * 1.35 : 0.95;
					self.setDeltaMovement(forward.x * speed, Math.min(0.0, self.getDeltaMovement().y), forward.z * speed);
					self.move(MoverType.SELF, self.getDeltaMovement());
					self.setDeltaMovement(Vec3.ZERO);
				}
			}
			case LEAP -> {
				if (active == 0L) {
					double speed = t.speed() > 0.0 ? t.speed() * 1.35 : 0.75;
					self.setDeltaMovement(forward.x * speed, 0.52, forward.z * speed);
				}
			}
			case BACKSTEP -> {
				if (active <= 6L) {
					double speed = t.speed() > 0.0 ? t.speed() * 1.35 : 0.70;
					self.setDeltaMovement(-forward.x * speed, Math.min(0.0, self.getDeltaMovement().y), -forward.z * speed);
					self.move(MoverType.SELF, self.getDeltaMovement());
					self.setDeltaMovement(Vec3.ZERO);
				}
			}
			case PLANTED -> {}
		}
	}

	/** Pre-impact telegraph: a sigil on the ground or a ring in the air, coloured by the technique. */
	public static void windupFx(ServerLevel level, LivingEntity self, OathboundTechnique t, float yaw, long el) {
		Vec3 origin = self.position();
		Vec3 chest = self.position().add(0.0, self.getBbHeight() * 0.6, 0.0);
		Vec3 forward = ahead(yaw);
		switch (t.shape()) {
			case CONE, WIDE_ARC, MULTI_HIT ->
					Sigils.layer(level, chest.add(forward.scale(0.9)), forward, SigilOption.RING, t.color(), 1.1F, 6, 0.15F);
			case LINE, TWIN_LINE, PULL_LINE, CROSS ->
					Light.groundRing(level, origin, t.color(), 0.3, Math.min(4.0, t.reach()), 0.08, 6);
			case RADIUS, RING, PULL_RADIUS, ORBITAL ->
					Light.groundRing(level, origin, t.color(), 0.3, t.reach(), 0.08, 6);
		}
		if (el % 6 == 0) {
			level.sendParticles(ParticleTypes.WAX_ON, chest.x, chest.y, chest.z, 2, 0.4, 0.4, 0.4, 0.03);
		}
	}

	/** The impact flourish. Shape decides the geometry, archetype decides the sound, colour decides the palette. */
	public static void strikeFx(ServerLevel level, LivingEntity self, OathboundTechnique t, float yaw) {
		Vec3 origin = self.position();
		Vec3 chest = origin.add(0.0, self.getBbHeight() * 0.6, 0.0);
		Vec3 forward = ahead(yaw);
		double span = Math.toRadians(t.half()) * 2.0;
		switch (t.shape()) {
			case CONE, MULTI_HIT -> {
				Light.slash(level, chest, UP, forward, t.color(), t.reach(), span, 0.2, 1, 9);
				level.sendParticles(ParticleTypes.CRIT, chest.x + forward.x, chest.y, chest.z + forward.z, 8, 0.4, 0.35, 0.4, 0.12);
			}
			case WIDE_ARC -> {
				Light.slash(level, chest, UP, forward, t.color(), t.reach() * 0.95, 2.6, 0.24, 1, 12);
				Light.slash(level, chest.add(0.0, 0.1, 0.0), UP, forward, 0xFFFFFF, t.reach() * 0.85, 2.4, 0.10, 1, 8);
			}
			case LINE -> {
				Light.ray(level, origin.add(0.0, 0.9, 0.0), origin.add(forward.scale(t.reach())).add(0.0, 0.9, 0.0), t.color(), 0.3, 8);
				level.sendParticles(ParticleTypes.END_ROD, chest.x + forward.x * 1.5, chest.y, chest.z + forward.z * 1.5,
						6, 0.2, 0.2, 0.2, 0.06);
			}
			case TWIN_LINE -> {
				Light.ray(level, origin.add(0.0, 0.9, 0.0), origin.add(forward.scale(t.reach())).add(0.0, 0.9, 0.0), t.color(), 0.3, 8);
				Light.ray(level, origin.add(0.0, 0.9, 0.0), origin.subtract(forward.scale(t.reach())).add(0.0, 0.9, 0.0), t.color(), 0.3, 8);
			}
			case RADIUS -> Light.groundRing(level, origin, t.color(), 0.4, t.reach(), 0.15, 12);
			case RING -> Light.ring(level, origin.add(0.0, 0.1, 0.0), UP, t.color(), t.inner(), t.reach(), 0.15, 14);
			case CROSS -> {
				Light.groundRing(level, origin, t.color(), 0.3, t.inner(), 0.14, 12);
				for (int i = 0; i < 4; i++) {
					Vec3 dir = new Vec3(i == 0 ? 1 : i == 1 ? -1 : 0, 0, i == 2 ? 1 : i == 3 ? -1 : 0);
					Light.ray(level, origin.add(0.0, 0.12, 0.0), origin.add(dir.scale(t.reach())).add(0.0, 0.12, 0.0),
							t.color(), 0.32, 10);
				}
			}
			case PULL_RADIUS, PULL_LINE, ORBITAL -> {
				Light.groundRing(level, origin, t.color(), 0.4, t.reach(), 0.12, 10);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, chest.x, chest.y, chest.z, 10, 0.5, 0.4, 0.5, 0.02);
			}
		}
		playImpactSound(level, origin, soundFor(t.archetype()), pitchFor(t.archetype()));
		Feels.sound(level, chest, feelFor(t.archetype()), 1.1F, pitchFor(t.archetype()));
	}

	private static String feelFor(OathboundTechnique.Archetype a) {
		return switch (a) {
			case OVERHEAD_SLAM, LEAP_SLAM, STOMP, KNEEL_STRIKE -> "aura_bell_warden_hammer";
			case SHIELD_PUSH, GUARD_BRACE -> "aura_bell_warden_shell";
			case ROAR, CAST_RAISE -> "aura_bell_warden_knell";
			case OVERHAND_THROW -> "aura_bell_warden_charge";
			default -> "duelist_clash";
		};
	}

	private static SoundEvent soundFor(OathboundTechnique.Archetype a) {
		return switch (a) {
			case OVERHEAD_SLAM, LEAP_SLAM, STOMP, KNEEL_STRIKE -> SoundEvents.MACE_SMASH_GROUND;
			case SHIELD_PUSH -> SoundEvents.ANVIL_LAND;
			case GUARD_BRACE -> SoundEvents.SHIELD_BLOCK.value();
			case ROAR -> SoundEvents.WARDEN_ROAR;
			case CAST_RAISE -> SoundEvents.BELL_RESONATE;
			case OVERHAND_THROW -> SoundEvents.TRIDENT_THROW.value();
			case THRUST -> SoundEvents.TRIDENT_HIT;
			case SPIN_SWEEP, HORIZONTAL_SWEEP, DIAGONAL_SLASH, CROSS_CUT, FLURRY, WEAPON_TWIRL ->
					SoundEvents.PLAYER_ATTACK_SWEEP;
			case UPPERCUT -> SoundEvents.PLAYER_ATTACK_STRONG;
			case BACKSTEP -> SoundEvents.PLAYER_ATTACK_STRONG;
		};
	}

	private static float pitchFor(OathboundTechnique.Archetype a) {
		return switch (a) {
			case OVERHEAD_SLAM, LEAP_SLAM, STOMP, KNEEL_STRIKE -> 0.82F;
			case ROAR -> 0.65F;
			case CAST_RAISE -> 1.25F;
			case THRUST, FLURRY -> 1.15F;
			default -> 0.95F;
		};
	}

	private static void playImpactSound(ServerLevel level, Vec3 at, SoundEvent event, float pitch) {
		level.playSound(null, at.x, at.y, at.z, event, SoundSource.HOSTILE, 1.25F, pitch);
	}

	public static Vec3 ahead(float yaw) {
		double rad = Math.toRadians(yaw);
		return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
	}
}
