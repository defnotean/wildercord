package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.Wildlife;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

/** The client's half of magical wildlife: each creature's model and renderer. */
public final class WildlifeClient {
	private WildlifeClient() {}

	public static void init() {
  SporebackRenderer.init();
  RootmoltRenderer.init();
  CinderBailiffRenderer.init();
  SiltcrestRenderer.init();
  MossveilDormouseRenderer.init();
  BlackBobcatRenderer.init();
  ThistleHedgehogRenderer.init();
  SnowcapStoatRenderer.init();
  BrookOtterRenderer.init();
  CinnabarPandaRenderer.init();
  SunwatchMeerkatRenderer.init();
  StonecrestPuffinRenderer.init();
  CapcrownSnailRenderer.init();
  GlowberryDormouseRenderer.init();
  HushwingOwletRenderer.init();
  BlossomTanukiRenderer.init();
  MirebathCapybaraRenderer.init();
  TussockPikaRenderer.init();
  FloePenguinRenderer.init();
  BrookbillDuckRenderer.init();
  PredatorRenderers.init();
  RidgebackStagRenderer.init();
  BurrowAndReefRenderers.init();
  dev.wildercord.client.fx.MossveilFilterClient.init();
  EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.REEDBACK_CRAB,ReedbackCrabModel::createLayer);
  EntityRendererRegistry.register(dev.wildercord.wildlife.ReedbackContent.CRAB,WildlifeRenderers.ReedbackCrabRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.LANTERN_NEWT,LanternNewtModel::createLayer);
		EntityRendererRegistry.register(dev.wildercord.wildlife.WetlandContent.NEWT,WildlifeRenderers.LanternNewtRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.GLIMMERWING, GlimmerwingModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.LUMEN_STAG, LumenStagModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.MOSSBACK_TORTOISE, MossbackTortoiseModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.CINDERFOX, CinderfoxModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.SKYRAY, SkyrayModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(WildlifeRenderers.RIMEHARE, RimehareModel::createLayer);
		EntityRendererRegistry.register(Wildlife.GLIMMERWING, WildlifeRenderers.GlimmerwingRenderer::new);
		EntityRendererRegistry.register(Wildlife.LUMEN_STAG, WildlifeRenderers.LumenStagRenderer::new);
		EntityRendererRegistry.register(Wildlife.MOSSBACK_TORTOISE, MossbackTortoiseRenderer::new);
		EntityRendererRegistry.register(Wildlife.CINDERFOX, WildlifeRenderers.CinderfoxRenderer::new);
		EntityRendererRegistry.register(Wildlife.SKYRAY, WildlifeRenderers.SkyrayRenderer::new);
		EntityRendererRegistry.register(dev.wildercord.wildlife.MountContent.BONDED_SKYRAY, WildlifeRenderers.SkyrayRenderer::new);
		EntityRendererRegistry.register(Wildlife.RIMEHARE, WildlifeRenderers.RimehareRenderer::new);
	}
}
