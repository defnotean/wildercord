package dev.wildercord.town;

/**
 * Wandering caravans (0.13), as plain numbers. Now and then a caravan makes camp near a traveller out in the overworld: a
 * caravaneer and two pack llamas. They stay about a day and sell what the inns don't: rare runes, mount gear and the
 * far-off ingredients the best meals need. A traveller the inns know well is offered more runes, and better ones.
 */
public final class CaravanRules {
	private CaravanRules() {}

	/** How often the road is checked for a caravan (ticks), and the chance one comes each time. */
	public static final int CHECK_TICKS = 20 * 60 * 8;
	public static final double CHANCE = 0.3;
	/** How long a caravan stays before it moves on (ticks): about a Minecraft day. */
	public static final int STAY_TICKS = 24000;
	/** Where it makes camp: this far from the traveller. */
	public static final int SPAWN_NEAR = 24, SPAWN_FAR = 40;
	/** No caravan comes within this many blocks of another. */
	public static final double APART = 256;
	/** Its pack llamas. */
	public static final int LLAMAS = 2;

	/** How many runes it carries for a traveller of {@code tier}: two for a stranger, up to four for the honoured. */
	public static int runes(BountyRules.Tier tier) {
		return 2 + (tier.ordinal() + 1) / 2;
	}

	/** The tier of one rune it carries, from a roll in [0, 1): tier 4 comes more often as the inns know you better. */
	public static int runeTier(BountyRules.Tier tier, double roll) {
		return roll < 0.15 + 0.15 * tier.ordinal() ? 4 : 3;
	}

	/** What a rune of {@code runeTier} costs, in emeralds. */
	public static int runePrice(int runeTier) {
		return runeTier >= 4 ? 20 : 12;
	}
}
