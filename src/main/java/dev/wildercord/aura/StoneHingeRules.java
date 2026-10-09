package dev.wildercord.aura;

/** Stone Hinge timing and the reviewed velocity turn, shared by the server, readout and headless tests. */
public final class StoneHingeRules {
	private StoneHingeRules() {}
	public static final int COST = 20, BRACE_TICKS = 6, CATCH_TICKS = 12, RECOVERY_TICKS = 10;
	/** Lane checked for wards, border and hazards before payment and again at the catch. Collision clipping is allowed. */
	public static final double LANE = 1.5, MIN_IMPULSE = 1.0E-8;
	/** Lane support is probed at this spacing so a gap narrower than the body cannot hide between samples. */
	public static final double SUPPORT_STEP = .25;
	/** Frontal means within 60 degrees of the planted facing, and only a shove within 60 degrees of straight back turns. */
	public static final double CONE_COS = .5;
	/** The planted body keeps 15% walking speed and must stay within a block of where it paid. */
	public static final double SLOW = .85, PLANT = 1;
	/** Phases continue Wall Turn's event numbering so one event channel carries both forms. */
	public static final int BRACE = 6, CATCH = 7, TURN = 8, SPENT = 9;
	public static final int EQUIP = 5;

	public static boolean eligible(int stage, boolean stoneClear) { return stage >= 5 && stoneClear; }
	public static boolean canPay(double aura, long now, long readyAt, boolean used) {
		return Double.isFinite(aura) && aura >= COST && now >= readyAt && !used;
	}
	/** Exactly one held strafe picks the side: +1 left, -1 right, 0 for none or both. */
	public static int side(boolean left, boolean right) { return left == right ? 0 : left ? 1 : -1; }
	/** The lateral unit vector for a strafe side at a body yaw in degrees (Minecraft's left strafe is +side). */
	public static double[] lateral(float yaw, int side) {
		double r = Math.toRadians(yaw);
		return new double[] {side * Math.cos(r), side * Math.sin(r)};
	}
	/** The planted facing at a body yaw in degrees (Minecraft's forward). */
	public static double[] forward(float yaw) {
		double r = Math.toRadians(yaw);
		return new double[] {-Math.sin(r), Math.cos(r)};
	}
	/** Whether a horizontal offset to the attacker lies within the cone in front of the plant. The camera plays no part. */
	public static boolean frontal(float yaw, double dx, double dz) {
		double length = Math.hypot(dx, dz);
		if (!Float.isFinite(yaw) || !Double.isFinite(length) || length <= 1.0E-2) return false;
		double[] f = forward(yaw);
		return (dx * f[0] + dz * f[1]) / length >= CONE_COS - 1.0E-9;
	}
	/**
	 * Native 26.3 knockback writes after = before / 2 + impulse. Keep the retained half and send the native impulse, at its own
	 * strength, straight to the held side of the plant, with no forward or backward part. Returns null for a non-finite or
	 * negligible impulse, or one that does not push within the cone behind the plant (an oblique or side shove stands).
	 */
	public static double[] turn(double beforeX, double beforeZ, double afterX, double afterZ, float yaw, int side) {
		double retainedX = beforeX * .5, retainedZ = beforeZ * .5;
		double nativeX = afterX - retainedX, nativeZ = afterZ - retainedZ, strength = Math.hypot(nativeX, nativeZ);
		if (!Double.isFinite(nativeX) || !Double.isFinite(nativeZ) || !Double.isFinite(retainedX) || !Double.isFinite(retainedZ)
			|| !Double.isFinite(strength) || strength <= MIN_IMPULSE || !Float.isFinite(yaw) || side != 1 && side != -1) return null;
		double[] f = forward(yaw);
		if (-(nativeX * f[0] + nativeZ * f[1]) / strength < CONE_COS - 1.0E-9) return null;
		double[] lateral = lateral(yaw, side);
		return new double[] {retainedX + strength * lateral[0], retainedZ + strength * lateral[1]};
	}
}
