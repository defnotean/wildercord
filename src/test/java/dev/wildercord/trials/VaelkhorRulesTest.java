package dev.wildercord.trials;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VaelkhorRulesTest {
	@Test void partyScalingHealthConduitsAndSoakRings() {
		assertEquals(320.0, VaelkhorRules.maxHealthFor(1), 1.0e-6);
		assertEquals(480.0, VaelkhorRules.maxHealthFor(2), 1.0e-6);
		assertEquals(640.0, VaelkhorRules.maxHealthFor(3), 1.0e-6);
		assertEquals(800.0, VaelkhorRules.maxHealthFor(4), 1.0e-6);

		assertEquals(2, VaelkhorRules.conduitCount(1));
		assertEquals(2, VaelkhorRules.conduitCount(2));
		assertEquals(3, VaelkhorRules.conduitCount(3));
		assertEquals(4, VaelkhorRules.conduitCount(4));

		assertEquals(1, VaelkhorRules.soakRingCount(1));
		assertEquals(2, VaelkhorRules.soakRingCount(2));
		assertEquals(2, VaelkhorRules.soakRingCount(3));
		assertEquals(3, VaelkhorRules.soakRingCount(4));
	}

	@Test void phaseTwoAndRotationFollowContract() {
		assertFalse(VaelkhorRules.isPhaseTwo(350.0F, 480.0F));
		assertTrue(VaelkhorRules.isPhaseTwo(288.0F, 480.0F));

		assertEquals(VaelkhorRules.HALBERD_CLEAVE, VaelkhorRules.nextAttack(0, false));
		assertEquals(VaelkhorRules.BELL_TOLL_CONDUIT, VaelkhorRules.nextAttack(1, false));
		assertEquals(VaelkhorRules.HALBERD_CLEAVE, VaelkhorRules.nextAttack(2, false));
		assertEquals(VaelkhorRules.LEYLINE_CATACLYSM, VaelkhorRules.nextAttack(2, true));
	}

	@Test void cleaveArcAndRingGeometryWorkForSouthAndWestFacings() {
		// aimYaw = 0 faces +Z (South); target at (0, +4) is straight ahead
		assertTrue(VaelkhorRules.inCleaveArc(0.0, 4.0, 0.0F, VaelkhorRules.CLEAVE_REACH, VaelkhorRules.CLEAVE_HALF_ANGLE_RAD));
		assertFalse(VaelkhorRules.inCleaveArc(0.0, -4.0, 0.0F, VaelkhorRules.CLEAVE_REACH, VaelkhorRules.CLEAVE_HALF_ANGLE_RAD));

		// aimYaw = 90 faces -X (West); target at (-4, 0) is straight ahead
		assertTrue(VaelkhorRules.inCleaveArc(-4.0, 0.0, 90.0F, VaelkhorRules.CLEAVE_REACH, VaelkhorRules.CLEAVE_HALF_ANGLE_RAD));
		assertFalse(VaelkhorRules.inCleaveArc(4.0, 0.0, 90.0F, VaelkhorRules.CLEAVE_REACH, VaelkhorRules.CLEAVE_HALF_ANGLE_RAD));

		assertTrue(VaelkhorRules.inRing(2.0, 2.0, VaelkhorRules.SOAK_RADIUS));
		assertFalse(VaelkhorRules.inRing(3.5, 0.0, VaelkhorRules.SOAK_RADIUS));
		assertTrue(VaelkhorRules.withinArena(10.0, 0.0, 10.0));
		assertFalse(VaelkhorRules.withinArena(18.0, 0.0, 18.0));
		assertEquals(13.5F, VaelkhorRules.staggerDamage(10.0F, true), 1.0e-4F);
	}

	@Test void apexModeScalesHealthTargetsDamageAndCooldowns() {
		assertEquals(432.0, VaelkhorRules.maxHealthFor(1, true), 1.0e-6);
		assertEquals(648.0, VaelkhorRules.maxHealthFor(2, true), 1.0e-6);
		assertEquals(3, VaelkhorRules.conduitCount(2, true));
		assertEquals(4, VaelkhorRules.conduitCount(4, true));
		assertEquals(2, VaelkhorRules.soakRingCount(1, true));
		assertEquals(3, VaelkhorRules.soakRingCount(3, true));
		assertEquals(16.8F, VaelkhorRules.cleaveDamage(true, true), 1.0e-4F);
		assertEquals(11L, VaelkhorRules.scaleCooldown(14L, true));
		assertEquals(14L, VaelkhorRules.scaleCooldown(14L, false));
	}
}
