package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DormouseRulesTest {
	@Test
	void itSleepsTheDayAwayUnlessWoken() {
		assertTrue(DormouseRules.sleeps(true, -1));
		assertFalse(DormouseRules.sleeps(false, -1), "up at night");
		assertFalse(DormouseRules.sleeps(true, 100), "just woken");
		assertTrue(DormouseRules.sleeps(true, DormouseRules.WAKE_TICKS), "and back off again");
	}

	@Test
	void runningSendsItOffButCreepingAndSleepDoNot() {
		assertTrue(DormouseRules.startles(true, 4, false, false));
		assertFalse(DormouseRules.startles(false, 1, false, false), "creeping up");
		assertFalse(DormouseRules.startles(true, 4, true, false), "someone it trusts");
		assertFalse(DormouseRules.startles(true, 4, false, true), "fast asleep");
		assertFalse(DormouseRules.startles(true, 10, false, false), "far off");
	}

	@Test
	void itReachesBerriesStandingUpButNotFarAbove() {
		assertTrue(DormouseRules.reaches(0));
		assertTrue(DormouseRules.reaches(DormouseRules.REACH));
		assertFalse(DormouseRules.reaches(DormouseRules.REACH + 1));
		assertFalse(DormouseRules.reaches(-1));
	}

	@Test
	void fullerCheeksGlowBrighter() {
		assertEquals(0, DormouseRules.glow(0), 1e-6);
		assertTrue(DormouseRules.glow(1) > 0);
		assertTrue(DormouseRules.glow(3) > DormouseRules.glow(1));
		assertEquals(DormouseRules.glow(DormouseRules.POUCH), DormouseRules.glow(99), 1e-6);
		assertTrue(DormouseRules.forages(false, 0));
		assertFalse(DormouseRules.forages(false, DormouseRules.POUCH), "cheeks full");
		assertFalse(DormouseRules.forages(true, 0), "asleep");
	}

	@Test
	void trustedItBringsBerriesNowAndThen() {
		assertTrue(DormouseRules.trusts(0));
		assertFalse(DormouseRules.trusts(DormouseRules.TRUST_TICKS));
		assertTrue(DormouseRules.gives(1, 1.5, -1));
		assertFalse(DormouseRules.gives(0, 1.5, -1), "nothing in its cheeks");
		assertFalse(DormouseRules.gives(1, 6, -1), "too far");
		assertFalse(DormouseRules.gives(1, 1.5, 10), "only just gave one");
	}
}
