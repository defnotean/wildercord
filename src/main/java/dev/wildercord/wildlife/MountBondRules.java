package dev.wildercord.wildlife;

/**
 * Mount bonds (0.13), the pure part: a tame mount ridden by its owner grows a bond with them, one point for each block
 * they travel together. Each of its five levels makes it a little quicker and hardier; at the third it learns to dash
 * when its rider sprints, and at the fifth it lands from a jump with a strike that throws back what's around it.
 */
public final class MountBondRules {
	private MountBondRules() {}

	/** The highest bond level. */
	public static final int MAX_LEVEL = 5;
	/** Bond points needed for each level, from level 1 up. */
	private static final int[] NEEDED = {200, 600, 1400, 3000, 6000};
	/** The most points one second's riding earns, so a teleport or a fall doesn't count as miles. */
	public static final double MAX_PER_SECOND = 24;
	/** Movement speed per level, as a fraction of its own. */
	public static final double SPEED_PER_LEVEL = 0.03;
	/** Max health per level. */
	public static final double HEALTH_PER_LEVEL = 2;
	/** The level it learns to dash at. */
	public static final int DASH_LEVEL = 3;
	/** How hard a dash throws it forward, in blocks a tick. */
	public static final double DASH_SPEED = 1.1;
	/** How long between dashes, in ticks. */
	public static final int DASH_REST = 160;
	/** The level it learns the jump-strike at. */
	public static final int STRIKE_LEVEL = 5;
	/** How far it must fall for its landing to strike. */
	public static final double STRIKE_FALL = 2.0;
	/** How far a strike reaches from where it lands. */
	public static final double STRIKE_RADIUS = 3.5;
	/** A strike's damage. */
	public static final float STRIKE_DAMAGE = 6.0F;
	/** How hard a strike throws things back. */
	public static final double STRIKE_KNOCKBACK = 1.2;

	/** The bond level that many points make. */
	public static int level(int points) {
		int level = 0;
		while (level < MAX_LEVEL && points >= NEEDED[level]) {
			level++;
		}
		return level;
	}

	/** The points the next level needs, or -1 at the top. */
	public static int next(int level) {
		return level >= MAX_LEVEL ? -1 : NEEDED[Math.max(0, level)];
	}

	/** The points a second's riding earns: the blocks covered, no more than {@link #MAX_PER_SECOND}. */
	public static int earned(double blocks) {
		if (!(blocks > 0.5)) {
			return 0;
		}
		return (int) Math.round(Math.min(MAX_PER_SECOND, blocks));
	}

	/** The speed a bond adds, as a fraction of its own. */
	public static double speed(int level) {
		return clamp(level) * SPEED_PER_LEVEL;
	}

	/** The max health a bond adds. */
	public static double health(int level) {
		return clamp(level) * HEALTH_PER_LEVEL;
	}

	public static boolean dashes(int level) {
		return level >= DASH_LEVEL;
	}

	public static boolean strikes(int level) {
		return level >= STRIKE_LEVEL;
	}

	/** Whether a dash is ready: it knows how, and has rested since the last. */
	public static boolean dashReady(int level, long now, long lastDash) {
		return dashes(level) && now - lastDash >= DASH_REST;
	}

	/** Which way a dash throws it: {@link #DASH_SPEED} along its heading, level. */
	public static double[] dash(float yaw) {
		double r = Math.toRadians(yaw);
		return new double[] {-Math.sin(r) * DASH_SPEED, 0, Math.cos(r) * DASH_SPEED};
	}

	/** Whether landing from that fall strikes. */
	public static boolean strikeOnLanding(int level, double fell) {
		return strikes(level) && fell >= STRIKE_FALL;
	}

	private static int clamp(int level) {
		return Math.max(0, Math.min(MAX_LEVEL, level));
	}
}
