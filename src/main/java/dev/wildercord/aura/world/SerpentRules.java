package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

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
	/** A bite: the head draws back {@link #STRIKE_TELL} ticks, then snaps forward along the aim it locked. */
	public static final int STRIKE = 4;
	/** A lunge: it coils back, then throws its whole length {@link #LUNGE_REACH} blocks along a fixed line. */
	public static final int LUNGE = 5;
	/** Its coils swing open to catch whoever stands in front of it, and close. */
	public static final int CONSTRICT = 6;
	/** Someone is caught in its coils and being squeezed. */
	public static final int SQUEEZE = 7;
	/** It slips a blow, sliding sideways. */
	public static final int SIDESTEP = 8;
	/** Spent after an attack, reared high with its pale belly bare: the opening. */
	public static final int RECOVER = 9;
	/** At half health it rears, hisses and slides into the water ring. */
	public static final int DIVE = 10;
	/** Under the water, circling the island out of reach. */
	public static final int SUBMERGED = 11;
	/** Bursting out of the water at someone. */
	public static final int BURST = 12;
	/** Its tail comes over and down in front of it, tearing up the ground and flinging it at its prey. */
	public static final int TAIL_SLAM = 13;
	/** Its tail tip lifts and rattles, then whips round it in a full turn at ankle height: jump it. */
	public static final int TAIL_SWEEP = 14;
	/** It rears, hisses down at the ground, then drives its head into the earth and roots burst up along a line toward its prey. */
	public static final int ROOT_LINE = 15;
	/** Its hood spreads and it sprays sap in a fan, swinging its head slowly across: get round its side. */
	public static final int HOOD_SPRAY = 16;
	/** It slides round its prey in a ring and draws it shut: get out before it closes, or over it. */
	public static final int RING_COIL = 17;
	/** A lunge that caught nothing: it lies sprawled out along the ground, slow to gather itself, belly bare. */
	public static final int SPRAWLED = 18;
	/** Its head stuck in the earth after driving the roots: it has to work it free. */
	public static final int BURIED = 19;

	/** How hard it is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.APPRENTICE;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.3);

	/** Anyone this close to the trunk, horizontally, wakes it: about five blocks from its coils. */
	public static final double WAKE_RADIUS = 7.5;
	/** How far above or below the trunk's foot a player still counts as on the island. */
	public static final double WAKE_HEIGHT = 6;
	public static final int UNCOIL_TICKS = 50;

	/** Where it may fight: the cavern round the tree. Beyond this it is put back on the island. */
	public static final double LEASH = 22.5;
	/** Who it hunts, measured from the trunk: anyone in the cavern. */
	public static final double ARENA = 20;
	/** Its lair, measured from the trunk: the arena, and {@link #LAIR_HEIGHT} up or down. Anything hostile in it, it kills. */
	public static final double LAIR_HEIGHT = 12;
	/** Ticks between the kills it makes guarding its lair. */
	public static final int GUARD_EVERY = 10;
	public static boolean inLair(double dx,double dy,double dz){return dx*dx+dz*dz<ARENA*ARENA && Math.abs(dy)<LAIR_HEIGHT;}
	/** The boss bar shows inside this, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 24, BAR_DROP = 28;
	/** With nobody to fight it heads back to its landing after this long, and coils up again at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether it was left alone for {@code away} ticks, long enough to have coiled back up unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}
	/** Landing spots tried, outward from the trunk toward the herb. */
	public static final double LAND_NEAR = 4.0, LAND_FAR = 6.5;

	/** How close it closes before it stops to face its prey. */
	public static final double CLOSE = 4.5;

	// ---- the fight: every blow has its own wind-up you can read, and its aim sets a few ticks before it lands.

	/** What each blow does before defences: about two thirds of a Master's cuts. */
	public static final double STRIKE_DAMAGE = TIER.hit(22), LUNGE_DAMAGE = TIER.hit(26), SQUEEZE_DAMAGE = TIER.hit(6), BURST_DAMAGE = TIER.hit(24);
	/** The share of what armour, Protection and Resistance would stop that its fangs and coils go through anyway. */
	public static final double PIERCE = 0.45;
	/** A bite: the head drawn back in an S, its aim set {@link #STRIKE_LOCK} ticks before it snaps; quicker when chained on. */
	public static final int STRIKE_TELL = 10, STRIKE_LOCK = 3, STRIKE_CHAIN_TELL = 7, STRIKE_HOLD = 3, STRIKE_RECOVER = 14;
	/** A lunge: it coils right down, then throws itself along a line it set {@link #LUNGE_LOCK} ticks before. */
	public static final int LUNGE_TELL = 14, LUNGE_LOCK = 4, LUNGE_TICKS = 6, LUNGE_RECOVER = 16;
	public static final double LUNGE_REACH = 9;
	/** A lunge that catches nobody leaves it sprawled this long, taking half again as much. */
	public static final int SPRAWL_TICKS = 36;
	public static final int CONSTRICT_TELL = 10, CONSTRICT_LOCK = 3, CONSTRICT_RECOVER = 14;
	/** Longest it holds anyone, and how often it squeezes. */
	public static final int SQUEEZE_TICKS = 70, SQUEEZE_EVERY = 10;
	/** Ways out of its coils: this many struggles (fresh presses of jump or a strafe), or this much damage dealt to it while it holds. */
	public static final int STRUGGLES = 6;
	public static final double BREAK_FREE = 30;
	/** Its next grab waits at least this long after the last. */
	public static final int CONSTRICT_REST = 160;
	public static final int SIDESTEP_TICKS = 4, SIDESTEP_REST = 30;
	public static final double SIDESTEP_SPEED = 0.7;
	/** Ticks between its blows, fewer once it is wounded. */
	public static final int GAP = 8, GAP_WOUNDED = 5;

	/**
	 * At half health it rouses: it towers, hood spread, roaring for {@link #ROUSE_TELL} ticks, then crashes down and a ring of roots
	 * runs out from it across the island ({@link #ROUSE_SPEED} blocks a tick, out to {@link #ROUSE_REACH}, knee high: jump it), and
	 * it plunges into the water. It comes back out roused: shorter gaps, blows run on into others, and fiercer roots and spray.
	 */
	public static final int DIVE_TICKS = 40, ROUSE_TELL = 24;
	public static final double ROUSE_SPEED = 0.7, ROUSE_REACH = 12, ROUSE_HEIGHT = 1.0, ROUSE_DAMAGE = TIER.hit(14);
	/** The water phase: how long it circles between bursts, and how many bursts before it comes ashore. */
	public static final int CIRCLE_TICKS = 50, BURST_TELL = 10, BURST_TICKS = 6, BURSTS = 3;
	/** Its path round the island under the water: radius from the trunk, and how far below the trunk's foot (the ring's bed). */
	public static final double RING = 11.25, DEPTH = 4;
	public static final double CIRCLE_SPEED = 0.35;

	/** The tail slam: its tell, its rest after, and the least time between two. It is thrown from up to {@link #SLAM_REACH} away. */
	public static final int TAIL_TELL = 14, TAIL_LOCK = 3, TAIL_RECOVER = 20, TAIL_REST = 100;
	public static final double SLAM_REACH = 13;
	/** Where the tail comes down, ahead of its feet, and the shock there: everyone within {@link #SHOCK_RADIUS} of it is hit. */
	public static final double SLAM_AHEAD = 2.2, SHOCK_RADIUS = 2.8, SHOCK_DAMAGE = TIER.hit(10);
	/** Clods of the island flung in a fan, {@link #DEBRIS_FAN} degrees apart, each thrown from {@link #DEBRIS_FROM} above the ground. */
	public static final int DEBRIS = 5, DEBRIS_LIFE = 30;
	public static final double DEBRIS_FAN = 12, DEBRIS_FROM = 0.5;
	/** How fast a clod flies (blocks a tick), its upward throw, and the pull of gravity on it each tick. */
	public static final double DEBRIS_SPEED = 0.85, DEBRIS_LIFT = 0.32, DEBRIS_GRAVITY = 0.04;
	/** A clod's blow, and the most of them that land on one player in a slam. */
	public static final double DEBRIS_DAMAGE = TIER.hit(12);
	public static final int DEBRIS_HITS = 2;
	/**
	 * The share of what defences would stop that the slam goes through anyway. Thrown earth does not care about enchantments: more
	 * than its fangs, so a clod still hurts through full netherite and Protection.
	 */
	public static final double SLAM_PIERCE = 0.65;

	/**
	 * The tail sweep: its tip lifts and rattles behind it, then whips a whole turn round it, from back over one shoulder, round the
	 * front and on round the back again, {@link #SWEEP_REACH} out and ankle high. Jump it. What it does to anyone at its back.
	 */
	public static final int SWEEP_TELL = 12, SWEEP_TICKS = 8, SWEEP_RECOVER = 14, SWEEP_REST = 60;
	public static final double SWEEP_REACH = 7.5, SWEEP_HEIGHT = 0.8, SWEEP_DAMAGE = TIER.hit(16), SWEEP_START = 150;
	/**
	 * The root line: it rears and hisses down at the ground, the earth cracks along its aim for the last {@link #ROOT_LOCK} ticks, then
	 * it drives its head in and roots burst up along the line, {@link #ROOT_SPEED} blocks a tick from {@link #ROOT_FROM} out to
	 * {@link #ROOT_REACH}, {@link #ROOT_HALF} either side. Step off the line. Its head stays stuck {@link #ROOT_STUCK} ticks after.
	 * Roused, it drives three lines, {@link #ROOT_FAN} degrees apart.
	 */
	public static final int ROOT_TELL = 16, ROOT_LOCK = 4, ROOT_STUCK = 30, ROOT_REST = 80;
	public static final double ROOT_FROM = 2.5, ROOT_REACH = 15, ROOT_SPEED = 1.0, ROOT_HALF = 1.1, ROOT_HEIGHT = 2.5, ROOT_LIFT = 0.55;
	public static final double ROOT_DAMAGE = TIER.hit(18), ROOT_FAN = 24;
	/**
	 * The hood spray: its hood spreads and it rears back gurgling, then sprays a jet of sap {@link #SPRAY_REACH} out, swinging its
	 * head across from one side of its aim to the other over {@link #SPRAY_TICKS} ticks. Each splash stings and slows; nobody takes
	 * more than {@link #SPRAY_HITS}. Get round its side or out of reach. Reared up for it, its belly is bare.
	 */
	public static final int SPRAY_TELL = 14, SPRAY_TICKS = 16, SPRAY_EVERY = 4, SPRAY_HITS = 2, SPRAY_RECOVER = 12, SPRAY_REST = 120, SPRAY_SLOW = 50;
	public static final double SPRAY_REACH = 8.5, SPRAY_HALF = 32, SPRAY_HALF_ROUSED = 52, SPRAY_JET = 14, SPRAY_DAMAGE = TIER.hit(7);
	/**
	 * The ring coil: it slides out to a ring {@link #RING_R} round where its prey stood, laps it once laying its body round, then
	 * draws it shut over {@link #RING_CLOSE} ticks, crushing anyone still inside. The first it catches it holds, if it may grab again.
	 * Get out before the ring closes.
	 */
	public static final int RING_SET = 6, RING_LAP = 26, RING_CLOSE = 10, RING_RECOVER = 18, RING_REST = 240;
	public static final double RING_R = 5.0, RING_DAMAGE = TIER.hit(14);
	/** How long a lunge waits on the last. */
	public static final int LUNGE_REST = 40;
	/** Roused, it runs one blow on into another this often, and never more than {@link #CHAIN_MOST} in a row. */
	public static final double CHAIN_CHANCE = 0.6;
	public static final int CHAIN_MOST = 1;

	/** The Coilroot, eaten once the serpent is dead: the breathing stance draws in this much more, and aura trickles in besides. */
	public static final double COILROOT_BREATH = 1.3;
	/** Aura a second the Coilroot trickles in, stance or not. */
	public static final double COILROOT_TRICKLE = 0.5;
	/** A living guardian this close keeps anyone from the herb. */
	public static final double COILROOT_GUARD = 40;

	/** An aura gain from {@code source}, for someone who has eaten the Coilroot: only their breath draws in more. */
	public static double coilroot(String source, double amount) {
		return source.equals("stance") || source.equals("beat") ? amount * COILROOT_BREATH : amount;
	}

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case STRIKE -> "strike";
			case LUNGE -> "lunge";
			case CONSTRICT -> "constrict";
			case SQUEEZE -> "squeeze";
			case SIDESTEP -> "sidestep";
			case RECOVER -> "recover";
			case DIVE -> "dive";
			case SUBMERGED -> "submerged";
			case BURST -> "burst";
			case TAIL_SLAM -> "tail_slam";
			case TAIL_SWEEP -> "tail_sweep";
			case ROOT_LINE -> "root_line";
			case HOOD_SPRAY -> "hood_spray";
			case RING_COIL -> "ring_coil";
			case SPRAWLED -> "sprawled";
			case BURIED -> "buried";
			default -> "watch";
		};
	}

	/** The blows it picks among. */
	public static final int[] PICKS = {STRIKE, LUNGE, TAIL_SLAM, CONSTRICT, TAIL_SWEEP, ROOT_LINE, HOOD_SPRAY, RING_COIL};

	/** The least time between two of {@code move}; shorter once it is roused. */
	public static int rest(int move, boolean roused) {
		int r = switch (move) {
			case LUNGE -> LUNGE_REST;
			case TAIL_SLAM -> TAIL_REST;
			case CONSTRICT -> CONSTRICT_REST;
			case TAIL_SWEEP -> SWEEP_REST;
			case ROOT_LINE -> ROOT_REST;
			case HOOD_SPRAY -> SPRAY_REST;
			case RING_COIL -> RING_REST;
			default -> 0;
		};
		return roused ? r * 7 / 10 : r;
	}

	/**
	 * How much it wants each of {@link #PICKS} with its prey {@code distance} away, standing {@code behind} it or not, {@code since}
	 * holding the ticks since it last made each move (by move). Close in front it bites, grabs and sprays; a little further off it
	 * lunges, slams or drives its roots; anyone at its back gets its tail; anyone who hangs about near it gets ringed. Nothing for a
	 * move still resting.
	 */
	public static double[] weights(double distance, boolean behind, long[] since, boolean roused) {
		double[] w = new double[PICKS.length];
		double d = distance;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (since[m] < rest(m, roused)) continue;
			w[i] = switch (m) {
				case STRIKE -> !behind && d <= 6.1 ? 3 : 0;
				case LUNGE -> !behind && d >= 3.5 && d <= LUNGE_REACH + 2 ? 2.5 : 0;
				case TAIL_SLAM -> d >= 3.2 && d <= SLAM_REACH ? (d > 6 ? 2.5 : 1) * (behind ? 0.3 : 1) : 0;
				case CONSTRICT -> !behind && d <= 4.3 ? 2 : 0;
				case TAIL_SWEEP -> d <= SWEEP_REACH ? (behind ? 4 : d <= 4 ? 1.5 : 0.8) : 0;
				case ROOT_LINE -> d >= 5 && d <= ROOT_REACH ? 2.5 : d >= 3 && d < 5 ? 0.8 : 0;
				case HOOD_SPRAY -> !behind && d >= 2.5 && d <= SPRAY_REACH ? 2 : 0;
				case RING_COIL -> d >= 2 && d <= 9 ? (roused ? 2 : 1.5) : 0;
				default -> 0;
			};
		}
		return w;
	}

	/** Its next blow, by {@link #weights} and what it has been doing lately; -1 to keep closing. {@code roll} is 0-1. */
	public static int choose(MoveDeck deck, double distance, boolean behind, long[] since, boolean roused, double roll) {
		return deck.pick(PICKS, weights(distance, behind, since, roused), roll);
	}

	/**
	 * Roused, the blow it runs straight on into after {@code move} ({@code landed} if that caught anyone), its prey {@code distance}
	 * away and {@code behind} it or not; -1 for none. A bite runs into the tail or a lunge, the tail back into a bite, a lunge that
	 * landed into a bite, a slam at someone keeping off into the roots, and a spray someone got round into the tail. {@code roll} is 0-1.
	 */
	public static int chain(int move, boolean landed, double distance, boolean behind, long[] since, double roll) {
		if (roll >= CHAIN_CHANCE) return -1;
		int next = switch (move) {
			case STRIKE -> behind || distance <= 4 ? TAIL_SWEEP : distance <= LUNGE_REACH ? LUNGE : -1;
			case TAIL_SWEEP -> !behind && distance <= 5.6 ? STRIKE : -1;
			case LUNGE -> landed && !behind && distance <= 5.6 ? STRIKE : -1;
			case TAIL_SLAM -> distance > 7 && distance <= ROOT_REACH ? ROOT_LINE : -1;
			case HOOD_SPRAY -> behind && distance <= SWEEP_REACH ? TAIL_SWEEP : -1;
			default -> -1;
		};
		return next >= 0 && since[next] >= rest(next, true) / 2 ? next : -1;
	}

	/** A clod's height above where it was thrown once it has flown {@code forward} blocks: a low arc, chest high on a player. */
	public static double rise(double forward) {
		double t = forward / DEBRIS_SPEED;
		return DEBRIS_LIFT * t - DEBRIS_GRAVITY * t * (t + 1) / 2;
	}

	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case STRIKE -> forward > -0.5 && forward <= 5.6 && Math.abs(side) <= 1.5 && height > -1.5 && height < 4.5;
			case LUNGE, BURST -> forward > -1.5 && forward <= 3.4 && Math.abs(side) <= 2.0 && height > -1.5 && height < 3.5;
			case CONSTRICT -> forward > -0.5 && forward <= 3.8 && Math.abs(side) <= 2.0 && height > -1.5 && height < 3.0;
			default -> false;
		};
	}

	/** Where its tail tip points in a sweep to {@code side}, degrees off its aim, {@code tick} ticks of {@link #SWEEP_TICKS} in. */
	public static double tail(int side, double tick) {
		return side * (SWEEP_START - 360 * Math.min(SWEEP_TICKS, Math.max(0, tick)) / SWEEP_TICKS);
	}

	/**
	 * Whether its tail, on tick {@code tick} of a sweep to {@code side}, passes through someone {@code angle} degrees off its aim,
	 * {@code flat} away along the ground with their feet {@code height} above its: only someone with their feet down.
	 */
	public static boolean sweeps(int side, int tick, double angle, double flat, double height) {
		if (flat > SWEEP_REACH + 0.5 || height <= -1.5 || height >= SWEEP_HEIGHT) return false;
		double a = tail(side, tick - 1), b = tail(side, tick), lo = Math.min(a, b) - 12, hi = Math.max(a, b) + 12, at = wrap(angle);
		for (int k = -1; k <= 1; k++) if (at + 360 * k >= lo && at + 360 * k <= hi) return true;
		return false;
	}

	/** How far along its line the roots have burst {@code tick} ticks after its head went in. */
	public static double roots(double tick) {
		return Math.min(ROOT_REACH, ROOT_FROM + ROOT_SPEED * Math.max(0, tick));
	}

	/**
	 * Whether roots bursting along a line from {@code r0} out to {@code r1} this tick catch someone {@code forward} along it,
	 * {@code side} across, feet {@code height} up. Too tall to jump.
	 */
	public static boolean rooted(double forward, double side, double height, double r0, double r1) {
		return forward > r0 - 0.5 && forward <= r1 + 0.5 && Math.abs(side) <= ROOT_HALF && height > -1.5 && height < ROOT_HEIGHT;
	}

	/** Where the spray points, degrees off its aim, {@code tick} ticks of {@link #SPRAY_TICKS} into a spray swung from {@code side}. */
	public static double jet(int side, double tick, boolean roused) {
		double half = roused ? SPRAY_HALF_ROUSED : SPRAY_HALF;
		return side * (half - 2 * half * Math.min(SPRAY_TICKS, Math.max(0, tick)) / SPRAY_TICKS);
	}

	/** Whether the spray, pointing {@code jet} degrees off its aim, splashes someone {@code angle} off its aim, {@code flat} out and {@code height} up. */
	public static boolean sprays(double jet, double angle, double flat, double height) {
		return Math.abs(wrap(angle - jet)) <= SPRAY_JET && flat >= 0.5 && flat <= SPRAY_REACH && height > -1.5 && height < 3.5;
	}

	/** How far its ring, drawn {@code from} the middle, lies from it {@code tick} ticks into drawing it shut. */
	public static double ring(double from, double tick) {
		return from * (1 - Math.min(RING_CLOSE, Math.max(0, tick)) / RING_CLOSE);
	}

	/** Whether its ring, drawing in from {@code r0} to {@code r1} this tick, crushes someone {@code distance} from the middle, feet {@code height} up. */
	public static boolean closes(double distance, double r0, double r1, double height) {
		return distance <= r0 + 0.7 && distance >= r1 - 0.7 && height > -1.5 && height < 2.5;
	}

	/** Whether the roots running out from it as it rouses, from {@code from} to {@code to} this tick, pass someone {@code distance} out, feet {@code height} up. */
	public static boolean crosses(double distance, double from, double to, double height) {
		return distance > from - 0.5 && distance <= to + 0.5 && height > -1.5 && height < ROUSE_HEIGHT;
	}

	/**
	 * What its scales leave of a blow. Reared and facing you, its scales turn most of it; its flanks less so; spent after an attack,
	 * reared to spray, sprawled after a lunge that missed or with its head stuck in the earth, its belly is bare. Under the water,
	 * and rousing, little reaches it.
	 */
	public static double scales(int move, boolean frontal) {
		if (move == SPRAWLED || move == BURIED) return 1.5;
		if (move == RECOVER) return 1.35;
		if (move == HOOD_SPRAY) return 1.25;
		if (move == SUBMERGED || move == DIVE) return 0.25;
		if (move == SQUEEZE) return 1.0;
		return frontal ? 0.5 : 0.85;
	}

	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its scaled front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 60;
	}

	/** Whether someone {@code angle} degrees off its facing stands at its back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= 125;
	}

	/** An angle in degrees brought into -180 to 180. */
	public static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}

	/** Whether it slips a blow: only a frontal one, only while it watches, and not too often. */
	public static boolean slips(int move, boolean frontal, long sinceSlip) {
		return move == WATCH && frontal && sinceSlip >= SIDESTEP_REST;
	}

	/** Whether it is due to dive: the first time it falls to half health. */
	public static boolean dives(float health, float max, boolean dived) {
		return !dived && health <= max * 0.5F;
	}


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
