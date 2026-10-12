package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PuffinRulesTest {
	@Test
	void aPufflingComesFirstThenAFriendOnceThenItself() {
		assertEquals(PuffinRules.FEED, PuffinRules.catchFor(5, 3, false));
		assertEquals(PuffinRules.GIFT, PuffinRules.catchFor(-1, 3, false));
		assertEquals(PuffinRules.KEEP, PuffinRules.catchFor(-1, 3, true), "one gift per feeding");
		assertEquals(PuffinRules.KEEP, PuffinRules.catchFor(-1, 40, false), "too far to bring it");
		assertEquals(PuffinRules.KEEP, PuffinRules.catchFor(-1, -1, false));
	}

	@Test
	void itDivesOnlyInDeepWaterAndUsuallyCatchesSomething() {
		assertFalse(PuffinRules.deepEnough(1));
		assertTrue(PuffinRules.deepEnough(PuffinRules.DIVE_DEPTH));
		assertFalse(PuffinRules.catches(0));
		assertTrue(PuffinRules.catches(1));
		assertTrue(PuffinRules.catches(2));
	}

	@Test
	void itFluttersDownSlowly() {
		assertEquals(-PuffinRules.GLIDE, PuffinRules.glide(-0.6));
		assertEquals(0.3, PuffinRules.glide(0.3));
	}

	@Test
	void itSleepsAtNightAndTrustWearsOff() {
		assertTrue(PuffinRules.night(18000));
		assertFalse(PuffinRules.night(6000));
		assertTrue(PuffinRules.trusts(0));
		assertFalse(PuffinRules.trusts(PuffinRules.TRUST_TICKS));
		assertFalse(PuffinRules.trusts(-1));
	}
}
