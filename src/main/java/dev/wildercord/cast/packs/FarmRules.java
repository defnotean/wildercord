package dev.wildercord.cast.packs;

/**
 * The numbers and pure rules behind the farmstead runes (see {@link FarmEffects}): how far each one
 * reaches, how much it does and how often it may be repeated. Kept free of the game so it can be tested.
 */
public final class FarmRules {
	private FarmRules() {}

	/** A patch rune's half-width: 2, a 5-by-5 patch, before Widen. */
	public static final int PATCH = 2;
	/** The widest any patch rune reaches, however widened (an 13-by-13 patch). */
	public static final int MAX_PATCH = 6;
	/** Plowline's furrow, and the longest it gets with power. */
	public static final int PLOW_LENGTH = 8;
	public static final int PLOW_MAX = 16;
	/** Field Sense reads this far; Herdsense this far. */
	public static final int FIELDSENSE_REACH = 6;
	public static final int HERDSENSE_REACH = 12;
	/** Herdcall's reach, and how many animals answer at once. */
	public static final int HERDCALL_REACH = 8;
	public static final int HERDCALL_MAX = 12;
	/** The husbandry runes' reach (Courtship, Fleece, Milkmaid, Henhouse, Gentle Hand, Fodder, Barnwarmth, Hive Hum). */
	public static final int HERD_REACH = 5;
	/** How many animals a husbandry rune tends at once. */
	public static final int HERD_MAX = 8;
	/** How many animals Courtship brings together at once. */
	public static final int COURTSHIP_MAX = 6;
	/** Pollinate counts the bees this far; Calm Smoke settles them this far. */
	public static final int BEE_REACH = 8;
	public static final int CALM_REACH = 6;
	/** Scarecrow shoos this far, for this long. */
	public static final int SCARE_REACH = 5;
	public static final int SCARECROW_SECONDS = 20;
	/** Dewkeep's mist lasts this long and wets the field this often. */
	public static final int DEWKEEP_SECONDS = 60;
	public static final int DEWKEEP_EVERY_TICKS = 100;
	/** Cloche's glass lasts this long; it ticks this many plants a second at most. */
	public static final int CLOCHE_SECONDS = 30;
	public static final int CLOCHE_PLANTS = 25;
	/** The most blocks a lingering rune's later pulse changes (the cast's own budget is spent on its first). */
	public static final int PULSE_BLOCKS = 25;
	/** Ditchwater wants farmland this close. */
	public static final int DITCH_FARMLAND_REACH = 4;
	/** Sugar cane and cactus are never raised past this. */
	public static final int STALK_MAX = 3;
	/** Henhouse and Hive Hum: how long before the same hen or hive may be hurried again. */
	public static final long HEN_COOLDOWN_TICKS = 2 * 60 * 20;
	public static final long HIVE_COOLDOWN_TICKS = 60 * 20;
	/** Wildflower, Sapling Sow and Sapling Rise caps. */
	public static final int WILDFLOWER_MAX = 6;
	public static final int SAPLINGSOW_MAX = 4;
	public static final int SAPLINGSOW_SPACING = 2;
	public static final int SAPLINGRISE_MAX = 3;
	public static final int SAPLINGRISE_MEALS = 8;
	/** Stewpot's bowls. */
	public static final int STEW_MAX = 3;
	/** Coppice fells no more logs than this (the cast's block budget). */
	public static final int COPPICE_MAX = 32;
	/** Hunger the Feast and the Picnic give. */
	public static final int FEAST_HUNGER = 6;
	public static final int PICNIC_HUNGER = 3;

	/** A patch rune's half-width for a radius scale (Widen), between 1 and {@link #MAX_PATCH}. */
	public static int patch(double radiusScale) {
		return clamp((int) Math.round(PATCH * radiusScale), 1, MAX_PATCH);
	}

	/** A reach scaled by Widen, never under 1 nor over twice the base. */
	public static int reach(int base, double radiusScale) {
		return clamp((int) Math.round(base * radiusScale), 1, base * 2);
	}

	/** How far Plowline ploughs at a power. */
	public static int plowLength(double power) {
		return clamp((int) Math.round(PLOW_LENGTH * power), 1, PLOW_MAX);
	}

	/** How many stages Ripen gives: 1, 2 at 1.5 power, 3 at 2.5 (and never more). */
	public static int ripenSteps(double power) {
		return power >= 2.5 ? 3 : power >= 1.5 ? 2 : 1;
	}

	/** The age a plant reaches {@code steps} stages on, never past its max (an age past it is left as it is). */
	public static int nextAge(int age, int max, int steps) {
		if (age >= max) {
			return age;
		}
		return Math.min(max, age + Math.max(0, steps));
	}

	/** Compost and Hearthcook: 8 items, 16 at double power. */
	public static int batch(double power) {
		return power >= 2.0 ? 16 : 8;
	}

	/** Bakehouse: 4 bakes, 8 at double power. */
	public static int bakes(double power) {
		return power >= 2.0 ? 8 : 4;
	}

	/** Pollinate: the stages its bees give the field (one each, two each at double power), within the block budget. */
	public static int pollinateStages(int bees, double power) {
		return clamp(bees * (power >= 2.0 ? 2 : 1), 0, 32);
	}

	/** Whether a hen or a hive last hurried at {@code last} (null: never) may be hurried again. */
	public static boolean ready(Long last, long now, long cooldown) {
		return last == null || now - last >= cooldown;
	}

	/** Whether a sugar cane or cactus this tall may be raised one more block. */
	public static boolean mayRise(int height) {
		return height >= 1 && height < STALK_MAX;
	}

	/** How many mushroom stews a pot of red mushrooms, brown mushrooms and bowls makes, within {@code budget}. */
	public static int stews(int red, int brown, int bowls, int budget) {
		return Math.max(0, Math.min(Math.min(red, brown), Math.min(bowls, budget)));
	}

	/** How many beetroot soups six beetroots a bowl make, within {@code budget}. */
	public static int soups(int beetroots, int bowls, int budget) {
		return Math.max(0, Math.min(beetroots / 6, Math.min(bowls, budget)));
	}

	/** How many loaves three wheat a loaf make, within {@code budget}. */
	public static int loaves(int wheat, int budget) {
		return Math.max(0, Math.min(wheat / 3, budget));
	}

	/** How many pumpkin pies a pumpkin, sugar and an egg each make, within {@code budget}. */
	public static int pies(int pumpkins, int sugar, int eggs, int budget) {
		return Math.max(0, Math.min(Math.min(pumpkins, sugar), Math.min(eggs, budget)));
	}

	/** The stripped log's path for a log's ({@code oak_log} to {@code stripped_oak_log}); null when it is stripped already. */
	public static String strippedPath(String path) {
		if (path == null || path.isEmpty() || path.startsWith("stripped_")) {
			return null;
		}
		return "stripped_" + path;
	}

	/** Whether two saplings planted by Sapling Sow stand far enough apart (Chebyshev distance, as a tree's crown grows). */
	public static boolean spaced(int dx, int dz) {
		return Math.max(Math.abs(dx), Math.abs(dz)) >= SAPLINGSOW_SPACING;
	}

	/** Seconds of an effect as ticks, scaled by Extend's duration, never less than one. */
	public static int ticks(double seconds, double duration) {
		return Math.max(1, (int) Math.round(seconds * 20 * duration));
	}

	/** How many pulses a lingering rune of {@code seconds} gives, one each {@code every} ticks (the first at once). */
	public static int pulses(double seconds, double duration, int every) {
		return Math.max(1, ticks(seconds, duration) / Math.max(1, every));
	}

	/** Hayloft's upward toss at a power (about 4 blocks at 1, never past a dangerous height). */
	public static double tossSpeed(double power) {
		return Math.min(1.3, 0.9 * Math.sqrt(Math.max(0.1, power)));
	}

	/** Beeline's dash speed at a power (about 6 blocks at 1). */
	public static double zipSpeed(double power) {
		return Math.min(2.6, 1.7 * Math.sqrt(Math.max(0.1, power)));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
