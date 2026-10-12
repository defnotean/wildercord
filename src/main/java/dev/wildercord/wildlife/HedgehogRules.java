package dev.wildercord.wildlife;

/**
 * The thistle hedgehog's numbers and decisions, kept pure for its tests: when something startles it into a ball, how long it
 * stays curled and when it peeks out, how sharp its quills are, how long a handful of berries buys its trust, and which
 * forage gift it brings.
 */
public final class HedgehogRules {
	private HedgehogRules() {}

	/** How near someone running past has to come to startle it, in blocks. */
	public static final double STARTLE_RANGE = 4.0;
	/** How long it stays curled up, and how much of the end of that it spends peeking out. */
	public static final int CURL_TICKS = 120, PEEK_TICKS = 35;
	/** What a blow does to it curled up (a share of the damage), and what its quills do back to whoever struck it bare-handed or with a blade. */
	public static final float CURLED_TAKES = 0.4F, PRICK = 1.0F;
	/** How long a handful of sweet berries buys its trust, and how near it then keeps to whoever fed it. */
	public static final int TRUST_TICKS = 2400;
	public static final double FOLLOW_FAR = 4.0, FOLLOW_NEAR = 1.6;
	/** When, after a feeding, it may bring something it found (a wait between the two, in ticks), and the chance that it does. */
	public static final int GIFT_MIN = 300, GIFT_MAX = 900;
	public static final double GIFT_CHANCE = 0.6;
	/** How near whoever fed it it has to be to hand the gift over. */
	public static final double GIFT_REACH = 3.0;
	/** About one tick in this many, it sneezes. */
	public static final int SNEEZE_ODDS = 900;

	/** What it forages, by weight: sweet berries, a brown mushroom, a red one, a dandelion, an apple, a beetroot seed. */
	public static final int BERRIES = 0, BROWN_MUSHROOM = 1, RED_MUSHROOM = 2, DANDELION = 3, APPLE = 4, SEEDS = 5;
	private static final int[] GIFT_WEIGHTS = {5, 3, 2, 3, 1, 2};

	/** Whether someone running by startles it: near enough, and not someone it trusts. Walking or sneaking past never does. */
	public static boolean startles(boolean sprinting, double distance, boolean trusted) {
		return sprinting && !trusted && distance <= STARTLE_RANGE;
	}

	/** Curled with {@code left} ticks to go: whether it is peeking out yet. */
	public static boolean peeking(int left) {
		return left > 0 && left <= PEEK_TICKS;
	}

	/** A blow it takes curled up, after its quills. */
	public static float blunted(float amount, boolean curled) {
		return curled ? amount * CURLED_TAKES : amount;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}

	/** When, after a feeding, its gift comes, given a roll in [0, 1). */
	public static int giftDelay(double roll) {
		return GIFT_MIN + (int) (roll * (GIFT_MAX - GIFT_MIN));
	}

	/** Which gift a roll in [0, 1) brings. */
	public static int gift(double roll) {
		int total = 0;
		for (int w : GIFT_WEIGHTS) {
			total += w;
		}
		double at = roll * total;
		for (int i = 0; i < GIFT_WEIGHTS.length; i++) {
			at -= GIFT_WEIGHTS[i];
			if (at < 0) {
				return i;
			}
		}
		return GIFT_WEIGHTS.length - 1;
	}
}
