package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OctopusRulesTest {
	@Test
	void itReachesOnlyForSwimmersClose() {
		assertTrue(OctopusRules.grabs(3, 100, 100, false, true));
		assertFalse(OctopusRules.grabs(3, 100, 100, false, false), "not in the water");
		assertFalse(OctopusRules.grabs(OctopusRules.GRAB_REACH + 1, 100, 100, false, true), "too far");
		assertFalse(OctopusRules.grabs(3, 99, 100, false, true), "not ready");
		assertFalse(OctopusRules.grabs(3, 100, 100, true, true), "arms full");
	}

	@Test
	void itHoldsOnTillTimeHurtOrOutOfTheWater() {
		assertFalse(OctopusRules.letsGo(10, 0, true));
		assertTrue(OctopusRules.letsGo(OctopusRules.HOLD_TICKS, 0, true));
		assertTrue(OctopusRules.letsGo(10, OctopusRules.LETGO_DAMAGE, true));
		assertTrue(OctopusRules.letsGo(10, 0, false));
		assertTrue(OctopusRules.squeezes(OctopusRules.SQUEEZE_EVERY));
		assertFalse(OctopusRules.squeezes(0));
	}

	@Test
	void itDrawsTheAirOutButLeavesTheDrowningToTheGame() {
		assertEquals(300 - OctopusRules.AIR_DRAIN, OctopusRules.drains(300));
		assertEquals(OctopusRules.AIR_FLOOR, OctopusRules.drains(OctopusRules.AIR_FLOOR + 1));
		assertEquals(OctopusRules.AIR_FLOOR, OctopusRules.drains(OctopusRules.AIR_FLOOR));
		assertEquals(-20, OctopusRules.drains(-20), "the game's count is left alone");
	}

	@Test
	void itInksNoMoreOftenThanItCan() {
		assertTrue(OctopusRules.inks(100, 100));
		assertFalse(OctopusRules.inks(99, 100));
	}
}
