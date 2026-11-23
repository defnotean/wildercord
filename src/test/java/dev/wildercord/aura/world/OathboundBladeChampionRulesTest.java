package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OathboundBladeChampionRulesTest {
	@Test
	void empowermentTriggersStrictlyBelowHalfHealth() {
		assertFalse(OathboundBladeChampionRules.empowered(135.0F, 135.0F));
		assertFalse(OathboundBladeChampionRules.empowered(67.5F, 135.0F));
		assertTrue(OathboundBladeChampionRules.empowered(67.0F, 135.0F));
		assertTrue(OathboundBladeChampionRules.empowered(1.0F, 135.0F));
	}

	@Test
	void parryInterceptsFrontalStrikesAndLeavesRearVulnerable() {
		// Facing south (yaw = 0 -> forward is +Z)
		int tell = OathboundBladeChampionRules.tellTicks(OathboundBladeChampionRules.WRATHFUL_RIPOSTE, false);
		assertTrue(OathboundBladeChampionRules.parriesAttack(
			OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell + 2, tell, false, false, 0.0F, 0.2, 2.5));
		assertFalse(OathboundBladeChampionRules.parriesAttack(
			OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell + 2, tell, false, false, 0.0F, 0.0, -2.5));
		assertFalse(OathboundBladeChampionRules.parriesAttack(
			OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell + 2, tell, true, false, 0.0F, 0.0, 2.5));
		assertFalse(OathboundBladeChampionRules.parriesAttack(
			OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell - 1, tell, false, false, 0.0F, 0.0, 2.5));
	}

	@Test
	void axeSmashBreaksFrontalParryAndStaggerAmplifiesDamage() {
		int tell = OathboundBladeChampionRules.RIPOSTE_TELL;
		assertTrue(OathboundBladeChampionRules.axeBreaksParry(
			OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell + 3, tell, false, true, 180.0F, 0.0, -2.0));
		assertFalse(OathboundBladeChampionRules.axeBreaksParry(
			OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell + 3, tell, false, false, 180.0F, 0.0, -2.0));
		assertFalse(OathboundBladeChampionRules.axeBreaksParry(
			OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell + 3, tell, false, true, 180.0F, 0.0, 2.0));

		assertEquals(OathboundBladeChampionRules.STAGGER_MULTIPLIER,
			OathboundBladeChampionRules.damageMultiplier(
				OathboundBladeChampionRules.STAGGERED, 5, 0, false, false, 0.0F, 0.0, 2.0),
			1.0e-6);
		assertEquals(OathboundBladeChampionRules.PARRY_MULTIPLIER,
			OathboundBladeChampionRules.damageMultiplier(
				OathboundBladeChampionRules.WRATHFUL_RIPOSTE, tell + 4, tell, false, false, 0.0F, 0.0, 2.0),
			1.0e-6);
	}

	@Test
	void guillotineFissureLineAndCloseHitGeometry() {
		// Yaw 180 -> facing north (-Z)
		assertTrue(OathboundBladeChampionRules.inGuillotineLine(180.0F, 0.4, -5.0));
		assertFalse(OathboundBladeChampionRules.inGuillotineLine(180.0F, 2.5, -5.0));
		assertFalse(OathboundBladeChampionRules.inGuillotineLine(180.0F, 0.0, 4.0));
		assertTrue(OathboundBladeChampionRules.inGuillotineLine(180.0F, 0.0, 1.8));

		assertEquals(OathboundBladeChampionRules.GUILLOTINE_CLOSE_DAMAGE,
			OathboundBladeChampionRules.guillotineDamage(1.9, false), 1.0e-6);
		assertEquals(OathboundBladeChampionRules.GUILLOTINE_LINE_DAMAGE,
			OathboundBladeChampionRules.guillotineDamage(4.8, false), 1.0e-6);
		assertTrue(OathboundBladeChampionRules.guillotineDamage(4.8, true)
			> OathboundBladeChampionRules.guillotineDamage(4.8, false));
	}

	@Test
	void crescentCleaveArcAndEmpoweredTimings() {
		// Facing south (+Z)
		assertTrue(OathboundBladeChampionRules.inCleaveArc(
			0.0F, 2.2, 2.2, OathboundBladeChampionRules.CRESCENT_REACH, OathboundBladeChampionRules.CRESCENT_HALF_ANGLE_DEG));
		assertFalse(OathboundBladeChampionRules.inCleaveArc(
			0.0F, 0.0, -2.5, OathboundBladeChampionRules.CRESCENT_REACH, OathboundBladeChampionRules.CRESCENT_HALF_ANGLE_DEG));

		int normalTell = OathboundBladeChampionRules.tellTicks(OathboundBladeChampionRules.GUILLOTINE_SLAM, false);
		int empTell = OathboundBladeChampionRules.tellTicks(OathboundBladeChampionRules.GUILLOTINE_SLAM, true);
		assertTrue(empTell < normalTell && empTell >= 4);
		// moveDuration runs the SPEED-compressed tell plus a compressed recovery, not the raw constants.
		int scaledRecovery = Math.max(3, (int) Math.round(
			OathboundBladeChampionRules.GUILLOTINE_RECOVERY * OathboundBladeChampionRules.SPEED));
		assertEquals(normalTell + scaledRecovery,
			OathboundBladeChampionRules.moveDuration(OathboundBladeChampionRules.GUILLOTINE_SLAM, false),
			"guillotine runs its compressed tell plus recovery");
	}
}
