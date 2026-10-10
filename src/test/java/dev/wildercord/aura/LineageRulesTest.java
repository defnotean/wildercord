package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Masters and disciples, the pure part ({@link LineageRules}): who may take whom and why not, the ceremony's parts, a disciple's faster
 * learning near their master, the master's trial and where it stops, the master's share, the end with honour, and the daily lesson.
 */
class LineageRulesTest {

	@Test
	void aMasterIsFormOrAboveAndADiscipleTwoStagesBelow() {
		assertEquals(AuraRules.FORM, LineageRules.MASTER_FROM);
		assertEquals(2, LineageRules.GAP);
		// A Form master takes Flow, Glow, or one not yet breathing.
		for (int d : new int[] {AuraRules.NONE, AuraRules.GLOW, AuraRules.FLOW}) {
			assertNull(LineageRules.refusal(true, AuraRules.FORM, d, 0, 3, false, false), "Form takes stage " + d);
		}
		assertEquals(LineageRules.Refusal.GAP, LineageRules.refusal(true, AuraRules.FORM, AuraRules.EDGE, 0, 3, false, false));
		// A Sovereign takes up to Edge.
		assertNull(LineageRules.refusal(true, AuraRules.SOVEREIGN, AuraRules.EDGE, 0, 3, false, false));
		assertEquals(LineageRules.Refusal.GAP, LineageRules.refusal(true, AuraRules.SOVEREIGN, AuraRules.FORM, 0, 3, false, false));
		assertEquals(LineageRules.Refusal.MASTER_STAGE, LineageRules.refusal(true, AuraRules.EDGE, AuraRules.GLOW, 0, 3, false, false));
	}

	@Test
	void theRefusalsComeInOrder() {
		assertEquals(LineageRules.Refusal.OFF, LineageRules.refusal(false, AuraRules.SOVEREIGN, AuraRules.GLOW, 0, 3, false, false));
		assertEquals(LineageRules.Refusal.ALREADY, LineageRules.refusal(true, AuraRules.EDGE, AuraRules.EDGE, 9, 3, true, true),
			"one already theirs is told so first");
		assertEquals(LineageRules.Refusal.HAS_MASTER, LineageRules.refusal(true, AuraRules.FORM, AuraRules.GLOW, 3, 3, false, true));
		assertEquals(LineageRules.Refusal.FULL, LineageRules.refusal(true, AuraRules.FORM, AuraRules.GLOW, 3, 3, false, false));
		assertNull(LineageRules.refusal(true, AuraRules.FORM, AuraRules.GLOW, 2, 3, false, false));
		assertEquals(LineageRules.Refusal.FULL, LineageRules.refusal(true, AuraRules.FORM, AuraRules.GLOW, 1, 0, false, false), "a master keeps one at least");
		assertEquals("message.wildercord.aura.lineage.refused.has_master", LineageRules.Refusal.HAS_MASTER.key());
	}

	@Test
	void theCeremonyAsksBindsAndSeals() {
		assertEquals(0, LineageRules.phase(0));
		assertEquals(0, LineageRules.phase(LineageRules.ASK_END - 1));
		assertEquals(1, LineageRules.phase(LineageRules.ASK_END));
		assertEquals(2, LineageRules.phase(LineageRules.BIND_END));
		assertEquals(2, LineageRules.phase(LineageRules.CEREMONY_TICKS));
		assertTrue(LineageRules.ASK_END > 20, "both have time to read who asks whom, and to stand up if it isn't wanted");
		assertTrue(LineageRules.CEREMONY_TICKS >= 100 && LineageRules.CEREMONY_TICKS <= 240);
		assertEquals(BladeRules.PASS_REACH, LineageRules.REACH, 1e-9, "the same kneeling as the blade's passing");
		assertEquals(BladeRules.PASS_FACING, LineageRules.FACING, 1e-9);
		assertTrue(LineageRules.LESSON_TICKS < LineageRules.CEREMONY_TICKS);
	}

	@Test
	void aDiscipleLearnsFasterNearTheirMaster() {
		assertEquals(10.0, LineageRules.near(10, false, 1.25), 1e-9);
		assertEquals(12.5, LineageRules.near(10, true, 1.25), 1e-9);
		assertEquals(10.0, LineageRules.near(10, true, 0.5), 1e-9, "never slower");
		assertTrue(LineageRules.NEAR_GAIN > 1 && LineageRules.NEAR_GAIN <= 1.5, "a help, not a road of its own");
		assertTrue(LineageRules.NEAR >= 16, "fighting side by side, not touching");
	}

	@Test
	void theMastersTrialStopsShortOfTheMastersOwnStage() {
		assertEquals("master", LineageRules.TRIAL);
		assertTrue(LineageRules.trialCounts(AuraRules.FORM, AuraRules.FLOW));
		assertTrue(LineageRules.trialCounts(AuraRules.FORM, AuraRules.EDGE));
		assertFalse(LineageRules.trialCounts(AuraRules.FORM, AuraRules.FORM), "to stand beside your master takes a trial of the world");
		assertTrue(LineageRules.trialCounts(AuraRules.SOVEREIGN, AuraRules.FORM));
		assertFalse(LineageRules.trialCounts(AuraRules.SOVEREIGN, AuraRules.SOVEREIGN));
	}

	@Test
	void theMasterEarnsAShareOfEachRoadWalked() {
		assertEquals((AuraRules.threshold(AuraRules.EDGE) - AuraRules.threshold(AuraRules.FLOW)) * 0.25, LineageRules.share(AuraRules.EDGE, 0.25), 1e-9);
		assertEquals(337.5, LineageRules.share(AuraRules.EDGE, 0.25), 1e-9);
		assertEquals(0, LineageRules.share(AuraRules.GLOW, 0.25), 1e-9, "the first method learned is no road walked");
		assertEquals(0, LineageRules.share(AuraRules.FORM, 0), 1e-9);
		// Three disciples carried all the way up give a master less than their own road from Form.
		double all = 0;
		for (int s = AuraRules.FLOW; s <= AuraRules.FORM; s++) {
			all += 3 * LineageRules.share(s, LineageRules.SHARE);
		}
		assertTrue(all < AuraRules.threshold(AuraRules.SOVEREIGN) - AuraRules.threshold(AuraRules.FORM), "teaching helps, never replaces the road: " + all);
	}

	@Test
	void aDiscipleWhoReachesTheirMasterGraduates() {
		assertFalse(LineageRules.graduates(AuraRules.EDGE, AuraRules.FORM));
		assertTrue(LineageRules.graduates(AuraRules.FORM, AuraRules.FORM));
		assertTrue(LineageRules.graduates(AuraRules.SOVEREIGN, AuraRules.FORM));
	}

	@Test
	void aLessonComesOnceADay() {
		assertTrue(LineageRules.lessonReady(Long.MIN_VALUE, 0));
		assertFalse(LineageRules.lessonReady(1000, 1000 + LineageRules.LESSON_REST - 1));
		assertTrue(LineageRules.lessonReady(1000, 1000 + LineageRules.LESSON_REST));
		assertEquals(SparRules.DAY, LineageRules.LESSON_REST);
	}
}
