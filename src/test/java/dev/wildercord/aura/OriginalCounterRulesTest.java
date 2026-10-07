package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OriginalCounterRulesTest {
	@Test void caughtHasItsOriginalInclusiveSixBlockLimit() {
		assertTrue(OriginalCounterRules.caught(36));
		assertFalse(OriginalCounterRules.caught(Math.nextUp(36.0)));
	}
	@Test void nearestKeepsFourBlockBodyWidthAndVerticalAllowance() {
		assertTrue(OriginalCounterRules.nearest(0, 2.2, 4.3, 0, 1, .6));
		assertFalse(OriginalCounterRules.nearest(0, Math.nextUp(2.2), 4, 0, 1, .6));
		assertFalse(OriginalCounterRules.nearest(0, 0, 4.31, 0, 1, .6));
		assertTrue(OriginalCounterRules.nearest(0, 0, 4.5, 0, 1, 1));
	}
	@Test void nearestUsesAcceptedFacingAndOriginalCone() {
		assertTrue(OriginalCounterRules.nearest(2, 0, 2, 0, 1, .6));
		assertFalse(OriginalCounterRules.nearest(2, 0, 0, 0, 1, .6));
		assertFalse(OriginalCounterRules.nearest(0, 0, -2, 0, 1, .6));
		assertTrue(OriginalCounterRules.nearest(0, 0, -2, 0, -1, .6));
	}
}
