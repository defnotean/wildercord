package dev.wildercord.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElixirRulesTest {
	@Test
	void noElixirChangesNothing() {
		assertEquals(1.0, ElixirRules.regen(0, 0), 1e-9);
		assertEquals(1.0, ElixirRules.max(0, 0), 1e-9);
		assertEquals(1.0, ElixirRules.condense(0), 1e-9);
		assertEquals(1.0, ElixirRules.cost(0), 1e-9);
	}

	@Test
	void everyElixirPaysForWhatItGives() {
		// Torrent: faster, but a smaller pool.
		assertTrue(ElixirRules.regen(1, 0) > 1 && ElixirRules.max(1, 0) < 1);
		// Deep Well: a bigger pool, but slower.
		assertTrue(ElixirRules.max(0, 1) > 1 && ElixirRules.regen(0, 1) < 1);
		// Condensing: more circle progress, but dearer spells.
		assertTrue(ElixirRules.condense(1) > 1 && ElixirRules.cost(1) > 1);
	}

	@Test
	void levelsStackToTwoAndNoFurther() {
		assertEquals(3.0, ElixirRules.regen(2, 0), 1e-9);
		assertEquals(ElixirRules.regen(2, 0), ElixirRules.regen(9, 0), 1e-9);
		assertEquals(0.5, ElixirRules.max(2, 0), 1e-9);
		assertEquals(1.8, ElixirRules.max(0, 2), 1e-9);
		assertEquals(2.0, ElixirRules.condense(5), 1e-9);
		assertEquals(1.0, ElixirRules.cost(-1), 1e-9);
	}

	@Test
	void deepWellNeverStopsRegenerationOutright() {
		assertTrue(ElixirRules.regen(0, ElixirRules.MAX_LEVEL) > 0);
	}

	@Test
	void torrentAndDeepWellTogetherRoughlyCancel() {
		assertEquals(1.2, ElixirRules.regen(1, 1), 1e-9);
		assertEquals(1.05, ElixirRules.max(1, 1), 1e-9);
	}
}
