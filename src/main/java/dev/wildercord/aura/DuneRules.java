package dev.wildercord.aura;

/**
 * Dune Breath, the pure parts: sand and the desert. Its blows throw grit in the eyes (blinded, and a creature loses its target),
 * its ground swallows feet like quicksand, and its footing shifts. Its arts' numbers, its passive, its awakened Dominion's and its
 * finisher's. Blindness on a player is held short ({@link #PVP_BLIND}), and never lands on a boss.
 */
public final class DuneRules {
	private DuneRules() {}

	// ------------------------------------------------------------------ the passive: a coated blow may throw grit

	/** The chance a coated blow throws grit in its foe's eyes, by stage. */
	public static double gritChance(int stage) {
		return stage <= AuraRules.NONE ? 0 : 0.05 + 0.05 * stage;
	}

	public static final int GRIT_TICKS = 30;

	/** The most a player is blinded by any one of Dune's grit (ticks). */
	public static final int PVP_BLIND = 20;

	/** How long grit blinds a target: as asked on a creature, {@link #PVP_BLIND} at most on a player, nothing on a boss. */
	public static int blind(int ticks, boolean player, boolean boss) {
		if (boss || ticks <= 0) {
			return 0;
		}
		return player ? Math.min(ticks, PVP_BLIND) : ticks;
	}

	/** How deep a sinking foe is slowed (Slowness level, 0 is I): at most II on a player, IV on a creature, a boss not at all (-1). */
	public static int sink(int depth, boolean player, boolean boss) {
		if (boss || depth < 0) {
			return -1;
		}
		return player ? Math.min(depth, 1) : Math.min(depth, 3);
	}

	// ------------------------------------------------------------------ I. Grit Flick: a cone of thrown sand

	public static final double FLICK_FACTOR = 0.7;
	public static final double FLICK_REACH = 4.5;
	public static final double FLICK_DEGREES = 70;
	public static final int FLICK_TARGETS = 4;
	public static final int FLICK_BLIND = 40;
	public static final int FLICK_SLOW = 30;

	// ------------------------------------------------------------------ II. Quicksand: the ground ahead swallows feet

	public static final double QUICKSAND_FACTOR = 0.8;
	public static final double QUICKSAND_AHEAD = 3.0;
	public static final double QUICKSAND_RADIUS = 3.0;
	public static final int QUICKSAND_TICKS = 80;
	public static final int QUICKSAND_PERIOD = 10;
	public static final int QUICKSAND_DEPTH = 3;
	public static final double QUICKSAND_SUCK = 0.08;

	// ------------------------------------------------------------------ III. Sandveil: a burst of sand round you

	public static final double VEIL_FACTOR = 1.0;
	public static final double VEIL_RADIUS = 3.5;
	public static final int VEIL_TARGETS = 6;
	public static final int VEIL_BLIND = 50;
	public static final int VEIL_TICKS = 50;

	// ------------------------------------------------------------------ IV. Dune Runner: across the sand, the footing shifting behind

	public static final double RUNNER_FACTOR = 1.0;
	public static final double RUNNER_DISTANCE = 7.0;
	public static final int RUNNER_TICKS = 7;
	public static final double RUNNER_WIDTH = 2.2;
	public static final int RUNNER_TARGETS = 4;
	public static final int RUNNER_SINK = 40;

	// ------------------------------------------------------------------ V. Sea of Sand: the desert rises

	public static final double SEA_FACTOR = 0.3;
	public static final int SEA_GUSTS = 6;
	public static final double SEA_RADIUS = 6.0;
	public static final int SEA_PERIOD = 10;
	public static final int SEA_BLIND = 40;
	public static final int SEA_DEPTH = 2;
	public static final double SEA_CRUSH = 0.4;
	public static final int SEA_TARGETS = 10;

	// ------------------------------------------------------------------ Sovereign: the Shifting Sea

	/** Each pulse foes inside sink and one is blinded. */
	public static final int SOVEREIGN_DEPTH = 2;
	public static final int SOVEREIGN_BLIND = 30;

	// ------------------------------------------------------------------ the finisher: Dust Devil

	public static final double FINISHER_FACTOR = 0.9;
	public static final int FINISHER_BLIND = 40;
}
