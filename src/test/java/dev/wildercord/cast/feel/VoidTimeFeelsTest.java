package dev.wildercord.cast.feel;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every void and time effect rune has a signature with an early cue at the hand, from its own element's kit. */
class VoidTimeFeelsTest {
	@Test
	void everyVoidAndTimeEffectHasASignatureWithAnEarlyCue() {
		// Registered directly (idempotent): other tests may have cleared the registry after the game's own start-up.
		VoidFeels.register();
		TimeFeels.register();
		int seen = 0;
		for (RuneDef rune : Runes.all()) {
			if (rune.family() != RuneFamily.EFFECT || !(rune.element().equals("void") || rune.element().equals("time"))) {
				continue;
			}
			seen++;
			Signature s = Signatures.get(rune.id());
			assertNotNull(s, rune.id() + " has no signature");
			Signature.Cue cue = s.soundOf(Phase.CUE);
			assertNotNull(cue, rune.id() + " has no cue");
			assertEquals(rune.element() + "_cue", cue.name(), rune.id() + " cues from its own element's kit");
			assertTrue(cue.pitch() >= 0.5F && cue.pitch() <= 2.0F, rune.id() + " pitch " + cue.pitch());
		}
		assertTrue(seen >= 45, "void and time runes seen: " + seen);
	}
}
