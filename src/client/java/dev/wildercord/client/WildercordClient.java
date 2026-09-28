package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.fx.AimPreview;
import dev.wildercord.client.fx.ChargeCircles;
import dev.wildercord.client.fx.LeyMotes;
import dev.wildercord.client.fx.SigilGroup;
import dev.wildercord.client.fx.SigilParticle;
import dev.wildercord.client.render.ArchivistRenderer;
import dev.wildercord.client.render.DummyModel;
import dev.wildercord.client.render.TrainingDummyRenderer;
import dev.wildercord.content.WildercordParticles;
import dev.wildercord.net.WildercordNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** Client entrypoint: keybinds, HUD, screens, renderers, the magic circle particle and the Grimoire toasts. */
public final class WildercordClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Bolts are drawn entirely with particles sent from the server.
		EntityRendererRegistry.register(WildercordEntities.RUNE_BOLT, NoopRenderer::new);
		EntityRendererRegistry.register(WildercordEntities.ARCHIVIST, ArchivistRenderer::new);
		ModelLayerRegistry.registerModelLayer(TrainingDummyRenderer.LAYER, DummyModel::createLayer);
		EntityRendererRegistry.register(WildercordEntities.TRAINING_DUMMY, TrainingDummyRenderer::new);

		ParticleGroupRegistry.register(SigilGroup.TYPE, SigilGroup::new);
		ParticleProviderRegistry.getInstance().register(WildercordParticles.SIGIL, SigilParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(WildercordParticles.SPELL_CIRCLE, new dev.wildercord.client.fx.SpellCircleParticle.Provider());
		ParticleProviderRegistry.getInstance().register(WildercordParticles.LIGHT, new dev.wildercord.client.fx.LightParticle.Provider());

		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.Discovery.TYPE, (payload, context) ->
			context.client().gui.toastManager().addToast(new GrimoireToast(payload.key())));
		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.LeySeed.TYPE, (payload, context) -> LeyMotes.setSeed(payload.seed()));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null || client.isPaused()) {
				return;
			}
			ChargeCircles.tick(client);
			AimPreview.tick(client);
			LeyMotes.tick(client);
		});
		ClientTickEvents.END_CLIENT_TICK.register(dev.wildercord.client.fx.ChargeHum::tick);
		WildercordKeys.init();
		SpellHud.init();
		Wildercord.LOGGER.info("Wildercord client initialized");
	}
}
