package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountBondRulesTest {
	@Test
	void levelsClimbWithPoints() {
		assertEquals(0, MountBondRules.level(0));
		assertEquals(0, MountBondRules.level(199));
		assertEquals(1, MountBondRules.level(200));
		assertEquals(3, MountBondRules.level(1400));
		assertEquals(MountBondRules.MAX_LEVEL, MountBondRules.level(1_000_000), "it tops out");
		assertEquals(200, MountBondRules.next(0));
		assertEquals(-1, MountBondRules.next(MountBondRules.MAX_LEVEL), "nothing past the top");
	}

	@Test
	void ridingEarnsItsDistanceWithinReason() {
		assertEquals(0, MountBondRules.earned(0.2), "standing about earns nothing");
		assertEquals(8, MountBondRules.earned(8.3));
		assertEquals((int) MountBondRules.MAX_PER_SECOND, MountBondRules.earned(500), "a teleport isn't miles");
		assertEquals(0, MountBondRules.earned(Double.NaN));
	}

	@Test
	void eachLevelIsQuickerAndHardier() {
		assertEquals(0, MountBondRules.speed(0), 1e-9);
		assertEquals(5 * MountBondRules.SPEED_PER_LEVEL, MountBondRules.speed(9), 1e-9, "no more than the top level gives");
		assertEquals(3 * MountBondRules.HEALTH_PER_LEVEL, MountBondRules.health(3), 1e-9);
	}

	@Test
	void dashAndStrikeComeWithTheBond() {
		assertFalse(MountBondRules.dashes(2));
		assertTrue(MountBondRules.dashReady(3, 1000, 1000 - MountBondRules.DASH_REST));
		assertFalse(MountBondRules.dashReady(3, 1000, 990), "it rests between dashes");
		double[] south = MountBondRules.dash(0);
		assertEquals(MountBondRules.DASH_SPEED, south[2], 1e-9);
		assertEquals(0, south[1], 1e-9);
		assertFalse(MountBondRules.strikeOnLanding(4, 10));
		assertFalse(MountBondRules.strikeOnLanding(5, 1), "a hop doesn't strike");
		assertTrue(MountBondRules.strikeOnLanding(5, MountBondRules.STRIKE_FALL));
	}
}
