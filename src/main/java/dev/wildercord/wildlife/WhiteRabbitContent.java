package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.net.ServerDisconnects;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers the forest White Rabbit entity, its Hollow Ancient Tree burrow portal block, spawn egg,
 * natural forest spawns, and Grimoire field guide entry.
 */
public final class WhiteRabbitContent {
	private WhiteRabbitContent() {}

	private static final ResourceKey<EntityType<?>> RABBIT_KEY =
		ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("white_rabbit"));

	public static final EntityType<WhiteRabbit> WHITE_RABBIT = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		RABBIT_KEY,
		EntityType.Builder.of(WhiteRabbit::new, MobCategory.CREATURE)
			.sized(0.5F, 0.65F)
			.eyeHeight(0.48F)
			.clientTrackingRange(8)
			.build(RABBIT_KEY));

	private static final ResourceKey<Block> PORTAL_KEY =
		ResourceKey.create(Registries.BLOCK, Wildercord.id("rabbit_hole_portal"));

	public static final RabbitHolePortalBlock RABBIT_HOLE_PORTAL = Registry.register(
		BuiltInRegistries.BLOCK,
		PORTAL_KEY,
		new RabbitHolePortalBlock(BlockBehaviour.Properties.of()
			
			.mapColor(MapColor.COLOR_PURPLE)
			.noCollission()
			.strength(-1.0F, 3600000.0F)
			.lightLevel(state -> 9)
			.sound(SoundType.AMETHYST)
			.noLootTable()));

	private static final ResourceKey<Item> PORTAL_ITEM_KEY =
		ResourceKey.create(Registries.ITEM, Wildercord.id("rabbit_hole_portal"));

	public static final Item RABBIT_HOLE_PORTAL_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		PORTAL_ITEM_KEY,
		new BlockItem(RABBIT_HOLE_PORTAL,
			new Item.Properties().rarity(Rarity.EPIC)));

	public static final BlockEntityType<RabbitHolePortalBlockEntity> RABBIT_HOLE_PORTAL_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("rabbit_hole_portal"),
		FabricBlockEntityTypeBuilder.create(RabbitHolePortalBlockEntity::new, RABBIT_HOLE_PORTAL).build());

	private static final ResourceKey<Item> EGG_KEY =
		ResourceKey.create(Registries.ITEM, Wildercord.id("white_rabbit_spawn_egg"));

	public static final Item WHITE_RABBIT_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		EGG_KEY,
		new SpawnEggItem(WHITE_RABBIT, new Item.Properties()));

	public static final TagKey<Block> GROUND =
		TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/white_rabbit"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(WHITE_RABBIT, WhiteRabbit.createAttributes());
		SpawnPlacements.register(
			WHITE_RABBIT,
			SpawnPlacementTypes.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) ->
				WildlifeSpawns.shaded(WildlifeRules.WHITE_RABBIT, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.WHITE_RABBIT, WHITE_RABBIT);
		FieldGuide.add(new FieldGuide.Entry("wildercord:white_rabbit", FieldGuide.Group.WILDLIFE, 0xF4F2FA));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> output.accept(WHITE_RABBIT_SPAWN_EGG));
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> output.accept(WHITE_RABBIT_SPAWN_EGG));

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> RabbitHolePortalBlockEntity.clearCooldowns());
		ServerDisconnects.register((handler, server) -> RabbitHolePortalBlockEntity.clearPlayerCooldown(handler.player));
	}
}
