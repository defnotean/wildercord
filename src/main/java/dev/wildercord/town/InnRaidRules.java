package dev.wildercord.town;

/**
 * Inn raids (0.13), as plain numbers. Bandits come for a Wayfarer Inn at night while a traveller the keepers know is inside: a
 * few waves of pillagers and vindicators, the last led by a Bandit Captain. Holding the inn pays emeralds and reputation to
 * everyone who stood their ground; falling, fleeing or running out of time just sends the bandits off (no penalty).
 */
public final class InnRaidRules {
	private InnRaidRules() {}

	/** The least standing that draws a raid: the bandits don't bother with strangers. */
	public static final BountyRules.Tier MIN_TIER = BountyRules.Tier.KNOWN;
	/** The chance, at each of the site check's beats (every 2 seconds) at night inside an inn, that a raid comes. */
	public static final double CHANCE = 1.0 / 200;
	/** Days between raids on one traveller. */
	public static final int REST_DAYS = 3;
	/** How far from the inn's middle the defenders may go before it counts as fleeing. */
	public static final double RADIUS = 40;
	/** Where the bandits come from: this far out from the inn's middle. */
	public static final int SPAWN_NEAR = 22, SPAWN_FAR = 30;
	/** Time for each wave, and the breath between waves (ticks). */
	public static final int WAVE_TICKS = 20 * 90, BREATH_TICKS = 100;
	/** How much tougher the Bandit Captain is than a tempered creature of its kind. */
	public static final double CAPTAIN_HEALTH = 2.5;

	/** Whether a raid may come for a traveller of {@code tier} today, having last been raided on {@code lastDay} (-1 for never). */
	public static boolean due(BountyRules.Tier tier, long lastDay, long today) {
		return tier.ordinal() >= MIN_TIER.ordinal() && (lastDay < 0 || today - lastDay >= REST_DAYS);
	}

	/** How many waves come: two for Known, three for Friend, four for Honoured. */
	public static int waves(BountyRules.Tier tier) {
		return 1 + Math.max(1, tier.ordinal());
	}

	/** How many bandits come in {@code wave} (1-based), not counting the Captain. */
	public static int waveSize(BountyRules.Tier tier, int wave) {
		return 2 + tier.ordinal() + wave;
	}

	/** How long a raid carried over a restart waits for a defender to come back before the bandits give up. */
	public static final int RESUME_GRACE = 20 * 60 * 5;

	/**
	 * The wave a raid carried over a restart starts again from: the one it had reached, fought afresh, since its bandits went
	 * with the server. A raid that hadn't sent a wave yet starts from the first.
	 */
	public static int resumeFrom(BountyRules.Tier tier, int reached) {
		return Math.max(1, Math.min(waves(tier), reached));
	}

	/** Whether {@code wave} is the last. */
	public static boolean lastWave(BountyRules.Tier tier, int wave) {
		return wave >= waves(tier);
	}

	/** Emeralds for holding the inn. */
	public static int emeralds(BountyRules.Tier tier) {
		return 4 + 3 * waves(tier);
	}

	/** Reputation for holding the inn. */
	public static int reputation(BountyRules.Tier tier) {
		return 2 * waves(tier);
	}
}
