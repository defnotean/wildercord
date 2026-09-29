package dev.wildercord.cast.feel;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeelTest {
	private static SpellPlan.Group first(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes)).root().groups.getFirst();
	}

	@Test
	void everyShapeHasAMotionAndTheEightAreAllUsed() {
		java.util.Set<Motion> seen = java.util.EnumSet.noneOf(Motion.class);
		int shapes = 0;
		for (RuneDef rune : Runes.all()) {
			if (rune.family() == RuneFamily.SHAPE) {
				shapes++;
				seen.add(Motion.of(rune.id()));
			}
		}
		assertTrue(shapes >= 39);
		assertEquals(java.util.EnumSet.allOf(Motion.class), seen);
	}

	@Test
	void roleComesFromTheFirstEffect() {
		assertEquals(Role.MEND, Feel.of(first(Runes.BOLT, Runes.HEAL), 12, 0).role());
		assertEquals(Role.STRIKE, Feel.of(first(Runes.BOLT, Runes.FIRE), 12, 0).role());
		assertEquals(Role.STRIKE, Feel.of(first(Runes.BOLT), 12, 0).role());
	}

	@Test
	void elementIsTheManaDominantOneAndTheFirstEffectWinsATie() {
		Feel a = Feel.of(first(Runes.BOLT, Runes.FIRE, Runes.FROST), 20, 0);
		assertEquals("fire", a.element());
		assertEquals("frost", a.accent());
		Feel b = Feel.of(first(Runes.BOLT, Runes.HARM, Runes.EXPLODE), 20, 0);
		assertEquals("fire", b.element(), "Explode costs more than Harm, so fire leads although Harm came first");
		assertEquals("", Feel.of(first(Runes.BOLT), 5, 0).element());
	}

	@Test
	void modifiersAreCounted() {
		Feel f = Feel.of(first(Runes.BOLT, Runes.QUICKEN, Runes.QUICKEN, Runes.FIRE, Runes.AMPLIFY), 20, 0);
		assertEquals(2, f.mod("quicken"));
		assertEquals(1, f.mod("amplify"));
		assertFalse(f.has("widen"));
	}

	@Test
	void bandMIsToday() {
		// A plain 12-mana spell sits inside band M, and the bands rise with cost and charge.
		assertEquals(Band.M, Band.of(Feel.scaleOf(12, 0, 1)));
		assertEquals(Band.S, Band.of(Feel.scaleOf(3, 0, 1)));
		assertTrue(Feel.scaleOf(44, 0, 4) > Feel.scaleOf(12, 0, 1));
		assertTrue(Feel.scaleOf(12, 1, 1) > Feel.scaleOf(12, 0, 1));
		assertEquals(Band.XL, Band.of(Feel.scaleOf(400, 1, 4)));
		assertTrue(Feel.scaleOf(1e9, 1, 9) <= 2.2 && Feel.scaleOf(0, 0, 1) >= 0.6);
	}

	@Test
	void everyRuneOfEveryFamilyGivesAFeelWithoutThrowing() {
		for (RuneDef shape : Runes.all()) {
			if (shape.family() != RuneFamily.SHAPE) {
				continue;
			}
			for (RuneDef effect : Runes.all()) {
				if (effect.family() != RuneFamily.EFFECT) {
					continue;
				}
				SpellPlan.Group g = SpellCompiler.compile(List.of(shape, effect)).root().groups.getFirst();
				Feel f = Feel.of(g, 30, 0.5);
				assertNotNull(f.role());
				assertNotNull(f.motion());
			}
		}
	}

	@Test
	void signaturesAdjustMotionAndScale() {
		Signature.of("wildercord:meteor").motion(Motion.CALL).scale(1.5).register();
		Feel f = Signatures.adjust(Feel.of(first(Runes.BOLT, Runes.METEOR), 24, 0));
		assertEquals(Motion.CALL, f.motion());
		assertTrue(f.scale() > Feel.of(first(Runes.BOLT, Runes.METEOR), 24, 0).scale());
		Signatures.clear();
	}
}
