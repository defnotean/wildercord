package dev.wildercord.aura.world;

import dev.wildercord.aura.world.OathboundTechnique.Archetype;
import dev.wildercord.aura.world.OathboundTechnique.Effect;
import dev.wildercord.aura.world.OathboundTechnique.Motion;
import dev.wildercord.aura.world.OathboundTechnique.Shape;

/**
 * Pure combat, timing, scaling, and spawn rules for {@link OathboundHalberdier} — Knight #3 (Way of the Storm-Glaive).
 * Kept free of Minecraft runtime state so every frame window, whirl radius, thrust/hook line, 4-way thunder cross,
 * and axe windup interrupt is directly unit-tested.
 */
public final class OathboundHalberdierRules {
	private OathboundHalberdierRules() {}

	// ------------------------------------------------------------------ move ids (matching 6-panel animation sheet)

	/** Agile low-glaive combat stalk & tracking stance (Animation Panel 1). */
	public static final int LOW_GLAIVE_STALK = 0;
	/** Full-body 360-degree spinning horizontal halberd cleave (Animation Panel 2). */
	public static final int TEMPEST_WHIRL = 1;
	/** High-speed Shadowstep dash and long spear thrust (Animation Panel 3). */
	public static final int SHADOWSTEP_THRUST = 2;
	/** Forward halberd rear-hook snag that yanks targets inward off-balance (Animation Panel 4). */
	public static final int HOOK_AND_REAP = 3;
	/** High-arch overhead Valkyrie vault windup -> vertical ground crash & 4-way lightning cross (Panels 5 & 6). */
	public static final int SKY_VAULT_CRASH = 4;
	/** Interrupted mid-windup by a heavy axe blow. */
	public static final int STAGGERED = 5;

	// ------------------------------------------------------------------ expanded technique sheet (ids 6..18)

	public static final int GLAIVE_SWEEP = 6;
	public static final int POLE_VAULT_KICK = 7;
	public static final int STORM_JAB = 8;
	public static final int CYCLONE_DANCE = 9;
	public static final int LIGHTNING_LUNGE = 10;
	public static final int REAPING_CIRCLE = 11;
	public static final int GLAIVE_THROW = 12;
	public static final int THUNDER_STOMP = 13;
	public static final int RISING_GLAIVE = 14;
	public static final int SHADOW_CLONE_STRIKE = 15;
	public static final int WHIRLWIND_PULL = 16;
	public static final int SKYFALL_CLEAVE = 17;
	public static final int STORMBREAKER = 18;

	public static final int[] COMBAT_MOVES = {
		TEMPEST_WHIRL, SHADOWSTEP_THRUST, HOOK_AND_REAP, SKY_VAULT_CRASH,
		GLAIVE_SWEEP, POLE_VAULT_KICK, STORM_JAB, CYCLONE_DANCE, LIGHTNING_LUNGE, REAPING_CIRCLE,
		GLAIVE_THROW, THUNDER_STOMP, RISING_GLAIVE, SHADOW_CLONE_STRIKE, WHIRLWIND_PULL,
		SKYFALL_CLEAVE, STORMBREAKER
	};

	private static final int CYAN = 0x48F0FF;
	private static final int VIOLET = 0x9A68FF;

	private static final OathboundTechnique[] TECHNIQUES = {
		OathboundTechnique.b(GLAIVE_SWEEP, "glaive_sweep", Shape.WIDE_ARC, Archetype.HORIZONTAL_SWEEP)
			.tell(8).active(6).recovery(10)
			.damage(11.0).knockback(1.10).reach(4.6).half(100.0).color(CYAN).build(),
		OathboundTechnique.b(POLE_VAULT_KICK, "pole_vault_kick", Shape.CONE, Archetype.UPPERCUT)
			.tell(7).active(6).recovery(10)
			.damage(10.0).knockback(1.00).reach(2.8).half(50.0).lift(0.50).color(CYAN)
			.effect(Effect.LAUNCH, 0).build(),
		OathboundTechnique.b(STORM_JAB, "storm_jab", Shape.MULTI_HIT, Archetype.FLURRY)
			.tell(5).active(8).recovery(8).hitDelay(1)
			.damage(4.5).knockback(0.20).reach(4.0).half(25.0).hits(3).color(CYAN).build(),
		OathboundTechnique.b(CYCLONE_DANCE, "cyclone_dance", Shape.MULTI_HIT, Archetype.SPIN_SWEEP)
			.motion(Motion.DASH).tell(10).active(12).recovery(12).hitDelay(1)
			.damage(6.0).knockback(0.60).reach(4.8).half(180.0).color(CYAN).speed(0.45).hits(3).build(),
		OathboundTechnique.b(LIGHTNING_LUNGE, "lightning_lunge", Shape.LINE, Archetype.THRUST)
			.motion(Motion.DASH).tell(7).active(7).recovery(12)
			.damage(17.0).knockback(1.00).reach(5.2).half(0.9).color(CYAN).speed(1.10).build(),
		OathboundTechnique.b(REAPING_CIRCLE, "reaping_circle", Shape.PULL_RADIUS, Archetype.WEAPON_TWIRL)
			.tell(10).active(6).recovery(14)
			.damage(8.0).knockback(0.0).reach(6.0).color(VIOLET).speed(1.10)
			.effect(Effect.SLOW, 40).build(),
		OathboundTechnique.b(GLAIVE_THROW, "glaive_throw", Shape.LINE, Archetype.OVERHAND_THROW)
			.tell(10).active(8).recovery(14)
			.damage(13.0).knockback(1.20).reach(9.0).half(0.9).color(CYAN).build(),
		OathboundTechnique.b(THUNDER_STOMP, "thunder_stomp", Shape.CROSS, Archetype.STOMP)
			.tell(12).active(6).recovery(14)
			.damage(10.0).knockback(0.80).reach(6.0).inner(1.8).half(1.1).color(CYAN).build(),
		OathboundTechnique.b(RISING_GLAIVE, "rising_glaive", Shape.CONE, Archetype.UPPERCUT)
			.tell(8).active(4).recovery(10)
			.damage(12.0).knockback(0.30).reach(2.8).half(45.0).lift(0.65).color(CYAN)
			.effect(Effect.LAUNCH, 0).build(),
		OathboundTechnique.b(SHADOW_CLONE_STRIKE, "shadow_clone_strike", Shape.TWIN_LINE, Archetype.DIAGONAL_SLASH)
			.tell(8).active(6).recovery(12)
			.damage(14.0).knockback(1.00).reach(4.4).half(0.9).color(VIOLET).build(),
		OathboundTechnique.b(WHIRLWIND_PULL, "whirlwind_pull", Shape.PULL_RADIUS, Archetype.SPIN_SWEEP)
			.tell(10).active(8).recovery(12)
			.damage(7.0).knockback(0.20).reach(5.0).color(VIOLET).speed(1.0)
			.effect(Effect.WEAKNESS, 50).build(),
		OathboundTechnique.b(SKYFALL_CLEAVE, "skyfall_cleave", Shape.LINE, Archetype.LEAP_SLAM)
			.motion(Motion.LEAP).tell(12).active(8).recovery(12).hitDelay(5)
			.damage(16.0).knockback(0.80).reach(5.5).half(1.0).color(CYAN).speed(0.50).build(),
		OathboundTechnique.b(STORMBREAKER, "stormbreaker", Shape.CROSS, Archetype.OVERHEAD_SLAM)
			.tell(16).active(8).recovery(18)
			.damage(18.0).knockback(1.30).reach(6.5).inner(2.4).half(1.3).color(VIOLET)
			.empoweredOnly().build()
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

	public static final double MAX_HEALTH = 520.0;
	public static final double ARMOR = 13.0;
	public static final double ARMOR_TOUGHNESS = 6.0;
	public static final double KNOCKBACK_RESISTANCE = 0.60;
	public static final double MOVEMENT_SPEED = 0.46;
	public static final double FOLLOW_RANGE = 32.0;
	public static final float SCALE = 1.18F;
	public static final int XP_REWARD = 50;
	/** Armor-piercing share for the storm-forged halberd. */
	public static final double PIERCE = 0.34;

	public static final float EMPOWERED_RATIO = 0.5F;
	public static final double EMPOWERED_DAMAGE_MULT = 1.18;
	public static final double EMPOWERED_TELL_SCALE = 0.85;

	// ------------------------------------------------------------------ timings & combat geometry

	public static final int WHIRL_TELL = 11;
	public static final int WHIRL_DURATION = 25;
	public static final double WHIRL_RADIUS = 4.6;
	public static final double WHIRL_DAMAGE = 13.0;
	public static final double WHIRL_KNOCKBACK = 1.05;

	public static final int THRUST_TELL = 9;
	public static final int THRUST_DASH_TICKS = 7;
	public static final int THRUST_DURATION = 24;
	public static final double THRUST_SPEED = 1.45;
	public static final double THRUST_LENGTH = 4.2;
	public static final double THRUST_HALF_WIDTH = 1.25;
	public static final double THRUST_DAMAGE = 15.0;
	public static final double THRUST_KNOCKBACK = 1.20;

	public static final int HOOK_TELL = 10;
	public static final int HOOK_DURATION = 22;
	public static final double HOOK_LENGTH = 5.4;
	public static final double HOOK_HALF_WIDTH = 1.35;
	public static final double HOOK_DAMAGE = 10.0;
	public static final double HOOK_PULL_SPEED = 1.30;
	public static final int HOOK_SLOW_TICKS = 45;

	public static final int CRASH_TELL = 18;
	public static final int CRASH_RECOVERY = 18;
	public static final int CRASH_DURATION = 36;
	public static final double CRASH_CLOSE_RADIUS = 2.4;
	public static final double CRASH_CROSS_LENGTH = 6.0;
	public static final double CRASH_CROSS_HALF_WIDTH = 1.25;
	public static final double CRASH_CLOSE_DAMAGE = 17.0;
	public static final double CRASH_CROSS_DAMAGE = 12.5;
	public static final double CRASH_KNOCKBACK = 1.00;

	public static final int STAGGER_TICKS = 45;
	public static final int MOVE_GAP_TICKS = 10;
	public static final int MOVE_GAP_EMPOWERED = 6;
	public static final int MEMORY = 2;

	public static final double CRASH_RECOVERY_MULTIPLIER = 1.25;
	public static final double STAGGER_MULTIPLIER = 1.45;

	// ------------------------------------------------------------------ pure geometric & state rules

	/** Whether the Halberdier has entered Storm-Glaive Overdrive (&lt; 50% HP). */
	public static boolean empowered(float health, float maxHealth) {
		return maxHealth > 0.0F && (health / maxHealth) < EMPOWERED_RATIO;
	}

	/** Global combat speed multiplier - lower is faster. */
	public static final double SPEED = 0.55;

	/** Windup ticks for a given move, accelerated during Overdrive. */
	public static int tellTicks(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			int base = Math.max(3, (int) Math.round(tech.tell() * SPEED));
			return empowered ? Math.max(2, (int) Math.round(base * EMPOWERED_TELL_SCALE)) : base;
		}
		int base = switch (move) {
			case TEMPEST_WHIRL -> WHIRL_TELL;
			case SHADOWSTEP_THRUST -> THRUST_TELL;
			case HOOK_AND_REAP -> HOOK_TELL;
			case SKY_VAULT_CRASH -> CRASH_TELL;
			default -> 0;
		};
		base = base == 0 ? 0 : Math.max(3, (int) Math.round(base * SPEED));
		return base == 0 ? 0 : Math.max(2, (int) Math.round(base * (empowered ? EMPOWERED_TELL_SCALE : 1.0)));
	}

	/** Total duration of a combat move in ticks. */
	public static int moveDuration(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			return tellTicks(move, empowered)
				+ Math.max(2, (int) Math.round(tech.active() * SPEED))
				+ Math.max(2, (int) Math.round(tech.recovery() * SPEED));
		}
		int tell = tellTicks(move, empowered);
		return switch (move) {
			case TEMPEST_WHIRL -> tell + Math.max(2, (int) Math.round((WHIRL_DURATION - WHIRL_TELL) * SPEED));
			case SHADOWSTEP_THRUST -> tell + Math.max(2, (int) Math.round(THRUST_DASH_TICKS * SPEED)) + Math.max(2, (int) Math.round((THRUST_DURATION - THRUST_TELL - THRUST_DASH_TICKS) * SPEED));
			case HOOK_AND_REAP -> tell + Math.max(2, (int) Math.round((HOOK_DURATION - HOOK_TELL) * SPEED));
			case SKY_VAULT_CRASH -> tell + Math.max(3, (int) Math.round(CRASH_RECOVERY * SPEED));
			case STAGGERED -> STAGGER_TICKS;
			default -> 0;
		};
	}

	/** Forward and lateral signed offsets relative to entity facing yaw (deg). */
	private static double[] localCoords(float facingYawDeg, double dx, double dz) {
		double rad = Math.toRadians(facingYawDeg);
		double fx = -Math.sin(rad);
		double fz = Math.cos(rad);
		double along = dx * fx + dz * fz;
		double across = dx * (-fz) + dz * fx;
		return new double[]{along, across};
	}

	/** True when a target lies within the 360-degree Tempest Whirl sweep radius. */
	public static boolean inWhirlRadius(double dx, double dz) {
		return Math.hypot(dx, dz) <= WHIRL_RADIUS;
	}

	/** True when a target stands along the Shadowstep Thrust spear corridor. */
	public static boolean inThrustLine(float facingYawDeg, double dx, double dz) {
		double[] c = localCoords(facingYawDeg, dx, dz);
		return c[0] >= 0.0 && c[0] <= THRUST_LENGTH && Math.abs(c[1]) <= THRUST_HALF_WIDTH;
	}

	/** True when a target stands inside the Hook-and-Reap snag corridor. */
	public static boolean inHookLine(float facingYawDeg, double dx, double dz) {
		double[] c = localCoords(facingYawDeg, dx, dz);
		return c[0] >= 0.0 && c[0] <= HOOK_LENGTH && Math.abs(c[1]) <= HOOK_HALF_WIDTH;
	}

	/** True when a target lies in the close impact zone or any of the four orthogonal Thunderbolt Crash cross arms. */
	public static boolean inThunderCross(float facingYawDeg, double dx, double dz) {
		if (Math.hypot(dx, dz) <= CRASH_CLOSE_RADIUS) {
			return true;
		}
		double[] c = localCoords(facingYawDeg, dx, dz);
		double absAlong = Math.abs(c[0]);
		double absAcross = Math.abs(c[1]);
		boolean nsArm = absAlong <= CRASH_CROSS_LENGTH && absAcross <= CRASH_CROSS_HALF_WIDTH;
		boolean ewArm = absAcross <= CRASH_CROSS_LENGTH && absAlong <= CRASH_CROSS_HALF_WIDTH;
		return nsArm || ewArm;
	}

	/** Crash damage scaled by epicenter vs cross-arm distance and Overdrive state. */
	public static double crashDamage(double horizontalDist, boolean empowered) {
		double base = horizontalDist <= CRASH_CLOSE_RADIUS ? CRASH_CLOSE_DAMAGE : CRASH_CROSS_DAMAGE;
		return empowered ? base * EMPOWERED_DAMAGE_MULT : base;
	}

	/** Heavy axes shatter vulnerable halberd windups into a stagger opening. */
	public static boolean axeInterruptsWindup(int move, long ticksSinceMoveBegan, int tell, boolean isAxe) {
		return isAxe
			&& ticksSinceMoveBegan < tell
			&& (move == TEMPEST_WHIRL || move == HOOK_AND_REAP || move == SKY_VAULT_CRASH);
	}

	/** Incoming damage multiplier during staggered opening vs crash recovery vs active combat poise. */
	public static double damageMultiplier(int move, long ticksSinceMoveBegan, int tell) {
		if (move == STAGGERED) {
			return STAGGER_MULTIPLIER;
		}
		if (move == SKY_VAULT_CRASH && ticksSinceMoveBegan > tell) {
			return CRASH_RECOVERY_MULTIPLIER;
		}
		return 1.0;
	}

	/** Cooldown in ticks between uses of a specific combat move. */
	public static int moveCooldown(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			int base = Math.max(10, (int) Math.round((tech.tell() + tech.recovery()) * SPEED));
			return empowered ? Math.max(8, (int) Math.round(base * 0.78)) : base;
		}
		int base = switch (move) {
			case TEMPEST_WHIRL -> 34;
			case SHADOWSTEP_THRUST -> 48;
			case HOOK_AND_REAP -> 68;
			case SKY_VAULT_CRASH -> 88;
			default -> 20;
		};
		base = Math.max(10, (int) Math.round(base * SPEED));
		return empowered ? Math.max(8, (int) Math.round(base * 0.78)) : base;
	}
}
