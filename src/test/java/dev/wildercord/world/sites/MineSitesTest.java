package dev.wildercord.world.sites;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** The mining sites' data: every site has its structure, set, biome tag, loot, advancement and text, and is reviewed for old worlds. */
class MineSitesTest {
	static final Path DATA = Path.of("src/main/resources/data/wildercord");
	static final Path LANG = Path.of("src/main/resources/assets/wildercord/lang/en_us.json");
	static final List<String> SITES = List.of("mine_hillside_mine", "mine_forge_hall", "mine_crystal_lab", "mine_collapsed_delve",
		"mine_basalt_foundry", "mine_deep_vault", "mine_prospector_camp", "mine_miners_rest");

	static JsonObject json(Path path) throws Exception {
		assertTrue(Files.exists(path), "missing " + path);
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}

	@Test
	void everySiteHasItsData() throws Exception {
		JsonObject lang = json(LANG);
		Set<String> tagged = new HashSet<>();
		json(DATA.resolve("tags/worldgen/structure/site.json")).getAsJsonArray("values").forEach(v -> tagged.add(v.getAsString()));
		for (String site : SITES) {
			JsonObject structure = json(DATA.resolve("worldgen/structure/" + site + ".json"));
			assertEquals("wildercord:site", structure.get("type").getAsString(), site);
			assertEquals(site, structure.get("site").getAsString());
			assertEquals("#wildercord:has_structure/" + site, structure.get("biomes").getAsString());
			assertFalse(json(DATA.resolve("tags/worldgen/biome/has_structure/" + site + ".json")).getAsJsonArray("values").isEmpty(), site);
			JsonObject placement = json(DATA.resolve("worldgen/structure_set/" + site + ".json")).getAsJsonObject("placement");
			int spacing = placement.get("spacing").getAsInt(), separation = placement.get("separation").getAsInt();
			assertTrue(spacing >= 28 && spacing <= 60 && separation >= 8 && separation <= 20 && separation < spacing, site);
			long salt = placement.get("salt").getAsLong();
			assertTrue(salt >= 2026101301L && salt <= 2026101308L, site + ": salt outside the pack's range");
			assertTrue(Files.exists(DATA.resolve("loot_table/chests/" + site + ".json")), site + ": no loot");
			JsonObject advancement = json(DATA.resolve("advancement/sites/" + site + ".json"));
			assertTrue(advancement.toString().contains("wildercord:" + site), site + ": advancement doesn't name the site");
			for (String key : List.of("advancements.wildercord.sites." + site + ".title", "advancements.wildercord.sites." + site + ".description",
					"journal.wildercord.entry.place." + site)) {
				assertTrue(lang.has(key), "no text for " + key);
				assertFalse(lang.get(key).getAsString().contains("—"), key + ": no em dashes");
			}
			assertTrue(tagged.contains("wildercord:" + site), site + ": not in the sites tag");
			assertTrue(SiteFamilies.ALL.stream().anyMatch(f -> f.id().equals("wildercord:" + site)), site + ": no old-world decision");
		}
	}

	@Test
	void everyStructureSetSaltIsUnique() throws Exception {
		Map<Long, String> seen = new HashMap<>();
		try (var files = Files.list(DATA.resolve("worldgen/structure_set"))) {
			for (Path file : files.toList()) {
				JsonObject placement = json(file).getAsJsonObject("placement");
				if (!placement.has("salt")) continue;
				String before = seen.put(placement.get("salt").getAsLong(), file.getFileName().toString());
				assertNull(before, file.getFileName() + " shares its salt with " + before);
			}
		}
	}

	@Test
	void lootPointsAtTheMiningRunesAndBreaths() throws Exception {
		String all = "";
		try (var files = Files.list(DATA.resolve("loot_table/chests"))) {
			for (Path file : files.filter(f -> f.getFileName().toString().startsWith("mine_")).toList()) all += Files.readString(file);
		}
		for (String rune : List.of("stairdelve", "toolmend", "hollowsense", "shoreup", "lavaseal", "deepway", "oretally", "caveward")) {
			assertTrue(all.contains("\"wildercord:" + rune + "\""), "no " + rune + " rune in the mining sites' loot");
		}
		for (String method : List.of("iron", "stone")) assertTrue(all.contains("\"wildercord:breathing_method\": \"" + method + "\""), method);
		assertFalse(all.contains("netherite_ingot"), "no netherite stacks");
	}
}
