package dev.wildercord.trials;

/** Pure rules, timings, scaling, and geometry for Nyxara, the Eclipse Sovereign. */
public final class NyxaraRules {
	private NyxaraRules() {}

	public static final int IDLE = 0;
	public static final int STRIDE = 1;
	public static final int CRESCENT_SEVER = 2;
	public static final int PRISM_CONVERGENCE = 3;
	public static final int TOTAL_ECLIPSE = 4;

	public static final int MAX_PARTY_PLAYERS = VaelkhorRules.MAX_PARTY_PLAYERS;
	public static final double ARENA_RADIUS = VaelkhorRules.ARENA_RADIUS;
	public static final double ARENA_Y_MIN = VaelkhorRules.ARENA_Y_MIN;
	public static final double ARENA_Y_MAX = VaelkhorRules.ARENA_Y_MAX;

	public static final double BASE_HEALTH = 420.0;
	public static final double HEALTH_PER_EXTRA_PLAYER = 190.0;

	public static final int SEVER_TELL_TICKS = 13;
	public static final int SEVER_ACTIVE_END_TICKS = 20;
	public static final int SEVER_DURATION_TICKS = 30;
	public static final double SEVER_REACH = 6.8;
	public static final double SEVER_HALF_ANGLE_RAD = Math.toRadians(74.0);
	public static final float SEVER_DAMAGE_P1 = 14.0F;
	public static final float SEVER_DAMAGE_P2 = 17.0F;

	public static final int PRISM_WINDOW_TICKS = 100;
	public static final int STAGGER_TICKS = 55;
	public static final float STAGGER_DAMAGE_MULTIPLIER = 1.35F;
	public static final float PRISM_PULSE_DAMAGE = 10.5F;

	public static final int ECLIPSE_TELL_TICKS = 40;
	public static final int ECLIPSE_RECOVERY_TICKS = 16;
	public static final double SOAK_RADIUS = 3.2;
	public static final float ECLIPSE_SOAKED_DAMAGE = 5.0F;
	public static final float ECLIPSE_UNSOAKED_DAMAGE = 19.0F;

	public static final float PHASE_TWO_RATIO = 0.60F;
	public static final int QUIET_RESET_TICKS = 120;

	public static final int[][] PRISM_OFFSETS = VaelkhorRules.CONDUIT_OFFSETS;
	public static final double[][] ECLIPSE_SOAK_OFFSETS = VaelkhorRules.SOAK_OFFSETS;

	public static double maxHealthFor(int challengers) {
		return maxHealthFor(challengers, false);
	}

	public static double maxHealthFor(int challengers, boolean apex) {
		int extra = Math.clamp(challengers - 1, 0, MAX_PARTY_PLAYERS - 1);
		double base = BASE_HEALTH + extra * HEALTH_PER_EXTRA_PLAYER;
		return apex ? Math.round(base * VaelkhorRules.APEX_HEALTH_MULTIPLIER) : base;
	}

	public static int prismCount(int challengers) {
		return VaelkhorRules.conduitCount(challengers);
	}

	public static int prismCount(int challengers, boolean apex) {
		return VaelkhorRules.conduitCount(challengers, apex);
	}

	public static int soakRingCount(int challengers) {
		return VaelkhorRules.soakRingCount(challengers);
	}

	public static int soakRingCount(int challengers, boolean apex) {
		return VaelkhorRules.soakRingCount(challengers, apex);
	}

	public static boolean isPhaseTwo(float health, float maxHealth) {
		return maxHealth > 0.0F && (health / maxHealth) <= PHASE_TWO_RATIO;
	}

	public static float severDamage(boolean phaseTwo, boolean apex) {
		return VaelkhorRules.scaleDamage(phaseTwo ? SEVER_DAMAGE_P2 : SEVER_DAMAGE_P1, apex);
	}

	public static String moveKey(int move) {
		return switch (move) {
			case STRIDE -> "stride";
			case CRESCENT_SEVER -> "crescent_sever";
			case PRISM_CONVERGENCE -> "prism_convergence";
			case TOTAL_ECLIPSE -> "total_eclipse";
			default -> "idle";
		};
	}

	public static int nextAttack(int completed, boolean phaseTwo) {
		if (phaseTwo && completed % 3 == 2) {
			return TOTAL_ECLIPSE;
		}
		return completed % 2 == 0 ? CRESCENT_SEVER : PRISM_CONVERGENCE;
	}

	public static float staggerDamage(float incoming, boolean staggered) {
		return staggered ? incoming * STAGGER_DAMAGE_MULTIPLIER : incoming;
	}
}
