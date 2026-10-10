package dev.wildercord.monster;

/**
 * Mage-hunters (0.13), as plain numbers. Hooded illagers who hunt casters: once a heart holds five circles, a band of them
 * may come for its owner by night. Bolts come apart near them, spells hurt them half as much, and every blow they land takes
 * mana. They carry no magic of their own.
 */
public final class MageHunterRules {
	private MageHunterRules() {}

	/** How far from a hunter a bolt someone else cast comes apart (blocks). */
	public static final double NULL_RANGE = 6.0;
	/** The share of a spell's harm a hunter takes. */
	public static final float SPELL_TAKEN = 0.5F;
	/** Mana a hunter's blow takes. */
	public static final float DRAIN = 15;
	/** The circles a heart needs before the hunters take notice. */
	public static final int MIN_CIRCLE = 5;
	/** How often the night is checked for a hunt (ticks), and the chance one comes for each caster at each check. */
	public static final int CHECK_TICKS = 20 * 120;
	public static final double CHANCE = 0.15;
	/** How far from their quarry a band appears (blocks). */
	public static final int SPAWN_NEAR = 24, SPAWN_FAR = 32;
	/** No new band while another hunter is this close to the quarry (blocks). */
	public static final int APART = 64;

	/** Whether a caster with {@code circles} formed is hunted at all. */
	public static boolean hunted(int circles) {
		return circles >= MIN_CIRCLE;
	}

	/** How many hunters come: one, and a second more often the more circles the quarry holds (every time from the 15th). */
	public static int band(int circles, double roll) {
		if (!hunted(circles)) return 0;
		return roll < (circles - MIN_CIRCLE) / 10.0 ? 2 : 1;
	}

	/** The mana a blow takes from a caster holding {@code mana}: never more than they have. */
	public static float drained(float mana) {
		return Math.max(0, Math.min(mana, DRAIN));
	}

	/** The harm a hunter takes from a spell's {@code amount}. */
	public static float spellHarm(float amount) {
		return amount * SPELL_TAKEN;
	}
}
