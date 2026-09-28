package dev.wildercord.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The vanilla particle pipeline's shared settings (shaders, vertex format, samplers), to build glow pipelines from. */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
	@Accessor("PARTICLE_SNIPPET")
	static RenderPipeline.Snippet wildercord$particleSnippet() {
		throw new AssertionError();
	}
}
