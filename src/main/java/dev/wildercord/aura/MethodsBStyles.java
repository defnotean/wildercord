package dev.wildercord.aura;

import java.util.List;

/**
 * The presentation ids of Echo, Dawn and Venom's fifteen arts: {@link #FIRST} onward, five per method in slot order, well clear of
 * the built-in styles. Each has its own Classic timeline ({@link MethodsBStyleAnimation}); the articulated renderer borrows the
 * body of the kindred built-in form named in {@link #articulated} (a long cut for a long cut, a dash for a dash).
 */
public final class MethodsBStyles {
	private MethodsBStyles() {}

	public static final int FIRST = 140;

	/** The arts in id order: {@code FIRST + i}. */
	public static final List<String> ARTS = List.of(
		"ringing_cut", "resonant_chord", "counterpoint", "reverb_step", "grand_resonance",
		"first_light", "sunrise_arc", "halo_guard", "dawnbreak_rush", "noon_zenith",
		"fang_strike", "spitting_cobra", "shed_skin", "serpent_slither", "hydra_coil");

	private static final int[] ARTICULATED = {
		ArticulatedCombatPose.ECHO_CUT, ArticulatedCombatPose.CRACKLE, ArticulatedCombatPose.GLACIER_MIRROR,
		ArticulatedCombatPose.RIFT_STEP, ArticulatedCombatPose.EVENT_HORIZON,
		ArticulatedCombatPose.KINDLING_DRAW, ArticulatedCombatPose.RISING_CINDERS, ArticulatedCombatPose.CONSTELLATION_GUARD,
		ArticulatedCombatPose.COMET_DASH, ArticulatedCombatPose.SUNFALL,
		ArticulatedCombatPose.STAR_NEEDLE, ArticulatedCombatPose.THORN_LASH, ArticulatedCombatPose.REWIND_LEAP,
		ArticulatedCombatPose.SKATE, ArticulatedCombatPose.GROVES_HEART};

	public static boolean owns(int id) {
		return id >= FIRST && id < FIRST + ARTS.size();
	}

	/** The slot of the art behind {@code id} (five per method, in slot order). */
	public static int slot(int id) {
		return (id - FIRST) % 5;
	}

	/** The built-in articulated form {@code id} borrows its body from, or -1 when it isn't one of the pack's. */
	public static int articulated(int id) {
		return owns(id) ? ARTICULATED[id - FIRST] : -1;
	}
}
