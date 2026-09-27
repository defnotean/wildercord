package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.WildercordEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** Client entrypoint: keybinds, HUD, screens and renderers. */
public final class WildercordClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Bolts are drawn entirely with particles sent from the server.
		EntityRendererRegistry.register(WildercordEntities.RUNE_BOLT, NoopRenderer::new);
		WildercordKeys.init();
		SpellHud.init();
		Wildercord.LOGGER.info("Wildercord client initialized");
	}
}
