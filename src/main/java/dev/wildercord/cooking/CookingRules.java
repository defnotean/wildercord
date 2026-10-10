package dev.wildercord.cooking;

import java.util.List;
import java.util.Map;

/**
 * Camp cooking (0.12 "Tempering"): a Camp Pot over heat turns a bowl and what's in the pack into a meal. Meals feed well and
 * carry buffs that keep a fighter going (mana, focus, stamina, footing, breath), never raw damage. Pure numbers and ids, so the
 * server, the cookbook and the tests agree.
 */
public final class CookingRules {
	private CookingRules() {}

	/** Max mana each level of Nourished adds. */
	public static final int NOURISHED_MANA = 20;
	/** Share off every spell's price each level of Focused takes. */
	public static final double FOCUSED_DISCOUNT = 0.08;
	/** Focused counts for at most this many levels. */
	public static final int FOCUSED_MAX = 2;
	/** Meals stack this high. */
	public static final int STACK = 16;

	public record Need(String item, int count) {}

	/** An effect by id, for {@code seconds}, at {@code level} (1 = I). */
	public record Buff(String effect, int seconds, int level) {}

	public record Recipe(String id, List<Need> needs, int nutrition, float saturation, List<Buff> buffs) {
		public int size() {
			int n = 0;
			for (Need need : needs) n += need.count;
			return n;
		}

		public boolean affordable(Map<String, Integer> have) {
			for (Need need : needs) {
				if (have.getOrDefault(need.item, 0) < need.count) return false;
			}
			return true;
		}
	}

	private static Need n(String item) { return new Need("minecraft:" + item, 1); }
	private static Need n(String item, int count) { return new Need("minecraft:" + item, count); }
	private static Buff b(String effect, int seconds, int level) {
		return new Buff(effect.contains(":") ? effect : "minecraft:" + effect, seconds, level);
	}

	private static final String CLARITY = "wildercord:clarity";
	private static final String NOURISHED = "wildercord:nourished";
	private static final String FOCUSED = "wildercord:focused";

	public static final List<Recipe> RECIPES = List.of(
		new Recipe("hearty_stew", List.of(n("beef"), n("potato"), n("carrot")), 10, 0.9F,
			List.of(b("regeneration", 30, 1), b(NOURISHED, 300, 1))),
		new Recipe("forager_soup", List.of(n("brown_mushroom"), n("red_mushroom"), n("carrot")), 7, 0.7F,
			List.of(b(CLARITY, 180, 1))),
		new Recipe("hunters_skewer", List.of(n("porkchop"), n("carrot")), 8, 0.8F,
			List.of(b("speed", 180, 1))),
		new Recipe("salmon_chowder", List.of(n("salmon"), n("potato"), n("kelp")), 9, 0.8F,
			List.of(b("water_breathing", 300, 1), b(NOURISHED, 180, 1))),
		new Recipe("pumpkin_porridge", List.of(n("pumpkin"), n("wheat"), n("sugar")), 8, 0.7F,
			List.of(b(NOURISHED, 480, 1))),
		new Recipe("sweetberry_tart", List.of(n("sweet_berries", 2), n("wheat"), n("sugar")), 7, 0.6F,
			List.of(b("speed", 120, 1), b(CLARITY, 120, 1))),
		new Recipe("glowberry_broth", List.of(n("glow_berries", 2), n("brown_mushroom")), 6, 0.6F,
			List.of(b("night_vision", 300, 1), b(FOCUSED, 180, 1))),
		new Recipe("miners_hash", List.of(n("potato"), n("porkchop"), n("brown_mushroom")), 9, 0.8F,
			List.of(b("haste", 240, 1))),
		new Recipe("emberroot_curry", List.of(n("chicken"), n("carrot"), n("blaze_powder")), 9, 0.8F,
			List.of(b("fire_resistance", 300, 1), b(FOCUSED, 120, 1))),
		new Recipe("frostfin_soup", List.of(n("cod"), n("beetroot"), n("kelp")), 7, 0.7F,
			List.of(b("absorption", 120, 1))),
		new Recipe("ruby_borscht", List.of(n("beetroot", 2), n("beef"), n("potato")), 10, 0.9F,
			List.of(b("regeneration", 45, 1), b(CLARITY, 240, 1))),
		new Recipe("honeyed_ham", List.of(n("porkchop"), n("honey_bottle")), 9, 0.9F,
			List.of(b("regeneration", 20, 1), b(NOURISHED, 240, 2))),
		new Recipe("scholars_tea", List.of(n("sweet_berries"), n("sugar"), n("amethyst_shard")), 4, 0.4F,
			List.of(b(FOCUSED, 180, 2))),
		new Recipe("wanderers_stew", List.of(n("rabbit"), n("carrot"), n("potato"), n("brown_mushroom")), 11, 1.0F,
			List.of(b("speed", 300, 1), b("jump_boost", 300, 1))),
		new Recipe("mutton_pottage", List.of(n("mutton"), n("beetroot"), n("wheat")), 9, 0.8F,
			List.of(b("resistance", 120, 1))),
		new Recipe("fishers_bouillabaisse", List.of(n("cod"), n("salmon"), n("tropical_fish")), 10, 0.9F,
			List.of(b("water_breathing", 480, 1), b("dolphins_grace", 60, 1))),
		new Recipe("mana_risotto", List.of(n("red_mushroom"), n("wheat"), n("lapis_lazuli")), 7, 0.7F,
			List.of(b(CLARITY, 120, 2))),
		new Recipe("starlit_consomme", List.of(n("chorus_fruit"), n("glow_berries"), n("beetroot")), 6, 0.6F,
			List.of(b("slow_falling", 180, 1), b(FOCUSED, 180, 1))),
		new Recipe("spiced_compote", List.of(n("apple"), n("sugar"), n("cocoa_beans")), 6, 0.6F,
			List.of(b("haste", 180, 1), b("speed", 180, 1))),
		new Recipe("duelists_broth", List.of(n("chicken"), n("egg"), n("wheat")), 8, 0.8F,
			List.of(b("resistance", 90, 1), b(NOURISHED, 180, 1))),
		new Recipe("archmages_feast", List.of(n("golden_carrot"), n("amethyst_shard"), n("glow_berries"), n("beef")), 14, 1.2F,
			List.of(b(CLARITY, 240, 2), b(FOCUSED, 240, 2), b(NOURISHED, 240, 2)))
	);

	public static Recipe recipe(String id) {
		for (Recipe recipe : RECIPES) {
			if (recipe.id.equals(id)) return recipe;
		}
		return null;
	}

	/**
	 * What the pot cooks from {@code have}: the chosen recipe if it can be afforded, otherwise the richest that can (the most
	 * ingredients, earlier in the cookbook on a tie), or null.
	 */
	public static Recipe pick(Map<String, Integer> have, String chosen) {
		Recipe wanted = chosen == null ? null : recipe(chosen);
		if (wanted != null && wanted.affordable(have)) return wanted;
		Recipe best = null;
		for (Recipe recipe : RECIPES) {
			if (recipe.affordable(have) && (best == null || recipe.size() > best.size())) best = recipe;
		}
		return best;
	}

	/** The next affordable recipe after {@code current} in the cookbook, wrapping; null if none can be afforded. */
	public static Recipe next(Map<String, Integer> have, String current) {
		int start = 0;
		for (int i = 0; i < RECIPES.size(); i++) {
			if (RECIPES.get(i).id.equals(current)) start = i + 1;
		}
		for (int k = 0; k < RECIPES.size(); k++) {
			Recipe recipe = RECIPES.get((start + k) % RECIPES.size());
			if (recipe.affordable(have)) return recipe;
		}
		return null;
	}

	/** What Focused at {@code level} (0 = none) leaves of a spell's price. */
	public static double focusedCost(int level) {
		return 1 - FOCUSED_DISCOUNT * Math.max(0, Math.min(FOCUSED_MAX, level));
	}
}
