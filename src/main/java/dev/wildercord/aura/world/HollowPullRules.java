package dev.wildercord.aura.world;

/**
 * Hollow's signature: a gravity well. For most of its warning the Master drags every challenger within reach toward
 * itself, a tug each tick; then the core collapses. Sprinting breaks the well's hold, so sprint against it and be outside
 * the core when it closes. Standing or merely walking is dragged in.
 */
public final class HollowPullRules {
	private HollowPullRules() {}
	public static final int TELL = 40, PULL_FROM = 4, RECOVERY = 16, WARNING_REFRESH = 4;
	public static final int END = TELL + RECOVERY;
	public static final double COST = 26, DAMAGE = 26, PULL = .25, REACH = 10, CORE = 4.5, NEAR = 3, FAR = 6, HOLD = .75;
	/** Vanilla ground slipperiness times drag, used only by {@link #drift}'s planning model. */
	public static final double FRICTION = .546;

	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == ElementalMasters.HOLLOW && ElementalMasters.eligible(school, sequence, aura, now, readyAt, COST)
			&& Double.isFinite(distance) && distance >= NEAR && distance <= FAR && Double.isFinite(height) && Math.abs(height) <= 1;
	}

	public static boolean pulling(int age) { return age >= PULL_FROM && age < TELL; }

	/** The inward tug (x, z) on a challenger at offset (x, z) from the well; zero when sprinting, beyond reach or at its heart. */
	public static double[] tug(double x, double z, boolean sprinting) {
		if (sprinting || !Double.isFinite(x) || !Double.isFinite(z)) return new double[] {0, 0};
		double distance = Math.sqrt(x * x + z * z);
		if (distance <= HOLD || distance > REACH) return new double[] {0, 0};
		return new double[] {-x / distance * PULL, -z / distance * PULL};
	}

	public static boolean hits(double x, double z, double height) {
		if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(height)) return false;
		return x * x + z * z <= CORE * CORE && Math.abs(height) <= 2;
	}

	/**
	 * Planning model only: radial distance at the collapse for a challenger starting {@code distance} out and moving straight
	 * away at {@code speed} blocks per tick (0 standing, about .215 walking, .28 sprinting). Needs playtesting with real input.
	 */
	public static double drift(double distance, double speed, boolean sprinting) {
		double velocity = 0, at = distance;
		for (int age = 0; age < TELL; age++) {
			if (pulling(age) && tug(at, 0, sprinting)[0] != 0) velocity -= PULL;
			velocity = velocity * FRICTION + speed * (1 - FRICTION);
			at = Math.max(0, at + velocity);
		}
		return at;
	}
}
