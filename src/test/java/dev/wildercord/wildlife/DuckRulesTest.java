package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuckRulesTest {
	@Test
	void theDucklingsKeepTheirPlaceInLine() {
		int[] brood = {4, 9, 12};
		assertEquals(0, DuckRules.place(4, brood));
		assertEquals(2, DuckRules.place(12, brood));
		assertEquals(3, DuckRules.place(7, brood), "a stranger falls in at the back");
	}

	@Test
	void theyCloseUpAndNoFurther() {
		assertEquals(0, DuckRules.pace(DuckRules.LINE_GAP));
		assertEquals(1.0, DuckRules.pace(2));
		assertTrue(DuckRules.pace(DuckRules.LINE_HURRY + 1) > DuckRules.pace(2));
	}

	@Test
	void onlyADucklingImprintsAndOnlyOnce() {
		assertTrue(DuckRules.imprints(true, true, false));
		assertFalse(DuckRules.imprints(false, true, false), "a grown duck doesn't");
		assertFalse(DuckRules.imprints(true, false, false), "not on bread");
		assertFalse(DuckRules.imprints(true, true, true), "it already has its someone");
	}

	@Test
	void itDabblesAfloatNowAndThen() {
		assertTrue(DuckRules.dabbles(true, true, true, -1));
		assertFalse(DuckRules.dabbles(true, false, true, -1), "on land");
		assertFalse(DuckRules.dabbles(false, true, true, -1), "ducklings just paddle");
		assertFalse(DuckRules.dabbles(true, true, true, DuckRules.DABBLE_COOLDOWN - 1));
		assertTrue(DuckRules.dabbles(true, true, true, DuckRules.DABBLE_COOLDOWN));
	}

	@Test
	void itShakesOffOnLandAndFlapsInTheRain() {
		assertTrue(DuckRules.shakes(true, false, true));
		assertFalse(DuckRules.shakes(false, false, true));
		assertFalse(DuckRules.shakes(true, true, true));
		assertTrue(DuckRules.rejoices(true, true, 0));
		assertFalse(DuckRules.rejoices(false, true, 0));
		assertFalse(DuckRules.rejoices(true, true, 0.5));
	}
}
