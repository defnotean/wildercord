package dev.wildercord.world;

/**
 * Ley lines: thin, winding veins where the world's mana runs close to the surface. They're not
 * placed blocks but a pattern worked out from the world seed, so the server and every client
 * agree on where they are without saving or sending anything but one number. On a ley line
 * mana regenerates twice as fast and Heart Circles form twice as quickly; a Wellstone set on
 * one becomes a mana well for everyone near it.
 *
 * <p>The pattern is where a warped noise field crosses zero, kept only where a second, broader
 * noise allows it, so lines come and go instead of forming a grid. Lines of one field never cross
 * (they're its contours), so the world has two weaves of them, from two seeds and at two sizes: the
 * first as it always was, and a second a little sparser and broader that runs across it. Where a line
 * of each meet is a <em>ley crossing</em>, a place of power: every spell is a little stronger and
 * cheaper there. They lie several hundred blocks apart, wherever the two weaves happen to meet.</p>
 */
public final class LeyLines {
	private LeyLines() {}

	/** How wide a line is, in noise units: about seven or eight blocks across. */
	private static final double WIDTH = 0.013;
	private static final double SCALE = 1 / 260.0;

	/** The seed a world's ley lines grow from: a one-way scramble of the world seed. */
	public static long seedOf(long worldSeed) {
		long z = worldSeed + 0x9E3779B97F4A7C15L * 7;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31) ^ 0x1E7C0DEL;
	}

	/** The second weave runs at this size (a little broader, so its lines lie further apart and wider)... */
	private static final double CROSS_SCALE = SCALE * 0.8;
	/** ...and only where its mask is above this: a little sparser than the first. */
	private static final double CROSS_MASK = 0.0;
	/** How strongly both weaves must run for a spot to count as their crossing. */
	public static final double CROSSING = 0.3;

	/** How strongly a ley line (of either weave) runs at this spot: 0 (none) to 1 (the heart of a line). */
	public static double strength(long seed, double x, double z) {
		return Math.max(first(seed, x, z), second(seed, x, z));
	}

	/** The first weave: the lines as they ran before there were two. */
	static double first(long seed, double x, double z) {
		return weave(seed, x, z, SCALE, -0.1);
	}

	/** The second weave, from a seed of its own, crossing the first. */
	static double second(long seed, double x, double z) {
		return weave(seed * 0x9E3779B97F4A7C15L + 0x5EEDL, x, z, CROSS_SCALE, CROSS_MASK);
	}

	private static double weave(long seed, double x, double z, double scale, double cut) {
		double mask = noise(seed ^ 0x51A7L, x * scale * 0.35, z * scale * 0.35);
		if (mask < cut) {
			return 0;
		}
		double wx = x * scale + 0.9 * noise(seed ^ 0xA11CEL, x * scale * 2.1, z * scale * 2.1);
		double wz = z * scale + 0.9 * noise(seed ^ 0xB0B5L, x * scale * 2.1 + 17.3, z * scale * 2.1 - 9.1);
		double n = Math.abs(noise(seed, wx, wz));
		double fade = Math.min(1, (mask - cut) / 0.25);
		return Math.max(0, 1 - n / WIDTH) * fade;
	}

	/** How strongly two lines cross here: the weaker of the two weaves (0 where only one runs). */
	public static double crossing(long seed, double x, double z) {
		return Math.min(first(seed, x, z), second(seed, x, z));
	}

	/** Whether this spot is a ley crossing: both weaves run strongly enough here. */
	public static boolean atCrossing(long seed, double x, double z) {
		return crossing(seed, x, z) >= CROSSING;
	}

	/**
	 * The heart of the ley crossing in one chunk, if one lies there: {x, z, strength}, the strongest spot on a
	 * two-block grid (a crossing on a chunk's edge can show in both chunks), or null.
	 */
	public static double[] crossingIn(long seed, int chunkX, int chunkZ) {
		double best = CROSSING;
		double[] heart = null;
		for (int dx = 0; dx < 16; dx += 2) {
			for (int dz = 0; dz < 16; dz += 2) {
				double x = (chunkX << 4) + dx + 1;
				double z = (chunkZ << 4) + dz + 1;
				double s = crossing(seed, x, z);
				if (s >= best) {
					best = s;
					heart = new double[] {x, z, s};
				}
			}
		}
		return heart;
	}

	/**
	 * The nearest ley crossing to a spot within {@code radius} blocks, searched chunk by chunk outward:
	 * {x, z, strength}, or null if none lies that close. For finding one (tests, hints), not for every tick.
	 */
	public static double[] nearestCrossing(long seed, double x, double z, int radius) {
		int cx = (int) Math.floor(x) >> 4;
		int cz = (int) Math.floor(z) >> 4;
		int rings = (radius >> 4) + 1;
		double[] best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int ring = 0; ring <= rings; ring++) {
			for (int ix = -ring; ix <= ring; ix++) {
				for (int iz = -ring; iz <= ring; iz++) {
					if (Math.max(Math.abs(ix), Math.abs(iz)) != ring) {
						continue;
					}
					double[] heart = crossingIn(seed, cx + ix, cz + iz);
					if (heart == null) {
						continue;
					}
					double d = Math.hypot(heart[0] - x, heart[1] - z);
					if (d <= radius && d < bestDistance) {
						best = heart;
						bestDistance = d;
					}
				}
			}
			// Anything in a further ring is at least this far away.
			if (best != null && bestDistance <= (ring - 1) * 16.0) {
				break;
			}
		}
		return best;
	}

	/** Classic gradient noise in [-1, 1], seeded by hashing the lattice corners. */
	static double noise(long seed, double x, double y) {
		int x0 = (int) Math.floor(x);
		int y0 = (int) Math.floor(y);
		double fx = x - x0;
		double fy = y - y0;
		double u = fade(fx);
		double v = fade(fy);
		double a = grad(seed, x0, y0, fx, fy);
		double b = grad(seed, x0 + 1, y0, fx - 1, fy);
		double c = grad(seed, x0, y0 + 1, fx, fy - 1);
		double d = grad(seed, x0 + 1, y0 + 1, fx - 1, fy - 1);
		return lerp(v, lerp(u, a, b), lerp(u, c, d)) * 1.4142;
	}

	private static double fade(double t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

	private static double lerp(double t, double a, double b) {
		return a + t * (b - a);
	}

	private static double grad(long seed, int ix, int iy, double dx, double dy) {
		long h = seed ^ (ix * 0x632BE59BD9B4E019L) ^ (iy * 0x8CB92BA72F3D8DD7L);
		h = (h ^ (h >>> 29)) * 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		double angle = (h & 0xFFFF) / 65536.0 * Math.PI * 2;
		return Math.cos(angle) * dx + Math.sin(angle) * dy;
	}
}
