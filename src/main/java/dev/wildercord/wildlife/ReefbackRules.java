package dev.wildercord.wildlife;

/**
 * The Reefback Turtle (0.13), the water mount, as plain numbers. Tamed and saddled like a horse, it plods on land but in
 * water flies the way its rider looks, up or down, and keeps its rider breathing.
 */
public final class ReefbackRules {
	private ReefbackRules() {}

	/** Its speed swimming (blocks a tick, about 12 a second), and what share of it strafing and backing off get. */
	public static final double SWIM_SPEED = 0.6, STRAFE = 0.5, BACK = 0.3;
	/** How steeply it dives or climbs (degrees), and how quickly it eases to the course asked of it (per tick). */
	public static final double MAX_PITCH = 75.0, EASE = 0.1;
	/** How long each refresh of the rider's Conduit Power lasts (ticks), refreshed every {@link #BREATH_EVERY}. */
	public static final int BREATH_TICKS = 100, BREATH_EVERY = 40;
	/** What one bite of kelp or seagrass heals, and how much it calms a wild one. */
	public static final float BITE_HEAL = 2F;
	public static final int BITE_TEMPER = 3;
	/** Where reefbacks come ashore, and how many together. */
	public static final int WEIGHT = 3, MIN_GROUP = 1, MAX_GROUP = 2;

	/**
	 * The velocity a rider asks for in water, looking at {@code yaw} and {@code pitch} (degrees, Minecraft's) with
	 * {@code forward} and {@code strafe} held (positive forward and left).
	 */
	public static double[] swim(float yaw, float pitch, float forward, float strafe) {
		double y = Math.toRadians(yaw);
		double p = Math.toRadians(Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch)));
		double vx = 0, vy = 0, vz = 0;
		if (forward > 0) {
			vx = -Math.sin(y) * Math.cos(p) * SWIM_SPEED;
			vy = -Math.sin(p) * SWIM_SPEED;
			vz = Math.cos(y) * Math.cos(p) * SWIM_SPEED;
		} else if (forward < 0) {
			vx = Math.sin(y) * SWIM_SPEED * BACK;
			vz = -Math.cos(y) * SWIM_SPEED * BACK;
		}
		if (strafe != 0) {
			double side = Math.signum(strafe) * SWIM_SPEED * STRAFE;
			vx += Math.cos(y) * side;
			vz += Math.sin(y) * side;
		}
		return new double[] {vx, vy, vz};
	}

	/** One tick's easing from {@code current} toward {@code target}. */
	public static double ease(double current, double target) {
		return current + (target - current) * EASE;
	}
}
