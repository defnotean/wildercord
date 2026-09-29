package dev.wildercord.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WildercordConfigTest {
	private static final WildercordConfig D = WildercordConfig.DEFAULTS;

	@Test
	void defaultsAreTheNumbersFromBeforeTheConfig() {
		assertEquals(64, D.maxCreatures());
		assertEquals(32, D.maxBlocks());
		assertTrue(D.spellsEditBlocks());
		assertEquals(0.6, D.pvpDamageScale(), 1e-9);
		assertEquals(1.0, D.manaRegenMultiplier(), 1e-9);
		assertEquals(1.0, D.manaCostMultiplier(), 1e-9);
		assertEquals(1.0, D.runeboundChance(), 1e-9);
		assertEquals(1.0, D.runeLootChance(), 1e-9);
		assertEquals(6, D.imbueMaxItems());
		assertEquals(12, D.imbueMaxGlyphs());
		assertTrue(D.worldEvents() && D.duels() && D.wildMagic() && D.worldChangingMagic());
		assertTrue(D.creatureAffinities() && D.elementalClimate());
	}

	@Test
	void affinitiesAndClimateSwitchOffOnTheirOwn() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"features\": {\"creature_affinities\": false}}");
		assertFalse(parsed.config().creatureAffinities());
		assertTrue(parsed.config().elementalClimate());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		WildercordConfig off = WildercordConfig.parse(D.toJson().replace("\"elemental_climate\": true", "\"elemental_climate\": false")).config();
		assertFalse(off.elementalClimate());
		assertTrue(off.creatureAffinities());
	}

	@Test
	void theWrittenFileReadsBackAsTheDefaults() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse(D.toJson());
		assertEquals(D, parsed.config());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
	}

	@Test
	void missingFieldsKeepTheirDefaults() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"casting\": {\"spells_edit_blocks\": false}, \"mana\": {\"cost_multiplier\": 1.5}}");
		assertFalse(parsed.config().spellsEditBlocks());
		assertEquals(1.5, parsed.config().manaCostMultiplier(), 1e-9);
		assertEquals(64, parsed.config().maxCreatures());
		assertEquals(6, parsed.config().imbueMaxItems());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(D, WildercordConfig.parse("{}").config());
	}

	@Test
	void outOfRangeValuesAreClampedWithAWarning() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse(
			"{\"casting\": {\"max_creatures_per_cast\": 100000, \"pvp_damage_scale\": -2}, \"imbuing\": {\"max_items\": 0}}");
		assertEquals(1024, parsed.config().maxCreatures());
		assertEquals(0.0, parsed.config().pvpDamageScale(), 1e-9);
		assertEquals(1, parsed.config().imbueMaxItems());
		assertEquals(3, parsed.warnings().size(), parsed.warnings().toString());
	}

	@Test
	void wrongTypesFallBackToTheDefault() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse(
			"{\"casting\": {\"max_blocks_per_cast\": 2.5, \"spells_edit_blocks\": \"no\"}, \"mana\": {\"regen_multiplier\": \"fast\"}, \"loot\": 7}");
		assertEquals(32, parsed.config().maxBlocks());
		assertTrue(parsed.config().spellsEditBlocks());
		assertEquals(1.0, parsed.config().manaRegenMultiplier(), 1e-9);
		assertEquals(1.0, parsed.config().runeLootChance(), 1e-9);
		assertEquals(4, parsed.warnings().size(), parsed.warnings().toString());
	}

	@Test
	void brokenFilesAndTyposAreReported() {
		WildercordConfig.Parsed broken = WildercordConfig.parse("{ this isn't json");
		assertEquals(D, broken.config());
		assertEquals(1, broken.warnings().size());
		assertEquals(D, WildercordConfig.parse("[1, 2]").config());
		WildercordConfig.Parsed typo = WildercordConfig.parse("{\"casting\": {\"max_creature_per_cast\": 5}, \"cheats\": {}}");
		assertEquals(64, typo.config().maxCreatures());
		assertEquals(2, typo.warnings().size(), typo.warnings().toString());
		assertTrue(typo.warnings().getFirst().contains("max_creature_per_cast"));
	}

	@Test
	void travelDefaults() {
		WildercordConfig.TravelSettings t = D.travel();
		assertTrue(t.enabled());
		assertEquals(3, t.maxHomes());
		assertEquals(3, t.warmupSeconds());
		assertEquals(30, t.cooldownSeconds());
		assertEquals(300, t.rtpCooldownSeconds());
		assertEquals(5000, t.rtpRadius());
		assertEquals(60, t.tpaTimeoutSeconds());
	}

	@Test
	void aFileFromBeforeTravelReadsWithItsDefaults() {
		// Written by 0.4.1, before the travel section existed.
		String old = "{\"version\": 1, \"casting\": {\"max_creatures_per_cast\": 50}, \"features\": {\"duels\": false}}";
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertEquals(WildercordConfig.TravelSettings.DEFAULTS, parsed.config().travel());
		assertEquals(50, parsed.config().maxCreatures());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
	}

	@Test
	void travelSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse(
			"{\"travel\": {\"enabled\": false, \"max_homes\": 5, \"warmup_seconds\": 999, \"rtp_radius\": 2, \"tpa_timeout_seconds\": 30, \"homes\": 1}}");
		WildercordConfig.TravelSettings t = parsed.config().travel();
		assertFalse(t.enabled());
		assertEquals(5, t.maxHomes());
		assertEquals(60, t.warmupSeconds());
		assertEquals(16, t.rtpRadius());
		assertEquals(30, t.tpaTimeoutSeconds());
		assertEquals(30, t.cooldownSeconds());
		// Two out of range, one unknown key.
		assertEquals(3, parsed.warnings().size(), parsed.warnings().toString());
	}

	@Test
	void theWrittenFileKeepsChangedTravelSettings() {
		WildercordConfig changed = WildercordConfig.parse(
			"{\"travel\": {\"enabled\": false, \"max_homes\": 7, \"warmup_seconds\": 5, \"cooldown_seconds\": 10, \"rtp_cooldown_seconds\": 60, \"rtp_radius\": 2000, \"tpa_timeout_seconds\": 90}}").config();
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config());
		assertTrue(D.toJson().contains("\"travel\""), "a fresh file should list the travel settings");
	}

	@Test
	void anOlderFileGainsTheNewerSettingsAndKeepsItsOwn() {
		// Written before the travel section and the affinity switches, with two settings changed and a note of the owner's.
		String old = "{\"mana\": {\"regen_multiplier\": 2.5}, \"features\": {\"duels\": false, \"_note\": \"no duels here\"}}";
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		WildercordConfig.Parsed parsed = WildercordConfig.parse(grown);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(2.5, parsed.config().manaRegenMultiplier(), 1e-9);
		assertFalse(parsed.config().duels());
		assertTrue(grown.contains("\"no duels here\""));
		for (String key : List.of("\"creature_affinities\"", "\"elemental_climate\"", "\"travel\"", "\"max_homes\"", "\"cost_multiplier\"", "\"casting\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		assertEquals(WildercordConfig.TravelSettings.DEFAULTS, parsed.config().travel());
		// Once complete, there's nothing more to add; a broken file is left alone.
		assertTrue(WildercordConfig.addMissing(grown).isEmpty());
		assertTrue(WildercordConfig.addMissing(D.toJson()).isEmpty());
		assertTrue(WildercordConfig.addMissing("{ not json").isEmpty());
		assertTrue(WildercordConfig.addMissing("[1, 2]").isEmpty());
		// Comments are read, but a rewrite would lose them: such a file is left as it is.
		String commented = "{\"mana\": {\"regen_multiplier\": 2.5}, // faster here\n \"features\": {\"duels\": false}}";
		assertEquals(2.5, WildercordConfig.parse(commented).config().manaRegenMultiplier(), 1e-9);
		assertTrue(WildercordConfig.addMissing(commented).isEmpty());
	}

	@Test
	void chancesScaleAndStayWithinAHundred() {
		assertEquals(35, WildercordConfig.scaledChance(35, 1.0));
		assertEquals(70, WildercordConfig.scaledChance(35, 2.0));
		assertEquals(100, WildercordConfig.scaledChance(35, 10.0));
		assertEquals(0, WildercordConfig.scaledChance(35, 0.0));
		assertEquals(18, WildercordConfig.scaledChance(35, 0.5));
	}
}
