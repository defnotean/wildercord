package dev.wildercord.client.render;

import dev.wildercord.cast.DungeonEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** The dungeon bosses' models and renderers. */
public final class DungeonRenderers {
	private DungeonRenderers() {}

	public static void register() {
		ModelLayerRegistry.registerModelLayer(CinderWardenRenderer.LAYER, CinderWardenModel::createLayer);
		EntityRendererRegistry.register(DungeonEntities.CINDER_WARDEN, CinderWardenRenderer::new);
		ModelLayerRegistry.registerModelLayer(StarEaterRenderer.LAYER, StarEaterModel::createLayer);
		EntityRendererRegistry.register(DungeonEntities.STAR_EATER, StarEaterRenderer::new);
	}
}
