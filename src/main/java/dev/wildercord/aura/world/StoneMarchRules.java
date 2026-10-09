package dev.wildercord.aura.world;

/** Three adjoining, fixed ground bands. Inward spent ground is a permanent route back to the exposed swordsman. */
public final class StoneMarchRules {
	private StoneMarchRules() {}
	public static final int TELL = 32, SECOND = 40, THIRD = 48, END = 96, RECOVERY = 48, COOLDOWN = 160;
	public static final int BANDS = 3, WARNING_REFRESH = 4, MAX_COVER_BOXES = 512;
	public static final double COST = 30, DAMAGE = 24, START = 1.5, LENGTH = 2, HALF_WIDTH = 1.75;
	public static final double LOW = -.05, HIGH = .65, ESCAPE_MARGIN = .15;

	/** A Stone ordinary fallback only; higher-priority responses are checked by the live admission boundary. */
	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == MastersRules.STONE && sequence >= 0 && sequence % 4 == 3 && now >= readyAt
			&& Double.isFinite(aura) && aura >= COST && aura <= MastersRules.AURA_MAX
			&& Double.isFinite(distance) && distance >= START && distance <= end(BANDS - 1)
			&& Double.isFinite(height) && Math.abs(height) <= .15;
	}

	public static double start(int band) { return START + LENGTH * band; }
	public static double end(int band) { return start(band) + LENGTH; }
	public static int pulseAge(int band) { return switch (band) { case 0 -> TELL; case 1 -> SECOND; case 2 -> THIRD; default -> -1; }; }
	public static int pulse(long age) { return age == TELL ? 0 : age == SECOND ? 1 : age == THIRD ? 2 : -1; }

	/** Feet choose one band only, including its near edge; only the final band's far edge is inclusive. */
	public static int band(double forward, double side, double height) {
		if (!Double.isFinite(forward) || !Double.isFinite(side) || !Double.isFinite(height)
			|| Math.abs(side) > HALF_WIDTH || height < LOW || height > HIGH || forward < START || forward > end(BANDS - 1)) return -1;
		return Math.min(BANDS - 1, (int) ((forward - START) / LENGTH));
	}
	public static boolean hits(int band, double forward, double side, double height) {
		return band >= 0 && band < BANDS && band(forward, side, height) == band;
	}

	/** Native 26.3 BlockCollisions scans this conservative block halo even for shapes extending from adjacent cells. */
	public static int collisionMin(double edge) { return (int) Math.floor(edge - 1e-7) - 1; }
	public static int collisionMax(double edge) { return (int) Math.floor(edge + 1e-7) + 1; }

	/** SAT overlap of a world-aligned collision box with the accepted oriented rectangle; thin cover cannot slip between rays. */
	public static boolean overlaps(double minX, double maxX, double minZ, double maxZ,
		double aimX, double aimZ, double low, double high, double halfWidth) {
		for (double value : new double[] {minX, maxX, minZ, maxZ, aimX, aimZ, low, high, halfWidth})
			if (!Double.isFinite(value)) return true;
		if (minX > maxX || minZ > maxZ || low > high || halfWidth < 0 || Math.abs(aimX * aimX + aimZ * aimZ - 1) > 1e-6) return true;
		double center = (low + high) * .5, along = (high - low) * .5;
		double dx = (minX + maxX) * .5 - aimX * center, dz = (minZ + maxZ) * .5 - aimZ * center;
		double rx = (maxX - minX) * .5, rz = (maxZ - minZ) * .5;
		return Math.abs(dx) <= rx + Math.abs(aimX) * along + Math.abs(aimZ) * halfWidth + 1e-9
			&& Math.abs(dz) <= rz + Math.abs(aimZ) * along + Math.abs(aimX) * halfWidth + 1e-9
			&& Math.abs(dx * aimX + dz * aimZ) <= along + rx * Math.abs(aimX) + rz * Math.abs(aimZ) + 1e-9
			&& Math.abs(-dx * aimZ + dz * aimX) <= halfWidth + rx * Math.abs(aimZ) + rz * Math.abs(aimX) + 1e-9;
	}
}
