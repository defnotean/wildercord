package dev.wildercord.monster;

import dev.wildercord.monster.MonsterVariantRules.Variant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterVariantRulesTest {
	@Test
	void whereItIsBornDecidesItsVariant() {
		assertEquals(Variant.FROST, MonsterVariantRules.of(-0.5F, true), "snowy taiga");
		assertEquals(Variant.FROST, MonsterVariantRules.of(0.0F, false), "frozen peaks");
		assertEquals(Variant.FROST, MonsterVariantRules.of(0.7F, true), "snowing at height counts");
		assertEquals(Variant.NONE, MonsterVariantRules.of(0.7F, false), "a forest");
		assertEquals(Variant.NONE, MonsterVariantRules.of(0.8F, false), "plains");
		assertEquals(Variant.ASH, MonsterVariantRules.of(2.0F, false), "desert, badlands, the Nether");
	}

	@Test
	void variantsAreDrawnDifferentlyAndRoundTrip() {
		for (Variant variant : Variant.values()) {
			assertEquals(variant, Variant.of(variant.ordinal()));
		}
		assertEquals(Variant.NONE, Variant.of(99));
		assertEquals(0xFFFFFF, Variant.NONE.tint);
		assertTrue(Variant.FROST.tint != Variant.ASH.tint);
	}

	@Test
	void anAlphaCallsFurtherAndHitsHarder() {
		assertTrue(MonsterVariantRules.callRange(true) > MonsterVariantRules.callRange(false));
		assertTrue(MonsterVariantRules.ALPHA_HEALTH > 0 && MonsterVariantRules.ALPHA_DAMAGE > 0);
		assertTrue(MonsterVariantRules.ALPHA_SCALE > 0 && MonsterVariantRules.ALPHA_SCALE <= 0.5, "bigger, but not a giant");
		assertTrue(MonsterVariantRules.ALPHA_CHANCE > 0 && MonsterVariantRules.ALPHA_CHANCE < 1);
	}
}
