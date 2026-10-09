package dev.wildercord.cast.packs;

import dev.wildercord.cast.feel.Phase;
import dev.wildercord.cast.feel.Signature;

/**
 * The support pack's cast signatures: mending is a soft restore, guards are glass or the ward stinger, place wards
 * strike the world chime, and the harmless crowd control marks its target.
 * Its void runes (Hexguard, Lure, Stillbind, Hobble, Spook) keep their element's knock, set in {@code VoidFeels}.
 */
public final class WardFeels {
	private WardFeels() {}

	private static void cue(String stinger, float pitch, int accent, String... runes) {
		for (String rune : runes) {
			Signature.of(rune).sound(Phase.CUE, stinger, 0.55F, pitch).accent(accent).register();
		}
	}

	public static void register() {
		cue("life_stinger_restore", 1.1F, 0xF5E6A8, "worst_first", "salve", "mending_mist", "hearthglow", "aftercare", "hearthsong", "tend", "staunch");
		cue("arcane_stinger_glass", 1.05F, 0xFFE7A0, "grace", "ironhold", "aegis", "faithful", "beastguard", "emberguard", "stoutheart");
		cue("life_stinger_ward", 1.0F, 0xC9A36B, "guardlink", "hearthguard", "shieldwall", "keepsafe", "citadel", "blastward");
		cue("arcane_stinger_summon", 1.2F, 0x8FB8FF, "managift", "manawell");
		cue("arcane_stinger_step", 1.1F, 0xCFF2E0, "rally", "morale", "shrug_off", "evade", "heel", "withdraw", "sentry");
		cue("life_stinger_world", 1.0F, 0xE8C6FF, "bellward", "sanctuary", "arrowveil", "firebreak", "accord");
		cue("arcane_stinger_mark", 0.9F, 0x9E7BFF, "pacify", "taunt", "nudge", "corral", "truce", "soothe");
	}
}
