package dev.wildercord.aura;

/**
 * Tide Breath, the pure parts: water and the current. Its blows push and drag foes, soak them (fire put out, a moment slowed) and
 * leave the ground wet. Its arts' numbers, its passive, its awakened Dominion's and its finisher's, kept apart from {@link ArtRules}
 * (which lists the arts by these numbers) so the method reads in one place.
 */
public final class TideRules {
	private TideRules() {}

	// ------------------------------------------------------------------ the passive: a coated blow pushes and soaks

	/** How hard a coated blow pushes its foe back, by stage. */
	public static double push(int stage) {
		return stage <= AuraRules.NONE ? 0 : 0.12 + 0.06 * stage;
	}

	/** How long a coated blow leaves its foe soaked (wet, its fire out) and a little slowed (ticks). */
	public static int soak(int stage) {
		return stage <= AuraRules.NONE ? 0 : 20 + 10 * stage;
	}

	/** How hard a current pushes a foe: a soaked one a fifth further (the water has hold of it), held for players and bosses as every throw is. */
	public static double current(double power, boolean soaked, boolean player, boolean boss) {
		return ArtRules.thrown(soaked ? power * 1.2 : power, player, boss);
	}

	// ------------------------------------------------------------------ I. Riptide Cut: an arc that drags foes in and soaks them

	public static final double RIPTIDE_FACTOR = 0.8;
	public static final double RIPTIDE_REACH = 4.0;
	public static final double RIPTIDE_DEGREES = 110;
	public static final int RIPTIDE_TARGETS = 4;
	/** How fast the current drags what it cuts toward the swordsman. */
	public static final double RIPTIDE_DRAG = 0.7;
	public static final int RIPTIDE_SOAK = 50;

	// ------------------------------------------------------------------ II. Breaker: a wave rolling out ahead

	public static final double BREAKER_FACTOR = 0.95;
	public static final double BREAKER_LENGTH = 7.0;
	public static final double BREAKER_WIDTH = 3.0;
	public static final int BREAKER_TARGETS = 6;
	public static final double BREAKER_PUSH = 1.1;
	/** The wet ground it leaves behind: slows foes in it and puts out fire. */
	public static final int BREAKER_WET = 80;

	// ------------------------------------------------------------------ III. Whirlpool: a ring of water drawing foes to its heart

	public static final double WHIRLPOOL_FACTOR = 0.3;
	public static final double WHIRLPOOL_RADIUS = 3.5;
	public static final double WHIRLPOOL_AHEAD = 2.5;
	public static final int WHIRLPOOL_TICKS = 60;
	public static final int WHIRLPOOL_PERIOD = 20;
	public static final double WHIRLPOOL_DRAG = 0.12;
	public static final int WHIRLPOOL_TARGETS = 6;

	// ------------------------------------------------------------------ IV. Surge: a wave dash carrying foes along

	public static final double SURGE_FACTOR = 1.0;
	public static final double SURGE_DISTANCE = 8.0;
	public static final int SURGE_TICKS = 6;
	public static final double SURGE_WIDTH = 2.4;
	public static final int SURGE_TARGETS = 5;
	public static final double SURGE_CARRY = 0.9;

	// ------------------------------------------------------------------ V. Maelstrom: the sea itself, round and round

	public static final double MAELSTROM_FACTOR = 0.35;
	public static final int MAELSTROM_WAVES = 5;
	public static final double MAELSTROM_CRASH = 0.75;
	public static final double MAELSTROM_RADIUS = 6.0;
	public static final int MAELSTROM_PERIOD = 10;
	public static final double MAELSTROM_THROW = 1.3;
	public static final int MAELSTROM_TARGETS = 10;

	// ------------------------------------------------------------------ Sovereign: the Drowning Tide

	/** Each pulse the foes inside are pushed toward its rim and soaked; the owner standing inside is put out. */
	public static final double SOVEREIGN_PUSH = 0.35;
	public static final int SOVEREIGN_SOAK = 40;

	// ------------------------------------------------------------------ the finisher: Undertow

	public static final double FINISHER_FACTOR = 0.8;
	public static final double FINISHER_THROW = 1.2;
}
