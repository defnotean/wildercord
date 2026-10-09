package dev.wildercord.spell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The sub-tick tap: a press and release between two key reads is up at both reads. Reading the held state alone (the old
 * Relay, Excise and Reweave key code) never saw it; the queued click makes it one whole press that the server admits.
 */
class RelayInputTapTest {
	@Test void heldStateAloneLosesASubTickTap() {
		assertEquals(RelayInputRules.NONE, RelayInputRules.edge(false, false, false), "The bug: an up-up read with no click is nothing");
		assertEquals(RelayInputRules.TAP, RelayInputRules.edge(false, false, true), "The fix: the queued click is a press");
	}

	@Test void ordinaryEdgesAreUnchanged() {
		assertEquals(RelayInputRules.DOWN, RelayInputRules.edge(true, false, true));
		assertEquals(RelayInputRules.DOWN, RelayInputRules.edge(true, false, false));
		assertEquals(RelayInputRules.UP, RelayInputRules.edge(false, true, false));
		assertEquals(RelayInputRules.UP, RelayInputRules.edge(false, true, true));
		assertEquals(RelayInputRules.NONE, RelayInputRules.edge(true, true, false), "Held is not a new press");
	}

	@Test void theServerAdmitsAWholeTapInOneTick() {
		var edges = new RelayInputRules.Edges();
		assertTrue(edges.accept(RelayInputRules.DOWN, 1, 10));
		assertTrue(edges.accept(RelayInputRules.UP, 2, 10));
		assertFalse(edges.accept(RelayInputRules.DOWN, 3, 10), "Still one press per tick");
		assertTrue(edges.accept(RelayInputRules.DOWN, 4, 11));
		assertTrue(edges.accept(RelayInputRules.UP, 5, 11));
		assertFalse(edges.accept(RelayInputRules.TAP, 6, 12), "TAP never goes on the wire");
	}
}
