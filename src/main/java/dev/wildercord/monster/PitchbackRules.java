package dev.wildercord.monster;

/**
 * The Pitchback Beetle's rules, apart from the game so they can be tested: when it lobs its resin, what the resin does to
 * whatever it lands on, how it aims its arc, and when fire bursts its sac.
 */
public final class PitchbackRules {
	private PitchbackRules() {}

	/** It lobs its resin from between these distances, and likes to keep about this far off. */
	public static final double LOB_MIN = 3.5;
	public static final double LOB_MAX = 12.0;
	public static final double KEEP = 7.0;
	/** Rearing up with the sac swelling: the tell. */
	public static final int LOB_WINDUP = 16;
	public static final int LOB_COOLDOWN = 70;
	/** Up close, its mandibles. */
	public static final double BITE_REACH = 2.2;
	public static final int BITE_COOLDOWN = 25;

	/** One glob gums you up: Slowness II, for this long... */
	public static final int GUM_TICKS = 80;
	public static final int GUM_LEVEL = 1;
	/** ...and a second while you're still gummed sticks you fast. */
	public static final int STUCK_TICKS = 30;

	/** Fire bursts its sac: everything this close is caught in the flash... */
	public static final double BURST_RADIUS = 3.0;
	public static final float BURST_DAMAGE = 4.0F;
	public static final int BURST_FIRE_SECONDS = 4;
	/** ...and the sac takes this long to fill again, with no resin to lob meanwhile. */
	public static final int REFILL_TICKS = 240;
	/** Fire hurts it this much worse. */
	public static final float FIRE_MULTIPLIER = 2.0F;

	/** Whether it rears up to lob now: its sac full, its lob ready, its prey neither too near nor too far. */
	public static boolean lobs(double distance, long now, long ready, boolean sacFull) {
		return sacFull && now >= ready && distance >= LOB_MIN && distance <= LOB_MAX;
	}

	/** Whether a glob sticks its mark fast: it was gummed up already (slowed at least as much as a glob slows). */
	public static boolean sticks(int slownessAmplifier) {
		return slownessAmplifier >= GUM_LEVEL;
	}

	/** How much higher than its mark it aims, {@code horizontal} blocks off, for the glob to drop onto it. */
	public static double lift(double horizontal) {
		return horizontal * 0.14;
	}

	/** Whether its sac bursts: full, and fire has reached it. */
	public static boolean bursts(boolean sacFull, boolean fire) {
		return sacFull && fire;
	}

	/** Whether it backs off rather than coming on: its sac full and its prey too close to lob at. */
	public static boolean backsOff(double distance, boolean sacFull) {
		return sacFull && distance < LOB_MIN;
	}
}
