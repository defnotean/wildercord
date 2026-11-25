package dev.wildercord.trials;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KharvokRulesTest {
	@Test void partyAndApexScalingFollowsContract() {
		assertEquals(380.0, KharvokRules.maxHealthFor(1), 1.0e-6);
		assertEquals(560.0, KharvokRules.maxHealthFor(2), 1.0e-6);
		assertEquals(513.0, KharvokRules.maxHealthFor(1, true), 1.0e-6);
		assertEquals(756.0, KharvokRules.maxHealthFor(2, true), 1.0e-6);

		assertEquals(2, KharvokRules.ventCount(1));
		assertEquals(3, KharvokRules.ventCount(1, true));
		assertEquals(4, KharvokRules.ventCount(4, true));
		assertEquals(2, KharvokRules.soakRingCount(1, true));

		assertEquals(16.0F, KharvokRules.shatterDamage(true, false), 1.0e-4F);
		assertEquals(19.2F, KharvokRules.shatterDamage(true, true), 1.0e-4F);
	}

	@Test void phaseTwoAndRotationFollowContract() {
		assertFalse(KharvokRules.isPhaseTwo(400.0F, 560.0F));
		assertTrue(KharvokRules.isPhaseTwo(336.0F, 560.0F));
		assertEquals(KharvokRules.ANVIL_SHATTER, KharvokRules.nextAttack(0, false));
		assertEquals(KharvokRules.CHAIN_SHACKLE, KharvokRules.nextAttack(1, false));
		assertEquals(KharvokRules.CRUCIBLE_MELTDOWN, KharvokRules.nextAttack(2, true));
		assertEquals(13.5F, KharvokRules.staggerDamage(10.0F, true), 1.0e-4F);
	}
}
