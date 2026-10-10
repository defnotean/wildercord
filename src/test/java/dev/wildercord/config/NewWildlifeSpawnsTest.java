package dev.wildercord.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NewWildlifeSpawnsTest {
	@Test
	void theNewMountsAndPredatorsFollowTheMasterSwitch() {
		WildercordConfig.WildlifeSettings on = WildercordConfig.WildlifeSettings.DEFAULTS;
		for (String id : new String[] {"reefback_turtle", "delver_mole", "frost_lynx", "dune_cougar"}) {
			assertTrue(on.spawns(id), id);
		}
		WildercordConfig.WildlifeSettings off = new WildercordConfig.WildlifeSettings(false, 1.0, true, true, true, true, true, true, true);
		assertFalse(off.spawns("reefback_turtle"));
		assertFalse(on.spawns("not_wildlife"));
	}
}
