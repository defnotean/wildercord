package dev.wildercord.aura.world;

/**
 * The opt-in Masters of Tomorrow trials. These are starting tuning values, not a claim that a fight has been measured as
 * twenty times harder than a vanilla boss. Difficulty comes from committed patterns and punishable mistakes; it needs playtesting.
 */
public final class MastersRules {
	private MastersRules() {}

	public static final int EMBER = 0, GALE = 1, STONE = 2;
	public static final int MAX_PARTICIPANTS = 8;
	public static final int MAX_ENCOUNTERS = 8;
	public static final int PREPARE_TICKS = 600;
	public static final int INTRODUCTION_TICKS = 200, WAITING_TICKS = 1200, ENTRY_CIRCLE = 8;
	public static final int ABANDON_TICKS = 200;
	public static final int ENCOUNTER_TICKS = 20 * 60 * 20;
	public static final double ARENA_RADIUS = 24.0;
	public static final double PROJECTILE_RANGE = ARENA_RADIUS * 2 + 4;
	public static final double HEALTH = 480.0;
	public static final double ARMOUR = 12.0;
	public static final int AIM_LOCK = 6;
	public static final int CUT_REST = 30;
	public static final int CUT_TELL = 6;
	public static final double CUT_RANGE = 3.5;
	public static final double CUT_FRONT = 0.5;
	public static final double CUT_INCOMING = 0.6;

	public static final double AURA_MAX = 100, ATTACK_COST = 16, GUARD_COST = 12, CUT_COST = 8, REDIRECT_COST = 20, DODGE_COST = 18;
	public static final int BREATH_TICKS = 60, REDIRECT_REST = 100, DODGE_REST = 80, DODGE_TICKS = 4;

	/** A reservation already occupies its slot; opening its lobby must not count as a ninth encounter. */
	public static boolean canAdmitEncounter(int activeCount, boolean alreadyRegistered) {
		return alreadyRegistered || activeCount < MAX_ENCOUNTERS;
	}

	public static int cutBudget(int count) {
		return Math.min(3, 1 + (participants(count) - 1) / 2);
	}

	/** A complete windup always precedes harm, and every attack has a guaranteed recovery. */
	public enum Move {
		SWEEP(18, 20, 30), THRUST(22, 24, 42), CRESCENT(20, 24, 36), BREAK_CAST(20, 24, 28),
		CINDER_WAKE(EmberWakeRules.TELL, EmberWakeRules.RECOVERY, EmberWakeRules.CUT_DAMAGE),
		PURSUIT_BREAK(MasterPursuitRules.TELL, MasterPursuitRules.RECOVERY, MasterPursuitRules.DAMAGE),
		CROSSWIND_REPRISE(GaleRepriseRules.TELL, GaleRepriseRules.RECOVERY, GaleRepriseRules.DAMAGE),
		STONE_FRACTURE(StoneFractureRules.TELL, StoneFractureRules.RECOVERY, StoneFractureRules.DAMAGE),
		KILN_RING(EmberKilnRules.TELL, EmberKilnRules.RECOVERY, EmberKilnRules.DAMAGE),
		STONE_FAULT_MARCH(StoneMarchRules.TELL, StoneMarchRules.END - StoneMarchRules.TELL, StoneMarchRules.DAMAGE);

		public final int tell, recovery;
		public final double damage;

		Move(int tell, int recovery, double damage) {
			this.tell = tell;
			this.recovery = recovery;
			this.damage = damage;
		}
	}

	private static final Move[][] PATTERNS = {
		{Move.SWEEP, Move.CRESCENT, Move.THRUST, Move.SWEEP},
		{Move.CRESCENT, Move.THRUST, Move.CRESCENT, Move.SWEEP},
		{Move.THRUST, Move.SWEEP, Move.THRUST, Move.CRESCENT}
	};

	public static int discipline(int value) {
		return Math.max(EMBER, Math.min(STONE, value));
	}

	public static int participants(int count) {
		return Math.max(1, Math.min(MAX_PARTICIPANTS, count));
	}

	/** Locked when the preparation ends; adding bystanders never changes an encounter. */
	public static double health(int count) {
		return HEALTH * (1 + 0.55 * (participants(count) - 1));
	}

	public static double damage(int count, int discipline, Move move) {
		return move.damage * (discipline(discipline) == STONE ? 1.1 : 1);
	}

	public static double postureMultiplier(int count) {
		return 1 + 0.25 * (participants(count) - 1);
	}

	/** More challengers add visible attack lanes. One volley may still damage each player only once. */
	public static java.util.List<Double> volleyAngles(int count) {
		return switch (participants(count)) {
			case 1 -> java.util.List.of(0.0);
			case 2 -> java.util.List.of(-8.0, 8.0);
			case 3 -> java.util.List.of(-12.0, 0.0, 12.0);
			case 4 -> java.util.List.of(-18.0, 0.0, 18.0);
			default -> java.util.List.of(-28.0, -14.0, 0.0, 14.0, 28.0);
		};
	}

	/** Elevated/pillar targets use the same telegraphed linear projectile; they never demand impossible ground navigation. */
	public static boolean needsCrescent(double distance, double height) {
		return distance > 6 || Math.abs(height) > 2.5;
	}

	/** Phase changes reorder the pattern, rather than shortening a previously learned tell. */
	public static Move move(int discipline, int sequence, int phase, double distance) {
		if (distance > 6) {
			return Move.CRESCENT;
		}
		Move[] pattern = PATTERNS[discipline(discipline)];
		return pattern[Math.floorMod(sequence + Math.max(0, Math.min(2, phase)), pattern.length)];
	}

	public static int phase(double health, double maximum) {
		if (!Double.isFinite(health) || !Double.isFinite(maximum) || maximum <= 0) {
			return 0;
		}
		return health / maximum <= 0.33 ? 2 : health / maximum <= 0.66 ? 1 : 0;
	}

	/** Stone braces after each attack; other schools after two. A guard is never raised during recovery. */
	public static boolean guardAfter(int discipline, int attacks) {
		return attacks > 0 && (discipline(discipline) == STONE || attacks % 2 == 0);
	}

	/** A held frontal blade can sever one approaching bolt; motionless, departing and rear bolts cannot be cut. */
	public static boolean canCut(long now, long raised, long ready, double distance, double facing, double incoming) {
		return raised >= 0 && now >= raised + CUT_TELL && now >= ready
			&& Double.isFinite(distance) && distance >= 0 && distance <= CUT_RANGE
			&& Double.isFinite(facing) && facing >= CUT_FRONT && Double.isFinite(incoming) && incoming >= CUT_INCOMING;
	}

	/** A spell may punish an early tell, while the final locked six ticks remain a committed attack. */
	public static boolean interruptible(boolean winding, long now, long releaseAt) {
		return winding && now < releaseAt - AIM_LOCK;
	}

	/** The locked line is authoritative, so sidestepping after the tell begins works. */
	public static boolean hits(Move move, double forward, double side, double height) {
		if (!Double.isFinite(forward) || !Double.isFinite(side) || !Double.isFinite(height) || Math.abs(height) > 2.5) {
			return false;
		}
		return switch (move) {
			case SWEEP, CINDER_WAKE -> forward >= 0 && forward <= 4 && Math.abs(side) <= Math.min(3.0, 0.8 + forward * 0.8);
			case THRUST -> forward >= 0 && forward <= 6 && Math.abs(side) <= 0.8;
			case BREAK_CAST -> forward >= 0 && forward <= 4 && Math.abs(side) <= 0.8;
			case PURSUIT_BREAK -> MasterPursuitRules.hits(forward, side, height);
			case CROSSWIND_REPRISE -> GaleRepriseRules.hits(forward, side, height);
			case STONE_FRACTURE -> StoneFractureRules.hits(forward, side, height);
			case KILN_RING -> EmberKilnRules.hits(forward, side, height);
			case STONE_FAULT_MARCH -> StoneMarchRules.band(forward, side, height) >= 0;
			case CRESCENT -> false; // The shared Crescents flight owns collision.
		};
	}
}
