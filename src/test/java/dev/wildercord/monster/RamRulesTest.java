package dev.wildercord.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RamRulesTest {
	@Test
	void chargesOnlyAtRangeWithAClearRun() {
		assertTrue(RamRules.charges(10, 100, 100, true));
		assertFalse(RamRules.charges(10, 100, 100, false));
		assertFalse(RamRules.charges(10, 99, 100, true));
		assertFalse(RamRules.charges(RamRules.CHARGE_MIN - 1, 100, 0, true));
		assertFalse(RamRules.charges(RamRules.CHARGE_MAX + 1, 100, 0, true));
	}

	@Test
	void aimSetsBeforeItGoes() {
		assertTrue(RamRules.aiming(RamRules.PAW_TICKS));
		assertFalse(RamRules.aiming(RamRules.AIM_SETS));
		assertFalse(RamRules.aiming(0));
	}

	@Test
	void chargeEndsWhenRunOrStopped() {
		assertFalse(RamRules.spent(10, RamRules.CHARGE_SPEED));
		assertTrue(RamRules.spent(RamRules.CHARGE_TICKS, RamRules.CHARGE_SPEED));
		assertTrue(RamRules.spent(10, 0));
		assertFalse(RamRules.spent(2, 0));
	}

	@Test
	void onlyAHardHitDazes() {
		assertTrue(RamRules.dazes(true, 10));
		assertFalse(RamRules.dazes(true, 1));
		assertFalse(RamRules.dazes(false, 10));
	}

	@Test
	void dazedTakesMore() {
		assertEquals(6, RamRules.hurt(4, true), 1.0E-6);
		assertEquals(4, RamRules.hurt(4, false), 1.0E-6);
	}

	@Test
	void buttsUpClose() {
		assertTrue(RamRules.butts(2, 50, 50));
		assertFalse(RamRules.butts(2, 49, 50));
		assertFalse(RamRules.butts(RamRules.BUTT_REACH + 0.5, 50, 0));
	}
}
