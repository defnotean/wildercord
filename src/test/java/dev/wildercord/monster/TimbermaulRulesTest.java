package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildercord.cast.MoveDeck;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TimbermaulRulesTest {
	private static double weight(int move, double distance, boolean behind, boolean crowded, boolean roused) {
		double[] w = TimbermaulRules.weights(distance, behind, crowded, roused);
		for (int i = 0; i < TimbermaulRules.PICKS.length; i++) {
			if (TimbermaulRules.PICKS[i] == move) return w[i];
		}
		throw new IllegalArgumentException("not a pick: " + move);
	}

	@Test
	void itNoticesWhoeverComesNearButBarelyASneak() {
		assertTrue(TimbermaulRules.notices(9, false));
		assertFalse(TimbermaulRules.notices(9, true));
		assertTrue(TimbermaulRules.notices(4, true));
		assertFalse(TimbermaulRules.notices(TimbermaulRules.NOTICE_RANGE + 1, false));
	}

	@Test
	void itWarnsThenLetsGoWhoeverBacksOffAndChargesWhoeverCrowdsIt() {
		assertTrue(TimbermaulRules.crowds(4));
		assertFalse(TimbermaulRules.crowds(8));
		assertTrue(TimbermaulRules.letsGo(TimbermaulRules.LET_GO));
		assertFalse(TimbermaulRules.letsGo(10), "not far enough yet");
	}

	@Test
	void upCloseItRakesSnapsAndCrushesAndCrushesMoreWhenCrowded() {
		assertTrue(weight(TimbermaulRules.RAKE, 2, false, false, false) > 0);
		assertTrue(weight(TimbermaulRules.SNAP, 2, false, false, false) > 0);
		assertTrue(weight(TimbermaulRules.CRUSH, 2, false, false, false) > 0);
		assertTrue(weight(TimbermaulRules.CRUSH, 2, false, true, false) > weight(TimbermaulRules.CRUSH, 2, false, false, false));
		assertEquals(0, weight(TimbermaulRules.POUNCE, 2, false, false, false), "too near to leap");
		assertEquals(0, weight(TimbermaulRules.CHARGE, 2, false, false, false), "too near to run");
		assertEquals(0, weight(TimbermaulRules.LOG, 2, false, false, false), "too near to roll a log at");
		assertEquals(0, weight(TimbermaulRules.DIG, 2, false, false, false), "nobody behind it");
	}

	@Test
	void fromAWayOffItPouncesChargesAndRollsLogsAndChargesMoreTheFartherOff() {
		assertTrue(weight(TimbermaulRules.POUNCE, 8, false, false, false) > 0);
		assertTrue(weight(TimbermaulRules.CHARGE, 8, false, false, false) > 0);
		assertTrue(weight(TimbermaulRules.LOG, 8, false, false, false) > 0);
		assertTrue(weight(TimbermaulRules.CHARGE, 12, false, false, false) > weight(TimbermaulRules.CHARGE, 7, false, false, false));
		assertEquals(0, weight(TimbermaulRules.RAKE, 8, false, false, false), "out of reach");
		assertEquals(0, weight(TimbermaulRules.POUNCE, TimbermaulRules.POUNCE_REACH + 1, false, false, false), "too far to leap");
	}

	@Test
	void itDigsAtWhoeverStandsBehindItAndOnlyRousedDoesItHug() {
		assertTrue(TimbermaulRules.behind(-0.5));
		assertFalse(TimbermaulRules.behind(0.2));
		assertTrue(weight(TimbermaulRules.DIG, 3, true, false, false) > weight(TimbermaulRules.RAKE, 3, true, false, false),
			"behind it, the dig comes first");
		assertEquals(0, weight(TimbermaulRules.HUG, 2, false, false, false), "calm, no hug");
		assertTrue(weight(TimbermaulRules.HUG, 2, false, false, true) > 0);
		assertEquals(0, weight(TimbermaulRules.HUG, 6, false, false, true), "too far to fall on");
	}

	@Test
	void itKeepsClosingInWhenNothingReachesOrOnlyWhatItJustDid() {
		var deck = new MoveDeck(TimbermaulRules.MEMORY);
		assertEquals(-1, TimbermaulRules.choose(deck, 30, false, false, false, 0.5), "nothing reaches");
		// Fifteen off, only the charge reaches; once it has charged it closes in rather than charge again.
		assertEquals(TimbermaulRules.CHARGE, TimbermaulRules.choose(deck, 15, false, false, false, 0.5));
		deck.played(TimbermaulRules.CHARGE);
		assertEquals(-1, TimbermaulRules.choose(deck, 15, false, false, false, 0.5));
	}

	@Test
	void aWholeFightNeverMakesTheSameMoveTwiceRunningAndMakesThemAll() {
		var deck = new MoveDeck(TimbermaulRules.MEMORY);
		var rng = new Random(7);
		double[] distances = {1.5, 2.5, 3.5, 5, 7, 9, 12};
		int[] made = new int[TimbermaulRules.MOVES];
		int last = -1;
		for (int i = 0; i < 600; i++) {
			boolean roused = i >= 300;
			double distance = distances[rng.nextInt(distances.length)];
			int move = TimbermaulRules.choose(deck, distance, rng.nextInt(4) == 0, rng.nextInt(3) == 0, roused, rng.nextDouble());
			if (move < 0) continue;
			assertNotEquals(last, move, "the same move twice running at step " + i);
			if (move == TimbermaulRules.HUG) assertTrue(roused, "a hug before it was roused");
			deck.played(move);
			made[move]++;
			last = move;
		}
		for (int move : TimbermaulRules.PICKS) {
			assertTrue(made[move] > 0, "never made move " + move);
		}
	}

	@Test
	void theRakeSweepsAcrossItsFrontFromTheSideItWasRaisedOn() {
		assertEquals(30, TimbermaulRules.swept(0), 1e-9);
		assertEquals(180, TimbermaulRules.swept(TimbermaulRules.RAKE_SWEEP - 1), 1e-9);
		assertEquals(180, TimbermaulRules.swept(40), 1e-9);
		// Raised on its right, it strikes its right first and its left only once swept across.
		assertTrue(TimbermaulRules.rakes(2, 80, 1, TimbermaulRules.swept(0)));
		assertFalse(TimbermaulRules.rakes(2, -60, 1, TimbermaulRules.swept(0)), "not swept across yet");
		assertTrue(TimbermaulRules.rakes(2, -60, 1, TimbermaulRules.swept(TimbermaulRules.RAKE_SWEEP - 1)));
		// Raised on its left, the other way about.
		assertTrue(TimbermaulRules.rakes(2, -80, -1, TimbermaulRules.swept(0)));
		assertFalse(TimbermaulRules.rakes(2, 60, -1, TimbermaulRules.swept(0)));
		assertFalse(TimbermaulRules.rakes(2, 150, 1, 180), "behind it");
		assertFalse(TimbermaulRules.rakes(TimbermaulRules.RAKE_REACH + 0.5, 0, 1, 180), "backed off out of reach");
	}

	@Test
	void theSnapAndTheChargeStrikeDownTheirLanesAndTheChargeRunsItsLength() {
		assertTrue(TimbermaulRules.inLane(3, 0.5, TimbermaulRules.SNAP_LENGTH, TimbermaulRules.SNAP_WIDTH));
		assertFalse(TimbermaulRules.inLane(3, 1.5, TimbermaulRules.SNAP_LENGTH, TimbermaulRules.SNAP_WIDTH), "stepped aside");
		assertFalse(TimbermaulRules.inLane(TimbermaulRules.SNAP_LENGTH + 1, 0, TimbermaulRules.SNAP_LENGTH, TimbermaulRules.SNAP_WIDTH));
		assertFalse(TimbermaulRules.inLane(-2, 0, TimbermaulRules.SNAP_LENGTH, TimbermaulRules.SNAP_WIDTH), "behind it");
		assertTrue(TimbermaulRules.CHARGE_SPEED * TimbermaulRules.CHARGE_RUN >= TimbermaulRules.CHARGE_LENGTH,
			"a clean run covers the whole line");
	}

	@Test
	void theCrushComesDownOnAFanBeforeItAndToppleItOnlyWhileItRears() {
		assertTrue(TimbermaulRules.inFan(3, 0.8));
		assertTrue(TimbermaulRules.inFan(4, 0.4), "wide");
		assertFalse(TimbermaulRules.inFan(3, 0), "off to its side");
		assertFalse(TimbermaulRules.inFan(TimbermaulRules.CRUSH_RADIUS + 0.5, 1), "out of the fan");
		assertTrue(TimbermaulRules.topples(TimbermaulRules.TOPPLE_BLOW, true));
		assertFalse(TimbermaulRules.topples(TimbermaulRules.TOPPLE_BLOW - 1, true), "too light");
		assertFalse(TimbermaulRules.topples(20, false), "not reared");
	}

	@Test
	void thePounceLandsOnItsPreyWithinWhatItCanLeapAndCatchesOnlyThoseInTheRing() {
		assertEquals(TimbermaulRules.POUNCE_NEAR, TimbermaulRules.pounceLength(1), 1e-9);
		assertEquals(7, TimbermaulRules.pounceLength(7), 1e-9);
		assertEquals(TimbermaulRules.POUNCE_REACH, TimbermaulRules.pounceLength(20), 1e-9);
		assertTrue(TimbermaulRules.inRing(1.5, 0, TimbermaulRules.POUNCE_RADIUS));
		assertFalse(TimbermaulRules.inRing(TimbermaulRules.POUNCE_RADIUS + 0.5, 0, TimbermaulRules.POUNCE_RADIUS), "left the spot");
		assertFalse(TimbermaulRules.inRing(1, 3, TimbermaulRules.POUNCE_RADIUS), "up out of reach");
	}

	@Test
	void theLogBowlsOverWhoeverItRollsIntoButNotWhoeverJumpsIt() {
		assertTrue(TimbermaulRules.bowls(6, 1, 0, 6.3));
		assertFalse(TimbermaulRules.bowls(6, 1, 0, 4), "not rolled that far yet");
		assertFalse(TimbermaulRules.bowls(6, TimbermaulRules.LOG_WIDTH + 0.5, 0, 6), "beside the lane");
		assertFalse(TimbermaulRules.bowls(6, 0, 1, 6), "jumped it");
	}

	@Test
	void theDigCatchesOnlyThoseBehindItAndRousedAWiderCone() {
		assertTrue(TimbermaulRules.inSpray(4, -0.9, false));
		assertFalse(TimbermaulRules.inSpray(4, 0.5, false), "before it");
		assertFalse(TimbermaulRules.inSpray(4, -0.3, false), "off to the side, calm");
		assertTrue(TimbermaulRules.inSpray(4, -0.3, true), "the wider cone, roused");
		assertFalse(TimbermaulRules.inSpray(TimbermaulRules.DIG_RANGE + 1, -1, true), "out of range");
	}

	@Test
	void itTakesMoreLyingOpenAndLessWhileItRoars() {
		assertEquals(15, TimbermaulRules.hurt(10, TimbermaulRules.TOPPLED), 1e-6);
		assertEquals(15, TimbermaulRules.hurt(10, TimbermaulRules.SPRAWLED), 1e-6);
		assertEquals(15, TimbermaulRules.hurt(10, TimbermaulRules.DAZED), 1e-6);
		assertEquals(12.5, TimbermaulRules.hurt(10, TimbermaulRules.PINNED), 1e-6);
		assertEquals(5, TimbermaulRules.hurt(10, TimbermaulRules.ROAR), 1e-6);
		assertEquals(10, TimbermaulRules.hurt(10, TimbermaulRules.RAKE), 1e-6);
	}

	@Test
	void itRoarsOnceAtHalfAndRousedEveryTellIsQuicker() {
		assertTrue(TimbermaulRules.roars(false, 60, 120));
		assertFalse(TimbermaulRules.roars(false, 61, 120));
		assertFalse(TimbermaulRules.roars(true, 30, 120), "only once");
		assertTrue(TimbermaulRules.ROUSED_RAKE_TELL < TimbermaulRules.RAKE_TELL);
		assertTrue(TimbermaulRules.ROUSED_SNAP_TELL < TimbermaulRules.SNAP_TELL);
		assertTrue(TimbermaulRules.ROUSED_CRUSH_TELL < TimbermaulRules.CRUSH_TELL);
		assertTrue(TimbermaulRules.ROUSED_POUNCE_TELL < TimbermaulRules.POUNCE_TELL);
		assertTrue(TimbermaulRules.ROUSED_CHARGE_TELL < TimbermaulRules.CHARGE_TELL);
		assertTrue(TimbermaulRules.ROUSED_LOG_TELL < TimbermaulRules.LOG_TELL);
		assertTrue(TimbermaulRules.ROUSED_DIG_TELL < TimbermaulRules.DIG_TELL);
		assertTrue(TimbermaulRules.ROUSED_REST < TimbermaulRules.REST);
	}
}
