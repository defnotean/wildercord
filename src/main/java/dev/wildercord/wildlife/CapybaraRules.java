package dev.wildercord.wildlife;

/**
 * The mirebath capybara's numbers and decisions, kept pure for its tests: when it settles into a bath and how long it soaks,
 * when it goes looking for water again, who soaks with it, and when its young climb up on its back.
 */
public final class CapybaraRules {
	private CapybaraRules() {}

	/** In the water and still this long, it settles into its bath. */
	public static final int SETTLE_TICKS = 20;
	/** How long it soaks, at the least and at the most. */
	public static final int BATH_MIN = 1200, BATH_MAX = 2400;
	/** Out of the bath, how long before it goes looking for water again, and how far it looks. */
	public static final int BATH_COOLDOWN = 1200;
	public static final int WATER_SEARCH = 10;
	/** How near a bather soaks with it, how often its calm reaches them, and for how long. */
	public static final double SOAK_RANGE = 3.5;
	public static final int SOAK_EVERY = 20, SOAK_TICKS = 100;
	/** How near a youngster climbs onto a grown one, and how long it rides. */
	public static final double CLIMB_RANGE = 2.5;
	public static final int RIDE_TICKS = 600;

	/** Whether it settles into its bath: in the water, held still long enough, and not carrying on with a bath just ended. */
	public static boolean settles(boolean inWater, int stillTicks, long sinceBath) {
		return inWater && stillTicks >= SETTLE_TICKS && (sinceBath < 0 || sinceBath >= BATH_COOLDOWN / 4);
	}

	/** Whether it stays in its bath: still in the water, and it hasn't had its fill ({@code length} is this bath's own). */
	public static boolean staysBathing(boolean inWater, long since, int length) {
		return inWater && since < length;
	}

	/** This bath's length, from a roll of 0 to 1. */
	public static int bathLength(double roll) {
		return BATH_MIN + (int) Math.round((BATH_MAX - BATH_MIN) * Math.max(0, Math.min(1, roll)));
	}

	/** Whether it wants a bath: it's been dry a while. */
	public static boolean wantsBath(long sinceBath) {
		return sinceBath < 0 || sinceBath >= BATH_COOLDOWN;
	}

	/** Whether someone soaks with it: it's bathing, they're in the water too, and close. */
	public static boolean soaks(boolean bathing, boolean theyreInWater, double distance) {
		return bathing && theyreInWater && distance <= SOAK_RANGE;
	}

	/** Whether a youngster climbs up: it's young, on its own feet, near a grown one with its back free. */
	public static boolean climbs(boolean young, boolean riding, boolean backFree, double distance) {
		return young && !riding && backFree && distance <= CLIMB_RANGE;
	}

	/** Whether a youngster riding hops down: it's grown, it's ridden long enough, or its carrier was hurt. */
	public static boolean hopsDown(boolean young, long ridden, boolean carrierHurt) {
		return !young || ridden >= RIDE_TICKS || carrierHurt;
	}
}
