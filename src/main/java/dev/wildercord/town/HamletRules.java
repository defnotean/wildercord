package dev.wildercord.town;

import dev.wildercord.town.BountyRules.Tier;

/**
 * Hamlets (0.13), as plain numbers. Every village is a hamlet with a name and a standing of its own, apart from the inns'.
 * Trading with its villagers and killing monsters near its bell earn it, a little each day. The better a hamlet knows you, the
 * more it treats you as its hero while you're there: cheaper trades and, now and then, a gift.
 */
public final class HamletRules {
	private HamletRules() {}

	/** How far from its bell a hamlet reaches (blocks). */
	public static final int RADIUS = 64;
	/** The villagers a bell needs about it to be a hamlet. */
	public static final int VILLAGERS = 3;
	/** Reputation for a trade, and for a monster slain near the bell. */
	public static final int TRADE = 1, DEFEND = 1;
	/** The most reputation one hamlet gives a traveller in a day. */
	public static final int DAILY = 8;
	/** How long the hamlet's favour lasts after you leave (ticks); it is refreshed while you stay. */
	public static final int FAVOUR_TICKS = 20 * 30;

	/** How much of {@code gain} a hamlet still gives today, having given {@code today} already. */
	public static int earned(int today, int gain) {
		return Math.max(0, Math.min(gain, DAILY - today));
	}

	/** The Hero of the Village level (as an amplifier) a hamlet's standing grants, or -1 for none: a stranger gets nothing. */
	public static int favour(Tier tier) {
		return tier.ordinal() - 1;
	}

	private static final String[] FIRST = {"Ash", "Bram", "Cold", "Dun", "Elder", "Fern", "Gold", "Hazel", "Iron", "Kings", "Lark", "Mill",
		"North", "Oak", "Pen", "Quarry", "Red", "Stone", "Thorn", "Upper", "Wil", "Yarrow", "Brook", "Crow", "Holly", "Marsh", "Rush", "Sedge"};
	private static final String[] LAST = {"ford", "wick", "by", "ton", "stead", "dale", "combe", "field", "holt", "mere", "hithe", "worth",
		"ley", "thorpe", "bury", "well", "cross", "hollow"};

	/** A hamlet's name, from its bell's place. */
	public static String name(long seed) {
		long h = seed * 0x9E3779B97F4A7C15L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		int first = (int) Math.floorMod(h, (long) FIRST.length);
		int last = (int) Math.floorMod(h >>> 16, (long) LAST.length);
		return FIRST[first] + LAST[last];
	}
}
