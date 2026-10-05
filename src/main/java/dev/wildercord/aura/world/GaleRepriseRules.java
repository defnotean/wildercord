package dev.wildercord.aura.world;

/** Gale's committed lateral footwork, followed by one independently warned, punishable reply. */
public final class GaleRepriseRules {
	private GaleRepriseRules() {}

	public static final int GATHER = 8, STEP_TICKS = 4, REPLY_TELL = 10;
	public static final int TELL = GATHER + STEP_TICKS + REPLY_TELL, RECOVERY = 32, COOLDOWN = 140, WARNING_REFRESH = 3;
	public static final double COST = 24, DAMAGE = 26, MIN_DISTANCE = 2.5, MAX_DISTANCE = 4.75;
	/** The existing shared exhaustion gate is checked before any special form is selected. */
	public static final double READY_AURA = Math.max(COST, MastersRules.ATTACK_COST + MastersRules.GUARD_COST);
	public static final double STEP_DISTANCE = 1.8, REACH = 5.25, HALF_WIDTH = .75, HEIGHT = 1.8;

	public enum Beat { GATHER, STEP, REPLY_WARNING, REPLY, EXPIRED }

	public static Beat beat(long age) {
		if (age < 0 || age > TELL) return Beat.EXPIRED;
		if (age < GATHER) return Beat.GATHER;
		if (age < GATHER + STEP_TICKS) return Beat.STEP;
		return age < TELL ? Beat.REPLY_WARNING : Beat.REPLY;
	}

	/** Once per four completed attacks; phase and party size never accelerate the cadence. */
	public static boolean eligible(int discipline, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return discipline == MastersRules.GALE && sequence >= 0 && sequence % 4 == 1 && now >= readyAt
			&& Double.isFinite(distance) && distance >= MIN_DISTANCE && distance <= MAX_DISTANCE
			&& Double.isFinite(height) && Math.abs(height) <= 1
			&& Double.isFinite(aura) && aura >= READY_AURA;
	}

	/** Slow reduces the accepted footwork; haste can never expand its marked distance. */
	public static double travel(double speedRatio) {
		return Double.isFinite(speedRatio) && speedRatio > 0 ? STEP_DISTANCE * Math.min(1, speedRatio) : 0;
	}

	public static double travelFraction(long age) {
		return Math.max(0, Math.min(1, (age - GATHER + 1.0) / STEP_TICKS));
	}

	public static boolean hits(double forward, double side, double height) {
		return Double.isFinite(forward) && Double.isFinite(side) && Double.isFinite(height)
			&& forward >= 0 && forward <= REACH && Math.abs(side) <= HALF_WIDTH && Math.abs(height) <= HEIGHT;
	}
}
