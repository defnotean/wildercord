package dev.wildercord.wildlife;

/**
 * The Delver Mole (0.13), the burrowing mount, as plain numbers. Tamed and saddled like a horse, it digs its rider a
 * tunnel through soft ground (whatever a shovel takes, no harder than grass), dropping what it digs. Ridden straight it
 * tunnels level; look well down and it digs down, well up and it digs up.
 */
public final class DelverRules {
	private DelverRules() {}

	/** The hardest block it digs through (grass, gravel and clay are 0.6; stone is 1.5). */
	public static final float MAX_HARDNESS = 1.0F;
	/** How often it takes a bite out of the ground ahead (ticks), and how far ahead it reaches (blocks). */
	public static final int DIG_EVERY = 4;
	public static final double REACH = 0.7;
	/** How far the rider must look down or up (degrees) before it digs down or up instead of level. */
	public static final float TILT = 35F;
	/** How long after its last bite it still shows as digging (ticks). */
	public static final int DIG_SHOWN = 10;
	/** Where delvers live, and how many together. */
	public static final int WEIGHT = 3, MIN_GROUP = 1, MAX_GROUP = 2;

	/** Whether it digs through a block: one a shovel takes, no harder than {@link #MAX_HARDNESS}, and holding nothing. */
	public static boolean digs(boolean shovel, float hardness, boolean holdsSomething) {
		return shovel && !holdsSomething && hardness >= 0 && hardness <= MAX_HARDNESS;
	}

	/**
	 * Where it reaches to dig, from where it stands, for a rider looking at {@code yaw} and {@code pitch} (degrees,
	 * Minecraft's): ahead along the ground, tipped down or up past {@link #TILT}.
	 */
	public static double[] reach(float yaw, float pitch) {
		double y = Math.toRadians(yaw);
		double dx = -Math.sin(y), dz = Math.cos(y), dy = 0;
		if (pitch > TILT) {
			dy = -1;
		} else if (pitch < -TILT) {
			dy = 1;
		}
		double scale = REACH / Math.sqrt(dx * dx + dy * dy + dz * dz);
		return new double[] {dx * scale, dy * scale, dz * scale};
	}
}
