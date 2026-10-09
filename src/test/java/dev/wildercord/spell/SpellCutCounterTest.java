package dev.wildercord.spell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The learned Spell Cut extends the existing cut window: a cut there, a paid miss just outside it, nothing when nothing comes. */
class SpellCutCounterTest {
	@Test void cutInsideTheExistingWindow() {
		assertTrue(SpellCutRules.approaching(3, .9, .9));
		assertEquals(SpellCutRules.Timing.CUT, SpellCutRules.counter(3, .9, .9));
	}

	@Test void earlyOrWideIsAMiss() {
		assertEquals(SpellCutRules.Timing.MISS, SpellCutRules.counter(7, .9, .9), "Too early");
		assertEquals(SpellCutRules.Timing.MISS, SpellCutRules.counter(3, .2, .9), "Facing too wide");
		assertEquals(SpellCutRules.Timing.MISS, SpellCutRules.counter(3, .9, .4), "Grazing approach");
	}

	@Test void nothingComingCostsNothing() {
		assertEquals(SpellCutRules.Timing.NONE, SpellCutRules.counter(12, .9, .9));
		assertEquals(SpellCutRules.Timing.NONE, SpellCutRules.counter(3, -.5, .9), "Behind the blade");
		assertEquals(SpellCutRules.Timing.NONE, SpellCutRules.counter(3, .9, -1), "Flying away");
		assertEquals(SpellCutRules.Timing.NONE, SpellCutRules.counter(Double.NaN, .9, .9));
		assertEquals(0, SpellCutRules.cost(SpellCutRules.Timing.NONE));
		assertEquals(0, SpellCutRules.recovery(SpellCutRules.Timing.NONE));
	}

	@Test void aMissCostsLessButRecoversLonger() {
		assertTrue(SpellCutRules.cost(SpellCutRules.Timing.MISS) > 0);
		assertTrue(SpellCutRules.cost(SpellCutRules.Timing.MISS) < SpellCutRules.cost(SpellCutRules.Timing.CUT));
		assertTrue(SpellCutRules.recovery(SpellCutRules.Timing.MISS) > SpellCutRules.recovery(SpellCutRules.Timing.CUT));
		assertEquals(SpellCutRules.Timing.CUT, SpellCutRules.best(SpellCutRules.Timing.MISS, SpellCutRules.Timing.CUT));
		assertEquals(SpellCutRules.Timing.MISS, SpellCutRules.best(SpellCutRules.Timing.NONE, SpellCutRules.Timing.MISS));
		assertEquals(SpellCutRules.Timing.NONE, SpellCutRules.best(SpellCutRules.Timing.NONE, SpellCutRules.Timing.NONE));
	}
}
