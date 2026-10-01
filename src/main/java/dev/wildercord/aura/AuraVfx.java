package dev.wildercord.aura;

import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Sigils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * How aura looks when it acts, sent from the server so everyone sees the same: the stance's breath, the beat, the guard and its
 * perfect moment, Flow's sweep, the slash's flight (the Crescent's crescents of shaped light, in the aura's colour) and the
 * burst of a breakthrough. Built from the mod's shaped light ({@link Light}), circles' flashes and motes, like every spell;
 * the blade's own glow is drawn by each client from the synced look. Kept to a few strong shapes, never a cloud of particles.
 */
public final class AuraVfx {
	private AuraVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** A colour lifted toward white, for a light's hot core. */
	static int hot(int color, double t) {
		return AuraRules.mix(color, 0xFFFFFF, t);
	}

	/** Each breath of the stance: a slow ring along the ground and a few motes drawn up into the body. */
	static void breathe(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Light.groundRing(level, feet, color, 0.3, 1.5, 0.05, 30);
		for (int i = 0; i < 4; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 from = feet.add(Math.cos(a) * 1.2, 0.2 + level.getRandom().nextDouble() * 0.5, Math.sin(a) * 1.2);
			Motes.seek(level, from, feet.add(0, 1.0, 0), hot(color, 0.3), 0.09, 18, 0.5);
		}
	}

	/** A breath on the beat: a brighter ring and a soft flash at the heart. */
	static void beat(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 heart = player.position().add(0, 1.1, 0);
		Light.groundRing(level, player.position(), hot(color, 0.35), 0.4, 2.2, 0.07, 14);
		Sigils.flash(level, heart, 0xFF000000 | hot(color, 0.4), 1.1F);
		Motes.burst(level, heart, 8, hot(color, 0.25), 0.08, 18, 0.06);
	}

	/** The guard rising: a ring of the aura's light braced in front of the blade. */
	static void guard(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		Light.ring(level, at, look, color, 0.2, 0.9, 0.06, 8);
		Light.ring(level, at, look, hot(color, 0.5), 0.1, 0.6, 0.03, 6);
	}

	/** A blow caught on a held guard: a ripple where it met the blade. */
	static void held(ServerPlayer player, int color, DamageSource source) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		Light.ring(level, at, look, color, 0.1, 1.1, 0.05, 6);
		Fx.send(level, ParticleTypes.CRIT, at, 4, 0.2, 0.15);
	}

	/** A perfect guard: the parry's gold, flashing and racing out across the blade's ring. */
	static void perfect(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 at = player.position().add(0, 1.1, 0).add(look.scale(0.8));
		Sigils.flash(level, at, 0xFF000000 | AuraGuard.PERFECT_COLOR, 1.8F);
		Light.ring(level, at, look, AuraGuard.PERFECT_COLOR, 0.2, 2.2, 0.08, 9);
		Light.ring(level, at, look, color, 0.1, 1.5, 0.05, 7);
		Fx.send(level, ParticleTypes.WAX_OFF, at, 12, 0.3, 0.4);
	}

	/** Flow's sweep: a wide crescent of the aura's colour round the player, at the height of the blow. */
	static void sweep(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 look = flat(player);
		Vec3 centre = player.position().add(0, 1.0, 0);
		Light.slash(level, centre, UP, look, color, AuraRules.FLOW_SWEEP_RANGE * 0.7, 2.6, 0.32, 3, 6);
		Light.slash(level, centre.add(0, 0.05, 0), UP, look, hot(color, 0.55), AuraRules.FLOW_SWEEP_RANGE * 0.68, 2.2, 0.12, 3, 5);
	}

	/** The slash leaving the blade: a bright crescent at the hand and a flash. */
	static void slashStart(ServerPlayer player, Vec3 origin, Vec3 aim, Vec3 side, int color) {
		ServerLevel level = player.level();
		Vec3 normal = UP.add(side.scale(0.18)).normalize();
		Light.slash(level, origin.add(aim.scale(0.6)), normal, aim, hot(color, 0.3), 1.0, 2.4, 0.3, 1, 4);
		Sigils.flash(level, origin.add(aim.scale(0.9)), 0xFF000000 | color, 1.2F);
	}

	/** One step of the slash's flight: the Crescent's crescent, in the aura's colour, white-hot at its edge. */
	static void slashStep(ServerLevel level, Vec3 front, Vec3 aim, Vec3 side, int color, int tick, boolean weak) {
		double width = AuraRules.SLASH_WIDTH * (weak ? 0.75 : 1.0);
		double radius = width * 0.62;
		double span = 2 * Math.asin(Math.min(0.99, width / 2 / radius));
		Vec3 normal = UP.add(side.scale(0.18)).normalize();
		Vec3 centre = front.subtract(aim.scale(radius * 0.8));
		Light.slash(level, centre, normal, aim, color, radius, span, tick == 0 ? 0.44 : 0.36, tick == 0 ? 2 : 1, 4);
		if (tick % 2 == 0) {
			Light.slash(level, centre.add(aim.scale(0.15)), normal, aim, hot(color, 0.65), radius * 0.96, span * 0.85, 0.12, 1, 3);
		}
		if (tick % 3 == 0) {
			Motes.glows(level, front, 2, width * 0.2, hot(color, 0.3), 0.08, 12, aim.scale(-0.02), 0.01);
		}
	}

	/** The slash breaking on a wall. */
	static void slashEnd(ServerLevel level, Vec3 at, Vec3 aim, int color) {
		Sigils.flash(level, at, 0xFF000000 | color, 1.4F);
		Light.ring(level, at, aim, color, 0.2, 1.4, 0.06, 7);
		Motes.burst(level, at, 6, hot(color, 0.3), 0.08, 14, 0.08);
	}

	/** The slash cutting a foe. */
	static void slashCut(ServerLevel level, Vec3 at, Vec3 aim, int color) {
		Sigils.flash(level, at, 0xFF000000 | hot(color, 0.3), 1.0F);
		Fx.send(level, ParticleTypes.CRIT, at, 6, 0.25, 0.2);
	}

	/** A method learned: the aura stirs for the first time, a quiet rise of light. */
	static void learned(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Light.groundRing(level, feet, color, 0.2, 2.0, 0.06, 20);
		Motes.glows(level, feet.add(0, 0.8, 0), 12, 0.5, hot(color, 0.25), 0.09, 30, new Vec3(0, 0.03, 0), 0.01);
	}

	/** A breakthrough: aura bursting out of the body in its colour, a column of light and a ring racing over the ground. */
	static void breakthrough(ServerPlayer player, int color, int stage) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Vec3 heart = feet.add(0, 1.1, 0);
		double size = 2.5 + stage * 0.8;
		Light.groundRing(level, feet, color, 0.3, size * 2, 0.12, 16);
		Light.groundRing(level, feet, hot(color, 0.5), 0.2, size * 1.4, 0.05, 12);
		Light.ring(level, heart, flat(player), color, 0.3, size, 0.08, 12);
		Light.ray(level, feet, feet.add(0, 6 + stage * 2, 0), color, 0.3, 14);
		Light.ray(level, feet, feet.add(0, 5 + stage * 2, 0), hot(color, 0.7), 0.1, 12);
		Sigils.flash(level, heart, 0xFF000000 | hot(color, 0.3), 3.0F);
		Motes.burst(level, heart, 28, hot(color, 0.2), 0.12, 34, 0.28);
		ScreenFx.shake(level, feet, 0.25F, 16);
	}

	private static Vec3 flat(ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}
}
