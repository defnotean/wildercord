package dev.wildercord.client.render;

import dev.wildercord.cast.DungeonEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

/** The dungeon bosses' models and renderers. */
public final class DungeonRenderers {
	private DungeonRenderers() {}

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(RootGuardianRenderer.LAYER, RootGuardianModel::createLayer);
		EntityRendererRegistry.register(DungeonEntities.ROOT_GUARDIAN, RootGuardianRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(StormConductorRenderer.LAYER, StormConductorModel::createLayer);
		EntityRendererRegistry.register(DungeonEntities.STORM_CONDUCTOR, StormConductorRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(CinderWardenRenderer.LAYER, CinderWardenModel::createLayer);
		EntityRendererRegistry.register(DungeonEntities.CINDER_WARDEN, CinderWardenRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(StarEaterRenderer.LAYER, StarEaterModel::createLayer);
		EntityRendererRegistry.register(DungeonEntities.STAR_EATER, StarEaterRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(TideScribeRenderer.LAYER, TideScribeModel::createLayer);
		EntityRendererRegistry.register(DungeonEntities.TIDE_SCRIBE, TideScribeRenderer::new);
	}
}
