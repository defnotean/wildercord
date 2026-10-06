package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RimehareBoundCooldownTest {
	@Test
	void flightCannotSpendTheLandingRecovery() {
		for (int delay = 3; delay <= 6; delay++) {
			var cooldown = new RimehareBoundCooldown();
			assertTrue(cooldown.tick(true, true));
			cooldown.launched(delay);
			assertFalse(cooldown.tick(true, false));
			for (int tick = 0; tick < 100; tick++) assertFalse(cooldown.tick(false, false));
			assertFalse(cooldown.tick(false, true), "Landing must get a native grounded navigation tick first");
			for (int tick = 1; tick < delay; tick++) assertFalse(cooldown.tick(true, true));
			assertTrue(cooldown.tick(true, true));
		}
	}

	@Test
	void walkingOffALedgePausesRecoveryUntilGroundNavigationResumes() {
		var cooldown = new RimehareBoundCooldown();
		cooldown.launched(3);
		assertFalse(cooldown.tick(true, true));
		assertFalse(cooldown.tick(true, false));
		assertFalse(cooldown.tick(false, false));
		assertFalse(cooldown.tick(false, true));
		assertFalse(cooldown.tick(true, true));
		assertTrue(cooldown.tick(true, true));
	}

	@Test
	void everyBoundStartsANewRecovery() {
		var cooldown = new RimehareBoundCooldown();
		for (int bound = 0; bound < 4; bound++) {
			assertTrue(cooldown.tick(true, true));
			cooldown.launched(3);
			assertFalse(cooldown.tick(false, true));
			assertFalse(cooldown.tick(true, true));
			assertFalse(cooldown.tick(true, true));
		}
	}
}
