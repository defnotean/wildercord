package dev.wildercord.wildlife;

/**
 * The stonecrest puffin's numbers and decisions, kept pure for its tests: when it dives and how often it comes up with fish,
 * who gets the fish it brings back, how slowly it flutters down, when it sleeps, and how long a gift of fish buys its trust.
 */
public final class PuffinRules {
	private PuffinRules() {}

	/** What it is doing, synced so the model can pose it. */
	public static final int IDLE = 0, DIVE = 1, FLY = 2, PREEN = 3, STRETCH = 4, TAP = 5, SLEEP = 6, EAT = 7;

	/** How long a dive, a flight hop, a preen, a wing stretch, a bill-tap, a meal and a night's sleep last. */
	public static final int DIVE_TICKS = 70, FLY_TICKS = 50, PREEN_TICKS = 70, STRETCH_TICKS = 24, TAP_TICKS = 60, EAT_TICKS = 40,
		SLEEP_TICKS = 1200;
	/** About one idle tick in this many it dives (afloat), preens, stretches its wings, looks for its mate, or takes off. */
	public static final int DIVE_ODDS = 90, PREEN_ODDS = 320, STRETCH_ODDS = 380, TAP_ODDS = 420, FLY_ODDS = 300, SLEEP_ODDS = 60;
	/** How deep the water must be under it to dive, and how far it looks for the sea to fly out to. */
	public static final int DIVE_DEPTH = 2, SEA_REACH = 10;
	/** How near its mate must be to bill-tap. */
	public static final double MATE_NEAR = 4.0;
	/** How long it carries its catch before eating it itself, and how far it will take it to someone. */
	public static final int CARRY_TICKS = 300;
	public static final double BRING_FAR = 16.0, BRING_NEAR = 1.6;
	/** How fast it may fall while it beats its wings. */
	public static final double GLIDE = 0.09;
	/** How long a gift of fish buys its trust, and how near it then keeps to whoever fed it. */
	public static final int TRUST_TICKS = 2400;
	public static final double FOLLOW_FAR = 5.0, FOLLOW_NEAR = 2.0;

	/** Who gets the fish it carries. */
	public static final int KEEP = 0, GIFT = 1, FEED = 2;

	/**
	 * Who gets its catch: a puffling near enough comes first, then someone it trusts (once per feeding), and otherwise it keeps it
	 * and eats it itself. {@code -1} for nobody there.
	 */
	public static int catchFor(double puffling, double truster, boolean gifted) {
		if (puffling >= 0 && puffling <= BRING_FAR) {
			return FEED;
		}
		if (truster >= 0 && truster <= BRING_FAR && !gifted) {
			return GIFT;
		}
		return KEEP;
	}

	/** Whether it comes up with fish: two dives in three. */
	public static boolean catches(int roll3) {
		return roll3 != 0;
	}

	/** Whether the water is deep enough to dive. */
	public static boolean deepEnough(int depth) {
		return depth >= DIVE_DEPTH;
	}

	/** Its fall slowed by its whirring wings. */
	public static double glide(double dy) {
		return Math.max(dy, -GLIDE);
	}

	/** Whether it is time to sleep: from dusk until just before dawn. */
	public static boolean night(long timeOfDay) {
		long t = Math.floorMod(timeOfDay, 24000L);
		return t >= 13000 && t < 23000;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}
}
