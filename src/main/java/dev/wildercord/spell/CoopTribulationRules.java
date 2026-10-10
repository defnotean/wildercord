package dev.wildercord.spell;

/**
 * Co-op tribulations (0.13): party members standing in the ring when a heart's tribulation begins face it beside the caster.
 * The sky answers in kind: every ally brings half a wave more and a quarter more health on everything that comes, so a full
 * party of four fights waves two and a half times the size of a lone caster's, every monster tougher. Only the caster's death ends it; an ally who falls or flees
 * just drops out. Allies who stand to the end take a share of the spoils: the experience, and half the Mana Crystals (never
 * the runes or the circle, which stay the caster's). Pure numbers, so the tribulation and the tests agree.
 */
public final class CoopTribulationRules {
	private CoopTribulationRules() {}

	public static final int MAX_ALLIES = 3;
	/** What each ally adds to a wave's size, as a share of the lone caster's. */
	public static final double SIZE_PER_ALLY = 0.5;
	/** What each ally adds to the tribulation's monsters' health bonus. */
	public static final double HEALTH_PER_ALLY = 0.25;

	/** Allies that count, out of {@code candidates}. */
	public static int allies(int candidates) {
		return Math.max(0, Math.min(MAX_ALLIES, candidates));
	}

	/** A wave of {@code base} monsters with {@code allies} beside the caster. */
	public static int waveSize(int base, int allies) {
		return (int) Math.ceil(base * (1 + SIZE_PER_ALLY * allies(allies)));
	}

	/** The monsters' health bonus with {@code allies} beside the caster, from the lone caster's {@code base}. */
	public static double healthBonus(double base, int allies) {
		return base + HEALTH_PER_ALLY * allies(allies);
	}

	/** Mana Crystals an ally who stood to the end takes, out of the caster's {@code crystals}. */
	public static int allyCrystals(int crystals) {
		return (crystals + 1) / 2;
	}
}
