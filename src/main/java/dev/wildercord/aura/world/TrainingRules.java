package dev.wildercord.aura.world;

/** Terrain training uses the existing, lifetime-capped practice allowance. No idle combat progression. */
public final class TrainingRules {
	private TrainingRules() {}
	public enum Ground { NONE, WATERFALL, SUMMIT }
	public static final int CHECK_TICKS = 40;
	public static final int WATERFALL_HEIGHT = 6;
	public static final int SUMMIT_RISE = 64;
	public static final double GAIN = 1.25;
	public static final double PRACTICE = 0.5;

	public static boolean waterfall(int fallingColumn, boolean nearby, boolean grounded, boolean dry) {
		return fallingColumn >= WATERFALL_HEIGHT && nearby && grounded && dry;
	}

	/** Eight terrain samples, four at six blocks and four at twelve. A cliff ledge isn't a summit. */
	public static boolean summit(int feet, int sea, boolean mountain, boolean sky, boolean grounded, int[] heights) {
		if (!mountain || !sky || !grounded || feet < sea + SUMMIT_RISE || heights.length != 8) return false;
		int drops = 0;
		for (int height : heights) {
			if (height > feet + 2) return false;
			if (height <= feet - 4) drops++;
		}
		return drops >= 4;
	}
}
