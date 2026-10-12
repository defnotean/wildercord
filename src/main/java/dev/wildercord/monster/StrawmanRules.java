package dev.wildercord.monster;

/**
 * The Strawman's rules, apart from the game so they can be tested: who counts as watching it, when it may strike, how
 * long a stare it bears before its crows burst out, and which way it stands when it's caught.
 */
public final class StrawmanRules {
	private StrawmanRules() {}

	/** How far off someone can be and still hold it still by looking at it. */
	public static final double WATCH_RANGE = 64.0;
	/**
	 * How square on someone has to be looking (the cosine of the angle off the middle of their view): about 60 degrees
	 * either side, a little inside the edge of a normal field of view, so it moves at the very edge of sight.
	 */
	public static final double LOOK_DOT = 0.5;
	/** How close it has to be to strike. */
	public static final double STRIKE_REACH = 2.2;
	public static final int STRIKE_COOLDOWN = 24;
	/** How long its strike shows (its arm swung). */
	public static final int STRIKE_TICKS = 6;
	public static final float STRIKE_DAMAGE = 7.0F;
	/** How fast it goes when nobody is looking (its walking speed multiplied). */
	public static final double CREEP_SPEED = 1.5;
	/** It hunts someone this close; further off, it waits. */
	public static final double HUNT_RANGE = 40.0;
	/** A stare from this close, held this long, and its crows burst out at whoever is staring. */
	public static final double STARE_RANGE = 16.0;
	public static final int STARE_TICKS = 100;
	/** How long the crows blind the starer, and how long before it can loose them again. */
	public static final int BLIND_TICKS = 40;
	public static final int CROW_COOLDOWN = 200;
	/** How long its crows show (its head thrown back, its arms flung wide). */
	public static final int CROW_TICKS = 12;
	/** How many ways it can be standing when someone looks back at it. */
	public static final int STANCES = 6;
	/** Fire takes it this much harder: it's straw. */
	public static final float FIRE_SCALE = 2.0F;

	/** The cosine of the angle between where someone is looking and the way to it: 1 straight at it. */
	public static double lookDot(double lookX, double lookY, double lookZ, double toX, double toY, double toZ) {
		double look = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
		double to = Math.sqrt(toX * toX + toY * toY + toZ * toZ);
		if (look < 1.0E-6 || to < 1.0E-6) {
			// Standing inside it: that's watching it.
			return 1.0;
		}
		return (lookX * toX + lookY * toY + lookZ * toZ) / (look * to);
	}

	/** Whether someone is watching it: looking its way, near enough, with nothing between, and not blinded. */
	public static boolean watches(double dot, double distance, boolean sight, boolean blind) {
		return !blind && sight && distance <= WATCH_RANGE && dot >= LOOK_DOT;
	}

	/** Whether it may strike: close enough, its arm ready, and nobody looking. */
	public static boolean strikes(double distance, boolean watched, long now, long ready) {
		return !watched && distance <= STRIKE_REACH && now >= ready;
	}

	/** Whether a stare has gone on long enough, close enough, for the crows to burst out. */
	public static boolean crows(int stared, double distance, long now, long ready) {
		return stared >= STARE_TICKS && distance <= STARE_RANGE && now >= ready;
	}

	/** The way it stands when caught next: any of its stances but the one it stood in last. */
	public static int nextStance(int last, int roll) {
		int pick = Math.floorMod(roll, STANCES - 1);
		return pick >= last ? pick + 1 : pick;
	}

	/** The damage it takes: fire hurts it double. */
	public static float damageScale(boolean fire) {
		return fire ? FIRE_SCALE : 1.0F;
	}
}
