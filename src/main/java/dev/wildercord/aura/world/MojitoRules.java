package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

/**
 * Mojito, the misty grove's guardian: a black standard poodle grown as tall as three men, a greatsword held crosswise in his jaws
 * and mint-green magic welling up on both his flanks. He lies curled asleep before the grave in the grove, the sword laid before
 * his paws, until someone comes far enough in; then he gets up, takes the sword in his mouth, shakes out his coat, and the fight
 * is on. Every blow he has asks something different of whoever fights him: jump the low sweep, step off the line of the chop and
 * the crack that runs on past it, get clear of the shadow where he will land and then hop the ring it sends out, circle round to
 * his face while he digs, keep moving through the magic; and he never throws the same blow twice running. At half health he howls
 * and turns wild: blows run into one another and he gets the zoomies. Above the Frost Wyrmling and below the Bell-Warden.
 */
public final class MojitoRules {
	private MojitoRules() {}

	/** Curled asleep before the grave, the sword laid in front of his paws. */
	public static final int DORMANT = 0;
	/** Getting up, taking up the sword and shaking out his coat; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** On his feet, tracking. */
	public static final int WATCH = 2;
	/** Curling up again once the grove has gone quiet: the rise, played backwards. */
	public static final int BED = 3;
	/** The blade swept low across the ground in front of him, scraping sparks, from one side right round to the other: jump it. */
	public static final int SWEEP = 4;
	/** Chasing his own tail, round and round three times with the blade out, drifting after his prey; then dizzy. */
	public static final int CHASE = 5;
	/** Crouched, then a great leap onto a spot marked by his shadow; landing, a ring of magic runs out along the ground: jump it. */
	public static final int POUNCE = 6;
	/** Magic welling up on both flanks, then three volleys: straight, then led, then a fan. */
	public static final int BOLTS = 7;
	/** A quick hop back out from under a blow. */
	public static final int HOP = 8;
	/** Spent after an attack, tongue out and panting: the opening. */
	public static final int RECOVER = 9;
	/**
	 * Wild only, the zoomies: a play-bow, then {@link #ZOOM_TICKS} ticks racing round the grove, sword out, now and then skidding,
	 * barking and cutting straight across at his prey, and leaving a trail of magic that stings whoever stands in it.
	 */
	public static final int ZOOMIES = 10;
	/** Flopped down flat after the zoomies, tongue lolling: the big opening. */
	public static final int FLOP = 11;
	/** Up on his hind legs, belly bare, then the sword brought straight down along a line, and a crack of magic running on past it. */
	public static final int CHOP = 12;
	/** The sword tossed spinning along a line, and him bounding after it to fetch it back. */
	public static final int FETCH = 13;
	/** Rump to his prey, digging, the dirt flung back in clods onto marked spots; his face is bare while he does it. */
	public static final int DIG = 14;
	/** Shaking himself off, everything close flung away: what he does about anyone crowding him or hitting him again and again. */
	public static final int SHAKE = 15;
	/** Sat back and howling, rings of magic running out one after another; once, at half health, and wild from then on. */
	public static final int HOWL = 16;
	/** Dizzy after chasing his tail, staggering, stars round his head: open. */
	public static final int DIZZY = 17;
	/** Prancing about, head high, the fetched sword in his mouth, very pleased with himself: open. */
	public static final int PRANCE = 18;

	/** How hard he is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.HEROIC;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.3);

	/** Anyone in the grove this close to where he sleeps, horizontally, wakes him. */
	public static final double WAKE_RADIUS = 14;
	/** How far above or below his bed a player still counts as on the grove floor. */
	public static final double WAKE_HEIGHT = 6;
	public static final int RISE_TICKS = 50;
	/** When in his rise he takes up the sword, and when he shakes out his coat. */
	public static final int GRAB_AT = 20, SHAKE_AT = 32;

	/** The grove he keeps. He is put back if he strays more than this outside. */
	public static final double LEASH = 3;
	/** The boss bar shows to anyone within this of the grove, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 12;
	/** Ticks between the kills he makes keeping his grove. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight he walks back to his bed after this long, and curls up at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether he was left alone for {@code away} ticks, long enough to have curled up again unseen. */
	public static boolean forgets(long away) {
		return away > RESET_TICKS;
	}

	/** How close he closes before he stops to face his prey: he is long, and so is the sword. */
	public static final double CLOSE = 5;
	/** The share of what armour, Protection and Resistance would stop that his blows go through anyway. */
	public static final double PIERCE = 0.35;

	/** The sweep: its tell (shorter run on from another blow), how long the blade takes across, the rest after; how far it reaches,
	 *  how high off the ground it passes (anyone whose feet are higher has jumped it), and what it does. */
	public static final int SWEEP_TELL = 12, SWEEP_TELL_CHAINED = 7, SWEEP_TICKS = 6, SWEEP_RECOVER = 12;
	public static final double SWEEP_REACH = 7.5, SWEEP_HEIGHT = 0.75, SWEEP_DAMAGE = TIER.hit(16);
	/** The chop: its tell (reared up), how long the sword sticks in the ground after, the rest after; how far along the line it
	 *  strikes and how wide; then the crack running on past it, how long after the chop it bursts, how far it runs, how wide, and
	 *  how high it throws whoever it catches. */
	public static final int CHOP_TELL = 16, CHOP_TELL_CHAINED = 10, CHOP_STUCK = 14, CHOP_RECOVER = 18, CHOP_CRACK = 9;
	public static final double CHOP_LENGTH = 8, CHOP_WIDTH = 1.4, CHOP_DAMAGE = TIER.hit(22);
	public static final double CRACK_LENGTH = 16, CRACK_WIDTH = 1.6, CRACK_DAMAGE = TIER.hit(12), CRACK_LIFT = 0.9;
	/** The fetch: its tell (the aim fixed {@link #FETCH_LOCK} ticks before the throw), how fast and far the sword flies and how
	 *  close it passes to hit; how fast he bounds after it and for how long at most, how close to it he catches it, how fast it
	 *  comes back to him if he can't get to it; the prance after a catch, the rest after it comes back; what the sword and he do. */
	public static final int FETCH_TELL = 16, FETCH_LOCK = 4, FETCH_RUN_TICKS = 34, FETCH_PRANCE = 36, FETCH_RECOVER = 14;
	public static final double FETCH_THROW = 1.4, FETCH_RANGE = 16, FETCH_HEIGHT = 1.2, SWORD_RADIUS = 1.5, FETCH_RUN = 0.85,
		FETCH_CATCH = 2.2, FETCH_RETURN = 1.1, SWORD_DAMAGE = TIER.hit(15), BODY_DAMAGE = TIER.hit(11);
	/** The dig: its tell (turning his rump round and pawing), how long he digs, how often a clod flies and how long it is in the
	 *  air, how far he can fling one, how wide it bursts, what it does and how long it slows; how fast he turns as he digs. */
	public static final int DIG_TELL = 14, DIG_TICKS = 42, DIG_EVERY = 5, DIG_FLIGHT = 14, DIG_RECOVER = 14, DIG_SLOW = 30;
	public static final double DIG_RANGE = 15, DIG_RADIUS = 1.6, DIG_DAMAGE = TIER.hit(8), DIG_TURN = 2;
	/** The pounce: its tell (the crouch, the spot fixed {@link #POUNCE_LOCK} ticks before), how long he is in the air, the rest
	 *  after; how far he leaps at most, how far round where he lands the sword hits, and what it does. */
	public static final int POUNCE_TELL = 16, POUNCE_LOCK = 4, POUNCE_TICKS = 14, POUNCE_RECOVER = 22;
	public static final double POUNCE_REACH = 20, POUNCE_RADIUS = 4, POUNCE_DAMAGE = TIER.hit(20);
	/** A ring of magic running out along the ground: how fast, how far, how high off the ground it reaches, what it does. */
	public static final double WAVE_SPEED = 0.8, WAVE_REACH = 12, WAVE_HEIGHT = 0.75, WAVE_DAMAGE = TIER.hit(9), WAVE_LIFT = 0.45;
	/** The bolts: the tell while the magic wells up, how many volleys and the ticks between them, the rest after; how fast a bolt
	 *  flies, how long it lasts, how close it has to pass to hit, how many bolts in the fan and how wide it spreads. */
	public static final int BOLT_TELL = 16, BOLT_VOLLEYS = 3, BOLT_EVERY = 10, BOLT_RECOVER = 16, BOLT_LIFE = 60, FAN = 5;
	public static final double BOLT_SPEED = 0.7, BOLT_RADIUS = 1.1, FAN_SPREAD = 30, BOLT_DAMAGE = TIER.hit(7);
	/** Chasing his tail: the tell (he spots it), how many times round and how long it takes, how fast he drifts after his prey,
	 *  how far the blade reaches; then how long he is dizzy. */
	public static final int CHASE_TELL = 12, CHASE_TURNS = 3, CHASE_TICKS = 30, DIZZY_TICKS = 34;
	public static final double CHASE_DRIFT = 0.15, CHASE_REACH = 6.5, CHASE_DAMAGE = TIER.hit(12);
	/** The shake: the tell (a shiver), the rest after, the least time between two; how many blows taken in how long, or how long
	 *  someone stands how close, sets it off; how far it flings, how hard, and what it does. */
	public static final int SHAKE_TELL = 12, SHAKE_RECOVER = 14, SHAKE_REST = 160, SHAKE_BLOWS = 4, SHAKE_WINDOW = 60, SHAKE_CROWD = 40;
	public static final double SHAKE_NEAR = 3.5, SHAKE_RADIUS = 5.5, SHAKE_PUSH = 1.8, SHAKE_LIFT = 0.5, SHAKE_DAMAGE = TIER.hit(9);
	/** The howl: the tell (sitting back, head going up), how many rings and the ticks between them, how far they run. */
	public static final int HOWL_TELL = 14, HOWL_RINGS = 3, HOWL_EVERY = 14;
	public static final double HOWL_REACH = 18;
	/** The hop back: how long it lasts, how fast, and the least time between two. */
	public static final int HOP_TICKS = 6, HOP_REST = 40;
	public static final double HOP_SPEED = 0.8;
	/** The zoomies: the play-bow before, how long he runs, how fast, the flop after, the least time between two, the least and most
	 *  radius of his circles, how often a struck player can be struck again; how often he cuts across, how long he skids and barks
	 *  first, how fast and how long the dash; the trail, how long it lingers, how often it stings, for how much, and how wide. */
	public static final int ZOOM_TELL = 18, ZOOM_TICKS = 100, ZOOM_FLOP = 70, ZOOM_REST = 420, ZOOM_REHIT = 20;
	public static final double ZOOM_SPEED = 0.95, ZOOM_MIN_RADIUS = 6, ZOOM_MAX_RADIUS = 12, ZOOM_DAMAGE = TIER.hit(14);
	public static final int DASH_EVERY = 30, DASH_SKID = 7, DASH_TICKS = 14;
	public static final double DASH_SPEED = 1.5;
	public static final int TRAIL_TICKS = 70, TRAIL_EVERY = 10;
	public static final double TRAIL_DAMAGE = TIER.hit(3), TRAIL_RADIUS = 1.3;
	/** Ticks between his blows, fewer once he is wild; and the longer rest at the end of a run of blows. */
	public static final int GAP = 12, GAP_WILD = 6, CHAIN_REST = 10, CHAIN_MOST = 2;

	/** The bar's name for what he is doing. */
	public static String key(int move) {
		return switch (move) {
			case SWEEP -> "sweep";
			case CHASE -> "chase";
			case POUNCE -> "pounce";
			case BOLTS -> "bolts";
			case HOP -> "hop";
			case RECOVER -> "recover";
			case ZOOMIES -> "zoomies";
			case FLOP -> "flop";
			case CHOP -> "chop";
			case FETCH -> "fetch";
			case DIG -> "dig";
			case SHAKE -> "shake";
			case HOWL -> "howl";
			case DIZZY -> "dizzy";
			case PRANCE -> "prance";
			default -> "watch";
		};
	}

	/** The blows he picks among. */
	public static final int[] PICKS = {SWEEP, CHOP, CHASE, HOP, FETCH, DIG, POUNCE, BOLTS};
	/** The least time between two of {@code move}. */
	public static int rest(int move) {
		return switch (move) {
			case CHOP -> 50;
			case CHASE -> 120;
			case HOP -> HOP_REST;
			case FETCH -> 160;
			case DIG -> 200;
			case POUNCE -> 120;
			case BOLTS -> 100;
			default -> 0;
		};
	}
	/**
	 * How much he wants each of {@link #PICKS} with his prey {@code distance} away, standing {@code behind} him or not, {@code since}
	 * holding the ticks since he last made each move (by move): up close the sweep and the chop, his tail for anyone at his back;
	 * further off the fetch, the dig and the pounce; far off the pounce, the fetch and the magic. Nothing for a move still resting.
	 */
	public static double[] weights(double distance, boolean behind, long[] since) {
		double[] w = new double[PICKS.length];
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (since[m] < rest(m)) continue;
			boolean far = distance > 10, mid = distance >= 5;
			w[i] = switch (m) {
				case SWEEP -> !behind && distance <= SWEEP_REACH ? (distance <= CLOSE ? 3 : 1.5) : 0;
				case CHOP -> !behind && distance <= CHOP_LENGTH ? 2 : 0;
				case CHASE -> distance <= CHASE_REACH ? (behind ? 3.5 : 1.2) : 0;
				case HOP -> distance <= 2.5 ? 0.6 : 0;
				case FETCH -> mid && distance <= FETCH_RANGE ? (far ? 2.5 : 2) : 0;
				case DIG -> mid ? (far ? 1 : 2) : 0;
				case POUNCE -> mid && distance <= POUNCE_REACH ? (far ? 2.5 : 1.5) : 0;
				case BOLTS -> distance >= 6 ? (far ? 2.5 : 1) : 0;
				default -> 0;
			};
		}
		return w;
	}
	/** His next blow, by {@link #weights} and what he has been doing lately; -1 to keep closing. {@code roll} is 0-1. */
	public static int choose(MoveDeck deck, double distance, boolean behind, long[] since, double roll) {
		return deck.pick(PICKS, weights(distance, behind, since), roll);
	}
	/** Wild, the blow he runs straight on into after {@code move}, his prey {@code distance} away; -1 for none. {@code roll} is 0-1. */
	public static int chain(int move, double distance, double roll) {
		return switch (move) {
			case SWEEP -> distance > SWEEP_REACH ? -1 : roll < 0.5 ? SWEEP : roll < 0.9 ? CHOP : -1;
			case POUNCE -> distance <= SWEEP_REACH && roll < 0.4 ? SWEEP : -1;
			case CHOP -> distance <= CLOSE && roll < 0.3 ? SWEEP : -1;
			default -> -1;
		};
	}
	/** Whether he starts the zoomies with his prey {@code distance} away: only wild, rested, and not right on top of him. */
	public static boolean zooms(boolean wild, long sinceZoom, double distance) {
		return wild && sinceZoom >= ZOOM_REST && distance >= 3;
	}
	/** Whether he shakes himself off, having taken {@code blows} blows lately and been crowded {@code crowded} ticks. */
	public static boolean shakes(int blows, int crowded, long sinceShake) {
		return sinceShake >= SHAKE_REST && (blows >= SHAKE_BLOWS || crowded >= SHAKE_CROWD);
	}
	/** Whether he howls now: down to half health for the first time. */
	public static boolean howls(float health, float max, boolean howled) {
		return !howled && wounded(health, max);
	}

	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above his feet, in his locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		double flat = forward * forward + side * side;
		return switch (move) {
			case CHOP -> forward >= 0.5 && forward <= CHOP_LENGTH && Math.abs(side) <= CHOP_WIDTH && height > -1.5 && height < 3.5;
			case CHASE -> flat <= CHASE_REACH * CHASE_REACH && height > -1.5 && height < 4.0;
			case FETCH -> forward > -2.0 && forward <= 3.2 && Math.abs(side) <= 2.2 && height > -1.5 && height < 5.0;
			case ZOOMIES -> forward > -2.0 && forward <= 3.5 && Math.abs(side) <= 2.4 && height > -1.5 && height < 5.0;
			default -> false;
		};
	}
	/** Whether the crack past a chop catches someone {@code forward} ahead along its line, {@code side} across, {@code height} up. */
	public static boolean cracks(double forward, double side, double height) {
		return forward > CHOP_LENGTH - 0.5 && forward <= CRACK_LENGTH && Math.abs(side) <= CRACK_WIDTH && Math.abs(height) < 2;
	}
	/** Where the blade of a sweep to {@code side} points, degrees off his aim, {@code tick} ticks of {@link #SWEEP_TICKS} in. */
	public static double blade(int side, double tick) {
		return side * (90 - 180 * Math.min(SWEEP_TICKS, Math.max(0, tick)) / SWEEP_TICKS);
	}
	/**
	 * Whether the blade of a sweep to {@code side}, on its tick {@code tick}, passes through someone {@code angle} degrees off his
	 * aim, {@code flat} away along the ground with their feet {@code height} above his: only someone with their feet down.
	 */
	public static boolean sweeps(int side, int tick, double angle, double flat, double height) {
		double a = blade(side, tick - 1), b = blade(side, tick), at = wrap(angle);
		return at >= Math.min(a, b) - 12 && at <= Math.max(a, b) + 12 && flat <= SWEEP_REACH && height > -1.5 && height < SWEEP_HEIGHT;
	}
	/** Whether a ring running out from {@code from} to {@code to} this tick passes someone {@code distance} out, feet {@code height} up. */
	public static boolean crosses(double distance, double from, double to, double height) {
		return distance > from - 0.5 && distance <= to + 0.5 && height > -1.5 && height < WAVE_HEIGHT;
	}
	/** Whether a pounce landing at an offset of ({@code dx},{@code dy},{@code dz}) from the player hits them. */
	public static boolean lands(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= POUNCE_RADIUS * POUNCE_RADIUS && dy > -2.5 && dy < 2.5;
	}
	/** Where a pounce at someone {@code distance} away comes down: on them, but never further than he can leap. */
	public static double leapLength(double distance) {
		return Math.max(0, Math.min(POUNCE_REACH, distance));
	}
	/** Whether a clod bursting at an offset of ({@code dx},{@code dy},{@code dz}) from the player hits them. */
	public static boolean clods(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= DIG_RADIUS * DIG_RADIUS && dy > -1.5 && dy < 2.5;
	}
	/** Whether a bolt at (bx,by,bz) touches someone standing at (px,py,pz) with height {@code tall}. */
	public static boolean boltHits(double bx, double by, double bz, double px, double py, double pz, double tall) {
		return touches(bx, by, bz, px, py, pz, tall, BOLT_RADIUS);
	}
	/** Whether the thrown sword at (sx,sy,sz) touches someone standing at (px,py,pz) with height {@code tall}. */
	public static boolean swordHits(double sx, double sy, double sz, double px, double py, double pz, double tall) {
		return touches(sx, sy, sz, px, py, pz, tall, SWORD_RADIUS);
	}
	private static boolean touches(double x, double y, double z, double px, double py, double pz, double tall, double radius) {
		double cy = Math.max(py, Math.min(py + tall, y));
		double dx = x - px, dy = y - cy, dz = z - pz;
		return dx * dx + dy * dy + dz * dz <= radius * radius;
	}
	/** The radius he runs his circles at, given how far his prey stands from the middle of the grove. */
	public static double zoomRadius(double preyFromMiddle) {
		return Math.max(ZOOM_MIN_RADIUS, Math.min(ZOOM_MAX_RADIUS, preyFromMiddle));
	}
	/**
	 * What his coat leaves of a blow. The sword across his jaws turns some of a blow from the front and his flanks take it all; but
	 * reared up for the chop his belly is bare, digging his face is (his fluffy rump turns blows), and spent, dizzy, prancing or
	 * flopped he is open, flopped widest.
	 */
	public static double armour(int move, boolean frontal, boolean rearing) {
		return switch (move) {
			case FLOP -> 1.5;
			case DIZZY -> 1.4;
			case RECOVER, PRANCE -> 1.3;
			case CHOP -> rearing ? 1.3 : frontal ? 0.7 : 1.0;
			case DIG -> frontal ? 1.5 : 0.8;
			case ZOOMIES -> 0.8;
			default -> frontal ? 0.7 : 1.0;
		};
	}
	/** Whether a blow arriving from {@code angle} degrees off his facing lands on his front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 50;
	}
	/** Whether someone {@code angle} degrees off his facing stands at his back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= 125;
	}
	private static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
	/** Whether he hops back from a blow: only while he watches, not too often, and only some of the time. */
	public static boolean dodges(int move, long sinceHop, double roll) {
		return move == WATCH && sinceHop >= HOP_REST && roll < 0.25;
	}
	/** Whether he is wounded: at half health or below he howls, and is wild from then on. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a grove from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from his bed is close enough to wake him (and inside the grove, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: he cannot be hurt, and he shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == BED;
	}

	/** How far up he is, 0 curled to 1 standing, {@code elapsed} ticks into a rise (or curling up, which runs it backwards). */
	public static float rise(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case BED -> 1 - t;
			default -> 1;
		};
	}

	/** Health he knits back this tick, {@code sinceCut} ticks after he was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
