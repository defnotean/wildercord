package dev.wildercord.monster;

/**
 * The Glowlure Angler's numbers and decisions, kept pure for its tests: who its lure draws in, when it snaps, how hard the
 * snap bites, how long it lies winded after a miss, and when it gives up a chase and settles back down to wait.
 */
public final class AnglerRules {
	private AnglerRules() {}

	/** Its lure draws in whoever looks at it from this close, a little each tick, unless they sneak. */
	public static final double DRAW_RANGE = 8;
	public static final double DRAW_STRENGTH = 0.035;
	/** How square on someone has to look at the lure to be drawn: the dot of their look and the way to it. */
	public static final double DRAW_LOOK = 0.96;
	/** It snaps at prey this close: a short tell, then a lunge. */
	public static final double SNAP_RANGE = 3.2;
	public static final int WINDUP_TICKS = 8;
	public static final int LUNGE_TICKS = 10;
	public static final double LUNGE_SPEED = 0.85;
	public static final float SNAP_DAMAGE = 9.0F;
	public static final int SNAP_SLOW_TICKS = 60;
	/** Missing, it lies winded, jaw hanging, and blows land half again as hard. */
	public static final int WINDED_TICKS = 50;
	public static final float WINDED_HURT = 1.5F;
	/** Between snaps, and its plain bite while chasing. */
	public static final int SNAP_COOLDOWN = 60;
	public static final int BITE_COOLDOWN = 25;
	public static final double BITE_REACH = 2.0;
	/** It chases this long at most before settling back to wait with its lure lit. */
	public static final int CHASE_TICKS = 160;

	/** Whether the lure draws someone this far away, looking at it this squarely. */
	public static boolean draws(double distance, double look, boolean sneaking) {
		return !sneaking && distance > SNAP_RANGE * 0.5 && distance <= DRAW_RANGE && look >= DRAW_LOOK;
	}

	/** How hard the lure pulls at this distance: strongest at the edge of its reach, easing as you near. */
	public static double pull(double distance) {
		if (distance > DRAW_RANGE) {
			return 0;
		}
		return DRAW_STRENGTH * (0.5 + 0.5 * distance / DRAW_RANGE);
	}

	/** Whether it snaps now. */
	public static boolean snaps(double distance, long now, long next) {
		return distance <= SNAP_RANGE && now >= next;
	}

	/** Whether it may bite as it chases. */
	public static boolean bites(double distance, long now, long next) {
		return distance <= BITE_REACH && now >= next;
	}

	/** Damage it takes: more when winded. */
	public static float hurt(float damage, boolean winded) {
		return winded ? damage * WINDED_HURT : damage;
	}

	/** Whether it gives up and settles back down to lure. */
	public static boolean settles(int chased, boolean hunting) {
		return !hunting || chased >= CHASE_TICKS;
	}
}
