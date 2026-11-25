package dev.wildercord.trials;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NyxaraRulesTest {
	@Test void partyAndApexScalingFollowsContract() {
		assertEquals(420.0, NyxaraRules.maxHealthFor(1), 1.0e-6);
		assertEquals(610.0, NyxaraRules.maxHealthFor(2), 1.0e-6);
		assertEquals(567.0, NyxaraRules.maxHealthFor(1, true), 1.0e-6);
		assertEquals(824.0, NyxaraRules.maxHealthFor(2, true), 1.0e-6);

		assertEquals(2, NyxaraRules.prismCount(1));
		assertEquals(3, NyxaraRules.prismCount(1, true));
		assertEquals(4, NyxaraRules.prismCount(4, true));
		assertEquals(2, NyxaraRules.soakRingCount(1, true));

		assertEquals(17.0F, NyxaraRules.severDamage(true, false), 1.0e-4F);
		assertEquals(20.4F, NyxaraRules.severDamage(true, true), 1.0e-4F);
	}

	@Test void phaseTwoAndRotationFollowContract() {
		assertFalse(NyxaraRules.isPhaseTwo(450.0F, 610.0F));
		assertTrue(NyxaraRules.isPhaseTwo(366.0F, 610.0F));
		assertEquals(NyxaraRules.CRESCENT_SEVER, NyxaraRules.nextAttack(0, false));
		assertEquals(NyxaraRules.PRISM_CONVERGENCE, NyxaraRules.nextAttack(1, false));
		assertEquals(NyxaraRules.TOTAL_ECLIPSE, NyxaraRules.nextAttack(2, true));
		assertEquals(13.5F, NyxaraRules.staggerDamage(10.0F, true), 1.0e-4F);
	}
}
