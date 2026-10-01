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
	}
}
