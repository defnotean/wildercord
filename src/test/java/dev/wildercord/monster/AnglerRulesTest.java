package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnglerRulesTest {
	@Test
	void theLureDrawsThoseWhoLookAtIt() {
		assertTrue(AnglerRules.draws(6, 0.99, false));
		assertFalse(AnglerRules.draws(6, 0.5, false), "looking away");
		assertFalse(AnglerRules.draws(6, 0.99, true), "sneaking");
		assertFalse(AnglerRules.draws(AnglerRules.DRAW_RANGE + 1, 0.99, false), "too far");
		assertFalse(AnglerRules.draws(1, 0.99, false), "already in its mouth's reach");
		assertTrue(AnglerRules.pull(AnglerRules.DRAW_RANGE) > AnglerRules.pull(3));
		assertEquals(0, AnglerRules.pull(AnglerRules.DRAW_RANGE + 1));
	}

	@Test
	void itSnapsUpClose() {
		assertTrue(AnglerRules.snaps(2, 100, 100));
		assertFalse(AnglerRules.snaps(5, 100, 100), "too far");
		assertFalse(AnglerRules.snaps(2, 99, 100), "still recovering");
		assertTrue(AnglerRules.bites(1.5, 10, 0));
		assertFalse(AnglerRules.bites(3, 10, 0));
	}

	@Test
	void aMissLeavesItWinded() {
		assertEquals(6.0F, AnglerRules.hurt(4, true), 1e-6);
		assertEquals(4.0F, AnglerRules.hurt(4, false));
	}

	@Test
	void itSettlesBackToWait() {
		assertTrue(AnglerRules.settles(0, false));
		assertFalse(AnglerRules.settles(10, true));
		assertTrue(AnglerRules.settles(AnglerRules.CHASE_TICKS, true));
	}
}
