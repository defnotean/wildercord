package dev.wildercord.monster;

/**
 * The Gulper Pitcher's rules, apart from the game so they can be tested: how close someone has to come before it stirs,
 * which of its two weapons it reaches for, and how hard its vine hauls whoever it catches.
 */
public final class GulperPitcherRules {
	private GulperPitcherRules() {}

	/** How close someone walking has to come before it stirs and opens its lid. */
	public static final double NOTICE_RANGE = 7.0;
	/** Crouching, much closer. */
	public static final double SNEAK_NOTICE_RANGE = 3.0;

	/** Its vine: the lid thrown open and the vine reared back first, then lashed out. */
	public static final double LASH_RANGE = 6.5;
	public static final int LASH_WINDUP = 16;
	public static final int LASH_TICKS = 8;
	public static final int LASH_COOLDOWN = 60;
	public static final float LASH_DAMAGE = 2.0F;
	/** Its pull: a little hauls you from close by, a lot from the end of the vine, never so much it flings you past. */
	public static final double PULL_BASE = 0.35;
	public static final double PULL_PER_BLOCK = 0.17;
	public static final double PULL_MAX = 1.4;
	/** Thrown a little up as well, so the haul carries over the ground. */
	public static final double PULL_LIFT = 0.3;

	/** Its bite: the lid slammed down on whoever is at its mouth. */
	public static final double BITE_REACH = 2.2;
	public static final int BITE_TICKS = 8;
	public static final int BITE_COOLDOWN = 30;
	public static final float BITE_DAMAGE = 5.0F;
	/** The acid of its pool: Poison, for this long. */
	public static final int ACID_TICKS = 80;
	public static final int ACID_LEVEL = 0;

	/** It is a plant: fire takes it this much harder. */
	public static final float FIRE_MULTIPLIER = 2.0F;
	/** With nothing near for this long it closes its lid again. */
	public static final int SETTLE_TICKS = 100;

	/** How close someone has to come before it stirs, by how they're moving. */
	public static double noticeRange(boolean sneaking) {
		return sneaking ? SNEAK_NOTICE_RANGE : NOTICE_RANGE;
	}

	/** Whether someone this close, moving like this, stirs it. */
	public static boolean notices(double distance, boolean sneaking) {
		return distance <= noticeRange(sneaking);
	}

	/** What it does next at this distance: 1 the bite, 2 the vine (reared first), 0 nothing yet. The vine needs a clear line. */
	public static int attack(double distance, boolean sees, long now, long biteReady, long lashReady) {
		if (distance <= BITE_REACH) {
			return now >= biteReady ? 1 : 0;
		}
		if (distance <= LASH_RANGE && sees && now >= lashReady) {
			return 2;
		}
		return 0;
	}

	/** How hard the vine hauls whoever it catches this far off, in blocks a tick. */
	public static double pull(double distance) {
		if (distance <= BITE_REACH) {
			return 0;
		}
		return Math.min(PULL_MAX, PULL_BASE + (distance - BITE_REACH) * PULL_PER_BLOCK);
	}

	/** Damage it takes, after its weakness to fire. */
	public static float damage(float damage, boolean fire) {
		return fire ? damage * FIRE_MULTIPLIER : damage;
	}
}
