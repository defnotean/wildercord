package dev.wildercord.spell;

import dev.wildercord.spell.CodexRules.Rank;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexRulesTest {
	@Test
	void killsEarnRanks() {
		assertEquals(Rank.UNSLAIN, CodexRules.rank(0));
		assertEquals(Rank.SLAIN, CodexRules.rank(1));
		assertEquals(Rank.SLAIN, CodexRules.rank(9));
		assertEquals(Rank.STUDIED, CodexRules.rank(10));
		assertEquals(Rank.MASTERED, CodexRules.rank(30));
		assertEquals(1, CodexRules.toNext(0));
		assertEquals(7, CodexRules.toNext(3));
		assertEquals(20, CodexRules.toNext(10));
		assertEquals(0, CodexRules.toNext(45));
		assertTrue(CodexRules.rankedUp(9, 10));
		assertFalse(CodexRules.rankedUp(10, 11));
	}

	@Test
	void onlyFieldGuideCreaturesAreTallied() {
		Map<String, Integer> tally = CodexRules.slay(Map.of(), "wildercord:lumen_stag");
		assertEquals(1, tally.get("wildercord:lumen_stag"));
		assertEquals(2, CodexRules.slay(tally, "wildercord:lumen_stag").get("wildercord:lumen_stag"));
		assertSame(tally, CodexRules.slay(tally, "minecraft:zombie"));
	}

	@Test
	void aTallyStopsAtItsCap() {
		Map<String, Integer> full = Map.of("wildercord:lumen_stag", CodexRules.CAP);
		assertSame(full, CodexRules.slay(full, "wildercord:lumen_stag"));
		Map<String, Integer> many = new HashMap<>();
		for (int i = 0; i < CodexRules.MAX_KINDS; i++) many.put("x:" + i, 1);
		assertSame(many, CodexRules.slay(many, "wildercord:lumen_stag"));
	}
}
