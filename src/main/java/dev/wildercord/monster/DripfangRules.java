package dev.wildercord.monster;

/**
 * The Dripfang Lurker's numbers and decisions, kept pure for its tests: what ceiling it will hang from, who walking below
 * makes it drop, how hard its tail-spike lands, how long it lies flipped when knocked down, and when it climbs back up.
 */
public final class DripfangRules {
	private DripfangRules() {}

	/** How far up it looks for a ceiling, and how high one has to be to be worth hanging from. */
	public static final int CEILING_REACH = 12;
	public static final int MIN_HANG = 3;
	/** It drops on prey this close sideways under it, and no further below than this. */
	public static final double DROP_REACH = 1.6;
	public static final double DROP_DEPTH = 14;
	/** Within this, its legs twitch now and then and it clicks: the only tell, for those who look up. */
	public static final double TWITCH_RANGE = 8;
	/** Its tail-spike landing: damage near where it falls, and the slow it leaves. */
	public static final float DROP_DAMAGE = 8.0F;
	public static final double DROP_RADIUS = 1.4;
	public static final int SLOW_TICKS = 60;
	/** Brought down (struck or shot while hanging) it lands on its back, and blows land half again as hard. */
	public static final int FLIPPED_TICKS = 60;
	public static final float FLIPPED_HURT = 1.5F;
	/** Idle this long on the ground with nothing to hunt, it climbs back up; how fast it climbs. */
	public static final int RECLIMB_TICKS = 200;
	public static final double CLIMB_SPEED = 0.16;
	/** Its bite on the ground. */
	public static final int BITE_COOLDOWN = 20;
	public static final double BITE_REACH = 1.9;

	/** Whether a ceiling this many blocks above the floor is one it will hang from. */
	public static boolean hangs(int height) {
		return height >= MIN_HANG && height <= CEILING_REACH;
	}

	/** Whether prey this far to the side and this far below sets it dropping. */
	public static boolean drops(double sideways, double below) {
		return sideways <= DROP_REACH && below > 0.5 && below <= DROP_DEPTH;
	}

	/** Whether prey this far away is near enough for it to twitch. */
	public static boolean twitches(double distance) {
		return distance <= TWITCH_RANGE;
	}

	/** Its landing's damage at this distance from where it struck: full at the heart, half at the edge, none past it. */
	public static float landing(double distance) {
		if (distance > DROP_RADIUS) {
			return 0;
		}
		return (float) (DROP_DAMAGE * (1 - 0.5 * distance / DROP_RADIUS));
	}

	/** Damage it takes: more when flipped on its back. */
	public static float hurt(float damage, boolean flipped) {
		return flipped ? damage * FLIPPED_HURT : damage;
	}

	/** Whether it heads back up: idle long enough, with a ceiling to go to. */
	public static boolean reclimbs(int idle, boolean hunting, boolean ceiling) {
		return !hunting && ceiling && idle >= RECLIMB_TICKS;
	}

	/** Whether it may bite. */
	public static boolean bites(double distance, long now, long next) {
		return distance <= BITE_REACH && now >= next;
	}
}
