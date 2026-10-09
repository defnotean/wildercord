package dev.wildercord.client.fx;

import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The everyday packs' utility runes, by element. They cast through their own effect routes, outside the reviewed
 * combat formation and flight rosters, so those suites compare against an element's combat runes only. Life keeps
 * its own list in {@link LifeRuntimePartitionChecks#UTILITY}.
 */
public final class EverydayRunes {
    private EverydayRunes() {}

    public static final Map<String, Set<String>> UTILITY = Map.ofEntries(
        Map.entry("void", Set.of("chestsort", "delvemark", "enderhush", "frame_veil", "grave_bearing", "hexguard", "hobble", "hollow_pocket",
            "homeward", "lodepull", "lure", "packtidy", "portal_reckoning", "restock", "softfoot", "spawner_sense",
            "spire_sense", "spook", "stillbind", "stow", "stronghold_compass", "unburden", "void_step")),
        Map.entry("earth", Set.of("aftercare", "agestone", "barkhide", "barkstrip", "berrybless", "blastward", "blockpack", "brickwork",
            "caveward", "citadel", "compost", "concreteset", "coppice", "deepsound", "deepwarn", "deepway",
            "depth_sounding", "fallow", "floorlay", "fodder", "gangue", "glyph_carve", "gourdcall", "guardlink",
            "hearthguard", "holefill", "ironhold", "keenkeep", "keepsafe", "land_reading", "landread", "leafshade",
            "levelground", "long_arm", "luckstrike", "millstone", "motherlode", "nest_tend", "orepluck", "oretally",
            "picnic", "pitfloor", "plankway", "plowline", "plumbline", "polish", "reed_cut", "relic_sense", "riser",
            "ruin_sense", "sandbar", "sapflow", "saplingrise", "saplingsow", "shellback", "shieldwall", "shoreup",
            "siftfall", "sow", "stairdelve", "stalkrise", "steady_brush", "stilt", "stoutheart", "surefoot", "tend",
            "tillage", "tilth", "tinker_hum", "unpack", "wildflower")),
        Map.entry("fire", Set.of("bakehouse", "barnwarmth", "calmsmoke", "ember_rest", "emberguard", "feastday", "fortress_sense", "gold_parley",
            "hearthcook", "hearthglow", "hearthsong", "kilnbake", "lamplighter", "lava_sense", "lavaseal", "morale",
            "portal_sense", "quickbrew", "smoke_signal", "stewpot", "sunbask", "thawfield", "torchfall", "trail_blaze",
            "warm_cloak")),
        Map.entry("frost", Set.of("air_pocket", "angler_lure", "axolotl_kinship", "brimming", "coral_mend", "currentkin", "dewcatch",
            "divers_hands", "diving_bell", "dolphin_call", "drift_net", "drown_ward", "firebreak", "ice_auger", "inkveil",
            "kelpsong", "lava_crust", "lily_path", "mending_mist", "milkmaid", "mooring_call", "oceans_favor", "porpoise",
            "quench", "reeling_tide", "refloat", "salve", "shipwreck_sense", "shoal_herd", "skaters_edge", "skimstep",
            "sluice", "snuffout", "soak_through", "sounding", "spring_draw", "springseek", "staunch", "upwell", "wring")),
        Map.entry("storm", Set.of("buttonpush", "dewfall", "dewkeep", "ditchwater", "doorcall", "dynamo_stride", "leverflip", "rain_cloud",
            "sky_reading", "skyread", "storm_glass")),
        Map.entry("wind", Set.of("arrowveil", "beeline", "corral", "evade", "fair_wind", "fieldstride", "fleece", "folk_call", "gentlehand",
            "glidewind", "hayloft", "heel", "herdcall", "hollowsense", "home_bearing", "honeydew", "leaffall", "nudge",
            "pollinate", "rally", "scarecrow", "sea_breeze", "shrug_off", "snuff_out", "softsole", "spawn_bearing",
            "steedsong", "trot", "village_sense", "wayfarer_hymn", "whistle", "withdraw")),
        Map.entry("arcane", Set.of("accord", "aegis", "appraise", "bait_blessing", "beacon_swell", "beastguard", "bellward", "bobber_bell",
            "camp_ward", "chalk_line", "chalkline", "courtship", "faithful", "fathom", "fieldsense", "folk_census",
            "gloomsight", "grace", "gravefinder", "haggle", "headlamp", "hearthpath", "herdsense", "lantern_soul",
            "lapis_thrift", "lodestar", "lore_reading", "lostfind", "lumenpath", "lux_reading", "managift", "manawell",
            "nightwatch", "orbcall", "pacify", "pearl_sight", "rally_light", "sanctuary", "school_sight", "sentry",
            "shelf_count", "shore_sense", "silklift", "soothe", "stand_pose", "starchart", "stillwell", "stocktake",
            "tackle_mend", "tide_lantern", "tide_marker", "toolmend", "truce", "water_reading", "waymark", "worst_first",
            "wreck_sense")),
        Map.entry("time", Set.of("cloche", "henhouse", "hivehum", "moon_reading", "ripen", "savor", "sun_reading", "tarry", "trade_renew")),
        Map.entry("blood", Set.of("clot", "heartsense", "taunt"))
    );

    /** An element's reviewed combat runes (paths): its runtime effect runes minus its everyday utilities. */
    public static Set<String> combatPaths(String element) {
        var runtime = Runes.all().stream().filter(r -> r.family() == RuneFamily.EFFECT && r.element().equals(element))
            .map(r -> r.path()).collect(Collectors.toSet());
        var utility = UTILITY.getOrDefault(element, Set.of());
        if (!runtime.containsAll(utility)) throw new AssertionError("Every listed " + element + " utility rune is registered");
        return runtime.stream().filter(path -> !utility.contains(path)).collect(Collectors.toSet());
    }

    /** The same combat runes as sorted full ids. */
    public static List<String> combatIds(String element) {
        return combatPaths(element).stream().map(path -> "wildercord:" + path).sorted().toList();
    }
}
