package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

/**
 * The Lantern-Bearer, the Barrow Field's guardian: its states, its wake-up and the field it keeps. A gaunt hooded spirit that lies in
 * the open grave under the gibbet by day and is not there at all; after dark, when someone comes in among the graves, it rises up out
 * of the grave, reaches its crook up to the soul lantern hanging over it and takes it down, its eyes catch light from it, the candles
 * on the graves kindle, and it wails. It fights with a gravedigger's spade in its right hand and the lantern on its crook in its left.
 * Dawn sends it back down into the grave and the lantern back up on its chain. A step below a Master, like the others.
 * <p>
 * It never does the same thing twice running: each time it picks from what suits where its prey stands (close or far, in front or
 * round its back, down on the field or up out of reach), and what it has done lately counts against doing it again. Half beaten it
 * holds its rite ({@link #RITE}), and fights on roused: quicker, its moves run on in pairs, each with more to it, and it puts its
 * lantern out ({@link #LIGHTS_OUT}) to come for them out of the dark.
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
	/**
	 * The low reap: the spade dragged low behind its right side, scraping the earth, then swept round at ankle height from its right to
	 * its left through everyone in front of it. Jump it. Roused, it sweeps straight back the other way.
	 */
	public static final int REAP = 4;
	/**
	 * The crook thrust out along a line marked in soul fire on the ground; caught on it, they are dragged in close. Step off the line:
	 * missed, the crook bites into the earth and it is stuck tugging it free ({@link #SNAGGED}). Roused, a miss is thrown again at once.
	 */
	public static final int HOOK = 5;
	/**
	 * The lantern whirled overhead, and soul wisps loosed from it one after another; they follow only whom they can see, so put
	 * something between you and them and they gutter out.
	 */
	public static final int WISPS = 6;
	/**
	 * The spade driven into the earth: a grave marked under each of them, a ring of souls closing on it until the earth bursts up
	 * there. Leave the spot. It is stuck with its spade in the ground until the last of them bursts.
	 */
	public static final int GRAVES = 7;
	/** Gone to smoke for a moment, drifting away from a blow, insubstantial while it lasts. */
	public static final int FADE = 8;
	/** Spent after an attack, leaning on its spade, the lantern hanging low: the opening. */
	public static final int RECOVER = 9;
	/**
	 * Lights Out, roused only: it bows its hood over its lantern and breathes it out, the candles die, everyone on the field is blinded a
	 * moment and left in the dark, and it is gone. Then, {@link #LIGHTS_ROUNDS} times, it stands up out of the dark behind one of them,
	 * its lantern glimmering for {@link #LIGHTS_GLIMMER} ticks, and reaps; strike it while it glimmers and the lantern flares back up
	 * and it is left spent.
	 */
	public static final int LIGHTS_OUT = 10;
	/**
	 * The flare: the lantern drawn in under its hood, the flame swelling and the reach of it ringed in soul fire on the ground, then
	 * brought down in a burst all round it. Back off. Roused, a ring of fire rolls on out across the field after it, to jump.
	 */
	public static final int FLARE = 11;
	/**
	 * The shovel: the spade dug in and levered back heaped, then grave earth flung out in a fan round where its prey stands, each clod
	 * landing on a closing ring. Get in close under it, or off the rings. Roused, a second fan follows.
	 */
	public static final int SHOVEL = 12;
	/**
	 * The backsweep: its head turned back over its shoulder, the crook drawn across its body, then swept round burning behind it. Get
	 * round to its flank.
	 */
	public static final int BACKSWEEP = 13;
	/**
	 * The breath: the lantern held up before its hood, souls drawn in, then a long breath of soul fire out across the lantern in a cone,
	 * turning slowly the way its head is cocked. Get behind it, or round the side it turns away from. It doubles over coughing smoke
	 * after. Roused, the breath swings out and right back across.
	 */
	public static final int BREATH = 14;
	/** Its crook bitten into the earth by a missed hook, tugging it free: the opening. */
	public static final int SNAGGED = 15;
	/**
	 * The rite, once, half beaten: it plants its spade and holds its crook up high while souls stream up out of the graves into its
	 * lantern and the candles flare, then brings the crook down, throwing back everyone near and sending a ring of soul fire rolling
	 * out across the whole field (jump it). It fights on roused.
	 */
	public static final int RITE = 16;

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
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway. */
	public static final double PIERCE = 0.35;

	// ---- the low reap
	/** The low reap: its tell (and run straight on into), how many ticks before the blow its aim locks and how fast it turns till then,
	 *  the ticks the sweep takes, how far round either side it sweeps, how far it reaches, how high it reaches (jump over it), roused
	 *  the ticks before it sweeps back, the rest after, and what it does. */
	public static final int REAP_TELL = 14, REAP_TELL_CHAINED = 10, REAP_LOCK = 4, REAP_SWEEP = 4, REAP_BACK = 7, REAP_RECOVER = 12;
	public static final double REAP_TURN = 8, REAP_ARC = 100, REAP_REACH = 4.8, REAP_LOW = 0.6, REAP_DAMAGE = TIER.hit(14);

	// ---- the hook
	/** The hook: its tell (and chained), when its aim locks and how fast it turns till then, how long before the thrust the line is
	 *  marked, the rethrow's tell, how long it drags, the rest after a catch, how long a miss leaves it snagged; how far it reaches, how
	 *  wide, how close it drags, and what it does. */
	public static final int HOOK_TELL = 14, HOOK_TELL_CHAINED = 10, HOOK_LOCK = 4, HOOK_MARK = 9, HOOK_RETELL = 10, HOOK_DRAG = 8,
		HOOK_RECOVER = 10, SNAG_TICKS = 32;
	public static final double HOOK_TURN = 12, HOOK_REACH = 10.5, HOOK_WIDTH = 1.1, HOOK_PULL_TO = 2.2, HOOK_DAMAGE = TIER.hit(6);

	// ---- the wisps
	/** The wisps: the tell (and chained), the ticks between each, how many (roused), the rest after the last, how long one lasts, how
	 *  long one goes without seeing its prey before it gutters out, how long they slow; how fast they go and turn, how close they burst,
	 *  and what they do. */
	public static final int WISP_TELL = 16, WISP_TELL_CHAINED = 12, WISP_EVERY = 4, WISP_COUNT = 3, WISP_COUNT_ROUSED = 5, WISP_RECOVER = 20,
		WISP_LIFE = 110, WISP_BLIND = 20, WISP_SLOW = 40;
	public static final double WISP_SPEED = 0.17, WISP_TURN = 0.09, WISP_RADIUS = 1.2, WISP_DAMAGE = TIER.hit(8);

	// ---- the graves
	/** The graves: the tell (and chained), how long a grave is marked before it bursts, how long it stays stuck after the last, the rest
	 *  after; how wide the marking ring starts and how wide it closes to, how wide a grave bursts, how far a roused grave is laid ahead
	 *  of where its prey is going, how high it throws, and what it does. */
	public static final int GRAVE_TELL = 16, GRAVE_TELL_CHAINED = 12, GRAVE_MARK = 22, GRAVE_HOLD = 8, GRAVE_RECOVER = 8;
	public static final double GRAVE_RING = 2.4, GRAVE_RING_END = 1.2, GRAVE_RADIUS = 1.6, GRAVE_LEAD = 10, GRAVE_LIFT = 0.7,
		GRAVE_DAMAGE = TIER.hit(13);

	// ---- the flare
	/** The flare: the tell (and chained), the rest after; how far round it bursts, how hard it throws, how long it burns, and roused how
	 *  far and fast the ring rolls on after; and what each does. */
	public static final int FLARE_TELL = 18, FLARE_TELL_CHAINED = 12, FLARE_RECOVER = 20, FLARE_BURN = 3;
	public static final double FLARE_RADIUS = 4.5, FLARE_SHOVE = 1.4, FLARE_WAVE_REACH = 10, FLARE_WAVE_SPEED = 0.45,
		FLARE_DAMAGE = TIER.hit(13), FLARE_WAVE_DAMAGE = TIER.hit(8);

	// ---- the shovel
	/** The shovel: the tell (and chained), when its aim locks and how fast it turns till then, the ticks before a roused second fan,
	 *  clods in a fan, how long a clod flies, the rest after; the nearest and furthest a clod lands, how wide the fan is, how wide a clod
	 *  lands, and what it does. */
	public static final int SHOVEL_TELL = 14, SHOVEL_TELL_CHAINED = 10, SHOVEL_LOCK = 4, SHOVEL_AGAIN = 9, CLODS = 5, CLOD_FLIGHT = 14,
		SHOVEL_RECOVER = 14;
	public static final double SHOVEL_TURN = 8, SHOVEL_NEAR = 5, SHOVEL_FAR = 12, SHOVEL_SPREAD = 32, CLOD_RADIUS = 1.5,
		CLOD_DAMAGE = TIER.hit(9);

	// ---- the backsweep
	/** The backsweep: the tell (and chained), the rest after; how far it reaches, how far off its facing behind it begins, how long it
	 *  burns, and what it does. */
	public static final int BACK_TELL = 12, BACK_TELL_CHAINED = 10, BACK_RECOVER = 12, BACK_BURN = 2;
	public static final double BACK_REACH = 5.2, BACK_FROM = 110, BACK_DAMAGE = TIER.hit(11);

	// ---- the breath
	/** The breath: the tell, how long it breathes (roused), how long it coughs after, the rest after that, how fast its aim turns
	 *  through the tell and when it locks, how long it burns; how far it reaches, how wide its cone is either side, how far it turns
	 *  (roused, out and right back across), and what it does. */
	public static final int BREATH_TELL = 16, BREATH_TICKS = 24, BREATH_TICKS_ROUSED = 32, BREATH_COUGH = 18, BREATH_RECOVER = 6,
		BREATH_LOCK = 4, BREATH_BURN = 3;
	public static final double BREATH_TURN_TELL = 6, BREATH_REACH = 9, BREATH_HALF = 22, BREATH_TURN = 60, BREATH_DAMAGE = TIER.hit(10);

	// ---- the rite
	/** The rite: its long tell, the rest after; how far round its blow throws, how hard and high; how far and fast its ring rolls out;
	 *  and what each does. */
	public static final int RITE_TELL = 40, RITE_RECOVER = 20;
	public static final double RITE_RADIUS = 5, RITE_SHOVE = 2, RITE_LIFT = 0.5, RITE_WAVE_REACH = 16, RITE_WAVE_SPEED = 0.6,
		RITE_DAMAGE = TIER.hit(8), RITE_WAVE_DAMAGE = TIER.hit(10);
	/** How high a rolling ring of fire reaches: jump it. */
	public static final double WAVE_LOW = 0.6;

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

	/** Ticks between its moves (fewer roused), how many it runs straight on into one after another, and the extra rest after a run. */
	public static final int GAP = 10, GAP_ROUSED = 5, CHAIN_MOST = 2, CHAIN_REST = 10;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case REAP -> "reap";
			case HOOK -> "hook";
			case WISPS -> "wisps";
			case GRAVES -> "graves";
			case FADE -> "fade";
			case RECOVER -> "recover";
			case LIGHTS_OUT -> "lights";
			case FLARE -> "flare";
			case SHOVEL -> "shovel";
			case BACKSWEEP -> "backsweep";
			case BREATH -> "breath";
			case SNAGGED -> "snagged";
			case RITE -> "rite";
			default -> "watch";
		};
	}

	/** The moves it picks among. */
	public static final int[] PICKS = {REAP, HOOK, WISPS, GRAVES, FLARE, SHOVEL, BACKSWEEP, BREATH, LIGHTS_OUT};
	/** The least time between two of {@code move}, shorter roused (but for Lights Out). */
	public static int rest(int move, boolean roused) {
		int r = switch (move) {
			case REAP -> 20;
			case HOOK -> 70;
			case WISPS -> 150;
			case GRAVES -> 140;
			case FLARE -> 60;
			case SHOVEL -> 100;
			case BACKSWEEP -> 30;
			case BREATH -> 160;
			case LIGHTS_OUT -> LIGHTS_REST;
			default -> 0;
		};
		return roused && move != LIGHTS_OUT ? r * 2 / 3 : r;
	}
	/**
	 * How much it wants each of {@link #PICKS} with its prey {@code distance} away, standing {@code behind} it or not, their feet
	 * {@code height} above its own, {@code since} ticks since it last made each move (by move), {@code roused} or not. Nothing for a move
	 * still resting or out of place. Crowded in front it reaps and flares; round its back it sweeps behind it; a way off it hooks,
	 * shovels and breathes; up out of reach it looses wisps; roused it may put its lantern out.
	 */
	public static double[] weights(double distance, boolean behind, double height, long[] since, boolean roused) {
		double[] w = new double[PICKS.length];
		boolean up = height > 2.5;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (since[m] < rest(m, roused)) continue;
			double d = distance;
			w[i] = switch (m) {
				case REAP -> up || behind || d > 4.6 ? 0 : d <= 3.4 ? 3 : 1.5;
				case HOOK -> up ? 1.5 : d < 4.5 || d > HOOK_REACH ? 0 : (d >= 7 ? 3 : 2) * (behind ? 0.4 : 1);
				case WISPS -> d < 4 ? 0.3 : up ? 3 : d >= 8 ? 2.5 : 1.5;
				case GRAVES -> up ? 0.5 : d <= 2 ? 0.6 : d <= 12 ? 1.8 : 1;
				case FLARE -> d <= 3.5 ? (behind ? 2.5 : 2) : d <= FLARE_RADIUS ? 1 : 0;
				case SHOVEL -> up ? 0.6 : d < SHOVEL_NEAR ? 0 : d <= SHOVEL_FAR ? 2.2 : 0.8;
				case BACKSWEEP -> !behind || d > BACK_REACH + 0.3 ? 0 : 4;
				case BREATH -> behind ? 0 : d <= 3 ? 0.8 : d <= BREATH_REACH ? 2 : 0.6;
				case LIGHTS_OUT -> roused ? 3.5 : 0;
				default -> 0;
			};
		}
		return w;
	}
	/** Its next move from {@code deck}, -1 to keep closing; {@code roll} is 0-1. */
	public static int choose(MoveDeck deck, double distance, boolean behind, double height, long[] since, boolean roused, double roll) {
		return deck.pick(PICKS, weights(distance, behind, height, since, roused), roll);
	}
	/**
	 * Roused, what it runs straight on into after {@code move} with its prey {@code distance} away, or -1: a catch on the crook into a
	 * reap, a reap into a flare, a backsweep turning into a reap, the graves into a shovel or a flare, a shovel into the hook, a flare
	 * into wisps after whoever backed off, wisps into graves. Never the same move again.
	 */
	public static int chain(int move, double distance, double roll) {
		return switch (move) {
			case HOOK -> distance <= 3.5 && roll < 0.6 ? REAP : -1;
			case REAP -> distance <= 4 && roll < 0.4 ? FLARE : -1;
			case BACKSWEEP -> distance <= REAP_REACH && roll < 0.5 ? REAP : -1;
			case GRAVES -> distance >= SHOVEL_NEAR ? (roll < 0.5 ? SHOVEL : -1) : roll < 0.4 ? FLARE : -1;
			case SHOVEL -> distance >= 5 && distance <= HOOK_REACH && roll < 0.45 ? HOOK : -1;
			case FLARE -> distance >= 4 && roll < 0.4 ? WISPS : -1;
			case WISPS -> roll < 0.4 ? GRAVES : -1;
			default -> -1;
		};
	}
	/** Whether it holds its rite now: down to half health for the first time. */
	public static boolean rouses(float health, float max, boolean roused) {
		return !roused && wounded(health, max);
	}
	/** The tell of {@code move}, shorter {@code chained} straight on from the last. */
	public static int tell(int move, boolean chained) {
		return switch (move) {
			case REAP -> chained ? REAP_TELL_CHAINED : REAP_TELL;
			case HOOK -> chained ? HOOK_TELL_CHAINED : HOOK_TELL;
			case WISPS -> chained ? WISP_TELL_CHAINED : WISP_TELL;
			case GRAVES -> chained ? GRAVE_TELL_CHAINED : GRAVE_TELL;
			case FLARE -> chained ? FLARE_TELL_CHAINED : FLARE_TELL;
			case SHOVEL -> chained ? SHOVEL_TELL_CHAINED : SHOVEL_TELL;
			case BACKSWEEP -> chained ? BACK_TELL_CHAINED : BACK_TELL;
			case BREATH -> BREATH_TELL;
			case LIGHTS_OUT -> SNUFF_TELL;
			case RITE -> RITE_TELL;
			default -> 0;
		};
	}

	/** Which way off its facing someone {@code forward} ahead and {@code side} across stands, in degrees: right positive, left negative. */
	public static double bearing(double forward, double side) {
		return Math.toDegrees(Math.atan2(side, forward));
	}
	/** Whether someone {@code angle} degrees off its facing is round its back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= BACK_FROM;
	}
	/** Where the low reap's edge has reached, in degrees off its facing, {@code k} ticks into its sweep: right to left, or {@code back}. */
	public static double reapArc(double k, boolean back) {
		double t = Math.max(0, Math.min(1, k / REAP_SWEEP)) * 2 * REAP_ARC;
		return back ? -REAP_ARC + t : REAP_ARC - t;
	}
	/** Whether a bearing lies between two edges of a sweep. */
	public static boolean sweeps(double bearing, double from, double to) {
		return bearing >= Math.min(from, to) && bearing <= Math.max(from, to);
	}
	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		double d2 = forward * forward + side * side, off = Math.abs(bearing(forward, side));
		return switch (move) {
			case REAP -> d2 <= REAP_REACH * REAP_REACH && off <= REAP_ARC && height > -1.2 && height < REAP_LOW;
			case HOOK -> forward >= 0.5 && forward <= HOOK_REACH && Math.abs(side) <= HOOK_WIDTH && height > -1.5 && height < 4.5;
			case FLARE -> d2 <= FLARE_RADIUS * FLARE_RADIUS && height > -1.5 && height < 3;
			case BACKSWEEP -> d2 <= BACK_REACH * BACK_REACH && off >= BACK_FROM && height > -1.5 && height < 3;
			case BREATH -> d2 >= 0.25 && d2 <= BREATH_REACH * BREATH_REACH && off <= BREATH_HALF && height > -1.5 && height < 3.5;
			case RITE -> d2 <= RITE_RADIUS * RITE_RADIUS && height > -1.5 && height < 3;
			case LIGHTS_OUT -> d2 <= 4.6 * 4.6 && forward >= Math.abs(side) * 0.364 && height > -1.5 && height < 3.5;
			default -> false;
		};
	}
	/** Whether a rolling ring of fire that ran from {@code from} to {@code to} out this tick catches someone {@code distance} out and
	 *  {@code height} above where it began: not if they are in the air over it. */
	public static boolean rolls(double distance, double from, double to, double height) {
		return distance > from - 0.4 && distance <= to + 0.4 && height > -1.2 && height < WAVE_LOW;
	}
	/** How wide the ring marking a grave is, {@code age} ticks after it was marked: closing in until it bursts. */
	public static double graveRing(double age) {
		double t = Math.max(0, Math.min(1, age / GRAVE_MARK));
		return GRAVE_RING + (GRAVE_RING_END - GRAVE_RING) * t;
	}
	/** Whether a grave bursting catches someone this far off it. */
	public static boolean buries(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= GRAVE_RADIUS * GRAVE_RADIUS && dy > -1.5 && dy < 2.5;
	}
	/** Whether a clod coming down catches someone this far off where it lands. */
	public static boolean clods(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= CLOD_RADIUS * CLOD_RADIUS && dy > -1.5 && dy < 2.5;
	}
	/**
	 * Where clod {@code i} of a fan of {@link #CLODS} lands, as {forward, side} along its aim, its prey {@code distance} off: spread
	 * round where they stand, the middle one on them, never nearer than {@link #SHOVEL_NEAR}; the {@code second} fan falls between.
	 */
	public static double[] shovelSpot(int i, double distance, boolean second) {
		double[] off = {1.6, -1.4, 0, -1.6, 1.4};
		double k = CLODS == 1 ? 0 : (2.0 * i / (CLODS - 1) - 1) + (second ? 1.0 / (CLODS - 1) : 0);
		double angle = Math.toRadians(k * SHOVEL_SPREAD);
		double d = Math.max(SHOVEL_NEAR, Math.min(SHOVEL_FAR, distance + off[(i + (second ? 2 : 0)) % off.length]));
		return new double[]{Math.cos(angle) * d, Math.sin(angle) * d};
	}
	/** How long the breath lasts. */
	public static int breathTicks(boolean roused) {
		return roused ? BREATH_TICKS_ROUSED : BREATH_TICKS;
	}
	/** How far round the breath has turned, in degrees toward the side its head is cocked, {@code k} ticks into it: roused, out and
	 *  right back across to the other side. */
	public static double breathTurn(double k, boolean roused) {
		double t = Math.max(0, Math.min(1, k / breathTicks(roused)));
		if (!roused) return BREATH_TURN * t;
		return t < 0.5 ? BREATH_TURN * t * 2 : BREATH_TURN * (1 - (t - 0.5) * 4);
	}
	/**
	 * Whether {@code move}, {@code elapsed} ticks in with tell {@code tell}, has left it open: its spade stuck in the earth after the
	 * graves, coughing after the breath, and past the tell of its rite.
	 */
	public static boolean open(int move, long elapsed, int tell, boolean roused) {
		return switch (move) {
			case GRAVES -> elapsed > tell;
			case BREATH -> elapsed >= tell + breathTicks(roused);
			case RITE -> elapsed >= tell;
			default -> false;
		};
	}
	/** How hard the crook drags someone in from {@code distance} away, in blocks a tick: hard at first, easing as they near. */
	public static double pull(double distance) {
		return Math.max(0, Math.min(1.6, (distance - HOOK_PULL_TO) * 0.28));
	}
	/** Whether a wisp {@code distance} from its prey bursts on them. */
	public static boolean bursts(double distance) {
		return distance <= WISP_RADIUS;
	}
	/** Whether a wisp that has gone {@code unseen} ticks without sight of its prey gutters out. */
	public static boolean gutters(int unseen) {
		return unseen >= WISP_BLIND;
	}
	/**
	 * What its rags leave of a blow. Its spade and crook turn some of a blow from the front; from behind it is only rags; spent,
	 * snagged, stuck or coughing it is open, more so behind a breath; gone to smoke nothing touches it; holding its rite it is half
	 * there.
	 */
	public static double armour(int move, boolean frontal, boolean open) {
		if (move == FADE) return 0;
		if (move == SNAGGED) return 1.4;
		if (move == RECOVER) return 1.3;
		if (move == RITE) return open ? 1.0 : 0.5;
		if (open) return 1.3;
		if (move == BREATH && !frontal) return 1.25;
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
	/** Whether it is wounded: at half health or below, and it rouses. */
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
