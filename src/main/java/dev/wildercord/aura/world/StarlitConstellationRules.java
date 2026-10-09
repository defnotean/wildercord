package dev.wildercord.aura.world;

/**
 * Starlit signature: four stars are marked round the challenger and joined in the order they will burst. Each burst
 * is a small column, so the answer is to read the drawing and leave each star before its turn (or stand on a spent one).
 */
public final class StarlitConstellationRules {
	private StarlitConstellationRules() {}

	public static final int STARS = 4, FIRST = 14, GAP = 6, TELL = FIRST + GAP * (STARS - 1), RECOVERY = 16;
	public static final int COOLDOWN = 160, WARNING_REFRESH = 3;
	public static final double COST = 30, DAMAGE = 18, RADIUS = 1.6, SPREAD = 2.5, LOW = -.5, HIGH = 2.5;
	public static final double MIN_DISTANCE = 2, MAX_DISTANCE = 7, MAX_HEIGHT = 1.5;

	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == MastersPackB.STARLIT && Math.floorMod(sequence, 4) == 1 && now >= readyAt
			&& Double.isFinite(aura) && aura >= COST + MastersRules.GUARD_COST && aura <= MastersRules.AURA_MAX
			&& Double.isFinite(distance) && distance >= MIN_DISTANCE && distance <= MAX_DISTANCE
			&& Double.isFinite(height) && Math.abs(height) <= MAX_HEIGHT;
	}

	/** Tick, from the start, on which star {@code index} bursts; the drawing order is the burst order. */
	public static int burst(int index) {
		if (index < 0 || index >= STARS) throw new IllegalArgumentException("No such star: " + index);
		return FIRST + GAP * index;
	}

	/** Star offsets from the challenger's feet, as {along the master's aim, across it}: under them, right, left, behind. */
	public static double[] offset(int index) {
		return switch (index) {
			case 0 -> new double[] {0, 0};
			case 1 -> new double[] {0, SPREAD};
			case 2 -> new double[] {0, -SPREAD};
			case 3 -> new double[] {SPREAD, 0};
			default -> throw new IllegalArgumentException("No such star: " + index);
		};
	}

	/** Feet within the star's column when it bursts. */
	public static boolean hits(double dx, double dz, double dy) {
		return Double.isFinite(dx) && Double.isFinite(dz) && Double.isFinite(dy)
			&& dx * dx + dz * dz <= RADIUS * RADIUS && dy >= LOW && dy <= HIGH;
	}
}
