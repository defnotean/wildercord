package dev.wildercord.world.sites;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.ocean.OceanRules;
import dev.wildercord.world.sites.ocean.AbyssalCitadelPiece;
import dev.wildercord.world.sites.ocean.CoralDomeSanctuaryPiece;
import dev.wildercord.world.sites.ocean.HydrothermalForgePiece;
import dev.wildercord.world.sites.ocean.KelpSpireWatchtowerPiece;
import dev.wildercord.world.sites.ocean.OceanSites;
import dev.wildercord.world.sites.ocean.SunkenGalleonPiece;
import dev.wildercord.world.upgrade.UpgradeCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies Deep Ocean structure bounds, SiteFamilies entries, worldgen JSONs, loot, advancements, and lang keys. */
class OceanSitesTest {
	static final List<String> SITES = List.of(
		OceanSites.CORAL_DOME_ID,
		OceanSites.GALLEON_ID,
		OceanSites.WATCHTOWER_ID,
		OceanSites.FORGE_ID,
		OceanSites.CITADEL_ID
	);

	private static Path res(String rel) {
		Path p = Path.of("src/main/resources", rel);
		if (!Files.exists(p)) {
			p = Path.of("../src/main/resources", rel);
		}
		return p;
	}

	@Test
	void boundsMatchOneToOneBlueprints() {
		assertEquals(OceanRules.SANCTUARY_SIZE_XZ, CoralDomeSanctuaryPiece.WIDTH);
		assertEquals(OceanRules.SANCTUARY_HEIGHT, CoralDomeSanctuaryPiece.HEIGHT);
		assertEquals(OceanRules.GALLEON_LENGTH, SunkenGalleonPiece.WIDTH);
		assertEquals(OceanRules.GALLEON_HEIGHT, SunkenGalleonPiece.HEIGHT);
		assertEquals(OceanRules.GALLEON_WIDTH, SunkenGalleonPiece.DEPTH);
		assertEquals(OceanRules.WATCHTOWER_SIZE_XZ, KelpSpireWatchtowerPiece.WIDTH);
		assertEquals(OceanRules.FORGE_SIZE_XZ, HydrothermalForgePiece.WIDTH);
		assertEquals(OceanRules.FORGE_HEIGHT, HydrothermalForgePiece.HEIGHT);
		assertEquals(OceanRules.CITADEL_SIZE_XZ, AbyssalCitadelPiece.WIDTH);
		assertEquals(OceanRules.CITADEL_HEIGHT, AbyssalCitadelPiece.HEIGHT);
	}

	@Test
	void everyOceanSiteHasUpgradeCatalogDecision() {
		for (String id : SITES) {
			assertTrue(
				SiteFamilies.ALL.stream().anyMatch(f -> f.id().equals("wildercord:" + id) && f.decision() == UpgradeCatalog.Decision.WORLDGEN_ONLY),
				"Missing SiteFamilies decision for wildercord:" + id
			);
		}
	}

	@Test
	void worldgenLootAdvancementsAndLangPresent() throws IOException {
		JsonObject lang = JsonParser.parseString(Files.readString(res("assets/wildercord/lang/en_us.json"))).getAsJsonObject();

		for (String id : SITES) {
			assertTrue(Files.exists(res("data/wildercord/worldgen/structure/" + id + ".json")), "Missing structure JSON: " + id);
			assertTrue(Files.exists(res("data/wildercord/worldgen/structure_set/" + id + ".json")), "Missing structure_set JSON: " + id);
			assertTrue(Files.exists(res("data/wildercord/tags/worldgen/biome/has_structure/" + id + ".json")), "Missing biome tag: " + id);
			assertTrue(Files.exists(res("data/wildercord/loot_table/chests/" + id + ".json")), "Missing chest loot: " + id);
			assertTrue(Files.exists(res("data/wildercord/advancement/world/" + id + ".json")), "Missing world advancement: " + id);
			assertTrue(lang.has("journal.wildercord.entry.place." + id), "Missing journal entry lang: " + id);
			assertTrue(lang.has("advancements.wildercord.world." + id + ".title"), "Missing world adv title: " + id);
		}

		for (String adv : List.of("deep_diver", "trench_forged", "sanctuary_trader", "conch_shelter", "sovereign_slayer")) {
			assertTrue(Files.exists(res("data/wildercord/advancement/ocean/" + adv + ".json")), "Missing ocean adv: " + adv);
			assertTrue(lang.has("advancements.wildercord.ocean." + adv + ".title"), "Missing ocean adv lang: " + adv);
		}

		for (String mob : List.of(
			"barnacle_hulk_dredger", "siren_veil_jelly", "coral_spine_mako", "dread_nautilus_sentinel",
			"thalassor", "singing_moon_whale", "pearl_pup", "lumi_seahorse"
		)) {
			assertTrue(Files.exists(res("data/wildercord/loot_table/entities/" + mob + ".json")), "Missing entity loot: " + mob);
		}

		for (String rec : List.of("kelp_wrapped_sashimi", "sea_salt_ramen", "siren_pearl_lantern", "trench_anvil")) {
			assertTrue(Files.exists(res("data/wildercord/recipe/" + rec + ".json")), "Missing recipe: " + rec);
		}

		String bossesTag = Files.readString(res("data/wildercord/tags/entity_type/bosses.json"));
		assertTrue(bossesTag.contains("wildercord:thalassor"), "Thalassor missing from wildercord:bosses tag");
	}
}
