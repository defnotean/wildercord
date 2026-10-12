package dev.wildercord.aura.world;

/**
 * The Hollow Matron, the Silk Hollow's guardian: a great pale spider that hangs head-down on her thread under the nest in the
 * miners' hall until someone comes far enough in, then drops, turns over as she lands, and rears up at them with her fangs spread.
 * Quick and skittish, light in the hand like the others; a step below a Master.
 */
public final class HollowMatronRules {
	private HollowMatronRules() {}

	/** Hanging head-down on her thread under the nest, legs drawn in. */
	public static final int DORMANT = 0;
	/** Dropping down her thread, turning over as she lands, and rearing up at whoever woke her; scripted, and untouchable. */
	public static final int DROP = 1;
	/** On her feet, tracking. */
	public static final int WATCH = 2;
	/** Climbing back up her thread once the hall has gone quiet: the drop, played backwards. */
	public static final int CLIMB = 3;
	/** Reared back with her fangs spread, then a lunge and a bite at whoever is in front of her. */
	public static final int BITE = 4;
	/** Her forelegs raised and drawn to one side, then swept across in front of her through whoever is there. */
	public static final int SWEEP = 5;
	/** Reared up, her abdomen curled forward under her, then a glob of web spat at her prey that slows whoever it lands on. */
	public static final int SPIT = 6;
	/** Crouched low on bent legs, then a leap at her prey, landing on whoever is under her. */
	public static final int POUNCE = 7;
	/** A quick skitter to one side out from under a blow; or, hurt too fast, back away across the hall. */
	public static final int SKITTER = 8;
	/** Spent after an attack, low on splayed legs: the opening. */
	public static final int RECOVER = 9;

	public static final double HEALTH = 340, ARMOUR = 6, SPEED = 0.32;

	/** Anyone in the hall this close to the spot under her, horizontally, wakes her: a few strides in from the drift mouth. */
	public static final double WAKE_RADIUS = 13;
	/** How far above or below the hall floor a player still counts as in reach of her. */
	public static final double WAKE_HEIGHT = 6;
	/** How long the drop takes, from hanging to rearing up on the floor. */
	public static final int DROP_TICKS = 44;
	/** How the drop runs: down the thread until {@link #FALL_END}, turning over from {@link #FLIP_FROM} to {@link #FLIP_TO}, and
	 *  rearing up after; the hiss comes at {@link #HISS_AT} ticks. */
	public static final float FALL_END = .5F, FLIP_FROM = .36F, FLIP_TO = .62F;
	public static final int HISS_AT = 30;

	/** The hall she keeps. She is put back if she strays more than this outside. */
	public static final double LEASH = 2;
	/** The boss bar shows to anyone within this of the hall, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 3, BAR_DROP = 10;
	/** Ticks between the kills she makes keeping her hall. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight she walks back under her thread after this long, and climbs it at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether she was left alone for {@code away} ticks, long enough to have climbed back up unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}

	/** How close she closes before she stops to face her prey, and how fast she turns, in degrees a tick: spiders wheel quickly. */
	public static final double CLOSE = 3.4;
	public static final float TURN = 16;

	// ---- the fight: quick tells and quick feet, a web that slows rather than a heavy blow, and a pounce that leaves her open.
	/** What each blow does before defences. */
	public static final double BITE_DAMAGE = 16, SWEEP_DAMAGE = 14, SPIT_DAMAGE = 6, POUNCE_DAMAGE = 20;
	/** The share of what armour, Protection and Resistance would stop that her blows go through anyway. */
	public static final double PIERCE = 0.35;
	/** The bite: its tell, how far the lunge carries her over {@link #LUNGE_TICKS}, and the rest after. */
	public static final int BITE_TELL = 7, LUNGE_TICKS = 3, BITE_RECOVER = 12;
	public static final double LUNGE = 2.2;
	/** The sweep: its tell, how long the forelegs take to cross, the rest after and the least time between two; its reach. */
	public static final int SWEEP_TELL = 9, SWEEP_TICKS = 4, SWEEP_RECOVER = 14, SWEEP_REST = 50;
	public static final double SWEEP_REACH = 5.0;
	/** The spit: its tell, the rest after, the least time between two; how fast the glob flies, how close it must pass, how long
	 *  it slows whoever it lands on, and how far it goes before it falls apart. */
	public static final int SPIT_TELL = 12, SPIT_RECOVER = 10, SPIT_REST = 90, WEB_SLOW_TICKS = 70;
	public static final double GLOB_SPEED = 1.2, GLOB_RADIUS = 1.3, GLOB_RANGE = 22;
	/** The pounce: its tell (the crouch), how long the leap lasts, the rest after, the least time between two; how far it can carry
	 *  her and how far round where she lands it hits. */
	public static final int POUNCE_TELL = 11, POUNCE_TICKS = 10, POUNCE_RECOVER = 24, POUNCE_REST = 110;
	public static final double POUNCE_REACH = 11, POUNCE_RADIUS = 2.8, POUNCE_HEIGHT = 2.6;
	/** The skitter aside: how long it lasts, how fast she goes, and the least time between two. */
	public static final int SKITTER_TICKS = 5, SKITTER_REST = 36;
	public static final double SKITTER_SPEED = 0.7;
	/** The back-off: how much hurt, fading each tick by {@link #STING_FADE}, sends her off, how fast she goes, and the least time between two. */
	public static final double STING_AFTER = 36, STING_FADE = 0.97, BACK_SPEED = 0.8;
	public static final int BACK_TICKS = 8, BACK_REST = 200;
	/** Ticks between her blows, fewer once she is wounded. */
	public static final int GAP = 9, GAP_WOUNDED = 5;

	/** The bar's name for what she is doing. */
	public static String key(int move) {
		return switch (move) {
			case BITE -> "bite";
			case SWEEP -> "sweep";
			case SPIT -> "spit";
			case POUNCE -> "pounce";
			case SKITTER -> "skitter";
			case RECOVER -> "recover";
			default -> "watch";
		};
	}

	/**
	 * Her choice of blow at {@code distance} from her prey; -1 to keep closing. {@code roll} is 0-1. Up close she bites, or raises her
	 * forelegs and sweeps; a little further she pounces; anyone keeping well away gets web spat at them.
	 */
	public static int choose(double distance, double roll, boolean maySweep, boolean mayPounce, boolean maySpit) {
		if (distance <= CLOSE) return maySweep && roll < 0.35 ? SWEEP : BITE;
		if (distance <= SWEEP_REACH - 0.5 && maySweep && roll < 0.5) return SWEEP;
		if (distance >= 5 && distance <= POUNCE_REACH && mayPounce && roll < 0.6) return POUNCE;
		if (distance >= 6 && distance <= GLOB_RANGE - 4 && maySpit) return SPIT;
		if (distance >= 5 && distance <= POUNCE_REACH && mayPounce) return POUNCE;
		return -1;
	}
	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above her feet, in her locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case BITE -> forward > -0.5 && forward <= 3.6 && Math.abs(side) <= 1.6 && height > -1.5 && height < 3.0;
			case SWEEP -> {
				double r = forward * forward + side * side;
				yield forward > -1.0 && r <= SWEEP_REACH * SWEEP_REACH && height > -1.5 && height < 3.0;
			}
			case POUNCE -> forward * forward + side * side <= POUNCE_RADIUS * POUNCE_RADIUS && height > -1.5 && height < 2.5;
			default -> false;
		};
	}
	/**
	 * What her carapace leaves of a blow. Her fangs and forelegs turn some of a blow from the front; her soft abdomen at the back
	 * takes it all and a little more; spent after an attack she is open.
	 */
	public static double armour(int move, boolean frontal, boolean rear) {
		if (move == RECOVER) return rear ? 1.5 : 1.3;
		if (rear) return 1.15;
		return frontal ? 0.7 : 1.0;
	}
	/** Whether a blow arriving from {@code angle} degrees off her facing lands on her front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 55;
	}
	/** Whether a blow arriving from {@code angle} degrees off her facing lands on her abdomen, behind. */
	public static boolean rear(double angle) {
		return Math.abs(wrap(angle)) >= 130;
	}
	private static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
	/** Whether she skitters aside from a blow: only while she watches, not too often, and only some of the time. */
	public static boolean dodges(int move, long sinceDodge, double roll) {
		return move == WATCH && sinceDodge >= SKITTER_REST && roll < 0.35;
	}
	/** Whether the hurt she has taken lately, {@code stung}, sends her backing off, {@code sinceBack} ticks after she last did. */
	public static boolean backs(int move, double stung, long sinceBack) {
		return (move == WATCH || move == RECOVER) && stung >= STING_AFTER && sinceBack >= BACK_REST;
	}
	/** Whether she is wounded: at half health or below, she is quicker. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a hall from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from the floor under her thread is close enough to wake her (and in the hall, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: she cannot be hurt, and shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == DROP || move == CLIMB;
	}

	/** How far through the drop she is, 0 hanging to 1 reared up on the floor, {@code elapsed} ticks in (climbing runs it back). */
	public static float drop(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / DROP_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case DROP -> t;
			case CLIMB -> 1 - t;
			default -> 1;
		};
	}
	/** How far down her thread she is, 0 hanging to 1 on the floor, {@code d} through the drop: she lets go and falls, braking at the end. */
	public static double descent(float d) {
		double k = Math.max(0, Math.min(1, d / FALL_END));
		return k < 0.8 ? (k / 0.8) * (k / 0.8) * 0.9 : 0.9 + 0.1 * Math.sin((k - 0.8) / 0.2 * Math.PI / 2);
	}

	/** Where a pounce of {@code ticks} ticks is, {@code tick} in: the share of the way along, and how high above the line. */
	public static double arc(int tick, int ticks) {
		double t = Math.max(0, Math.min(1, tick / (double) ticks));
		return 4 * t * (1 - t) * POUNCE_HEIGHT;
	}

	/** Health she knits back this tick, {@code sinceCut} ticks after she was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
