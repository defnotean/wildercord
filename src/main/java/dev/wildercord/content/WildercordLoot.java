package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneSources;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where magic is found. Structure chests get one extra roll for a rune (and some for a Mana
 * Crystal); mobs and bosses drop the runes that fit them; fishing brings up runes too (see
 * {@link FishingRules}). Only vanilla's own tables are touched, so a datapack that replaces a
 * table keeps full control of it.
 */
public final class WildercordLoot {
	private WildercordLoot() {}

	/** A chance (out of 100) of one rune, picked evenly from the list. */
	private record RunePool(int chance, List<RuneDef> runes) {}

	private static final Map<ResourceKey<LootTable>, Integer> CRYSTAL_CHANCE = Map.of(
		BuiltInLootTables.ANCIENT_CITY, 30,
		BuiltInLootTables.END_CITY_TREASURE, 25,
		BuiltInLootTables.STRONGHOLD_LIBRARY, 30,
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, 25,
		BuiltInLootTables.BURIED_TREASURE, 20,
		BuiltInLootTables.BASTION_TREASURE, 25,
		BuiltInLootTables.WOODLAND_MANSION, 20
	);

	/** Torn Pages (riddles of secret spells): out of 100, in the old libraries and ruins of the world. */
	private static final Map<ResourceKey<LootTable>, Integer> PAGE_CHANCE = Map.of(
		BuiltInLootTables.STRONGHOLD_LIBRARY, 45,
		BuiltInLootTables.ANCIENT_CITY, 25,
		BuiltInLootTables.SIMPLE_DUNGEON, 12,
		BuiltInLootTables.WOODLAND_MANSION, 25,
		BuiltInLootTables.DESERT_PYRAMID, 12,
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, 15
	);

	private static final Map<ResourceKey<LootTable>, RunePool> RUNE_POOLS = new HashMap<>();
	/** Archaeology tables give one find per brush: their runes are added to vanilla's pool instead of a pool of their own. */
	private static final Map<ResourceKey<LootTable>, RunePool> ARCHAEOLOGY_POOLS = new HashMap<>();

	static {
		List<RuneDef> common = List.of(Runes.TOUCH, Runes.FEATHER_FALL, Runes.SWIFT, Runes.NIGHT_EYE, Runes.HEAL, Runes.HARM, Runes.LIGHT, Runes.GROW,
			Runes.AMPLIFY, Runes.EXTEND, Runes.DELAY, Runes.ARC, Runes.SHOCK, Runes.HASTE, Runes.REVEAL, Runes.FRUGAL_MOD,
			Runes.BLIND, Runes.CHILL, Runes.NOURISH, Runes.TIDEBREATH, Runes.LEAP, Runes.HARVEST, Runes.ICEPATH, Runes.COLLECT,
			Runes.RAMPART, Runes.WEIGH, Runes.AFTERSHOCK, Runes.SWAP);
		List<RuneDef> uncommon = List.of(Runes.BEAM, Runes.BURST, Runes.CONE, Runes.TRAIL, Runes.SHIELD, Runes.LAUNCH, Runes.DASH, Runes.PULL, Runes.FIRE,
			Runes.FROST, Runes.BREAK, Runes.REGROWTH, Runes.CLEANSE, Runes.STONESKIN, Runes.ROOT, Runes.VEIL, Runes.EMPOWER, Runes.LEVITATE,
			Runes.WIDEN, Runes.QUICKEN, Runes.PIERCE_MOD, Runes.BOUNCE_MOD, Runes.LINGER_MOD, Runes.VOLLEY_MOD, Runes.ON_HIT, Runes.ON_LAND,
			Runes.PULSE, Runes.ON_HURT, Runes.RING, Runes.PILLAR, Runes.WAVE, Runes.MINE, Runes.VENOM, Runes.THUNDERCLAP, Runes.SILENCE,
			Runes.FIREWARD, Runes.GRAPPLE, Runes.EXCAVATE, Runes.FOCUS_MOD, Runes.RAPID_MOD, Runes.IF_SNEAKING,
			Runes.CRESCENT, Runes.BARRAGE, Runes.BLITZ, Runes.DISMANTLE, Runes.RIPPLE, Runes.REPEL, Runes.DECREE, Runes.SHACKLE, Runes.BUBBLE,
			Runes.OVERDRIVE, Runes.ZIPPER, Runes.EXECUTE_MOD, Runes.IF_AIRBORNE, Runes.ACCELERATE, Runes.FORESIGHT);
		RUNE_POOLS.put(BuiltInLootTables.SIMPLE_DUNGEON, new RunePool(35, common));
		RUNE_POOLS.put(BuiltInLootTables.ABANDONED_MINESHAFT, new RunePool(25, common));
		RUNE_POOLS.put(BuiltInLootTables.SHIPWRECK_TREASURE, new RunePool(30, uncommon));
		RUNE_POOLS.put(BuiltInLootTables.BURIED_TREASURE, new RunePool(40, uncommon));
		RUNE_POOLS.put(BuiltInLootTables.DESERT_PYRAMID, new RunePool(35, List.of(Runes.EXPLODE, Runes.METEOR, Runes.FIRE, Runes.CONE, Runes.BURST, Runes.INFERNO,
			Runes.PRIMER)));
		RUNE_POOLS.put(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON, new RunePool(30, uncommon));
		RUNE_POOLS.put(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, new RunePool(45,
			List.of(Runes.ZONE, Runes.SPLIT_MOD, Runes.CHAIN_MOD, Runes.WALL, Runes.ORBIT, Runes.FREEZE, Runes.VOLLEY_MOD, Runes.ON_HURT,
				Runes.COMBO, Runes.REFLECT, Runes.PRIMER, Runes.ON_REACTION, Runes.SOAR)));
		RUNE_POOLS.put(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE, new RunePool(70,
			List.of(Runes.ZONE, Runes.SPLIT_MOD, Runes.CHAIN_MOD, Runes.WALL, Runes.ORBIT, Runes.FREEZE, Runes.TREMOR, Runes.GRAVITY_WELL,
				Runes.TOTEM, Runes.OVERCHARGE_MOD, Runes.ORB, Runes.BLACKSPARK, Runes.VOW_MOD, Runes.INFINITY, Runes.REVERSAL)));
		RUNE_POOLS.put(BuiltInLootTables.ANCIENT_CITY, new RunePool(45,
			List.of(Runes.ZONE, Runes.SPLIT_MOD, Runes.CHAIN_MOD, Runes.ECHO, Runes.FREEZE, Runes.GRAVITY_WELL, Runes.VEIL,
				Runes.ON_LOW_HEALTH, Runes.SMITE, Runes.SHADES, Runes.SHADOWSTEP, Runes.DOMAIN, Runes.VOW_MOD, Runes.BLACKFLAME, Runes.DROWSE)));
		RUNE_POOLS.put(BuiltInLootTables.END_CITY_TREASURE, new RunePool(45,
			List.of(Runes.RAIN, Runes.HOMING_MOD, Runes.BLINK, Runes.LEVITATE, Runes.GRAVITY_WELL, Runes.ORBIT, Runes.ORB, Runes.TIME_SKIP,
				Runes.STASIS, Runes.REWIND, Runes.PROLONG, Runes.SOAR)));
		RUNE_POOLS.put(BuiltInLootTables.STRONGHOLD_LIBRARY, new RunePool(50,
			List.of(Runes.RAIN, Runes.HOMING_MOD, Runes.WALL, Runes.PULSE, Runes.ECHO, Runes.LINGER_MOD, Runes.SMITE, Runes.TOTEM,
				Runes.RESONANCE, Runes.FORESIGHT, Runes.RESTORE)));
		RUNE_POOLS.put(BuiltInLootTables.BASTION_TREASURE, new RunePool(45,
			List.of(Runes.ON_KILL, Runes.EXPLODE, Runes.METEOR, Runes.TREMOR, Runes.EMPOWER, Runes.INFERNO, Runes.OVERCHARGE_MOD,
				Runes.CLEAVE, Runes.BLACKSPARK, Runes.BLACKFLAME, Runes.OVERDRIVE, Runes.BLOOD_PRICE_MOD)));
		RUNE_POOLS.put(BuiltInLootTables.WOODLAND_MANSION, new RunePool(40,
			List.of(Runes.ON_KILL, Runes.VEIL, Runes.PULSE, Runes.ORBIT, Runes.CLEAVE, Runes.RESONANCE, Runes.SHADOWSTEP, Runes.BLOOD_PRICE_MOD)));
		ARCHAEOLOGY_POOLS.put(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE, new RunePool(20, List.of(Runes.LIGHTNING, Runes.SHOCK)));
		// ---- shapes pack: the field and kin shapes turn up where their work is done (and are all crafted too).
		// Merged, not put, so another pack's pool on the same chest is kept beside this one.
		packPool(BuiltInLootTables.VILLAGE_PLAINS_HOUSE, 20, List.of(Runes.FURROW, Runes.PLOT, Runes.SEEDBED, Runes.HEDGEROW,
			Runes.LATTICE, Runes.HERD, Runes.NURSERY, Runes.SADDLE, Runes.PACKBOND, Runes.FELLOWSHIP));
		packPool(BuiltInLootTables.VILLAGE_TOOLSMITH, 30, List.of(Runes.SHAFT, Runes.STAIRWELL, Runes.CORRIDOR, Runes.SEAM,
			Runes.LODESEEK, Runes.PIT, Runes.VAULT, Runes.COLLAPSE, Runes.FISSURE, Runes.SPIRE));
		packPool(BuiltInLootTables.VILLAGE_MASON, 30, List.of(Runes.FACADE, Runes.DOME, Runes.FOOTING, Runes.CANOPY,
			Runes.PERIMETER, Runes.CROSSWAY, Runes.LAMPLIT, Runes.SPIRAL, Runes.ROSETTE, Runes.STEPSTONES, Runes.CAUSEWAY));
		packPool(BuiltInLootTables.SHIPWRECK_SUPPLY, 20, List.of(Runes.SHORELINE, Runes.BOBBER, Runes.SHOAL, Runes.FAN));
		packPool(BuiltInLootTables.PILLAGER_OUTPOST, 25, List.of(Runes.REARGUARD, Runes.GRUDGE, Runes.SENTINEL, Runes.AUREOLE,
			Runes.TETHER, Runes.FLOCK));
	}

	// ---- shapes pack (shared by every pack: merged, so packs on the same chest keep each other's runes)
	private static void packPool(ResourceKey<LootTable> table, int chance, List<RuneDef> runes) {
		RUNE_POOLS.merge(table, new RunePool(chance, runes), (a, b) -> new RunePool(Math.max(a.chance(), b.chance()),
			java.util.stream.Stream.concat(a.runes().stream(), b.runes().stream()).distinct().toList()));
	}

	// ---- fx-passive pack: the hearth runes (cast/HearthEffects), found in homes, camps and travellers' chests.
	static {
		List<RuneDef> hearthHome = List.of(Runes.SLOWBURN, Runes.WARM_CLOAK, Runes.SOFTSOLE, Runes.LULLABY, Runes.ORBCALL, Runes.LANTERN_SOUL,
			Runes.DEW_DRINK, Runes.SUNBASK, Runes.TRAILBLAZE, Runes.HEARTHPATH, Runes.LUCKCHARM, Runes.SAVOR, Runes.STEEDMEND, Runes.PETWARD,
			Runes.WHISTLE, Runes.CLOT, Runes.QUENCH, Runes.EMBER_REST);
		List<RuneDef> hearthRoad = List.of(Runes.WAYMARK, Runes.LODESTAR, Runes.STARCHART, Runes.SKYREAD, Runes.GRAVEFINDER, Runes.LOSTFIND,
			Runes.LANDREAD, Runes.SPRINGSEEK, Runes.SMOKE_SIGNAL, Runes.STEEDSONG, Runes.CAMP_WARD, Runes.HOLLOW_POCKET, Runes.SUREFOOT,
			Runes.NIGHTWATCH, Runes.WAYFARER_HYMN, Runes.GLIDEWIND, Runes.HOMEWARD);
		List<RuneDef> hearthCraft = List.of(Runes.TINKER_HUM, Runes.KEENKEEP, Runes.LONG_ARM, Runes.SUREFOOT, Runes.DEEPWARN, Runes.DYNAMO_STRIDE,
			Runes.STILLWELL, Runes.TARRY, Runes.HEARTSENSE, Runes.ENDERHUSH, Runes.SOFTFOOT, Runes.CURRENTKIN, Runes.HEARTHBOND, Runes.RALLY_LIGHT);
		packPool(BuiltInLootTables.VILLAGE_PLAINS_HOUSE, 12, hearthHome);
		packPool(BuiltInLootTables.VILLAGE_TAIGA_HOUSE, 12, hearthHome);
		packPool(BuiltInLootTables.VILLAGE_SNOWY_HOUSE, 12, hearthHome);
		packPool(BuiltInLootTables.VILLAGE_CARTOGRAPHER, 25, hearthRoad);
		packPool(BuiltInLootTables.SHIPWRECK_MAP, 20, hearthRoad);
		packPool(BuiltInLootTables.VILLAGE_TOOLSMITH, 15, hearthCraft);
		packPool(BuiltInLootTables.VILLAGE_TEMPLE, 15, hearthCraft);
	}

	// ---- fx-fish pack
	static {
		// The runes of the coast and sea, in the chests of the sea and its fishers. Ocean's Favor is never crafted: ruins only.
		packPool(BuiltInLootTables.UNDERWATER_RUIN_SMALL, 20, List.of(Runes.AIR_POCKET, Runes.TIDE_LANTERN, Runes.KELPSONG,
			Runes.UPWELL, Runes.SOUNDING, Runes.REFLOAT, Runes.CORAL_MEND, Runes.PEARL_SIGHT, Runes.DROWN_WARD, Runes.DIVERS_HANDS));
		packPool(BuiltInLootTables.UNDERWATER_RUIN_BIG, 30, List.of(Runes.CORAL_MEND, Runes.PEARL_SIGHT, Runes.DROWN_WARD,
			Runes.INKVEIL, Runes.PORPOISE, Runes.DOLPHIN_CALL, Runes.WRECK_SENSE, Runes.DIVING_BELL, Runes.OCEANS_FAVOR));
		packPool(BuiltInLootTables.SHIPWRECK_SUPPLY, 20, List.of(Runes.MOORING_CALL, Runes.FAIR_WIND, Runes.TIDE_MARKER,
			Runes.SHORE_SENSE, Runes.SEA_BREEZE, Runes.SHELLBACK, Runes.SKIMSTEP, Runes.SANDBAR, Runes.LILY_PATH, Runes.DEWCATCH));
		packPool(BuiltInLootTables.VILLAGE_FISHER, 30, List.of(Runes.ANGLER_LURE, Runes.BAIT_BLESSING, Runes.REELING_TIDE,
			Runes.TACKLE_MEND, Runes.BOBBER_BELL, Runes.WATER_READING, Runes.REED_CUT, Runes.SPRING_DRAW, Runes.BRIMMING, Runes.WRING,
			Runes.SLUICE, Runes.SOAK_THROUGH, Runes.RAIN_CLOUD, Runes.NEST_TEND, Runes.AXOLOTL_KINSHIP, Runes.SKATERS_EDGE));
		// ---- fx-farm pack: the farmstead runes turn up in village houses and the shepherd's and butcher's chests
		// (Cloche and the Feast, Tier IV, at the lowest tier weight).
		List<RuneDef> farmstead = List.of(Runes.TILLAGE, Runes.DEWFALL, Runes.TILTH, Runes.PLOWLINE, Runes.SOW, Runes.RIPEN, Runes.DEWKEEP,
			Runes.FIELDSENSE, Runes.THAWFIELD, Runes.CLOCHE, Runes.SCARECROW, Runes.FALLOW, Runes.DITCHWATER, Runes.COMPOST, Runes.STALKRISE,
			Runes.GOURDCALL, Runes.BERRYBLESS, Runes.COURTSHIP, Runes.HERDCALL, Runes.FLEECE, Runes.MILKMAID, Runes.HENHOUSE, Runes.GENTLEHAND,
			Runes.FODDER, Runes.BARNWARMTH, Runes.HERDSENSE, Runes.HEARTHCOOK, Runes.STEWPOT, Runes.BAKEHOUSE, Runes.POLLINATE, Runes.HIVEHUM,
			Runes.CALMSMOKE, Runes.WILDFLOWER, Runes.SAPLINGRISE, Runes.SAPLINGSOW, Runes.LEAFFALL, Runes.BARKSTRIP, Runes.COPPICE,
			Runes.FEASTDAY, Runes.PICNIC, Runes.HONEYDEW, Runes.LEAFSHADE, Runes.BARKHIDE, Runes.SAPFLOW, Runes.TROT, Runes.BEELINE,
			Runes.FIELDSTRIDE, Runes.HAYLOFT);
		packPool(BuiltInLootTables.VILLAGE_PLAINS_HOUSE, 20, farmstead);
		packPool(BuiltInLootTables.VILLAGE_SAVANNA_HOUSE, 20, farmstead);
		packPool(BuiltInLootTables.VILLAGE_TAIGA_HOUSE, 20, farmstead);
		packPool(BuiltInLootTables.VILLAGE_SHEPHERD, 25, farmstead);
		packPool(BuiltInLootTables.VILLAGE_BUTCHER, 25, farmstead);
		// ---- fx-mine pack: the delving runes in the village workshops that use them (their Tier IV two come from the Archives).
		packPool(BuiltInLootTables.VILLAGE_TOOLSMITH, 20,
			List.of(Runes.STAIRDELVE, Runes.RISER, Runes.PLUMBLINE, Runes.SIFTFALL, Runes.GANGUE, Runes.OREPLUCK, Runes.LUCKSTRIKE,
			Runes.SILKLIFT, Runes.DEEPSOUND, Runes.ORETALLY, Runes.LAVASEAL, Runes.DEEPWAY, Runes.HOLLOWSENSE, Runes.KILNBAKE,
			Runes.BLOCKPACK, Runes.UNPACK, Runes.MILLSTONE, Runes.TOOLMEND, Runes.CAVEWARD));
		packPool(BuiltInLootTables.VILLAGE_MASON, 20,
			List.of(Runes.LEVELGROUND, Runes.HOLEFILL, Runes.SHOREUP, Runes.STILT, Runes.PLANKWAY, Runes.POLISH, Runes.BRICKWORK,
			Runes.AGESTONE, Runes.CONCRETESET, Runes.CHALKLINE, Runes.PITFLOOR, Runes.FLOORLAY));
		packPool(BuiltInLootTables.VILLAGE_TEMPLE, 20,
			List.of(Runes.TORCHFALL, Runes.GLOOMSIGHT, Runes.LUMENPATH, Runes.SNUFFOUT, Runes.HEADLAMP, Runes.LEVERFLIP, Runes.BUTTONPUSH,
			Runes.DOORCALL, Runes.CHESTSORT, Runes.STOW, Runes.RESTOCK, Runes.STOCKTAKE, Runes.UNBURDEN, Runes.LODEPULL,
			Runes.PACKTIDY));
	}

	// ---- links-mods pack: the hearth pack's everyday runes, found in village houses' chests (one roll per chest).
	private static final List<RuneDef> HEARTH = List.of(
		Runes.TIDY, Runes.REPLANTING, Runes.KILNED, Runes.SILKEN, Runes.WINDFALL, Runes.VEINFOLLOW, Runes.TIMBERING, Runes.LEVEL_GROUND,
		Runes.STEADY, Runes.DAMP, Runes.MAGNETIC, Runes.SOWING, Runes.FURROWING, Runes.FERTILE, Runes.TORCHSET, Runes.ORE_SENSING, Runes.FETCHING,
		Runes.BOUNTIFUL, Runes.CULLING, Runes.HEADHUNTING, Runes.HALLOWED, Runes.TAPERING, Runes.POOLED, Runes.SUNLIT, Runes.GENTLE,
		Runes.SPARING, Runes.SOOTHING, Runes.CUSHIONED, Runes.MENDING_MOD, Runes.NOURISHING, Runes.PURIFYING, Runes.MATCHMAKING, Runes.FLEECING,
		Runes.INWARD, Runes.SELFLESS, Runes.TRIAGE, Runes.IF_NIGHT, Runes.IF_DAY, Runes.IF_RAINING, Runes.IF_UNDERGROUND, Runes.IF_ALONE,
		Runes.IF_NEAR_ALLY, Runes.IF_UNHURT, Runes.IF_HOLDING_TOOL, Runes.IF_BRIMMING, Runes.IF_IN_FIELDS, Runes.ON_MINE, Runes.ON_HARVEST,
		Runes.ON_CATCH, Runes.ON_SPRINT, Runes.ON_SPLASH, Runes.ON_MOUNT, Runes.ON_WAKE);

	static {
		packPool(BuiltInLootTables.VILLAGE_PLAINS_HOUSE, 20, HEARTH);
		packPool(BuiltInLootTables.VILLAGE_TAIGA_HOUSE, 20, HEARTH);
		packPool(BuiltInLootTables.VILLAGE_SAVANNA_HOUSE, 20, HEARTH);
		packPool(BuiltInLootTables.VILLAGE_SNOWY_HOUSE, 20, HEARTH);
		packPool(BuiltInLootTables.VILLAGE_DESERT_HOUSE, 20, HEARTH);
		// ---- fx-explore pack: the wayfarer's runes, in the chests of mapmakers, temples, wrecks and far realms.
		packPool(BuiltInLootTables.VILLAGE_CARTOGRAPHER, 40, List.of(Runes.LAND_READING, Runes.SPAWN_BEARING, Runes.HOME_BEARING,
			Runes.VILLAGE_SENSE, Runes.CHALK_LINE, Runes.FOLK_CENSUS, Runes.GLYPH_CARVE, Runes.TRAIL_BLAZE, Runes.SKY_READING, Runes.SUN_READING,
			Runes.MOON_READING));
		packPool(BuiltInLootTables.SHIPWRECK_MAP, 30, List.of(Runes.SHIPWRECK_SENSE, Runes.RUIN_SENSE, Runes.PORTAL_RECKONING,
			Runes.GRAVE_BEARING, Runes.LUX_READING, Runes.SLIME_SENSE, Runes.DEPTH_SOUNDING));
		packPool(BuiltInLootTables.VILLAGE_TEMPLE, 30, List.of(Runes.APPRAISE, Runes.TRADE_RENEW, Runes.HAGGLE, Runes.FOLK_CALL,
			Runes.POTION_STEEP, Runes.QUICKBREW, Runes.LORE_READING, Runes.SHELF_COUNT, Runes.LAPIS_THRIFT, Runes.SIGN_GLOW));
		packPool(BuiltInLootTables.VILLAGE_SHEPHERD, 30, List.of(Runes.DYE_WASH, Runes.CHECKER_DYE, Runes.LAMPLIGHTER,
			Runes.SNUFF_OUT, Runes.STAND_POSE, Runes.FRAME_VEIL));
		packPool(BuiltInLootTables.RUINED_PORTAL, 20, List.of(Runes.PORTAL_SENSE, Runes.LAVA_CRUST, Runes.LAVA_SENSE,
			Runes.GOLD_PARLEY));
		packPool(BuiltInLootTables.NETHER_BRIDGE, 25, List.of(Runes.FORTRESS_SENSE, Runes.LAVA_CRUST, Runes.LAVA_SENSE,
			Runes.SPAWNER_SENSE, Runes.GOLD_PARLEY));
		packPool(BuiltInLootTables.STRONGHOLD_CORRIDOR, 25, List.of(Runes.STRONGHOLD_COMPASS, Runes.SPIRE_SENSE, Runes.VOID_STEP,
			Runes.BEACON_SWELL, Runes.SPAWNER_SENSE));
		ARCHAEOLOGY_POOLS.put(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, new RunePool(10, List.of(Runes.RELIC_SENSE, Runes.STEADY_BRUSH)));
	}

	/**
	 * The runes of the world (see {@link RuneSources}): a chance (out of 100) that a structure's chest
	 * rolls one of the runes found only there. Its own pool, on top of the ones above.
	 */
	private record SourcePool(int chance, RuneSources.Source source) {}

	private static final Map<ResourceKey<LootTable>, List<SourcePool>> SOURCE_POOLS = new HashMap<>();
	/** The same for archaeology: one find per brush, so the runes join vanilla's own pool. */
	private static final Map<ResourceKey<LootTable>, SourcePool> SOURCE_DIGS = new HashMap<>();

	private static void sourcePool(ResourceKey<LootTable> table, int chance, RuneSources.Source source) {
		SOURCE_POOLS.computeIfAbsent(table, k -> new java.util.ArrayList<>()).add(new SourcePool(chance, source));
	}

	static {
		sourcePool(BuiltInLootTables.ANCIENT_CITY, 20, RuneSources.ANCIENT_CITY);
		sourcePool(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, 12, RuneSources.TRIAL_VAULT);
		sourcePool(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE, 25, RuneSources.OMINOUS_VAULT);
		sourcePool(BuiltInLootTables.STRONGHOLD_LIBRARY, 25, RuneSources.STRONGHOLD);
		sourcePool(BuiltInLootTables.DESERT_PYRAMID, 20, RuneSources.DESERT_PYRAMID);
		sourcePool(BuiltInLootTables.JUNGLE_TEMPLE, 30, RuneSources.JUNGLE_TEMPLE);
		sourcePool(BuiltInLootTables.IGLOO_CHEST, 50, RuneSources.IGLOO);
		sourcePool(BuiltInLootTables.PILLAGER_OUTPOST, 25, RuneSources.PILLAGER_OUTPOST);
		sourcePool(BuiltInLootTables.WOODLAND_MANSION, 25, RuneSources.WOODLAND_MANSION);
		sourcePool(BuiltInLootTables.SHIPWRECK_TREASURE, 20, RuneSources.SHIPWRECK);
		sourcePool(BuiltInLootTables.BURIED_TREASURE, 30, RuneSources.BURIED_TREASURE);
		sourcePool(BuiltInLootTables.BASTION_TREASURE, 25, RuneSources.BASTION);
		sourcePool(BuiltInLootTables.BASTION_OTHER, 8, RuneSources.BASTION);
		sourcePool(BuiltInLootTables.NETHER_BRIDGE, 20, RuneSources.NETHER_FORTRESS);
		sourcePool(BuiltInLootTables.END_CITY_TREASURE, 20, RuneSources.END_CITY);
		sourcePool(BuiltInLootTables.RUINED_PORTAL, 15, RuneSources.RUINED_PORTAL);
		SOURCE_DIGS.put(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, new SourcePool(8, RuneSources.TRAIL_RUINS));
	}

	// ---- fx-support pack
	// The support pack: healers' and keepers' runes in village temples and houses, guards' runes with the smiths,
	// and the crowd-calming ones in outposts and igloos. A table some other pack also fills keeps both lists.
	private static void supportPool(ResourceKey<LootTable> table, int chance, RuneDef... runes) {
		RUNE_POOLS.merge(table, new RunePool(chance, List.of(runes)), (had, more) -> {
			List<RuneDef> both = new java.util.ArrayList<>(had.runes());
			both.addAll(more.runes());
			return new RunePool(Math.max(had.chance(), more.chance()), List.copyOf(both));
		});
	}

	static {
		supportPool(BuiltInLootTables.VILLAGE_TEMPLE, 35, Runes.WORST_FIRST, Runes.SALVE, Runes.MENDING_MIST, Runes.AFTERCARE, Runes.HEARTHSONG,
			Runes.MANAGIFT, Runes.MANAWELL, Runes.HEXGUARD, Runes.STAUNCH, Runes.SANCTUARY, Runes.SOOTHE, Runes.TRUCE, Runes.GRACE);
		supportPool(BuiltInLootTables.VILLAGE_PLAINS_HOUSE, 12, Runes.HEARTHGLOW, Runes.TEND, Runes.BEASTGUARD, Runes.HEEL, Runes.FAITHFUL,
			Runes.KEEPSAFE, Runes.FIREBREAK, Runes.HEARTHGUARD, Runes.BELLWARD);
		supportPool(BuiltInLootTables.VILLAGE_WEAPONSMITH, 20, Runes.GUARDLINK, Runes.RALLY, Runes.MORALE, Runes.STOUTHEART, Runes.IRONHOLD,
			Runes.EVADE, Runes.EMBERGUARD, Runes.SHIELDWALL, Runes.SENTRY, Runes.ARROWVEIL, Runes.BLASTWARD, Runes.AEGIS);
		supportPool(BuiltInLootTables.VILLAGE_ARMORER, 20, Runes.IRONHOLD, Runes.SHIELDWALL, Runes.ARROWVEIL, Runes.SHRUG_OFF, Runes.CITADEL);
		supportPool(BuiltInLootTables.PILLAGER_OUTPOST, 20, Runes.PACIFY, Runes.LURE, Runes.STILLBIND, Runes.TAUNT, Runes.NUDGE, Runes.HOBBLE,
			Runes.CORRAL, Runes.SPOOK, Runes.WITHDRAW, Runes.ACCORD);
		supportPool(BuiltInLootTables.IGLOO_CHEST, 25, Runes.HEARTHGLOW, Runes.SALVE, Runes.SOOTHE, Runes.PACIFY);
	}

	/**
	 * Fishing: the runes of the sea, crafted runes of water, frost and storm and a few a fisher is glad of, that a
	 * treasure catch can be (open water only) and magic waters can tangle in the line. The runes found only by fishing
	 * ({@link RuneSources#FISHING}) come up beside them, likelier than a sea rune of their tier (see {@link FishingRules}).
	 */
	private static final List<RuneDef> SEA = List.of(Runes.TIDEBREATH, Runes.CHILL, Runes.ICICLE, Runes.ICEPATH, Runes.SHOCK, Runes.FEATHER_FALL,
		Runes.NIGHT_EYE, Runes.SWIFT, Runes.HEAL, Runes.COLLECT, Runes.LEAP, Runes.BUBBLE, Runes.FROST, Runes.THUNDERCLAP, Runes.JOLT, Runes.PULL,
		Runes.GRAPPLE, Runes.LEVITATE, Runes.WAVE, Runes.FREEZE, Runes.LIGHTNING,
		// ---- fx-fish pack
		Runes.ANGLER_LURE, Runes.BAIT_BLESSING, Runes.REELING_TIDE, Runes.BOBBER_BELL, Runes.WATER_READING, Runes.TACKLE_MEND,
		Runes.SCHOOL_SIGHT, Runes.SHOAL_HERD, Runes.STORM_GLASS, Runes.FATHOM, Runes.ICE_AUGER, Runes.DRIFT_NET);

	/** The crafted runes a fishing line brings up beside the runes found only by fishing (see {@link #SEA}). */
	public static List<RuneDef> seaRunes() {
		return SEA;
	}

	/**
	 * One fished rune, as an inline loot table: a rune of the {@link #SEA} list or one found only by fishing, picked by
	 * tier, each fishing rune {@code worldWeight} times as likely as a sea rune of its tier.
	 */
	private static LootTable fishedRunes(int worldWeight) {
		LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
		for (RuneDef rune : SEA) {
			pool.add(runeEntry(rune).setWeight(tierWeight(rune.tier())));
		}
		for (RuneDef rune : RuneSources.FISHING.runes()) {
			pool.add(runeEntry(rune).setWeight(tierWeight(rune.tier()) * worldWeight));
		}
		return LootTable.lootTable().setParamSet(LootContextParamSets.FISHING).withPool(pool).build();
	}

	/** Ocean monuments have no chests: their Elder Guardians carry the monument's runes (chance out of 100). */
	private static final Map<EntityType<?>, SourcePool> SOURCE_GUARDIANS = Map.of(
		EntityTypes.ELDER_GUARDIAN, new SourcePool(50, RuneSources.OCEAN_MONUMENT)
	);

	/** A chance in 100 that a slain Runebound Adept's Cord gives up a rune of the world. */
	public static final int ADEPT_FIND_CHANCE = 8;

	/** {@link #ADEPT_FIND_CHANCE} times the server's rune loot multiplier. */
	public static int adeptFindChance() {
		return scaled(ADEPT_FIND_CHANCE, dev.wildercord.config.Config.get().runeLootChance());
	}

	/**
	 * One rune found at {@code source} (a {@link RuneSources} id, e.g. {@code ember_sanctum} or
	 * {@code starfall}), picked by tier like a chest picks, or an empty stack for an unknown source.
	 * For features that hand out their own runes: a dungeon vault, a boss, a world event.
	 */
	public static ItemStack foundRune(String source, net.minecraft.util.RandomSource random) {
		List<RuneDef> runes = RuneSources.forSource(source);
		if (runes.isEmpty()) {
			return ItemStack.EMPTY;
		}
		int total = runes.stream().mapToInt(r -> tierWeight(r.tier())).sum();
		int roll = random.nextInt(total);
		for (RuneDef rune : runes) {
			roll -= tierWeight(rune.tier());
			if (roll < 0) {
				return RuneItem.stack(rune);
			}
		}
		return RuneItem.stack(runes.getLast());
	}

	/** A loot pool of one rune found at {@code source} (tier-weighted), rolled {@code chance} times in 100. */
	public static LootPool.Builder foundRunePool(String source, int chance) {
		LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
		List<RuneDef> runes = RuneSources.forSource(source);
		int total = chance * 10;
		int weights = Math.max(1, runes.stream().mapToInt(r -> tierWeight(r.tier())).sum());
		for (RuneDef rune : runes) {
			builder.add(runeEntry(rune).setWeight(Math.max(1, Math.round((float) total * tierWeight(rune.tier()) / weights))));
		}
		if (chance < 100) {
			builder.add(EmptyLootItem.emptyItem().setWeight(Math.max(1, (100 - chance) * 10)));
		}
		return builder;
	}

	/** Mob drops: chance out of 100, and the rune. Bosses always drop their first. */
	private static final Map<EntityType<?>, List<Map.Entry<Integer, RuneDef>>> MOB_DROPS = Map.ofEntries(
		Map.entry(EntityTypes.CREEPER, List.of(Map.entry(1, Runes.EXPLODE))),
		Map.entry(EntityTypes.ENDERMAN, List.of(Map.entry(2, Runes.BLINK), Map.entry(1, Runes.TIME_SKIP))),
		Map.entry(EntityTypes.SHULKER, List.of(Map.entry(5, Runes.HOMING_MOD))),
		Map.entry(EntityTypes.EVOKER, List.of(Map.entry(15, Runes.SUMMON), Map.entry(10, Runes.DECREE))),
		Map.entry(EntityTypes.VINDICATOR, List.of(Map.entry(4, Runes.CLEAVE))),
		Map.entry(EntityTypes.WITCH, List.of(Map.entry(5, Runes.BUBBLE))),
		Map.entry(EntityTypes.PHANTOM, List.of(Map.entry(5, Runes.IF_AIRBORNE))),
		Map.entry(EntityTypes.BREEZE, List.of(Map.entry(8, Runes.REPEL))),
		Map.entry(EntityTypes.WARDEN, List.of(Map.entry(100, Runes.SONIC_BOOM), Map.entry(35, Runes.DOMAIN))),
		Map.entry(EntityTypes.WITHER, List.of(Map.entry(100, Runes.WITHER), Map.entry(50, Runes.HOLLOW))),
		Map.entry(EntityTypes.ELDER_GUARDIAN, List.of(Map.entry(100, Runes.STARFALL), Map.entry(25, Runes.STASIS))),
		// ---- fx-support pack: raiders carry the Tier IV wards.
		Map.entry(EntityTypes.RAVAGER, List.of(Map.entry(8, Runes.CITADEL), Map.entry(5, Runes.AEGIS))),
		Map.entry(EntityTypes.PILLAGER, List.of(Map.entry(1, Runes.ACCORD), Map.entry(1, Runes.GRACE)))
	);

	public static void init() {
		Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Wildercord.id("magic_waters"), MagicWatersCondition.MAP_CODEC);
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Wildercord.id("fished_rune"), FishedRuneFunction.MAP_CODEC);
		Map<ResourceKey<LootTable>, List<Map.Entry<Integer, RuneDef>>> mobTables = new HashMap<>();
		MOB_DROPS.forEach((type, drop) -> type.getDefaultLootTable().ifPresent(key -> mobTables.put(key, drop)));
		Map<ResourceKey<LootTable>, SourcePool> guardianTables = new HashMap<>();
		SOURCE_GUARDIANS.forEach((type, pool) -> type.getDefaultLootTable().ifPresent(key -> guardianTables.put(key, pool)));

		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			// The server's loot multipliers (config/wildercord.json) scale every chance here.
			dev.wildercord.config.WildercordConfig config = dev.wildercord.config.Config.get();
			Integer page = PAGE_CHANCE.get(key);
			if (page != null && scaled(page, config.pageLootChance()) > 0) {
				table.withPool(chance(scaled(page, config.pageLootChance()), LootItem.lootTableItem(WildercordItems.TORN_PAGE)));
			}
			Integer crystal = CRYSTAL_CHANCE.get(key);
			if (crystal != null && scaled(crystal, config.crystalLootChance()) > 0) {
				table.withPool(chance(scaled(crystal, config.crystalLootChance()), LootItem.lootTableItem(WildercordItems.MANA_CRYSTAL)));
			}
			RunePool found = ARCHAEOLOGY_POOLS.get(key);
			RunePool dig = found == null || scaled(found.chance(), config.runeLootChance()) <= 0 ? null
				: new RunePool(Math.min(95, scaled(found.chance(), config.runeLootChance())), found.runes());
			if (dig != null) {
				// Brushing gives exactly one find, so the runes join vanilla's own pool (12 finds of weight 1).
				int total = Math.max(1, Math.round(12.0F * dig.chance() / (100 - dig.chance())));
				int weights = dig.runes().stream().mapToInt(r -> tierWeight(r.tier())).sum();
				table.modifyPools(builder -> {
					for (RuneDef rune : dig.runes()) {
						builder.add(runeEntry(rune).setWeight(Math.max(1, Math.round((float) total * tierWeight(rune.tier()) / weights))));
					}
				});
			}
			RunePool listed = RUNE_POOLS.get(key);
			RunePool pool = listed == null || scaled(listed.chance(), config.runeLootChance()) <= 0 ? null
				: new RunePool(scaled(listed.chance(), config.runeLootChance()), listed.runes());
			if (pool != null) {
				LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
				// The pool's chance of a rune is shared out by tier: high tiers are much rarer finds.
				int total = pool.chance() * 10;
				int weights = pool.runes().stream().mapToInt(r -> tierWeight(r.tier())).sum();
				for (RuneDef rune : pool.runes()) {
					builder.add(runeEntry(rune).setWeight(Math.max(1, Math.round((float) total * tierWeight(rune.tier()) / weights))));
				}
				if (pool.chance() < 100) {
					builder.add(EmptyLootItem.emptyItem().setWeight((100 - pool.chance()) * 10));
				}
				table.withPool(builder);
			}
			for (Map.Entry<Integer, RuneDef> drop : mobTables.getOrDefault(key, List.of())) {
				// A boss's sure drop stays sure; everything else follows the multiplier.
				int odds = drop.getKey() >= 100 ? 100 : scaled(drop.getKey(), config.runeLootChance());
				if (odds > 0) {
					table.withPool(chance(odds, runeEntry(drop.getValue())));
				}
			}
			// The runes of the world: found only in these places, as often as the rune multiplier says.
			for (SourcePool sourcePool : SOURCE_POOLS.getOrDefault(key, List.of())) {
				int odds = scaled(sourcePool.chance(), config.runeLootChance());
				if (odds > 0) {
					table.withPool(foundRunePool(sourcePool.source().id(), odds));
				}
			}
			SourcePool guardian = guardianTables.get(key);
			int guardianOdds = guardian == null ? 0 : scaled(guardian.chance(), config.runeLootChance());
			if (guardianOdds > 0) {
				table.withPool(foundRunePool(guardian.source().id(), guardianOdds));
			}
			SourcePool dug = SOURCE_DIGS.get(key);
			int dugOdds = dug == null ? 0 : Math.min(95, scaled(dug.chance(), config.runeLootChance()));
			if (dugOdds > 0) {
				int total = Math.max(1, Math.round(12.0F * dugOdds / (100 - dugOdds)));
				List<RuneDef> runes = dug.source().runes();
				int weights = runes.stream().mapToInt(r -> tierWeight(r.tier())).sum();
				table.modifyPools(builder -> {
					for (RuneDef rune : runes) {
						builder.add(runeEntry(rune).setWeight(Math.max(1, Math.round((float) total * tierWeight(rune.tier()) / weights))));
					}
				});
			}
			// Fishing. A treasure catch is one item, so the rune and the page join vanilla's six treasures (weight 1 each).
			if (key.equals(BuiltInLootTables.FISHING_TREASURE)) {
				int[] weights = FishingRules.treasureWeights(scaled(FishingRules.TREASURE_RUNE_CHANCE, config.runeLootChance()),
					scaled(FishingRules.TREASURE_PAGE_CHANCE, config.pageLootChance()));
				if (weights[0] > 0 || weights[1] > 0) {
					table.modifyPools(builder -> {
						if (weights[0] > 0) {
							builder.add(NestedLootTable.inlineLootTable(fishedRunes(FishingRules.WORLD_WEIGHT_TREASURE)).setWeight(weights[0])
								.apply(() -> new FishedRuneFunction(false)));
						}
						if (weights[1] > 0) {
							builder.add(LootItem.lootTableItem(WildercordItems.TORN_PAGE).setWeight(weights[1]));
						}
					});
				}
			}
			// Magic waters: where magic runs strong at the bobber, a rune comes up tangled in the line, on top of the catch.
			if (key.equals(BuiltInLootTables.FISHING) && config.runeLootChance() > 0) {
				double multiplier = config.runeLootChance();
				table.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
					.add(NestedLootTable.inlineLootTable(fishedRunes(FishingRules.WORLD_WEIGHT_MAGIC)))
					.when(() -> new MagicWatersCondition(multiplier))
					.apply(() -> new FishedRuneFunction(true)));
			}
		});

		// The Ender Dragon has no loot table: its runes fall at the feet of whoever killed it (or the
		// nearest player), not where it dies, which is often over the void or the exit portal.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof EnderDragon && entity.level() instanceof ServerLevel level) {
				net.minecraft.world.entity.Entity at = source.getEntity() instanceof net.minecraft.world.entity.player.Player killer && killer.level() == level
					? killer : level.getNearestPlayer(entity, 256);
				if (at == null) {
					at = entity;
				}
				for (RuneDef rune : List.of(Runes.DRAGON_BREATH, Runes.INFINITY)) {
					ItemEntity drop = new ItemEntity(level, at.getX(), at.getY() + 0.5, at.getZ(), RuneItem.stack(rune));
					drop.setGlowingTag(true);
					drop.setUnlimitedLifetime();
					level.addFreshEntity(drop);
				}
			}
		});
	}

	private static int scaled(int chance, double multiplier) {
		return dev.wildercord.config.WildercordConfig.scaledChance(chance, multiplier);
	}

	/** How likely a rune of this tier is next to the others in its pool: Tier I 8, II 5, III 2, IV 1. */
	static int tierWeight(int tier) {
		return switch (tier) {
			case 1 -> 8;
			case 2 -> 5;
			case 3 -> 2;
			default -> 1;
		};
	}

	private static net.minecraft.world.level.storage.loot.entries.UniformContainerBase.Builder<?> runeEntry(RuneDef rune) {
		return LootItem.lootTableItem(WildercordItems.RUNE).apply(SetComponentsFunction.setComponent(WildercordComponents.RUNE, rune.id()));
	}

	private static LootPool.Builder chance(int chance, net.minecraft.world.level.storage.loot.entries.UniformContainerBase.Builder<?> entry) {
		LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(entry.setWeight(chance));
		if (chance < 100) {
			builder.add(EmptyLootItem.emptyItem().setWeight(100 - chance));
		}
		return builder;
	}
}
