package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dungeon bosses' phases, and no blow carrying one past the start of its next. */
class BossRulesTest {
	@Test
	void phasesAtTwoThirdsAndOneThird() {
		assertEquals(1, BossRules.phaseFor(300, 300));
		assertEquals(1, BossRules.phaseFor(201, 300));
		assertEquals(2, BossRules.phaseFor(200, 300));
		assertEquals(2, BossRules.phaseFor(101, 300));
		assertEquals(3, BossRules.phaseFor(100, 300));
		assertEquals(3, BossRules.phaseFor(1, 300));
		// The floor of each phase is where the next begins.
		for (float max : new float[] {300, 320, 340}) {
			assertEquals(2, BossRules.phaseFor(BossRules.floor(1, max), max));
			assertEquals(3, BossRules.phaseFor(BossRules.floor(2, max), max));
			assertEquals(0, BossRules.floor(3, max));
		}
	}

	@Test
	void aBlowIsHeldAtTheNextPhase() {
		float max = 300;
		// From full health, a killing blow stops at two thirds.
		assertEquals(200, BossRules.capped(1, max, 300, -500), 1e-4);
		// A blow that stays in the phase lands in full.
		assertEquals(250, BossRules.capped(1, max, 300, 250), 1e-4);
		// In the second phase, at one third.
		assertEquals(100, BossRules.capped(2, max, 200, 10), 1e-4);
		// In the last, it can die.
		assertEquals(-20, BossRules.capped(3, max, 100, -20), 1e-4);
		// Already under the floor (health set by a command): nothing to hold.
		assertEquals(50, BossRules.capped(1, max, 150, 50), 1e-4);
		// Never more than one phase at a time.
		for (int phase = 1; phase <= 2; phase++) {
			float held = BossRules.capped(phase, max, max, 0);
			assertTrue(held > 0);
			assertEquals(phase + 1, BossRules.phaseFor(held, max));
		}
		assertTrue(BossRules.STRAND_IMMUNE_TICKS >= 60);
	}
}
