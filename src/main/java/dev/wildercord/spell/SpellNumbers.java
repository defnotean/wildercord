package dev.wildercord.spell;

/**
 * Every tunable number a modifier changes, in one place, so the readout the player sees
 * and the spell the server runs can never disagree.
 */
public final class SpellNumbers {
	private SpellNumbers() {}

	public static final int MAX_COPIES = 9;
	public static final int MAX_ECHOES = 3;

	public static double power(SpellPlan.EffectNode e) {
		return Math.pow(1.5, e.count(Runes.AMPLIFY)) * Math.pow(0.6, e.count(Runes.FRUGAL_MOD))
			* Math.pow(2.5, e.count(Runes.OVERCHARGE_MOD)) * Math.pow(1.5, e.count(Runes.FOCUS_MOD));
	}

	/** Focus and Vow on a shape, times the shape's own strength per hit (Stand and Barrage hit often, so softer). */
	public static double groupPower(SpellPlan.Group g) {
		return Math.pow(1.5, g.count(Runes.FOCUS_MOD)) * Math.pow(2.0, g.count(Runes.VOW_MOD)) * shapeStrength(g.shape);
	}

	/** Power per hit for shapes that hit many times. */
	public static double shapeStrength(RuneDef shape) {
		if (shape.is(Runes.STAND.id())) {
			return 0.6;
		}
		if (shape.is(Runes.BARRAGE.id())) {
			return 0.35;
		}
		return 1.0;
	}

	/** Execute on an effect: power multiplier against targets under half health. */
	public static double executeBonus(SpellPlan.EffectNode e) {
		return Math.pow(2.0, e.count(Runes.EXECUTE_MOD));
	}

	public static int standSeconds(SpellPlan.Group g) {
		return (int) Math.round(8 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	/** Ticks between a Stand's strikes: 15, halved by each Quicken. */
	public static int standInterval(SpellPlan.Group g) {
		return Math.max(4, (int) Math.round(15 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	public static double standReach(SpellPlan.Group g) {
		return 5.0 * shapeRadius(g);
	}

	/** Domain radius: 9 blocks, widened up to 24 at most (beyond that it can't be seen or kept up). */
	public static final double MAX_DOMAIN_RADIUS = 24.0;

	public static double domainRadius(SpellPlan.Group g) {
		return Math.min(MAX_DOMAIN_RADIUS, 9.0 * shapeRadius(g));
	}

	public static int domainSeconds(SpellPlan.Group g) {
		return (int) Math.round(6 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	/** Ticks between a Domain's strikes: 20, halved by each Quicken. */
	public static int domainInterval(SpellPlan.Group g) {
		return Math.max(5, (int) Math.round(20 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	public static double crescentWidth(SpellPlan.Group g) {
		return 5.0 * shapeRadius(g);
	}

	/** Crescent speed in blocks per tick. */
	public static double crescentSpeed(SpellPlan.Group g) {
		return 1.5 * Math.pow(2.0, g.count(Runes.QUICKEN));
	}

	public static int barrageBlows(SpellPlan.Group g) {
		return Math.min(20, 8 + 4 * g.count(Runes.QUICKEN));
	}

	public static double orbRadius(SpellPlan.Group g) {
		return 2.0 * shapeRadius(g);
	}

	/** Orb speed in blocks per tick. */
	public static double orbSpeed(SpellPlan.Group g) {
		return 0.5 * Math.pow(1.6, g.count(Runes.QUICKEN));
	}

	public static double blitzWidth(SpellPlan.Group g) {
		return 1.2 * shapeRadius(g);
	}

	/** Radius factor on a shape from Widen and Focus. */
	private static double shapeRadius(SpellPlan.Group g) {
		return Math.pow(1.5, g.count(Runes.WIDEN)) * Math.pow(0.5, g.count(Runes.FOCUS_MOD));
	}

	public static double duration(SpellPlan.EffectNode e) {
		return Math.pow(2.0, e.count(Runes.EXTEND)) * Math.pow(0.6, e.count(Runes.FRUGAL_MOD));
	}

	/** Extra times a lingering effect lands, one second apart. */
	public static int lingerHits(SpellPlan.EffectNode e) {
		return Math.min(6, 2 * e.count(Runes.LINGER_MOD));
	}

	/** Shots fired in succession by Volley (1 without it). */
	public static int volleyShots(SpellPlan.Group g) {
		return Math.min(9, 1 + 2 * g.count(Runes.VOLLEY_MOD));
	}

	public static double coneLength(SpellPlan.Group g) {
		return 6.0 * shapeRadius(g);
	}

	public static double wallWidth(SpellPlan.Group g) {
		return 7.0 * shapeRadius(g);
	}

	public static int wallSeconds(SpellPlan.Group g) {
		return (int) Math.round(5 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	/** Ticks between a Wall's hits: 10, halved by each Quicken. */
	public static int wallInterval(SpellPlan.Group g) {
		return Math.max(4, (int) Math.round(10 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	public static int orbs(SpellPlan.Group g) {
		return Math.min(MAX_COPIES, 3 * copies(g));
	}

	public static int orbitSeconds(SpellPlan.Group g) {
		return (int) Math.round(8 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	public static int trailSeconds(SpellPlan.Group g) {
		return (int) Math.round(5 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	/** Arc speed in blocks per tick. */
	public static double arcSpeed(SpellPlan.Group g) {
		return 1.1 * Math.pow(1.5, g.count(Runes.QUICKEN));
	}

	public static final int PULSES = 3;

	/** Ticks between Pulse firings: 20, halved by each Quicken. */
	public static int pulseInterval(SpellPlan.Link link) {
		return Math.max(5, (int) Math.round(20 / Math.pow(2.0, link.count(Runes.QUICKEN))));
	}

	public static double effectRadius(SpellPlan.EffectNode e) {
		return Math.pow(1.5, e.count(Runes.WIDEN)) * Math.pow(0.5, e.count(Runes.FOCUS_MOD));
	}

	public static int copies(SpellPlan.Group g) {
		return (int) Math.min(MAX_COPIES, Math.pow(3, g.count(Runes.SPLIT_MOD)));
	}

	public static double burstRadius(SpellPlan.Group g) {
		return 4.0 * shapeRadius(g);
	}

	public static double zoneRadius(SpellPlan.Group g) {
		return 3.0 * shapeRadius(g);
	}

	public static int zoneSeconds(SpellPlan.Group g) {
		return (int) Math.round(6 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	/** Ticks between a Zone's pulses: 20, halved by each Quicken. */
	public static int zoneInterval(SpellPlan.Group g) {
		return Math.max(5, (int) Math.round(20 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	public static double rainRadius(SpellPlan.Group g) {
		return 4.0 * shapeRadius(g);
	}

	public static double ringRadius(SpellPlan.Group g) {
		return 7.0 * shapeRadius(g);
	}

	public static double pillarRadius(SpellPlan.Group g) {
		return 1.5 * shapeRadius(g);
	}

	public static double waveWidth(SpellPlan.Group g) {
		return 3.0 * shapeRadius(g);
	}

	/** Wave speed in blocks per tick. */
	public static double waveSpeed(SpellPlan.Group g) {
		return Math.pow(2.0, g.count(Runes.QUICKEN));
	}

	public static double mineRadius(SpellPlan.Group g) {
		return 3.0 * shapeRadius(g);
	}

	public static double totemRadius(SpellPlan.Group g) {
		return 5.0 * shapeRadius(g);
	}

	public static int totemSeconds(SpellPlan.Group g) {
		return (int) Math.round(10 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	/** Ticks between a Totem's pulses: 40, halved by each Quicken. */
	public static int totemInterval(SpellPlan.Group g) {
		return Math.max(10, (int) Math.round(40 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	public static int pierce(SpellPlan.Group g) {
		return 3 * g.count(Runes.PIERCE_MOD);
	}

	public static int bounces(SpellPlan.Group g) {
		return 3 * g.count(Runes.BOUNCE_MOD);
	}

	public static int chainJumps(SpellPlan.Group g) {
		return 3 * g.count(Runes.CHAIN_MOD);
	}

	public static boolean homing(SpellPlan.Group g) {
		return g.count(Runes.HOMING_MOD) > 0;
	}

	/** Bolt speed in blocks per tick. */
	public static double boltSpeed(SpellPlan.Group g) {
		return 1.6 * Math.pow(2.0, g.count(Runes.QUICKEN));
	}

	public static int delayTicks(SpellPlan.Link link) {
		return Math.max(2, (int) Math.round(20 * Math.pow(2.0, link.count(Runes.EXTEND)) / Math.pow(2.0, link.count(Runes.QUICKEN))));
	}

	public static double explodeRadius(SpellPlan.EffectNode e) {
		return 3.5 * effectRadius(e);
	}

	/** Cooldown in ticks: one tick per mana, between 0.5 s and 20 s. */
	public static int cooldownTicks(double cost) {
		return (int) Math.max(10, Math.min(400, Math.round(cost)));
	}

	/** Cooldown after Rapid: halved per Rapid anywhere in the spell, never under a quarter second. */
	public static int cooldownTicks(double cost, int rapid) {
		return cooldownTicks(cost, rapid, 0);
	}

	/** Cooldown after Rapid and Vow: each Vow makes it 4x longer, up to a minute. */
	public static int cooldownTicks(double cost, int rapid, int vows) {
		double ticks = cooldownTicks(cost) * Math.pow(0.5, rapid) * Math.pow(4.0, vows);
		return (int) Math.max(5, Math.min(1200, Math.round(ticks)));
	}

	/** Blood Price: health paid instead of mana, 1 per 5 mana, at least 1. */
	public static int healthCost(double cost) {
		return (int) Math.max(1, Math.ceil(cost / 5.0 - 1e-9));
	}
}
