package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MastersRulesTest {
	@Test
	void everyAttackHasALearnedTellAndRealRecovery() {
		for (MastersRules.Move move : MastersRules.Move.values()) {
			assertTrue(move.tell >= 5, "a Master's cut is quick but still shows a quarter of a second of windup");
			assertTrue(move.tell > MastersRules.AIM_LOCK);
			assertTrue(move.recovery >= 5, "a real window for a counter or a quick cast");
		}
	}

	@Test
	void partyScalingIsBoundedAndDoesNotMultiplyIncomingDamageByPartySize() {
		assertEquals(480, MastersRules.health(1));
		assertEquals(480, MastersRules.health(-5));
		assertEquals(744, MastersRules.health(2), 1e-6);
		assertEquals(1272, MastersRules.health(4), 1e-6);
		assertEquals(2328, MastersRules.health(8), 1e-6);
		assertEquals(1.0, MastersRules.postureMultiplier(1));
		assertEquals(1.25, MastersRules.postureMultiplier(2), 1e-6);
		assertEquals(1.75, MastersRules.postureMultiplier(4), 1e-6);
		assertEquals(1, MastersRules.volleyAngles(1).size());
		assertEquals(2, MastersRules.volleyAngles(2).size());
		assertEquals(3, MastersRules.volleyAngles(4).size());
		assertEquals(5, MastersRules.volleyAngles(8).size());
		assertEquals(MastersRules.health(8), MastersRules.health(100));
		for (int school = 0; school < 3; school++) {
			for (MastersRules.Move move : MastersRules.Move.values()) {
				assertEquals(1.0, MastersRules.damage(4, school, move) / MastersRules.damage(1, school, move), 1e-6);
			}
		}
	}

	@Test
	void spellbaneErasesEveryBoltClosingInFromAnySide() {
		assertTrue(MastersRules.erasesBolt(0, 1));
		assertTrue(MastersRules.erasesBolt(MastersRules.SPELLBANE_RANGE, MastersRules.SPELLBANE_INCOMING));
		assertTrue(MastersRules.erasesBolt(3, 0.5));
		assertFalse(MastersRules.erasesBolt(MastersRules.SPELLBANE_RANGE + 0.01, 1));
		assertFalse(MastersRules.erasesBolt(2, 0.1));
		assertFalse(MastersRules.erasesBolt(2, -1));
		assertFalse(MastersRules.erasesBolt(-1, 1));
		assertFalse(MastersRules.erasesBolt(Double.NaN, 1));
		assertFalse(MastersRules.erasesBolt(2, Double.NaN));
	}

	@Test
	void aLockedThrustCanBeSidesteppedAndASweepCanBeBackstepped() {
		assertTrue(MastersRules.hits(MastersRules.Move.THRUST, 5, 0, 0));
		assertFalse(MastersRules.hits(MastersRules.Move.THRUST, 5, 1.1, 0));
		assertFalse(MastersRules.hits(MastersRules.Move.THRUST, -1, 0, 0));
		assertTrue(MastersRules.hits(MastersRules.Move.SWEEP, 3, 1, 0));
		assertFalse(MastersRules.hits(MastersRules.Move.SWEEP, 4.1, 0, 0));
		assertFalse(MastersRules.hits(MastersRules.Move.BREAK_CAST, 4.1, 0, 0));
		assertFalse(MastersRules.hits(MastersRules.Move.THRUST, 1, 0, 3));
		assertFalse(MastersRules.hits(MastersRules.Move.THRUST, Double.NaN, 0, 0));
	}

	@Test
	void magicCanInterruptEarlyTellsButNotTheCommittedAimLock() {
		assertTrue(MastersRules.interruptible(true, 100, 120));
		assertTrue(MastersRules.interruptible(true, 115, 120));
		assertFalse(MastersRules.interruptible(true, 116, 120));
		assertFalse(MastersRules.interruptible(true, 120, 120));
		assertFalse(MastersRules.interruptible(false, 100, 120));
	}

	@Test
	void airborneTargetsHaveALinearRangedAnswerWithinTheWholeArena() {
		assertFalse(MastersRules.needsCrescent(4, 0));
		assertTrue(MastersRules.needsCrescent(4, 3));
		assertTrue(MastersRules.needsCrescent(4, -3));
		assertTrue(MastersRules.needsCrescent(20, 0));
		assertTrue(MastersRules.PROJECTILE_RANGE >= MastersRules.ARENA_RADIUS * 2);
	}

	@Test
	void phasesChangePatternsWithoutShorteningTells() {
		assertEquals(0, MastersRules.phase(700, 700));
		assertEquals(1, MastersRules.phase(462, 700));
		assertEquals(2, MastersRules.phase(231, 700));
		assertEquals(0, MastersRules.phase(0, 0));
		assertEquals(MastersRules.Move.CRESCENT, MastersRules.move(0, 1, 2, 10));
		assertNotEquals(MastersRules.move(0, 0, 0, 3), MastersRules.move(0, 0, 1, 3));
		assertNotEquals(MastersRules.move(0, 0, 0, 3), MastersRules.move(1, 0, 0, 3));
		assertTrue(MastersRules.guardAfter(MastersRules.STONE, 1));
		assertFalse(MastersRules.guardAfter(MastersRules.GALE, 1));
		assertFalse(MastersRules.guardAfter(MastersRules.GALE, 2));
		assertTrue(MastersRules.guardAfter(MastersRules.GALE, 3));
	}
	@Test
	void theLastReservedEncounterCanOpenWithoutAdmittingANinth() {
		assertTrue(MastersRules.canAdmitEncounter(7, false));
		assertFalse(MastersRules.canAdmitEncounter(8, false));
		assertTrue(MastersRules.canAdmitEncounter(8, true));
		assertFalse(MastersRules.canAdmitEncounter(9, false));
	}

	@Test
	void aMastersCutGoesThroughPartOfWhatDefencesStop() {
		// Netherite under Protection IV leaves about a sixth of a 30 cut; the blade still lands about half of it.
		assertEquals(30, MastersRules.pierced(30, 30), 1e-5);
		assertEquals(4.75F + 25.25F * (float) MastersRules.PIERCE, MastersRules.pierced(30, 4.75F), 1e-4);
		assertTrue(MastersRules.pierced(30, 0) < 30, "defences still count for something");
		assertEquals(40, MastersRules.pierced(30, 40), 1e-5);
	}
}
