package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PredatorRulesTest {
	@Test
	void aBiteChillsUpToFullyFrozen() {
		assertEquals(PredatorRules.CHILL_FROST, PredatorRules.chilled(0, 140));
		assertEquals(140, PredatorRules.chilled(100, 140));
		assertEquals(200, PredatorRules.chilled(200, 140), "it never thaws what's already colder");
	}

	@Test
	void aCougarWatchesOnlyWhenTameStandingAndNear() {
		long beat = PredatorRules.MARK_INTERVAL * 10L;
		assertTrue(PredatorRules.marks(true, false, 5, beat, 0));
		assertFalse(PredatorRules.marks(false, false, 5, beat, 0));
		assertFalse(PredatorRules.marks(true, true, 5, beat, 0));
		assertFalse(PredatorRules.marks(true, false, PredatorRules.MARK_LEASH + 1, beat, 0));
		assertFalse(PredatorRules.marks(true, false, 5, beat + 1, 0));
		assertTrue(PredatorRules.marks(true, false, 5, beat - 7, 7), "each cougar keeps its own beat");
	}

	@Test
	void aMarkOutlastsTheBeat() {
		assertTrue(PredatorRules.MARK_TICKS > PredatorRules.MARK_INTERVAL);
		assertTrue(PredatorRules.COUGAR_TAME_CHANCE < PredatorRules.LYNX_TAME_CHANCE);
	}
}
