package dev.wildercord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Minecraft reads data files through codecs that skip keys they don't know, so a file written in an
 * older format still loads and quietly does less (26.3 renamed loot tables' {@code "functions"} and
 * {@code "conditions"}, and every chest rune came out blank). Every kind of file the mod ships is
 * held here to the keys Minecraft 26.3's own codecs read (taken from the game's codecs and checked
 * against vanilla's data files) and to the keys they require. A new kind of file must be added here.
 */
class DataFormatTest {
	private static final Path DATA = Path.of("src/main/resources/data");

	/** The keys of a loot condition or modifier's name: 26.3 names each by "type". */
	private static final Set<String> OLD_NAMES = Set.of("condition", "function");

	private static final Map<String, Set<String>> TRIGGER_CONDITIONS = Map.of(
		"wildercord:feat", Set.of("player", "feat"),
		"wildercord:grimoire", Set.of("player", "entry", "prefix", "count", "all", "signatures"),
		"wildercord:heart_circle", Set.of("player", "level"),
		"wildercord:runes_known", Set.of("player", "count", "all"),
		"wildercord:cord", Set.of("player", "tier"),
		"wildercord:moment", Set.of("player", "moment"),
		"minecraft:inventory_changed", Set.of("player", "slots", "items"),
		"minecraft:recipe_unlocked", Set.of("player", "recipes"),
		"minecraft:location", Set.of("player"));

	@Test
	void advancements() throws IOException {
		for (Path file : files("wildercord/advancement")) {
			JsonObject advancement = read(file);
			allowed(advancement, file, "parent", "display", "rewards", "criteria", "requirements", "sends_telemetry_event");
			required(advancement, file, "criteria");
			for (Map.Entry<String, JsonElement> entry : advancement.getAsJsonObject("criteria").entrySet()) {
				JsonObject criterion = entry.getValue().getAsJsonObject();
				String where = file + " criterion " + entry.getKey();
				allowed(criterion, where, "trigger", "conditions");
				required(criterion, where, "trigger");
				String trigger = criterion.get("trigger").getAsString();
				Set<String> keys = TRIGGER_CONDITIONS.get(trigger);
				assertTrue(keys != null, where + ": no format known for the trigger " + trigger + ", add it here once checked");
				if (criterion.has("conditions")) {
					JsonObject conditions = criterion.getAsJsonObject("conditions");
					allowed(conditions, where, keys.toArray(String[]::new));
					if (conditions.has("player")) {
						condition(conditions.get("player"), where + " player");
					}
				}
			}
			if (advancement.has("display")) {
				JsonObject display = advancement.getAsJsonObject("display");
				allowed(display, file + " display", "icon", "title", "description", "background", "frame", "show_toast", "announce_to_chat", "hidden");
				required(display, file + " display", "icon", "title", "description");
				stack(display.getAsJsonObject("icon"), file + " icon");
			}
			if (advancement.has("rewards")) {
				allowed(advancement.getAsJsonObject("rewards"), file + " rewards", "experience", "loot", "recipes", "function");
			}
		}
	}

	@Test
	void lootTables() throws IOException {
		for (Path file : files("wildercord/loot_table")) {
			JsonObject table = read(file);
			allowed(table, file, "type", "random_sequence", "pools", "modifier");
			required(table, file, "type", "pools");
			for (JsonElement p : table.getAsJsonArray("pools")) {
				JsonObject pool = p.getAsJsonObject();
				allowed(pool, file + " pool", "entries", "condition", "modifier", "rolls", "bonus_rolls");
				required(pool, file + " pool", "entries", "rolls");
				if (pool.has("condition")) {
					condition(pool.get("condition"), file + " pool condition");
				}
				for (JsonElement e : pool.getAsJsonArray("entries")) {
					JsonObject entry = e.getAsJsonObject();
					allowed(entry, file + " entry", "type", "name", "weight", "quality", "condition", "modifier");
					required(entry, file + " entry", "type", "name");
					if (entry.has("modifier")) {
						condition(entry.get("modifier"), file + " entry modifier");
					}
					if (entry.has("condition")) {
						condition(entry.get("condition"), file + " entry condition");
					}
				}
			}
		}
	}

	@Test
	void enchantments() throws IOException {
		for (Path file : files("wildercord/enchantment")) {
			JsonObject enchantment = read(file);
			allowed(enchantment, file, "description", "exclusive_set", "effects", "supported_items", "primary_items", "weight", "max_level",
				"min_cost", "max_cost", "anvil_cost", "slots");
			required(enchantment, file, "description", "supported_items", "weight", "max_level", "min_cost", "max_cost", "anvil_cost", "slots");
			for (String cost : List.of("min_cost", "max_cost")) {
				allowed(enchantment.getAsJsonObject(cost), file + " " + cost, "base", "per_level_above_first");
				required(enchantment.getAsJsonObject(cost), file + " " + cost, "base", "per_level_above_first");
			}
		}
	}

	@Test
	void recipes() throws IOException {
		for (Path file : files("wildercord/recipe")) {
			JsonObject recipe = read(file);
			required(recipe, file, "type");
			switch (recipe.get("type").getAsString()) {
				case "minecraft:crafting_shaped" -> {
					allowed(recipe, file, "type", "category", "group", "show_notification", "key", "pattern", "result");
					required(recipe, file, "key", "pattern", "result");
					recipe.getAsJsonObject("key").entrySet().forEach(key -> ingredient(key.getValue(), file + " key " + key.getKey()));
					stack(recipe.getAsJsonObject("result"), file + " result");
				}
				case "minecraft:smithing_transform" -> {
					allowed(recipe, file, "type", "template", "base", "addition", "result");
					required(recipe, file, "template", "base", "addition", "result");
					for (String part : List.of("template", "base", "addition")) {
						ingredient(recipe.get(part), file + " " + part);
					}
					stack(recipe.getAsJsonObject("result"), file + " result");
				}
				case "minecraft:crafting_shapeless", "wildercord:upgrade", "wildercord:cord_upgrade", "wildercord:manual_pages" -> {
					allowed(recipe, file, "type", "category", "group", "show_notification", "ingredients", "result");
					required(recipe, file, "ingredients", "result");
					recipe.getAsJsonArray("ingredients").forEach(ingredient -> ingredient(ingredient, file + " ingredient"));
					stack(recipe.getAsJsonObject("result"), file + " result");
				}
				case "minecraft:crafting_dye" -> {
					allowed(recipe, file, "type", "category", "group", "show_notification", "target", "dye", "result");
					required(recipe, file, "target", "dye", "result");
					ingredient(recipe.get("target"), file + " target");
					ingredient(recipe.get("dye"), file + " dye");
					stack(recipe.getAsJsonObject("result"), file + " result");
				}
				case "minecraft:brewing" -> {
					allowed(recipe, file, "type", "input", "reagent", "output");
					required(recipe, file, "input", "reagent", "output");
					for (String part : List.of("input", "reagent")) {
						allowed(recipe.getAsJsonObject(part), file + " " + part, "item", "potion_contents");
						required(recipe.getAsJsonObject(part), file + " " + part, "item");
					}
					stack(recipe.getAsJsonObject("output"), file + " output");
				}
				default -> fail(file + ": no format known for the recipe type " + recipe.get("type").getAsString() + ", add it here once checked");
			}
		}
	}

	@Test
	void villagerTrades() throws IOException {
		for (Path file : files("wildercord/villager_trade")) {
			JsonObject trade = read(file);
			allowed(trade, file, "wants", "additional_wants", "gives", "max_uses", "xp", "reputation_discount", "merchant_predicate",
				"given_item_modifier", "double_trade_price_enchantments");
			required(trade, file, "wants", "gives");
			for (String cost : List.of("wants", "additional_wants")) {
				if (trade.has(cost)) {
					allowed(trade.getAsJsonObject(cost), file + " " + cost, "id", "count", "components");
					required(trade.getAsJsonObject(cost), file + " " + cost, "id");
				}
			}
			stack(trade.getAsJsonObject("gives"), file + " gives");
			if (trade.has("given_item_modifier")) {
				condition(trade.get("given_item_modifier"), file + " given_item_modifier");
			}
		}
		for (Path file : files("wildercord/trade_set")) {
			JsonObject set = read(file);
			allowed(set, file, "trades", "amount", "allow_duplicates", "random_sequence");
			required(set, file, "trades", "amount");
		}
	}

	@Test
	void worldgen() throws IOException {
		for (Path file : files("wildercord/worldgen/structure")) {
			JsonObject structure = read(file);
			required(structure, file, "type", "biomes", "spawn_overrides", "step");
			if (structure.get("type").getAsString().equals("wildercord:dungeon")) {
				allowed(structure, file, "type", "biomes", "spawn_overrides", "step", "terrain_adaptation", "dungeon");
				required(structure, file, "dungeon");
			} else {
				allowed(structure, file, "type", "biomes", "spawn_overrides", "step", "terrain_adaptation");
			}
			for (Map.Entry<String, JsonElement> category : structure.getAsJsonObject("spawn_overrides").entrySet()) {
				JsonObject override = category.getValue().getAsJsonObject();
				allowed(override, file + " " + category.getKey(), "bounding_box", "spawns");
				required(override, file + " " + category.getKey(), "bounding_box", "spawns");
				for (JsonElement spawn : override.getAsJsonArray("spawns")) {
					allowed(spawn.getAsJsonObject(), file + " spawn", "type", "count", "weight");
					required(spawn.getAsJsonObject(), file + " spawn", "type", "count", "weight");
				}
			}
		}
		for (Path file : files("wildercord/worldgen/structure_set")) {
			JsonObject set = read(file);
			allowed(set, file, "structures", "placement");
			required(set, file, "structures", "placement");
			JsonObject placement = set.getAsJsonObject("placement");
			allowed(placement, file + " placement", "type", "salt", "spacing", "separation", "spread_type", "frequency",
				"frequency_reduction_method", "locate_offset", "exclusion_zone");
			required(placement, file + " placement", "type", "salt", "spacing", "separation");
			if (placement.has("exclusion_zone")) {
				allowed(placement.getAsJsonObject("exclusion_zone"), file + " exclusion_zone", "other_set", "chunk_count");
				required(placement.getAsJsonObject("exclusion_zone"), file + " exclusion_zone", "other_set", "chunk_count");
			}
			for (JsonElement entry : set.getAsJsonArray("structures")) {
				allowed(entry.getAsJsonObject(), file + " structure", "structure", "weight");
				required(entry.getAsJsonObject(), file + " structure", "structure", "weight");
			}
		}
	}

	@Test
	void damageTypes() throws IOException {
		Set<String> scalings = Set.of("never", "when_caused_by_living_non_player", "always");
		for (Path file : files("wildercord/damage_type")) {
			JsonObject type = read(file);
			allowed(type, file, "message_id", "scaling", "exhaustion", "effects", "death_message_type");
			required(type, file, "message_id", "scaling", "exhaustion");
			assertTrue(scalings.contains(type.get("scaling").getAsString()), file + " has an unknown scaling");
			type.get("exhaustion").getAsFloat();
		}
	}

	@Test
	void tags() throws IOException {
		for (String root : List.of("wildercord/tags", "minecraft/tags")) {
			for (Path file : files(root)) {
				JsonObject tag = read(file);
				allowed(tag, file, "values", "replace");
				required(tag, file, "values");
			}
		}
	}

	/** Every data file is one of the kinds checked above. */
	@Test
	void everyKindOfDataIsChecked() throws IOException {
		Set<String> checked = Set.of("wildercord/advancement", "wildercord/loot_table", "wildercord/enchantment", "wildercord/recipe",
			"wildercord/villager_trade", "wildercord/trade_set", "wildercord/worldgen/structure", "wildercord/worldgen/structure_set",
			"wildercord/tags", "minecraft/tags", "wildercord/dimension", "wildercord/damage_type");
		List<Path> all = files("");
		assertTrue(!all.isEmpty(), "there should be data files to check");
		for (Path file : all) {
			String path = DATA.relativize(file).toString().replace('\\', '/');
			assertTrue(checked.stream().anyMatch(kind -> path.startsWith(kind + "/")),
				file + " is a kind of data nothing checks: compare it with Minecraft's codec and add it to DataFormatTest");
		}
	}

	@Test
	void practiceDimensionUsesAValidFlatGenerator() throws IOException {
		for (Path file : files("wildercord/dimension")) {
			JsonObject dimension = read(file);
			assertEquals("minecraft:overworld", dimension.get("type").getAsString());
			JsonObject generator = dimension.getAsJsonObject("generator");
			assertEquals("minecraft:flat", generator.get("type").getAsString());
			JsonObject settings = generator.getAsJsonObject("settings");
			assertEquals("minecraft:plains",settings.get("biome").getAsString());
			assertFalse(settings.get("features").getAsBoolean());
			assertFalse(settings.get("lakes").getAsBoolean());
			int height = 0;
			for (var layer : settings.getAsJsonArray("layers")) {
				int thickness = layer.getAsJsonObject().get("height").getAsInt();
				assertTrue(thickness > 0); height += thickness;
				assertTrue(layer.getAsJsonObject().get("block").getAsString().startsWith("minecraft:"));
			}
			assertTrue(height <= 384);
			assertTrue(settings.getAsJsonArray("structure_overrides").isEmpty());
		}
	}

	// ------------------------------------------------------------------ helpers

	private static List<Path> files(String under) throws IOException {
		try (Stream<Path> files = Files.walk(DATA.resolve(under))) {
			return files.filter(p -> p.toString().endsWith(".json")).toList();
		}
	}

	private static JsonObject read(Path file) throws IOException {
		return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
	}

	private static void allowed(JsonObject object, Object where, String... keys) {
		Set<String> known = Set.of(keys);
		List<String> unknown = new ArrayList<>();
		for (String key : object.keySet()) {
			if (!known.contains(key)) {
				unknown.add(key);
			}
		}
		assertTrue(unknown.isEmpty(), where + ": Minecraft 26.3 ignores the keys " + unknown);
	}

	private static void required(JsonObject object, Object where, String... keys) {
		for (String key : keys) {
			assertTrue(object.has(key), where + ": missing \"" + key + "\"");
		}
	}

	/** A loot condition or modifier (one, or a list of them): named by "type", never the older names. */
	private static void condition(JsonElement element, String where) {
		if (element.isJsonPrimitive()) {
			return; // a reference to a predicate or item modifier file
		}
		if (element.isJsonArray()) {
			element.getAsJsonArray().forEach(e -> condition(e, where));
			return;
		}
		JsonObject object = element.getAsJsonObject();
		required(object, where, "type");
		for (String old : OLD_NAMES) {
			assertTrue(!object.has(old) || !object.get(old).isJsonPrimitive(), where + ": named by \"" + old + "\", which 26.3 no longer reads");
		}
	}

	/** An item stack as a result, icon or trade: {@code {"id", "count", "components"}}, never the older {@code "item"}. */
	private static void stack(JsonObject stack, String where) {
		allowed(stack, where, "id", "count", "components");
		required(stack, where, "id");
	}

	/** An ingredient: an item id, a {@code #tag} or a list of them; the older {@code {"item": ...}} objects fail. */
	private static void ingredient(JsonElement ingredient, String where) {
		if (ingredient.isJsonArray()) {
			ingredient.getAsJsonArray().forEach(e -> ingredient(e, where));
			return;
		}
		assertTrue(ingredient.isJsonPrimitive(), where + ": an ingredient is an item id or #tag in 26.3, not " + ingredient);
	}
}
