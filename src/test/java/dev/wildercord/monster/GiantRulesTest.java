package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GiantRulesTest {
	@Test
	void theLandPicksTheGiant() {
		assertEquals(GiantRules.Kind.MATRIARCH, GiantRules.kind(0.0F, true));
		assertEquals(GiantRules.Kind.MATRIARCH, GiantRules.kind(-0.5F, false));
		assertEquals(GiantRules.Kind.COLOSSUS, GiantRules.kind(2.0F, false));
		assertEquals(GiantRules.Kind.ELDER, GiantRules.kind(0.8F, false));
		for (GiantRules.Kind kind : GiantRules.Kind.values()) {
			assertEquals(kind, GiantRules.kind(kind == GiantRules.Kind.MATRIARCH ? 0 : kind == GiantRules.Kind.COLOSSUS ? 2 : 0.8F, false));
		}
		assertEquals("monster.wildercord.giant.colossus", GiantRules.Kind.COLOSSUS.key());
	}

	@Test
	void itStompsOnlyInAFightAndOnItsOwnBeat() {
		long beat = GiantRules.STOMP_INTERVAL * 7L;
		assertTrue(GiantRules.stomps(true, beat, 0));
		assertFalse(GiantRules.stomps(false, beat, 0));
		assertFalse(GiantRules.stomps(true, beat + 1, 0));
		assertTrue(GiantRules.stomps(true, beat - 3, 3));
	}

	@Test
	void aStompFallsOffWithDistance() {
		assertEquals(GiantRules.STOMP_DAMAGE, GiantRules.stompDamage(0), 1e-6);
		assertEquals(GiantRules.STOMP_DAMAGE / 2, GiantRules.stompDamage(GiantRules.STOMP_RADIUS), 1e-6);
		assertEquals(0, GiantRules.stompDamage(GiantRules.STOMP_RADIUS + 0.1), 1e-6);
		assertTrue(GiantRules.stompDamage(2) > GiantRules.stompDamage(4));
	}

	@Test
	void itRoamsOnlyWhenIdle() {
		long beat = GiantRules.ROAM_INTERVAL * 3L;
		assertTrue(GiantRules.roams(false, beat, 0));
		assertFalse(GiantRules.roams(true, beat, 0));
		assertFalse(GiantRules.roams(false, beat + 1, 0));
	}

	@Test
	void itDropsItsHeart() {
		assertEquals(1, GiantRules.hearts(0.9));
		assertEquals(2, GiantRules.hearts(0.1));
		assertTrue(GiantRules.WAKE_FAR > GiantRules.WAKE_NEAR && GiantRules.APART > GiantRules.WAKE_FAR);
	}
}
