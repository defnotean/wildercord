package dev.wildercord.cast.feel;

/**
 * The signatures of the void runes. The element's own cast sound swells for most of a second before it peaks, so every void
 * rune gets an early one: {@code void_cue}, a low dark knock heard the instant the hand moves, at a pitch of its own (a hook is
 * high and sharp, a well or a black hole is deep). The rest of each rune's identity (its sounds at the target, its shapes) lives
 * where it is drawn: {@code VoidFx}, {@code Vfx}, {@code ExplorerVfx}, {@code FusedVoidVfx}; the kit is {@code tools/feel/void.py}.
 *
 * <p>{@code scale} tunes how big a rune's spells read against what its cost says (a Hollow or a Singularity is bigger than its
 * mana, an Umbra or a Hex a little smaller).</p>
 */
final class VoidFeels {
	private VoidFeels() {}

	/** The early knock at the hand, at {@code pitch}, and the scale the rune reads at. */
	private static Signature rune(String id, float pitch, double scale) {
		Signature s = Signature.of(id).sound(Phase.CUE, "void_cue", 0.9F, pitch);
		if (scale != 1.0) {
			s.scale(scale);
		}
		return s;
	}

	static void register() {
		// Tier 1
		rune("anchor", 0.6F, 0.85).register();
		rune("blind", 1.1F, 0.85).register();
		rune("collect", 1.5F, 0.75).register();
		rune("hex", 1.0F, 0.85).register();
		rune("phantom", 0.95F, 1.0).register();
		rune("umbra", 1.0F, 0.85).register();
		// Tier 2
		rune("banish", 1.2F, 1.0).register();
		rune("echolocate", 1.4F, 1.0).register();
		rune("grapple", 1.35F, 1.0).register();
		rune("hush", 0.7F, 1.0).register();
		rune("portalfall", 0.85F, 1.0).register();
		rune("pull", 1.25F, 1.0).register();
		rune("veil", 0.9F, 1.0).register();
		rune("warp_step", 1.5F, 1.0).register();
		rune("zipper", 1.45F, 0.9).register();
		// Tier 3
		rune("blackflame", 0.85F, 1.0).accent(0x8060C0).register();
		rune("blackspark", 1.15F, 1.0).register();
		rune("blink", 1.6F, 1.0).register();
		rune("devour", 0.7F, 1.0).register();
		rune("eclipse", 0.6F, 1.2).register();
		rune("entropy", 0.9F, 1.0).register();
		rune("gravity_well", 0.6F, 1.1).register();
		rune("malison", 0.8F, 1.0).register();
		rune("resonant_shriek", 1.3F, 1.0).register();
		rune("riftcall", 0.75F, 1.1).register();
		rune("shades", 0.7F, 1.0).register();
		rune("shadowstep", 1.05F, 1.0).register();
		rune("shulkershell", 0.65F, 1.0).register();
		rune("singularity", 0.55F, 1.4).register();
		rune("warp", 1.3F, 1.0).register();
		// Tier 4
		rune("dragon_breath", 0.7F, 1.15).register();
		rune("hollow", 0.65F, 1.3).register();
		rune("infinity", 0.8F, 1.1).register();
		rune("sonic_boom", 1.0F, 1.2).register();
		rune("starmaw", 0.55F, 1.2).register();
		rune("wither", 0.75F, 1.1).register();
	}
}
