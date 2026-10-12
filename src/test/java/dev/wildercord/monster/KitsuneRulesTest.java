package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KitsuneRulesTest {
	@Test
	void itThrowsFoxfireFromAfar() {
		assertTrue(KitsuneRules.throwsFoxfire(8, 100, 100, true));
		assertFalse(KitsuneRules.throwsFoxfire(8, 99, 100, true), "not ready");
		assertFalse(KitsuneRules.throwsFoxfire(8, 100, 100, false), "can't see");
		assertFalse(KitsuneRules.throwsFoxfire(KitsuneRules.FOXFIRE_RANGE + 1, 100, 100, true), "too far");
		assertFalse(KitsuneRules.throwsFoxfire(2, 100, 100, true), "close enough to bite");
	}

	@Test
	void itBitesUpClose() {
		assertTrue(KitsuneRules.bites(1.5, 10, 10));
		assertFalse(KitsuneRules.bites(3, 10, 10));
		assertFalse(KitsuneRules.bites(1.5, 9, 10));
	}

	@Test
	void itSplitsAtTwoThirdsAndAThird() {
		assertFalse(KitsuneRules.splits(24, 24, 0));
		assertTrue(KitsuneRules.splits(16, 24, 0));
		assertFalse(KitsuneRules.splits(16, 24, 1), "already split once");
		assertTrue(KitsuneRules.splits(8, 24, 1));
		assertFalse(KitsuneRules.splits(1, 24, 2), "no more than twice");
		assertEquals(2, KitsuneRules.splitsEarned(1, 24));
		assertEquals(0, KitsuneRules.splitsEarned(1, 0));
	}

	@Test
	void copiesFade() {
		assertFalse(KitsuneRules.decoyFades(10, true, 5));
		assertTrue(KitsuneRules.decoyFades(KitsuneRules.DECOY_TICKS, true, 5));
		assertTrue(KitsuneRules.decoyFades(10, false, 5));
		assertTrue(KitsuneRules.decoyFades(10, true, KitsuneRules.DECOY_LEASH + 1));
	}
}
