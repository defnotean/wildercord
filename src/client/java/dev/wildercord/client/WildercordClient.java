package dev.wildercord.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * 1.21.1 client entrypoint. The 1.21.3+ render-state client is dropped: the custom renderers are gone,
 * so the 1.21.1 client launches on vanilla visuals only — custom entities are functional but not visibly
 * drawn (their models and render layers live in the dropped client package, not in the 1.21.1 renderers).
 * The server keeps the full gameplay.
 */
public final class WildercordClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register a renderer for every wildercord entity so the dispatcher does not hard-fail.
		for (var id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			if (id.getNamespace().equals("wildercord")) {
				var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
				if (type != null) {
					EntityRendererRegistry.register(type, NoopRenderer::new);
				}
			}
		}
	}
}
