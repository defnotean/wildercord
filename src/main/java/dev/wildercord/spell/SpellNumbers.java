package dev.wildercord.spell;

/**
 * Every tunable number a modifier changes, in one place, so the readout the player sees
 * and the spell the server runs can never disagree.
 */
public final class SpellNumbers {
	private SpellNumbers() {}

	public static final int MAX_COPIES = 9;
	/** Touch: what you lay hands on takes the spell this much harder. */
	public static final double TOUCH_POWER = 1.3;
	/** Cone: the near half of the cone strikes this much harder, the far half this much softer. */
	public static final double CONE_NEAR = 1.35;
	public static final double CONE_FAR = 0.85;
	/** Rain: the share of strikes that fall on an enemy in the area, and how wide each strikes. */
	public static final double RAIN_SEEK = 0.6;
	public static final double RAIN_STRIKE = 2.0;
	/** Orb: how far it drifts, and how sharply your aim turns it each tick (0 = not at all, 1 = at once). */
	public static final double ORB_RANGE = 30.0;
	public static final double ORB_STEER = 0.08;
	/** Ring: the share of its radius around the caster that it leaves alone. */
	public static final double RING_HOLLOW = 0.22;
	/** Linger's landings after the first are echoes: this share of the power. */
	public static final double LINGER_POWER = 0.6;
	/** The k-th creature an On Hit payload fires at in one landing gets this much of the last one's power. */
	public static final double TRIGGER_FALLOFF = 0.85;
	public static final int MAX_ECHOES = 3;

	public static double power(SpellPlan.EffectNode e) {
		return Math.pow(1.5, e.count(Runes.AMPLIFY)) * Math.pow(0.6, e.count(Runes.FRUGAL_MOD))
			* Math.pow(2.5, e.count(Runes.OVERCHARGE_MOD)) * Math.pow(1.5, ModifierLimits.count(e.mods, Runes.FOCUS_MOD)) * RuneNumbers.power(e.mods) * Math.pow(1.3, e.count(Runes.KINDLED))
			* belatedPower(e);
	}

	/** Focus and Vow on a shape, times the shape's own strength per hit (Barrage hits often, so softer). */
	public static double groupPower(SpellPlan.Group g) {
		return Math.pow(1.5, ModifierLimits.count(g.shapeMods, Runes.FOCUS_MOD)) * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.VOW_MOD)) * shapeStrength(g.shape) * CircleDisciplines.profile(g).power();
	}

	/** Power per hit for shapes that hit many times (Barrage, Stream), and for the cheap Spark. */
	public static double shapeStrength(RuneDef shape) {
		if (shape.is(Runes.BARRAGE.id()) || shape.is(Runes.STREAM.id())) {
			return 0.35;
		}
		if (shape.is(Runes.SPARK.id())) {
			return 0.75;
		}
		if (shape.is(Runes.LATCH.id())) {
			return LATCH_STRENGTH;
		}
		return 1.0;
	}

	/** Execute on an effect: power multiplier against targets under half health. */
	public static double executeBonus(SpellPlan.EffectNode e) {
		return Math.pow(2.0, ModifierLimits.count(e.mods, Runes.EXECUTE_MOD));
	}

	// ---- runes of the world

	/** Trial Key on an effect: power multiplier against targets at full health (1 = none). */
	public static double trialKeyBonus(SpellPlan.EffectNode e) {
		return Math.pow(1.6, ModifierLimits.count(e.mods, Runes.TRIAL_KEY));
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
		return (int) Math.round(3 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
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
		return Math.min(MAX_QUICKEN, Math.pow(each, g.count(Runes.QUICKEN)) * CircleDisciplines.profile(g).speed());
	}

	/** Domain radius: 9 blocks, widened up to 24 at most (beyond that it can't be seen or kept up). */
	public static final double MAX_DOMAIN_RADIUS = 24.0;

	public static double domainRadius(SpellPlan.Group g) {
		return Math.min(MAX_DOMAIN_RADIUS, 9.0 * shapeRadius(g));
	}

	public static int domainSeconds(SpellPlan.Group g) {
		return (int) Math.round(6 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
	}

	/** Ticks between a Domain's strikes: 20, halved by each Quicken. */
	/** A Domain's life in ticks (Quicken shortens it with its interval: the same strikes, sooner). */
	public static int domainTicks(SpellPlan.Group g) {
		return domainSeconds(g) * domainInterval(g);
	}

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

	/** A Barrage always lands 8 blows; Quicken makes them come faster (see {@link #barrageTicks}), not more. */
	public static int barrageBlows(SpellPlan.Group g) {
		return 8;
	}

	/** Ticks a Barrage's blows are spread over: 20, halved by each Quicken (never under 6). */
	public static int barrageTicks(SpellPlan.Group g) {
		return Math.max(6, (int) Math.round(20 / Math.pow(2.0, g.count(Runes.QUICKEN))));
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

	/** Strikes in a Stream: always 6; Quicken packs them into less time ({@link #streamTicks}). */
	public static int streamStrikes(SpellPlan.Group g) {
		return 6;
	}

	/** Ticks a Stream lasts: 20, halved by each Quicken (never under 8). */
	public static int streamTicks(SpellPlan.Group g) {
		return Math.max(8, (int) Math.round(STREAM_TICKS / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	/** Radius factor on a shape from Widen and Focus (and add-on radius modifiers). */
	public static double shapeRadius(SpellPlan.Group g) {
		return Math.pow(1.5, g.count(Runes.WIDEN)) * Math.pow(0.5, ModifierLimits.count(g.shapeMods, Runes.FOCUS_MOD)) * RuneNumbers.radius(g.shapeMods) * CircleDisciplines.profile(g).radius();
	}

	public static double duration(SpellPlan.EffectNode e) {
		return Math.pow(2.0, ModifierLimits.count(e.mods, Runes.EXTEND)) * Math.pow(0.6, e.count(Runes.FRUGAL_MOD)) * RuneNumbers.duration(e.mods);
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
		return (int) Math.round(5 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
	}

	/** Ticks between a Wall's hits: 20 (once a second, as a Wall has always struck), halved by each Quicken. */
	/** A Wall's life in ticks: its seconds, shortened in step with its interval by Quicken (the same strikes, sooner). */
	public static int wallTicks(SpellPlan.Group g) {
		return wallSeconds(g) * wallInterval(g);
	}

	public static int wallInterval(SpellPlan.Group g) {
		return Math.max(4, (int) Math.round(20 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	public static int orbs(SpellPlan.Group g) {
		return Math.min(MAX_COPIES, 3 * copies(g));
	}

	public static int orbitSeconds(SpellPlan.Group g) {
		return (int) Math.round(8 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
	}

	public static int trailSeconds(SpellPlan.Group g) {
		return (int) Math.round(5 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
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
		return Math.min(MAX_WIDEN, Math.pow(1.5, e.count(Runes.WIDEN))) * Math.pow(0.5, ModifierLimits.count(e.mods, Runes.FOCUS_MOD)) * RuneNumbers.radius(e.mods);
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
		return (int) Math.round(6 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
	}

	/** Ticks between a Zone's pulses: 20, halved by each Quicken. */
	/** A Zone's pulses: one a second of its life whatever Quicken does (Quicken only makes them come faster). */
	public static int zonePulses(SpellPlan.Group g) {
		return Math.max(1, zoneSeconds(g));
	}

	/** How long a Zone lasts, in seconds: its pulses at their interval. */
	public static double zoneLife(SpellPlan.Group g) {
		return zonePulses(g) * zoneInterval(g) / 20.0;
	}

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
		return (int) Math.round(10 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
	}

	/** Ticks between a Totem's pulses: 40, halved by each Quicken. */
	/** A Totem's life in ticks (Quicken shortens it with its interval: the same pulses, sooner). */
	public static int totemTicks(SpellPlan.Group g) {
		return totemSeconds(g) * totemInterval(g) / 2;
	}

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
		return Math.max(2, (int) Math.round(20 * Math.pow(2.0, ModifierLimits.count(link.mods, Runes.EXTEND)) / Math.pow(2.0, link.count(Runes.QUICKEN))));
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

	/** Cooldown after Rapid and Vow: each Vow makes it 5x longer, up to a minute. */
	public static int cooldownTicks(double cost, int rapid, int vows) {
		double ticks = cooldownTicks(cost) * Math.pow(0.5, rapid) * Math.pow(5.0, vows);
		return (int) Math.max(5, Math.min(1200, Math.round(ticks)));
	}

	/** Blood Price: health paid instead of mana, 1 per 4 mana, at least 1. */
	public static int healthCost(double cost) {
		return (int) Math.max(1, Math.ceil(cost / 4.0 - 1e-9));
	}

	// ---- new runes (batch 2): Glaive, Imprint and Latch; Kindred, Thirst and Belated

	/** How far a Glaive flies out before it turns back (sooner at a wall). */
	public static final double GLAIVE_RANGE = 12.0;
	/** The longest a Glaive's way back may take, in ticks, however far its caster ran meanwhile. */
	public static final int GLAIVE_RETURN_TICKS = 60;

	/** How close to a Glaive's spinning blade a creature must be to be struck. */
	public static double glaiveWidth(SpellPlan.Group g) {
		return 1.0 * shapeRadius(g);
	}

	/** Glaive speed in blocks per tick, out and back. */
	public static double glaiveSpeed(SpellPlan.Group g) {
		return 1.0 * quickened(g, 1.5);
	}

	/** How far around an Imprint it erupts. */
	public static double imprintRadius(SpellPlan.Group g) {
		return 3.0 * shapeRadius(g);
	}

	/** Ticks before an Imprint erupts: 40, halved by each Quicken. */
	public static int imprintDelay(SpellPlan.Group g) {
		return Math.max(10, (int) Math.round(40 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	/** How far from the aim a Latch finds its creature, and how far it may then stray before the thread snaps. */
	public static final double LATCH_RANGE = 16.0;
	public static final double LATCH_HOLD = 24.0;
	/** A Latch's power per strike: it strikes often, and never misses. */
	public static final double LATCH_STRENGTH = 0.7;

	/** Ticks between a Latch's strikes: 20, halved by each Quicken. */
	public static int latchInterval(SpellPlan.Group g) {
		return Math.max(5, (int) Math.round(20 / Math.pow(2.0, g.count(Runes.QUICKEN))));
	}

	/** A Latch's strikes: 4, doubled by each Extend (longer), 16 at most. Quicken makes them come faster, not more. */
	public static int latchStrikes(SpellPlan.Group g) {
		return (int) Math.min(16, 4 * Math.pow(2.0, ModifierLimits.count(g.shapeMods, Runes.EXTEND)));
	}

	/** Kindred: the share of the effect's power that you and the ally it missed get, and how far it looks for that ally. */
	public static final double KINDRED_SHARE = 0.5;
	public static final double KINDRED_REACH = 8.0;

	/** Thirst: the share of the damage an effect deals that heals its caster (a quarter each, three quarters at most). */
	public static double thirstShare(SpellPlan.EffectNode e) {
		return Math.min(0.75, 0.25 * e.count(Runes.THIRST));
	}

	/** Belated: each one counted (three at most) makes the effect this much stronger and this many ticks later. */
	public static final double BELATED_POWER = 1.25;
	public static final int BELATED_TICKS = 30;
	private static final int MAX_BELATED = 3;

	public static double belatedPower(SpellPlan.EffectNode e) {
		return Math.pow(BELATED_POWER, Math.min(MAX_BELATED, e.count(Runes.BELATED)));
	}

	/** Ticks a Belated effect waits before it lands (0 = at once). */
	public static int belatedTicks(SpellPlan.EffectNode e) {
		return BELATED_TICKS * Math.min(MAX_BELATED, e.count(Runes.BELATED));
	}
}
