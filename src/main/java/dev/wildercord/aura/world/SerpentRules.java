package dev.wildercord.aura.world;

/**
 * The Rootcoil Serpent, the serpent grove's guardian: its states, its wake-up and its leash. It sleeps coiled round the grove's
 * tree until someone sets foot near it, uncoils down to the island, and only then is the fight on. Starting tuning values; it is
 * meant to sit a step below a Master, not beside one.
 */
public final class SerpentRules {
	private SerpentRules() {}

	/** Coiled round the trunk, watching the herb. */
	public static final int DORMANT = 0;
	/** Sliding down off the trunk to the island; scripted, and untouchable while it lasts. */
	public static final int UNCOIL = 1;
	/** Awake on the ground, reared and tracking. */
	public static final int WATCH = 2;
	/** Climbing back round the trunk after the grove has gone quiet: the uncoil, played backwards. */
	public static final int RECOIL = 3;

	public static final double HEALTH = 360, ARMOUR = 10, SPEED = 0.3;

	/** Anyone this close to the trunk, horizontally, wakes it: about five blocks from its coils. */
	public static final double WAKE_RADIUS = 7.5;
	/** How far above or below the trunk's foot a player still counts as on the island. */
	public static final double WAKE_HEIGHT = 6;
	public static final int UNCOIL_TICKS = 50;

	/** Where it may fight: the cavern round the tree. Beyond this it is put back on the island. */
	public static final double LEASH = 22.5;
	/** Who it hunts, measured from the trunk: anyone in the cavern. */
	public static final double ARENA = 20;
	/** The boss bar shows inside this, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 24, BAR_DROP = 28;
	/** With nobody to fight it heads back to its landing after this long, and coils up again at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Landing spots tried, outward from the trunk toward the herb. */
	public static final double LAND_NEAR = 4.0, LAND_FAR = 6.5;

	/** How close it closes before it stops to face its prey. */
	public static final double CLOSE = 4.5;

	/** Whether a player at this offset from the trunk's foot is close enough to wake it. */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** Whether it has strayed off its ground and must be put back. */
	public static boolean leashed(double dx, double dy, double dz) {
		return dx * dx + dz * dz > LEASH * LEASH || dy < -6 || dy > 10;
	}

	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == UNCOIL || move == RECOIL;
	}

	/** Uncoil progress, 0 coiled to 1 landed, {@code elapsed} ticks into an uncoil (or a recoil, which runs it backwards). */
	public static float uncoil(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / UNCOIL_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case UNCOIL -> t;
			case RECOIL -> 1 - t;
			default -> 1;
		};
	}

	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
