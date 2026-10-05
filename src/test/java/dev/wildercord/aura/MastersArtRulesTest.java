package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MastersArtRulesTest {
	@Test
	void requestsNeverClampIntoAnotherMove() {
		assertNull(MastersArtRules.move(-1));
		assertNull(MastersArtRules.move(3));
		assertNull(MastersArtRules.move(Integer.MAX_VALUE));
		assertSame(MastersArtRules.SPELLCUT, MastersArtRules.move(0));
	}

	@Test
	void eligibilityAndExactCooldownAreAuthoritative() {
		var move = MastersArtRules.RISING_BREAK;
		assertFalse(MastersArtRules.canPay(move, 3, 160, 100, 100));
		assertFalse(MastersArtRules.canPay(move, 4, 19.99, 100, 100));
		assertFalse(MastersArtRules.canPay(move, 4, 160, 99, 100));
		assertTrue(MastersArtRules.canPay(move, 4, 20, 100, 100));
		assertFalse(MastersArtRules.canPay(move, 5, Double.NaN, 100, 100));
		assertFalse(MastersArtRules.canPay(move, 5, Double.POSITIVE_INFINITY, 100, 100));
	}

	@Test
	void everyMoveHasReadableWindupRecoveryAndBoundedTargets() {
		for (int i = 0; i < 3; i++) {
			var move = MastersArtRules.move(i);
			assertTrue(move.windup() >= 4);
			assertTrue(move.recovery() >= 12);
			assertTrue(move.rest() > move.windup() + move.recovery());
			assertTrue(move.targets() >= 1 && move.targets() <= 3);
			assertTrue(move.reach() <= 5);
		}
	}
	@Test
	void fullBlocksPreserveWindupButAbsorbedAndPartialHitsInterrupt() {
		assertFalse(MastersArtRules.interrupts(8, 0, true));
		assertTrue(MastersArtRules.interrupts(8, 2, true));
		assertTrue(MastersArtRules.interrupts(8, 0, false));
		assertFalse(MastersArtRules.interrupts(0, 0, false));
		assertFalse(MastersArtRules.interrupts(Float.NaN, 2, false));
	}

}
