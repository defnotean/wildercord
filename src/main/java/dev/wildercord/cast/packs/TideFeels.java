package dev.wildercord.cast.packs;

import dev.wildercord.cast.feel.Phase;
import dev.wildercord.cast.feel.Signature;

/**
 * The cast signatures of the Tide pack (fx-fish): a quiet tag at the hand in a water voice, the pack's sea-glass
 * accents, and each rune's own burst and sound (in {@link TideEffects}) in place of the element's generic impact.
 */
public final class TideFeels {
	private TideFeels() {}

	private static final int SEA = 0x4AC8E0;
	private static final int FOAM = 0xCFF4F0;
	private static final int DEEP = 0x2E6AA8;
	private static final int SHORE = 0xD9C38A;
	private static final int REED = 0x8FBF6A;

	private static void tide(String rune, double scale, int accent, String tag, float pitch) {
		Signature.of(rune).scale(scale).accent(accent).sound(Phase.CUE, tag, 0.3F, pitch).replace(Phase.IMPACT, Phase.HIT).register();
	}

	public static void register() {
		// Fishing: a light pluck and a bobber's tick.
		tide("angler_lure", 0.8, SEA, "frost_hook", 1.3F);
		tide("bait_blessing", 0.8, 0xF2D27A, "arcane_glint", 1.1F);
		tide("reeling_tide", 0.8, SEA, "frost_hook", 1.0F);
		tide("school_sight", 0.9, 0x9FE8FF, "arcane_glint_ping", 0.9F);
		tide("tackle_mend", 0.8, 0xF2D27A, "arcane_pluck", 1.2F);
		tide("bobber_bell", 0.8, FOAM, "arcane_ping", 1.4F);
		tide("water_reading", 0.75, FOAM, "arcane_ping", 1.0F);
		tide("dolphin_call", 1.0, SEA, "frost_tide_surge", 1.3F);
		tide("axolotl_kinship", 0.9, 0xF4A6C8, "frost_bubble_in", 1.3F);
		tide("shoal_herd", 0.9, SEA, "frost_drag", 1.3F);
		tide("refloat", 0.9, SEA, "frost_splat", 1.0F);
		// Water work: pours, crusts and drips.
		tide("reed_cut", 0.85, REED, "earth_snap", 1.2F);
		tide("wring", 1.0, FOAM, "frost_drag", 0.9F);
		tide("spring_draw", 0.8, SEA, "frost_basin_pour", 1.1F);
		tide("brimming", 0.85, SEA, "frost_basin_pour", 1.0F);
		tide("oceans_favor", 1.3, 0x5BE3C8, "frost_tide_surge", 0.8F);
		tide("diving_bell", 1.15, DEEP, "frost_bubble_in", 0.8F);
		tide("tide_lantern", 0.8, 0x9FF0FF, "arcane_glint", 1.3F);
		tide("sluice", 0.9, SEA, "frost_splat", 0.9F);
		tide("soak_through", 0.9, SHORE, "earth_gloop", 1.0F);
		tide("rain_cloud", 1.1, 0xA8C8E8, "storm_cloud", 1.1F);
		tide("storm_glass", 0.75, 0xA8C8E8, "storm_pip", 1.0F);
		tide("kelpsong", 0.9, REED, "frost_bubble_in", 1.1F);
		tide("coral_mend", 0.95, 0xFF8FA8, "frost_bubble_pop", 1.1F);
		tide("nest_tend", 0.85, SHORE, "earth_tick", 1.2F);
		tide("lily_path", 0.9, REED, "frost_rime_step", 1.2F);
		tide("sandbar", 1.05, SHORE, "earth_grind", 1.1F);
		tide("ice_auger", 0.85, 0xEAF8FF, "frost_crack", 1.1F);
		tide("tide_marker", 0.8, FOAM, "arcane_stamp", 1.2F);
		tide("shore_sense", 0.8, SHORE, "arcane_glint_ping", 1.0F);
		tide("fathom", 0.75, DEEP, "arcane_ping", 0.7F);
		tide("wreck_sense", 1.0, 0xE8C060, "arcane_resonate", 0.9F);
		tide("drift_net", 0.9, SEA, "frost_hook", 0.9F);
		tide("mooring_call", 0.9, SHORE, "frost_drag", 1.0F);
		// Movement: surges up, down and along.
		tide("fair_wind", 1.0, 0xDDF6FF, "wind_gale", 1.1F);
		tide("upwell", 0.9, SEA, "frost_surge", 1.3F);
		tide("sounding", 0.9, DEEP, "frost_surge", 0.7F);
		tide("porpoise", 0.95, SEA, "frost_tide_surge", 1.2F);
		// Gifts: breath and shells.
		tide("skimstep", 1.0, FOAM, "frost_rime_step", 1.0F);
		tide("skaters_edge", 0.85, 0xEAF8FF, "frost_crust", 1.2F);
		tide("air_pocket", 0.75, FOAM, "frost_breath", 1.2F);
		tide("drown_ward", 0.9, SEA, "frost_ward", 1.1F);
		tide("pearl_sight", 0.9, 0x9FE8FF, "frost_lotus", 1.2F);
		tide("sea_breeze", 0.9, 0xDDF6FF, "wind_feather", 1.1F);
		tide("inkveil", 0.95, 0x30304A, "frost_splat", 0.7F);
		tide("shellback", 0.95, SHORE, "earth_clamp", 1.0F);
		tide("dewcatch", 0.75, FOAM, "frost_bubble_pop", 1.3F);
		tide("divers_hands", 0.8, SEA, "frost_breath", 0.9F);
	}
}
