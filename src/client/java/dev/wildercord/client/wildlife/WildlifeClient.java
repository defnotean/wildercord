package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.Wildlife;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** The client's half of magical wildlife: each creature's model and renderer. */
public final class WildlifeClient {
	private WildlifeClient() {}

	public static void init() {
  SporebackRenderer.init();
  ModelLayerRegistry.registerModelLayer(WildlifeRenderers.REEDBACK_CRAB,ReedbackCrabModel::createLayer);
  EntityRendererRegistry.register(dev.wildercord.wildlife.ReedbackContent.CRAB,WildlifeRenderers.ReedbackCrabRenderer::new);
		ModelLayerRegistry.registerModelLayer(WildlifeRenderers.LANTERN_NEWT,LanternNewtModel::createLayer);
		EntityRendererRegistry.register(dev.wildercord.wildlife.WetlandContent.NEWT,WildlifeRenderers.LanternNewtRenderer::new);
		ModelLayerRegistry.registerModelLayer(WildlifeRenderers.GLIMMERWING, GlimmerwingModel::createLayer);
		ModelLayerRegistry.registerModelLayer(WildlifeRenderers.LUMEN_STAG, LumenStagModel::createLayer);
		ModelLayerRegistry.registerModelLayer(WildlifeRenderers.MOSSBACK_TORTOISE, MossbackTortoiseModel::createLayer);
		ModelLayerRegistry.registerModelLayer(WildlifeRenderers.CINDERFOX, CinderfoxModel::createLayer);
		ModelLayerRegistry.registerModelLayer(WildlifeRenderers.SKYRAY, SkyrayModel::createLayer);
		ModelLayerRegistry.registerModelLayer(WildlifeRenderers.RIMEHARE, RimehareModel::createLayer);
		EntityRendererRegistry.register(Wildlife.GLIMMERWING, WildlifeRenderers.GlimmerwingRenderer::new);
		EntityRendererRegistry.register(Wildlife.LUMEN_STAG, WildlifeRenderers.LumenStagRenderer::new);
		EntityRendererRegistry.register(Wildlife.MOSSBACK_TORTOISE, MossbackTortoiseRenderer::new);
		EntityRendererRegistry.register(Wildlife.CINDERFOX, WildlifeRenderers.CinderfoxRenderer::new);
		EntityRendererRegistry.register(Wildlife.SKYRAY, WildlifeRenderers.SkyrayRenderer::new);
		EntityRendererRegistry.register(Wildlife.RIMEHARE, WildlifeRenderers.RimehareRenderer::new);
	}
}
