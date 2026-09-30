package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class AnimationSignatureTest {
	@Test
	void everyBuiltInShapeAndEffectHasAStableDistinctAnimationFingerprint() {
		var fingerprints = new HashSet<Long>();
		int castable = 0;
		for (RuneDef rune : Runes.all()) {
			if (rune.family() != RuneFamily.SHAPE && rune.family() != RuneFamily.EFFECT) continue;
			castable++;
			AnimationSignature style = AnimationSignature.of(rune);
			assertEquals(style, AnimationSignature.of(rune), rune.id());
			assertTrue(fingerprints.add(style.fingerprint()), rune.id() + " shares a fingerprint");
			assertTrue(style.motif() >= 0 && style.motif() < 8);
			assertTrue(style.arms() >= 3 && style.arms() <= 6);
			assertTrue(style.beat() >= 2 && style.beat() <= 5);
			if (rune.id().startsWith("wildercord:")) {
				String emblem = "/assets/wildercord/textures/particle/circle/" + rune.path() + "_mark.png";
				assertNotNull(AnimationSignatureTest.class.getResource(emblem), rune.id() + " needs its own illustrated emblem");
			}
		}
		assertTrue(castable > 250, "the complete roster of castable runes is covered");
		assertEquals(castable, fingerprints.size());
	}

	@Test
	void semanticKindsChooseReadableStrokeFamilies() {
		assertTrue(java.util.Set.of(0, 2, 3, 7).contains(AnimationSignature.of(Runes.HEAL).motif()));
		assertTrue(java.util.Set.of(1, 4, 5, 6).contains(AnimationSignature.of(Runes.FIRE).motif()));
	}
}
