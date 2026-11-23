package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordParticles;
import dev.wildercord.net.WildercordNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Client entrypoint. The custom entity renderers are the part 1.21.1 cannot carry (they are written
 * against the render state 1.21.2 introduced), so every wildercord entity is given vanilla's no-op
 * renderer and the creatures go undrawn; the server keeps the full gameplay. What the client does keep
 * is everything that does not need a render state: the spell particles, the HUDs, the screens, the
 * keybinds, the toasts and the client-side feel of aura and sword strings.
 */
public final class WildercordClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// No custom renderers here: every wildercord entity draws nothing rather than crashing the dispatcher.
		for (var id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
			if (id.getNamespace().equals("wildercord")) {
				var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
				if (type != null) {
					EntityRendererRegistry.register(type, NoopRenderer::new);
				}
			}
		}

		// The spell's own effects, drawn from particles the server sends.
		dev.wildercord.client.fx.SpellFormations.init();
		dev.wildercord.client.fx.LifeOutcomeClient.init();
		dev.wildercord.client.fx.RootCarryClient.init();
		dev.wildercord.client.fx.RainshieldClient.init();
		dev.wildercord.client.fx.CampConcordClient.init();
		// Magic drawn the plain way under a shader pack (Iris), so the pack's lighting doesn't break on it.
		dev.wildercord.client.compat.ShaderCompat.init();

		ParticleFactoryRegistry.getInstance().register(WildercordParticles.SIGIL, dev.wildercord.client.fx.SigilParticle.Provider::new);
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.SPELL_CIRCLE, new dev.wildercord.client.fx.SpellCircleParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.LIGHT, new dev.wildercord.client.fx.LightParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.SHIELD, new dev.wildercord.client.fx.ShieldCircles.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.MOTE, new dev.wildercord.client.fx.MoteParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.EARTH, new dev.wildercord.client.fx.EarthParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.CAMP, new dev.wildercord.client.fx.CampParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.LIFE, new dev.wildercord.client.fx.LifeParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.VOID_MATERIAL, new dev.wildercord.client.fx.VoidParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.AIRFLOW, new dev.wildercord.client.fx.AirflowParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.MATERIAL, new dev.wildercord.client.fx.MaterialParticle.Provider());
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.RITUAL, new dev.wildercord.client.fx.RitualCircles.Provider());

		// Tooltips, the Grimoire's toasts, and the mastery screens the Cord opens.
		dev.wildercord.client.ImbuedTooltip.init();
		dev.wildercord.client.MasteryClient.init();
		dev.wildercord.client.RelayClient.init();
		dev.wildercord.client.RelayLessonScreen.register();
		dev.wildercord.client.ReweaveClient.init();
		dev.wildercord.client.ReweaveLessonScreen.register();
		dev.wildercord.client.ExciseLessonScreen.register();
		dev.wildercord.client.ExciseClient.init();
		dev.wildercord.client.PackLessonScreen.register();
		dev.wildercord.client.LessonPackClient.init();
		// The altar and the backpack open real container menus.
		net.minecraft.client.gui.screens.MenuScreens.register(dev.wildercord.menu.WildercordMenus.FUSION_ALTAR, dev.wildercord.client.FusionAltarScreen::new);
		net.minecraft.client.gui.screens.MenuScreens.register(dev.wildercord.menu.WildercordMenus.BACKPACK, dev.wildercord.client.BackpackScreen::new);
		dev.wildercord.client.BlankRuneTooltip.init();
		dev.wildercord.client.Tooltips.init();
		// A rune's text, everywhere it's shown, only as far as the player has read it.
		dev.wildercord.client.RuneReadingText.init();
		// The server's Wildercord version: a word in chat if it isn't ours.
		dev.wildercord.client.VersionWatch.init();

		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.LeySeed.TYPE, (payload, context) ->
			dev.wildercord.client.fx.LeyMotes.setSeed(payload.seed()));
		// The runelab's notebook: the server sends the snapshot, the screen shows it over the last one.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.net.NotebookPayload.TYPE, (payload, context) -> {
			var previous = context.client().screen instanceof RuneNotebookScreen board ? board : null;
			context.client().setScreen(new RuneNotebookScreen(payload, previous));
		});
		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.ScreenFx.TYPE,
			(payload, context) -> dev.wildercord.client.fx.ScreenEffects.receive(payload));
		// The server's cost and regeneration multipliers, for the Cord screen and HUD; forgotten on leaving.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.config.Config.Sync.TYPE,
			(payload, context) -> dev.wildercord.config.Config.receive(payload));
		// The elemental climate where the player stands, for the HUD's marks and the Grimoire.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.cast.Climate.Sync.TYPE,
			(payload, context) -> dev.wildercord.cast.Climate.receive(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			dev.wildercord.config.Config.receive(null);
			dev.wildercord.cast.Climate.receive(null);
			dev.wildercord.client.fx.LeyMotes.forget();
			dev.wildercord.client.SpellHud.forget();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null) {
				// Out of the world: these let go of everything they remembered about it.
				dev.wildercord.client.fx.ArchiveAmbience.tick(client);
				dev.wildercord.client.fx.WellstoneHalo.tick(client);
				dev.wildercord.client.fx.StormSky.tick(client);
				dev.wildercord.client.fx.ScreenEffects.tick(client);
				dev.wildercord.client.fx.ShieldCircles.tick(client);
				dev.wildercord.client.fx.SoarWings.tick(client);
				dev.wildercord.client.fx.Incantations.tick(client);
				dev.wildercord.client.SigilTrace.tick(client);
				return;
			}
			if (client.isPaused()) {
				return;
			}
			dev.wildercord.client.fx.ChargeCircles.tick(client);
			dev.wildercord.client.fx.Incantations.tick(client);
			dev.wildercord.client.SigilTrace.tick(client);
			dev.wildercord.client.fx.ShieldCircles.tick(client);
			dev.wildercord.client.fx.ScreenEffects.tick(client);
			dev.wildercord.client.fx.AimPreview.tick(client);
			dev.wildercord.client.fx.StormSky.tick(client);
			dev.wildercord.client.fx.LeyMotes.tick(client);
			dev.wildercord.client.fx.SoarWings.tick(client);
			dev.wildercord.client.fx.ArchiveAmbience.tick(client);
			dev.wildercord.client.fx.WellstoneHalo.tick(client);
		});
		ClientTickEvents.END_CLIENT_TICK.register(dev.wildercord.client.fx.ChargeHum::tick);
		// Charging casters' incantations, drawn among the frame's other things as vanilla text.
		dev.wildercord.client.compat.WorldRenderCompat.registerAfterEntities(dev.wildercord.client.fx.Incantations::submit);

		dev.wildercord.client.CastingOptions.load();
		dev.wildercord.client.WildercordKeys.init();
		dev.wildercord.client.MastersArtsClient.init();
		dev.wildercord.client.SpellHud.init();
		// Aura: what aura sense outlines, and the blade's trails, impacts and banners.
		dev.wildercord.client.AuraClient.init();
		dev.wildercord.client.AuraFxClient.init();
		// Sword strings: the player's swings read into strings, their indicator and sounds.
		dev.wildercord.client.SwordStringsClient.init();
		dev.wildercord.client.fx.FrameBenchmark.init();
		dev.wildercord.client.WaypointHud.init();
		// ---- lore pack: the lore journal key and screen.
		dev.wildercord.client.lore.LoreJournalClient.init();
		Wildercord.LOGGER.info("Wildercord client initialized (1.21.1: vanilla visuals, no custom renderers)");
	}
}
