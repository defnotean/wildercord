package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MasterPursuitRulesTest {
	@Test
	void appendsOneMoveWithoutChangingExistingIdsOrTells() {
		assertEquals(5, MastersRules.Move.CINDER_WAKE.ordinal() + 1);
		assertEquals(6, MastersRules.Move.PURSUIT_BREAK.ordinal() + 1);
		assertEquals(6, MasterAnimationRules.PURSUIT_BREAK);
		assertEquals(18, MastersRules.Move.SWEEP.tell);
		assertEquals(20, MastersRules.Move.BREAK_CAST.tell);
		assertEquals(32, MastersRules.Move.PURSUIT_BREAK.tell);
		assertEquals(30, MastersRules.Move.PURSUIT_BREAK.recovery);
	}

	@Test
	void allThreeBeatsRemainHarmlessUntilOneExactStrike() {
		for (long t = -1; t <= 34; t++) {
			var beat = MasterPursuitRules.beat(t);
			if (t < 0 || t > 32) assertEquals(MasterPursuitRules.Beat.EXPIRED, beat);
			else if (t < 12) assertEquals(MasterPursuitRules.Beat.WARNING, beat);
			else if (t < 20) assertEquals(MasterPursuitRules.Beat.DASH, beat);
			else if (t < 32) assertEquals(MasterPursuitRules.Beat.STRIKE_WARNING, beat);
			else assertEquals(MasterPursuitRules.Beat.STRIKE, beat);
		}
		assertTrue(MasterPursuitRules.STRIKE_TELL >= 12);
		assertTrue(MasterPursuitRules.RECOVERY >= 30);
	}

	@Test
	void finiteSchoolTuningChangesFootworkWithoutChangingLearnedTiming() {
		var gale = MasterPursuitRules.school(MastersRules.GALE);
		var ember = MasterPursuitRules.school(MastersRules.EMBER);
		var stone = MasterPursuitRules.school(MastersRules.STONE);
		assertTrue(gale.travel() > ember.travel() && ember.travel() > stone.travel());
		assertTrue(gale.cooldown() < ember.cooldown() && ember.cooldown() < stone.cooldown());
		for (int school = 0; school < 3; school++) {
			var tuning = MasterPursuitRules.school(school);
			assertTrue(tuning.cost() >= 26 && tuning.cost() <= 30);
			assertTrue(tuning.cooldown() >= MasterPursuitRules.TELL + MasterPursuitRules.RECOVERY);
			assertTrue(tuning.travel() / MasterPursuitRules.DASH_TICKS <= .8);
			assertTrue(tuning.range() - tuning.travel() < MasterPursuitRules.REACH);
			assertFalse(MasterPursuitRules.eligible(school, 6, 0, tuning.cost() - .01, 100, 100));
			assertTrue(MasterPursuitRules.eligible(school, 6, 0, tuning.cost(), 100, 100));
			assertFalse(MasterPursuitRules.eligible(school, 6, 0, 100, 99, 100));
			for (int count = 1; count <= 8; count++) assertEquals(MastersRules.damage(1, school, MastersRules.Move.PURSUIT_BREAK),
				MastersRules.damage(count, school, MastersRules.Move.PURSUIT_BREAK));
		}
	}

	@Test
	void oneTickFeintsNeverQualifyButAnObservedHeldCastCanBeBaited() {
		for (int age = -2; age < 6; age++) assertFalse(MasterPursuitRules.observedCharge(age));
		assertTrue(MasterPursuitRules.observedCharge(6));
		assertTrue(MasterPursuitRules.observedCharge(200));
		assertFalse(MasterPursuitRules.observedCharge(201));
	}

	@Test
	void inputsAndStaleOpeningsAreBounded() {
		for (double distance : new double[] {0, 4, 10, Double.NaN, Double.POSITIVE_INFINITY})
			assertFalse(MasterPursuitRules.eligible(MastersRules.GALE, distance, 0, 100, 100, 0));
		assertFalse(MasterPursuitRules.eligible(MastersRules.GALE, 6, 1.01, 100, 100, 0));
		assertFalse(MasterPursuitRules.eligible(MastersRules.GALE, 6, 0, Double.NaN, 100, 0));
		assertEquals(0, MasterPursuitRules.travel(1, 6, Double.NaN));
		assertEquals(0, MasterPursuitRules.travel(1, 6, 0));
		assertEquals(4, MasterPursuitRules.travel(1, 6, 1));
		assertEquals(2, MasterPursuitRules.travel(1, 6, .5));
		assertEquals(4, MasterPursuitRules.travel(1, 6, 10));
	}

	@Test
	void eightBoundedSegmentsReachOnlyTheCapturedEndpoint() {
		assertEquals(0, MasterPursuitRules.travelFraction(11));
		double previous = 0;
		for (int tick = 12; tick < 20; tick++) {
			double next = MasterPursuitRules.travelFraction(tick);
			assertEquals(1.0 / 8, next - previous, 1e-12);
			previous = next;
		}
		assertEquals(1, previous);
		assertEquals(1, MasterPursuitRules.travelFraction(1000));
	}

	@Test
	void shortLockedLaneLeavesSideAndBackstepAnswers() {
		assertTrue(MasterPursuitRules.hits(2, 0, 0));
		assertFalse(MasterPursuitRules.hits(2, .81, 0));
		assertFalse(MasterPursuitRules.hits(3.26, 0, 0));
		assertFalse(MasterPursuitRules.hits(-.01, 0, 0));
		assertFalse(MasterPursuitRules.hits(2, 0, 1.81));
		assertFalse(MasterPursuitRules.hits(Double.NaN, 0, 0));
	}

	@Test
	void fullSweptFootprintMustStayInsideTheArena() {
		assertTrue(MasterPursuitRules.insideArena(-.4, .4, -.4, 6.8, 0, 0, 24));
		assertFalse(MasterPursuitRules.insideArena(23.5, 24.1, -.3, .3, 0, 0, 24));
		assertFalse(MasterPursuitRules.insideArena(16.8, 17.4, 16.8, 17.4, 0, 0, 24));
		assertFalse(MasterPursuitRules.insideArena(0, Double.NaN, 0, 1, 0, 0, 24));
		assertFalse(MasterPursuitRules.insideArena(1, 0, 0, 1, 0, 0, 24));
	}

	@Test
	void originalFallbackFlowsFromStepToPlantedStrikeToExposedRecovery() {
		var step = pose(12); var strike = pose(32);
		assertEquals(1, step.weight());
		assertEquals(1, strike.weight());
		assertTrue(step.body().x() > strike.body().x());
		assertTrue(strike.bladeTilt() < -90);
		assertNotEquals(step, strike);
		assertTrue(pose(16).stance() < 0 && pose(12).stance() > 0, "Feet alternate during real server travel and plant before the strike");
		for (float frame : new float[] {0, 6, 12, 20, 20.8F, 32, 36, 62}) {
			var a = pose(frame - .0001F); var b = pose(frame + .0001F);
			assertEquals(a.body().x() * a.weight(), b.body().x() * b.weight(), .001F);
			assertEquals(a.sword().x() * a.weight(), b.sword().x() * b.weight(), .001F);
			assertEquals(a.bladeTilt() * a.weight(), b.bladeTilt() * b.weight(), .02F);
		}
		assertSame(MasterAnimationRules.NONE, pose(62));
	}

	private static MasterAnimationRules.Pose pose(float age) {
		return MasterAnimationRules.sample(6, age, 32, 1, 29);
	}
}
