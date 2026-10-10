package dev.wildercord.ritual;

import dev.wildercord.ritual.RitualRules.Ritual;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RitualRulesTest {
	@Test
	void ritualsCycleAndComeBackRound() {
		Ritual ritual = Ritual.BOUNTY;
		for (int i = 0; i < Ritual.values().length; i++) ritual = ritual.next();
		assertEquals(Ritual.BOUNTY, ritual);
		assertEquals(Ritual.DAWN, Ritual.byId("dawn"));
		assertEquals(Ritual.BOUNTY, Ritual.byId("nonsense"));
	}

	@Test
	void greaterRitualsAskMoreOfTheHeart() {
		assertFalse(RitualRules.canLead(Ritual.DAWN, 9));
		assertTrue(RitualRules.canLead(Ritual.DAWN, 10));
		assertTrue(RitualRules.canLead(Ritual.BOUNTY, Ritual.BOUNTY.minCircle));
		for (Ritual ritual : Ritual.values()) {
			if (ritual.ordinal() > 0) assertTrue(ritual.cost > Ritual.values()[ritual.ordinal() - 1].cost);
		}
	}

	@Test
	void theCircleSharesTheCostEvenly() {
		assertArrayEquals(new float[] {40, 40, 40}, RitualRules.split(new float[] {100, 100, 100}, 120), 0.01F);
		assertArrayEquals(new float[] {120}, RitualRules.split(new float[] {200}, 120), 0.01F);
	}

	@Test
	void whoeverIsShortGivesAllAndTheRestCoverIt() {
		float[] paid = RitualRules.split(new float[] {10, 100, 100}, 120);
		assertArrayEquals(new float[] {10, 55, 55}, paid, 0.01F);
		paid = RitualRules.split(new float[] {10, 30, 200}, 120);
		assertArrayEquals(new float[] {10, 30, 80}, paid, 0.01F);
	}

	@Test
	void tooLittleManaWorksNothing() {
		assertNull(RitualRules.split(new float[] {50, 50}, 120));
		assertNull(RitualRules.split(new float[0], 10));
		assertArrayEquals(new float[] {60, 60}, RitualRules.split(new float[] {60, 60}, 120), 0.01F);
	}
}
