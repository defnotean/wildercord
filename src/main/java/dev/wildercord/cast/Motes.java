package dev.wildercord.cast;

import dev.wildercord.content.MoteOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Small soft lights and vapours ({@link MoteOption}): glowing specks, motes that spiral in to a
 * point, butterflies of light, and billows of steam and smoke. Everything goes out through
 * {@link Fx#send} (to players within 32 blocks, never in front of someone's own eyes), and a burst
 * of several goes out as one packet, the client scattering them.
 */
public final class Motes {
	private Motes() {}

	/** Pale, soft smoke: what the mod's own fires and blasts give off, in place of vanilla's black squares. */
	public static final int SMOKE = 0x8A8680;
	/** Steam: nearly white, a touch of blue. */
	public static final int STEAM = 0xF4F8FF;

	/** A glowing speck drifting along {@code velocity} (blocks a tick), wandering by {@code wander}. */
	public static void glow(ServerLevel level, Vec3 at, int color, double size, int lifetime, Vec3 velocity, double wander) {
		send(level, new MoteOption(MoteOption.GLOW, color, (float) size, lifetime, (float) velocity.x, (float) velocity.y, (float) velocity.z,
			(float) wander), at, 1, 0, 0);
	}

	/** {@code count} glowing specks scattered {@code spread} round {@code at}, each drifting along {@code velocity} (give or take {@code jitter}). */
	public static void glows(ServerLevel level, Vec3 at, int count, double spread, int color, double size, int lifetime, Vec3 velocity, double jitter) {
		send(level, new MoteOption(MoteOption.GLOW, color, (float) size, lifetime, (float) velocity.x, (float) velocity.y, (float) velocity.z, 0.01F),
			at, count, spread, jitter);
	}

	/** A glowing speck flying from {@code from} to {@code to} over {@code lifetime} ticks, spiralling {@code turns} times round the way. */
	public static void seek(ServerLevel level, Vec3 from, Vec3 to, int color, double size, int lifetime, double turns) {
		send(level, seekOption(from, to, color, size, lifetime, turns), from, 1, 0, 0);
	}

	/** The particle behind {@link #seek}, sent from {@code from}, for sending another way (to everyone but the caster, say). */
	public static MoteOption seekOption(Vec3 from, Vec3 to, int color, double size, int lifetime, double turns) {
		Vec3 d = to.subtract(from);
		return new MoteOption(MoteOption.SEEK, color, (float) size, lifetime, (float) d.x, (float) d.y, (float) d.z, (float) turns);
	}

	/** Specks thrown out from {@code at} in every direction at {@code speed}, slowing as they go and drifting up. */
	public static void burst(ServerLevel level, Vec3 at, int count, int color, double size, int lifetime, double speed) {
		RandomSource r = level.getRandom();
		MoteOption mote = new MoteOption(MoteOption.GLOW, color, (float) size, lifetime, 0, 0.006F, 0, 0.004F);
		for (int i = 0; i < count; i++) {
			double y = 1 - (i + 0.5) * 2.0 / count;
			double s = Math.sqrt(Math.max(0, 1 - y * y));
			double a = i * 2.39996323 + r.nextDouble() * 0.3;
			// A count of 0 hands the client this exact push (the direction times the speed), which dies away.
			Fx.send(level, mote, at.x, at.y, at.z, 0, Math.cos(a) * s, y, Math.sin(a) * s, speed * (0.7 + 0.5 * r.nextDouble()));
		}
	}

	/** One speck pushed off along {@code dir} at {@code speed} (the push dies away), drifting along {@code drift} after. */
	public static void fling(ServerLevel level, Vec3 at, Vec3 dir, double speed, int color, double size, int lifetime, Vec3 drift) {
		MoteOption mote = new MoteOption(MoteOption.GLOW, color, (float) size, lifetime, (float) drift.x, (float) drift.y, (float) drift.z, 0.004F);
		Fx.send(level, mote, at.x, at.y, at.z, 0, dir.x, dir.y, dir.z, speed);
	}

	/** A butterfly of light {@code size} across (wingtip to wingtip), fluttering along {@code velocity}. */
	public static void butterfly(ServerLevel level, Vec3 at, int color, double size, int lifetime, Vec3 velocity) {
		send(level, new MoteOption(MoteOption.BUTTERFLY, color, (float) size, lifetime, (float) velocity.x, (float) velocity.y, (float) velocity.z,
			0.6F), at, 1, 0, 0);
	}

	/**
	 * {@code count} billows of {@code color} (steam, smoke) scattered {@code spread} round {@code at},
	 * each swelling to {@code size} as it rises along {@code velocity} (give or take {@code jitter}),
	 * {@code thickness} (0 to 1) at its thickest.
	 */
	public static void clouds(ServerLevel level, Vec3 at, int count, double spread, int color, double size, int lifetime, Vec3 velocity, double jitter,
			double thickness) {
		send(level, new MoteOption(MoteOption.CLOUD, color, (float) size, lifetime, (float) velocity.x, (float) velocity.y, (float) velocity.z,
			(float) thickness), at, count, spread, jitter);
	}

	/** Soft grey smoke rising off {@code at}: a few billows, never the black squares. */
	public static void smoke(ServerLevel level, Vec3 at, int count, double spread) {
		clouds(level, at, count, spread, SMOKE, 0.9 + spread * 0.6, 36, new Vec3(0, 0.035, 0), 0.015, 0.4);
	}

	private static void send(ServerLevel level, MoteOption mote, Vec3 at, int count, double spread, double jitter) {
		// With a count, the client scatters them (by up to about twice the spread) and hands each a random nudge of the jitter.
		Fx.send(level, mote, at.x, at.y, at.z, count, spread, spread, spread, jitter);
	}
}
