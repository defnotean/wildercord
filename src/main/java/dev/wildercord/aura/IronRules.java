package dev.wildercord.aura;

/**
 * Iron Breath, the pure parts: metal and the forge. Its blows sunder armour for a while and its guard is heavy. Its arts' numbers,
 * its passive, its awakened Dominion's and its finisher's. Sundered armour is a lowering of the foe's armour for a while, never below
 * nothing and never past {@link #SUNDER_MAX} from every source together.
 */
public final class IronRules {
	private IronRules() {}

	// ------------------------------------------------------------------ the passive: a coated blow cracks armour

	/** Armour a coated blow cracks off its foe for {@link #CRACK_TICKS} (points), by stage. */
	public static double crack(int stage) {
		return stage <= AuraRules.NONE ? 0 : 0.5 + 0.5 * stage;
	}

	public static final int CRACK_TICKS = 60;

	/** The most armour sundered from one foe at once, from every source together. */
	public static final double SUNDER_MAX = 8.0;

	/** Armour a sunder takes from a foe already missing {@code sundered} of its {@code armour}: never past {@link #SUNDER_MAX} in all, never more than it has. */
	public static double sunder(double asked, double sundered, double armour) {
		double room = Math.max(0, SUNDER_MAX - Math.max(0, sundered));
		return Math.max(0, Math.min(Math.min(asked, room), Math.max(0, armour)));
	}

	// ------------------------------------------------------------------ I. Sunder Cut: one heavy cut through the plates

	public static final double SUNDER_CUT_FACTOR = 1.0;
	public static final double SUNDER_CUT_REACH = 3.5;
	public static final double SUNDER_CUT_DEGREES = 60;
	public static final int SUNDER_CUT_TARGETS = 2;
	public static final double SUNDER_CUT_ARMOUR = 4.0;
	public static final int SUNDER_CUT_TICKS = 100;

	// ------------------------------------------------------------------ II. Anvil Fall: a leap and a smash

	public static final double ANVIL_FACTOR = 1.2;
	public static final double ANVIL_REACH = 5.0;
	public static final double ANVIL_RADIUS = 2.5;
	public static final int ANVIL_TARGETS = 5;
	public static final int ANVIL_HOLD = 20;
	public static final double ANVIL_ARMOUR = 3.0;
	public static final int ANVIL_SUNDER_TICKS = 80;

	// ------------------------------------------------------------------ III. Bulwark: the heavy guard

	public static final double BULWARK_FACTOR = 0.8;
	public static final int BULWARK_TICKS = 60;
	/** While it stands: Resistance at this level (0 is I), unmoved by blows, and a foe near is shoved back each beat. */
	public static final int BULWARK_RESIST = 1;
	public static final double BULWARK_SHOVE = 0.9;
	public static final double BULWARK_RADIUS = 3.0;
	public static final int BULWARK_TARGETS = 4;

	// ------------------------------------------------------------------ IV. Forge Charge: shoulder first, through the line

	public static final double CHARGE_FACTOR = 1.15;
	public static final double CHARGE_DISTANCE = 7.0;
	public static final int CHARGE_TICKS = 7;
	public static final double CHARGE_WIDTH = 2.0;
	public static final int CHARGE_TARGETS = 4;
	public static final double CHARGE_ARMOUR = 2.0;

	// ------------------------------------------------------------------ V. Worldforge: the hammer of the world

	public static final double WORLDFORGE_FACTOR = 2.0;
	public static final double WORLDFORGE_RING = 0.6;
	public static final double WORLDFORGE_RADIUS = 5.0;
	public static final int WORLDFORGE_TARGETS = 10;
	public static final double WORLDFORGE_ARMOUR = 6.0;
	public static final int WORLDFORGE_TICKS = 160;
	public static final double WORLDFORGE_THROW = 1.2;

	// ------------------------------------------------------------------ Sovereign: the Anvil Court

	/** Each pulse a hammer of sparks cracks the armour of every foe inside; the owner standing inside is hardened. */
	public static final double SOVEREIGN_ARMOUR = 2.0;
	public static final int SOVEREIGN_TICKS = 40;

	// ------------------------------------------------------------------ the finisher: Quench

	public static final double FINISHER_FACTOR = 1.0;
	public static final double FINISHER_ARMOUR = 4.0;
}
