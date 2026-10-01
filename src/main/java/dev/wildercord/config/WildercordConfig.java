package dev.wildercord.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The server owner's settings, as read from {@code config/wildercord.json}. Pure (no Minecraft types),
 * so parsing, defaults and validation are unit-tested. Every field has a default equal to the number
 * the mod used before it had a config, a missing or broken field falls back to it, and a value out of
 * range is clamped (each with a warning), so a hand-edited file can never stop a server.
 *
 * <p>The feature switches ({@code world_events}, {@code duels}, {@code wild_magic},
 * {@code world_changing_magic}, {@code creature_affinities}, {@code elemental_climate},
 * {@code player_affinity}, {@code unread_runes}) are read by those features; each defaults to on.</p>
 *
 * @param maxCreatures       creatures one cast may touch (links and echoes included)
 * @param maxBlocks          blocks one cast may change
 * @param spellsEditBlocks   whether spells may change blocks at all (Break, Grow, Rampart...)
 * @param pvpDamageScale     rune damage to players from players, as a fraction
 * @param manaRegenMultiplier mana regeneration, times this
 * @param manaCostMultiplier every spell's mana (and Blood Price health) cost, times this
 * @param runeboundChance    the chance a monster spawns as a Runebound, times this
 * @param runeLootChance     the chance of a rune in a structure chest (or a mob's rune drop), times this
 * @param crystalLootChance  the chance of a Mana Crystal in a structure chest, times this
 * @param pageLootChance     the chance of a Torn Page in a structure chest, times this
 * @param gearLootChance     the chance of casting gear in a structure chest or from a boss, times this
 * @param imbueMaxItems      imbued items one caster keeps before the oldest fades
 * @param imbueMaxGlyphs     glyphs one caster keeps in a world before the oldest fades
 * @param playerAffinity     whether players grow affinities with the elements (and what they give: power, resistance, cheaper spells)
 * @param affinityGain       how fast affinity points come, times this (the daily allowances count what's done, not what it's worth)
 * @param travel             the travel commands ({@code /home}, {@code /warp}, {@code /tpa}...): see {@link TravelSettings}
 * @param defence            how players stand up to spells (armour, the bonus cap, the spellguard): see {@link DefenceSettings}
 * @param mastery            spells that grow with their caster (ranks, traits, sigils, spoken names): see {@link MasterySettings}
 * @param channeling         how a charge can be pushed and steadied (overchannel, the beat, sigil tracing): see {@link ChannelingSettings}
 * @param unreadRunes        whether a newly learned rune starts unread, its text a hint until it's been cast and seen at work
 * @param resonances         each world's own resonances and rune quirks: see {@link ResonanceSettings}
 * @param residues           the lasting marks big magic leaves on the world: see {@link ResidueSettings}
 * @param power              places and times of power (ley crossings, the moon, the hour, the weather): see {@link PowerSettings}
 * @param wildlife           magical wildlife spawning in its biomes (the wildlife keys of the {@code creatures} section): see {@link WildlifeSettings}
 */
public record WildercordConfig(
	int maxCreatures,
	int maxBlocks,
	boolean spellsEditBlocks,
	double pvpDamageScale,
	double manaRegenMultiplier,
	double manaCostMultiplier,
	double runeboundChance,
	double runeLootChance,
	double crystalLootChance,
	double pageLootChance,
	double gearLootChance,
	int imbueMaxItems,
	int imbueMaxGlyphs,
	boolean worldEvents,
	boolean duels,
	boolean wildMagic,
	boolean worldChangingMagic,
	boolean creatureAffinities,
	boolean elementalClimate,
	boolean playerAffinity,
	double affinityGain,
	TravelSettings travel,
	DefenceSettings defence,
	MasterySettings mastery,
	ChannelingSettings channeling,
	boolean unreadRunes,
	ResonanceSettings resonances,
	ResidueSettings residues,
	PowerSettings power,
	WildlifeSettings wildlife
) {
	public static final WildercordConfig DEFAULTS = new WildercordConfig(64, 32, true, 0.6, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 6, 12,
		true, true, true, true, true, true, true, 1.0, TravelSettings.DEFAULTS, DefenceSettings.DEFAULTS, MasterySettings.DEFAULTS,
		ChannelingSettings.DEFAULTS, true, ResonanceSettings.DEFAULTS, ResidueSettings.DEFAULTS, PowerSettings.DEFAULTS, WildlifeSettings.DEFAULTS);

	/**
	 * The travel commands' settings (the {@code travel} section). A file written before the section
	 * existed reads as these defaults.
	 *
	 * @param enabled            whether the commands exist at all (read when commands are registered: at start and on {@code /reload})
	 * @param maxHomes           homes one player may set
	 * @param warmupSeconds      how long a player stands still before a teleport happens (operators and creative players don't wait)
	 * @param cooldownSeconds    how long before {@code /home}, {@code /warp}, {@code /spawn}, {@code /back} or {@code /tpa} can be used again
	 * @param rtpCooldownSeconds how long before {@code /rtp} can be used again
	 * @param rtpRadius          how far from world spawn {@code /rtp} may land, in blocks
	 * @param tpaTimeoutSeconds  how long a teleport request waits for an answer
	 */
	public record TravelSettings(boolean enabled, int maxHomes, int warmupSeconds, int cooldownSeconds, int rtpCooldownSeconds, int rtpRadius,
			int tpaTimeoutSeconds) {
		public static final TravelSettings DEFAULTS = new TravelSettings(true, 3, 3, 30, 300, 5000, 60);
	}

	/**
	 * How players stand up to spells (the {@code defence} section), whoever cast them. None of it touches spells landing on
	 * creatures. A file written before the section existed reads as these defaults.
	 *
	 * @param spellguard                whether the spellguard is on: a single spell hit can't take a player from high health
	 *                                  straight to dead, and leaves them on one heart instead
	 * @param spellguardHealth          the share of full health (0.8 is 80%) a player needs for the spellguard to hold
	 * @param spellguardRechargeSeconds how long the spellguard takes to come back after it has held
	 * @param maxBonus                  the most a hit's bonuses together (execute, reactions, affinities, backstabs...) may
	 *                                  multiply a spell against a player
	 * @param armourRate                how much of armour's worth against blades counts against spells that ignore armour
	 *                                  (magic, frost): 0 is none, 1 all of it
	 */
	public record DefenceSettings(boolean spellguard, double spellguardHealth, int spellguardRechargeSeconds, double maxBonus, double armourRate) {
		public static final DefenceSettings DEFAULTS = new DefenceSettings(true, 0.8, 60, 2.5, 0.55);
	}

	/**
	 * Spell mastery (the {@code mastery} section): spells that grow with the one who casts them. A file written before the
	 * section existed reads as these defaults.
	 *
	 * @param enabled       whether spells gain experience and ranks at all (records already earned are kept while it's off)
	 * @param xpMultiplier  how fast spells gain experience, times this
	 * @param traits        whether the traits players chose for their spells take effect
	 * @param spokenNames   whether a named spell of Adept rank or higher shows its name to everyone nearby when cast
	 * @param inscription   whether an Adept spell can be inscribed onto a scroll with its traits for someone else to learn
	 */
	public record MasterySettings(boolean enabled, double xpMultiplier, boolean traits, boolean spokenNames, boolean inscription) {
		public static final MasterySettings DEFAULTS = new MasterySettings(true, 1.0, true, true, true);
	}

	/**
	 * How a charge can be pushed and steadied (the {@code channeling} section). A file written before the
	 * section existed reads as these defaults. Overchannel and tracing bonuses count inside
	 * {@link DefenceSettings#maxBonus} against players.
	 *
	 * @param overchannel          whether a charge held past full climbs overchannel stages (off: it just waits, as before)
	 * @param powerPerStage        the power each stage adds (0.2: +20%, +40%, +60%)
	 * @param drainPerSecond       the share of the spell's mana price each second of overchannel drains (never the price itself)
	 * @param surgeChancePerStage  the chance of a wild surge on release each stage adds (wild magic must be on)
	 * @param beatBonus            the power added by letting go on the beat (as the charge fills or a stage lands)
	 * @param backfireStunSeconds  how long a channel held too long dazes its caster (at most 2)
	 * @param backfireManaBurn     the share of full mana a channel held too long burns away
	 * @param sigilTracing         whether a glyph traced while charging (holding sneak) steadies the spell
	 * @param tracePower           the power a perfectly traced glyph adds
	 */
	public record ChannelingSettings(boolean overchannel, double powerPerStage, double drainPerSecond, double surgeChancePerStage, double beatBonus,
			double backfireStunSeconds, double backfireManaBurn, boolean sigilTracing, double tracePower) {
		public static final ChannelingSettings DEFAULTS = new ChannelingSettings(true, 0.2, 0.15, 0.07, 0.1, 1.5, 0.3, true, 0.08);

		/** These settings as the overchannel rules read them. */
		public dev.wildercord.spell.Overchannel.Tuning tuning() {
			return new dev.wildercord.spell.Overchannel.Tuning(overchannel, powerPerStage, drainPerSecond, surgeChancePerStage, beatBonus,
				(int) Math.round(backfireStunSeconds * 20), backfireManaBurn, sigilTracing, tracePower);
		}
	}

	/**
	 * Each world's own magic (the {@code harmonies} section; players call resonances harmonies): its resonances, drawn from the world's seed, and its rune
	 * quirks. A file written before the section existed reads as these defaults.
	 *
	 * @param enabled    whether resonances and quirks wake at all (off: casting their runes is only the ordinary spell)
	 * @param count      how many resonances the world holds (0 to {@link dev.wildercord.spell.ResonanceForge#MAX_COUNT})
	 * @param rerollSalt any text: changing it draws the world a fresh set (and forgets who found the old ones); empty keeps the seed's own
	 * @param announce   whether the server tells everyone in chat when someone finds a resonance
	 * @param quirks     how many runes have a quirk in this world (0 to {@link dev.wildercord.spell.RuneQuirks#MAX_COUNT})
	 */
	public record ResonanceSettings(boolean enabled, int count, String rerollSalt, boolean announce, int quirks) {
		public static final ResonanceSettings DEFAULTS = new ResonanceSettings(true, dev.wildercord.spell.ResonanceForge.DEFAULT_COUNT, "", true,
			dev.wildercord.spell.RuneQuirks.DEFAULT_COUNT);
		/** The longest reroll salt kept (a longer one is cut short, with a warning). */
		public static final int MAX_SALT = 64;
	}

	/**
	 * The lasting marks big magic leaves (the {@code residues} section): ash, everfrost, storm-glass, strange flowers, a
	 * scar of void... each fading on its own. They only ever take natural ground or open air, and they ask the same
	 * permission any spell's block change does ({@code casting.spells_edit_blocks} and claims). A file written before the
	 * section existed reads as these defaults.
	 *
	 * @param enabled            whether magic leaves residues at all
	 * @param minSpellCost       the mana a spell must cost (its list price) to leave one; an overcast always does
	 * @param lifetimeMultiplier how long residues last, times this (everfrost holds about a day at 1.0)
	 * @param maxPerChunk        the most residue blocks one chunk holds
	 * @param maxPerDimension    the most residue blocks one dimension holds
	 */
	public record ResidueSettings(boolean enabled, double minSpellCost, double lifetimeMultiplier, int maxPerChunk, int maxPerDimension) {
		public static final ResidueSettings DEFAULTS = new ResidueSettings(true, dev.wildercord.world.ResidueRules.MIN_SPELL_COST, 1.0,
			dev.wildercord.world.ResidueRules.PER_CHUNK, dev.wildercord.world.ResidueRules.PER_DIMENSION);
	}

	/**
	 * Places and times of power (the {@code places_of_power} section), on top of the elemental climate (switching
	 * {@code features.elemental_climate} off turns these off too). A file written before the section existed reads as
	 * these defaults.
	 *
	 * @param leyCrossings        whether standing where two ley lines cross strengthens and cheapens every spell
	 * @param crossingBonus       how much: 0.1 is every element 10% stronger and every spell 10% cheaper
	 * @param celestial           whether the moon, the hour and the weather favour elements (a full moon arcane and void,
	 *                            noon fire, dawn and dusk time, rain frost...)
	 * @param celestialMultiplier those bonuses, scaled: 1 as designed, 0.5 half as strong, 2 twice
	 */
	public record PowerSettings(boolean leyCrossings, double crossingBonus, boolean celestial, double celestialMultiplier) {
		public static final PowerSettings DEFAULTS = new PowerSettings(true, dev.wildercord.spell.ClimateRules.CROSSING_BONUS, true, 1.0);
	}

	/**
	 * Magical wildlife (the wildlife keys of the {@code creatures} section): glimmerwings, lumen stags, mossback tortoises,
	 * cinderfoxes, skyrays and rimehares, each spawning on its own in its biomes. Spawn eggs and {@code /summon} work
	 * whatever these say. A file written before the settings existed reads as these defaults.
	 *
	 * @param enabled          whether wildlife spawns naturally at all
	 * @param spawnMultiplier  how often it spawns, times this: below 1 fewer spawns are let through at once; above 1 their
	 *                         spawn weights grow too, from the next time the world loads. 0 is the same as switching it off
	 * @param glimmerwing      whether glimmerwings spawn (forests and flower fields, at night)
	 * @param lumenStag        whether lumen stags spawn (old forests, taigas and cherry groves; rare)
	 * @param mossbackTortoise whether mossback tortoises spawn (swamps, mangroves and jungles)
	 * @param cinderfox        whether cinderfoxes spawn (deserts and badlands)
	 * @param skyray           whether skyrays spawn (mountains, windswept hills and meadows; rare)
	 * @param rimehare         whether rimehares spawn (snowy biomes)
	 */
	public record WildlifeSettings(boolean enabled, double spawnMultiplier, boolean glimmerwing, boolean lumenStag, boolean mossbackTortoise,
			boolean cinderfox, boolean skyray, boolean rimehare) {
		public static final WildlifeSettings DEFAULTS = new WildlifeSettings(true, 1.0, true, true, true, true, true, true);

		/**
		 * Whether one creature spawns naturally, by its id ({@code "lumen_stag"}): the master switch, a multiplier above 0 and
		 * its own switch. An id that isn't wildlife never does.
		 */
		public boolean spawns(String creature) {
			if (!enabled || spawnMultiplier <= 0) {
				return false;
			}
			return switch (creature) {
				case "glimmerwing" -> glimmerwing;
				case "lumen_stag" -> lumenStag;
				case "mossback_tortoise" -> mossbackTortoise;
				case "cinderfox" -> cinderfox;
				case "skyray" -> skyray;
				case "rimehare" -> rimehare;
				default -> false;
			};
		}
	}

	/** The file's format version, written so later versions can migrate it. */
	public static final int VERSION = 1;

	/** What a file said, and everything wrong with it (each already fixed in {@link #config}). */
	public record Parsed(WildercordConfig config, List<String> warnings) {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	// ------------------------------------------------------------------ reading

	public static Parsed parse(String json) {
		List<String> warnings = new ArrayList<>();
		JsonObject root;
		try {
			JsonElement element = JsonParser.parseString(json);
			if (!element.isJsonObject()) {
				warnings.add("the file isn't a JSON object; using the defaults");
				return new Parsed(DEFAULTS, warnings);
			}
			root = element.getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			warnings.add("the file isn't valid JSON (" + e.getMessage() + "); using the defaults");
			return new Parsed(DEFAULTS, warnings);
		}
		Reader r = new Reader(root, warnings);
		WildercordConfig d = DEFAULTS;
		WildercordConfig config = new WildercordConfig(
			r.integer("casting", "max_creatures_per_cast", d.maxCreatures, 1, 1024),
			r.integer("casting", "max_blocks_per_cast", d.maxBlocks, 0, 4096),
			r.bool("casting", "spells_edit_blocks", d.spellsEditBlocks),
			r.number("casting", "pvp_damage_scale", d.pvpDamageScale, 0, 10),
			r.number("mana", "regen_multiplier", d.manaRegenMultiplier, 0, 100),
			r.number("mana", "cost_multiplier", d.manaCostMultiplier, 0, 100),
			r.number("world", "runebound_chance_multiplier", d.runeboundChance, 0, 100),
			r.number("loot", "rune_chance_multiplier", d.runeLootChance, 0, 100),
			r.number("loot", "crystal_chance_multiplier", d.crystalLootChance, 0, 100),
			r.number("loot", "page_chance_multiplier", d.pageLootChance, 0, 100),
			r.number("loot", "gear_chance_multiplier", d.gearLootChance, 0, 100),
			r.integer("imbuing", "max_items", d.imbueMaxItems, 1, 64),
			r.integer("imbuing", "max_glyphs", d.imbueMaxGlyphs, 1, 256),
			r.bool("features", "world_events", d.worldEvents),
			r.bool("features", "duels", d.duels),
			r.bool("features", "wild_magic", d.wildMagic),
			r.bool("features", "world_changing_magic", d.worldChangingMagic),
			r.bool("features", "creature_affinities", d.creatureAffinities),
			r.bool("features", "elemental_climate", d.elementalClimate),
			r.bool("features", "player_affinity", d.playerAffinity),
			r.number("affinity", "gain_multiplier", d.affinityGain, 0, 100),
			new TravelSettings(
				r.bool("travel", "enabled", d.travel.enabled()),
				r.integer("travel", "max_homes", d.travel.maxHomes(), 0, 1000),
				r.integer("travel", "warmup_seconds", d.travel.warmupSeconds(), 0, 60),
				r.integer("travel", "cooldown_seconds", d.travel.cooldownSeconds(), 0, 86400),
				r.integer("travel", "rtp_cooldown_seconds", d.travel.rtpCooldownSeconds(), 0, 86400),
				r.integer("travel", "rtp_radius", d.travel.rtpRadius(), 16, 1000000),
				r.integer("travel", "tpa_timeout_seconds", d.travel.tpaTimeoutSeconds(), 5, 3600)),
			new DefenceSettings(
				r.bool("defence", "spellguard", d.defence.spellguard()),
				r.number("defence", "spellguard_health", d.defence.spellguardHealth(), 0.1, 1),
				r.integer("defence", "spellguard_recharge_seconds", d.defence.spellguardRechargeSeconds(), 0, 3600),
				r.number("defence", "max_bonus", d.defence.maxBonus(), 1, 100),
				r.number("defence", "armour_rate", d.defence.armourRate(), 0, 1)),
			new MasterySettings(
				r.bool("mastery", "enabled", d.mastery.enabled()),
				r.number("mastery", "xp_multiplier", d.mastery.xpMultiplier(), 0, 100),
				r.bool("mastery", "traits", d.mastery.traits()),
				r.bool("mastery", "spoken_names", d.mastery.spokenNames()),
				r.bool("mastery", "inscription", d.mastery.inscription())),
			new ChannelingSettings(
				r.bool("channeling", "overchannel", d.channeling.overchannel()),
				r.number("channeling", "power_per_stage", d.channeling.powerPerStage(), 0, 1),
				r.number("channeling", "drain_per_second", d.channeling.drainPerSecond(), 0, 2),
				r.number("channeling", "surge_chance_per_stage", d.channeling.surgeChancePerStage(), 0, 0.33),
				r.number("channeling", "beat_bonus", d.channeling.beatBonus(), 0, 0.5),
				r.number("channeling", "backfire_stun_seconds", d.channeling.backfireStunSeconds(), 0, 2),
				r.number("channeling", "backfire_mana_burn", d.channeling.backfireManaBurn(), 0, 1),
				r.bool("channeling", "sigil_tracing", d.channeling.sigilTracing()),
				r.number("channeling", "trace_power", d.channeling.tracePower(), 0, 0.25)),
			r.bool("features", "unread_runes", d.unreadRunes),
			new ResonanceSettings(
				r.bool("harmonies", "enabled", d.resonances.enabled()),
				r.integer("harmonies", "count", d.resonances.count(), 0, dev.wildercord.spell.ResonanceForge.MAX_COUNT),
				r.string("harmonies", "reroll_salt", d.resonances.rerollSalt(), ResonanceSettings.MAX_SALT),
				r.bool("harmonies", "announce", d.resonances.announce()),
				r.integer("harmonies", "quirks", d.resonances.quirks(), 0, dev.wildercord.spell.RuneQuirks.MAX_COUNT)),
			new ResidueSettings(
				r.bool("residues", "enabled", d.residues.enabled()),
				r.number("residues", "min_spell_cost", d.residues.minSpellCost(), 1, 1000),
				r.number("residues", "lifetime_multiplier", d.residues.lifetimeMultiplier(), 0.05, 10),
				r.integer("residues", "max_per_chunk", d.residues.maxPerChunk(), 1, 256),
				r.integer("residues", "max_per_dimension", d.residues.maxPerDimension(), 0, 100000)),
			new PowerSettings(
				r.bool("places_of_power", "ley_crossings", d.power.leyCrossings()),
				r.number("places_of_power", "crossing_bonus", d.power.crossingBonus(), 0, 0.5),
				r.bool("places_of_power", "celestial", d.power.celestial()),
				r.number("places_of_power", "celestial_multiplier", d.power.celestialMultiplier(), 0, 2)),
			new WildlifeSettings(
				r.bool("creatures", "wildlife", d.wildlife.enabled()),
				r.number("creatures", "wildlife_spawn_multiplier", d.wildlife.spawnMultiplier(), 0, 10),
				r.bool("creatures", "glimmerwing", d.wildlife.glimmerwing()),
				r.bool("creatures", "lumen_stag", d.wildlife.lumenStag()),
				r.bool("creatures", "mossback_tortoise", d.wildlife.mossbackTortoise()),
				r.bool("creatures", "cinderfox", d.wildlife.cinderfox()),
				r.bool("creatures", "skyray", d.wildlife.skyray()),
				r.bool("creatures", "rimehare", d.wildlife.rimehare())));
		r.unknown();
		return new Parsed(config, warnings);
	}

	/** Every section and the keys it holds, in the order the file lists them. */
	static final Map<String, Set<String>> KEYS = new LinkedHashMap<>();

	static {
		KEYS.put("casting", Set.of("max_creatures_per_cast", "max_blocks_per_cast", "spells_edit_blocks", "pvp_damage_scale"));
		KEYS.put("mana", Set.of("regen_multiplier", "cost_multiplier"));
		KEYS.put("world", Set.of("runebound_chance_multiplier"));
		KEYS.put("loot", Set.of("rune_chance_multiplier", "crystal_chance_multiplier", "page_chance_multiplier", "gear_chance_multiplier"));
		KEYS.put("imbuing", Set.of("max_items", "max_glyphs"));
		KEYS.put("features", Set.of("world_events", "duels", "wild_magic", "world_changing_magic", "creature_affinities", "elemental_climate",
			"player_affinity", "unread_runes"));
		KEYS.put("affinity", Set.of("gain_multiplier"));
		KEYS.put("travel", Set.of("enabled", "max_homes", "warmup_seconds", "cooldown_seconds", "rtp_cooldown_seconds", "rtp_radius", "tpa_timeout_seconds"));
		KEYS.put("defence", Set.of("spellguard", "spellguard_health", "spellguard_recharge_seconds", "max_bonus", "armour_rate"));
		KEYS.put("mastery", Set.of("enabled", "xp_multiplier", "traits", "spoken_names", "inscription"));
		KEYS.put("channeling", Set.of("overchannel", "power_per_stage", "drain_per_second", "surge_chance_per_stage", "beat_bonus",
			"backfire_stun_seconds", "backfire_mana_burn", "sigil_tracing", "trace_power"));
		KEYS.put("harmonies", Set.of("enabled", "count", "reroll_salt", "announce", "quirks"));
		KEYS.put("residues", Set.of("enabled", "min_spell_cost", "lifetime_multiplier", "max_per_chunk", "max_per_dimension"));
		KEYS.put("places_of_power", Set.of("ley_crossings", "crossing_bonus", "celestial", "celestial_multiplier"));
		KEYS.put("creatures", Set.of("wildlife", "wildlife_spawn_multiplier", "glimmerwing", "lumen_stag", "mossback_tortoise", "cinderfox", "skyray",
			"rimehare"));
	}

	/** Reads fields out of the sections, falling back and clamping with a warning for each problem. */
	private static final class Reader {
		final JsonObject root;
		final List<String> warnings;

		Reader(JsonObject root, List<String> warnings) {
			this.root = root;
			this.warnings = warnings;
		}

		JsonPrimitive field(String section, String key) {
			JsonElement s = root.get(section);
			if (s == null) {
				return null;
			}
			if (!s.isJsonObject()) {
				String message = "\"" + section + "\" should be an object; using its defaults";
				if (!warnings.contains(message)) {
					warnings.add(message);
				}
				return null;
			}
			JsonElement value = s.getAsJsonObject().get(key);
			if (value == null || value.isJsonNull()) {
				return null;
			}
			if (!value.isJsonPrimitive()) {
				warnings.add(section + "." + key + " should be a single value; using the default");
				return null;
			}
			return value.getAsJsonPrimitive();
		}

		double number(String section, String key, double fallback, double min, double max) {
			JsonPrimitive p = field(section, key);
			if (p == null) {
				return fallback;
			}
			if (!p.isNumber()) {
				warnings.add(section + "." + key + " should be a number; using " + fallback);
				return fallback;
			}
			double value = p.getAsDouble();
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				warnings.add(section + "." + key + " should be a number; using " + fallback);
				return fallback;
			}
			if (value < min || value > max) {
				double clamped = Math.max(min, Math.min(max, value));
				warnings.add(section + "." + key + " must be between " + trim(min) + " and " + trim(max) + "; using " + trim(clamped));
				return clamped;
			}
			return value;
		}

		int integer(String section, String key, int fallback, int min, int max) {
			JsonPrimitive p = field(section, key);
			if (p == null) {
				return fallback;
			}
			if (!p.isNumber() || p.getAsDouble() != Math.rint(p.getAsDouble())) {
				warnings.add(section + "." + key + " should be a whole number; using " + fallback);
				return fallback;
			}
			double value = p.getAsDouble();
			if (value < min || value > max) {
				int clamped = (int) Math.max(min, Math.min(max, value));
				warnings.add(section + "." + key + " must be between " + min + " and " + max + "; using " + clamped);
				return clamped;
			}
			return (int) value;
		}

		boolean bool(String section, String key, boolean fallback) {
			JsonPrimitive p = field(section, key);
			if (p == null) {
				return fallback;
			}
			if (!p.isBoolean()) {
				warnings.add(section + "." + key + " should be true or false; using " + fallback);
				return fallback;
			}
			return p.getAsBoolean();
		}

		String string(String section, String key, String fallback, int maxLength) {
			JsonPrimitive p = field(section, key);
			if (p == null) {
				return fallback;
			}
			if (!p.isString()) {
				warnings.add(section + "." + key + " should be text in quotes; using \"" + fallback + "\"");
				return fallback;
			}
			String value = p.getAsString();
			if (value.length() > maxLength) {
				warnings.add(section + "." + key + " may be at most " + maxLength + " characters; using the first " + maxLength);
				return value.substring(0, maxLength);
			}
			return value;
		}

		/** Warns about keys nobody reads (usually a typo), so they aren't silently ignored. */
		void unknown() {
			for (Map.Entry<String, JsonElement> section : root.entrySet()) {
				String name = section.getKey();
				if (name.startsWith("_") || name.equals("version")) {
					continue;
				}
				Set<String> keys = KEYS.get(name);
				if (keys == null) {
					warnings.add("unknown section \"" + name + "\" (ignored)");
					continue;
				}
				if (section.getValue().isJsonObject()) {
					for (String key : section.getValue().getAsJsonObject().keySet()) {
						if (!keys.contains(key) && !key.startsWith("_")) {
							warnings.add("unknown setting " + name + "." + key + " (ignored)");
						}
					}
				}
			}
		}
	}

	private static String trim(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
	}

	// ------------------------------------------------------------------ writing

	/** The whole file, every field present and explained, as written on first start. */
	public String toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("_about", "Wildercord server settings. Change a value and run /wildercord reload. Every field has a default; delete one to get it back.");
		root.addProperty("version", VERSION);

		JsonObject casting = new JsonObject();
		casting.addProperty("_about", "Hard caps per cast (links, echoes and pulses included), block editing and PvP.");
		casting.addProperty("max_creatures_per_cast", maxCreatures);
		casting.addProperty("max_blocks_per_cast", maxBlocks);
		casting.addProperty("spells_edit_blocks", spellsEditBlocks);
		casting.addProperty("pvp_damage_scale", pvpDamageScale);
		root.add("casting", casting);

		JsonObject mana = new JsonObject();
		mana.addProperty("_about", "Multipliers on mana regeneration and on every spell's cost (the Cord screen shows the result).");
		mana.addProperty("regen_multiplier", manaRegenMultiplier);
		mana.addProperty("cost_multiplier", manaCostMultiplier);
		root.add("mana", mana);

		JsonObject world = new JsonObject();
		world.addProperty("_about", "Runebound monsters. Structure spacing lives in the datapack: data/wildercord/worldgen/structure_set/archives.json.");
		world.addProperty("runebound_chance_multiplier", runeboundChance);
		root.add("world", world);

		JsonObject loot = new JsonObject();
		loot.addProperty("_about", "Multipliers on how often magic is found. Chest changes apply when loot tables load (world start, or /reload).");
		loot.addProperty("rune_chance_multiplier", runeLootChance);
		loot.addProperty("crystal_chance_multiplier", crystalLootChance);
		loot.addProperty("page_chance_multiplier", pageLootChance);
		loot.addProperty("gear_chance_multiplier", gearLootChance);
		root.add("loot", loot);

		JsonObject imbuing = new JsonObject();
		imbuing.addProperty("_about", "How many imbued items and glyphs one caster keeps before the oldest fades.");
		imbuing.addProperty("max_items", imbueMaxItems);
		imbuing.addProperty("max_glyphs", imbueMaxGlyphs);
		root.add("imbuing", imbuing);

		JsonObject features = new JsonObject();
		features.addProperty("_about", "Switch whole features off: world events, duels, wild magic, world-changing magic, creature affinities, elemental climate, players' own affinities, "
			+ "and unread runes (a newly learned rune's text is a hint until it has been cast and seen at work).");
		features.addProperty("world_events", worldEvents);
		features.addProperty("duels", duels);
		features.addProperty("wild_magic", wildMagic);
		features.addProperty("world_changing_magic", worldChangingMagic);
		features.addProperty("creature_affinities", creatureAffinities);
		features.addProperty("elemental_climate", elementalClimate);
		features.addProperty("player_affinity", playerAffinity);
		features.addProperty("unread_runes", unreadRunes);
		root.add("features", features);

		JsonObject affinity = new JsonObject();
		affinity.addProperty("_about", "Players' affinities with the elements, grown by casting and by everyday things (smelting, fishing, mining...). 2.0 grows them twice as fast.");
		affinity.addProperty("gain_multiplier", affinityGain);
		root.add("affinity", affinity);

		JsonObject travelSection = new JsonObject();
		travelSection.addProperty("_about", "The travel commands (/home, /warp, /waypoint, /tpa, /back, /spawn, /rtp). Times are in seconds; operators skip warmups and cooldowns.");
		travelSection.addProperty("enabled", travel.enabled());
		travelSection.addProperty("max_homes", travel.maxHomes());
		travelSection.addProperty("warmup_seconds", travel.warmupSeconds());
		travelSection.addProperty("cooldown_seconds", travel.cooldownSeconds());
		travelSection.addProperty("rtp_cooldown_seconds", travel.rtpCooldownSeconds());
		travelSection.addProperty("rtp_radius", travel.rtpRadius());
		travelSection.addProperty("tpa_timeout_seconds", travel.tpaTimeoutSeconds());
		root.add("travel", travelSection);

		JsonObject defenceSection = new JsonObject();
		defenceSection.addProperty("_about", "How players stand up to spells, from players and monsters alike. The spellguard stops one spell hit taking a player "
			+ "from spellguard_health (0.8 is 80%) of their health straight to dead, leaving them on one heart, then recharges. max_bonus caps what a hit's "
			+ "bonuses together multiply a spell by against a player. armour_rate is how much of armour's worth counts against spells that ignore armour.");
		defenceSection.addProperty("spellguard", defence.spellguard());
		defenceSection.addProperty("spellguard_health", defence.spellguardHealth());
		defenceSection.addProperty("spellguard_recharge_seconds", defence.spellguardRechargeSeconds());
		defenceSection.addProperty("max_bonus", defence.maxBonus());
		defenceSection.addProperty("armour_rate", defence.armourRate());
		root.add("defence", defenceSection);

		JsonObject masterySection = new JsonObject();
		masterySection.addProperty("_about", "Spell mastery: spells grow with the one who casts them, through ranks (Kindled to Mythic) earned by casts that matter. "
			+ "xp_multiplier changes how fast (2.0 is twice as fast). traits switches the chosen traits' effects off without losing them, spoken_names whether "
			+ "named Adept spells show their name to people nearby, inscription whether Adept spells can be inscribed onto scrolls with their traits.");
		masterySection.addProperty("enabled", mastery.enabled());
		masterySection.addProperty("xp_multiplier", mastery.xpMultiplier());
		masterySection.addProperty("traits", mastery.traits());
		masterySection.addProperty("spoken_names", mastery.spokenNames());
		masterySection.addProperty("inscription", mastery.inscription());
		root.add("mastery", masterySection);
		JsonObject channelingSection = new JsonObject();
		channelingSection.addProperty("_about", "Casting as a performance. Held past full, a charge overchannels: every 1.2 seconds it climbs a stage (one "
			+ "for a young heart, up to three from the 4th Heart Circle), adding power_per_stage and surge_chance_per_stage while it drains "
			+ "drain_per_second of the spell's price (never the price itself). Letting go just as the charge fills or a stage lands adds beat_bonus. "
			+ "Held too long past the last stage it tears loose: the spell fizzles and the caster is dazed for backfire_stun_seconds and loses "
			+ "backfire_mana_burn of their mana, never their health. Holding sneak while charging traces the spell's glyph (sigil_tracing): "
			+ "accuracy steadies the channel and adds up to trace_power. Against players these bonuses count inside defence.max_bonus.");
		channelingSection.addProperty("overchannel", channeling.overchannel());
		channelingSection.addProperty("power_per_stage", channeling.powerPerStage());
		channelingSection.addProperty("drain_per_second", channeling.drainPerSecond());
		channelingSection.addProperty("surge_chance_per_stage", channeling.surgeChancePerStage());
		channelingSection.addProperty("beat_bonus", channeling.beatBonus());
		channelingSection.addProperty("backfire_stun_seconds", channeling.backfireStunSeconds());
		channelingSection.addProperty("backfire_mana_burn", channeling.backfireManaBurn());
		channelingSection.addProperty("sigil_tracing", channeling.sigilTracing());
		channelingSection.addProperty("trace_power", channeling.tracePower());
		root.add("channeling", channelingSection);
		JsonObject resonanceSection = new JsonObject();
		resonanceSection.addProperty("_about", "Each world's own magic, drawn from its seed: count harmonies (exact rune sequences the world answers with a twist) "
			+ "and quirks (small tweaks to runes). Change reroll_salt to any other text to draw a fresh set (the old ones and who found them are forgotten). "
			+ "announce tells the whole server in chat when someone finds one.");
		resonanceSection.addProperty("enabled", resonances.enabled());
		resonanceSection.addProperty("count", resonances.count());
		resonanceSection.addProperty("reroll_salt", resonances.rerollSalt());
		resonanceSection.addProperty("announce", resonances.announce());
		resonanceSection.addProperty("quirks", resonances.quirks());
		root.add("harmonies", resonanceSection);
		JsonObject residueSection = new JsonObject();
		residueSection.addProperty("_about", "The lasting marks big magic leaves where it lands (ash, everfrost, storm-glass, strange flowers, a scar of void...). "
			+ "A spell leaves one when its mana price reaches min_spell_cost (an overcast always does). They take only natural ground or open air, follow "
			+ "casting.spells_edit_blocks and claims, fade on their own (lifetime_multiplier 2.0 keeps them twice as long) and give reagents when harvested.");
		residueSection.addProperty("enabled", residues.enabled());
		residueSection.addProperty("min_spell_cost", residues.minSpellCost());
		residueSection.addProperty("lifetime_multiplier", residues.lifetimeMultiplier());
		residueSection.addProperty("max_per_chunk", residues.maxPerChunk());
		residueSection.addProperty("max_per_dimension", residues.maxPerDimension());
		root.add("residues", residueSection);

		JsonObject powerSection = new JsonObject();
		powerSection.addProperty("_about", "Places and times of power, part of the elemental climate (features.elemental_climate turns them off too). Where two "
			+ "ley lines cross every spell is crossing_bonus stronger and cheaper (0.1 is 10%). With celestial on, the moon, the hour and the weather favour "
			+ "elements (a full moon arcane and void, noon fire, dawn and dusk time, rain frost); celestial_multiplier scales those bonuses.");
		powerSection.addProperty("ley_crossings", power.leyCrossings());
		powerSection.addProperty("crossing_bonus", power.crossingBonus());
		powerSection.addProperty("celestial", power.celestial());
		powerSection.addProperty("celestial_multiplier", power.celestialMultiplier());
		root.add("places_of_power", powerSection);

		JsonObject creaturesSection = new JsonObject();
		creaturesSection.addProperty("_about", "Creatures of the world. wildlife switches the magical wildlife's natural spawns on or off (glimmerwings, lumen "
			+ "stags, mossback tortoises, cinderfoxes, skyrays and rimehares; spawn eggs and /summon still work). wildlife_spawn_multiplier scales how "
			+ "often they spawn: below 1 it lets fewer through at once, above 1 it also raises their spawn weights from the next world load, and the "
			+ "rare ones stay rare and kept apart either way. Each creature has its own switch too.");
		creaturesSection.addProperty("wildlife", wildlife.enabled());
		creaturesSection.addProperty("wildlife_spawn_multiplier", wildlife.spawnMultiplier());
		creaturesSection.addProperty("glimmerwing", wildlife.glimmerwing());
		creaturesSection.addProperty("lumen_stag", wildlife.lumenStag());
		creaturesSection.addProperty("mossback_tortoise", wildlife.mossbackTortoise());
		creaturesSection.addProperty("cinderfox", wildlife.cinderfox());
		creaturesSection.addProperty("skyray", wildlife.skyray());
		creaturesSection.addProperty("rimehare", wildlife.rimehare());
		root.add("creatures", creaturesSection);
		return GSON.toJson(root) + "\n";
	}

	/**
	 * The file's text with every setting it lacks added at its default, so a file written by an older
	 * version shows the newer settings too; empty when nothing is missing, or when the file isn't
	 * settings at all (broken JSON is left for its owner to fix). Nothing already there changes: an
	 * owner's values, unknown keys and notes all stay. A section that gains a setting gets its current
	 * {@code _about} as well, since that describes the new setting. A file with comments in it (which
	 * {@link #parse} reads, leniently) is left alone too: writing it out again would lose them.
	 */
	public static java.util.Optional<String> addMissing(String json) {
		JsonObject root;
		try {
			// Strictly: no comments or other leniencies, which a rewrite couldn't keep.
			com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new java.io.StringReader(json));
			JsonElement element = GSON.getAdapter(JsonElement.class).read(reader);
			if (element == null || !element.isJsonObject() || reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
				return java.util.Optional.empty();
			}
			root = element.getAsJsonObject();
		} catch (JsonParseException | java.io.IOException | IllegalStateException e) {
			return java.util.Optional.empty();
		}
		JsonObject defaults = JsonParser.parseString(DEFAULTS.toJson()).getAsJsonObject();
		boolean added = false;
		for (Map.Entry<String, Set<String>> section : KEYS.entrySet()) {
			JsonObject fresh = defaults.getAsJsonObject(section.getKey());
			JsonElement have = root.get(section.getKey());
			if (have == null) {
				root.add(section.getKey(), fresh);
				added = true;
				continue;
			}
			if (!have.isJsonObject()) {
				// Already warned about; the owner's file is theirs to fix.
				continue;
			}
			JsonObject theirs = have.getAsJsonObject();
			boolean grew = false;
			for (String key : fresh.keySet()) {
				if (section.getValue().contains(key) && !theirs.has(key)) {
					theirs.add(key, fresh.get(key));
					grew = true;
				}
			}
			if (grew && fresh.has("_about")) {
				theirs.add("_about", fresh.get("_about"));
			}
			added |= grew;
		}
		return added ? java.util.Optional.of(GSON.toJson(root) + "\n") : java.util.Optional.empty();
	}

	// ------------------------------------------------------------------ derived

	/** A chance out of 100, scaled by a multiplier and kept within 0-100. */
	public static int scaledChance(int chance, double multiplier) {
		return (int) Math.max(0, Math.min(100, Math.round(chance * multiplier)));
	}
}
