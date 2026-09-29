package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A player's own affinity with each of the ten elements, the pure part. Points grow from what a player
 * does: casting the element (by the mana its effects cost, so cheap spam is worth no more than real
 * spells), setting off reactions it takes part in, finding creatures weak to it, and everyday things that
 * fit it (smelting for fire, fishing for frost, mining for earth...). Five levels at rising thresholds,
 * and each gives a little: more power with the element, from III a resistance to it when others' spells
 * land, and at V cheaper spells of it.
 *
 * <p>Every way of earning points has a daily allowance (an in-game day of ticks, which sleeping doesn't
 * skip), past which it earns nothing, or for casting a tenth. Shared by the server
 * ({@code cast.PlayerAffinities}, which grants points), the Cord screen and the unit tests.</p>
 */
public final class PlayerAffinity {
	private PlayerAffinity() {}

	public static final int MAX_LEVEL = 5;
	/** The points (running totals) each level needs, from I to V. */
	private static final int[] THRESHOLDS = {100, 600, 1800, 4500, 10000};
	/** More power with the element for each level: +15% at V. */
	public static final double POWER_PER_LEVEL = 0.03;
	/** The level from which others' spells of the element land softer on you. */
	public static final int RESIST_FROM = 3;
	/** How much of the element's damage from others' spells you shrug off, by level (none below III). */
	private static final double[] RESIST = {0, 0, 0, 0.10, 0.15, 0.20};
	/** At V: the element's share of a spell's price costs this much less. */
	public static final double DISCOUNT = 0.10;
	/** How long an allowance lasts: an in-game day of ticks. */
	public static final long DAY_TICKS = 24000;
	/** Ticks between the checks of where a player is and what they're doing (standing in snow, on a ley line...). */
	public static final int SAMPLE_TICKS = 100;
	/** The Grimoire key for having reached level I with an element: {@code affinity:fire}. */
	public static final String KEY_PREFIX = "affinity:";
	/** Mana condensed toward the next Heart Circle by each first level. */
	public static final int REWARD = 100;

	/**
	 * A way of earning points.
	 *
	 * @param element its element, or "" for one that feeds whichever element it's about (casting, reactions, the Bestiary)
	 * @param points  points for each unit: an event, a check (every {@link #SAMPLE_TICKS}), a point of mana, an item...
	 * @param daily   points a day it earns at full rate
	 * @param tail    what it earns past that, as a fraction (0: nothing)
	 */
	public enum Source {
		// Every element.
		CAST("cast", "", 0.1, 100, 0.1),
		REACTION("reaction", "", 3, 45, 0),
		BESTIARY("bestiary", "", 25, 100, 0),
		// Fire.
		SMELT("smelt", "fire", 0.25, 30, 0),
		FIRE_KILL("fire_kill", "fire", 2, 30, 0),
		BURNING("burning", "fire", 1, 15, 0),
		LAVA("lava", "fire", 0.5, 15, 0),
		// Frost (water belongs to frost).
		FISH("fish", "frost", 3, 30, 0),
		COLD("cold", "frost", 0.5, 15, 0),
		FROZEN("frozen", "frost", 1, 15, 0),
		// Storm.
		THUNDER("thunder", "storm", 1, 30, 0),
		STRUCK("struck", "storm", 40, 80, 0),
		ROD("rod", "storm", 8, 24, 0),
		// Wind.
		GLIDE("glide", "wind", 1, 30, 0),
		FALL("fall", "wind", 4, 20, 0),
		HEIGHTS("heights", "wind", 0.5, 15, 0),
		// Earth.
		STONE("stone", "earth", 0.1, 15, 0),
		ORE("ore", "earth", 1, 30, 0),
		DEEP("deep", "earth", 0.5, 15, 0),
		// Life.
		HARVEST("harvest", "life", 0.25, 25, 0),
		BREED("breed", "life", 3, 30, 0),
		HEAL("heal", "life", 0.25, 30, 0),
		TAME("tame", "life", 10, 20, 0),
		// Void.
		END("end", "void", 0.5, 20, 0),
		VOID_KILL("void_kill", "void", 2, 30, 0),
		PEARL("pearl", "void", 1, 15, 0),
		DEEP_DARK("deep_dark", "void", 1, 20, 0),
		// Arcane.
		ENCHANT("enchant", "arcane", 5, 30, 0),
		MEDITATE("meditate", "arcane", 0.5, 20, 0),
		PAGE("page", "arcane", 10, 30, 0),
		RUNE("rune", "arcane", 15, 60, 0),
		LEY("ley", "arcane", 0.5, 20, 0),
		// Time.
		NIGHT_WATCH("night_watch", "time", 20, 20, 0),
		AGE("age", "time", 1, 20, 0),
		CLOCK("clock", "time", 0.25, 10, 0),
		// Blood.
		MELEE_KILL("melee_kill", "blood", 2, 30, 0),
		BLOOD_PRICE("blood_price", "blood", 1, 40, 0),
		HEAVY_HIT("heavy_hit", "blood", 3, 21, 0);

		public final String id;
		public final String element;
		public final double points;
		public final double daily;
		public final double tail;

		Source(String id, String element, double points, double daily, double tail) {
			this.id = id;
			this.element = element;
			this.points = points;
			this.daily = daily;
			this.tail = tail;
		}

		/** Whether it feeds whichever element it's about, rather than one of its own. */
		public boolean general() {
			return element.isEmpty();
		}

		/** The key its daily allowance is kept under: one per element for a general source. */
		public String tallyKey(String forElement) {
			return general() ? id + ":" + forElement : id;
		}
	}

	/** The elements each reaction takes part in, and so feeds: Shatter is fire on frost, Conduct storm through water. */
	public static final Map<String, List<String>> REACTION_ELEMENTS = Map.ofEntries(
		Map.entry("shatter", List.of("fire", "frost")),
		Map.entry("conduct", List.of("storm", "frost")),
		Map.entry("wildfire", List.of("fire", "wind")),
		Map.entry("implode", List.of("void", "fire")),
		Map.entry("collapse", List.of("void", "wind")),
		Map.entry(ReactionRules.OVERLOAD, List.of("storm", "fire")),
		Map.entry(ReactionRules.FRACTURE, List.of("earth", "frost")),
		Map.entry(ReactionRules.BLIGHT, List.of("life", "void")),
		Map.entry(ReactionRules.UNWEAVE, List.of("arcane")),
		Map.entry(ReactionRules.RUPTURE, List.of("wind", "blood")),
		Map.entry(ReactionRules.ELAPSE, List.of("time")));

	public static boolean isElement(String element) {
		return Affinity.ELEMENTS.contains(element);
	}

	// ------------------------------------------------------------------ levels

	/** The level these points reach, 0 (none yet) to {@link #MAX_LEVEL}. */
	public static int level(int points) {
		int level = 0;
		while (level < MAX_LEVEL && points >= THRESHOLDS[level]) {
			level++;
		}
		return level;
	}

	/** The points level {@code level} needs (0 for level 0). */
	public static int threshold(int level) {
		return level <= 0 ? 0 : THRESHOLDS[Math.min(level, MAX_LEVEL) - 1];
	}

	/** The points the next level needs, or -1 once the last is reached. */
	public static int next(int points) {
		int level = level(points);
		return level >= MAX_LEVEL ? -1 : THRESHOLDS[level];
	}

	/** How far through the current level these points are, 0 to 1 (1 at the last level). */
	public static double progress(int points) {
		int level = level(points);
		if (level >= MAX_LEVEL) {
			return 1.0;
		}
		int from = threshold(level);
		return Math.max(0, Math.min(1, (points - from) / (double) (THRESHOLDS[level] - from)));
	}

	// ------------------------------------------------------------------ what a level gives

	/** The power multiplier on the element's effects at {@code level}. */
	public static double power(int level) {
		return 1.0 + POWER_PER_LEVEL * Math.max(0, Math.min(MAX_LEVEL, level));
	}

	/** How much of the element's damage from others' spells is shrugged off at {@code level}: 0 below III. */
	public static double resistance(int level) {
		return RESIST[Math.max(0, Math.min(MAX_LEVEL, level))];
	}

	/** What of the element's damage from others' spells still lands at {@code level}. */
	public static double damageTaken(int level) {
		return 1.0 - resistance(level);
	}

	/**
	 * The factor on a spell's price: {@link #DISCOUNT} off the share of it that comes from each element at
	 * level V ({@code shares} from {@link SpellCompiler#elementShares}). A spell of fire alone costs 10% less
	 * with Fire V; one half fire and half frost, 5% less.
	 */
	public static double costFactor(Map<String, Double> shares, Map<String, Integer> points) {
		double mastered = 0;
		for (Map.Entry<String, Double> share : shares.entrySet()) {
			if (level(points.getOrDefault(share.getKey(), 0)) >= MAX_LEVEL) {
				mastered += share.getValue();
			}
		}
		return 1.0 - DISCOUNT * Math.max(0, Math.min(1, mastered));
	}

	/** Whether any element has reached level V (the cost can't change otherwise, so nothing else need be read). */
	public static boolean anyMastered(Map<String, Integer> points) {
		for (int value : points.values()) {
			if (level(value) >= MAX_LEVEL) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ allowances

	/**
	 * The points {@code offered} more earns from {@code source} when {@code earned} were already offered by
	 * it today: in full up to its daily allowance, and its tail rate past it.
	 */
	public static double grant(Source source, double earned, double offered) {
		if (offered <= 0) {
			return 0;
		}
		double full = Math.max(0, Math.min(offered, source.daily - Math.max(0, earned)));
		return full + (offered - full) * source.tail;
	}

	/** The in-game day (of {@link #DAY_TICKS}) a game time falls in, which allowances are kept for. */
	public static long day(long gameTime) {
		return Math.floorDiv(gameTime, DAY_TICKS);
	}

	/** The ways of earning points with {@code element}: the general ones first, then its own. */
	public static List<Source> sources(String element) {
		List<Source> out = new ArrayList<>();
		for (Source source : Source.values()) {
			if (source.general() || source.element.equals(element)) {
				out.add(source);
			}
		}
		return out;
	}

	/**
	 * Whether a night was watched: out under the night sky for at least four checks in five of it. A night
	 * is 10,000 ticks, so {@code nightChecks} checks of {@link #SAMPLE_TICKS} each.
	 */
	public static boolean nightWatched(int outdoorChecks, int nightChecks) {
		return nightChecks > 0 && outdoorChecks * 5 >= nightChecks * 4;
	}

	/** The checks one whole night holds. */
	public static final int NIGHT_CHECKS = (int) (10000 / SAMPLE_TICKS);

	// ------------------------------------------------------------------ from before affinities

	/** Points given for each cast of an element counted before affinities existed. */
	public static final int SEED_PER_CAST = 2;

	/**
	 * A caster's points from the casts per element counted before affinities existed (for leaning): a
	 * start, never past level II, so what's earned since still matters.
	 */
	public static int seed(int casts) {
		return (int) Math.min((long) Math.max(0, casts) * SEED_PER_CAST, threshold(2));
	}
}
