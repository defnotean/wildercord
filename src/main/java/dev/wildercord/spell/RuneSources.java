package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where the runes of the world are found. These runes are never crafted: each belongs to a place
 * (a vanilla structure, a biome through Attunement, one of Wildercord's dungeons or bosses, or a
 * world event), so exploring is how a spellbook grows. Pure data, like {@link Runes}: the loot
 * code, the tooltips (through tools/generate_assets.py, which reads the {@code source(...)} lines
 * below, so keep each on one line), the Grimoire and the tests all read it.
 *
 * <p>For features that hand runes out themselves (dungeon vaults, bosses, events),
 * {@link #forSource(String)} gives a source's runes by id; {@code WildercordLoot.foundRune} picks
 * one as an item stack.</p>
 */
public final class RuneSources {
	private RuneSources() {}

	/**
	 * A place runes are found.
	 *
	 * @param id    e.g. {@code ancient_city}, {@code ember_sanctum}, {@code attunement:cherry_grove}
	 * @param where how the tooltip names it, e.g. "Ancient cities"
	 */
	public record Source(String id, String where, List<RuneDef> runes) {}

	private static final Map<String, Source> ALL = new LinkedHashMap<>();

	// ---- Vanilla structures (loot injected into their chests by WildercordLoot)
	public static final Source ANCIENT_CITY = source("ancient_city", "Ancient cities", Runes.ECHOLOCATE, Runes.RESONANT_SHRIEK);
	public static final Source OCEAN_MONUMENT = source("ocean_monument", "Ocean monuments (Elder Guardians)", Runes.TIDECALL);
	public static final Source TRIAL_VAULT = source("trial_vault", "Trial vaults", Runes.TRIAL_KEY);
	public static final Source OMINOUS_VAULT = source("ominous_vault", "Ominous vaults", Runes.TRIAL_KEY, Runes.VORTEX);
	public static final Source STRONGHOLD = source("stronghold", "Stronghold libraries", Runes.INFEST, Runes.IF_WOUNDED);
	public static final Source DESERT_PYRAMID = source("desert_pyramid", "Desert pyramids", Runes.SANDSTORM);
	public static final Source JUNGLE_TEMPLE = source("jungle_temple", "Jungle temples", Runes.VINELASH, Runes.SNARE);
	public static final Source IGLOO = source("igloo", "Igloo basements", Runes.REMEDY);
	public static final Source PILLAGER_OUTPOST = source("pillager_outpost", "Pillager outposts", Runes.WARCRY);
	public static final Source WOODLAND_MANSION = source("woodland_mansion", "Woodland mansions", Runes.FANGS, Runes.IF_OUTNUMBERED);
	public static final Source TRAIL_RUINS = source("trail_ruins", "Trail ruins (brushing)", Runes.ANCIENT_SEED);
	public static final Source SHIPWRECK = source("shipwreck", "Shipwrecks", Runes.UNDERTOW);
	public static final Source BURIED_TREASURE = source("buried_treasure", "Buried treasure", Runes.TREASURE_SENSE);
	public static final Source BASTION = source("bastion", "Bastions", Runes.TUSK_CHARGE);
	public static final Source NETHER_FORTRESS = source("nether_fortress", "Nether fortresses", Runes.BLAZECALL);
	public static final Source END_CITY = source("end_city", "End cities", Runes.SHULKERSHELL);
	public static final Source RUINED_PORTAL = source("ruined_portal", "Ruined portals", Runes.PORTALFALL);

	// ---- Biomes: a Blank Rune attuned there (see Attunements for the conditions)
	public static final Source ATTUNE_CHERRY_GROVE = source("attunement:cherry_grove", "Attuned in a cherry grove", Runes.MOONPETAL);
	public static final Source ATTUNE_ICE_SPIKES = source("attunement:ice_spikes", "Attuned among ice spikes", Runes.HOARFROST);
	public static final Source ATTUNE_DEEP_DARK = source("attunement:deep_dark", "Attuned in the deep dark", Runes.HUSH);
	public static final Source ATTUNE_MUSHROOM_FIELDS = source("attunement:mushroom_fields", "Attuned in mushroom fields", Runes.SPOREBLOOM);
	public static final Source ATTUNE_BADLANDS = source("attunement:badlands", "Attuned in the badlands", Runes.SUNSCORCH);
	public static final Source ATTUNE_SWAMP = source("attunement:swamp", "Attuned in a swamp", Runes.MIRE);
	public static final Source ATTUNE_LUSH_CAVES = source("attunement:lush_caves", "Attuned in lush caves", Runes.GLOWVINE);
	public static final Source ATTUNE_MANGROVE_SWAMP = source("attunement:mangrove_swamp", "Attuned in a mangrove swamp", Runes.ROOTSNARE);
	public static final Source ATTUNE_DRIPSTONE_CAVES = source("attunement:dripstone_caves", "Attuned in dripstone caves", Runes.STALACTITE);
	public static final Source ATTUNE_PEAKS = source("attunement:peaks", "Attuned on a mountain peak", Runes.SUMMIT_WIND);
	public static final Source ATTUNE_SOUL_SAND_VALLEY = source("attunement:soul_sand_valley", "Attuned in a soul sand valley", Runes.SOULFIRE);
	public static final Source ATTUNE_WARPED_FOREST = source("attunement:warped_forest", "Attuned in a warped forest", Runes.WARP_STEP);
	public static final Source ATTUNE_CRIMSON_FOREST = source("attunement:crimson_forest", "Attuned in a crimson forest", Runes.BLOOD_MOSS);
	public static final Source ATTUNE_BASALT_DELTAS = source("attunement:basalt_deltas", "Attuned in the basalt deltas", Runes.BASALT_SURGE);
	public static final Source ATTUNE_OUTER_END = source("attunement:outer_end", "Attuned on the End's outer islands", Runes.STARLIGHT_TETHER);

	// ---- Wildercord's own places: dungeon vaults, their bosses, and world events
	public static final Source EMBER_SANCTUM = source("ember_sanctum", "The Ember Sanctum", Runes.CINDERBRAND, Runes.ASHEN_VEIL, Runes.KINDLED);
	public static final Source CINDER_WARDEN = source("cinder_warden", "the Cinder Warden", Runes.CINDERHEART);
	public static final Source ASTRAL_OBSERVATORY = source("astral_observatory", "The Astral Observatory", Runes.CONSTELLATION, Runes.ECLIPSE);
	public static final Source STAR_EATER = source("star_eater", "the Star Eater", Runes.STARMAW);
	public static final Source DROWNED_SCRIPTORIUM = source("drowned_scriptorium", "The Drowned Scriptorium", Runes.DROWNING_WORD, Runes.IF_WET);
	public static final Source TIDE_SCRIBE = source("tide_scribe", "the Tide Scribe", Runes.TIDEWRIT);
	public static final Source STARFALL = source("starfall", "Fallen Star craters", Runes.STARSHARD);
	public static final Source RIFT = source("rift", "Rift sieges", Runes.RIFTCALL, Runes.UNSTABLE);
	public static final Source RIFTCALLER = source("riftcaller", "the Riftcaller", Runes.UNSTABLE);
	public static final Source MANA_STORM = source("mana_storm", "Mana storms (a surge after 10 casts)", Runes.MANABURN, Runes.MANATIDE);

	// ---- Now and then elsewhere, too
	public static final Source ARCHIVE = source("archive", "Archive libraries", Runes.ECHOLOCATE, Runes.INFEST, Runes.FANGS, Runes.TREASURE_SENSE, Runes.IF_WOUNDED);
	public static final Source RUNEBOUND_ADEPT = source("runebound_adept", "Runebound Adepts", Runes.VINELASH, Runes.UNDERTOW, Runes.BLAZECALL, Runes.PORTALFALL, Runes.WARCRY);

	/** The runes found at {@code id} (empty for an unknown source). */
	public static List<RuneDef> forSource(String id) {
		Source source = ALL.get(id);
		return source == null ? List.of() : source.runes();
	}

	public static Optional<Source> source(String id) {
		return Optional.ofNullable(ALL.get(id));
	}

	public static java.util.Collection<Source> all() {
		return Collections.unmodifiableCollection(ALL.values());
	}

	/** Every place {@code rune} is found, in the order they're listed here. */
	public static List<Source> sourcesOf(RuneDef rune) {
		List<Source> out = new ArrayList<>();
		for (Source source : ALL.values()) {
			if (source.runes().contains(rune)) {
				out.add(source);
			}
		}
		return out;
	}

	/** Whether {@code rune} is one of the runes of the world: found in particular places, never crafted. */
	public static boolean foundOnly(RuneDef rune) {
		for (Source source : ALL.values()) {
			if (source.runes().contains(rune)) {
				return true;
			}
		}
		return false;
	}

	/** Every rune of the world, each once, in roster order. */
	public static List<RuneDef> runes() {
		List<RuneDef> out = new ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			if (foundOnly(rune)) {
				out.add(rune);
			}
		}
		return out;
	}

	private static Source source(String id, String where, RuneDef... runes) {
		Source source = new Source(id, where, List.of(runes));
		if (ALL.put(id, source) != null) {
			throw new IllegalStateException("Duplicate rune source " + id);
		}
		return source;
	}
}
