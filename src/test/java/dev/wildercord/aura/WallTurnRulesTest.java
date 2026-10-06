package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WallTurnRulesTest {
	@Test void onlySovereignAndExistingGaleClearOpenTheLesson() {
		for (int stage = 0; stage < 5; stage++) assertFalse(WallTurnRules.eligible(stage, true));
		assertFalse(WallTurnRules.eligible(5, false));
		assertTrue(WallTurnRules.eligible(5, true));
	}
	@Test void fixedPriceDoesNotHaveAnAwakeningOrMethodDiscountPath() {
		for (double aura : new double[] {0, 19.999, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
			assertFalse(WallTurnRules.canPay(aura, 120, 120, false));
		assertTrue(WallTurnRules.canPay(20, 120, 120, false));
		assertFalse(WallTurnRules.canPay(160, 119, 120, false));
		assertFalse(WallTurnRules.canPay(160, 120, 120, true));
	}
	@Test void pathCannotAddUnlimitedHeightOrDistance() {
		for (int step = -100; step <= 100; step++) {
			assertTrue(WallTurnRules.distance(step) >= 0 && WallTurnRules.distance(step) <= 4);
			assertTrue(WallTurnRules.rise(step) >= 0 && WallTurnRules.rise(step) <= 1.25);
		}
		assertEquals(4, WallTurnRules.distance(8)); assertEquals(0, WallTurnRules.rise(8), 1.0E-12);
		assertEquals(1.25, WallTurnRules.rise(4), 1.0E-12);
	}
	@Test void releaseAndASeparateTickAreRequiredBetweenFreshPresses() {
		var input = new WallTurnRules.Input();
		assertTrue(input.admit(1, WallTurnRules.PRESS, 1));
		assertFalse(input.admit(1, WallTurnRules.PRESS, 2), "Duplicate sequence is rejected");
		assertFalse(input.admit(2, WallTurnRules.PRESS, 121), "Holding through cooldown does not produce another press");
		assertTrue(input.admit(3, WallTurnRules.RELEASE, 122));
		assertFalse(input.admit(4, WallTurnRules.PRESS, 122), "One packet burst is not a fresh release and repress");
		assertTrue(input.admit(5, WallTurnRules.PRESS, 123));
	}
	@Test void menuCancellationNeedsAReleaseAndInvalidPacketsCannotAdvanceTheSequence() {
		var input = new WallTurnRules.Input();
		assertFalse(input.admit(1, 100, 1));
		assertFalse(input.admit(Long.MAX_VALUE, WallTurnRules.PRESS, 1));
		assertTrue(input.admit(1, WallTurnRules.CANCEL, 1));
		assertFalse(input.admit(2, WallTurnRules.PRESS, 2));
		assertTrue(input.admit(3, WallTurnRules.RELEASE, 3));
		assertTrue(input.admit(4, WallTurnRules.PRESS, 4));
	}
	@Test void originalBodyAndHandsHaveDistinctAcceptedPhasesAndNoPhantomLanding() {
		var brace = WallTurnAnimation.sample(WallTurnRules.BRACE, 10, 2);
		var kick = WallTurnAnimation.sample(WallTurnRules.KICK, 1, 1);
		var land = WallTurnAnimation.sample(WallTurnRules.LAND, 10, 1);
		assertNotEquals(brace.frontLeg(), kick.frontLeg());
		assertNotEquals(brace.hand(), kick.hand());
		assertTrue(land.lower() > brace.lower());
		assertSame(MastersArtAnimation.NONE, WallTurnAnimation.sample(WallTurnRules.ABORT, 5, 3));
		assertSame(MastersArtAnimation.NONE, WallTurnAnimation.sample(WallTurnRules.FALL, 0, 5));
		assertSame(MastersArtAnimation.NONE, WallTurnAnimation.sample(WallTurnRules.LAND, 10, 10));
		for (float age : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY})
			assertSame(MastersArtAnimation.NONE, WallTurnAnimation.sample(WallTurnRules.BRACE, 10, age));
	}
	@Test void bothHandsAndFreeLookKeepTheOriginalWeaponInFrontOfTheCamera() {
		for (int phase : new int[] {WallTurnRules.BRACE, WallTurnRules.KICK, WallTurnRules.LAND, WallTurnRules.ABORT})
			for (int remaining = 1; remaining <= 10; remaining++) for (boolean left : new boolean[] {false, true}) {
				var pose = WallTurnAnimation.sample(phase, remaining, .5F);
				var view = MastersArtAnimation.view(pose, left, 0, 0, 0);
				assertTrue(Float.isFinite(view.transform().pitch()) && view.transform().z() <= 0);
				assertTrue(Math.abs(view.transform().x()) <= .15 && Math.abs(view.transform().roll()) <= 20);
				var hip = MastersArtAnimation.pivot(pose, 0, 12, 0, left);
				assertEquals(12 + pose.lower(), hip.y(), .0001F, "Torso and limb roots remain on the original hip pivot");
			}
	}
}
