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
		new SpawnEggItem(new Item.Properties().spawnEgg(FLOE_PENGUIN).setId(EGG_KEY)));

	/** What a floe penguin may stand on to spawn (written by tools/penguin_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/floe_penguin"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(FLOE_PENGUIN, FloePenguin.createAttributes());
		SpawnPlacements.register(FLOE_PENGUIN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.FLOE_PENGUIN, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.FLOE_PENGUIN, FLOE_PENGUIN);
		FieldGuide.add(new FieldGuide.Entry("wildercord:floe_penguin", FieldGuide.Group.WILDLIFE, 0x2A2E36));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
