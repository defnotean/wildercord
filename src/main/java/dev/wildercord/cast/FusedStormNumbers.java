package dev.wildercord.cast;

/**
 * The numbers of the storm and wind fused effects ({@link FusedStorm}), at power 1 and duration 1 as the
 * rune descriptions give them, with the little rules behind them kept pure so they can be tested: what a
 * Stormweave web and a Downdraft slam deal, when Heartstopper's heart skips, and how hard to throw a
 * creature so it lands a given number of blocks away.
 */
final class FusedStormNumbers {
	private FusedStormNumbers() {}

	// ---- Riftbolt
	static final double RIFTBOLT_DAMAGE = 7;
	/** How far the rift can carry its target, in blocks. */
	static final double RIFT_REACH = 5;
	static final double RIFT_DARKNESS_SECONDS = 3;

	// ---- Stormweave
	/** Enemies one web can hold. */
	static final int WEAVE_MAX = 4;
	static final double WEAVE_RADIUS = 6;
	/** "A moment later": ticks between the marks and the lightning. */
	static final int WEAVE_DELAY = 15;
	/** What a lone target takes on top: the lightning grounds through it. */
	static final int WEAVE_ANCHOR = 2;

	/** What each enemy caught in a web of {@code caught} takes: 4, and 1 more for every other one caught with it. */
	static double weaveDamage(int caught) {
		if (caught <= 0) {
			return 0;
		}
		return 4 + (Math.min(caught, WEAVE_MAX) - 1);
	}

	// ---- Stormclock
	static final double CLOCK_FIRST_DAMAGE = 4;
	static final double CLOCK_STRIKE_DAMAGE = 3;
	static final double CLOCK_RADIUS = 1.5;
	/** Ticks after the first strike at which the same spot is struck again. */
	static final int[] CLOCK_STRIKES = {40, 80};
	/** The clock's hand takes this many steps between strikes. */
	static final int CLOCK_STEPS = 8;
	/** Clocks one caster may have ticking at once; a new one past this stops the oldest. */
	static final int CLOCKS_MAX = 6;

	// ---- Heartstopper
	static final double HEART_DAMAGE = 5;
	static final double HEART_SECONDS = 6;
	/** Ticks between skipped beats. */
	static final int HEART_EVERY = 40;
	/** How long each skip stuns, in ticks. */
	static final int HEART_STUN = 10;

	/** How many times the heart skips over a window of {@code windowTicks}: once every 2 seconds, while it lasts. */
	static int heartSkips(int windowTicks) {
		return Math.max(0, windowTicks / HEART_EVERY);
	}

	// ---- Thunderhead
	static final double CLOUD_STRIKE_DAMAGE = 2;
	static final double CLOUD_REACH = 4;
	static final double CLOUD_SECONDS = 4;
	/** How far over its target's head a cloud hangs (lower under a ceiling). */
	static final double CLOUD_HEIGHT = 3.0;
	/** Clouds one caster may have gathered at once; a new one past this takes the place of the oldest. */
	static final int CLOUDS_MAX = 4;

	/** Strikes a cloud lasting {@code ticks} makes: one a second, the first a second after it gathers. */
	static int cloudStrikes(int ticks) {
		return Math.max(0, ticks / 20);
	}

	// ---- Downdraft
	static final double DOWNDRAFT_RADIUS = 6;
	static final double SLAM_DAMAGE = 4;
	static final int SLAM_BONUS_MAX = 6;
	/** With nothing airborne, the downburst pins what's under it: Slowness III for a second and this much damage. */
	static final int DOWNDRAFT_PIN_TICKS = 20;
	static final double DOWNDRAFT_PIN_DAMAGE = 2;

	/** A slam's damage: 4, and 1 more for every whole block fallen, up to 6 more. */
	static double slamDamage(double fallen) {
		return SLAM_DAMAGE + Math.min(SLAM_BONUS_MAX, Math.floor(Math.max(0, fallen)));
	}

	// ---- Updraft
	static final double UPDRAFT_RADIUS = 2.5;
	static final double UPDRAFT_DAMAGE = 4;
	/** How fast the updraft throws a creature up (blocks a tick): about 7 blocks high. */
	static final double UPDRAFT_LIFT = 1.2;
	/** "A moment later": ticks from the throw to the smash, near the top of the flight. */
	static final int UPDRAFT_SMASH = 13;

	// ---- Skyglyph
	static final double GLYPH_SECONDS = 10;
	static final double GLYPH_RADIUS = 1.5;
	/** Glyphs one caster may have written at once; a new one past this takes the place of the oldest. */
	static final int GLYPHS_MAX = 2;
	/** Ticks a creature must wait between two launches (by any glyph). */
	static final int GLYPH_REST = 20;
	/** An ally's launch: up (blocks a tick, about 7 blocks high) and forward. */
	static final double GLYPH_LIFT = 1.2;
	static final double GLYPH_FORWARD = 0.9;
	static final double GLYPH_THROW = 4;
	/** How often (ticks) a glyph looks for who's standing on it. */
	static final int GLYPH_CHECK = 2;

	// ---- Recoil
	static final double RECOIL_THROW = 5;
	static final int RECOIL_DELAY = 40;
	static final double RECOIL_DAMAGE = 5;
	/** A creature further than this from where it stood isn't pulled back (it's a snap, not a leash across the world). */
	static final double RECOIL_LEASH = 24;

	// ---- throwing

	/** The hop a throw along the ground gives (a jump's worth, blocks a tick). */
	static final double HOP = 0.42;

	/**
	 * How far along the ground a creature of ordinary weight thrown from standing at {@code speed} (blocks
	 * a tick, level) with a hop of {@code up} ends up, under vanilla's drag: its first tick still drags on
	 * the ground (0.6 × 0.91), then 0.91 a tick in the air, gravity 0.08 and 0.98 on the way up and down,
	 * and it slides to a stop once it lands.
	 */
	static double glide(double speed, double up) {
		double x = 0;
		double y = 0;
		double vx = speed;
		double vy = up;
		boolean grounded = true;
		for (int tick = 0; tick < 400; tick++) {
			double friction = grounded ? 0.6 * 0.91 : 0.91;
			x += vx;
			y += vy;
			if (y <= 0 && tick > 0) {
				y = 0;
				vy = 0;
				grounded = true;
			} else {
				grounded = false;
				vy = (vy - 0.08) * 0.98;
			}
			vx *= friction;
			if (grounded && vx < 1.0E-3) {
				break;
			}
		}
		return x;
	}

	/** The level speed that throws a creature {@code blocks} along the ground with {@link #HOP} (the inverse of {@link #glide}). */
	static double throwSpeed(double blocks) {
		double low = 0;
		double high = 4;
		for (int i = 0; i < 40; i++) {
			double mid = (low + high) / 2;
			if (glide(mid, HOP) < blocks) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return (low + high) / 2;
	}
}
