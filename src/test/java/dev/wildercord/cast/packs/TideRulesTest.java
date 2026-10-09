package dev.wildercord.cast.packs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TideRulesTest {
	@Test
	void lureHalvesTheWaitButKeepsASecond() {
		assertEquals(300, TideRules.lured(600));
		assertEquals(20, TideRules.lured(30));
		assertEquals(15, TideRules.lured(15));
		assertEquals(0, TideRules.lured(-4));
	}

	@Test
	void mendIsBoundedByPower() {
		assertEquals(16, TideRules.mend(1.0));
		assertEquals(1, TideRules.mend(0.0));
		assertEquals(64, TideRules.mend(100.0));
	}

	@Test
	void drownWardOnlyRefillsWhenLowAndUnspent() {
		assertTrue(TideRules.wardRefills(10, 1));
		assertFalse(TideRules.wardRefills(10, 0));
		assertFalse(TideRules.wardRefills(300, 3));
	}

	@Test
	void blockNamesMapOnlyWhereTheyShould() {
		assertEquals("red_concrete", TideRules.concreteFor("red_concrete_powder"));
		assertNull(TideRules.concreteFor("sand"));
		assertEquals("tube_coral_fan", TideRules.livingCoral("dead_tube_coral_fan"));
		assertEquals("brain_coral_block", TideRules.livingCoral("dead_brain_coral_block"));
		assertNull(TideRules.livingCoral("dead_bush"));
		assertNull(TideRules.livingCoral("tube_coral"));
	}

	@Test
	void eggsNeverPassTheLastStage() {
		assertEquals(1, TideRules.nextHatch(0));
		assertEquals(2, TideRules.nextHatch(1));
		assertEquals(2, TideRules.nextHatch(2));
	}

	@Test
	void stormGlassReadsTheCounters() {
		assertEquals(2, TideRules.weatherTurn(true, 1300, 0));
		assertEquals(-1, TideRules.weatherTurn(false, 5000, 600));
		assertEquals(-5, TideRules.weatherTurn(false, 6000, 0));
		assertEquals(0, TideRules.weatherTurn(false, 0, 0));
	}

	@Test
	void wetAndIcyChoicesAreFixed() {
		assertEquals(10, TideRules.inkveilSeconds(true));
		assertEquals(3, TideRules.inkveilSeconds(false));
		assertEquals(30, TideRules.shellbackSeconds(true));
		assertEquals(1, TideRules.skateAmplifier(true));
		assertEquals(0, TideRules.luckAmplifier(0));
		assertEquals(1, TideRules.luckAmplifier(2));
	}
}
