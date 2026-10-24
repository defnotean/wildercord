package dev.wildercord.runesmith;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractRulesTest {
	@Test
	void aFreshCreditIsIdleSoItCanBeSwept() {
		assertTrue(new ContractRules.Credit().idle(0));
		assertTrue(new ContractRules.Credit().idle(1_000_000));
	}

	@Test
	void aCreditWithAHitThisTickIsNotIdle() {
		ContractRules.Credit credit = new ContractRules.Credit();
		credit.hit(500);
		assertFalse(credit.idle(500));
		assertTrue(credit.idle(502));
	}
}
