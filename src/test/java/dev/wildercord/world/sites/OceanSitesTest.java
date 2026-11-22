package dev.wildercord.world.sites;

import dev.wildercord.ocean.OceanRules;
import dev.wildercord.world.sites.ocean.AbyssalCitadelPiece;
import dev.wildercord.world.sites.ocean.CoralDomeSanctuaryPiece;
import dev.wildercord.world.sites.ocean.HydrothermalForgePiece;
import dev.wildercord.world.sites.ocean.KelpSpireWatchtowerPiece;
import dev.wildercord.world.sites.ocean.OceanSites;
import dev.wildercord.world.sites.ocean.SunkenGalleonPiece;
import dev.wildercord.world.upgrade.UpgradeCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies Deep Ocean structure bounds match the approved 1:1 reference dimensions and SiteFamilies. */
class OceanSitesTest {
	static final List<String> SITES = List.of(
		OceanSites.CORAL_DOME_ID,
		OceanSites.GALLEON_ID,
		OceanSites.WATCHTOWER_ID,
		OceanSites.FORGE_ID,
		OceanSites.CITADEL_ID
	);

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
}
