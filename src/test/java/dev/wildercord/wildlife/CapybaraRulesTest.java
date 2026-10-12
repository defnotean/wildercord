package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CapybaraRulesTest {
	@Test
	void itSettlesIntoTheWater() {
		assertTrue(CapybaraRules.settles(true, CapybaraRules.SETTLE_TICKS, -1));
		assertFalse(CapybaraRules.settles(false, 100, -1), "dry");
		assertFalse(CapybaraRules.settles(true, 5, -1), "still moving");
		assertFalse(CapybaraRules.settles(true, 100, 10), "just got out");
		assertTrue(CapybaraRules.settles(true, 100, CapybaraRules.BATH_COOLDOWN));
	}

	@Test
	void itSoaksAWhileThenGetsOut() {
		int length = CapybaraRules.bathLength(0.5);
		assertTrue(length >= CapybaraRules.BATH_MIN && length <= CapybaraRules.BATH_MAX);
		assertEquals(CapybaraRules.BATH_MIN, CapybaraRules.bathLength(-1));
		assertEquals(CapybaraRules.BATH_MAX, CapybaraRules.bathLength(2));
		assertTrue(CapybaraRules.staysBathing(true, 10, length));
		assertFalse(CapybaraRules.staysBathing(true, length, length));
		assertFalse(CapybaraRules.staysBathing(false, 10, length));
		assertTrue(CapybaraRules.wantsBath(-1));
		assertFalse(CapybaraRules.wantsBath(10));
		assertTrue(CapybaraRules.wantsBath(CapybaraRules.BATH_COOLDOWN));
	}

	@Test
	void bathersNearbySoakWithIt() {
		assertTrue(CapybaraRules.soaks(true, true, 2));
		assertFalse(CapybaraRules.soaks(false, true, 2), "not bathing");
		assertFalse(CapybaraRules.soaks(true, false, 2), "they're dry");
		assertFalse(CapybaraRules.soaks(true, true, CapybaraRules.SOAK_RANGE + 1), "too far");
	}

	@Test
	void youngRideOnTheirBacks() {
		assertTrue(CapybaraRules.climbs(true, false, true, 1));
		assertFalse(CapybaraRules.climbs(false, false, true, 1), "grown");
		assertFalse(CapybaraRules.climbs(true, true, true, 1), "already up");
		assertFalse(CapybaraRules.climbs(true, false, false, 1), "back taken");
		assertFalse(CapybaraRules.climbs(true, false, true, 5), "too far");
		assertFalse(CapybaraRules.hopsDown(true, 10, false));
		assertTrue(CapybaraRules.hopsDown(false, 10, false));
		assertTrue(CapybaraRules.hopsDown(true, CapybaraRules.RIDE_TICKS, false));
		assertTrue(CapybaraRules.hopsDown(true, 10, true));
	}
}
