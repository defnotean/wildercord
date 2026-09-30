package dev.wildercord.client.compat;

import dev.wildercord.client.familiar.WispRenderer;
import dev.wildercord.client.fx.GlowLayers;
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
		api.assignPipeline(GlowLayers.GLOW_PIPELINE, IrisProgram.PARTICLES_TRANSLUCENT);
		api.assignPipeline(GlowLayers.DARK_PIPELINE, IrisProgram.PARTICLES_TRANSLUCENT);
		api.assignPipeline(WispRenderer.GLOW_PIPELINE, IrisProgram.EMISSIVE_ENTITIES);
		if (api.getMinorApiRevision() >= 4) {
			api.assignPipelineShadow(GlowLayers.GLOW_PIPELINE, IrisShadowProgram.SHADOW);
			api.assignPipelineShadow(GlowLayers.DARK_PIPELINE, IrisShadowProgram.SHADOW);
			api.assignPipelineShadow(WispRenderer.GLOW_PIPELINE, IrisShadowProgram.SHADOW_ENTITIES);
		}
	}

	static boolean shaderPackInUse() {
		return IrisApi.getInstance().isShaderPackInUse();
	}

	static boolean shadowPass() {
		return IrisApi.getInstance().isRenderingShadowPass();
	}
}
