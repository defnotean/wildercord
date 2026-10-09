package dev.wildercord.cast;

import dev.wildercord.cast.feel.Motion;
import dev.wildercord.cast.feel.Phase;
import dev.wildercord.cast.feel.Signature;

import java.util.Set;

/**
 * The feel of the hearth runes (fx-passive pack, see {@link HearthEffects}): soft, low cues and warm accents, because these
 * are gentle long-lasting helps, not strikes. Void and time runes keep their element's early cue ({@code void_cue},
 * {@code time_cue}) at a pitch of their own; Life runes have their own authored cue and landing
 * ({@code tools/feel/life_outcomes_audio.py}); the rest borrow a quiet voice from their element's kit.
 *
 * <p>Registered from {@code VoidFeels.register()} so every place that builds the signature table gets them.</p>
 */
public final class HearthFeels {
	private HearthFeels() {}

	/** The Life hearth runes: each has {@code life_auth_<path>_cue} and {@code _outcome} in the kit. */
	public static final Set<String> LIFE = Set.of("slowburn", "lullaby", "dew_drink", "petward", "luckcharm", "steedmend", "hearthbond",
		"trailblaze");

	private static void quiet(String rune, String kit, float pitch, int accent) {
		Signature.of(rune).sound(Phase.CUE, kit, 0.45F, pitch).accent(accent).scale(0.85).register();
	}

	private static void sealed(String rune, String kit, float pitch, int accent) {
		Signature.of(rune).sound(Phase.CUE, kit, 0.5F, pitch).motion(Motion.SEAL).accent(accent).register();
	}

	public static void register() {
		// Life: their own authored cue (the landing plays where the rune takes hold, HearthEffects.glow).
		for (String rune : LIFE) {
			Signature.of(rune).sound(Phase.CUE, "life_auth_" + rune + "_cue", 0.55F, 1.0F).accent(0xD8D0A0).scale(0.85).register();
		}
		// Void: the element's early knock, low and soft.
		Signature.of("softfoot").sound(Phase.CUE, "void_cue", 0.6F, 1.3F).accent(0x8E7FB8).scale(0.85).register();
		Signature.of("hollow_pocket").sound(Phase.CUE, "void_cue", 0.6F, 0.8F).accent(0x6E5A9E).scale(0.8).register();
		Signature.of("homeward").sound(Phase.CUE, "void_cue", 0.8F, 0.6F).motion(Motion.CALL).accent(0xB7A3E8).register();
		Signature.of("enderhush").sound(Phase.CUE, "void_cue", 0.55F, 1.1F).accent(0x5FA89A).scale(0.85).register();
		// Time: the element's early tick.
		Signature.of("tarry").sound(Phase.CUE, "time_cue", 0.6F, 0.84F).accent(0xE8D49A).register();
		Signature.of("savor").sound(Phase.CUE, "time_cue", 0.5F, 1.26F).accent(0xF0C878).scale(0.85).register();
		// Arcane: glass for wards and marks, step for pointers.
		sealed("camp_ward", "arcane_stinger_glass", 0.84F, 0xC8D8FF);
		sealed("lodestar", "arcane_stinger_mark", 0.9F, 0xFFE8A8);
		quiet("gravefinder", "arcane_stinger_step", 0.75F, 0xD8E0F0);
		quiet("waymark", "arcane_stinger_mark", 1.12F, 0xFFF0C0);
		quiet("orbcall", "arcane_stinger_step", 1.26F, 0xC8FF9A);
		quiet("lantern_soul", "arcane_stinger_glass", 1.12F, 0xFFE6A0);
		sealed("rally_light", "arcane_stinger_summon", 1.0F, 0xFFF4C8);
		quiet("nightwatch", "arcane_stinger_mark", 0.84F, 0xB8C0E8);
		quiet("hearthpath", "arcane_stinger_step", 1.0F, 0xFFC890);
		quiet("lostfind", "arcane_stinger_step", 1.12F, 0xFFD860);
		quiet("stillwell", "arcane_stinger_glass", 0.75F, 0xA8C8FF);
		quiet("starchart", "arcane_stinger_mark", 1.26F, 0xD8E8FF);
		// Fire: warm, small.
		quiet("warm_cloak", "fire_kindly", 1.0F, 0xFFB070);
		sealed("ember_rest", "fire_coals", 0.9F, 0xFF9A50);
		quiet("sunbask", "fire_sun", 1.12F, 0xFFD878);
		quiet("smoke_signal", "fire_smoulder", 0.9F, 0xC8C0B8);
		// Wind: feathers and lifts.
		quiet("softsole", "wind_feather", 1.0F, 0xE0F0E8);
		quiet("steedsong", "wind_spirit_gallop", 1.0F, 0xD8F0D0);
		quiet("glidewind", "wind_soar", 1.0F, 0xC8E8F8);
		quiet("whistle", "wind_eddy", 1.26F, 0xE8F8F0);
		quiet("wayfarer_hymn", "wind_rise", 1.0F, 0xD0F0C8);
		// Earth: steady stone.
		quiet("tinker_hum", "earth_ping", 1.0F, 0xC8B898);
		quiet("keenkeep", "earth_ping", 1.26F, 0xD8D0C0);
		quiet("landread", "earth_tick", 1.0F, 0xB8A888);
		quiet("surefoot", "earth_press", 1.12F, 0xA89878);
		quiet("long_arm", "earth_creak", 1.0F, 0xC0A880);
		quiet("deepwarn", "earth_rumble", 1.0F, 0xD07040);
		// Frost: water.
		quiet("currentkin", "frost_tide_surge", 1.12F, 0x88D8F0);
		quiet("quench", "frost_breath", 1.0F, 0xA8E0F8);
		quiet("springseek", "frost_glint", 1.12F, 0x80C8F8);
		// Storm.
		quiet("skyread", "storm_cloud", 1.0F, 0xC8D8E8);
		quiet("dynamo_stride", "storm_hum", 1.12F, 0xF8E880);
		// Blood: a pulse, never a cut.
		quiet("clot", "blood_stop", 1.0F, 0xC86060);
		quiet("heartsense", "blood_heart", 1.0F, 0xE07070);
	}
}
