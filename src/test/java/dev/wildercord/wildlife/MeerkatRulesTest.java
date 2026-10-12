package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MeerkatRulesTest {
	@Test
	void aWatcherSeesFurtherAndSomeoneCreepingIsSeenLate() {
		assertTrue(MeerkatRules.threatens(10, true, false, false));
		assertFalse(MeerkatRules.threatens(10, false, false, false), "nose down, it doesn't see that far");
		assertFalse(MeerkatRules.threatens(6, true, true, false), "crouched and creeping");
		assertTrue(MeerkatRules.threatens(3, true, true, false), "right there, crouched or not");
		assertFalse(MeerkatRules.threatens(1, true, false, true), "someone it trusts");
	}

	@Test
	void itSunsInTheMorningAndSleepsAtNight() {
		assertTrue(MeerkatRules.morning(500));
		assertTrue(MeerkatRules.morning(23800));
		assertFalse(MeerkatRules.morning(6000));
		assertTrue(MeerkatRules.night(18000));
		assertFalse(MeerkatRules.night(6000));
		assertFalse(MeerkatRules.night(23500), "up before the sun");
	}

	@Test
	void itStaysRightDownThenPeeksBackOut() {
		assertEquals(0F, MeerkatRules.peek(MeerkatRules.HIDE_TICKS));
		assertEquals(0F, MeerkatRules.peek(MeerkatRules.PEEK_TICKS));
		assertTrue(MeerkatRules.peek(MeerkatRules.PEEK_TICKS / 2) > 0);
		assertEquals(1F, MeerkatRules.peek(0));
	}

	@Test
	void oneKeepsWatchAtATimeAndTrustWearsOff() {
		assertTrue(MeerkatRules.takesWatch(Double.MAX_VALUE));
		assertFalse(MeerkatRules.takesWatch(4));
		assertTrue(MeerkatRules.trusts(0));
		assertFalse(MeerkatRules.trusts(MeerkatRules.TRUST_TICKS));
		assertFalse(MeerkatRules.trusts(-1));
	}
}
