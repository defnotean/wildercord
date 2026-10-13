package dev.wildercord.cast;

/**
 * How the Star-Eater fights: each of its moves, how long it winds up, how far it reaches and what it does, how much it wants each
 * with its prey where they stand, and the shapes its blows take. It never makes the same move twice running.
 * <p>
 * While its shard shield is up it throws its star shards (each one a chance to knock it back into the shield), drops out of the dark
 * onto its prey, and sends rings skimming out across the floor; from the second phase it stares and cages, and in the last it rains
 * shards from the glass and swallows the light. Broken open and sagging, it whips its tendrils about itself and calls home the
 * shards it shed. In its last phase it makes its moves in pairs, with shorter rests between.
 */
public final class StarEaterRules {
	private StarEaterRules() {}

	/** A fan of star shards spread before its eye, loosed one after another, each homing in: knock each back as it arrives, in time. */
	public static final int VOLLEY = 1;
	/** All its shards folded into one long spear before its eye, a glint along its aim, then thrown dead straight: step off the line. */
	public static final int LANCE = 2;
	/** It rises high with its shards in a crown, a mark following its prey, and drops onto it like a falling star: leave the mark once it stops. */
	public static final int PLUNGE = 3;
	/** Sunk low with its shards spun flat, it sends rings skimming out across the floor: jump the low ones, crouch under the high ones. */
	public static final int NOVA = 4;
	/** Its eye opened wide and a thread of light run to its prey, then a burning stare that turns after them: put a pillar between you, or outrun its turn. */
	public static final int GAZE = 5;
	/** Its shards leave it to circle its prey and close in on where they stood: slip out between them before the cage shuts. */
	public static final int CAGE = 6;
	/** Spikes raised and the stars in the glass flaring, shards fall straight down onto marks round each of its prey: step off the marks. */
	public static final int STARFALL = 7;
	/** Its eye gone black, it swallows the light and drags everything toward a grinding ring of shards under it: walk out against the pull. */
	public static final int HORIZON = 8;
	/**
	 * Broken open, it reaches for the shards it shed and calls them all home in straight lines, every one that gets back hastening
	 * its shield: keep off their paths, tread them to dust before it calls, or knock them into it as they pass.
	 */
	public static final int GATHER = 9;
	/** Broken open, its tendrils curled up and back, then whipped all the way round it: back out of the ring, then come in while they hang limp. */
	public static final int LASH = 10;
	/** Fallen to the floor after a plunge, its shards strewn about it, blades reach it through its shield: the opening. */
	public static final int GROUNDED = 11;
	/** How many moves of its own it has, ids 1 up to this. */
	public static final int OWN = 11;

	/** The moves it picks from when it is free to choose. */
	public static final int[] PICKS = {VOLLEY, LANCE, PLUNGE, NOVA, GAZE, CAGE, STARFALL, HORIZON, GATHER, LASH};

	/** What one star shard does, before the world's difficulty; each move's damage below is a share of it. */
	public static final double SHARD = 6;
	/** How long its shield takes to grow back once broken, by phase. */
	public static final int[] OPEN_TICKS = {0, 180, 160, 130};
	/** Its prey this far over the floor is up out of reach, on a pillar or the telescope. */
	public static final double UP = 2.5;

	/**
	 * The volley: its tell (shorter when it follows another move), the ticks between shards, its rest after; how fast the shards fly
	 * and what each does. It looses two more shards than its phase.
	 */
	public static final int VOLLEY_TELL = 14, VOLLEY_TELL_CHAINED = 10, VOLLEY_EVERY = 5, VOLLEY_RECOVER = 12;
	public static final double VOLLEY_SPEED = 0.45, VOLLEY_DAMAGE = 1.0;
	/**
	 * The lance: its tell, when its aim locks, how fast the spear flies, how wide it cuts and what it does. In its last phase it throws
	 * a second, re-aimed, after {@link #LANCE_RETELL}. Knocked back, the spear breaks the shield outright.
	 */
	public static final int LANCE_TELL = 16, LANCE_TELL_CHAINED = 11, LANCE_LOCK = 5, LANCE_RETELL = 10, LANCE_RECOVER = 14;
	public static final double LANCE_SPEED = 1.1, LANCE_HALF = 0.8, LANCE_DAMAGE = 1.5;
	/**
	 * The plunge: how long it rises, when the mark stops following, how long the drop takes; how high it rises over the floor and how
	 * far from its altar it will go; how wide the landing is and what it does; from the second phase the ring the landing sends out,
	 * how fast and far it rolls and what it does. In its last phase it bounces up into a second plunge after {@link #PLUNGE_RETELL}.
	 * Then it lies grounded a while, shorter in its last phase.
	 */
	public static final int PLUNGE_TELL = 16, PLUNGE_TELL_CHAINED = 12, PLUNGE_LOCK = 5, PLUNGE_FALL = 5, PLUNGE_RETELL = 12, GROUNDED_TICKS = 40,
		GROUNDED_TICKS_LAST = 30;
	public static final double PLUNGE_HEIGHT = 9, PLUNGE_MOST = 9, PLUNGE_RADIUS = 3, PLUNGE_DAMAGE = 1.6, WAVE_SPEED = 0.55, WAVE_REACH = 10,
		WAVE_DAMAGE = 0.8;
	/**
	 * The nova: its tell, the ticks between rings, its rest after; how far the rings roll, how high a low ring reaches, how low a high
	 * ring comes (a crouch passes under it) and how high it reaches, and what a ring does; how fast they roll, by phase.
	 */
	public static final int NOVA_TELL = 16, NOVA_TELL_CHAINED = 11, NOVA_GAP = 12, NOVA_RECOVER = 14;
	public static final double NOVA_REACH = 13, NOVA_LOW = 0.9, NOVA_DUCK = 1.6, NOVA_HIGH_TOP = 2.6, NOVA_DAMAGE = 1.0;
	public static final double[] NOVA_SPEED = {0, 0.45, 0.5, 0.6};
	/**
	 * The gaze: its tell, when the thread of light stops following, how long the stare burns, how often it sears, its rest after; how
	 * far it reaches, how wide it burns and how fast it turns after its prey (both more in its last phase), and what each searing does.
	 */
	public static final int GAZE_TELL = 14, GAZE_TELL_CHAINED = 10, GAZE_LOCK = 4, GAZE_TICKS = 40, GAZE_EVERY = 8, GAZE_RECOVER = 16;
	public static final double GAZE_REACH = 26, GAZE_HALF = 0.6, GAZE_HALF_LAST = 0.9, GAZE_TURN = 2.5, GAZE_TURN_LAST = 3.5, GAZE_DAMAGE = 0.5;
	/**
	 * The cage: its tell, when the ring stops following its prey, how long it takes to shut (quicker in its last phase, with more shards),
	 * its rest after; how wide the ring starts and what a shard does.
	 */
	public static final int CAGE_TELL = 14, CAGE_TELL_CHAINED = 10, CAGE_LOCK = 4, CAGE_SHUT = 18, CAGE_SHUT_LAST = 13, CAGE_RECOVER = 12;
	public static final double CAGE_RADIUS = 4, CAGE_DAMAGE = 1.0;
	/**
	 * The starfall: its tell, when the marks settle, how long the shards hang glinting before they fall, its rest after, and how many
	 * marks fall scattered besides one on each of its prey; how high the shards hang, how fast they fall, how far the scattered marks
	 * spread, how wide a landing is and what it does.
	 */
	public static final int STARFALL_TELL = 16, STARFALL_TELL_CHAINED = 11, STARFALL_LOCK = 5, STARFALL_HANG = 6, STARFALL_RECOVER = 24,
		STARFALL_SCATTER = 4;
	public static final double STARFALL_HEIGHT = 8, STARFALL_SPEED = 0.9, STARFALL_SPREAD = 6, STARFALL_RADIUS = 1.3, STARFALL_DAMAGE = 1.2;
	/**
	 * The horizon: its tell, how long it pulls, how often the ring under it grinds, its rest after; how far the pull reaches and how
	 * hard it drags, how wide the grinding ring is and what it does, and how wide the collapse at the end is and what it does.
	 */
	public static final int HORIZON_TELL = 14, HORIZON_TELL_CHAINED = 10, HORIZON_PULL = 36, GRIND_EVERY = 10, HORIZON_RECOVER = 20;
	public static final double HORIZON_RADIUS = 12, HORIZON_DRAG = 0.06, GRIND_RADIUS = 2.5, GRIND_DAMAGE = 0.5, COLLAPSE_RADIUS = 4,
		COLLAPSE_DAMAGE = 1.3;
	/**
	 * Calling its shards home: its tell, the longest they fly; how fast they come, what one does on the way, and how near a foot must
	 * come to a strewn shard to tread it to dust.
	 */
	public static final int GATHER_TELL = 12, GATHER_FLIGHT = 30;
	public static final double GATHER_SPEED = 0.6, GATHER_DAMAGE = 0.8, TREAD = 0.9;
	/**
	 * The lash: its tell, how long the whip takes to go round, the second tell in its last phase, and how long its tendrils hang limp
	 * after; how far they reach and what they do.
	 */
	public static final int LASH_TELL = 12, LASH_SPIN = 4, LASH_RETELL = 10, LASH_RECOVER = 18;
	public static final double LASH_REACH = 3.5, LASH_DAMAGE = 1.2;
	/** Its rest between moves, by phase; the rest after a pair; how often it makes a pair in its last phase; what a spell weighs. */
	public static final int[] REST = {0, 34, 26, 16};
	public static final int CHAIN_REST = 24;
	public static final double CHAIN_CHANCE = 0.5, SPELL_WEIGHT = 0.7;

	/** The bar's name for {@code move}. */
	public static String key(int move) {
		return switch (move) {
			case VOLLEY -> "volley";
			case LANCE -> "lance";
			case PLUNGE -> "plunge";
			case NOVA -> "nova";
			case GAZE -> "gaze";
			case CAGE -> "cage";
			case STARFALL -> "starfall";
			case HORIZON -> "horizon";
			case GATHER -> "gather";
			case LASH -> "lash";
			case GROUNDED -> "grounded";
			default -> "watch";
		};
	}

	/** The phase in which it learns {@code move}; never, for what isn't a move it picks. */
	public static int unlocks(int move) {
		return switch (move) {
			case VOLLEY, LANCE, PLUNGE, NOVA, GATHER, LASH -> 1;
			case GAZE, CAGE -> 2;
			case STARFALL, HORIZON -> 3;
			default -> Integer.MAX_VALUE;
		};
	}

	/** Whether it can pick {@code move} in {@code phase}. */
	public static boolean unlocked(int move, int phase) {
		return unlocks(move) <= phase;
	}

	/** Whether {@code move} is one it makes only broken open (the rest it makes only behind its shield). */
	public static boolean exposedOnly(int move) {
		return move == GATHER || move == LASH;
	}

	/** The tell of {@code move}, shorter when it follows another move in the same breath. */
	public static int tell(int move, boolean chained) {
		return switch (move) {
			case VOLLEY -> chained ? VOLLEY_TELL_CHAINED : VOLLEY_TELL;
			case LANCE -> chained ? LANCE_TELL_CHAINED : LANCE_TELL;
			case PLUNGE -> chained ? PLUNGE_TELL_CHAINED : PLUNGE_TELL;
			case NOVA -> chained ? NOVA_TELL_CHAINED : NOVA_TELL;
			case GAZE -> chained ? GAZE_TELL_CHAINED : GAZE_TELL;
			case CAGE -> chained ? CAGE_TELL_CHAINED : CAGE_TELL;
			case STARFALL -> chained ? STARFALL_TELL_CHAINED : STARFALL_TELL;
			case HORIZON -> chained ? HORIZON_TELL_CHAINED : HORIZON_TELL;
			case GATHER -> GATHER_TELL;
			case LASH -> LASH_TELL;
			default -> 0;
		};
	}

	/** How many ticks before the blow the aim of {@code move} stops following its prey (0: it doesn't). */
	public static int lock(int move) {
		return switch (move) {
			case LANCE, PLUNGE, STARFALL -> 5;
			case GAZE, CAGE -> 4;
			default -> 0;
		};
	}

	/** The least time between two of {@code move}, a third shorter in its last phase. */
	public static long cooldown(int move, int phase) {
		long r = switch (move) {
			case VOLLEY -> 60;
			case LANCE -> 80;
			case PLUNGE -> 160;
			case NOVA -> 120;
			case GAZE -> 220;
			case CAGE -> 180;
			case STARFALL -> 220;
			case HORIZON -> 300;
			case GATHER -> 50;
			case LASH -> 60;
			default -> 0;
		};
		return phase >= 3 ? r * 2 / 3 : r;
	}

	/**
	 * How much it wants each of its moves (by id) in {@code phase}, its shield up ({@code guarded}) or broken, its prey {@code distance}
	 * off across the floor and {@code height} over it, in its sight or not, with {@code strewn} of its shards lying about, {@code since}
	 * ticks (by id) after it last made each, {@code last} the move it made last. Shards for anyone it can see, the spear most for anyone keeping away; the plunge and the
	 * rings for anyone close under it; the stare for anyone in sight a way off and the cage for anyone keeping away; the starfall most
	 * for anyone up out of reach or hiding; the pull for anyone running. Broken open, the lash for anyone close and the call for its
	 * shards whenever any lie about. Nothing it made too lately or last of all, nothing it has yet to learn.
	 */
	public static double[] weights(int phase, boolean guarded, double distance, double height, boolean sight, int strewn, long[] since,
			int last) {
		double[] w = new double[OWN + 1];
		boolean up = height > UP;
		double d = distance;
		for (int m : PICKS) {
			if (m == last || !unlocked(m, phase) || exposedOnly(m) == guarded || since[m] < cooldown(m, phase)) {
				continue;
			}
			w[m] = switch (m) {
				case VOLLEY -> sight ? (d >= 5 ? 2.5 : 1.5) : 0.4;
				case LANCE -> !sight || d < 4 ? 0 : d >= 8 ? 3 : 2;
				case PLUNGE -> up ? 0.3 : d <= 5 ? 3 : d <= 12 ? 1.5 : 0.6;
				case NOVA -> d <= 7 ? 2.5 : d <= 11 ? 1.2 : 0;
				case GAZE -> !sight ? 0 : d >= 5 ? 2.5 : 1.2;
				case CAGE -> up ? 0.6 : d >= 6 ? 2.5 : 1.2;
				case STARFALL -> up || !sight ? 3 : 2;
				case HORIZON -> d >= 6 && d <= HORIZON_RADIUS ? 2.5 : d < 6 ? 1.5 : 0.6;
				case GATHER -> strewn <= 0 ? 0 : strewn >= 4 ? 3 : 2.5;
				case LASH -> d <= LASH_REACH + 1.5 ? 3 : 0;
				default -> 0;
			};
		}
		return w;
	}

	/** Its pick from {@link #PICKS} by {@link #weights} and what it made lately, {@code roll} 0-1; -1 for none. */
	public static int choose(MoveDeck deck, int phase, boolean guarded, double distance, double height, boolean sight, int strewn, long[] since,
			double roll) {
		double[] all = weights(phase, guarded, distance, height, sight, strewn, since, deck.last());
		double[] w = new double[PICKS.length];
		for (int i = 0; i < PICKS.length; i++) {
			w[i] = all[PICKS[i]];
		}
		return deck.pick(PICKS, w, roll);
	}

	/**
	 * In its last phase, what it follows {@code move} with in the same breath, its prey {@code distance} off, {@code roll} 0-1; -1 for
	 * nothing. Shards then the spear; the spear then a plunge on anyone near, or shards; rings then shards; the stare then the starfall;
	 * the cage then the spear through it; the starfall then the cage; the pull then the rings.
	 */
	public static int chain(int move, int phase, double distance, double roll) {
		if (phase < 3 || roll >= CHAIN_CHANCE) {
			return -1;
		}
		return switch (move) {
			case VOLLEY -> LANCE;
			case LANCE -> distance <= 8 ? PLUNGE : VOLLEY;
			case NOVA -> VOLLEY;
			case GAZE -> STARFALL;
			case CAGE -> LANCE;
			case STARFALL -> CAGE;
			case HORIZON -> NOVA;
			default -> -1;
		};
	}

	/** How many shards a volley looses in {@code phase}. */
	public static int volley(int phase) {
		return phase + 2;
	}

	/** The rings a nova sends out in {@code phase}, in order: true for a high one to crouch under, false for a low one to jump. */
	public static boolean[] rings(int phase) {
		return phase >= 3 ? new boolean[] {false, true, false} : phase == 2 ? new boolean[] {false, true} : new boolean[] {false};
	}

	/** How many shards a cage is made of in {@code phase}. */
	public static int cage(int phase) {
		return phase >= 3 ? 8 : 6;
	}

	/** How many shards it sheds when its shield breaks in {@code phase}. */
	public static int shed(int phase) {
		return 4 + phase;
	}

	/** How much sooner its shield grows back for each called shard that gets home, in {@code phase}. */
	public static int hasten(int phase) {
		return OPEN_TICKS[Math.max(1, Math.min(3, phase))] / 8;
	}

	/** How long it lies grounded after a plunge in {@code phase}. */
	public static int grounded(int phase) {
		return phase >= 3 ? GROUNDED_TICKS_LAST : GROUNDED_TICKS;
	}

	/** How fast its stare turns after its prey, in degrees a tick, in {@code phase}. */
	public static double gazeTurn(int phase) {
		return phase >= 3 ? GAZE_TURN_LAST : GAZE_TURN;
	}

	/** How wide its stare burns either side of its line in {@code phase}. */
	public static double gazeHalf(int phase) {
		return phase >= 3 ? GAZE_HALF_LAST : GAZE_HALF;
	}

	/** How long its cage takes to shut in {@code phase}. */
	public static int cageShut(int phase) {
		return phase >= 3 ? CAGE_SHUT_LAST : CAGE_SHUT;
	}

	/**
	 * Whether a nova ring rolling from {@code from} out to {@code to} this tick catches someone {@code distance} off with their feet
	 * {@code feet} and their head {@code top} over the floor: a low ring under anyone who didn't jump, a high one into anyone who didn't
	 * crouch.
	 */
	public static boolean skims(boolean high, double distance, double from, double to, double feet, double top) {
		if (distance <= from - 0.35 || distance > to + 0.35) {
			return false;
		}
		return high ? top > NOVA_DUCK && feet < NOVA_HIGH_TOP : feet > -1.5 && feet < NOVA_LOW;
	}

	/** Whether its stare, reaching {@code reach} and burning {@code half} either side, sears someone {@code along} its line and {@code across} it. */
	public static boolean burns(double along, double across, double reach, double half) {
		return along > 0 && along <= reach && across <= half;
	}

	/** Whether its landing from a plunge catches someone {@code dx},{@code dz} from where it lands and {@code height} over the floor there. */
	public static boolean lands(double dx, double dz, double height) {
		return dx * dx + dz * dz <= PLUNGE_RADIUS * PLUNGE_RADIUS && height > -1.5 && height < 3;
	}

	/** Whether a falling shard catches someone {@code dx},{@code dz} from where it lands and {@code height} over the floor there. */
	public static boolean fallsOn(double dx, double dz, double height) {
		return dx * dx + dz * dz <= STARFALL_RADIUS * STARFALL_RADIUS && height > -1 && height < 2.5;
	}

	/** How hard the horizon drags someone {@code distance} off toward it each tick. */
	public static double drag(double distance) {
		return distance > 1.5 && distance <= HORIZON_RADIUS ? HORIZON_DRAG : 0;
	}

	/** Whether the grinding ring under the horizon catches someone {@code distance} off and {@code height} over the floor. */
	public static boolean grinds(double distance, double height) {
		return distance <= GRIND_RADIUS && height > -1.5 && height < 2.5;
	}

	/** Whether the horizon's collapse catches someone {@code distance} off and {@code height} over the floor. */
	public static boolean collapses(double distance, double height) {
		return distance <= COLLAPSE_RADIUS && height > -1.5 && height < 3.5;
	}

	/** Whether its whipping tendrils reach someone {@code distance} off across the floor with their feet {@code height} over it. */
	public static boolean lashes(double distance, double height) {
		return distance <= LASH_REACH && height > -1 && height < 4.5;
	}

	/**
	 * Whether a sweep taking {@code spin} ticks to go all the way round has passed someone {@code offset} degrees round from where it
	 * began, {@code el} ticks in; its last tick takes in whatever is left.
	 */
	public static boolean swept(double offset, int el, int spin) {
		double off = ((offset % 360) + 360) % 360, from = (el - 1) * 360.0 / spin, to = el * 360.0 / spin;
		return el >= 1 && off >= from && (off < to || el >= spin);
	}

	/** How wide its cage still is {@code el} ticks after it began to shut over {@code shut} ticks. */
	public static double closing(int el, int shut) {
		return CAGE_RADIUS * Math.max(0, 1 - el / (double) Math.max(1, shut));
	}

	/** Its facing, {@code from} degrees, turned toward {@code to} by no more than {@code most}. */
	public static double turn(double from, double to, double most) {
		return from + Math.max(-most, Math.min(most, wrap(to - from)));
	}

	/** {@code angle} folded into -180 to 180. */
	public static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}

	/** What an offset {@code dx},{@code dz} from its altar is scaled by to keep it within {@code most}. */
	public static double within(double dx, double dz, double most) {
		double len = Math.sqrt(dx * dx + dz * dz);
		return len <= most ? 1 : most / len;
	}
}
