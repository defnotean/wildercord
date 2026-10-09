package dev.wildercord.aura.world;

import java.util.List;

/** Ember's two-beat lesson: leave the broad cut, then leave the separately marked narrow wake. */
public final class EmberWakeRules {
	private EmberWakeRules() {}

	public static final int TELL = 24, AFTERBURN_TELL = 26, RECOVERY = 46;
	public static final double COST = 24, CUT_DAMAGE = 26, AFTERBURN_DAMAGE = 26;
	public static final double START = 1, RANGE = 7, HALF_WIDTH = .7, HEIGHT = 1.4;
	public static final int WARNING_REFRESH = 4;

	/** Phases change frequency only. The same two warnings and counter window survive every phase. */
	public static boolean next(int discipline, int sequence, int phase, double distance) {
		return discipline == MastersRules.EMBER && Double.isFinite(distance) && distance >= 0 && distance <= 6
			&& Math.floorMod(sequence, 4 - Math.max(0, Math.min(2, phase))) == 0;
	}

	/** Parallel, separated lanes add party coverage without ever widening a learned lane or increasing damage. */
	public static List<Double> lanes(int participants) {
		int count = MastersRules.participants(participants);
		return count <= 2 ? List.of(0.0) : count <= 5 ? List.of(-2.0, 2.0) : List.of(-3.0, 0.0, 3.0);
	}

	public static boolean hits(double forward, double side, double height) {
		return Double.isFinite(forward) && Double.isFinite(side) && Double.isFinite(height)
			&& forward >= START && forward <= RANGE && Math.abs(side) <= HALF_WIDTH && Math.abs(height) <= HEIGHT;
	}

	/** A missed tick expires harmlessly; a warning cannot become an invisible, arbitrarily late hit. */
	public static boolean ignites(long now, long warnedAt) { return now == warnedAt + AFTERBURN_TELL; }
}
