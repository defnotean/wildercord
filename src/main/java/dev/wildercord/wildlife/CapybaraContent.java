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
 * The mirebath capybara ({@link MirebathCapybara}): its entity type, spawn egg, spawns in the swamps and jungles
 * ({@link WildlifeRules#MIREBATH_CAPYBARA}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/capybara_art.py.
 */
public final class CapybaraContent {
	private CapybaraContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("mirebath_capybara"));
	public static final EntityType<MirebathCapybara> MIREBATH_CAPYBARA = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(MirebathCapybara::new, MobCategory.CREATURE).sized(0.85F, 0.75F).eyeHeight(0.6F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("mirebath_capybara_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		dev.wildercord.compat.McSpawnEgg.create(MIREBATH_CAPYBARA, new Item.Properties()