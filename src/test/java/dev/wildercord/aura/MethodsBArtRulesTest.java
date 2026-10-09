package dev.wildercord.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MethodsBArtRulesTest {
	@Test
	void venomsToxinStacksToItsCapAndAPlayersIsLower() {
		assertEquals(-1, MethodsBArtRules.toxinAfter(0, false));
		assertEquals(0, MethodsBArtRules.toxinAfter(1, false));
		assertEquals(1, MethodsBArtRules.toxinAfter(2, false));
		assertEquals(MethodsBArtRules.TOXIN_CAP, MethodsBArtRules.toxinAfter(20, false));
		assertEquals(MethodsBArtRules.PVP_TOXIN_CAP, MethodsBArtRules.toxinAfter(20, true));
		assertTrue(MethodsBArtRules.PVP_TOXIN_CAP < MethodsBArtRules.TOXIN_CAP);
		// A fresh bite starts at the first stack whatever a stale amplifier says.
		assertEquals(0, MethodsBArtRules.toxin(5, false, false));
		assertEquals(MethodsBArtRules.TOXIN_CAP, MethodsBArtRules.toxin(MethodsBArtRules.TOXIN_CAP, true, false));
	}

	@Test
	void dawnBurnsTheUndeadHarder() {
		assertEquals(1.0, MethodsBArtRules.dawnFactor(1.0, false), 1e-9);
		assertEquals(MethodsBArtRules.UNDEAD_BANE, MethodsBArtRules.dawnFactor(1.0, true), 1e-9);
		assertEquals(0, MethodsBArtRules.dawnFactor(-2, true), 1e-9);
	}

	@Test
	void coatedPassivesGrowWithStageAndAreNothingWithoutAura() {
		assertEquals(0, MethodsBArtRules.echoChance(AuraRules.NONE), 1e-9);
		assertEquals(0, MethodsBArtRules.dawnGlow(AuraRules.NONE));
		assertEquals(0, MethodsBArtRules.venomChance(AuraRules.NONE), 1e-9);
		assertEquals(0, MethodsBArtRules.venomTicks(AuraRules.NONE));
		assertEquals(0, MethodsBArtRules.echoReel(AuraRules.FLOW));
		assertTrue(MethodsBArtRules.echoReel(AuraRules.EDGE) > 0);
		assertEquals(0, MethodsBArtRules.dawnGlint(AuraRules.FLOW - 1), 1e-9);
		assertTrue(MethodsBArtRules.dawnGlint(AuraRules.FLOW) > 0);
		assertEquals(1.0, MethodsBArtRules.venomChance(AuraRules.EDGE), 1e-9);
		for (int stage = 1; stage < AuraRules.MAX_STAGE; stage++) {
			assertTrue(MethodsBArtRules.echoChance(stage + 1) > MethodsBArtRules.echoChance(stage));
			assertTrue(MethodsBArtRules.dawnGlow(stage + 1) > MethodsBArtRules.dawnGlow(stage));
			assertTrue(MethodsBArtRules.venomChance(stage + 1) >= MethodsBArtRules.venomChance(stage));
			assertTrue(MethodsBArtRules.venomTicks(stage + 1) > MethodsBArtRules.venomTicks(stage));
			assertTrue(MethodsBArtRules.echoChance(stage) < 1);
		}
	}

	@Test
	void fighterBytesAndStylesSitClearOfTheBuiltIns() {
		for (String method : MethodsBPack.METHODS) {
			int index = MethodsBPack.fighterIndex(method);
			assertTrue(index >= MethodsBPack.FIGHTER_BASE && index < 127);
			assertEquals(method, MethodsBPack.fighterMethod(index));
		}
		assertEquals(-1, MethodsBPack.fighterIndex("ember"));
		assertNull(MethodsBPack.fighterMethod(0));
		assertEquals(15, MethodsBStyles.ARTS.size());
		for (int i = 0; i < MethodsBStyles.ARTS.size(); i++) {
			int id = MethodsBStyles.FIRST + i;
			assertTrue(MethodsBStyles.owns(id));
			assertEquals(i % 5, MethodsBStyles.slot(id));
			assertTrue(MethodsBStyles.articulated(id) >= 0);
		}
		assertEquals(-1, MethodsBStyles.articulated(MethodsBStyles.FIRST - 1));
		assertEquals(-1, MethodsBStyles.articulated(MethodsBStyles.FIRST + 15));
	}
}
