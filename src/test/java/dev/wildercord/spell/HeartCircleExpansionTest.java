package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Progression, save compatibility and bounds for the twenty-circle heart. */
class HeartCircleExpansionTest {
	@Test
	void firstEightThresholdsAndMilestonesStayCompatible() {
		int[] original = {0, 600, 2000, 5000, 10000, 18000, 30000, 50000, 80000};
		for (int n = 0; n < original.length; n++) {
			assertEquals(original[n], Circles.condenseNeeded(n));
			assertEquals(n, Circles.count(n));
		}
		assertEquals(8, Circles.ARCHMAGE);
		assertEquals(2, Passives.slots(20));
	}

	@Test
	void twelveLaterCirclesHaveRisingCostsAndAchievableSoloBreakthroughs() {
		assertEquals(20, Circles.MAX);
		assertEquals(120000, Circles.condenseNeeded(9));
		assertEquals(1220000, Circles.condenseNeeded(20));
		for (int n = 9; n <= Circles.MAX; n++) {
			assertTrue(Circles.condenseNeeded(n) > Circles.condenseNeeded(n - 1));
			assertFalse(Circles.requirements(n).isEmpty());
			for (var need : Circles.requirements(n)) {
				assertTrue(need.amount() > 0);
				switch (need.need()) {
					case RUNES -> assertTrue(need.amount() <= Runes.all().stream().filter(r -> !Runes.innate(r)).count());
					case REACTIONS -> assertTrue(need.amount() <= Feats.REACTIONS.size());
					case SECRETS -> assertTrue(need.amount() <= Secrets.ALL.size());
					case FEAT -> {
						assertTrue(Feats.FEATS.stream().anyMatch(f -> f.id().equals(need.feat())));
						assertFalse(Feats.OPTIONAL.contains(need.feat()), "No multiplayer or random innate lock");
					}
					default -> { }
				}
			}
		}
	}

	@Test
	void twentyCircleBenefitsStayLinearWithoutRepeatingPerks() {
		assertEquals(300, Circles.MAX * Circles.MANA_PER_CIRCLE);
		assertEquals(10, Circles.MAX * Circles.REGEN_PER_CIRCLE);
		assertEquals(1.6, Circles.power(0, 20, false), 1e-9);
		assertEquals(1.6 * 1.3, Circles.power(0, 20, true), 1e-9);
		assertEquals(Circles.cost(3, 8), Circles.cost(3, 20), 1e-9);
		assertEquals(Circles.cooldown(3, 8), Circles.cooldown(3, 20), 1e-9);
	}

	@Test
	void malformedSavedCountsCannotGrantUnboundedBenefits() {
		assertEquals(0, Circles.count(Integer.MIN_VALUE));
		assertEquals(20, Circles.count(Integer.MAX_VALUE));
		assertEquals(1, Circles.power(0, Integer.MIN_VALUE, true), 1e-9);
		assertEquals(1.6, Circles.power(0, Integer.MAX_VALUE, false), 1e-9);
		assertEquals(0, Circles.condenseNeeded(-1));
		assertEquals(Circles.condenseNeeded(20), Circles.condenseNeeded(Integer.MAX_VALUE));
	}

	@Test
	void condensedManaSaturatesWithoutLosingLifetimeProgress() {
		assertEquals(80000, Circles.addCondensed(80000, 0));
		assertEquals(120000, Circles.addCondensed(80000, 40000));
		assertEquals(Integer.MAX_VALUE, Circles.addCondensed(Integer.MAX_VALUE - 1, 2000));
		assertEquals(Integer.MAX_VALUE, Circles.addCondensed(Integer.MAX_VALUE, Integer.MAX_VALUE));
		assertEquals(50, Circles.addCondensed(-1, 50));
		assertEquals(50, Circles.addCondensed(50, -1));
	}

	@Test
	void allTwentyRingsFitAroundTheHeart() {
		assertEquals(0.3, Circles.ringRadius(1), 1e-9);
		assertEquals(0.93, Circles.ringRadius(8), 1e-9);
		assertEquals(1.35, Circles.ringRadius(20), 1e-9);
		for (int n = 2; n <= Circles.MAX; n++) assertTrue(Circles.ringRadius(n) > Circles.ringRadius(n - 1));
		assertEquals(Circles.ringRadius(1), Circles.ringRadius(Integer.MIN_VALUE));
		assertEquals(Circles.ringRadius(20), Circles.ringRadius(Integer.MAX_VALUE));
	}

	@Test
	void laterOrdinalsHandleTheTeens() {
		assertEquals("9th", Circles.ordinal(9));
		assertEquals("11th", Circles.ordinal(11));
		assertEquals("12th", Circles.ordinal(12));
		assertEquals("13th", Circles.ordinal(13));
		assertEquals("20th", Circles.ordinal(20));
	}
}
