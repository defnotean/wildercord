package dev.wildercord.spell;

/**
 * Rune ranks. Three copies of the same rune at the Fusion Altar make the next rank: rank I is the
 * rune as crafted, rank II hits 25% harder and rank III 50% harder, at the same mana cost. A caster
 * knows each rune at one rank, and learning a higher one upgrades that rune everywhere it's threaded.
 * Only effects with power can be ranked up (see {@link #rankable}).
 */
public final class Ranks {
	private Ranks() {}

	public static final int MAX = 3;

	/** A caster's rank for each rune id: 1 for anything they haven't ranked up. */
	@FunctionalInterface
	public interface Lookup {
		int rank(String runeId);

		/** Everything at rank I: the readout for a spell nobody in particular casts (a scroll, a code in chat). */
		Lookup NONE = id -> 1;
	}

	/** Power multiplier at {@code rank}: 1, 1.25, 1.5. */
	public static double power(int rank) {
		return switch (clamp(rank)) {
			case 2 -> 1.25;
			case 3 -> 1.5;
			default -> 1.0;
		};
	}

	/**
	 * Levels added to effects measured in levels (Speed III, Haste II...) and to mining tiers: rank III
	 * is half again as strong, as one Amplify is, so it counts as one Amplify there.
	 */
	public static int levels(int rank) {
		return clamp(rank) >= 3 ? 1 : 0;
	}

	/** XP levels the Fusion Altar asks to make {@code rank}: 2 for rank II, 5 for rank III. */
	public static int xpCost(int rank) {
		return clamp(rank) >= 3 ? 5 : 2;
	}

	/** Whether a rune has ranks: effects with power, other than innate runes (they grow with the heart instead). */
	public static boolean rankable(RuneDef rune) {
		return rune.family() == RuneFamily.EFFECT && rune.has(Trait.POWER) && !Runes.innate(rune);
	}

	public static int clamp(int rank) {
		return Math.max(1, Math.min(MAX, rank));
	}

	/** " II" or " III" after a rune's name, or nothing at rank I. */
	public static String suffix(int rank) {
		return switch (clamp(rank)) {
			case 2 -> " II";
			case 3 -> " III";
			default -> "";
		};
	}
}
