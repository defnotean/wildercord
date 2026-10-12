package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DripfangRulesTest {
	@Test
	void itHangsFromHighEnoughCeilings() {
		assertFalse(DripfangRules.hangs(2), "too low to hide");
		assertTrue(DripfangRules.hangs(DripfangRules.MIN_HANG));
		assertTrue(DripfangRules.hangs(DripfangRules.CEILING_REACH));
		assertFalse(DripfangRules.hangs(DripfangRules.CEILING_REACH + 1));
	}

	@Test
	void itDropsOnPreyRightBelow() {
		assertTrue(DripfangRules.drops(0.5, 6));
		assertFalse(DripfangRules.drops(3, 6), "off to the side");
		assertFalse(DripfangRules.drops(0.5, 0.2), "level with it");
		assertFalse(DripfangRules.drops(0.5, DripfangRules.DROP_DEPTH + 1), "too far down");
		assertTrue(DripfangRules.twitches(5));
		assertFalse(DripfangRules.twitches(DripfangRules.TWITCH_RANGE + 1));
	}

	@Test
	void theLandingFallsOffFromTheHeart() {
		assertEquals(DripfangRules.DROP_DAMAGE, DripfangRules.landing(0));
		assertEquals(DripfangRules.DROP_DAMAGE / 2, DripfangRules.landing(DripfangRules.DROP_RADIUS), 1e-4);
		assertEquals(0, DripfangRules.landing(DripfangRules.DROP_RADIUS + 0.1));
		assertTrue(DripfangRules.landing(0.5) > DripfangRules.landing(1.0));
	}

	@Test
	void flippedItTakesMore() {
		assertEquals(4, DripfangRules.hurt(4, false));
		assertEquals(6, DripfangRules.hurt(4, true));
	}

	@Test
	void itClimbsBackWhenIdle() {
		assertTrue(DripfangRules.reclimbs(DripfangRules.RECLIMB_TICKS, false, true));
		assertFalse(DripfangRules.reclimbs(DripfangRules.RECLIMB_TICKS, true, true), "hunting");
		assertFalse(DripfangRules.reclimbs(DripfangRules.RECLIMB_TICKS, false, false), "no ceiling");
		assertFalse(DripfangRules.reclimbs(10, false, true), "not yet");
		assertTrue(DripfangRules.bites(1.5, 20, 20));
		assertFalse(DripfangRules.bites(1.5, 19, 20));
		assertFalse(DripfangRules.bites(3, 20, 20));
	}
}
