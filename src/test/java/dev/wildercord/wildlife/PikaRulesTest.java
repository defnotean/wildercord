package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PikaRulesTest {
	@Test
	void itGoesHayingNowAndThen() {
		assertTrue(PikaRules.hays(true, true, PikaRules.NOTHING, -1, false));
		assertFalse(PikaRules.hays(false, true, PikaRules.NOTHING, -1, false), "young");
		assertFalse(PikaRules.hays(true, false, PikaRules.NOTHING, -1, false), "busy");
		assertFalse(PikaRules.hays(true, true, PikaRules.GRASS, -1, false), "mouth full");
		assertFalse(PikaRules.hays(true, true, PikaRules.NOTHING, PikaRules.GIFT_COOLDOWN, false), "too soon alone");
		assertTrue(PikaRules.hays(true, true, PikaRules.NOTHING, PikaRules.GIFT_COOLDOWN, true), "eager for a friend");
		assertTrue(PikaRules.hays(true, true, PikaRules.NOTHING, PikaRules.HAY_COOLDOWN, false));
	}

	@Test
	void flowersOnlyForAFriend() {
		assertTrue(PikaRules.cuts(true, false, false));
		assertFalse(PikaRules.cuts(false, true, false));
		assertTrue(PikaRules.cuts(false, true, true));
		assertFalse(PikaRules.cuts(false, false, true));
		assertEquals(PikaRules.FLOWER, PikaRules.carries(true));
		assertEquals(PikaRules.GRASS, PikaRules.carries(false));
		assertFalse(PikaRules.cut(PikaRules.NIBBLE_TICKS - 1));
		assertTrue(PikaRules.cut(PikaRules.NIBBLE_TICKS));
	}

	@Test
	void itBasksOnSunnyRock() {
		assertTrue(PikaRules.basks(true, true, true, -1));
		assertFalse(PikaRules.basks(false, true, true, -1), "night");
		assertFalse(PikaRules.basks(true, true, false, -1), "not on rock");
		assertFalse(PikaRules.basks(true, true, true, 10), "just basked");
		assertEquals(PikaRules.BASK_MIN, PikaRules.baskLength(-1));
		assertEquals(PikaRules.BASK_MAX, PikaRules.baskLength(5));
		assertTrue(PikaRules.staysBasking(true, false, 10, 200));
		assertFalse(PikaRules.staysBasking(true, true, 10, 200), "disturbed");
		assertFalse(PikaRules.staysBasking(true, false, 200, 200), "had its fill");
	}

	@Test
	void trustWearsOff() {
		assertFalse(PikaRules.trusts(-1));
		assertTrue(PikaRules.trusts(0));
		assertFalse(PikaRules.trusts(PikaRules.TRUST_TICKS));
	}
}
