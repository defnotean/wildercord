package dev.wildercord.world.hollowbell;

/** Server hit shapes and matching client ground markings, measured from the Bell Sanctum's central resonance plinth. */
public final class HollowBellRules {
	private HollowBellRules() {}
	public static final int IDLE = 0, TOLL = 1, CLAPPER = 2, CHORD = 3;
	public static final int START = 60, QUIET_RESET = 200, RETIRE = 36, MAX_CHALLENGERS = 64;
	public static final double ARENA = 14, RING_INNER = 3.0, RING_OUTER = 6.5;
	public static final double CLAPPER_RADIUS = 2.25, SAFE_HALF_WIDTH = 1.75;
	public static final float HEALTH = 180;

	public static int nextMove(int completed, boolean second) {
		int index = Math.floorMod(completed, 3);
		return second ? (index + 2) % 3 + 1 : index + 1;
	}
	public static int tell(int move) { return switch (move) { case TOLL -> 32; case CLAPPER -> 44; case CHORD -> 56; default -> 0; }; }
	public static int recovery(int move) { return switch (move) { case TOLL -> 24; case CLAPPER -> 32; case CHORD -> 40; default -> RETIRE; }; }
	public static float damage(int move) { return switch (move) { case TOLL -> 6; case CLAPPER -> 5; case CHORD -> 7; default -> 0; }; }
	public static String key(int move) { return switch (move) { case TOLL -> "toll"; case CLAPPER -> "clapper"; case CHORD -> "chord"; default -> "idle"; }; }
	public static boolean second(float health, float maximum) { return maximum > 0 && health <= maximum * .5F; }
	public static boolean within(double x, double y, double z) {
		return Math.abs(x) <= ARENA && Math.abs(z) <= ARENA && y >= -1 && y <= 4;
	}
	/** Annular acoustic pressure wave: the quiet eye directly beneath the bell (< 3.0) and outer hall (> 6.5) remain safe. */
	public static boolean toll(double x, double z) {
		double distance = Math.hypot(x, z);
		return distance >= RING_INNER && distance <= RING_OUTER;
	}
	public static boolean clapper(double x, double z, double markX, double markZ) {
		return Math.hypot(x - markX, z - markZ) <= CLAPPER_RADIUS;
	}
	/** The damped acoustic shutter lane remains at least 3.5 blocks wide for the whole tell and impact. */
	public static boolean chord(double x, double z, boolean acrossX) {
		return Math.abs(x) <= ARENA && Math.abs(z) <= ARENA && Math.abs(acrossX ? z : x) > SAFE_HALF_WIDTH;
	}
	public static boolean hits(int move, double x, double y, double z, double markX, double markZ, boolean acrossX) {
		if (!within(x, y, z)) return false;
		return switch (move) {
			case TOLL -> toll(x, z);
			case CLAPPER -> clapper(x, z, markX, markZ);
			case CHORD -> chord(x, z, acrossX);
			default -> false;
		};
	}
}
