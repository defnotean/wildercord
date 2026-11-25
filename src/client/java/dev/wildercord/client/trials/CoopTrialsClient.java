package dev.wildercord.client.trials;

import dev.wildercord.trials.BiomeTrialContent;
import dev.wildercord.trials.CoopTrialContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Registers client entity renderers for Multiplayer Co-Op Trials and custom biomes. */
public final class CoopTrialsClient {
	private CoopTrialsClient() {}

	public static void init() {
		EntityRendererRegistry.register(CoopTrialContent.VAELKHOR, VaelkhorRenderer::new);
		EntityRendererRegistry.register(CoopTrialContent.KHARVOK, KharvokRenderer::new);
		EntityRendererRegistry.register(BiomeTrialContent.LEYWEAVER_MOTH,
			ctx -> new GeoMobRenderer<>(ctx, "trials/leyweaver_moth", "leyweaver_moth", 0.35F, 1.0F));
		EntityRendererRegistry.register(BiomeTrialContent.CALDERA_CINDERHOUND,
			ctx -> new GeoMobRenderer<>(ctx, "trials/caldera_cinderhound", "caldera_cinderhound", 0.65F, 1.0F));
	}
}
