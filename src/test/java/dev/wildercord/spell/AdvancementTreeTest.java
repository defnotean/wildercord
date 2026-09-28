package dev.wildercord.spell;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.content.CordTier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The generated advancement tab (tools/generate_assets.py): every feat has an advancement, every
 * parent exists, there is one root with its background, and every name, icon, criterion and
 * reward it points at is real.
 */
class AdvancementTreeTest {
	private static final Path RESOURCES = Path.of("src/main/resources");
	private static final Path ADVANCEMENTS = RESOURCES.resolve("data/wildercord/advancement");
	private static final Set<String> TRIGGERS = Set.of("wildercord:feat", "wildercord:grimoire", "wildercord:heart_circle",
		"wildercord:runes_known", "wildercord:cord", "wildercord:moment", "minecraft:inventory_changed", "minecraft:location");

	/** Advancement id (e.g. {@code wildercord:casting/overcast}) -> its JSON, recipe-book unlocks left out. */
	private static Map<String, JsonObject> tree;
	private static JsonObject lang;

	@BeforeAll
	static void load() throws IOException {
		tree = new TreeMap<>();
		try (Stream<Path> files = Files.walk(ADVANCEMENTS)) {
			for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
				String path = ADVANCEMENTS.relativize(file).toString().replace('\\', '/');
				if (!path.startsWith("recipes/")) {
					tree.put("wildercord:" + path.substring(0, path.length() - ".json".length()), read(file));
				}
			}
		}
		lang = read(RESOURCES.resolve("assets/wildercord/lang/en_us.json"));
	}

	private static JsonObject read(Path file) throws IOException {
		return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
	}

	private static List<JsonObject> criteria(JsonObject advancement) {
		List<JsonObject> all = new ArrayList<>();
		for (Map.Entry<String, JsonElement> criterion : advancement.getAsJsonObject("criteria").entrySet()) {
			all.add(criterion.getValue().getAsJsonObject());
		}
		return all;
	}

	@Test
	void everyFeatHasAnAdvancement() {
		Set<String> granted = new HashSet<>();
		for (JsonObject advancement : tree.values()) {
			for (JsonObject criterion : criteria(advancement)) {
				if (criterion.get("trigger").getAsString().equals("wildercord:feat")) {
					granted.add(criterion.getAsJsonObject("conditions").get("feat").getAsString());
				}
			}
		}
		for (Feats.Feat feat : Feats.FEATS) {
			assertTrue(granted.contains(feat.id()), "no advancement for the feat " + feat.id() + ": add a feat_adv line to tools/generate_assets.py");
		}
	}

	@Test
	void everyParentExistsAndThereIsOneRoot() {
		List<String> roots = new ArrayList<>();
		for (Map.Entry<String, JsonObject> entry : tree.entrySet()) {
			JsonObject advancement = entry.getValue();
			if (!advancement.has("parent")) {
				roots.add(entry.getKey());
				continue;
			}
			String parent = advancement.get("parent").getAsString();
			assertTrue(tree.containsKey(parent), entry.getKey() + ": its parent " + parent + " doesn't exist");
			// Walking up always reaches the root: no loops.
			Set<String> seen = new HashSet<>();
			for (String at = entry.getKey(); tree.get(at).has("parent"); at = tree.get(at).get("parent").getAsString()) {
				assertTrue(seen.add(at), entry.getKey() + ": its parents loop");
			}
		}
		assertEquals(List.of("wildercord:root"), roots);
		String background = tree.get("wildercord:root").getAsJsonObject("display").get("background").getAsString();
		Path texture = RESOURCES.resolve("assets/wildercord/textures/" + background.substring("wildercord:".length()) + ".png");
		assertTrue(Files.exists(texture), "the tab's background " + texture + " is missing");
	}

	@Test
	void everythingItNamesExists() {
		Set<String> ranks = Set.of("task", "goal", "challenge");
		for (Map.Entry<String, JsonObject> entry : tree.entrySet()) {
			String id = entry.getKey();
			JsonObject advancement = entry.getValue();
			JsonObject display = advancement.getAsJsonObject("display");
			for (String part : List.of("title", "description")) {
				String key = display.getAsJsonObject(part).get("translate").getAsString();
				assertTrue(lang.has(key), id + ": no text for " + key);
			}
			assertTrue(!display.has("frame") || ranks.contains(display.get("frame").getAsString()), id);
			JsonObject icon = display.getAsJsonObject("icon");
			if (icon.has("components")) {
				String rune = icon.getAsJsonObject("components").get("wildercord:rune").getAsString();
				assertTrue(Runes.get(rune).isPresent(), id + ": its icon is an unknown rune " + rune);
			}

			Set<String> names = advancement.getAsJsonObject("criteria").keySet();
			assertFalse(names.isEmpty(), id);
			Set<String> required = new HashSet<>();
			advancement.getAsJsonArray("requirements").forEach(group -> group.getAsJsonArray().forEach(name -> required.add(name.getAsString())));
			assertEquals(names, required, id + ": its requirements should name every criterion");
			for (JsonObject criterion : criteria(advancement)) {
				String trigger = criterion.get("trigger").getAsString();
				assertTrue(TRIGGERS.contains(trigger), id + ": unknown trigger " + trigger);
				JsonObject conditions = criterion.getAsJsonObject("conditions");
				switch (trigger) {
					case "wildercord:cord" -> assertTrue(java.util.Arrays.stream(CordTier.values()).anyMatch(t -> t.key.equals(conditions.get("tier").getAsString())), id);
					case "wildercord:heart_circle" -> {
						int level = conditions.get("level").getAsInt();
						assertTrue(level >= 1 && level <= Circles.MAX, id);
					}
					case "wildercord:grimoire" -> {
						if (conditions.has("entry")) {
							assertTrue(Feats.everyEntry().contains(conditions.get("entry").getAsString()), id + ": no such Grimoire entry");
						}
					}
					default -> {}
				}
			}

			if (advancement.has("rewards") && advancement.getAsJsonObject("rewards").has("loot")) {
				for (JsonElement table : advancement.getAsJsonObject("rewards").getAsJsonArray("loot")) {
					String path = table.getAsString().substring("wildercord:".length());
					assertTrue(Files.exists(RESOURCES.resolve("data/wildercord/loot_table/" + path + ".json")), id + ": no loot table " + path);
				}
			}
		}
	}

	@Test
	void aFullGrimoireHoldsEveryFeatReactionAndSecret() {
		List<String> every = Feats.everyEntry();
		assertEquals(Feats.FEATS.size() + Feats.REACTIONS.size() + Secrets.ALL.size(), every.size());
		assertEquals(every.size(), new HashSet<>(every).size());
		List<String> almost = new ArrayList<>(every);
		String last = almost.removeLast();
		assertFalse(Feats.complete(almost, ""));
		assertEquals(!last.startsWith("secret:"), Feats.complete(almost, "secret:"));
		assertTrue(Feats.complete(every, ""));
		// Riddles read don't fill it.
		assertFalse(Feats.complete(List.of("hint:sunfall"), "secret:"));
	}
}
