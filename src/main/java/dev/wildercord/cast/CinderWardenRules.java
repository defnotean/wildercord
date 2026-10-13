package dev.wildercord.cast;

/**
 * How the Cinder Warden fights (see {@link CinderWarden}): its moves, which phase each wakes in, how much it wants each one at
 * wherever its prey stands, the shape each one strikes, and what follows what. It never makes the same move twice running.
 * <p>
 * Its fists and chains are for anyone close; the seam it drags through the floor, the chain it casts and the slag it spews are for
 * anyone keeping away; the steam out of its back is for anyone behind it. Four of its moves leave it open to a reaction for a
 * moment after (steaming, pulled taut, fanned, gone cold and brittle), so its armour is played through the way it fights. Each move
 * has a tell of its own, harmless while it lasts, and a way out of it. In its last phase it chains its moves in pairs and rests less.
 */
public final class CinderWardenRules {
	private CinderWardenRules() {
	}

	/** Both fists clasped overhead while a ring glows on the floor before it; brought down in a burst of fire: back out of the ring. */
	public static final int HAMMERFALL = 1;
	/** Its wrist chains let out and dragged scraping round in a ring: jump the chain as it comes, or hug its legs inside the ring. */
	public static final int SWEEP = 2;
	/** A gauntlet dragged through the floor, a glowing seam running on along its aim; then the seam bursts, near end first: sidestep. */
	public static final int SEAM = 3;
	/** Hunched, its back plates lifting and hissing; steam blasts out of its back: get round to its front. It is left steaming. */
	public static final int VENT = 4;
	/** A wrist chain whirled overhead and cast along a line: sidestep. Caught, you're hauled to its feet; missed, it hauls itself after the hook. */
	public static final int HAUL = 5;
	/** Head thrown back and chest opened, it spews slag up over the hall to land on where everyone stands: keep moving. */
	public static final int SLAG = 6;
	/** Chest heaving as it draws the hall's air in, it breathes fire in a cone that turns after its prey: get behind it or wide of it. */
	public static final int BELLOWS = 7;
	/** It draws all its heat in, glowing white, then bursts it out across the whole hall: put a basalt pillar between you. */
	public static final int FLARE = 8;
	/** Its fists sunk in the floor after a hammerfall while it heaves at them. */
	public static final int STUCK = 9;
	/** Fouled in its own chain at the end of a haul that missed, pulled taut. */
	public static final int TANGLED = 10;
	/** Burned out after a flare, grey and cold and brittle. */
	public static final int SPENT = 11;
	/** Rocked back on its heels by a reaction that caught it mid-tell. */
	public static final int REEL = 12;
	/** Its moves, ids 1 up to this. */
	public static final int MOVES = 12;

	/** The moves it picks from when it is free to choose. */
	public static final int[] PICKS = {HAMMERFALL, SWEEP, SEAM, VENT, HAUL, SLAG, BELLOWS, FLARE};

	/** How far round from its face (degrees) its prey counts as at its back, and as before it. */
	public static final double BEHIND = 110, FRONT = 70;
	/** Most degrees it turns a tick while it tracks its prey through a tell, before the aim locks. */
	public static final float TURN = 20;
	/** Above this, its prey is up out of reach of its fists and the chain on the floor. */
	public static final double OUT_OF_REACH = 2.5;
	/** Lately this near, foes count toward the crowd round it. */
	public static final double CROWD = 8;
	/** In its last phase, how often a move runs on into another. */
	public static final double CHAIN_CHANCE = 0.55;

	/**
	 * The hammerfall: its tell (shorter run on from another move), when its aim locks, how far ahead of its feet the ring sits and how
	 * wide, how high the blow reaches, what it does, and how long its fists stay sunk after. From the second phase the blow sends a
	 * wall of fire rolling out along the floor, faster and further in the third: jump it.
	 */
	public static final int HAMMER_TELL = 16, HAMMER_TELL_CHAINED = 10, HAMMER_LOCK = 5, STUCK_TICKS = 28;
	public static final double HAMMER_AHEAD = 1.6, HAMMER_RADIUS = 3.8, HAMMER_HEIGHT = 2.5, HAMMER_DAMAGE = 12;
	public static final double WAVE_SPEED = 0.45, WAVE_SPEED_LAST = 0.6, WAVE_REACH = 9, WAVE_REACH_LAST = 11, WAVE_HEIGHT = 0.9, WAVE_DAMAGE = 6;
	/**
	 * The sweep: its tell, how long a full turn takes, the beat between turns (from the second phase it goes round once more, the other
	 * way) and its rest; the ring the chain scrapes (wider in the third phase), how high off the floor the chain rides, what it does.
	 */
	public static final int SWEEP_TELL = 14, SWEEP_TELL_CHAINED = 10, SWEEP_SPIN = 10, SWEEP_RETELL = 6, SWEEP_RECOVER = 8;
	public static final double SWEEP_INNER = 2.2, SWEEP_REACH = 7, SWEEP_REACH_LAST = 8.5, SWEEP_HEIGHT = 0.8, SWEEP_DAMAGE = 9;
	/**
	 * The seam: its tell, when its aim locks, how long the burst takes to run its length and the rest after; where along its aim the seam
	 * starts and ends, how wide and high it bursts, how fast, and what it does. In the third phase it drags three, fanned
	 * {@link #SEAM_FAN} degrees apart.
	 */
	public static final int SEAM_TELL = 16, SEAM_TELL_CHAINED = 10, SEAM_LOCK = 5, SEAM_RUN = 7, SEAM_RECOVER = 10;
	public static final double SEAM_FROM = 1, SEAM_TO = 13, SEAM_HALF = 1.1, SEAM_HEIGHT = 3, SEAM_SPEED = 2, SEAM_DAMAGE = 10, SEAM_FAN = 30;
	/**
	 * The vent: its tell, how long the steam blasts and its rest; how far the steam reaches out of its back and how wide it spreads
	 * (more of both from the second phase), what it does, and how long it is left steaming after.
	 */
	public static final int VENT_TELL = 14, VENT_BLAST = 6, VENT_RECOVER = 8, STEAM_TICKS = 100, STEAM_TICKS_LATER = 140;
	public static final double VENT_REACH = 7, VENT_REACH_LATER = 8, VENT_HALF = 55, VENT_HALF_LATER = 70, VENT_DAMAGE = 8;
	/**
	 * The haul: its tell, when its aim locks, how long the cast takes; where along its aim the chain bites and how wide; how hard whoever
	 * it catches is hauled (per block off, at most) and lifted, how long they fly before it strikes, how near they must be for the blow
	 * and what it does. Missed, the hook bites at the end and it hauls itself there, how fast and for how long at most, never further
	 * than {@link #HAUL_LEASH} from its altar, bowling over anyone in its way; it ends tangled, pulled taut a while.
	 */
	public static final int HAUL_TELL = 16, HAUL_TELL_CHAINED = 10, HAUL_LOCK = 5, HAUL_CAST = 4, HAUL_DRAG = 8, HAUL_PULL_MOST = 12;
	public static final int TANGLED_TICKS = 30, PULLED_TICKS = 60;
	public static final double HAUL_FROM = 1.5, HAUL_REACH = 12, HAUL_HALF = 1.0, HAUL_HEIGHT = 2.5, HAUL_TUG = 0.15, HAUL_TUG_MOST = 1.6;
	public static final double HAUL_LIFT = 0.32, HAUL_STRIKE = 3.5, HAUL_DAMAGE = 8, HAUL_PULL = 0.9, HAUL_LEASH = 11, HAUL_BOWL = 1.6, HAUL_BOWL_DAMAGE = 7;
	/**
	 * The slag: its tell and its rest; how far off it marks its prey, how many more marks fall scattered round each and how far, how long
	 * the slag is in the air, how wide a gob lands and what it does; then the pool it leaves, how long it burns, how wide, how often and
	 * how hard. In the third phase a second volley follows onto wherever they stand when the first lands.
	 */
	public static final int SLAG_TELL = 14, SLAG_RECOVER = 10, SLAG_SCATTER = 2, SLAG_FALL = 22, POOL_TICKS = 40, POOL_EVERY = 10;
	public static final double SLAG_RANGE = 20, SLAG_SPREAD = 3.5, SLAG_RADIUS = 1.6, SLAG_DAMAGE = 7, POOL_RADIUS = 1.2, POOL_DAMAGE = 2;
	/**
	 * The bellows: its tell, when its aim locks, how long it breathes and its rest; how far the fire reaches and how wide, how fast it turns after its prey
	 * (faster in the third phase), how often it catches the same one and how hard, and how long it is left fanned open after.
	 */
	public static final int BELLOWS_TELL = 16, BREATH_LOCK = 4, BREATH_TICKS = 24, BELLOWS_RECOVER = 10, BREATH_EVERY = 8, FANNED_TICKS = 80;
	public static final double BREATH_REACH = 9, BREATH_HALF = 28, BREATH_TURN = 3, BREATH_TURN_LAST = 5, BREATH_DAMAGE = 4;
	/**
	 * The flare: its tell, how long it gathers after, how long at least between flares; how far the burst reaches, what it does, how hard
	 * it throws; then how long it stands spent and how long it stays brittle.
	 */
	public static final int FLARE_TELL = 18, FLARE_GATHER = 22, FLARE_EVERY = 300, SPENT_TICKS = 50, BRITTLE_TICKS = 80;
	public static final double FLARE_RADIUS = 16, FLARE_DAMAGE = 14, FLARE_PUSH = 1.2;
	/** How long it reels when a reaction catches it mid-tell, and how long at least between reels. */
	public static final int REEL_TICKS = 24, REEL_EVERY = 200;
	/** The burst when it throws its fists down between phases: what it does, and how far. */
	public static final double SHIFT_DAMAGE = 5, SHIFT_REACH = 8;

	/** The phase {@code move} wakes in; its states are never picked. */
	public static int unlock(int move) {
		return switch (move) {
			case HAMMERFALL, SWEEP, SEAM, VENT -> 1;
			case HAUL, SLAG, BELLOWS -> 2;
			case FLARE -> 3;
			default -> Integer.MAX_VALUE;
		};
	}

	/** Whether it may pick {@code move} in {@code phase}. */
	public static boolean unlocked(int move, int phase) {
		return phase >= unlock(move);
	}

	/** {@code move}'s name in its text: {@code boss.wildercord.cinder_warden.move.<key>}. */
	public static String key(int move) {
		return switch (move) {
			case HAMMERFALL -> "hammerfall";
			case SWEEP -> "sweep";
			case SEAM -> "seam";
			case VENT -> "vent";
			case HAUL -> "haul";
			case SLAG -> "slag";
			case BELLOWS -> "bellows";
			case FLARE -> "flare";
			case STUCK -> "stuck";
			case TANGLED -> "tangled";
			case SPENT -> "spent";
			case REEL -> "reel";
			default -> "watching";
		};
	}

	/** {@code move}'s tell, shorter when it runs on from another. */
	public static int tell(int move, boolean chained) {
		return switch (move) {
			case HAMMERFALL -> chained ? HAMMER_TELL_CHAINED : HAMMER_TELL;
			case SWEEP -> chained ? SWEEP_TELL_CHAINED : SWEEP_TELL;
			case SEAM -> chained ? SEAM_TELL_CHAINED : SEAM_TELL;
			case VENT -> VENT_TELL;
			case HAUL -> chained ? HAUL_TELL_CHAINED : HAUL_TELL;
			case SLAG -> SLAG_TELL;
			case BELLOWS -> BELLOWS_TELL;
			case FLARE -> FLARE_TELL;
			default -> 0;
		};
	}

	/**
	 * How much it wants each move (by id) at its prey: {@code distance} off along the floor, {@code angle} degrees round from its face,
	 * {@code above} its feet; with {@code crowd} foes about it, its prey {@code idle} (barely moved lately), {@code sinceFlare} ticks since
	 * its last flare. Nothing or less: not now.
	 */
	public static double[] weights(int phase, double distance, double angle, double above, int crowd, boolean idle, long sinceFlare) {
		double[] w = new double[MOVES + 1];
		boolean high = above > OUT_OF_REACH;
		boolean front = Math.abs(wrap(angle)) <= FRONT;
		boolean back = behind(angle);
		int more = Math.max(0, Math.min(crowd, 4) - 1);
		// Its fists, for anyone close before it.
		if (!high) {
			w[HAMMERFALL] = distance <= 4.5 && front ? 3.0 : distance <= 6 ? 1.0 : 0;
		}
		// Its chains round it, for anyone in the ring, more for a crowd; hardly worth it at its feet.
		double reach = sweepReach(phase);
		if (!high && distance <= reach) {
			w[SWEEP] = distance < SWEEP_INNER ? 0.3 : 2.0 + 0.6 * more;
		}
		// The seam, for anyone keeping away along the floor; past where it can run, now and then anyway, to drive them back in.
		w[SEAM] = distance > SEAM_TO ? 0.3 : distance < 4 ? 0.4 : distance >= 7 ? 2.2 : 1.6;
		// The steam, for anyone at its back; now and then it vents anyway, to cool.
		w[VENT] = back && distance <= ventReach(phase) ? 3.5 : 0.35;
		// The chain cast: anyone a way off; anyone further still, it hauls itself toward.
		w[HAUL] = distance < 5 ? 0.2 : distance <= HAUL_REACH ? 2.2 : 1.4;
		// The slag, for anyone keeping away, up out of reach, or standing still.
		w[SLAG] = (high ? 2.2 : distance >= 6 ? 1.8 : 0.8) * (idle ? 1.8 : 1);
		// The breath, for anyone before it and in reach.
		w[BELLOWS] = distance > BREATH_REACH ? 0.3 : Math.abs(wrap(angle)) <= 60 ? 2.0 : 1.0;
		// The flare, once in a long while, more with a crowd about.
		w[FLARE] = sinceFlare >= FLARE_EVERY ? 1.2 + 0.4 * more : 0;
		for (int m = 1; m <= MOVES; m++) {
			if (!unlocked(m, phase)) {
				w[m] = 0;
			}
		}
		return w;
	}

	/** How much it wants to cast one of its spells: a phase's first spell is its lesson and comes early, then mostly at range. */
	public static double spellWeight(boolean taught, double distance) {
		return !taught ? 3.0 : distance > 5 ? 1.2 : 0.5;
	}

	/** Ticks of rest after {@code move} in {@code phase}, {@code roll} 0-1: less each phase, and little after it was left open. */
	public static int rest(int phase, int move, double roll) {
		if (move == STUCK || move == TANGLED || move == SPENT || move == REEL) {
			return 12;
		}
		double r = Math.max(0, Math.min(roll, 0.999));
		return phase >= 3 ? 14 + (int) (r * 10) : phase == 2 ? 24 + (int) (r * 12) : 30 + (int) (r * 15);
	}

	/**
	 * In its last phase, the move {@code move} runs on into, its prey {@code distance} off, {@code caught} by it ({@code roll} 0-1);
	 * -1 for none. Those that leave it open never run on.
	 */
	public static int chain(int move, int phase, double distance, boolean caught, double roll) {
		if (phase < 3 || roll >= CHAIN_CHANCE) {
			return -1;
		}
		return switch (move) {
			case HAUL -> caught ? HAMMERFALL : -1;
			case SWEEP -> distance >= 4 ? SEAM : HAMMERFALL;
			case SEAM -> distance >= 5 ? HAUL : -1;
			case SLAG -> SEAM;
			default -> -1;
		};
	}

	// ------------------------------------------------------------------ shapes

	/** {@code degrees} brought round into -180 to 180. */
	public static double wrap(double degrees) {
		double d = degrees % 360;
		if (d > 180) {
			d -= 360;
		} else if (d <= -180) {
			d += 360;
		}
		return d;
	}

	/** Whether something {@code angle} degrees round from its face is at its back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= BEHIND;
	}

	/** Whether a spot {@code forward} ahead of its feet and {@code side} across, {@code above} them, is under the hammerfall. */
	public static boolean hammerHits(double forward, double side, double above) {
		double f = forward - HAMMER_AHEAD;
		return f * f + side * side <= HAMMER_RADIUS * HAMMER_RADIUS && above > -1 && above <= HAMMER_HEIGHT;
	}

	/** Whether the wall of fire, spreading from {@code from} to {@code to} out this tick, runs over someone {@code distance} out and {@code above}. */
	public static boolean crosses(double distance, double from, double to, double above) {
		return distance > from - 0.4 && distance <= to + 0.4 && above < WAVE_HEIGHT && above > -1;
	}

	/** Whether a spot {@code distance} out lies in a ring from {@code inner} to {@code outer}. */
	public static boolean inRing(double distance, double inner, double outer) {
		return distance >= inner && distance <= outer;
	}

	/**
	 * Whether the chain, swinging from {@code from} to {@code to} degrees this tick (either way, any number of turns in), passes over
	 * something at {@code bearing} degrees.
	 */
	public static boolean swept(double bearing, double from, double to) {
		double lo = Math.min(from, to), hi = Math.max(from, to);
		if (hi - lo >= 360) {
			return true;
		}
		double k = ((bearing - lo) % 360 + 360) % 360;
		return k <= hi - lo;
	}

	/** Whether a spot {@code along} its aim and {@code side} across lies in a lane from {@code from} to {@code to}, {@code half} wide each way. */
	public static boolean inLane(double along, double side, double from, double to, double half) {
		return along >= from && along <= to && Math.abs(side) <= half;
	}

	/** Whether a spot {@code along} its aim and {@code side} across lies in a cone {@code reach} long, {@code half} degrees each way. */
	public static boolean inCone(double along, double side, double reach, double half) {
		if (along <= 0 || along * along + side * side > reach * reach) {
			return false;
		}
		return Math.toDegrees(Math.atan2(Math.abs(side), along)) <= half;
	}

	/** Whether the vent's steam out of its back reaches a spot {@code along} its facing and {@code side} across, in {@code phase}. */
	public static boolean vented(double along, double side, int phase) {
		return inCone(-along, side, ventReach(phase), phase >= 2 ? VENT_HALF_LATER : VENT_HALF);
	}

	/** The degrees off its aim each seam runs in {@code phase}: one straight on, three fanned in the last. */
	public static double[] seams(int phase) {
		return phase >= 3 ? new double[] {-SEAM_FAN, 0, SEAM_FAN} : new double[] {0};
	}

	/** How hard whoever the haul catches {@code distance} off is hauled in: enough to land them about at its feet. */
	public static double tug(double distance) {
		return Math.max(0.3, Math.min(HAUL_TUG_MOST, HAUL_TUG * (distance - 1.5)));
	}

	/** Whether the flare's burst catches someone {@code distance} off, {@code sheltered} behind stone or not. */
	public static boolean flared(double distance, boolean sheltered) {
		return !sheltered && distance <= FLARE_RADIUS;
	}

	/** How far its chains reach in a sweep in {@code phase}. */
	public static double sweepReach(int phase) {
		return phase >= 3 ? SWEEP_REACH_LAST : SWEEP_REACH;
	}

	/** How far its steam reaches in {@code phase}. */
	public static double ventReach(int phase) {
		return phase >= 2 ? VENT_REACH_LATER : VENT_REACH;
	}

	/** How fast the wall of fire from a hammerfall rolls out in {@code phase}, in blocks a tick. */
	public static double waveSpeed(int phase) {
		return phase >= 3 ? WAVE_SPEED_LAST : WAVE_SPEED;
	}

	/** How far the wall of fire from a hammerfall rolls out in {@code phase}. */
	public static double waveReach(int phase) {
		return phase >= 3 ? WAVE_REACH_LAST : WAVE_REACH;
	}

	/** How many degrees a tick it turns after its prey while it breathes, in {@code phase}. */
	public static double breathTurn(int phase) {
		return phase >= 3 ? BREATH_TURN_LAST : BREATH_TURN;
	}
}
