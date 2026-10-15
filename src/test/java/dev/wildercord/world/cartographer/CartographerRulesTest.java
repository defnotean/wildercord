package dev.wildercord.world.cartographer;

import org.junit.jupiter.api.Test;

import static dev.wildercord.world.cartographer.CartographerRules.*;
import static org.junit.jupiter.api.Assertions.*;

class CartographerRulesTest {
	@Test
	void moveOrderReordersInSecondPhaseAndKeepsWindowsReadable() {
		assertEquals(SWEEP, nextMove(0, false));
		assertEquals(PLUMB, nextMove(1, false));
		assertEquals(GRID, nextMove(2, false));
		assertEquals(GRID, nextMove(0, true));
		assertEquals(SWEEP, nextMove(1, true));
		assertEquals(PLUMB, nextMove(2, true));
		for (int move : new int[]{SWEEP, PLUMB, GRID}) {
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
		assertTrue(hits(SWEEP, 0, 0, 5.0, 0, 0, 0, true), "inside forward transit fan");
		assertFalse(hits(SWEEP, 0, 0, -3.0, 0, 0, 0, true), "behind locked transit sweep is safe");
		assertFalse(hits(SWEEP, 0, 0, 7.0, 0, 0, 0, true), "beyond transit reach is safe");

		assertTrue(hits(PLUMB, 4.0, 0, -3.0, 0, 3.0, -3.0, true), "inside locked plumb circle");
		assertFalse(hits(PLUMB, 6.0, 0, -3.0, 0, 3.0, -3.0, true), "stepping outside locked plumb circle avoids impact");

		assertFalse(hits(GRID, 6.0, 0, 1.5, 0, 0, 0, true), "3.5-block east-west meridian lane is safe");
		assertTrue(hits(GRID, 6.0, 0, 2.5, 0, 0, 0, true), "outside east-west meridian lane is struck");
		assertFalse(hits(GRID, -1.5, 0, 6.0, 0, 0, 0, false), "3.5-block north-south meridian lane is safe");
		assertTrue(hits(GRID, -2.5, 0, 6.0, 0, 0, 0, false), "outside north-south meridian lane is struck");
		assertFalse(hits(GRID, 20.0, 0, 20.0, 0, 0, 0, true), "outside arena bounds is never hit");
	}
}
