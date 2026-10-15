package dev.wildercord.world.hollowbell;

import org.junit.jupiter.api.Test;

import static dev.wildercord.world.hollowbell.HollowBellRules.*;
import static org.junit.jupiter.api.Assertions.*;

class HollowBellRulesTest {
	@Test
	void moveOrderReordersInSecondPhaseAndKeepsWindowsReadable() {
		assertEquals(TOLL, nextMove(0, false));
		assertEquals(CLAPPER, nextMove(1, false));
		assertEquals(CHORD, nextMove(2, false));
		assertEquals(CHORD, nextMove(0, true));
		assertEquals(TOLL, nextMove(1, true));
		assertEquals(CLAPPER, nextMove(2, true));
		for (int move : new int[]{TOLL, CLAPPER, CHORD}) {
			assertTrue(tell(move) >= 32, "every tell lasts at least 1.6s: " + move);
			assertTrue(recovery(move) >= 24, "every move leaves a recovery window: " + move);
			assertTrue(damage(move) >= 5 && damage(move) <= 7);
			assertNotEquals("idle", key(move));
		}
		assertTrue(second(90, 180));
		assertFalse(second(91, 180));
	}

	@Test
	void hitShapesMatchTheirMarkedSafeZones() {
		assertTrue(hits(TOLL, 0, 0, 5.0, 0, 0, true), "inside annular toll ring (3.0..6.5)");
		assertFalse(hits(TOLL, 0, 0, 2.0, 0, 0, true), "quiet eye directly beneath the bell (< 3.0) is safe");
		assertFalse(hits(TOLL, 0, 0, 7.0, 0, 0, true), "beyond outer toll ring (> 6.5) is safe");

		assertTrue(hits(CLAPPER, 4.0, 0, -3.0, 3.0, -3.0, true), "inside locked clapper circle");
		assertFalse(hits(CLAPPER, 6.0, 0, -3.0, 3.0, -3.0, true), "stepping outside locked clapper circle avoids impact");

		assertFalse(hits(CHORD, 6.0, 0, 1.5, 0, 0, true), "3.5-block east-west damped lane is safe");
		assertTrue(hits(CHORD, 6.0, 0, 2.5, 0, 0, true), "outside east-west damped lane is struck");
		assertFalse(hits(CHORD, -1.5, 0, 6.0, 0, 0, false), "3.5-block north-south damped lane is safe");
		assertTrue(hits(CHORD, -2.5, 0, 6.0, 0, 0, false), "outside north-south damped lane is struck");
		assertFalse(hits(CHORD, 20.0, 0, 20.0, 0, 0, true), "outside arena bounds is never hit");
	}
}
