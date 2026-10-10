package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TribulationRulesTest {
	@Test
	void everyFifthCircleIsATribulation() {
		for (int n = 1; n <= Circles.MAX; n++) {
			assertEquals(n % 5 == 0, TribulationRules.tribulation(n), "circle " + n);
		}
		assertFalse(TribulationRules.tribulation(0));
		assertFalse(TribulationRules.tribulation(Circles.MAX + 5));
	}

	@Test
	void laterTribulationsAreHarder() {
		assertEquals(3, TribulationRules.waves(5));
		assertEquals(6, TribulationRules.waves(20));
		for (int n = 10; n <= 20; n += 5) {
			assertTrue(TribulationRules.waves(n) > TribulationRules.waves(n - 5));
			assertTrue(TribulationRules.waveSize(n, 1) > TribulationRules.waveSize(n - 5, 1));
			assertTrue(TribulationRules.healthBonus(n) > TribulationRules.healthBonus(n - 5));
		}
		assertTrue(TribulationRules.waveSize(5, 3) > TribulationRules.waveSize(5, 1));
	}

	@Test
	void adeptsNeverOutnumberTheWave() {
		for (int n = 5; n <= 20; n += 5) {
			for (int w = 1; w <= TribulationRules.waves(n); w++) {
				assertTrue(TribulationRules.adepts(n, w) <= TribulationRules.waveSize(n, w));
				assertTrue(TribulationRules.adepts(n, w) >= 0);
			}
		}
		assertTrue(TribulationRules.lastWave(5, 3));
		assertFalse(TribulationRules.lastWave(5, 2));
	}

	@Test
	void spoilsGrowWithTheTier() {
		assertEquals(2, TribulationRules.spoilRunes(5));
		assertEquals(8, TribulationRules.spoilRunes(20));
		assertEquals(2, TribulationRules.spoilRuneTier(5, 0.0));
		assertEquals(3, TribulationRules.spoilRuneTier(10, 0.1));
		assertEquals(4, TribulationRules.spoilRuneTier(20, 0.1));
		assertTrue(TribulationRules.spoilXp(20) > TribulationRules.spoilXp(5));
	}

	@Test
	void eachTribulationCircleLeavesAScar() {
		assertEquals(0, TribulationRules.scars(4));
		assertEquals(1, TribulationRules.scars(5));
		assertEquals(3, TribulationRules.scars(19));
		assertEquals(4, TribulationRules.scars(20));
		assertEquals(4, TribulationRules.scars(99));
		assertEquals(0, TribulationRules.scars(-3));
	}

	@Test
	void fromTheTenthCircleTheLastFoeWearsYourFace() {
		assertFalse(TribulationRules.rival(5));
		assertTrue(TribulationRules.rival(10));
		assertTrue(TribulationRules.rival(20));
		assertFalse(TribulationRules.rival(11), "no tribulation, no rival");
		assertEquals(40, TribulationRules.shadowHealth(10, 20));
		assertEquals(120, TribulationRules.shadowHealth(15, 40));
		assertEquals(160, TribulationRules.shadowHealth(20, 40));
		assertEquals(40, TribulationRules.shadowHealth(10, 4), "never a pushover");
	}
}
