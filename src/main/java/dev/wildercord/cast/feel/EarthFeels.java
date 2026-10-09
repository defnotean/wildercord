package dev.wildercord.cast.feel;

/**
 * The signatures of the earth runes (docs/audit/spell-feel-storm-earth.md, table 2D). One owner per file. Each rune gets an
 * accent of its own (so two earth spells never glow alike), and the mining runes a pick's bite of their own pitch; the rest
 * of each rune's look and sound lives in its effect code.
 */
final class EarthFeels {
	private EarthFeels() {}

	static void register() {
		Signature.of("strata_rise").accent(0xD5C8AE).sound(Phase.CUE,"earth_grind",.6F,.84F).register();
		// Tollgate: a ground seal, a low clamp as the threshold is laid.
		Signature.of("tollgate").motion(Motion.SEAL).accent(0xE0A060).sound(Phase.CUE,"earth_clamp",.55F,.84F).register();
		// The mining runes: one crunch, pitched by the work (a careful tick to a low double).
		Signature.of("chisel").accent(0xF0F0E8).sound(Phase.CUE, "earth_dig", 0.6F, 1.26F).register();
		Signature.of("break").accent(0xA0968A).sound(Phase.CUE, "earth_dig", 0.7F, 1.0F).register();
		Signature.of("excavate").accent(0x8A7A66).sound(Phase.CUE, "earth_dig", 0.8F, 0.75F).register();
		Signature.of("tunnel").accent(0x7A6A58).sound(Phase.CUE, "earth_dig", 0.7F, 0.84F).register();
		Signature.of("vein").accent(0xFFE6A0).sound(Phase.CUE, "earth_dig", 0.6F, 1.12F).register();
		Signature.of("fell").accent(0x8A6A44).register();
		Signature.of("prospect").accent(0xFFD24A).register();
		Signature.of("rampart").accent(0x7A6448).register();
		// Wards: sandstone plates, a short high clack, deepslate.
		Signature.of("stoneskin").accent(0xE8C890).register();
		Signature.of("brace").accent(0xB0B0B0).register();
		Signature.of("stoneform").accent(0x98A2BE).register();
		Signature.of("geode").accent(0xA064F0).register();
		Signature.of("shield").motion(Motion.SEAL).accent(0xF5B04A).sound(Phase.CUE, "earth_clamp", 0.65F, 1.12F).register();
		// Holds: roots, weight, a chain, a bog.
		Signature.of("root").accent(0x7A5A36).register();
		Signature.of("weigh").accent(0x6E5436).register();
		Signature.of("shackle").accent(0x9A9AA0).register();
		Signature.of("mire").accent(0x5A4630).register();
		// Strikes from the ground.
		Signature.of("pelt").accent(0x9A9A9A).register();
		Signature.of("aftershock").accent(0xFFE0B0).register();
		Signature.of("tremor").accent(0xE8C890).scale(1.1).sound(Phase.CUE, "earth_stomp", 0.6F, 0.84F).register();
		Signature.of("thunderquake").accent(0xFFE650).sound(Phase.CUE, "earth_stomp", 0.6F, 0.75F).register();
		Signature.of("monolith").accent(0xB0A898).scale(1.15).register();
		Signature.of("bonespur").accent(0xE8E0C8).register();
		Signature.of("basalt_surge").accent(0x8A8A96).register();
		Signature.of("stalactite").accent(0xA08868).register();
		Signature.of("tusk_charge").accent(0xC89060).register();
		// Fields and pits.
		Signature.of("magma").accent(0xFF6A2A).register();
		Signature.of("sandstorm").accent(0xE3C98E).register();
		Signature.of("sinkhole").accent(0x3A1060).register();
		Signature.of("infest").accent(0x8A8A80).register();
		Signature.of("fossilize").accent(0x6E6E68).register();
		// ---- fx-mine pack: the delving runes (DelveEffects): a soft cue each, in their working colour.
		Signature.of("stairdelve").accent(0x8A7A66).sound(Phase.CUE, "earth_dig", 0.6F, 0.84F).register();
		Signature.of("riser").accent(0x9A8A70).sound(Phase.CUE, "earth_dig", 0.6F, 1.12F).register();
		Signature.of("plumbline").accent(0x7A6A5A).sound(Phase.CUE, "earth_sink", 0.6F, 1.0F).register();
		Signature.of("siftfall").accent(0xE3C98E).sound(Phase.CUE, "earth_rattle", 0.6F, 1.0F).register();
		Signature.of("gangue").accent(0x8A8A80).sound(Phase.CUE, "earth_rumble", 0.6F, 0.9F).register();
		Signature.of("orepluck").accent(0xC89060).sound(Phase.CUE, "earth_snap", 0.6F, 1.12F).register();
		Signature.of("luckstrike").accent(0x6AE08A).sound(Phase.CUE, "earth_ping", 0.6F, 1.26F).register();
		Signature.of("silklift").accent(0xFFD8FA).sound(Phase.CUE, "arcane_glint", 0.6F, 1.0F).register();
		Signature.of("deepsound").accent(0x5A6E8A).sound(Phase.CUE, "earth_ping", 0.6F, 0.84F).register();
		Signature.of("oretally").accent(0xB0A898).sound(Phase.CUE, "earth_tick", 0.6F, 1.12F).register();
		Signature.of("lavaseal").accent(0x3A1060).sound(Phase.CUE, "fire_steam", 0.6F, 1.0F).register();
		Signature.of("deepway").accent(0xA08868).sound(Phase.CUE, "earth_rumble", 0.6F, 0.75F).register();
		Signature.of("hollowsense").accent(0xD8F0FF).sound(Phase.CUE, "wind_lift", 0.6F, 1.12F).register();
		Signature.of("kilnbake").accent(0xFF8A3A).sound(Phase.CUE, "fire_coals", 0.6F, 1.0F).register();
		Signature.of("blockpack").accent(0xE8C890).sound(Phase.CUE, "earth_press", 0.6F, 1.0F).register();
		Signature.of("unpack").accent(0xD8C8A0).sound(Phase.CUE, "earth_crack", 0.6F, 1.12F).register();
		Signature.of("millstone").accent(0xC8B898).sound(Phase.CUE, "earth_grind", 0.6F, 1.0F).register();
		Signature.of("toolmend").accent(0xF0D070).sound(Phase.CUE, "earth_clamp", 0.6F, 1.26F).register();
		Signature.of("levelground").accent(0x9A8A70).sound(Phase.CUE, "earth_grind", 0.6F, 0.84F).register();
		Signature.of("holefill").accent(0x8A6A44).sound(Phase.CUE, "earth_press", 0.6F, 1.0F).register();
		Signature.of("shoreup").accent(0x7A5A36).sound(Phase.CUE, "earth_clamp", 0.6F, 1.0F).register();
		Signature.of("stilt").accent(0x8A6A44).sound(Phase.CUE, "earth_stomp", 0.6F, 1.12F).register();
		Signature.of("plankway").accent(0xB08850).sound(Phase.CUE, "earth_tick", 0.6F, 1.0F).register();
		Signature.of("polish").accent(0xE0E0E8).sound(Phase.CUE, "earth_grind", 0.6F, 1.26F).register();
		Signature.of("brickwork").accent(0xB06040).sound(Phase.CUE, "earth_press", 0.6F, 0.9F).register();
		Signature.of("agestone").accent(0x6E8A4A).sound(Phase.CUE, "earth_creak", 0.6F, 1.0F).register();
		Signature.of("concreteset").accent(0xE6FAFF).sound(Phase.CUE, "fire_steam", 0.6F, 1.26F).register();
		Signature.of("chalkline").accent(0xF0F0F0).sound(Phase.CUE, "arcane_stamp", 0.6F, 1.0F).register();
		Signature.of("pitfloor").accent(0x8A6A44).sound(Phase.CUE, "earth_press", 0.6F, 0.84F).register();
		Signature.of("torchfall").accent(0xFFC040).sound(Phase.CUE, "fire_kindly", 0.6F, 1.0F).register();
		Signature.of("gloomsight").accent(0xFF6A8A).sound(Phase.CUE, "arcane_glyph", 0.6F, 1.0F).register();
		Signature.of("lumenpath").accent(0xFFF0A0).sound(Phase.CUE, "arcane_ping", 0.6F, 1.12F).register();
		Signature.of("snuffout").accent(0xA0B8C8).sound(Phase.CUE, "fire_out", 0.6F, 1.0F).register();
		Signature.of("headlamp").accent(0xFFF8D0).sound(Phase.CUE, "arcane_ping", 0.6F, 1.26F).register();
		Signature.of("leverflip").accent(0xC04040).sound(Phase.CUE, "storm_tick", 0.6F, 1.0F).register();
		Signature.of("buttonpush").accent(0xB0B0B0).sound(Phase.CUE, "storm_pip", 0.6F, 1.0F).register();
		Signature.of("doorcall").accent(0xA07840).sound(Phase.CUE, "storm_tick", 0.6F, 0.84F).register();
		Signature.of("stocktake").accent(0xD8D0FF).sound(Phase.CUE, "arcane_glint", 0.6F, 1.0F).register();
		Signature.of("caveward").accent(0xD0B080).sound(Phase.CUE, "earth_clamp", 0.6F, 1.12F).register();
		Signature.of("motherlode").accent(0xFFD040).sound(Phase.CUE, "earth_rumble", 0.6F, 0.75F).register();
		Signature.of("floorlay").accent(0xC8C0B0).sound(Phase.CUE, "earth_press", 0.6F, 0.75F).register();
		// ---- fx-explore pack
		Signature.of("land_reading").accent(0xA89070).sound(Phase.CUE, "earth_tick", 0.5F, 1.0F).register();
		Signature.of("depth_sounding").accent(0x7A6A58).sound(Phase.CUE, "earth_ping", 0.5F, 0.84F).register();
		Signature.of("ruin_sense").accent(0xC89870).sound(Phase.CUE, "earth_rumble", 0.45F, 1.12F).register();
		Signature.of("relic_sense").accent(0xE8D2A0).sound(Phase.CUE, "earth_ping", 0.5F, 1.26F).register();
		Signature.of("steady_brush").accent(0xD8C08A).sound(Phase.CUE, "earth_hiss", 0.45F, 1.0F).register();
		Signature.of("glyph_carve").accent(0xF0E8D0).sound(Phase.CUE, "earth_dig", 0.45F, 1.5F).register();
	}
}
