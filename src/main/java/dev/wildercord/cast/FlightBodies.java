package dev.wildercord.cast;

import java.util.Arrays;
import java.util.List;

/** Shared coverage contract: only fully authored groups replace the older travel layer. */
public final class FlightBodies {
    private FlightBodies() {}

    public static final List<String> FIRE = List.of(
        "ember", "fire", "firestorm", "steam", "meteor", "soulfire", "starfire", "phoenix_pyre",
        "flashfire", "explode", "inferno", "primer", "kindling", "sunscorch", "blazecall", "cinderbrand",
        "ashen_veil", "cinderheart", "searing_edge", "fireward", "smelt", "hellmouth", "everburn",
        "conflagration", "seethe", "skyburst", "cinder_bulwark", "boiling_surge", "cinder_sieve");

    public static boolean supports(String id) {
        return supportsFire(id) || supportsFrost(id) || supportsStorm(id) || supportsWind(id) || supportsEarth(id) || supportsLife(id);
    }

    public static final List<String> FROST = List.of(
        "basinfill", "absolute_zero", "avalanche", "black_ice", "blizzard", "bubble", "chill", "coldsnap",
        "cryostasis", "current", "drowning_word", "flash_freeze", "freeze", "frost", "frostbite",
        "frostbloom", "frostward", "glacier", "hail", "hoarfrost", "icepath", "icicle", "mirrorfrost",
        "rime_causeway", "rime_seal", "tidal_lift", "tidebreath", "tidecall", "tidehook", "tidewrit", "undertow", "springbed");

    public static boolean supportsFire(String id) {
        return id.startsWith("wildercord:") && FIRE.contains(id.substring(11));
    }

    public static boolean supportsFrost(String id) {
        return id.startsWith("wildercord:") && FROST.contains(id.substring(11));
    }

    public static final List<String> STORM = List.of(
        "frostwire", "galvanize", "jolt", "lightning", "magnetize", "plasma", "riftbolt", "ripple",
        "shock", "stormclock", "stormheart", "stormweave", "surge", "tempest", "thunder_tide",
        "thunder_walk", "thunderbird", "thunderclap", "thunderhead", "thunderstep");

    public static boolean supportsStorm(String id) {
        return id.startsWith("wildercord:") && STORM.contains(id.substring(11));
    }

    public static final List<String> WIND = List.of(
        "cushion", "cyclone", "dash", "deflect", "disarm", "downdraft", "dust_devil", "feather_fall",
        "gale_mantle", "launch", "leap", "levitate", "prune", "push", "razorgale", "recoil", "repel",
        "skyglyph", "soar", "summit_wind", "swift", "updraft", "wind_steps", "windcut", "zephyr", "skylatch", "thresherwind");

    public static boolean supportsWind(String id) {
        return id.startsWith("wildercord:") && WIND.contains(id.substring(11));
    }

    public static final List<String> EARTH = List.of(
        "shield", "break", "stoneskin", "root", "tremor", "excavate", "aftershock", "weigh", "shackle",
        "rampart", "brace", "chisel", "tunnel", "vein", "fell", "pelt", "stoneform", "magma", "sinkhole",
        "geode", "fossilize", "bonespur", "monolith", "strata_rise", "thunderquake", "infest", "sandstorm",
        "tusk_charge", "mire", "stalactite", "basalt_surge", "prospect", "clockroot");

    public static boolean supportsEarth(String id) {
        return id.startsWith("wildercord:") && EARTH.contains(id.substring(11));
    }

    public static final List<String> LIFE = List.of(
        "heal", "grow", "regrowth", "cleanse", "venom", "nourish", "harvest", "reversal", "restore",
        "bramble", "haven", "glimmer", "fortune", "bloom", "soulbond", "second_wind", "lifebloom",
        "root_bulwark", "bloomstep", "stitchtime", "vinelash", "remedy", "ancient_seed", "moonpetal",
        "sporebloom", "glowvine", "rootsnare", "drowse", "ashen_mercy");

    public static boolean supportsLife(String id) {
        return id.startsWith("wildercord:") && LIFE.contains(id.substring(11));
    }

    public static boolean covers(String identities) {
        return !identities.isEmpty() && Arrays.stream(identities.split(",", -1)).allMatch(FlightBodies::supports);
    }
}
