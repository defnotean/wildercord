package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OathboundHalberdierRulesTest {
	@Test
	void overdriveTriggersStrictlyBelowHalfHealth() {
		assertFalse(OathboundHalberdierRules.empowered(125.0F, 125.0F));
		assertFalse(OathboundHalberdierRules.empowered(62.5F, 125.0F));
		assertTrue(OathboundHalberdierRules.empowered(62.0F, 125.0F));
		assertTrue(OathboundHalberdierRules.empowered(1.0F, 125.0F));
	}

	@Test
	void whirlThrustAndHookGeometriesMatchContract() {
		assertTrue(OathboundHalberdierRules.inWhirlRadius(3.2, -3.2));
		assertFalse(OathboundHalberdierRules.inWhirlRadius(4.0, 4.0));

		// Facing north (yaw = 180 -> forward is -Z)
		assertTrue(OathboundHalberdierRules.inThrustLine(180.0F, 0.5, -3.5));
		assertFalse(OathboundHalberdierRules.inThrustLine(180.0F, 2.0, -3.5));
		assertFalse(OathboundHalberdierRules.inThrustLine(180.0F, 0.0, 2.0));

		assertTrue(OathboundHalberdierRules.inHookLine(180.0F, -0.8, -5.0));
		assertFalse(OathboundHalberdierRules.inHookLine(180.0F, 0.0, -6.0));
	}

	@Test
	void thunderCrossCoversFourOrthogonalArmsAndScalesDamage() {
		// Facing south (yaw = 0): close epicenter + N/S/E/W arms, corners excluded
		assertTrue(OathboundHalberdierRules.inThunderCross(0.0F, 0.4, 5.5));
		assertTrue(OathboundHalberdierRules.inThunderCross(0.0F, 5.5, 0.4));
		assertTrue(OathboundHalberdierRules.inThunderCross(0.0F, -5.5, -0.4));
		assertTrue(OathboundHalberdierRules.inThunderCross(0.0F, 1.5, 1.5));
		assertFalse(OathboundHalberdierRules.inThunderCross(0.0F, 4.5, 4.5));

		assertEquals(OathboundHalberdierRules.CRASH_CLOSE_DAMAGE,
			OathboundHalberdierRules.crashDamage(1.8, false), 1.0e-6);
		assertEquals(OathboundHalberdierRules.CRASH_CROSS_DAMAGE,
			OathboundHalberdierRules.crashDamage(4.8, false), 1.0e-6);
		assertTrue(OathboundHalberdierRules.crashDamage(4.8, true)
			> OathboundHalberdierRules.crashDamage(4.8, false));
	}

	@Test
	void axeInterruptsWindupsAndStaggerAmplifiesDamage() {
		int tell = OathboundHalberdierRules.tellTicks(OathboundHalberdierRules.SKY_VAULT_CRASH, false);
		assertTrue(OathboundHalberdierRules.axeInterruptsWindup(OathboundHalberdierRules.SKY_VAULT_CRASH, 5, tell, true));
		assertTrue(OathboundHalberdierRules.axeInterruptsWindup(OathboundHalberdierRules.TEMPEST_WHIRL, 3, 11, true));
		assertFalse(OathboundHalberdierRules.axeInterruptsWindup(OathboundHalberdierRules.SHADOWSTEP_THRUST, 3, 9, true));
		assertFalse(OathboundHalberdierRules.axeInterruptsWindup(OathboundHalberdierRules.SKY_VAULT_CRASH, tell, tell, true));
		assertFalse(OathboundHalberdierRules.axeInterruptsWindup(OathboundHalberdierRules.SKY_VAULT_CRASH, 5, tell, false));

		assertEquals(OathboundHalberdierRules.STAGGER_MULTIPLIER,
			OathboundHalberdierRules.damageMultiplier(OathboundHalberdierRules.STAGGERED, 5, 0), 1.0e-6);
		assertEquals(OathboundHalberdierRules.CRASH_RECOVERY_MULTIPLIER,
			OathboundHalberdierRules.damageMultiplier(OathboundHalberdierRules.SKY_VAULT_CRASH, tell + 4, tell), 1.0e-6);
	}
}
