package dev.wildercord.spell;

/**
 * The plain numbers behind the support pack's runes (Worst First to Faithful), kept apart from the game so they can be
 * tested on their own. Every rest is in ticks.
 */
public final class WardRules {
	private WardRules() {}

	/** Guardlink: the share of an ally's hurt that goes to its guardian. */
	public static final double GUARDLINK_SHARE = 0.4;
	/** Guardlink: the guardian never takes it below this health; at or under it the link breaks. */
	public static final float GUARDLINK_FLOOR = 6.0F;
	/** Guardlink: the link breaks past this distance. */
	public static final double GUARDLINK_RANGE = 16.0;
	/** Grace and Faithful: once per creature per 10 minutes. */
	public static final int SAVE_REST = 12000;
	/** Aegis: once per caster per 2 minutes. */
	public static final int AEGIS_REST = 2400;
	/** Hearthguard and Bellward: what a monster's blow is cut to. */
	public static final double VILLAGE_TAKES = 0.4;
	/** Citadel: what an ally inside takes. */
	public static final double CITADEL_TAKES = 0.8;
	/** Aegis: what an ally under it takes. */
	public static final double AEGIS_TAKES = 0.5;
	/** Managift: the most mana given to each ally. */
	public static final double MANAGIFT_MAX = 20.0;
	/** Managift: how much of the gift arrives. */
	public static final double MANAGIFT_KEPT = 0.75;

	/** Guardlink: how much of {@code damage} goes to a guardian at {@code guardianHealth}. */
	public static float redirect(float damage, float guardianHealth) {
		if (damage <= 0) {
			return 0;
		}
		return (float) Math.max(0.0, Math.min(damage * GUARDLINK_SHARE, guardianHealth - GUARDLINK_FLOOR));
	}

	/** Ironhold: the most one blow may deal (4, a little more with power), so a blow under it is untouched. */
	public static float ironhold(float damage, double power) {
		return (float) Math.min(damage, 4.0 * Math.max(1.0, power));
	}

	/** Hearthsong: what each ally heals with {@code allies} allies in reach (you count). */
	public static double hearthsong(int allies, double power) {
		return Math.min(6.0, 2.0 + Math.max(0, allies)) * power;
	}

	/** Morale: absorption hearts (amplifier + 1) for {@code allies} allies in reach; at least 1, at most 3. */
	public static int moraleHearts(int allies) {
		return Math.max(1, Math.min(3, allies));
	}

	/** Managift: what the giver pays to one ally, from {@code has} mana and what the ally lacks. */
	public static double managiftGiven(double has, double lacks, double power) {
		double wanted = Math.min(MANAGIFT_MAX * Math.max(0.0, power), lacks / MANAGIFT_KEPT);
		return Math.max(0.0, Math.min(has, wanted));
	}

	/** Managift: what arrives of {@code given}. */
	public static double managiftReceived(double given) {
		return given * MANAGIFT_KEPT;
	}

	/** Whether a once-per-rest save last spent at {@code spentAt} is still resting at {@code now}. */
	public static boolean resting(long spentAt, long now, int rest) {
		return spentAt <= now && now - spentAt < rest;
	}

	/** Aftercare: the heals a cast gives in all (6, a little more with power). */
	public static int aftercareHeals(double power) {
		return Math.max(1, (int) Math.round(6.0 * power));
	}
}
