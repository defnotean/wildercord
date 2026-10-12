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
 * The snowcap stoat ({@link SnowcapStoat}): its entity type, spawn egg, spawns in the snowy plains, taigas and slopes
 * ({@link WildlifeRules#SNOWCAP_STOAT}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/stoat_art.py.
 */
public final class StoatContent {
	private StoatContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("snowcap_stoat"));
	public static final EntityType<SnowcapStoat> SNOWCAP_STOAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(SnowcapStoat::new, MobCategory.CREATURE).sized(0.5F, 0.5F).eyeHeight(0.4F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("snowcap_stoat_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(SNOWCAP_STOAT).setId(EGG_KEY)));

	/** What a stoat may stand on to spawn (written by tools/stoat_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/snowcap_stoat"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(SNOWCAP_STOAT, SnowcapStoat.createAttributes());
		SpawnPlacements.register(SNOWCAP_STOAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.SNOWCAP_STOAT, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.SNOWCAP_STOAT, SNOWCAP_STOAT);
		FieldGuide.add(new FieldGuide.Entry("wildercord:snowcap_stoat", FieldGuide.Group.WILDLIFE, 0xE8ECEF));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
