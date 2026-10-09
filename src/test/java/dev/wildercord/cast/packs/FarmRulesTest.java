package dev.wildercord.cast.packs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The farmstead runes' pure rules: reaches, caps, cooldowns and the kitchen's sums. */
class FarmRulesTest {
	@Test
	void patchesGrowWithWidenButStayBounded() {
		assertEquals(2, FarmRules.patch(1.0));
		assertEquals(3, FarmRules.patch(1.5));
		assertEquals(1, FarmRules.patch(0.1));
		assertEquals(FarmRules.MAX_PATCH, FarmRules.patch(100));
	}

	@Test
	void reachesNeverPassTwiceTheirBase() {
		assertEquals(8, FarmRules.reach(8, 1.0));
		assertEquals(16, FarmRules.reach(8, 5.0));
		assertEquals(1, FarmRules.reach(8, 0.0));
	}

	@Test
	void plowlineLengthensWithPowerToItsCap() {
		assertEquals(FarmRules.PLOW_LENGTH, FarmRules.plowLength(1.0));
		assertEquals(12, FarmRules.plowLength(1.5));
		assertEquals(FarmRules.PLOW_MAX, FarmRules.plowLength(10));
		assertEquals(1, FarmRules.plowLength(0.01));
	}

	@Test
	void ripenGivesOneToThreeStagesAndNeverPassesTheMax() {
		assertEquals(1, FarmRules.ripenSteps(1.0));
		assertEquals(2, FarmRules.ripenSteps(1.5));
		assertEquals(3, FarmRules.ripenSteps(2.5));
		assertEquals(3, FarmRules.ripenSteps(50));
		assertEquals(3, FarmRules.nextAge(2, 7, 1));
		assertEquals(7, FarmRules.nextAge(6, 7, 3));
		assertEquals(7, FarmRules.nextAge(7, 7, 3));
		assertEquals(9, FarmRules.nextAge(9, 7, 1), "an age already past the max is left alone");
		assertEquals(4, FarmRules.nextAge(4, 7, -2), "a negative step never shrinks a plant");
	}

	@Test
	void batchesDoubleOnlyAtDoublePower() {
		assertEquals(8, FarmRules.batch(1.0));
		assertEquals(8, FarmRules.batch(1.99));
		assertEquals(16, FarmRules.batch(2.0));
		assertEquals(4, FarmRules.bakes(1.0));
		assertEquals(8, FarmRules.bakes(3.0));
	}

	@Test
	void pollinationIsBoundedByTheBlockBudget() {
		assertEquals(0, FarmRules.pollinateStages(0, 1));
		assertEquals(5, FarmRules.pollinateStages(5, 1));
		assertEquals(10, FarmRules.pollinateStages(5, 2));
		assertEquals(32, FarmRules.pollinateStages(100, 2));
	}

	@Test
	void hensAndHivesWaitOutTheirCooldown() {
		assertTrue(FarmRules.ready(null, 0, FarmRules.HEN_COOLDOWN_TICKS));
		assertFalse(FarmRules.ready(100L, 100 + FarmRules.HEN_COOLDOWN_TICKS - 1, FarmRules.HEN_COOLDOWN_TICKS));
		assertTrue(FarmRules.ready(100L, 100 + FarmRules.HEN_COOLDOWN_TICKS, FarmRules.HEN_COOLDOWN_TICKS));
		assertTrue(FarmRules.HIVE_COOLDOWN_TICKS < FarmRules.HEN_COOLDOWN_TICKS);
	}

	@Test
	void stalksRiseOnlyBelowTheVanillaHeight() {
		assertFalse(FarmRules.mayRise(0));
		assertTrue(FarmRules.mayRise(1));
		assertTrue(FarmRules.mayRise(2));
		assertFalse(FarmRules.mayRise(3));
	}

	@Test
	void theKitchenNeverMakesFoodFromNothing() {
		assertEquals(2, FarmRules.stews(2, 5, 9, 3));
		assertEquals(0, FarmRules.stews(4, 4, 0, 3), "no bowls, no stew");
		assertEquals(3, FarmRules.stews(9, 9, 9, FarmRules.STEW_MAX));
		assertEquals(1, FarmRules.soups(11, 4, 3), "six beetroots a soup");
		assertEquals(0, FarmRules.soups(5, 4, 3));
		assertEquals(3, FarmRules.loaves(10, 8), "three wheat a loaf");
		assertEquals(4, FarmRules.loaves(64, 4));
		assertEquals(1, FarmRules.pies(5, 5, 1, 4), "an egg a pie");
		assertEquals(0, FarmRules.pies(0, 5, 5, 4));
		assertEquals(0, FarmRules.loaves(-3, 4));
	}

	@Test
	void barkstripOnlyStripsUnstrippedLogs() {
		assertEquals("stripped_oak_log", FarmRules.strippedPath("oak_log"));
		assertEquals("stripped_crimson_stem", FarmRules.strippedPath("crimson_stem"));
		assertNull(FarmRules.strippedPath("stripped_oak_log"));
		assertNull(FarmRules.strippedPath(""));
		assertNull(FarmRules.strippedPath(null));
	}

	@Test
	void saplingsAreSpacedForTheirCrowns() {
		assertFalse(FarmRules.spaced(0, 0));
		assertFalse(FarmRules.spaced(1, 1));
		assertTrue(FarmRules.spaced(2, 0));
		assertTrue(FarmRules.spaced(-1, -2));
	}

	@Test
	void timingsScaleWithExtendAndNeverVanish() {
		assertEquals(1200, FarmRules.ticks(FarmRules.DEWKEEP_SECONDS, 1.0));
		assertEquals(2400, FarmRules.ticks(FarmRules.DEWKEEP_SECONDS, 2.0));
		assertEquals(1, FarmRules.ticks(0, 1.0));
		assertEquals(12, FarmRules.pulses(FarmRules.DEWKEEP_SECONDS, 1.0, FarmRules.DEWKEEP_EVERY_TICKS));
		assertEquals(1, FarmRules.pulses(1, 1.0, 1000));
	}

	@Test
	void movementRunesStaySurvivalSafe() {
		assertEquals(0.9, FarmRules.tossSpeed(1.0), 1e-9);
		assertEquals(1.3, FarmRules.tossSpeed(100), 1e-9, "Hayloft never tosses past a safe height");
		assertTrue(FarmRules.tossSpeed(0) > 0);
		assertEquals(1.7, FarmRules.zipSpeed(1.0), 1e-9);
		assertEquals(2.6, FarmRules.zipSpeed(100), 1e-9);
	}
}
