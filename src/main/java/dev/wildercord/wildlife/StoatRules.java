package dev.wildercord.wildlife;

/**
 * The snowcap stoat's numbers and decisions, kept pure for its tests: when it stands up to look about, when it hunts a rabbit and
 * how long it dances at it first, when it dives into the snow and where in the dive it comes up, and how long an egg buys its
 * trust.
 */
public final class StoatRules {
	private StoatRules() {}

	/** What it is doing, synced so the model can pose it. */
	public static final int IDLE = 0, STAND = 1, DANCE = 2, DIVE = 3;

	/** How long it stands up on its hind legs to look about, and how near an unfamiliar player has to come to make it. */
	public static final int STAND_TICKS = 50;
	public static final double STAND_RANGE = 8.0;
	/** About one tick in this many, idle, it stands up anyway; one in this many it breaks into a dance for no reason at all. */
	public static final int STAND_ODDS = 400, PLAY_ODDS = 1400;
	/** How long its war dance runs (twisting hops), at a rabbit or in play. */
	public static final int DANCE_TICKS = 60;
	/** How far off it notices a rabbit, how near it creeps before it dances, how long between hunts, and how hard it bites. */
	public static final double HUNT_RANGE = 12.0, DANCE_RANGE = 4.0;
	public static final int HUNT_REST = 600;
	public static final float BITE = 4.0F;
	/** How long a dive into the snow lasts, the tick in it where it moves under the snow, and how far it can come up from where it went in. */
	public static final int DIVE_TICKS = 44, DIVE_SHIFT = 22;
	public static final int DIVE_REACH = 4;
	/** About one idle tick in this many, on snow, it dives in for the fun of it. */
	public static final int DIVE_ODDS = 500;
	/** How long an egg buys its trust, and how near it then keeps to whoever fed it. */
	public static final int TRUST_TICKS = 2400;
	public static final double FOLLOW_FAR = 5.0, FOLLOW_NEAR = 2.0;

	/** Whether someone it doesn't trust, this far off, makes it stand up to look at them. */
	public static boolean perks(double distance, boolean trusted) {
		return !trusted && distance <= STAND_RANGE;
	}

	/** Whether it goes after a rabbit: grown, the rabbit near enough, and rested since its last hunt. */
	public static boolean hunts(boolean baby, double distance, long sinceLastHunt) {
		return !baby && distance <= HUNT_RANGE && sinceLastHunt >= HUNT_REST;
	}

	/** Whether it can dive in where it stands: grown, and on snow. */
	public static boolean dives(boolean baby, boolean onSnow) {
		return !baby && onSnow;
	}

	/** Diving with {@code left} ticks to go: whether it is under the snow (and so can't be seen or hit). */
	public static boolean buried(int left) {
		return left > 8 && left < DIVE_TICKS - 8;
	}

	/** Its coat: white where it is cold enough to snow, the brown summer coat elsewhere. */
	public static boolean winterCoat(boolean coldEnoughToSnow) {
		return coldEnoughToSnow;
	}

	/** How high its dance hops it at a tick into the dance (each hop a quick arc), in blocks. */
	public static float hop(float age) {
		float t = age % 10;
		return t < 6 ? 0.45F * (float) Math.sin(Math.PI * t / 6.0) : 0;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}
}
