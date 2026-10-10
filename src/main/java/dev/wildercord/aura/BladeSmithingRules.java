package dev.wildercord.aura;

/**
 * Blade smithing (0.13): a bonded blade is tempered at an anvil with Master's Steel, the sliver a Sword Master's blade leaves
 * when they fall. Each temper makes coated blows land a little harder. How far a blade can be tempered waits on how far its bond
 * has grown, and the steel comes only from beating Masters, so a fully tempered blade is the mark of having beaten all sixteen
 * (or many of them again). Pure numbers, so the anvil, the tooltip and the tests agree.
 */
public final class BladeSmithingRules {
	private BladeSmithingRules() {}

	public static final int MAX_TEMPER = 5;
	/** The highest temper a blade of each bond tier takes: none unbonded, I bonded, II named, III awakened, V soulforged. */
	private static final int[] CAP = {0, 1, 2, 3, MAX_TEMPER};
	/** What each temper adds to a coated blow's bonus. */
	public static final double PER_TEMPER = 0.05;
	/** Against another player a temper counts for this share (and stays under their cap and the PvP scale). */
	public static final double AGAINST_PLAYER = 0.5;
	/** Master's Steel for a Master's first fall at your hands, and for every fall after. */
	public static final int FIRST_STEEL = 2;
	public static final int REPEAT_STEEL = 1;

	/** The highest temper a blade of {@code tier} (see {@link BladeRules}) takes. */
	public static int cap(int tier) {
		return tier < 0 ? 0 : CAP[Math.min(tier, CAP.length - 1)];
	}

	/** Whether a blade at {@code temper} of {@code tier} can take another. */
	public static boolean canTemper(int temper, int tier) {
		return temper >= 0 && temper < cap(tier);
	}

	/** Master's Steel the temper to {@code next} takes: two for each level, 30 from nothing to V. */
	public static int steel(int next) {
		return 2 * Math.max(1, Math.min(MAX_TEMPER, next));
	}

	/** Experience levels the temper to {@code next} takes at the anvil. */
	public static int levels(int next) {
		return 4 + 2 * Math.max(1, Math.min(MAX_TEMPER, next));
	}

	/** What a coated blow's bonus is multiplied by from a blade at {@code temper}. */
	public static double coat(int temper, boolean againstPlayer) {
		double per = againstPlayer ? PER_TEMPER * AGAINST_PLAYER : PER_TEMPER;
		return 1 + per * Math.max(0, Math.min(MAX_TEMPER, temper));
	}

	/** Master's Steel a credited swordsman takes from a Master's fall. */
	public static int reward(boolean firstClear) {
		return firstClear ? FIRST_STEEL : REPEAT_STEEL;
	}

	/** Total steel from nothing to {@code temper}. */
	public static int totalSteel(int temper) {
		int n = 0;
		for (int t = 1; t <= Math.min(MAX_TEMPER, temper); t++) n += steel(t);
		return n;
	}
}
