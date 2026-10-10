package dev.wildercord.player;

import dev.wildercord.spell.Circles;

/** The mana rebate belongs only to the current, nonlethal native health wound. */
public final class ManaSkinRules {
	private ManaSkinRules() {}

	/** Preserve the existing minimum recovery; tiny wounds and tiny mana reserves do not trigger a rebate. */
	public static final float MIN_RECOVERY = 0.25F;

	public static float recovery(float healthBefore, float healthAfter, float healthNow, float mana) {
		return recovery(healthBefore, healthAfter, healthNow, mana, Circles.MANA_SKIN_SHARE);
	}

	/** @param shareOfWound the share of the wound turned back (the Path of the Ward turns back more) */
	public static float recovery(float healthBefore, float healthAfter, float healthNow, float mana, double shareOfWound) {
		if (!Float.isFinite(healthBefore) || !Float.isFinite(healthAfter) || !Float.isFinite(healthNow) || !Float.isFinite(mana)
				|| healthAfter <= 0 || healthNow <= 0 || healthBefore <= healthAfter || mana <= 0) return 0;
		float share = (float) Math.min((healthBefore - healthAfter) * shareOfWound,
			Math.min(Math.max(0, healthBefore - healthNow), mana / Circles.MANA_SKIN_COST));
		return share >= MIN_RECOVERY ? share : 0;
	}

	/** Healing suppression costs nothing; rounding cannot overdraw the available mana. */
	public static float payment(float restored, float mana) {
		if (!Float.isFinite(restored) || !Float.isFinite(mana) || restored <= 0 || mana <= 0) return 0;
		return Math.min(mana, restored * Circles.MANA_SKIN_COST);
	}
}
