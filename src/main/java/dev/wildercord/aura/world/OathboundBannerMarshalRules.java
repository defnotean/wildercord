package dev.wildercord.aura.world;

import dev.wildercord.aura.world.OathboundTechnique.Archetype;
import dev.wildercord.aura.world.OathboundTechnique.Effect;
import dev.wildercord.aura.world.OathboundTechnique.Motion;
import dev.wildercord.aura.world.OathboundTechnique.Shape;

/**
 * Pure combat, timing, scaling, command-aura, and shield-guard rules for {@link OathboundBannerMarshal} — Knight #4
 * (Way of the Sovereign Banner). Kept free of Minecraft runtime state so every rule window, command aura radius,
 * solar guard arc, axe guard-break, and judgment pillar falloff is directly unit-tested.
 */
public final class OathboundBannerMarshalRules {
	private OathboundBannerMarshalRules() {}

	// ------------------------------------------------------------------ move ids (matching 6-panel storyboard)

	/** Regal upright march with Sovereign Sunblade & War-Banner high (Animation Panel 1). */
	public static final int SOVEREIGN_MARCH = 0;
	/** Skyward Sunblade decree casting a golden command ring that rallies allied knights (Animation Panel 2). */
	public static final int DECREE_OF_THE_STANDARD = 1;
	/** Locked Solar Aegis chest guard blocking frontal hits, reflecting arrows & counter-thrusting (Animation Panel 3). */
	public static final int SOLAR_AEGIS_GUARD = 2;
	/** Wide diagonal golden broadsword sweep (Animation Panel 4). */
	public static final int RADIANT_SUNBLADE_CLEAVE = 3;
	/** Overhead Heaven's Lance windup -> downward command calling pillars of solar light (Animation Panels 5 & 6). */
	public static final int SOVEREIGN_JUDGMENT = 4;
	/** Guard shattered by a heavy axe during Solar Aegis Guard. */
	public static final int STAGGERED = 5;

	// ------------------------------------------------------------------ expanded technique sheet (ids 6..18)

	public static final int BANNER_SWEEP = 6;
	public static final int SUNLANCE_THRUST = 7;
	public static final int RALLYING_CRY = 8;
	public static final int SOLAR_FLARE = 9;
	public static final int BANNER_CHARGE = 10;
	public static final int JUDGMENT_BEAM = 11;
	public static final int AEGIS_BASH = 12;
	public static final int DAWN_UPPERCUT = 13;
	public static final int CONSECRATED_GROUND = 14;
	public static final int HEAVENLY_LANCE = 15;
	public static final int SOVEREIGN_RIPOSTE = 16;
	public static final int BANNER_PLANT = 17;
	public static final int SOLAR_ECLIPSE = 18;

	public static final int[] COMBAT_MOVES = {
		DECREE_OF_THE_STANDARD, SOLAR_AEGIS_GUARD, RADIANT_SUNBLADE_CLEAVE, SOVEREIGN_JUDGMENT,
		BANNER_SWEEP, SUNLANCE_THRUST, RALLYING_CRY, SOLAR_FLARE, BANNER_CHARGE, JUDGMENT_BEAM,
		AEGIS_BASH, DAWN_UPPERCUT, CONSECRATED_GROUND, HEAVENLY_LANCE, SOVEREIGN_RIPOSTE,
		BANNER_PLANT, SOLAR_ECLIPSE
	};

	private static final int SOLAR = 0xFFE066;
	private static final int DAWN = 0xFFB040;

	private static final OathboundTechnique[] TECHNIQUES = {
		OathboundTechnique.b(BANNER_SWEEP, "banner_sweep", Shape.WIDE_ARC, Archetype.HORIZONTAL_SWEEP)
			.tell(9).active(6).recovery(12)
			.damage(12.0).knockback(1.00).reach(4.4).half(100.0).color(SOLAR).build(),
		OathboundTechnique.b(SUNLANCE_THRUST, "sunlance_thrust", Shape.LINE, Archetype.THRUST)
			.tell(8).active(6).recovery(12)
			.damage(14.0).knockback(0.90).reach(4.8).half(0.8).color(SOLAR).build(),
		OathboundTechnique.b(RALLYING_CRY, "rallying_cry", Shape.RADIUS, Archetype.ROAR)
			.tell(10).active(8).recovery(16)
			.damage(4.0).knockback(1.00).reach(7.0).color(SOLAR)
			.effect(Effect.FEAR, 70).build(),
		OathboundTechnique.b(SOLAR_FLARE, "solar_flare", Shape.RADIUS, Archetype.CAST_RAISE)
			.tell(12).active(6).recovery(16)
			.damage(6.0).knockback(0.60).reach(5.5).color(SOLAR)
			.effect(Effect.BLIND, 60).build(),
		OathboundTechnique.b(BANNER_CHARGE, "banner_charge", Shape.LINE, Archetype.SHIELD_PUSH)
			.motion(Motion.DASH).tell(10).active(10).recovery(12)
			.damage(10.0).knockback(1.40).reach(2.4).half(1.7).color(SOLAR).speed(0.80).build(),
		OathboundTechnique.b(JUDGMENT_BEAM, "judgment_beam", Shape.LINE, Archetype.CAST_RAISE)
			.tell(14).active(8).recovery(14)
			.damage(16.0).knockback(0.50).reach(10.0).half(0.7).color(SOLAR).build(),
		OathboundTechnique.b(AEGIS_BASH, "aegis_bash", Shape.CONE, Archetype.SHIELD_PUSH)
			.tell(8).active(5).recovery(10)
			.damage(10.0).knockback(1.50).reach(3.0).half(60.0).color(SOLAR)
			.effect(Effect.SLOW, 40).build(),
		OathboundTechnique.b(DAWN_UPPERCUT, "dawn_uppercut", Shape.CONE, Archetype.UPPERCUT)
			.tell(7).active(4).recovery(10)
			.damage(10.0).knockback(0.30).reach(2.6).half(45.0).lift(0.60).color(DAWN)
			.effect(Effect.LAUNCH, 0).build(),
		OathboundTechnique.b(CONSECRATED_GROUND, "consecrated_ground", Shape.RING, Archetype.KNEEL_STRIKE)
			.tell(12).active(10).recovery(14)
			.damage(5.0).knockback(0.20).reach(5.5).inner(1.5).color(SOLAR)
			.effect(Effect.WITHER, 60).build(),
		OathboundTechnique.b(HEAVENLY_LANCE, "heavenly_lance", Shape.RADIUS, Archetype.OVERHAND_THROW)
			.tell(14).active(8).recovery(16)
			.damage(9.0).knockback(0.60).reach(6.5).color(SOLAR)
			.effect(Effect.WEAKNESS, 40).build(),
		OathboundTechnique.b(SOVEREIGN_RIPOSTE, "sovereign_riposte", Shape.CONE, Archetype.GUARD_BRACE)
			.tell(4).active(8).recovery(14)
			.damage(13.0).knockback(1.20).reach(3.8).half(80.0).color(SOLAR).build(),
		OathboundTechnique.b(BANNER_PLANT, "banner_plant", Shape.RADIUS, Archetype.CAST_RAISE)
			.tell(10).active(10).recovery(14)
			.damage(7.0).knockback(0.40).reach(5.0).color(SOLAR)
			.effect(Effect.SLOW, 60).build(),
		OathboundTechnique.b(SOLAR_ECLIPSE, "solar_eclipse", Shape.CROSS, Archetype.OVERHEAD_SLAM)
			.tell(18).active(8).recovery(18)
			.damage(19.0).knockback(1.40).reach(6.8).inner(2.6).half(1.3).color(SOLAR)
			.effect(Effect.BLIND, 50).empoweredOnly().build()
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

	public static final double MAX_HEALTH = 600.0;
	public static final double ARMOR = 17.0;
	public static final double ARMOR_TOUGHNESS = 8.0;
	public static final double KNOCKBACK_RESISTANCE = 0.80;
	public static final double MOVEMENT_SPEED = 0.26;
	public static final double FOLLOW_RANGE = 32.0;
	public static final float SCALE = 1.24F;
	public static final int XP_REWARD = 55;
	/** Armor-piercing share for the radiant Sovereign Sunblade. */
	public static final double PIERCE = 0.30;

	public static final float EMPOWERED_RATIO = 0.5F;
	public static final double EMPOWERED_DAMAGE_MULT = 1.16;
	public static final double EMPOWERED_TELL_SCALE = 0.85;

	// ------------------------------------------------------------------ timings & combat geometry

	public static final int DECREE_TELL = 10;
	public static final int DECREE_DURATION = 26;
	public static final double DECREE_RADIUS = 12.0;
	public static final float DECREE_HEAL = 8.0F;
	public static final int DECREE_BUFF_TICKS = 160;

	public static final int GUARD_TELL = 5;
	public static final int GUARD_WINDOW = 28;
	public static final int GUARD_COUNTER_TICKS = 11;
	public static final int GUARD_DURATION = GUARD_TELL + GUARD_WINDOW + 5;
	public static final double COUNTER_REACH = 4.0;
	public static final double COUNTER_HALF_ANGLE_DEG = 60.0;
	public static final double COUNTER_DAMAGE = 13.5;
	public static final double COUNTER_KNOCKBACK = 1.15;

	public static final int CLEAVE_TELL = 11;
	public static final int CLEAVE_DURATION = 24;
	public static final double CLEAVE_REACH = 4.1;
	public static final double CLEAVE_HALF_ANGLE_DEG = 75.0;
	public static final double CLEAVE_DAMAGE = 14.0;
	public static final double CLEAVE_KNOCKBACK = 0.90;

	public static final int JUDGMENT_TELL = 20;
	public static final int JUDGMENT_RECOVERY = 18;
	public static final int JUDGMENT_DURATION = JUDGMENT_TELL + JUDGMENT_RECOVERY;
	public static final double JUDGMENT_INNER_RADIUS = 2.5;
	public static final double JUDGMENT_OUTER_RADIUS = 5.5;
	public static final double JUDGMENT_INNER_DAMAGE = 17.0;
	public static final double JUDGMENT_OUTER_DAMAGE = 11.5;
	public static final double JUDGMENT_KNOCKBACK = 0.85;

	public static final int STAGGER_AXE_TICKS = 50;
	public static final int MOVE_GAP_TICKS = 12;
	public static final int MOVE_GAP_EMPOWERED = 8;
	public static final int MEMORY = 2;

	public static final double GUARD_HALF_ARC = 70.0;
	public static final double GUARD_MULTIPLIER = 0.15;
	public static final double JUDGMENT_RECOVERY_MULTIPLIER = 1.25;
	public static final double STAGGER_MULTIPLIER = 1.45;

	// ------------------------------------------------------------------ pure geometric & state rules

	/** Whether the Banner-Marshal has ignited Solar Sovereignty (&lt; 50% HP). */
	public static boolean empowered(float health, float maxHealth) {
		return maxHealth > 0.0F && (health / maxHealth) < EMPOWERED_RATIO;
	}

	/** Windup ticks for a given move, sped up slightly when Empowered. */
	public static int tellTicks(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			return empowered ? Math.max(4, (int) Math.round(tech.tell() * EMPOWERED_TELL_SCALE)) : tech.tell();
		}
		int base = switch (move) {
			case DECREE_OF_THE_STANDARD -> DECREE_TELL;
			case SOLAR_AEGIS_GUARD -> GUARD_TELL;
			case RADIANT_SUNBLADE_CLEAVE -> CLEAVE_TELL;
			case SOVEREIGN_JUDGMENT -> JUDGMENT_TELL;
			default -> 0;
		};
		return base == 0 ? 0 : Math.max(4, (int) Math.round(base * (empowered ? EMPOWERED_TELL_SCALE : 1.0)));
	}

	/** Total duration of a scripted combat move in ticks. */
	public static int moveDuration(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			return tellTicks(move, empowered) + tech.active() + tech.recovery();
		}
		int tell = tellTicks(move, empowered);
		return switch (move) {
			case DECREE_OF_THE_STANDARD -> tell + (DECREE_DURATION - DECREE_TELL);
			case SOLAR_AEGIS_GUARD -> tell + GUARD_WINDOW + 5;
			case RADIANT_SUNBLADE_CLEAVE -> tell + (CLEAVE_DURATION - CLEAVE_TELL);
			case SOVEREIGN_JUDGMENT -> tell + JUDGMENT_RECOVERY;
			case STAGGERED -> STAGGER_AXE_TICKS;
			default -> 0;
		};
	}

	/** Angle in degrees [0..180] between facing vector and vector to attacker. */
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

	/** True when Solar Aegis Guard shield stance is actively defending. */
	public static boolean guardActive(int move, long ticksSinceBegan, int tell, boolean countered) {
		return move == SOLAR_AEGIS_GUARD
			&& !countered
			&& ticksSinceBegan >= tell
			&& ticksSinceBegan <= tell + GUARD_WINDOW;
	}

	/** True when an attacker stands in front of the Solar Aegis shield arc. */
	public static boolean blocksFrontal(float facingYawDeg, double dx, double dz) {
		return angleFromFacing(facingYawDeg, dx, dz) <= GUARD_HALF_ARC;
	}

	/** Heavy axes smash straight through Solar Aegis Guard and stagger the Banner-Marshal. */
	public static boolean axeBreaksGuard(int move, long ticksSinceBegan, int tell, boolean countered,
			boolean isAxe, float facingYawDeg, double dx, double dz) {
		return isAxe && guardActive(move, ticksSinceBegan, tell, countered) && blocksFrontal(facingYawDeg, dx, dz);
	}

	/** Incoming damage multiplier based on stagger recovery vs active solar guard. */
	public static double damageMultiplier(int move, long ticksSinceBegan, int tell, boolean countered,
			boolean unblockable, float facingYawDeg, double dx, double dz) {
		if (move == STAGGERED) {
			return STAGGER_MULTIPLIER;
		}
		if (move == SOVEREIGN_JUDGMENT && ticksSinceBegan > tell) {
			return JUDGMENT_RECOVERY_MULTIPLIER;
		}
		if (!unblockable && guardActive(move, ticksSinceBegan, tell, countered) && blocksFrontal(facingYawDeg, dx, dz)) {
			return GUARD_MULTIPLIER;
		}
		return 1.0;
	}

	/** True when a target lies within a horizontal slash sector (Sunblade Cleave / Counter-Thrust). */
	public static boolean inSunbladeArc(float facingYawDeg, double dx, double dz, double reach, double halfAngleDeg) {
		return Math.hypot(dx, dz) <= reach && angleFromFacing(facingYawDeg, dx, dz) <= halfAngleDeg;
	}

	/** True when an allied knight stands inside the Decree of the Standard command circle. */
	public static boolean inDecreeAura(double dx, double dz) {
		return Math.hypot(dx, dz) <= DECREE_RADIUS;
	}

	/** Judgment pillar damage inside the concentric golden sigil ring (or 0 outside). */
	public static double judgmentDamage(double dist, boolean empowered) {
		if (dist > JUDGMENT_OUTER_RADIUS) {
			return 0.0;
		}
		double base = dist <= JUDGMENT_INNER_RADIUS ? JUDGMENT_INNER_DAMAGE : JUDGMENT_OUTER_DAMAGE;
		return empowered ? base * EMPOWERED_DAMAGE_MULT : base;
	}

	/** Cooldown in ticks between uses of a specific combat move. */
	public static int moveCooldown(int move, boolean empowered) {
		OathboundTechnique tech = technique(move);
		if (tech != null) {
			int base = Math.max(20, tech.tell() + tech.recovery());
			return empowered ? Math.max(16, (int) Math.round(base * 0.78)) : base;
		}
		int base = switch (move) {
			case DECREE_OF_THE_STANDARD -> 140;
			case SOLAR_AEGIS_GUARD -> 75;
			case RADIANT_SUNBLADE_CLEAVE -> 24;
			case SOVEREIGN_JUDGMENT -> 95;
			default -> 20;
		};
		return empowered ? Math.max(20, (int) Math.round(base * 0.78)) : base;
	}
}
