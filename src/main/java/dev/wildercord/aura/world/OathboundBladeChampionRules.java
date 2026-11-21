package dev.wildercord.aura.world;

/**
 * Pure combat, timing, scaling, and spawn rules for {@link OathboundBladeChampion} — Knight #2 (Way of the Blade).
 * Kept free of Minecraft runtime state so every frame window, parry arc, axe guard break, crescent cleave,
 * pommel guard-shatter, and guillotine fissure line is directly unit-tested.
 */
public final class OathboundBladeChampionRules {
	private OathboundBladeChampionRules() {}

	// ------------------------------------------------------------------ move ids (matching 6-panel animation sheet)

	/** One-handed shouldered greatsword stalk & tracking stance (Animation Panel 1). */
	public static final int IDLE_STALK = 0;
	/** Two-handed diagonal chest parry stance with golden-crimson riposte flare + counter cleave (Animation Panel 2). */
	public static final int WRATHFUL_RIPOSTE = 1;
	/** Wide 180-degree horizontal crimson crescent sweep (Animation Panel 3). */
	public static final int CRESCENT_CLEAVE = 2;
	/** Explosive forward pommel-first lunge that shatters player shields (Animation Panel 4). */
	public static final int POMMEL_BREAK = 3;
	/** Heroic overhead greatsword windup -> vertical ground-splitting crimson fissure slam (Animation Panels 5 & 6). */
	public static final int GUILLOTINE_SLAM = 4;
	/** Guard shattered by an axe during Wrathful Riposte. */
	public static final int STAGGERED = 5;

	public static final int[] COMBAT_MOVES = {WRATHFUL_RIPOSTE, CRESCENT_CLEAVE, POMMEL_BREAK, GUILLOTINE_SLAM};

	// ------------------------------------------------------------------ core attributes & thresholds

	public static final double MAX_HEALTH = 135.0;
	public static final double ARMOR = 15.0;
	public static final double ARMOR_TOUGHNESS = 7.0;
	public static final double KNOCKBACK_RESISTANCE = 0.72;
	public static final double MOVEMENT_SPEED = 0.30;
	public static final double FOLLOW_RANGE = 32.0;
	public static final float SCALE = 1.20F;
	public static final int XP_REWARD = 50;
	/** Pierce share through heavy player armour for the rune-etched greatsword. */
	public static final double PIERCE = 0.32;

	public static final float EMPOWERED_RATIO = 0.5F;
	public static final double EMPOWERED_DAMAGE_MULT = 1.18;
	public static final double EMPOWERED_TELL_SCALE = 0.85;

	// ------------------------------------------------------------------ timings & combat geometry

	public static final int RIPOSTE_TELL = 5;
	public static final int RIPOSTE_WINDOW = 26;
	public static final int RIPOSTE_COUNTER_TICKS = 12;
	public static final int RIPOSTE_DURATION = RIPOSTE_TELL + RIPOSTE_WINDOW + 6;
	public static final double RIPOSTE_COUNTER_DAMAGE = 15.0;
	public static final double RIPOSTE_COUNTER_REACH = 4.2;
	public static final double RIPOSTE_COUNTER_HALF_ANGLE_DEG = 85.0;
	public static final double RIPOSTE_COUNTER_KNOCKBACK = 1.25;

	public static final int CRESCENT_TELL = 12;
	public static final int CRESCENT_DURATION = 26;
	public static final double CRESCENT_REACH = 4.4;
	public static final double CRESCENT_HALF_ANGLE_DEG = 90.0;
	public static final double CRESCENT_DAMAGE = 13.0;
	public static final double CRESCENT_KNOCKBACK = 1.10;

	public static final int POMMEL_TELL = 10;
	public static final int POMMEL_LUNGE_TICKS = 8;
	public static final int POMMEL_DURATION = POMMEL_TELL + POMMEL_LUNGE_TICKS + 8;
	public static final double POMMEL_LUNGE_SPEED = 0.92;
	public static final double POMMEL_REACH = 2.1;
	public static final double POMMEL_DAMAGE = 10.0;
	public static final double POMMEL_KNOCKBACK = 1.55;
	public static final int POMMEL_SHIELD_DISABLE_TICKS = 100;
	public static final int POMMEL_SLOW_TICKS = 45;

	public static final int GUILLOTINE_TELL = 20;
	public static final int GUILLOTINE_RECOVERY = 20;
	public static final int GUILLOTINE_DURATION = GUILLOTINE_TELL + GUILLOTINE_RECOVERY;
	public static final double GUILLOTINE_CLOSE_RADIUS = 2.4;
	public static final double GUILLOTINE_CLOSE_DAMAGE = 18.0;
	public static final double GUILLOTINE_LINE_LENGTH = 6.5;
	public static final double GUILLOTINE_LINE_HALF_WIDTH = 1.35;
	public static final double GUILLOTINE_LINE_DAMAGE = 12.0;
	public static final double GUILLOTINE_KNOCKBACK = 0.95;

	public static final int STAGGER_AXE_TICKS = 50;
	public static final int MOVE_GAP_TICKS = 11;
	public static final int MOVE_GAP_EMPOWERED = 7;
	public static final int MEMORY = 2;

	public static final double PARRY_HALF_ARC = 75.0;
	public static final double BEHIND_MIN_ANGLE = 110.0;
	public static final double PARRY_MULTIPLIER = 0.10;
	public static final double GUILLOTINE_RECOVERY_MULTIPLIER = 1.25;
	public static final double STAGGER_MULTIPLIER = 1.45;

	// ------------------------------------------------------------------ pure geometric & state rules

	/** Whether the champion has entered Crimson Overdrive (&lt; 50% HP). */
	public static boolean empowered(float health, float maxHealth) {
		return maxHealth > 0.0F && (health / maxHealth) < EMPOWERED_RATIO;
	}

	/** Windup ticks for a given move, sped up slightly in Crimson Overdrive. */
	public static int tellTicks(int move, boolean empowered) {
		int base = switch (move) {
			case WRATHFUL_RIPOSTE -> RIPOSTE_TELL;
			case CRESCENT_CLEAVE -> CRESCENT_TELL;
			case POMMEL_BREAK -> POMMEL_TELL;
			case GUILLOTINE_SLAM -> GUILLOTINE_TELL;
			default -> 0;
		};
		return base == 0 ? 0 : Math.max(4, (int) Math.round(base * (empowered ? EMPOWERED_TELL_SCALE : 1.0)));
	}

	/** Total duration of a scripted combat move in ticks. */
	public static int moveDuration(int move, boolean empowered) {
		int tell = tellTicks(move, empowered);
		return switch (move) {
			case WRATHFUL_RIPOSTE -> tell + RIPOSTE_WINDOW + 6;
			case CRESCENT_CLEAVE -> tell + (CRESCENT_DURATION - CRESCENT_TELL);
			case POMMEL_BREAK -> tell + POMMEL_LUNGE_TICKS + 8;
			case GUILLOTINE_SLAM -> tell + GUILLOTINE_RECOVERY;
			case STAGGERED -> STAGGER_AXE_TICKS;
			default -> 0;
		};
	}

	/** Angle in degrees [0..180] between the champion's horizontal facing vector and the offset toward an attacker. */
	public static double angleFromFacing(float facingYawDeg, double dx, double dz) {
		double dist = Math.hypot(dx, dz);
		if (dist < 1.0e-5) {
			return 0.0;
		}
		double rad = Math.toRadians(facingYawDeg);
		double fx = -Math.sin(rad);
		double fz = Math.cos(rad);
		double dot = Math.max(-1.0, Math.min(1.0, (fx * dx + fz * dz) / dist));
		return Math.toDegrees(Math.acos(dot));
	}

	/** True when an attacker is within the champion's frontal parry arc. */
	public static boolean frontalParry(float facingYawDeg, double attackerDx, double attackerDz) {
		return angleFromFacing(facingYawDeg, attackerDx, attackerDz) <= PARRY_HALF_ARC;
	}

	/** True when an attacker stands behind the champion's shoulder guard. */
	public static boolean isBehind(float facingYawDeg, double attackerDx, double attackerDz) {
		return angleFromFacing(facingYawDeg, attackerDx, attackerDz) >= BEHIND_MIN_ANGLE;
	}

	/** Whether Wrathful Riposte is actively parrying incoming frontal strikes. */
	public static boolean parryActive(int move, long ticksSinceMoveBegan, int tell, boolean counterTriggered) {
		return move == WRATHFUL_RIPOSTE
			&& !counterTriggered
			&& ticksSinceMoveBegan >= tell
			&& ticksSinceMoveBegan <= tell + RIPOSTE_WINDOW;
	}

	/** Whether an incoming strike is intercepted by the active Wrathful Riposte stance. */
	public static boolean parriesAttack(int move, long ticksSinceMoveBegan, int tell, boolean counterTriggered,
			boolean unblockable, float facingYawDeg, double attackerDx, double attackerDz) {
		if (unblockable) {
			return false;
		}
		if (!parryActive(move, ticksSinceMoveBegan, tell, counterTriggered)) {
			return false;
		}
		return frontalParry(facingYawDeg, attackerDx, attackerDz);
	}

	/** Heavy axes smash straight through the sword parry and stagger the Blade-Champion. */
	public static boolean axeBreaksParry(int move, long ticksSinceMoveBegan, int tell, boolean counterTriggered,
			boolean isAxe, float facingYawDeg, double attackerDx, double attackerDz) {
		return isAxe && parryActive(move, ticksSinceMoveBegan, tell, counterTriggered)
			&& frontalParry(facingYawDeg, attackerDx, attackerDz);
	}

	/** Incoming damage multiplier based on stagger recovery vs active blade parry vs neutral poise. */
	public static double damageMultiplier(int move, long ticksSinceMoveBegan, int tell, boolean counterTriggered,
			boolean unblockable, float facingYawDeg, double attackerDx, double attackerDz) {
		if (move == STAGGERED) {
			return STAGGER_MULTIPLIER;
		}
		if (move == GUILLOTINE_SLAM && ticksSinceMoveBegan > tell) {
			return GUILLOTINE_RECOVERY_MULTIPLIER;
		}
		if (parriesAttack(move, ticksSinceMoveBegan, tell, counterTriggered, unblockable, facingYawDeg, attackerDx, attackerDz)) {
			return PARRY_MULTIPLIER;
		}
		return 1.0;
	}

	/** True when a target lies within the horizontal Crescent Cleave or Riposte Counter sector. */
	public static boolean inCleaveArc(float facingYawDeg, double dx, double dz, double reach, double halfAngleDeg) {
		double dist = Math.hypot(dx, dz);
		return dist <= reach && angleFromFacing(facingYawDeg, dx, dz) <= halfAngleDeg;
	}

	/**
	 * True when a target stands inside the forward Sky-Rending Guillotine line fissure.
	 * Uses projection along the facing vector ({@code [0..length]}) and lateral perpendicular distance ({@code <= halfWidth}).
	 */
	public static boolean inGuillotineLine(float facingYawDeg, double dx, double dz) {
		double rad = Math.toRadians(facingYawDeg);
		double fx = -Math.sin(rad);
		double fz = Math.cos(rad);
		double along = dx * fx + dz * fz;
		double across = Math.abs(dx * (-fz) + dz * fx);
		if (Math.hypot(dx, dz) <= GUILLOTINE_CLOSE_RADIUS) {
			return true;
		}
		return along >= 0.0 && along <= GUILLOTINE_LINE_LENGTH && across <= GUILLOTINE_LINE_HALF_WIDTH;
	}

	/** Guillotine damage scaled by close impact vs forward crimson line fissure and Empowered phase. */
	public static double guillotineDamage(double horizontalDist, boolean empowered) {
		double base = horizontalDist <= GUILLOTINE_CLOSE_RADIUS ? GUILLOTINE_CLOSE_DAMAGE : GUILLOTINE_LINE_DAMAGE;
		return empowered ? base * EMPOWERED_DAMAGE_MULT : base;
	}

	/** Cooldown in ticks between uses of a specific combat move. */
	public static int moveCooldown(int move, boolean empowered) {
		int base = switch (move) {
			case WRATHFUL_RIPOSTE -> 75;
			case CRESCENT_CLEAVE -> 24;
			case POMMEL_BREAK -> 65;
			case GUILLOTINE_SLAM -> 95;
			default -> 20;
		};
		return empowered ? Math.max(18, (int) Math.round(base * 0.78)) : base;
	}
}
