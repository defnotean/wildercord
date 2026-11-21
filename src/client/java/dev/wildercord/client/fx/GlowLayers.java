package dev.wildercord.client.fx;

import dev.wildercord.client.compat.McQuadParticle;
import dev.wildercord.client.compat.RenderCompat;

/**
 * How magic is blended into the world. {@link #GLOW}: light that adds to what's behind it
 * (overlapping light burns brighter, like real light) and never hides anything behind it.
 * {@link #DARK}: the reverse, taking light away, for void magic: a hole in the world. Both are
 * vanilla's particle pipeline with a different blend and no depth writes.
 */
public final class GlowLayers {
	private GlowLayers() {}

	/** Added light: source × alpha + destination. */
	public static final McQuadParticle.Layer GLOW = RenderCompat.glowParticleLayer();
	/**
	 * Darkness is drawn with the solid particles: Improved Transparency can only add light, never take
	 * it away, so a translucent darkness layer would have no pipeline there. It writes no depth, so it
	 * still never hides what's behind it.
	 */
	public static final McQuadParticle.Layer DARK = RenderCompat.darkParticleLayer();

	/** The colour flag (see {@code Light.DARK}) that marks a light as darkness. */
	public static final int DARK_FLAG = 0x01000000;

	/**
	 * What to subtract to darken toward {@code tint}: mostly white (so it darkens evenly), less of the
	 * tint's own channels, so what's left leans to the tint.
	 */
	public static int darkColor(int tint) {
		int r = (tint >> 16) & 0xFF;
		int g = (tint >> 8) & 0xFF;
		int b = tint & 0xFF;
		return (sub(r) << 16) | (sub(g) << 8) | sub(b);
	}

	private static int sub(int c) {
		return Math.round(255 * 0.7F + (255 - c) * 0.3F);
	}

	/**
	 * A darkness colour ({@link #darkColor}: what to take away) as the plain colour to lay over the world
	 * instead, for when it can't be taken away (under a shader pack, see ShaderCompat): what's left after
	 * taking it from white, a deep shade of the tint. The alpha stays.
	 */
	public static int darkAsShade(int argb) {
		return (argb & 0xFF000000) | (~argb & 0xFFFFFF);
	}
}
