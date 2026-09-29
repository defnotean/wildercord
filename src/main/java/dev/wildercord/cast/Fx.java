package dev.wildercord.cast;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
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

	/** Whether particles and sounds are being left out right now (a passive renewing quietly). */
	public static boolean muted() {
		return muted;
	}

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
		Packet<?> packet = null;
		for (ServerPlayer player : level.players()) {
			if (player != except && inRange(level, player, false, at.x, at.y, at.z)) {
				if (packet == null) {
					packet = packet(particle, false, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
				player.connection.send(packet);
			}
		}
	}

	/** Like {@link #send}, but also to the player it's around: for effects that sit below the eyes, like Heart Circles. */
	public static void sendAll(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		if (muted) {
			return;
		}
		Packet<?> packet = null;
		for (ServerPlayer player : level.players()) {
			if (inRange(level, player, false, at.x, at.y, at.z)) {
				if (packet == null) {
					packet = packet(particle, false, false, at.x, at.y, at.z, count, spread, spread, spread, speed);
				}
				player.connection.send(packet);
			}
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
		Packet<?> packet = null;
		for (ServerPlayer player : level.players()) {
			double ex = x - player.getX();
			double ey = y - player.getEyeY();
			double ez = z - player.getZ();
			if (ex * ex + ey * ey + ez * ez < clearance * clearance || !inRange(level, player, false, x, y, z)) {
				continue;
			}
			if (packet == null) {
				packet = packet(particle, false, false, x, y, z, count, dx, dy, dz, speed);
			}
			player.connection.send(packet);
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
		Packet<?> packet = null;
		for (ServerPlayer player : level.players()) {
			double ex = at.x - player.getX();
			double ey = at.y - player.getEyeY();
			double ez = at.z - player.getZ();
			if (ex * ex + ey * ey + ez * ez < EYE_CLEARANCE * EYE_CLEARANCE || !inRange(level, player, true, at.x, at.y, at.z)) {
				continue;
			}
			if (packet == null) {
				packet = packet(particle, true, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
			player.connection.send(packet);
		}
	}

	/**
	 * Whether a particle at {@code x, y, z} reaches {@code player}: vanilla's own test in
	 * {@code ServerLevel.sendParticles}, the same world and within 32 blocks of their block (512 when
	 * {@code far}, its override of the limit).
	 *
	 * <p>The senders here do that test themselves and build the packet once, the first time a
	 * player passes it, then hand every player that same packet. {@code sendParticles(player, ...)}
	 * builds a new packet for every player it's called for, even the ones it then leaves out; with
	 * hundreds of particle calls a second in a fight, that's a lot of garbage for nothing.</p>
	 */
	static boolean inRange(ServerLevel level, ServerPlayer player, boolean far, double x, double y, double z) {
		double range = far ? 512.0 : 32.0;
		return player.level() == level && player.blockPosition().distToCenterSqr(x, y, z) < range * range;
	}

	/** The packet {@code ServerLevel.sendParticles} would build for these arguments. */
	static Packet<?> packet(ParticleOptions particle, boolean far, boolean alwaysShow, double x, double y, double z, int count, double dx, double dy,
			double dz, double speed) {
		return new ClientboundLevelParticlesPacket(particle, far, alwaysShow, x, y, z, (float) dx, (float) dy, (float) dz, (float) speed, count);
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
