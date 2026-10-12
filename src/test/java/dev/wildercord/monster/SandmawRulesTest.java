package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildercord.cast.MoveDeck;
import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SandmawRulesTest {
	private static double weight(double distance, boolean crowded, int move) {
		double[] w = SandmawRules.weights(distance, crowded);
		for (int i = 0; i < SandmawRules.PICKS.length; i++) {
			if (SandmawRules.PICKS[i] == move) {
				return w[i];
			}
		}
		throw new IllegalArgumentException("not a pick: " + move);
	}

	@Test
	void itFeelsFootstepsButBarelyASneak() {
		assertTrue(SandmawRules.feels(12, false));
		assertFalse(SandmawRules.feels(12, true));
		assertTrue(SandmawRules.feels(4, true));
	}

	@Test
	void itRisesUnderItsPreyOrWhenTiredOfWaiting() {
		assertTrue(SandmawRules.rises(1, SandmawRules.MIN_BURROW));
		assertFalse(SandmawRules.rises(1, SandmawRules.MIN_BURROW - 1), "only just went down");
		assertFalse(SandmawRules.rises(4, SandmawRules.MIN_BURROW), "not under them yet");
		assertTrue(SandmawRules.rises(20, SandmawRules.MAX_BURROW), "tired of waiting");
	}

	@Test
	void itBreachesAtPreyAWayOffButNeverTwiceRunning() {
		assertTrue(SandmawRules.breaches(8, SandmawRules.MIN_BURROW, false, 0));
		assertFalse(SandmawRules.breaches(8, SandmawRules.MIN_BURROW, true, 0), "just breached");
		assertFalse(SandmawRules.breaches(2, SandmawRules.MIN_BURROW, false, 0), "close enough to rise under them");
		assertFalse(SandmawRules.breaches(SandmawRules.BREACH_FAR + 1, SandmawRules.MIN_BURROW, false, 0), "too far");
		assertFalse(SandmawRules.breaches(8, SandmawRules.MIN_BURROW - 1, false, 0), "only just went down");
		assertFalse(SandmawRules.breaches(8, SandmawRules.MIN_BURROW, false, 0.99), "not this time");
	}

	@Test
	void itBreachesPastItsPreyWithinBounds() {
		assertEquals(10 + SandmawRules.BREACH_PAST, SandmawRules.breachLength(10));
		assertEquals(SandmawRules.BREACH_MIN, SandmawRules.breachLength(1));
		assertEquals(SandmawRules.BREACH_MAX, SandmawRules.breachLength(40));
	}

	@Test
	void upTopItPicksWhatSuitsTheRange() {
		assertTrue(weight(3, false, SandmawRules.SLAM) > 0, "slams the near");
		assertEquals(0, weight(SandmawRules.SLAM_LENGTH + 1, false, SandmawRules.SLAM));
		assertEquals(0, weight(2, false, SandmawRules.SPIT), "too close to spit");
		assertTrue(weight(10, false, SandmawRules.SPIT) > weight(6, false, SandmawRules.SPIT), "spits at the far most");
		assertEquals(0, weight(1, false, SandmawRules.GULP), "too close to plunge on");
		assertEquals(0, weight(SandmawRules.GULP_REACH + 1, false, SandmawRules.GULP));
		assertTrue(weight(SandmawRules.SINK_RADIUS, false, SandmawRules.SINKHOLE) > 0);
		assertEquals(0, weight(SandmawRules.SINK_RADIUS + 1, false, SandmawRules.SINKHOLE));
		assertTrue(weight(14, false, SandmawRules.GEYSER) > 0, "geysers reach the farthest");
		assertTrue(weight(3, true, SandmawRules.QUILLS) > weight(3, false, SandmawRules.QUILLS), "crowded, it bristles");
		for (int move : SandmawRules.PICKS) {
			assertEquals(0, weight(SandmawRules.GEYSER_REACH + 1, false, move), "nothing reaches that far");
		}
	}

	@Test
	void itNeverMakesTheSameMoveTwiceRunningAndAlwaysHasOneInReach() {
		var deck = new MoveDeck(SandmawRules.MEMORY);
		var rng = new Random(7);
		var seen = new HashSet<Integer>();
		int last = -1;
		for (int i = 0; i < 400; i++) {
			double d = rng.nextDouble() * 12;
			int move = SandmawRules.choose(deck, d, rng.nextBoolean(), rng.nextDouble());
			assertTrue(move >= 0, "nothing to do at " + d);
			assertNotEquals(last, move);
			deck.played(move);
			last = move;
			seen.add(move);
		}
		for (int move : SandmawRules.PICKS) {
			assertTrue(seen.contains(move), "never " + move);
		}
	}

	@Test
	void itDivesAfterPreyBeyondEverythingItHas() {
		assertEquals(-1, SandmawRules.choose(new MoveDeck(SandmawRules.MEMORY), SandmawRules.GEYSER_REACH + 2, false, 0.5));
	}

	@Test
	void theSlamStrikesAlongItsLineOnly() {
		assertTrue(SandmawRules.inLine(4, 0.5));
		assertFalse(SandmawRules.inLine(4, 2), "stepped aside");
		assertFalse(SandmawRules.inLine(SandmawRules.SLAM_LENGTH + 1, 0), "beyond its reach");
		assertFalse(SandmawRules.inLine(-2, 0), "behind it");
	}

	@Test
	void theSpitStrikesDownItsCone() {
		assertTrue(SandmawRules.inSpit(8, 0.95));
		assertFalse(SandmawRules.inSpit(8, 0.3), "off to the side");
		assertFalse(SandmawRules.inSpit(SandmawRules.SPIT_RANGE + 1, 1), "out of range");
	}

	@Test
	void theGulpSwallowsOnlyWhoeverStaysOnTheSpot() {
		assertTrue(SandmawRules.swallows(1, 0));
		assertFalse(SandmawRules.swallows(SandmawRules.GULP_RADIUS + 0.5, 0), "stepped off");
		assertFalse(SandmawRules.swallows(1, 3), "jumped clear above");
	}

	@Test
	void theSinkholeDragsFromFarButOnlyItsJawsBite() {
		assertTrue(SandmawRules.drags(8, 0));
		assertFalse(SandmawRules.drags(SandmawRules.SINK_RADIUS + 1, 0), "outside it");
		assertFalse(SandmawRules.drags(0.2, 0), "already at the middle");
		assertTrue(SandmawRules.snaps(2, 0));
		assertFalse(SandmawRules.snaps(4, 0), "held out of reach of the jaws");
	}

	@Test
	void theSpinesFlyAlongRaysWithGapsBetween() {
		double gap = 360.0 / SandmawRules.QUILL_RAYS;
		assertTrue(SandmawRules.quilled(0, 5, 0, 0), "on a ray");
		assertTrue(SandmawRules.quilled(gap * 3, 5, 0, 0), "on another ray");
		assertFalse(SandmawRules.quilled(gap / 2, 5, 0, 0), "in the gap between");
		assertTrue(SandmawRules.quilled(gap / 2, 5, 0, gap / 2), "the gaps of the first are the rays of the second");
		assertFalse(SandmawRules.quilled(0, SandmawRules.QUILL_LENGTH + 1, 0, 0), "out of reach");
		assertFalse(SandmawRules.quilled(0, 5, 4, 0), "far above");
		assertTrue(SandmawRules.quilled(gap / 2, 1, 0, 0), "close in, the gaps narrow to nothing");
	}

	@Test
	void itDivesWhenUpLongOrHurtEnough() {
		assertTrue(SandmawRules.dives(SandmawRules.MAX_SURFACED, 0, 90));
		assertTrue(SandmawRules.dives(10, 25, 90));
		assertFalse(SandmawRules.dives(10, 10, 90));
	}

	@Test
	void itThrashesOnceAtHalfHealth() {
		assertTrue(SandmawRules.thrashes(false, 45, 90));
		assertFalse(SandmawRules.thrashes(false, 46, 90));
		assertFalse(SandmawRules.thrashes(true, 20, 90), "already roused");
	}

	@Test
	void itCannotBeHurtUnderTheSandAndIsOpenWhenCaughtOut() {
		assertEquals(0, SandmawRules.hurt(10, true, SandmawRules.STUCK));
		assertEquals(15, SandmawRules.hurt(10, false, SandmawRules.STUCK));
		assertEquals(15, SandmawRules.hurt(10, false, SandmawRules.CHOKING));
		assertEquals(15, SandmawRules.hurt(10, false, SandmawRules.BEACHED));
		assertEquals(12.5F, SandmawRules.hurt(10, false, SandmawRules.SINKHOLE));
		assertEquals(12.5F, SandmawRules.hurt(10, false, SandmawRules.GULPED), "hit it to make it let go");
		assertEquals(5, SandmawRules.hurt(10, false, SandmawRules.THRASH));
		assertEquals(10, SandmawRules.hurt(10, false, SandmawRules.SLAM));
	}

	@Test
	void itIsTheSoftestElite() {
		assertEquals(EliteTier.BRUTE, SandmawRules.TIER);
		assertTrue(EliteTier.BRUTE.health < EliteTier.CHAMPION.health && EliteTier.CHAMPION.health < EliteTier.WARLORD.health);
		assertTrue(EliteTier.WARLORD.health < dev.wildercord.aura.world.BossTier.APPRENTICE.health, "below every guardian");
	}
}
