package dev.wildercord.cast;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static dev.wildercord.cast.CinderWardenRules.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Cinder Warden's moves: what it picks where, what each phase wakes, the shapes its blows strike, and never the same move twice running. */
class CinderWardenRulesTest {
	/** The move it wants most out of {@code w}. */
	private static int most(double[] w) {
		int best = -1;
		for (int m : PICKS) {
			if (w[m] > 0 && (best < 0 || w[m] > w[best])) {
				best = m;
			}
		}
		return best;
	}

	@Test
	void everyMoveHasAFairTellAndItsOwnName() {
		assertEquals(8, PICKS.length);
		Set<String> keys = new HashSet<>();
		for (int m = 1; m <= MOVES; m++) {
			assertTrue(keys.add(key(m)), "two moves share " + key(m));
			assertNotEquals("watching", key(m));
		}
		for (int m : PICKS) {
			int tell = tell(m, false);
			assertTrue(tell >= 10 && tell <= 18, key(m) + " tells for " + tell);
			int chained = tell(m, true);
			assertTrue(chained >= 10 && chained <= tell, key(m) + " run on tells for " + chained);
		}
		// Each aim locks a few ticks before the blow, inside even the shortest tell.
		assertTrue(HAMMER_LOCK < HAMMER_TELL_CHAINED);
		assertTrue(SEAM_LOCK < SEAM_TELL_CHAINED);
		assertTrue(HAUL_LOCK < HAUL_TELL_CHAINED);
		assertTrue(BREATH_LOCK < BELLOWS_TELL);
		// What it is left open in is never picked.
		for (int m : new int[] {STUCK, TANGLED, SPENT, REEL}) {
			assertEquals(0, tell(m, false));
			for (int phase = 1; phase <= 3; phase++) {
				assertFalse(unlocked(m, phase));
			}
		}
	}

	@Test
	void eachPhaseWakesItsOwnMoves() {
		for (int m : new int[] {HAMMERFALL, SWEEP, SEAM, VENT}) {
			assertTrue(unlocked(m, 1));
		}
		for (int m : new int[] {HAUL, SLAG, BELLOWS}) {
			assertFalse(unlocked(m, 1));
			assertTrue(unlocked(m, 2));
		}
		assertFalse(unlocked(FLARE, 2));
		assertTrue(unlocked(FLARE, 3));
		// Locked moves weigh nothing, wherever its prey stands.
		Random random = new Random(3);
		for (int i = 0; i < 500; i++) {
			int phase = 1 + random.nextInt(3);
			double[] w = weights(phase, random.nextDouble() * 20, random.nextDouble() * 360 - 180, random.nextDouble() * 5 - 1,
				1 + random.nextInt(5), random.nextBoolean(), random.nextInt(1000));
			assertEquals(MOVES + 1, w.length);
			for (int m = 1; m <= MOVES; m++) {
				if (!unlocked(m, phase)) {
					assertEquals(0, w[m], key(m) + " in phase " + phase);
				}
			}
		}
	}

	@Test
	void itPicksWhatSuitsWhereItsPreyStands() {
		// Close before it: its fists.
		assertEquals(HAMMERFALL, most(weights(1, 3, 0, 0, 1, false, 0)));
		assertEquals(HAMMERFALL, most(weights(3, 3, 10, 0, 1, false, 0)));
		// At its back: the steam.
		assertEquals(VENT, most(weights(1, 4, 180, 0, 1, false, 0)));
		assertEquals(VENT, most(weights(3, 5, -150, 0, 1, false, 0)));
		// Keeping away: the seam, or the chain cast once it has it.
		assertEquals(SEAM, most(weights(1, 10, 0, 0, 1, false, 0)));
		int far = most(weights(2, 10, 0, 0, 1, false, 0));
		assertTrue(far == SEAM || far == HAUL, key(far));
		// Up out of reach: the slag.
		assertEquals(SLAG, most(weights(2, 3, 0, 4, 1, false, 0)));
		// A crowd in the ring draws its chains; standing still draws slag.
		assertTrue(weights(1, 5, 0, 0, 4, false, 0)[SWEEP] > weights(1, 5, 0, 0, 1, false, 0)[SWEEP]);
		assertTrue(weights(2, 8, 0, 0, 1, true, 0)[SLAG] > weights(2, 8, 0, 0, 1, false, 0)[SLAG]);
		// Its fists and its chains can't reach anyone up out of reach.
		double[] high = weights(3, 3, 0, 4, 1, false, 0);
		assertEquals(0, high[HAMMERFALL]);
		assertEquals(0, high[SWEEP]);
		// The flare only in its last phase, and only once in a long while.
		assertEquals(0, weights(2, 5, 0, 0, 3, false, 10_000)[FLARE]);
		assertEquals(0, weights(3, 5, 0, 0, 3, false, FLARE_EVERY - 1)[FLARE]);
		assertTrue(weights(3, 5, 0, 0, 3, false, FLARE_EVERY)[FLARE] > 0);
		// There's always more than one move it can make, so it never has to make the same one twice.
		Random random = new Random(11);
		for (int i = 0; i < 500; i++) {
			double[] w = weights(1 + random.nextInt(3), random.nextDouble() * 22, random.nextDouble() * 360 - 180,
				random.nextDouble() * 6 - 1, 1 + random.nextInt(5), random.nextBoolean(), random.nextInt(600));
			int open = 0;
			for (int m : PICKS) {
				if (w[m] > 0) {
					open++;
				}
			}
			assertTrue(open >= 2);
		}
	}

	@Test
	void itRestsLessEachPhaseAndOnlyRunsOnInTheLast() {
		for (double roll = 0; roll <= 1; roll += 0.05) {
			for (int m : PICKS) {
				assertTrue(rest(1, m, roll) > rest(2, m, roll));
				assertTrue(rest(2, m, roll) > rest(3, m, roll));
			}
			for (int m : new int[] {STUCK, TANGLED, SPENT, REEL}) {
				assertTrue(rest(3, m, roll) < rest(3, HAMMERFALL, roll));
			}
		}
		for (int m : PICKS) {
			for (double d = 0; d <= 20; d += 0.5) {
				assertEquals(-1, chain(m, 1, d, true, 0));
				assertEquals(-1, chain(m, 2, d, true, 0));
				// Never into itself, and never at all past the chance.
				assertNotEquals(m, chain(m, 3, d, true, 0));
				assertNotEquals(m, chain(m, 3, d, false, 0));
				assertEquals(-1, chain(m, 3, d, true, CHAIN_CHANCE));
			}
		}
		assertEquals(SEAM, chain(SWEEP, 3, 6, false, 0));
		assertEquals(HAMMERFALL, chain(SWEEP, 3, 2, false, 0));
		assertEquals(HAMMERFALL, chain(HAUL, 3, 2, true, 0));
		assertEquals(-1, chain(HAUL, 3, 8, false, 0));
		assertEquals(HAUL, chain(SEAM, 3, 8, false, 0));
		// Those that leave it open never run on.
		assertEquals(-1, chain(HAMMERFALL, 3, 3, false, 0));
		assertEquals(-1, chain(FLARE, 3, 3, false, 0));
		assertEquals(-1, chain(BELLOWS, 3, 3, false, 0));
		assertEquals(-1, chain(VENT, 3, 3, false, 0));
	}

	@Test
	void anglesWrapAndItsBackIsItsBack() {
		assertEquals(10, wrap(370), 1e-9);
		assertEquals(170, wrap(-190), 1e-9);
		assertEquals(180, wrap(-180), 1e-9);
		assertTrue(behind(180));
		assertTrue(behind(-150));
		assertTrue(behind(540));
		assertFalse(behind(100));
		assertFalse(behind(0));
	}

	@Test
	void theHammerfallStrikesTheMarkedRingAndItsWallCanBeJumped() {
		// Under its fists, and beside it, but not behind it, not past the ring, and not high above.
		assertTrue(hammerHits(HAMMER_AHEAD, 0, 0));
		assertTrue(hammerHits(0, 3, 0));
		assertFalse(hammerHits(-2.5, 0, 0));
		assertFalse(hammerHits(HAMMER_AHEAD + HAMMER_RADIUS + 0.1, 0, 0));
		assertFalse(hammerHits(HAMMER_AHEAD, 0, HAMMER_HEIGHT + 0.5));
		// The wall of fire catches whoever it rolls over on the floor; a jump clears it.
		assertTrue(crosses(5, 4.8, 5.2, 0));
		assertFalse(crosses(5, 4.8, 5.2, WAVE_HEIGHT + 0.1));
		assertFalse(crosses(8, 4.8, 5.2, 0));
		assertTrue(waveSpeed(3) > waveSpeed(2));
		assertTrue(waveReach(3) > waveReach(2));
	}

	@Test
	void theSweepPassesOverWhatItTurnsThrough() {
		assertTrue(inRing(5, SWEEP_INNER, SWEEP_REACH));
		assertFalse(inRing(1.5, SWEEP_INNER, SWEEP_REACH));
		assertFalse(inRing(SWEEP_REACH + 0.5, SWEEP_INNER, SWEEP_REACH));
		assertTrue(sweepReach(3) > sweepReach(1));
		// Either way round.
		assertTrue(swept(10, 0, 36));
		assertFalse(swept(40, 0, 36));
		assertTrue(swept(-5, 0, -36));
		assertFalse(swept(-40, 0, -36));
		// Past a full turn's mark, and whole turns in.
		assertTrue(swept(5, 350, 386));
		assertTrue(swept(350, 340, 376));
		assertTrue(swept(-355, 0, 36));
		assertTrue(swept(10, 1080, 1116));
		assertFalse(swept(180, 1080, 1116));
		// A turn of a whole circle passes over everything.
		assertTrue(swept(123, 0, 400));
	}

	@Test
	void lanesAndConesHaveTheirEdges() {
		// The seam's lane: from its feet out, and a step wide.
		assertTrue(inLane(5, 0.5, SEAM_FROM, SEAM_TO, SEAM_HALF));
		assertFalse(inLane(5, 1.5, SEAM_FROM, SEAM_TO, SEAM_HALF));
		assertFalse(inLane(SEAM_TO + 1, 0, SEAM_FROM, SEAM_TO, SEAM_HALF));
		assertFalse(inLane(0.5, 0, SEAM_FROM, SEAM_TO, SEAM_HALF));
		assertEquals(1, seams(2).length);
		assertEquals(3, seams(3).length);
		// The breath's cone: before it, in reach, and not too far round.
		assertTrue(inCone(5, 0, BREATH_REACH, BREATH_HALF));
		assertFalse(inCone(5, 5, BREATH_REACH, BREATH_HALF));
		assertFalse(inCone(-1, 0, BREATH_REACH, BREATH_HALF));
		assertFalse(inCone(BREATH_REACH + 1, 0, BREATH_REACH, BREATH_HALF));
		assertTrue(breathTurn(3) > breathTurn(2));
		// The steam blasts out behind it, wider and further after the first phase.
		assertTrue(vented(-4, 0, 1));
		assertFalse(vented(4, 0, 1));
		assertTrue(vented(-4, 4.5, 1));
		assertFalse(vented(-3, 4.5, 1));
		assertTrue(vented(-3, 4.5, 2));
		assertFalse(vented(-7.5, 0, 1));
		assertTrue(vented(-7.5, 0, 2));
	}

	@Test
	void theHaulAndTheFlare() {
		// Hauled in harder the further off, but never flung past it.
		assertEquals(0.3, tug(1.5), 1e-9);
		double was = 0;
		for (double d = 0; d <= HAUL_REACH + 5; d += 0.5) {
			double pull = tug(d);
			assertTrue(pull >= was && pull <= HAUL_TUG_MOST);
			was = pull;
		}
		// Only stone shelters from the flare, out to its edge.
		assertTrue(flared(10, false));
		assertFalse(flared(10, true));
		assertFalse(flared(FLARE_RADIUS + 1, false));
	}

	@Test
	void aRunOfPicksNeverRepeatsAMove() {
		Random random = new Random(7);
		MoveDeck deck = new MoveDeck(4);
		int last = -1;
		int[] seen = new int[MOVES + 1];
		for (int i = 0; i < 300; i++) {
			int phase = 1 + i / 100;
			double distance = random.nextDouble() * 18;
			double[] all = weights(phase, distance, random.nextDouble() * 360 - 180, random.nextDouble() * 4 - 1,
				1 + random.nextInt(4), random.nextBoolean(), random.nextInt(600));
			double[] w = new double[PICKS.length];
			for (int k = 0; k < PICKS.length; k++) {
				w[k] = all[PICKS[k]];
			}
			int move = deck.pick(PICKS, w, random.nextDouble());
			assertTrue(move > 0);
			assertNotEquals(last, move, "picked " + key(move) + " twice running");
			assertTrue(unlocked(move, phase));
			deck.played(move);
			seen[move]++;
			last = move;
			// In its last phase a move may run on into another.
			int next = chain(move, phase, distance, random.nextBoolean(), random.nextDouble());
			if (next > 0) {
				assertNotEquals(last, next);
				deck.played(next);
				seen[next]++;
				last = next;
			}
		}
		// It makes all of them, over a long fight.
		for (int m : PICKS) {
			assertTrue(seen[m] > 0, key(m) + " never made");
		}
	}
}
