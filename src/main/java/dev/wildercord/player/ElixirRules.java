package dev.wildercord.player;

/**
 * Mana elixirs (0.13): brewed potions that bend how mana works, each at a price. Torrent refills mana faster but leaves less
 * room for it; Deep Well holds more but refills slower; Condensing turns more of the mana spent into circle progress but
 * makes every spell dearer. Each counts for at most {@link #MAX_LEVEL} levels. Pure numbers, so the server, the HUD and the
 * tests agree.
 */
public final class ElixirRules {
	private ElixirRules() {}

	public static final int MAX_LEVEL = 2;
	/** Torrent: regeneration added per level. */
	public static final double TORRENT_REGEN = 1.0;
	/** Torrent: share of max mana lost per level. */
	public static final double TORRENT_MAX = 0.25;
	/** Deep Well: share of max mana added per level. */
	public static final double DEEP_MAX = 0.4;
	/** Deep Well: share of regeneration lost per level. */
	public static final double DEEP_REGEN = 0.4;
	/** Condensing: share added to what spent mana condenses, per level. */
	public static final double CONDENSE_BONUS = 0.5;
	/** Condensing: share added to every spell's price, per level. */
	public static final double CONDENSE_COST = 0.2;

	private static int clamp(int level) {
		return Math.max(0, Math.min(MAX_LEVEL, level));
	}

	/** What regeneration is multiplied by under Torrent and Deep Well at these levels (0 = none). */
	public static double regen(int torrent, int deep) {
		return (1 + TORRENT_REGEN * clamp(torrent)) * (1 - DEEP_REGEN * clamp(deep));
	}

	/** What max mana is multiplied by under Torrent and Deep Well at these levels (0 = none). */
	public static double max(int torrent, int deep) {
		return (1 - TORRENT_MAX * clamp(torrent)) * (1 + DEEP_MAX * clamp(deep));
	}

	/** What spent mana condenses toward the next circle at under Condensing at this level. */
	public static double condense(int condensing) {
		return 1 + CONDENSE_BONUS * clamp(condensing);
	}

	/** What a spell's price is multiplied by under Condensing at this level. */
	public static double cost(int condensing) {
		return 1 + CONDENSE_COST * clamp(condensing);
	}
}
