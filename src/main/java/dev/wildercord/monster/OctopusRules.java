package dev.wildercord.monster;

/**
 * The Reefgrasp Octopus's rules, apart from the game so they can be tested: when it reaches out of its hiding place to
 * grab a swimmer, how long it holds on and what makes it let go, how fast it draws the air out of its prey, and when a blow
 * makes it ink and jet away.
 */
public final class OctopusRules {
	private OctopusRules() {}

	/** It reaches for swimmers this close, its arms rising this long first, and not again for a while after. */
	public static final double GRAB_REACH = 4.5;
	public static final int GRAB_WINDUP = 8;
	public static final int GRAB_COOLDOWN = 80;
	/** It holds on this long, squeezing for this much every so often. */
	public static final int HOLD_TICKS = 80;
	public static final int SQUEEZE_EVERY = 20;
	public static final float SQUEEZE_DAMAGE = 2.0F;
	/** This much harm while it holds on makes it let go. */
	public static final float LETGO_DAMAGE = 6.0F;
	/** Held under, its prey loses this much more air a tick. */
	public static final int AIR_DRAIN = 3;
	/** The lowest it drives its prey's air to: one short of where drowning bites, so the game's own count does the rest. */
	public static final int AIR_FLOOR = -19;
	/** Roused, it bites at this reach, this often. */
	public static final double BITE_REACH = 2.0;
	public static final int BITE_COOLDOWN = 25;
	/** A blow makes it ink, no more often than this; the ink blinds those this close this long, and it jets off this long. */
	public static final int INK_COOLDOWN = 160;
	public static final double INK_RADIUS = 4.0;
	public static final int INK_BLIND = 80;
	public static final int JET_TICKS = 30;
	/** Out of the water it dries out a little every this often. */
	public static final int DRY_EVERY = 20;

	/** Hiding, its prey a swimmer this close, ready, its arms free: it reaches. */
	public static boolean grabs(double distance, long now, long ready, boolean holding, boolean preyInWater) {
		return !holding && preyInWater && now >= ready && distance <= GRAB_REACH;
	}

	/** Holding on: it lets go once its time's up, once it's been hurt enough, or once its prey is out of the water. */
	public static boolean letsGo(int held, float harm, boolean preyInWater) {
		return held >= HOLD_TICKS || harm >= LETGO_DAMAGE || !preyInWater;
	}

	/** Whether it squeezes this tick of holding on. */
	public static boolean squeezes(int held) {
		return held > 0 && held % SQUEEZE_EVERY == 0;
	}

	/** Its prey's air after it's held under a tick longer. */
	public static int drains(int air) {
		return air <= AIR_FLOOR ? air : Math.max(AIR_FLOOR, air - AIR_DRAIN);
	}

	/** Hurt and ready: it inks. */
	public static boolean inks(long now, long ready) {
		return now >= ready;
	}
}
