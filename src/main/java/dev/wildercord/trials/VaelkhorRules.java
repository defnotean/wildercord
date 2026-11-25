package dev.wildercord.trials;

import net.minecraft.util.Mth;

/** Pure rules, timings, scaling, and hit-volume geometry for Vaelkhor and Co-Op Trials. */
public final class VaelkhorRules {
	private VaelkhorRules() {}

	public static final int IDLE = 0;
	public static final int STRIDE = 1;
	public static final int HALBERD_CLEAVE = 2;
	public static final int BELL_TOLL_CONDUIT = 3;
	public static final int LEYLINE_CATACLYSM = 4;

	public static final int MAX_PARTY_PLAYERS = 6;
	public static final double ARENA_RADIUS = 18.5;
	public static final double ARENA_Y_MIN = -4.0;
	public static final double ARENA_Y_MAX = 12.0;

	public static final double BASE_HEALTH = 320.0;
	public static final double HEALTH_PER_EXTRA_PLAYER = 160.0;
	public static final double APEX_HEALTH_MULTIPLIER = 1.35;
	public static final float APEX_DAMAGE_MULTIPLIER = 1.20F;
	public static final double APEX_COOLDOWN_MULTIPLIER = 0.80;

	public static final int CLEAVE_TELL_TICKS = 14;
	public static final int CLEAVE_ACTIVE_END_TICKS = 21;
	public static final int CLEAVE_DURATION_TICKS = 31;
	public static final double CLEAVE_REACH = 6.6;
	public static final double CLEAVE_HALF_ANGLE_RAD = Math.toRadians(68.0);
	public static final float CLEAVE_DAMAGE_P1 = 11.0F;
	public static final float CLEAVE_DAMAGE_P2 = 14.0F;

	public static final int CONDUIT_ANIM_TICKS = 40;
	public static final int CONDUIT_WINDOW_TICKS = 100;
	public static final int STAGGER_TICKS = 50;
	public static final float STAGGER_DAMAGE_MULTIPLIER = 1.35F;
	public static final float CONDUIT_PULSE_DAMAGE = 9.0F;

	public static final int CATACLYSM_TELL_TICKS = 40;
	public static final int CATACLYSM_RECOVERY_TICKS = 18;
	public static final double SOAK_RADIUS = 3.2;
	public static final float CATACLYSM_SOAKED_DAMAGE = 4.0F;
	public static final float CATACLYSM_UNSOAKED_DAMAGE = 16.0F;

	public static final float PHASE_TWO_RATIO = 0.60F;
	public static final int QUIET_RESET_TICKS = 120;

	/** Four arena conduit pylon XZ offsets from the sanctum center. */
	public static final int[][] CONDUIT_OFFSETS = {
		{-12, -12},
		{12, -12},
		{-12, 12},
		{12, 12},
	};

	/** Candidate soak ring XZ offsets from the sanctum center during Leyline Cataclysm. */
	public static final double[][] SOAK_OFFSETS = {
		{-7.5, -7.5},
		{7.5, -7.5},
		{0.0, 8.5},
	};

	public static double maxHealthFor(int challengers) {
		return maxHealthFor(challengers, false);
	}

	public static double maxHealthFor(int challengers, boolean apex) {
		int extra = Math.clamp(challengers - 1, 0, MAX_PARTY_PLAYERS - 1);
		double base = BASE_HEALTH + extra * HEALTH_PER_EXTRA_PLAYER;
		return apex ? Math.round(base * APEX_HEALTH_MULTIPLIER) : base;
	}

	public static int conduitCount(int challengers) {
		if (challengers <= 2) return 2;
		if (challengers == 3) return 3;
		return 4;
	}

	public static int conduitCount(int challengers, boolean apex) {
		int base = conduitCount(challengers);
		return apex ? Math.min(4, base + 1) : base;
	}

	public static int soakRingCount(int challengers) {
		if (challengers <= 1) return 1;
		if (challengers <= 3) return 2;
		return 3;
	}

	public static int soakRingCount(int challengers, boolean apex) {
		int base = soakRingCount(challengers);
		return apex ? Math.min(3, base + 1) : base;
	}

	public static boolean isPhaseTwo(float health, float maxHealth) {
		return maxHealth > 0.0F && (health / maxHealth) <= PHASE_TWO_RATIO;
	}

	public static float cleaveDamage(boolean phaseTwo) {
		return cleaveDamage(phaseTwo, false);
	}

	public static float cleaveDamage(boolean phaseTwo, boolean apex) {
		return scaleDamage(phaseTwo ? CLEAVE_DAMAGE_P2 : CLEAVE_DAMAGE_P1, apex);
	}

	public static float scaleDamage(float baseDamage, boolean apex) {
		return apex ? baseDamage * APEX_DAMAGE_MULTIPLIER : baseDamage;
	}

	public static long scaleCooldown(long baseTicks, boolean apex) {
		return apex ? Math.max(1L, Math.round(baseTicks * APEX_COOLDOWN_MULTIPLIER)) : baseTicks;
	}

	public static String moveKey(int move) {
		return switch (move) {
			case STRIDE -> "stride";
			case HALBERD_CLEAVE -> "halberd_cleave";
			case BELL_TOLL_CONDUIT -> "bell_toll_conduit";
			case LEYLINE_CATACLYSM -> "leyline_cataclysm";
			default -> "idle";
		};
	}

	public static int nextAttack(int completed, boolean phaseTwo) {
		if (phaseTwo && completed % 3 == 2) {
			return LEYLINE_CATACLYSM;
		}
		return completed % 2 == 0 ? HALBERD_CLEAVE : BELL_TOLL_CONDUIT;
	}

	public static boolean withinArena(double dx, double dy, double dz) {
		return (dx * dx + dz * dz) <= (ARENA_RADIUS * ARENA_RADIUS) && dy >= ARENA_Y_MIN && dy <= ARENA_Y_MAX;
	}

	/** Tests whether {@code (dx, dz)} lies within Minecraft-yaw {@code aimYawDeg} cleave cone. */
	public static boolean inCleaveArc(double dx, double dz, float aimYawDeg, double reach, double halfAngleRad) {
		double distSq = dx * dx + dz * dz;
		if (distSq > reach * reach) {
			return false;
		}
		if (distSq < 1.2) {
			return true;
		}
		double rad = aimYawDeg * Mth.DEG_TO_RAD;
		double fx = -Math.sin(rad);
		double fz = Math.cos(rad);
		double dist = Math.sqrt(distSq);
		double dot = Mth.clamp((dx * fx + dz * fz) / dist, -1.0, 1.0);
		return dot >= Math.cos(halfAngleRad);
	}

	public static boolean inRing(double dx, double dz, double radius) {
		return (dx * dx + dz * dz) <= (radius * radius);
	}

	public static float staggerDamage(float incoming, boolean staggered) {
		return staggered ? incoming * STAGGER_DAMAGE_MULTIPLIER : incoming;
	}
}
