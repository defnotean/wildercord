package dev.wildercord;

import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.command.WildercordCommand;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordEffects;
import dev.wildercord.content.WildercordLoot;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint: runs on both the dedicated server and the client's integrated server. */
public final class Wildercord implements ModInitializer {
	public static final String MOD_ID = "wildercord";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		WildercordComponents.init();
		WildercordEffects.init();
		WildercordBlocks.init();
		WildercordItems.init();
		dev.wildercord.gear.GearItems.init();
		dev.wildercord.config.Config.init();
		dev.wildercord.content.WildercordParticles.init();
		dev.wildercord.content.WildercordSounds.init();
		WildercordLoot.init();
		dev.wildercord.gear.GearLoot.init();
		WildercordEntities.init();
		WildercordAttachments.init();
		WildercordNetworking.init();
		Scheduler.init();
		dev.wildercord.cast.Spirits.init();
		dev.wildercord.cast.Wards.init();
		dev.wildercord.cast.Shields.init();
		dev.wildercord.cast.Imbuing.init();
		dev.wildercord.cast.HeartCircles.init();
		dev.wildercord.cast.PassiveCaster.init();
		dev.wildercord.cast.SecretSpells.init();
		dev.wildercord.cast.BlockFx.init();
		dev.wildercord.cast.CordLook.init();
		dev.wildercord.cast.Effects.init();
		dev.wildercord.cast.Innates.init();
		dev.wildercord.cast.Unison.init();
		dev.wildercord.cast.Runebound.init();
		dev.wildercord.cast.LeyWalker.init();
		dev.wildercord.cast.DomainClash.init();
		dev.wildercord.cast.SpellChat.init();
		dev.wildercord.world.WildercordWorldgen.init();
		SpellCaster.init();
		WildercordCommand.init();
		// Last: add-ons (the "wildercord" entrypoint) extend everything above.
		dev.wildercord.api.WildercordApi.loadAddons();
		LOGGER.info("Wildercord initialized");
	}
}
