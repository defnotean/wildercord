package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
 * The floe penguin ({@link FloePenguin}): its entity type, spawn egg, spawns on the frozen shores
 * ({@link WildlifeRules#FLOE_PENGUIN}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/penguin_art.py.
 */
public final class PenguinContent {
	private PenguinContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("floe_penguin"));
	public static final EntityType<FloePenguin> FLOE_PENGUIN = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(FloePenguin::new, MobCategory.CREATURE).sized(0.5F, 0.85F).eyeHeight(0.75F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("floe_penguin_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		dev.wildercord.compat.McSpawnEgg.create(FLOE_PENGUIN, new Item.Properties()