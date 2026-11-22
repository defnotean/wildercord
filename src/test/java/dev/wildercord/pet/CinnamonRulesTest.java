package dev.wildercord.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CinnamonRulesTest {
	@Test
	void grandBowScaleAndMagicBuffsHoldPermanently() {
		assertEquals(1.0D, CinnamonRules.effectiveScale(false, 1.0D, false));
		assertEquals(2.0D, CinnamonRules.effectiveScale(false, 2.0D, true));
		assertEquals(3.0D, CinnamonRules.effectiveScale(true, 1.0D, false));
		assertEquals(3.0D, CinnamonRules.effectiveScale(true, 2.5D, true));

		assertEquals(90, CinnamonRules.castIntervalTicks(false));
		assertEquals(55, CinnamonRules.castIntervalTicks(true));
		assertEquals(16.0D, CinnamonRules.spellRange(false));
		assertEquals(24.0D, CinnamonRules.spellRange(true));
		assertEquals(1.0F, CinnamonRules.spellPower(false));
		assertEquals(1.6F, CinnamonRules.spellPower(true));
	}

	@Test
	void partyLickHealingThresholdsAndPriority() {
		assertTrue(CinnamonRules.needsLickHeal(10.0F, 20.0F));
		assertFalse(CinnamonRules.needsLickHeal(14.0F, 20.0F));
		assertFalse(CinnamonRules.needsLickHeal(0.0F, 20.0F));
		assertFalse(CinnamonRules.needsLickHeal(20.0F, 20.0F));

		assertEquals(6.0F, CinnamonRules.lickHealAmount(false, false));
		assertEquals(10.0F, CinnamonRules.lickHealAmount(true, false));
		assertEquals(10.0F, CinnamonRules.lickHealAmount(false, true));

		assertTrue(CinnamonRules.isMoreInjured(6.0F, 20.0F, 12.0F, 20.0F));
		assertFalse(CinnamonRules.isMoreInjured(12.0F, 20.0F, 6.0F, 20.0F));
	}

	@Test
	void harnessCapacityReservesSeatForOwner() {
		assertFalse(CinnamonRules.canAcceptRider(false, 0, true, false));
		assertTrue(CinnamonRules.canAcceptRider(true, 0, false, false));
		assertTrue(CinnamonRules.canAcceptRider(true, 2, false, false));
		assertFalse(CinnamonRules.canAcceptRider(true, 3, false, false));
		assertTrue(CinnamonRules.canAcceptRider(true, 3, true, false));
		assertFalse(CinnamonRules.canAcceptRider(true, 4, true, true));

		var seat0 = CinnamonRules.seatOffset(0, 4);
		var seat2 = CinnamonRules.seatOffset(2, 4);
		var seat3 = CinnamonRules.seatOffset(3, 4);
		assertTrue(seat0.forward() > seat2.forward());
		assertEquals(-seat2.side(), seat3.side());
	}

	@Test
	void flightVelocitiesRespondToClimbPitchAndGrandBow() {
		assertTrue(CinnamonRules.flightSpeed(true) > CinnamonRules.flightSpeed(false));
		assertTrue(CinnamonRules.verticalFlightVelocity(true, false, 0.0F, 0.0D, false) > 0.25D);
		assertTrue(CinnamonRules.verticalFlightVelocity(true, false, 0.0F, 0.0D, true)
			> CinnamonRules.verticalFlightVelocity(true, false, 0.0F, 0.0D, false));
		assertTrue(CinnamonRules.verticalFlightVelocity(false, false, -45.0F, 1.0D, false) > 0.2D);
		assertTrue(CinnamonRules.verticalFlightVelocity(false, false, 45.0F, 1.0D, false) < -0.2D);
		assertEquals(0.0D, CinnamonRules.verticalFlightVelocity(false, false, 0.0F, 0.0D, false));
	}

	@Test
	void hitPauseSpansTwoToThreeSecondsAndIgnoresAllies() {
		assertEquals(40, CinnamonRules.rollHitPauseTicks(0));
		assertEquals(60, CinnamonRules.rollHitPauseTicks(20));
		assertEquals(40, CinnamonRules.rollHitPauseTicks(-5));
		assertEquals(60, CinnamonRules.rollHitPauseTicks(99));

		assertTrue(CinnamonRules.isHitPaused(100L, 145L));
		assertFalse(CinnamonRules.isHitPaused(145L, 145L));

		assertTrue(CinnamonRules.shouldPauseFromAttack(false, false, false));
		assertFalse(CinnamonRules.shouldPauseFromAttack(true, false, false));
		assertFalse(CinnamonRules.shouldPauseFromAttack(false, true, false));
		assertFalse(CinnamonRules.shouldPauseFromAttack(false, false, true));
	}
}
