package dev.wildercord.world;

/**
 * Ley lines: thin, winding veins where the world's mana runs close to the surface. They're not
 * placed blocks but a pattern worked out from the world seed, so the server and every client
 * agree on where they are without saving or sending anything but one number. On a ley line
 * mana regenerates twice as fast and Heart Circles form twice as quickly; a Wellstone set on
 * one becomes a mana well for everyone near it.
 *
 * <p>The pattern is where a warped noise field crosses zero, kept only where a second, broader
 * noise allows it, so lines come and go instead of forming a grid.</p>
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

	/** How strongly a ley line runs at this spot: 0 (none) to 1 (the heart of a line). */
	public static double strength(long seed, double x, double z) {
		double mask = noise(seed ^ 0x51A7L, x * SCALE * 0.35, z * SCALE * 0.35);
		if (mask < -0.1) {
			return 0;
		}
		double wx = x * SCALE + 0.9 * noise(seed ^ 0xA11CEL, x * SCALE * 2.1, z * SCALE * 2.1);
		double wz = z * SCALE + 0.9 * noise(seed ^ 0xB0B5L, x * SCALE * 2.1 + 17.3, z * SCALE * 2.1 - 9.1);
		double n = Math.abs(noise(seed, wx, wz));
		double fade = Math.min(1, (mask + 0.1) / 0.25);
		return Math.max(0, 1 - n / WIDTH) * fade;
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
