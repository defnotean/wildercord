package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WolverineRulesTest {
	@Test
	void itSnatchesOnlyWithEmptyJawsAndFoodToTake() {
		assertTrue(WolverineRules.snatches(2, 100, 100, false, true, false));
		assertFalse(WolverineRules.snatches(2, 99, 100, false, true, false), "not ready");
		assertFalse(WolverineRules.snatches(2, 100, 100, true, true, false), "jaws full");
		assertFalse(WolverineRules.snatches(2, 100, 100, false, false, false), "nothing to take");
		assertFalse(WolverineRules.snatches(2, 100, 100, false, true, true), "cornered, it fights");
		assertFalse(WolverineRules.snatches(WolverineRules.SNATCH_REACH + 1, 100, 100, false, true, false), "too far");
	}

	@Test
	void itTakesAFewAtMost() {
		assertEquals(1, WolverineRules.take(1));
		assertEquals(WolverineRules.SNATCH_MAX, WolverineRules.take(64));
		assertEquals(0, WolverineRules.take(0));
	}

	@Test
	void itEatsOnceFarOffOrAfterAWhile() {
		assertFalse(WolverineRules.eats(5, 10));
		assertTrue(WolverineRules.eats(WolverineRules.FLEE_DIST, 10));
		assertTrue(WolverineRules.eats(5, WolverineRules.EAT_AFTER));
	}

	@Test
	void corneredItFightsQuicker() {
		assertFalse(WolverineRules.furious(20, 28));
		assertTrue(WolverineRules.furious(10, 28));
		assertTrue(WolverineRules.biteCooldown(true) < WolverineRules.biteCooldown(false));
		assertTrue(WolverineRules.dropsWhenHit(0.1F));
		assertFalse(WolverineRules.dropsWhenHit(0.9F));
		assertEquals(8.0F, WolverineRules.heal(4, 4));
	}
}
