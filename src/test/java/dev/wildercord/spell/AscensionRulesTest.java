package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AscensionRulesTest {
	@Test
	void eachAscensionCostsMoreThanTheLast() {
		int top = Circles.condenseNeeded(Circles.MAX);
		assertEquals(top, AscensionRules.needed(0));
		int lastStep = top - Circles.condenseNeeded(Circles.MAX - 1);
		for (int r = 1; r <= AscensionRules.MAX; r++) {
			int step = AscensionRules.needed(r) - AscensionRules.needed(r - 1);
			assertTrue(step >= lastStep, "Ascension " + r + " costs at least as much as the step before it");
			lastStep = step;
		}
		assertEquals(AscensionRules.needed(AscensionRules.MAX), AscensionRules.needed(AscensionRules.MAX + 5), "no rank past the last");
	}

	@Test
	void onlyAWholeTwentyCircleHeartAscends() {
		int enough = AscensionRules.needed(1);
		assertTrue(AscensionRules.ready(0, 20, 20, enough));
		assertFalse(AscensionRules.ready(0, 20, 20, enough - 1));
		assertFalse(AscensionRules.ready(0, 19, 19, enough), "the Twentieth Circle first");
		assertFalse(AscensionRules.ready(0, 20, 18, enough), "a cracked circle holds it back");
		assertFalse(AscensionRules.ready(1, 20, 20, enough), "the next needs more");
		assertFalse(AscensionRules.ready(AscensionRules.MAX, 20, 20, Integer.MAX_VALUE), "ten in all");
	}

	@Test
	void ascensionsAddUpAndFallSilentWhenCracked() {
		assertEquals(CircleVows.Effect.NONE, AscensionRules.effect(0, 20));
		assertEquals(CircleVows.Effect.NONE, AscensionRules.effect(5, 19));
		CircleVows.Effect ten = AscensionRules.effect(10, 20);
		assertEquals(50, ten.mana());
		assertEquals(1.0F, ten.regen(), 1e-5);
		assertEquals(1.10, ten.power(), 1e-9);
		assertEquals(ten, AscensionRules.effect(999, 20), "an edited save can grant no more");
		assertEquals(0, AscensionRules.clamp(-4));
		assertEquals("X", AscensionRules.numeral(10));
	}
}
