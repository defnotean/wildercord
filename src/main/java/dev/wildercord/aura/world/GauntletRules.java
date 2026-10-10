package dev.wildercord.aura.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Master Gauntlet (0.13), as plain numbers. Once a player has beaten every Sword Master, {@code /master gauntlet} sends them
 * all again, one after another in a shuffled order, with only a short breath between. The first full run pays a blade of
 * their own; every run is timed against their best.
 */
public final class GauntletRules {
	private GauntletRules() {}

	/** The breath between one Master falling and the next arriving (ticks). */
	public static final int BREATH_TICKS = 100;
	/** How long each Master waits before it draws, instead of a trial's thirty-second lobby (ticks). */
	public static final int OPENING_TICKS = 60;
	/** The share of their health a challenger gets back in each breath. */
	public static final double HEAL_SHARE = 0.25;

	/** Every school with a Master, in id order. */
	public static List<Integer> schools() {
		List<Integer> schools = new ArrayList<>();
		for (int school = 0; school < MastersRules.SCHOOLS; school++) if (MastersRules.knownSchool(school)) schools.add(school);
		return schools;
	}

	/** Whether a record of first clears ({@code cleared} bits) opens the Gauntlet: every Master beaten. */
	public static boolean eligible(int cleared) {
		return (cleared & MasterVictoryRules.ALL) == MasterVictoryRules.ALL;
	}

	/** How many Masters are still to beat before the Gauntlet opens. */
	public static int missing(int cleared) {
		return Integer.bitCount(MasterVictoryRules.ALL & ~cleared);
	}

	/** The order a run meets the Masters in: every school once, shuffled by {@code seed}. */
	public static List<Integer> order(long seed) {
		List<Integer> order = schools();
		Collections.shuffle(order, new Random(seed));
		return order;
	}

	/** Whether a run of {@code seconds} beats the best so far ({@code best} 0 for none). */
	public static boolean better(int best, int seconds) {
		return best <= 0 || seconds < best;
	}

	/** A run's time as minutes and seconds. */
	public static String time(int seconds) {
		return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
	}
}
