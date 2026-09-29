package dev.wildercord.cast.feel;

import dev.wildercord.cast.FireBloodVfx;

/**
 * The signatures of the blood runes (docs/audit/spell-feel-fire-blood.md, Appendix C). One owner per file: add a
 * {@link Signature} per rune here (see docs/ADDING_RUNES.md, "Give it its own feel").
 *
 * <p>Blood is wet and close. Each rune plays its own kit sound (tools/feel/blood.py) from its own display
 * ({@link FireBloodVfx} and the fused effects' own vfx), so none lets the element's generic impact sound or the touched glow
 * play over it. The signature gives the gesture, the size, the second colour and, for the runes whose arm draws something
 * first (a cut, a drop of blood), the anticipation at the hand.</p>
 */
final class BloodFeels {
	private BloodFeels() {}

	private static Signature own(String rune) {
		return Signature.of(rune).replace(Phase.IMPACT, Phase.HIT);
	}

	static void register() {
		// Cuts.
		own("bleed").motion(Motion.SLASH).scale(0.85).accent(0xFF6474).register();
		own("gash").motion(Motion.SLASH).accent(0xA01830).register();
		own("dismantle").motion(Motion.SLASH).accent(0xFFE0E4).register();
		own("cleave").motion(Motion.SLASH).scale(1.3).accent(0xFFFFFF).hook(Phase.CUE, FireBloodVfx::cueCut).register();
		own("rend").motion(Motion.FLICK).accent(0xC8D0E0).register();

		// Drinking.
		own("leech").motion(Motion.FLICK).accent(0xFF6474).register();
		own("lifesteal").motion(Motion.FLICK).accent(0xA01830).register();
		own("blood_moss").motion(Motion.SEAL).accent(0x5E8A3A).register();
		own("parasite").motion(Motion.HURL).accent(0x7AD060).register();

		// The pulse.
		own("overdrive").motion(Motion.AURA).scale(1.2).accent(0xFF6474).hook(Phase.CUE, FireBloodVfx::cueDrop).register();
		own("warcry").motion(Motion.AURA).scale(1.2).accent(0xFF6474).register();
		own("heartstopper").motion(Motion.HURL).scale(1.3).accent(0xFFE060).register();
		own("hemomancy").motion(Motion.AURA).accent(0xA060FF).register();

		// The rites.
		own("sanguine_rite").motion(Motion.BEAM).scale(1.3).accent(0x8A1030).hook(Phase.CUE, FireBloodVfx::cueCut).register();
		own("crimson_mist").motion(Motion.SEAL).scale(1.3).accent(0xFF6474).register();
		own("transfusion").motion(Motion.HURL).accent(0x7AD060).register();
		own("blood_thread").motion(Motion.BEAM).scale(0.9).accent(0xFF5060).register();
	}
}
