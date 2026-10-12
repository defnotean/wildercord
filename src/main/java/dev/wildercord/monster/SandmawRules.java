package dev.wildercord.monster;

import dev.wildercord.cast.MoveDeck;

/**
 * The Sandmaw Wyrm's numbers and decisions, kept pure for its tests. Under the sand it swims after footsteps and rises beneath its
 * prey, or now and then breaches clean out along a furrow and comes down across them. Up, it never makes the same move twice
 * running: it slams its head down along a line (step aside), spits a gout of sand down a cone (get out of it, or under it), rears
 * and plunges jaws first on a marked spot to swallow whoever stands there (move off it), sinks into a sinkhole that drags everyone
 * in toward its jaws (run out), drives its head under and sends geysers up beneath its prey one after another (keep moving), or
 * rattles its spines and looses them along marked rays (stand in the gaps). Each leaves it open after: stuck, choking, beached or
 * hauling itself out. Half beaten, it thrashes, throws everyone back and fights on roused: quicker, more of everything.
 */
public final class SandmawRules {
	private SandmawRules() {}

	/** Its step among the elites. */
	public static final EliteTier TIER = EliteTier.BRUTE;

	/** Waiting about, between moves. */
	public static final int IDLE = 0;
	/** Swimming under the sand. */
	public static final int BURROW = 1;
	/** The ground rumbling where it is about to burst up. */
	public static final int RUMBLE = 2;
	/** Bursting up through the sand. */
	public static final int ERUPT = 3;
	/** Going back under. */
	public static final int DIVE = 4;
	/** Reared back, then the head slammed down along a line. */
	public static final int SLAM = 5;
	/** Its head stuck in the sand after a slam. */
	public static final int STUCK = 6;
	/** Reared with the jaws flared, then a gout of sand down a cone. */
	public static final int SPIT = 7;
	/** Reared straight up, then a plunge jaws first on a marked spot. */
	public static final int GULP = 8;
	/** Someone swallowed and held, being chewed. */
	public static final int GULPED = 9;
	/** Missed the plunge, or had its prey break free: choking on sand. */
	public static final int CHOKING = 10;
	/** Sunk halfway into a sinkhole that drags everyone in toward its jaws. */
	public static final int SINKHOLE = 11;
	/** Its head driven under, sending geysers up beneath its prey. */
	public static final int GEYSER = 12;
	/** Hunched, its spines rattling, then loosed along rays. */
	public static final int QUILLS = 13;
	/** A leap clean out of the sand along a furrow, down across its prey. */
	public static final int BREACH = 14;
	/** Lying slumped on the sand after a breach, writhing to right itself. */
	public static final int BEACHED = 15;
	/** Half beaten: rearing and thrashing, throwing everyone back. Once. */
	public static final int THRASH = 16;
	/** The moves it chooses among once up. */
	public static final int[] PICKS = {SLAM, SPIT, GULP, SINKHOLE, GEYSER, QUILLS};
	public static final int MEMORY = 4;

	/** Under the sand it feels footsteps this far off, or only this near from someone sneaking. */
	public static final double FEEL_RANGE = 16, FEEL_SNEAKING = 5;
	/** Under the sand it rises once it is this near (across the ground) beneath its prey, after at least this long down. */
	public static final double UNDER = 1.5;
	public static final int MIN_BURROW = 50;
	/** It gives up waiting beneath and rises where it is after this long down. */
	public static final int MAX_BURROW = 200;
	/** The ground rumbles this long (the tell), then it bursts up, striking and flinging all this near. */
	public static final int RUMBLE_TICKS = 20, ERUPT_TICKS = 10, DIVE_TICKS = 16;
	public static final double ERUPT_RADIUS = 2.2, ERUPT_DAMAGE = 8, ERUPT_LAUNCH = 0.9;
	/** Between moves up top, calm and roused. */
	public static final int REST = 22, ROUSED_REST = 10;
	/** It dives again after this long up, or once it has taken this share of its health since it rose. */
	public static final int MAX_SURFACED = 260;
	public static final double DIVE_HURT = 0.25;

	/** The slam: reared back this long, its line locked this long before it falls, then stuck this long. Roused, once more. */
	public static final int SLAM_TELL = 16, ROUSED_SLAM_TELL = 11, SLAM_AGAIN_TELL = 8, SLAM_LOCK = 4, STUCK_TICKS = 30;
	public static final double SLAM_LENGTH = 6.5, SLAM_WIDTH = 1.3, SLAM_DAMAGE = 10;
	/** The spit: reared this long, its aim locked this long before, a gout this long; roused, three gouts fanned this many degrees. */
	public static final int SPIT_TELL = 14, SPIT_LOCK = 3, SPIT_GOUT = 8, SPIT_RECOVER = 16;
	public static final double SPIT_RANGE = 12, SPIT_CONE = 0.8, SPIT_DAMAGE = 3, SPIT_FAN = 25;
	/** The gulp: reared this long, its spot following its prey until this long before, the plunge, then the hold or the choke. */
	public static final int GULP_TELL = 18, ROUSED_GULP_TELL = 13, GULP_LOCK = 4, GULP_PLUNGE = 4;
	public static final int GULP_HOLD = 60, GULP_CHEW_EVERY = 20, CHOKING_TICKS = 36, GULP_RECOVER = 20;
	/** How far off it plunges at most, who it swallows, and who else the plunge strikes. */
	public static final double GULP_REACH = 7, GULP_RADIUS = 1.6, GULP_SPLASH = 2.6, GULP_SPLASH_DAMAGE = 6;
	/** What a chew does, what a swallowed player must deal it to break free, and the spit-out at the end. */
	public static final double GULP_CHEW = 4, GULP_BREAK = 12, GULP_SPAT = 4, GULP_THROW = 1.2, GULP_THROW_UP = 0.8;
	/** The sinkhole: sinking this long, then pulling this long, the jaws marked this long before they snap, then hauling out. */
	public static final int SINK_TELL = 20, SINK_PULL = 50, ROUSED_SINK_PULL = 60, SINK_MARK = 10, SINK_RECOVER = 26;
	public static final double SINK_RADIUS = 9, SINK_DRAG = 0.09, SINK_JAWS = 2.4, SINK_DAMAGE = 12;
	/** The geysers: head down this long, one marked every so often beneath its prey, each going up so long after. */
	public static final int GEYSER_TELL = 14, GEYSER_EVERY = 8, ROUSED_GEYSER_EVERY = 6, GEYSER_FUSE = 12;
	public static final int GEYSER_COUNT = 5, ROUSED_GEYSER_COUNT = 7, GEYSER_RECOVER = 16;
	public static final double GEYSER_RADIUS = 1.3, GEYSER_DAMAGE = 7, GEYSER_LAUNCH = 0.8, GEYSER_REACH = 16;
	/** The spines: rattling this long with the rays drawn, then loosed; roused, a second volley so long after, turned between. */
	public static final int QUILL_TELL = 18, QUILL_RAYS = 8, QUILL_AGAIN = 14, QUILL_RECOVER = 20;
	public static final double QUILL_LENGTH = 10, QUILL_WIDTH = 0.8, QUILL_DAMAGE = 6;
	/** The breach: the furrow shown this long (its line locked this long before), the flight, the crash and lying beached. */
	public static final int BREACH_TELL = 24, ROUSED_BREACH_TELL = 12, BREACH_LOCK = 6, BREACH_FLIGHT = 16, BEACHED_TICKS = 36;
	/** How far it flies (past its prey by so much), how high, and how far apart the ends may stand in height. */
	public static final double BREACH_MIN = 8, BREACH_MAX = 16, BREACH_PAST = 4, BREACH_ARC = 4, BREACH_STEP = 5;
	/** How near and far its prey must be for it to breach, and how often it does when they are. */
	public static final double BREACH_NEAR = 5, BREACH_FAR = 12, BREACH_CHANCE = 0.3;
	public static final double BREACH_BURST = 2, BREACH_BURST_DAMAGE = 6, BREACH_PASS_DAMAGE = 8, BREACH_CRASH = 2.6, BREACH_CRASH_DAMAGE = 10;
	/** The thrash at half health: how long, how far it throws, how hard and what it does. */
	public static final int THRASH_TICKS = 40;
	public static final double THRASH_RADIUS = 7, THRASH_PUSH = 1.4, THRASH_DAMAGE = 4;

	/** Whether, down below, it feels someone walking this far off. */
	public static boolean feels(double distance, boolean sneaking) {
		return distance <= (sneaking ? FEEL_SNEAKING : FEEL_RANGE);
	}

	/** Whether, down below, it is close enough under its prey to rise. */
	public static boolean under(double across) {
		return across <= UNDER;
	}

	/** Whether it rises now: beneath its prey after a little while down, or tired of waiting. */
	public static boolean rises(double across, int down) {
		return down >= MAX_BURROW || down >= MIN_BURROW && under(across);
	}

	/** Whether, down below, it breaches out at its prey this far off instead of swimming on under them; never twice running. */
	public static boolean breaches(double across, int down, boolean lastWasBreach, double roll) {
		return !lastWasBreach && down >= MIN_BURROW && across >= BREACH_NEAR && across <= BREACH_FAR && roll < BREACH_CHANCE;
	}

	/** How far along the furrow it flies, its prey this far off: past them, within bounds. */
	public static double breachLength(double distance) {
		return Math.max(BREACH_MIN, Math.min(BREACH_MAX, distance + BREACH_PAST));
	}

	/** What each move is worth to it now, by how far off its prey is and whether it is crowded or being worn down. */
	public static double[] weights(double distance, boolean crowded) {
		var w = new double[PICKS.length];
		for (int i = 0; i < PICKS.length; i++) w[i] = switch (PICKS[i]) {
			case SLAM -> distance <= SLAM_LENGTH ? 3 : 0;
			case SPIT -> distance >= 4 && distance <= SPIT_RANGE ? distance > 8 ? 3 : 2.5 : 0;
			case GULP -> distance >= 2 && distance <= GULP_REACH ? 2 : 0;
			case SINKHOLE -> distance <= SINK_RADIUS ? distance <= 5 ? 1.5 : 1.2 : 0;
			case GEYSER -> distance >= 3 && distance <= GEYSER_REACH ? distance > 8 ? 2.5 : 2 : 0;
			case QUILLS -> distance <= QUILL_LENGTH - 3 ? crowded ? 3 : 1.5 : 0;
			default -> 0;
		};
		return w;
	}

	/** Which move it makes next, or -1 to dive again: when nothing reaches, or only what it just did. */
	public static int choose(MoveDeck deck, double distance, boolean crowded, double roll) {
		int move = deck.pick(PICKS, weights(distance, crowded), roll);
		return move == deck.last() ? -1 : move;
	}

	/** Whether the slam strikes something this far along its line and this far to the side of it. */
	public static boolean inLine(double along, double across) {
		return along >= -0.5 && along <= SLAM_LENGTH && Math.abs(across) <= SLAM_WIDTH;
	}

	/** Whether the spit strikes something this far away, at this dot of its look and the way to them. */
	public static boolean inSpit(double distance, double dot) {
		return distance <= SPIT_RANGE && dot >= SPIT_CONE;
	}

	/** Whether the plunge swallows someone this far (across the ground) from where it comes down, and this far above it. */
	public static boolean swallows(double across, double rise) {
		return across <= GULP_RADIUS && Math.abs(rise) < 2;
	}

	/** Whether the sinkhole drags someone this far off, and whether its jaws catch them when they snap. */
	public static boolean drags(double across, double rise) {
		return across <= SINK_RADIUS && across > 0.4 && Math.abs(rise) < 4;
	}

	public static boolean snaps(double across, double rise) {
		return across <= SINK_JAWS && Math.abs(rise) < 3;
	}

	/**
	 * Whether a volley of spines, the first ray loosed at {@code start} degrees and the rest evenly round, strikes someone at this
	 * bearing (degrees) and distance across the ground, and this far above.
	 */
	public static boolean quilled(double bearing, double across, double rise, double start) {
		if (across > QUILL_LENGTH || across < 0.5 || Math.abs(rise) >= 3) return false;
		double gap = 360.0 / QUILL_RAYS;
		double off = ((bearing - start) % gap + gap) % gap;
		double nearest = Math.min(off, gap - off);
		return across * Math.sin(Math.toRadians(nearest)) <= QUILL_WIDTH;
	}

	/** Whether it dives again: up long enough, or hurt enough since it rose. */
	public static boolean dives(int up, double hurt, double max) {
		return up >= MAX_SURFACED || hurt >= max * DIVE_HURT;
	}

	/** Whether the thrash at half health is due: not yet roused and worn down to half. */
	public static boolean thrashes(boolean roused, double health, double max) {
		return !roused && health <= max * 0.5;
	}

	/** Damage it takes: none under the sand, more while it lies open, less while it thrashes. */
	public static float hurt(float damage, boolean burrowed, int move) {
		if (burrowed) return 0;
		return damage * (float) switch (move) {
			case STUCK, CHOKING, BEACHED -> 1.5;
			case GEYSER, SINKHOLE, GULPED -> 1.25;
			case THRASH -> 0.5;
			default -> 1;
		};
	}
}
