package dev.wildercord.cast.feel;

/**
 * The signatures of the frost runes (and the water runes, which belong to frost's family). Every rune has:
 * an accent by family (rime is near white, steel is a hard blue, lock is deep cyan, water is saturated aqua, with
 * Black Ice violet and Cryostasis gold), a scale that matches its power (a T1 poke reads small, Avalanche and
 * Tidewrit read big), a quiet cast tag at the hand in the rune's own voice (30 to 50% volume), and its own impact:
 * the shape's flare and rings stay, but the element's shared impact sound goes (the rune's effect plays its own
 * kit sound at the target), and a generic "touched" glow is dropped where the rune already draws its own burst.
 *
 * <p>Families: rime (Chill, Frost, Hoarfrost, Frostbite, Icepath), shatter (Icicle, Hail, Coldsnap, Avalanche,
 * Absolute Zero), lock (Freeze, Glacier, Black Ice, Rime Seal, Cryostasis, Flash Freeze), water (Bubble, Undertow,
 * Tidecall, Tidewrit, Tidehook, Drowning Word, Current, Tidebreath), wards (Frostward, Frostbloom, Mirrorfrost).</p>
 */
final class FrostFeels {
	private FrostFeels() {}

	private static final int RIME = 0xEAF8FF;
	private static final int STEEL = 0xBFD8FF;
	private static final int LOCK = 0x5AB4FF;
	private static final int WATER = 0x4AA8FF;

	/** A frost or water signature: accent, scale, cast tag; the effect's own burst replaces the generic impact and touch. */
	private static Signature frost(String rune, double scale, int accent, String tag, float volume, float pitch) {
		return Signature.of(rune).scale(scale).accent(accent).sound(Phase.CUE, tag, volume, pitch).replace(Phase.IMPACT, Phase.HIT);
	}

	static void register() {
		// Rime: creeping, rising, slow.
		frost("chill", 0.85, RIME, "frost_crust", 0.3F, 1.4F).hook(Phase.AFTERMATH, FrostWindFx::frostPatch).register();
		frost("frost", 1.0, RIME, "frost_needle", 0.3F, 1.0F).hook(Phase.AFTERMATH, FrostWindFx::frostPatch).register();
		frost("hoarfrost", 1.0, RIME, "frost_creep", 0.3F, 1.0F).register();
		frost("frostbite", 1.05, 0xE8A0B4, "frost_creep", 0.3F, 0.8F).register();
		frost("icepath", 0.8, RIME, "frost_crust", 0.25F, 0.6F).register();

		// Shatter: needles, hard cracks, things that fall and snap.
		frost("icicle", 0.9, STEEL, "frost_needle", 0.3F, 1.3F).register();
		frost("coldsnap", 1.0, RIME, "frost_crust", 0.35F, 0.8F).motion(Motion.BLAST).hook(Phase.AFTERMATH, FrostWindFx::frostPatch).register();
		frost("hail", 1.1, STEEL, "frost_hail", 0.3F, 1.0F).motion(Motion.CALL).hook(Phase.AFTERMATH, FrostWindFx::frostPatch).register();
		frost("avalanche", 1.3, STEEL, "frost_whump", 0.4F, 0.7F).motion(Motion.CALL).register();
		frost("absolute_zero", 1.25, 0x2A5CFF, "frost_zero", 0.4F, 1.0F).register();

		// Lock: shells, sigils, cocoons: closing in.
		frost("freeze", 1.1, LOCK, "frost_tick", 0.5F, 0.8F).register();
		frost("glacier", 1.15, LOCK, "frost_lock", 0.3F, 0.8F).register();
		frost("black_ice", 1.1, 0x8A5CFF, "frost_tick", 0.5F, 0.6F).register();
		frost("rime_seal", 1.0, 0xB8F0FF, "frost_tick", 0.4F, 1.2F).motion(Motion.SEAL).register();
		frost("cryostasis", 1.15, 0xFFD87A, "frost_tick", 0.5F, 1.0F).register();
		frost("flash_freeze", 0.95, LOCK, "frost_splat", 0.25F, 1.2F).register();
		frost("blizzard", 1.3, 0xDDF6FF, "frost_whump", 0.4F, 0.8F).motion(Motion.SEAL).register();

		// Water: spheres, jets, lines, walls; rolling, dragging, sinking.
		frost("bubble", 0.95, WATER, "frost_bubble_in", 0.35F, 1.2F).register();
		frost("undertow", 1.0, WATER, "frost_drag", 0.3F, 1.2F).hook(Phase.AFTERMATH, FrostWindFx::wetSheen).register();
		frost("tidecall", 1.15, WATER, "frost_surge", 0.3F, 1.3F).hook(Phase.AFTERMATH, FrostWindFx::wetSheen).register();
		frost("tidewrit", 1.4, WATER, "frost_surge", 0.4F, 0.7F).motion(Motion.SLASH).register();
		frost("tidehook", 1.0, WATER, "frost_hook", 0.35F, 1.2F).motion(Motion.BEAM).register();
		frost("drowning_word", 1.0, 0x2E6AA8, "frost_drag", 0.3F, 0.8F).register();
		frost("current", 0.9, WATER, "frost_surge", 0.25F, 1.5F).motion(Motion.AURA).register();
		frost("tidebreath", 0.8, WATER, "frost_breath", 0.3F, 1.0F).register();

		// Wards and the innate.
		frost("frostward", 0.85, 0xCFEFFF, "frost_ward", 0.35F, 1.2F).register();
		frost("frostbloom", 1.0, 0xFFE0F4, "frost_lotus", 0.3F, 1.0F).register();
		frost("mirrorfrost", 1.0, 0xE8F4FF, "frost_mirror", 0.4F, 1.0F).register();
	}
}
