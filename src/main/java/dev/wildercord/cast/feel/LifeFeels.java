package dev.wildercord.cast.feel;

/**
 * The signatures of the life runes (see docs/ADDING_RUNES.md, "Give it its own feel"): each rune's first 200 ms is a role
 * stinger (restore, ward, world, thorn) chosen by what it does, and its accent colour keeps life's palette from turning
 * every spell the same green. The landing sounds and shapes are the runes' own displays ({@code LifeArcaneFx}).
 *
 * <p>Life's grammar: the bloom means restoration; combat life is thorns and vines; crowd life is spores and pollen;
 * world life is ripples and lamps.</p>
 */
final class LifeFeels {
	private LifeFeels() {}

	private static void cue(String stinger, int accent, String... runes) {
		for (String rune : runes) {
			Signature.of(rune).sound(Phase.CUE, stinger, 0.6F, 1.0F).accent(accent).register();
		}
	}

	static void register() {
		Signature.of("root_bulwark").accent(0xD4E895).sound(Phase.CUE,"earth_grind",.35F,1.12F).register();
		// Restoring: two soft notes up a third; the accent is a warm gold-green.
		cue("life_stinger_restore", 0xC8F090, "heal", "regrowth", "restore", "remedy", "cleanse", "bloom", "lifebloom", "stitchtime", "nourish");
		// Wards: a low warm swell and a click.
		cue("life_stinger_ward", 0xFFE39A, "haven", "bramble", "soulbond", "reversal", "second_wind");
		// The world: one bright bloop and a wood tick; lamps and gardens.
		cue("life_stinger_world", 0x7CFFE0, "grow", "harvest", "glimmer", "glowvine", "ancient_seed", "bloomstep", "fortune");
		// The thorn: a dry twig snap; venom, vines, roots, spores and sleep.
		cue("life_stinger_thorn", 0x86D23A, "venom", "vinelash", "rootsnare", "sporebloom", "drowse", "moonpetal");
		// Actual effect owners provide the authored outcomes and voices.
		LifeOutcomeSignatures.register();
	}
}
