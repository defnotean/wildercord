package dev.wildercord.spell;

import java.util.Map;

/**
 * Elemental leaning: the element a caster casts most slowly colours their magic. Once one
 * element has at least {@link #MIN_CASTS} casts and clearly leads the rest, the caster leans
 * toward it: their Heart Circles and magic circles take its colour, and its effects hit a
 * little harder. It's cosmetic first; the perk is small so nobody feels locked in.
 */
public final class Leaning {
	private Leaning() {}

	public static final int MIN_CASTS = 40;
	/** The leading element needs this many times the casts of the runner-up. */
	public static final double LEAD = 1.25;
	/** Extra power for effects of the element you lean toward. */
	public static final double POWER = 0.10;

	/** The element these cast counts lean toward, or "" for none yet. */
	public static String of(Map<String, Integer> casts) {
		String best = "";
		int first = 0;
		int second = 0;
		for (Map.Entry<String, Integer> entry : casts.entrySet()) {
			int n = entry.getValue();
			if (n > first) {
				second = first;
				first = n;
				best = entry.getKey();
			} else if (n > second) {
				second = n;
			}
		}
		return first >= MIN_CASTS && first >= second * LEAD ? best : "";
	}
}
