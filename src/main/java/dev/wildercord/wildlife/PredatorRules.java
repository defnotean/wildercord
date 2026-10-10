package dev.wildercord.wildlife;

/**
 * The 0.13 tameable predators, kin to the black bobcat, each with a perk of its own: the Frost Lynx of the snowfields, whose
 * bite chills, and the Dune Cougar of the savannas and badlands, whose eye picks out the monsters stalking its owner. Pure
 * numbers, so they can be tested without a world.
 */
public final class PredatorRules {
	private PredatorRules() {}

	/** The chance raw rabbit or chicken tames a wild Frost Lynx. */
	public static final double LYNX_TAME_CHANCE = 1 / 3.0;
	/** The chance raw beef, mutton or pork tames a wild Dune Cougar: warier than its kin. */
	public static final double COUGAR_TAME_CHANCE = 1 / 4.0;

	/** How long a lynx's bite slows what it bit, in ticks. */
	public static final int CHILL_TICKS = 60;
	/** The slowness it gives (amplifier 1 is Slowness II). */
	public static final int CHILL_AMPLIFIER = 1;
	/** The frost each bite adds to what it bit, in ticks of freezing. */
	public static final int CHILL_FROST = 100;

	/** How often a cougar looks over its owner's surroundings, in ticks. */
	public static final int MARK_INTERVAL = 60;
	/** How far from its owner it picks out monsters, in blocks. */
	public static final double MARK_RADIUS = 16;
	/** How long a monster it marks glows, in ticks: a little longer than between looks, so a mark never flickers off. */
	public static final int MARK_TICKS = MARK_INTERVAL + 20;
	/** How near its owner a cougar must be to watch over them, in blocks. */
	public static final double MARK_LEASH = 24;

	/** The frost a bite leaves on something already this frozen, never past the point it's fully frozen. */
	public static int chilled(int frozen, int required) {
		return Math.max(frozen, Math.min(required, frozen + CHILL_FROST));
	}

	/** Whether a cougar watches over its owner now: tame, standing, near them, and on its beat. */
	public static boolean marks(boolean tame, boolean sitting, double ownerDistance, long now, int id) {
		return tame && !sitting && ownerDistance <= MARK_LEASH && Math.floorMod(now + id, MARK_INTERVAL) == 0;
	}
}
