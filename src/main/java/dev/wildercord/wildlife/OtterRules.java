package dev.wildercord.wildlife;

/**
 * The brook otter's numbers and decisions, kept pure for its tests: when it rolls onto its back to float, when it knocks a shell
 * on its belly or juggles a pebble ashore, when two floating otters drift together to hold paws and which paw each reaches with,
 * when it hunts a fish, how long a fish buys its trust, and when a trusting otter brings a gift.
 */
public final class OtterRules {
	private OtterRules() {}

	/** What it is doing, synced so the model can pose it. */
	public static final int IDLE = 0, FLOAT = 1, CRACK = 2, JUGGLE = 3, HOLD = 4;
	/** What it holds on its chest while it knocks or eats: a shell, or a fish it caught. */
	public static final int SHELL = 0, FISH = 1;

	/** How long it floats on its back at a time, and about one idle tick in this many, in water, it rolls over to float. */
	public static final int FLOAT_TICKS = 360, FLOAT_ODDS = 160;
	/** Floating, about one tick in this many it brings up a shell to knock on its belly; how long the knocking (or a meal) goes on. */
	public static final int CRACK_ODDS = 220, CRACK_TICKS = 90;
	/** Knocks fall this many ticks apart. */
	public static final int KNOCK_EVERY = 9;
	/** Ashore, about one idle tick in this many it sits up to juggle a pebble, and for how long. */
	public static final int JUGGLE_ODDS = 900, JUGGLE_TICKS = 100;
	/** How near another floating otter must be to drift over and take its paw, how close they float side by side, and how long they hold on. */
	public static final double HOLD_RANGE = 6.0, HOLD_GAP = 0.58;
	public static final int HOLD_TICKS = 500;
	/** How far off it notices a fish, how long between hunts, and how hard it bites. */
	public static final double HUNT_RANGE = 10.0;
	public static final int HUNT_REST = 900;
	public static final float BITE = 6.0F;
	/** How long a fish buys its trust, how near it then keeps to whoever fed it, and how long between its gifts. */
	public static final int TRUST_TICKS = 2400, GIFT_REST = 1200;
	public static final double FOLLOW_FAR = 6.0, FOLLOW_NEAR = 2.5, GIFT_RANGE = 5.0;

	/** Whether it rolls over to float: grown or not, it must be in water and doing nothing else. */
	public static boolean floats(boolean inWater, int pose) {
		return inWater && pose == IDLE;
	}

	/** Whether two otters take each other's paws: both grown, both floating on their own, and near enough. */
	public static boolean holds(int pose, int otherPose, boolean eitherBaby, double distance) {
		return pose == FLOAT && otherPose == FLOAT && !eitherBaby && distance <= HOLD_RANGE;
	}

	/**
	 * Which side of the leader the partner floats on, from where the partner lies: {@code rightDot} is the partner's offset dotted
	 * with the leader's right. True for its right, so the leader reaches with its right paw and the partner with its left.
	 */
	public static boolean partnerOnRight(double rightDot) {
		return rightDot >= 0;
	}

	/** Whether it goes after a fish: grown, the fish near enough, and rested since its last hunt. */
	public static boolean hunts(boolean baby, double distance, long sinceLastHunt) {
		return !baby && distance <= HUNT_RANGE && sinceLastHunt >= HUNT_REST;
	}

	/** Knocking with {@code left} ticks to go: whether the shell comes down on its belly this tick. */
	public static boolean knocks(int left) {
		return left > 6 && left % KNOCK_EVERY == 0;
	}

	/** How high the juggled pebble is between its paws at a tick into the juggling, in model pixels, and which paw it is over (-1 to 1). */
	public static float tossHeight(float age) {
		float t = age % 12;
		return 5 * (float) Math.sin(Math.PI * t / 12.0);
	}

	public static float tossSide(float age) {
		float t = age % 24;
		return t < 12 ? -1 + t / 6F : 1 - (t - 12) / 6F;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}

	/** Whether a trusting otter brings a gift: near whoever it trusts and rested since its last. */
	public static boolean gifts(boolean trusted, double distance, long sinceLastGift) {
		return trusted && distance <= GIFT_RANGE && sinceLastGift >= GIFT_REST;
	}

	/** Which gift, from a roll in [0, 100): clay most often, then kelp, then flint, and now and then a gold nugget. */
	public static int gift(int roll) {
		return roll < 40 ? 0 : roll < 70 ? 1 : roll < 94 ? 2 : 3;
	}
}
