package dev.wildercord.ocean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure math, diving gear, combat, and Trench Anvil recipe verification for Deep Ocean rules. */
class OceanRulesTest {
	@Test
	void tankOxygenDrainsSlowlyWithHelmAndRefillsInAir() {
		int start = OceanRules.CHEST_OXYGEN_MAX;
		int afterEvenTick = OceanRules.stepTankOxygen(start, true, true, true, false, 2);
		int afterOddTick = OceanRules.stepTankOxygen(afterEvenTick, true, true, true, false, 3);
		assertEquals(start - 1, afterEvenTick);
		assertEquals(start - 1, afterOddTick, "helm halves oxygen drain rate");

		int refilled = OceanRules.stepTankOxygen(100, true, true, false, false, 4);
		assertEquals(100 + OceanRules.CHEST_OXYGEN_REFILL_RATE, refilled);
	}

	@Test
	void hydroSlashConeAcceptsForwardAndRejectsBehindTargets() {
		assertTrue(OceanRules.isInHydroSlashArc(0, 64, 0, 0, 1, 1.5, 64.5, 4.0));
		assertFalse(OceanRules.isInHydroSlashArc(0, 64, 0, 0, 1, 0.0, 64.0, -3.0), "target behind wielder must not be hit");
		assertFalse(OceanRules.isInHydroSlashArc(0, 64, 0, 0, 1, 0.0, 64.0, 8.0), "target beyond glaive range must not be hit");
	}

	@Test
	void conchDomeAndLanternRepelBoundsHold() {
		assertTrue(OceanRules.isInsideConchDome(10, 40, 10, 13.0, 41.0, 11.0));
		assertFalse(OceanRules.isInsideConchDome(10, 40, 10, 15.5, 40.0, 10.0));
		assertTrue(OceanRules.shouldRepelSwarmer(36.0, true));
		assertFalse(OceanRules.shouldRepelSwarmer(121.0, true));
		assertFalse(OceanRules.shouldRepelSwarmer(9.0, false));
	}

	@Test
	void trenchAnvilRecipesMatchBothMaterialOrders() {
		var helm = OceanRules.matchTrenchRecipe(
			"minecraft:diamond_helmet",
			"wildercord:abyssal_iron_plate", 4,
			"wildercord:abyssal_pearl", 2
		);
		assertTrue(helm.isPresent());
		assertEquals("wildercord:abyssal_diving_helm", helm.get().outputItemId());

		var glaiveSwapped = OceanRules.matchTrenchRecipe(
			"minecraft:netherite_ingot",
			"wildercord:leviathan_heart_pearl", 1,
			"wildercord:coral_edge_fang", 2
		);
		assertTrue(glaiveSwapped.isPresent());
		assertEquals("wildercord:leviathan_glaive", glaiveSwapped.get().outputItemId());

		assertTrue(OceanRules.matchTrenchRecipe(
			"minecraft:netherite_ingot",
			"wildercord:leviathan_heart_pearl", 0,
			"wildercord:coral_edge_fang", 2
		).isEmpty());
	}

	@Test
	void thalassorPhasesFollowNonGlassCannonThresholds() {
		assertTrue(OceanRules.THALASSOR_MAX_HP >= 2000.0);
		assertEquals(1, OceanRules.thalassorPhase(2000, OceanRules.THALASSOR_MAX_HP));
		assertEquals(2, OceanRules.thalassorPhase(1000, OceanRules.THALASSOR_MAX_HP));
		assertEquals(3, OceanRules.thalassorPhase(500, OceanRules.THALASSOR_MAX_HP));
	}

	@Test
	void fullDivingSuitReducesAbyssalDamageAndSeahorseDashTracksLook() {
		assertEquals(0.75f, OceanRules.abyssalDamageMultiplier(4), 1.0e-5f);
		assertEquals(1.0f, OceanRules.abyssalDamageMultiplier(3), 1.0e-5f);

		double[] dive = OceanRules.seahorseDashVelocity(0.0f, 45.0f);
		assertTrue(dive[1] < -0.5, "looking down should pitch dash downward");
		assertTrue(dive[2] > 0.5, "south yaw should propel along +Z");
	}
}
