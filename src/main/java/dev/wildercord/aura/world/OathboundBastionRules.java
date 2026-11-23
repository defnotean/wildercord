package dev.wildercord.aura.world;

import dev.wildercord.aura.world.OathboundTechnique.Archetype;
import dev.wildercord.aura.world.OathboundTechnique.Effect;
import dev.wildercord.aura.world.OathboundTechnique.Motion;
import dev.wildercord.aura.world.OathboundTechnique.Shape;

/**
 * Pure combat, timing, scaling, and spawn rules for {@link OathboundBastion} — Knight #1 (Way of the Bulwark).
 * Kept free of Minecraft runtime state so every frame window, shield angle, shockwave falloff, and guard break
 * is directly unit-tested.
 */
public final class OathboundBastionRules {
	private OathboundBastionRules() {}

	// ------------------------------------------------------------------ move ids (matching animation sheet 1..6)

	/** Guarded march & tracking stance (Animation Panel 1). */
	public static final int IDLE_MARCH = 0;
	/** Crouched behind grounded tower shield + hexagonal Aura barrier (Animation Panel 2). */
	public static final int PHALANX_BRACE = 1;
	/** Forward-pitched battering-ram charge (Animation Panel 3). */
	public static final int SHIELD_RAM = 2;
	/** Overhead flanged-mace windup -> one-knee ground slam & concentric shockwave (Animation Panels 4 & 5). */
	public static final int ANVIL_SLAM = 3;
	/** Wide horizontal sweeping bash with the tower shield (Animation Panel 6). */
	public static final int SHIELD_BASH = 4;
	/** Heavy diagonal cleave with the radiant Sun-Flanged Greatmace. */
	public static final int MACE_CLEAVE = 5;
	/** Guard shattered by an axe or self-stunned by crashing into stone during Shield-Ram. */
	public static final int STAGGERED = 6;

	// ------------------------------------------------------------------ expanded technique sheet (ids 7..19)

	public static final int AEGIS_TOSS = 7;
	public static final int EARTHSPLIT_STOMP = 8;
	public static final int MACE_OVERHEAD = 9;
	public static final int SHIELD_WALL_SLIDE = 10;
	public static final int WHIRLING_BULWARK = 11;
	public static final int GAUNTLET_UPPERCUT = 12;
	public static final int ANVIL_DROP = 13;
	public static final int CROSS_GUARD_COUNTER = 14;
	public static final int SHIELD_RICOCHET = 15;
	public static final int MACE_SWEEP_LOW = 16;
	public static final int BULWARK_ROAR = 17;
	public static final int SEISMIC_LINE = 18;
	public static final int SUN_ANVIL_JUDGMENT = 19;

	public static final int[] COMBAT_MOVES = {
		PHALANX_BRACE, SHIELD_RAM, ANVIL_SLAM, SHIELD_BASH, MACE_CLEAVE,
		AEGIS_TOSS, EARTHSPLIT_STOMP, MACE_OVERHEAD, SHIELD_WALL_SLIDE, WHIRLING_BULWARK,
		GAUNTLET_UPPERCUT, ANVIL_DROP, CROSS_GUARD_COUNTER, SHIELD_RICOCHET, MACE_SWEEP_LOW,
		BULWARK_ROAR, SEISMIC_LINE, SUN_ANVIL_JUDGMENT
	};

	private static final int GOLD = 0xFFD048;

	private static final OathboundTechnique[] TECHNIQUES = {
		OathboundTechnique.b(AEGIS_TOSS, "aegis_toss", Shape.LINE, Archetype.OVERHAND_THROW)
			.tell(12).active(8).recovery(14).hitDelay(2)
			.damage(12.0).knockback(1.20).reach(9.0).half(0.9).color(GOLD).build(),
		OathboundTechnique.b(EARTHSPLIT_STOMP, "earthsplit_stomp", Shape.RING, Archetype.STOMP)
			.tell(14).active(6).recovery(12).hitDelay(3)
			.damage(9.0).knockback(0.90).reach(5.0).inner(1.0).color(GOLD)
			.effect(Effect.SLOW, 40).build(),
		OathboundTechnique.b(MACE_OVERHEAD, "mace_overhead", Shape.CONE, Archetype.OVERHEAD_SLAM)
			.tell(16).active(4).recovery(14)
			.damage(18.0).knockback(0.60).reach(3.0).half(28.0).color(GOLD).build(),
		OathboundTechnique.b(SHIELD_WALL_SLIDE, "shield_wall_slide", Shape.LINE, Archetype.SHIELD_PUSH)
			.motion(Motion.DASH).tell(10).active(10).recovery(10)
			.damage(8.0).knockback(1.50).reach(2.4).half(1.6).color(GOLD).speed(0.70).build(),
		OathboundTechnique.b(WHIRLING_BULWARK, "whirling_bulwark", Shape.RADIUS, Archetype.SPIN_SWEEP)
			.tell(12).active(10).recovery(12)
			.damage(11.0).knockback(1.60).reach(3.8).color(GOLD).build(),
		OathboundTechnique.b(GAUNTLET_UPPERCUT, "gauntlet_uppercut", Shape.CONE, Archetype.UPPERCUT)
			.tell(6).active(4).recovery(10)
			.damage(9.0).knockback(0.30).reach(2.2).half(45.0).lift(0.55).color(GOLD)
			.effect(Effect.LAUNCH, 0).build(),
		OathboundTechnique.b(ANVIL_DROP, "anvil_drop", Shape.RADIUS, Archetype.LEAP_SLAM)
			.motion(Motion.LEAP).tell(14).active(10).recovery(14).hitDelay(6)
			.damage(15.0).knockback(1.10).reach(4.2).color(GOLD).speed(0.40).build(),
		OathboundTechnique.b(CROSS_GUARD_COUNTER, "cross_guard_counter", Shape.CONE, Archetype.CROSS_CUT)
			.tell(5).active(6).recovery(14)
			.damage(14.0).knockback(0.80).reach(3.6).half(70.0).color(GOLD).build(),
		OathboundTechnique.b(SHIELD_RICOCHET, "shield_ricochet", Shape.MULTI_HIT, Archetype.WEAPON_TWIRL)
			.tell(10).active(6).recovery(14).hitDelay(2)
			.damage(7.0).knockback(0.80).reach(7.0).half(18.0).hits(3).color(GOLD).build(),
		OathboundTechnique.b(MACE_SWEEP_LOW, "mace_sweep_low", Shape.WIDE_ARC, Archetype.HORIZONTAL_SWEEP)
			.tell(10).active(6).recovery(12)
			.damage(9.0).knockback(1.00).reach(4.2).half(110.0).color(GOLD)
			.effect(Effect.SLOW, 60).build(),
		OathboundTechnique.b(BULWARK_ROAR, "bulwark_roar", Shape.RADIUS, Archetype.ROAR)
			.tell(12).active(8).recovery(16)
			.damage(5.0).knockback(1.40).reach(6.0).color(GOLD)
			.effect(Effect.FEAR, 80).build(),
		OathboundTechnique.b(SEISMIC_LINE, "seismic_line", Shape.LINE, Archetype.KNEEL_STRIKE)
			.tell(14).active(8).recovery(12)
			.damage(13.0).knockback(0.90).reach(8.0).half(1.2).color(GOLD)
			.effect(Effect.SLOW, 30).build(),
		OathboundTechnique.b(SUN_ANVIL_JUDGMENT, "sun_anvil_judgment", Shape.CROSS, Archetype.OVERHEAD_SLAM)
			.tell(20).active(8).recovery(18)
			.damage(20.0).knockback(1.60).reach(6.5).inner(2.8).half(1.3).color(GOLD)
			.effect(Effect.LAUNCH, 0).lift(0.35).empoweredOnly().build()
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

	public static final double MAX_HEALTH = 650.0;
	public static final double ARMOR = 18.0;
	public static final double ARMOR_TOUGHNESS = 8.0;
	public static final double KNOCKBACK_RESISTANCE = 0.85;
	public static final double MOVEMENT_SPEED = 0.24;
	public static final double FOLLOW_RANGE = 32.0;
	public static final float SCALE = 1.25F;
	public static final int XP_REWARD = 45;
	/** Pierce share through heavy player armour so the Bastion's heavy mace and shield stay dangerous. */
	public static final double PIERCE = 0.28;

	public static final float EMPOWERED_RATIO = 0.5F;
	public static final double EMPOWERED_DAMAGE_MULT = 1.15;
	public static final double EMPOWERED_TELL_SCALE = 0.85;

	// ------------------------------------------------------------------ timings & combat geometry

	public static final int PHALANX_TELL = 8;
	public static final int PHALANX_DURATION = 36;

	public static final int RAM_TELL = 14;
	public static final int RAM_CHARGE_TICKS = 22;
	public static final int RAM_DURATION = RAM_TELL + RAM_CHARGE_TICKS + 4;
	public static final double RAM_SPEED = 0.78;
	public static final double RAM_DAMAGE = 13.0;
	public static final double RAM_KNOCKBACK = 1.7;
	public static final double RAM_HIT_RADIUS = 1.8;

	public static final int ANVIL_TELL = 22;
	public static final int ANVIL_RECOVERY = 20;
	public static final int ANVIL_DURATION = ANVIL_TELL + ANVIL_RECOVERY;
	public static final double ANVIL_DIRECT_RADIUS = 2.6;
	public static final double ANVIL_DIRECT_DAMAGE = 16.0;
	public static final double ANVIL_SHOCKWAVE_RADIUS = 5.8;
	public static final double ANVIL_SHOCKWAVE_MAX = 10.0;
	public static final double ANVIL_SHOCKWAVE_MIN = 6.0;
	public static final double ANVIL_LIFT = 0.48;

	public static final int BASH_TELL = 11;
	public static final int BASH_DURATION = 22;
	public static final double BASH_REACH = 3.6;
	public static final double BASH_HALF_ANGLE_DEG = 70.0;
	public static final double BASH_DAMAGE = 11.0;
	public static final double BASH_KNOCKBACK = 1.45;
	public static final int BASH_SLOW_TICKS = 50;

	public static final int CLEAVE_TELL = 13;
	public static final int CLEAVE_DURATION = 24;
	public static final double CLEAVE_REACH = 3.8;
	public static final double CLEAVE_HALF_ANGLE_DEG = 55.0;
	public static final double CLEAVE_DAMAGE = 14.0;
	public static final double CLEAVE_KNOCKBACK = 0.7;

	public static final int STAGGER_AXE_TICKS = 55;
	public static final int STAGGER_WALL_TICKS = 50;
	public static final int MOVE_GAP_TICKS = 14;
	public static final int MOVE_GAP_EMPOWERED = 9;
	public static final int MEMORY = 3;

	public static final double FRONTAL_HALF_ARC = 65.0;
	public static final double BEHIND_MIN_ANGLE = 115.0;
	public static final double SHIELD_BRACE_MULTIPLIER = 0.15;
	public static final double PASSIVE_FRONT_MULTIPLIER = 0.85;
	public static final double ANVIL_RECOVERY_MULTIPLIER = 1.25;
	public static final double STAGGER_MULTIPLIER = 1.45;

	public static final int MIN_CHAMPION_HAUNT_RANK = 2;
	public static final double CHAMPION_REPLACE_CHANCE = 0.15;
	public static final double CHAMPION_CROWD_RADIUS = 48.0;

	/** Folds {@code angle} into {@code [-180, 180)}. */
	public static double wrap(double angle) {
		return ((angle % 360.0) + 540.0) % 360.0 - 180.0;
	}

	/** Whether a hit arriving at relative yaw {@code relYaw} falls inside the tower shield's frontal arc. */
	public static boolean frontal(double relYaw) {
		return Math.abs(wrap(relYaw)) <= FRONTAL_HALF_ARC;
	}

	/** Whether {@code relYaw} lies squarely behind the knight. */
	public static boolean behind(double relYaw) {
		return Math.abs(wrap(relYaw)) >= BEHIND_MIN_ANGLE;
	}

	/** Whether the Bastion has entered its empowered phase below half health. */
	public static boolean empowered(float health, float maxHealth) {
		return maxHealth > 0 && health <= maxHealth * EMPOWERED_RATIO;
	}

	/** Windup/tell ticks before {@code move} strikes (compressed by 15% when empowered). */
	public static int tellTicks(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			return empowered ? Math.max(4, (int) Math.round(tech.tell() * EMPOWERED_TELL_SCALE)) : tech.tell();
		}
		int base = switch (move) {
			case PHALANX_BRACE -> PHALANX_TELL;
			case SHIELD_RAM -> RAM_TELL;
			case ANVIL_SLAM -> ANVIL_TELL;
			case SHIELD_BASH -> BASH_TELL;
			case MACE_CLEAVE -> CLEAVE_TELL;
			default -> 0;
		};
		if (base <= 0 || !empowered) {
			return base;
		}
		return Math.max(5, (int) Math.round(base * EMPOWERED_TELL_SCALE));
	}

	/** Total ticks {@code move} runs before returning to {@link #IDLE_MARCH}. */
	public static int moveDuration(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			return tellTicks(move, empowered) + tech.active() + tech.recovery();
		}
		int tell = tellTicks(move, empowered);
		return switch (move) {
			case PHALANX_BRACE -> PHALANX_DURATION;
			case SHIELD_RAM -> tell + RAM_CHARGE_TICKS + 4;
			case ANVIL_SLAM -> tell + ANVIL_RECOVERY;
			case SHIELD_BASH -> tell + (BASH_DURATION - BASH_TELL);
			case MACE_CLEAVE -> tell + (CLEAVE_DURATION - CLEAVE_TELL);
			case STAGGERED -> STAGGER_AXE_TICKS;
			default -> 0;
		};
	}

	/** Damage multiplier taken given current move, hit direction, and whether the knight is kneeling after Anvil Slam. */
	public static double damageMultiplier(int move, boolean frontal, boolean inAnvilRecovery) {
		if (move == STAGGERED) {
			return STAGGER_MULTIPLIER;
		}
		if (move == ANVIL_SLAM && inAnvilRecovery) {
			return ANVIL_RECOVERY_MULTIPLIER;
		}
		if (move == PHALANX_BRACE && frontal) {
			return SHIELD_BRACE_MULTIPLIER;
		}
		if (move == SHIELD_RAM && frontal) {
			return 0.45;
		}
		return frontal ? PASSIVE_FRONT_MULTIPLIER : 1.0;
	}

	/** Whether an incoming projectile hitting from {@code frontal} is reflected by Phalanx Brace. */
	public static boolean deflectsProjectile(int move, boolean frontal) {
		return move == PHALANX_BRACE && frontal;
	}

	/** Whether a frontal axe strike shatters Phalanx Brace into {@link #STAGGERED}. */
	public static boolean axeBreaksGuard(int move, boolean frontal) {
		return move == PHALANX_BRACE && frontal;
	}

	/** Damage dealt by the expanding Anvil Slam golden shockwave at radial {@code distance} from the Bastion. */
	public static double anvilShockwaveDamage(double distance, boolean empowered) {
		if (distance < 0 || distance > ANVIL_SHOCKWAVE_RADIUS) {
			return 0.0;
		}
		double mult = empowered ? EMPOWERED_DAMAGE_MULT : 1.0;
		if (distance <= ANVIL_DIRECT_RADIUS) {
			return ANVIL_DIRECT_DAMAGE * mult;
		}
		double t = (distance - ANVIL_DIRECT_RADIUS) / (ANVIL_SHOCKWAVE_RADIUS - ANVIL_DIRECT_RADIUS);
		double base = ANVIL_SHOCKWAVE_MAX + (ANVIL_SHOCKWAVE_MIN - ANVIL_SHOCKWAVE_MAX) * Math.max(0.0, Math.min(1.0, t));
		return base * mult;
	}

	/** Scaled move damage when empowered vs normal. */
	public static double scaleDamage(double base, boolean empowered) {
		return empowered ? base * EMPOWERED_DAMAGE_MULT : base;
	}

	/** Whether a haunt qualifies to swap in an Oathbound Bastion instead of a regular Fallen Knight. */
	public static boolean championRollEligible(int hauntRank, int nearbyChampions) {
		return hauntRank >= MIN_CHAMPION_HAUNT_RANK && nearbyChampions <= 0;
	}

	/** Whether {@code distance} and {@code relYaw} fall within a frontal cone of {@code reach} and {@code halfAngleDeg}. */
	public static boolean inCone(double distance, double dy, double relYaw, double reach, double halfAngleDeg) {
		return distance <= reach && Math.abs(dy) <= 2.6 && Math.abs(wrap(relYaw)) <= halfAngleDeg;
	}

	/** Translation key fragment for a move. */
	public static String moveKey(int move) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			return tech.key();
		}
		return switch (move) {
			case PHALANX_BRACE -> "phalanx_brace";
			case SHIELD_RAM -> "shield_ram";
			case ANVIL_SLAM -> "anvil_slam";
			case SHIELD_BASH -> "shield_bash";
			case MACE_CLEAVE -> "mace_cleave";
			case STAGGERED -> "staggered";
			default -> "idle_march";
		};
	}
}
