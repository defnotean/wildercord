package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReefbackRulesTest {
	@Test
	void forwardSwimsTheWayYouLook() {
		double[] south = ReefbackRules.swim(0, 0, 1, 0);
		assertEquals(ReefbackRules.SWIM_SPEED, south[2], 1e-9);
		assertEquals(0, south[1], 1e-9);
		assertTrue(ReefbackRules.swim(0, 40, 1, 0)[1] < 0, "looking down dives");
		assertTrue(ReefbackRules.swim(0, -40, 1, 0)[1] > 0, "looking up climbs");
		double[] steep = ReefbackRules.swim(0, 90, 1, 0);
		assertEquals(-Math.sin(Math.toRadians(ReefbackRules.MAX_PITCH)) * ReefbackRules.SWIM_SPEED, steep[1], 1e-9, "no steeper than the cap");
		assertTrue(steep[2] > 0, "it always keeps some way on");
	}

	@Test
	void backingOffAndStrafingAreSlower() {
		double[] back = ReefbackRules.swim(0, 60, -1, 0);
		assertEquals(-ReefbackRules.SWIM_SPEED * ReefbackRules.BACK, back[2], 1e-9);
		assertEquals(0, back[1], 1e-9, "backing off never dives");
		double[] left = ReefbackRules.swim(0, 0, 0, 1);
		assertEquals(ReefbackRules.SWIM_SPEED * ReefbackRules.STRAFE, left[0], 1e-9, "facing south, left is east");
		double[] still = ReefbackRules.swim(45, 30, 0, 0);
		assertEquals(0, Math.abs(still[0]) + Math.abs(still[1]) + Math.abs(still[2]), 1e-9, "nothing held: it holds still");
	}

	@Test
	void itEasesIntoItsCourseAndKeepsItsRiderBreathing() {
		assertEquals(ReefbackRules.EASE, ReefbackRules.ease(0, 1), 1e-9);
		assertTrue(ReefbackRules.BREATH_TICKS > ReefbackRules.BREATH_EVERY, "the breath never runs out between refreshes");
	}
}
