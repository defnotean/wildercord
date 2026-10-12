package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OtterRulesTest {
	@Test
	void itFloatsOnlyInWaterAndWhenDoingNothingElse() {
		assertTrue(OtterRules.floats(true, OtterRules.IDLE));
		assertFalse(OtterRules.floats(false, OtterRules.IDLE), "ashore");
		assertFalse(OtterRules.floats(true, OtterRules.CRACK), "busy knocking");
	}

	@Test
	void twoGrownFloatingOttersNearbyHoldPaws() {
		assertTrue(OtterRules.holds(OtterRules.FLOAT, OtterRules.FLOAT, false, 3));
		assertFalse(OtterRules.holds(OtterRules.FLOAT, OtterRules.HOLD, false, 3), "already holding someone");
		assertFalse(OtterRules.holds(OtterRules.FLOAT, OtterRules.FLOAT, true, 3), "pups don't");
		assertFalse(OtterRules.holds(OtterRules.FLOAT, OtterRules.FLOAT, false, OtterRules.HOLD_RANGE + 1), "too far apart");
		assertTrue(OtterRules.partnerOnRight(0.4));
		assertFalse(OtterRules.partnerOnRight(-0.4));
	}

	@Test
	void onlyARestedGrownOtterHuntsANearbyFish() {
		assertTrue(OtterRules.hunts(false, 4, OtterRules.HUNT_REST));
		assertFalse(OtterRules.hunts(true, 4, OtterRules.HUNT_REST));
		assertFalse(OtterRules.hunts(false, OtterRules.HUNT_RANGE + 1, OtterRules.HUNT_REST));
		assertFalse(OtterRules.hunts(false, 4, OtterRules.HUNT_REST - 1));
	}

	@Test
	void itKnocksInARhythmAndTheJuggledPebbleArcsFromPawToPaw() {
		assertTrue(OtterRules.knocks(OtterRules.KNOCK_EVERY * 3));
		assertFalse(OtterRules.knocks(OtterRules.KNOCK_EVERY * 3 + 1));
		assertFalse(OtterRules.knocks(0), "done");
		assertEquals(0, OtterRules.tossHeight(0), 1e-6);
		assertEquals(5, OtterRules.tossHeight(6), 1e-5);
		assertEquals(-1, OtterRules.tossSide(0), 1e-6);
		assertEquals(1, OtterRules.tossSide(12), 1e-6);
		for (float t = 0; t < 48; t += 0.5F) {
			assertTrue(OtterRules.tossHeight(t) >= 0);
			assertTrue(Math.abs(OtterRules.tossSide(t)) <= 1.0001F);
		}
	}

	@Test
	void trustLastsAWhileAndBringsGifts() {
		assertTrue(OtterRules.trusts(0));
		assertFalse(OtterRules.trusts(OtterRules.TRUST_TICKS));
		assertFalse(OtterRules.trusts(-1));
		assertTrue(OtterRules.gifts(true, 2, OtterRules.GIFT_REST));
		assertFalse(OtterRules.gifts(false, 2, OtterRules.GIFT_REST));
		assertFalse(OtterRules.gifts(true, OtterRules.GIFT_RANGE + 1, OtterRules.GIFT_REST));
		assertFalse(OtterRules.gifts(true, 2, OtterRules.GIFT_REST - 1));
		assertEquals(0, OtterRules.gift(0));
		assertEquals(1, OtterRules.gift(50));
		assertEquals(2, OtterRules.gift(80));
		assertEquals(3, OtterRules.gift(99));
	}
}
