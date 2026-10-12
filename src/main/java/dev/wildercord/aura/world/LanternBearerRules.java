package dev.wildercord.aura.world;

/**
 * The Lantern-Bearer, the Barrow Field's guardian: its states, its wake-up and the field it keeps. A gaunt hooded spirit that lies in
 * the open grave under the gibbet by day and is not there at all; after dark, when someone comes in among the graves, it rises up out
 * of the grave, reaches its crook up to the soul lantern hanging over it and takes it down, its eyes catch light from it, the candles
 * on the graves kindle, and it wails. It fights with a gravedigger's spade in one hand and the lantern on its crook in the other. Dawn
 * sends it back down into the grave and the lantern back up on its chain. A step below a Master, like the others.
 */
public final class LanternBearerRules {
	private LanternBearerRules() {}

	/** Down in the grave, not there at all: unseen, untouchable. */
	public static final int DORMANT = 0;
	/** Rising out of the grave and taking down its lantern; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** Up and tracking. */
	public static final int WATCH = 2;
	/** Sinking back down into the grave, the rise played backwards. */
	public static final int BED = 3;
	/** The spade swung round in a flat reap in front of it, wound back {@link #REAP_TELL} ticks. */
	public static final int REAP = 4;
	/** The crook thrust out at someone a way off; caught on it, they are dragged in close. */
	public static final int HOOK = 5;
	/** The lantern raised and swung, and soul wisps loosed from it to drift after whoever they were sent at. */
	public static final int WISPS = 6;
	/** The spade raised over its head in both hands and driven down into the ground, throwing back everyone close round it. */
	public static final int SLAM = 7;
	/** Gone to smoke for a moment, drifting away from a blow, insubstantial while it lasts. */
	public static final int FADE = 8;
	/** Spent after an attack, leaning on its spade, the lantern hanging low: the opening. */
	public static final int RECOVER = 9;
	/**
	 * Lights Out, its signature once wounded: it bows its hood over its lantern and breathes it out, the candles die, everyone on the
	 * field is blinded a moment and left in the dark, and it is gone. Then, {@link #LIGHTS_ROUNDS} times, it stands up out of the dark
	 * behind one of them, its lantern glimmering for {@link #LIGHTS_GLIMMER} ticks, and reaps; strike it while it glimmers and the
	 * lantern flares back up and it is left spent.
	 */
	public static final int LIGHTS_OUT = 10;

	/** How hard it is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.JOURNEY;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.3);

	/** Anyone in the field this close to the grave, horizontally, after dark, wakes it: in among the graves. */
	public static final double WAKE_RADIUS = 11;
	/** How far above or below the grave a player still counts as on the field. */
	public static final double WAKE_HEIGHT = 5;
	public static final int RISE_TICKS = 60;
	/** When in its rise its crook takes the lantern off the gibbet: once it is up and reaching. */
	public static final int HOOK_AT = RISE_TICKS * 62 / 100;
	/** When in its rise it wails: once the lantern is down and its eyes have caught. */
	public static final int ROAR_AT = RISE_TICKS * 4 / 5;
	/** Once it has been light for this long while it is up, it goes back down into its grave. */
	public static final int DAWN_TICKS = 60;

	/** The field it keeps. It is put back if it strays more than this outside. */
	public static final double LEASH = 2;
	/** The boss bar shows to anyone within this of the field, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 10;
	/** Ticks between the kills it makes keeping its field. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight it drifts back to its grave after this long, and goes down into it at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether it was left alone for {@code away} ticks, long enough to have gone back down unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}

	/** How close it closes before it stops to face its prey. */
	public static final double CLOSE = 3.0;

	// ---- the fight: a long spade at arm's length, a crook to drag back anyone who keeps away, and wisps for anyone who runs.
	/** What each blow does before defences. */
	public static final double REAP_DAMAGE = TIER.hit(16), HOOK_DAMAGE = TIER.hit(6), WISP_DAMAGE = TIER.hit(9), SLAM_DAMAGE = TIER.hit(14);
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway. */
	public static final double PIERCE = 0.35;
	/** The reap: its tell and the rest after. */
	public static final int REAP_TELL = 8, REAP_RECOVER = 12;
	/** The hook: its tell, the rest after, the least time between two; how far it reaches, how wide, and how close it drags. */
	public static final int HOOK_TELL = 10, HOOK_RECOVER = 10, HOOK_REST = 80, HOOK_DRAG = 8;
	public static final double HOOK_REACH = 10, HOOK_WIDTH = 1.1, HOOK_PULL_TO = 2.2;
	/** The wisps: its tell, the rest after, the least time between two; how many, how long they last, how fast they go and turn,
	 *  how close they burst, and how long they slow. */
	public static final int WISP_TELL = 16, WISP_RECOVER = 18, WISP_REST = 160, WISP_COUNT = 3, WISP_LIFE = 100, WISP_SLOW = 40;
	public static final double WISP_SPEED = 0.16, WISP_TURN = 0.08, WISP_RADIUS = 1.2;
	/** The slam: its tell, the rest after, the least time between two; how far round it strikes and how hard it throws. */
	public static final int SLAM_TELL = 9, SLAM_RECOVER = 16, SLAM_REST = 60;
	public static final double SLAM_RADIUS = 3.6, SLAM_SHOVE = 1.6;

	/** The fade: how long it is smoke, how fast it drifts, and the least time between two. */
	public static final int FADE_TICKS = 6, FADE_REST = 50;
	public static final double FADE_SPEED = 0.7;
	/** The drift away: how much hurt, fading each tick by {@link #STING_FADE}, sends it off, how fast it goes, and the least time between two. */
	public static final double STING_AFTER = 40, STING_FADE = 0.97, LEAP_SPEED = 0.9;
	public static final int LEAP_TICKS = 8, LEAP_REST = 220;

	/**
	 * Lights Out: the snuffing (its tell), the ticks it takes to fade out once the lantern is out, then each round: unseen, glimmering
	 * behind its prey, and the reap; how many rounds; the rest after (and after its glimmer is struck); the least time between two; how long the snuffing blinds; what the
	 * reap does; and how far behind its prey it stands up.
	 */
	public static final int SNUFF_TELL = 16, LIGHTS_VANISH = 4, LIGHTS_GONE = 20, LIGHTS_GLIMMER = 16, LIGHTS_SWING = 6, LIGHTS_ROUNDS = 3,
		LIGHTS_RECOVER = 30, LIGHTS_BROKEN = 50, LIGHTS_REST = 500, LIGHTS_BLIND = 30;
	public static final double LIGHTS_DAMAGE = TIER.hit(14), LIGHTS_BEHIND = 2.2;
	public static final int LIGHTS_ROUND = LIGHTS_GONE + LIGHTS_GLIMMER + LIGHTS_SWING, LIGHTS_TICKS = SNUFF_TELL + LIGHTS_ROUNDS * LIGHTS_ROUND;
	/** The stages of Lights Out. */
	public static final int SNUFFING = 0, GONE = 1, GLIMMER = 2, SWING = 3, OVER = 4;
	/** Whether, wounded and rested, it puts its lantern out. */
	public static boolean lightsOut(boolean wounded, long sinceLights) {
		return wounded && sinceLights >= LIGHTS_REST;
	}
	/** Which stage of Lights Out it is in, {@code elapsed} ticks in. */
	public static int lightsStage(float elapsed) {
		if (elapsed < SNUFF_TELL) return SNUFFING;
		if (elapsed >= LIGHTS_TICKS) return OVER;
		float in = (elapsed - SNUFF_TELL) % LIGHTS_ROUND;
		return in < LIGHTS_GONE ? GONE : in < LIGHTS_GONE + LIGHTS_GLIMMER ? GLIMMER : SWING;
	}
	/** Which round of Lights Out it is in, {@code elapsed} ticks in: -1 while it snuffs the lantern. */
	public static int lightsRound(float elapsed) {
		if (elapsed < SNUFF_TELL) return -1;
		return Math.min(LIGHTS_ROUNDS - 1, (int) ((elapsed - SNUFF_TELL) / LIGHTS_ROUND));
	}
	/** Ticks since it began glimmering this round (negative while it is still unseen). */
	public static float lightsGlimmer(float elapsed) {
		return elapsed - SNUFF_TELL - Math.max(0, lightsRound(elapsed)) * LIGHTS_ROUND - LIGHTS_GONE;
	}
	/** Whether it can be seen, and struck, {@code elapsed} ticks into Lights Out: not once the lantern is out, until it glimmers. */
	public static boolean lightsSeen(float elapsed) {
		int stage = lightsStage(elapsed);
		return stage == SNUFFING || stage == GLIMMER || stage == SWING || stage == OVER || elapsed < SNUFF_TELL + LIGHTS_VANISH;
	}
	/** Whether its reap lands this tick, {@code elapsed} ticks into Lights Out. */
	public static boolean lightsStrikes(long elapsed) {
		return elapsed >= SNUFF_TELL && elapsed < LIGHTS_TICKS && (elapsed - SNUFF_TELL) % LIGHTS_ROUND == LIGHTS_GONE + LIGHTS_GLIMMER;
	}
	/** Whether it moves round behind its prey this tick: a few ticks before it glimmers, while it is still unseen. */
	public static boolean lightsAppears(long elapsed) {
		return elapsed >= SNUFF_TELL && elapsed < LIGHTS_TICKS && (elapsed - SNUFF_TELL) % LIGHTS_ROUND == LIGHTS_GONE - LIGHTS_VANISH;
	}
	/**
	 * How solid it is through Lights Out, as a part of whole: whole while it snuffs the lantern, thinning away as it goes, nothing out in
	 * the dark, standing up thin out of it as it glimmers and whole for the reap, thinning away again after (but not after the last).
	 */
	public static float lightsSolid(float elapsed) {
		if (elapsed < SNUFF_TELL) return 1;
		int stage = lightsStage(elapsed);
		if (stage == OVER) return 1;
		float g = lightsGlimmer(elapsed);
		return switch (stage) {
			case GONE -> lightsRound(elapsed) == 0 ? Math.max(0, 1 - (elapsed - SNUFF_TELL) / LIGHTS_VANISH) : 0;
			case GLIMMER -> Math.min(1, g / LIGHTS_GLIMMER * 0.8F + 0.15F);
			default -> lightsRound(elapsed) == LIGHTS_ROUNDS - 1 ? 1 : Math.max(0, 1 - (g - LIGHTS_GLIMMER - 1) / (LIGHTS_SWING - 1));
		};
	}

	/** Ticks between its blows, fewer once it is wounded. */
	public static final int GAP = 10, GAP_WOUNDED = 6;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case REAP -> "reap";
			case HOOK -> "hook";
			case WISPS -> "wisps";
			case SLAM -> "slam";
			case FADE -> "fade";
			case RECOVER -> "recover";
			case LIGHTS_OUT -> "lights";
			default -> "watch";
		};
	}

	/**
	 * Its choice of blow at {@code distance} from its prey, who stands {@code angle} degrees off its facing; -1 to keep closing.
	 * {@code roll} is 0-1. Anyone crowding it or round its flank gets the spade driven down among them; in front, within the spade's
	 * reach, it reaps; a way off it hooks them in, or looses wisps after them.
	 */
	public static int choose(double distance, double angle, double roll, boolean mayHook, boolean mayWisps, boolean maySlam) {
		double off = Math.abs(wrap(angle));
		if (maySlam && ((off >= 80 && distance <= CLOSE + 1) || distance <= 2.2)) return SLAM;
		if (distance <= CLOSE + 0.6) return REAP;
		if (mayHook && distance >= 5 && distance <= HOOK_REACH && roll < 0.5) return HOOK;
		if (mayWisps && distance >= 4) return WISPS;
		if (mayHook && distance >= 5 && distance <= HOOK_REACH) return HOOK;
		return -1;
	}
	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case REAP -> forward * forward + side * side <= 4.6 * 4.6 && forward >= Math.abs(side) * 0.364 && height > -1.5 && height < 3.5;
			case HOOK -> forward >= 0.5 && forward <= HOOK_REACH && Math.abs(side) <= HOOK_WIDTH && height > -1.5 && height < 3.5;
			case SLAM -> forward * forward + side * side <= SLAM_RADIUS * SLAM_RADIUS && height > -1.5 && height < 2.5;
			default -> false;
		};
	}
	/** How fast, in blocks a tick, the crook drags someone in from {@code distance} away: hard at first, easing as they near. */
	public static double pull(double distance) {
		return Math.max(0, Math.min(1.6, (distance - HOOK_PULL_TO) * 0.28));
	}
	/** Whether a wisp {@code distance} from its prey bursts on them. */
	public static boolean bursts(double distance) {
		return distance <= WISP_RADIUS;
	}
	/**
	 * What its rags leave of a blow. Its spade and crook turn some of a blow from the front; from behind it is only rags; spent it is
	 * open; gone to smoke nothing touches it.
	 */
	public static double armour(int move, boolean frontal) {
		if (move == FADE) return 0;
		if (move == RECOVER) return 1.3;
		return frontal ? 0.7 : 1.0;
	}
	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 50;
	}
	private static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
	/** Whether it fades from a blow: only while it watches, not too often, and only some of the time. */
	public static boolean dodges(int move, long sinceDodge, double roll) {
		return move == WATCH && sinceDodge >= FADE_REST && roll < 0.3;
	}
	/** Whether the hurt it has taken lately, {@code stung}, sends it drifting away, {@code sinceLeap} ticks after it last did. */
	public static boolean leaps(int move, double stung, long sinceLeap) {
		return (move == WATCH || move == RECOVER) && stung >= STING_AFTER && sinceLeap >= LEAP_REST;
	}
	/** Whether it is wounded: at half health or below, it is quicker. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a field from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from its grave, {@code dark} out, is close enough to wake it (and inside the field, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz, boolean dark) {
		return dark && dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}
	/** Whether, up and it having been light outside for {@code light} ticks, it goes back down into its grave. */
	public static boolean dawn(long light) {
		return light >= DAWN_TICKS;
	}

	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == BED;
	}

	/** How far up it is, 0 down in the grave to 1 standing with its lantern, {@code elapsed} ticks into a rise (or the sinking back, which runs it backwards). */
	public static float rise(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case BED -> 1 - t;
			default -> 1;
		};
	}
	/** Whether, {@code rise} of the way up, it holds the lantern rather than the gibbet. */
	public static boolean holdsLantern(float rise) {
		return rise >= (float) HOOK_AT / RISE_TICKS;
	}

	/** How far round the Barrow Lantern a living Bearer keeps it from being taken. */
	public static final double LANTERN_GUARD = 48;
	/**
	 * The Barrow Steed the Bearer's Lantern calls up: how fast it runs and how high it leaps (a little past the Pale Steed), how long
	 * the lantern must rest after, how often it checks it is still ridden, how many empty checks before it fades, and how long it
	 * would linger unwatched (renewed while it is ridden, so a restart can never leave it behind).
	 */
	public static final double STEED_SPEED = 0.3375, STEED_JUMP = 0.9;
	public static final int STEED_REST = 200, STEED_CHECK = 5, STEED_EMPTY = 3, STEED_LINGER = 1200;

	/** Whether the steed stays, ridden or not for {@code empty} checks in a row: it fades the moment its rider has been off it a while. */
	public static boolean steedStays(boolean riderAlive, int empty) {
		return riderAlive && empty < STEED_EMPTY;
	}

	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
