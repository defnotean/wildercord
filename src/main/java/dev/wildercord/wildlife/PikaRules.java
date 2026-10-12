package dev.wildercord.wildlife;

/**
 * The tussock pika's numbers and decisions, kept pure for its tests: how often it goes haying and how far, how long it
 * nibbles a stalk through, when it basks on a sunny rock and for how long, and how long a handful of wheat buys its trust
 * (and with it, flowers brought to your feet).
 */
public final class PikaRules {
	private PikaRules() {}

	/** How far it looks for something to cut, and how long it nibbles before the stalk comes away. */
	public static final int HAY_RANGE = 8;
	public static final int NIBBLE_TICKS = 30;
	/** Between trips: alone it hays now and then; for a friend, eagerly. */
	public static final int HAY_COOLDOWN = 900;
	public static final int GIFT_COOLDOWN = 240;
	/** A trip given up on after this long, stuck or lost. */
	public static final int TRIP_TICKS = 400;
	/** How near home (or its friend) it has to be to set down what it carries. */
	public static final double DROP_RANGE = 1.8;
	/** Basking: at least this long, at most this long, and not again for a while after. */
	public static final int BASK_MIN = 200, BASK_MAX = 500;
	public static final int BASK_COOLDOWN = 1200;
	/** How long a handful of wheat buys its trust. */
	public static final int TRUST_TICKS = 4800;
	public static final double FOLLOW_FAR = 6.0, FOLLOW_NEAR = 3.0;

	/** What it carries in its mouth. */
	public static final int NOTHING = 0, GRASS = 1, FLOWER = 2;

	/** Whether it sets off haying: grown, idle, empty-mouthed, and its last trip long enough ago (sooner with a friend). */
	public static boolean hays(boolean grown, boolean idle, int carrying, long sinceLast, boolean friend) {
		return grown && idle && carrying == NOTHING && (sinceLast < 0 || sinceLast >= (friend ? GIFT_COOLDOWN : HAY_COOLDOWN));
	}

	/** Whether it may cut this plant: grass and ferns always; flowers only to bring to a friend. */
	public static boolean cuts(boolean grass, boolean flower, boolean friend) {
		return grass || (flower && friend);
	}

	/** What it comes away with from a plant. */
	public static int carries(boolean flower) {
		return flower ? FLOWER : GRASS;
	}

	/** Whether the stalk comes away: nibbled long enough. */
	public static boolean cut(int nibbled) {
		return nibbled >= NIBBLE_TICKS;
	}

	/** Whether it settles to bask: by day, idle, on bare rock in the open, and not just after the last time. */
	public static boolean basks(boolean day, boolean idle, boolean sunnyRock, long sinceLast) {
		return day && idle && sunnyRock && (sinceLast < 0 || sinceLast >= BASK_COOLDOWN);
	}

	/** How long it basks, from a 0 to 1 roll. */
	public static int baskLength(double roll) {
		double r = Math.max(0, Math.min(1, roll));
		return (int) Math.round(BASK_MIN + (BASK_MAX - BASK_MIN) * r);
	}

	/** Whether it keeps basking: still day, nothing disturbing it, and not yet had its fill. */
	public static boolean staysBasking(boolean day, boolean disturbed, long since, int length) {
		return day && !disturbed && since < length;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}
}
