package dev.wildercord.aura.world;

import dev.wildercord.aura.world.OathboundTechnique.Archetype;
import dev.wildercord.aura.world.OathboundTechnique.Effect;
import dev.wildercord.aura.world.OathboundTechnique.Motion;
import dev.wildercord.aura.world.OathboundTechnique.Shape;

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

	// ------------------------------------------------------------------ expanded technique sheet (ids 6..18)

	public static final int LUNGE_THRUST = 6;
	public static final int RISING_SLASH = 7;
	public static final int CROSS_CUT = 8;
	public static final int BLADE_FLURRY = 9;
	public static final int SPIN_SLASH = 10;
	public static final int BACKSTEP_SLASH = 11;
	public static final int MIRROR_STANCE = 12;
	public static final int SEVERING_ARC = 13;
	public static final int PIERCING_RAIN = 14;
	public static final int BLADE_BIND = 15;
	public static final int CRIMSON_TEMPEST = 16;
	public static final int EXECUTIONER_DROP = 17;
	public static final int PHANTOM_STRIKE = 18;

	public static final int[] COMBAT_MOVES = {
		WRATHFUL_RIPOSTE, CRESCENT_CLEAVE, POMMEL_BREAK, GUILLOTINE_SLAM,
		LUNGE_THRUST, RISING_SLASH, CROSS_CUT, BLADE_FLURRY, SPIN_SLASH, BACKSTEP_SLASH,
		MIRROR_STANCE, SEVERING_ARC, PIERCING_RAIN, BLADE_BIND, CRIMSON_TEMPEST,
		EXECUTIONER_DROP, PHANTOM_STRIKE
	};

	private static final int CRIMSON = 0xE63830;
	private static final int GOLD = 0xFFD048;

	private static final OathboundTechnique[] TECHNIQUES = {
		OathboundTechnique.b(LUNGE_THRUST, "lunge_thrust", Shape.LINE, Archetype.THRUST)
			.motion(Motion.DASH).tell(8).active(6).recovery(10)
			.damage(16.0).knockback(0.90).reach(4.6).half(0.8).color(CRIMSON).speed(1.0).build(),
		OathboundTechnique.b(RISING_SLASH, "rising_slash", Shape.CONE, Archetype.UPPERCUT)
			.tell(7).active(4).recovery(10)
			.damage(11.0).knockback(0.40).reach(2.6).half(55.0).lift(0.60).color(CRIMSON)
			.effect(Effect.LAUNCH, 0).build(),
		OathboundTechnique.b(CROSS_CUT, "cross_cut", Shape.WIDE_ARC, Archetype.CROSS_CUT)
			.tell(10).active(6).recovery(12)
			.damage(13.0).knockback(1.00).reach(3.9).half(85.0).color(CRIMSON).build(),
		OathboundTechnique.b(BLADE_FLURRY, "blade_flurry", Shape.MULTI_HIT, Archetype.FLURRY)
			.tell(6).active(10).recovery(10).hitDelay(1)
			.damage(5.0).knockback(0.30).reach(3.2).half(65.0).hits(4).color(GOLD).build(),
		OathboundTechnique.b(SPIN_SLASH, "spin_slash", Shape.RADIUS, Archetype.SPIN_SWEEP)
			.tell(9).active(8).recovery(12)
			.damage(12.0).knockback(1.30).reach(3.9).color(CRIMSON).build(),
		OathboundTechnique.b(BACKSTEP_SLASH, "backstep_slash", Shape.CONE, Archetype.BACKSTEP)
			.motion(Motion.BACKSTEP).tell(6).active(5).recovery(10)
			.damage(10.0).knockback(0.80).reach(3.4).half(70.0).color(CRIMSON).speed(0.60).build(),
		OathboundTechnique.b(MIRROR_STANCE, "mirror_stance", Shape.CONE, Archetype.GUARD_BRACE)
			.tell(4).active(8).recovery(14)
			.damage(12.0).knockback(1.10).reach(3.6).half(80.0).color(GOLD).build(),
		OathboundTechnique.b(SEVERING_ARC, "severing_arc", Shape.WIDE_ARC, Archetype.HORIZONTAL_SWEEP)
			.tell(12).active(7).recovery(14)
			.damage(15.0).knockback(1.40).reach(4.6).half(135.0).color(CRIMSON).build(),
		OathboundTechnique.b(PIERCING_RAIN, "piercing_rain", Shape.RADIUS, Archetype.LEAP_SLAM)
			.motion(Motion.LEAP).tell(10).active(8).recovery(14).hitDelay(5)
			.damage(14.0).knockback(0.90).reach(3.6).color(CRIMSON).speed(0.50).build(),
		OathboundTechnique.b(BLADE_BIND, "blade_bind", Shape.CONE, Archetype.SHIELD_PUSH)
			.tell(8).active(5).recovery(12)
			.damage(8.0).knockback(0.20).reach(2.2).half(50.0).color(GOLD)
			.effect(Effect.WEAKNESS, 60).build(),
		OathboundTechnique.b(CRIMSON_TEMPEST, "crimson_tempest", Shape.MULTI_HIT, Archetype.SPIN_SWEEP)
			.tell(12).active(12).recovery(14).hitDelay(1)
			.damage(6.0).knockback(0.50).reach(4.4).half(180.0).hits(5).color(CRIMSON)
			.empoweredOnly().build(),
		OathboundTechnique.b(EXECUTIONER_DROP, "executioner_drop", Shape.LINE, Archetype.OVERHEAD_SLAM)
			.tell(18).active(5).recovery(16)
			.damage(22.0).knockback(0.60).reach(5.0).half(1.0).color(CRIMSON).build(),
		OathboundTechnique.b(PHANTOM_STRIKE, "phantom_strike", Shape.TWIN_LINE, Archetype.DIAGONAL_SLASH)
			.motion(Motion.DASH).tell(8).active(6).recovery(12)
			.damage(14.0).knockback(1.10).reach(4.2).half(1.0).color(GOLD).speed(0.60).build()
	};

	/** The extra technique with {@code id}, or {@code null} for a signature move. */
	public static OathboundTechnique technique(int id) {
		for (OathboundTechnique t : TECHNIQUES) {
			if (t.id() == id) {
				return t;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ core attributes & thresholds

	public static final double MAX_HEALTH = 560.0;
	public static final double ARMOR = 15.0;
	public static final double ARMOR_TOUGHNESS = 7.0;
	public static final double KNOCKBACK_RESISTANCE = 0.72;
	public static final double MOVEMENT_SPEED = 0.42;
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
	public static final double POMMEL_LUNGE_SPEED = 1.30;
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

	/** Global combat speed multiplier - lower is faster. */
	public static final double SPEED = 0.55;

	/** Windup ticks for a given move, sped up slightly in Crimson Overdrive. */
	public static int tellTicks(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			int base = Math.max(3, (int) Math.round(tech.tell() * SPEED));
			return empowered ? Math.max(2, (int) Math.round(base * EMPOWERED_TELL_SCALE)) : base;
		}
		int base = switch (move) {
			case WRATHFUL_RIPOSTE -> RIPOSTE_TELL;
			case CRESCENT_CLEAVE -> CRESCENT_TELL;
			case POMMEL_BREAK -> POMMEL_TELL;
			case GUILLOTINE_SLAM -> GUILLOTINE_TELL;
			default -> 0;
		};
		base = base == 0 ? 0 : Math.max(3, (int) Math.round(base * SPEED));
		return base == 0 ? 0 : Math.max(2, (int) Math.round(base * (empowered ? EMPOWERED_TELL_SCALE : 1.0)));
	}

	/** Total duration of a scripted combat move in ticks. */
	public static int moveDuration(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			return tellTicks(move, empowered)
				+ Math.max(2, (int) Math.round(tech.active() * SPEED))
				+ Math.max(2, (int) Math.round(tech.recovery() * SPEED));
		}
		int tell = tellTicks(move, empowered);
		return switch (move) {
			case WRATHFUL_RIPOSTE -> tell + Math.max(2, (int) Math.round(RIPOSTE_WINDOW * SPEED)) + 3;
			case CRESCENT_CLEAVE -> tell + Math.max(2, (int) Math.round((CRESCENT_DURATION - CRESCENT_TELL) * SPEED));
			case POMMEL_BREAK -> tell + Math.max(2, (int) Math.round(POMMEL_LUNGE_TICKS * SPEED)) + 4;
			case GUILLOTINE_SLAM -> tell + Math.max(3, (int) Math.round(GUILLOTINE_RECOVERY * SPEED));
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
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			int base = Math.max(10, (int) Math.round((tech.tell() + tech.recovery()) * SPEED));
			return empowered ? Math.max(8, (int) Math.round(base * 0.78)) : base;
		}
		int base = switch (move) {
			case WRATHFUL_RIPOSTE -> 75;
			case CRESCENT_CLEAVE -> 24;
			case POMMEL_BREAK -> 65;
			case GUILLOTINE_SLAM -> 95;
			default -> 20;
		};
		base = Math.max(10, (int) Math.round(base * SPEED));
		return empowered ? Math.max(18, (int) Math.round(base * 0.78)) : base;
	}
}
