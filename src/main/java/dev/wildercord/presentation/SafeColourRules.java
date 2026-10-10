package dev.wildercord.presentation;

/**
 * Colour-blind-safe telegraphs (0.13): with the option on, every magic circle, ring and warning reticle is drawn in the
 * nearest colour of the Okabe-Ito palette instead of its own. That palette's eight colours stay apart under the common
 * kinds of colour blindness, so a red warning and a green ward never look alike. Pale and grey colours keep their own,
 * since brightness alone already reads. Only the colour's hue picks the swap, so the same element always gets the same
 * colour. Pure, so the mapping is unit tested.
 */
public final class SafeColourRules {
	private SafeColourRules() {}

	public static final int VERMILLION = 0xD55E00;
	public static final int ORANGE = 0xE69F00;
	public static final int YELLOW = 0xF0E442;
	public static final int BLUISH_GREEN = 0x009E73;
	public static final int SKY_BLUE = 0x56B4E9;
	public static final int BLUE = 0x0072B2;
	public static final int REDDISH_PURPLE = 0xCC79A7;
	/** Below this saturation a colour is grey or white enough to keep. */
	public static final float GREY = 0.2F;

	/** {@code argb}'s colour swapped for its safe one; the top byte (alpha or flags) is kept as it was. */
	public static int safe(int argb) {
		int top = argb & 0xFF000000;
		int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
		int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		if (max == 0 || (max - min) / (float) max < GREY) return argb;
		return top | byHue(hue(r, g, b, max, min));
	}

	/** The safe colour for a hue in degrees. */
	public static int byHue(float hue) {
		float h = ((hue % 360) + 360) % 360;
		if (h < 20 || h >= 345) return VERMILLION;
		if (h < 45) return ORANGE;
		if (h < 70) return YELLOW;
		if (h < 165) return BLUISH_GREEN;
		if (h < 200) return SKY_BLUE;
		if (h < 255) return BLUE;
		return REDDISH_PURPLE;
	}

	private static float hue(int r, int g, int b, int max, int min) {
		float d = max - min;
		float h;
		if (max == r) h = ((g - b) / d) % 6;
		else if (max == g) h = (b - r) / d + 2;
		else h = (r - g) / d + 4;
		return h * 60;
	}
}
