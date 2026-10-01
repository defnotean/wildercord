package dev.wildercord.aura;

/**
 * Sword strings' numbers, the pure part: how long each swing of a string may wait on the one before, how long a counter or a
 * step cut stays open, what the server allows for the network when it checks what it saw, and the placeholder arts' prices
 * (see {@link PlaceholderArts}). The grammar is {@link SwordString}, the reading {@link StringReader}; the server runtime is
 * {@link SwordStrings}.
 *
 * <p>The window is counted from the moment the blade is ready again, not from the swing before, so a string is paced to its
 * weapon: a sword (ready in 11 ticks) keeps a string going a swing every second or so, an axe (20) every second and a half, a
 * mace (30) every two seconds, and quick half-strength swings always keep up. It's deliberate, not frantic: a full swing never
 * has to be rushed, but a pause breaks the string. The server owner sets the window ({@code aura.string_window_seconds}).</p>
 */
public final class StringRules {
	private StringRules() {}

	// ------------------------------------------------------------------ reading a string

	/** How long after the blade is ready again the next swing of a string may come, by default (ticks): half a second. */
	public static final int WINDOW = 10;
	/** The window's range in the config, in seconds. */
	public static final double MIN_WINDOW_SECONDS = 0.1;
	public static final double MAX_WINDOW_SECONDS = 2.0;
	/** A counter: the first swing within this long (ticks) of a perfect guard (a staggered foe stands for 40). */
	public static final int COUNTER_TICKS = 16;
	/** A step cut: the first swing within this long (ticks) of an Aura Step (the step itself takes 3). */
	public static final int STEP_CUT_TICKS = 14;
	/**
	 * A swing that would have finished a string, coming no later than this (ticks) after the string ran out, is a fumble: the
	 * player meant it, and should know they were late. Any later and it's simply a new string.
	 */
	public static final int LATE_GRACE = 10;
	/** The most swings a reader remembers: the longest string there can be. */
	public static final int CHAIN = SwordString.MAX_LENGTH;
	/**
	 * A swing at nothing (no creature under the crosshair) counts toward a string only this long (ticks) after a blow given or
	 * taken: a whiff in a fight counts, waving a blade about in peace doesn't.
	 */
	public static final int ENGAGED_TICKS = AuraRules.COMBAT_TICKS;
	/** The most ticks a blade's recovery adds to a window (a very slow weapon still keeps a string within reach). */
	public static final int MAX_RECOVER = 60;

	/**
	 * How long after a swing the blade is ready for a full one again (ticks), for a weapon whose full charge takes
	 * {@code delayTicks} (vanilla's attack strength delay: 20 over the attack speed): the attack strength reaches
	 * {@link AuraRules#FULL_SWING} when {@code (ticks + 0.5) / delay} does.
	 */
	public static int recover(double delayTicks) {
		if (!(delayTicks > 0)) {
			return 0;
		}
		return Math.max(0, Math.min(MAX_RECOVER, (int) Math.ceil(AuraRules.FULL_SWING * delayTicks - 0.5 - 1.0E-9)));
	}

	/** The window (ticks) the next swing has after one struck with a blade that recovers in {@code recover} ticks. */
	public static int window(int baseWindow, int recover) {
		return Math.max(1, baseWindow) + Math.max(0, recover);
	}

	/** The window in ticks for a window of {@code seconds} (the config's), held to its range. */
	public static int windowTicks(double seconds) {
		double s = Math.max(MIN_WINDOW_SECONDS, Math.min(MAX_WINDOW_SECONDS, seconds));
		return Math.max(1, (int) Math.round(s * 20));
	}

	/** The longest a string of {@code length} swings can take, first swing to last (ticks). */
	public static long span(int length, int baseWindow, int recover) {
		return Math.max(0, length - 1) * (long) window(baseWindow, recover);
	}

	// ------------------------------------------------------------------ the server's check

	/**
	 * When the server checks a performed string against the swings it saw itself, it allows this much (ticks) for the network:
	 * packets arrive in order, but bunched or spread out by a slow connection.
	 */
	public static final int SEEN_SLACK = 20;
	/** The string's last swing must have reached the server no longer than this (ticks) before the string did (sent together). */
	public static final int LAST_SWING_SLACK = 10;
	/** Performed strings a player may send: a burst of this many, then one every {@link #REQUEST_TICKS} ticks. No hand comes near it. */
	public static final int REQUEST_BURST = 4;
	public static final int REQUEST_TICKS = 5;

	// ------------------------------------------------------------------ the placeholder arts

	/*
	 * Until each method's own arts arrive, every method plays the same five placeholder arts (PlaceholderArts): a burst of
	 * projected aura off the blade, in the method's element, against the spell defences for a player, like the slash. Their
	 * prices sit under the slash's (12 aura, every 2 seconds, the weapon's damage x 1.2 down a 14-block line) for what they
	 * reach, and the Final Art's beside Dominion's.
	 */

	/** The First Art (Glow, "swing swing low"): a short arc in front. */
	public static final double FIRST_COST = 6.0;
	public static final int FIRST_COOLDOWN = 60;
	public static final double FIRST_FACTOR = 0.6;
	/** The Second Art (Flow, "leap low"): an arc that lifts what it cuts. */
	public static final double SECOND_COST = 8.0;
	public static final int SECOND_COOLDOWN = 80;
	public static final double SECOND_FACTOR = 0.8;
	public static final double SECOND_LIFT = 0.55;
	/** The Third Art (Edge, "counter"): a cut that staggers its foe again. */
	public static final double THIRD_COST = 8.0;
	public static final int THIRD_COOLDOWN = 80;
	public static final double THIRD_FACTOR = 1.0;
	/** The Fourth Art (Form, "step"): a line cut ahead, through everything in it. */
	public static final double FOURTH_COST = 10.0;
	public static final int FOURTH_COOLDOWN = 100;
	public static final double FOURTH_FACTOR = 1.0;
	public static final double FOURTH_LENGTH = 5.0;
	/** The Final Art (Sovereign, "full full full low"): a ring round you that throws foes back. */
	public static final double FINAL_COST = 40.0;
	public static final int FINAL_COOLDOWN = 600;
	public static final double FINAL_FACTOR = 2.0;
	public static final double FINAL_RADIUS = 4.0;
	/**
	 * The Final Art waits on a full aura pool (until momentum and awakening exist to gate it). Full means nine tenths or more:
	 * the string's own coated swings spend a little on the way.
	 */
	public static final double FINAL_POOL = 0.9;

	/** How far the placeholder arcs reach in front (blocks), how wide they open (degrees), and how many foes one cuts at most. */
	public static final double ARC_REACH = 3.5;
	public static final double ARC_DEGREES = 130.0;
	public static final int ARC_TARGETS = 4;
	public static final int FINAL_TARGETS = 8;

	/** Whether {@code aura} out of {@code capacity} is a full pool, for the Final Art. */
	public static boolean poolFull(double aura, double capacity) {
		return capacity > 0 && aura >= FINAL_POOL * capacity - 1.0E-6;
	}

	/** Whether a point {@code dx, dz} away (flat) lies inside an arc of {@code degrees} facing {@code fx, fz} and {@code reach} long. */
	public static boolean inArc(double dx, double dz, double fx, double fz, double reach, double degrees) {
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d > reach) {
			return false;
		}
		if (d < 0.5) {
			// Standing right on top of you: always in it.
			return true;
		}
		double f = Math.sqrt(fx * fx + fz * fz);
		if (f < 1.0E-6) {
			return true;
		}
		double cos = (dx * fx + dz * fz) / (d * f);
		return cos >= Math.cos(Math.toRadians(degrees / 2));
	}
}
