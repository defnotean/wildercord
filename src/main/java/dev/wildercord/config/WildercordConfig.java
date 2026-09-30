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
 * {@code player_affinity}) are read by those features; each defaults to on.</p>
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
	DefenceSettings defence
) {
	public static final WildercordConfig DEFAULTS = new WildercordConfig(64, 32, true, 0.6, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 6, 12,
		true, true, true, true, true, true, true, 1.0, TravelSettings.DEFAULTS, DefenceSettings.DEFAULTS);

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
				r.number("defence", "armour_rate", d.defence.armourRate(), 0, 1)));
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
			"player_affinity"));
		KEYS.put("affinity", Set.of("gain_multiplier"));
		KEYS.put("travel", Set.of("enabled", "max_homes", "warmup_seconds", "cooldown_seconds", "rtp_cooldown_seconds", "rtp_radius", "tpa_timeout_seconds"));
		KEYS.put("defence", Set.of("spellguard", "spellguard_health", "spellguard_recharge_seconds", "max_bonus", "armour_rate"));
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
		features.addProperty("_about", "Switch whole features off: world events, duels, wild magic, world-changing magic, creature affinities, elemental climate and players' own affinities.");
		features.addProperty("world_events", worldEvents);
		features.addProperty("duels", duels);
		features.addProperty("wild_magic", wildMagic);
		features.addProperty("world_changing_magic", worldChangingMagic);
		features.addProperty("creature_affinities", creatureAffinities);
		features.addProperty("elemental_climate", elementalClimate);
		features.addProperty("player_affinity", playerAffinity);
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
