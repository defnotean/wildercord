package dev.wildercord.town;

import dev.wildercord.town.BountyRules.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InnRaidRulesTest {
	@Test
	void banditsLeaveStrangersAloneAndRestBetweenRaids() {
		assertFalse(InnRaidRules.due(Tier.STRANGER, -1, 10));
		assertTrue(InnRaidRules.due(Tier.KNOWN, -1, 10));
		assertFalse(InnRaidRules.due(Tier.KNOWN, 9, 10));
		assertFalse(InnRaidRules.due(Tier.HONOURED, 10 - InnRaidRules.REST_DAYS + 1, 10));
		assertTrue(InnRaidRules.due(Tier.HONOURED, 10 - InnRaidRules.REST_DAYS, 10));
	}

	@Test
	void raidsGrowWithStanding() {
		assertEquals(2, InnRaidRules.waves(Tier.KNOWN));
		assertEquals(4, InnRaidRules.waves(Tier.HONOURED));
		assertTrue(InnRaidRules.waveSize(Tier.HONOURED, 1) > InnRaidRules.waveSize(Tier.KNOWN, 1));
		assertTrue(InnRaidRules.waveSize(Tier.FRIEND, 3) > InnRaidRules.waveSize(Tier.FRIEND, 1));
		assertTrue(InnRaidRules.lastWave(Tier.KNOWN, 2));
		assertFalse(InnRaidRules.lastWave(Tier.FRIEND, 2));
		assertTrue(InnRaidRules.emeralds(Tier.HONOURED) > InnRaidRules.emeralds(Tier.KNOWN));
		assertTrue(InnRaidRules.reputation(Tier.HONOURED) > InnRaidRules.reputation(Tier.KNOWN));
	}

	@Test
	void aRestartFightsTheReachedWaveAgain() {
		assertEquals(1, InnRaidRules.resumeFrom(Tier.KNOWN, 0));
		assertEquals(2, InnRaidRules.resumeFrom(Tier.FRIEND, 2));
		assertEquals(2, InnRaidRules.resumeFrom(Tier.KNOWN, 7));
	}
}
