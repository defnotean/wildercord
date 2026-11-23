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
 * The tussock pika ({@link TussockPika}): its entity type, spawn egg, spawns on the meadows and high stony slopes
 * ({@link WildlifeRules#TUSSOCK_PIKA}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/pika_art.py.
 */
public final class PikaContent {
	private PikaContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("tussock_pika"));
	public static final EntityType<TussockPika> TUSSOCK_PIKA = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(TussockPika::new, MobCategory.CREATURE).sized(0.45F, 0.4F).eyeHeight(0.3F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("tussock_pika_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		dev.wildercord.compat.McSpawnEgg.create(TUSSOCK_PIKA, new Item.Properties()