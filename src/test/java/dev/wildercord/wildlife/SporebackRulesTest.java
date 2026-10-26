package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SporebackRulesTest {
	@Test
	void onlyTwoShareACave() {
		assertTrue(SporebackRules.room(1));
		assertFalse(SporebackRules.room(2));
	}

	@Test
	void itIsReadyFromItsDeadlineOn() {
		assertFalse(SporebackRules.ready(99, 100));
		assertTrue(SporebackRules.ready(100, 100));
		assertTrue(SporebackRules.ready(101, 100));
	}

	@Test
	void itsRestsAreFiniteAndOrdered() {
		assertTrue(SporebackRules.HIDE_TICKS > 0 && SporebackRules.HIDE_TICKS < SporebackRules.RESPONSE_REST);
		assertTrue(SporebackRules.RESPONSE_REST < SporebackRules.FORAGE_REST);
		assertTrue(SporebackRules.FORAGE_REST < SporebackRules.GATHER_REST);
	}
}
