package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GulperPitcherRulesTest {
	@Test
	void creepingUpOnItGetsYouMuchCloser() {
		assertTrue(GulperPitcherRules.notices(6.0, false));
		assertFalse(GulperPitcherRules.notices(8.0, false));
		assertFalse(GulperPitcherRules.notices(4.0, true), "crouching past");
		assertTrue(GulperPitcherRules.notices(2.5, true), "right at its mouth");
		assertTrue(GulperPitcherRules.noticeRange(true) < GulperPitcherRules.noticeRange(false));
	}

	@Test
	void itBitesAtItsMouthAndLashesFurtherOut() {
		assertEquals(1, GulperPitcherRules.attack(1.5, true, 100, 0, 0), "at its mouth: bite");
		assertEquals(0, GulperPitcherRules.attack(1.5, true, 100, 200, 0), "at its mouth, bite not ready: it waits, no vine");
		assertEquals(2, GulperPitcherRules.attack(5.0, true, 100, 0, 0), "further out: the vine");
		assertEquals(0, GulperPitcherRules.attack(5.0, false, 100, 0, 0), "no clear line for the vine");
		assertEquals(0, GulperPitcherRules.attack(5.0, true, 100, 0, 200), "vine not ready");
		assertEquals(0, GulperPitcherRules.attack(9.0, true, 100, 0, 0), "out of reach");
	}

	@Test
	void theVineHaulsHarderFromFurtherButNotWithoutLimit() {
		assertEquals(0, GulperPitcherRules.pull(1.0), 1e-9, "already at its mouth");
		assertTrue(GulperPitcherRules.pull(3.0) > 0);
		assertTrue(GulperPitcherRules.pull(6.0) > GulperPitcherRules.pull(3.0));
		assertTrue(GulperPitcherRules.pull(100.0) <= GulperPitcherRules.PULL_MAX);
	}

	@Test
	void fireTakesItHarder() {
		assertEquals(8.0F, GulperPitcherRules.damage(4.0F, true), 1e-6);
		assertEquals(4.0F, GulperPitcherRules.damage(4.0F, false), 1e-6);
	}
}
