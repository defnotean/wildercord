package dev.wildercord.world.kilnwidow;

/** Server hit shapes and matching client ground markings, measured from the Kiln Court's central firing plinth. */
public final class KilnWidowRules {
	private KilnWidowRules() {}
	public static final int IDLE = 0, DRAFT = 1, SLAG = 2, SWEEP = 3;
	public static final int START = 60, QUIET_RESET = 200, RETIRE = 36, MAX_CHALLENGERS = 64;
	public static final double ARENA = 14, RING_INNER = 3.0, RING_OUTER = 6.5;
	public static final double SLAG_RADIUS = 2.25, SAFE_HALF_WIDTH = 1.75;
	public static final float HEALTH = 180;

	public static int nextMove(int completed, boolean second) {
		int index = Math.floorMod(completed, 3);
		return second ? (index + 2) % 3 + 1 : index + 1;
	}
	public static int tell(int move) { return switch (move) { case DRAFT -> 32; case SLAG -> 44; case SWEEP -> 56; default -> 0; }; }
	public static int recovery(int move) { return switch (move) { case DRAFT -> 24; case SLAG -> 32; case SWEEP -> 40; default -> RETIRE; }; }
	public static float damage(int move) { return switch (move) { case DRAFT -> 6; case SLAG -> 5; case SWEEP -> 7; default -> 0; }; }
	public static String key(int move) { return switch (move) { case DRAFT -> "draft"; case SLAG -> "slag"; case SWEEP -> "sweep"; default -> "idle"; }; }
	public static boolean second(float health, float maximum) { return maximum > 0 && health <= maximum * .5F; }
	public static boolean within(double x, double y, double z) {
		return Math.abs(x) <= ARENA && Math.abs(z) <= ARENA && y >= -1 && y <= 4;
	}
	/** Annular kiln-draft wave: the cooled hearth directly beside the plinth (< 3.0) and outer firebreak (> 6.5) remain safe. */
	public static boolean draft(double x, double z) {
		double distance = Math.hypot(x, z);
		return distance >= RING_INNER && distance <= RING_OUTER;
	}
	public static boolean slag(double x, double z, double markX, double markZ) {
		return Math.hypot(x - markX, z - markZ) <= SLAG_RADIUS;
	}
	/** The cooled firebreak flue lane remains at least 3.5 blocks wide for the whole tell and impact. */
	public static boolean sweep(double x, double z, boolean acrossX) {
		return Math.abs(x) <= ARENA && Math.abs(z) <= ARENA && Math.abs(acrossX ? z : x) > SAFE_HALF_WIDTH;
	}
	public static boolean hits(int move, double x, double y, double z, double markX, double markZ, boolean acrossX) {
		if (!within(x, y, z)) return false;
		return switch (move) {
			case DRAFT -> draft(x, z);
			case SLAG -> slag(x, z, markX, markZ);
			case SWEEP -> sweep(x, z, acrossX);
			default -> false;
		};
	}
}
