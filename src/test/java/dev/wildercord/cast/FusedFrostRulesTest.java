package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Frostbite's cold setting in, Cryostasis's healing spread over the seal, and how long a Rime Seal waits. */
class FusedFrostRulesTest {
	@Test
	void frostbiteBeatsOnceASecondForFiveSeconds() {
		assertEquals(5, FusedFrostRules.frostbiteBeats(1.0));
		assertEquals(10, FusedFrostRules.frostbiteBeats(2.0));
		assertEquals(1, FusedFrostRules.frostbiteBeats(0.01));
	}

	@Test
	void frostbiteSlowsDeeperEachThird() {
		// Five beats: I, II, II, III, III (and I from the bite itself, before the first).
		int[] expected = {0, 1, 1, 2, 2};
		for (int beat = 1; beat <= 5; beat++) {
			assertEquals(expected[beat - 1], FusedFrostRules.frostbiteStage(beat, 5), "beat " + beat);
		}
		// Whatever the length, it only ever deepens, never past III, and ends at III.
		for (int beats = 1; beats <= 20; beats++) {
			int last = 0;
			for (int beat = 1; beat <= beats; beat++) {
				int stage = FusedFrostRules.frostbiteStage(beat, beats);
				assertTrue(stage >= last && stage <= 2, beats + " beats, beat " + beat);
				last = stage;
			}
			assertEquals(2, last, beats + " beats should end at Slowness III");
		}
	}

	@Test
	void cryostasisHealsItsWholeAmountInEqualParts() {
		// Two seconds: four heals of 1.5, six in all.
		assertEquals(4, FusedFrostRules.sealHeals(40));
		assertEquals(1.5F, FusedFrostRules.sealHealEach(6, 4), 1e-6);
		// A very short seal still heals, all at once.
		assertEquals(1, FusedFrostRules.sealHeals(3));
		assertEquals(6F, FusedFrostRules.sealHealEach(6, FusedFrostRules.sealHeals(3)), 1e-6);
		for (int ticks = 1; ticks <= 200; ticks++) {
			int heals = FusedFrostRules.sealHeals(ticks);
			assertEquals(6.0, FusedFrostRules.sealHealEach(6, heals) * heals, 1e-4, ticks + " ticks");
		}
	}

	@Test
	void theSealWaitsASecond() {
		assertFalse(FusedFrostRules.stoodLongEnough(10, 25));
		assertTrue(FusedFrostRules.stoodLongEnough(10, 30));
	}
}
