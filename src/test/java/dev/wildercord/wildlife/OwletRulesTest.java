package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OwletRulesTest {
	@Test
	void itSleepsByDayUnlessWoken() {
		assertTrue(OwletRules.sleeps(true, -1));
		assertFalse(OwletRules.sleeps(false, -1));
		assertFalse(OwletRules.sleeps(true, 10), "just woken");
		assertTrue(OwletRules.sleeps(true, OwletRules.WAKE_TICKS));
	}

	@Test
	void itStaresDownCreakingsItCanSee() {
		assertTrue(OwletRules.stares(false, 8, true));
		assertFalse(OwletRules.stares(true, 8, true), "asleep");
		assertFalse(OwletRules.stares(false, 8, false), "out of sight");
		assertFalse(OwletRules.stares(false, OwletRules.STARE_RANGE + 1, true), "too far");
	}

	@Test
	void fedItTrustsAndWarnsNowAndThen() {
		assertTrue(OwletRules.trusts(0));
		assertFalse(OwletRules.trusts(-1));
		assertFalse(OwletRules.trusts(OwletRules.TRUST_TICKS));
		assertTrue(OwletRules.warns(false, 1, -1));
		assertFalse(OwletRules.warns(false, 0, -1), "nothing there");
		assertFalse(OwletRules.warns(true, 1, -1), "asleep");
		assertFalse(OwletRules.warns(false, 1, 10), "too soon");
		assertTrue(OwletRules.warns(false, 1, OwletRules.WARN_TICKS));
	}

	@Test
	void itFlutters() {
		assertEquals(-0.3, OwletRules.flutter(-0.5), 1.0E-9);
		assertEquals(0.4, OwletRules.flutter(0.4), 1.0E-9);
	}
}
