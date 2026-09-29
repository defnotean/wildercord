package dev.wildercord.spell;

import java.util.Map;

/**
 * Elemental leaning: a caster's deepest affinity colours their magic. Once one element's affinity has
 * reached level I and clearly leads the rest, the caster leans toward it: their Heart Circles and
 * charging circles take its colour. It's the face of an affinity, not a second bonus: the power comes
 * from the affinity's own level (see {@link PlayerAffinity}), so nobody feels locked in by it.
 */
public final class Leaning {
	private Leaning() {}

	/** The leading element needs this many times the affinity points of the runner-up. */
	public static final double LEAD = 1.25;

	/** The element these affinity points lean toward, or "" for none yet. */
	public static String of(Map<String, Integer> points) {
		String best = "";
		int first = 0;
		int second = 0;
		for (Map.Entry<String, Integer> entry : points.entrySet()) {
			int n = entry.getValue();
			if (n > first) {
				second = first;
				first = n;
				best = entry.getKey();
			} else if (n > second) {
				second = n;
			}
		}
		return first >= PlayerAffinity.threshold(1) && first >= second * LEAD ? best : "";
	}
}
