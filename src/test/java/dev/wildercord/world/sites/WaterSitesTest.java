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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The water pack's data: every site is whole (structure, set, biomes, loot, advancement, text) and known to the upgrade catalog. */
class WaterSitesTest {
	static final List<String> SITES = List.of("water_stilt_smokehouse", "water_lighthouse", "water_watermill", "water_ice_camp",
		"water_pier_boathouse", "water_sunken_shrine", "water_netweaver_village", "water_tidepool_grotto");
	static final Path DATA = Path.of("src/main/resources/data/wildercord");
	static final Path LANG = Path.of("src/main/resources/assets/wildercord/lang/en_us.json");
	static final Path RUNES = Path.of("src/main/java/dev/wildercord/spell/Runes.java");

	static JsonObject json(Path path) throws IOException {
		assertTrue(Files.exists(path), "missing " + path);
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}

	@Test
	void everySiteIsWhole() throws IOException {
		JsonObject lang = json(LANG);
		String runes = Files.readString(RUNES);
		for (String id : SITES) {
			JsonObject structure = json(DATA.resolve("worldgen/structure/" + id + ".json"));
			assertEquals("wildercord:site", structure.get("type").getAsString(), id);
			assertEquals(id, structure.get("site").getAsString(), id);
			assertEquals("#wildercord:has_structure/" + id, structure.get("biomes").getAsString(), id);

			JsonObject placement = json(DATA.resolve("worldgen/structure_set/" + id + ".json")).getAsJsonObject("placement");
			int spacing = placement.get("spacing").getAsInt(), separation = placement.get("separation").getAsInt();
			assertTrue(spacing >= 28 && spacing <= 60 && separation >= 8 && separation <= 20 && separation < spacing, id);
			long salt = placement.get("salt").getAsLong();
			assertTrue(salt >= 2026101201L && salt <= 2026101208L, id + " salt " + salt);

			JsonArray biomes = json(DATA.resolve("tags/worldgen/biome/has_structure/" + id + ".json")).getAsJsonArray("values");
			assertFalse(biomes.isEmpty(), id);

			JsonObject loot = json(DATA.resolve("loot_table/chests/" + id + ".json"));
			int runeEntries = 0;
			for (JsonElement pool : loot.getAsJsonArray("pools")) {
				for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
					JsonObject e = entry.getAsJsonObject();
					if (e.get("name").getAsString().equals("wildercord:rune")) {
						var named = java.util.regex.Pattern.compile("\"wildercord:rune\":\"wildercord:([a-z_]+)\"").matcher(e.toString());
						assertTrue(named.find(), id + " has a rune entry with no rune named");
						String rune = named.group(1);
						assertTrue(runes.contains("(\"" + rune + "\""), id + " gives an unknown rune " + rune);
						runeEntries++;
					}
				}
			}
			assertTrue(runeEntries >= 3, id + " has too few runes");
			// An exploration map's destination is a structure tag; a bare id stops the whole data pack from loading.
			var destination = java.util.regex.Pattern.compile("\"destination\":\"([^\"]*)\"").matcher(loot.toString());
			while (destination.find()) {
				assertTrue(destination.group(1).startsWith("#"), id + " map destination must be a tag: " + destination.group(1));
			}

			JsonObject adv = json(DATA.resolve("advancement/world/" + id + ".json"));
			assertTrue(adv.toString().contains("wildercord:" + id), id + " advancement is not for its structure");
			assertTrue(lang.has("advancements.wildercord.world." + id + ".title"), id);
			assertTrue(lang.has("advancements.wildercord.world." + id + ".description"), id);
		}
		JsonObject structures = json(DATA.resolve("tags/worldgen/structure/site.json"));
		for (String id : SITES) {
			assertTrue(structures.getAsJsonArray("values").toString().contains("\"wildercord:" + id + "\""), id);
		}
	}

	@Test
	void booksHaveTheirPages() throws IOException {
		JsonObject lang = json(LANG);
		Map<String, Integer> books = Map.of("water_lighthouse", 3, "water_sunken_shrine", 2, "water_tidepool_grotto", 2);
		books.forEach((id, pages) -> {
			for (int i = 1; i <= pages; i++) {
				String key = "book.wildercord." + id + "." + i;
				assertTrue(lang.has(key), key);
				assertFalse(lang.get(key).getAsString().contains("—"), key);
			}
		});
	}

	@Test
	void saltsAreUniqueAcrossEveryStructureSet() throws IOException {
		Map<Long, Path> seen = new HashMap<>();
		try (Stream<Path> files = Files.walk(DATA.getParent())) {
			for (Path file : files.filter(p -> p.toString().replace('\\', '/').contains("/worldgen/structure_set/") && p.toString().endsWith(".json")).toList()) {
				JsonObject placement = json(file).getAsJsonObject("placement");
				if (placement == null || !placement.has("salt")) {
					continue;
				}
				Path before = seen.put(placement.get("salt").getAsLong(), file);
				assertNull(before, "salt shared by " + before + " and " + file);
			}
		}
	}

	@Test
	void everySiteHasItsDecision() {
		for (String id : SITES) {
			assertTrue(SiteFamilies.ALL.stream().anyMatch(f -> f.id().equals("wildercord:" + id) && f.decision() == UpgradeCatalog.Decision.WORLDGEN_ONLY), id);
		}
	}
}
