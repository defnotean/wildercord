package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelverRulesTest {
	@Test
	void itDigsOnlySoftShovelGround() {
		assertTrue(DelverRules.digs(true, 0.5F, false), "dirt");
		assertTrue(DelverRules.digs(true, 0.6F, false), "grass, gravel, clay");
		assertFalse(DelverRules.digs(false, 0.5F, false), "not a shovel's: a pick's or an axe's");
		assertFalse(DelverRules.digs(true, 1.5F, false), "too hard");
		assertFalse(DelverRules.digs(true, -1F, false), "unbreakable");
		assertFalse(DelverRules.digs(true, 0.5F, true), "holds something");
	}

	@Test
	void itReachesAheadLevelOrTippedDownOrUp() {
		double[] south = DelverRules.reach(0, 0);
		assertEquals(DelverRules.REACH, south[2], 1e-9);
		assertEquals(0, south[1], 1e-9);
		double[] east = DelverRules.reach(-90, 20);
		assertEquals(DelverRules.REACH, east[0], 1e-9, "a glance down still tunnels level");
		double[] down = DelverRules.reach(0, 60);
		assertTrue(down[1] < 0 && down[2] > 0, "looking well down digs down and ahead");
		double[] up = DelverRules.reach(0, -60);
		assertTrue(up[1] > 0, "looking well up digs up");
		assertEquals(DelverRules.REACH, Math.sqrt(down[0] * down[0] + down[1] * down[1] + down[2] * down[2]), 1e-9, "always the same reach");
	}
}
