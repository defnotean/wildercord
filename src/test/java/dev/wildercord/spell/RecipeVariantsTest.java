package dev.wildercord.spell;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Blank Rune and all shaped/shapeless crafting recipes accept deepslate and block variations
 * through item tags rather than locking onto a single stone, plank, wool, or glass variant.
 */
class RecipeVariantsTest {
	private static final Path DATA = Path.of("src/main/resources/data/wildercord");
	private static final Path LANG = Path.of("src/main/resources/assets/wildercord/lang/en_us.json");

	private static final Set<String> LOCKED_SINGLE_VARIANTS = Set.of(
		"minecraft:cobblestone", "minecraft:cobbled_deepslate", "minecraft:stone", "minecraft:smooth_stone",
		"minecraft:tuff", "minecraft:polished_deepslate", "minecraft:deepslate_tiles", "minecraft:deepslate_bricks",
		"minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:mossy_cobblestone",
		"minecraft:cobblestone_slab", "minecraft:stone_slab", "minecraft:smooth_stone_slab",
		"minecraft:cobblestone_stairs", "minecraft:stone_stairs", "minecraft:cobblestone_wall",
		"minecraft:sandstone", "minecraft:glass", "minecraft:magenta_stained_glass", "minecraft:glass_pane",
		"minecraft:oak_planks", "minecraft:oak_log", "minecraft:spruce_log",
		"minecraft:oak_sapling", "minecraft:birch_sapling", "minecraft:oak_leaves",
		"minecraft:oak_slab", "minecraft:oak_fence", "minecraft:oak_fence_gate", "minecraft:oak_door",
		"minecraft:oak_sign", "minecraft:oak_boat", "minecraft:white_wool",
		"minecraft:white_carpet", "minecraft:black_carpet", "minecraft:white_bed", "minecraft:red_bed",
		"minecraft:white_banner", "minecraft:white_concrete_powder", "minecraft:stone_button",
		"minecraft:sand", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt"
	);

	private static JsonObject readJson(Path p) throws IOException {
		return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
	}

	@Test
	void blankRuneAcceptsCobblestoneDeepslateAndStoneVariants() throws IOException {
		JsonObject recipe = readJson(DATA.resolve("recipe/blank_rune.json"));
		assertEquals("#wildercord:cobblestone_variants", recipe.getAsJsonObject("key").get("C").getAsString());

		JsonObject tag = readJson(DATA.resolve("tags/item/cobblestone_variants.json"));
		Set<String> values = new HashSet<>();
		for (JsonElement e : tag.getAsJsonArray("values")) {
			values.add(e.getAsString());
		}
		for (String required : Set.of(
			"#minecraft:stone_crafting_materials",
			"minecraft:deepslate",
			"minecraft:polished_deepslate",
			"minecraft:deepslate_bricks",
			"minecraft:deepslate_tiles",
			"minecraft:stone",
			"minecraft:mossy_cobblestone"
		)) {
			assertTrue(values.contains(required), "cobblestone_variants tag missing " + required);
		}
	}

	@Test
	void craftingRecipesUseVariantTagsAndCustomTagsHaveDefinitionsAndLang() throws IOException {
		JsonObject lang = readJson(LANG);
		Set<String> referencedCustomTags = new HashSet<>();
		try (Stream<Path> files = Files.list(DATA.resolve("recipe"))) {
			for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
				JsonObject recipe = readJson(path);
				String type = recipe.get("type").getAsString();
				if (!type.equals("minecraft:crafting_shaped") && !type.equals("minecraft:crafting_shapeless")) {
					continue;
				}
				Set<String> items = new HashSet<>();
				if (recipe.has("ingredients")) {
					for (JsonElement e : recipe.getAsJsonArray("ingredients")) {
						if (e.isJsonPrimitive()) {
							items.add(e.getAsString());
						}
					}
				}
				if (recipe.has("key")) {
					for (var entry : recipe.getAsJsonObject("key").entrySet()) {
						if (entry.getValue().isJsonPrimitive()) {
							items.add(entry.getValue().getAsString());
						}
					}
				}
				for (String item : items) {
					assertFalse(LOCKED_SINGLE_VARIANTS.contains(item),
						path.getFileName() + " still locks onto single variant " + item);
					if (item.startsWith("#wildercord:")) {
						referencedCustomTags.add(item.substring("#wildercord:".length()));
					}
				}
			}
		}
		assertFalse(referencedCustomTags.isEmpty(), "Expected custom variant tags to be referenced by recipes");
		for (String tag : referencedCustomTags) {
			Path tagFile = DATA.resolve("tags/item/" + tag + ".json");
			assertTrue(Files.isRegularFile(tagFile), "Missing custom item tag file " + tagFile);
			assertFalse(readJson(tagFile).getAsJsonArray("values").isEmpty(), "Empty custom item tag " + tag);
			assertTrue(lang.has("tag.item.wildercord." + tag), "Missing translation key tag.item.wildercord." + tag);
		}
	}
}
