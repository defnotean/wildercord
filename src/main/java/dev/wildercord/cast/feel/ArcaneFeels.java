package dev.wildercord.cast.feel;

/**
 * The signatures of the arcane runes (see docs/ADDING_RUNES.md, "Give it its own feel"): a role stinger for the first
 * 200 ms (strike, mark, glass, step, summon), a bigger read for the sky strikers and the summon, and an accent that stops
 * every arcane spell being the same pink.
 *
 * <p>Arcane's grammar: a star seal marks someone (Exposed, a brand, a reflecting shell); instant strikes are needles,
 * columns and comets; wards are glass; movement is a pair of arcs.</p>
 */
final class ArcaneFeels {
	private ArcaneFeels() {}

	private static void cue(String stinger, int accent, String... runes) {
		for (String rune : runes) {
			Signature.of(rune).sound(Phase.CUE, stinger, 0.6F, 1.0F).accent(accent).register();
		}
	}

	static void register() {
		cue("arcane_stinger_strike", 0xFF9AF0, "harm", "fangs", "starshard", "manaburn");
		// The sky strikers read bigger than their price and are called from above.
		for (String rune : new String[] {"smite", "starfall", "cometfall"}) {
			Signature.of(rune).sound(Phase.CUE, "arcane_stinger_strike", 0.7F, 0.85F).motion(Motion.CALL).scale(1.3).accent(0xFFF0B0).register();
		}
		cue("arcane_stinger_mark", 0xE8B0FF, "spellbrand", "reveal", "resonance", "decree", "silence", "prismatic_burst", "nullify");
		cue("arcane_stinger_glass", 0xB8D8FF, "barrier", "reflect", "span", "haste", "empower", "halo");
		cue("arcane_stinger_step", 0xFFD8FA, "swap", "light", "night_eye", "treasure_sense", "starlight_tether");
		Signature.of("summon").sound(Phase.CUE, "arcane_stinger_summon", 0.8F, 1.0F).motion(Motion.CALL).scale(1.5).accent(0xB8C8FF).register();
		// Lifeline: one pale strand from the hand to a friend.
		Signature.of("lifeline").motion(Motion.BEAM).accent(0x9FE8FF).sound(Phase.CUE, "arcane_stinger_step", 0.5F, 1.12F).register();
		cue("arcane_stinger_summon", 0xFFE8FF, "twin_star", "manatide");
		// ---- fx-explore pack
		cue("arcane_stinger_mark", 0xE8D0FF, "lux_reading", "appraise", "folk_census", "lore_reading", "shelf_count");
		cue("arcane_stinger_step", 0xFFE8C0, "chalk_line", "stand_pose");
		cue("arcane_stinger_glass", 0x9FD8FF, "haggle", "lapis_thrift", "beacon_swell");
		// ---- fx-support pack
		dev.wildercord.cast.packs.WardFeels.register();
	}
}
