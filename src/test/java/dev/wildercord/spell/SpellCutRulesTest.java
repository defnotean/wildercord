package dev.wildercord.spell;

import org.junit.jupiter.api.Test;
import java.util.List;
import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

class SpellCutRulesTest {
	private boolean cuts(RuneDef... runes) {
		SpellPlan.Segment root = SpellCompiler.compile(List.of(runes)).root();
		return SpellCutRules.harmful(root.groups.getFirst(), root.link);
	}

	@Test void harmfulBoltsCanBeCutButHelpfulSpellsAndHelpfulLinksCannot() {
		assertTrue(cuts(BOLT, FIRE));
		assertFalse(cuts(BOLT, HEAL));
		assertFalse(cuts(BOLT, HEAL, ON_HIT, HEAL));
		assertTrue(cuts(BOLT, HEAL, ON_HIT, HARM));
	}

	@Test void cutCannotReachThroughItsRangeOrBehindTheBlade() {
		assertTrue(SpellCutRules.approaching(4, .5, .6));
		assertFalse(SpellCutRules.approaching(4.01, 1, 1));
		assertFalse(SpellCutRules.approaching(3, -.1, 1));
		assertFalse(SpellCutRules.approaching(3, 1, 0));
		assertFalse(SpellCutRules.approaching(3, 1, -1));
		assertFalse(SpellCutRules.approaching(3, Double.NaN, 1));
		assertFalse(SpellCutRules.approaching(Double.POSITIVE_INFINITY, 1, 1));
	}
}
