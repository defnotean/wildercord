package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The storm and earth audit's rules that need no game: which runes keep the ground-heave overlay, that the texts say
 * what the code does, and the fused numbers the audit tuned.
 */
class StormEarthBalanceTest {
	@Test
	void theRunesWithTheirOwnGroundShowDoNotHeaveItToo() {
		for (RuneDef own : java.util.List.of(MIRE, SINKHOLE, FOSSILIZE, TREMOR, MONOLITH, THUNDERQUAKE, MAGMA, SANDSTORM, BASALT_SURGE, PELT)) {
			assertEquals(WorldRules.Interaction.NONE, WorldRules.of(own), own.id() + " has its own ground show");
		}
		// The plain ones still throw creatures on the ground into a hop.
		assertEquals(WorldRules.Interaction.HEAVE, WorldRules.of(AFTERSHOCK));
		assertEquals(WorldRules.Interaction.HEAVE, WorldRules.of(STALACTITE));
	}

	@Test
	void theTextsSayWhatTheCodeNowDoes() {
		assertTrue(LIGHTNING.description().contains("slows"), "Lightning slows, it does not stun");
		assertTrue(!LIGHTNING.description().contains("stuns"));
		assertTrue(THUNDERCLAP.description().contains("stunned for half a second"));
		assertTrue(RIPPLE.description().contains("doubled against undead"));
		assertTrue(STONESKIN.description().contains("Slowness I"));
		assertTrue(PLASMA.description().contains("ionised"));
		assertTrue(THUNDERBIRD.description().contains("12 seconds"));
	}

	@Test
	void theTunedCostsAreStillTheTiersOwn() {
		// Prices did not move: the audit changed what the runes do for them.
		assertEquals(20, LIGHTNING.cost(), 1e-9);
		assertEquals(22, TEMPEST.cost(), 1e-9);
		assertEquals(20, PLASMA.cost(), 1e-9);
		assertEquals(16, SURGE.cost(), 1e-9);
		assertEquals(12, THUNDERCLAP.cost(), 1e-9);
	}
}
