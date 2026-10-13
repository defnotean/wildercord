package dev.wildercord.cast;

import static dev.wildercord.cast.RootGuardianRules.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RootGuardianRulesTest {
	private static int best(double[] w) {
		int best = -1;
		for (int m : PICKS) if (best < 0 || w[m] > w[best]) best = m;
		return best;
	}

	@Test void everyMoveWarnsReadablyAndLessWhenRunOnFromAnother() {
		for (int m : PICKS) {
			assertTrue(tell(m, false) >= 10 && tell(m, false) <= 18, "tell of " + key(m));
			assertTrue(tell(m, true) >= 10 && tell(m, true) <= tell(m, false), "chained tell of " + key(m));
		}
		assertTrue(SPEARS_LOCK < SPEARS_TELL && LASH_LOCK < LASH_TELL && SEEDS_LOCK < SEEDS_TELL && SAP_LOCK < SAP_TELL && UPROOT_LOCK < UPROOT_TELL && CANOPY_LOCK < CANOPY_TELL);
		assertTrue(SPEARS_LOCK < SPEARS_CHAINED && UPROOT_LOCK < UPROOT_CHAINED && CANOPY_LOCK < CANOPY_CHAINED);
	}

	@Test void everyMoveAndPoseHasItsOwnName() {
		Set<String> keys = new HashSet<>();
		for (int m = 1; m <= POSES; m++) assertTrue(!key(m).isBlank() && keys.add(key(m)), "key of " + m);
		assertEquals("", key(0));
		assertEquals(PICKS.length, 9);
		assertTrue(POSES < DungeonBoss.MOVES);
	}

	@Test void laterMovesStayLockedUntilTheirPhase() {
		for (double d = 0; d < 16; d += 0.5) for (double a = -180; a <= 180; a += 30) {
			double[] one = weights(1, d, a, 0, 3, 3, 3, false, 0), two = weights(2, d, a, 0, 3, 3, 3, false, 0);
			for (int m : new int[] {THORNS, UPROOT, POLLEN, CANOPY}) assertEquals(0, one[m], key(m) + " in the first phase");
			assertEquals(0, two[CANOPY], "the canopy in the second phase");
		}
		double[] three = weights(3, 5, 0, 0, 3, 3, 3, false, 0);
		for (int m : PICKS) assertTrue(three[m] > 0, key(m) + " in the last phase");
		assertTrue(weights(2, 3, 0, 0, 3, 1, 1, false, 0)[THORNS] > 0);
	}

	@Test void beforeItCloseInItLashesAndBehindItThornsBurstOut() {
		assertEquals(LASH, best(weights(1, 3, 0, 0, 3, 1, 1, false, 0)));
		assertEquals(THORNS, best(weights(2, 3, 170, 0, 3, 1, 1, false, 0)));
		assertEquals(THORNS, best(weights(3, 3, 0, 0, 3, 4, 4, false, 0)), "a crowd round it");
		assertEquals(0, weights(1, 3, 0, 0, 0, 1, 1, false, 0)[LASH], "no binding left to swing");
		assertEquals(0, weights(1, 8, 0, 0, 3, 1, 1, false, 0)[LASH], "out of the lash's reach");
	}

	@Test void whoeverKeepsFarOffIsHuntedByTheRootAndNotLashed() {
		double[] far = weights(1, 13, 0, 0, 3, 0, 1, false, 0);
		assertEquals(CREEP, best(far));
		assertEquals(0, far[LASH]);
		assertTrue(far[CREEP] > far[SEEDS] && far[SPEARS] > far[SEEDS]);
		double[] close = weights(1, 2, 0, 0, 3, 1, 1, false, 0);
		assertEquals(0, close[CREEP]);
		assertTrue(weights(1, 7, 0, 0, 3, 1, 1, false, 0)[SEEDS] > close[SEEDS] * 5, "pods for the middle ground");
		assertTrue(weights(2, 7, 0, 0, 3, 1, 1, false, 0)[UPROOT] > weights(2, 2, 0, 0, 3, 1, 1, false, 0)[UPROOT], "a charge needs room");
	}

	@Test void itHoldsBackFromWhatCannotReachAndWhatIsAlreadyOut() {
		double[] high = weights(3, 5, 0, 4, 3, 1, 1, false, 0);
		assertEquals(0, high[UPROOT]);
		assertEquals(0, high[LASH]);
		assertEquals(0, high[CREEP]);
		assertTrue(high[SPEARS] > 0);
		assertEquals(0, weights(2, 3, 0, 0, 3, 1, 1, true, 0)[POLLEN]);
		assertTrue(weights(1, 7, 0, 0, 3, 1, 1, false, 5)[SEEDS] < weights(1, 7, 0, 0, 3, 1, 1, false, 0)[SEEDS], "briars already thick");
		assertTrue(weights(3, 6, 0, 0, 3, 1, 4, false, 0)[CANOPY] > weights(3, 6, 0, 0, 3, 1, 1, false, 0)[CANOPY]);
		assertTrue(spellWeight(12) > spellWeight(4) && spellWeight(4) < weights(1, 4, 0, 0, 3, 1, 1, false, 0)[LASH]);
	}

	@Test void itChainsOnlyInItsLastPhaseAndNeverIntoItself() {
		for (int m = 1; m <= POSES; m++) for (double d = 0; d < 14; d += 0.5) for (int b = 0; b <= 3; b++) {
			assertEquals(-1, chain(1, m, d, b, 0), "first phase");
			assertEquals(-1, chain(2, m, d, b, 0), "second phase");
			assertEquals(-1, chain(3, m, d, b, CHAIN_CHANCE), "an unlucky roll");
			int next = chain(3, m, d, b, 0);
			assertNotEquals(m, next);
			if (b == 0) assertNotEquals(LASH, next, "no binding to swing");
			if (next > 0) assertTrue(next <= PICKS.length);
		}
		assertEquals(LASH, chain(3, CREEP, 2, 3, 0));
		assertEquals(UPROOT, chain(3, SAP, 6, 3, 0));
		assertEquals(-1, chain(3, CANOPY, 6, 3, 0));
		assertEquals(-1, chain(3, UPROOT, 6, 3, 0));
	}

	@Test void itRestsLessEachPhaseAndLongestAfterItsHeaviest() {
		assertTrue(rest(3, SPEARS, 0) < rest(2, SPEARS, 0) && rest(2, SPEARS, 0) < rest(1, SPEARS, 0));
		assertTrue(rest(1, CANOPY, 0) > rest(1, SPEARS, 0));
		assertTrue(rest(1, SNAGGED, 0) < rest(1, SPEARS, 0));
		assertTrue(rest(1, SPEARS, 0.99) > rest(1, SPEARS, 0));
	}

	@Test void itsBindingsTurnMostOfABlowAndLessWhenItIsTangled() {
		assertEquals(0.35F, taken(3, false), 1e-6);
		assertEquals(0.35F, taken(1, false), 1e-6);
		assertEquals(0.7F, taken(2, true), 1e-6);
		assertEquals(1F, taken(0, false), 1e-6);
		assertEquals(1F, taken(0, true), 1e-6);
	}

	@Test void theShapesOnTheFloor() {
		assertEquals(170, wrap(-190), 1e-9);
		assertEquals(-170, wrap(190), 1e-9);
		assertEquals(0, wrap(720), 1e-9);
		assertTrue(inArc(3, 0, LASH_REACH, LASH_HALF));
		assertTrue(inArc(0.5, 2, LASH_REACH, LASH_HALF), "almost at its side");
		assertFalse(inArc(-1, 1, LASH_REACH, LASH_HALF), "behind it");
		assertFalse(inArc(6, 0, LASH_REACH, LASH_HALF), "out of reach");
		assertTrue(onLane(5, 0.9, SAP_FROM, SAP_TO, SAP_HALF));
		assertFalse(onLane(5, 1.5, SAP_FROM, SAP_TO, SAP_HALF), "a step aside");
		assertFalse(onLane(0.5, 0, SAP_FROM, SAP_TO, SAP_HALF), "at its feet");
		assertEquals(4, spoke(4, 0, THORNS_SPOKES, 0, THORNS_HALF), 1e-9);
		double step = Math.toRadians(360.0 / THORNS_SPOKES);
		assertEquals(3, spoke(3 * Math.cos(step), 3 * Math.sin(step), THORNS_SPOKES, 0, THORNS_HALF), 1e-9, "the next spoke round");
		double r = 2.5, a = step / 2;
		assertEquals(-1, spoke(r * Math.cos(a), r * Math.sin(a), THORNS_SPOKES, 0, THORNS_HALF), 1e-9, "between the spokes, even close in");
		assertTrue(r * Math.sin(a) - THORNS_HALF > 0.5, "room to stand between them at arm's length");
		assertEquals(r, spoke(r * Math.cos(a), r * Math.sin(a), THORNS_SPOKES, 180.0 / THORNS_SPOKES, THORNS_HALF), 1e-9, "the second burst fills the gaps");
		assertEquals(THORNS_FROM + 3 * THORNS_SPEED, front(3, THORNS_SPEED, THORNS_FROM), 1e-9);
		assertTrue(crossed(1, 3, 2) && !crossed(1, 3, 1) && crossed(1, 3, 3) && !crossed(1, 3, 3.5));
		assertEquals(16, steer(10, 40, 6), 1e-9);
		assertEquals(-176, steer(178, -150, 6), 1e-9, "turns the short way round");
		assertEquals(1, lob(0.5), 1e-9);
		assertEquals(0, lob(0), 1e-9);
		assertEquals(SEEDS_NEAR, podReach(1), 1e-9);
		assertEquals(6, podReach(6), 1e-9);
		assertEquals(SEEDS_FAR, podReach(20), 1e-9);
		assertTrue(SEEDS_NEAR - 2 > SEEDS_RADIUS, "close under its crown no pod lands on you");
	}

	@Test void aLongFightNeverRepeatsAMoveBackToBack() {
		Random random = new Random(7);
		MoveDeck deck = new MoveDeck(4);
		int last = -1, spells = 0, own = 0;
		for (int phase = 1; phase <= 3; phase++) {
			deck.clear();
			last = -1;
			int count = phase == 3 ? 2 : 2;
			for (int i = 0; i < 600; i++) {
				double d = random.nextDouble() * 14, angle = random.nextDouble() * 360 - 180, height = random.nextDouble() < 0.1 ? 3 : 0;
				int bindings = random.nextInt(4), crowd = random.nextInt(4), foes = 1 + random.nextInt(4);
				double[] w = weights(phase, d, angle, height, bindings, crowd, foes, random.nextBoolean(), random.nextInt(6));
				int[] moves = new int[PICKS.length + count];
				double[] weights = new double[moves.length];
				for (int k = 0; k < PICKS.length; k++) {
					moves[k] = PICKS[k];
					weights[k] = w[PICKS[k]];
				}
				for (int k = 0; k < count; k++) {
					moves[PICKS.length + k] = DungeonBoss.SPELL + k;
					weights[PICKS.length + k] = spellWeight(d);
				}
				int picked = deck.pick(moves, weights, random.nextDouble());
				assertTrue(picked > 0);
				assertNotEquals(last, picked, "the same move twice running");
				deck.played(picked);
				last = picked;
				if (picked >= DungeonBoss.SPELL) {
					spells++;
					continue;
				}
				own++;
				int next = chain(phase, picked, d, bindings, random.nextDouble());
				if (next > 0) {
					assertNotEquals(last, next, "a chain into itself");
					deck.played(next);
					last = next;
				}
			}
		}
		assertTrue(own > spells * 2, "its own moves are the heart of the fight");
	}
}
