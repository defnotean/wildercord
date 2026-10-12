package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PitchbackRulesTest {
	@Test
	void itLobsFromMiddleRangeWithAFullSac() {
		assertTrue(PitchbackRules.lobs(7, 100, 100, true));
		assertFalse(PitchbackRules.lobs(7, 99, 100, true), "not ready");
		assertFalse(PitchbackRules.lobs(7, 100, 100, false), "sac empty");
		assertFalse(PitchbackRules.lobs(2, 100, 100, true), "too close");
		assertFalse(PitchbackRules.lobs(PitchbackRules.LOB_MAX + 1, 100, 100, true), "too far");
	}

	@Test
	void aSecondGlobSticksYouFast() {
		assertFalse(PitchbackRules.sticks(-1), "not slowed");
		assertFalse(PitchbackRules.sticks(0), "barely slowed");
		assertTrue(PitchbackRules.sticks(PitchbackRules.GUM_LEVEL));
		assertTrue(PitchbackRules.sticks(6), "rooted already");
	}

	@Test
	void fireBurstsAFullSacOnly() {
		assertTrue(PitchbackRules.bursts(true, true));
		assertFalse(PitchbackRules.bursts(false, true));
		assertFalse(PitchbackRules.bursts(true, false));
	}

	@Test
	void itAimsHigherFurtherOffAndBacksOffWhenCrowded() {
		assertTrue(PitchbackRules.lift(10) > PitchbackRules.lift(4));
		assertTrue(PitchbackRules.lift(0) == 0);
		assertTrue(PitchbackRules.backsOff(2, true));
		assertFalse(PitchbackRules.backsOff(2, false), "empty, it comes on");
		assertFalse(PitchbackRules.backsOff(6, true));
	}
}
