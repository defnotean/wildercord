package dev.wildercord.spell;

import dev.wildercord.aura.AuraRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 0.12 Tempering: condensing, farm decay, boss credit and re-measuring old saves. */
class CondenseRulesTest {
	@Test
	void onlyATenthCondensesWithoutAHostileHit() {
		assertEquals(10, CondenseRules.immediate(100), 1e-4);
		assertEquals(90, CondenseRules.pending(100), 1e-4);
		assertEquals(0, CondenseRules.immediate(Float.NaN));
		assertEquals(0, CondenseRules.pending(-5));
		assertEquals(CondenseRules.PENDING_CAP, CondenseRules.addPending(590, 90));
	}

	@Test
	void aFarmsStreamOfKillsMostlyDoesNotCount() {
		assertEquals(1.0, CondenseRules.killChance(0));
		assertEquals(1.0, CondenseRules.killChance(CondenseRules.KILL_FREE - 1));
		assertEquals(CondenseRules.KILL_FARMED, CondenseRules.killChance(CondenseRules.KILL_FREE));
	}

	@Test
	void standingByABossEarnsNothing() {
		assertFalse(CondenseRules.bossCredit(0, 300));
		assertFalse(CondenseRules.bossCredit(5, 300));
		assertTrue(CondenseRules.bossCredit(6, 300));
		assertFalse(CondenseRules.bossCredit(10, 0));
	}

	@Test
	void oldHeartsKeepOnlyWhatTheirManaPaysFor() {
		assertEquals(0, CondenseRules.retemper(0, 0));
		assertEquals(8, CondenseRules.retemper(8, Circles.condenseNeeded(8)));
		// A heart at the old 8th's 80,000 keeps five circles under the new thresholds.
		assertEquals(5, CondenseRules.retemper(8, 80000));
		assertEquals(3, CondenseRules.retemper(3, 1_000_000), "never gains circles");
		assertEquals(0, CondenseRules.retemper(2, 500));
	}

	@Test
	void oldAurasSettleToWhatTheirExperienceReaches() {
		assertEquals(AuraRules.GLOW, AuraRules.retemper(AuraRules.FLOW, 150, AuraRules::threshold));
		assertEquals(AuraRules.EDGE, AuraRules.retemper(AuraRules.SOVEREIGN, 4500, AuraRules::threshold));
		assertEquals(AuraRules.SOVEREIGN, AuraRules.retemper(AuraRules.SOVEREIGN, 13500, AuraRules::threshold));
		assertEquals(AuraRules.GLOW, AuraRules.retemper(AuraRules.GLOW, 0, AuraRules::threshold));
	}

	@Test
	void theMomentIsHeldToOneAndAHalf() {
		assertEquals(1.5, AuraRules.moment(3.0));
		assertEquals(1.2, AuraRules.moment(1.2));
	}
}
