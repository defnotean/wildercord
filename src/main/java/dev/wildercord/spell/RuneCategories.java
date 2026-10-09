package dev.wildercord.spell;

import java.util.List;

/**
 * Sub-categories inside each family, so the Codex can be browsed by what a rune is for.
 * Order here is the order the Cord screen shows them in.
 */
public final class RuneCategories {
	private RuneCategories() {}

	public static final List<String> SHAPE = List.of("personal", "direct", "projectile", "area", "lingering",
		// ---- shapes pack
		"field", "kin");
	public static final List<String> EFFECT = List.of("damage", "control", "support", "movement", "time", "world", "summon", "innate");
	public static final List<String> MODIFIER = List.of("power", "area", "timing", "projectile", "circle");
	public static final List<String> LINK = List.of("timing", "trigger", "reactive", "condition");
	public static final List<String> KNOT = List.of("knot");

	/** Categories add-ons added (see {@code dev.wildercord.api}), shown after the built-in ones. */
	private static final java.util.Map<RuneFamily, List<String>> ADDED = new java.util.concurrent.ConcurrentHashMap<>();

	// ---- links-mods pack: the hearth modifiers' own categories, shown after the built-in ones
	static {
		add(RuneFamily.MODIFIER, "gathering");
		add(RuneFamily.MODIFIER, "tending");
	}

	public static List<String> of(RuneFamily family) {
		List<String> builtIn = switch (family) {
			case SHAPE -> SHAPE;
			case EFFECT -> EFFECT;
			case MODIFIER -> MODIFIER;
			case LINK -> LINK;
			case KNOT -> KNOT;
		};
		List<String> added = ADDED.get(family);
		if (added == null) {
			return builtIn;
		}
		List<String> all = new java.util.ArrayList<>(builtIn);
		all.addAll(added);
		return List.copyOf(all);
	}

	/** Adds a category to a family (a no-op if it's already there). Its Codex label is the lang key {@code category.wildercord.<family>.<category>}. */
	public static synchronized void add(RuneFamily family, String category) {
		if (of(family).contains(category)) {
			return;
		}
		List<String> added = new java.util.ArrayList<>(ADDED.getOrDefault(family, List.of()));
		added.add(category);
		ADDED.put(family, List.copyOf(added));
	}

	/** Sort position of a category within its family (unknown categories sort last). */
	public static int order(RuneDef rune) {
		int index = of(rune.family()).indexOf(rune.category());
		return index < 0 ? Integer.MAX_VALUE : index;
	}

	/** The built-in runes' categories. Add-on runes will declare their own. */
	static String categoryFor(String path, RuneFamily family) {
		return switch (family) {
			case SHAPE -> switch (path) {
				case "self", "orbit" -> "personal";
				case "touch", "beam", "barrage", "blitz", "ray", "lance", "sweep", "prism", "stream", "relay" -> "direct";
				case "bolt", "arc", "wave", "crescent", "orb", "spark", "wisp", "comet", "ricochet", "cluster" -> "projectile";
				// New runes (batch 2); Imprint waits where you stood, as a Mine does, so it's lingering.
				case "latch" -> "direct";
				case "glaive" -> "projectile";
				case "burst", "cone", "ring", "pillar", "rain", "nova", "constellation" -> "area";
				// ---- shapes pack
				case "furrow", "plot", "seedbed", "shaft", "stairwell", "corridor", "seam", "facade", "dome", "footing", "canopy", "shoreline",
					"perimeter", "spire", "pit", "crossway", "lodeseek", "vault", "lamplit", "fissure", "spiral", "rosette", "stepstones", "causeway",
					"hedgerow", "lattice", "collapse", "fan", "bobber" -> "field";
				case "herd", "fellowship", "saddle", "packbond", "nursery", "shoal", "rearguard", "grudge", "sentinel", "aureole", "tether", "flock" -> "kin";
				default -> "lingering";
			};
			case EFFECT -> switch (path) {
				case "push", "pull", "launch", "root", "freeze", "levitate", "gravity_well", "blind", "chill", "silence", "reveal",
					"decree", "weigh", "shackle", "bubble", "hex", "rend", "jolt", "banish", "cyclone",
					"echolocate", "undertow", "portalfall", "hush", "mire", "rootsnare", "starlight_tether", "sporebloom", "tidehook" -> "control";
				case "manabraid", "heal", "shield", "regrowth", "cleanse", "stoneskin", "empower", "haste", "swift", "night_eye", "feather_fall", "veil",
					"fireward", "nourish", "tidebreath", "leap", "infinity", "reversal", "reflect", "overdrive", "foresight", "restore",
					"barrier", "brace", "anchor", "bramble", "frostward", "cushion", "deflect", "haven",
					"remedy", "warcry", "treasure_sense", "shulkershell", "ashen_veil", "cinderheart", "manatide" -> "support";
				case "dash", "blink", "grapple", "swap", "zipper", "shadowstep", "tusk_charge", "warp_step", "current" -> "movement";
				case "stasis", "rewind", "accelerate", "time_skip" -> "time";
				case "light", "grow", "break", "harvest", "icepath", "basinfill", "collect", "excavate", "rampart", "chisel", "glimmer", "prune", "tunnel",
					"vein", "smelt", "fell", "span", "ancient_seed", "glowvine", "root_carry", "watchweft" -> "world";
				case "summon", "shades", "thunderbird" -> "summon";
				case "glacier", "nullify", "blizzard", "black_ice", "rime_seal", "absolute_zero", "magnetize", "heartstopper", "updraft", "recoil",
					"sinkhole", "fossilize", "timesteal" -> "control";
				case "bloom", "surge", "phoenix_pyre", "frostbloom", "cryostasis", "zephyr", "geode", "soulbond", "second_wind", "transfusion",
					"lifebloom" -> "support";
				case "warp", "skyglyph" -> "movement";
				case "chronoshift" -> "time";
				// New runes (batch 2); Spellbrand and Umbra are damage.
				case "gash", "flash_freeze", "drowse", "disarm" -> "control";
				case "searing_edge" -> "support";
				case "prospect", "galvanize" -> "world";
				case "prolong" -> "time";
				// Flight.
				case "soar", "wind_steps", "rime_causeway", "thunder_walk" -> "movement";
				case "strata_rise", "cinder_bulwark", "root_bulwark" -> "world";
				// Signature fusions (see Fusions.SIGNATURES); the rest of them are damage.
				case "seethe", "dust_devil", "malison" -> "control";
				case "stitchtime", "halo", "riposte" -> "support";
				case "bloomstep", "thunderstep" -> "movement";
				case "blood_thread", "kindling", "twin_star", "borrowed_time", "gale_mantle", "stoneform", "mirrorfrost", "fortune", "phantom",
					"stormheart" -> "innate";
				// ---- fx-passive pack
				case "slowburn", "warm_cloak", "softsole", "softfoot", "hollow_pocket", "gravefinder", "skyread", "lullaby", "steedsong", "orbcall",
					"tinker_hum", "lantern_soul", "keenkeep", "dew_drink", "sunbask", "currentkin", "surefoot", "long_arm", "nightwatch", "trailblaze",
					"hearthpath", "lostfind", "stillwell", "starchart", "petward", "whistle", "luckcharm", "wayfarer_hymn", "steedmend", "dynamo_stride",
					"clot", "heartsense", "quench", "hearthbond", "springseek", "savor", "deepwarn", "enderhush" -> "support";
				case "homeward", "glidewind" -> "movement";
				case "tarry" -> "time";
				case "camp_ward", "lodestar", "waymark", "ember_rest", "landread", "rally_light", "smoke_signal" -> "world";
				// ---- fx-fish pack
				case "angler_lure", "reeling_tide", "school_sight", "bobber_bell", "water_reading", "dolphin_call", "axolotl_kinship", "shoal_herd", "refloat", "reed_cut", "wring", "spring_draw", "brimming", "diving_bell", "tide_lantern", "sluice", "soak_through", "rain_cloud", "storm_glass", "kelpsong", "coral_mend", "nest_tend", "lily_path", "sandbar", "ice_auger", "tide_marker", "shore_sense", "fathom", "wreck_sense", "drift_net", "mooring_call", "dewcatch" -> "world";
				case "bait_blessing", "tackle_mend", "oceans_favor", "skimstep", "skaters_edge", "air_pocket", "drown_ward", "pearl_sight", "sea_breeze", "inkveil", "shellback", "divers_hands" -> "support";
				case "fair_wind", "upwell", "sounding", "porpoise" -> "movement";
				// ---- fx-farm pack: farming, husbandry, kitchen, hive and forestry runes (see cast/packs/FarmEffects).
				case "tillage", "dewfall", "tilth", "plowline", "sow", "ripen", "dewkeep", "fieldsense", "thawfield", "cloche", "scarecrow",
					"fallow", "ditchwater", "compost", "stalkrise", "gourdcall", "berrybless", "courtship", "herdcall", "fleece", "milkmaid",
					"henhouse", "gentlehand", "fodder", "barnwarmth", "herdsense", "hearthcook", "stewpot", "bakehouse", "pollinate", "hivehum",
					"calmsmoke", "wildflower", "saplingrise", "saplingsow", "leaffall", "barkstrip", "coppice" -> "world";
				case "feastday", "picnic", "honeydew", "leafshade", "barkhide", "sapflow" -> "support";
				case "trot", "beeline", "fieldstride", "hayloft" -> "movement";
				// ---- fx-mine pack
				case "stairdelve", "riser", "plumbline", "siftfall", "gangue", "orepluck", "luckstrike", "silklift", "deepsound", "oretally",
					"lavaseal", "deepway", "hollowsense", "kilnbake", "blockpack", "unpack", "millstone", "levelground", "holefill", "shoreup",
					"stilt", "plankway", "polish", "brickwork", "agestone", "concreteset", "chalkline", "pitfloor", "torchfall", "gloomsight",
					"lumenpath", "snuffout", "leverflip", "buttonpush", "doorcall", "chestsort", "stow", "restock", "stocktake", "unburden",
					"delvemark", "motherlode", "floorlay", "packtidy" -> "world";
				case "toolmend", "headlamp", "lodepull", "caveward" -> "support";
				// ---- fx-explore pack
				case "land_reading", "depth_sounding", "spawn_bearing", "home_bearing", "grave_bearing", "portal_reckoning", "slime_sense", "sky_reading",
					"moon_reading", "sun_reading", "lux_reading", "chalk_line", "village_sense", "ruin_sense", "shipwreck_sense", "portal_sense", "fortress_sense",
					"stronghold_compass", "spire_sense", "trail_blaze", "relic_sense", "spawner_sense", "steady_brush", "appraise", "trade_renew", "folk_call",
					"folk_census", "lore_reading", "shelf_count", "dye_wash", "checker_dye", "glyph_carve", "lamplighter", "snuff_out", "sign_glow",
					"frame_veil", "stand_pose", "lava_crust", "void_step", "lava_sense" -> "world";
				case "haggle", "lapis_thrift", "quickbrew", "potion_steep", "beacon_swell", "gold_parley" -> "support";
				// ---- fx-support pack
				case "worst_first", "salve", "mending_mist", "hearthglow", "aftercare", "hearthsong", "grace", "managift", "manawell", "guardlink", "rally",
					"morale", "shrug_off", "hexguard", "stoutheart", "ironhold", "evade", "emberguard", "beastguard", "hearthguard", "heel", "aegis",
					"shieldwall", "staunch", "sentry", "tend", "withdraw", "faithful" -> "support";
				case "pacify", "lure", "stillbind", "taunt", "nudge", "hobble", "corral", "truce", "spook", "soothe" -> "control";
				case "bellward", "sanctuary", "arrowveil", "blastward", "firebreak", "accord", "citadel", "keepsafe" -> "world";
				default -> "damage";
			};
			case MODIFIER -> path.endsWith("_circle") ? "circle" : switch (path) {
				case "amplify", "overcharge", "frugal", "vow", "blood_price", "execute", "trial_key", "kindled", "unstable" -> "power";
				case "widen", "focus", "split" -> "area";
				case "extend", "linger", "rapid" -> "timing";
				// New runes (batch 2): Kindred spreads an effect to more creatures.
				case "thirst" -> "power";
				case "kindred" -> "area";
				case "belated" -> "timing";
				// ---- links-mods pack (the hearth pack; Gathering and Tending are added below)
				case "culling", "headhunting", "hallowed", "tapering", "pooled", "sunlit", "bountiful", "soothing", "steady" -> "power";
				case "inward", "selfless", "triage", "gentle", "sparing" -> "area";
				case "tidy", "replanting", "kilned", "silken", "windfall", "veinfollow", "timbering", "level_ground", "magnetic", "fetching",
					"fleecing", "ore_sensing", "torchset" -> "gathering";
				case "sowing", "furrowing", "fertile", "mending", "nourishing", "purifying", "matchmaking", "cushioned", "damp" -> "tending";
				default -> "projectile";
			};
			case LINK -> switch (path) {
				case "delay", "pulse", "echo" -> "timing";
				case "on_hurt", "on_low_health" -> "reactive";
				case "if_sneaking", "if_airborne", "combo", "if_wounded", "if_outnumbered", "if_wet" -> "condition";
				// ---- links-mods pack (the hearth conditions; its watchers are triggers)
				case "if_night", "if_day", "if_raining", "if_underground", "if_alone", "if_near_ally", "if_unhurt", "if_holding_tool",
					"if_brimming", "if_in_fields" -> "condition";
				default -> "trigger";
			};
			case KNOT -> "knot";
		};
	}
}
