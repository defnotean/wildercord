package dev.wildercord.wildlife;

/**
 * The cinnabar panda's numbers and decisions, kept pure for its tests: when it rears up at a stranger, when it naps through the
 * bright middle of the day with its tail over its face, when it washes, forages a berry bush or tumbles with another, how its
 * tumble rolls it over, and how long a handful of berries buys its trust.
 */
public final class RedPandaRules {
	private RedPandaRules() {}

	/** What it is doing, synced so the model can pose it. */
	public static final int IDLE = 0, STAND = 1, WASH = 2, SLEEP = 3, EAT = 4, TUMBLE = 5;

	/** How long it rears up on its hind legs with its paws spread, and how near a stranger has to come to make it. */
	public static final int STAND_TICKS = 40;
	public static final double STAND_RANGE = 6.0;
	/** How long it washes its face, eats, naps and tumbles. */
	public static final int WASH_TICKS = 70, EAT_TICKS = 60, SLEEP_TICKS = 900, TUMBLE_TICKS = 72;
	/** About one idle tick in this many it washes, one in this many it looks for someone to tumble with, one in this many (drowsy) it naps. */
	public static final int WASH_ODDS = 500, PLAY_ODDS = 700, SLEEP_ODDS = 160;
	/** How near another has to be to tumble with, how far off it smells a ripe berry bush, and how long between forages. */
	public static final double PLAY_RANGE = 3.5, FORAGE_RANGE = 10.0;
	public static final int FORAGE_REST = 1200;
	/** How long a handful of berries buys its trust, and how near it then keeps to whoever fed it. */
	public static final int TRUST_TICKS = 2400;
	public static final double FOLLOW_FAR = 5.0, FOLLOW_NEAR = 2.0;
	/** How long one roll of a tumble takes, and how long it lies sprawled after before the next. */
	public static final int ROLL_TICKS = 16, ROLL_PAUSE = 8;

	/** Whether a stranger, this far off, makes it rear up (asleep, it only rears up if they come right up to it). */
	public static boolean rears(double distance, boolean trusted, boolean asleep) {
		return !trusted && distance <= (asleep ? STAND_RANGE / 2 : STAND_RANGE);
	}

	/** Whether it is drowsy at this time of day: through the bright middle of the day, as red pandas are. */
	public static boolean drowsy(long timeOfDay) {
		long t = Math.floorMod(timeOfDay, 24000L);
		return t >= 3000 && t < 9000;
	}

	/** Whether it goes to forage a ripe berry bush: grown or not, rested since it last ate. */
	public static boolean forages(long sinceLastMeal) {
		return sinceLastMeal >= FORAGE_REST;
	}

	/** Whether it tumbles with another this far off: both awake and idle, and near. */
	public static boolean tumbles(double distance, boolean otherIdle) {
		return otherIdle && distance <= PLAY_RANGE;
	}

	/** How far over a tumble has rolled it at a tick into the tumble, in radians: one eased full roll, then a sprawled pause. */
	public static float roll(float age) {
		float t = age % (ROLL_TICKS + ROLL_PAUSE);
		if (t >= ROLL_TICKS) {
			return 0;
		}
		float p = t / ROLL_TICKS;
		return (float) (Math.PI * 2 * (p * p * (3 - 2 * p)));
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}
}
