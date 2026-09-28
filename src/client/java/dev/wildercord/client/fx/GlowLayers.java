package dev.wildercord.client.fx;

import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.BlendOp;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.wildercord.Wildercord;
import dev.wildercord.client.mixin.RenderPipelinesAccessor;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * How magic is blended into the world. {@link #GLOW}: light that adds to what's behind it
 * (overlapping light burns brighter, like real light) and never hides anything behind it.
 * {@link #DARK}: the reverse, taking light away, for void magic: a hole in the world. Both are
 * vanilla's particle pipeline with a different blend and no depth writes.
 */
public final class GlowLayers {
	private GlowLayers() {}

	/** Added light: source × alpha + destination. */
	public static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/glow_particle"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();

	/** Taken light: destination − source × alpha. */
	public static final RenderPipeline DARK_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/dark_particle"))
		.withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendOp.REVERSE_SUBTRACT)))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();

	public static final SingleQuadParticle.Layer GLOW = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, GLOW_PIPELINE);
	public static final SingleQuadParticle.Layer DARK = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, DARK_PIPELINE);

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
}
