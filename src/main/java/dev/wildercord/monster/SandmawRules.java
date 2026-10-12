package dev.wildercord.monster;

/**
 * The Sandmaw Wyrm's numbers and decisions, kept pure for its tests: when it rises under its prey, which move it makes
 * once up, when it dives again, and how much a blow hurts it.
 */
public final class SandmawRules {
	private SandmawRules() {}

	/** Its step among the elites. */
	public static final EliteTier TIER = EliteTier.BRUTE;

	/** Under the sand it feels footsteps this far off, or only this near from someone sneaking. */
	public static final double FEEL_RANGE = 16, FEEL_SNEAKING = 5;
	/** Under the sand it rises once it is this near (across the ground) beneath its prey, after at least this long down. */
	public static final double UNDER = 1.5;
	public static final int MIN_BURROW = 50;
	/** It gives up waiting beneath and rises where it is after this long down. */
	public static final int MAX_BURROW = 200;
	/** The ground rumbles this long (the tell), then it bursts up, striking and flinging all this near. */
	public static final int RUMBLE_TICKS = 20;
	public static final double ERUPT_RADIUS = 2.2;
	public static final double ERUPT_DAMAGE = 8;
	public static final double ERUPT_LAUNCH = 0.9;

	/** Up, it rears back (the tell) and slams down along a line this long and wide. */
	public static final double SLAM_RANGE = 6;
	public static final int SLAM_WINDUP = 16;
	public static final double SLAM_LENGTH = 6.5, SLAM_WIDTH = 1.3;
	public static final double SLAM_DAMAGE = 10;
	/** Slammed, its head sticks in the sand this long, and blows land half again as hard. */
	public static final int BURIED_TICKS = 30;
	public static final float BURIED_HURT = 1.5F;
	/** Or, at range, it rears and spits a gout of sand down a cone, blinding and slowing. */
	public static final double SPIT_RANGE = 12;
	public static final int SPIT_WINDUP = 12;
	public static final double SPIT_CONE = 0.8;
	public static final double SPIT_DAMAGE = 3;
	/** Between moves up top. */
	public static final int MOVE_COOLDOWN = 25;
	/** It dives again after this long up, or once it has taken this share of its health since it rose. */
	public static final int MAX_SURFACED = 160;
	public static final double DIVE_HURT = 0.25;

	public enum Move { NONE, SLAM, SPIT, DIVE }

	/** Whether, down below, it feels someone walking this far off. */
	public static boolean feels(double distance, boolean sneaking) {
		return distance <= (sneaking ? FEEL_SNEAKING : FEEL_RANGE);
	}

	/** Whether, down below, it is close enough under its prey to rise. */
	public static boolean under(double across) {
		return across <= UNDER;
	}

	/** Whether it rises now: beneath its prey after a little while down, or tired of waiting. */
	public static boolean rises(double across, int down) {
		return down >= MAX_BURROW || down >= MIN_BURROW && under(across);
	}

	/** Which move it makes up top, its prey this far away, if it's ready. */
	public static Move choose(double distance, long now, long next) {
		if (now < next) {
			return Move.NONE;
		}
		if (distance <= SLAM_RANGE) {
			return Move.SLAM;
		}
		return distance <= SPIT_RANGE ? Move.SPIT : Move.DIVE;
	}

	/** Whether the slam strikes something this far along its line and this far to the side of it. */
	public static boolean inLine(double along, double across) {
		return along >= -0.5 && along <= SLAM_LENGTH && Math.abs(across) <= SLAM_WIDTH;
	}

	/** Whether the spit strikes something this far away, at this dot of its look and the way to them. */
	public static boolean inSpit(double distance, double dot) {
		return distance <= SPIT_RANGE && dot >= SPIT_CONE;
	}

	/** Whether it dives again: up long enough, or hurt enough since it rose. */
	public static boolean dives(int up, double hurt, double max) {
		return up >= MAX_SURFACED || hurt >= max * DIVE_HURT;
	}

	/** Damage it takes: none under the sand, more with its head stuck in it. */
	public static float hurt(float damage, boolean burrowed, boolean buried) {
		if (burrowed) {
			return 0;
		}
		return buried ? damage * BURIED_HURT : damage;
	}
}
