package dev.wildercord.pet;

/**
 * Pure gameplay rules and balance constants for Cinnamon's companion abilities:
 * party lick healing, permanent Grand Bow scaling, multi-rider flying harness,
 * dual saddlebag chests, universal passive magic, and post-hit pause balance.
 */
public final class CinnamonRules {
	public static final double NORMAL_SCALE = 1.0D;
	public static final double GRAND_BOW_SCALE = 3.0D;

	public static final double GRAND_BOW_MAX_HEALTH_BONUS = 60.0D;
	public static final double GRAND_BOW_ATTACK_DAMAGE_BONUS = 12.0D;
	public static final double GRAND_BOW_ARMOR_BONUS = 10.0D;
	public static final double GRAND_BOW_ARMOR_TOUGHNESS_BONUS = 4.0D;
	public static final double GRAND_BOW_KNOCKBACK_RESISTANCE_BONUS = 0.6D;
	public static final double GRAND_BOW_SPEED_BONUS = 0.11D;
	public static final float GRAND_BOW_DAMAGE_BUDGET_BONUS = 40.0F;

	public static final int NORMAL_CAST_INTERVAL_TICKS = 90;
	public static final int GRAND_BOW_CAST_INTERVAL_TICKS = 55;
	public static final double NORMAL_SPELL_RANGE = 16.0D;
	public static final double GRAND_BOW_SPELL_RANGE = 24.0D;
	public static final float NORMAL_SPELL_POWER = 1.0F;
	public static final float GRAND_BOW_SPELL_POWER = 1.6F;

	public static final float LICK_HEAL_THRESHOLD_RATIO = 0.65F;
	public static final float NORMAL_LICK_HEAL = 6.0F;
	public static final float GRAND_BOW_LICK_HEAL = 10.0F;
	public static final int LICK_REGEN_DURATION_TICKS = 80;
	public static final int LICK_HEAL_COOLDOWN_TICKS = 100;
	public static final double LICK_HEAL_REACH = 2.8D;
	public static final double PARTY_HEAL_SEARCH_RADIUS = 18.0D;

	public static final int MAX_RIDERS = 4;
	public static final int CHEST_ROWS = 6;
	public static final int SADDLEBAG_SLOTS = CHEST_ROWS * 9;

	public static final double NORMAL_FLIGHT_SPEED = 0.42D;
	public static final double GRAND_BOW_FLIGHT_SPEED = 0.64D;
	public static final double FLIGHT_CLIMB_SPEED = 0.30D;
	public static final double GRAND_BOW_FLIGHT_CLIMB_SPEED = 0.38D;
	public static final double FLIGHT_DESCEND_SPEED = -0.26D;

	public static final int HIT_PAUSE_MIN_TICKS = 40;
	public static final int HIT_PAUSE_MAX_TICKS = 60;

	public record SeatOffset(double forward, double up, double side) {}

	private CinnamonRules() {}

	public static double effectiveScale(boolean grandBow, double growthScale, boolean growing) {
		if (grandBow) {
			return growing ? Math.max(GRAND_BOW_SCALE, growthScale) : GRAND_BOW_SCALE;
		}
		return growing ? growthScale : NORMAL_SCALE;
	}

	public static int castIntervalTicks(boolean grandBow) {
		return grandBow ? GRAND_BOW_CAST_INTERVAL_TICKS : NORMAL_CAST_INTERVAL_TICKS;
	}

	public static double spellRange(boolean grandBow) {
		return grandBow ? GRAND_BOW_SPELL_RANGE : NORMAL_SPELL_RANGE;
	}

	public static float spellPower(boolean grandBow) {
		return grandBow ? GRAND_BOW_SPELL_POWER : NORMAL_SPELL_POWER;
	}

	public static boolean needsLickHeal(float health, float maxHealth) {
		return maxHealth > 0.0F && health > 0.0F && health < maxHealth
			&& (health / maxHealth) < LICK_HEAL_THRESHOLD_RATIO;
	}

	public static float lickHealAmount(boolean grandBow, boolean big) {
		return (grandBow || big) ? GRAND_BOW_LICK_HEAL : NORMAL_LICK_HEAL;
	}

	public static boolean isMoreInjured(float firstHealth, float firstMax, float secondHealth, float secondMax) {
		if (firstMax <= 0.0F) return false;
		if (secondMax <= 0.0F) return true;
		return (firstHealth / firstMax) < (secondHealth / secondMax);
	}

	/**
	 * Always reserves one seat for Cinnamon's owner until the owner is already riding.
	 */
	public static boolean canAcceptRider(boolean hasHarness, int currentRiders, boolean candidateIsOwner, boolean ownerAlreadyRiding) {
		if (!hasHarness || currentRiders < 0 || currentRiders >= MAX_RIDERS) return false;
		if (candidateIsOwner) return true;
		return ownerAlreadyRiding || currentRiders < (MAX_RIDERS - 1);
	}

	/**
	 * Seat offsets in unscaled entity units (forward, up, right-side). Multiply by Cinnamon's scale in world space.
	 */
	public static SeatOffset seatOffset(int seatIndex, int passengerCount) {
		int clamped = Math.max(0, Math.min(MAX_RIDERS - 1, seatIndex));
		return switch (clamped) {
			case 0 -> new SeatOffset(0.16D, 0.78D, 0.0D);
			case 1 -> new SeatOffset(-0.14D, 0.78D, 0.0D);
			case 2 -> new SeatOffset(-0.44D, 0.76D, passengerCount > 3 ? -0.22D : 0.0D);
			default -> new SeatOffset(-0.44D, 0.76D, 0.22D);
		};
	}

	public static double flightSpeed(boolean grandBow) {
		return grandBow ? GRAND_BOW_FLIGHT_SPEED : NORMAL_FLIGHT_SPEED;
	}

	public static double verticalFlightVelocity(boolean climbPressed, boolean descendPressed, float xRotDeg, double forwardInput, boolean grandBow) {
		if (climbPressed) {
			return grandBow ? GRAND_BOW_FLIGHT_CLIMB_SPEED : FLIGHT_CLIMB_SPEED;
		}
		if (descendPressed) {
			return FLIGHT_DESCEND_SPEED;
		}
		if (Math.abs(forwardInput) > 0.05D) {
			double pitchClimb = -Math.sin(Math.toRadians(xRotDeg));
			return pitchClimb * (grandBow ? 0.44D : 0.34D);
		}
		return 0.0D;
	}

	public static int rollHitPauseTicks(int randomOffset) {
		int span = HIT_PAUSE_MAX_TICKS - HIT_PAUSE_MIN_TICKS;
		int clamped = Math.max(0, Math.min(span, randomOffset));
		return HIT_PAUSE_MIN_TICKS + clamped;
	}

	public static boolean isHitPaused(long gameTime, long hitPauseUntil) {
		return gameTime < hitPauseUntil;
	}

	public static boolean shouldPauseFromAttack(boolean attackerIsSelf, boolean attackerIsAlly, boolean attackerIsPassenger) {
		return !attackerIsSelf && !attackerIsAlly && !attackerIsPassenger;
	}
}
