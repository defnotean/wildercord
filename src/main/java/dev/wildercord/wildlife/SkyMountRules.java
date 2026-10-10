package dev.wildercord.wildlife;

/**
 * The flying mount (0.13), as plain numbers. Beating the Tenth Circle's tribulation (the first with a Shadow) earns a
 * Skyray Bridle, which calls down a {@link BondedSkyray}: a skyray of your own that carries you through the air. Hold
 * forward and it flies the way you look; strafe to slip sideways, back to brake and drift backwards, nothing to hover.
 */
public final class SkyMountRules {
	private SkyMountRules() {}

	/** The circle whose tribulation earns the bridle. */
	public static final int REWARD_CIRCLE = 10;
	/** Top speed (blocks a tick, about 17 a second), and what share of it strafing and backing off get. */
	public static final double SPEED = 0.85, STRAFE = 0.5, BACK = 0.25;
	/** How steeply it will climb or dive (degrees), and how quickly it eases to the course asked of it (per tick). */
	public static final double MAX_PITCH = 60.0, EASE = 0.12;
	/** How long it hovers once its rider is off before it leaves (ticks: ten seconds). */
	public static final int LINGER_TICKS = 200;
	/** How long the bridle rests after a call (ticks: five seconds), and after the skyray is brought down (five minutes). */
	public static final int CALL_COOLDOWN = 100, FALLEN_COOLDOWN = 6000;
	/** How long a rider who steps off falls slowly (ticks). */
	public static final int SLOW_FALL_TICKS = 160;

	/** Whether beating the tribulation of circle {@code circle} earns the bridle. */
	public static boolean rewards(int circle) {
		return circle == REWARD_CIRCLE;
	}

	/**
	 * The velocity a rider asks for, looking at {@code yaw} and {@code pitch} (degrees, Minecraft's: yaw 0 is south, pitch
	 * up is negative) with {@code forward} and {@code strafe} held (positive forward and left).
	 */
	public static double[] steer(float yaw, float pitch, float forward, float strafe) {
		double y = Math.toRadians(yaw);
		double p = Math.toRadians(Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch)));
		double vx = 0, vy = 0, vz = 0;
		if (forward > 0) {
			vx = -Math.sin(y) * Math.cos(p) * SPEED;
			vy = -Math.sin(p) * SPEED;
			vz = Math.cos(y) * Math.cos(p) * SPEED;
		} else if (forward < 0) {
			vx = Math.sin(y) * SPEED * BACK;
			vz = -Math.cos(y) * SPEED * BACK;
		}
		if (strafe != 0) {
			double side = Math.signum(strafe) * SPEED * STRAFE;
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
