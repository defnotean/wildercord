package dev.wildercord.cast;

/**
 * How the Root Guardian fights between its spells: what it reaches for where its prey stands, how long each move warns before it
 * lands, how long it rests after, and the shapes its roots take on the floor. It drives roots up under everyone's feet, lashes out
 * with the lowest of its own bindings, lobs seed pods that burst into briars, gushes sap down a lane, sends a root burrowing after
 * whoever keeps away, bursts thorns out along spokes all round it, tears its legs out of the floor and charges, shakes a pollen
 * cloud out of its crown, and at the last brings the canopy down. Kept free of the world so it can be tested.
 */
public final class RootGuardianRules {
	private RootGuardianRules() {}

	/** Its own moves. */
	public static final int SPEARS = 1, LASH = 2, SEEDS = 3, SAP = 4, CREEP = 5, THORNS = 6, UPROOT = 7, POLLEN = 8, CANOPY = 9;
	/** What its moves leave it in: a binding snagged in the floor, tangled in its own roots, yanking a leg back out, thrown back with its heart bare. */
	public static final int SNAGGED = 10, TANGLED = 11, STAGGER = 12, REEL = 13, POSES = 13;
	/** What it picks among. */
	public static final int[] PICKS = {SPEARS, LASH, SEEDS, SAP, CREEP, THORNS, UPROOT, POLLEN, CANOPY};
	private static final String[] KEYS = {"", "spears", "lash", "seeds", "sap", "creep", "thorns", "uproot", "pollen", "canopy", "snagged", "tangled", "stagger", "reel"};

	/** Behind it, and before it, in degrees off its facing; how far it turns a tick through a tell; who counts as crowding it. */
	public static final double BEHIND = 120, FRONT = 75, TURN = 18, CROWD = 5;
	/** How often a move in its last phase runs straight on into another. */
	public static final double CHAIN_CHANCE = 0.5;
	/** What reaches its heart through its bindings, and through them strained when it's tangled in its own roots. */
	public static final float BOUND_TAKES = 0.35F, TANGLED_TAKES = 0.7F;
	public static final int REEL_TICKS = 30;
	/** How far the stamp that ends its change of phase throws everyone back, and how hard. */
	public static final double SHIFT_REACH = 5, SHIFT_DAMAGE = 4;

	// Root spears: roots driven up under every foe's feet.
	public static final int SPEARS_TELL = 16, SPEARS_CHAINED = 11, SPEARS_LOCK = 7, SPEARS_SECOND = 12, SPEARS_RECOVER = 16;
	public static final double SPEARS_RADIUS = 1.2, SPEARS_HEIGHT = 2.5, SPEARS_DAMAGE = 8;
	public static final int SPEARS_MOST = 4;
	// The lash: its lowest binding uncoiled into a whip and swung across before it.
	public static final int LASH_TELL = 14, LASH_CHAINED = 10, LASH_LOCK = 4, LASH_BACK = 9, LASH_RECOVER = 6, SNAG_TICKS = 40;
	public static final double LASH_REACH = 5.5, LASH_HALF = 80, LASH_HEIGHT = 2.5, LASH_DAMAGE = 8, LASH_SHOVE = 0.7;
	// Seed pods: lobbed from its crown, bursting into briars where they land.
	public static final int SEEDS_TELL = 16, SEEDS_CHAINED = 11, SEEDS_LOCK = 4, SEEDS_FLIGHT = 12, SEEDS_RECOVER = 10;
	public static final double SEEDS_NEAR = 3.5, SEEDS_FAR = 9, SEEDS_RADIUS = 1.4, SEEDS_DAMAGE = 5;
	public static final int BRIAR_TICKS = 100, BRIAR_EVERY = 10, BRIAR_MOST = 6;
	public static final double BRIAR_DAMAGE = 2;
	// Sap: gushed out of its heart down a lane, holding whoever it washes over, left lying as a sticky pool.
	public static final int SAP_TELL = 15, SAP_CHAINED = 10, SAP_LOCK = 5, SAP_RUN = 5, SAP_RECOVER = 8, SAP_HOLD = 30, POOL_TICKS = 120, POOL_MOST = 2;
	public static final double SAP_SPEED = 2, SAP_FROM = 1, SAP_TO = 10, SAP_HALF = 1.0, SAP_DAMAGE = 4, SCORCH_DAMAGE = 3;
	// The creeping root: burrowed out from a planted leg after whoever keeps away.
	public static final int CREEP_TELL = 16, CREEP_CHAINED = 11, CREEP_TICKS = 50, DRAG_MOST = 24, STAGGER_TICKS = 28, CREEP_RECOVER = 8;
	public static final double CREEP_SPEED = 0.32, CREEP_TURN = 6, CREEP_CATCH = 1.0, CREEP_DAMAGE = 5, DRAG_SPEED = 0.45, DRAG_NEAR = 2.2;
	// Thorns: burst out along spokes all round it.
	public static final int THORNS_TELL = 16, THORNS_CHAINED = 11, THORNS_SPOKES = 6, THORNS_RUN = 7, THORNS_SECOND = 10, THORNS_RECOVER = 10;
	public static final double THORNS_HALF = 0.6, THORNS_SPEED = 1.2, THORNS_FROM = 1, THORNS_REACH = 9, THORNS_HEIGHT = 2, THORNS_DAMAGE = 7;
	// Uprooting: its legs torn out of the floor, and a charge down a lane.
	public static final int UPROOT_TELL = 18, UPROOT_CHAINED = 12, UPROOT_LOCK = 5, CHARGE_TICKS = 20, RETELL = 10, TANGLED_TICKS = 45, TANGLED_WALL = 70;
	public static final double CHARGE_SPEED = 0.45, CHARGE_HALF = 1.3, CHARGE_HOME = 16, TRAMPLE_DAMAGE = 9, TRAMPLE_SHOVE = 0.9;
	// Pollen: a golden cloud shaken out of its crown, that flashes into flame at a spark.
	public static final int POLLEN_TELL = 14, POLLEN_CHAINED = 10, POLLEN_RECOVER = 10, CLOUD_EVERY = 20;
	public static final double CLOUD_RADIUS = 4, CLOUD_DAMAGE = 1, FLASH_DAMAGE = 5;
	// The canopy: boughs brought crashing down on shadows round everyone.
	public static final int CANOPY_TELL = 18, CANOPY_CHAINED = 12, CANOPY_LOCK = 10, CANOPY_EVERY = 4, CANOPY_RECOVER = 22;
	public static final double CANOPY_RADIUS = 1.7, CANOPY_DAMAGE = 9;

	/** Its move's name in its lang keys. */
	public static String key(int move) {
		return move > 0 && move < KEYS.length ? KEYS[move] : "";
	}

	/** The phase a move first comes in. */
	public static int wakes(int move) {
		return switch (move) { case THORNS, UPROOT, POLLEN -> 2; case CANOPY -> 3; default -> 1; };
	}

	/** How long {@code move} warns before it lands; shorter run on from another. */
	public static int tell(int move, boolean chained) {
		return switch (move) {
			case SPEARS -> chained ? SPEARS_CHAINED : SPEARS_TELL;
			case LASH -> chained ? LASH_CHAINED : LASH_TELL;
			case SEEDS -> chained ? SEEDS_CHAINED : SEEDS_TELL;
			case SAP -> chained ? SAP_CHAINED : SAP_TELL;
			case CREEP -> chained ? CREEP_CHAINED : CREEP_TELL;
			case THORNS -> chained ? THORNS_CHAINED : THORNS_TELL;
			case UPROOT -> chained ? UPROOT_CHAINED : UPROOT_TELL;
			case POLLEN -> chained ? POLLEN_CHAINED : POLLEN_TELL;
			case CANOPY -> chained ? CANOPY_CHAINED : CANOPY_TELL;
			default -> 0;
		};
	}

	/** How long its cloud of pollen hangs, by phase. */
	public static int cloudTicks(int phase) {
		return phase >= 3 ? 100 : 80;
	}

	/** How many seed pods it lobs, by phase. */
	public static int pods(int phase) {
		return 2 + phase;
	}

	/** Ticks before it picks again after {@code move}: a long breath after its heaviest, little after being thrown off one, less each phase. */
	public static int rest(int phase, int move, double roll) {
		double base = move == CANOPY || move == UPROOT ? 30 : move == REEL ? 10 : move > PICKS.length ? 8 : 20;
		return (int) Math.round(base * (phase >= 3 ? 0.6 : phase == 2 ? 0.85 : 1) + roll * 10);
	}

	/** What share of a blow reaches its heart. */
	public static float taken(int bindings, boolean tangled) {
		return bindings <= 0 ? 1 : tangled ? TANGLED_TAKES : BOUND_TAKES;
	}

	/**
	 * How much it wants each move (indexed by move) with its prey {@code distance} away, {@code angle} degrees off its facing and
	 * {@code height} above its feet; {@code crowd} foes close round it, {@code foes} in its arena, its cloud already out or not, and
	 * {@code briars} patches already lying about.
	 */
	public static double[] weights(int phase, double distance, double angle, double height, int bindings, int crowd, int foes, boolean clouded, int briars) {
		double[] w = new double[PICKS.length + 1];
		boolean high = height > 2.5, behind = Math.abs(angle) > BEHIND, front = Math.abs(angle) <= FRONT;
		int more = Math.max(0, crowd - 1);
		w[SPEARS] = 1 + (distance > 8 ? 0.3 : 0);
		w[LASH] = bindings > 0 && distance <= 6 && !high ? front ? 2.2 : 0.6 : 0;
		w[SEEDS] = (distance >= 5 && distance <= SEEDS_FAR ? 1.6 : distance > SEEDS_FAR ? 0.6 : distance >= 3 ? 0.5 : 0.15) * (briars >= 4 ? 0.3 : 1);
		w[SAP] = distance >= 3 && distance <= 11 ? 1.5 : distance > 11 ? 0.7 : 0.3;
		w[CREEP] = high ? 0 : distance >= 5 ? 1.4 + (distance > 9 ? 0.4 : 0) : distance >= 3.5 ? 0.5 : 0;
		w[THORNS] = distance <= 4 ? 1.4 + (behind ? 1 : 0) + 0.4 * more : distance <= 7 ? 0.4 : 0;
		w[UPROOT] = high ? 0 : distance >= 4 && distance <= 10 ? 1.5 : distance < 4 ? 0.2 : 0.6;
		w[POLLEN] = clouded ? 0 : distance <= 5 ? 1.2 + 0.4 * more : 0.2;
		w[CANOPY] = 0.9 + 0.35 * Math.max(0, foes - 1);
		for (int m : PICKS) if (wakes(m) > phase) w[m] = 0;
		return w;
	}

	/** How much it wants one of its spells: a little, more when its prey keeps far off. */
	public static double spellWeight(double distance) {
		return 0.3 + (distance > 9 ? 0.25 : 0);
	}

	/**
	 * In its last phase, what {@code move} may run straight on into, or -1: roots up after a lash, the lash after a drag, a charge after
	 * the sap that held them. Never the same move twice, never the lash with no binding left to swing.
	 */
	public static int chain(int phase, int move, double distance, int bindings, double roll) {
		if (phase < 3 || roll >= CHAIN_CHANCE) return -1;
		int next = switch (move) {
			case SPEARS -> distance <= 5 ? LASH : SEEDS;
			case LASH -> distance <= 4 ? THORNS : SPEARS;
			case SEEDS -> CREEP;
			case SAP -> UPROOT;
			case CREEP -> LASH;
			case THORNS -> SPEARS;
			case POLLEN -> THORNS;
			default -> -1;
		};
		if (next == LASH && bindings <= 0) next = SPEARS;
		return next == move ? -1 : next;
	}

	// ------------------------------------------------------------------ the shapes on the floor

	/** An angle brought into -180 to 180. */
	public static double wrap(double degrees) {
		return ((degrees % 360) + 540) % 360 - 180;
	}

	/** Whether a spot {@code forward} along its facing and {@code side} across lies in an arc {@code reach} long and {@code half} degrees each way. */
	public static boolean inArc(double forward, double side, double reach, double half) {
		return Math.hypot(forward, side) <= reach && Math.abs(Math.toDegrees(Math.atan2(side, forward))) <= half;
	}

	/** Whether a spot lies on a lane from {@code from} to {@code to} out along its facing, {@code half} wide each way. */
	public static boolean onLane(double forward, double side, double from, double to, double half) {
		return forward >= from && forward <= to && Math.abs(side) <= half;
	}

	/**
	 * How far out along one of {@code spokes} spokes a spot lies (the first spoke {@code offset} degrees off its facing, toward its
	 * side), if it lies within {@code half} of one; -1 if it lies between them.
	 */
	public static double spoke(double forward, double side, int spokes, double offset, double half) {
		for (int i = 0; i < spokes; i++) {
			double a = Math.toRadians(offset + i * 360.0 / spokes), along = forward * Math.cos(a) + side * Math.sin(a);
			if (along >= 0 && Math.abs(-forward * Math.sin(a) + side * Math.cos(a)) <= half) return along;
		}
		return -1;
	}

	/** Where a front running out from {@code from} at {@code speed} a tick has reached {@code t} ticks after it set off. */
	public static double front(int t, double speed, double from) {
		return from + t * speed;
	}

	/** Whether a front moving from {@code was} to {@code now} passed {@code distance} this tick. */
	public static boolean crossed(double was, double now, double distance) {
		return distance > was && distance <= now;
	}

	/** A heading turned toward {@code toward} by at most {@code most} degrees. */
	public static double steer(double heading, double toward, double most) {
		return wrap(heading + Math.max(-most, Math.min(most, wrap(toward - heading))));
	}

	/** How high a lobbed pod rides at {@code f} of its flight (0 to 1), as a share of its peak. */
	public static double lob(double f) {
		return 4 * f * (1 - f);
	}

	/** How far out a pod aimed at a foe {@code distance} away lands: never close in under its crown, never past its reach. */
	public static double podReach(double distance) {
		return Math.max(SEEDS_NEAR, Math.min(SEEDS_FAR, distance));
	}
}
