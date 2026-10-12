package dev.wildercord.cast;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TideScribeRulesTest {
	/** Where the fight numbers its spells, past any move of a boss's own. */
	private static final int SPELL = 100;

	/** Every move made long enough ago to be ready again. */
	private static long[] rested() {
		long[] since = new long[TideScribeRules.MOVES + 1];
		Arrays.fill(since, 100_000);
		return since;
	}

	private static double weight(double[] w, int move) {
		for (int i = 0; i < TideScribeRules.PICKS.length; i++) {
			if (TideScribeRules.PICKS[i] == move) {
				return w[i];
			}
		}
		throw new IllegalArgumentException("not a pick: " + move);
	}

	private static double[] dry(int phase, double distance) {
		return TideScribeRules.weights(phase, false, false, distance, false, 0, false, rested());
	}

	private static double[] wet(int phase, double distance, boolean wading) {
		return TideScribeRules.weights(phase, true, false, distance, false, 0, wading, rested());
	}

	// ------------------------------------------------------------------ what it wants

	@Test void itTearsOutItsPagesForAnyoneCloseAndThrowsQuillsAtAnyoneKeepingOff() {
		double[] close = dry(2, 3);
		double[] far = dry(2, 12);
		assertTrue(weight(close, TideScribeRules.PAGES) > weight(close, TideScribeRules.QUILLS));
		assertTrue(weight(far, TideScribeRules.QUILLS) > weight(far, TideScribeRules.PAGES));
		assertEquals(0, weight(dry(1, 20), TideScribeRules.QUILLS), 1e-9, "past the quills' reach");
	}

	@Test void theWhirlpoolIsForAnyoneWading() {
		assertTrue(weight(wet(1, 5, true), TideScribeRules.WHIRLPOOL) > weight(wet(1, 5, false), TideScribeRules.WHIRLPOOL));
		assertTrue(weight(wet(1, 5, true), TideScribeRules.WHIRLPOOL) > weight(wet(1, 15, true), TideScribeRules.WHIRLPOOL));
	}

	@Test void itNeverSpraysInkAtYourBackOrPastItsReach() {
		long[] since = rested();
		assertEquals(0, weight(TideScribeRules.weights(2, false, false, 5, true, 0, false, since), TideScribeRules.INK), 1e-9);
		assertEquals(0, weight(TideScribeRules.weights(2, false, false, 12, false, 0, false, since), TideScribeRules.INK), 1e-9);
		assertTrue(weight(TideScribeRules.weights(2, false, false, 5, false, 0, false, since), TideScribeRules.INK) > 0);
	}

	@Test void itCallsTheTideOnlyWhileItIsOutAndOnlyInItsThirdPhase() {
		long[] since = rested();
		assertTrue(weight(TideScribeRules.weights(3, false, true, 6, false, 0, false, since), TideScribeRules.TIDE_CALL) > 0);
		assertEquals(0, weight(TideScribeRules.weights(3, false, false, 6, false, 0, false, since), TideScribeRules.TIDE_CALL), 1e-9);
		assertEquals(0, weight(TideScribeRules.weights(2, false, true, 6, false, 0, false, since), TideScribeRules.TIDE_CALL), 1e-9);
		assertEquals(0, weight(TideScribeRules.weights(3, true, true, 6, false, 0, false, since), TideScribeRules.TIDE_CALL), 1e-9);
	}

	@Test void laterPhasesBringNewMovesAndNothingIsMadeBeforeItsPhase() {
		double[] first = dry(1, 6);
		double[] firstWet = wet(1, 6, true);
		for (int m : new int[]{TideScribeRules.INK, TideScribeRules.PAGES, TideScribeRules.BREAKER, TideScribeRules.DIVE, TideScribeRules.TIDE_CALL}) {
			assertEquals(0, weight(first, m), 1e-9, TideScribeRules.key(m));
			assertEquals(0, weight(firstWet, m), 1e-9, TideScribeRules.key(m));
		}
		assertTrue(weight(dry(2, 6), TideScribeRules.PAGES) > 0);
		assertTrue(weight(wet(2, 6, true), TideScribeRules.DIVE) > 0);
		for (int m : TideScribeRules.PICKS) {
			assertTrue(TideScribeRules.unlocks(m) >= 1 && TideScribeRules.unlocks(m) <= 3, TideScribeRules.key(m));
		}
	}

	@Test void theCurrentsWantTheFloodAndTheWritingWantsTheDryFloor() {
		double[] floor = dry(3, 6);
		double[] flood = wet(3, 6, true);
		for (int m : new int[]{TideScribeRules.WHIRLPOOL, TideScribeRules.RIPTIDE, TideScribeRules.DIVE}) {
			assertEquals(0, weight(floor, m), 1e-9, TideScribeRules.key(m));
			assertTrue(weight(flood, m) > 0, TideScribeRules.key(m));
		}
		for (int m : new int[]{TideScribeRules.QUILLS, TideScribeRules.SCRIPT, TideScribeRules.PAGES}) {
			assertEquals(0, weight(flood, m), 1e-9, TideScribeRules.key(m));
			assertTrue(weight(floor, m) > 0, TideScribeRules.key(m));
		}
		assertTrue(weight(floor, TideScribeRules.INK) > 0 && weight(flood, TideScribeRules.INK) > 0, "ink, wet or dry");
		assertTrue(weight(floor, TideScribeRules.BREAKER) > 0 && weight(flood, TideScribeRules.BREAKER) > 0, "the breaker, wet or dry");
	}

	@Test void aMoveMadeLatelyWaitsItsTurnAndWaitsLessInTheThirdPhase() {
		long[] since = rested();
		since[TideScribeRules.QUILLS] = 10;
		assertEquals(0, weight(TideScribeRules.weights(1, false, false, 8, false, 0, false, since), TideScribeRules.QUILLS), 1e-9);
		for (int m : TideScribeRules.PICKS) {
			assertTrue(TideScribeRules.cooldown(m, 3) < TideScribeRules.cooldown(m, 1), TideScribeRules.key(m));
			assertTrue(TideScribeRules.rest(m, 3) < TideScribeRules.rest(m, 1), TideScribeRules.key(m));
			assertTrue(TideScribeRules.cooldown(m, 1) > TideScribeRules.length(m, 1), TideScribeRules.key(m) + " can't follow itself");
		}
	}

	@Test void aStormSpellIsWantedMoreWhileItsPreyWadesAndFrostLessOnTheDryFloor() {
		double base = TideScribeRules.spellWeight(false, false, true, true);
		assertTrue(TideScribeRules.spellWeight(true, false, true, true) > base);
		assertEquals(base, TideScribeRules.spellWeight(true, false, true, false), 1e-9);
		assertTrue(TideScribeRules.spellWeight(false, true, false, false) < base);
		assertEquals(base, TideScribeRules.spellWeight(false, true, true, false), 1e-9);
	}

	// ------------------------------------------------------------------ fair warning

	@Test void everyMoveWindsUpFairlyAndHoldsItsAimBeforeTheBlow() {
		Set<String> keys = new HashSet<>();
		for (int m = 1; m <= TideScribeRules.MOVES; m++) {
			assertNotNull(TideScribeRules.key(m), "move " + m);
			assertTrue(keys.add(TideScribeRules.key(m)), "a name of its own: " + m);
			assertTrue(TideScribeRules.length(m, 1) >= 1);
		}
		for (int m : TideScribeRules.PICKS) {
			int tell = TideScribeRules.tell(m);
			assertTrue(tell >= 10 && tell <= 18, TideScribeRules.key(m) + " tell " + tell);
			assertFalse(TideScribeRules.opening(m));
			for (int phase = 1; phase <= 3; phase++) {
				assertTrue(TideScribeRules.length(m, phase) > tell, TideScribeRules.key(m));
			}
		}
		// The second goes and the words' fuses warn as fairly.
		for (int tell : new int[]{TideScribeRules.QUILL_AGAIN, TideScribeRules.RIP_AGAIN, TideScribeRules.DIVE_TELL - 2,
			TideScribeRules.SCRIPT_FUSE, TideScribeRules.SCRIPT_AGAIN, TideScribeRules.BREAKER_AGAIN}) {
			assertTrue(tell >= 10 && tell <= 18, "tell " + tell);
		}
		for (int lock : new int[]{TideScribeRules.QUILL_LOCK, TideScribeRules.RIP_LOCK, TideScribeRules.INK_LOCK}) {
			assertTrue(lock >= 3 && lock < 10, "lock " + lock);
		}
		assertTrue(TideScribeRules.DIVE_LOCK >= 10 && TideScribeRules.DIVE_LOCK < TideScribeRules.DIVE_HUNT);
	}

	@Test void itIsOpenOnlyWhenBowedSpunOrSurfacedAndNeverPicksThoseOfItself() {
		for (int m = 1; m <= TideScribeRules.MOVES; m++) {
			boolean open = m == TideScribeRules.BOWED || m == TideScribeRules.SPUN || m == TideScribeRules.SURFACED;
			assertEquals(open, TideScribeRules.opening(m), "move " + m);
			if (open) {
				assertEquals(0, TideScribeRules.tell(m));
				assertTrue(TideScribeRules.unlocks(m) > 3);
				assertTrue(TideScribeRules.length(m, 1) >= 30, "long enough to punish");
			}
		}
		assertTrue(TideScribeRules.OPEN > 1);
	}

	@Test void lengthsCoverTheirRingsAndGrowInTheThirdPhase() {
		assertEquals(24, TideScribeRules.rolls(13, 0.55));
		assertEquals(24, TideScribeRules.rolls(TideScribeRules.CALL_FROM, TideScribeRules.CALL_SPEED));
		assertTrue(TideScribeRules.length(TideScribeRules.PAGES, 3) > TideScribeRules.length(TideScribeRules.PAGES, 2));
		assertTrue(TideScribeRules.length(TideScribeRules.BREAKER, 3) > TideScribeRules.length(TideScribeRules.BREAKER, 2));
		assertTrue(TideScribeRules.length(TideScribeRules.SCRIPT, 3) > TideScribeRules.length(TideScribeRules.SCRIPT, 2));
		assertTrue(TideScribeRules.length(TideScribeRules.BREAKER, 1)
			>= TideScribeRules.BREAKER_TELL + TideScribeRules.rolls(TideScribeRules.BREAKER_REACH, TideScribeRules.BREAKER_SPEED));
		assertTrue(TideScribeRules.length(TideScribeRules.DIVE, 1) > TideScribeRules.DIVE_TELL + TideScribeRules.DIVE_HUNT);
	}

	// ------------------------------------------------------------------ where its blows land

	@Test void theQuillsFlyDownTheirLanesAndTheSecondVolleyGoesDownTheGaps() {
		assertEquals(-22, TideScribeRules.lane(0, 3, 22, 0), 1e-9);
		assertEquals(0, TideScribeRules.lane(1, 3, 22, 0), 1e-9);
		assertEquals(22, TideScribeRules.lane(2, 3, 22, 0), 1e-9);
		assertEquals(-9, TideScribeRules.lane(1, 4, 18, 0), 1e-9);
		assertEquals((TideScribeRules.lane(1, 4, 18, 0) + TideScribeRules.lane(2, 4, 18, 0)) / 2, TideScribeRules.lane(1, 4, 18, 9), 1e-9,
			"half a spread over, the second volley flies down the gap");
		assertTrue(TideScribeRules.quilled(8, 0.3, 0));
		assertFalse(TideScribeRules.quilled(8, 1.0, 0), "between the lanes");
		assertFalse(TideScribeRules.quilled(1, 0, 0), "right under it");
		assertFalse(TideScribeRules.quilled(17, 0, 0), "past their reach");
		assertTrue(TideScribeRules.QUILL_LANES[3] > TideScribeRules.QUILL_LANES[1]);
	}

	@Test void aWordBurstsUnderWhoeverStandsOnIt() {
		assertTrue(TideScribeRules.scribed(0.5, 0.5, 0));
		assertFalse(TideScribeRules.scribed(1.2, 1.2, 0), "stepped off");
		assertFalse(TideScribeRules.scribed(0, 0, 3), "high over it");
	}

	@Test void theWhirlpoolDragsTheWadersInAndChurnsAtItsHeart() {
		assertEquals(0, TideScribeRules.drag(0.5, 1), 1e-9);
		assertEquals(0, TideScribeRules.drag(TideScribeRules.WHIRL_RADIUS + 0.5, 3), 1e-9);
		assertTrue(TideScribeRules.drag(5, 2) > TideScribeRules.drag(5, 1));
		assertTrue(TideScribeRules.churns(1.5, 0));
		assertFalse(TideScribeRules.churns(3, 0));
	}

	@Test void theRiptideTakesItsLaneAndNothingBesideIt() {
		assertTrue(TideScribeRules.swept(6, 1.0, 0));
		assertFalse(TideScribeRules.swept(6, 2.2, 0), "stepped out of the lane");
		assertFalse(TideScribeRules.swept(-2, 0, 0), "behind it");
		assertFalse(TideScribeRules.swept(16, 0, 0), "past its end");
	}

	@Test void theInkFillsItsConeAndWidensInTheThirdPhase() {
		assertTrue(TideScribeRules.inked(5, 1, 0, false));
		assertFalse(TideScribeRules.inked(5, 4.5, 0, false), "42 degrees off");
		assertTrue(TideScribeRules.inked(5, 4.5, 0, true), "42 degrees off, widened");
		assertFalse(TideScribeRules.inked(-3, 0, 0, true), "at its back");
		assertFalse(TideScribeRules.inked(10, 0, 0, true), "past its reach");
	}

	@Test void thePagesCutTheRingAndSpareInsideAndOut() {
		assertTrue(TideScribeRules.cut(4.5, 0));
		assertFalse(TideScribeRules.cut(2, 0), "hugging it");
		assertFalse(TideScribeRules.cut(7, 0), "well outside");
		assertFalse(TideScribeRules.cut(4.5, 4), "high over");
	}

	@Test void aRollingWaveIsJumpedOnTheFloorAndDuckedUnderOnTheBottomOfTheFlood() {
		assertTrue(TideScribeRules.crosses(5, 4.8, 5.35, 0), "standing in its way");
		assertTrue(TideScribeRules.crosses(5, 5.35, 4.8, 0), "rolling in from the rim");
		assertFalse(TideScribeRules.crosses(5, 4.8, 5.35, 1.0), "jumped");
		assertFalse(TideScribeRules.crosses(5, 4.8, 5.35, -1.6), "under it on the bottom");
		assertFalse(TideScribeRules.crosses(8, 4.8, 5.35, 0), "not reached yet");
		assertTrue(TideScribeRules.WAVE_HEIGHT < 1.2, "a jump clears it");
	}

	@Test void itBurstsUpUnderItsMarkAndItsTrailCanBeOutrun() {
		assertTrue(TideScribeRules.burst(1, 1, 0));
		assertFalse(TideScribeRules.burst(2, 1, 0), "off the mark");
		assertEquals(0.3, TideScribeRules.follow(0, 5, 0.3), 1e-9);
		assertEquals(4.9, TideScribeRules.follow(5, 4.9, 0.3), 1e-9);
		assertTrue(TideScribeRules.DIVE_FOLLOW < 0.5, "slower than someone running");
	}

	@Test void itKeepsToItsPitAndKnowsItsBack() {
		assertEquals(1, TideScribeRules.keep(3, 4), 1e-9);
		assertEquals(TideScribeRules.ROAM / 20, TideScribeRules.keep(12, 16), 1e-9);
		assertTrue(TideScribeRules.ROAM <= TideScribe.PIT_RADIUS);
		assertTrue(TideScribeRules.behind(180));
		assertTrue(TideScribeRules.behind(-150));
		assertFalse(TideScribeRules.behind(90));
		assertEquals(-170, TideScribeRules.wrap(190), 1e-9);
		assertEquals(10, TideScribeRules.wrap(370), 1e-9);
	}

	// ------------------------------------------------------------------ in the same breath

	@Test void inItsThirdPhaseItFollowsSomeMovesWithAnotherThatSuitsTheTide() {
		assertEquals(-1, TideScribeRules.chain(TideScribeRules.RIPTIDE, true, 6, 0.7), "not every time");
		assertEquals(TideScribeRules.WHIRLPOOL, TideScribeRules.chain(TideScribeRules.RIPTIDE, true, 6, 0.1));
		assertEquals(TideScribeRules.SCRIPT, TideScribeRules.chain(TideScribeRules.QUILLS, false, 6, 0.1));
		assertEquals(TideScribeRules.DIVE, TideScribeRules.chain(TideScribeRules.INK, true, 6, 0.1));
		assertEquals(TideScribeRules.QUILLS, TideScribeRules.chain(TideScribeRules.INK, false, 6, 0.1));
		assertEquals(TideScribeRules.RIPTIDE, TideScribeRules.chain(TideScribeRules.BREAKER, true, 6, 0.1));
		assertEquals(TideScribeRules.QUILLS, TideScribeRules.chain(TideScribeRules.BREAKER, false, 10, 0.1));
		assertEquals(TideScribeRules.PAGES, TideScribeRules.chain(TideScribeRules.BREAKER, false, 3, 0.1));
		for (int m = 1; m <= TideScribeRules.MOVES; m++) {
			for (boolean flooded : new boolean[]{false, true}) {
				int next = TideScribeRules.chain(m, flooded, 6, 0.1);
				if (next > 0) {
					assertNotEquals(m, next, "never itself");
					assertFalse(TideScribeRules.opening(next));
					assertTrue(TideScribeRules.open(next, 3, flooded), "suits the tide: " + TideScribeRules.key(next));
				}
			}
		}
	}

	// ------------------------------------------------------------------ a whole fight

	@Test void overALongFightItMakesEveryMoveAndNeverTheSameTwiceRunning() {
		Random random = new Random(7);
		MoveDeck deck = new MoveDeck(4);
		long[] made = new long[TideScribeRules.MOVES + 1];
		Arrays.fill(made, Long.MIN_VALUE / 2);
		int[] count = new int[TideScribeRules.MOVES + 1];
		int spells = 3;
		long now = 0;
		int last = -1, own = 0, picks = 0;
		boolean flooded = false;
		for (int i = 0; picks < 300 && i < 5000; i++) {
			int phase = 1 + Math.min(2, picks / 100);
			if (random.nextDouble() < 0.12) {
				flooded = !flooded;
			}
			double distance = 1 + random.nextDouble() * 16;
			boolean behind = random.nextDouble() < 0.25;
			double height = random.nextDouble() < 0.1 ? 3 : 0;
			boolean wading = flooded && random.nextDouble() < 0.7;
			boolean tideOut = !flooded && random.nextDouble() < 0.5;
			long[] since = new long[TideScribeRules.MOVES + 1];
			for (int m = 1; m <= TideScribeRules.MOVES; m++) {
				since[m] = now - made[m];
			}
			// As the fight picks: its own moves alongside its spells.
			double[] mine = TideScribeRules.weights(phase, flooded, tideOut, distance, behind, height, wading, since);
			int[] moves = new int[mine.length + spells];
			double[] weights = new double[moves.length];
			for (int k = 0; k < mine.length; k++) {
				moves[k] = TideScribeRules.PICKS[k];
				weights[k] = mine[k];
			}
			for (int k = 0; k < spells; k++) {
				moves[mine.length + k] = SPELL + k;
				weights[mine.length + k] = TideScribeRules.spellWeight(k == 0, k == 1, flooded, wading);
			}
			int pick = deck.pick(moves, weights, random.nextDouble());
			assertNotEquals(-1, pick);
			assertNotEquals(last, pick, "the same twice running at pick " + picks);
			deck.played(pick);
			last = pick;
			picks++;
			if (pick < SPELL) {
				assertTrue(TideScribeRules.open(pick, phase, flooded), TideScribeRules.key(pick) + " out of its phase or tide");
				made[pick] = now;
				count[pick]++;
				own++;
				now += TideScribeRules.length(pick, phase) + TideScribeRules.rest(pick, phase);
			} else {
				now += 30;
			}
		}
		assertEquals(300, picks);
		assertTrue(own > 150, "mostly its own moves: " + own);
		for (int m : TideScribeRules.PICKS) {
			assertTrue(count[m] >= 3, TideScribeRules.key(m) + " made " + count[m]);
			assertTrue(count[m] < own * 0.3, TideScribeRules.key(m) + " made " + count[m] + " of " + own);
		}
	}
}
