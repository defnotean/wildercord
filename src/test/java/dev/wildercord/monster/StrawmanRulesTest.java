package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrawmanRulesTest {
	@Test
	void onlyALookItsWayWithNothingBetweenHoldsItStill() {
		double ahead = StrawmanRules.lookDot(0, 0, 1, 0, 0, 10);
		double behind = StrawmanRules.lookDot(0, 0, 1, 0, 0, -10);
		double edge = StrawmanRules.lookDot(0, 0, 1, 10, 0, 10);
		assertEquals(1.0, ahead, 1.0E-9);
		assertTrue(StrawmanRules.watches(ahead, 20, true, false));
		assertTrue(StrawmanRules.watches(edge, 20, true, false), "45 degrees off is still in view");
		assertFalse(StrawmanRules.watches(behind, 20, true, false), "back turned");
		assertFalse(StrawmanRules.watches(ahead, 20, false, false), "a wall between");
		assertFalse(StrawmanRules.watches(ahead, 20, true, true), "blinded");
		assertFalse(StrawmanRules.watches(ahead, StrawmanRules.WATCH_RANGE + 1, true, false), "too far off");
	}

	@Test
	void itStrikesOnlyUnwatchedAndCloseWithItsArmReady() {
		assertTrue(StrawmanRules.strikes(1.5, false, 100, 100));
		assertFalse(StrawmanRules.strikes(1.5, true, 100, 100), "someone is looking");
		assertFalse(StrawmanRules.strikes(4.0, false, 100, 100), "too far");
		assertFalse(StrawmanRules.strikes(1.5, false, 99, 100), "not ready");
	}

	@Test
	void aLongCloseStareLetsTheCrowsOut() {
		assertTrue(StrawmanRules.crows(StrawmanRules.STARE_TICKS, 6, 10, 0));
		assertFalse(StrawmanRules.crows(StrawmanRules.STARE_TICKS - 1, 6, 10, 0));
		assertFalse(StrawmanRules.crows(StrawmanRules.STARE_TICKS, StrawmanRules.STARE_RANGE + 1, 10, 0));
		assertFalse(StrawmanRules.crows(StrawmanRules.STARE_TICKS, 6, 10, 11));
	}

	@Test
	void itNeverStandsTheSameWayTwiceRunning() {
		for (int last = 0; last < StrawmanRules.STANCES; last++) {
			for (int roll = -20; roll < 20; roll++) {
				int next = StrawmanRules.nextStance(last, roll);
				assertNotEquals(last, next);
				assertTrue(next >= 0 && next < StrawmanRules.STANCES);
			}
		}
	}

	@Test
	void fireTakesItHarder() {
		assertEquals(StrawmanRules.FIRE_SCALE, StrawmanRules.damageScale(true));
		assertEquals(1.0F, StrawmanRules.damageScale(false));
	}
}
