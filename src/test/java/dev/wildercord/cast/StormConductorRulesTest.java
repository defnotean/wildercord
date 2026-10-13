package dev.wildercord.cast;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StormConductorRulesTest {
	/** Where the fight numbers its spells, past any move of a boss's own. */
	private static final int SPELL = 100;

	/** Every move made long enough ago to be ready again. */
	private static long[] rested() {
		long[] since = new long[StormConductorRules.MOVES + 1];
		Arrays.fill(since, 100_000);
		return since;
	}

	private static double weight(double[] w, int move) {
		for (int i = 0; i < StormConductorRules.PICKS.length; i++) {
			if (StormConductorRules.PICKS[i] == move) {
				return w[i];
			}
		}
		throw new IllegalArgumentException("not a pick: " + move);
	}

	/** Its prey out in the open, {@code distance} off, in front of it, all three rods standing. */
	private static double[] open(int phase, double distance) {
		return StormConductorRules.weights(phase, distance, false, false, false, 2, rested());
	}

	/** Its prey sheltering at a quiet rod's foot, {@code distance} off. */
	private static double[] sheltered(int phase, double distance) {
		return StormConductorRules.weights(phase, distance, false, true, false, 2, rested());
	}

	/** Its prey closing on the sparking rod, {@code distance} off. */
	private static double[] closing(int phase, double distance) {
		return StormConductorRules.weights(phase, distance, true, false, false, 2, rested());
	}

	// ------------------------------------------------------------------ what it wants

	@Test void theDownbeatIsForAnyoneOutInTheOpen() {
		assertTrue(weight(open(1, 6), StormConductorRules.DOWNBEAT) > weight(sheltered(1, 6), StormConductorRules.DOWNBEAT));
	}

	@Test void theGaleAndTheChordAreForAnyoneShelteringAtARod() {
		assertTrue(weight(sheltered(2, 6), StormConductorRules.GALE) > weight(open(2, 6), StormConductorRules.GALE));
		assertTrue(weight(closing(2, 6), StormConductorRules.GALE) > weight(open(2, 6), StormConductorRules.GALE));
		assertTrue(weight(sheltered(2, 6), StormConductorRules.CHORD) > weight(open(2, 6), StormConductorRules.CHORD));
		double[] at = sheltered(2, 6);
		assertTrue(weight(at, StormConductorRules.GALE) > weight(at, StormConductorRules.DOWNBEAT), "a rod draws the downbeat anyway");
	}

	@Test void itHoldsItsNoteForAnyoneClosingOnTheSparkingRod() {
		assertTrue(weight(closing(1, 6), StormConductorRules.FERMATA) > weight(open(1, 6), StormConductorRules.FERMATA));
	}

	@Test void itDischargesOnlyForAnyoneAtItsFeet() {
		assertTrue(weight(open(1, 2), StormConductorRules.DISCHARGE) > 0);
		assertEquals(0, weight(open(1, 6), StormConductorRules.DISCHARGE), 1e-9);
		double[] hug = open(1, 2);
		for (int m : StormConductorRules.PICKS) {
			if (m != StormConductorRules.DISCHARGE && m != StormConductorRules.GALE) {
				assertTrue(weight(hug, StormConductorRules.DISCHARGE) > weight(hug, m), StormConductorRules.key(m) + " over the discharge");
			}
		}
	}

	@Test void itNeverBlowsItsGaleAtYourBackOrPastItsReach() {
		long[] since = rested();
		assertEquals(0, weight(StormConductorRules.weights(1, 5, false, true, true, 2, since), StormConductorRules.GALE), 1e-9);
		assertEquals(0, weight(StormConductorRules.weights(1, 12, false, true, false, 2, since), StormConductorRules.GALE), 1e-9);
		assertTrue(weight(StormConductorRules.weights(1, 5, false, true, false, 2, since), StormConductorRules.GALE) > 0);
	}

	@Test void theBallIsForAnyoneKeepingOffAndNotForAnyoneAtTheSparkingRod() {
		assertTrue(weight(open(2, 8), StormConductorRules.BALL) > weight(open(2, 2), StormConductorRules.BALL));
		assertTrue(weight(open(2, 8), StormConductorRules.BALL) > weight(closing(2, 8), StormConductorRules.BALL));
	}

	@Test void itRidesTheStaccatoOutToMiddlingRange() {
		assertTrue(weight(open(1, 7), StormConductorRules.STACCATO) > weight(open(1, 1.5), StormConductorRules.STACCATO));
		assertTrue(weight(open(1, 7), StormConductorRules.STACCATO) > weight(open(1, 16), StormConductorRules.STACCATO));
	}

	@Test void itCastsMoreForPreyPastItsOwnReach() {
		assertTrue(StormConductorRules.spellWeight(16) > StormConductorRules.spellWeight(6));
	}

	@Test void itWaitsOutEachMovesCooldown() {
		long[] since = rested();
		since[StormConductorRules.DOWNBEAT] = 10;
		assertEquals(0, weight(StormConductorRules.weights(1, 6, false, false, false, 2, since), StormConductorRules.DOWNBEAT), 1e-9);
		since[StormConductorRules.DOWNBEAT] = StormConductorRules.cooldown(StormConductorRules.DOWNBEAT, 1);
		assertTrue(weight(StormConductorRules.weights(1, 6, false, false, false, 2, since), StormConductorRules.DOWNBEAT) > 0);
	}

	// ------------------------------------------------------------------ phases

	@Test void itsLaterMovesWaitForTheirPhase() {
		for (int m : new int[] {StormConductorRules.BALL, StormConductorRules.CHORD, StormConductorRules.ROLL}) {
			assertEquals(0, weight(sheltered(1, 6), m), 1e-9, StormConductorRules.key(m) + " in its first phase");
			assertTrue(weight(sheltered(2, 6), m) > 0, StormConductorRules.key(m) + " in its second phase");
		}
		assertEquals(0, weight(open(2, 6), StormConductorRules.COUNTERPOINT), 1e-9);
		assertTrue(weight(open(3, 6), StormConductorRules.COUNTERPOINT) > 0);
		for (int m : StormConductorRules.PICKS) {
			assertTrue(StormConductorRules.unlocks(m) >= 1 && StormConductorRules.unlocks(m) <= 3);
		}
	}

	@Test void itNeedsTwoQuietRodsForItsChord() {
		assertEquals(0, weight(StormConductorRules.weights(2, 6, false, true, false, 1, rested()), StormConductorRules.CHORD), 1e-9);
		assertTrue(weight(StormConductorRules.weights(2, 6, false, true, false, 2, rested()), StormConductorRules.CHORD) > 0);
		assertFalse(StormConductorRules.open(StormConductorRules.CHORD, 3, 0));
	}

	@Test void itQuickensInItsThirdPhase() {
		for (int m : StormConductorRules.PICKS) {
			assertTrue(StormConductorRules.cooldown(m, 3) < StormConductorRules.cooldown(m, 1), StormConductorRules.key(m));
			assertTrue(StormConductorRules.rest(m, 3) < StormConductorRules.rest(m, 2), StormConductorRules.key(m));
			assertTrue(StormConductorRules.rest(m, 2) <= StormConductorRules.rest(m, 1), StormConductorRules.key(m));
			assertTrue(StormConductorRules.cooldown(m, 1) > StormConductorRules.length(m, 1), StormConductorRules.key(m) + " outlasts its cooldown");
			assertTrue(StormConductorRules.length(m, 3) >= StormConductorRules.length(m, 1), StormConductorRules.key(m));
		}
		assertTrue(StormConductorRules.waves(3) > StormConductorRules.waves(2));
		assertTrue(StormConductorRules.lines(3) > StormConductorRules.lines(1));
		assertTrue(StormConductorRules.length(StormConductorRules.DOWNBEAT, 3) > StormConductorRules.length(StormConductorRules.DOWNBEAT, 2), "a second beat");
		assertTrue(StormConductorRules.length(StormConductorRules.CRESCENDO, 3) > StormConductorRules.length(StormConductorRules.CRESCENDO, 2), "a second ring");
		assertTrue(StormConductorRules.length(StormConductorRules.CHORD, 3) > StormConductorRules.length(StormConductorRules.CHORD, 2), "a second pulse");
	}

	// ------------------------------------------------------------------ the shape of each move

	@Test void everyMoveHasItsOwnNameAndAFairTell() {
		Set<String> keys = new HashSet<>();
		for (int m = 1; m <= StormConductorRules.MOVES; m++) {
			assertNotNull(StormConductorRules.key(m));
			assertTrue(keys.add(StormConductorRules.key(m)), "two moves called " + StormConductorRules.key(m));
		}
		assertNull(StormConductorRules.key(0));
		assertEquals(10, StormConductorRules.PICKS.length);
		for (int m : StormConductorRules.PICKS) {
			int tell = StormConductorRules.tell(m);
			assertTrue(tell >= 10 && tell <= 18, StormConductorRules.key(m) + " tell " + tell);
			assertFalse(StormConductorRules.opening(m));
			if (StormConductorRules.aimed(m)) {
				int lock = StormConductorRules.lock(m);
				assertTrue(lock >= 3 && lock <= 9 && lock < tell, StormConductorRules.key(m) + " lock " + lock);
			} else {
				assertEquals(0, StormConductorRules.lock(m));
			}
			assertTrue(StormConductorRules.length(m, 1) > tell, StormConductorRules.key(m) + " lands after its tell");
		}
	}

	@Test void itsOpeningsLeaveItOpenAWhile() {
		for (int m : new int[] {StormConductorRules.WINDED, StormConductorRules.SPENT}) {
			assertTrue(StormConductorRules.opening(m));
			assertEquals(0, StormConductorRules.tell(m));
			assertTrue(StormConductorRules.unlocks(m) > 3, "never picked");
			assertTrue(StormConductorRules.length(m, 3) >= 30);
		}
	}

	@Test void theDownbeatLandsOnItsMarkUnlessARodDrawsItDown() {
		assertTrue(StormConductorRules.marked(0, 0, 0));
		assertTrue(StormConductorRules.marked(1.0, 1.0, 0));
		assertFalse(StormConductorRules.marked(1.5, 1.0, 0), "stepped off the mark");
		assertFalse(StormConductorRules.marked(0, 0, 4), "high above it");
		assertTrue(StormConductorRules.drawn(1.5, 0));
		assertTrue(StormConductorRules.drawn(1.2, 1.2));
		assertFalse(StormConductorRules.drawn(2.5, 0));
	}

	@Test void aRingCatchesOnlyThoseWhoDoNotJumpIt() {
		assertTrue(StormConductorRules.crosses(5.2, 5.0, 5.5, 0));
		assertTrue(StormConductorRules.crosses(5.2, 5.5, 5.0, 0), "rolling in");
		assertFalse(StormConductorRules.crosses(5.2, 5.0, 5.5, 1.2), "jumped");
		assertFalse(StormConductorRules.crosses(8, 5.0, 5.5, 0), "not there yet");
		assertEquals(22, StormConductorRules.rolls(11, 0.5));
	}

	@Test void theSparksPopNearToFarOnTheBeat() {
		assertEquals(0, StormConductorRules.popping(0));
		assertEquals(-1, StormConductorRules.popping(1));
		assertEquals(1, StormConductorRules.popping(StormConductorRules.STAC_EVERY));
		assertEquals(-1, StormConductorRules.popping(StormConductorRules.STAC_COUNT * StormConductorRules.STAC_EVERY));
		assertEquals(-1, StormConductorRules.popping(-2));
		assertTrue(StormConductorRules.spark(1) > StormConductorRules.spark(0));
		assertEquals(StormConductorRules.STAC_STEP * StormConductorRules.STAC_COUNT, StormConductorRules.spark(StormConductorRules.STAC_COUNT - 1), 1e-9);
		assertTrue(StormConductorRules.sparked(0.5, 0.5, 0));
		assertFalse(StormConductorRules.sparked(1.0, 0.5, 0), "beside the line");
		assertEquals(0, StormConductorRules.lane(0, 1, 30, 0), 1e-9);
		assertEquals(-30, StormConductorRules.lane(0, 3, 30, 0), 1e-9);
		assertEquals(30, StormConductorRules.lane(2, 3, 30, 0), 1e-9);
	}

	@Test void theHeldNoteSparesWhoeverKeepsStillOrCreeps() {
		assertTrue(StormConductorRules.still(0));
		assertTrue(StormConductorRules.still(0.065 * StormConductorRules.FERM_EVERY), "creeping");
		assertFalse(StormConductorRules.still(0.215 * StormConductorRules.FERM_EVERY), "walking");
	}

	@Test void theGaleFillsAConeBeforeIt() {
		assertTrue(StormConductorRules.blown(5, 0, 0, false));
		assertTrue(StormConductorRules.blown(5, 5, 0, false), "45 degrees off");
		assertFalse(StormConductorRules.blown(5, 7, 0, false), "54 degrees off");
		assertTrue(StormConductorRules.blown(5, 7, 0, true), "wider in its third phase");
		assertFalse(StormConductorRules.blown(-2, 0, 0, true), "behind it");
		assertFalse(StormConductorRules.blown(10, 0, 0, true), "past its reach");
		assertTrue(StormConductorRules.shove(true) < StormConductorRules.shove(false), "crouching into it");
	}

	@Test void theDischargeReachesFartherInItsThirdPhase() {
		assertTrue(StormConductorRules.discharged(3, 0, false));
		assertFalse(StormConductorRules.discharged(4, 0, false));
		assertTrue(StormConductorRules.discharged(4, 0, true));
		assertFalse(StormConductorRules.discharged(5, 0, true));
	}

	@Test void theBallDriftsSlowerThanYouWalkAndTheSparkingRodSwallowsIt() {
		assertTrue(StormConductorRules.BALL_FOLLOW_FAST < 0.215, "you can always outwalk it");
		double f = StormConductorRules.pace(3, 4, StormConductorRules.BALL_FOLLOW);
		assertEquals(StormConductorRules.BALL_FOLLOW, f * 5, 1e-9);
		assertEquals(1, StormConductorRules.pace(0.05, 0.05, StormConductorRules.BALL_FOLLOW), 1e-9);
		assertTrue(StormConductorRules.touches(0.5, 1.0, 0.5));
		assertFalse(StormConductorRules.touches(1.0, 1.0, 1.0));
		assertFalse(StormConductorRules.touches(0, 3, 0), "under it");
		assertTrue(StormConductorRules.swallowed(1.0, 1.0));
		assertFalse(StormConductorRules.swallowed(2.0, 0.5));
		assertTrue(StormConductorRules.popped(1.5, 1.0, 0));
		assertFalse(StormConductorRules.popped(2.0, 1.0, 0));
	}

	@Test void theChordRunsAlongItsLinesEndToEnd() {
		assertEquals(0, StormConductorRules.segment(5, 0, 0, 0, 10, 0), 1e-9);
		assertEquals(2, StormConductorRules.segment(5, 2, 0, 0, 10, 0), 1e-9);
		assertEquals(5, StormConductorRules.segment(-3, 4, 0, 0, 10, 0), 1e-9, "past its end");
		assertEquals(5, StormConductorRules.segment(3, 4, 0, 0, 0, 0), 1e-9, "a line with no length");
		assertTrue(StormConductorRules.chorded(0.5, 0));
		assertFalse(StormConductorRules.chorded(1.2, 0));
	}

	@Test void theCounterpointStrikesHalfTheSquaresThenTheOther() {
		assertEquals(0, StormConductorRules.tile(1.9));
		assertEquals(-1, StormConductorRules.tile(-0.1));
		assertEquals(3, StormConductorRules.centre(1), 1e-9);
		int struck = 0;
		for (double x = -9.5; x <= 9.5; x += 0.5) {
			for (double z = -9.5; z <= 9.5; z += 0.5) {
				boolean first = StormConductorRules.counter(x, z, 0, 0), second = StormConductorRules.counter(x, z, 1, 0);
				assertFalse(first && second, "struck twice at " + x + "," + z);
				if (first) {
					struck++;
				}
			}
		}
		assertTrue(struck > 50);
		assertTrue(StormConductorRules.counter(2.5, 0.5, 1, 0) != StormConductorRules.counter(4.5, 0.5, 1, 0), "the next square over is the other half");
		assertFalse(StormConductorRules.counter(0.5, 0.5, 0, 0) || StormConductorRules.counter(0.5, 0.5, 1, 0), "its own podium");
		assertFalse(StormConductorRules.counter(13, 0.5, 0, 0) || StormConductorRules.counter(13, 0.5, 1, 0), "past its reach");
	}

	@Test void itKnowsWhatIsAtItsBack() {
		assertTrue(StormConductorRules.behind(180));
		assertTrue(StormConductorRules.behind(-140));
		assertFalse(StormConductorRules.behind(30));
		assertEquals(-170, StormConductorRules.wrap(190), 1e-9);
	}

	// ------------------------------------------------------------------ chains

	@Test void inItsThirdPhaseOneMoveRunsOnIntoAnother() {
		assertEquals(StormConductorRules.DOWNBEAT, StormConductorRules.chain(StormConductorRules.FERMATA, 0.1), "the held note into the downbeat");
		assertEquals(StormConductorRules.STACCATO, StormConductorRules.chain(StormConductorRules.GALE, 0.1));
		assertEquals(StormConductorRules.ROLL, StormConductorRules.chain(StormConductorRules.CRESCENDO, 0.1));
		assertEquals(-1, StormConductorRules.chain(StormConductorRules.FERMATA, 0.9), "not every time");
		assertEquals(-1, StormConductorRules.chain(StormConductorRules.DISCHARGE, 0.1), "it is spent after a discharge");
		for (int m = 1; m <= StormConductorRules.MOVES; m++) {
			int next = StormConductorRules.chain(m, 0);
			assertNotEquals(m, next, StormConductorRules.key(m) + " into itself");
			if (next > 0) {
				assertFalse(StormConductorRules.opening(next));
				assertTrue(StormConductorRules.unlocks(next) <= 3);
			}
		}
		assertEquals(-1, StormConductorRules.chain(StormConductorRules.WINDED, 0));
		assertEquals(-1, StormConductorRules.chain(StormConductorRules.SPENT, 0));
	}

	// ------------------------------------------------------------------ a whole fight

	@Test void overALongFightItMakesEveryMoveAndNeverTheSameTwiceRunning() {
		Random random = new Random(7);
		MoveDeck deck = new MoveDeck(4);
		long[] made = new long[StormConductorRules.MOVES + 1];
		Arrays.fill(made, Long.MIN_VALUE / 2);
		int[] count = new int[StormConductorRules.MOVES + 1];
		int spells = 2;
		long now = 0;
		int last = -1, own = 0, picks = 0;
		for (int i = 0; picks < 300 && i < 5000; i++) {
			int phase = 1 + Math.min(2, picks / 100);
			double distance = 1 + random.nextDouble() * 15;
			boolean nearRod = random.nextDouble() < 0.25;
			boolean sheltered = nearRod || random.nextDouble() < 0.2;
			boolean behind = random.nextDouble() < 0.2;
			int quiet = random.nextDouble() < 0.1 ? 1 : 2;
			long[] since = new long[StormConductorRules.MOVES + 1];
			for (int m = 1; m <= StormConductorRules.MOVES; m++) {
				since[m] = now - made[m];
			}
			// As the fight picks: its own moves alongside its spells.
			double[] mine = StormConductorRules.weights(phase, distance, nearRod, sheltered, behind, quiet, since);
			int[] moves = new int[mine.length + spells];
			double[] weights = new double[moves.length];
			for (int k = 0; k < mine.length; k++) {
				moves[k] = StormConductorRules.PICKS[k];
				weights[k] = mine[k];
			}
			for (int k = 0; k < spells; k++) {
				moves[mine.length + k] = SPELL + k;
				weights[mine.length + k] = StormConductorRules.spellWeight(distance);
			}
			int pick = deck.pick(moves, weights, random.nextDouble());
			assertNotEquals(-1, pick);
			assertNotEquals(last, pick, "the same twice running at pick " + picks);
			deck.played(pick);
			last = pick;
			picks++;
			if (pick < SPELL) {
				assertTrue(StormConductorRules.open(pick, phase, quiet), StormConductorRules.key(pick) + " out of its phase");
				made[pick] = now;
				count[pick]++;
				own++;
				now += StormConductorRules.length(pick, phase) + StormConductorRules.rest(pick, phase);
			} else {
				now += 30;
			}
		}
		assertEquals(300, picks);
		assertTrue(own > 150, "mostly its own moves: " + own);
		for (int m : StormConductorRules.PICKS) {
			assertTrue(count[m] >= 3, StormConductorRules.key(m) + " made " + count[m]);
			assertTrue(count[m] < own * 0.3, StormConductorRules.key(m) + " made " + count[m] + " of " + own);
		}
	}
}
