package dev.wildercord.aura.world;

import dev.wildercord.aura.TechniqueRules;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MasterVictoryRulesTest {
	@Test void firstClearsAreIdempotentAndTheRecordIsBounded() {
		var none = MasterVictoryRules.Progress.NONE;
		assertFalse(none.cleared(MastersRules.EMBER));
		var once = none.withClear(MastersRules.EMBER);
		assertTrue(once.cleared(MastersRules.EMBER));
		assertEquals(once, once.withClear(MastersRules.EMBER));
		assertFalse(once.cleared(MastersRules.STONE));
		assertEquals(once, once.withClear(99));
		assertFalse(once.cleared(-1));
		assertEquals(7, new MasterVictoryRules.Progress(-1).schools());
		assertEquals(0, new MasterVictoryRules.Progress(8).schools());
	}

	@Test void rewardsAreExistingHorizontalParts() {
		assertEquals(TechniqueRules.ECHO, MasterVictoryRules.reward(MastersRules.EMBER));
		assertEquals(TechniqueRules.AFTERIMAGE, MasterVictoryRules.reward(MastersRules.GALE));
		assertEquals(TechniqueRules.SUNDER, MasterVictoryRules.reward(MastersRules.STONE));
		assertEquals("", MasterVictoryRules.reward(99));
		for (int school = 0; school < 3; school++) assertTrue(TechniqueRules.family(MasterVictoryRules.reward(school)).isPresent());
	}

	@Test void adminInactiveAndUnenrolledKillsNeverCount() {
		assertTrue(MasterVictoryRules.legitimate(true, true, false, true));
		assertFalse(MasterVictoryRules.legitimate(false, true, false, true));
		assertFalse(MasterVictoryRules.legitimate(true, false, false, true));
		assertFalse(MasterVictoryRules.legitimate(true, true, true, true));
		assertFalse(MasterVictoryRules.legitimate(true, true, false, false));
	}

	@Test void cooperativeCreditIncludesSupportButExcludesBystandersAndAbsentPlayers() {
		UUID striker = UUID.randomUUID(), support = UUID.randomUUID(), absent = UUID.randomUUID(), bystander = UUID.randomUUID();
		assertEquals(Set.of(striker, support), MasterVictoryRules.credit(Set.of(striker, support, absent), Set.of(striker, support, bystander)));
	}
}
