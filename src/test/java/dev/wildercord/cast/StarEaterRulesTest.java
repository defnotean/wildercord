package dev.wildercord.cast;

import static dev.wildercord.cast.StarEaterRules.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class StarEaterRulesTest {
	private static long[] rested() {
		long[] since = new long[OWN + 1];
		Arrays.fill(since, 100_000);
		return since;
	}

	private static double weight(int move, int phase, boolean guarded, double distance, double height, boolean sight, int strewn) {
		return weights(phase, guarded, distance, height, sight, strewn, rested(), -1)[move];
	}

	private static double shielded(int move, int phase, double distance) {
		return weight(move, phase, true, distance, 0, true, 0);
	}

	@Test
	void everyMoveHasItsOwnName() {
		var names = new HashSet<String>();
		for (int m = 1; m <= OWN; m++) {
			assertTrue(names.add(key(m)), "named twice: " + key(m));
		}
		assertFalse(names.contains("watch"));
		assertEquals("watch", key(0));
		assertTrue(PICKS.length >= 7 && PICKS.length <= 10);
		for (int m : PICKS) {
			assertTrue(m >= 1 && m <= OWN && m != GROUNDED, key(m));
		}
		assertTrue(OWN < DungeonBoss.MOVES);
	}

	@Test
	void everyMoveIsWoundUpLongEnoughToReadAndQuickerChained() {
		for (int m : PICKS) {
			assertTrue(tell(m, false) >= 10 && tell(m, false) <= 18, key(m) + " tell " + tell(m, false));
			assertTrue(tell(m, true) <= tell(m, false) && tell(m, true) >= 9, key(m) + " chained tell " + tell(m, true));
			// Its aim locks a few ticks before the blow, so a step at the last moment still counts.
			assertTrue(lock(m) == 0 || lock(m) >= 3 && lock(m) <= 6, key(m) + " lock " + lock(m));
			assertTrue(lock(m) < tell(m, true), key(m));
		}
		assertEquals(LANCE_LOCK, lock(LANCE));
		assertEquals(PLUNGE_LOCK, lock(PLUNGE));
		assertEquals(GAZE_LOCK, lock(GAZE));
		assertEquals(CAGE_LOCK, lock(CAGE));
		assertEquals(STARFALL_LOCK, lock(STARFALL));
		for (int retell : new int[] {LANCE_RETELL, PLUNGE_RETELL, LASH_RETELL}) {
			assertTrue(retell >= 9 && retell <= 18, "second tell " + retell);
		}
		assertEquals(0, tell(GROUNDED, false));
	}

	@Test
	void itLearnsTwoMovesInEachLaterPhaseAndForgetsNone() {
		for (int m : PICKS) {
			assertTrue(unlocked(m, 3), key(m));
		}
		int first = 0, second = 0, third = 0;
		for (int m : PICKS) {
			switch (unlocks(m)) {
				case 1 -> first++;
				case 2 -> second++;
				case 3 -> third++;
				default -> fail(key(m));
			}
		}
		assertTrue(first >= 4 && second >= 2 && third >= 2);
		assertFalse(unlocked(GAZE, 1));
		assertFalse(unlocked(CAGE, 1));
		assertFalse(unlocked(STARFALL, 2));
		assertFalse(unlocked(HORIZON, 2));
		assertFalse(unlocked(GROUNDED, 3));
		for (double d = 0; d <= 20; d += 1) {
			for (double h : new double[] {0, 4}) {
				for (boolean sight : new boolean[] {true, false}) {
					double[] one = weights(1, true, d, h, sight, 0, rested(), -1);
					double[] two = weights(2, true, d, h, sight, 0, rested(), -1);
					assertEquals(0, one[GAZE]);
					assertEquals(0, one[CAGE]);
					assertEquals(0, one[STARFALL]);
					assertEquals(0, one[HORIZON]);
					assertEquals(0, two[STARFALL]);
					assertEquals(0, two[HORIZON]);
					assertEquals(0, weights(3, false, d, h, sight, 6, rested(), -1)[GROUNDED]);
				}
			}
		}
	}

	@Test
	void behindItsShieldItThrowsAndBrokenOpenItOnlyLashesAndCalls() {
		for (double d = 0; d <= 16; d += 0.5) {
			double[] guarded = weights(3, true, d, 0, true, 6, rested(), -1);
			double[] open = weights(3, false, d, 0, true, 6, rested(), -1);
			assertEquals(0, guarded[GATHER]);
			assertEquals(0, guarded[LASH]);
			for (int m : PICKS) {
				if (!exposedOnly(m)) {
					assertEquals(0, open[m], key(m) + " broken open at " + d);
				}
			}
			assertTrue(open[GATHER] > 0);
		}
		assertEquals(0, weight(GATHER, 2, false, 3, 0, true, 0), "nothing strewn, nothing to call");
		assertTrue(weight(GATHER, 2, false, 3, 0, true, 5) > weight(GATHER, 2, false, 3, 0, true, 1));
		assertTrue(weight(LASH, 1, false, 3, 0, true, 0) > 0);
		assertEquals(0, weight(LASH, 1, false, 9, 0, true, 0), "it doesn't lash at nothing");
	}

	@Test
	void itThrowsTheSpearAtAnyoneKeepingAwayAndDropsOnAnyoneUnderIt() {
		assertEquals(0, shielded(LANCE, 1, 2), "no spear at anyone under it");
		assertTrue(shielded(LANCE, 1, 10) > shielded(LANCE, 1, 5));
		assertEquals(0, weight(LANCE, 1, true, 10, 0, false, 0), "no spear at anyone it can't see");
		assertTrue(shielded(PLUNGE, 1, 3) > shielded(PLUNGE, 1, 10));
		assertTrue(shielded(PLUNGE, 1, 3) > shielded(LANCE, 1, 3));
		assertTrue(weight(PLUNGE, 1, true, 3, 4, true, 0) < shielded(PLUNGE, 1, 3), "it won't drop on a pillar top");
		assertTrue(shielded(NOVA, 1, 4) > shielded(NOVA, 1, 10));
		assertEquals(0, shielded(NOVA, 1, 14), "its rings don't roll that far");
		assertTrue(shielded(VOLLEY, 1, 9) > weight(VOLLEY, 1, true, 9, 0, false, 0));
	}

	@Test
	void itStaresAtWhoeverItSeesAndRainsOnWhoeverHidesOrClimbs() {
		assertEquals(0, weight(GAZE, 2, true, 10, 0, false, 0), "it stares only at what it sees");
		assertTrue(shielded(GAZE, 2, 10) > shielded(GAZE, 2, 3));
		assertTrue(shielded(CAGE, 2, 10) > shielded(CAGE, 2, 3));
		assertTrue(weight(STARFALL, 3, true, 6, 4, true, 0) > shielded(STARFALL, 3, 6), "anyone up a pillar");
		assertTrue(weight(STARFALL, 3, true, 6, 0, false, 0) > shielded(STARFALL, 3, 6), "anyone hiding");
		assertTrue(weight(STARFALL, 3, true, 6, 4, true, 0) > weight(PLUNGE, 3, true, 6, 4, true, 0));
		assertTrue(shielded(HORIZON, 3, 9) > shielded(HORIZON, 3, 3), "it drags back whoever runs");
	}

	@Test
	void eachMoveRestsAWhileBeforeItComesAgainAndLessInTheLastPhase() {
		for (int m : PICKS) {
			assertTrue(cooldown(m, 1) >= 40, key(m));
			assertTrue(cooldown(m, 3) < cooldown(m, 1), key(m));
			long[] since = rested();
			since[m] = cooldown(m, 3) - 1;
			boolean guarded = !exposedOnly(m);
			for (double d = 0; d <= 16; d += 1) {
				assertEquals(0, weights(3, guarded, d, 0, true, 6, since, -1)[m], key(m) + " too soon at " + d);
				assertEquals(0, weights(3, guarded, d, 0, true, 6, rested(), m)[m], key(m) + " made last, at " + d);
			}
			since[m] = cooldown(m, 3);
			boolean any = false;
			for (double d = 0; d <= 16; d += 1) {
				any |= weights(3, guarded, d, 0, true, 6, since, -1)[m] > 0;
			}
			assertTrue(any, key(m) + " never wanted once rested");
		}
		assertEquals(-1, choose(new MoveDeck(4), 3, true, 6, 0, true, 0, new long[OWN + 1], 0.5), "everything too soon");
	}

	@Test
	void itNeverMakesTheSameMoveTwiceRunningAndMixesThemAllUp() {
		var deck = new MoveDeck(4);
		var rng = new Random(7);
		long[] made = new long[OWN + 1];
		Arrays.fill(made, -100_000);
		Map<Integer, Integer> count = new HashMap<>();
		long now = 0;
		int last = -1, picks = 0;
		for (int i = 0; picks < 300 && i < 5000; i++) {
			now += 10;
			int phase = 1 + Math.min(2, picks / 100);
			boolean guarded = i % 9 != 0;
			double d = new double[] {2, 4, 7, 10, 13}[i % 5];
			double h = i % 11 == 0 ? 4 : 0;
			boolean sight = i % 6 != 0;
			long[] since = new long[OWN + 1];
			for (int m = 0; m <= OWN; m++) {
				since[m] = now - made[m];
			}
			int m = choose(deck, phase, guarded, d, h, sight, guarded ? 0 : 5, since, rng.nextDouble());
			if (m < 0) {
				continue;
			}
			assertNotEquals(last, m, "the same move twice running at " + d);
			assertTrue(unlocked(m, phase), key(m) + " before it learnt it");
			assertEquals(!guarded, exposedOnly(m), key(m));
			deck.played(m);
			made[m] = now;
			count.merge(m, 1, Integer::sum);
			last = m;
			picks++;
			now += 50;
			int next = chain(m, phase, d, rng.nextDouble());
			if (next > 0) {
				assertNotEquals(m, next);
				deck.played(next);
				made[next] = now;
				last = next;
				now += 50;
			}
		}
		assertEquals(300, picks);
		assertEquals(PICKS.length, count.size(), "only " + count.keySet());
		for (var e : count.entrySet()) {
			assertTrue(e.getValue() < 90, key(e.getKey()) + " made " + e.getValue() + " times");
		}
	}

	@Test
	void inItsLastPhaseItRunsOnIntoAnotherMoveAndNeverTheSameAgain() {
		assertEquals(LANCE, chain(VOLLEY, 3, 10, 0.1));
		assertEquals(PLUNGE, chain(LANCE, 3, 4, 0.1));
		assertEquals(VOLLEY, chain(LANCE, 3, 12, 0.1));
		assertEquals(STARFALL, chain(GAZE, 3, 10, 0.1));
		assertEquals(LANCE, chain(CAGE, 3, 10, 0.1));
		assertEquals(NOVA, chain(HORIZON, 3, 10, 0.1));
		assertEquals(-1, chain(VOLLEY, 3, 10, 0.9));
		assertEquals(-1, chain(VOLLEY, 2, 10, 0.1), "only in its last phase");
		for (int m : new int[] {PLUNGE, GATHER, LASH, GROUNDED}) {
			assertEquals(-1, chain(m, 3, 4, 0.1), key(m));
		}
		for (int m : PICKS) {
			for (double d = 0; d <= 16; d += 0.5) {
				int n = chain(m, 3, d, 0.1);
				assertNotEquals(m, n, key(m));
				if (n > 0) {
					assertTrue(unlocked(n, 3) && !exposedOnly(n) && !exposedOnly(m), key(m) + " into " + key(n));
				}
			}
		}
		assertTrue(CHAIN_REST > REST[3]);
		assertTrue(REST[3] < REST[2] && REST[2] < REST[1]);
	}

	@Test
	void itsRingsSkimLowForAJumpOrHighForACrouch() {
		assertArrayEquals(new boolean[] {false}, rings(1));
		assertArrayEquals(new boolean[] {false, true}, rings(2));
		assertArrayEquals(new boolean[] {false, true, false}, rings(3));
		// Standing in its path, it catches you either way.
		assertTrue(skims(false, 6, 5.6, 6.1, 0, 1.8));
		assertTrue(skims(true, 6, 5.6, 6.1, 0, 1.8));
		// Jumped, the low one passes under; crouched, the high one passes over.
		assertFalse(skims(false, 6, 5.6, 6.1, 1.0, 2.8));
		assertFalse(skims(true, 6, 5.6, 6.1, 0, 1.5));
		// Up on a pillar, neither reaches.
		assertFalse(skims(false, 6, 5.6, 6.1, 4, 5.8));
		assertFalse(skims(true, 6, 5.6, 6.1, 4, 5.8));
		// Only where the ring is this tick.
		assertFalse(skims(false, 9, 5.6, 6.1, 0, 1.8));
		assertFalse(skims(false, 3, 5.6, 6.1, 0, 1.8));
		assertTrue(NOVA_SPEED[3] > NOVA_SPEED[1]);
	}

	@Test
	void itsStareBurnsAlongItsLineAndTurnsNoFasterThanItCan() {
		assertTrue(burns(10, 0.3, GAZE_REACH, gazeHalf(2)));
		assertFalse(burns(10, 1.2, GAZE_REACH, gazeHalf(2)));
		assertFalse(burns(-2, 0, GAZE_REACH, gazeHalf(2)), "nothing behind its eye");
		assertFalse(burns(30, 0, GAZE_REACH, gazeHalf(2)));
		assertTrue(gazeHalf(3) > gazeHalf(2));
		assertEquals(12.5, turn(10, 40, 2.5), 1e-9);
		assertEquals(7.5, turn(10, -40, 2.5), 1e-9);
		assertEquals(11, turn(10, 11, 2.5), 1e-9);
		// The short way round, across the seam.
		assertEquals(172.5, turn(170, -170, 2.5), 1e-9);
		assertEquals(180 + 2.5, turn(180, -175, 2.5), 1e-9);
		assertTrue(gazeTurn(3) > gazeTurn(2));
		assertEquals(-170, wrap(190), 1e-9);
		assertEquals(10, wrap(370), 1e-9);
	}

	@Test
	void itsPlungeAndStarfallLandOnMarksYouCanStepOff() {
		assertTrue(lands(1, 1, 0));
		assertTrue(lands(2.9, 0, 0));
		assertFalse(lands(3.2, 0, 0));
		assertFalse(lands(1, 1, 4), "not up on a pillar");
		assertTrue(fallsOn(0.5, 0.5, 0));
		assertFalse(fallsOn(1.5, 0, 0));
		assertFalse(fallsOn(0, 0, 3));
		assertTrue(grounded(1) > grounded(3));
		assertTrue(grounded(3) >= 20, "the opening after its fall is worth the walk");
		assertEquals(1, within(3, 4, 9), 1e-9);
		assertEquals(0.5, within(12, 16, 10), 1e-9);
	}

	@Test
	void itsCageClosesInAndItsPullGrindsOnlyClose() {
		assertEquals(CAGE_RADIUS, closing(0, 18), 1e-9);
		assertEquals(CAGE_RADIUS / 2, closing(9, 18), 1e-9);
		assertEquals(0, closing(18, 18), 1e-9);
		assertEquals(0, closing(30, 18), 1e-9);
		assertTrue(cage(3) > cage(2));
		assertTrue(cageShut(3) < cageShut(2));
		assertTrue(drag(8) > 0);
		assertEquals(0, drag(1), "no drag once in the grind");
		assertEquals(0, drag(14), "nothing past its reach");
		assertTrue(drag(8) * HORIZON_PULL < HORIZON_RADIUS, "a walk out against the pull can win");
		assertTrue(grinds(2, 0));
		assertFalse(grinds(3, 0));
		assertTrue(collapses(3.5, 0));
		assertFalse(collapses(5, 0));
	}

	@Test
	void itsLashGoesAllTheWayRoundOnce() {
		for (int deg = 0; deg < 360; deg += 5) {
			int hits = 0;
			for (int el = 1; el <= LASH_SPIN; el++) {
				if (swept(deg, el, LASH_SPIN)) {
					hits++;
				}
			}
			assertEquals(1, hits, "at " + deg);
		}
		assertFalse(swept(10, 0, LASH_SPIN), "nothing during its tell");
		assertTrue(lashes(3, 0));
		assertFalse(lashes(4, 0), "a step back out of its reach");
		assertFalse(lashes(2, 5), "nor up over it");
	}

	@Test
	void brokenOpenItShedsShardsYouCanTreadAndCallsThemHome() {
		assertTrue(shed(3) > shed(1));
		// Broken open it has only its lash and its call, and it never makes the same one twice running: it hangs open instead.
		double[] after = weights(1, false, 3, 0, true, 5, rested(), LASH);
		assertEquals(0, after[LASH]);
		assertTrue(after[GATHER] > 0);
		after = weights(1, false, 3, 0, true, 0, rested(), LASH);
		for (double w : after) {
			assertEquals(0, w);
		}
		assertEquals(-1, choose(deckAfter(LASH), 1, false, 3, 0, true, 0, rested(), 0.5));
		for (int p = 1; p <= 3; p++) {
			assertTrue(hasten(p) * shed(p) <= OPEN_TICKS[p], "even all of them home leaves it open a while");
			assertTrue(hasten(p) > 0);
		}
		assertEquals(3, volley(1));
		assertTrue(volley(3) > volley(1));
	}

	private static MoveDeck deckAfter(int move) {
		var deck = new MoveDeck(4);
		deck.played(move);
		return deck;
	}
}
