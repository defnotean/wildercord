package dev.wildercord.cast;

import dev.wildercord.cast.PracticeRules.Refusal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PracticeRulesTest {
	private static final long NOW = 100_000;

	@Test
	void aSettledPlayerMayEnter() {
		assertEquals(Refusal.NONE, PracticeRules.refusal(false, false, true, false, false, NOW, PracticeRules.NEVER));
		assertEquals(Refusal.NONE, PracticeRules.refusal(false, false, true, false, false, NOW, NOW - PracticeRules.COOLDOWN_TICKS));
	}

	@Test
	void theRoomIsNoEscapeHatch() {
		assertEquals(Refusal.DUEL, PracticeRules.refusal(true, false, true, false, false, NOW, PracticeRules.NEVER));
		assertEquals(Refusal.HURT, PracticeRules.refusal(false, true, true, false, false, NOW, PracticeRules.NEVER));
		assertEquals(Refusal.FALLING, PracticeRules.refusal(false, false, false, false, false, NOW, PracticeRules.NEVER));
		assertEquals(Refusal.BURNING, PracticeRules.refusal(false, false, true, true, false, NOW, PracticeRules.NEVER));
		assertEquals(Refusal.RIDING, PracticeRules.refusal(false, false, false, false, true, NOW, PracticeRules.NEVER));
	}

	@Test
	void enteringAgainWaitsForTheCooldown() {
		assertEquals(Refusal.COOLDOWN, PracticeRules.refusal(false, false, true, false, false, NOW, NOW - 1));
		assertEquals(Refusal.COOLDOWN, PracticeRules.refusal(false, false, true, false, false, NOW, NOW - PracticeRules.COOLDOWN_TICKS + 1));
	}
}
