package dev.wildercord.client.compat;

import net.minecraft.util.ARGB;

/** 1.21.3 names the per-channel float helpers on {@code ARGB} differently from 1.21.4. */
public final class ColorCompat {
	private ColorCompat() {}

	public static float alphaFloat(int argb) { return ARGB.from8BitChannel(ARGB.alpha(argb)); }
	public static float redFloat(int argb) { return ARGB.from8BitChannel(ARGB.red(argb)); }
	public static float greenFloat(int argb) { return ARGB.from8BitChannel(ARGB.green(argb)); }
	public static float blueFloat(int argb) { return ARGB.from8BitChannel(ARGB.blue(argb)); }
}
