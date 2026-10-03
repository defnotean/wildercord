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
        "conflagration", "seethe", "skyburst", "cinder_bulwark", "boiling_surge");

    public static boolean supports(String id) {
        return id.startsWith("wildercord:") && FIRE.contains(id.substring(11));
    }

    public static boolean covers(String identities) {
        return !identities.isEmpty() && Arrays.stream(identities.split(",", -1)).allMatch(FlightBodies::supports);
    }
}
