package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HyenaRulesTest {
	@Test
	void itDartsInAtATurnedBackAndOnlyLaterAtAFace() {
		assertTrue(HyenaRules.darts(5, 100, 100, true, false), "back turned, ready");
		assertFalse(HyenaRules.darts(5, 100, 100, false, false), "facing it: it waits");
		assertTrue(HyenaRules.darts(5, 100 + HyenaRules.PATIENCE, 100, false, false), "it runs out of patience");
		assertFalse(HyenaRules.darts(5, 99, 100, true, false), "not ready");
		assertFalse(HyenaRules.darts(HyenaRules.DART_RANGE + 1, 100, 100, true, false), "too far");
		assertFalse(HyenaRules.darts(5, 500, 100, true, true), "one at a time");
	}

	@Test
	void aBackIsTurnedWhenTheyLookAway() {
		assertTrue(HyenaRules.backTurned(-1));
		assertTrue(HyenaRules.backTurned(0));
		assertFalse(HyenaRules.backTurned(0.9));
	}

	@Test
	void weakPreyMakesThePackBold() {
		assertTrue(HyenaRules.bold(10, 20));
		assertFalse(HyenaRules.bold(11, 20));
		assertFalse(HyenaRules.bold(0, 0));
		assertTrue(HyenaRules.orbit(true) < HyenaRules.orbit(false), "closer");
		assertTrue(HyenaRules.cooldown(true) < HyenaRules.cooldown(false), "quicker");
		assertEquals(HyenaRules.DART_COOLDOWN, HyenaRules.cooldown(false));
	}

	@Test
	void aBadlyHurtOneSlinksOffButNotAgainAtOnce() {
		assertTrue(HyenaRules.cowers(4, 16, 100, 100));
		assertFalse(HyenaRules.cowers(8, 16, 100, 100), "not hurt enough");
		assertFalse(HyenaRules.cowers(4, 16, 99, 100), "slunk off lately");
		assertFalse(HyenaRules.cowers(0, 0, 100, 0));
	}
}
