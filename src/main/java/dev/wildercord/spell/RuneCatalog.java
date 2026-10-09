package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * The rune catalog's pure side: what each rune is for (its uses: combat, farming, fishing...), and how a list of runes
 * is searched, filtered, sorted and paged. No game classes, so the rules are tested on their own; the screen
 * ({@code client.RuneCatalogScreen}) passes in only what the player may read of each rune.
 *
 * <p>Uses are worked out from a rune's own data, so new runes sort themselves: its family, kind and Codex category,
 * the Passives page's rules, and plain words in its name and text ("crop", "ore", "fish"). A rune can have several.
 * A rune with none (Amplify, Delay) fits any spell, and shows only under "Any use".</p>
 */
public final class RuneCatalog {
	private RuneCatalog() {}

	/** What a rune is for. Order is the order the filter cycles through. */
	public enum Use {
		COMBAT, SUPPORT, FARMING, FISHING, MINING, BUILDING, EXPLORING, TRAVEL, PASSIVE;

		/** The lang key suffix: {@code screen.wildercord.catalog.use.<key>}. */
		public String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** Which of the player's runes to list. */
	public enum Show {
		/** Every rune they know. */
		KNOWN,
		/** Every rune there is: the ones they don't know show only their hint. */
		ALL,
		/** Only runes they can thread on the Cord they wear now. */
		READY,
		/** Only runes they're still reading (not yet understood). */
		READING;

		public String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** How the list is ordered. */
	public enum Sort {
		/** By family, then Codex category, then tier and name: the Codex's own order. */
		GROUP,
		NAME,
		/** Lowest tier first. */
		TIER,
		/** Cheapest first. */
		COST;

		public String key() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	// ------------------------------------------------------------------ uses

	/** Words that tie a rune to a use. Matched at the start of a word, so "ore" finds "ores" but not "more"; a word ending in "!" must match whole. */
	private static final Map<Use, Pattern> WORDS = Map.of(
		Use.FARMING, words("crop", "farm", "seed", "sow", "sapling", "plant", "harvest", "bone meal", "bone-meal", "animal", "herd", "breed",
			"cow", "sheep", "wool", "shear", "milk", "chicken", "egg", "hive", "bee", "honey", "flower", "tree", "log", "leaves", "food",
			"cook", "bake", "stew", "hunger", "fodder", "wheat", "berr", "pumpkin", "melon", "compost", "till", "plough", "plow", "mushroom",
			"sugar cane", "cactus", "bamboo", "feast", "picnic", "young"),
		Use.FISHING, words("fish", "catch", "bobber", "rod", "water", "sea", "ocean", "shore", "boat", "swim", "drown", "tide", "kelp",
			"coral", "axolotl", "dolphin", "squid", "turtle", "lily", "underwater", "under water", "pocket of air", "conduit", "shoal"),
		Use.MINING, words("ore", "mine", "mines", "mining", "pickaxe", "cave", "tunnel", "dig", "digs", "delve", "lava", "shaft", "stair",
			"rock", "gravel", "sand", "smelt", "fortune", "silk touch", "obsidian", "dark floor", "pitch-dark"),
		Use.BUILDING, words("build", "wall", "bridge", "plank", "brick", "floor", "door", "chest", "barrel", "concrete", "glass", "masonry",
			"polish", "pillar", "platform", "column", "path", "torch", "lever", "button", "redstone", "dye", "sign", "frame", "armor stand",
			"candle", "stone", "light", "lamp", "mud"),
		Use.EXPLORING, words("tells", "sense", "senses", "bearing", "which way", "nearest", "village", "villager", "ruin", "shipwreck",
			"fortress", "stronghold", "portal", "structure", "biome", "weather", "moon", "dusk", "dawn", "treasure", "brush",
			"suspicious", "piglin", "trade", "enchant", "brew", "beacon", "reads", "points to", "points you", "waymark", "lodestar", "nearest"),
		Use.TRAVEL, words("teleport", "dash", "speed", "jump", "glide", "fall damage", "slow falling", "ride", "mount", "horse", "fly",
			"flight", "soar", "sprint", "walk on", "blink", "leap", "float", "takes you", "go back"),
		Use.SUPPORT, words("heal!", "heals", "healing", "healed", "shield", "ally", "allies", "protect", "ward", "regenerat", "resist", "cure", "cleanse", "mend", "absorption",
			"invisib", "fire resistance", "less damage", "pet", "pets", "golem"),
		Use.COMBAT, words("damage", "enemy", "enemies", "foe", "foes", "hostile", "kill", "kills", "monster", "monsters", "undead", "strength",
			"weapon", "blow", "blows", "hunting you"));

	private static Pattern words(String... words) {
		StringBuilder out = new StringBuilder("\\b(?:");
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			boolean whole = word.endsWith("!");
			out.append(i == 0 ? "" : "|").append(Pattern.quote(whole ? word.substring(0, word.length() - 1) : word)).append(whole ? "\\b" : "");
		}
		return Pattern.compile(out.append(")").toString());
	}

	private static final Map<String, Set<Use>> CACHE = new ConcurrentHashMap<>();

	/** What {@code rune} is for (empty: it fits any spell). Worked out once per rune. */
	public static Set<Use> uses(RuneDef rune) {
		return CACHE.computeIfAbsent(rune.id(), id -> Set.copyOf(work(rune)));
	}

	private static EnumSet<Use> work(RuneDef rune) {
		EnumSet<Use> out = EnumSet.noneOf(Use.class);
		String text = (rune.name() + " " + rune.description()).toLowerCase(Locale.ROOT);
		switch (rune.family()) {
			case EFFECT -> {
				switch (rune.kind()) {
					case HARMFUL -> out.add(Use.COMBAT);
					case HELPFUL -> out.add(Use.SUPPORT);
					case MOVEMENT -> out.add(Use.TRAVEL);
					default -> {}
				}
				if (rune.category().equals("movement")) {
					out.add(Use.TRAVEL);
				}
				// Fighting runes only gain a trade use (farming, mining...) from their words if they act on the world:
				// a stone wall that hurts is building too, but Stoneskin is not mining.
				boolean fighting = rune.kind() == EffectKind.HARMFUL && !rune.category().equals("world");
				boolean helping = rune.kind() == EffectKind.HELPFUL && !rune.category().equals("world");
				for (Use use : Use.values()) {
					if (use == Use.COMBAT && rune.kind() == EffectKind.HELPFUL && rune.category().equals("support")) {
						continue;
					}
					if (fighting && use != Use.COMBAT && use != Use.TRAVEL) {
						continue;
					}
					if (helping && (use == Use.MINING || use == Use.BUILDING) && !Passives.allowed(rune)) {
						continue;
					}
					if (WORDS.containsKey(use) && WORDS.get(use).matcher(text).find()) {
						out.add(use);
					}
				}
				if (rune.kind() == EffectKind.WORLD && out.isEmpty()) {
					out.add(Use.BUILDING);
				}
			}
			case SHAPE -> {
				switch (rune.category()) {
					case "field", "kin" -> {
						for (Use use : Use.values()) {
							if (use != Use.EXPLORING && WORDS.containsKey(use) && WORDS.get(use).matcher(text).find()) {
								out.add(use);
							}
						}
						if (out.isEmpty()) {
							out.add(rune.category().equals("field") ? Use.BUILDING : Use.COMBAT);
						}
					}
					case "personal" -> {}
					default -> out.add(Use.COMBAT);
				}
			}
			case MODIFIER, LINK -> {
				// Only the everyday ones carry a trade: Amplify or Delay is for any spell.
				if (HearthLinkRules.ownsModifier(rune) || HearthLinkRules.ownsLink(rune)) {
					for (Use use : Use.values()) {
						if (WORDS.containsKey(use) && WORDS.get(use).matcher(text).find()) {
							out.add(use);
						}
					}
				} else if (rune.family() == RuneFamily.MODIFIER && rune.category().equals("projectile")) {
					out.add(Use.COMBAT);
				}
			}
			case KNOT -> {}
		}
		if (Passives.allowed(rune)) {
			out.add(Use.PASSIVE);
		}
		return out;
	}

	// ------------------------------------------------------------------ search, filter, sort, page

	/**
	 * What the player has chosen. Null (or blank) parts don't filter.
	 *
	 * @param element an element tag ("fire"), "none" for runes with no element, or null for any
	 */
	public record Filter(String query, RuneFamily family, String element, Use use, Show show, Sort sort) {
		public static final Filter NONE = new Filter("", null, null, null, Show.KNOWN, Sort.GROUP);

		public Filter {
			query = query == null ? "" : query;
			show = show == null ? Show.KNOWN : show;
			sort = sort == null ? Sort.GROUP : sort;
		}

		public Filter withQuery(String q) {
			return new Filter(q, family, element, use, show, sort);
		}

		public Filter withFamily(RuneFamily f) {
			return new Filter(query, f, element, use, show, sort);
		}

		public Filter withElement(String e) {
			return new Filter(query, family, e, use, show, sort);
		}

		public Filter withUse(Use u) {
			return new Filter(query, family, element, u, show, sort);
		}

		public Filter withShow(Show s) {
			return new Filter(query, family, element, use, s, sort);
		}

		public Filter withSort(Sort s) {
			return new Filter(query, family, element, use, show, s);
		}
	}

	/**
	 * What the screen knows of the player, so the logic stays free of the game.
	 *
	 * @param name     a rune's name as shown
	 * @param tags     words a rune is always found by: its family, category, element and use names (never its text)
	 * @param readable what the player can read of its text: a hint, the text with numbers veiled, or all of it
	 * @param known    whether the player knows it
	 * @param ready    whether it can go on the Cord worn now
	 * @param reading  whether it is still being read
	 */
	public record View(Function<RuneDef, String> name, Function<RuneDef, String> tags, Function<RuneDef, String> readable,
			Predicate<RuneDef> known, Predicate<RuneDef> ready, Predicate<RuneDef> reading) {}

	/** Whether {@code rune} passes every part of {@code filter} but the search words. */
	public static boolean passes(RuneDef rune, Filter filter, View view) {
		if (filter.family() != null && rune.family() != filter.family()) {
			return false;
		}
		if (filter.element() != null && !filter.element().equals(rune.element().isEmpty() ? "none" : rune.element())) {
			return false;
		}
		if (filter.use() != null && !uses(rune).contains(filter.use())) {
			return false;
		}
		return switch (filter.show()) {
			case KNOWN -> view.known().test(rune);
			case ALL -> true;
			case READY -> view.known().test(rune) && view.ready().test(rune);
			case READING -> view.known().test(rune) && view.reading().test(rune);
		};
	}

	/**
	 * Whether every word of {@code query} is found: in the rune's name and tags, or (words of four letters or more) in
	 * what the player can read of its text. Text is read only when the name and tags miss, as it's dearer.
	 */
	public static boolean matches(String query, String nameAndTags, java.util.function.Supplier<String> readable) {
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			return true;
		}
		String hay = nameAndTags.toLowerCase(Locale.ROOT);
		String text = null;
		for (String token : q.split("\\s+")) {
			if (hay.contains(token)) {
				continue;
			}
			if (token.length() >= 4) {
				if (text == null) {
					text = readable.get().toLowerCase(Locale.ROOT);
				}
				if (text.contains(token)) {
					continue;
				}
			}
			return false;
		}
		return true;
	}

	/** The runes that pass {@code filter}, in its order. Names that start with the search come first. */
	public static List<RuneDef> select(Collection<RuneDef> runes, Filter filter, View view) {
		List<RuneDef> out = new ArrayList<>();
		Map<RuneDef, String> names = new java.util.IdentityHashMap<>();
		for (RuneDef rune : runes) {
			if (!passes(rune, filter, view)) {
				continue;
			}
			String name = view.name().apply(rune);
			if (matches(filter.query(), name + " " + view.tags().apply(rune), () -> view.readable().apply(rune))) {
				out.add(rune);
				names.put(rune, name);
			}
		}
		String q = filter.query().trim().toLowerCase(Locale.ROOT);
		Comparator<RuneDef> byName = Comparator.comparing(r -> names.get(r).toLowerCase(Locale.ROOT));
		Comparator<RuneDef> order = switch (filter.sort()) {
			case GROUP -> Comparator.<RuneDef>comparingInt(r -> r.family().ordinal()).thenComparingInt(RuneCategories::order)
				.thenComparingInt(RuneDef::tier).thenComparing(byName);
			case NAME -> byName;
			case TIER -> Comparator.<RuneDef>comparingInt(RuneDef::tier).thenComparing(byName);
			case COST -> Comparator.<RuneDef>comparingDouble(RuneDef::cost).thenComparing(byName);
		};
		if (!q.isEmpty()) {
			order = Comparator.<RuneDef>comparingInt(r -> names.get(r).toLowerCase(Locale.ROOT).startsWith(q) ? 0 : 1).thenComparing(order);
		}
		out.sort(order);
		return out;
	}

	/** Page {@code page} (from 0, clamped) of {@code all}, {@code size} to a page. */
	public static <T> List<T> page(List<T> all, int page, int size) {
		int n = Math.max(1, size);
		int p = clampPage(all.size(), page, n);
		return all.subList(Math.min(all.size(), p * n), Math.min(all.size(), (p + 1) * n));
	}

	/** How many pages {@code count} things fill, {@code size} to a page (at least one). */
	public static int pages(int count, int size) {
		return Math.max(1, (count + Math.max(1, size) - 1) / Math.max(1, size));
	}

	public static int clampPage(int count, int page, int size) {
		return Math.max(0, Math.min(page, pages(count, size) - 1));
	}

	/** Every element a rune in {@code runes} has, in a fixed order ("none" last, if any rune has none). */
	public static List<String> elements(Collection<RuneDef> runes) {
		java.util.TreeSet<String> found = new java.util.TreeSet<>();
		boolean none = false;
		for (RuneDef rune : runes) {
			if (rune.element().isEmpty()) {
				none = true;
			} else {
				found.add(rune.element());
			}
		}
		List<String> out = new ArrayList<>(found);
		if (none) {
			out.add("none");
		}
		return out;
	}
}
