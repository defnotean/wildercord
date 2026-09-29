package dev.wildercord.spell;

import java.util.List;

/**
 * Creature affinities, the pure part: a creature may be weak to an element (that element's spells hit
 * it 50% harder), resist one (half) or, in a few cases, be immune (nothing). Which creature is which is
 * data, entity type tags under {@code data/wildercord/tags/entity_type/affinity/} read by
 * {@code cast.Affinities}; this says how what's true of a creature and a hit becomes one multiplier.
 * Players have no affinities.
 */
public final class Affinity {
	private Affinity() {}

	/** A weakness: the hit lands 50% harder. */
	public static final double WEAK = 1.5;
	/** A resistance: half. */
	public static final double RESIST = 0.5;

	/** The ten elements, in the order the Grimoire and the HUD list them. */
	public static final List<String> ELEMENTS = List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood");

	/** What a creature's affinity made of one hit. */
	public enum Verdict {
		NONE, WEAK, RESISTED, IMMUNE
	}

	/**
	 * How a hit of one element fares against a creature.
	 *
	 * @param weak     the creature is weak to the element
	 * @param resists  it resists it (its kind does, or it's a Runebound whose Cord carries that element)
	 * @param immune   it's immune to it
	 * @param reaction a reaction went off on it with this very hit: a resistance doesn't hold against one
	 *                 (immunity still does)
	 */
	public static Verdict judge(boolean weak, boolean resists, boolean immune, boolean reaction) {
		if (immune) {
			return Verdict.IMMUNE;
		}
		if (weak && resists) {
			// A Runebound carrying the element it's weak to: the two cancel out.
			return Verdict.NONE;
		}
		if (weak) {
			return Verdict.WEAK;
		}
		return resists && !reaction ? Verdict.RESISTED : Verdict.NONE;
	}

	public static double multiplier(Verdict verdict) {
		return switch (verdict) {
			case WEAK -> WEAK;
			case RESISTED -> RESIST;
			case IMMUNE -> 0.0;
			case NONE -> 1.0;
		};
	}

	/**
	 * The element a hit with no effect behind it counts as, from what its damage is (a collision's
	 * burst, a secret spell's blast): burning is fire, freezing frost, lightning storm, a sonic boom
	 * void. Empty when the damage doesn't say.
	 *
	 * @param fire      the damage burns
	 * @param freezing  it freezes
	 * @param lightning it's lightning
	 * @param sonic     it's a sonic boom
	 */
	public static String elementOf(boolean fire, boolean freezing, boolean lightning, boolean sonic) {
		return fire ? "fire" : freezing ? "frost" : lightning ? "storm" : sonic ? "void" : "";
	}
}
