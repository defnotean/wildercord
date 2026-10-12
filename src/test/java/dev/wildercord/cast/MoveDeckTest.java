package dev.wildercord.cast;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MoveDeckTest {
	@Test void itNeverMakesTheSameMoveTwiceRunningWhileThereIsAnythingElse() {
		var deck = new MoveDeck(4);
		deck.played(1);
		for (double roll = 0; roll < 1; roll += 0.05) assertEquals(2, deck.pick(new int[]{1, 2}, new double[]{10, 1}, roll));
	}

	@Test void withNothingElseItRepeatsAndWithNothingAtAllItWaits() {
		var deck = new MoveDeck(4);
		deck.played(1);
		assertEquals(1, deck.pick(new int[]{1, 2}, new double[]{3, 0}, 0.5));
		assertEquals(-1, deck.pick(new int[]{1, 2}, new double[]{0, -1}, 0.5));
	}

	@Test void aMoveMadeLatelyCountsForLessAndOneNotMadeForMore() {
		var deck = new MoveDeck(6);
		deck.played(1);
		deck.played(1);
		deck.played(3);
		assertEquals(1 * MoveDeck.REPEAT * MoveDeck.REPEAT, deck.weigh(1, 1), 1e-9);
		assertEquals(MoveDeck.FRESH, deck.weigh(2, 1), 1e-9);
		assertEquals(2, deck.repeats(1));
		assertEquals(3, deck.last());
	}

	@Test void itForgetsWhatFallsOutOfItsMemory() {
		var deck = new MoveDeck(2);
		deck.played(1);
		deck.played(2);
		deck.played(3);
		assertEquals(0, deck.repeats(1));
		deck.clear();
		assertEquals(-1, deck.last());
	}

	@Test void overAFightItMixesItsMovesUp() {
		var deck = new MoveDeck(4);
		var rng = new java.util.Random(7);
		Map<Integer, Integer> made = new HashMap<>();
		int runs = 0, longest = 0, last = -1;
		for (int i = 0; i < 400; i++) {
			int m = deck.pick(new int[]{1, 2, 3, 4}, new double[]{4, 1, 1, 1}, rng.nextDouble());
			made.merge(m, 1, Integer::sum);
			runs = m == last ? runs + 1 : 1;
			longest = Math.max(longest, runs);
			last = m;
			deck.played(m);
		}
		assertEquals(1, longest, "never twice running");
		for (int m = 1; m <= 4; m++) assertTrue(made.getOrDefault(m, 0) > 50, "move " + m + " made only " + made.get(m));
	}
}
