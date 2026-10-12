package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DunestingerRulesTest {
	@Test
	void runningFeetCarryFurtherThroughTheSandThanCarefulOnes() {
		assertTrue(DunestingerRules.wakes(3.5, false, false));
		assertFalse(DunestingerRules.wakes(5.0, false, false));
		assertTrue(DunestingerRules.wakes(6.5, true, false), "sprinting");
		assertFalse(DunestingerRules.wakes(2.5, false, true), "crouching past");
		assertTrue(DunestingerRules.wakes(1.5, true, true), "right on top of it, crouching wins");
		assertTrue(DunestingerRules.wakeRange(true, false) > DunestingerRules.wakeRange(false, false));
		assertTrue(DunestingerRules.wakeRange(false, true) < DunestingerRules.wakeRange(false, false));
	}

	@Test
	void itStingsWhenItCanAndPinchesOtherwise() {
		assertEquals(2, DunestingerRules.attack(1.0, 100, 0, 0), "sting ready: sting");
		assertEquals(1, DunestingerRules.attack(1.0, 100, 0, 200), "sting not ready: pincers");
		assertEquals(2, DunestingerRules.attack(2.5, 100, 0, 0), "the tail reaches further than the claws");
		assertEquals(0, DunestingerRules.attack(2.5, 100, 0, 200));
		assertEquals(0, DunestingerRules.attack(5.0, 100, 0, 0), "too far for either");
		assertEquals(0, DunestingerRules.attack(1.0, 100, 200, 200), "neither ready");
	}

	@Test
	void hurtOnSandItGoesUnderButNotAgainAtOnce() {
		assertTrue(DunestingerRules.burrows(5, 20, true, 100, 100));
		assertFalse(DunestingerRules.burrows(5, 20, false, 100, 100), "not on sand");
		assertFalse(DunestingerRules.burrows(15, 20, true, 100, 100), "not hurt enough");
		assertFalse(DunestingerRules.burrows(5, 20, true, 99, 100), "went under lately");
		assertFalse(DunestingerRules.burrows(0, 0, true, 100, 0));
	}
}
