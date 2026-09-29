package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers of the fused effects of void, arcane and time, against their rune descriptions. */
class FusedVoidRulesTest {
	@Test
	void entropyClimbsByHalfAPointFromOneToThree() {
		assertEquals(5, FusedVoidRules.entropyWounds(1.0));
		double[] expected = {1.0, 1.5, 2.0, 2.5, 3.0};
		double total = 0;
		for (int step = 1; step <= 5; step++) {
			assertEquals(expected[step - 1], FusedVoidRules.entropyWound(step), 1e-9);
			total += FusedVoidRules.entropyWound(step);
		}
		assertEquals(10.0, total, 1e-9);
		// Wound on (or Extended), it stays at its peak.
		assertEquals(3.0, FusedVoidRules.entropyWound(9), 1e-9);
		assertEquals(10, FusedVoidRules.entropyWounds(2.0));
		assertEquals(1, FusedVoidRules.entropyWounds(0.01));
	}

	@Test
	void devourBitesHarderTheEmptierItsPrey() {
		assertEquals(5, FusedVoidRules.devourDamage(20, 20), 1e-9);
		assertEquals(5, FusedVoidRules.devourDamage(19, 20), 1e-9);
		assertEquals(8, FusedVoidRules.devourDamage(14, 20), 1e-9);
		assertEquals(10, FusedVoidRules.devourDamage(1, 20), 1e-9);
		assertEquals(10, FusedVoidRules.devourDamage(0, 20), 1e-9);
	}

	@Test
	void reckoningHealsHalfOfWhatComesDueUpToSix() {
		assertEquals(0, FusedVoidRules.reckoningHeal(0), 1e-9);
		assertEquals(3, FusedVoidRules.reckoningHeal(6), 1e-9);
		assertEquals(6, FusedVoidRules.reckoningHeal(12), 1e-9);
		assertEquals(6, FusedVoidRules.reckoningHeal(40), 1e-9);
	}

	@Test
	void timestealTakesAtMostItsCap() {
		assertEquals(200, FusedVoidRules.stolenTicks(200, 600));
		assertEquals(600, FusedVoidRules.stolenTicks(1200, 600));
		assertEquals(600, FusedVoidRules.stolenTicks(Integer.MAX_VALUE, 600));
		// A creature keeps nothing; a boss keeps what wasn't taken, and its endless effects are never touched.
		assertEquals(0, FusedVoidRules.keptTicks(1200, 600, false));
		assertEquals(600, FusedVoidRules.keptTicks(1200, 600, true));
		assertEquals(0, FusedVoidRules.keptTicks(200, 200, true));
	}

	@Test
	void hemomancyGainsOneForEveryTwoMissing() {
		assertEquals(0, FusedVoidRules.hemomancyBonus(20, 20));
		assertEquals(0, FusedVoidRules.hemomancyBonus(20, 19));
		assertEquals(1, FusedVoidRules.hemomancyBonus(20, 18));
		assertEquals(3, FusedVoidRules.hemomancyBonus(20, 13));
		assertEquals(6, FusedVoidRules.hemomancyBonus(20, 8));
		assertEquals(6, FusedVoidRules.hemomancyBonus(20, 1));
		assertEquals(0, FusedVoidRules.hemomancyBonus(20, 25));
	}

	@Test
	void reckoningIsHalfTheLedgerAtMostTwelve() {
		assertEquals(0, FusedVoidRules.reckoningDue(0, 1), 1e-9);
		assertEquals(6, FusedVoidRules.reckoningDue(12, 1), 1e-9);
		assertEquals(12, FusedVoidRules.reckoningDue(24, 1), 1e-9);
		assertEquals(12, FusedVoidRules.reckoningDue(100, 1), 1e-9);
		assertEquals(18, FusedVoidRules.reckoningDue(100, 1.5), 1e-9);
	}

	@Test
	void singularityFlingsSofterTowardAWall() {
		double open = FusedVoidRules.flingStrength(1, 4);
		assertTrue(FusedVoidRules.flingStrength(1, 1) < open);
		assertTrue(FusedVoidRules.flingStrength(1, 0) > 0);
		assertEquals(open, FusedVoidRules.flingStrength(1, 10), 1e-9);
	}

	@Test
	void prismaticBurstGainsThreeAMark() {
		assertEquals(4, FusedVoidRules.prismaticDamage(0), 1e-9);
		assertEquals(13, FusedVoidRules.prismaticDamage(3), 1e-9);
		assertEquals(22, FusedVoidRules.prismaticDamage(6), 1e-9);
		assertEquals(22, FusedVoidRules.prismaticDamage(9), 1e-9);
	}

	@Test
	void chronoshiftTakesThreeSecondsNeverPastNow() {
		assertEquals(60, FusedVoidRules.chronoshiftTicks(1.0));
		assertEquals(120, FusedVoidRules.chronoshiftTicks(5.0));
		assertEquals(30, FusedVoidRules.chronoshiftTicks(0.1));
		assertEquals(1140, FusedVoidRules.shifted(1200, 1000, 60));
		assertEquals(1000, FusedVoidRules.shifted(1030, 1000, 60));
		// A spell already ready stays as it was.
		assertEquals(900, FusedVoidRules.shifted(900, 1000, 60));
	}
}
