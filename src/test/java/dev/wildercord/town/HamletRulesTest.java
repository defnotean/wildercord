package dev.wildercord.town;

import dev.wildercord.town.BountyRules.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HamletRulesTest {
	@Test
	void aHamletGivesOnlySoMuchADay() {
		assertEquals(1, HamletRules.earned(0, 1));
		assertEquals(1, HamletRules.earned(HamletRules.DAILY - 1, 5));
		assertEquals(0, HamletRules.earned(HamletRules.DAILY, 1));
	}

	@Test
	void favourGrowsWithStanding() {
		assertEquals(-1, HamletRules.favour(Tier.STRANGER));
		assertEquals(0, HamletRules.favour(Tier.KNOWN));
		assertEquals(2, HamletRules.favour(Tier.HONOURED));
	}

	@Test
	void hamletsHaveSteadyNames() {
		assertEquals(HamletRules.name(12345), HamletRules.name(12345));
		assertFalse(HamletRules.name(1).isEmpty());
		long distinct = java.util.stream.LongStream.range(0, 200).mapToObj(HamletRules::name).distinct().count();
		assertTrue(distinct > 100, "names vary: " + distinct);
	}
}
