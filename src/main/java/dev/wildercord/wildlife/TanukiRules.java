package dev.wildercord.wildlife;

/**
 * The blossom tanuki's numbers and decisions, kept pure for its tests: what startles it into a bush and how long it stays
 * one, when it drums its belly, how long a scrap buys its trust, and when it shares its leaf trick with its friend.
 */
public final class TanukiRules {
	private TanukiRules() {}

	/** How near someone running (or anything that hurts it) startles it. */
	public static final double STARTLE_RANGE = 6.0;
	/** The leaf on its head, before it turns: how long it holds still with it there. */
	public static final int LEAF_TICKS = 12;
	/** Turned, how long at least it stays a bush, and how near a threat keeps it one longer. */
	public static final int BUSH_TICKS = 300;
	public static final double THREAT_RANGE = 5.0;
	/** Out of its bush again, how long before it can turn again. */
	public static final int TURN_COOLDOWN = 200;
	/** How long a scrap buys its trust. */
	public static final int TRUST_TICKS = 3600;
	public static final double FOLLOW_FAR = 5.0, FOLLOW_NEAR = 2.5;
	/** Crouched beside it this long, its friend gets its leaf; this near; how long they're unseen; and how often it'll do it. */
	public static final int TRICK_CROUCH = 40;
	public static final double TRICK_RANGE = 2.5;
	public static final int TRICK_TICKS = 200, TRICK_COOLDOWN = 2400;
	/** Drumming, the ticks between beats. */
	public static final int BEAT_TICKS = 8;

	/** Whether it's startled into a bush: something close enough that's running or hurt it, and it hasn't just turned back. */
	public static boolean startled(double distance, boolean threatening, long sinceTurnedBack) {
		return threatening && distance <= STARTLE_RANGE && (sinceTurnedBack < 0 || sinceTurnedBack >= TURN_COOLDOWN);
	}

	/** Whether it stays a bush: not long enough yet, or a threat still near. */
	public static boolean staysBush(long since, boolean threatNear) {
		return since < BUSH_TICKS || threatNear;
	}

	/** Whether it drums its belly: at night under a full moon, with nothing to be afraid of. */
	public static boolean drums(boolean night, int moonPhase, boolean hidden) {
		return night && moonPhase == 0 && !hidden;
	}

	/** The beat's pitch: a pom-pom, pom-pom, the second of each pair lower. */
	public static float beatPitch(int beat) {
		return beat % 2 == 0 ? 0.9F : 0.7F;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}

	/** Whether it puts its leaf on its friend's head: they've crouched beside it long enough, and it's been a while. */
	public static boolean tricks(boolean trusts, int crouched, double distance, long sinceLast) {
		return trusts && crouched >= TRICK_CROUCH && distance <= TRICK_RANGE && (sinceLast < 0 || sinceLast >= TRICK_COOLDOWN);
	}
}
