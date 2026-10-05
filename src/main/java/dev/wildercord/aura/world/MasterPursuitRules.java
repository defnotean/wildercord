package dev.wildercord.aura.world;

/** A free Master's finite answer to a live cast. All schools preserve the same readable three-beat timing. */
public final class MasterPursuitRules {
	private MasterPursuitRules() {}

	public static final int WINDUP = 12, DASH_TICKS = 8, STRIKE_TELL = 12;
	public static final int TELL = WINDUP + DASH_TICKS + STRIKE_TELL, RECOVERY = 30, WARNING_REFRESH = 3, MIN_CHARGE_AGE = 6, MAX_CHARGE_AGE = 200;
	public static final double DAMAGE = 28, MIN_DISTANCE = 4.25, STOP_SHORT = 2.0;
	public static final double REACH = 3.25, HALF_WIDTH = .8, HEIGHT = 1.8, PATH_SAMPLE = .2;
	public record School(double range, double travel, double cost, int cooldown) {}
	private static final School EMBER = new School(7.5, 4.8, 28, 140);
	private static final School GALE = new School(9.1, 6.4, 26, 100);
	private static final School STONE = new School(6.25, 3.6, 30, 160);

	public static School school(int discipline) {
		return switch (MastersRules.discipline(discipline)) {
			case MastersRules.GALE -> GALE;
			case MastersRules.STONE -> STONE;
			default -> EMBER;
		};
	}

	public enum Beat { WARNING, DASH, STRIKE_WARNING, STRIKE, EXPIRED }
	public static Beat beat(long elapsed) {
		if (elapsed < 0 || elapsed > TELL) return Beat.EXPIRED;
		if (elapsed < WINDUP) return Beat.WARNING;
		if (elapsed < WINDUP + DASH_TICKS) return Beat.DASH;
		return elapsed < TELL ? Beat.STRIKE_WARNING : Beat.STRIKE;
	}

	/** Movement targets a captured point, not a new target position each tick. No arrival can shorten the final tell. */
	public static double travelFraction(long elapsed) {
		return Math.max(0, Math.min(1, (elapsed - WINDUP + 1.0) / DASH_TICKS));
	}

	public static boolean observedCharge(long age) { return age >= MIN_CHARGE_AGE && age <= MAX_CHARGE_AGE; }

	public static boolean eligible(int discipline, double distance, double height, double aura, long now, long readyAt) {
		School school = school(discipline);
		return now >= readyAt && Double.isFinite(aura) && aura >= school.cost()
			&& Double.isFinite(distance) && distance >= MIN_DISTANCE && distance <= school.range()
			&& Double.isFinite(height) && Math.abs(height) <= 1.0;
	}

	/** A slow can shorten this attempt; speed buffs cannot expand the accepted school cap. */
	public static double travel(int discipline, double distance, double speedRatio) {
		if (!Double.isFinite(distance) || distance < MIN_DISTANCE || !Double.isFinite(speedRatio) || speedRatio <= 0) return 0;
		return Math.min(school(discipline).travel(), distance - STOP_SHORT) * Math.min(1, speedRatio);
	}

	public static boolean hits(double forward, double side, double height) {
		return Double.isFinite(forward) && Double.isFinite(side) && Double.isFinite(height)
			&& forward >= 0 && forward <= REACH && Math.abs(side) <= HALF_WIDTH && Math.abs(height) <= HEIGHT;
	}

	/** Check every corner of a swept body's horizontal bounds; checking only the centre allows boundary overhang. */
	public static boolean insideArena(double minX, double maxX, double minZ, double maxZ, double homeX, double homeZ, double radius) {
		if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minZ) || !Double.isFinite(maxZ)
			|| !Double.isFinite(homeX) || !Double.isFinite(homeZ) || !Double.isFinite(radius) || radius <= 0 || minX > maxX || minZ > maxZ) return false;
		double dx = Math.max(Math.abs(minX - homeX), Math.abs(maxX - homeX));
		double dz = Math.max(Math.abs(minZ - homeZ), Math.abs(maxZ - homeZ));
		return dx * dx + dz * dz <= radius * radius;
	}
}
