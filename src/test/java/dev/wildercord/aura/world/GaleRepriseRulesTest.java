package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GaleRepriseRulesTest {
	@Test
	void onlyGaleUsesTheFourthAttackCadenceWithinItsFiniteBudget() {
		for (int school = MastersRules.EMBER; school <= MastersRules.STONE; school++) {
			for (int sequence = 0; sequence < 20; sequence++) {
				assertEquals(school == MastersRules.GALE && sequence % 4 == 1,
					GaleRepriseRules.eligible(school, sequence, 4, 0, 28, 140, 140));
			}
		}
		assertFalse(GaleRepriseRules.eligible(MastersRules.GALE, -3, 4, 0, 100, 140, 140));
		assertEquals(24, GaleRepriseRules.COST);
		assertEquals(28, GaleRepriseRules.READY_AURA);
		for (double aura : new double[] {23.999, 24, 27.999}) {
			assertFalse(GaleRepriseRules.eligible(MastersRules.GALE, 1, 4, 0, aura, 140, 140),
				"The existing shared exhaustion threshold remains authoritative before admission");
		}
		assertFalse(GaleRepriseRules.eligible(MastersRules.GALE, 1, 4, 0, 100, 139, 140));
		assertEquals(4, (int) (MastersRules.AURA_MAX / GaleRepriseRules.COST), "Even without other costs, an Aura pool funds only four attempts");
		assertTrue(GaleRepriseRules.COOLDOWN > GaleRepriseRules.TELL + GaleRepriseRules.RECOVERY);
	}

	@Test
	void admissionRejectsNonFiniteInputsAndOutOfReachTargets() {
		for (double distance : new double[] {2.499, 4.751, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
			assertFalse(GaleRepriseRules.eligible(MastersRules.GALE, 1, distance, 0, 100, 140, 140));
		}
		for (double distance : new double[] {2.5, 4, 4.75}) {
			assertTrue(GaleRepriseRules.eligible(MastersRules.GALE, 1, distance, 1, 100, 140, 140));
		}
		for (double height : new double[] {1.001, -1.001, Double.NaN, Double.NEGATIVE_INFINITY}) {
			assertFalse(GaleRepriseRules.eligible(MastersRules.GALE, 1, 4, height, 100, 140, 140));
		}
		for (double aura : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
			assertFalse(GaleRepriseRules.eligible(MastersRules.GALE, 1, 4, 0, aura, 140, 140));
		}
	}

	@Test
	void footworkMovesExactlyOnePointEightBlocksAndNeverShortensTheReplyTell() {
		assertEquals(GaleRepriseRules.Beat.EXPIRED, GaleRepriseRules.beat(-1));
		assertEquals(GaleRepriseRules.Beat.GATHER, GaleRepriseRules.beat(7));
		assertEquals(GaleRepriseRules.Beat.STEP, GaleRepriseRules.beat(8));
		assertEquals(GaleRepriseRules.Beat.STEP, GaleRepriseRules.beat(11));
		assertEquals(GaleRepriseRules.Beat.REPLY_WARNING, GaleRepriseRules.beat(12));
		assertEquals(GaleRepriseRules.Beat.REPLY_WARNING, GaleRepriseRules.beat(21));
		assertEquals(GaleRepriseRules.Beat.REPLY, GaleRepriseRules.beat(22));
		assertEquals(GaleRepriseRules.Beat.EXPIRED, GaleRepriseRules.beat(23));
		double previous = 0, total = 0;
		for (int age = 0; age <= GaleRepriseRules.TELL; age++) {
			double position = GaleRepriseRules.STEP_DISTANCE * GaleRepriseRules.travelFraction(age);
			double step = position - previous;
			assertEquals(age >= 8 && age < 12 ? .45 : 0, step, 1e-9);
			total += step; previous = position;
		}
		assertEquals(1.8, total, 1e-9);
		assertEquals(10, GaleRepriseRules.TELL - GaleRepriseRules.GATHER - GaleRepriseRules.STEP_TICKS);
		assertEquals(32, MastersRules.Move.CROSSWIND_REPRISE.recovery);
	}

	@Test
	void slowsCanShortenFootworkButHasteCannotExtendIt() {
		assertEquals(.9, GaleRepriseRules.travel(.5), 1e-9);
		assertEquals(1.8, GaleRepriseRules.travel(1), 1e-9);
		assertEquals(1.8, GaleRepriseRules.travel(10), 1e-9);
		for (double ratio : new double[] {-1, 0, Double.NaN, Double.POSITIVE_INFINITY}) assertEquals(0, GaleRepriseRules.travel(ratio));
	}

	@Test
	void narrowLockedReplyHasRealSidestepBackstepAndVerticalMisses() {
		assertTrue(GaleRepriseRules.hits(5.25, .75, 1.8));
		assertFalse(GaleRepriseRules.hits(5.251, 0, 0));
		assertFalse(GaleRepriseRules.hits(4, .751, 0));
		assertFalse(GaleRepriseRules.hits(4, -.751, 0));
		assertFalse(GaleRepriseRules.hits(-.001, 0, 0));
		assertFalse(GaleRepriseRules.hits(4, 0, 1.801));
		assertFalse(GaleRepriseRules.hits(Double.NaN, 0, 0));
		assertFalse(GaleRepriseRules.hits(4, Double.POSITIVE_INFINITY, 0));
		assertFalse(GaleRepriseRules.hits(4, 0, Double.NaN));
		for (int party = 1; party <= MastersRules.MAX_PARTICIPANTS; party++) {
			assertEquals(26, MastersRules.damage(party, MastersRules.GALE, MastersRules.Move.CROSSWIND_REPRISE));
		}
		assertEquals(GaleRepriseRules.hits(4, .6, 0), MastersRules.hits(MastersRules.Move.CROSSWIND_REPRISE, 4, .6, 0));
	}
}
