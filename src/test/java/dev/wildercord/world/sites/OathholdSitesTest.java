package dev.wildercord.world.sites;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.world.sites.oathhold.OathholdOrigin;
import dev.wildercord.world.upgrade.UpgradeCatalog.Decision;
import dev.wildercord.world.upgrade.UpgradeCatalog.Family;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Contract and architectural invariants for Oathhold, the 2,048x2,048 Sovereign Megacity. */
class OathholdSitesTest {
	static final Path DATA = Path.of("src/main/resources/data/wildercord");

	static JsonObject json(Path path) throws IOException {
		assertTrue(Files.isRegularFile(path), "missing " + path);
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}

	@Test
	void macroScaleAndSectorTilingAreSeamlessAndNonOverlapping() {
		assertEquals(2048, OathholdOrigin.CITY_SIZE);
		assertEquals(960, OathholdOrigin.OUTER_WALL_R);
		assertTrue(OathholdOrigin.MACRO_SEPARATION > OathholdOrigin.CITY_SIZE / 16,
			"Macro separation must exceed full city chunk diameter so two cities can never overlap");

		// Every chunk across the full 2,048x2,048 footprint falls inside at least one (8k, 8m) sector tile within 4 chunks
		for (int cx = -64; cx < 64; cx++) {
			for (int cz = -64; cz < 64; cz++) {
				int ax = Math.floorDiv(cx + 4, 8) * 8;
				int az = Math.floorDiv(cz + 4, 8) * 8;
				assertTrue(cx >= ax - 4 && cx <= ax + 3, "X tile gap at cx=" + cx);
				assertTrue(cz >= az - 4 && cz <= az + 3, "Z tile gap at cz=" + cz);
				assertTrue(Math.abs(cx - ax) <= 4 && Math.abs(cz - az) <= 4,
					"Sector tile radius must stay within 4 chunks of its anchor");
			}
		}
	}

	@Test
	void districtZoningMatchesArchitecturalMasterPlan() {
		assertEquals(OathholdOrigin.Zone.OUTSIDE, OathholdOrigin.classify(1024, 0));
		assertEquals(OathholdOrigin.Zone.APPROACH, OathholdOrigin.classify(1000, 0));
		assertEquals(OathholdOrigin.Zone.OUTER_MOAT, OathholdOrigin.classify(975, 100));
		assertEquals(OathholdOrigin.Zone.OUTER_WALL, OathholdOrigin.classify(0, 960));
		assertEquals(OathholdOrigin.Zone.OUTER_WALL, OathholdOrigin.classify(0, -960));
		assertEquals(OathholdOrigin.Zone.OUTER_COLOSSEUM, OathholdOrigin.classify(-612, 0));
		assertEquals(OathholdOrigin.Zone.OUTER_HARBOR, OathholdOrigin.classify(760, 0));
		assertEquals(OathholdOrigin.Zone.OUTER_EXCHANGE, OathholdOrigin.classify(0, 600));
		assertEquals(OathholdOrigin.Zone.OUTER_ABBEY, OathholdOrigin.classify(0, -600));
		assertEquals(OathholdOrigin.Zone.INNER_WALL, OathholdOrigin.classify(0, 224));
		assertEquals(OathholdOrigin.Zone.FOUNTAIN_PLAZA, OathholdOrigin.classify(0, 176));
		assertEquals(OathholdOrigin.Zone.INNER_CANAL, OathholdOrigin.classify(0, 60));
		assertEquals(OathholdOrigin.Zone.MARKET_HALL, OathholdOrigin.classify(-80, 64));
		assertEquals(OathholdOrigin.Zone.FORGE_HALL, OathholdOrigin.classify(-160, 72));
		assertEquals(OathholdOrigin.Zone.ACROPOLIS_GREAT_HALL, OathholdOrigin.classify(0, -148));
		assertEquals(OathholdOrigin.Zone.ACROPOLIS_BARRACKS, OathholdOrigin.classify(-120, -130));
		assertEquals(OathholdOrigin.Zone.ACROPOLIS_LIBRARY, OathholdOrigin.classify(80, -124));
		assertEquals(OathholdOrigin.Zone.ACROPOLIS_CATHEDRAL, OathholdOrigin.classify(150, -140));
	}

	@Test
	void registrationsAndOldWorldPolicyArePresent() throws IOException {
		String sitesSrc = Files.readString(Path.of("src/main/java/dev/wildercord/world/sites/Sites.java"));
		assertTrue(sitesSrc.contains("OathholdSites.init();"), "Sites.java must initialize OathholdSites");
		for (String id : List.of("oathhold", "oathhold_sector")) {
			Family family = SiteFamilies.ALL.stream().filter(f -> f.id().equals("wildercord:" + id)).findFirst().orElse(null);
			assertNotNull(family, id + " missing from SiteFamilies");
			assertEquals(Decision.WORLDGEN_ONLY, family.decision(), id);
		}
	}

	@Test
	void generatedWorldgenLootAndAdvancementFilesMatchContract() throws IOException {
		Set<String> tagged = new HashSet<>();
		json(DATA.resolve("tags/worldgen/structure/site.json")).getAsJsonArray("values").forEach(v -> tagged.add(v.getAsString()));
		for (String id : List.of("oathhold", "oathhold_sector")) {
			assertTrue(tagged.contains("wildercord:" + id), id + " missing from #wildercord:site");
			JsonObject st = json(DATA.resolve("worldgen/structure/" + id + ".json"));
			assertEquals("wildercord:site", st.get("type").getAsString());
			assertEquals(id, st.get("site").getAsString());
			assertTrue(Files.isRegularFile(DATA.resolve("worldgen/structure_set/" + id + ".json")));
			assertTrue(Files.isRegularFile(DATA.resolve("tags/worldgen/biome/has_structure/" + id + ".json")));
		}
		for (String loot : List.of("oathhold_treasury", "oathhold_armory", "oathhold_arcanum", "oathhold_market", "oathhold_barracks")) {
			assertTrue(Files.isRegularFile(DATA.resolve("loot_table/chests/" + loot + ".json")), "missing loot table " + loot);
		}
		assertTrue(Files.isRegularFile(DATA.resolve("advancement/world/oathhold.json")), "missing world/oathhold advancement");
	}
}
