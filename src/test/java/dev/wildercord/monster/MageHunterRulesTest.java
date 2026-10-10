package dev.wildercord.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MageHunterRulesTest {
	@Test
	void huntersComeForTheFifthCircle() {
		assertFalse(MageHunterRules.hunted(4));
		assertTrue(MageHunterRules.hunted(MageHunterRules.MIN_CIRCLE));
		assertEquals(0, MageHunterRules.band(4, 0.0));
	}

	@Test
	void bandsGrowWithTheCircles() {
		assertEquals(1, MageHunterRules.band(5, 0.0));
		assertEquals(1, MageHunterRules.band(10, 0.6));
		assertEquals(2, MageHunterRules.band(10, 0.4));
		assertEquals(2, MageHunterRules.band(15, 0.99));
	}

	@Test
	void aBlowTakesManaButNeverMoreThanThereIs() {
		assertEquals(MageHunterRules.DRAIN, MageHunterRules.drained(100));
		assertEquals(4, MageHunterRules.drained(4));
		assertEquals(0, MageHunterRules.drained(0));
	}

	@Test
	void spellsHurtHalfAsMuch() {
		assertEquals(5, MageHunterRules.spellHarm(10));
	}
}
