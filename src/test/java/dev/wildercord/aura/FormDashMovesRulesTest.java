package dev.wildercord.aura;

import dev.wildercord.aura.world.MastersRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.aura.FormDashRules.*;

/** The moves pack: parry follow-ups, aerial forms and Spell Cut as field forms, with old numbers and saves untouched. */
class FormDashMovesRulesTest {
	private static final int[] NEW = {AIR_STEP, PLUNGE, RIPOSTE, SHOVE, GUARD_BREAK, SPELL_CUT};

	@Test void newFormsAreKnownWithoutMovingOldNumbers() {
		assertEquals(2, CINDER_LUNGE);
		assertEquals(3, REED_SLIP);
		assertFalse(form(4), "Bit 4 stays unused");
		for (int f : NEW) { assertTrue(form(f)); assertNotEquals(0, KNOWN & bit(f)); }
		assertFalse(form(SPELL_CUT + 1));
		for (int p = IDLE; p <= MISS; p++) assertTrue(phase(p));
		assertFalse(phase(MISS + 1));
	}

	@Test void oneFollowUpPerSchoolAndOnlyMovementFormsTakeTheSlot() {
		assertEquals(MastersRules.EMBER, school(RIPOSTE));
		assertEquals(MastersRules.GALE, school(SHOVE));
		assertEquals(MastersRules.STONE, school(GUARD_BREAK));
		assertEquals(MastersRules.GALE, school(AIR_STEP));
		assertEquals(MastersRules.STONE, school(PLUNGE));
		assertEquals(MastersRules.EMBER, school(SPELL_CUT));
		for (int f : new int[] {CINDER_LUNGE, REED_SLIP, AIR_STEP, PLUNGE}) assertTrue(slot(f));
		for (int f : new int[] {RIPOSTE, SHOVE, GUARD_BREAK, SPELL_CUT, 0, 1, 4}) assertFalse(slot(f));
		for (int f : new int[] {RIPOSTE, SHOVE, GUARD_BREAK}) {
			assertTrue(followUp(f));
			assertEquals(0, rest(f));
			assertTrue(setTicks(f) > 0, "Every follow-up has a tell");
			assertTrue(recovery(f, false) > 0);
		}
		assertTrue(aerial(AIR_STEP) && aerial(PLUNGE) && !aerial(CINDER_LUNGE));
	}

	@Test void stageGates() {
		assertFalse(eligible(PLUNGE, AuraRules.FORM, true));
		assertTrue(eligible(PLUNGE, AuraRules.SOVEREIGN, true));
		for (int f : new int[] {AIR_STEP, RIPOSTE, SHOVE, GUARD_BREAK, SPELL_CUT}) {
			assertTrue(eligible(f, AuraRules.FORM, true));
			assertFalse(eligible(f, AuraRules.FORM, false));
		}
	}

	@Test void everyNewFormPaysAndOwesRecovery() {
		for (int f : NEW) {
			assertTrue(cost(f) > 0);
			assertFalse(canPay(f, cost(f) - .01, 100, 0, 0));
			assertTrue(canPay(f, cost(f), 100, 0, 0));
			assertFalse(canPay(f, cost(f), 100, 0, 101), "Recovery gates it");
			assertTrue(recovery(f, true) <= MAX_RECOVERY && recovery(f, false) <= MAX_RECOVERY && recovery(f, false) <= MAX_TICKS);
			assertTrue(rest(f) <= MAX_REST);
		}
		assertEquals(CINDER_REST, rest(CINDER_LUNGE));
		assertEquals(REED_REST, rest(REED_SLIP));
		assertEquals(STALL_RECOVERY, recovery(CINDER_LUNGE, true));
		assertEquals(REED_RECOVERY, recovery(REED_SLIP, true));
		assertEquals(CINDER_RECOVERY, recovery(CINDER_LUNGE, false));
	}

	@Test void pickFollowsMoveIntentAndFallsBackToALearnedFollowUp() {
		int all = bit(RIPOSTE) | bit(SHOVE) | bit(GUARD_BREAK);
		assertEquals(RIPOSTE, pick(all, 1, 0));
		assertEquals(SHOVE, pick(all, 0, 1));
		assertEquals(SHOVE, pick(all, 0, -1));
		assertEquals(GUARD_BREAK, pick(all, 0, 0));
		assertEquals(GUARD_BREAK, pick(all, -1, 0));
		assertEquals(SHOVE, pick(bit(SHOVE), 1, 0), "One lesson always answers");
		assertEquals(RIPOSTE, pick(bit(RIPOSTE), 0, 0));
		assertEquals(0, pick(bit(CINDER_LUNGE) | bit(AIR_STEP) | bit(SPELL_CUT), 1, 0), "Only follow-ups answer a parry");
		assertEquals(GUARD_BREAK, pick(all, Double.NaN, Double.NaN));
	}

	@Test void groundedEnds() {
		assertEquals(CUT, endPhase(CINDER_LUNGE, false));
		assertEquals(CUT, endPhase(RIPOSTE, false));
		assertEquals(STRIKE, endPhase(SHOVE, false));
		assertEquals(STRIKE, endPhase(GUARD_BREAK, false));
		assertEquals(RECOVER, endPhase(REED_SLIP, false));
		assertEquals(STALL, endPhase(RIPOSTE, true));
		assertEquals(0, stride(SHOVE));
		assertEquals(LUNGE, travelPhase(RIPOSTE));
		assertEquals(LUNGE, travelPhase(CINDER_LUNGE));
		assertEquals(DIVE, travelPhase(PLUNGE));
		assertEquals(IDLE, travelPhase(GUARD_BREAK));
		assertEquals(PLUNGE_TICKS, travelSpan(PLUNGE));
		assertEquals(CINDER_TICKS, travelSpan(CINDER_LUNGE));
		assertEquals(CUT_SCALE, cutScale(CINDER_LUNGE));
		assertTrue(stride(RIPOSTE) <= 2, "Swept steps stay under the two-block limit");
	}

	@Test void aerialStartsAndThreeDistinctEnds() {
		assertTrue(airborne(false, false, false));
		assertFalse(airborne(true, false, false));
		assertFalse(airborne(false, true, false));
		assertFalse(airborne(false, false, true), "Once per time in the air");
		assertEquals(LAND, landing(true, false, 3, AIR_TIMEOUT));
		assertEquals(SPLASH, landing(false, true, 3, AIR_TIMEOUT));
		assertEquals(STALL, landing(false, false, AIR_TIMEOUT, AIR_TIMEOUT));
		assertEquals(IDLE, landing(false, false, 3, AIR_TIMEOUT));
		assertNotEquals(landingRecovery(PLUNGE, LAND), landingRecovery(PLUNGE, SPLASH));
		assertEquals(STALL_RECOVERY, landingRecovery(AIR_STEP, STALL));
		assertEquals(SPLASH_RECOVERY, landingRecovery(AIR_STEP, SPLASH));
		assertEquals(AIR_LAND_RECOVERY, landingRecovery(AIR_STEP, LAND));
		assertEquals(PLUNGE_RECOVERY, landingRecovery(PLUNGE, LAND));
		assertTrue(PLUNGE_STRIDE <= 2 && PLUNGE_MIN_DROP <= 2, "Every dive sweep is one legal swept call");
		assertTrue(PLUNGE_TICKS <= MAX_TICKS && AIR_STEP_TICKS <= MAX_TICKS);
	}

	@Test void everyNewPhaseMovesTheFirstPersonHand() {
		var still = new MastersArtAnimation.Hand(0, 0, 0, 0, 0, 0);
		int[][] cases = {{RIPOSTE, SET}, {RIPOSTE, LUNGE}, {RIPOSTE, CUT}, {SHOVE, SET}, {SHOVE, STRIKE}, {GUARD_BREAK, SET}, {GUARD_BREAK, STRIKE},
			{AIR_STEP, RISE}, {AIR_STEP, LAND}, {AIR_STEP, SPLASH}, {PLUNGE, SET}, {PLUNGE, DIVE}, {PLUNGE, LAND}, {SPELL_CUT, SEVER}, {SPELL_CUT, MISS},
			{CINDER_LUNGE, SET}, {REED_SLIP, SLIP}};
		for (int[] c : cases) {
			var pose = FormDashAnimation.sample(c[0], c[1], 8, 1);
			assertTrue(pose.weight() > 0, "Pose shows: form " + c[0] + " phase " + c[1]);
			assertNotEquals(still, pose.hand(), "Hand moves: form " + c[0] + " phase " + c[1]);
			assertEquals(0, FormDashAnimation.sample(c[0], c[1], 8, 40).weight(), "Pose ends: form " + c[0] + " phase " + c[1]);
		}
	}
}
