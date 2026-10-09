package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoneFractureRulesTest {
	@Test
	void onlyStoneHasABoundedPaidFourthAttackForm() {
		for (int school = 0; school <= 2; school++) for (int sequence = 0; sequence < 20; sequence++) {
			assertEquals(school == MastersRules.STONE && sequence % 4 == 1,
				StoneFractureRules.eligible(school, sequence, 4, 0, 28, 180, 180));
		}
		assertFalse(StoneFractureRules.eligible(MastersRules.STONE, -3, 4, 0, 28, 180, 180));
		assertFalse(StoneFractureRules.eligible(MastersRules.STONE, 1, 4, 0, 27.999, 180, 180));
		assertFalse(StoneFractureRules.eligible(MastersRules.STONE, 1, 4, 0, 100, 179, 180));
		assertTrue(StoneFractureRules.COST >= MastersRules.ATTACK_COST + MastersRules.GUARD_COST);
		assertEquals(3, (int) (MastersRules.AURA_MAX / StoneFractureRules.COST));
		assertTrue(StoneFractureRules.COOLDOWN > StoneFractureRules.TELL + StoneFractureRules.RECOVERY);
		for (int count = 1; count <= MastersRules.MAX_PARTICIPANTS; count++) {
			assertEquals(30.8, MastersRules.damage(count, MastersRules.STONE, MastersRules.Move.STONE_FRACTURE), 1e-9);
			assertEquals(MastersRules.damage(count, MastersRules.STONE, MastersRules.Move.PURSUIT_BREAK),
				MastersRules.damage(count, MastersRules.STONE, MastersRules.Move.STONE_FRACTURE));
		}
	}

	@Test
	void admissionRejectsBadInputsAndPreservesCloseRangeLimits() {
		for (double distance : new double[] {1.499, 5.501, -1, Double.NaN, Double.POSITIVE_INFINITY})
			assertFalse(StoneFractureRules.eligible(2, 1, distance, 0, 28, 180, 180));
		for (double distance : new double[] {1.5, 4, 5.5}) assertTrue(StoneFractureRules.eligible(2, 1, distance, 1, 28, 180, 180));
		for (double height : new double[] {-1.001, 1.001, Double.NaN, Double.NEGATIVE_INFINITY})
			assertFalse(StoneFractureRules.eligible(2, 1, 4, height, 28, 180, 180));
		for (double aura : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY})
			assertFalse(StoneFractureRules.eligible(2, 1, 4, 0, aura, 180, 180));
	}

	@Test
	void braceAndReplyHaveSeparateCompleteWarningsAndAnExposedRecovery() {
		for (int age = -1; age <= 33; age++) {
			var expected = age < 0 || age > 32 ? StoneFractureRules.Beat.EXPIRED
				: age < 8 ? StoneFractureRules.Beat.PLANT : age < 20 ? StoneFractureRules.Beat.BRACE
				: age < 32 ? StoneFractureRules.Beat.REPLY_WARNING : StoneFractureRules.Beat.REPLY;
			assertEquals(expected, StoneFractureRules.beat(age));
		}
		assertEquals(32, MastersRules.Move.STONE_FRACTURE.tell);
		assertEquals(30, MastersRules.Move.STONE_FRACTURE.recovery);
		assertTrue(MastersRules.interruptible(true, 25, 32));
		assertFalse(MastersRules.interruptible(true, 26, 32), "The final reply keeps the original committed six ticks");
	}

	@Test
	void lockedLaneAllowsSideRearDistanceAndHeightAnswers() {
		assertTrue(StoneFractureRules.hits(6, .7, 1.8));
		for (double[] p : new double[][] {{-.001, 0, 0}, {6.001, 0, 0}, {4, .701, 0}, {4, -.701, 0}, {4, 0, 1.801},
			{Double.NaN, 0, 0}, {4, Double.POSITIVE_INFINITY, 0}, {4, 0, Double.NaN}})
			assertFalse(StoneFractureRules.hits(p[0], p[1], p[2]));
		assertTrue(StoneFractureRules.rear(-.001));
		for (double front : new double[] {0, 1, Double.NaN, Double.NEGATIVE_INFINITY}) assertFalse(StoneFractureRules.rear(front));
		assertTrue(MastersRules.hits(MastersRules.Move.STONE_FRACTURE, 4, .7, 0));
		assertFalse(MastersRules.hits(MastersRules.Move.STONE_FRACTURE, 4, .701, 0));
	}
}
