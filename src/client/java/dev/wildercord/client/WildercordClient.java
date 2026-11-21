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
import net.fabricmc.fabric.api.client.particle.v1.ParticleRendererRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** Client entrypoint: keybinds, HUD, screens, renderers, the magic circle particle and the Grimoire toasts. */
public final class WildercordClient implements ClientModInitializer {
	public static boolean inStartClientTick;

	@Override
	public void onInitializeClient() {
		dev.wildercord.client.fx.SpellFormations.init();
		dev.wildercord.client.fx.LifeOutcomeClient.init();
		dev.wildercord.client.fx.TidewardClient.init();
		dev.wildercord.client.fx.RootCarryClient.init();
		dev.wildercord.client.fx.RainshieldClient.init();
		dev.wildercord.client.fx.CampConcordClient.init();
		// Bolts are drawn entirely with particles sent from the server.
		EntityRendererRegistry.register(WildercordEntities.RUNE_BOLT, NoopRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(ArchivistRenderer.LAYER, ArchivistModel::createLayer);
		EntityRendererRegistry.register(WildercordEntities.ARCHIVIST, ArchivistRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(TrainingDummyRenderer.LAYER, DummyModel::createLayer);
		// The Cord on every player's wrist.
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.CordLayer.BAND, dev.wildercord.client.render.CordModel::createBand);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.CordLayer.SLIM_BAND, dev.wildercord.client.render.CordModel::createSlimBand);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.CordLayer.BEAD, dev.wildercord.client.render.CordModel::createBead);
		// Casting gear in its slots, worn on the back, off a shoulder and at the hip.
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.BELT, dev.wildercord.client.render.GearModel::createBelt);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.ARMORED_BELT, dev.wildercord.client.render.GearModel::createArmoredBelt);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.TIE, dev.wildercord.client.render.GearModel::createTie);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.LOOP, dev.wildercord.client.render.GearModel::createLoop);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.MOTE, dev.wildercord.client.render.GearModel::createMote);
		// A worn backpack, on the back with its straps down the chest.
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.BACKPACK, dev.wildercord.client.render.BackpackModel::createPack);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.GearLayer.BACKPACK_STRAP, dev.wildercord.client.render.BackpackModel::createStrap);
		// Aura armour's shell, the body drawn again a little larger.
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.AuraShellLayer.SHELL, dev.wildercord.client.render.AuraShellLayer::createShell);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.render.AuraShellLayer.SLIM_SHELL, dev.wildercord.client.render.AuraShellLayer::createSlimShell);
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
				helper.register(new dev.wildercord.client.render.CordLayer(avatar, context));
				helper.register(new dev.wildercord.client.render.GearLayer(avatar, context));
				helper.register(new dev.wildercord.client.render.AuraShellLayer(avatar, context));
				helper.register(new dev.wildercord.client.render.AuraBodyLayer(avatar));
			}
		});
		EntityRendererRegistry.register(WildercordEntities.TRAINING_DUMMY, TrainingDummyRenderer::new);
		dev.wildercord.client.render.DungeonRenderers.register();
		RuneMarksLayer.register();
		dev.wildercord.client.familiar.FamiliarClient.init();
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.pet.CinnamonRenderer.LAYER, dev.wildercord.client.pet.CinnamonModel::createLayer);
		EntityRendererRegistry.register(dev.wildercord.pet.CinnamonContent.CINNAMON, dev.wildercord.client.pet.CinnamonRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.wildlife.ShrineCatRenderer.LAYER, dev.wildercord.client.wildlife.ShrineCatModel::createLayer);
		EntityRendererRegistry.register(dev.wildercord.wildlife.ShrineCatContent.SHRINE_CAT, dev.wildercord.client.wildlife.ShrineCatRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(dev.wildercord.client.wildlife.SakuraVillagerRenderer.LAYER, dev.wildercord.client.wildlife.SakuraVillagerModel::createLayer);
		EntityRendererRegistry.register(dev.wildercord.wildlife.ShrineCatContent.SAKURA_VILLAGER, dev.wildercord.client.wildlife.SakuraVillagerRenderer::new);
		// The magical monsters of the wilds.
		dev.wildercord.client.monster.MonsterClient.init();
		dev.wildercord.client.auraworld.AuraWorldClient.init();
		dev.wildercord.client.wildlife.WildlifeClient.init();
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(dev.wildercord.town.Town.KEEPER, net.minecraft.client.renderer.entity.WanderingTraderRenderer::new);
		ClientTickEvents.END_CLIENT_TICK.register(dev.wildercord.client.cosmetic.CordTrails::tick);
		// Magic drawn the plain way under a shader pack (Iris), so the pack's lighting doesn't break on it.
		dev.wildercord.client.compat.ShaderCompat.init();

		ParticleRendererRegistry.register(SigilGroup.TYPE, SigilGroup::new);
		ParticleFactoryRegistry.getInstance().register(WildercordParticles.SIGIL, SigilParticle.Provider::new);
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
		ImbuedTooltip.init();
		MasteryClient.init();
		RelayClient.init();
		RelayLessonScreen.register();
        ReweaveClient.init();
        ReweaveLessonScreen.register();
        ExciseLessonScreen.register();
        BreakthroughScreen.register();
        ExciseClient.init();
        PackLessonScreen.register();
        LessonPackClient.init();
		net.minecraft.client.gui.screens.MenuScreens.register(dev.wildercord.menu.WildercordMenus.FUSION_ALTAR, FusionAltarScreen::new);
		net.minecraft.client.gui.screens.MenuScreens.register(dev.wildercord.menu.WildercordMenus.BACKPACK, BackpackScreen::new);
		BlankRuneTooltip.init();
		Tooltips.init();

		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.Discovery.TYPE, (payload, context) ->
			context.client().getToastManager().addToast(new GrimoireToast(payload.key())));
		// This world's magic revealed (a resonance found, a quirk met): a toast with the name just shown.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.cast.WorldResonances.Revealed.TYPE, (payload, context) ->
			context.client().getToastManager().addToast(GrimoireToast.revealed(payload.kind(), payload.name(), payload.color())));
		// A rune's text, everywhere it's shown, only as far as the player has read it.
		RuneReadingText.init();
		// An affinity reached a new level: a toast in its element's colour.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.cast.PlayerAffinities.Rise.TYPE, (payload, context) ->
			context.client().getToastManager().addToast(GrimoireToast.affinity(payload.element(), payload.level())));
		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.LeySeed.TYPE, (payload, context) -> LeyMotes.setSeed(payload.seed()));
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.net.NotebookPayload.TYPE, (payload, context) -> {
			var previous = context.client().screen instanceof RuneNotebookScreen board ? board : null;
			context.client().setScreen(new RuneNotebookScreen(payload, previous));
		});
		// The server's Wildercord version: a word in chat if it isn't ours (runes one of us doesn't know go silent).
		VersionWatch.init();
		ClientPlayNetworking.registerGlobalReceiver(WildercordNetworking.ScreenFx.TYPE,
			(payload, context) -> dev.wildercord.client.fx.ScreenEffects.receive(payload));
		// The server's cost and regeneration multipliers, for the Cord screen and HUD; forgotten on leaving.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.config.Config.Sync.TYPE, (payload, context) -> dev.wildercord.config.Config.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.runesmith.Contracts.ShowBoard.TYPE, (payload, context) -> context.client().setScreen(new ContractBoardScreen(payload)));
		// The elemental climate where the player stands, for the HUD's marks and the Grimoire.
		ClientPlayNetworking.registerGlobalReceiver(dev.wildercord.cast.Climate.Sync.TYPE, (payload, context) -> dev.wildercord.cast.Climate.receive(payload));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			dev.wildercord.config.Config.receive(null);
			dev.wildercord.cast.Climate.receive(null);
			CordGlow.clear();
			LeyMotes.forget();
			SpellHud.forget();
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.packetProcessor() != null) {
				inStartClientTick = true;
				try {
					client.packetProcessor().processQueuedPackets();
				} finally {
					inStartClientTick = false;
				}
			}
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
		dev.wildercord.client.compat.WorldRenderCompat.registerAfterEntities(dev.wildercord.client.fx.Incantations::submit);
		CastingOptions.load();
		WildercordKeys.init();
		MastersArtsClient.init();
		SpellHud.init();
		// Aura: what aura sense outlines (the bar is SpellHud's, the blade's glow AuraBlade's).
		AuraClient.init();
		// Sword strings: the player's swings read into strings, their indicator and sounds.
		SwordStringsClient.init();
		// Aura's feel: blade trails, impacts and hit-stop, banners, bursts and the body's aura, as this client sees them.
		AuraFxClient.init();
		dev.wildercord.client.fx.FrameBenchmark.init();
		WaypointHud.init();
		// ---- lore pack: the lore journal key and screen.
		dev.wildercord.client.lore.LoreJournalClient.init();
		Wildercord.LOGGER.info("Wildercord client initialized");
	}
}
