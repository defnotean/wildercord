package dev.wildercord.monster;

/**
 * The Foxfire Kitsune's rules, apart from the game so they can be tested: when it throws its foxfire, when it bites, when
 * it splits into copies of itself, and how long the copies last.
 */
public final class KitsuneRules {
	private KitsuneRules() {}

	/** How far it throws its foxfire from, and how close it won't bother (it bites instead). */
	public static final double FOXFIRE_RANGE = 12.0;
	public static final double FOXFIRE_MIN = 3.0;
	/** Its tails fanning up before it throws: the tell. */
	public static final int FOXFIRE_WINDUP = 20;
	/** Between one foxfire and the next. */
	public static final int FOXFIRE_COOLDOWN = 80;
	/** What the foxfire does where it lands: magic damage, and alight this many seconds. */
	public static final float FOXFIRE_DAMAGE = 3.0F;
	public static final int FOXFIRE_BURN = 3;

	/** How close it has to be to bite, and between one bite and the next. */
	public static final double BITE_REACH = 1.8;
	public static final int BITE_COOLDOWN = 20;

	/** How many copies of itself each split makes, and how many times it splits at most. */
	public static final int DECOYS = 2;
	public static final int MAX_SPLITS = 2;
	/** How long a copy lasts before it fades away. */
	public static final int DECOY_TICKS = 400;
	/** How far off its maker a copy can stray before it fades. */
	public static final double DECOY_LEASH = 32.0;
	/** The spin as it splits. */
	public static final int SPLIT_TICKS = 12;

	/** Whether it throws its foxfire now: far enough off to bother, near enough to reach, and ready. */
	public static boolean throwsFoxfire(double distance, long now, long ready, boolean sees) {
		return sees && distance >= FOXFIRE_MIN && distance <= FOXFIRE_RANGE && now >= ready;
	}

	/** Whether it bites now. */
	public static boolean bites(double distance, long now, long ready) {
		return distance <= BITE_REACH && now >= ready;
	}

	/** How many splits its health has earned it: one at two thirds, another at a third. */
	public static int splitsEarned(float health, float maxHealth) {
		if (maxHealth <= 0) {
			return 0;
		}
		float share = health / maxHealth;
		return share <= 1 / 3.0F ? 2 : share <= 2 / 3.0F ? 1 : 0;
	}

	/** Whether it splits now: hurt past the next mark, and not split too often yet. */
	public static boolean splits(float health, float maxHealth, int splitsDone) {
		return splitsDone < MAX_SPLITS && splitsEarned(health, maxHealth) > splitsDone;
	}

	/** Whether a copy fades: its time is up, or its maker is gone or far off. */
	public static boolean decoyFades(int age, boolean makerAlive, double makerDistance) {
		return age >= DECOY_TICKS || !makerAlive || makerDistance > DECOY_LEASH;
	}
}
