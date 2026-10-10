package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GauntletRulesTest {
	@Test
	void theGauntletSendsEverySixteenMastersOnce() {
		assertEquals(16, GauntletRules.schools().size());
		List<Integer> order = GauntletRules.order(42L);
		assertEquals(16, new HashSet<>(order).size());
		assertEquals(new HashSet<>(GauntletRules.schools()), new HashSet<>(order));
		assertEquals(order, GauntletRules.order(42L), "the same seed, the same order");
		assertNotEquals(order, GauntletRules.order(43L), "another run, another order");
	}

	@Test
	void itOpensOnlyOnceEveryMasterIsBeaten() {
		assertFalse(GauntletRules.eligible(0));
		assertEquals(16, GauntletRules.missing(0));
		int all = MasterVictoryRules.ALL;
		assertTrue(GauntletRules.eligible(all));
		assertEquals(0, GauntletRules.missing(all));
		int lowest = Integer.lowestOneBit(all);
		assertFalse(GauntletRules.eligible(all & ~lowest));
		assertEquals(1, GauntletRules.missing(all & ~lowest));
	}

	@Test
	void runsAreTimedAgainstTheBest() {
		assertTrue(GauntletRules.better(0, 900));
		assertTrue(GauntletRules.better(900, 899));
		assertFalse(GauntletRules.better(900, 900));
		assertEquals("15:00", GauntletRules.time(900));
		assertEquals("1:05", GauntletRules.time(65));
	}
}
