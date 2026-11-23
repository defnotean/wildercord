package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossHoldRulesTest {
	@Test
	void constantsMatchOneToTwoSecondHoldAndFiveSecondImmunity() {
		assertEquals(20, BossRules.BOSS_HOLD_MIN_TICKS);
		assertEquals(35, BossRules.BOSS_HOLD_MAX_TICKS);
		assertEquals(100, BossRules.BOSS_HOLD_IMMUNE_TICKS);
	}

	@Test
	void bossHoldTicksClampsPositiveRequestsBetweenTwentyAndThirtyFiveTicks() {
		assertEquals(0, BossRules.bossHoldTicks(0));
		assertEquals(0, BossRules.bossHoldTicks(-15));
		assertEquals(20, BossRules.bossHoldTicks(1));
		assertEquals(20, BossRules.bossHoldTicks(10));
		assertEquals(25, BossRules.bossHoldTicks(25));
		assertEquals(35, BossRules.bossHoldTicks(35));
		assertEquals(35, BossRules.bossHoldTicks(200));
	}

	@Test
	void holdWindowBlocksStackingAndEnforcesFiveSecondImmunityAfterBreakout() {
		BossRules.HoldWindow window = BossRules.beginBossHold(1000L, 120);
		assertEquals(1035L, window.heldUntil());
		assertEquals(1135L, window.immuneUntil());

		assertTrue(BossRules.isBossHeld(window, 1000L));
		assertFalse(BossRules.isBossHoldImmune(window, 1000L));
		assertFalse(BossRules.canHoldBoss(window, 1000L));

		assertTrue(BossRules.isBossHeld(window, 1034L));
		assertFalse(BossRules.canHoldBoss(window, 1034L));

		assertFalse(BossRules.isBossHeld(window, 1035L));
		assertTrue(BossRules.isBossHoldImmune(window, 1035L));
		assertFalse(BossRules.canHoldBoss(window, 1035L));

		assertFalse(BossRules.isBossHeld(window, 1134L));
		assertTrue(BossRules.isBossHoldImmune(window, 1134L));
		assertFalse(BossRules.canHoldBoss(window, 1134L));

		assertFalse(BossRules.isBossHeld(window, 1135L));
		assertFalse(BossRules.isBossHoldImmune(window, 1135L));
		assertTrue(BossRules.canHoldBoss(window, 1135L));
	}

	@Test
	void nullOrZeroWindowAllowsFreshHold() {
		assertTrue(BossRules.canHoldBoss(null, 0L));
		assertFalse(BossRules.isBossHeld(null, 0L));
		assertFalse(BossRules.isBossHoldImmune(null, 0L));

		BossRules.HoldWindow zero = BossRules.beginBossHold(500L, 0);
		assertEquals(500L, zero.heldUntil());
		assertEquals(500L, zero.immuneUntil());
		assertTrue(BossRules.canHoldBoss(zero, 500L));
	}
}
