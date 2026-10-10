package dev.wildercord.spell;

/**
 * Minecraft-free Ascension (0.13): past the Twentieth Circle the heart has no more rings to form, but mana still condenses. Each
 * further stretch of it pays for an Ascension, formed by meditating like a circle: ten in all, each a little more mana,
 * regeneration and power. Ascensions are silent while any circle is cracked, since they stand on all twenty.
 */
public final class AscensionRules {
	private AscensionRules() {}

	public static final int MAX = 10;
	/** Per Ascension: max mana, mana regeneration a second, and spell power. */
	public static final int MANA = 5;
	public static final float REGEN = .1F;
	public static final double POWER = .01;
	/** The condensed mana each Ascension needs past the Twentieth Circle's: this much more for each, growing by the square. */
	public static final int STEP = 400_000, GROWTH = 40_000;

	/** The saved rank, kept in range so an edited save can never grant more. */
	public static int clamp(int saved) {
		return Math.clamp(saved, 0, MAX);
	}

	/** The total condensed mana Ascension {@code rank} needs (1-based). */
	public static int needed(int rank) {
		int r = clamp(rank);
		return r <= 0 ? Circles.condenseNeeded(Circles.MAX) : Circles.condenseNeeded(Circles.MAX) + STEP * r + GROWTH * r * r;
	}

	/** Whether a heart of {@code circles} formed, {@code active} unbroken, may form its next Ascension. */
	public static boolean ready(int rank, int circles, int active, int condensed) {
		int r = clamp(rank);
		return circles >= Circles.MAX && active >= Circles.MAX && r < MAX && condensed >= needed(r + 1);
	}

	public static CircleVows.Effect effect(int rank, int active) {
		int r = clamp(rank);
		if (r == 0 || active < Circles.MAX) return CircleVows.Effect.NONE;
		return new CircleVows.Effect(MANA * r, REGEN * r, 1 + POWER * r, 1, 1, 1);
	}

	private static final String[] NUMERALS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	public static String numeral(int rank) {
		return NUMERALS[clamp(rank)];
	}
}
