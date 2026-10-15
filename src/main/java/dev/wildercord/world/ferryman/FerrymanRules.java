package dev.wildercord.world.ferryman;

/** Server hit shapes and the matching client markings, measured from the raft's fixed mooring. */
public final class FerrymanRules {
	private FerrymanRules() {}
	public static final int IDLE = 0, SWEEP = 1, ANCHOR = 2, WAKE = 3;
	public static final int START = 60, QUIET_RESET = 200, RETIRE = 36, MAX_CHALLENGERS = 64;
	public static final double ARENA = 14, SWEEP_REACH = 6.5, SWEEP_HALF_ANGLE = Math.toRadians(70);
	public static final double ANCHOR_RADIUS = 2.25, SAFE_HALF_WIDTH = 1.75;
	public static final float HEALTH = 180;

	public static int nextMove(int completed, boolean second) {
		int index = Math.floorMod(completed, 3);
		return second ? (index + 2) % 3 + 1 : index + 1;
	}
	public static int tell(int move) { return switch (move) { case SWEEP -> 32; case ANCHOR -> 44; case WAKE -> 56; default -> 0; }; }
	public static int recovery(int move) { return switch (move) { case SWEEP -> 24; case ANCHOR -> 32; case WAKE -> 40; default -> RETIRE; }; }
	public static float damage(int move) { return switch (move) { case SWEEP -> 6; case ANCHOR -> 5; case WAKE -> 7; default -> 0; }; }
	public static String key(int move) { return switch (move) { case SWEEP -> "sweep"; case ANCHOR -> "anchor"; case WAKE -> "wake"; default -> "idle"; }; }
	public static boolean second(float health, float maximum) { return maximum > 0 && health <= maximum * .5F; }
	public static boolean within(double x, double y, double z) {
		return Math.abs(x) <= ARENA && Math.abs(z) <= ARENA && y >= -1 && y <= 4;
	}
	/** Yaw is in radians, with zero along positive Z, positive toward positive X. */
	public static boolean sweep(double x, double z, double yaw) {
		double distance = Math.hypot(x, z);
		return distance <= SWEEP_REACH && (distance < 1e-6 || (x * Math.sin(yaw) + z * Math.cos(yaw)) / distance >= Math.cos(SWEEP_HALF_ANGLE));
	}
	public static boolean anchor(double x, double z, double markX, double markZ) {
		return Math.hypot(x - markX, z - markZ) <= ANCHOR_RADIUS;
	}
	/** The crossing remains at least 3.5 blocks wide for the whole tell and impact. */
	public static boolean wake(double x, double z, boolean acrossX) {
		return Math.abs(x) <= ARENA && Math.abs(z) <= ARENA && Math.abs(acrossX ? z : x) > SAFE_HALF_WIDTH;
	}
	public static boolean hits(int move, double x, double y, double z, double yaw, double markX, double markZ, boolean acrossX) {
		if (!within(x, y, z)) return false;
		return switch (move) {
			case SWEEP -> sweep(x, z, yaw);
			case ANCHOR -> anchor(x, z, markX, markZ);
			case WAKE -> wake(x, z, acrossX);
			default -> false;
		};
	}
}
