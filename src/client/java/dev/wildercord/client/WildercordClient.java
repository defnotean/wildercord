package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.fx.AimPreview;
import dev.wildercord.client.fx.ArchiveAmbience;
import dev.wildercord.client.fx.ChargeCircles;
import dev.wildercord.client.fx.LeyMotes;
import dev.wildercord.client.fx.RuneAura;
import dev.wildercord.client.fx.SigilGroup;
import dev.wildercord.client.fx.SigilParticle;
import dev.wildercord.client.fx.WellstoneHalo;
import dev.wildercord.client.render.ArchivistModel;
import dev.wildercord.client.render.ArchivistRenderer;
import dev.wildercord.client.render.DummyModel;
import dev.wildercord.client.render.RuneMarksLayer;
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
		dev.wildercord.client.fx.SpellFormations.init();
		// Bolts are drawn entirely with particles sent from the server.
		EntityRendererRegistry.register(WildercordEntities.RUNE_BOLT, NoopRenderer::new);
		ModelLayerRegistry.registerModelLayer(ArchivistRenderer.LAYER, ArchivistModel::createLayer);
		EntityRendererRegistry.register(WildercordEntities.ARCHIVIST, ArchivistRenderer::new);
		ModelLayerRegistry.registerModelLayer(TrainingDummyRenderer.LAYER, DummyModel::createLayer);
		// The Cord on every player's wrist.
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.CordLayer.BAND, dev.wildercord.client.render.CordModel::createBand);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.CordLayer.SLIM_BAND, dev.wildercord.client.render.CordModel::createSlimBand);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.CordLayer.BEAD, dev.wildercord.client.render.CordModel::createBead);
		// Casting gear in its slots, worn on the back, off a shoulder and at the hip.
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.BELT, dev.wildercord.client.render.GearModel::createBelt);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.ARMORED_BELT, dev.wildercord.client.render.GearModel::createArmoredBelt);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.TIE, dev.wildercord.client.render.GearModel::createTie);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.LOOP, dev.wildercord.client.render.GearModel::createLoop);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.MOTE, dev.wildercord.client.render.GearModel::createMote);
		// A worn backpack, on the back with its straps down the chest.
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.BACKPACK, dev.wildercord.client.render.BackpackModel::createPack);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.BACKPACK_STRAP, dev.wildercord.client.render.BackpackModel::createStrap);
		// Aura armour's shell, the body drawn again a little larger.
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.AuraShellLayer.SHELL, dev.wildercord.client.render.AuraShellLayer::createShell);
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.AuraShellLayer.SLIM_SHELL, dev.wildercord.client.render.AuraShellLayer::createSlimShell);
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
				helper.register(new dev.wildercord.client.render.CordLayer(avatar, context));
				helper.register(new dev.wildercord.client.render.GearLayer(avatar, context));
				helper.register(new dev.wildercord.client.render.AuraShellLayer(avatar, context));
			}
		});
		EntityRendererRegistry.register(WildercordEntities.TRAINING_DUMMY, TrainingDummyRenderer::new);
		dev.wildercord.client.render.DungeonRenderers.register();
		RuneMarksLayer.register();
		dev.wildercord.client.familiar.FamiliarClient.init();
		ModelLayerRegistry.registerModelLayer(dev.wildercord.client.pet.CinnamonRenderer.LAYER, dev.wildercord.client.pet.CinnamonModel::createLayer);
		EntityRendererRegistry.register(dev.wildercord.pet.CinnamonContent.CINNAMON, dev.wildercord.client.pet.CinnamonRenderer::new);
		// The magical monsters of the wilds.
		dev.wildercord.client.monster.MonsterClient.init();
		dev.wildercord.client.wildlife.WildlifeClient.init();
		ClientTickEvents.END_CLIENT_TICK.register(dev.wildercord.client.cosmetic.CordTrails::tick);
		// Magic drawn the plain way under a shader pack (Iris), so the pack's lighting doesn't break on it.
		dev.wildercord.client.compat.ShaderCompat.init();

		ParticleGroupRegistry.register(SigilGroup.TYPE, SigilGroup::new);
		ParticleProviderRegistry.getInstance().register(WildercordParticles.SIGIL, SigilParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(WildercordParticles.SPELL_CIRCLE, new dev.wildercord.client.fx.SpellCircleParticle.Provider());
		ParticleProviderRegistry.getInstance().register(WildercordParticles.LIGHT, new dev.wildercord.client.fx.LightParticle.Provider());
		ParticleProviderRegistry.getInstance().register(WildercordParticles.SHIELD, new dev.wildercord.client.fx.ShieldCircles.Provider());
		ParticleProviderRegistry.getInstance().register(WildercordParticles.MOTE, new dev.wildercord.client.fx.MoteParticle.Provider());
		ParticleProviderRegistry.getInstance().register(WildercordParticles.MATERIAL, new dev.wildercord.client.fx.MaterialParticle.Provider());
		ParticleProviderRegistry.getInstance().register(WildercordParticles.RITUAL, new dev.wildercord.client.fx.RitualCircles.Provider());
		ImbuedTooltip.init();
		MasteryClient.init();
		net.minecraft.client.gui.screens.MenuScreens.register(dev.wildercord.menu.WildercordMenus.FUSION_ALTAR, FusionAltarScreen::new);
		net.minecraft.client.gui.screens.MenuScreens.register(dev.wildercord.menu.WildercordMenus.BACKPACK, BackpackScreen::new);
		BlankRuneTooltip.init();
		Tooltips.init();

		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.Discovery.TYPE, (payload, context) ->
			context.client().gui.toastManager().addToast(new GrimoireToast(payload.key())));
		// This world's magic revealed (a resonance found, a quirk met): a toast with the name just shown.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.cast.WorldResonances.Revealed.TYPE, (payload, context) ->
			context.client().gui.toastManager().addToast(GrimoireToast.revealed(payload.kind(), payload.name(), payload.color())));
		// A rune's text, everywhere it's shown, only as far as the player has read it.
		RuneReadingText.init();
		// An affinity reached a new level: a toast in its element's colour.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.cast.PlayerAffinities.Rise.TYPE, (payload, context) ->
			context.client().gui.toastManager().addToast(GrimoireToast.affinity(payload.element(), payload.level())));
		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.LeySeed.TYPE, (payload, context) -> LeyMotes.setSeed(payload.seed()));
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.net.NotebookPayload.TYPE, (payload, context) -> {
			var previous = context.client().gui.screen() instanceof RuneNotebookScreen board ? board : null;
			context.client().gui.setScreen(new RuneNotebookScreen(payload, previous));
		});
		// The server's Wildercord version: a word in chat if it isn't ours (runes one of us doesn't know go silent).
		VersionWatch.init();
		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.ScreenFx.TYPE,
			(payload, context) -> dev.wildercord.client.fx.ScreenEffects.receive(payload));
		// The server's cost and regeneration multipliers, for the Cord screen and HUD; forgotten on leaving.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.config.Config.Sync.TYPE, (payload, context) -> dev.wildercord.config.Config.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.runesmith.Contracts.ShowBoard.TYPE, (payload, context) -> context.client().gui.setScreen(new ContractBoardScreen(payload)));
		// The elemental climate where the player stands, for the HUD's marks and the Grimoire.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.cast.Climate.Sync.TYPE, (payload, context) -> dev.wildercord.cast.Climate.receive(payload));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			dev.wildercord.config.Config.receive(null);
			dev.wildercord.cast.Climate.receive(null);
			CordGlow.clear();
			LeyMotes.forget();
			SpellHud.forget();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null) {
				// Out of the world: these let go of everything they remembered about it.
				ArchiveAmbience.tick(client);
				WellstoneHalo.tick(client);
				dev.wildercord.client.fx.StormSky.tick(client);
				dev.wildercord.client.fx.BoltComets.tick(client);
				dev.wildercord.client.fx.ScreenEffects.tick(client);
				dev.wildercord.client.fx.ShieldCircles.tick(client);
				dev.wildercord.client.fx.RitualCircles.tick(client);
				dev.wildercord.client.fx.SoarWings.tick(client);
				dev.wildercord.client.fx.Incantations.tick(client);
				SigilTrace.tick(client);
				return;
			}
			if (client.isPaused()) {
				return;
			}
			ChargeCircles.tick(client);
			dev.wildercord.client.fx.Incantations.tick(client);
			SigilTrace.tick(client);
			dev.wildercord.client.fx.ShieldCircles.tick(client);
			dev.wildercord.client.fx.RitualCircles.tick(client);
			dev.wildercord.client.fx.BoltComets.tick(client);
			dev.wildercord.client.fx.ScreenEffects.tick(client);
			AimPreview.tick(client);
			dev.wildercord.client.fx.StormSky.tick(client);
			LeyMotes.tick(client);
			RuneAura.tick(client);
			dev.wildercord.client.fx.SoarWings.tick(client);
			ArchiveAmbience.tick(client);
			WellstoneHalo.tick(client);
		});
		ClientTickEvents.END_CLIENT_TICK.register(dev.wildercord.client.fx.ChargeHum::tick);
		// Charging casters' incantations, drawn among the frame's other things as vanilla text.
		net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(dev.wildercord.client.fx.Incantations::submit);
		CastingOptions.load();
		WildercordKeys.init();
		SpellHud.init();
		// Aura: what aura sense outlines (the bar is SpellHud's, the blade's glow AuraBlade's).
		AuraClient.init();
		dev.wildercord.client.fx.FrameBenchmark.init();
		WaypointHud.init();
		Wildercord.LOGGER.info("Wildercord client initialized");
	}
}
