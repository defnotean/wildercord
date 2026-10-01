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
	void playerAffinitiesHaveASwitchAndAPace() {
		assertTrue(D.playerAffinity());
		assertEquals(1.0, D.affinityGain(), 1e-9);
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"features\": {\"player_affinity\": false}, \"affinity\": {\"gain_multiplier\": 2.5}}");
		assertFalse(parsed.config().playerAffinity());
		assertEquals(2.5, parsed.config().affinityGain(), 1e-9);
		assertTrue(parsed.config().creatureAffinities());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(100.0, WildercordConfig.parse("{\"affinity\": {\"gain_multiplier\": 1000}}").config().affinityGain(), 1e-9);
		assertTrue(D.toJson().contains("\"player_affinity\": true") && D.toJson().contains("\"gain_multiplier\": 1.0"));
		// A file from before affinities gains both, switched on at the normal pace.
		String grown = WildercordConfig.addMissing("{\"features\": {\"duels\": false}}").orElseThrow();
		assertTrue(grown.contains("\"player_affinity\"") && grown.contains("\"gain_multiplier\""), grown);
		assertTrue(WildercordConfig.parse(grown).config().playerAffinity());
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
	void defenceDefaultsAreTheSpellDefenceNumbers() {
		WildercordConfig.DefenceSettings d = D.defence();
		assertTrue(d.spellguard());
		assertEquals(dev.wildercord.cast.SpellDefenceRules.GUARD_HEALTH, d.spellguardHealth(), 1e-9);
		assertEquals(dev.wildercord.cast.SpellDefenceRules.GUARD_RECHARGE, d.spellguardRechargeSeconds());
		assertEquals(dev.wildercord.cast.SpellDefenceRules.MAX_BONUS, d.maxBonus(), 1e-9);
		assertEquals(dev.wildercord.cast.SpellDefenceRules.ARMOUR_RATE, d.armourRate(), 1e-9);
		assertTrue(D.toJson().contains("\"defence\""), "a fresh file should list the defence settings");
	}

	@Test
	void defenceSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse(
			"{\"defence\": {\"spellguard\": false, \"spellguard_health\": 0.9, \"spellguard_recharge_seconds\": 99999, \"max_bonus\": 0.5, \"armour_rate\": 0.3, \"guard\": 1}}");
		WildercordConfig.DefenceSettings d = parsed.config().defence();
		assertFalse(d.spellguard());
		assertEquals(0.9, d.spellguardHealth(), 1e-9);
		assertEquals(3600, d.spellguardRechargeSeconds());
		// A cap under 1 would turn a weakened hit into a stronger one: 1 is the least.
		assertEquals(1.0, d.maxBonus(), 1e-9);
		assertEquals(0.3, d.armourRate(), 1e-9);
		// Two out of range, one unknown key.
		assertEquals(3, parsed.warnings().size(), parsed.warnings().toString());
		assertEquals(1.0, WildercordConfig.parse("{\"defence\": {\"armour_rate\": 4}}").config().defence().armourRate(), 1e-9);
		assertEquals(0.1, WildercordConfig.parse("{\"defence\": {\"spellguard_health\": 0}}").config().defence().spellguardHealth(), 1e-9);
		// Everything else is untouched.
		assertEquals(WildercordConfig.TravelSettings.DEFAULTS, parsed.config().travel());
		assertEquals(0.6, parsed.config().pvpDamageScale(), 1e-9);
	}

	@Test
	void theWrittenFileKeepsChangedDefenceSettings() {
		WildercordConfig changed = WildercordConfig.parse(
			"{\"defence\": {\"spellguard\": false, \"spellguard_health\": 0.5, \"spellguard_recharge_seconds\": 30, \"max_bonus\": 4, \"armour_rate\": 0.8}}").config();
		assertEquals(new WildercordConfig.DefenceSettings(false, 0.5, 30, 4.0, 0.8), changed.defence());
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config());
	}

	@Test
	void aFileFromBeforeTheDefenceSectionGainsIt() {
		// Written by 0.6.1, before the defence section existed.
		String old = "{\"version\": 1, \"casting\": {\"pvp_damage_scale\": 0.4}, \"features\": {\"duels\": false}}";
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertEquals(WildercordConfig.DefenceSettings.DEFAULTS, parsed.config().defence());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"defence\"", "\"spellguard\"", "\"spellguard_health\"", "\"spellguard_recharge_seconds\"", "\"max_bonus\"", "\"armour_rate\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		WildercordConfig regrown = WildercordConfig.parse(grown).config();
		assertEquals(0.4, regrown.pvpDamageScale(), 1e-9);
		assertEquals(WildercordConfig.DefenceSettings.DEFAULTS, regrown.defence());
		// A defence section missing one setting gets just that one back, and keeps the owner's others.
		String partial = D.toJson().replace("\"max_bonus\": 2.5,", "").replace("\"armour_rate\": 0.55", "\"armour_rate\": 0.2");
		assertFalse(partial.contains("\"max_bonus\""), partial);
		WildercordConfig.DefenceSettings fixed = WildercordConfig.parse(WildercordConfig.addMissing(partial).orElseThrow()).config().defence();
		assertEquals(2.5, fixed.maxBonus(), 1e-9);
		assertEquals(0.2, fixed.armourRate(), 1e-9);
	}

	@Test
	void residuesAndPlacesOfPowerDefaultToTheirDesign() {
		WildercordConfig.ResidueSettings r = D.residues();
		assertTrue(r.enabled());
		assertEquals(dev.wildercord.world.ResidueRules.MIN_SPELL_COST, r.minSpellCost(), 1e-9);
		assertEquals(1.0, r.lifetimeMultiplier(), 1e-9);
		assertEquals(dev.wildercord.world.ResidueRules.PER_CHUNK, r.maxPerChunk());
		assertEquals(dev.wildercord.world.ResidueRules.PER_DIMENSION, r.maxPerDimension());
		WildercordConfig.PowerSettings p = D.power();
		assertTrue(p.leyCrossings() && p.celestial());
		assertEquals(dev.wildercord.spell.ClimateRules.CROSSING_BONUS, p.crossingBonus(), 1e-9);
		assertEquals(1.0, p.celestialMultiplier(), 1e-9);
		assertTrue(D.toJson().contains("\"residues\"") && D.toJson().contains("\"places_of_power\""), "a fresh file lists both sections");
	}

	@Test
	void residueAndPowerSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"residues\": {\"enabled\": false, \"min_spell_cost\": 0, \"lifetime_multiplier\": 50,"
			+ " \"max_per_chunk\": 4, \"max_per_dimension\": -3, \"bogus\": 1},"
			+ " \"places_of_power\": {\"ley_crossings\": false, \"crossing_bonus\": 0.9, \"celestial\": false, \"celestial_multiplier\": 1.5}}");
		WildercordConfig.ResidueSettings r = parsed.config().residues();
		assertFalse(r.enabled());
		assertEquals(1, r.minSpellCost(), 1e-9);
		assertEquals(10, r.lifetimeMultiplier(), 1e-9);
		assertEquals(4, r.maxPerChunk());
		assertEquals(0, r.maxPerDimension());
		WildercordConfig.PowerSettings p = parsed.config().power();
		assertFalse(p.leyCrossings());
		assertFalse(p.celestial());
		assertEquals(0.5, p.crossingBonus(), 1e-9);
		assertEquals(1.5, p.celestialMultiplier(), 1e-9);
		assertEquals(5, parsed.warnings().size(), parsed.warnings().toString());
		assertTrue(parsed.warnings().stream().anyMatch(w -> w.contains("residues.bogus")));
		WildercordConfig changed = parsed.config();
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config(), "the written file keeps every changed setting");
	}

	@Test
	void aFileFromBeforeResiduesGainsBothSections() {
		// Written by 0.7.1, before residues and places of power.
		String old = D.toJson().replaceAll("(?s),\\s*\"residues\": \\{.*$", "\n}\n");
		assertFalse(old.contains("\"residues\"") || old.contains("\"places_of_power\""), old);
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.ResidueSettings.DEFAULTS, parsed.config().residues());
		assertEquals(WildercordConfig.PowerSettings.DEFAULTS, parsed.config().power());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"residues\"", "\"min_spell_cost\"", "\"lifetime_multiplier\"", "\"max_per_chunk\"", "\"max_per_dimension\"",
				"\"places_of_power\"", "\"ley_crossings\"", "\"crossing_bonus\"", "\"celestial\"", "\"celestial_multiplier\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		// A section missing one setting gets just that one back, and keeps the owner's others.
		String partial = D.toJson().replace("\"max_per_chunk\": 12,", "").replace("\"lifetime_multiplier\": 1.0", "\"lifetime_multiplier\": 3.0");
		assertFalse(partial.contains("\"max_per_chunk\""), partial);
		WildercordConfig.ResidueSettings fixed = WildercordConfig.parse(WildercordConfig.addMissing(partial).orElseThrow()).config().residues();
		assertEquals(12, fixed.maxPerChunk());
		assertEquals(3.0, fixed.lifetimeMultiplier(), 1e-9);
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
