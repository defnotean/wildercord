package dev.wildercord.content;

/**
 * The odds of fishing up magic, pure so they can be tested ({@link WildercordLoot} builds the loot from them, and
 * {@code cast.Fishing} reads the water).
 * <ul>
 *   <li><b>Treasure.</b> Vanilla's treasure catch (open water only, likelier with Luck of the Sea) is one item from a
 *       pool of six, each of weight 1. A rune and a Torn Page join that pool, weighted so that about 4 treasure catches
 *       in 11 are a rune and 1 in 11 a Torn Page, as the server's loot multipliers allow.</li>
 *   <li><b>Magic waters.</b> Where magic runs strong at the bobber, a rune comes up tangled in the line on top of the
 *       catch: under a mana storm, on or near a ley line, or in a thunderstorm. Their chances add up, to a cap.</li>
 * </ul>
 */
public final class FishingRules {
	private FishingRules() {}

	/** Vanilla's treasure pool: six entries of weight 1 (a name tag, a saddle, a bow, a rod, a book and a nautilus shell). */
	public static final int VANILLA_TREASURE_WEIGHT = 6;
	/** Treasure catches out of 100 that are a rune, before the rune loot multiplier. */
	public static final int TREASURE_RUNE_CHANCE = 36;
	/** Treasure catches out of 100 that are a Torn Page, before the page loot multiplier. */
	public static final int TREASURE_PAGE_CHANCE = 9;
	/** The most of a treasure catch, out of 100, the rune and the page may take together, however high the multipliers. */
	public static final int TREASURE_MOST = 90;

	/**
	 * The weights a rune and a Torn Page join vanilla's treasure pool with, so that each comes up {@code runeChance} and
	 * {@code pageChance} times in 100 treasure catches (as near as whole weights beside vanilla's six can): {runes, page}.
	 * A chance of 0 leaves that one out (weight 0); any other chance gets at least weight 1. Together they never take
	 * more than {@link #TREASURE_MOST} in 100, so vanilla's treasures still turn up.
	 */
	public static int[] treasureWeights(int runeChance, int pageChance) {
		double runes = Math.max(0, runeChance);
		double page = Math.max(0, pageChance);
		if (runes + page > TREASURE_MOST) {
			double scale = TREASURE_MOST / (runes + page);
			runes *= scale;
			page *= scale;
		}
		double rest = 100 - runes - page;
		return new int[] {weight(runes, rest), weight(page, rest)};
	}

	private static int weight(double chance, double rest) {
		if (chance <= 0) {
			return 0;
		}
		return Math.max(1, (int) Math.round(VANILLA_TREASURE_WEIGHT * chance / rest));
	}

	/** Magic waters, out of 100 catches: under a mana storm. */
	public static final int STORM_CHANCE = 12;
	/** On or near a ley line. */
	public static final int LEY_CHANCE = 5;
	/** In a thunderstorm, with its rain (or snow) falling on the bobber. */
	public static final int THUNDER_CHANCE = 5;
	/** The most all three add up to, out of 100, before the rune loot multiplier. */
	public static final int MAGIC_MOST = 20;

	/**
	 * The chance (0 to 1) that a catch brings up a rune tangled in the line as well: the chances of the magic at the
	 * bobber added up (never past {@link #MAGIC_MOST} in 100), times the server's rune loot multiplier.
	 */
	public static double magicWatersChance(boolean storm, boolean ley, boolean thunder, double multiplier) {
		int sum = (storm ? STORM_CHANCE : 0) + (ley ? LEY_CHANCE : 0) + (thunder ? THUNDER_CHANCE : 0);
		double chance = Math.min(sum, MAGIC_MOST) * Math.max(0, multiplier) / 100.0;
		return Math.max(0, Math.min(1, chance));
	}

	/**
	 * How much likelier each of the runes found only by fishing is than a rune of the sea list of its tier: in a treasure
	 * catch, and (likelier still) in a rune tangled in the line in magic waters. Against the sea list's weight they make
	 * about 2 in 11 of treasure runes and 1 in 3 of tangled ones.
	 */
	public static final int WORLD_WEIGHT_TREASURE = 5;
	public static final int WORLD_WEIGHT_MAGIC = 11;

	/** How close to a ley line's heart (0 to 1) the bobber counts as near it: a little wider than standing on one. */
	public static final double NEAR_LEY = 0.2;
}
