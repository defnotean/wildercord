package dev.wildercord.content;

import dev.wildercord.config.WildercordConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The odds of fishing up magic: runes and Torn Pages in a treasure catch, and a rune tangled in the line in magic waters. */
class FishingRulesTest {
	/** The share of treasure catches a weight added beside vanilla's six takes. */
	private static double share(int[] weights, int index) {
		return (double) weights[index] / (FishingRules.VANILLA_TREASURE_WEIGHT + weights[0] + weights[1]);
	}

	@Test
	void aboutFourTreasureCatchesInElevenAreARuneAndOneATornPage() {
		int[] weights = FishingRules.treasureWeights(FishingRules.TREASURE_RUNE_CHANCE, FishingRules.TREASURE_PAGE_CHANCE);
		assertArrayEquals(new int[] {4, 1}, weights, "a rune of weight 4 and a page of weight 1 beside vanilla's six treasures");
		assertEquals(4.0 / 11, share(weights, 0), 1e-9);
		assertEquals(1.0 / 11, share(weights, 1), 1e-9);
	}

	@Test
	void theWeightsKeepTheSharesAsTheMultipliersRise() {
		// (Lower down, a page can't weigh less than 1 beside vanilla's six: it stays a little commoner than asked.)
		for (double multiplier : new double[] {1.0, 1.5, 2.0}) {
			int runes = WildercordConfig.scaledChance(FishingRules.TREASURE_RUNE_CHANCE, multiplier);
			int page = WildercordConfig.scaledChance(FishingRules.TREASURE_PAGE_CHANCE, multiplier);
			int[] weights = FishingRules.treasureWeights(runes, page);
			// Whole weights beside six of weight 1 can't be exact, but they stay close.
			assertEquals(runes / 100.0, share(weights, 0), 0.06, "runes at x" + multiplier);
			assertEquals(page / 100.0, share(weights, 1), 0.06, "pages at x" + multiplier);
		}
	}

	@Test
	void aMultiplierOfNothingLeavesThemOut() {
		assertArrayEquals(new int[] {0, 0}, FishingRules.treasureWeights(0, 0));
		assertArrayEquals(new int[] {0, 1}, FishingRules.treasureWeights(0, FishingRules.TREASURE_PAGE_CHANCE), "pages alone");
		assertEquals(0, FishingRules.treasureWeights(FishingRules.TREASURE_RUNE_CHANCE, 0)[1], "runes alone");
		assertTrue(FishingRules.treasureWeights(1, 1)[0] >= 1, "any chance at all is at least weight 1");
	}

	@Test
	void vanillasTreasuresAlwaysStillTurnUp() {
		int[] weights = FishingRules.treasureWeights(100, 100);
		double vanilla = (double) FishingRules.VANILLA_TREASURE_WEIGHT / (FishingRules.VANILLA_TREASURE_WEIGHT + weights[0] + weights[1]);
		assertTrue(vanilla >= (100 - FishingRules.TREASURE_MOST) / 100.0 - 0.02, "vanilla's treasures keep " + vanilla);
	}

	@Test
	void magicWatersAddUpToACap() {
		assertEquals(0, FishingRules.magicWatersChance(false, false, false, 1.0), 1e-9, "plain water: nothing tangled");
		assertEquals(0.12, FishingRules.magicWatersChance(true, false, false, 1.0), 1e-9, "a mana storm");
		assertEquals(0.05, FishingRules.magicWatersChance(false, true, false, 1.0), 1e-9, "a ley line");
		assertEquals(0.05, FishingRules.magicWatersChance(false, false, true, 1.0), 1e-9, "a thunderstorm");
		assertEquals(0.10, FishingRules.magicWatersChance(false, true, true, 1.0), 1e-9, "a thunderstorm over a ley line");
		assertEquals(0.17, FishingRules.magicWatersChance(true, true, false, 1.0), 1e-9, "a mana storm over a ley line");
		assertEquals(0.20, FishingRules.magicWatersChance(true, true, true, 1.0), 1e-9, "all three: the cap, not 22%");
	}

	@Test
	void magicWatersFollowTheRuneLootMultiplier() {
		assertEquals(0, FishingRules.magicWatersChance(true, true, true, 0), 1e-9, "a multiplier of 0 turns it off");
		assertEquals(0.06, FishingRules.magicWatersChance(true, false, false, 0.5), 1e-9);
		assertEquals(0.40, FishingRules.magicWatersChance(true, true, true, 2.0), 1e-9, "the cap scales too");
		assertEquals(1.0, FishingRules.magicWatersChance(true, true, true, 10.0), 1e-9, "never past certain");
		assertEquals(0, FishingRules.magicWatersChance(true, false, false, -1.0), 1e-9);
	}

	@Test
	void theFishingRunesAreLikelierInMagicWatersThanInTreasure() {
		assertTrue(FishingRules.WORLD_WEIGHT_TREASURE > 1, "likelier than a sea rune of their tier in treasure");
		assertTrue(FishingRules.WORLD_WEIGHT_MAGIC > FishingRules.WORLD_WEIGHT_TREASURE, "and likelier still in magic waters");
	}
}
