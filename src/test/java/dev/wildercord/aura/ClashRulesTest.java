package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The clash, the pure part ({@link ClashRules}): the rhythm's shape, judging a press, a side's tally (and why hammering loses), who wins
 * (and the Blade's edge), how the struggle leans, the network's allowance, the swordsmen of the world's own timing (fair, readable, and
 * beatable), and what comes of it.
 */
class ClashRulesTest {
	@Test
	void aDelayedLastPressIsNotClosedOnAnUncompensatedClock() {
		for (int latency : new int[] {0, 100, 300, 600, 1500}) {
			ClashRules.Tally tally = new ClashRules.Tally();
			int arrival = ClashRules.beat(2) + ClashRules.GOOD + ClashRules.lag(latency);
			tally.close(ClashRules.judgingTime(arrival, latency), false);
			assertNull(tally.beat(2));
			assertEquals(ClashRules.Grade.GOOD, tally.press(ClashRules.judgingTime(arrival, latency), false));
			assertTrue(arrival < ClashRules.serverLength());
		}
	}

	// ------------------------------------------------------------------ the rhythm

	@Test
	void theRhythmIsShortSteadyAndReadable() {
		assertEquals(3, ClashRules.BEATS, "three beats: short, and enough to come back from a bad one");
		for (int k = 1; k < ClashRules.BEATS; k++) {
			assertEquals(ClashRules.GAP, ClashRules.beat(k) - ClashRules.beat(k - 1), "the beats are evenly spaced");
		}
		assertTrue(ClashRules.LOCK >= 8, "the first ring has time to close before its beat");
		assertTrue(ClashRules.length() <= 40, "the whole struggle is under two seconds (" + ClashRules.length() + " ticks)");
		assertTrue(ClashRules.GAP >= 2 * ClashRules.GOOD + 1, "two good windows never overlap for a side without the edge");
		assertTrue(ClashRules.PERFECT < ClashRules.GOOD);
		assertTrue(ClashRules.lastClose() < ClashRules.length(), "the last window closes before the result");
	}

	@Test
	void aPressIsJudgedByHowFarItIsFromItsBeat() {
		assertEquals(ClashRules.Grade.PERFECT, ClashRules.judge(0, false));
		assertEquals(ClashRules.Grade.PERFECT, ClashRules.judge(-2, false));
		assertEquals(ClashRules.Grade.PERFECT, ClashRules.judge(2, false));
		assertEquals(ClashRules.Grade.GOOD, ClashRules.judge(3, false));
		assertEquals(ClashRules.Grade.GOOD, ClashRules.judge(-4, false));
		assertEquals(ClashRules.Grade.FUMBLE, ClashRules.judge(5, false));
		// The Blade's edge: each window a tick wider.
		assertEquals(ClashRules.Grade.PERFECT, ClashRules.judge(3, true));
		assertEquals(ClashRules.Grade.GOOD, ClashRules.judge(5, true));
		assertEquals(ClashRules.Grade.FUMBLE, ClashRules.judge(6, true));
		assertEquals(ClashRules.GOOD + ClashRules.EDGE, ClashRules.window(true));
	}

	@Test
	void aPressAnswersTheNearestBeatOrNone() {
		assertEquals(0, ClashRules.beatFor(ClashRules.beat(0), false));
		assertEquals(1, ClashRules.beatFor(ClashRules.beat(1) - 3, false));
		assertEquals(2, ClashRules.beatFor(ClashRules.beat(2) + 4, false));
		assertEquals(-1, ClashRules.beatFor(0, false), "pressing as the lock takes hold answers nothing");
		assertEquals(-1, ClashRules.beatFor(ClashRules.beat(2) + 6, false), "nor does pressing after the last beat");
		// With the edge two windows touch: a press between two beats answers the nearer.
		int between = ClashRules.beat(0) + ClashRules.GOOD + 1;
		assertEquals(1, ClashRules.beatFor(between, true));
		assertEquals(0, ClashRules.beatFor(between - 1, true));
	}

	// ------------------------------------------------------------------ a side's tally

	@Test
	void threePerfectPressesScoreSix() {
		ClashRules.Tally tally = new ClashRules.Tally();
		for (int k = 0; k < ClashRules.BEATS; k++) {
			assertEquals(ClashRules.Grade.PERFECT, tally.press(ClashRules.beat(k), false));
		}
		assertTrue(tally.done());
		assertEquals(6, tally.score());
		assertEquals(0, tally.fumbles());
	}

	@Test
	void aBeatAnsweredTwiceOrAPressInNoWindowIsAFumble() {
		ClashRules.Tally tally = new ClashRules.Tally();
		assertEquals(ClashRules.Grade.GOOD, tally.press(ClashRules.beat(0) - 3, false));
		assertEquals(ClashRules.Grade.FUMBLE, tally.press(ClashRules.beat(0), false), "the beat was answered already");
		assertEquals(ClashRules.Grade.FUMBLE, tally.press(2, false), "in no window");
		assertEquals(ClashRules.Grade.GOOD, tally.beat(0), "the first answer stands");
		assertEquals(-1, tally.score(), "one good less two fumbles");
	}

	@Test
	void hammeringTheButtonLoses() {
		// Pressing every tick of the struggle against a side who presses once a beat, merely well.
		ClashRules.Tally hammer = new ClashRules.Tally();
		for (int t = 0; t < ClashRules.length(); t++) {
			hammer.press(t, false);
		}
		hammer.close(ClashRules.length(), false);
		ClashRules.Tally steady = new ClashRules.Tally();
		for (int k = 0; k < ClashRules.BEATS; k++) {
			steady.press(ClashRules.beat(k) + 3, false);
		}
		assertTrue(hammer.score() < 0, "hammering scores below nothing (" + hammer.score() + ")");
		assertEquals(ClashRules.Outcome.B, ClashRules.outcome(hammer.score(), steady.score(), false, false));
		// Even with the Blade's edge.
		ClashRules.Tally edged = new ClashRules.Tally();
		for (int t = 0; t < ClashRules.length(); t++) {
			edged.press(t, true);
		}
		assertTrue(edged.score() < steady.score());
	}

	@Test
	void anUnansweredBeatIsMissedOnceItsWindowCloses() {
		ClashRules.Tally tally = new ClashRules.Tally();
		assertEquals(0, tally.close(ClashRules.beat(0) + ClashRules.GOOD, false), "still open on its last tick");
		assertEquals(1, tally.close(ClashRules.beat(0) + ClashRules.GOOD + 1, false));
		assertEquals(ClashRules.Grade.MISS, tally.beat(0));
		assertEquals(0, tally.close(ClashRules.beat(0) + ClashRules.GOOD + 1, false), "missed once only");
		assertEquals(0b110, tally.close(ClashRules.length(), false));
		assertTrue(tally.done());
		assertEquals(0, tally.score());
		// The edge holds a window open a tick longer.
		ClashRules.Tally edged = new ClashRules.Tally();
		assertEquals(0, edged.close(ClashRules.beat(0) + ClashRules.GOOD + 1, true));
	}

	// ------------------------------------------------------------------ who wins

	@Test
	void theHigherScoreWinsAndTheBladeWinsAnEqualOne() {
		assertEquals(ClashRules.Outcome.A, ClashRules.outcome(4, 3, false, false));
		assertEquals(ClashRules.Outcome.B, ClashRules.outcome(2, 5, true, false), "the edge never outweighs a better struggle");
		assertEquals(ClashRules.Outcome.EVEN, ClashRules.outcome(3, 3, false, false), "equal scores break both, as clashes always did");
		assertEquals(ClashRules.Outcome.A, ClashRules.outcome(3, 3, true, false), "the Blade wins an equal score");
		assertEquals(ClashRules.Outcome.B, ClashRules.outcome(3, 3, false, true));
		assertEquals(ClashRules.Outcome.EVEN, ClashRules.outcome(3, 3, true, true), "two Blades are equals");
	}

	@Test
	void theBladesEdgeIsAnEdgeNotAWin() {
		// A Blade that misses every beat loses to anyone who answers one of them.
		ClashRules.Tally blade = new ClashRules.Tally();
		blade.close(ClashRules.length(), true);
		ClashRules.Tally other = new ClashRules.Tally();
		other.press(ClashRules.beat(1) + 4, false);
		other.close(ClashRules.length(), false);
		assertEquals(ClashRules.Outcome.B, ClashRules.outcome(blade.score(), other.score(), true, false));
		// A press a tick past the last beat's good window is a Blade's good and anyone else's fumble.
		int late = ClashRules.beat(ClashRules.BEATS - 1) + ClashRules.GOOD + 1;
		assertEquals(ClashRules.Grade.GOOD, new ClashRules.Tally().press(late, true));
		assertEquals(ClashRules.Grade.FUMBLE, new ClashRules.Tally().press(late, false));
	}

	@Test
	void theStruggleLeansTowardWhoeverIsAhead() {
		assertEquals(0, ClashRules.balance(0, 0), 1e-9);
		assertEquals(0, ClashRules.balance(3, 3), 1e-9);
		assertTrue(ClashRules.balance(2, 0) > 0);
		assertTrue(ClashRules.balance(0, 2) < 0);
		assertEquals(1, ClashRules.balance(6, 0), 1e-9);
		assertEquals(-1, ClashRules.balance(-3, 6), 1e-9, "never past either end");
		assertTrue(ClashRules.balance(1, 0) < ClashRules.balance(2, 0), "a bigger lead leans further");
	}

	// ------------------------------------------------------------------ the network

	@Test
	void aPressIsMovedBackForTheSendersConnection() {
		assertEquals(0, ClashRules.lag(0));
		assertEquals(0, ClashRules.lag(40));
		assertEquals(1, ClashRules.lag(100), "a tenth of a second there and back: half of it each way, a tick");
		assertEquals(3, ClashRules.lag(300));
		assertEquals(ClashRules.MAX_LAG, ClashRules.lag(5000), "never more than the most");
		assertEquals(0, ClashRules.lag(-20));
		assertTrue(ClashRules.MAX_LAG < ClashRules.GAP, "never far enough to answer the beat before");
	}

	// ------------------------------------------------------------------ the swordsmen of the world

	@Test
	void theWorldsSwordsmenGrowSteadierByStage() {
		for (int s = AuraRules.GLOW; s < AuraRules.SOVEREIGN; s++) {
			assertTrue(ClashRules.npcSpread(s + 1) < ClashRules.npcSpread(s), "tighter at " + (s + 1));
			assertTrue(ClashRules.npcMiss(s + 1) < ClashRules.npcMiss(s), "fewer misses at " + (s + 1));
		}
		assertTrue(ClashRules.npcMiss(AuraRules.SOVEREIGN) > 0, "even a Sovereign misses sometimes");
		assertEquals(ClashRules.NPC_MISS, ClashRules.npcOffset(AuraRules.EDGE, 0.0, 0.0), "a low draw misses");
		assertEquals(0, ClashRules.npcOffset(AuraRules.EDGE, 0.99, 0.0));
		assertEquals(ClashRules.NPC_MISS, ClashRules.npcOffset(AuraRules.GLOW, 0.99, 3.0), "a press outside its window is a miss, never a fumble");
		int off = ClashRules.npcOffset(AuraRules.FORM, 0.99, 0.5);
		assertTrue(Math.abs(off) <= ClashRules.GOOD);
	}

	/** The mean score of {@code rounds} clashes a swordsman of the world at {@code stage} plays, from one seed. */
	private static double npcMean(int stage, int rounds, long seed) {
		Random random = new Random(seed);
		double total = 0;
		for (int r = 0; r < rounds; r++) {
			ClashRules.Tally tally = new ClashRules.Tally();
			for (int k = 0; k < ClashRules.BEATS; k++) {
				int o = ClashRules.npcOffset(stage, random.nextDouble(), random.nextGaussian());
				if (o != ClashRules.NPC_MISS) {
					tally.set(k, ClashRules.judge(o, false));
				}
			}
			tally.close(ClashRules.length(), false);
			total += tally.score();
		}
		return total / rounds;
	}

	@Test
	void theWorldsSwordsmenAreFairAndBeatable() {
		double last = -1;
		for (int s = AuraRules.GLOW; s <= AuraRules.SOVEREIGN; s++) {
			double mean = npcMean(s, 20000, 7L + s);
			assertTrue(mean > last, "a higher stage clashes better (" + s + ": " + mean + ")");
			last = mean;
		}
		double glow = npcMean(AuraRules.GLOW, 20000, 3);
		double sovereign = npcMean(AuraRules.SOVEREIGN, 20000, 5);
		assertTrue(glow > 1.5 && glow < 3.0, "a Glow duelist manages about half the beats (" + glow + ")");
		assertTrue(sovereign > 3.6 && sovereign < 5.0, "a Sovereign is formidable but short of perfect (" + sovereign + ")");
		// A player who hits every beat perfectly never loses and nearly always wins; one who hits each beat merely well (three goods) beats a Glow
		// swordsman of the world more often than not.
		Random random = new Random(11);
		int perfectWins = 0;
		int perfectLosses = 0;
		int goodWins = 0;
		int rounds = 5000;
		for (int r = 0; r < rounds; r++) {
			ClashRules.Tally npc = new ClashRules.Tally();
			for (int k = 0; k < ClashRules.BEATS; k++) {
				int o = ClashRules.npcOffset(AuraRules.GLOW, random.nextDouble(), random.nextGaussian());
				if (o != ClashRules.NPC_MISS) {
					npc.set(k, ClashRules.judge(o, false));
				}
			}
			npc.close(ClashRules.length(), false);
			perfectWins += ClashRules.outcome(6, npc.score(), false, false) == ClashRules.Outcome.A ? 1 : 0;
			perfectLosses += ClashRules.outcome(6, npc.score(), false, false) == ClashRules.Outcome.B ? 1 : 0;
			goodWins += ClashRules.outcome(3, npc.score(), false, false) == ClashRules.Outcome.A ? 1 : 0;
		}
		assertEquals(0, perfectLosses, "perfect timing never loses");
		assertTrue(perfectWins > rounds * 0.95, "and beats a Glow duelist nearly always (" + perfectWins + ")");
		assertTrue(goodWins > rounds / 2, "steady timing beats one more often than not (" + goodWins + ")");
	}

	// ------------------------------------------------------------------ what comes of it

	@Test
	void theWinnerFliesOnALittleSpent() {
		assertTrue(ClashRules.CARRY > 0.5 && ClashRules.CARRY <= 1.0);
		assertEquals(4.8, ClashRules.carried(6, 0.8), 1e-9);
		assertEquals(0, ClashRules.carried(6, -1), 1e-9);
		assertEquals(0, ClashRules.carried(-3, 0.8), 1e-9);
		assertTrue(ClashRules.ANSWER_CARRY > 0 && ClashRules.ANSWER_CARRY < 1);
		assertTrue(ClashRules.WIN_MOMENTUM > 0 && ClashRules.WIN_MOMENTUM <= MomentumRules.TIERS[0], "a clash won builds momentum, short of a tier");
	}

	@Test
	void theSameTwoRestBeforeLockingAgain() {
		assertTrue(ClashRules.rested(Long.MIN_VALUE, 0));
		assertFalse(ClashRules.rested(100, 100 + ClashRules.REST - 1));
		assertTrue(ClashRules.rested(100, 100 + ClashRules.REST));
		assertTrue(ClashRules.REST > ClashRules.length(), "a clash can't be chained into the next before the first is over and gone");
	}

	@Test
	void theMeetingsReachAndTheAnswersMoment() {
		assertTrue(ClashRules.CROSS > 0 && ClashRules.CROSS < 20, "an answer is the same breath, under a second");
		assertTrue(ClashRules.MEET_REACH < ClashRules.ANSWER_REACH);
		assertTrue(ClashRules.FACING > 0 && ClashRules.FACING < 1);
		assertEquals(ClashRules.Kind.CRESCENTS, ClashRules.Kind.of(-1));
		assertEquals(ClashRules.Kind.ARTS, ClashRules.Kind.of(2));
		assertEquals(ClashRules.Grade.MISS, ClashRules.Grade.of(9));
		assertEquals(ClashRules.Grade.FUMBLE, ClashRules.Grade.of(3));
	}
}
