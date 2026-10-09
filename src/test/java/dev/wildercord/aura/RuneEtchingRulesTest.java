package dev.wildercord.aura;

import dev.wildercord.spell.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuneEtchingRulesTest {
	@Test void ordinaryEffectsArePricedAsTheirTouchSpell() {
		for (RuneDef rune : Runes.all()) {
			boolean expected=rune.family()==RuneFamily.EFFECT && !Runes.innate(rune) && !rune.equals(Runes.EXCISE) && !LessonPackRules.contains(java.util.List.of(rune)) && rune.kind()!=EffectKind.NONE;
			assertEquals(expected,RuneEtchingRules.accepts(rune),rune.id());
			if (expected) assertEquals(Math.max(1,SpellCompiler.compile(java.util.List.of(Runes.TOUCH,rune)).manaCost()),
				RuneEtchingRules.price(rune),rune.id());
		}
	}
	@Test void noShapesLinksModifiersOrInnateTransplants() {
		assertFalse(RuneEtchingRules.accepts(null));
		assertFalse(RuneEtchingRules.accepts(Runes.TOUCH));
		assertFalse(RuneEtchingRules.accepts(Runes.AMPLIFY));
		assertEquals(-1,RuneEtchingRules.price(Runes.TOUCH));
	}
    @Test void heldExciseCannotBecomeAnEtchedTouchSpell() {
        assertFalse(RuneEtchingRules.accepts(Runes.EXCISE));
        assertEquals(-1, RuneEtchingRules.price(Runes.EXCISE));
        assertTrue(SpellCompiler.compile(java.util.List.of(Runes.TOUCH, Runes.EXCISE)).isEmpty());
        assertTrue(RuneEtchingRules.accepts(Runes.HARM));
        assertTrue(RuneEtchingRules.price(Runes.HARM) > 0);
    }
	@Test void deadlineIsInclusiveAndCannotOverflow() {
		assertFalse(RuneEtchingRules.ready(99,100));
		assertTrue(RuneEtchingRules.ready(100,100));
		assertFalse(RuneEtchingRules.ready(-1,0));
		assertFalse(RuneEtchingRules.ready(Long.MAX_VALUE,0));
	}
}
