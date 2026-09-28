package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure rules of the fused runes of life and blood: Second Wind's lockout, Transfusion's gift, the blood price. */
class FusedLifeRulesTest {
	@Test
	void secondWindLocksOutForAMinuteAfterItSaves() {
		long saved = 5000;
		assertEquals(1200, FusedLifeRules.SECOND_WIND_LOCKOUT_TICKS);
		assertFalse(FusedLifeRules.lockedOut(-1, 0), "never saved: no lockout");
		assertTrue(FusedLifeRules.lockedOut(saved, saved), "the moment it saves");
		assertTrue(FusedLifeRules.lockedOut(saved, saved + 20), "a second later (a recast off cooldown)");
		assertTrue(FusedLifeRules.lockedOut(saved, saved + 1199), "just short of a minute");
		assertFalse(FusedLifeRules.lockedOut(saved, saved + 1200), "a minute on");
		assertFalse(FusedLifeRules.lockedOut(saved, saved + 100_000));
		assertEquals(60, FusedLifeRules.lockoutSecondsLeft(saved, saved));
		assertEquals(59, FusedLifeRules.lockoutSecondsLeft(saved, saved + 20));
		assertEquals(1, FusedLifeRules.lockoutSecondsLeft(saved, saved + 1199));
		assertEquals(0, FusedLifeRules.lockoutSecondsLeft(saved, saved + 1200));
	}

	@Test
	void secondWindLeavesFourHealth() {
		assertEquals(4, FusedLifeRules.secondWindHealth(1.0, 20), 1e-6);
		assertEquals(6, FusedLifeRules.secondWindHealth(1.5, 20), 1e-6);
		// Never under 1 (a weak cast still saves), never over the creature's most.
		assertEquals(1, FusedLifeRules.secondWindHealth(0.1, 20), 1e-6);
		assertEquals(3, FusedLifeRules.secondWindHealth(1.0, 3), 1e-6);
	}

	@Test
	void transfusionGivesUpToFourNeverBelowTwoNeverMoreThanNeeded() {
		// Plenty on both sides: 4.
		assertEquals(4, FusedLifeRules.transfusionGift(1.0, 20, 30, false), 1e-6);
		// Stronger: more.
		assertEquals(6, FusedLifeRules.transfusionGift(1.5, 20, 30, false), 1e-6);
		// The giver at 5 may only go down to 2.
		assertEquals(3, FusedLifeRules.transfusionGift(1.0, 5, 30, false), 1e-6);
		// At 2, or hardly above it, nothing.
		assertEquals(0, FusedLifeRules.transfusionGift(1.0, 2, 30, false), 1e-6);
		assertEquals(0, FusedLifeRules.transfusionGift(1.0, 2.1F, 30, false), 1e-6);
		// No more than half of what the ally is missing (it heals double): none wasted, none on the whole.
		assertEquals(0.5, FusedLifeRules.transfusionGift(1.0, 20, 1, false), 1e-6);
		assertEquals(0, FusedLifeRules.transfusionGift(1.0, 20, 0, false), 1e-6);
		// Creative gives without paying, still no more than 4 or than is needed.
		assertEquals(4, FusedLifeRules.transfusionGift(1.0, 1, 30, true), 1e-6);
		assertEquals(1, FusedLifeRules.transfusionGift(1.0, 1, 2, true), 1e-6);
	}

	@Test
	void theBloodPriceIsNeverTheLastOfIt() {
		assertTrue(FusedLifeRules.canPayBlood(20, 3));
		assertTrue(FusedLifeRules.canPayBlood(3.5F, 3));
		assertFalse(FusedLifeRules.canPayBlood(3, 3));
		assertFalse(FusedLifeRules.canPayBlood(1, 3));
	}
}
