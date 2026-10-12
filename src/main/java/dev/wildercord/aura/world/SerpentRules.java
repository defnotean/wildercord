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

	// ---- the fight: tells of three to four ticks like a Master's, but fewer and softer blows, and longer openings between them.

	/** What each blow does before defences: about two thirds of a Master's cuts. */
	public static final double STRIKE_DAMAGE = 22, LUNGE_DAMAGE = 26, SQUEEZE_DAMAGE = 6, BURST_DAMAGE = 24;
	/** The share of what armour, Protection and Resistance would stop that its fangs and coils go through anyway. */
	public static final double PIERCE = 0.45;
	/** A bite's tell, the tell of each bite chained after it, how long its head stays out after biting, and its rest after the last. */
	public static final int STRIKE_TELL = 4, STRIKE_CHAIN_TELL = 3, STRIKE_HOLD = 2, STRIKE_RECOVER = 12;
	/** Bites in a flurry, at most. */
	public static final int FLURRY = 3;
	public static final int LUNGE_TELL = 7, LUNGE_TICKS = 5, LUNGE_RECOVER = 18;
	public static final double LUNGE_REACH = 6.5;
	public static final int CONSTRICT_TELL = 5, CONSTRICT_RECOVER = 14;
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

	/** The water phase: its rearing tell, how long it circles between bursts, and how many bursts before it comes ashore. */
	public static final int DIVE_TICKS = 24, CIRCLE_TICKS = 50, BURST_TELL = 10, BURST_TICKS = 6, BURSTS = 3;
	/** Its path round the island under the water: radius from the trunk, and how far below the trunk's foot (the ring's bed). */
	public static final double RING = 11.25, DEPTH = 4;
	public static final double CIRCLE_SPEED = 0.35;

	/** The tail slam: its tell, its rest after, and the least time between two. It is thrown from up to {@link #SLAM_REACH} away. */
	public static final int TAIL_TELL = 8, TAIL_RECOVER = 20, TAIL_REST = 100;
	public static final double SLAM_REACH = 13;
	/** Where the tail comes down, ahead of its feet, and the shock there: everyone within {@link #SHOCK_RADIUS} of it is hit. */
	public static final double SLAM_AHEAD = 2.2, SHOCK_RADIUS = 2.8, SHOCK_DAMAGE = 10;
	/** Clods of the island flung in a fan, {@link #DEBRIS_FAN} degrees apart, each thrown from {@link #DEBRIS_FROM} above the ground. */
	public static final int DEBRIS = 5, DEBRIS_LIFE = 30;
	public static final double DEBRIS_FAN = 12, DEBRIS_FROM = 0.5;
	/** How fast a clod flies (blocks a tick), its upward throw, and the pull of gravity on it each tick. */
	public static final double DEBRIS_SPEED = 0.85, DEBRIS_LIFT = 0.32, DEBRIS_GRAVITY = 0.04;
	/** A clod's blow, and the most of them that land on one player in a slam. */
	public static final double DEBRIS_DAMAGE = 12;
	public static final int DEBRIS_HITS = 2;
	/**
	 * The share of what defences would stop that the slam goes through anyway. Thrown earth does not care about enchantments: more
	 * than its fangs, so a clod still hurts through full netherite and Protection.
	 */
	public static final double SLAM_PIERCE = 0.65;

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
			default -> "watch";
		};
	}

	/**
	 * Its choice of blow at {@code distance} from its prey; -1 to keep closing. {@code roll} is 0-1. Up close it bites or grabs; at a
	 * lunge's reach it lunges, or slams its tail; beyond that, while the slam is ready, it flings the ground at anyone keeping away.
	 */
	public static int choose(double distance, double roll, boolean mayConstrict, boolean maySlam) {
		if (distance <= CLOSE) return mayConstrict && roll < 0.28 ? CONSTRICT : STRIKE;
		if (distance <= LUNGE_REACH + 2.5) return maySlam && roll < 0.4 ? TAIL_SLAM : LUNGE;
		if (distance <= SLAM_REACH && maySlam) return TAIL_SLAM;
		return -1;
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

	/**
	 * What its scales leave of a blow. Reared and facing you, its scales turn most of it; its flanks less so; spent after an attack
	 * its belly is bare. Under the water little reaches it.
	 */
	public static double scales(int move, boolean frontal) {
		if (move == RECOVER) return 1.35;
		if (move == SUBMERGED || move == DIVE) return 0.25;
		if (move == SQUEEZE) return 1.0;
		return frontal ? 0.5 : 0.85;
	}

	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its scaled front. */
	public static boolean frontal(double angle) {
		double a = Math.abs(((angle % 360) + 540) % 360 - 180);
		return a <= 60;
	}

	/** Whether it slips a blow: only a frontal one, only while it watches, and not too often. */
	public static boolean slips(int move, boolean frontal, long sinceSlip) {
		return move == WATCH && frontal && sinceSlip >= SIDESTEP_REST;
	}

	/** Bites in this flurry: two, or three a third of the time (half the time once it is wounded). */
	public static int flurry(double roll, boolean wounded) {
		return roll < (wounded ? 0.5 : 0.33) ? FLURRY : FLURRY - 1;
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
