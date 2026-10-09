package dev.wildercord.world.sites;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Master halls' data (sites-masters pack): every site has its worldgen, loot, advancement and text, and a salt of its own. */
class MastersSitesTest {
	private static final Path DATA = Path.of("src/main/resources/data/wildercord");
	private static final Path LANG = Path.of("src/main/resources/assets/wildercord/lang/en_us.json");

	/** Each site and the two schools it may keep. */
	private static final Map<String, List<String>> SITES = Map.of(
		"master_forge_dojo", List.of("ember", "crimson"),
		"master_wind_gate", List.of("gale", "thunder"),
		"master_quarry_hall", List.of("stone", "iron"),
		"master_waterfall_shrine", List.of("rime", "tide"),
		"master_root_temple", List.of("verdant", "venom"),
		"master_sundial_court", List.of("dune", "hourglass"),
		"master_star_terrace", List.of("starlit", "dawn"),
		"master_resonance_chamber", List.of("hollow", "echo"));

	private static JsonObject read(Path p) throws IOException {
		assertTrue(Files.isRegularFile(p), "missing " + p);
		return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
	}

	@Test
	void everySiteHasItsData() throws IOException {
		JsonObject lang = read(LANG);
		for (var e : SITES.entrySet()) {
			String id = e.getKey();
			JsonObject structure = read(DATA.resolve("worldgen/structure/" + id + ".json"));
			assertEquals("wildercord:site", structure.get("type").getAsString(), id);
			assertEquals(id, structure.get("site").getAsString(), id);
			assertEquals("#wildercord:has_structure/" + id, structure.get("biomes").getAsString(), id);
			JsonObject set = read(DATA.resolve("worldgen/structure_set/" + id + ".json"));
			assertEquals("minecraft:random_spread", set.getAsJsonObject("placement").get("type").getAsString(), id);
			assertEquals("wildercord:" + id, set.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString(), id);
			assertTrue(read(DATA.resolve("tags/worldgen/biome/has_structure/" + id + ".json")).getAsJsonArray("values").size() > 0, id);
			read(DATA.resolve("loot_table/chests/" + id + ".json"));
			for (String school : e.getValue()) {
				String loot = read(DATA.resolve("loot_table/chests/" + id + "_" + school + ".json")).toString();
				assertTrue(loot.contains("wildercord:breathing_manual") && loot.contains("wildercord:manual_page"), id + " " + school + " manual");
				assertTrue(loot.contains("\"" + school + "\""), id + " " + school + " method");
				assertTrue(loot.contains("wildercord:technique_scroll") && loot.contains("wildercord:rune"), id + " " + school + " scroll and runes");
				assertTrue(lang.has("book.wildercord.master_site." + school), school + " lore");
			}
			String adv = read(DATA.resolve("advancement/sites/" + id + ".json")).toString();
			assertTrue(adv.contains("\"wildercord:" + id + "\""), id + " advancement");
			for (String key : List.of("advancements.wildercord.sites." + id + ".title", "advancements.wildercord.sites." + id + ".description",
					"book.wildercord." + id + ".trial")) assertTrue(lang.has(key), key);
		}
		assertTrue(lang.get("book.wildercord.master_site.challenge").getAsString().contains("%2$s"));
		assertTrue(read(DATA.resolve("tags/worldgen/structure/site.json")).toString().contains("wildercord:master_resonance_chamber"));
	}

	@Test
	void saltsAreUniqueAcrossEveryStructureSet() throws IOException {
		Map<Long, Path> seen = new HashMap<>();
		try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
			for (Path p : files.filter(f -> f.toString().replace('\\', '/').contains("/worldgen/structure_set/") && f.toString().endsWith(".json")).toList()) {
				var placement = read(p).getAsJsonObject("placement");
				if (placement == null || !placement.has("salt")) continue;
				Path before = seen.put(placement.get("salt").getAsLong(), p);
				assertTrue(before == null, "salt shared by " + before + " and " + p);
			}
		}
		for (int n = 1; n <= SITES.size(); n++) assertTrue(seen.containsKey(2026101000L + n), "salt " + (2026101000L + n));
	}

	@Test
	void sitesHaveWorldgenDecisions() {
		var ids = SiteFamilies.ALL.stream().map(f -> f.id()).toList();
		for (String id : SITES.keySet()) assertTrue(ids.contains("wildercord:" + id), id);
	}
}
