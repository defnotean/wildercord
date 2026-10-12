package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SnapjawRulesTest {
	@Test
	void itSnapsOnlyAtWhatComesClose() {
		assertTrue(SnapjawRules.snaps(2, 100, 100, false));
		assertFalse(SnapjawRules.snaps(SnapjawRules.SNAP_REACH + 0.5, 100, 100, false), "too far");
		assertFalse(SnapjawRules.snaps(2, 99, 100, false), "not ready");
		assertFalse(SnapjawRules.snaps(2, 100, 100, true), "already holding on");
	}

	@Test
	void itHoldsOnTillTimeHurtOrPulledAway() {
		assertFalse(SnapjawRules.letsGo(10, 0, 1));
		assertTrue(SnapjawRules.letsGo(SnapjawRules.LATCH_TICKS, 0, 1));
		assertTrue(SnapjawRules.letsGo(10, SnapjawRules.LETGO_DAMAGE, 1));
		assertTrue(SnapjawRules.letsGo(10, 0, SnapjawRules.LATCH_SLIP + 1));
		assertTrue(SnapjawRules.worries(SnapjawRules.WORRY_EVERY));
		assertFalse(SnapjawRules.worries(0));
		assertFalse(SnapjawRules.worries(SnapjawRules.WORRY_EVERY + 1));
	}

	@Test
	void itsShellTurnsBlowsFromAboveAndBehind() {
		assertFalse(SnapjawRules.onShell(0, 1), "face on");
		assertTrue(SnapjawRules.onShell(1, 1), "from above");
		assertTrue(SnapjawRules.onShell(0, -1), "from behind");
		assertFalse(SnapjawRules.onShell(0, 0), "from the side");
		assertEquals(10, SnapjawRules.damage(10, false, false), 1e-6);
		assertEquals(10 * SnapjawRules.SHELL_FACTOR, SnapjawRules.damage(10, true, false), 1e-6);
		assertEquals(10 * SnapjawRules.WITHDRAWN_FACTOR, SnapjawRules.damage(10, false, true), 1e-6);
	}

	@Test
	void badlyHurtItPullsIn() {
		assertFalse(SnapjawRules.withdraws(30, 40, 100, 0));
		assertTrue(SnapjawRules.withdraws(10, 40, 100, 0));
		assertFalse(SnapjawRules.withdraws(10, 40, 100, 200), "not again so soon");
	}
}
