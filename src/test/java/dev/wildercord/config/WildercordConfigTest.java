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
	void spellMasteryHasSwitchesAndAPace() {
		WildercordConfig.MasterySettings m = D.mastery();
		assertTrue(m.enabled() && m.traits() && m.spokenNames() && m.inscription());
		assertEquals(1.0, m.xpMultiplier(), 1e-9);
		WildercordConfig.Parsed parsed = WildercordConfig.parse(
			"{\"mastery\": {\"enabled\": false, \"xp_multiplier\": 2.5, \"traits\": false, \"spoken_names\": false, \"inscription\": false}}");
		assertEquals(new WildercordConfig.MasterySettings(false, 2.5, false, false, false), parsed.config().mastery());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		// Out of range is clamped, with a warning; a typo is reported.
		WildercordConfig.Parsed wild = WildercordConfig.parse("{\"mastery\": {\"xp_multiplier\": 500, \"xp_multiplyer\": 2}}");
		assertEquals(100.0, wild.config().mastery().xpMultiplier(), 1e-9);
		assertEquals(2, wild.warnings().size(), wild.warnings().toString());
		// The written file explains it and reads back the same.
		assertTrue(D.toJson().contains("\"mastery\"") && D.toJson().contains("\"xp_multiplier\": 1.0"));
		WildercordConfig changed = WildercordConfig.parse(D.toJson().replace("\"spoken_names\": true", "\"spoken_names\": false")).config();
		assertFalse(changed.mastery().spokenNames());
		assertTrue(changed.mastery().enabled());
	}

	@Test
	void anOlderFileGainsTheMasterySection() {
		String older = "{\"features\": {\"duels\": false}, \"defence\": {\"max_bonus\": 3.0}}";
		String grown = WildercordConfig.addMissing(older).orElseThrow();
		assertTrue(grown.contains("\"mastery\"") && grown.contains("\"inscription\""), grown);
		WildercordConfig config = WildercordConfig.parse(grown).config();
		assertEquals(WildercordConfig.MasterySettings.DEFAULTS, config.mastery());
		assertFalse(config.duels());
		assertEquals(3.0, config.defence().maxBonus(), 1e-9);
		// Nothing more to add once it's there.
		assertTrue(WildercordConfig.addMissing(grown).isEmpty());
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
	void channelingDefaultsAreTheOverchannelNumbers() {
		WildercordConfig.ChannelingSettings c = D.channeling();
		assertTrue(c.overchannel() && c.sigilTracing());
		assertEquals(dev.wildercord.spell.Overchannel.Tuning.DEFAULTS, c.tuning(), "the file's defaults are the rules' defaults");
		assertEquals(30, c.tuning().stunTicks(), "a second and a half");
		assertTrue(D.toJson().contains("\"channeling\"") && D.toJson().contains("\"sigil_tracing\": true"));
	}

	@Test
	void channelingSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"channeling\": {\"overchannel\": false, \"power_per_stage\": 0.3, "
			+ "\"surge_chance_per_stage\": 2, \"backfire_stun_seconds\": 60, \"backfire_mana_burn\": -1, \"trace_power\": 0.1, \"stages\": 5}}");
		WildercordConfig.ChannelingSettings c = parsed.config().channeling();
		assertFalse(c.overchannel());
		assertEquals(0.3, c.powerPerStage(), 1e-9);
		// Three stages can never be a sure surge, nor a daze longer than two seconds, nor a burn below nothing.
		assertEquals(0.33, c.surgeChancePerStage(), 1e-9);
		assertEquals(2.0, c.backfireStunSeconds(), 1e-9);
		assertEquals(0.0, c.backfireManaBurn(), 1e-9);
		assertEquals(0.1, c.tracePower(), 1e-9);
		assertTrue(c.sigilTracing(), "untouched settings keep their defaults");
		// Three out of range, one unknown key.
		assertEquals(4, parsed.warnings().size(), parsed.warnings().toString());
		assertEquals(0, c.tuning().enabled() ? 1 : 0, "switched off reaches the rules");
		assertEquals(0, dev.wildercord.spell.Overchannel.stagesFor(7, c.tuning().enabled()));
		assertEquals(WildercordConfig.DefenceSettings.DEFAULTS, parsed.config().defence());
		// Changed settings survive being written out and read back.
		assertEquals(parsed.config(), WildercordConfig.parse(parsed.config().toJson()).config());
	}

	@Test
	void aFileFromBeforeTheChannelingSectionGainsIt() {
		// Written by 0.7.1, before casting became a performance.
		String old = "{\"version\": 1, \"features\": {\"wild_magic\": false}, \"defence\": {\"max_bonus\": 3.0}}";
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertEquals(WildercordConfig.ChannelingSettings.DEFAULTS, parsed.config().channeling());
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"channeling\"", "\"overchannel\"", "\"power_per_stage\"", "\"drain_per_second\"", "\"surge_chance_per_stage\"",
				"\"beat_bonus\"", "\"backfire_stun_seconds\"", "\"backfire_mana_burn\"", "\"sigil_tracing\"", "\"trace_power\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		WildercordConfig regrown = WildercordConfig.parse(grown).config();
		assertFalse(regrown.wildMagic(), "the owner's settings stay");
		assertEquals(3.0, regrown.defence().maxBonus(), 1e-9);
		assertEquals(WildercordConfig.ChannelingSettings.DEFAULTS, regrown.channeling());
		// A channeling section missing one setting gets just that one back.
		String partial = D.toJson().replace("\"beat_bonus\": 0.1,", "").replace("\"trace_power\": 0.08", "\"trace_power\": 0.02");
		assertFalse(partial.contains("\"beat_bonus\""), partial);
		WildercordConfig.ChannelingSettings fixed = WildercordConfig.parse(WildercordConfig.addMissing(partial).orElseThrow()).config().channeling();
		assertEquals(0.1, fixed.beatBonus(), 1e-9);
		assertEquals(0.02, fixed.tracePower(), 1e-9);
	}

	@Test
	void resonanceDefaultsAndTheUnreadSwitch() {
		assertTrue(D.unreadRunes());
		WildercordConfig.ResonanceSettings r = D.resonances();
		assertTrue(r.enabled());
		assertEquals(dev.wildercord.spell.ResonanceForge.DEFAULT_COUNT, r.count());
		assertEquals("", r.rerollSalt());
		assertTrue(r.announce());
		assertEquals(dev.wildercord.spell.RuneQuirks.DEFAULT_COUNT, r.quirks());
		assertTrue(D.toJson().contains("\"harmonies\""), "a fresh file should list the harmony settings");
		assertTrue(D.toJson().contains("\"unread_runes\""));
		assertFalse(WildercordConfig.parse("{\"features\": {\"unread_runes\": false}}").config().unreadRunes());
	}

	@Test
	void resonanceSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse(
			"{\"harmonies\": {\"enabled\": false, \"count\": 400, \"reroll_salt\": \"second age\", \"announce\": false, \"quirks\": -2, \"riddles\": 1}}");
		WildercordConfig.ResonanceSettings r = parsed.config().resonances();
		assertFalse(r.enabled());
		assertEquals(dev.wildercord.spell.ResonanceForge.MAX_COUNT, r.count());
		assertEquals("second age", r.rerollSalt());
		assertFalse(r.announce());
		assertEquals(0, r.quirks());
		// Two out of range, one unknown key.
		assertEquals(3, parsed.warnings().size(), parsed.warnings().toString());
		// A salt that isn't text, or is far too long, is reported and fixed.
		WildercordConfig.Parsed number = WildercordConfig.parse("{\"harmonies\": {\"reroll_salt\": 7}}");
		assertEquals("", number.config().resonances().rerollSalt());
		assertEquals(1, number.warnings().size(), number.warnings().toString());
		String longSalt = "s".repeat(200);
		assertEquals(WildercordConfig.ResonanceSettings.MAX_SALT,
			WildercordConfig.parse("{\"harmonies\": {\"reroll_salt\": \"" + longSalt + "\"}}").config().resonances().rerollSalt().length());
		// The written file keeps every changed setting.
		WildercordConfig changed = parsed.config();
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config());
		assertEquals(WildercordConfig.TravelSettings.DEFAULTS, changed.travel());
	}

	@Test
	void aFileFromBeforeResonancesGainsThem() {
		// Written by 0.7.1, before the resonances section and the unread switch existed.
		String old = "{\"version\": 1, \"features\": {\"duels\": false}}";
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.ResonanceSettings.DEFAULTS, parsed.config().resonances());
		assertTrue(parsed.config().unreadRunes());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"harmonies\"", "\"reroll_salt\"", "\"count\"", "\"announce\"", "\"quirks\"", "\"unread_runes\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		WildercordConfig regrown = WildercordConfig.parse(grown).config();
		assertFalse(regrown.duels());
		assertEquals(WildercordConfig.ResonanceSettings.DEFAULTS, regrown.resonances());
		assertTrue(WildercordConfig.addMissing(grown).isEmpty());
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
	void monstersDefaultToSpawningEverywhereTheyLive() {
		WildercordConfig.MonsterSettings m = D.monsters();
		assertEquals(WildercordConfig.MonsterSettings.DEFAULTS, m);
		assertTrue(m.enabled());
		assertEquals(1.0, m.spawnRate(), 1e-9);
		for (String id : List.of("bramblewalker", "gloomstalker", "thunderwing_harpy", "geode_crawler", "bog_witch_frog", "mana_ooze")) {
			assertTrue(m.spawns(id), id + " should spawn by default");
		}
		assertFalse(m.spawns("zombie"), "only the six are asked about");
		assertTrue(D.toJson().contains("\"monsters\""), "a fresh file lists the section");
	}

	@Test
	void monsterSettingsAreReadKeptInRangeAndSwitchEachCreature() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"monsters\": {\"spawn_rate\": 9, \"gloomstalker\": false, \"mana_ooze\": false,"
			+ " \"griffin\": true}}");
		WildercordConfig.MonsterSettings m = parsed.config().monsters();
		assertTrue(m.enabled());
		assertEquals(WildercordConfig.MonsterSettings.MAX_SPAWN_RATE, m.spawnRate(), 1e-9);
		assertFalse(m.spawns("gloomstalker"));
		assertFalse(m.spawns("mana_ooze"));
		assertTrue(m.spawns("bramblewalker") && m.spawns("thunderwing_harpy") && m.spawns("geode_crawler") && m.spawns("bog_witch_frog"));
		assertEquals(2, parsed.warnings().size(), parsed.warnings().toString());
		assertTrue(parsed.warnings().stream().anyMatch(w -> w.contains("monsters.griffin")));
		assertTrue(parsed.warnings().stream().anyMatch(w -> w.contains("monsters.spawn_rate")));
		assertEquals(parsed.config(), WildercordConfig.parse(parsed.config().toJson()).config(), "the written file keeps every changed setting");
		// The master switch, or a rate of nothing, stops them all.
		WildercordConfig.MonsterSettings off = WildercordConfig.parse("{\"monsters\": {\"enabled\": false}}").config().monsters();
		assertFalse(off.spawns("bramblewalker"));
		WildercordConfig.MonsterSettings none = WildercordConfig.parse("{\"monsters\": {\"spawn_rate\": 0}}").config().monsters();
		assertFalse(none.spawns("bog_witch_frog"));
		assertEquals(0.0, WildercordConfig.parse("{\"monsters\": {\"spawn_rate\": -2}}").config().monsters().spawnRate(), 1e-9);
	}

	@Test
	void aFileFromBeforeTheMonstersGainsTheirSection() {
		// Written by 0.8.0, before the monsters of the wilds.
		String old = D.toJson().replaceAll("(?s),\\s*\"monsters\": \\{.*$", "\n}\n");
		assertFalse(old.contains("\"monsters\""), old);
		assertTrue(old.contains("\"places_of_power\""), "only the monsters section is gone");
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.MonsterSettings.DEFAULTS, parsed.config().monsters());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"monsters\"", "\"spawn_rate\"", "\"bramblewalker\"", "\"gloomstalker\"", "\"thunderwing_harpy\"", "\"geode_crawler\"",
				"\"bog_witch_frog\"", "\"mana_ooze\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		// A section missing one setting gets just that one back, and keeps the owner's others.
		String partial = D.toJson().replace("\"geode_crawler\": true,", "").replace("\"spawn_rate\": 1.0", "\"spawn_rate\": 0.5");
		assertFalse(partial.contains("\"geode_crawler\""), partial);
		WildercordConfig.MonsterSettings fixed = WildercordConfig.parse(WildercordConfig.addMissing(partial).orElseThrow()).config().monsters();
		assertTrue(fixed.geodeCrawler());
		assertEquals(0.5, fixed.spawnRate(), 1e-9);
	}

	@Test
	void chancesScaleAndStayWithinAHundred() {
		assertEquals(35, WildercordConfig.scaledChance(35, 1.0));
		assertEquals(70, WildercordConfig.scaledChance(35, 2.0));
		assertEquals(100, WildercordConfig.scaledChance(35, 10.0));
		assertEquals(0, WildercordConfig.scaledChance(35, 0.0));
		assertEquals(18, WildercordConfig.scaledChance(35, 0.5));
	}

	@Test
	void wildlifeSpawnsEverywhereByDefault() {
		WildercordConfig.WildlifeSettings w = D.wildlife();
		assertEquals(WildercordConfig.WildlifeSettings.DEFAULTS, w);
		assertTrue(w.enabled());
		assertEquals(1.0, w.spawnMultiplier(), 1e-9);
		for (String creature : List.of("glimmerwing", "lumen_stag", "mossback_tortoise", "cinderfox", "skyray", "rimehare")) {
			assertTrue(w.spawns(creature), creature + " should spawn by default");
		}
		assertFalse(w.spawns("zombie"), "only wildlife has a switch here");
		assertTrue(D.toJson().contains("\"creatures\"") && D.toJson().contains("\"wildlife_spawn_multiplier\""), "a fresh file lists the section");
	}

	@Test
	void wildlifeSwitchesAndMultiplierAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"creatures\": {\"wildlife\": true, \"wildlife_spawn_multiplier\": 40,"
			+ " \"lumen_stag\": false, \"skyray\": false, \"bogus\": 1}}");
		WildercordConfig.WildlifeSettings w = parsed.config().wildlife();
		assertEquals(10.0, w.spawnMultiplier(), 1e-9, "clamped to 10");
		assertFalse(w.spawns("lumen_stag"));
		assertFalse(w.spawns("skyray"));
		assertTrue(w.spawns("glimmerwing") && w.spawns("rimehare") && w.spawns("cinderfox") && w.spawns("mossback_tortoise"));
		assertTrue(parsed.warnings().stream().anyMatch(x -> x.contains("creatures.wildlife_spawn_multiplier")), parsed.warnings().toString());
		assertTrue(parsed.warnings().stream().anyMatch(x -> x.contains("creatures.bogus")), parsed.warnings().toString());
		// The master switch, and a multiplier of nothing, stop every creature.
		WildercordConfig.WildlifeSettings off = WildercordConfig.parse("{\"creatures\": {\"wildlife\": false}}").config().wildlife();
		assertFalse(off.spawns("glimmerwing") || off.spawns("rimehare"));
		WildercordConfig.WildlifeSettings none = WildercordConfig.parse("{\"creatures\": {\"wildlife_spawn_multiplier\": 0}}").config().wildlife();
		assertFalse(none.spawns("cinderfox"));
		WildercordConfig.Parsed wrong = WildercordConfig.parse("{\"creatures\": {\"rimehare\": \"no\"}}");
		assertTrue(wrong.config().wildlife().rimehare(), "a wrong type keeps the default");
		assertFalse(wrong.warnings().isEmpty());
	}

	@Test
	void theWrittenFileKeepsChangedWildlifeSettings() {
		WildercordConfig changed = WildercordConfig.parse("{\"creatures\": {\"wildlife_spawn_multiplier\": 0.25, \"cinderfox\": false}}").config();
		WildercordConfig again = WildercordConfig.parse(changed.toJson()).config();
		assertEquals(0.25, again.wildlife().spawnMultiplier(), 1e-9);
		assertFalse(again.wildlife().cinderfox());
		assertEquals(changed, again);
	}

	@Test
	void aFileFromBeforeWildlifeGainsTheCreaturesSection() {
		// Written by 0.8.0, before wildlife.
		String old = D.toJson().replaceAll("(?s),\\s*\"creatures\": \\{.*$", "\n}\n");
		assertFalse(old.contains("\"creatures\""), old);
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.WildlifeSettings.DEFAULTS, parsed.config().wildlife());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"creatures\"", "\"wildlife\"", "\"wildlife_spawn_multiplier\"", "\"glimmerwing\"", "\"lumen_stag\"",
				"\"mossback_tortoise\"", "\"cinderfox\"", "\"skyray\"", "\"rimehare\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		// A creatures section missing one switch gets just that one back, keeping the owner's others.
		String partial = D.toJson().replace("\"skyray\": true,", "").replace("\"wildlife_spawn_multiplier\": 1.0", "\"wildlife_spawn_multiplier\": 0.5");
		assertFalse(partial.contains("\"skyray\""), partial);
		WildercordConfig.WildlifeSettings fixed = WildercordConfig.parse(WildercordConfig.addMissing(partial).orElseThrow()).config().wildlife();
		assertTrue(fixed.skyray());
		assertEquals(0.5, fixed.spawnMultiplier(), 1e-9);
	}

	@Test
	void auraDefaultsAreTheRulesNumbers() {
		WildercordConfig.AuraSettings a = D.aura();
		assertTrue(a.enabled());
		assertEquals(1.0, a.xpMultiplier(), 1e-9);
		assertEquals(1.0, a.gainMultiplier(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.COAT_BONUS, a.coatBonus(), 1e-9);
		assertEquals(1.0, a.damageScale(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.SLASH_FACTOR, a.slashDamage(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.SLASH_COST, a.slashCost(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.SLASH_COOLDOWN, a.slashCooldownTicks());
		assertEquals(0.6, a.pvpScale(), 1e-9, "aura against players starts where spells do");
		assertEquals(dev.wildercord.aura.AuraRules.BACKLASH_TICKS, a.backlashTicks());
		assertEquals(dev.wildercord.aura.AuraRules.GUARD_SHARE, a.guardShare(), 1e-9);
		assertTrue(D.toJson().contains("\"aura\"") && D.toJson().contains("\"slash_cooldown_seconds\""), "a fresh file lists the aura section");
	}

	@Test
	void theTopStagesDefaultsAreTheRulesNumbers() {
		WildercordConfig.AuraHeights h = D.aura().heights();
		assertEquals(dev.wildercord.aura.AuraRules.STEP_COST, h.stepCost(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.STEP_COOLDOWN, h.stepCooldownTicks());
		assertEquals(dev.wildercord.aura.AuraRules.STEP_DISTANCE, h.stepDistance(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.ARMOUR_SHARE, h.armourShare(), 1e-9);
		assertTrue(h.intentPvp());
		assertEquals(dev.wildercord.aura.AuraRules.INTENT_PVP_SLOW, h.intentPvpSlow(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.DOMINION_COST, h.dominionCost(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.DOMINION_TICKS, h.dominionTicks());
		assertEquals(dev.wildercord.aura.AuraRules.DOMINION_COOLDOWN, h.dominionCooldownTicks());
		assertEquals(dev.wildercord.aura.AuraRules.DOMINION_WEAKEN, h.dominionWeaken(), 1e-9);
		assertEquals(dev.wildercord.aura.AuraRules.SPELLBLADE_TICKS, h.spellbladeTicks());
		assertEquals(1.0, h.markChanceMultiplier(), 1e-9);
		for (String key : List.of("step_cost", "step_cooldown_seconds", "step_distance", "armour_share", "intent_pvp", "intent_pvp_slow", "dominion_cost",
				"dominion_seconds", "dominion_cooldown_seconds", "dominion_weaken", "spellblade_seconds", "mark_chance_multiplier")) {
			assertTrue(D.toJson().contains("\"" + key + "\""), "a fresh file lists " + key);
		}
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, new WildercordConfig.AuraSettings(true, 1.0, 1.0, dev.wildercord.aura.AuraRules.COAT_BONUS,
			1.0, dev.wildercord.aura.AuraRules.SLASH_FACTOR, dev.wildercord.aura.AuraRules.SLASH_COST, dev.wildercord.aura.AuraRules.SLASH_COOLDOWN / 20.0,
			0.6, dev.wildercord.aura.AuraRules.BACKLASH_TICKS / 20.0, dev.wildercord.aura.AuraRules.GUARD_SHARE), "the older constructor takes the top stages' defaults");
	}

	@Test
	void theTopStagesSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura\": {\"step_cost\": 20, \"step_cooldown_seconds\": 0.5, \"step_distance\": 40,"
			+ " \"armour_share\": 0.9, \"intent_pvp\": false, \"intent_pvp_slow\": 0.1, \"dominion_cost\": 60, \"dominion_seconds\": 0,"
			+ " \"dominion_cooldown_seconds\": 30, \"dominion_weaken\": 0.5, \"spellblade_seconds\": 8, \"mark_chance_multiplier\": -2}}");
		WildercordConfig.AuraHeights h = parsed.config().aura().heights();
		assertEquals(20.0, h.stepCost(), 1e-9);
		assertEquals(10, h.stepCooldownTicks());
		assertEquals(12.0, h.stepDistance(), 1e-9, "a step never carries further than twelve blocks");
		assertEquals(0.75, h.armourShare(), 1e-9, "aura armour never takes everything");
		assertFalse(h.intentPvp());
		assertEquals(0.1, h.intentPvpSlow(), 1e-9);
		assertEquals(60.0, h.dominionCost(), 1e-9);
		assertEquals(20, h.dominionTicks(), "a Dominion lasts a second at least");
		assertEquals(600, h.dominionCooldownTicks());
		assertEquals(0.5, h.dominionWeaken(), 1e-9);
		assertEquals(160, h.spellbladeTicks());
		assertEquals(0.0, h.markChanceMultiplier(), 1e-9, "clamped to never, not below");
		assertEquals(4, parsed.warnings().size(), parsed.warnings().toString());
		WildercordConfig changed = parsed.config();
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config(), "the written file keeps every changed setting");
	}

	@Test
	void aFileFromBeforeTheTopStagesGainsTheirKeys() {
		// An aura section written before Form and Sovereign: the top stages' keys are missing.
		String old = D.toJson();
		for (String key : List.of("step_cost", "step_cooldown_seconds", "step_distance", "armour_share", "intent_pvp", "intent_pvp_slow", "dominion_cost",
				"dominion_seconds", "dominion_cooldown_seconds", "dominion_weaken", "spellblade_seconds", "mark_chance_multiplier")) {
			old = old.replaceAll(",\\s*\"" + key + "\": [^,\\n}]+", "");
		}
		assertFalse(old.contains("\"step_cost\"") || old.contains("\"mark_chance_multiplier\""), old);
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraHeights.DEFAULTS, parsed.config().aura().heights());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		assertTrue(grown.contains("\"step_cost\"") && grown.contains("\"dominion_cooldown_seconds\"") && grown.contains("\"mark_chance_multiplier\""), grown);
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		// The owner's own aura settings are kept as they were.
		String theirs = old.replace("\"guard_share\": 0.5", "\"guard_share\": 0.3");
		assertEquals(0.3, WildercordConfig.parse(WildercordConfig.addMissing(theirs).orElseThrow()).config().aura().guardShare(), 1e-9);
	}

	@Test
	void auraSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura\": {\"enabled\": false, \"xp_multiplier\": 3, \"gain_multiplier\": -1,"
			+ " \"coat_bonus\": 0.25, \"damage_scale\": 40, \"slash_damage\": 2, \"slash_cost\": 30, \"slash_cooldown_seconds\": 0.5,"
			+ " \"pvp_scale\": 0.3, \"backlash_seconds\": 100, \"guard_share\": 0.75, \"slashh_cost\": 1}}");
		WildercordConfig.AuraSettings a = parsed.config().aura();
		assertFalse(a.enabled());
		assertEquals(3.0, a.xpMultiplier(), 1e-9);
		assertEquals(0.0, a.gainMultiplier(), 1e-9, "clamped to nothing, never negative");
		assertEquals(0.25, a.coatBonus(), 1e-9);
		assertEquals(10.0, a.damageScale(), 1e-9);
		assertEquals(2.0, a.slashDamage(), 1e-9);
		assertEquals(30.0, a.slashCost(), 1e-9);
		assertEquals(10, a.slashCooldownTicks());
		assertEquals(0.3, a.pvpScale(), 1e-9);
		assertEquals(30.0, a.backlashSeconds(), 1e-9);
		assertEquals(0.75, a.guardShare(), 1e-9);
		assertEquals(4, parsed.warnings().size(), parsed.warnings().toString());
		assertTrue(parsed.warnings().stream().anyMatch(w -> w.contains("aura.slashh_cost")), "a typo is reported");
		WildercordConfig changed = parsed.config();
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config(), "the written file keeps every changed setting");
	}

	@Test
	void aFileFromBeforeAuraGainsTheSection() {
		// Written by 0.8.0, before aura.
		String old = D.toJson().replaceAll("(?s),\\s*\"aura\": \\{.*$", "\n}\n");
		assertFalse(old.contains("\"aura\""), old);
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, parsed.config().aura());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"aura\"", "\"xp_multiplier\"", "\"gain_multiplier\"", "\"coat_bonus\"", "\"damage_scale\"", "\"slash_damage\"",
				"\"slash_cost\"", "\"slash_cooldown_seconds\"", "\"pvp_scale\"", "\"backlash_seconds\"", "\"guard_share\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		// A section missing one setting gets just that one back, and keeps the owner's others.
		String partial = D.toJson().replace("\"guard_share\": 0.5", "\"guard_share\": 0.4").replace("\"pvp_scale\": 0.6,", "");
		WildercordConfig.AuraSettings fixed = WildercordConfig.parse(WildercordConfig.addMissing(partial).orElseThrow()).config().aura();
		assertEquals(0.6, fixed.pvpScale(), 1e-9);
		assertEquals(0.4, fixed.guardShare(), 1e-9);
	}

	@Test
	void swordStringsDefaultsAreTheRulesNumbers() {
		WildercordConfig.AuraStrings s = D.aura().strings();
		assertTrue(s.enabled());
		assertEquals(dev.wildercord.aura.StringRules.WINDOW, s.windowTicks());
		assertEquals(dev.wildercord.aura.StringRules.WINDOW / 20.0, s.windowSeconds(), 1e-9);
		for (String key : List.of("strings", "string_window_seconds")) {
			assertTrue(D.toJson().contains("\"" + key + "\""), "a fresh file lists " + key);
		}
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, new WildercordConfig.AuraSettings(true, 1.0, 1.0, dev.wildercord.aura.AuraRules.COAT_BONUS,
			1.0, dev.wildercord.aura.AuraRules.SLASH_FACTOR, dev.wildercord.aura.AuraRules.SLASH_COST, dev.wildercord.aura.AuraRules.SLASH_COOLDOWN / 20.0,
			0.6, dev.wildercord.aura.AuraRules.BACKLASH_TICKS / 20.0, dev.wildercord.aura.AuraRules.GUARD_SHARE, WildercordConfig.AuraHeights.DEFAULTS),
			"the constructor from before sword strings takes their defaults");
		assertEquals(WildercordConfig.AuraStrings.DEFAULTS, new WildercordConfig.AuraSettings(true, 1.0, 1.0, 0.1, 1.0, 1.2, 12, 2, 0.6, 3, 0.5,
			null, null).strings(), "a missing part reads as its defaults");
	}

	@Test
	void swordStringsSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura\": {\"strings\": false, \"string_window_seconds\": 5}}");
		WildercordConfig.AuraStrings s = parsed.config().aura().strings();
		assertFalse(s.enabled());
		assertEquals(dev.wildercord.aura.StringRules.MAX_WINDOW_SECONDS, s.windowSeconds(), 1e-9, "held to two seconds");
		assertEquals(40, s.windowTicks());
		assertEquals(1, parsed.warnings().size(), parsed.warnings().toString());
		WildercordConfig.AuraStrings quick = WildercordConfig.parse("{\"aura\": {\"string_window_seconds\": 0}}").config().aura().strings();
		assertEquals(dev.wildercord.aura.StringRules.MIN_WINDOW_SECONDS, quick.windowSeconds(), 1e-9, "a tenth of a second at least");
		assertEquals(2, quick.windowTicks());
		assertEquals(14, WildercordConfig.parse("{\"aura\": {\"string_window_seconds\": 0.7}}").config().aura().strings().windowTicks());
		WildercordConfig changed = parsed.config();
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config(), "the written file keeps every changed setting");
	}

	@Test
	void aFileFromBeforeSwordStringsGainsTheirKeys() {
		// An aura section written by 0.9.0, before sword strings.
		String old = D.toJson();
		for (String key : List.of("strings", "string_window_seconds")) {
			old = old.replaceAll(",\\s*\"" + key + "\": [^,\\n}]+", "");
		}
		assertFalse(old.contains("\"strings\"") || old.contains("\"string_window_seconds\""), old);
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraStrings.DEFAULTS, parsed.config().aura().strings());
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, parsed.config().aura(), "everything else as it was");
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		assertTrue(grown.contains("\"strings\"") && grown.contains("\"string_window_seconds\""), grown);
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		// The owner's own aura settings are kept as they were.
		String theirs = old.replace("\"guard_share\": 0.5", "\"guard_share\": 0.3");
		WildercordConfig.AuraSettings kept = WildercordConfig.parse(WildercordConfig.addMissing(theirs).orElseThrow()).config().aura();
		assertEquals(0.3, kept.guardShare(), 1e-9);
		assertEquals(WildercordConfig.AuraStrings.DEFAULTS, kept.strings());
	}

	@Test
	void theMethodsArtsSettingsHaveDefaultsAndRanges() {
		WildercordConfig.AuraStrings s = D.aura().strings();
		assertEquals(1.0, s.artDamage(), 1e-9);
		assertTrue(s.artTerrain());
		for (String key : List.of("art_damage", "art_terrain")) {
			assertTrue(D.toJson().contains("\"" + key + "\""), "a fresh file lists " + key);
		}
		assertEquals(WildercordConfig.AuraStrings.DEFAULTS, new WildercordConfig.AuraStrings(true, dev.wildercord.aura.StringRules.WINDOW / 20.0),
			"the constructor from before the arts takes their defaults");
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura\": {\"art_damage\": 9, \"art_terrain\": false}}");
		WildercordConfig.AuraStrings read = parsed.config().aura().strings();
		assertEquals(5.0, read.artDamage(), 1e-9, "held to five times");
		assertFalse(read.artTerrain());
		assertEquals(1, parsed.warnings().size(), parsed.warnings().toString());
		assertEquals(0.0, WildercordConfig.parse("{\"aura\": {\"art_damage\": -1}}").config().aura().strings().artDamage(), 1e-9);
		assertEquals(parsed.config(), WildercordConfig.parse(parsed.config().toJson()).config(), "the written file keeps them");
	}

	@Test
	void aFileFromBeforeTheArtsGainsTheirKeys() {
		// An aura section written with sword strings but before the methods' arts.
		String old = D.toJson();
		for (String key : List.of("art_damage", "art_terrain")) {
			old = old.replaceAll(",\\s*\"" + key + "\": [^,\\n}]+", "");
		}
		assertFalse(old.contains("\"art_damage\"") || old.contains("\"art_terrain\""), old);
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, parsed.config().aura(), "the arts' settings read as their defaults");
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		assertTrue(grown.contains("\"art_damage\"") && grown.contains("\"art_terrain\""), grown);
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
	}

	@Test
	void momentumAndStanceSettingsHaveDefaultsAndRanges() {
		WildercordConfig.AuraMomentum m = D.aura().momentum();
		assertEquals(WildercordConfig.AuraMomentum.DEFAULTS, m);
		assertTrue(m.momentum() && m.stance() && m.pvpStance());
		assertEquals(1.0, m.momentumGain(), 1e-9);
		assertEquals(1.0, m.momentumEbb(), 1e-9);
		assertEquals(1.0, m.stanceDamage(), 1e-9);
		assertEquals(1.0, m.finisherDamage(), 1e-9);
		for (String key : List.of("momentum", "momentum_gain", "momentum_ebb", "stance", "stance_damage", "finisher_damage", "pvp_stance")) {
			assertTrue(D.toJson().contains("\"" + key + "\""), "a fresh file lists " + key);
		}
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura\": {\"momentum\": false, \"momentum_gain\": 9, \"momentum_ebb\": -2, "
			+ "\"stance\": false, \"stance_damage\": 0.5, \"finisher_damage\": 7, \"pvp_stance\": false}}");
		WildercordConfig.AuraMomentum read = parsed.config().aura().momentum();
		assertFalse(read.momentum());
		assertFalse(read.stance());
		assertFalse(read.pvpStance());
		assertEquals(5.0, read.momentumGain(), 1e-9, "held to five times");
		assertEquals(0.0, read.momentumEbb(), 1e-9, "never below nothing (no ebb at all)");
		assertEquals(0.5, read.stanceDamage(), 1e-9);
		assertEquals(3.0, read.finisherDamage(), 1e-9, "held to three times");
		assertEquals(3, parsed.warnings().size(), parsed.warnings().toString());
		assertEquals(parsed.config(), WildercordConfig.parse(parsed.config().toJson()).config(), "the written file keeps them");
		// The constructor from before momentum (and a missing part) take its defaults.
		WildercordConfig.AuraSettings before = new WildercordConfig.AuraSettings(true, 1.0, 1.0, 0.1, 1.0, 1.2, 12, 2, 0.6, 3, 0.5,
			WildercordConfig.AuraHeights.DEFAULTS, WildercordConfig.AuraStrings.DEFAULTS);
		assertEquals(WildercordConfig.AuraMomentum.DEFAULTS, before.momentum());
		assertEquals(WildercordConfig.AuraMomentum.DEFAULTS, new WildercordConfig.AuraSettings(true, 1.0, 1.0, 0.1, 1.0, 1.2, 12, 2, 0.6, 3, 0.5,
			null, null, null).momentum());
	}

	@Test
	void aFileFromBeforeMomentumGainsItsKeys() {
		// An aura section written with the methods' arts but before momentum.
		List<String> keys = List.of("momentum", "momentum_gain", "momentum_ebb", "stance", "stance_damage", "finisher_damage", "pvp_stance");
		String old = D.toJson();
		for (String key : keys) {
			old = old.replaceAll(",\\s*\"" + key + "\": [^,\\n}]+", "");
		}
		for (String key : keys) {
			assertFalse(old.contains("\"" + key + "\""), key + " gone: " + old);
		}
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, parsed.config().aura(), "momentum's settings read as their defaults");
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : keys) {
			assertTrue(grown.contains("\"" + key + "\""), key + " added: " + grown);
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		// The owner's own settings are kept as they were.
		String theirs = old.replace("\"guard_share\": 0.5", "\"guard_share\": 0.3");
		WildercordConfig.AuraSettings kept = WildercordConfig.parse(WildercordConfig.addMissing(theirs).orElseThrow()).config().aura();
		assertEquals(0.3, kept.guardShare(), 1e-9);
		assertEquals(WildercordConfig.AuraMomentum.DEFAULTS, kept.momentum());
	}

	@Test
	void momentumAndStanceTravelToTheClient() {
		// The low bits are the switches; the awakening's momentum rides higher up (see awakeningTravelsToTheClient).
		int switches = 0xFF;
		Config.Sync on = Config.Sync.of(D);
		assertEquals(Config.Sync.MOMENTUM | Config.Sync.STANCE | Config.Sync.AWAKENING | Config.Sync.WAYS, on.combat() & switches);
		assertEquals(Config.Sync.DEFAULT.combat(), on.combat(), "the default before the server speaks is the same");
		WildercordConfig off = WildercordConfig.parse("{\"aura\": {\"momentum\": false}}").config();
		assertEquals(Config.Sync.STANCE | Config.Sync.AWAKENING | Config.Sync.WAYS, Config.Sync.of(off).combat() & switches);
		WildercordConfig neither = WildercordConfig.parse("{\"aura\": {\"momentum\": false, \"stance\": false}}").config();
		assertEquals(Config.Sync.AWAKENING | Config.Sync.WAYS, Config.Sync.of(neither).combat() & switches);
	}

	@Test
	void awakeningSettingsAreTheRulesNumbers() {
		WildercordConfig.AuraAwakening a = D.aura().awakening();
		assertEquals(WildercordConfig.AuraAwakening.DEFAULTS, a);
		assertTrue(a.awakening());
		assertEquals(dev.wildercord.aura.AwakeningRules.MOMENTUM, a.awakeningMomentum(), 1e-9);
		assertEquals(1.0, a.awakeningDuration(), 1e-9);
		assertEquals(dev.wildercord.aura.AwakeningRules.COOLDOWN_TICKS, a.cooldownTicks());
		assertEquals(dev.wildercord.aura.AwakeningRules.SPENT_TICKS, a.spentTicks());
		assertEquals(dev.wildercord.aura.AwakeningRules.ART_PRICE, a.awakeningArtPrice(), 1e-9);
		assertEquals(dev.wildercord.aura.AwakeningRules.DAMAGE, a.awakeningDamage(), 1e-9);
		assertEquals(dev.wildercord.aura.AwakeningRules.SPEED, a.awakeningSpeed(), 1e-9);
		for (String key : List.of("awakening", "awakening_momentum", "awakening_duration", "awakening_cooldown_seconds", "spent_seconds",
				"awakening_art_price", "awakening_damage", "awakening_speed")) {
			assertTrue(D.toJson().contains("\"" + key + "\""), "a fresh file lists " + key);
		}
	}

	@Test
	void awakeningSettingsAreReadAndHeldInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura\": {\"awakening\": false, \"awakening_momentum\": 150, "
			+ "\"awakening_duration\": 9, \"awakening_cooldown_seconds\": -5, \"spent_seconds\": 900, \"awakening_art_price\": 2, "
			+ "\"awakening_damage\": 3, \"awakening_speed\": 0.9}}");
		WildercordConfig.AuraAwakening read = parsed.config().aura().awakening();
		assertFalse(read.awakening());
		assertEquals(100.0, read.awakeningMomentum(), 1e-9, "at most the whole meter");
		assertEquals(4.0, read.awakeningDuration(), 1e-9, "at most four times as long");
		assertEquals(0.0, read.awakeningCooldownSeconds(), 1e-9, "never below nothing");
		assertEquals(300.0, read.spentSeconds(), 1e-9, "at most five minutes spent");
		assertEquals(1.0, read.awakeningArtPrice(), 1e-9, "never more than an art's own price");
		assertEquals(1.0, read.awakeningDamage(), 1e-9, "at most twice as hard");
		assertEquals(0.5, read.awakeningSpeed(), 1e-9, "at most half again as fast");
		assertEquals(7, parsed.warnings().size(), parsed.warnings().toString());
		assertEquals(parsed.config(), WildercordConfig.parse(parsed.config().toJson()).config(), "the written file keeps them");
		WildercordConfig.AuraAwakening fine = WildercordConfig.parse("{\"aura\": {\"awakening_momentum\": 75, \"awakening_duration\": 0.5, "
			+ "\"awakening_art_price\": 0.25, \"spent_seconds\": 12}}").config().aura().awakening();
		assertEquals(75.0, fine.awakeningMomentum(), 1e-9);
		assertEquals(0.5, fine.awakeningDuration(), 1e-9);
		assertEquals(0.25, fine.awakeningArtPrice(), 1e-9);
		assertEquals(240, fine.spentTicks());
		// The constructor from before awakening (and a missing part) take its defaults.
		WildercordConfig.AuraSettings before = new WildercordConfig.AuraSettings(true, 1.0, 1.0, 0.1, 1.0, 1.2, 12, 2, 0.6, 3, 0.5,
			WildercordConfig.AuraHeights.DEFAULTS, WildercordConfig.AuraStrings.DEFAULTS, WildercordConfig.AuraMomentum.DEFAULTS);
		assertEquals(WildercordConfig.AuraAwakening.DEFAULTS, before.awakening());
		assertEquals(WildercordConfig.AuraAwakening.DEFAULTS, new WildercordConfig.AuraSettings(true, 1.0, 1.0, 0.1, 1.0, 1.2, 12, 2, 0.6, 3, 0.5,
			null, null, null, null).awakening());
	}

	@Test
	void aFileFromBeforeAwakeningGainsItsKeys() {
		List<String> keys = List.of("awakening", "awakening_momentum", "awakening_duration", "awakening_cooldown_seconds", "spent_seconds",
			"awakening_art_price", "awakening_damage", "awakening_speed");
		String old = D.toJson();
		for (String key : keys) {
			old = old.replaceAll(",\\s*\"" + key + "\": [^,\\n}]+", "");
		}
		for (String key : keys) {
			assertFalse(old.contains("\"" + key + "\""), key + " gone: " + old);
		}
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, parsed.config().aura(), "awakening's settings read as their defaults");
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : keys) {
			assertTrue(grown.contains("\"" + key + "\""), key + " added: " + grown);
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		String theirs = old.replace("\"guard_share\": 0.5", "\"guard_share\": 0.3").replace("\"momentum_gain\": 1.0", "\"momentum_gain\": 2.0");
		WildercordConfig.AuraSettings kept = WildercordConfig.parse(WildercordConfig.addMissing(theirs).orElseThrow()).config().aura();
		assertEquals(0.3, kept.guardShare(), 1e-9);
		assertEquals(2.0, kept.momentum().momentumGain(), 1e-9, "the owner's momentum settings are kept");
		assertEquals(WildercordConfig.AuraAwakening.DEFAULTS, kept.awakening());
	}

	@Test
	void awakeningTravelsToTheClient() {
		Config.Sync on = Config.Sync.of(D);
		assertTrue((on.combat() & Config.Sync.AWAKENING) != 0);
		assertEquals((int) dev.wildercord.aura.AwakeningRules.MOMENTUM, on.awakeningMomentum(), "the momentum it asks for travels too");
		assertEquals(on.awakeningMomentum(), Config.Sync.DEFAULT.awakeningMomentum());
		WildercordConfig off = WildercordConfig.parse("{\"aura\": {\"awakening\": false}}").config();
		assertEquals(0, Config.Sync.of(off).combat() & Config.Sync.AWAKENING);
		assertTrue((Config.Sync.of(off).combat() & Config.Sync.MOMENTUM) != 0, "the other switches stay as they were");
		for (int needed : new int[] {0, 1, 37, 80, 100}) {
			WildercordConfig set = WildercordConfig.parse("{\"aura\": {\"awakening_momentum\": " + needed + "}}").config();
			assertEquals(needed, Config.Sync.of(set).awakeningMomentum(), "momentum " + needed);
			assertEquals(Config.Sync.MOMENTUM | Config.Sync.STANCE | Config.Sync.AWAKENING | Config.Sync.WAYS, Config.Sync.of(set).combat() & 0xFF);
		}
	}

	@Test
	void waysSettingsAreTheRulesNumbers() {
		WildercordConfig.AuraWays w = D.aura().ways();
		assertEquals(WildercordConfig.AuraWays.DEFAULTS, w);
		assertTrue(w.ways());
		assertTrue(w.changeAtPower(), "changing Way asks a place of power by default");
		assertEquals(dev.wildercord.aura.WayRules.SETTLE_XP, w.settleXp(), 1e-9);
		assertEquals(dev.wildercord.aura.WayRules.BANNER_RANGE, w.bannerRange(), 1e-9);
		assertEquals(dev.wildercord.aura.WayRules.BANNER_MOMENTUM, w.bannerShare(), 1e-9);
		assertEquals(dev.wildercord.aura.WayRules.BANNER_AURA, w.bannerAuraShare(), 1e-9);
		for (String key : List.of("ways", "way_settle_xp", "way_change_at_power", "banner_range", "banner_share", "banner_aura_share")) {
			assertTrue(D.toJson().contains("\"" + key + "\""), "a fresh file lists " + key);
		}
	}

	@Test
	void waysSettingsAreReadAndHeldInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura\": {\"ways\": false, \"way_settle_xp\": -10, \"way_change_at_power\": false, "
			+ "\"banner_range\": 900, \"banner_share\": 4, \"banner_aura_share\": -1}}");
		WildercordConfig.AuraWays read = parsed.config().aura().ways();
		assertFalse(read.ways());
		assertFalse(read.changeAtPower());
		assertEquals(0.0, read.settleXp(), 1e-9, "never owing less than nothing");
		assertEquals(48.0, read.bannerRange(), 1e-9, "at most three chunks");
		assertEquals(1.0, read.bannerShare(), 1e-9, "never more than all of it");
		assertEquals(0.0, read.bannerAuraShare(), 1e-9, "never less than none");
		assertEquals(4, parsed.warnings().size(), parsed.warnings().toString());
		assertEquals(parsed.config(), WildercordConfig.parse(parsed.config().toJson()).config(), "the written file keeps them");
		WildercordConfig.AuraWays fine = WildercordConfig.parse("{\"aura\": {\"way_settle_xp\": 600, \"banner_range\": 20, \"banner_share\": 0.5}}")
			.config().aura().ways();
		assertEquals(600.0, fine.settleXp(), 1e-9);
		assertEquals(20.0, fine.bannerRange(), 1e-9);
		assertEquals(0.5, fine.bannerShare(), 1e-9);
		assertTrue(fine.ways() && fine.changeAtPower(), "what isn't given stays at its default");
		// The constructor from before Ways (and a missing part) take their defaults.
		WildercordConfig.AuraSettings before = new WildercordConfig.AuraSettings(true, 1.0, 1.0, 0.1, 1.0, 1.2, 12, 2, 0.6, 3, 0.5,
			WildercordConfig.AuraHeights.DEFAULTS, WildercordConfig.AuraStrings.DEFAULTS, WildercordConfig.AuraMomentum.DEFAULTS,
			WildercordConfig.AuraAwakening.DEFAULTS);
		assertEquals(WildercordConfig.AuraWays.DEFAULTS, before.ways());
		assertEquals(WildercordConfig.AuraWays.DEFAULTS, new WildercordConfig.AuraSettings(true, 1.0, 1.0, 0.1, 1.0, 1.2, 12, 2, 0.6, 3, 0.5,
			null, null, null, null, null).ways());
	}

	@Test
	void aFileFromBeforeWaysGainsTheirKeys() {
		List<String> keys = List.of("ways", "way_settle_xp", "way_change_at_power", "banner_range", "banner_share", "banner_aura_share");
		String old = D.toJson();
		for (String key : keys) {
			old = old.replaceAll(",\\s*\"" + key + "\": [^,\\n}]+", "");
		}
		for (String key : keys) {
			assertFalse(old.contains("\"" + key + "\""), key + " gone: " + old);
		}
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraSettings.DEFAULTS, parsed.config().aura(), "the Ways' settings read as their defaults");
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : keys) {
			assertTrue(grown.contains("\"" + key + "\""), key + " added: " + grown);
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		String theirs = old.replace("\"awakening_duration\": 1.0", "\"awakening_duration\": 2.0");
		WildercordConfig.AuraSettings kept = WildercordConfig.parse(WildercordConfig.addMissing(theirs).orElseThrow()).config().aura();
		assertEquals(2.0, kept.awakening().awakeningDuration(), 1e-9, "the owner's awakening settings are kept");
		assertEquals(WildercordConfig.AuraWays.DEFAULTS, kept.ways());
	}

	@Test
	void waysTravelToTheClient() {
		assertTrue((Config.Sync.of(D).combat() & Config.Sync.WAYS) != 0);
		assertTrue((Config.Sync.DEFAULT.combat() & Config.Sync.WAYS) != 0, "on until the server says otherwise");
		WildercordConfig off = WildercordConfig.parse("{\"aura\": {\"ways\": false}}").config();
		assertEquals(0, Config.Sync.of(off).combat() & Config.Sync.WAYS);
		assertEquals(Config.Sync.MOMENTUM | Config.Sync.STANCE | Config.Sync.AWAKENING, Config.Sync.of(off).combat() & 0xFF,
			"the other switches stay as they were");
		assertEquals(Config.Sync.of(D).awakeningMomentum(), Config.Sync.of(off).awakeningMomentum(), "the momentum an awakening asks for is untouched");
		// Its bit is one of the free low ones, clear of the awakening's momentum (bits 8 to 15).
		assertTrue(Config.Sync.WAYS < 1 << 8);
		assertEquals(0, Config.Sync.WAYS & (Config.Sync.MOMENTUM | Config.Sync.STANCE | Config.Sync.AWAKENING));
	}

	@Test
	void auraWorldDefaultsAreTheRulesNumbers() {
		WildercordConfig.AuraWorldSettings w = D.auraWorld();
		assertTrue(w.duelists() && w.knights() && w.forgedGear() && w.duelistCamps());
		assertTrue(w.duelistsSpawn() && w.knightsSpawn());
		assertEquals(1.0, w.duelistSpawnRate(), 1e-9);
		assertEquals(2, w.maxDuelists());
		assertEquals(1.0, w.knightSpawnRate(), 1e-9);
		assertEquals(2, w.maxKnightsNearby());
		assertEquals(dev.wildercord.aura.world.AuraWorldRules.LUMENEDGE_GAIN, w.lumenedgeGain(), 1e-9);
		assertEquals(dev.wildercord.aura.world.AuraWorldRules.SKYREND_SLASH, w.skyrendSlash(), 1e-9);
		assertEquals(dev.wildercord.aura.world.AuraWorldRules.BULWARK_GUARD_COST, w.bulwarkGuardCost(), 1e-9);
		assertEquals(dev.wildercord.aura.world.AuraWorldRules.SASH_CAPACITY, w.sashCapacity(), 1e-9);
		assertTrue(D.toJson().contains("\"aura_world\"") && D.toJson().contains("\"max_knights_nearby\""), "a fresh file lists the aura_world section");
	}

	@Test
	void auraWorldSettingsAreReadAndKeptInRange() {
		WildercordConfig.Parsed parsed = WildercordConfig.parse("{\"aura_world\": {\"duelists\": false, \"duelist_spawn_rate\": 9, \"max_duelists\": 3,"
			+ " \"duelist_camps\": false, \"knights\": true, \"knight_spawn_rate\": 0.5, \"max_knights_nearby\": 20, \"forged_gear\": false,"
			+ " \"lumenedge_gain\": 2, \"skyrend_slash\": 0.5, \"bulwark_guard_cost\": 0.25, \"sash_capacity\": 1.5, \"knight_rate\": 1}}");
		WildercordConfig.AuraWorldSettings w = parsed.config().auraWorld();
		assertFalse(w.duelists());
		assertFalse(w.duelistsSpawn());
		assertEquals(4.0, w.duelistSpawnRate(), 1e-9, "clamped to 4");
		assertEquals(3, w.maxDuelists());
		assertFalse(w.duelistCamps());
		assertEquals(0.5, w.knightSpawnRate(), 1e-9);
		assertEquals(8, w.maxKnightsNearby(), "clamped to 8");
		assertFalse(w.forgedGear());
		assertEquals(2.0, w.lumenedgeGain(), 1e-9);
		assertEquals(1.0, w.skyrendSlash(), 1e-9, "never weaker than an unforged slash");
		assertEquals(0.25, w.bulwarkGuardCost(), 1e-9);
		assertEquals(1.5, w.sashCapacity(), 1e-9);
		assertEquals(4, parsed.warnings().size(), parsed.warnings().toString());
		assertTrue(parsed.warnings().stream().anyMatch(w2 -> w2.contains("aura_world.knight_rate")), "a typo is reported");
		WildercordConfig changed = parsed.config();
		assertEquals(changed, WildercordConfig.parse(changed.toJson()).config(), "the written file keeps every changed setting");
		assertFalse(WildercordConfig.parse("{\"aura_world\": {\"knight_spawn_rate\": 0}}").config().auraWorld().knightsSpawn(), "a rate of 0 stops them");
		assertFalse(WildercordConfig.parse("{\"aura_world\": {\"max_duelists\": 0}}").config().auraWorld().duelistsSpawn(), "room for none stops them");
	}

	@Test
	void aFileFromBeforeTheWorldOfAuraGainsTheSection() {
		String old = D.toJson().replaceAll("(?s),\\s*\"aura_world\": \\{.*$", "\n}\n");
		assertFalse(old.contains("\"aura_world\""), old);
		assertTrue(old.contains("\"aura\""), "only the newer section is gone");
		WildercordConfig.Parsed parsed = WildercordConfig.parse(old);
		assertTrue(parsed.warnings().isEmpty(), parsed.warnings().toString());
		assertEquals(WildercordConfig.AuraWorldSettings.DEFAULTS, parsed.config().auraWorld());
		String grown = WildercordConfig.addMissing(old).orElseThrow();
		for (String key : List.of("\"aura_world\"", "\"duelists\"", "\"duelist_spawn_rate\"", "\"max_duelists\"", "\"duelist_camps\"", "\"knights\"",
				"\"knight_spawn_rate\"", "\"max_knights_nearby\"", "\"forged_gear\"", "\"lumenedge_gain\"", "\"skyrend_slash\"", "\"bulwark_guard_cost\"",
				"\"sash_capacity\"")) {
			assertTrue(grown.contains(key), key + " should have been added");
		}
		assertTrue(WildercordConfig.addMissing(grown).isEmpty(), "nothing more to add the second time");
		String partial = D.toJson().replace("\"max_duelists\": 2", "\"max_duelists\": 5").replace("\"knight_spawn_rate\": 1.0,", "");
		WildercordConfig.AuraWorldSettings fixed = WildercordConfig.parse(WildercordConfig.addMissing(partial).orElseThrow()).config().auraWorld();
		assertEquals(1.0, fixed.knightSpawnRate(), 1e-9);
		assertEquals(5, fixed.maxDuelists());
	}
}
