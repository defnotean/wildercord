package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoopTribulationRulesTest {
	@Test
	void aLoneCasterFacesTheSameStormAsBefore() {
		for (int n = 5; n <= 20; n += 5) {
			for (int w = 1; w <= TribulationRules.waves(n); w++) {
				assertEquals(TribulationRules.waveSize(n, w), CoopTribulationRules.waveSize(TribulationRules.waveSize(n, w), 0));
			}
			assertEquals(TribulationRules.healthBonus(n), CoopTribulationRules.healthBonus(TribulationRules.healthBonus(n), 0), 1e-9);
		}
	}

	@Test
	void everyAllyMakesTheStormBigger() {
		for (int a = 1; a <= CoopTribulationRules.MAX_ALLIES; a++) {
			assertTrue(CoopTribulationRules.waveSize(4, a) > CoopTribulationRules.waveSize(4, a - 1));
			assertTrue(CoopTribulationRules.healthBonus(1.0, a) > CoopTribulationRules.healthBonus(1.0, a - 1));
		}
		// A full party of four fights a storm two and a half times the size, each monster tougher.
		assertEquals(10, CoopTribulationRules.waveSize(4, 3));
		assertEquals(1.75, CoopTribulationRules.healthBonus(1.0, 3), 1e-9);
	}

	@Test
	void onlyThreeAlliesCount() {
		assertEquals(3, CoopTribulationRules.allies(7));
		assertEquals(0, CoopTribulationRules.allies(-1));
		assertEquals(CoopTribulationRules.waveSize(4, 3), CoopTribulationRules.waveSize(4, 9));
	}

	@Test
	void anAllysShareIsHalfTheCrystals() {
		assertEquals(0, CoopTribulationRules.allyCrystals(0));
		assertEquals(1, CoopTribulationRules.allyCrystals(1));
		assertEquals(2, CoopTribulationRules.allyCrystals(4));
		assertEquals(3, CoopTribulationRules.allyCrystals(5));
	}
}
