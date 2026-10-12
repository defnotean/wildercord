package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StoatRulesTest {
	@Test
	void strangersCloseByMakeItStandUp() {
		assertTrue(StoatRules.perks(3, false));
		assertFalse(StoatRules.perks(StoatRules.STAND_RANGE + 1, false), "too far off");
		assertFalse(StoatRules.perks(2, true), "not someone it trusts");
	}

	@Test
	void onlyARestedGrownStoatHuntsANearbyRabbit() {
		assertTrue(StoatRules.hunts(false, 5, StoatRules.HUNT_REST));
		assertFalse(StoatRules.hunts(true, 5, StoatRules.HUNT_REST), "kits don't hunt");
		assertFalse(StoatRules.hunts(false, StoatRules.HUNT_RANGE + 1, StoatRules.HUNT_REST), "too far off");
		assertFalse(StoatRules.hunts(false, 5, StoatRules.HUNT_REST - 1), "still resting");
		assertTrue(StoatRules.DANCE_RANGE < StoatRules.HUNT_RANGE, "it creeps closer before it dances");
	}

	@Test
	void itDivesOnlyIntoSnowAndIsHiddenOnlyMidDive() {
		assertTrue(StoatRules.dives(false, true));
		assertFalse(StoatRules.dives(false, false));
		assertFalse(StoatRules.dives(true, true));
		assertFalse(StoatRules.buried(StoatRules.DIVE_TICKS), "going in");
		assertTrue(StoatRules.buried(StoatRules.DIVE_SHIFT), "under when it moves");
		assertFalse(StoatRules.buried(1), "coming out");
	}

	@Test
	void itsDanceHopsAndLands() {
		assertEquals(0, StoatRules.hop(0), 1e-6);
		assertTrue(StoatRules.hop(3) > 0.4F);
		assertEquals(0, StoatRules.hop(8), 1e-6);
		for (float t = 0; t < 30; t += 0.25F) {
			assertTrue(StoatRules.hop(t) >= 0);
		}
	}

	@Test
	void itsCoatFollowsTheColdAndTrustLastsAWhile() {
		assertTrue(StoatRules.winterCoat(true));
		assertFalse(StoatRules.winterCoat(false));
		assertTrue(StoatRules.trusts(0));
		assertFalse(StoatRules.trusts(StoatRules.TRUST_TICKS));
		assertFalse(StoatRules.trusts(-1));
	}
}
