package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HedgehogRulesTest {
	@Test
	void onlySomeoneRunningCloseByStartlesIt() {
		assertTrue(HedgehogRules.startles(true, 2, false));
		assertFalse(HedgehogRules.startles(false, 2, false), "walking past doesn't");
		assertFalse(HedgehogRules.startles(true, HedgehogRules.STARTLE_RANGE + 0.5, false), "too far off");
		assertFalse(HedgehogRules.startles(true, 1, true), "not someone it trusts");
	}

	@Test
	void itPeeksOutOnlyAtTheEndOfACurl() {
		assertFalse(HedgehogRules.peeking(HedgehogRules.CURL_TICKS));
		assertTrue(HedgehogRules.peeking(HedgehogRules.PEEK_TICKS));
		assertTrue(HedgehogRules.peeking(1));
		assertFalse(HedgehogRules.peeking(0), "uncurled isn't peeking");
		assertTrue(HedgehogRules.PEEK_TICKS < HedgehogRules.CURL_TICKS);
	}

	@Test
	void curledUpItTakesLess() {
		assertEquals(10, HedgehogRules.blunted(10, false));
		assertEquals(10 * HedgehogRules.CURLED_TAKES, HedgehogRules.blunted(10, true), 1e-6);
		assertTrue(HedgehogRules.CURLED_TAKES < 1);
	}

	@Test
	void trustLastsAWhileAfterAFeeding() {
		assertTrue(HedgehogRules.trusts(0));
		assertTrue(HedgehogRules.trusts(HedgehogRules.TRUST_TICKS - 1));
		assertFalse(HedgehogRules.trusts(HedgehogRules.TRUST_TICKS));
		assertFalse(HedgehogRules.trusts(-1), "never fed");
	}

	@Test
	void giftsComeWithinTheirWindowAndCoverEveryKind() {
		assertEquals(HedgehogRules.GIFT_MIN, HedgehogRules.giftDelay(0));
		assertTrue(HedgehogRules.giftDelay(0.999) < HedgehogRules.GIFT_MAX);
		assertTrue(HedgehogRules.GIFT_MAX < HedgehogRules.TRUST_TICKS, "the gift comes while it still trusts you");
		assertEquals(HedgehogRules.BERRIES, HedgehogRules.gift(0));
		assertEquals(HedgehogRules.SEEDS, HedgehogRules.gift(0.9999));
		boolean[] seen = new boolean[6];
		for (int i = 0; i < 1000; i++) {
			seen[HedgehogRules.gift(i / 1000.0)] = true;
		}
		for (boolean s : seen) {
			assertTrue(s);
		}
	}

	@Test
	void berriesTameAWildOneAboutOneTimeInThree() {
		assertTrue(HedgehogRules.tames(0));
		assertTrue(HedgehogRules.tames(0.33));
		assertFalse(HedgehogRules.tames(0.34));
		assertFalse(HedgehogRules.tames(0.99));
		int tamed = 0;
		for (int i = 0; i < 300; i++) {
			if (HedgehogRules.tames(i / 300.0)) tamed++;
		}
		assertEquals(100, tamed, "one roll in three");
	}

	@Test
	void aTameOneBringsSomethingEveryFewMinutes() {
		assertEquals(HedgehogRules.TAME_GIFT_MIN, HedgehogRules.tameGiftDelay(0));
		assertTrue(HedgehogRules.tameGiftDelay(0.999) < HedgehogRules.TAME_GIFT_MAX);
		assertTrue(HedgehogRules.TAME_GIFT_MIN > HedgehogRules.GIFT_MAX, "a tame one's gifts are rarer than a fed wild one's first");
		assertTrue(HedgehogRules.BERRY_HEAL > 0);
	}
}
