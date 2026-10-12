package dev.wildercord.aura.world;

/**
 * The Drowned Bell-Warden, the sunken chapel's guardian: its states, its wake-up and the hall it keeps. It kneels in vigil before
 * the fallen bell-frame with its own cracked bell over its head, until someone walks far enough up the nave; then it rises, tolls
 * and the fight is on. Starting tuning values; a step below a Master, like the serpent.
 */
public final class BellWardenRules {
	private BellWardenRules() {}

	/** Kneeling in vigil before the bell-frame. */
	public static final int DORMANT = 0;
	/** Getting up off its knees, tolling as it stands; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** On its feet, tracking. */
	public static final int WATCH = 2;
	/** Kneeling again once the chapel has gone quiet: the rise, played backwards. */
	public static final int KNEEL = 3;

	public static final double HEALTH = 420, ARMOUR = 12, SPEED = 0.24;

	/** Anyone in the hall this close to its vigil, horizontally, wakes it: about halfway up the nave. */
	public static final double WAKE_RADIUS = 18;
	/** How far above or below its knees a player still counts as on the chapel floor. */
	public static final double WAKE_HEIGHT = 5;
	public static final int RISE_TICKS = 46;
	/** When in its rise the bell tolls: as it straightens. */
	public static final int TOLL_AT = RISE_TICKS / 2;

	/** The hall it keeps is the chapel, nave and apse. It is put back if it strays more than this outside. */
	public static final double LEASH = 2;
	/** The boss bar shows to anyone within this of the hall, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 10;
	/** Ticks between the kills it makes keeping its hall. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight it walks back to its vigil after this long, and kneels at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether it was left alone for {@code away} ticks, long enough to have knelt again unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}

	/** How close it closes before it stops to face its prey, and its least rest between blows. */
	public static final double CLOSE = 3.5;
	public static final int GAP = 10;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return "watch";
	}

	/** Whether a point lies inside a hall from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from its vigil is close enough to wake it (and inside the hall, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == KNEEL;
	}

	/** How far up it is, 0 kneeling to 1 standing, {@code elapsed} ticks into a rise (or a kneel, which runs it backwards). */
	public static float rise(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case KNEEL -> 1 - t;
			default -> 1;
		};
	}

	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
