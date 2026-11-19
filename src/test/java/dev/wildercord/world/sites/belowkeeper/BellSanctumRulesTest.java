package dev.wildercord.world.sites.belowkeeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The ruined belfry is built only where the ground is within reach of the vault roof. */
class BellSanctumRulesTest {
	@Test void plainsSanctumGetsTheBelfry() {
		// ground local 64 - 24 + 2 = 42, so the piece carries the court plus the belfry above it
		assertEquals(42 + BellSanctumRules.ABOVE, BellSanctumRules.towerHeight(64, 24));
	}

	@Test void shaftNeedsAtLeastThreeBlocksOfRock() {
		assertEquals(14 + BellSanctumRules.ABOVE, BellSanctumRules.towerHeight(36, 24)); // ground local 14
		assertEquals(BellSanctumRules.ROOM, BellSanctumRules.towerHeight(35, 24));       // ground local 13: too near
	}

	@Test void tooDeepOrTooHighStaysSealed() {
		assertEquals(52 + BellSanctumRules.ABOVE, BellSanctumRules.towerHeight(74, 24)); // ground local 52
		assertEquals(BellSanctumRules.ROOM, BellSanctumRules.towerHeight(75, 24));       // ground local 53: too far
		assertEquals(BellSanctumRules.ROOM, BellSanctumRules.towerHeight(70, -24));      // a deep vault never reaches
	}

	@Test void theTowerAlwaysClearsTheGroundByItsFullHeight() {
		for (int surface = 30; surface <= 80; surface++) {
			int height = BellSanctumRules.towerHeight(surface, 24);
			if (height == BellSanctumRules.ROOM) continue;
			int ground = surface - 24 + 2;
			assertTrue(height >= ground + BellSanctumRules.ABOVE, "belfry must fit above ground " + surface);
			assertTrue(ground > 11, "the shaft must start above the vault roof at local 11");
		}
	}
}
