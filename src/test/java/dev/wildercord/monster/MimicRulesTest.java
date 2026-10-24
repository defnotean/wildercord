package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MimicRulesTest {
	@Test
	void onlySomeoneLingeringRightBesideItWakesIt() {
		assertTrue(MimicRules.wakes(1.0, 0.0, MimicRules.LINGER));
		assertFalse(MimicRules.wakes(1.0, 0.0, MimicRules.LINGER - 1), "not long enough");
		assertFalse(MimicRules.wakes(MimicRules.WAKE_REACH + 0.5, 0.0, 999), "too far off");
		assertFalse(MimicRules.wakes(1.0, 3.0, 999), "on the floor above");
	}

	@Test
	void itBitesUpCloseLashesAtMiddleReachAndLungesFurther() {
		assertEquals(MimicRules.Move.BITE, MimicRules.choose(1.5, true, true, true, true));
		assertEquals(MimicRules.Move.CHASE, MimicRules.choose(1.5, true, false, true, true), "bite not ready: it keeps close");
		assertEquals(MimicRules.Move.LASH, MimicRules.choose(4.0, true, true, true, true));
		assertEquals(MimicRules.Move.LUNGE, MimicRules.choose(4.0, true, true, false, true), "tongue resting");
		assertEquals(MimicRules.Move.LUNGE, MimicRules.choose(7.5, true, true, true, true), "past the tongue's reach");
		assertEquals(MimicRules.Move.CHASE, MimicRules.choose(4.0, false, true, true, true), "can't see its prey");
		assertEquals(MimicRules.Move.CHASE, MimicRules.choose(12.0, true, true, true, true), "too far for anything");
	}

	@Test
	void clampedItShrugsBlowsOffAndFlippedItTakesMore() {
		assertEquals(MimicRules.CLAMPED_SCALE, MimicRules.damageScale(true, false));
		assertEquals(MimicRules.FLIPPED_SCALE, MimicRules.damageScale(false, true));
		assertEquals(1.0F, MimicRules.damageScale(false, false));
		assertTrue(MimicRules.clamps(12, 40));
		assertFalse(MimicRules.clamps(11, 40));
	}

	@Test
	void itSettlesSquareToTheWorld() {
		assertEquals(0.0F, MimicRules.snapYaw(10));
		assertEquals(90.0F, MimicRules.snapYaw(80));
		assertEquals(180.0F, MimicRules.snapYaw(-170));
		assertEquals(270.0F, MimicRules.snapYaw(-95));
		assertEquals(0.0F, MimicRules.snapYaw(350));
	}

	@Test
	void aBookTamesItAThirdOfTheTime() {
		assertTrue(MimicRules.tames(0.0F));
		assertTrue(MimicRules.tames(0.33F));
		assertFalse(MimicRules.tames(0.34F));
		assertFalse(MimicRules.tames(0.99F));
	}

	@Test
	void aTamedOneFollowsItsOwnerUnlessToldToStay() {
		assertEquals(MimicRules.Heel.STAY, MimicRules.heel(4.0, false, false), "near enough: it keeps its spot");
		assertEquals(MimicRules.Heel.WALK, MimicRules.heel(MimicRules.FOLLOW_START + 1, false, false));
		assertEquals(MimicRules.Heel.WALK, MimicRules.heel(4.0, false, true), "once following it comes right up");
		assertEquals(MimicRules.Heel.STAY, MimicRules.heel(2.0, false, true), "beside them it stops");
		assertEquals(MimicRules.Heel.BLINK, MimicRules.heel(MimicRules.FOLLOW_BLINK + 1, false, true), "far behind it turns up beside them");
		assertEquals(MimicRules.Heel.STAY, MimicRules.heel(40.0, true, false), "told to stay, it stays");
	}

	@Test
	void itsBellyOpensOnlyForItsOwnerNearby() {
		assertTrue(MimicRules.opensFor(true, true, 3.0));
		assertFalse(MimicRules.opensFor(true, false, 3.0), "a stranger");
		assertFalse(MimicRules.opensFor(true, true, MimicRules.STORAGE_REACH + 0.5), "walked off");
		assertFalse(MimicRules.opensFor(false, true, 1.0), "dead");
	}
}
