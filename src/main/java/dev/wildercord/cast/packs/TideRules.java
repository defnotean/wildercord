package dev.wildercord.cast.packs;

/**
 * The plain numbers and rules behind the fishing, water and coast pack ({@link TideEffects}), kept free of
 * the game so they can be tested on their own.
 */
public final class TideRules {
	private TideRules() {}

	/** Angler's Lure never leaves less than this many ticks of waiting. */
	public static final int LURE_FLOOR = 20;
	/** Tackle Mend's durability at power 1, and the most one cast can mend. */
	public static final int MEND_BASE = 16, MEND_MAX = 64;
	/** Drown Ward refills air below this much (vanilla's full is 300), at most this many times. */
	public static final int WARD_AIR_LOW = 60, WARD_REFILLS = 3;
	/** The block and creature caps. */
	public static final int WRING_MAX = 24, SLUICE_MAX = 24, SOAK_MAX = 12, REED_MAX = 12, KELP_MAX = 8, CORAL_MAX = 8,
		NEST_MAX = 4, CAULDRON_MAX = 4, LILY_MAX = 10, SANDBAR_LENGTH = 12, AUGER_DEPTH = 3, WRECK_MAX = 12, NET_MAX = 16,
		BOTTLE_MAX = 4, CROP_MAX = 8;

	/** The wait left after Angler's Lure: half, but never under {@link #LURE_FLOOR}, and never longer than it was. */
	public static int lured(int ticks) {
		if (ticks <= LURE_FLOOR) return Math.max(0, ticks);
		return Math.max(LURE_FLOOR, ticks / 2);
	}

	/** How much Tackle Mend mends at this power. */
	public static int mend(double power) {
		return (int) Math.max(1, Math.min(MEND_MAX, Math.round(MEND_BASE * power)));
	}

	/** Whether Drown Ward steps in now. */
	public static boolean wardRefills(int air, int refillsLeft) {
		return refillsLeft > 0 && air < WARD_AIR_LOW;
	}

	/** Inkveil's seconds: longer in water. */
	public static int inkveilSeconds(boolean inWater) {
		return inWater ? 10 : 3;
	}

	/** Shellback's seconds: longer when wet. */
	public static int shellbackSeconds(boolean wet) {
		return wet ? 30 : 15;
	}

	/** Skater's Edge: Speed II (amplifier 1) on ice, Speed I elsewhere. */
	public static int skateAmplifier(boolean onIce) {
		return onIce ? 1 : 0;
	}

	/** Bait Blessing: Luck I, Luck II with any Amplify. */
	public static int luckAmplifier(int amplify) {
		return amplify > 0 ? 1 : 0;
	}

	/** The concrete a powder sets into, by block path ("red_concrete_powder" to "red_concrete"), or null. */
	public static String concreteFor(String powderPath) {
		return powderPath != null && powderPath.endsWith("_concrete_powder")
			? powderPath.substring(0, powderPath.length() - "_powder".length()) : null;
	}

	/** The living coral a dead one becomes ("dead_tube_coral_fan" to "tube_coral_fan"), or null. */
	public static String livingCoral(String deadPath) {
		return deadPath != null && deadPath.startsWith("dead_") && deadPath.contains("coral")
			? deadPath.substring("dead_".length()) : null;
	}

	/** A turtle egg's next hatch stage: one closer, never past vanilla's last (2). */
	public static int nextHatch(int hatch) {
		return Math.min(2, Math.max(0, hatch) + 1);
	}

	/** Whole minutes, rounded up, for a weather time in ticks. */
	public static int minutes(int ticks) {
		return Math.max(1, (Math.max(0, ticks) + 1199) / 1200);
	}

	/**
	 * What Storm Glass tells, from the weather's own counters: +minutes until rain ends, -minutes until it
	 * comes, or 0 when the weather can't be read (clear set for good).
	 */
	public static int weatherTurn(boolean raining, int rainTime, int clearTime) {
		if (raining) return rainTime > 0 ? minutes(rainTime) : 0;
		int wait = clearTime > 0 ? clearTime : rainTime;
		return wait > 0 ? -minutes(wait) : 0;
	}
}
