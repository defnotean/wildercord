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

import static org.junit.jupiter.api.Assertions.*;

/** The farmstead pack's data: every site is placed, tagged, stocked, found and named, with its own salt. */
class FarmSitesTest {
	private static final Path DATA = Path.of("src/main/resources/data/wildercord");
	private static final Path LANG = Path.of("src/main/resources/assets/wildercord/lang/en_us.json");
	private static final List<String> IDS = List.of("farm_windmill", "farm_herbalist", "farm_apiary", "farm_orchard", "farm_shepherd",
		"farm_mushroom_ring", "farm_granary", "farm_scarecrow");

	private static JsonObject read(Path file) throws IOException {
		assertTrue(Files.isRegularFile(file), "missing " + file);
		return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
	}

	@Test
	void everySiteHasItsData() throws IOException {
		JsonObject lang = read(LANG);
		JsonObject tag = read(DATA.resolve("tags/worldgen/structure/site.json"));
		for (String id : IDS) {
			JsonObject structure = read(DATA.resolve("worldgen/structure/" + id + ".json"));
			assertEquals("wildercord:site", structure.get("type").getAsString(), id);
			assertEquals(id, structure.get("site").getAsString(), id);
			assertEquals("#wildercord:has_structure/" + id, structure.get("biomes").getAsString(), id);
			JsonObject placement = read(DATA.resolve("worldgen/structure_set/" + id + ".json")).getAsJsonObject("placement");
			int spacing = placement.get("spacing").getAsInt(), separation = placement.get("separation").getAsInt();
			assertTrue(spacing >= 28 && spacing <= 60 && separation >= 8 && separation <= 20 && separation < spacing, id + " spacing");
			assertFalse(read(DATA.resolve("tags/worldgen/biome/has_structure/" + id + ".json")).getAsJsonArray("values").isEmpty(), id + " biomes");
			String chest = Files.readString(DATA.resolve("loot_table/chests/" + id + ".json"));
			assertTrue(chest.contains("\"wildercord:rune\"") && !chest.contains("\"functions\""), id + " loot gives runes in 26.3 form");
			JsonObject advancement = read(DATA.resolve("advancement/world/" + id + ".json"));
			assertEquals("wildercord:world/farm_sites", advancement.get("parent").getAsString(), id);
			assertTrue(advancement.toString().contains("wildercord:" + id), id + " advancement finds the site");
			assertTrue(lang.has("advancements.wildercord.world." + id + ".title") && lang.has("advancements.wildercord.world." + id + ".description"), id + " lang");
			assertTrue(tag.toString().contains("\"wildercord:" + id + "\""), id + " in the site tag");
		}
		assertTrue(lang.has("advancements.wildercord.world.farm_sites.title"));
		for (String extra : List.of("farm_herbalist_loft", "farm_orchard_cache", "farm_granary_shrine"))
			assertTrue(Files.isRegularFile(DATA.resolve("loot_table/chests/" + extra + ".json")), extra);
		for (String key : List.of("farm_windmill", "farm_apiary", "farm_orchard", "farm_granary", "farm_scarecrow"))
			assertTrue(lang.has("sign.wildercord." + key + ".1"), key + " sign text");
	}

	@Test
	void saltsAreUniqueAcrossEveryStructureSet() throws IOException {
		Map<Integer, Path> seen = new HashMap<>();
		try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data"))) {
			for (Path file : files.filter(f -> f.toString().replace('\\', '/').contains("/worldgen/structure_set/") && f.toString().endsWith(".json")).toList()) {
				JsonObject placement = read(file).getAsJsonObject("placement");
				if (placement == null || !placement.has("salt")) continue;
				Path before = seen.put(placement.get("salt").getAsInt(), file);
				assertNull(before, "salt shared by " + before + " and " + file);
			}
		}
		for (int n = 1; n <= IDS.size(); n++) assertTrue(seen.containsKey(2026100900 + 200 + n), "farm salt " + n);
	}

	@Test
	void everySiteHasAnOldWorldDecision() {
		for (String id : IDS)
			assertTrue(SiteFamilies.ALL.stream().anyMatch(f -> f.id().equals("wildercord:" + id)), id + " in SiteFamilies");
	}
}
