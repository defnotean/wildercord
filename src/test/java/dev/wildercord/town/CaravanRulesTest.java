package dev.wildercord.town;

import dev.wildercord.town.BountyRules.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaravanRulesTest {
	@Test
	void theHonouredAreOfferedMoreRunes() {
		assertEquals(2, CaravanRules.runes(Tier.STRANGER));
		assertEquals(4, CaravanRules.runes(Tier.HONOURED));
		for (Tier tier : Tier.values()) {
			if (tier.next() != null) assertTrue(CaravanRules.runes(tier.next()) >= CaravanRules.runes(tier));
		}
	}

	@Test
	void betterRunesComeAsTheInnsKnowYou() {
		assertEquals(3, CaravanRules.runeTier(Tier.STRANGER, 0.2));
		assertEquals(4, CaravanRules.runeTier(Tier.HONOURED, 0.2));
		assertEquals(4, CaravanRules.runeTier(Tier.STRANGER, 0.0));
		assertEquals(3, CaravanRules.runeTier(Tier.HONOURED, 0.99));
		assertTrue(CaravanRules.runePrice(4) > CaravanRules.runePrice(3));
	}

	@Test
	void aCaravanCampsInSightButNotUnderfoot() {
		assertTrue(CaravanRules.SPAWN_NEAR > 16 && CaravanRules.SPAWN_FAR > CaravanRules.SPAWN_NEAR);
		assertTrue(CaravanRules.APART > CaravanRules.SPAWN_FAR * 2);
		assertTrue(CaravanRules.CHANCE > 0 && CaravanRules.CHANCE < 1);
	}
}
