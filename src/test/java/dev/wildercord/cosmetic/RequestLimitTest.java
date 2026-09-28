package dev.wildercord.cosmetic;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestLimitTest {
	private static final UUID ALEX = new UUID(0, 1);
	private static final UUID SAM = new UUID(0, 2);

	@Test
	void fourASecondThenTheRestAreDropped() {
		RequestLimit limit = new RequestLimit(CordCosmetics.REQUESTS_PER_SECOND, 20);
		for (int i = 0; i < CordCosmetics.REQUESTS_PER_SECOND; i++) {
			assertTrue(limit.allow(ALEX, 100), "request " + i);
		}
		assertFalse(limit.allow(ALEX, 100), "a fifth in the same tick");
		assertFalse(limit.allow(ALEX, 119), "still within the second");
		assertTrue(limit.allow(SAM, 100), "each player has their own allowance");
		for (int i = 0; i < CordCosmetics.REQUESTS_PER_SECOND; i++) {
			assertTrue(limit.allow(ALEX, 120), "a second later there's room again");
		}
		assertFalse(limit.allow(ALEX, 120));
		limit.forget(ALEX);
		assertTrue(limit.allow(ALEX, 121));
		assertThrows(IllegalArgumentException.class, () -> new RequestLimit(0, 20));
	}

	@Test
	void spreadOutRequestsAreNeverDropped() {
		RequestLimit limit = new RequestLimit(4, 20);
		for (long tick = 0; tick < 400; tick += 5) {
			assertTrue(limit.allow(ALEX, tick), "tick " + tick);
		}
	}
}
