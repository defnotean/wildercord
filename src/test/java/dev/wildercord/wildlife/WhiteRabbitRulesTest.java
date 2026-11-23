package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WhiteRabbitRulesTest {
	@Test
	void outcomeRollCoversAllThreeDestinations() {
		assertEquals(WhiteRabbitRules.Outcome.RANDOM_CAVE, WhiteRabbitRules.rollOutcome(0.0));
		assertEquals(WhiteRabbitRules.Outcome.RANDOM_CAVE, WhiteRabbitRules.rollOutcome(0.399));
		assertEquals(WhiteRabbitRules.Outcome.TREASURE_VAULT, WhiteRabbitRules.rollOutcome(0.40));
		assertEquals(WhiteRabbitRules.Outcome.TREASURE_VAULT, WhiteRabbitRules.rollOutcome(0.749));
		assertEquals(WhiteRabbitRules.Outcome.ALL_BOSSES, WhiteRabbitRules.rollOutcome(0.75));
		assertEquals(WhiteRabbitRules.Outcome.ALL_BOSSES, WhiteRabbitRules.rollOutcome(1.0));
		assertEquals(WhiteRabbitRules.Outcome.RANDOM_CAVE, WhiteRabbitRules.Outcome.fromId("random_cave"));
		assertEquals(WhiteRabbitRules.Outcome.TREASURE_VAULT, WhiteRabbitRules.Outcome.fromId("treasure_vault"));
		assertEquals(WhiteRabbitRules.Outcome.ALL_BOSSES, WhiteRabbitRules.Outcome.fromId("all_bosses"));
	}

	@Test
	void guidingAndDiveThresholdsBehaveCleanly() {
		assertTrue(WhiteRabbitRules.shouldAdvance(6.0, 18.0));
		assertFalse(WhiteRabbitRules.shouldAdvance(14.0, 18.0));
		assertTrue(WhiteRabbitRules.shouldWaitForPlayer(14.0, 18.0));
		assertFalse(WhiteRabbitRules.shouldWaitForPlayer(8.0, 18.0));

		assertTrue(WhiteRabbitRules.shouldDiveIn(6.0, 2.5));
		assertFalse(WhiteRabbitRules.shouldDiveIn(18.0, 2.5));
		assertFalse(WhiteRabbitRules.shouldDiveIn(4.0, 8.0));
	}

	@Test
	void spawnsAndArenaRingMatchSpec() {
		assertTrue(WildlifeRules.WHITE_RABBIT.biomes().contains("minecraft:forest"));
		assertTrue(WildlifeRules.WHITE_RABBIT.biomes().contains("minecraft:dark_forest"));
		assertTrue(WildlifeRules.WHITE_RABBIT.biomes().contains("minecraft:taiga"));
		assertEquals(17, WhiteRabbitRules.ALL_BOSSES_COUNT);

		int[] north = WhiteRabbitRules.bossRingOffset(0, 8, 12.0);
		assertEquals(12, north[0]);
		assertEquals(0, north[1]);
	}
}
