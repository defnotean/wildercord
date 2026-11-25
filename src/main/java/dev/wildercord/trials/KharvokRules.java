package dev.wildercord.trials;

/** Pure rules, timings, scaling, and hit-volume geometry for Kharvok, the Crucible Sovereign. */
public final class KharvokRules {
	private KharvokRules() {}

	public static final int IDLE = 0;
	public static final int STRIDE = 1;
	public static final int ANVIL_SHATTER = 2;
	public static final int CHAIN_SHACKLE = 3;
	public static final int CRUCIBLE_MELTDOWN = 4;

	public static final int MAX_PARTY_PLAYERS = VaelkhorRules.MAX_PARTY_PLAYERS;
	public static final double ARENA_RADIUS = VaelkhorRules.ARENA_RADIUS;
	public static final double ARENA_Y_MIN = VaelkhorRules.ARENA_Y_MIN;
	public static final double ARENA_Y_MAX = VaelkhorRules.ARENA_Y_MAX;

	public static final double BASE_HEALTH = 380.0;
	public static final double HEALTH_PER_EXTRA_PLAYER = 180.0;

	public static final int SHATTER_TELL_TICKS = 15;
	public static final int SHATTER_ACTIVE_END_TICKS = 22;
	public static final int SHATTER_DURATION_TICKS = 31;
	public static final double SHATTER_REACH = 6.8;
	public static final double SHATTER_HALF_ANGLE_RAD = Math.toRadians(72.0);
	public static final float SHATTER_DAMAGE_P1 = 13.0F;
	public static final float SHATTER_DAMAGE_P2 = 16.0F;

	public static final int SHACKLE_WINDOW_TICKS = 100;
	public static final int STAGGER_TICKS = 55;
	public static final float STAGGER_DAMAGE_MULTIPLIER = 1.35F;
	public static final float SHACKLE_PULSE_DAMAGE = 10.0F;

	public static final int MELTDOWN_TELL_TICKS = 42;
	public static final int MELTDOWN_RECOVERY_TICKS = 16;
	public static final double SOAK_RADIUS = 3.2;
	public static final float MELTDOWN_SOAKED_DAMAGE = 4.5F;
	public static final float MELTDOWN_UNSOAKED_DAMAGE = 18.0F;

	public static final float PHASE_TWO_RATIO = 0.60F;
	public static final int QUIET_RESET_TICKS = 120;

	public static final int[][] VENT_OFFSETS = VaelkhorRules.CONDUIT_OFFSETS;
	public static final double[][] MELTDOWN_SOAK_OFFSETS = VaelkhorRules.SOAK_OFFSETS;

	public static double maxHealthFor(int challengers) {
		return maxHealthFor(challengers, false);
	}

	public static double maxHealthFor(int challengers, boolean apex) {
		int extra = Math.clamp(challengers - 1, 0, MAX_PARTY_PLAYERS - 1);
		double base = BASE_HEALTH + extra * HEALTH_PER_EXTRA_PLAYER;
		return apex ? Math.round(base * VaelkhorRules.APEX_HEALTH_MULTIPLIER) : base;
	}

	public static int ventCount(int challengers) {
		return VaelkhorRules.conduitCount(challengers);
	}

	public static int ventCount(int challengers, boolean apex) {
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

	public static float shatterDamage(boolean phaseTwo, boolean apex) {
		return VaelkhorRules.scaleDamage(phaseTwo ? SHATTER_DAMAGE_P2 : SHATTER_DAMAGE_P1, apex);
	}

	public static String moveKey(int move) {
		return switch (move) {
			case STRIDE -> "stride";
			case ANVIL_SHATTER -> "anvil_shatter";
			case CHAIN_SHACKLE -> "chain_shackle";
			case CRUCIBLE_MELTDOWN -> "crucible_meltdown";
			default -> "idle";
		};
	}

	public static int nextAttack(int completed, boolean phaseTwo) {
		if (phaseTwo && completed % 3 == 2) {
			return CRUCIBLE_MELTDOWN;
		}
		return completed % 2 == 0 ? ANVIL_SHATTER : CHAIN_SHACKLE;
	}

	public static float staggerDamage(float incoming, boolean staggered) {
		return staggered ? incoming * STAGGER_DAMAGE_MULTIPLIER : incoming;
	}
}
