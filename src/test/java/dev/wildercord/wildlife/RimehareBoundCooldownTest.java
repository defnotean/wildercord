package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RimehareBoundCooldownTest {
	@Test
	void flightCannotSpendTheLandingRecovery() {
		for (int delay = 3; delay <= 6; delay++) {
			var cooldown = new RimehareBoundCooldown();
			assertTrue(cooldown.tick(true, true));
			assertFalse(cooldown.shouldBrake(.5, .78));
			cooldown.launched(delay);
			assertTrue(cooldown.shouldBrake(.5, .78));
			assertFalse(cooldown.tick(true, false));
			for (int tick = 0; tick < 100; tick++) assertFalse(cooldown.tick(false, false));
			assertFalse(cooldown.tick(false, true), "Landing must get a native grounded navigation tick first");
			for (int tick = 1; tick < delay; tick++) {
				assertTrue(cooldown.shouldBrake(.5, .78));
				assertFalse(cooldown.tick(true, true));
			}
			assertTrue(cooldown.shouldBrake(.5, .78));
			assertTrue(cooldown.tick(true, true));
			assertFalse(cooldown.shouldBrake(.5, .78));
		}
	}

	@Test
	void recoveryBrakesOnlyInputsThatCouldSkipAWaypointCell() {
		var cooldown = new RimehareBoundCooldown();
		assertFalse(cooldown.shouldBrake(.5, .78), "No preceding bound means no recovery braking");
		cooldown.launched(3);
		assertTrue(cooldown.shouldBrake(.5, .78), "Fast recovery must let native friction shed excess speed");
		assertFalse(cooldown.shouldBrake(.3, .78), "A sprint step that still fits the cell keeps its input");
		assertFalse(cooldown.shouldBrake(.2, .3), "Berry attraction keeps its ordinary movement input");
		assertFalse(cooldown.shouldBrake(.5, .6), "Ordinary fleeing keeps its movement input");
		assertTrue(cooldown.shouldBrake(0, 1.2), "Inputs above one use native normalization");
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
