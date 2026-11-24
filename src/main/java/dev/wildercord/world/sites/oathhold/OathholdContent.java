package dev.wildercord.world.sites.oathhold;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers Oathhold's living-city entities and civic guild infrastructure:
 * {@link #CITIZEN} inhabitants, {@link #GUILD_REQUEST_BOARD}, {@link OathholdLaw}, and {@link OathholdQuests}.
 */
public final class OathholdContent {
	private OathholdContent() {}

	private static final ResourceKey<EntityType<?>> CITIZEN_KEY =
		ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("oathhold_citizen"));
	public static final EntityType<OathholdCitizen> CITIZEN = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		CITIZEN_KEY,
		EntityType.Builder.of(OathholdCitizen::new, MobCategory.MISC)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.62F)
			.clientTrackingRange(10)
			.build(CITIZEN_KEY.location().toString())
	);

	private static final ResourceKey<Item> CITIZEN_EGG_KEY =
		ResourceKey.create(Registries.ITEM, Wildercord.id("oathhold_citizen_spawn_egg"));
	public static final Item CITIZEN_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		CITIZEN_EGG_KEY,
		dev.wildercord.compat.McSpawnEgg.create(CITIZEN, new Item.Properties())
	);

	private static final ResourceKey<net.minecraft.world.level.block.Block> BOARD_KEY =
		ResourceKey.create(Registries.BLOCK, Wildercord.id("guild_request_board"));
	public static final GuildRequestBoardBlock GUILD_REQUEST_BOARD = Registry.register(
		BuiltInRegistries.BLOCK,
		BOARD_KEY,
		new GuildRequestBoardBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.sound(SoundType.WOOD)
			.strength(-1.0F, 3600000.0F)
			.noOcclusion()
			.noLootTable()
			)
	);

	private static final ResourceKey<Item> BOARD_ITEM_KEY =
		ResourceKey.create(Registries.ITEM, Wildercord.id("guild_request_board"));
	public static final Item GUILD_REQUEST_BOARD_ITEM = Registry.register(
		BuiltInRegistries.ITEM,
		BOARD_ITEM_KEY,
		new BlockItem(GUILD_REQUEST_BOARD, new Item.Properties())
	);

	public static void init() {
		FabricDefaultAttributeRegistry.register(CITIZEN, OathholdCitizen.createAttributes());
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> {
				output.accept(CITIZEN_SPAWN_EGG);
				output.accept(GUILD_REQUEST_BOARD_ITEM);
			});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(output -> output.accept(CITIZEN_SPAWN_EGG));
		OathholdLaw.init();
		OathholdQuests.init();
	}
}
