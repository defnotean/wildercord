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
			* Math.pow(2.5, e.count(Runes.OVERCHARGE_MOD)) * Math.pow(1.5, e.count(Runes.FOCUS_MOD)) * RuneNumbers.power(e.mods) * Math.pow(1.2, e.count(Runes.KINDLED));
	}

	/** Focus and Vow on a shape, times the shape's own strength per hit (Barrage hits often, so softer). */
	public static double groupPower(SpellPlan.Group g) {
		return Math.pow(1.5, g.count(Runes.FOCUS_MOD)) * Math.pow(2.0, g.count(Runes.VOW_MOD)) * shapeStrength(g.shape);
	}

	/** Power per hit for shapes that hit many times (Barrage, Stream), and for the cheap Spark. */
	public static double shapeStrength(RuneDef shape) {
		if (shape.is(Runes.BARRAGE.id()) || shape.is(Runes.STREAM.id())) {
			return 0.35;
		}
		if (shape.is(Runes.SPARK.id())) {
			return 0.75;
		}
		return 1.0;
	}

	/** Execute on an effect: power multiplier against targets under half health. */
	public static double executeBonus(SpellPlan.EffectNode e) {
		return Math.pow(2.0, e.count(Runes.EXECUTE_MOD));
	}

	// ---- runes of the world

	/** Trial Key on an effect: power multiplier against targets at full health (1 = none). */
	public static double trialKeyBonus(SpellPlan.EffectNode e) {
		return Math.pow(1.6, e.count(Runes.TRIAL_KEY));
	}

	/** Kindled on an effect: seconds it sets what it hits alight (0 = none). */
	public static int kindledSeconds(SpellPlan.EffectNode e) {
		return e.count(Runes.KINDLED) > 0 ? 4 * e.count(Runes.KINDLED) : 0;
	}

	/** Unstable on an effect: the lowest and highest power it can swing to. */
	public static final double UNSTABLE_LOW = 0.5;
	public static final double UNSTABLE_HIGH = 2.0;

	/** One roll of Unstable's swing, from {@code roll} in [0, 1): as likely to weaken as to strengthen. */
	public static double unstableSwing(SpellPlan.EffectNode e, double roll) {
		if (e.count(Runes.UNSTABLE) == 0) {
			return 1.0;
		}
		// Log-uniform between half and double: the average cast is about as strong as a steady one.
		return Math.exp(Math.log(UNSTABLE_LOW) + roll * (Math.log(UNSTABLE_HIGH) - Math.log(UNSTABLE_LOW)));
	}

	/** How far a Vortex drags creatures in from. */
	public static double vortexRadius(SpellPlan.Group g) {
		return 5.0 * shapeRadius(g);
	}

	/** The Vortex's eye, where it strikes. */
	public static double vortexEye(SpellPlan.Group g) {
		return 1.8 * shapeRadius(g);
	}

	public static int vortexSeconds(SpellPlan.Group g) {
		return (int) Math.round(3 * Math.pow(2.0, g.count(Runes.EXTEND)));
	}

	/** Ticks between a Vortex's strikes. */
	public static final int VORTEX_INTERVAL = 10;

	/** A Snare's tripwire: how far it reaches, how long it waits, and how wide it springs. */
	public static final double SNARE_LENGTH = 12.0;
	public static final int SNARE_SECONDS = 30;

	public static double snareRadius(SpellPlan.Group g) {
		return 2.5 * shapeRadius(g);
	}

	/** How far from you a Constellation finds its stars, and how many. */
	public static double constellationRange(SpellPlan.Group g) {
		return 12.0 * shapeRadius(g);
	}

	public static final int CONSTELLATION_STARS = 5;

	/**
	 * Quicken makes a flying shape at most this many times as fast (three Quickens on a bolt). Past that it only
	 * lengthened each step of a flight, until one step crossed hundreds of blocks and loaded the world along it.
	 */
	public static final double MAX_QUICKEN = 8.0;

	/** A flying shape's speed factor from Quicken: {@code each} per Quicken, up to {@link #MAX_QUICKEN}. */
	private static double quickened(SpellPlan.Group g, double each) {
		return Math.min(MAX_QUICKEN, Math.pow(each, g.count(Runes.QUICKEN)));
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
		return 1.5 * quickened(g, 2.0);
	}

	public static int barrageBlows(SpellPlan.Group g) {
		return Math.min(20, 8 + 4 * g.count(Runes.QUICKEN));
	}

	public static double orbRadius(SpellPlan.Group g) {
		return 2.0 * shapeRadius(g);
	}

	/** Orb speed in blocks per tick. */
	public static double orbSpeed(SpellPlan.Group g) {
		return 0.5 * quickened(g, 1.6);
	}

	public static double blitzWidth(SpellPlan.Group g) {
		return 1.2 * shapeRadius(g);
	}

	// ---- batch 6: sparks, energy balls and beams

	public static final double SPARK_RANGE = 16.0;
	public static final double RAY_RANGE = 10.0;
	public static final double LANCE_RANGE = 16.0;
	public static final double PRISM_RANGE = 16.0;
	/** How far each of a Prism's three rays reaches beyond where it split. */
	public static final double PRISM_RAY_RANGE = 10.0;
	public static final double STREAM_RANGE = 20.0;
	public static final double COMET_RANGE = 24.0;
	/** How far a Wisp looks for an enemy to chase, and how long it flies. */
	public static final double WISP_SEEK = 16.0;
	public static final int WISP_TICKS = 80;
	public static final int CLUSTER_SHARDS = 5;
	/** A Stream strikes over this many ticks. */
	public static final int STREAM_TICKS = 20;

	/** Spark speed in blocks per tick. */
	public static double sparkSpeed(SpellPlan.Group g) {
		return 2.4 * quickened(g, 2.0);
	}

	public static double novaRadius(SpellPlan.Group g) {
		return 2.5 * shapeRadius(g);
	}

	/** Wisp speed in blocks per tick. */
	public static double wispSpeed(SpellPlan.Group g) {
		return 0.7 * quickened(g, 1.5);
	}

	/** Comet speed in blocks per tick. */
	public static double cometSpeed(SpellPlan.Group g) {
		return 1.1 * quickened(g, 1.6);
	}

	public static double cometRadius(SpellPlan.Group g) {
		return 3.0 * shapeRadius(g);
	}

	/** Ricochet speed in blocks per tick, as it leaves the hand. */
	public static double ricochetSpeed(SpellPlan.Group g) {
		return 1.2 * quickened(g, 1.5);
	}

	/** Bounces before a Ricochet stops: 4, and each Bounce adds 3. */
	public static int ricochetBounces(SpellPlan.Group g) {
		return 4 + bounces(g);
	}

	/** Cluster speed in blocks per tick. */
	public static double clusterSpeed(SpellPlan.Group g) {
		return 1.2 * quickened(g, 1.6);
	}

	/** How far around each of a Cluster's shards it strikes. */
	public static double clusterRadius(SpellPlan.Group g) {
		return 1.5 * shapeRadius(g);
	}

	/** How close to a Lance's line a creature must be to be run through. */
	public static double lanceWidth(SpellPlan.Group g) {
		return 0.7 * shapeRadius(g);
	}

	public static double sweepLength(SpellPlan.Group g) {
		return 10.0 * shapeRadius(g);
	}

	/** Ticks a Sweep takes to cross from one side to the other: 10, halved by each Quicken. */
	public static int sweepTicks(SpellPlan.Group g) {
		return Math.max(3, (int) Math.round(10 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	/** Strikes in a Stream's second: 6, doubled by each Quicken (12 at most). */
	public static int streamStrikes(SpellPlan.Group g) {
		return (int) Math.min(12, 6 * Math.pow(2.0, g.count(Runes.QUICKEN)));
	}

	/** Radius factor on a shape from Widen and Focus (and add-on radius modifiers). */
	public static double shapeRadius(SpellPlan.Group g) {
		return Math.pow(1.5, g.count(Runes.WIDEN)) * Math.pow(0.5, g.count(Runes.FOCUS_MOD)) * RuneNumbers.radius(g.shapeMods);
	}

	public static double duration(SpellPlan.EffectNode e) {
		return Math.pow(2.0, e.count(Runes.EXTEND)) * Math.pow(0.6, e.count(Runes.FRUGAL_MOD)) * RuneNumbers.duration(e.mods);
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

	/** Ticks between a Wall's hits: 20 (once a second, as a Wall has always struck), halved by each Quicken. */
	public static int wallInterval(SpellPlan.Group g) {
		return Math.max(4, (int) Math.round(20 / Math.pow(2.0, g.count(Runes.QUICKEN))));
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
		return 1.1 * quickened(g, 1.5);
	}

	public static final int PULSES = 3;

	/** How many times something imbued can release what it holds (the stored part is paid for this many times). */
	public static final int IMBUE_CHARGES = 3;

	/** How long a Shield holds, in ticks: 30 s, doubled by each Extend. */
	public static int shieldTicks(SpellPlan.EffectNode e) {
		return (int) Math.round(600 * duration(e));
	}

	/** Ticks between Pulse firings: 20, halved by each Quicken. */
	public static int pulseInterval(SpellPlan.Link link) {
		return Math.max(5, (int) Math.round(20 / Math.pow(2.0, link.count(Runes.QUICKEN))));
	}

	/** Widen grows an effect at most this many times over (five Widens), so no spell scans half a world of blocks. */
	public static final double MAX_WIDEN = 8.0;

	public static double effectRadius(SpellPlan.EffectNode e) {
		return Math.min(MAX_WIDEN, Math.pow(1.5, e.count(Runes.WIDEN))) * Math.pow(0.5, e.count(Runes.FOCUS_MOD)) * RuneNumbers.radius(e.mods);
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
		return quickened(g, 2.0);
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
		return 1.6 * quickened(g, 2.0);
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
