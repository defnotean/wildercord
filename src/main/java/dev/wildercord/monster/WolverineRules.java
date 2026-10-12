package dev.wildercord.monster;

/**
 * The Rimecoat Wolverine's rules, apart from the game so they can be tested: when it snatches food off its prey and how
 * much, when it stops running to eat what it stole, when a blow makes it drop it, and when it turns and fights.
 */
public final class WolverineRules {
	private WolverineRules() {}

	/** It snatches from this close, after a growl this long, and not again for a while after. */
	public static final double SNATCH_REACH = 2.4;
	public static final int SNATCH_WINDUP = 8;
	public static final int SNATCH_COOLDOWN = 60;
	/** The most it gets in its jaws at once. */
	public static final int SNATCH_MAX = 4;
	/** Food on the ground draws it from this far. */
	public static final double SCENT_RANGE = 10.0;
	/** Its bite, and how much quicker it bites once cornered. */
	public static final double BITE_REACH = 2.3;
	public static final int BITE_COOLDOWN = 22;
	public static final int FURY_BITE_COOLDOWN = 11;
	/** Carrying, it runs till it's this far off, or this long has passed, then stops to eat. */
	public static final double FLEE_DIST = 14.0;
	public static final int EAT_AFTER = 200;
	/** Chewing takes this long. */
	public static final int EAT_TICKS = 40;
	/** A blow while it carries makes it drop what it carries this often. */
	public static final float DROP_CHANCE = 0.35F;
	/** Cornered: this hurt, it stops running and fights. */
	public static final float FURY_FRACTION = 0.4F;

	/** Whether it growls and goes for its prey's food: close enough, ready, its jaws empty, not cornered, and food to take. */
	public static boolean snatches(double distance, long now, long ready, boolean carrying, boolean preyHasFood, boolean furious) {
		return !carrying && !furious && preyHasFood && now >= ready && distance <= SNATCH_REACH;
	}

	/** How much of a stack of {@code count} it gets in its jaws. */
	public static int take(int count) {
		return Math.max(0, Math.min(count, SNATCH_MAX));
	}

	/** Whether it's cornered: hurt enough that it stops running and fights, quicker and harder. */
	public static boolean furious(float health, float max) {
		return health <= max * FURY_FRACTION;
	}

	/** Whether it stops running and eats: far enough off, or carrying long enough. */
	public static boolean eats(double distance, int carried) {
		return distance >= FLEE_DIST || carried >= EAT_AFTER;
	}

	/** Whether a blow makes it drop what it carries ({@code roll} 0 to 1). */
	public static boolean dropsWhenHit(float roll) {
		return roll < DROP_CHANCE;
	}

	/** How long between bites. */
	public static int biteCooldown(boolean furious) {
		return furious ? FURY_BITE_COOLDOWN : BITE_COOLDOWN;
	}

	/** How much eating heals it: half a heart a point of nutrition, for each one it ate. */
	public static float heal(int nutrition, int count) {
		return Math.max(0, nutrition) * Math.max(0, count) * 0.5F;
	}
}
