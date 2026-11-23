package dev.wildercord.client.ocean;

import dev.wildercord.ocean.OceanEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Registers all Deep Ocean Expansion client model layers and entity renderers.
 */
public final class OceanClient {
	private OceanClient() {}

	public static void init() {
		EntityModelLayerRegistry.registerModelLayer(BarnacleHulkRenderer.LAYER, BarnacleHulkModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(SirenVeilJellyRenderer.LAYER, SirenVeilJellyModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(CoralSpineMakoRenderer.LAYER, CoralSpineMakoModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(DreadNautilusRenderer.LAYER, DreadNautilusModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(ThalassorRenderer.LAYER, ThalassorModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(BrineMerchantRenderer.LAYER, BrineMerchantModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(LumiSeahorseRenderer.LAYER, LumiSeahorseModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(PearlPupRenderer.LAYER, PearlPupModel::createLayer);
		EntityModelLayerRegistry.registerModelLayer(SingingMoonWhaleRenderer.LAYER, SingingMoonWhaleModel::createLayer);

		EntityModelLayerRegistry.registerModelLayer(AbyssalDivingSuitLayer.HELM_LAYER, AbyssalDivingSuitLayer::createHelmLayer);
		EntityModelLayerRegistry.registerModelLayer(AbyssalDivingSuitLayer.TANKS_LAYER, AbyssalDivingSuitLayer::createTanksLayer);
		EntityModelLayerRegistry.registerModelLayer(AbyssalDivingSuitLayer.RIGHT_ARM_LAYER, AbyssalDivingSuitLayer::createRightArmLayer);
		EntityModelLayerRegistry.registerModelLayer(AbyssalDivingSuitLayer.LEFT_ARM_LAYER, AbyssalDivingSuitLayer::createLeftArmLayer);
		EntityModelLayerRegistry.registerModelLayer(AbyssalDivingSuitLayer.RIGHT_LEG_LAYER, AbyssalDivingSuitLayer::createRightLegLayer);
		EntityModelLayerRegistry.registerModelLayer(AbyssalDivingSuitLayer.LEFT_LEG_LAYER, AbyssalDivingSuitLayer::createLeftLegLayer);

		EntityRendererRegistry.register(OceanEntities.BARNACLE_HULK_DREDGER, BarnacleHulkRenderer::new);
		EntityRendererRegistry.register(OceanEntities.SIREN_VEIL_JELLY, SirenVeilJellyRenderer::new);
		EntityRendererRegistry.register(OceanEntities.CORAL_SPINE_MAKO, CoralSpineMakoRenderer::new);
		EntityRendererRegistry.register(OceanEntities.DREAD_NAUTILUS_SENTINEL, DreadNautilusRenderer::new);
		EntityRendererRegistry.register(OceanEntities.THALASSOR, ThalassorRenderer::new);
		EntityRendererRegistry.register(OceanEntities.BRINE_MERCHANT, BrineMerchantRenderer::new);
		EntityRendererRegistry.register(OceanEntities.LUMI_SEAHORSE, LumiSeahorseRenderer::new);
		EntityRendererRegistry.register(OceanEntities.PEARL_PUP, PearlPupRenderer::new);
		EntityRendererRegistry.register(OceanEntities.SINGING_MOON_WHALE, SingingMoonWhaleRenderer::new);
		EntityRendererRegistry.register(OceanEntities.THROWN_HARPOON, ctx -> new ThrownItemRenderer<>(ctx, 1.35F, true));
	}
}
