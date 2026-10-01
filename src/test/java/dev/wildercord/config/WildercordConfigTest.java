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
	void chancesScaleAndStayWithinAHundred() {
		assertEquals(35, WildercordConfig.scaledChance(35, 1.0));
		assertEquals(70, WildercordConfig.scaledChance(35, 2.0));
		assertEquals(100, WildercordConfig.scaledChance(35, 10.0));
		assertEquals(0, WildercordConfig.scaledChance(35, 0.0));
		assertEquals(18, WildercordConfig.scaledChance(35, 0.5));
	}
}
