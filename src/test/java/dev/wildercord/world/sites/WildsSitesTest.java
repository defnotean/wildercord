package dev.wildercord.world.sites;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.world.upgrade.UpgradeCatalog.Decision;
import dev.wildercord.world.upgrade.UpgradeCatalog.Family;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The wild sites' data: each has its structure, set, biomes, loot, advancement and text, and a worldgen-only decision. */
class WildsSitesTest {
	static final Path DATA = Path.of("src/main/resources/data/wildercord");
	static final List<String> IDS = List.of("wilds_venom_ziggurat", "wilds_dune_temple", "wilds_rime_monastery", "wilds_iron_gatehouse",
		"wilds_dawn_pavilion", "wilds_echo_post", "wilds_ember_outpost", "wilds_void_lantern");

	static JsonObject json(Path path) throws IOException {
		assertTrue(Files.isRegularFile(path), "missing " + path);
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}

	@Test void theCodeNamesTheSameSites() throws IOException {
		String src = Files.readString(Path.of("src/main/java/dev/wildercord/world/sites/wilds/WildsSites.java"));
		for (String id : IDS) {
			assertTrue(src.contains("Sites.register(\"" + id + "\""), id + " is not registered");
			assertTrue(src.contains("Sites.piece(\"" + id + "\""), id + " has no piece type");
		}
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/world/sites/Sites.java")).contains("WildsSites.init();"));
	}

	@Test void everySiteHasItsData() throws IOException {
		JsonObject lang = json(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"));
		Set<String> tagged = new HashSet<>();
		json(DATA.resolve("tags/worldgen/structure/site.json")).getAsJsonArray("values").forEach(v -> tagged.add(v.getAsString()));
		for (String id : IDS) {
			JsonObject structure = json(DATA.resolve("worldgen/structure/" + id + ".json"));
			assertEquals("wildercord:site", structure.get("type").getAsString(), id);
			assertEquals(id, structure.get("site").getAsString(), id);
			assertEquals("#wildercord:has_structure/" + id, structure.get("biomes").getAsString(), id);
			assertTrue(tagged.contains("wildercord:" + id), id + " is missing from the site tag");
			JsonObject set = json(DATA.resolve("worldgen/structure_set/" + id + ".json"));
			JsonObject placement = set.getAsJsonObject("placement");
			int spacing = placement.get("spacing").getAsInt(), separation = placement.get("separation").getAsInt();
			assertTrue(spacing >= 28 && spacing <= 60 && separation >= 8 && separation <= 20 && separation < spacing, id + " spacing");
			assertEquals("wildercord:" + id, set.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());
			assertFalse(json(DATA.resolve("tags/worldgen/biome/has_structure/" + id + ".json")).getAsJsonArray("values").isEmpty(), id);
			String vault = Files.readString(DATA.resolve("loot_table/chests/" + id + ".json"));
			assertTrue(vault.contains("wildercord:manual_page") && vault.contains("wildercord:breathing_method"), id + " vault has no manual page");
			assertTrue(vault.contains("\"wildercord:rune\""), id + " vault has no runes");
			assertTrue(Files.readString(DATA.resolve("loot_table/chests/" + id + "_hall.json")).contains("\"wildercord:rune\""), id + " hall");
			String advancement = Files.readString(DATA.resolve("advancement/world/" + id + ".json"));
			assertTrue(advancement.contains("\"structures\": \"wildercord:" + id + "\""), id + " advancement");
			assertTrue(lang.has("advancements.wildercord.world." + id + ".title") && lang.has("advancements.wildercord.world." + id + ".description"), id);
		}
		assertTrue(Files.readString(DATA.resolve("loot_table/chests/wilds_venom_ziggurat_darts.json")).contains("minecraft:poison"));
		assertTrue(Files.readString(DATA.resolve("loot_table/chests/wilds_dune_temple_sand.json")).contains("minecraft:archaeology"));
	}

	@Test void theEchoPostKeepsAwayFromAncientCities() throws IOException {
		JsonObject zone = json(DATA.resolve("worldgen/structure_set/wilds_echo_post.json")).getAsJsonObject("placement").getAsJsonObject("exclusion_zone");
		assertNotNull(zone, "no exclusion zone");
		assertEquals("minecraft:ancient_cities", zone.get("other_set").getAsString());
		assertTrue(zone.get("chunk_count").getAsInt() >= 6);
	}

	@Test void saltsAreUniqueAcrossEveryStructureSet() throws IOException {
		Map<Long, String> seen = new HashMap<>();
		try (Stream<Path> files = Files.list(DATA.resolve("worldgen/structure_set"))) {
			for (Path f : files.toList()) {
				JsonObject placement = json(f).getAsJsonObject("placement");
				if (placement == null || !placement.has("salt")) continue;
				String before = seen.put(placement.get("salt").getAsLong(), f.getFileName().toString());
				assertNull(before, "salt shared by " + before + " and " + f.getFileName());
			}
		}
		for (int n = 0; n < IDS.size(); n++)
			assertEquals(2026100900L + 600 + n + 1, json(DATA.resolve("worldgen/structure_set/" + IDS.get(n) + ".json"))
				.getAsJsonObject("placement").get("salt").getAsLong(), IDS.get(n));
	}

	@Test void everySiteIsWorldgenOnly() {
		for (String id : IDS) {
			Family family = SiteFamilies.ALL.stream().filter(f -> f.id().equals("wildercord:" + id)).findFirst().orElse(null);
			assertNotNull(family, id + " has no decision in SiteFamilies");
			assertEquals(Decision.WORLDGEN_ONLY, family.decision(), id);
		}
	}
}
