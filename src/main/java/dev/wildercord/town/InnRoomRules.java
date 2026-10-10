package dev.wildercord.town;

/**
 * Inn rooms (0.13), as plain numbers. The Inn Cook lets rooms: a Room Key, used at an inn, rents a room there for a few days.
 * While it's yours you wake there, and any inn's keepers open your room's chest for you. The better they know you, the less a
 * key costs. The chest keeps your things between stays.
 */
public final class InnRoomRules {
	private InnRoomRules() {}

	/** How long one key rents a room for (Minecraft days). */
	public static final int RENT_DAYS = 3;
	/** The room chest's rows of nine. */
	public static final int CHEST_ROWS = 3;
	/** How near a keeper a key must be used (blocks). */
	public static final double NEAR_KEEPER = 16;

	/** What a key costs a traveller of {@code tier}, in emeralds: 6 for a stranger, down to 3 for the honoured. */
	public static int price(BountyRules.Tier tier) {
		return 6 - tier.ordinal();
	}

	/** Whether a room let until {@code until} is still yours at {@code now} (game ticks). */
	public static boolean rented(long until, long now) {
		return until > now;
	}

	/** When a room let until {@code until} is let until after one more key at {@code now}: a key adds to a stay still running. */
	public static long extend(long until, long now) {
		return Math.max(until, now) + (long) RENT_DAYS * BountyRules.DAY;
	}

	/** Whole days left, rounded up, on a room let until {@code until}. */
	public static int daysLeft(long until, long now) {
		return until <= now ? 0 : (int) ((until - now + BountyRules.DAY - 1) / BountyRules.DAY);
	}
}
