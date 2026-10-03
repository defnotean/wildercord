package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraRules;

/** The draw is a deliberate six-second commitment; the blade favours answers over pursuit. */
public final class SleepingBladeRules {
	private SleepingBladeRules() {}
	public static final int DRAW_TICKS = 120;
	public static final double REACH = 3.5;
	public static final double RESONANCE = 120;
	public static boolean intent(int stage, boolean enabled, boolean emptyHand, boolean bonded) {
		return enabled && stage >= AuraRules.FORM && emptyHand && !bonded;
	}
	public static boolean holding(boolean alive, boolean kneeling, boolean grounded, boolean sameWorld,
			boolean hurt, double movedSqr, double distanceSqr) {
		return alive && kneeling && grounded && sameWorld && !hurt && movedSqr <= .09 && distanceSqr <= REACH * REACH;
	}
	public static double momentum(String source) {
		return source.equals("guard") ? 1.30 : source.equals("hit") ? .80 : 1;
	}
	public static double guardCost() { return .85; }
	public static boolean footing(int surface, int floor, int sea, int[] neighbours) {
		if (surface != floor || surface < sea || neighbours.length != 4) return false;
		for (int n : neighbours) if (Math.abs(n - surface) > 3) return false;
		return true;
	}
}
