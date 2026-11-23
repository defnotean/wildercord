package dev.wildercord.client.compat;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 1.21.1 has no {@code net.minecraft.util.ARGB} (the colour helpers arrived with the render-state
 * versions). These are the two magic uses, done by hand: packing floats into an ARGB int, and reading
 * an RGB24 colour as a linear triple.
 */
public final class ARGB {
	private ARGB() {}

	public static int colorFromFloat(float alpha, float red, float green, float blue) {
		return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
	}

	public static Vector3fc vector3fFromRGB24(int rgb) {
		return new Vector3f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
	}

	public static int alpha(int argb) { return (argb >> 24) & 0xFF; }

	public static int red(int argb) { return (argb >> 16) & 0xFF; }

	public static int green(int argb) { return (argb >> 8) & 0xFF; }

	public static int blue(int argb) { return argb & 0xFF; }

	public static float from8BitChannel(int channel) { return channel / 255.0F; }

	private static int channel(float value) {
		return Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
	}
}
