package dev.wildercord.spell;

/**
 * The Tribulation (0.12 "Tempering"): the 5th, 10th, 15th and 20th circles don't simply form. When the heart is ready and the
 * meditation completes, the world answers with waves of tempered Runebound around the caster; only beating every wave forms
 * the circle. Pure numbers, so the server and the tests agree.
 */
public final class TribulationRules {
	private TribulationRules() {}

	/** Every fifth circle is a tribulation. */
	public static final int EVERY = 5;
	/** How far from where it began the caster may go before it counts as fleeing. */
	public static final double RADIUS = 24;
	/** Rest between one wave falling and the next arriving (ticks). */
	public static final int BREATH_TICKS = 80;
	/** Time allowed for each wave (ticks): a tribulation not met in time is lost. */
	public static final int WAVE_TICKS = 20 * 75;
	/** How long a failed tribulation waits before it can be faced again (ticks). */
	public static final int RETRY_TICKS = 20 * 60 * 5;

	/** Health (half-hearts) each tribulation circle held adds for good: its scar. */
	public static final double SCAR_HEALTH = 2;

	/** The scars a heart of {@code circles} carries: one for each tribulation circle among them. */
	public static int scars(int circles) {
		return Math.max(0, Math.min(circles, Circles.MAX)) / EVERY;
	}

	/** Whether forming circle {@code n} calls a tribulation. */
	public static boolean tribulation(int n) {
		return n > 0 && n <= Circles.MAX && n % EVERY == 0;
	}

	/** Its tier: 1 for the 5th circle up to 4 for the 20th. */
	public static int tier(int n) {
		return Math.max(1, Math.min(Circles.MAX / EVERY, n / EVERY));
	}

	/** How many waves it sends: three at the 5th, six at the 20th. */
	public static int waves(int n) {
		return 2 + tier(n);
	}

	/** How many monsters come in {@code wave} (1-based). */
	public static int waveSize(int n, int wave) {
		return 3 + tier(n) + Math.max(0, wave - 1);
	}

	/** How many of them are Adepts (stronger casters). */
	public static int adepts(int n, int wave) {
		return Math.min(waveSize(n, wave), tier(n) - 1 + wave / 2);
	}

	/** Extra health on each of its monsters, as a share of their own (added to base). */
	public static double healthBonus(int n) {
		return 0.5 * tier(n);
	}

	/** Extra melee damage on each of its monsters, as a share of their own. */
	public static double damageBonus(int n) {
		return 0.25 * tier(n);
	}

	/** Runes a won tribulation leaves: two per tier. */
	public static int spoilRunes(int n) {
		return 2 * tier(n);
	}

	/** The highest rune tier among its spoils: rare runes from the 10th circle on, the rarest from the 15th. */
	public static int spoilRuneTier(int n, double roll) {
		int tier = tier(n);
		if (tier >= 3 && roll < 0.25) return 4;
		return tier >= 2 && roll < 0.6 ? 3 : 2;
	}

	/** Mana Crystals a won tribulation leaves: one per tier. */
	public static int spoilCrystals(int n) {
		return tier(n);
	}

	/** Experience a won tribulation gives. */
	public static int spoilXp(int n) {
		return 80 * tier(n);
	}

	/** Whether the last wave of tribulation {@code n} has been reached. */
	public static boolean lastWave(int n, int wave) {
		return wave >= waves(n);
	}
}
