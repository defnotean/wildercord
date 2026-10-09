package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatePruneTest {
	@Test
	void smallMapsAreLeftAlone() {
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i < StatePrune.SOFT_CAP; i++) m.put(i, 0L);
		assertEquals(0, StatePrune.expired(m, 1000));
		assertEquals(0, StatePrune.rested(m, 1000, 10));
		assertEquals(StatePrune.SOFT_CAP, m.size());
	}

	@Test
	void expiredDropsOnlyPastDeadlinesOverTheCap() {
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i < 400; i++) m.put(i, i < 300 ? 50L : 500L);
		m.put(-1, 100L);
		assertEquals(300, StatePrune.expired(m, 100));
		assertEquals(101, m.size());
		assertTrue(m.containsKey(-1), "a deadline equal to now still holds");
	}

	@Test
	void restedDropsRunRestsAndFutureStamps() {
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i < 300; i++) m.put(i, 0L);
		m.put(1000, 950L);
		m.put(1001, 5000L);
		m.put(1002, 900L);
		assertEquals(302, StatePrune.rested(m, 1000, 100));
		assertEquals(Map.of(1000, 950L), m);
	}

	@Test
	void sweepIsBoundedUnderChurn() {
		Map<Integer, Long> m = new HashMap<>();
		for (int t = 0; t < 10_000; t++) {
			m.put(t, (long) t + 20);
			StatePrune.expired(m, t);
		}
		assertTrue(m.size() <= StatePrune.SOFT_CAP + 1, "size " + m.size());
	}
}
