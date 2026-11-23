package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OathboundBannerMarshalRulesTest {
	@Test
	void solarSovereigntyTriggersStrictlyBelowHalfHealth() {
		assertFalse(OathboundBannerMarshalRules.empowered(145.0F, 145.0F));
		assertFalse(OathboundBannerMarshalRules.empowered(72.5F, 145.0F));
		assertTrue(OathboundBannerMarshalRules.empowered(72.0F, 145.0F));
		assertTrue(OathboundBannerMarshalRules.empowered(1.0F, 145.0F));
	}

	@Test
	void solarGuardBlocksFrontLeavesBackExposedAndBreaksToAxes() {
		int tell = OathboundBannerMarshalRules.tellTicks(OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD, false);
		// Facing north (-Z): attacker at z=-2 is frontal, z=+2 is behind
		assertEquals(OathboundBannerMarshalRules.GUARD_MULTIPLIER,
			OathboundBannerMarshalRules.damageMultiplier(
				OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD, tell + 3, tell, false, false, 180.0F, 0.0, -2.0),
			1.0e-6);
		assertEquals(1.0,
			OathboundBannerMarshalRules.damageMultiplier(
				OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD, tell + 3, tell, false, false, 180.0F, 0.0, 2.0),
			1.0e-6);
		assertTrue(OathboundBannerMarshalRules.axeBreaksGuard(
			OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD, tell + 3, tell, false, true, 180.0F, 0.0, -2.0));
		assertFalse(OathboundBannerMarshalRules.axeBreaksGuard(
			OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD, tell + 3, tell, false, false, 180.0F, 0.0, -2.0));
		assertEquals(OathboundBannerMarshalRules.STAGGER_MULTIPLIER,
			OathboundBannerMarshalRules.damageMultiplier(
				OathboundBannerMarshalRules.STAGGERED, 4, 0, false, false, 180.0F, 0.0, -2.0),
			1.0e-6);
	}

	@Test
	void decreeAuraAndJudgmentPillarsScaleWithDistance() {
		assertTrue(OathboundBannerMarshalRules.inDecreeAura(6.0, -8.0));
		assertFalse(OathboundBannerMarshalRules.inDecreeAura(9.0, 9.0));

		assertEquals(OathboundBannerMarshalRules.JUDGMENT_INNER_DAMAGE,
			OathboundBannerMarshalRules.judgmentDamage(2.0, false), 1.0e-6);
		assertEquals(OathboundBannerMarshalRules.JUDGMENT_OUTER_DAMAGE,
			OathboundBannerMarshalRules.judgmentDamage(4.5, false), 1.0e-6);
		assertEquals(0.0, OathboundBannerMarshalRules.judgmentDamage(6.0, true), 1.0e-6);
		assertTrue(OathboundBannerMarshalRules.judgmentDamage(4.5, true)
			> OathboundBannerMarshalRules.judgmentDamage(4.5, false));
	}

	@Test
	void sunbladeArcMatchesReachAndHalfAngle() {
		assertTrue(OathboundBannerMarshalRules.inSunbladeArc(
			0.0F, 1.5, 2.5, OathboundBannerMarshalRules.CLEAVE_REACH, OathboundBannerMarshalRules.CLEAVE_HALF_ANGLE_DEG));
		assertFalse(OathboundBannerMarshalRules.inSunbladeArc(
			0.0F, 0.0, -2.5, OathboundBannerMarshalRules.CLEAVE_REACH, OathboundBannerMarshalRules.CLEAVE_HALF_ANGLE_DEG));
	}
}
