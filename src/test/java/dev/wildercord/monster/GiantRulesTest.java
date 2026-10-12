package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildercord.cast.MoveDeck;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
	void theBrambleGiantPicksByWhereYouStand() {
		double[] high = GiantRules.Bramble.weights(10, true, false, true);
		for (int i = 0; i < high.length; i++) assertEquals(i == 2 || i == 6, high[i] > 0, "up out of reach " + i);
		double[] near = GiantRules.Bramble.weights(3, false, true, false);
		assertTrue(near[0] > 0 && near[7] > 0, "the Colossus stomps and kindles close in");
		assertEquals(0, near[6], 1e-9, "only the Elder raises brambles");
		assertEquals(0, GiantRules.Bramble.weights(3, false, true, true)[7], 1e-9, "only the Colossus kindles");
		double[] far = GiantRules.Bramble.weights(20, false, false, true);
		assertTrue(far[2] > 0 && far[3] > 0 && far[6] > 0, "a way off it throws, roots and brambles");
		assertEquals(0, far[0] + far[1] + far[4] + far[5], 1e-9, "a way off it can't reach");
		assertEquals(-1, GiantRules.Bramble.choose(new MoveDeck(GiantRules.Bramble.MEMORY), 60, false, false, true, 0.5));
	}

	@Test
	void theBrambleGiantNeverRepeatsAndUsesEverything() {
		for (boolean elder : new boolean[] {true, false}) {
			MoveDeck deck = new MoveDeck(GiantRules.Bramble.MEMORY);
			Random random = new Random(7);
			Set<Integer> seen = new HashSet<>();
			int last = -1;
			for (int i = 0; i < 400; i++) {
				double d = 2 + random.nextDouble() * 20;
				int move = GiantRules.Bramble.choose(deck, d, false, random.nextBoolean(), elder, random.nextDouble());
				if (move < 0) continue;
				assertNotEquals(last, move, "a move twice running");
				deck.played(move);
				seen.add(move);
				last = move;
			}
			assertEquals(GiantRules.Bramble.PICKS.length - 1, seen.size(), "every move of the " + (elder ? "Elder" : "Colossus"));
			assertFalse(seen.contains(elder ? GiantRules.Bramble.KINDLE : GiantRules.Bramble.BRAMBLES));
		}
	}

	@Test
	void theBrambleGiantsGripBreaksAndItsReachHasAShape() {
		assertTrue(GiantRules.Bramble.grabs(4, 0.9));
		assertFalse(GiantRules.Bramble.grabs(4, 0.2), "beside it");
		assertFalse(GiantRules.Bramble.grabs(GiantRules.Bramble.GRAB_RANGE + 1, 1));
		assertTrue(GiantRules.Bramble.breaks(GiantRules.Bramble.GRAB_BREAK * 400, 400));
		assertFalse(GiantRules.Bramble.breaks(GiantRules.Bramble.GRAB_BREAK * 400 - 1, 400));
		assertTrue(GiantRules.Bramble.onWave(6, 6.3, true));
		assertFalse(GiantRules.Bramble.onWave(6, 6.3, false), "jumped");
		assertFalse(GiantRules.Bramble.onWave(3, 6.3, true), "already passed");
		assertEquals(15, GiantRules.Bramble.hurt(10, GiantRules.Bramble.STUCK), 1e-6);
		assertEquals(5, GiantRules.Bramble.hurt(10, GiantRules.Bramble.ROAR), 1e-6);
		assertEquals(10, GiantRules.Bramble.hurt(10, GiantRules.Bramble.SWEEP), 1e-6);
	}

	@Test
	void arcsAndLanes() {
		assertTrue(GiantRules.inArc(5, 60, 9.5, 150));
		assertFalse(GiantRules.inArc(5, 100, 9.5, 150), "out past its side");
		assertFalse(GiantRules.inArc(10, 0, 9.5, 150), "out of reach");
		assertTrue(GiantRules.inArc(5, 350, 9.5, 150), "wrapped round");
		assertTrue(GiantRules.inLane(5, 1, 11, 1.6));
		assertFalse(GiantRules.inLane(-1, 0, 11, 1.6), "behind it");
		assertFalse(GiantRules.inLane(5, 2, 11, 1.6), "stepped out");
		assertTrue(GiantRules.rouses(false, 50, 100));
		assertFalse(GiantRules.rouses(true, 10, 100));
		assertFalse(GiantRules.rouses(false, 60, 100));
	}

	@Test
	void theMatriarchPicksByWhereYouStand() {
		double[] high = GiantRules.Matriarch.weights(10, true, false, false, 0);
		for (int i = 0; i < high.length; i++) assertEquals(i == 3 || i == 6, high[i] > 0, "up out of reach " + i);
		double[] behind = GiantRules.Matriarch.weights(4, false, true, false, 4);
		assertEquals(4, behind[2], 1e-9, "her tail waits behind her");
		assertEquals(0, behind[1] + behind[4], 1e-9, "no swipes or breath at her back");
		assertEquals(0, behind[3], 1e-9, "she howls no more with her pups all about");
		assertEquals(0, GiantRules.Matriarch.weights(4, false, false, false, 0)[2], 1e-9, "no tail in front");
		assertTrue(GiantRules.Matriarch.weights(10, false, false, false, 0)[5] > 0, "a way off she vanishes");
		assertEquals(-1, GiantRules.Matriarch.choose(new MoveDeck(GiantRules.Matriarch.MEMORY), 60, false, false, false, 4, 0.5));
	}

	@Test
	void theMatriarchNeverRepeatsAndUsesEverything() {
		MoveDeck deck = new MoveDeck(GiantRules.Matriarch.MEMORY);
		Random random = new Random(7);
		Set<Integer> seen = new HashSet<>();
		int last = -1;
		for (int i = 0; i < 400; i++) {
			double d = 1 + random.nextDouble() * 18;
			int move = GiantRules.Matriarch.choose(deck, d, false, random.nextInt(3) == 0, random.nextBoolean(), random.nextInt(5),
				random.nextDouble());
			if (move < 0) continue;
			assertNotEquals(last, move, "a move twice running");
			deck.played(move);
			seen.add(move);
			last = move;
		}
		assertEquals(GiantRules.Matriarch.PICKS.length, seen.size(), "every move");
	}

	@Test
	void theMatriarchsBackAndBreath() {
		assertTrue(GiantRules.Matriarch.behind(-0.8));
		assertFalse(GiantRules.Matriarch.behind(0.1));
		assertEquals(-GiantRules.Matriarch.BREATH_SWEEP, GiantRules.Matriarch.breathAngle(0, false), 1e-9);
		assertEquals(GiantRules.Matriarch.BREATH_SWEEP, GiantRules.Matriarch.breathAngle(GiantRules.Matriarch.BREATH_TICKS, false), 1e-9);
		assertEquals(GiantRules.Matriarch.BREATH_SWEEP, GiantRules.Matriarch.breathAngle(0, true), 1e-9, "back the other way");
		assertEquals(0, GiantRules.Matriarch.breathAngle(GiantRules.Matriarch.BREATH_TICKS / 2, false), 1e-9);
		assertEquals(15, GiantRules.Matriarch.hurt(10, GiantRules.Matriarch.SPRAWL), 1e-6);
		assertEquals(5, GiantRules.Matriarch.hurt(10, GiantRules.Matriarch.ROAR), 1e-6);
		assertEquals(10, GiantRules.Matriarch.hurt(10, GiantRules.Matriarch.POUNCE), 1e-6);
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
