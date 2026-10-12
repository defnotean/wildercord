package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MantisRulesTest {
	@Test
	void sneakingSlipsPastItsNotice() {
		assertTrue(MantisRules.notices(10, false));
		assertFalse(MantisRules.notices(10, true), "sneaking, from afar");
		assertTrue(MantisRules.notices(3, true), "sneaking, too close");
		assertFalse(MantisRules.notices(MantisRules.NOTICE_RANGE + 1, false));
	}

	@Test
	void itStrikesFromCloseAndOnlyWhenReady() {
		assertTrue(MantisRules.strikes(2, 100, 100));
		assertFalse(MantisRules.strikes(2, 99, 100), "still recovering");
		assertFalse(MantisRules.strikes(MantisRules.STRIKE_RANGE + 0.5, 100, 0), "too far");
	}

	@Test
	void theStrikeCatchesOnlyWhatIsInFront() {
		assertTrue(MantisRules.catches(2, 0.9));
		assertFalse(MantisRules.catches(2, 0.1), "stepped aside");
		assertFalse(MantisRules.catches(MantisRules.STRIKE_REACH + 0.5, 1), "out of reach");
	}

	@Test
	void itHoldsOnAndNibblesUntilItLetsGo() {
		assertFalse(MantisRules.nibbles(0));
		assertTrue(MantisRules.nibbles(MantisRules.NIBBLE_EVERY));
		assertFalse(MantisRules.nibbles(MantisRules.NIBBLE_EVERY + 1));
		assertFalse(MantisRules.letsGo(MantisRules.CLUTCH_TICKS - 1));
		assertTrue(MantisRules.letsGo(MantisRules.CLUTCH_TICKS));
	}

	@Test
	void aHardBlowBreaksItsGripAndCaughtOutItTakesMore() {
		assertTrue(MantisRules.breaksFree(MantisRules.BREAK_FREE));
		assertFalse(MantisRules.breaksFree(1));
		assertEquals(6, MantisRules.hurt(4, true));
		assertEquals(4, MantisRules.hurt(4, false));
	}
}
