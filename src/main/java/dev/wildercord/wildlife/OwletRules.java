package dev.wildercord.wildlife;

/**
 * The hushwing owlet's numbers and decisions, kept pure for its tests: when it sleeps, which creaking it stares down and for
 * how long its stare holds one, how long a scrap of meat buys its trust, and how often it then hoots out what's stalking its
 * friend.
 */
public final class OwletRules {
	private OwletRules() {}

	/** Woken (struck, fed), how long it stays up even by day. */
	public static final int WAKE_TICKS = 1200;
	/** How far it sees a creaking to stare it down. */
	public static final double STARE_RANGE = 12.0;
	/** How long a scrap buys its trust, and how near it then keeps. */
	public static final int TRUST_TICKS = 3600;
	public static final double FOLLOW_FAR = 4.0, FOLLOW_NEAR = 2.0;
	/** Trusting, how far it watches for anything stalking its friend, how often it hoots them out, and how long they glow for it. */
	public static final double WARN_RANGE = 14.0;
	public static final int WARN_TICKS = 200, WARN_GLOW = 80;
	/** How much slower than a stone it falls, fluttering. */
	public static final double FLUTTER = 0.6;

	/** Whether it sleeps: by day, unless something woke it a little while ago. */
	public static boolean sleeps(boolean day, long sinceWoken) {
		return day && (sinceWoken < 0 || sinceWoken >= WAKE_TICKS);
	}

	/** Whether it stares down a creaking this far off: awake, near enough, and in sight. */
	public static boolean stares(boolean asleep, double distance, boolean sees) {
		return !asleep && sees && distance <= STARE_RANGE;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}

	/** Whether it hoots out what's stalking its friend now: awake, something there, and long enough since the last. */
	public static boolean warns(boolean asleep, int stalkers, long sinceLast) {
		return !asleep && stalkers > 0 && (sinceLast < 0 || sinceLast >= WARN_TICKS);
	}

	/** Its fall a tick, fluttering: slowed while it drops, left alone while it rises. */
	public static double flutter(double dy) {
		return dy < 0 ? dy * FLUTTER : dy;
	}
}
