package dev.wildercord.client.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.api.v0.IrisShadowProgram;

/** The calls into Iris's API. Only loaded when Iris is installed (see ShaderCompat). */
final class IrisBridge {
	private IrisBridge() {}

	/**
	 * Tells Iris which of the pack's programs draws each of Wildercord's own pipelines. Magic is drawn
	 * with vanilla's pipelines while a pack is on, so this only matters for a frame drawn just as one is
	 * switched on; without it Iris would draw them with no program of the pack's at all.
	 */
	static void assignPipelines() {
		IrisApi api = IrisApi.getInstance();
		// Pipelines could be assigned from API 0.3, and for the shadow pass from 0.4.
		if (api.getMinorApiRevision() < 3) {
			return;
		}
		api.assignPipeline(RenderCompat.glowParticlePipeline(), IrisProgram.PARTICLES_TRANSLUCENT);
		api.assignPipeline(RenderCompat.darkParticlePipeline(), IrisProgram.PARTICLES_TRANSLUCENT);
		api.assignPipeline(RenderCompat.wispGlowPipeline(), IrisProgram.EMISSIVE_ENTITIES);
		api.assignPipeline(RenderCompat.auraBodyGlowPipeline(), IrisProgram.EMISSIVE_ENTITIES);
		if (api.getMinorApiRevision() >= 4) {
			assignShadow(api, RenderCompat.glowParticlePipeline(), IrisShadowProgram.SHADOW);
			assignShadow(api, RenderCompat.darkParticlePipeline(), IrisShadowProgram.SHADOW);
			assignShadow(api, RenderCompat.wispGlowPipeline(), IrisShadowProgram.SHADOW_ENTITIES);
			assignShadow(api, RenderCompat.auraBodyGlowPipeline(), IrisShadowProgram.SHADOW_ENTITIES);
		}
	}

	/**
	 * 26.1 pins Iris 1.10.9, whose compile API predates assignPipelineShadow; the call is reflective so
	 * the seam compiles against the pinned jar and still assigns shadow programs on newer Iris.
	 */
	private static void assignShadow(IrisApi api, RenderPipeline pipeline, IrisShadowProgram program) {
		try {
			api.getClass().getMethod("assignPipelineShadow", RenderPipeline.class, IrisShadowProgram.class)
				.invoke(api, pipeline, program);
		} catch (ReflectiveOperationException absent) {
			// Iris without the shadow-pass API draws the pipeline with no shadow program.
		}
	}

	static boolean shaderPackInUse() {
		return IrisApi.getInstance().isShaderPackInUse();
	}

	static boolean shadowPass() {
		return IrisApi.getInstance().isRenderingShadowPass();
	}
}
