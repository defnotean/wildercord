package dev.wildercord.world.sites;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.spell.RuneCatalog;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.sites.travel.TravelSites;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** The travel sites' data: structure, set, biome tag, loot, advancement and text per site, and loot that is for travelling. */
class TravelSitesTest {
	static final Path DATA = Path.of("src/main/resources/data/wildercord");
	static final Path LANG = Path.of("src/main/resources/assets/wildercord/lang/en_us.json");

	static JsonObject json(Path path) throws Exception {
		assertTrue(Files.exists(path), "missing " + path);
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}

	@Test
	void everySiteHasItsData() throws Exception {
		assertEquals(8, TravelSites.IDS.size());
		JsonObject lang = json(LANG);
		Set<String> tagged = new HashSet<>();
		json(DATA.resolve("tags/worldgen/structure/site.json")).getAsJsonArray("values").forEach(v -> tagged.add(v.getAsString()));
		for (String site : TravelSites.IDS) {
			assertTrue(site.startsWith("travel_"), site);
			JsonObject structure = json(DATA.resolve("worldgen/structure/" + site + ".json"));
			assertEquals("wildercord:site", structure.get("type").getAsString(), site);
			assertEquals(site, structure.get("site").getAsString());
			assertEquals("#wildercord:has_structure/" + site, structure.get("biomes").getAsString());
			assertTrue(json(DATA.resolve("tags/worldgen/biome/has_structure/" + site + ".json")).getAsJsonArray("values").size() >= 6, site + ": broad biomes");
			JsonObject placement = json(DATA.resolve("worldgen/structure_set/" + site + ".json")).getAsJsonObject("placement");
			int spacing = placement.get("spacing").getAsInt(), separation = placement.get("separation").getAsInt();
			assertTrue(spacing >= 28 && spacing <= 48 && separation >= 8 && separation < spacing, site);
			long salt = placement.get("salt").getAsLong();
			assertTrue(salt >= 2026101400L && salt <= 2026101407L, site + ": salt outside the pack's range");
			assertTrue(Files.exists(DATA.resolve("loot_table/chests/" + site + ".json")), site + ": no loot");
			JsonObject advancement = json(DATA.resolve("advancement/world/" + site + ".json"));
			assertTrue(advancement.toString().contains("\"wildercord:" + site + "\""), site + ": advancement doesn't name the site");
			for (String key : List.of("advancements.wildercord.world." + site + ".title", "advancements.wildercord.world." + site + ".description",
					"journal.wildercord.entry.place." + site)) {
				assertTrue(lang.has(key), "no text for " + key);
			}
			assertTrue(lang.get("journal.wildercord.entry.place." + site).getAsString().length() <= 96, site + ": journal entry too long");
			assertTrue(tagged.contains("wildercord:" + site), site + ": not in the sites tag");
			assertTrue(SiteFamilies.ALL.stream().anyMatch(f -> f.id().equals("wildercord:" + site)), site + ": no old-world decision");
		}
	}

	@Test
	void everyBookPageHasText() throws Exception {
		JsonObject lang = json(LANG);
		List<String> keys = new ArrayList<>();
		TravelSites.BOOKS.forEach((stem, pages) -> { for (int p = 1; p <= pages; p++) keys.add("book.wildercord." + stem + "." + p); });
		for (int v = 0; v < TravelSites.RIDDLES; v++) keys.add("book.wildercord.travel_riddle." + v);
		keys.add("book.wildercord.travel_riddle.end");
		keys.add("book.wildercord.travel_vows.vow");
		for (String key : keys) {
			assertTrue(lang.has(key), "no text for " + key);
			String text = lang.get(key).getAsString();
			assertFalse(text.contains("—"), key + ": no em dashes");
			assertTrue(text.length() <= 256, key + ": too long for a book page");
		}
		assertTrue(lang.get("book.wildercord.travel_catalog.1").getAsString().contains("Ctrl+B"), "the catalog book names its key");
		assertTrue(lang.get("book.wildercord.travel_vows.vow").getAsString().contains("%7$s"), "the vow page names both sides");
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
	void lootRunesAreForTravellingOrExploring() throws Exception {
		Pattern rune = Pattern.compile("\"wildercord:rune\": \"wildercord:(\\w+)\"");
		for (String site : TravelSites.IDS) {
			String loot = Files.readString(DATA.resolve("loot_table/chests/" + site + ".json"));
			Matcher m = rune.matcher(loot);
			int count = 0;
			while (m.find()) {
				RuneDef def = Runes.get("wildercord:" + m.group(1)).orElseThrow(() -> new AssertionError(site + ": no rune " + m.group(1)));
				Set<RuneCatalog.Use> uses = RuneCatalog.uses(def);
				assertTrue(uses.contains(RuneCatalog.Use.TRAVEL) || uses.contains(RuneCatalog.Use.EXPLORING), site + ": " + def.path() + " is for " + uses);
				count++;
			}
			assertTrue(count >= 5, site + ": too few runes");
		}
		String hut = Files.readString(DATA.resolve("loot_table/chests/travel_cartographer_hut.json"));
		assertTrue(hut.contains("minecraft:exploration_map") && hut.contains("#wildercord:travel_map_targets"), "the hut's map leads somewhere");
		assertTrue(Files.readString(DATA.resolve("loot_table/chests/travel_watchtower.json")).contains("minecraft:flint_and_steel"), "the tower gives a way to light its fire");
		List<String> targets = new ArrayList<>();
		json(DATA.resolve("tags/worldgen/structure/travel_map_targets.json")).getAsJsonArray("values").forEach(v -> targets.add(v.getAsString()));
		assertEquals(7, targets.size());
		assertFalse(targets.contains("wildercord:" + TravelSites.CARTOGRAPHER), "the map never points at a hut");
	}
}
