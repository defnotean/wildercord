package dev.wildercord.cast.packs;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.cast.feel.Phase;
import dev.wildercord.cast.feel.Signature;

/**
 * The cast signatures of the farmstead runes (see {@link FarmEffects}): an accent of each one's own and a short kit cue at the
 * hand, soft and homely (a spade's bite, a breath of wind, coals, a glint). The time runes take the time family's {@code time_cue}
 * on a step of the scale, as every time rune does.
 */
public final class FarmFeels {
	private FarmFeels() {}

	public static void register() {
		// The field.
		rune("tillage", 0x8C6B40, "earth_dig", 0.55F, 1.12F);
		rune("dewfall", 0x9AD0F0, "storm_pip", 0.5F, 1.26F);
		rune("tilth", 0x7E5C38, "earth_rattle", 0.5F, 1.0F);
		rune("plowline", 0x6A5238, "earth_grind", 0.55F, 1.0F);
		rune("sow", 0xC8D870, "earth_tick", 0.5F, 1.26F);
		time("ripen", 0xF0C850, 2);
		rune("dewkeep", 0xB8E0F8, "storm_cloud", 0.5F, 1.12F);
		rune("fieldsense", 0xFFE68A, "arcane_glint", 0.5F, 1.12F);
		rune("thawfield", 0xFFB080, "fire_steam", 0.5F, 1.12F);
		time("cloche", 0xE8F4FF, 0);
		rune("scarecrow", 0xD8B060, "wind_whirl", 0.55F, 0.84F);
		rune("fallow", 0x5C4832, "earth_press", 0.5F, 0.84F);
		rune("ditchwater", 0x4A90D0, "storm_hum", 0.5F, 0.84F);
		rune("compost", 0x5E7A30, "earth_clamp", 0.5F, 0.84F);
		rune("stalkrise", 0x90C860, "earth_tick", 0.5F, 1.5F);
		rune("gourdcall", 0xE08A2A, "earth_stomp", 0.45F, 1.12F);
		rune("berrybless", 0xD0303C, "earth_ping", 0.5F, 1.26F);
		// The herd.
		rune("courtship", 0xF08AB0, "arcane_flutter", 0.5F, 1.26F);
		rune("herdcall", 0xC8A878, "wind_eddy", 0.5F, 1.0F);
		rune("fleece", 0xF4F4F0, "wind_feather", 0.5F, 1.12F);
		rune("milkmaid", 0xFAFAF4, "frost_glint", 0.45F, 1.26F);
		time("henhouse", 0xF8E8C0, 4);
		rune("gentlehand", 0xD8E8C8, "wind_feather", 0.45F, 0.84F);
		rune("fodder", 0xD8C060, "earth_tick", 0.5F, 1.0F);
		rune("barnwarmth", 0xF0A060, "fire_kindly", 0.5F, 1.0F);
		rune("herdsense", 0xB0A0F0, "arcane_ping", 0.5F, 1.0F);
		// The kitchen.
		rune("hearthcook", 0xD06A2A, "fire_coals", 0.55F, 0.84F);
		rune("stewpot", 0xB07A4A, "fire_smoulder", 0.5F, 1.0F);
		rune("bakehouse", 0xE8B060, "fire_hop", 0.5F, 1.12F);
		// The hive.
		rune("pollinate", 0xFFD040, "wind_whirl", 0.5F, 1.5F);
		time("hivehum", 0xF0B020, 1);
		rune("calmsmoke", 0xB8B0A8, "fire_steam", 0.45F, 0.84F);
		// The wood.
		rune("wildflower", 0xF0A0D0, "earth_ping", 0.5F, 1.5F);
		rune("saplingrise", 0x4A9A3A, "earth_grind", 0.5F, 1.26F);
		rune("saplingsow", 0x6AB04A, "earth_tick", 0.5F, 1.12F);
		rune("leaffall", 0xC89A3A, "wind_feather", 0.5F, 1.0F);
		rune("barkstrip", 0xC8A070, "earth_rattle", 0.5F, 1.26F);
		rune("coppice", 0x76563A, "earth_dig", 0.65F, 0.84F);
		// The table and the lane.
		rune("feastday", 0xFFC850, "fire_kindly", 0.6F, 1.12F);
		rune("picnic", 0xE84A4A, "earth_ping", 0.5F, 1.12F);
		rune("honeydew", 0xFFB830, "wind_feather", 0.5F, 1.26F);
		rune("leafshade", 0x3A8A3A, "earth_press", 0.5F, 1.12F);
		rune("barkhide", 0x94704A, "earth_clamp", 0.55F, 1.0F);
		rune("sapflow", 0x9AD060, "earth_ping", 0.5F, 1.0F);
		rune("trot", 0xA07850, "wind_dash", 0.5F, 1.0F);
		rune("beeline", 0xFFDC5A, "wind_dash", 0.5F, 1.5F);
		rune("fieldstride", 0xBCD47A, "wind_lift", 0.5F, 1.12F);
		rune("hayloft", 0xE8D080, "wind_rise", 0.55F, 1.0F);
	}

	private static void rune(String id, int accent, String kit, float volume, float pitch) {
		Signature.of(id).accent(accent).sound(Phase.CUE, kit, volume, pitch).register();
	}

	private static void time(String id, int accent, int step) {
		Signature.of(id).accent(accent).sound(Phase.CUE, "time_cue", 0.9F, Feels.step(step)).register();
	}
}
