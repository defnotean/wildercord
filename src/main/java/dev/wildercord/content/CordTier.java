package dev.wildercord.content;

/**
 * What each Cord allows: sockets per spell, how many spells, which rune tiers it can hold,
 * and its mana. Numbers match docs/DESIGN.md.
 */
public enum CordTier {
	TWINE("twine", 3, 1, 1, 100, 5),
	COPPER("copper", 5, 2, 2, 150, 6),
	AMETHYST("amethyst", 8, 3, 3, 225, 7),
	ECHO("echo", 12, 4, 4, 300, 8);

	public static final int MAX_SOCKETS = 12;
	public static final int MAX_SPELLS = 4;

	public final String key;
	public final int sockets;
	public final int spells;
	/** The highest rune tier this Cord can hold. Stronger runes stay threaded but go quiet. */
	public final int maxRuneTier;
	public final int maxMana;
	public final int regenPerSecond;

	CordTier(String key, int sockets, int spells, int maxRuneTier, int maxMana, int regenPerSecond) {
		this.key = key;
		this.sockets = sockets;
		this.spells = spells;
		this.maxRuneTier = maxRuneTier;
		this.maxMana = maxMana;
		this.regenPerSecond = regenPerSecond;
	}

	public boolean holds(int runeTier) {
		return runeTier <= maxRuneTier;
	}

	/** The smallest Cord that can hold a rune of this tier. */
	public static CordTier forRuneTier(int runeTier) {
		for (CordTier tier : values()) {
			if (tier.holds(runeTier)) {
				return tier;
			}
		}
		return ECHO;
	}

	/** The smallest Cord with at least this many spells. */
	public static CordTier forSpells(int spells) {
		for (CordTier tier : values()) {
			if (tier.spells >= spells) {
				return tier;
			}
		}
		return ECHO;
	}

	public String itemKey() {
		return "item.wildercord." + key + "_cord";
	}
}
