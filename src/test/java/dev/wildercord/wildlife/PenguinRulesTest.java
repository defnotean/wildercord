package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PenguinRulesTest {
	@Test
	void itSlidesOnIceWhenInAHurry() {
		assertTrue(PenguinRules.slides(true, true, 1.3));
		assertFalse(PenguinRules.slides(false, true, 1.3), "on grass it waddles");
		assertFalse(PenguinRules.slides(true, false, 1.3), "in the air");
		assertFalse(PenguinRules.slides(true, true, 1.0), "strolling");
		assertEquals(0.2F * PenguinRules.SLIDE_BOOST, PenguinRules.speed(0.2F, true), 1e-6);
		assertEquals(0.2F, PenguinRules.speed(0.2F, false));
	}

	@Test
	void itFishesNowAndThenAndEagerlyForAFriend() {
		assertTrue(PenguinRules.fishes(true, true, PenguinRules.NOTHING, -1, false));
		assertFalse(PenguinRules.fishes(false, true, PenguinRules.NOTHING, -1, false), "chicks don't fish");
		assertFalse(PenguinRules.fishes(true, true, PenguinRules.COD, -1, false), "beak full");
		assertFalse(PenguinRules.fishes(true, true, PenguinRules.NOTHING, PenguinRules.GIFT_COOLDOWN, false));
		assertTrue(PenguinRules.fishes(true, true, PenguinRules.NOTHING, PenguinRules.GIFT_COOLDOWN, true));
		assertTrue(PenguinRules.fishes(true, true, PenguinRules.NOTHING, PenguinRules.FISH_COOLDOWN, false));
	}

	@Test
	void itCatchesMostlyCod() {
		assertEquals(PenguinRules.COD, PenguinRules.catchOf(0.1));
		assertEquals(PenguinRules.SALMON, PenguinRules.catchOf(0.9));
		assertEquals(PenguinRules.FISH_MIN, PenguinRules.fishLength(-1));
		assertEquals(PenguinRules.FISH_MAX, PenguinRules.fishLength(2));
	}

	@Test
	void theFishGoesToAFriendThenAChick() {
		assertEquals(PenguinRules.Mouth.FRIEND, PenguinRules.mouth(true, true));
		assertEquals(PenguinRules.Mouth.CHICK, PenguinRules.mouth(false, true));
		assertEquals(PenguinRules.Mouth.SELF, PenguinRules.mouth(false, false));
	}

	@Test
	void theColonyHuddlesAtNight() {
		assertTrue(PenguinRules.huddles(true, true, 2, false));
		assertFalse(PenguinRules.huddles(false, true, 2, false), "by day");
		assertFalse(PenguinRules.huddles(true, true, 0, false), "alone");
		assertFalse(PenguinRules.huddles(true, false, 2, false), "chicks are tucked in among them anyway");
		assertFalse(PenguinRules.huddles(true, true, 2, true), "following its friend");
	}
}
