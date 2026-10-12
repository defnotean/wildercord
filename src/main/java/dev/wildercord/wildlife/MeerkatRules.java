package dev.wildercord.wildlife;

/**
 * The sunwatch meerkat's numbers and decisions, kept pure for its tests: who keeps watch and how far a watcher sees, what counts
 * as a threat, how long a mob stays down its holes after the alarm and when it peeks back out, when it suns itself and when it
 * sleeps, and how long a gift of food buys its trust.
 */
public final class MeerkatRules {
	private MeerkatRules() {}

	/** What it is doing, synced so the model can pose it. */
	public static final int IDLE = 0, WATCH = 1, HIDE = 2, SUN = 3, DIG = 4, SLEEP = 5, EAT = 6;

	/** How long a watch lasts, and how far a watcher (up on its hind legs) and anyone else (nose down) notices a threat. */
	public static final int WATCH_TICKS = 400;
	public static final double WATCH_SIGHT = 12.0, GROUND_SIGHT = 5.0;
	/** How far a watcher looks for a higher spot to stand on, and how near another watcher keeps it from taking a turn. */
	public static final int LOOKOUT_REACH = 5;
	public static final double WATCH_SPACING = 12.0;
	/** How far an alarm carries, how long they stay down their holes after it, and how long of that they spend peeking. */
	public static final double ALARM_RANGE = 16.0;
	public static final int HIDE_TICKS = 140, PEEK_TICKS = 40;
	/** How long it digs, suns itself, eats and sleeps. */
	public static final int DIG_TICKS = 60, SUN_TICKS = 300, EAT_TICKS = 50, SLEEP_TICKS = 1200;
	/** About one idle tick in this many it digs, takes a turn on watch, or (in the morning) suns itself. */
	public static final int DIG_ODDS = 220, WATCH_ODDS = 160, SUN_ODDS = 120, SLEEP_ODDS = 80;
	/** How near the others it settles down at night: it trots over until it is this close. */
	public static final double HUDDLE_NEAR = 1.3, HUDDLE_FAR = 10.0;
	/** How long a gift of food buys its trust, and how near it then keeps to whoever fed it. */
	public static final int TRUST_TICKS = 2400;
	public static final double FOLLOW_FAR = 5.0, FOLLOW_NEAR = 2.0;

	/**
	 * Whether a player this far off is a threat worth an alarm: a stranger walking upright in plain sight is; one creeping up
	 * crouched isn't seen until they are right there; someone it trusts never is.
	 */
	public static boolean threatens(double distance, boolean watching, boolean sneaking, boolean trusted) {
		if (trusted) {
			return false;
		}
		double sight = watching ? WATCH_SIGHT : GROUND_SIGHT;
		return distance <= (sneaking ? sight / 3 : sight);
	}

	/** Whether it suns itself at this time of day: the early morning, belly to the sun to warm up. */
	public static boolean morning(long timeOfDay) {
		long t = Math.floorMod(timeOfDay, 24000L);
		return t < 2500 || t >= 23500;
	}

	/** Whether it is time to sleep: from dusk until just before dawn, all down together. */
	public static boolean night(long timeOfDay) {
		long t = Math.floorMod(timeOfDay, 24000L);
		return t >= 12800 && t < 23200;
	}

	/** How far it has come back up out of its hole, 0 (right down) to 1 (only its head out), with {@code left} ticks of hiding to go. */
	public static float peek(int left) {
		if (left >= PEEK_TICKS) {
			return 0;
		}
		float p = 1 - (float) left / PEEK_TICKS;
		return Math.min(1, p * 1.6F);
	}

	/** Whether it takes a turn on watch: nobody else is on watch near enough already. */
	public static boolean takesWatch(double nearestWatcher) {
		return nearestWatcher > WATCH_SPACING;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}
}
