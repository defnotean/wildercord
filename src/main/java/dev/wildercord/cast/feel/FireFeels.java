package dev.wildercord.cast.feel;

import dev.wildercord.cast.FireBloodVfx;

/**
 * The signatures of the fire runes (docs/audit/spell-feel-fire-blood.md, Appendix C). One owner per file: add a
 * {@link Signature} per rune here (see docs/ADDING_RUNES.md, "Give it its own feel").
 *
 * <p>Every fire rune plays its own kit sound (tools/feel/fire.py) from its own display, at the moment that display happens
 * (a fuse ticks, a meteor lands): the display code is {@link FireBloodVfx}. So none of them lets the element's generic impact
 * sound or the touched glow play over it ({@code replace(IMPACT, HIT)}); what a signature adds here is the gesture (motion), how
 * big it reads (scale), its second colour (accent) and, where the arm does something first, the anticipation at the hand.</p>
 */
final class FireFeels {
	private FireFeels() {}

	/** A rune that draws and voices its own impact and touch. */
	private static Signature own(String rune) {
		return Signature.of(rune).replace(Phase.IMPACT, Phase.HIT);
	}

	static void register() {
		Signature.of("cinder_bulwark").accent(0xFFA26A).sound(Phase.CUE,"earth_crack",.45F,.75F).register();
		Signature.of("boiling_surge").accent(0xFFCF9A).sound(Phase.CUE,"frost_surge",.45F,1.12F).register();
		// The simple ones: a cinder, a burn, a flash.
		own("ember").motion(Motion.FLICK).scale(0.85).accent(0xFFD060).register();
		own("fire").accent(0xFFB040).register();
		own("flashfire").motion(Motion.BLAST).scale(0.9).accent(0xFFF3D0).register();
		own("explode").motion(Motion.BLAST).scale(1.15).accent(0xFFFFFF).hook(Phase.CUE, FireBloodVfx::cueBlast).register();
		own("meteor").motion(Motion.CALL).scale(1.4).accent(0xFF5A20).hook(Phase.CUE, FireBloodVfx::cueCall).register();
		own("inferno").motion(Motion.SEAL).scale(1.1).accent(0xFFB040).register();
		own("primer").accent(0xFF6EC7).register();
		own("kindling").accent(0xFFD060).register();

		// The fused and found ones.
		own("firestorm").accent(0xE6F0FF).register();
		own("steam").motion(Motion.BLAST).accent(0xE6FAFF).register();
		own("sunscorch").motion(Motion.CALL).scale(1.2).accent(0xFFF3D0).register();
		own("soulfire").accent(0x5AD8E6).register();
		own("blazecall").motion(Motion.HURL).accent(0xFF8030).register();
		own("cinderbrand").accent(0xFF7040).register();
		own("ashen_veil").motion(Motion.AURA).accent(0x8A8480).register();
		own("cinderheart").motion(Motion.AURA).scale(1.3).accent(0xFF7A20).register();
		own("searing_edge").motion(Motion.FLICK).accent(0xFFB040).register();
		own("fireward").motion(Motion.AURA).accent(0xFFC060).register();
		// Smelt keeps its furnace flash: only its second colour is its own.
		Signature.of("smelt").accent(0xFFE080).register();

		// The great ones.
		own("hellmouth").motion(Motion.SEAL).scale(1.5).accent(0x6A2AA0).register();
		own("starfire").motion(Motion.HURL).accent(0xFF80C0).register();
		own("everburn").accent(0xFFD060).register();
		own("conflagration").motion(Motion.CALL).scale(1.5).accent(0xFFFFFF).register();
		own("phoenix_pyre").motion(Motion.AURA).scale(1.5).accent(0x7AD060).register();
		own("bloodboil").accent(0xFF5A40).register();

		// Fire and its partners.
		own("seethe").accent(0x4AA8FF).register();
		own("skyburst").motion(Motion.CALL).scale(1.3).accent(0xFFD060).register();
	}
}
