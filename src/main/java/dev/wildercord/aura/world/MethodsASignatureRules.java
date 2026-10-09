package dev.wildercord.aura.world;

/**
 * Pure geometry, timing and counterplay for the Tide, Iron and Dune signatures. Each is two drawn beats from a
 * planted Master, and the second beat's answer is the opposite of the first's, so standing still never answers both.
 * <ul>
 * <li>Tide, Undertow Ring: a wave breaks over a ring band (step inside it), then the undertow drags the inner circle
 * (step back out).</li>
 * <li>Iron, Anvil Verdict: a hammer falls on the spot the challenger stood on when it locked (sidestep), then the anvil
 * rings through the ground (be off it: jump).</li>
 * <li>Dune, Shifting Sands: a sand lane runs forward and blinds whoever it catches (sidestep), then the storm takes the
 * whole circle except the lane the sand has already left (step back into the pale lane).</li>
 * </ul>
 */
public final class MethodsASignatureRules {
	private MethodsASignatureRules() {}

	public static final double COST = 28, DAMAGE = 26, MAX_HEIGHT = 1.5, HIT_HEIGHT = 2.5;
	public static final int RECOVERY = 16, COOLDOWN = 150, WARNING_REFRESH = 3;

	// Tide: Undertow Ring.
	public static final int WAVE = 14, UNDERTOW = 28;
	public static final double WAVE_INNER = 2.5, WAVE_OUTER = 6.5, UNDERTOW_RADIUS = 3.5;
	// Iron: Anvil Verdict. The hammer's spot follows the challenger until LOCK ticks before it falls.
	public static final int HAMMER = 13, SHOCK = 26, HAMMER_LOCK = 6;
	public static final double HAMMER_RADIUS = 2, SHOCK_RADIUS = 5, AIRBORNE = .35;
	// Dune: Shifting Sands. The lane's aim follows the challenger until LOCK ticks before it runs.
	public static final int LANE = 13, STORM = 27, LANE_LOCK = 6, BLIND_TICKS = 40;
	public static final double LANE_REACH = 7, LANE_HALF_WIDTH = 1.1, STORM_RADIUS = 5.5;

	/** The tick each beat of this signature lands on, from the form's first tick. */
	public static int[] beats(MastersRules.Move kind) {
		return switch (kind) {
			case TIDE_UNDERTOW_RING -> new int[] {WAVE, UNDERTOW};
			case IRON_ANVIL_VERDICT -> new int[] {HAMMER, SHOCK};
			case DUNE_SHIFTING_SANDS -> new int[] {LANE, STORM};
			default -> new int[0];
		};
	}

	/** The whole form is the tell: the master commits until its last beat. */
	public static int tell(MastersRules.Move kind) {
		int[] beats = beats(kind);
		return beats.length == 0 ? 0 : beats[beats.length - 1];
	}

	/** One slot in four, at a fighting distance from which both answers can be reached in the warning time. */
	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		if (!MethodsAMasters.owns(school) || Math.floorMod(sequence, 4) != 1 || now < readyAt) return false;
		if (!Double.isFinite(aura) || aura < COST + MastersRules.GUARD_COST || aura > MastersRules.AURA_MAX) return false;
		if (!Double.isFinite(distance) || !Double.isFinite(height) || Math.abs(height) > MAX_HEIGHT) return false;
		return switch (school) {
			case MethodsAMasters.TIDE -> distance >= 2 && distance <= WAVE_OUTER - .5;
			case MethodsAMasters.IRON -> distance >= 1.5 && distance <= 8;
			default -> distance >= 1.5 && distance <= LANE_REACH - 1;
		};
	}

	/** Tide's first beat: the band from WAVE_INNER to WAVE_OUTER. Inside the inner circle is dry. */
	public static boolean wave(double radius, double height) {
		return finite(radius, height) && Math.abs(height) <= HIT_HEIGHT && radius >= WAVE_INNER && radius <= WAVE_OUTER;
	}

	/** Tide's second beat: the inner circle. Beyond it is safe. */
	public static boolean undertow(double radius, double height) {
		return finite(radius, height) && Math.abs(height) <= HIT_HEIGHT && radius >= 0 && radius <= UNDERTOW_RADIUS;
	}

	/** Iron's first beat, measured from the locked spot rather than the master. */
	public static boolean hammer(double dx, double dz, double height) {
		return finite(dx, dz) && Double.isFinite(height) && Math.abs(height) <= HIT_HEIGHT && dx * dx + dz * dz <= HAMMER_RADIUS * HAMMER_RADIUS;
	}

	/** Iron's second beat runs through the ground: anyone standing on it inside the ring, nobody in the air. */
	public static boolean shock(double radius, double heightAboveGround, boolean grounded) {
		return finite(radius, heightAboveGround) && grounded && heightAboveGround < AIRBORNE && radius >= 0 && radius <= SHOCK_RADIUS;
	}

	/** Dune's first beat: the forward lane from the planted origin along the locked aim. */
	public static boolean lane(double forward, double side, double height) {
		return finite(forward, side) && Double.isFinite(height) && Math.abs(height) <= HIT_HEIGHT
			&& forward >= 0 && forward <= LANE_REACH && Math.abs(side) <= LANE_HALF_WIDTH;
	}

	/** Dune's second beat: the whole circle except the spent lane. */
	public static boolean storm(double forward, double side, double height) {
		if (!finite(forward, side) || !Double.isFinite(height) || Math.abs(height) > HIT_HEIGHT) return false;
		return forward * forward + side * side <= STORM_RADIUS * STORM_RADIUS && !lane(forward, side, height);
	}

	/** Whether a challenger at this frame is struck by beat {@code beat} of {@code kind}. */
	public static boolean hits(MastersRules.Move kind, int beat, double forward, double side, double height, boolean grounded) {
		double radius = Math.sqrt(forward * forward + side * side);
		return switch (kind) {
			case TIDE_UNDERTOW_RING -> beat == 0 ? wave(radius, height) : beat == 1 && undertow(radius, height);
			// The hammer is measured from its locked spot by the executor; here the frame is already relative to it.
			case IRON_ANVIL_VERDICT -> beat == 0 ? hammer(forward, side, height) : beat == 1 && shock(radius, height, grounded);
			case DUNE_SHIFTING_SANDS -> beat == 0 ? lane(forward, side, height) : beat == 1 && storm(forward, side, height);
			default -> false;
		};
	}

	/** A hit from the sand lane blinds a player only as long as Dune's PvP cap allows. */
	public static int blind(boolean player, boolean boss) {
		return dev.wildercord.aura.DuneRules.blind(BLIND_TICKS, player, boss);
	}

	/** What a finished form leaves behind for a challenger, a test or a log: how many beats ran, landed and were evaded. */
	public record Receipt(MastersRules.Move kind, int beats, int landed, int evaded) {
		public static final Receipt NONE = new Receipt(null, 0, 0, 0);
		public Receipt {
			if (beats < 0 || landed < 0 || evaded < 0) throw new IllegalArgumentException("negative receipt");
		}
		public Receipt beat(int hit, int missed) { return new Receipt(kind, beats + 1, landed + hit, evaded + missed); }
		public boolean complete() { return kind != null && beats == MethodsASignatureRules.beats(kind).length; }
	}

	private static boolean finite(double a, double b) { return Double.isFinite(a) && Double.isFinite(b); }
}
