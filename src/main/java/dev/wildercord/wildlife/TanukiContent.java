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
 * The blossom tanuki ({@link BlossomTanuki}): its entity type, spawn egg, spawns in the cherry groves
 * ({@link WildlifeRules#BLOSSOM_TANUKI}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/tanuki_art.py.
 */
public final class TanukiContent {
	private TanukiContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("blossom_tanuki"));
	public static final EntityType<BlossomTanuki> BLOSSOM_TANUKI = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(BlossomTanuki::new, MobCategory.CREATURE).sized(0.6F, 0.6F).eyeHeight(0.45F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("blossom_tanuki_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(BLOSSOM_TANUKI).setId(EGG_KEY)));

	/** What a blossom tanuki may stand on to spawn (written by tools/tanuki_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/blossom_tanuki"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(BLOSSOM_TANUKI, BlossomTanuki.createAttributes());
		SpawnPlacements.register(BLOSSOM_TANUKI, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.BLOSSOM_TANUKI, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.BLOSSOM_TANUKI, BLOSSOM_TANUKI);
		FieldGuide.add(new FieldGuide.Entry("wildercord:blossom_tanuki", FieldGuide.Group.WILDLIFE, 0xE8A0B8));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
