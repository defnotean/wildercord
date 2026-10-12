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
 * The thistle hedgehog ({@link ThistleHedgehog}): its entity type, spawn egg, spawns in the meadows, plains and forests
 * ({@link WildlifeRules#THISTLE_HEDGEHOG}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/hedgehog_art.py.
 */
public final class HedgehogContent {
	private HedgehogContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("thistle_hedgehog"));
	public static final EntityType<ThistleHedgehog> THISTLE_HEDGEHOG = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(ThistleHedgehog::new, MobCategory.CREATURE).sized(0.7F, 0.55F).eyeHeight(0.35F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("thistle_hedgehog_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(THISTLE_HEDGEHOG).setId(EGG_KEY)));

	/** What a hedgehog may stand on to spawn (written by tools/hedgehog_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/thistle_hedgehog"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(THISTLE_HEDGEHOG, ThistleHedgehog.createAttributes());
		SpawnPlacements.register(THISTLE_HEDGEHOG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.THISTLE_HEDGEHOG, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.THISTLE_HEDGEHOG, THISTLE_HEDGEHOG);
		FieldGuide.add(new FieldGuide.Entry("wildercord:thistle_hedgehog", FieldGuide.Group.WILDLIFE, 0x8A6A4A));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
