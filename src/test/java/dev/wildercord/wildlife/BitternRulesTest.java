package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BitternRulesTest {
	@Test
	void aBirdOnItsBankStrollsOnlyAlongBanks() {
		assertTrue(WildlifeRules.bitternStroll(true, true));
		assertFalse(WildlifeRules.bitternStroll(true, false), "a resting bird does not wander inland");
		assertTrue(WildlifeRules.bitternStroll(false, false), "off a bank it wanders until bank return runs");
		assertTrue(WildlifeRules.bitternStroll(false, true));
	}
}
