package dev.wildercord.net;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketThrottleTest {
	private static final UUID A = new UUID(0, 1);
	private static final UUID B = new UUID(0, 2);

	@Test
	void aBurstGoesThroughThenAFloodIsDropped() {
		PacketThrottle throttle = new PacketThrottle(5, 2);
		for (int i = 0; i < 5; i++) {
			assertTrue(throttle.allow(A, 100), "packet " + i + " of the burst");
		}
		assertFalse(throttle.allow(A, 100));
		// Another player has their own allowance.
		assertTrue(throttle.allow(B, 100));
	}

	@Test
	void itRefillsOverTime() {
		PacketThrottle throttle = new PacketThrottle(3, 2);
		for (int i = 0; i < 3; i++) {
			throttle.allow(A, 0);
		}
		assertFalse(throttle.allow(A, 1));
		assertTrue(throttle.allow(A, 3), "one back after two ticks");
		assertFalse(throttle.allow(A, 3));
		// Never more than the burst, however long it waited.
		for (int i = 0; i < 3; i++) {
			assertTrue(throttle.allow(A, 10_000));
		}
		assertFalse(throttle.allow(A, 10_000));
	}

	@Test
	void aClockThatWentBackStartsOver() {
		PacketThrottle throttle = new PacketThrottle(2, 20);
		throttle.allow(A, 500);
		throttle.allow(A, 500);
		assertFalse(throttle.allow(A, 500));
		assertTrue(throttle.allow(A, 10));
		throttle.forget(A);
		assertTrue(throttle.allow(A, 10));
	}
}
