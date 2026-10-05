package dev.wildercord.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManaCrystalRulesTest {
	@Test
	void oldTenCrystalSaveCanGrowToOneHundredWithoutLosingProgress() {
		assertEquals(10, ManaCrystalRules.count(10));
		assertEquals(100, ManaCrystalRules.bonus(10));
		assertEquals(110, ManaCrystalRules.bonus(11));
		assertEquals(990, ManaCrystalRules.bonus(99));
		assertEquals(1000, ManaCrystalRules.bonus(100));
		assertEquals(100, ManaCrystalRules.MAX_CRYSTALS);
	}

	@Test
	void malformedSavedCountsCannotOverflowMana() {
		assertEquals(0, ManaCrystalRules.bonus(Integer.MIN_VALUE));
		assertEquals(0, ManaCrystalRules.bonus(-1));
		assertEquals(1000, ManaCrystalRules.bonus(Integer.MAX_VALUE));
	}
}
