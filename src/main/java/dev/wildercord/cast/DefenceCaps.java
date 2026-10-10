package dev.wildercord.cast;

/**
 * How far a player's defences can stack. Every spell, art and ward that grants Resistance gives a player at most
 * {@link #RESISTANCE_CAP} (Resistance II, 40% less), so no guard turns a fight into nothing; only a dodge (a grant of at most
 * {@link #DODGE_TICKS}, like Time Skip's moment out of time) keeps its full level.
 */
public final class DefenceCaps {
	private DefenceCaps() {}

	/** The highest Resistance amplifier a player keeps from a lasting grant: 1, Resistance II. */
	public static final int RESISTANCE_CAP = 1;
	/** A grant this short (ticks) is a dodge, not a guard, and keeps its level. */
	public static final int DODGE_TICKS = 10;

	/** The amplifier a player keeps from a Resistance grant of {@code amplifier} lasting {@code ticks}. */
	public static int resistance(int amplifier, int ticks) {
		if (ticks > 0 && ticks <= DODGE_TICKS) {
			return amplifier;
		}
		return Math.min(RESISTANCE_CAP, amplifier);
	}
}
