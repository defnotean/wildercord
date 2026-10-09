package dev.wildercord.monster;

/**
 * Bounded pressure for two ordinary monsters. Difficulty changes idle time and pursuit, never health, damage, tells or
 * a missed attack's recovery. Easy keeps the original cadence; these rules do not change the world's difficulty.
 */
public final class MonsterPressureRules {
	private MonsterPressureRules() {}

	public static final int GLOOM_CROUCH = 14;
	public static final int GLOOM_MISSED = 30;
	public static final double GLOOM_POUNCE_MIN = 3.2;
	public static final double GLOOM_POUNCE_MAX = 9.5;
	public static final double GLOOM_MAX_RISE = 2.0;
	public static final double GLOOM_MAX_DROP = 3.0;
	public static final int GLOOM_CANCEL_RETRY = 20;
	public static final double GLOOM_CIRCLE_RADIUS = 6.0;
	public static final double GLOOM_CIRCLE_STEP = 0.35;
	public static final double GLOOM_CIRCLE_SPEED = 0.9;
	public static final double GLOOM_SWIPE_RANGE = 2.2;
	public static final int GLOOM_SWIPE_INTERVAL = 20;
	public static final int GLOOM_RETREAT_CANDIDATES = 6;
	public static final double GLOOM_RETREAT_SPEED = 1.35;
	public static final int GLOOM_RETREAT_HEIGHT = 6;

	public static final int FROG_SWELL = 18;
	public static final int FROG_MOUTH = 10;
	public static final int FROG_GULP = 20;
	public static final double FROG_TONGUE_MIN = 2.8;
	public static final double FROG_TONGUE_RANGE = 6.0;
	public static final double FROG_BUBBLE_MIN = 4.0;
	public static final double FROG_BUBBLE_MAX = 16.0;
	public static final double FROG_APPROACH_RANGE = 12.0;

	/** A successful route is refreshed at most this often; a failed route is retried more slowly. */
	public static final int REPATH = 8;
	public static final int FAILED_REPATH = 20;

	/** Inclusive interval, sampled with nextInt(spread()); a separate value makes every endpoint testable. */
	public record Delay(int min, int max) {
		public int spread() { return max - min + 1; }
		public int sample(int roll) { return min + Math.floorMod(roll, spread()); }
	}

	private static int difficulty(int difficulty) {
		return Math.max(0, Math.min(3, difficulty));
	}

	public static boolean active(int difficulty) {
		return difficulty(difficulty) >= 2;
	}

	public static Delay gloomStalk(int difficulty, boolean afterRetreat) {
		return switch (difficulty(difficulty)) {
			case 2 -> afterRetreat ? new Delay(28, 47) : new Delay(26, 45);
			case 3 -> afterRetreat ? new Delay(22, 37) : new Delay(20, 35);
			default -> afterRetreat ? new Delay(50, 99) : new Delay(40, 79);
		};
	}

	public static Delay gloomRetreat(int difficulty) {
		return switch (difficulty(difficulty)) {
			case 2 -> new Delay(32, 47);
			case 3 -> new Delay(26, 39);
			default -> new Delay(50, 79);
		};
	}

	public static double gloomChaseSpeed(int difficulty) {
		return switch (difficulty(difficulty)) {
			case 2 -> 1.20;
			case 3 -> 1.25;
			default -> 1.10;
		};
	}

	/** Beyond the useful orbit, behind cover, or on a high ledge: use ground navigation, not a blind pounce. */
	public static boolean gloomChase(int difficulty, double distance, double height, boolean sight) {
		return active(difficulty) ? distance > 8.0 || !sight || !gloomHeight(height) : distance > 10.0;
	}

	public static boolean gloomCanCrouch(double distance, double height, boolean sight) {
		return distance >= GLOOM_POUNCE_MIN && gloomCanRelease(distance, height, sight);
	}

	/** Rechecked after the whole tell. Closing into melee still permits the leap; leaving its reach or sight does not. */
	public static boolean gloomCanRelease(double distance, double height, boolean sight) {
		return sight && Double.isFinite(distance) && distance >= 0 && distance <= GLOOM_POUNCE_MAX && gloomHeight(height);
	}

	private static boolean gloomHeight(double height) {
		return Double.isFinite(height) && height <= GLOOM_MAX_RISE && height >= -GLOOM_MAX_DROP;
	}

	public static Delay frogBubble(int difficulty) {
		return switch (difficulty(difficulty)) {
			case 2 -> new Delay(60, 89);
			case 3 -> new Delay(50, 74);
			default -> new Delay(80, 129);
		};
	}

	/** A ready tongue stops close backpedalling; it still has its full mouth-open tell and shield counter. */
	public static boolean frogTongueFirst(int difficulty, double distance, boolean sight, boolean ready) {
		return active(difficulty) && ready && sight && distance >= FROG_TONGUE_MIN && distance <= FROG_TONGUE_RANGE;
	}

	public static boolean frogCanBubble(double distance, boolean sight) {
		return sight && distance >= FROG_BUBBLE_MIN && distance <= FROG_BUBBLE_MAX;
	}

	public static boolean frogApproach(int difficulty, double distance, boolean sight) {
		return active(difficulty) && (distance > FROG_APPROACH_RANGE || !sight);
	}

	public static double frogChaseSpeed(int difficulty) {
		return switch (difficulty(difficulty)) {
			case 2 -> 1.10;
			case 3 -> 1.15;
			default -> 1.0;
		};
	}

	public static int repathDelay(boolean routeFound) {
		return routeFound ? REPATH : FAILED_REPATH;
	}
}
