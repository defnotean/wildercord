package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;

/**
 * Breathing methods the game tests register for themselves. Every built-in method has arts of its own now, so a test of the common
 * arts (the five a method without arts plays, as an add-on's might) needs a method of its own: {@link #PLAIN}, in the neutral teal,
 * with no passive and no arts. Its name is in the test mod's own language file. Registering it is harmless to repeat; once
 * registered it stays for the run (its swatch shows on the Aura page's Sword strings tab, dimmed, as any method playing the common
 * arts does).
 */
final class TestMethods {
	private TestMethods() {}

	/** A method with no arts of its own and no passive. */
	static final String PLAIN = "wildercord-gametest:plain";

	/** Registers {@link #PLAIN} (both sides share the registry in a single-player test) and returns its id. */
	static String plain() {
		if (BreathingMethods.byId(PLAIN).isEmpty()) {
			AuraApi.registerMethod(new BreathingMethod(PLAIN, "", 0x40C8BE, 0xC8FFF8, BreathingMethod.Flavour.NONE));
		}
		if (AuraApi.hasArts(PLAIN)) {
			throw new AssertionError(PLAIN + " should have no arts of its own");
		}
		return PLAIN;
	}
}
