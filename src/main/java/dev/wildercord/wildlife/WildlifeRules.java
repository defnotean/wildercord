package dev.wildercord.wildlife;

import java.util.List;
import java.util.Map;

/**
 * Magical wildlife's numbers and decisions, kept pure (no Minecraft types) so the creatures, their spawns and the
 * unit tests all read the same rules: where each one lives and how often it spawns, how bright a lumen stag's antlers
 * burn under each moon, when a stag trusts someone enough to shed, which garden a mossback tortoise grows, and the
 * rest. See DESIGN.md, "Magical wildlife".
 */
public final class WildlifeRules {
	private WildlifeRules() {}

	/** Which spawning pool a creature draws from: ambient things come and go, creatures stay where they spawned. */
	public enum Pool { AMBIENT, CREATURE }

	/**
	 * One kind of wildlife and how it spawns.
	 *
	 * @param id        its entity id's path ({@code lumen_stag}), also its config switch
	 * @param pool      the spawning pool it's in
	 * @param weight    its spawn weight in each of its biomes (vanilla's pools weigh 1 to 12 an entry)
	 * @param minGroup  the fewest that spawn together
	 * @param maxGroup  the most that spawn together
	 * @param chance    the share of otherwise good spawn attempts let through at multiplier 1 (how rare it is on top of its weight)
	 * @param crowd     how many of its kind may already be near a spawn for another to join them
	 * @param crowdRange how near "near" is, in blocks
	 * @param biomes    the biomes it spawns in, as ids
	 */
	public record Kind(String id, Pool pool, int weight, int minGroup, int maxGroup, double chance, int crowd, int crowdRange, List<String> biomes) {}

	public static final Kind GLIMMERWING = new Kind("glimmerwing", Pool.AMBIENT, 8, 3, 5, 1 / 4.0, 9, 40, List.of(
		"minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest",
		"minecraft:meadow", "minecraft:sunflower_plains", "minecraft:cherry_grove", "minecraft:swamp", "minecraft:mangrove_swamp"));
	public static final Kind LUMEN_STAG = new Kind("lumen_stag", Pool.CREATURE, 2, 1, 1, 1 / 3.0, 0, 80, List.of(
		"minecraft:old_growth_birch_forest", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:taiga",
		"minecraft:cherry_grove"));
	public static final Kind MOSSBACK_TORTOISE = new Kind("mossback_tortoise", Pool.CREATURE, 5, 1, 2, 1.0, 4, 48, List.of(
		"minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"));
	public static final Kind CINDERFOX = new Kind("cinderfox", Pool.CREATURE, 6, 1, 3, 1.0, 5, 48, List.of(
		"minecraft:desert", "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"));
	public static final Kind SKYRAY = new Kind("skyray", Pool.AMBIENT, 3, 1, 1, 1 / 800.0, 1, 112, List.of(
		"minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:meadow",
		"minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:snowy_slopes"));
	public static final Kind RIMEHARE = new Kind("rimehare", Pool.CREATURE, 6, 2, 3, 1.0, 6, 48, List.of(
		"minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:snowy_slopes", "minecraft:grove"));

	/** The black bobcat: rare and solitary, in dark forests and old taigas (its own module, so not in {@link #ALL}). */
	public static final Kind BLACK_BOBCAT = new Kind("black_bobcat", Pool.CREATURE, 3, 1, 1, 1 / 2.0, 1, 64, List.of(
		"minecraft:dark_forest", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:taiga",
		"minecraft:snowy_taiga"));
	/** The Frost Lynx (0.13): a tameable predator of the snowfields (its own module, so not in {@link #ALL}). */
	public static final Kind FROST_LYNX = new Kind("frost_lynx", Pool.CREATURE, 3, 1, 1, 1 / 2.0, 1, 64, List.of(
		"minecraft:snowy_taiga", "minecraft:snowy_plains", "minecraft:snowy_slopes", "minecraft:grove"));
	/** The Dune Cougar (0.13): a tameable predator of the savannas and badlands (its own module, so not in {@link #ALL}). */
	public static final Kind DUNE_COUGAR = new Kind("dune_cougar", Pool.CREATURE, 3, 1, 1, 1 / 2.0, 1, 64, List.of(
		"minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:badlands", "minecraft:wooded_badlands"));
	/** The Reefback Turtle (0.13): a water mount come ashore on beaches (a mount, so not in {@link #ALL}). */
	public static final Kind REEFBACK_TURTLE = new Kind("reefback_turtle", Pool.CREATURE, ReefbackRules.WEIGHT, ReefbackRules.MIN_GROUP,
		ReefbackRules.MAX_GROUP, 1 / 2.0, 2, 64, List.of("minecraft:beach", "minecraft:mangrove_swamp"));
	/** The Delver Mole (0.13): a burrowing mount of the plains, meadows and forests (a mount, so not in {@link #ALL}). */
	public static final Kind DELVER_MOLE = new Kind("delver_mole", Pool.CREATURE, DelverRules.WEIGHT, DelverRules.MIN_GROUP,
		DelverRules.MAX_GROUP, 1 / 2.0, 2, 64, List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
		"minecraft:forest", "minecraft:birch_forest", "minecraft:flower_forest"));

	/** Every kind, in the order the guide and the config list them. */
	public static final List<Kind> ALL = List.of(GLIMMERWING, LUMEN_STAG, MOSSBACK_TORTOISE, CINDERFOX, SKYRAY, RIMEHARE);

	public static Kind kind(String id) {
		for (Kind kind : ALL) {
			if (kind.id().equals(id)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("not wildlife: " + id);
	}

	// ------------------------------------------------------------------ spawning

	/**
	 * A kind's spawn weight under the server's multiplier: never below its own (fewer spawns come from letting fewer
	 * through, see {@link #chance}), grown above 1, and at least 1 so the entry stays valid.
	 */
	public static int weight(Kind kind, double multiplier) {
		return Math.max(1, (int) Math.round(kind.weight() * Math.max(1, multiplier)));
	}

	/** The share of good spawn attempts let through: the kind's own rarity, times the multiplier when it's below 1. */
	public static double chance(Kind kind, double multiplier) {
		if (multiplier <= 0) {
			return 0;
		}
		return Math.min(1, kind.chance() * Math.min(1, multiplier));
	}

	/** Whether another may spawn with {@code near} of its kind already within {@link Kind#crowdRange} of the spot. */
	public static boolean roomFor(Kind kind, int near) {
		return near <= kind.crowd();
	}

	// ------------------------------------------------------------------ time

	/** The day a clock time falls on (a stag sheds once a day). */
	public static long day(long clock) {
		return Math.floorDiv(clock, 24000L);
	}

	/**
	 * How dark it is outside, 0 at noon to 1 at midnight, from the sky's darkening (0 to 11, as the game keeps it).
	 * Dusk and dawn pass through the middle.
	 */
	public static float night(int skyDarken) {
		return clamp((skyDarken - 2) / 8F, 0, 1);
	}

	// ------------------------------------------------------------------ the lumen stag

	/** Whether a stag that last shed on {@code lastShed} (-1: never) may shed today. */
	public static boolean mayShed(long lastShed, long today) {
		return lastShed != today;
	}

	/**
	 * How brightly a stag's antlers burn (0 to 1): with the moon at night, faint by day. A full moon (phase 0) brings
	 * them to full; a new moon (phase 4) leaves them at a third, the phases between in proportion.
	 */
	public static float antlerGlow(int moonPhase, float night) {
		int phase = Math.floorMod(moonPhase, 8);
		// 0 at a full moon, 1 at a new moon.
		float wane = Math.min(phase, 8 - phase) / 4F;
		float moon = 1 - 0.65F * wane;
		return clamp(0.22F + (moon - 0.22F) * clamp(night, 0, 1), 0, 1);
	}

	/** What a stag does about a player near it. */
	public enum Stance {
		/** Nothing: too far to care. */
		IGNORE,
		/** Turns and bolts. */
		FLEE,
		/** Stands still, head up, and watches. */
		WATCH,
		/** Lets the player close: trust builds while they stay. */
		TRUST
	}

	/** Beyond this a stag pays a player no mind. */
	public static final double STAG_NOTICE = 16;
	/** A calm player this near has the stag's trust. */
	public static final double STAG_TRUST = 3.5;
	/** How long (ticks) a calm player must stay that near before the stag sheds for them. */
	public static final int STAG_TRUST_TICKS = 60;
	/** Faster than this (blocks a tick) isn't calm, sneaking or not. */
	public static final double CALM_SPEED = 0.12;

	/**
	 * A stag's stance toward a player: anyone who isn't a calm, sneaking presence sends it running; a calm one is
	 * watched, then trusted up close. Once frightened (struck, or a stag killed near it) it flees from everyone.
	 *
	 * @param distance   blocks to the player
	 * @param sneaking   whether they're sneaking
	 * @param speed      how fast they move, blocks a tick
	 * @param frightened whether the stag is frightened
	 */
	public static Stance stagStance(double distance, boolean sneaking, double speed, boolean frightened) {
		if (distance > STAG_NOTICE) {
			return Stance.IGNORE;
		}
		if (frightened || !sneaking || speed > CALM_SPEED) {
			return Stance.FLEE;
		}
		return distance <= STAG_TRUST ? Stance.TRUST : Stance.WATCH;
	}

	/** How long Bad Luck follows whoever kills a lumen stag (ticks: five minutes). */
	public static final int STAG_CURSE_TICKS = 6000;

	// ------------------------------------------------------------------ the mossback tortoise

	/** The little garden on a mossback tortoise's shell, which grows from where it lives. */
	public enum Garden {
		/** Blue orchids and a brown mushroom in thick moss. */
		SWAMP,
		/** Mangrove roots' moss, a lily pad and a propagule. */
		MANGROVE,
		/** Ferns and a jungle flower. */
		JUNGLE;

		public static Garden byId(int id) {
			Garden[] all = values();
			return all[Math.floorMod(id, all.length)];
		}
	}

	private static final Map<String, Garden> GARDENS = Map.of(
		"minecraft:swamp", Garden.SWAMP,
		"minecraft:mangrove_swamp", Garden.MANGROVE,
		"minecraft:jungle", Garden.JUNGLE,
		"minecraft:sparse_jungle", Garden.JUNGLE,
		"minecraft:bamboo_jungle", Garden.JUNGLE);

	/** The garden a biome grows, or null for a biome that grows none (the tortoise keeps the one it has). */
	public static Garden garden(String biome) {
		return GARDENS.get(biome);
	}

	/** Checks (a minute apart) a tortoise must spend in another garden's biome before its garden grows over. */
	public static final int GARDEN_REGROW_CHECKS = 10;

	/** How much of a hit a tortoise hiding in its shell takes. */
	public static final float SHELL_GUARD = 0.4F;
	/** How long it stays in its shell after a hit (ticks). */
	public static final int SHELL_TICKS = 100;

	/** Ticks until an adult tortoise next lets a scute go: ten to twenty minutes. */
	public static int scuteInterval(double random) {
		return 12000 + (int) (clamp((float) random, 0, 1) * 12000);
	}

	// ------------------------------------------------------------------ the cinderfox

	/** A cinderfox's bite: its own damage, half again on a creature weak to fire (it bites with embers). */
	public static float sparkBite(float base, boolean weakToFire) {
		return weakToFire ? base * 1.5F : base;
	}

	/** Seconds a spark-bite sets a fire-weak creature burning. */
	public static final int SPARK_BURN_SECONDS = 4;
	/** The chance a rabbit tames a wild cinderfox. */
	public static final double TAME_CHANCE = 1 / 3.0;
	/** The chance a fish tames a wild black bobcat. */
	public static final double BOBCAT_TAME_CHANCE = 1 / 3.0;

	// ------------------------------------------------------------------ the skyray

	/** How high above the ground a skyray cruises, at least and at most. */
	public static final int SKYRAY_LOW = 26;
	public static final int SKYRAY_HIGH = 44;
	/** A skyray's loop round its anchor, at least and at most (blocks). */
	public static final double SKYRAY_MIN_RADIUS = 14;
	public static final double SKYRAY_MAX_RADIUS = 26;
	/** Never higher than this, whatever the ground (blocks above sea level's 63, below the clouds). */
	public static final int SKYRAY_CEILING = 190;

	/**
	 * The height a skyray glides at over ground {@code ground} high: {@code altitude} (0 to 1) of the way between its
	 * low and high cruise, under the ceiling.
	 */
	public static double cruise(int ground, double altitude) {
		double height = ground + SKYRAY_LOW + (SKYRAY_HIGH - SKYRAY_LOW) * clamp((float) altitude, 0, 1);
		return Math.min(height, SKYRAY_CEILING);
	}

	/**
	 * How far a skyray leans into a turn, in radians, from how much its heading changed in a tick (degrees). A gentle
	 * curve leans a little; it never rolls past 40 degrees.
	 */
	public static float bank(float turnDegrees) {
		return clamp(turnDegrees * 0.12F, -0.7F, 0.7F);
	}

	/** Ticks until a skyray next sheds a membrane: seven and a half to fifteen minutes. */
	public static int membraneInterval(double random) {
		return 9000 + (int) (clamp((float) random, 0, 1) * 9000);
	}

	// ------------------------------------------------------------------ the glimmerwing

	/** How bright a glimmerwing glows: fully at night, softly by day. */
	public static float glimmerGlow(float night) {
		return 0.3F + 0.7F * clamp(night, 0, 1);
	}

	/** How long after a spell a caster still draws glimmerwings (ticks). */
	public static final int CAST_LURE_TICKS = 160;
	/** How far a fresh spell draws them from (blocks). */
	public static final double CAST_LURE_RANGE = 14;
	/** Block light at least this bright draws them. */
	public static final int LIGHT_LURE = 10;

	// ------------------------------------------------------------------ the rimehare

	/** Beyond this a rimehare doesn't bolt (blocks). */
	public static final float HARE_NOTICE = 10;

	/** Whether a rimehare bolts from a player: anyone near who isn't holding out berries, and anyone at all who runs. */
	public static boolean hareBolts(double distance, boolean offeringBerries, boolean sprinting) {
		if (distance > HARE_NOTICE) {
			return false;
		}
		return sprinting || !offeringBerries;
	}

	static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}

	/** Moves {@code value} toward {@code target} by at most {@code step}: how the creatures ease into and out of a pose. */
	public static float approach(float value, float target, float step) {
		return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
	}
}
