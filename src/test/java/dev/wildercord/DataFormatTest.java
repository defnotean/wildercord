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
 * Minecraft reads data files through codecs that skip keys they don't know, so a file written in another
 * version's format can still load and quietly do less. Every kind of file the mod ships is held here to
 * the keys read by the active {@link TestDataProfile#MC_VERSION} codecs and to the keys they require. A new
 * kind of file must be added here.
 */
class DataFormatTest {
	private static final Path DATA = Path.of("src/main/resources/data");
	private static final Set<String> OLD_NAMES = Set.of("condition", "function");

	private static final Map<String, Set<String>> TRIGGER_CONDITIONS = Map.ofEntries(
		Map.entry("wildercord:feat", Set.of("player", "feat")),
		Map.entry("wildercord:grimoire", Set.of("player", "entry", "prefix", "count", "all", "signatures")),
		Map.entry("wildercord:heart_circle", Set.of("player", "level")),
		Map.entry("wildercord:runes_known", Set.of("player", "count", "all")),
		Map.entry("wildercord:cord", Set.of("player", "tier")),
		Map.entry("wildercord:moment", Set.of("player", "moment")),
		Map.entry("minecraft:inventory_changed", Set.of("player", "slots", "items")),
		Map.entry("minecraft:consume_item", Set.of("player", "item")),
		Map.entry("minecraft:recipe_unlocked", TestDataProfile.is26_2() ? Set.of("player", "recipe") : Set.of("player", "recipes")),
		Map.entry("minecraft:location", Set.of("player")),
		Map.entry("minecraft:villager_trade", Set.of("player", "villager", "item")),
		Map.entry("minecraft:player_killed_entity", Set.of("player", "entity", "killing_blow")));

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
						JsonElement player = conditions.get("player");
						if (TestDataProfile.is26_2()) {
							assertTrue(player.isJsonArray(), where + " player: 26.2 expects a condition list");
							conditionList(player, where + " player", "condition");
						} else {
							condition(player, where + " player");
						}
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
		boolean is262 = TestDataProfile.is26_2();
		for (Path file : files("wildercord/loot_table")) {
			JsonObject table = read(file);
			if (is262) {
				allowed(table, file, "type", "random_sequence", "pools", "functions");
				if (table.has("functions")) {
					conditionList(table.get("functions"), file + " table functions", "function");
				}
			} else {
				allowed(table, file, "type", "random_sequence", "pools", "modifier");
			}
			required(table, file, "type", "pools");
			for (JsonElement p : table.getAsJsonArray("pools")) {
				JsonObject pool = p.getAsJsonObject();
				if (is262) {
					allowed(pool, file + " pool", "entries", "conditions", "functions", "rolls", "bonus_rolls");
					if (pool.has("conditions")) {
						conditionList(pool.get("conditions"), file + " pool conditions", "condition");
					}
					if (pool.has("functions")) {
						conditionList(pool.get("functions"), file + " pool functions", "function");
					}
				} else {
					allowed(pool, file + " pool", "entries", "condition", "modifier", "rolls", "bonus_rolls");
					if (pool.has("condition")) {
						condition(pool.get("condition"), file + " pool condition");
					}
				}
				required(pool, file + " pool", "entries", "rolls");
				for (JsonElement e : pool.getAsJsonArray("entries")) {
					JsonObject entry = e.getAsJsonObject();
					required(entry, file + " entry", "type", "name");
					if (is262) {
						allowed(entry, file + " entry", "type", "name", "weight", "quality", "conditions", "functions");
						if (entry.has("functions")) {
							conditionList(entry.get("functions"), file + " entry functions", "function");
						}
						if (entry.has("conditions")) {
							conditionList(entry.get("conditions"), file + " entry conditions", "condition");
						}
					} else {
						allowed(entry, file + " entry", "type", "name", "weight", "quality", "condition", "modifier");
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
					assertFalse(TestDataProfile.is26_2(), file + ": brewing lives under data/wildercord/brewing in 26.2");
					checkBrewingRecipe(file, recipe);
				}
				default -> fail(file + ": no format known for the recipe type " + recipe.get("type").getAsString() + ", add it here once checked");
			}
		}
	}

	@Test
	void brewing() throws IOException {
		if (!TestDataProfile.is26_2()) {
			return;
		}
		for (Path file : files("wildercord/brewing")) {
			JsonObject recipe = read(file);
			checkBrewingRecipe(file, recipe);
		}
	}

	private static void checkBrewingRecipe(Path file, JsonObject recipe) {
		allowed(recipe, file, "type", "input", "reagent", "output");
		required(recipe, file, "type", "input", "reagent", "output");
		assertEquals("minecraft:brewing", recipe.get("type").getAsString(), file + " type");
		for (String part : List.of("input", "reagent")) {
			allowed(recipe.getAsJsonObject(part), file + " " + part, "item", "potion_contents");
			required(recipe.getAsJsonObject(part), file + " " + part, "item");
		}
		stack(recipe.getAsJsonObject("output"), file + " output");
	}

	@Test
	void villagerTrades() throws IOException {
		boolean is262 = TestDataProfile.is26_2();
		String modifierKey = is262 ? "given_item_modifiers" : "given_item_modifier";
		for (Path file : files("wildercord/villager_trade")) {
			JsonObject trade = read(file);
			allowed(trade, file, "wants", "additional_wants", "gives", "max_uses", "xp", "reputation_discount", "merchant_predicate",
				modifierKey, "double_trade_price_enchantments");
			required(trade, file, "wants", "gives");
			for (String cost : List.of("wants", "additional_wants")) {
				if (trade.has(cost)) {
					allowed(trade.getAsJsonObject(cost), file + " " + cost, "id", "count", "components");
					required(trade.getAsJsonObject(cost), file + " " + cost, "id");
				}
			}
			stack(trade.getAsJsonObject("gives"), file + " gives");
			if (trade.has(modifierKey)) {
				if (is262) {
					conditionList(trade.get(modifierKey), file + " " + modifierKey, "function");
				} else {
					condition(trade.get(modifierKey), file + " " + modifierKey);
				}
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
		boolean is262 = TestDataProfile.is26_2();
		for (Path file : files("wildercord/worldgen/structure")) {
			JsonObject structure = read(file);
			required(structure, file, "type", "biomes", "spawn_overrides", "step");
			if (structure.get("type").getAsString().equals("wildercord:dungeon")) {
				allowed(structure, file, "type", "biomes", "spawn_overrides", "step", "terrain_adaptation", "dungeon");
				required(structure, file, "dungeon");
			} else if (structure.get("type").getAsString().equals("wildercord:site")) {
				allowed(structure, file, "type", "biomes", "spawn_overrides", "step", "terrain_adaptation", "site");
				required(structure, file, "site");
			} else {
				allowed(structure, file, "type", "biomes", "spawn_overrides", "step", "terrain_adaptation");
			}
			for (Map.Entry<String, JsonElement> category : structure.getAsJsonObject("spawn_overrides").entrySet()) {
				JsonObject override = category.getValue().getAsJsonObject();
				allowed(override, file + " " + category.getKey(), "bounding_box", "spawns");
				required(override, file + " " + category.getKey(), "bounding_box", "spawns");
				for (JsonElement spawn : override.getAsJsonArray("spawns")) {
					if (is262) {
						allowed(spawn.getAsJsonObject(), file + " spawn", "type", "minCount", "maxCount", "weight");
						required(spawn.getAsJsonObject(), file + " spawn", "type", "minCount", "maxCount", "weight");
					} else {
						allowed(spawn.getAsJsonObject(), file + " spawn", "type", "count", "weight");
						required(spawn.getAsJsonObject(), file + " spawn", "type", "count", "weight");
					}
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
	void vegetationFeatureFormats() throws IOException {
		boolean is262 = TestDataProfile.is26_2();
		for (Path file : files("wildercord/worldgen/" + TestDataProfile.featureDirName())) {
			var feature = read(file);
			String type = feature.get("type").getAsString();
			if (is262) {
				allowed(feature, file, "type", "config");
				required(feature, file, "type", "config");
				var cfg = feature.getAsJsonObject("config");
				switch (type) {
					case "minecraft:simple_block" -> {
						allowed(cfg, file, "to_place", "schedule_tick");
						required(cfg, file, "to_place");
						var provider = cfg.getAsJsonObject("to_place");
						allowed(provider, file + " provider", "type", "state");
						required(provider, file + " provider", "type", "state");
						assertEquals("minecraft:simple_state_provider", provider.get("type").getAsString(), file + " provider");
						var state = provider.getAsJsonObject("state");
						allowed(state, file + " state", "Name", "Properties");
						required(state, file, "Name");
					}
					case "wildercord:moonreed_patch", "wildercord:glowcap_patch", "wildercord:breathmark_site", "wildercord:belowkeeper_drainhouse", "wildercord:cinder_fern_patch", "wildercord:ley_spire_patch", "wildercord:caldera_fumarole_patch" -> allowed(cfg, file + " config");
					default -> fail(file + ": unsupported vegetation feature " + feature);
				}
			} else {
				required(feature, file, "type");
				switch (type) {
					case "minecraft:simple_block" -> {
						allowed(feature, file, "type", "to_place", "schedule_tick");
						required(feature, file, "to_place");
						var state = feature.getAsJsonObject("to_place");
						allowed(state, file + " state", "id", "properties");
						required(state, file, "id");
					}
					case "wildercord:moonreed_patch", "wildercord:glowcap_patch", "wildercord:breathmark_site", "wildercord:belowkeeper_drainhouse", "wildercord:cinder_fern_patch", "wildercord:ley_spire_patch", "wildercord:caldera_fumarole_patch" -> allowed(feature, file, "type");
					default -> fail(file + ": unsupported vegetation feature " + feature);
				}
			}
		}
		for(Path file:files("wildercord/worldgen/placed_feature")) {
			var feature=read(file);allowed(feature,file,"feature","placement");required(feature,file,"feature","placement");
			for(var e:feature.getAsJsonArray("placement")) {
				var modifier=e.getAsJsonObject();required(modifier,file,"type");
				switch(modifier.get("type").getAsString()) {
					case "minecraft:rarity_filter" -> {allowed(modifier,file,"type","chance");required(modifier,file,"chance");assertTrue(modifier.get("chance").getAsInt()>0);}
					case "minecraft:in_square","minecraft:biome" -> allowed(modifier,file,"type");
					case "minecraft:heightmap" -> {allowed(modifier,file,"type","heightmap");required(modifier,file,"heightmap");}
					case "minecraft:height_range" -> {
						allowed(modifier,file,"type","height");required(modifier,file,"height");
						var height=modifier.getAsJsonObject("height");
						allowed(height,file,"type","min_inclusive","max_inclusive");required(height,file,"type","min_inclusive","max_inclusive");
						assertEquals("minecraft:uniform",height.get("type").getAsString(),file+" height provider");
						var low=height.getAsJsonObject("min_inclusive");var high=height.getAsJsonObject("max_inclusive");
						allowed(low,file,"absolute");required(low,file,"absolute");allowed(high,file,"absolute");required(high,file,"absolute");
						assertTrue(low.get("absolute").getAsInt()<=high.get("absolute").getAsInt(),file+" inverted height bounds");
					}
					case "minecraft:count" -> {allowed(modifier,file,"type","count");required(modifier,file,"count");assertTrue(modifier.get("count").getAsInt()>0);}
					case "minecraft:random_offset" -> {
						assertTrue(is262, file + ": minecraft:random_offset belongs to 26.2");
						allowed(modifier,file,"type","xz_spread","y_spread");required(modifier,file,"xz_spread","y_spread");
						for(String axis:List.of("xz_spread","y_spread")) {var provider=modifier.getAsJsonObject(axis);allowed(provider,file,"type","min","max","plateau");required(provider,file,"type","min","max","plateau");assertEquals("minecraft:trapezoid",provider.get("type").getAsString());assertTrue(provider.get("min").getAsInt()<=provider.get("max").getAsInt());}
					}
					case "minecraft:offset" -> {
						assertFalse(is262, file + ": minecraft:offset belongs to 26.3");
						allowed(modifier,file,"type","x","y","z");required(modifier,file,"x","y","z");
						for(String axis:List.of("x","y","z")) {var provider=modifier.getAsJsonObject(axis);allowed(provider,file,"type","min","max","plateau");required(provider,file,"type","min","max","plateau");assertEquals("minecraft:trapezoid",provider.get("type").getAsString());assertTrue(provider.get("min").getAsInt()<=provider.get("max").getAsInt());}
					}
					case "minecraft:block_predicate_filter" -> {allowed(modifier,file,"type","predicate");required(modifier,file,"predicate");vegetationPredicate(modifier.getAsJsonObject("predicate"),file);}
					default -> fail(file+": unsupported vegetation placement modifier "+modifier);
				}
			}
		}
	}
	private static void vegetationPredicate(JsonObject predicate,Object where) {
		required(predicate,where,"type");
		switch(predicate.get("type").getAsString()) {
			case "minecraft:all_of" -> {allowed(predicate,where,"type","predicates");required(predicate,where,"predicates");for(var child:predicate.getAsJsonArray("predicates"))vegetationPredicate(child.getAsJsonObject(),where);}
			case "minecraft:matching_block_tag" -> {allowed(predicate,where,"type","tag","offset");required(predicate,where,"tag");if(predicate.has("offset"))assertEquals(3,predicate.getAsJsonArray("offset").size());}
			default -> fail(where+": unsupported vegetation predicate "+predicate);
		}
	}

	/** Every data file is one of the kinds checked above. */
	@Test
	void regionalBiomesUseTheAttributeBasedFormat() throws IOException {
		boolean is262 = TestDataProfile.is26_2();
		for (Path file : files("wildercord/worldgen/biome")) {
			var biome = read(file);
			if (is262) {
				allowed(biome, file, "attributes", "carvers", "downfall", "effects", "features", "has_precipitation",
					"spawn_costs", "spawners", "temperature", "temperature_modifier");
				required(biome, file, "effects", "features", "carvers", "has_precipitation", "spawn_costs", "spawners", "temperature", "downfall");
			} else {
				allowed(biome, file, "attributes", "carvers", "downfall", "effects", "features", "has_precipitation", "temperature", "temperature_modifier");
				required(biome, file, "effects", "features", "carvers", "has_precipitation", "temperature", "downfall");
			}
			assertTrue(biome.get("has_precipitation").getAsJsonPrimitive().isBoolean());
			assertTrue(Double.isFinite(biome.get("temperature").getAsDouble()));
			assertTrue(biome.get("downfall").getAsDouble() >= 0 && biome.get("downfall").getAsDouble() <= 1);
			var effects = biome.getAsJsonObject("effects");
			allowed(effects, file, "water_color", "foliage_color", "dry_foliage_color", "grass_color", "grass_color_modifier");
			required(effects, file, "water_color");
			for (var effect : effects.entrySet()) {
				if (effect.getKey().endsWith("_color")) assertTrue(effect.getValue().getAsString().matches("#[0-9a-fA-F]{6}"), file + " color " + effect.getKey());
			}
			for (var carver : biome.getAsJsonArray("carvers")) assertTrue(carver.getAsString().matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"));
			for (var stage : biome.getAsJsonArray("features")) {
				assertTrue(stage.isJsonArray(), file + " decoration stage");
				for (var feature : stage.getAsJsonArray()) assertTrue(feature.getAsString().matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"));
			}
			var attributes = biome.getAsJsonObject("attributes");
			if (is262) {
				allowed(attributes, file, "minecraft:audio/background_music", "minecraft:gameplay/increased_fire_burnout",
					"minecraft:visual/sky_color", "minecraft:visual/water_fog_color", "minecraft:visual/water_fog_end_distance");
				var spawners = biome.getAsJsonObject("spawners");
				allowed(spawners, file, "ambient", "axolotls", "creature", "misc", "monster", "underground_water_creature", "water_ambient", "water_creature");
				for (var pool : spawners.entrySet()) for (var value : pool.getValue().getAsJsonArray()) {
					var spawn = value.getAsJsonObject();
					allowed(spawn, file, "type", "weight", "minCount", "maxCount"); required(spawn, file, "type", "weight", "minCount", "maxCount");
					assertTrue(spawn.get("type").getAsString().matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"));
					assertTrue(spawn.get("weight").getAsInt() > 0);
					int minCount = spawn.get("minCount").getAsInt();
					int maxCount = spawn.get("maxCount").getAsInt();
					assertTrue(minCount > 0 && maxCount >= minCount, file + " spawn count range");
				}
			} else {
				allowed(attributes, file, "minecraft:audio/background_music", "minecraft:gameplay/increased_fire_burnout",
					"minecraft:gameplay/natural_mob_spawns", "minecraft:visual/sky_color", "minecraft:visual/water_fog_color", "minecraft:visual/water_fog_end_distance");
				var spawns = attributes.getAsJsonObject("minecraft:gameplay/natural_mob_spawns");
				allowed(spawns, file, "modifier", "argument"); required(spawns, file, "modifier", "argument");
				assertEquals("overlay", spawns.get("modifier").getAsString());
				var argument = spawns.getAsJsonObject("argument");
				allowed(argument, file, "spawns_by_category", "spawn_costs"); required(argument, file, "spawns_by_category", "spawn_costs");
				for (var pool : argument.getAsJsonObject("spawns_by_category").entrySet()) for (var value : pool.getValue().getAsJsonArray()) {
					var spawn = value.getAsJsonObject();
					allowed(spawn, file, "type", "weight", "count"); required(spawn, file, "type", "weight", "count");
					assertTrue(spawn.get("type").getAsString().matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"));
					assertTrue(spawn.get("weight").getAsInt() > 0);
					var count = spawn.get("count");
					if (count.isJsonPrimitive()) assertTrue(count.getAsInt() > 0);
					else {
						var range = count.getAsJsonObject();
						allowed(range, file, "type", "min_inclusive", "max_inclusive"); required(range, file, "type", "min_inclusive", "max_inclusive");
						assertEquals("minecraft:uniform", range.get("type").getAsString());
						assertTrue(range.get("min_inclusive").getAsInt() > 0 && range.get("max_inclusive").getAsInt() >= range.get("min_inclusive").getAsInt());
					}
				}
			}
		}
	}

	/** Every data file is one of the kinds checked above. */
	@Test
	void everyKindOfDataIsChecked() throws IOException {
		Set<String> checked = new java.util.HashSet<>(Set.of("wildercord/advancement", "wildercord/loot_table", "wildercord/enchantment", "wildercord/recipe",
			"wildercord/villager_trade", "wildercord/trade_set", "wildercord/worldgen/structure", "wildercord/worldgen/structure_set",
			"wildercord/worldgen/" + TestDataProfile.featureDirName(), "wildercord/worldgen/placed_feature", "wildercord/worldgen/biome",
			"wildercord/tags", "minecraft/tags", "wildercord/dimension", "wildercord/damage_type"));
		if (TestDataProfile.is26_2()) {
			checked.add("wildercord/brewing");
		}
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
		assertTrue(unknown.isEmpty(), where + ": Minecraft " + TestDataProfile.MC_VERSION + " ignores the keys " + unknown);
	}

	private static void required(JsonObject object, Object where, String... keys) {
		for (String key : keys) {
			assertTrue(object.has(key), where + ": missing \"" + key + "\"");
		}
	}

	/** A loot condition or modifier in 26.3+ schema: named by "type", never the older names. */
	private static void condition(JsonElement element, String where) {
		if (element.isJsonPrimitive()) {
			return;
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

	/** A list of loot conditions or functions: each named by {@code key} ("condition" or "function"), never "type". */
	private static void conditionList(JsonElement element, String where, String key) {
		assertTrue(element.isJsonArray(), where + ": 26.2 expects a JSON array");
		for (JsonElement entry : element.getAsJsonArray()) {
			if (entry.isJsonPrimitive()) continue;
			JsonObject object = entry.getAsJsonObject();
			required(object, where, key);
			assertTrue(!object.has("type"), where + ": unexpected \"type\" key in 26.2");
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
		assertTrue(ingredient.isJsonPrimitive(), where + ": an ingredient is an item id or #tag in " + TestDataProfile.MC_VERSION + ", not " + ingredient);
	}
}
