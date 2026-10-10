package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyMountRulesTest {
	@Test
	void onlyTheTenthCircleEarnsTheBridle() {
		assertTrue(SkyMountRules.rewards(10));
		assertFalse(SkyMountRules.rewards(5));
		assertFalse(SkyMountRules.rewards(15));
	}

	@Test
	void forwardFliesTheWayYouLook() {
		double[] south = SkyMountRules.steer(0, 0, 1, 0);
		assertEquals(SkyMountRules.SPEED, south[2], 1e-9);
		assertEquals(0, south[0], 1e-9);
		assertEquals(0, south[1], 1e-9);
		double[] climb = SkyMountRules.steer(0, -30, 1, 0);
		assertTrue(climb[1] > 0, "looking up climbs");
		double[] steep = SkyMountRules.steer(0, -90, 1, 0);
		assertEquals(Math.sin(Math.toRadians(SkyMountRules.MAX_PITCH)) * SkyMountRules.SPEED, steep[1], 1e-9, "no steeper than the cap");
		assertTrue(steep[2] > 0, "it always keeps some way on");
	}

	@Test
	void backingOffAndStrafingAreSlower() {
		double[] back = SkyMountRules.steer(0, -45, -1, 0);
		assertEquals(-SkyMountRules.SPEED * SkyMountRules.BACK, back[2], 1e-9);
		assertEquals(0, back[1], 1e-9, "backing off never climbs");
		double[] left = SkyMountRules.steer(0, 0, 0, 1);
		assertEquals(SkyMountRules.SPEED * SkyMountRules.STRAFE, left[0], 1e-9, "facing south, left is east");
		double[] still = SkyMountRules.steer(90, 20, 0, 0);
		assertEquals(0, still[0] + still[1] + still[2], 1e-9, "nothing held: it hovers");
	}

	@Test
	void itEasesIntoItsCourse() {
		double v = 0;
		for (int i = 0; i < 60; i++) v = SkyMountRules.ease(v, 1);
		assertTrue(v > 0.99 && v < 1);
		assertEquals(SkyMountRules.EASE, SkyMountRules.ease(0, 1), 1e-9);
	}
}
