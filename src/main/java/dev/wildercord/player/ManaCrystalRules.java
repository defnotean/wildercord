package dev.wildercord.player;

/** Permanent crystal growth, kept pure so old saves and the absorption boundary can be checked headlessly. */
public final class ManaCrystalRules {
	private ManaCrystalRules() {}

	public static final int MANA_PER_CRYSTAL = 10;
	public static final int MAX_CRYSTALS = 100;

	/** Old saves keep their count; malformed counts cannot grant negative or unbounded mana. */
	public static int count(int saved) {
		return Math.max(0, Math.min(MAX_CRYSTALS, saved));
	}

	public static int bonus(int saved) {
		return count(saved) * MANA_PER_CRYSTAL;
	}
}
