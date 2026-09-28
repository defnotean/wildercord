package dev.wildercord.cast;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Particles and sounds for spells. Everything is sent from the server so every player sees it. */
public final class Fx {
	private Fx() {}

	/**
	 * How close to a player's eyes a particle may appear before it is left out for that player.
	 * Particles right in front of the camera fill the whole screen in first person.
	 */
	private static final double EYE_CLEARANCE = 1.1;

	/** While true, nothing is sent: passives renew their buffs quietly after the first time. */
	private static boolean muted;

	/** Runs {@code action} with every particle and sound left out. */
	public static void quietly(Runnable action) {
		boolean was = muted;
		muted = true;
		try {
			action.run();
		} finally {
			muted = was;
		}
	}

	/** To every player except {@code except}: a flourish around someone that only onlookers need to see. */
	public static void sendOthers(ServerLevel level, ServerPlayer except, ParticleOptions particle, Vec3 at) {
		if (muted) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (player != except) {
				level.sendParticles(player, particle, false, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
	}

	/** Like {@link #send}, but also to the player it's around: for effects that sit below the eyes, like Heart Circles. */
	public static void sendAll(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		if (muted) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			level.sendParticles(player, particle, false, false, at.x, at.y, at.z, count, spread, spread, spread, speed);
		}
	}

	/**
	 * Sends particles to every nearby player, except to a player whose camera they would sit
	 * in front of. Everyone else still sees them, so your own spells never block your view.
	 */
	public static void send(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		if (muted) {
			return;
		}
		double spread = count > 0 ? Math.min(0.6, Math.max(dx, Math.max(dy, dz))) : 0.0;
		double clearance = EYE_CLEARANCE + spread;
		for (ServerPlayer player : level.players()) {
			Vec3 eye = player.getEyePosition();
			double ex = x - eye.x;
			double ey = y - eye.y;
			double ez = z - eye.z;
			if (ex * ex + ey * ey + ez * ez < clearance * clearance) {
				continue;
			}
			level.sendParticles(player, particle, false, false, x, y, z, count, dx, dy, dz, speed);
		}
	}

	/**
	 * Like {@link #send}, but seen from up to 512 blocks instead of 32: for things as big as a
	 * Domain's dome, whose far side is often more than 32 blocks from the player inside it.
	 */
	public static void sendFar(ServerLevel level, ParticleOptions particle, Vec3 at) {
		if (muted) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			Vec3 eye = player.getEyePosition();
			if (eye.distanceToSqr(at) < EYE_CLEARANCE * EYE_CLEARANCE) {
				continue;
			}
			level.sendParticles(player, particle, true, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
	}

	public static void send(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		send(level, particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
	}

	public static DustParticleOptions dust(int color, float scale) {
		return new DustParticleOptions(color, scale);
	}

	public static void puff(ServerLevel level, Vec3 pos, int color, int count, double spread) {
		send(level, dust(color, 1.2F), pos, count, spread, 0.0);
	}

	public static void particle(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
		send(level, particle, pos, count, spread, speed);
	}

	public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
		if (muted) {
			return;
		}
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	public static void sound(ServerLevel level, Vec3 pos, Holder<SoundEvent> sound, float volume, float pitch) {
		if (muted) {
			return;
		}
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}
}
