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
		dev.wildercord.content.ResidueBlocks.init();
		WildercordItems.init();
		dev.wildercord.cooking.Meals.init();
		dev.wildercord.content.Reagents.init();
		dev.wildercord.content.UpgradeRecipe.init();
		dev.wildercord.menu.WildercordMenus.init();
		dev.wildercord.gear.GearItems.init();
		dev.wildercord.gear.GearSlots.init();
		dev.wildercord.backpack.Backpacks.init();
		dev.wildercord.config.Config.init();
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> dev.wildercord.spell.CompiledSpellCache.clear());
		dev.wildercord.content.WildercordParticles.init();
		dev.wildercord.content.WildercordSounds.init();
		WildercordLoot.init();
		dev.wildercord.gear.GearLoot.init();
		WildercordEntities.init();
		WildercordAttachments.init();
		dev.wildercord.party.Parties.init();
		dev.wildercord.advancement.Advancements.init();
		WildercordNetworking.init();
		dev.wildercord.net.VersionCheck.init();
		Scheduler.init();
		dev.wildercord.cast.PracticeRoom.init();
		dev.wildercord.command.RuneLabCommand.init();
		dev.wildercord.cast.VisualMetrics.init();
		dev.wildercord.gear.ElementalArmor.init();
		dev.wildercord.cast.ArmorResponses.init();
		dev.wildercord.cast.Spirits.init();
		dev.wildercord.cast.Wards.init();
		dev.wildercord.cast.Shields.init();
		dev.wildercord.cast.Imbuing.init();
		dev.wildercord.cast.HeartCircles.init();
		dev.wildercord.cast.PassiveCaster.init();
		dev.wildercord.cast.SecretSpells.init();
		dev.wildercord.cast.WorldResonances.init();
		dev.wildercord.cast.RuneReadings.init();
		dev.wildercord.cast.DeathsDoor.init();
		dev.wildercord.cast.SpellDefence.init();
		dev.wildercord.cast.BlockFx.init();
		dev.wildercord.cast.CordLook.init();
		dev.wildercord.cast.Effects.init();
		dev.wildercord.cast.Soar.init();
		dev.wildercord.cast.Attunement.init();
		dev.wildercord.cast.ExplorerEffects.init();
		dev.wildercord.cast.FusedEffects.init();
		dev.wildercord.cast.CraftedRunes.init();
		dev.wildercord.cast.Thaws.init();
		dev.wildercord.cast.TemporaryBlocks.init();
		dev.wildercord.cast.PhysicalMagic.init();
		dev.wildercord.cast.Residues.init();
		dev.wildercord.cast.WorldMagic.init();
		dev.wildercord.cast.Innates.init();
		dev.wildercord.cast.Unison.init();
		dev.wildercord.cast.Affinities.init();
		dev.wildercord.cast.Climate.init();
		dev.wildercord.cast.PlayerAffinities.init();
		dev.wildercord.cast.Runebound.init();
		dev.wildercord.cast.LeyWalker.init();
		dev.wildercord.cast.DomainClash.init();
		dev.wildercord.cast.SpellChat.init();
		dev.wildercord.world.WildercordWorldgen.init();
		dev.wildercord.world.sites.Sites.init();
		dev.wildercord.world.upgrade.WorldUpgrades.init();
		dev.wildercord.cast.events.WorldEvents.init();
		dev.wildercord.runesmith.Runesmith.init();
		dev.wildercord.duel.Duels.init();
		dev.wildercord.chorus.Chorus.init();
		dev.wildercord.cosmetic.CordCosmetics.init();
		dev.wildercord.familiar.FamiliarContent.init();
		dev.wildercord.familiar.Familiars.init();
		dev.wildercord.pet.CinnamonContent.init();
		// The magical monsters of the wilds (after the config, which their spawns read).
		dev.wildercord.monster.Monsters.init();
		dev.wildercord.monster.Tempering.init();
		// Magical wildlife: glimmerwings, lumen stags, mossback tortoises, cinderfoxes, skyrays and rimehares.
		dev.wildercord.wildlife.Wildlife.init();
		dev.wildercord.wildlife.FoxCompanions.init();
		dev.wildercord.wildlife.HighlandContent.init();
		dev.wildercord.wildlife.WetlandContent.init();
		dev.wildercord.wildlife.WetlandGarden.init();
		dev.wildercord.wildlife.WetlandShelters.init();
  dev.wildercord.wildlife.ReedbackContent.init();
  dev.wildercord.wildlife.ReedRattle.init();
  dev.wildercord.wildlife.SporebackContent.init();
  dev.wildercord.wildlife.FungalGarden.init();
  dev.wildercord.wildlife.RootmoltContent.init();
  dev.wildercord.wildlife.TidewardEquipment.init();
  dev.wildercord.wildlife.TidewardSurvey.init();
  dev.wildercord.wildlife.EmberContent.init();
  dev.wildercord.wildlife.SiltcrestContent.init();
  dev.wildercord.wildlife.MossveilContent.init();
		dev.wildercord.wildlife.BobcatContent.init();
		dev.wildercord.wildlife.MountContent.init();
		dev.wildercord.town.Town.init();
		dev.wildercord.town.InnRaid.init();
		dev.wildercord.NameTags.init();
		dev.wildercord.player.Retemper.init();
		dev.wildercord.player.TribulationScars.init();
  dev.wildercord.wildlife.RootCarry.init();
  dev.wildercord.wildlife.RooksRainshield.init();
  dev.wildercord.cast.CampConcordMagic.init();
  dev.wildercord.wildlife.DrainhouseContent.init();
		dev.wildercord.cast.Dungeons.init();
		dev.wildercord.world.dungeons.DungeonWards.init();
		SpellCaster.init();
		dev.wildercord.cast.RelayCircles.init();
		dev.wildercord.content.RelayLesson.init();
        dev.wildercord.cast.ReweaveFields.init();
        dev.wildercord.cast.ExciseCasting.init();
        dev.wildercord.content.ExciseLesson.init();
        dev.wildercord.cast.LessonPackCasting.init();
        dev.wildercord.content.PackLesson.init();
        dev.wildercord.content.ReweaveLesson.init();
		WildercordCommand.init();
		dev.wildercord.travel.Travel.init();
		dev.wildercord.loadout.Loadouts.init();
		dev.wildercord.cast.Mastery.init();
		// Aura, the swordsman's path: breathing methods, stages and the Aura key's techniques.
		dev.wildercord.aura.Aura.init();
		// The world of aura: wandering duelists, fallen knights, aura-forged gear.
		dev.wildercord.aura.world.AuraWorld.init();
		// The world's own magic, spells that grow and the marks magic leaves, tied to each other.
		dev.wildercord.cast.WorldBonds.init();
		// Last: add-ons (the "wildercord" entrypoint) extend everything above.
		dev.wildercord.api.WildercordApi.loadAddons();
		dev.wildercord.cast.feel.Feels.init();
        dev.wildercord.wildlife.RootCarryFeels.init();
        dev.wildercord.cast.FieldFusions.init();
        dev.wildercord.cast.CounterSignatures.init();
        dev.wildercord.cast.SupportSignatures.init();
        dev.wildercord.cast.TrailSignatures.init();
        dev.wildercord.cast.NextSignatureFeels.register();
		// ---- lore pack: the lore journal, discovery quests and teachers' lines.
		dev.wildercord.lore.LoreJournal.init();
		// ---- prog pack
		dev.wildercord.cast.CircleVowCommands.init();
        // ---- fx-support pack
        dev.wildercord.cast.packs.WardState.init();
		// ---- perf pack
		dev.wildercord.cast.PerfHygiene.init();
		LOGGER.info("Wildercord initialized");
	}
}
