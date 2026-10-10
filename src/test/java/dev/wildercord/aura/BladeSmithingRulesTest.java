package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BladeSmithingRulesTest {
	@Test
	void theBondDecidesHowFarABladeTempers() {
		assertEquals(0, BladeSmithingRules.cap(BladeRules.NONE));
		assertEquals(1, BladeSmithingRules.cap(BladeRules.BONDED));
		assertEquals(2, BladeSmithingRules.cap(BladeRules.NAMED));
		assertEquals(3, BladeSmithingRules.cap(BladeRules.AWAKENED));
		assertEquals(5, BladeSmithingRules.cap(BladeRules.SOULFORGED));
		assertEquals(5, BladeSmithingRules.cap(99));
		assertEquals(0, BladeSmithingRules.cap(-1));
		assertFalse(BladeSmithingRules.canTemper(0, BladeRules.NONE));
		assertTrue(BladeSmithingRules.canTemper(0, BladeRules.BONDED));
		assertFalse(BladeSmithingRules.canTemper(1, BladeRules.BONDED));
		assertTrue(BladeSmithingRules.canTemper(4, BladeRules.SOULFORGED));
		assertFalse(BladeSmithingRules.canTemper(5, BladeRules.SOULFORGED));
	}

	@Test
	void aFullTemperTakesNearlyEveryMastersFirstFall() {
		assertEquals(30, BladeSmithingRules.totalSteel(BladeSmithingRules.MAX_TEMPER));
		assertTrue(16 * BladeSmithingRules.reward(true) >= BladeSmithingRules.totalSteel(BladeSmithingRules.MAX_TEMPER));
		assertTrue(14 * BladeSmithingRules.reward(true) < BladeSmithingRules.totalSteel(BladeSmithingRules.MAX_TEMPER));
		assertEquals(1, BladeSmithingRules.reward(false));
	}

	@Test
	void eachTemperCostsMoreThanTheLast() {
		for (int t = 2; t <= BladeSmithingRules.MAX_TEMPER; t++) {
			assertTrue(BladeSmithingRules.steel(t) > BladeSmithingRules.steel(t - 1));
			assertTrue(BladeSmithingRules.levels(t) > BladeSmithingRules.levels(t - 1));
		}
		// Never "Too Expensive" at a survival anvil.
		assertTrue(BladeSmithingRules.levels(BladeSmithingRules.MAX_TEMPER) < 40);
	}

	@Test
	void temperIsModestAndHalvedAgainstPlayers() {
		assertEquals(1.0, BladeSmithingRules.coat(0, false), 1e-9);
		assertEquals(1.25, BladeSmithingRules.coat(5, false), 1e-9);
		assertEquals(1.125, BladeSmithingRules.coat(5, true), 1e-9);
		assertEquals(BladeSmithingRules.coat(5, false), BladeSmithingRules.coat(50, false), 1e-9);
	}
}
