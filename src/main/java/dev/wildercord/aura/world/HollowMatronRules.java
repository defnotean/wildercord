package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

/**
 * The Hollow Matron, the Silk Hollow's guardian: a great pale spider that hangs head-down on her thread under the nest in the
 * miners' hall until someone comes far enough in, then drops, turns over as she lands, and rears up at them with her fangs spread.
 * Quick and skittish, light in the hand like the others; a step below a Master. No two of her moves come the same way twice running,
 * each with its own tell and its own way out. Half beaten she rouses: silk streams off her, she throws everyone near her back, and
 * fights on quicker, her moves running on into one another.
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
	/** Reared back with her fangs spread and dripping, then a lunge straight along the line she settled on; missing, her fangs sink into the floor. */
	public static final int BITE = 4;
	/** Her forelegs raised high and drawn back to one side, then scythed across in front of her at chest height: duck under it. */
	public static final int SCYTHE = 5;
	/** Head down and fangs pumping, then a glob of web spat along the line she settled on, slowing whoever it lands on and leaving the floor sticky. */
	public static final int SPIT = 6;
	/** Crouched low on bent legs with the spot she means marked, then a leap onto it, landing on whoever is under her. */
	public static final int POUNCE = 7;
	/** A quick skitter to one side out from under a blow; or, hurt too fast, back away across the hall. */
	public static final int SKITTER = 8;
	/** Spent after an attack, low on splayed legs: the opening. */
	public static final int RECOVER = 9;
	/** Wounded, she makes for one of the silk pillars, quick on her feet. */
	public static final int HASTE = 10;
	/** Up the pillar, nose to the silk, legs reaching over each other. */
	public static final int SCALE = 11;
	/** Turned head-down at the top of the pillar, watching her prey while her brood hatch; dust sifting down where she will land. */
	public static final int LURK = 12;
	/** Off the pillar and down onto the spot she marked, turning over to land on her feet. */
	public static final int PLUNGE = 13;
	/** Cut down off her pillar: falling, legs flailing, until she hits the floor. */
	public static final int TUMBLE = 14;
	/** Reared on her hind legs with her forelegs trembling high and dust sifting from the roof, then slammed down: a ring of dust runs out across the floor to jump. */
	public static final int DRUM = 15;
	/** Her abdomen curled under her and her spinnerets aimed past her fangs, then a thread shot along the line she settled on: whoever it catches is hauled in. */
	public static final int REEL = 16;
	/** Hauling whoever her thread caught in to her fangs, foreleg over foreleg, until she bites them; two blows on her snap the thread. */
	public static final int HAUL = 17;
	/** Her abdomen raised and quivering at whoever is behind her, her hind legs scraping, then stinging hairs flicked off it in a cone behind. */
	public static final int BRISTLE = 18;
	/** Her abdomen thrown up and silk shot into the roof, then clumps of web falling onto the rings marked across the floor: whoever is still in one is stuck fast. */
	public static final int SNARE = 19;
	/** Her fangs sunk in the floor after a bite that found nobody, wrenching at them, her face bare. */
	public static final int STUCK = 20;
	/** Half beaten: hunched with her legs drawn in and quivering, silk streaming up off her, then thrown wide, flinging everyone near her back. */
	public static final int ROUSE = 21;

	/** How hard it is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.SEASONED;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.32);

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

	// ---- the fight: quick feet, a tell for every move, and each move with its own way out.
	/** The share of what armour, Protection and Resistance would stop that her blows go through anyway. */
	public static final double PIERCE = 0.35;
	/** The bite: its tell (shorter run on from another move), the aim fixed for the last {@link #BITE_LOCK} of it, how long the lunge
	 *  lasts and how far it carries her (further roused), the rest after, what it does and how long its venom lasts; and how long
	 *  her fangs stay stuck in the floor after one that found nobody. */
	public static final int BITE_TELL = 12, BITE_TELL_CHAINED = 8, BITE_LOCK = 4, LUNGE_TICKS = 3, BITE_RECOVER = 12, BITE_POISON = 60, STUCK_TICKS = 30;
	public static final double LUNGE = 2.6, LUNGE_ROUSED = 3.4, BITE_DAMAGE = TIER.hit(16);
	/** The scythe: its tell, the sweep fixed for the last {@link #SCYTHE_LOCK} of it, how long the forelegs take across, and roused
	 *  how long before they come back the other way; the rest after, its reach, and what it does. */
	public static final int SCYTHE_TELL = 13, SCYTHE_TELL_CHAINED = 9, SCYTHE_LOCK = 3, SCYTHE_TICKS = 4, SCYTHE_BACK = 7, SCYTHE_RECOVER = 14;
	public static final double SCYTHE_REACH = 5.2, SCYTHE_DAMAGE = TIER.hit(15);
	/** The spit: its tell, the aim fixed for the last {@link #SPIT_LOCK} of it, the rest after, and how long a glob slows whoever it
	 *  lands on; how fast a glob flies, how close it must pass, how far it goes before it falls apart, what it does, and roused the
	 *  angle between the three she spits in a fan. */
	public static final int SPIT_TELL = 12, SPIT_TELL_CHAINED = 8, SPIT_LOCK = 3, SPIT_RECOVER = 10, WEB_SLOW_TICKS = 60;
	public static final double GLOB_SPEED = 1.2, GLOB_RADIUS = 1.1, GLOB_RANGE = 22, SPIT_DAMAGE = TIER.hit(6), SPIT_FAN = 16;
	/** A sticky patch of web where a glob splats or a clump falls: how long it lasts, and how far across it is. */
	public static final int PATCH_TICKS = 100;
	public static final double PATCH_RADIUS = 1.5;
	/** The pounce: its tell (the crouch), the spot fixed for the last {@link #POUNCE_LOCK} of it, how long the leap lasts, the rest
	 *  after; how far it can carry her, how high, how far round where she lands it hits, and what it does. */
	public static final int POUNCE_TELL = 14, POUNCE_TELL_CHAINED = 10, POUNCE_LOCK = 4, POUNCE_TICKS = 10, POUNCE_RECOVER = 24;
	public static final double POUNCE_REACH = 11, POUNCE_RADIUS = 2.8, POUNCE_HEIGHT = 2.6, POUNCE_DAMAGE = TIER.hit(20);
	/** The drum: its tell (reared up), roused how long before she slams down again, how long she stays low after, and the rest
	 *  after that; how fast the ring of dust runs out, how far, how high it comes (jump it), and what it does. */
	public static final int DRUM_TELL = 16, DRUM_TELL_CHAINED = 11, DRUM_AGAIN = 10, DRUM_LOW = 14, DRUM_RECOVER = 8;
	public static final double WAVE_SPEED = 0.6, WAVE_REACH = 12, WAVE_HEIGHT = 0.75, DRUM_DAMAGE = TIER.hit(10);
	/** The reel: its tell, the aim fixed for the last {@link #REEL_LOCK} of it; how fast her thread flies, how far, how close it must
	 *  pass; how long hauling her prey in takes, how long into the haul she bites (held before her fangs between), and where in
	 *  front of her they are held; what her bite does, the blows on her that snap the thread, and the rest after one that caught,
	 *  one that missed and one that was snapped. */
	public static final int REEL_TELL = 15, REEL_TELL_CHAINED = 10, REEL_LOCK = 4, HAUL_PULL = 14, HAUL_TICKS = 32, REEL_SNAP = 2;
	public static final int REEL_RECOVER = 12, REEL_MISS = 28, REEL_SNAPPED = 36;
	public static final double THREAD_SPEED = 2.2, REEL_RANGE = 17, THREAD_RADIUS = 0.9, HAUL_STOP = 2.6, REEL_DAMAGE = TIER.hit(14);
	/** The bristle: its tell, the aim fixed for the last {@link #BRISTLE_LOCK} of it, how long her abdomen stays up and bare after,
	 *  and roused how long before a second flick; how far the hairs carry, the half-angle of the cone (wider for the second), what
	 *  they do and how long they weaken whoever they catch. */
	public static final int BRISTLE_TELL = 13, BRISTLE_TELL_CHAINED = 9, BRISTLE_LOCK = 3, BRISTLE_OPEN = 20, BRISTLE_AGAIN = 8, BRISTLE_WEAK = 80;
	public static final double BRISTLE_REACH = 6.5, BRISTLE_ANGLE = 40, BRISTLE_WIDE = 60, BRISTLE_DAMAGE = TIER.hit(10);
	/** The snare: its tell, how long after the silk goes up the web comes down, the rest after; how wide a ring is, how long it
	 *  holds whoever it lands on and what it does; the rings she throws past the ones under her prey (more roused) and how far out. */
	public static final int SNARE_TELL = 16, SNARE_FALL = 24, SNARE_RECOVER = 12, SNARE_ROOT = 40, SNARE_EXTRA = 3, SNARE_EXTRA_ROUSED = 5;
	public static final double SNARE_RADIUS = 1.8, SNARE_DAMAGE = TIER.hit(5), SNARE_SCATTER = 9;
	/** Her rousing: its tell, the rest after; how far round her the blast throws, and what it does. */
	public static final int ROUSE_TELL = 30, ROUSE_RECOVER = 20;
	public static final double ROUSE_RADIUS = 6.5, ROUSE_DAMAGE = TIER.hit(12);
	/** The skitter aside: how long it lasts, how fast she goes, and the least time between two. */
	public static final int SKITTER_TICKS = 5, SKITTER_REST = 36;
	public static final double SKITTER_SPEED = 0.7;
	/** The back-off: how much hurt, fading each tick by {@link #STING_FADE}, sends her off, how fast she goes, and the least time between two. */
	public static final double STING_AFTER = 36, STING_FADE = 0.97, BACK_SPEED = 0.8;
	public static final int BACK_TICKS = 8, BACK_REST = 200;
	/** Ticks between her moves, fewer once she is roused; how many she runs on into one after another roused, and the extra rest after. */
	public static final int GAP = 9, GAP_ROUSED = 5, CHAIN_MOST = 2, CHAIN_REST = 10;

	// ---- the ambush: up a pillar, brood from the sacs, and down on whoever she marks. Strike the pillar's silk and she comes down.
	/** At or below this share of her health she makes for a pillar, and again every {@link #AMBUSH_REST} ticks after. */
	public static final double AMBUSH_AT = 0.7;
	public static final int AMBUSH_REST = 520;
	/** The longest she runs for her pillar before climbing from wherever she is, how long the climb takes, how long she lurks at
	 *  the top and the dust sifting down for the last {@link #LURK_TELL} of it, and how long her plunge takes. */
	public static final int HASTE_TICKS = 70, SCALE_TICKS = 28, LURK_TICKS = 56, LURK_TELL = 22, PLUNGE_TICKS = 12;
	/** How far out from a pillar's silk she clings, and how far below its top. */
	public static final double CLING = 1.7, CLING_DROP = 4.5;
	/** Her landing: what it does before defences, how far round it hits, and the rest after. */
	public static final double PLUNGE_DAMAGE = TIER.hit(22), PLUNGE_RADIUS = 3.2;
	public static final int PLUNGE_RECOVER = 30;
	/** Strikes on her pillar's silk that bring her down, the least time between two that count, the hurt the fall does her and the
	 *  long rest after it, lying stunned. */
	public static final int CUT_STRIKES = 3, STRIKE_GAP = 4, CUT_RECOVER = 70, TUMBLE_MAX = 60;
	public static final double CUT_DAMAGE = TIER.hit(24);
	/** The most brood that hatch for one ambush. */
	public static final int BROOD_MAX = 5;

	/** Whether she makes for a pillar: wounded enough, and long enough since she last did. */
	public static boolean ambushes(float health, float max, long sinceLast) {
		return health <= max * AMBUSH_AT && sinceLast >= AMBUSH_REST;
	}
	/** How many spiderlings hatch for {@code players} players in her hall: two, and one more for each player past the first, up to {@link #BROOD_MAX}. */
	public static int brood(int players) {
		return Math.max(2, Math.min(BROOD_MAX, 1 + players));
	}
	/** Whether {@code strikes} strikes on her pillar have cut her down. */
	public static boolean cut(int strikes) {
		return strikes >= CUT_STRIKES;
	}
	/** The moves she makes off the ground, on her pillar or through the air from it. */
	public static boolean aloft(int move) {
		return move == SCALE || move == LURK || move == PLUNGE;
	}
	/** How far down a plunge she is, {@code k} 0 to 1 along it: a small push off the silk, then falling faster and faster. Below 0 is above where she started. */
	public static double plunge(double k) {
		k = Math.max(0, Math.min(1, k));
		return k * k - 0.6 * k * (1 - k);
	}

	/**
	 * Weaver's Silk, taken up once the Matron is dead: walk into a wall and you climb it as she does, or hold sneak to cling; the silk
	 * holds you {@link #SILK_GRIP} ticks off the ground, then lets go until you have touched ground again.
	 */
	public static final int SILK_GRIP = 120;
	/** A living Matron this close keeps anyone from the silk. */
	public static final double SILK_GUARD = 48;

	/** Whether the silk still holds, {@code offGround} ticks since its wearer last stood on the ground. */
	public static boolean grips(long offGround) {
		return offGround < SILK_GRIP;
	}

	/**
	 * Ticks off the ground at tick {@code now}, last grounded at {@code grounded}. A mark from before the count restarted (a relog or a
	 * respawn) is later than now and counts as having just landed, so it can never give an endless grip.
	 */
	public static long offGround(int now, int grounded) {
		return grounded > now ? 0 : now - grounded;
	}

	/** Whether the silk holds its wearer to a wall: never while it is put away, otherwise while it still grips. */
	public static boolean holds(boolean stowed, long offGround) {
		return !stowed && grips(offGround);
	}

	/** The bar's name for what she is doing. */
	public static String key(int move) {
		return switch (move) {
			case BITE -> "bite";
			case SCYTHE -> "scythe";
			case SPIT -> "spit";
			case POUNCE -> "pounce";
			case SKITTER -> "skitter";
			case RECOVER -> "recover";
			case HASTE -> "haste";
			case SCALE -> "scale";
			case LURK -> "lurk";
			case PLUNGE -> "plunge";
			case TUMBLE -> "tumble";
			case DRUM -> "drum";
			case REEL -> "reel";
			case HAUL -> "haul";
			case BRISTLE -> "bristle";
			case SNARE -> "snare";
			case STUCK -> "stuck";
			case ROUSE -> "rouse";
			default -> "watch";
		};
	}

	/** The moves she picks among. */
	public static final int[] PICKS = {BITE, SCYTHE, SPIT, POUNCE, DRUM, REEL, BRISTLE, SNARE};
	/** The least time between two of {@code move}, shorter once she is {@code roused}. */
	public static int rest(int move, boolean roused) {
		int r = switch (move) {
			case BITE -> 24;
			case SCYTHE -> 40;
			case SPIT -> 70;
			case POUNCE -> 100;
			case DRUM -> 110;
			case REEL -> 170;
			case BRISTLE -> 45;
			case SNARE -> 280;
			default -> 0;
		};
		return roused ? r * 2 / 3 : r;
	}
	/**
	 * How much she wants each of {@link #PICKS} with her prey {@code distance} away, {@code behind} her or not, their feet
	 * {@code height} above her own, {@code since} holding the ticks since she last made each move (by move). Up close in front her
	 * fangs and her forelegs; the hairs off her abdomen for anyone at her back; the leap a little way out; the drum for anyone not
	 * far; web spat, a thread shot and web brought down on anyone keeping away, and on anyone up out of reach. Nothing for a move
	 * still resting.
	 */
	public static double[] weights(double distance, boolean behind, double height, long[] since, boolean roused) {
		double[] w = new double[PICKS.length];
		boolean up = height > 2.5;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (since[m] < rest(m, roused)) continue;
			double d = distance;
			w[i] = switch (m) {
				case BITE -> up || behind || d > 5 ? 0 : d <= CLOSE ? 3 : 1.6;
				case SCYTHE -> up || behind || d > SCYTHE_REACH + 0.5 ? 0 : d <= CLOSE ? 2.2 : 2.6;
				case SPIT -> d < 5 || d > GLOB_RANGE - 4 ? 0 : up ? 3 : 1.8;
				case POUNCE -> up || d < 5 || d > POUNCE_REACH ? 0 : (d >= 7 ? 3 : 2) * (behind ? 0.5 : 1);
				case DRUM -> up ? 0.4 : d <= 7 ? (behind ? 1.8 : 1.5) : d <= 10 ? 0.8 : 0;
				case REEL -> d < 6 || d > REEL_RANGE ? 0 : up ? 2.5 : d >= 9 ? 2.6 : 1.6;
				case BRISTLE -> !behind || up || d > BRISTLE_REACH + 0.5 ? 0 : 4.5;
				case SNARE -> up ? 1.5 : d < 4 ? 0.5 : 1.4;
				default -> 0;
			};
		}
		return w;
	}
	/**
	 * Her next move, by {@link #weights} and what she has been doing lately; -1 to keep closing. Never the move she just made: with
	 * nothing else to hand she keeps closing until something else is. {@code roll} is 0-1.
	 */
	public static int choose(MoveDeck deck, double distance, boolean behind, double height, long[] since, boolean roused, double roll) {
		int m = deck.pick(PICKS, weights(distance, behind, height, since, roused), roll);
		return m == deck.last() ? -1 : m;
	}
	/** Roused, the move she runs straight on into after {@code move}, her prey {@code distance} away; -1 for none. {@code roll} is 0-1. */
	public static int chain(int move, double distance, double roll) {
		return switch (move) {
			case BITE -> roll < 0.45 ? SCYTHE : -1;
			case SCYTHE -> distance <= 4.5 ? (roll < 0.4 ? BITE : -1) : distance >= 6 && roll < 0.5 ? POUNCE : -1;
			case SPIT -> distance >= 5 && distance <= POUNCE_REACH && roll < 0.5 ? POUNCE : -1;
			case POUNCE -> roll < 0.5 ? DRUM : -1;
			case DRUM -> distance >= 6 ? (roll < 0.5 ? SPIT : -1) : roll < 0.45 ? SCYTHE : -1;
			case HAUL -> roll < 0.4 ? DRUM : -1;
			case SNARE -> distance >= 4 && distance <= POUNCE_REACH ? (roll < 0.6 ? POUNCE : -1) : roll < 0.4 ? SPIT : -1;
			default -> -1;
		};
	}
	/** Whether she rouses now: down to half health for the first time. */
	public static boolean rouses(float health, float max, boolean roused) {
		return !roused && wounded(health, max);
	}
	/** Whether {@code blows} on her while she hauls someone in snap her thread. */
	public static boolean snaps(int blows) {
		return blows >= REEL_SNAP;
	}
	/** How many globs a spit throws: one, or roused three in a fan. */
	public static int globs(boolean roused) {
		return roused ? 3 : 1;
	}
	/** How many rings a snare throws past the ones under her prey. */
	public static int scatter(boolean roused) {
		return roused ? SNARE_EXTRA_ROUSED : SNARE_EXTRA;
	}

	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above her feet, in her locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case BITE -> forward > -0.5 && forward <= 2.4 && Math.abs(side) <= 1.3 && height > -1.5 && height < 3.0;
			case POUNCE -> forward * forward + side * side <= POUNCE_RADIUS * POUNCE_RADIUS && height > -1.5 && height < 2.5;
			case PLUNGE -> forward * forward + side * side <= PLUNGE_RADIUS * PLUNGE_RADIUS && height > -1.5 && height < 2.5;
			default -> false;
		};
	}
	/** Whether her forelegs, scything across at chest height, catch someone {@code forward} ahead, {@code side} across, {@code height} up; not if they are {@code ducked} under. */
	public static boolean scythes(double forward, double side, double height, boolean ducked) {
		if (ducked) return false;
		return forward > -1.0 && forward * forward + side * side <= SCYTHE_REACH * SCYTHE_REACH && height > -1.5 && height < 3.0;
	}
	/** Whether the hairs flicked off her abdomen catch someone {@code forward} ahead (so, behind her), {@code side} across, {@code height} up; in the {@code wide} cone of a second flick or not. */
	public static boolean bristles(double forward, double side, double height, boolean wide) {
		if (forward > 0.5 || height <= -1.5 || height >= 3.0) return false;
		return forward * forward + side * side <= BRISTLE_REACH * BRISTLE_REACH
			&& Math.toDegrees(Math.atan2(Math.abs(side), -forward)) <= (wide ? BRISTLE_WIDE : BRISTLE_ANGLE);
	}
	/** Whether a ring of dust running out from {@code from} to {@code to} this tick passes someone {@code distance} out, feet {@code height} up. */
	public static boolean crosses(double distance, double from, double to, double height) {
		return distance > from - 0.5 && distance <= to + 0.5 && height > -1.5 && height < WAVE_HEIGHT;
	}
	/** Whether web coming down on a ring at an offset of ({@code dx},{@code dy},{@code dz}) from someone holds them fast. */
	public static boolean snared(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= SNARE_RADIUS * SNARE_RADIUS && dy > -1.5 && dy < 2.5;
	}
	/** Whether someone at an offset of ({@code dx},{@code dy},{@code dz}) from the middle of a sticky patch is standing in it. */
	public static boolean sticks(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= PATCH_RADIUS * PATCH_RADIUS && dy > -0.6 && dy < 1.2;
	}
	/** Whether the blast of her rousing, at an offset of ({@code dx},{@code dy},{@code dz}), reaches someone. */
	public static boolean blasts(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= ROUSE_RADIUS * ROUSE_RADIUS && dy > -1.5 && dy < 3.5;
	}
	/**
	 * What her carapace leaves of a blow. Her fangs and forelegs turn some of a blow from the front; her soft abdomen at the back
	 * takes it all and a little more; spent after an attack she is open, and with her fangs stuck in the floor her face is bare.
	 */
	public static double armour(int move, boolean frontal, boolean rear) {
		return armour(move, false, frontal, rear);
	}
	/**
	 * As {@link #armour(int, boolean, boolean)}, {@code thrown} once the move's blow has gone: her abdomen stays raised and bare a
	 * while after a bristle, and she is low and open a moment after a drum.
	 */
	public static double armour(int move, boolean thrown, boolean frontal, boolean rear) {
		if (move == RECOVER || move == TUMBLE) return rear ? 1.5 : 1.3;
		if (move == STUCK) return frontal ? 1.6 : rear ? 1.5 : 1.3;
		if (thrown && move == BRISTLE) return rear ? 1.8 : 1.1;
		if (thrown && move == DRUM) return frontal ? 1.3 : rear ? 1.4 : 1.2;
		if (rear) return 1.15;
		return frontal ? 0.7 : 1.0;
	}
	/** Whether a blow arriving from {@code angle} degrees off her facing lands on her front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 55;
	}
	/** Whether a blow arriving from {@code angle} degrees off her facing lands on her abdomen, behind; and whether her prey stands there. */
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
	/** Whether she is wounded: at half health or below, and she rouses. */
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
