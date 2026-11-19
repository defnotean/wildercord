package dev.wildercord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A sentinel tick subtracted raw overflows negative, which reads as "not idle yet" forever. */
class TicksTest {
	@Test
	void freshMarksReadAsLongPast() {
		assertEquals(Long.MAX_VALUE, Ticks.since(Ticks.NEVER, 1000), "never timed reads as long past");
		assertEquals(Long.MAX_VALUE, Ticks.since(Long.MIN_VALUE, 1000), "the raw sentinel reads as long past too");
		assertEquals(Long.MAX_VALUE, Ticks.since(5000, 10), "a mark from before a restart reads as long past, not stuck");
	}

	@Test
	void ordinarySpansSubtract() {
		assertEquals(200, Ticks.since(1000, 1200));
		assertEquals(0, Ticks.since(1200, 1200));
	}
}
