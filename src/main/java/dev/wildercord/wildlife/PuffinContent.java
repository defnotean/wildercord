package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The stonecrest puffin ({@link StonecrestPuffin}): its entity type, spawn egg, spawns in little colonies on the stony shores and
 * beaches ({@link WildlifeRules#STONECREST_PUFFIN}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/puffin_art.py.
 */
public final class PuffinContent {
	private PuffinContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("stonecrest_puffin"));
	public static final EntityType<StonecrestPuffin> STONECREST_PUFFIN = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(StonecrestPuffin::new, MobCategory.CREATURE).sized(0.45F, 0.65F).eyeHeight(0.55F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("stonecrest_puffin_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(STONECREST_PUFFIN).setId(EGG_KEY)));

	/** What a stonecrest puffin may stand on to spawn (written by tools/puffin_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/stonecrest_puffin"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(STONECREST_PUFFIN, StonecrestPuffin.createAttributes());
		SpawnPlacements.register(STONECREST_PUFFIN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.STONECREST_PUFFIN, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.STONECREST_PUFFIN, STONECREST_PUFFIN);
		FieldGuide.add(new FieldGuide.Entry("wildercord:stonecrest_puffin", FieldGuide.Group.WILDLIFE, 0xE8642A));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
