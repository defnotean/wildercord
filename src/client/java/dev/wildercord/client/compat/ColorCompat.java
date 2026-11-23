package dev.wildercord.client.compat;

/** 1.21.1 has no {@code net.minecraft.util.ARGB}; the per-channel float helpers are done by hand here. */
public final class ColorCompat {
	private ColorCompat() {}

	private static float from8BitChannel(int channel) { return channel / 255.0F; }

	public static float alphaFloat(int argb) { return from8BitChannel((argb >> 24) & 0xFF); }
	public static float redFloat(int argb) { return from8BitChannel((argb >> 16) & 0xFF); }
	public static float greenFloat(int argb) { return from8BitChannel((argb >> 8) & 0xFF); }
	public static float blueFloat(int argb) { return from8BitChannel(argb & 0xFF); }
}
