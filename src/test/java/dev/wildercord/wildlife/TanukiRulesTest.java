package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TanukiRulesTest {
	@Test
	void runningCloseStartlesItIntoABush() {
		assertTrue(TanukiRules.startled(3, true, -1));
		assertFalse(TanukiRules.startled(3, false, -1), "walking past");
		assertFalse(TanukiRules.startled(TanukiRules.STARTLE_RANGE + 1, true, -1), "too far");
		assertFalse(TanukiRules.startled(3, true, 10), "just turned back");
		assertTrue(TanukiRules.startled(3, true, TanukiRules.TURN_COOLDOWN));
	}

	@Test
	void itStaysABushWhileThreatened() {
		assertTrue(TanukiRules.staysBush(10, false));
		assertFalse(TanukiRules.staysBush(TanukiRules.BUSH_TICKS, false));
		assertTrue(TanukiRules.staysBush(TanukiRules.BUSH_TICKS, true));
	}

	@Test
	void itDrumsUnderTheFullMoon() {
		assertTrue(TanukiRules.drums(true, 0, false));
		assertFalse(TanukiRules.drums(false, 0, false), "day");
		assertFalse(TanukiRules.drums(true, 3, false), "not full");
		assertFalse(TanukiRules.drums(true, 0, true), "hiding");
		assertNotEquals(TanukiRules.beatPitch(0), TanukiRules.beatPitch(1));
	}

	@Test
	void itSharesItsLeafWithAFriendWhoCrouches() {
		assertTrue(TanukiRules.tricks(true, TanukiRules.TRICK_CROUCH, 1, -1));
		assertFalse(TanukiRules.tricks(false, TanukiRules.TRICK_CROUCH, 1, -1), "not trusted");
		assertFalse(TanukiRules.tricks(true, 5, 1, -1), "not long enough");
		assertFalse(TanukiRules.tricks(true, TanukiRules.TRICK_CROUCH, 4, -1), "too far");
		assertFalse(TanukiRules.tricks(true, TanukiRules.TRICK_CROUCH, 1, 100), "too soon");
		assertTrue(TanukiRules.trusts(0));
		assertFalse(TanukiRules.trusts(TanukiRules.TRUST_TICKS));
	}
}
