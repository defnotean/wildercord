package dev.wildercord.client.compat;

import net.irisshaders.iris.api.v0.IrisApi;

/**
 * The calls into Iris's API. Only loaded when Iris is installed (see ShaderCompat).
 *
 * 1.21.4 pins Iris 1.8.8, whose API has no pipeline assignment: the magic quads ride vanilla's
 * particle and eye layers under a pack anyway (see ShaderCompat), so there is nothing to hand over.
 */
final class IrisBridge {
	private IrisBridge() {}

		static boolean shaderPackInUse() {
		return IrisApi.getInstance().isShaderPackInUse();
	}

	static boolean shadowPass() {
		return IrisApi.getInstance().isRenderingShadowPass();
	}
}
