package dev.wildercord.wildlife;

/**
 * The glowberry dormouse's numbers and decisions, kept pure for its tests: when it sleeps, what sends it running, which
 * glow berries it can reach standing up, how many its cheeks hold and how brightly they glow, and how long a berry buys its
 * trust and how often it then brings you one.
 */
public final class DormouseRules {
	private DormouseRules() {}

	/** How near someone running has to come to send it off, and how near it lets something hostile get. */
	public static final double STARTLE_RANGE = 6.0, ENEMY_RANGE = 5.0;
	/** Woken (struck, fed), how long it stays up even by day. */
	public static final int WAKE_TICKS = 1200;
	/** How far above its feet (blocks) a berry can be and still be reached standing on its hind legs. */
	public static final int REACH = 2;
	/** How far it looks for berries, across and up. */
	public static final int SEARCH = 7, SEARCH_UP = 4;
	/** How many berries its cheeks hold. */
	public static final int POUCH = 3;
	/** How long it stands up at a vine before the berry comes away. */
	public static final int PLUCK_TICKS = 24;
	/** How long between berries it eats from its own cheeks, left to itself. */
	public static final int EAT_TICKS = 1800;
	/** How long a berry buys its trust, how near it then keeps, and how often it brings a berry from its cheeks. */
	public static final int TRUST_TICKS = 3600, GIFT_TICKS = 500;
	public static final double FOLLOW_FAR = 3.0, FOLLOW_NEAR = 1.5, GIFT_RANGE = 2.5;

	/** Whether it sleeps: by day, unless something woke it a little while ago. */
	public static boolean sleeps(boolean day, long sinceWoken) {
		return day && (sinceWoken < 0 || sinceWoken >= WAKE_TICKS);
	}

	/** Whether someone sends it running: running near, and not someone it trusts. Creeping up never does; nor anything, asleep. */
	public static boolean startles(boolean sprinting, double distance, boolean trusted, boolean asleep) {
		return !asleep && sprinting && !trusted && distance <= STARTLE_RANGE;
	}

	/** Whether a berry {@code above} blocks over its feet is in reach standing up. */
	public static boolean reaches(int above) {
		return above >= 0 && above <= REACH;
	}

	/** Whether it goes looking for berries: awake and with room in its cheeks. */
	public static boolean forages(boolean asleep, int pouch) {
		return !asleep && pouch < POUCH;
	}

	/** How brightly its cheeks glow with {@code pouch} berries in them (0 when empty). */
	public static float glow(int pouch) {
		int held = Math.max(0, Math.min(POUCH, pouch));
		return held == 0 ? 0 : 0.35F + 0.65F * held / POUCH;
	}

	/** Whether it trusts the one who fed it, {@code since} ticks after the feeding. */
	public static boolean trusts(long since) {
		return since >= 0 && since < TRUST_TICKS;
	}

	/** Whether it brings a berry from its cheeks now: one to give, near whoever it trusts, and long enough since the last. */
	public static boolean gives(int pouch, double distance, long sinceLast) {
		return pouch > 0 && distance <= GIFT_RANGE && (sinceLast < 0 || sinceLast >= GIFT_TICKS);
	}
}
