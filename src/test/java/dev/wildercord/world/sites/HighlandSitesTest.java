package dev.wildercord.world.sites;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.world.upgrade.UpgradeCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the Windscar Highland sites' data, books, loot runes, and upgrade catalog decisions. */
class HighlandSitesTest {
	static final List<String> SITES = List.of("highland_cairn_lodge", "highland_survey_ring");
	static final Map<String, Integer> BOOKS = Map.of("cairn_keeper", 4, "survey_ring", 2);
	static final Path DATA = Path.of("src/main/resources/data/wildercord");
	static final Path LANG = Path.of("src/main/resources/assets/wildercord/lang/en_us.json");
	static final Path RUNES = Path.of("src/main/java/dev/wildercord/spell/Runes.java");

	static JsonObject json(Path path) throws IOException {
		assertTrue(Files.exists(path), "missing " + path);
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}

	@Test
	void everyHighlandSiteIsWhole() throws IOException {
		JsonObject lang = json(LANG);
		String runes = Files.readString(RUNES);
		for (String id : SITES) {
			JsonObject structure = json(DATA.resolve("worldgen/structure/" + id + ".json"));
			assertEquals("wildercord:site", structure.get("type").getAsString(), id);
			assertEquals(id, structure.get("site").getAsString(), id);
			assertEquals("#wildercord:has_structure/" + id, structure.get("biomes").getAsString(), id);

			JsonObject placement = json(DATA.resolve("worldgen/structure_set/" + id + ".json")).getAsJsonObject("placement");
			int spacing = placement.get("spacing").getAsInt(), separation = placement.get("separation").getAsInt();
			assertTrue(spacing >= 28 && spacing <= 48 && separation >= 8 && separation < spacing, id);

			JsonArray biomes = json(DATA.resolve("tags/worldgen/biome/has_structure/" + id + ".json")).getAsJsonArray("values");
			assertTrue(biomes.toString().contains("\"wildercord:windscar_highlands\""), id);

			JsonObject loot = json(DATA.resolve("loot_table/chests/" + id + ".json"));
			int runeEntries = 0;
			for (JsonElement pool : loot.getAsJsonArray("pools")) {
				for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
					JsonObject e = entry.getAsJsonObject();
					if (e.get("name").getAsString().equals("wildercord:rune")) {
						var named = java.util.regex.Pattern.compile("\"wildercord:rune\":\"wildercord:([a-z_]+)\"").matcher(e.toString());
						assertTrue(named.find(), id + " has a rune entry with no rune named");
						assertTrue(runes.contains("(\"" + named.group(1) + "\""), id + " gives an unknown rune " + named.group(1));
						runeEntries++;
					}
				}
			}
			assertTrue(runeEntries >= 5, id + " has at least 5 runes");

			JsonObject adv = json(DATA.resolve("advancement/world/" + id + ".json"));
			assertTrue(adv.toString().contains("wildercord:" + id), id);
			assertTrue(lang.has("advancements.wildercord.world." + id + ".title"), id);
			assertTrue(lang.has("advancements.wildercord.world." + id + ".description"), id);
			assertTrue(lang.has("journal.wildercord.entry.place." + id), id);
			assertTrue(SiteFamilies.ALL.stream().anyMatch(f -> f.id().equals("wildercord:" + id) && f.decision() == UpgradeCatalog.Decision.WORLDGEN_ONLY), id);
		}
		JsonObject structures = json(DATA.resolve("tags/worldgen/structure/site.json"));
		for (String id : SITES) {
			assertTrue(structures.getAsJsonArray("values").toString().contains("\"wildercord:" + id + "\""), id);
		}
	}

	@Test
	void highlandBooksHaveTheirPages() throws IOException {
		JsonObject lang = json(LANG);
		for (Map.Entry<String, Integer> entry : BOOKS.entrySet()) {
			for (int i = 1; i <= entry.getValue(); i++) {
				String key = "book.wildercord." + entry.getKey() + "." + i;
				assertTrue(lang.has(key), key);
				String text = lang.get(key).getAsString();
				assertFalse(text.contains("—"), key);
				assertTrue(text.length() <= 256, key);
			}
		}
	}
}
