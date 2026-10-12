package dev.wildercord.wildlife;

/**
 * The capcrown snail's numbers and decisions, kept pure for its tests: what makes it draw into its shell and for how long,
 * what a blow does to it there, how many mushrooms its cap gives up to shears and how long the cap takes to grow back, and how
 * long a mushroom buys its trust.
 */
public final class SnailRules {
	private SnailRules() {}

	/** How near someone running past has to come to make it hide, and how near something hostile. */
	public static final double STARTLE_RANGE = 3.5, ENEMY_RANGE = 2.5;
	/** How long it stays in its shell, and how much of the end of that its eyes spend peeking out. */
	public static final int HIDE_TICKS = 100, PEEK_TICKS = 30;
	/** A poke on the eye with a bare hand: a shorter hide. */
	public static final int POKE_TICKS = 50;
	/** What a blow does to it hidden in its shell (a share of the damage). */
	public static final float HIDDEN_TAKES = 0.3F;
	/** Mushrooms its cap gives up to shears, the fewest and the most. */
	public static final int YIELD_MIN = 2, YIELD_MAX = 4;
	/** How long its cap takes to grow back after shearing, and how much sooner each mushroom it's fed meanwhile brings it. */
	public static final int REGROW_TICKS = 6000, FED_HASTENS = 1500;
	/** How long a mushroom buys its trust, and how near it then keeps to whoever fed it. */
	public static final int TRUST_TICKS = 2400;
	public static final double FOLLOW_FAR = 3.0, FOLLOW_NEAR = 1.4;
	/** In the rain it glides about a little quicker. */
	public static final double RAIN_SPEED = 1.35;

	/** Whether someone running by makes it hide: near enough, and not someone it trusts. Walking or sneaking past never does. */
	public static boolean startles(boolean sprinting, double distance, boolean trusted) {
		return sprinting && !trusted && distance <= STARTLE_RANGE;
	}

	/** Hidden with {@code left} ticks to go: whether its eyes are peeking out yet. */
	public static boolean peeking(int left) {
		return left > 0 && left <= PEEK_TICKS;
	}

	/** A blow it takes hidden in its shell. */
	public static float blunted(float amount, boolean hidden) {
		return hidden ? amount * HIDDEN_TAKES : amount;
	}

	/** How many mushrooms a shearing gives, given a roll in [0, 1). */
	public static int yield(double roll) {
		return YIELD_MIN + Math.min(YIELD_MAX - YIELD_MIN, (int) (roll * (YIELD_MAX - YIELD_MIN + 1)));
	}

	/** When a shorn cap is due back, after a mushroom fed to it: sooner, but never before now. */
	public static long hastened(long regrowAt, long now) {
		return Math.max(now, regrowAt - FED_HASTENS);
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}
}
