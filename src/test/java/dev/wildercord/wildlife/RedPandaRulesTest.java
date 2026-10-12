package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RedPandaRulesTest {
	@Test
	void strangersCloseByMakeItRearUpAndASleeperOnlyRightUpClose() {
		assertTrue(RedPandaRules.rears(4, false, false));
		assertFalse(RedPandaRules.rears(RedPandaRules.STAND_RANGE + 1, false, false), "too far off");
		assertFalse(RedPandaRules.rears(2, true, false), "not someone it trusts");
		assertFalse(RedPandaRules.rears(4, false, true), "asleep, it doesn't notice that far off");
		assertTrue(RedPandaRules.rears(1, false, true));
	}

	@Test
	void itNapsThroughTheMiddleOfTheDay() {
		assertTrue(RedPandaRules.drowsy(6000));
		assertTrue(RedPandaRules.drowsy(24000 * 3 + 6000));
		assertFalse(RedPandaRules.drowsy(0), "morning");
		assertFalse(RedPandaRules.drowsy(13000), "dusk and night, it's about");
		assertFalse(RedPandaRules.drowsy(23000));
	}

	@Test
	void itForagesWhenRestedAndTumblesOnlyWithAnIdleNeighbour() {
		assertTrue(RedPandaRules.forages(RedPandaRules.FORAGE_REST));
		assertFalse(RedPandaRules.forages(RedPandaRules.FORAGE_REST - 1));
		assertTrue(RedPandaRules.tumbles(2, true));
		assertFalse(RedPandaRules.tumbles(2, false), "the other is busy");
		assertFalse(RedPandaRules.tumbles(RedPandaRules.PLAY_RANGE + 1, true), "too far off");
	}

	@Test
	void aTumbleRollsAllTheWayOverThenLiesStill() {
		assertEquals(0, RedPandaRules.roll(0), 1e-6);
		assertEquals(Math.PI, RedPandaRules.roll(RedPandaRules.ROLL_TICKS / 2F), 1e-4);
		assertEquals(0, RedPandaRules.roll(RedPandaRules.ROLL_TICKS + 1), 1e-6);
		float last = -1;
		for (float t = 0; t < RedPandaRules.ROLL_TICKS; t += 0.5F) {
			float r = RedPandaRules.roll(t);
			assertTrue(r >= last && r <= Math.PI * 2 + 1e-4, "it rolls one way, once");
			last = r;
		}
	}

	@Test
	void trustLastsAWhile() {
		assertTrue(RedPandaRules.trusts(0));
		assertFalse(RedPandaRules.trusts(RedPandaRules.TRUST_TICKS));
		assertFalse(RedPandaRules.trusts(-1));
	}
}
