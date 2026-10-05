package dev.wildercord.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Old-vs-new cadence and decisions, not a simulated win-rate or performance benchmark. */
class MonsterPressureRulesTest {
	@Test
	void easyAndPeacefulKeepEveryOriginalCadenceEndpoint() {
		for (int difficulty : new int[] {-10, 0, 1}) {
			assertFalse(MonsterPressureRules.active(difficulty));
			assertEquals(new MonsterPressureRules.Delay(40, 79), MonsterPressureRules.gloomStalk(difficulty, false));
			assertEquals(new MonsterPressureRules.Delay(50, 99), MonsterPressureRules.gloomStalk(difficulty, true));
			assertEquals(new MonsterPressureRules.Delay(50, 79), MonsterPressureRules.gloomRetreat(difficulty));
			assertEquals(new MonsterPressureRules.Delay(80, 129), MonsterPressureRules.frogBubble(difficulty));
			assertEquals(1.1, MonsterPressureRules.gloomChaseSpeed(difficulty));
			assertFalse(MonsterPressureRules.frogApproach(difficulty, 24, false));
		}
	}

	@Test
	void normalAndHardHaveShorterButBoundedIdleIntervals() {
		assertEquals(new MonsterPressureRules.Delay(26, 45), MonsterPressureRules.gloomStalk(2, false));
		assertEquals(new MonsterPressureRules.Delay(20, 35), MonsterPressureRules.gloomStalk(3, false));
		assertEquals(new MonsterPressureRules.Delay(28, 47), MonsterPressureRules.gloomStalk(2, true));
		assertEquals(new MonsterPressureRules.Delay(22, 37), MonsterPressureRules.gloomStalk(3, true));
		assertEquals(new MonsterPressureRules.Delay(32, 47), MonsterPressureRules.gloomRetreat(2));
		assertEquals(new MonsterPressureRules.Delay(26, 39), MonsterPressureRules.gloomRetreat(3));
		assertEquals(new MonsterPressureRules.Delay(60, 89), MonsterPressureRules.frogBubble(2));
		assertEquals(new MonsterPressureRules.Delay(50, 74), MonsterPressureRules.frogBubble(3));
		for (int difficulty = 2; difficulty <= 3; difficulty++) {
			assertTrue(MonsterPressureRules.gloomStalk(difficulty, false).max() < 79);
			assertTrue(MonsterPressureRules.gloomStalk(difficulty, true).max() < 99);
			assertTrue(MonsterPressureRules.gloomRetreat(difficulty).min() >= 26);
			assertTrue(MonsterPressureRules.frogBubble(difficulty).min() - MonsterPressureRules.FROG_SWELL >= 32);
		}
	}

	@Test
	void everySampleIncludesBothEndpointsAndStaysInsideItsInterval() {
		for (int difficulty = 0; difficulty <= 3; difficulty++) {
			for (var delay : new MonsterPressureRules.Delay[] {MonsterPressureRules.gloomStalk(difficulty, false),
				MonsterPressureRules.gloomStalk(difficulty, true), MonsterPressureRules.gloomRetreat(difficulty),
				MonsterPressureRules.frogBubble(difficulty)}) {
				assertEquals(delay.min(), delay.sample(0));
				assertEquals(delay.max(), delay.sample(delay.spread() - 1));
				for (int roll = -200; roll <= 200; roll++) {
					assertTrue(delay.sample(roll) >= delay.min() && delay.sample(roll) <= delay.max());
				}
			}
		}
	}

	@Test
	void difficultyCannotScaleBeyondHard() {
		assertEquals(MonsterPressureRules.gloomStalk(3, true), MonsterPressureRules.gloomStalk(Integer.MAX_VALUE, true));
		assertEquals(MonsterPressureRules.gloomRetreat(3), MonsterPressureRules.gloomRetreat(Integer.MAX_VALUE));
		assertEquals(MonsterPressureRules.frogBubble(3), MonsterPressureRules.frogBubble(Integer.MAX_VALUE));
		assertEquals(1.25, MonsterPressureRules.gloomChaseSpeed(Integer.MAX_VALUE));
		assertEquals(1.15, MonsterPressureRules.frogChaseSpeed(Integer.MAX_VALUE));
	}

	@Test
	void theOldNinePointFiveToTenBlockOrbitGapBecomesAPursuit() {
		for (double distance : new double[] {9.51, 9.75, 10}) {
			assertFalse(MonsterPressureRules.gloomChase(1, distance, 0, true), "legacy Easy orbit");
			assertFalse(MonsterPressureRules.gloomCanCrouch(distance, 0, true));
			assertTrue(MonsterPressureRules.gloomChase(2, distance, 0, true));
			assertTrue(MonsterPressureRules.gloomChase(3, distance, 0, true));
		}
		assertFalse(MonsterPressureRules.gloomChase(2, 6, 0, true), "the signature circling remains in range");
	}

	@Test
	void coverAndHighLedgesSelectGroundNavigationInsteadOfBlindPounces() {
		assertFalse(MonsterPressureRules.gloomChase(1, 6, 0, false));
		assertTrue(MonsterPressureRules.gloomChase(2, 6, 0, false));
		assertTrue(MonsterPressureRules.gloomChase(2, 6, 4, true));
		assertFalse(MonsterPressureRules.gloomCanCrouch(6, 4, true));
		assertFalse(MonsterPressureRules.gloomCanCrouch(6, 0, false));
		assertTrue(MonsterPressureRules.frogApproach(2, 6, false));
		assertTrue(MonsterPressureRules.frogApproach(2, 16.5, true));
		assertFalse(MonsterPressureRules.frogApproach(2, 8, true));
	}

	@Test
	void releasingThePounceRechecksRangeHeightAndCover() {
		assertTrue(MonsterPressureRules.gloomCanCrouch(3.2, 0, true));
		assertTrue(MonsterPressureRules.gloomCanCrouch(9.5, 2, true));
		assertTrue(MonsterPressureRules.gloomCanRelease(2, 0, true));
		assertFalse(MonsterPressureRules.gloomCanCrouch(2, 0, true));
		assertFalse(MonsterPressureRules.gloomCanRelease(9.51, 0, true));
		assertFalse(MonsterPressureRules.gloomCanRelease(6, 2.01, true));
		assertFalse(MonsterPressureRules.gloomCanRelease(6, -3.01, true));
		assertFalse(MonsterPressureRules.gloomCanRelease(6, 0, false));
		assertFalse(MonsterPressureRules.gloomCanRelease(Double.NaN, 0, true));
		assertFalse(MonsterPressureRules.gloomCanRelease(6, Double.NaN, true));
	}

	@Test
	void readyTongueCanCatchBackpedallingWithoutRemovingItsCounters() {
		assertFalse(MonsterPressureRules.frogTongueFirst(1, 5, true, true), "Easy keeps bubble-first behavior");
		assertTrue(MonsterPressureRules.frogTongueFirst(2, 5, true, true));
		assertTrue(MonsterPressureRules.frogTongueFirst(3, 6, true, true));
		assertFalse(MonsterPressureRules.frogTongueFirst(2, 6.01, true, true));
		assertFalse(MonsterPressureRules.frogTongueFirst(2, 2.79, true, true));
		assertFalse(MonsterPressureRules.frogTongueFirst(2, 5, false, true));
		assertFalse(MonsterPressureRules.frogTongueFirst(2, 5, true, false));
	}

	@Test
	void movingOutOfRangeOrBehindCoverDuringTheSwellCancelsItsShot() {
		assertTrue(MonsterPressureRules.frogCanBubble(4, true));
		assertTrue(MonsterPressureRules.frogCanBubble(16, true));
		assertFalse(MonsterPressureRules.frogCanBubble(3.99, true));
		assertFalse(MonsterPressureRules.frogCanBubble(16.01, true));
		assertFalse(MonsterPressureRules.frogCanBubble(8, false));
		assertFalse(MonsterPressureRules.frogCanBubble(Double.NaN, true));
	}

	@Test
	void telegraphsMissRecoveryAndPursuitSpeedsStayBounded() {
		assertEquals(14, MonsterPressureRules.GLOOM_CROUCH);
		assertEquals(30, MonsterPressureRules.GLOOM_MISSED);
		assertEquals(18, MonsterPressureRules.FROG_SWELL);
		assertEquals(10, MonsterPressureRules.FROG_MOUTH);
		assertEquals(20, MonsterPressureRules.FROG_GULP);
		assertEquals(20, MonsterPressureRules.GLOOM_CANCEL_RETRY);
		assertEquals(1.20, MonsterPressureRules.gloomChaseSpeed(2));
		assertEquals(1.10, MonsterPressureRules.frogChaseSpeed(2));
		assertEquals(8, MonsterPressureRules.repathDelay(true));
		assertEquals(20, MonsterPressureRules.repathDelay(false));
	}
}
