package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeathsDoorTest {
	@Test
	void theRestCountsDownInWholeSeconds() {
		assertEquals(0, DeathsDoor.left(null, 5000, DeathsDoor.REST), "never saved: no rest");
		assertEquals(60, DeathsDoor.left(1000L, 1000, DeathsDoor.REST), "just saved: the whole minute");
		assertEquals(30, DeathsDoor.left(1000L, 1600, DeathsDoor.REST));
		assertEquals(1, DeathsDoor.left(1000L, 1000 + DeathsDoor.REST - 1, DeathsDoor.REST), "never 0 while it lasts");
		assertEquals(0, DeathsDoor.left(1000L, 1000 + DeathsDoor.REST, DeathsDoor.REST), "over at the minute");
		assertEquals(180, DeathsDoor.left(1000L, 1000, DeathsDoor.REBIRTH_REST));
		// A save "in the future" (the clock turned back) doesn't hold anyone for ever.
		assertEquals(0, DeathsDoor.left(9000L, 1000, DeathsDoor.REST));
	}
}
