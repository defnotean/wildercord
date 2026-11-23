package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.world.OathboundBastionRules.*;
import static org.junit.jupiter.api.Assertions.*;

class OathboundBastionRulesTest {
	@Test
	void anglesFoldCleanlyIntoFrontalAndBehindArcs() {
		assertEquals(-170.0, wrap(190.0), 1e-6);
		assertEquals(170.0, wrap(-190.0), 1e-6);
		assertTrue(frontal(0.0));
		assertTrue(frontal(64.9));
		assertTrue(frontal(-64.9));
		assertTrue(frontal(360.0));
		assertFalse(frontal(65.1));
		assertTrue(behind(180.0));
		assertTrue(behind(-120.0));
		assertFalse(behind(110.0));
	}

	@Test
	void phalanxBraceDeflectsFrontalsAndBreaksToAxes() {
		assertTrue(deflectsProjectile(PHALANX_BRACE, true));
		assertFalse(deflectsProjectile(PHALANX_BRACE, false));
		assertFalse(deflectsProjectile(IDLE_MARCH, true));

		assertTrue(axeBreaksGuard(PHALANX_BRACE, true));
		assertFalse(axeBreaksGuard(PHALANX_BRACE, false));
		assertFalse(axeBreaksGuard(ANVIL_SLAM, true));

		assertEquals(SHIELD_BRACE_MULTIPLIER, damageMultiplier(PHALANX_BRACE, true, false), 1e-6);
		assertEquals(1.0, damageMultiplier(PHALANX_BRACE, false, false), 1e-6);
		assertEquals(STAGGER_MULTIPLIER, damageMultiplier(STAGGERED, true, false), 1e-6);
		assertEquals(ANVIL_RECOVERY_MULTIPLIER, damageMultiplier(ANVIL_SLAM, true, true), 1e-6);
	}

	@Test
	void empoweredPhaseTriggersAtHalfHealthAndQuickensTells() {
		assertFalse(empowered(76.0F, 150.0F));
		assertTrue(empowered(75.0F, 150.0F));
		assertTrue(empowered(20.0F, 150.0F));
		assertFalse(empowered(10.0F, 0.0F));

		// SPEED (0.55) compresses every authored windup, so the floors below are the shipped
		// minimums: the five signature moves keep at least 4 readable ticks, the technique kit
		// (authored as low as 4 ticks for quick combos) keeps the engine's 3-tick floor.
		for (int move : new int[]{PHALANX_BRACE, SHIELD_RAM, ANVIL_SLAM, SHIELD_BASH, MACE_CLEAVE}) {
			int normalTell = tellTicks(move, false);
			int fastTell = tellTicks(move, true);
			assertTrue(normalTell >= 4, "tell readable for " + moveKey(move));
			assertTrue(fastTell < normalTell && fastTell >= 3, "empowered tell quicker yet readable for " + moveKey(move));
			assertTrue(moveDuration(move, false) > normalTell);
		}
		for (int move : COMBAT_MOVES) {
			OathboundTechnique tech = technique(move);
			if (tech == null) {
				continue;
			}
			int normalTell = tellTicks(move, false);
			assertTrue(normalTell >= 3, "technique tell readable for " + moveKey(move));
			assertTrue(moveDuration(move, false) > normalTell);
			assertTrue(tech.damage() > 0.0);
		}
	}

	@Test
	void anvilShockwaveTapersWithDistanceAndScalesWhenEmpowered() {
		assertEquals(ANVIL_DIRECT_DAMAGE, anvilShockwaveDamage(0.0, false), 1e-6);
		assertEquals(ANVIL_DIRECT_DAMAGE, anvilShockwaveDamage(ANVIL_DIRECT_RADIUS, false), 1e-6);
		double mid = anvilShockwaveDamage((ANVIL_DIRECT_RADIUS + ANVIL_SHOCKWAVE_RADIUS) / 2.0, false);
		assertEquals((ANVIL_SHOCKWAVE_MAX + ANVIL_SHOCKWAVE_MIN) / 2.0, mid, 1e-6);
		assertEquals(ANVIL_SHOCKWAVE_MIN, anvilShockwaveDamage(ANVIL_SHOCKWAVE_RADIUS, false), 1e-6);
		assertEquals(0.0, anvilShockwaveDamage(ANVIL_SHOCKWAVE_RADIUS + 0.01, false), 1e-6);

		assertEquals(ANVIL_DIRECT_DAMAGE * EMPOWERED_DAMAGE_MULT, anvilShockwaveDamage(1.0, true), 1e-6);
	}

	@Test
	void championSpawnsRequireDeepHauntAndNoNearbyChampion() {
		assertFalse(championRollEligible(1, 0));
		assertTrue(championRollEligible(2, 0));
		assertTrue(championRollEligible(3, 0));
		assertFalse(championRollEligible(3, 1));
	}

	@Test
	void coneChecksCoverShieldBashAndMaceCleave() {
		assertTrue(inCone(3.0, 0.5, 45.0, BASH_REACH, BASH_HALF_ANGLE_DEG));
		assertFalse(inCone(3.0, 0.5, 80.0, BASH_REACH, BASH_HALF_ANGLE_DEG));
		assertFalse(inCone(BASH_REACH + 0.5, 0.5, 0.0, BASH_REACH, BASH_HALF_ANGLE_DEG));
		assertFalse(inCone(2.0, 3.5, 0.0, BASH_REACH, BASH_HALF_ANGLE_DEG));
	}
}
