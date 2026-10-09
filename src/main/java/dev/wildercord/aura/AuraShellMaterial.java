package dev.wildercord.aura;

/** Pure shell colour calculation, shared by the classic and articulated client render paths. */
public final class AuraShellMaterial {
	private AuraShellMaterial() {}

	/**
	 * Colours an already-funded shell. The caller keeps ownership of presence and the original sine sample (-1 to 1).
	 * Reduced flash holds the normal base opacity and tint, suppressing breathing and the short white hit flare.
	 */
	public static int argb(int auraColor, float pulse, float ticksSinceStrike, boolean reducedFlash) {
		float flare = !reducedFlash && ticksSinceStrike >= 0 && ticksSinceStrike < 8 ? 1 - ticksSinceStrike / 8F : 0;
		float alpha = Math.min(1.0F, 0.3F + (reducedFlash ? 0 : 0.07F * pulse) + 0.4F * flare);
		float white = 0.1F + 0.3F * flare;
		int r = Math.round(((auraColor >> 16) & 0xFF) * (1 - white) + 255 * white);
		int g = Math.round(((auraColor >> 8) & 0xFF) * (1 - white) + 255 * white);
		int b = Math.round((auraColor & 0xFF) * (1 - white) + 255 * white);
		return (Math.clamp(Math.round(alpha * 255), 0, 255) << 24) | (r << 16) | (g << 8) | b;
	}
}
