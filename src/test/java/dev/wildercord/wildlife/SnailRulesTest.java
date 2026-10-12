package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnailRulesTest {
	@Test
	void onlyRunningCloseByStrangersStartleIt() {
		assertTrue(SnailRules.startles(true, 2.0, false));
		assertFalse(SnailRules.startles(false, 1.0, false));
		assertFalse(SnailRules.startles(true, 2.0, true));
		assertFalse(SnailRules.startles(true, SnailRules.STARTLE_RANGE + 0.1, false));
	}

	@Test
	void itPeeksOutAtTheEndOfHiding() {
		assertFalse(SnailRules.peeking(0));
		assertFalse(SnailRules.peeking(SnailRules.HIDE_TICKS));
		assertTrue(SnailRules.peeking(SnailRules.PEEK_TICKS));
		assertTrue(SnailRules.peeking(1));
	}

	@Test
	void itsShellBluntsBlows() {
		assertEquals(10F, SnailRules.blunted(10F, false));
		assertEquals(3F, SnailRules.blunted(10F, true), 1e-6);
	}

	@Test
	void shearingGivesTwoToFourMushrooms() {
		assertEquals(SnailRules.YIELD_MIN, SnailRules.yield(0.0));
		assertEquals(SnailRules.YIELD_MAX, SnailRules.yield(0.9999));
		for (double roll = 0; roll < 1; roll += 0.01) {
			int n = SnailRules.yield(roll);
			assertTrue(n >= SnailRules.YIELD_MIN && n <= SnailRules.YIELD_MAX);
		}
	}

	@Test
	void feedingHastensTheCapButNotIntoThePast() {
		assertEquals(5000 - SnailRules.FED_HASTENS, SnailRules.hastened(5000, 0));
		assertEquals(4900, SnailRules.hastened(5000, 4900));
	}

	@Test
	void trustLastsAWhile() {
		assertTrue(SnailRules.trusts(0));
		assertFalse(SnailRules.trusts(SnailRules.TRUST_TICKS));
		assertFalse(SnailRules.trusts(-1));
	}
}
