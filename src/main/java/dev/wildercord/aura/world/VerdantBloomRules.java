package dev.wildercord.aura.world;

/**
 * Verdant's signature: a rooting bloom ring. Petals ring the spot the challenger stood on when it began; when the bloom
 * opens, anyone standing inside it is cut and rooted. Leave the ring, or be in the air as it opens.
 */
public final class VerdantBloomRules {
	private VerdantBloomRules() {}
	public static final int TELL = 30, RECOVERY = 16, WARNING_REFRESH = 4, ROOT_TICKS = 40, ROOT_AMPLIFIER = 3;
	public static final int END = TELL + RECOVERY;
	public static final double COST = 26, DAMAGE = 22, RADIUS = 3, NEAR = 2, FAR = 7;
	public static final double LOW = -.5, HIGH = .75;

	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == ElementalMasters.VERDANT && ElementalMasters.eligible(school, sequence, aura, now, readyAt, COST)
			&& Double.isFinite(distance) && distance >= NEAR && distance <= FAR && Double.isFinite(height) && Math.abs(height) <= 1;
	}

	/** Feet at (x, height, z) relative to the bloom's centre. */
	public static boolean hits(double x, double z, double height) {
		if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(height)) return false;
		return x * x + z * z <= RADIUS * RADIUS && height >= LOW && height <= HIGH;
	}
}
