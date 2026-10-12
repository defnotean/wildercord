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
 * The brookbill duck ({@link BrookbillDuck}): its entity type, spawn egg, spawns by rivers, ponds and swamps
 * ({@link WildlifeRules#BROOKBILL_DUCK}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/duck_art.py.
 */
public final class DuckContent {
	private DuckContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("brookbill_duck"));
	public static final EntityType<BrookbillDuck> BROOKBILL_DUCK = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(BrookbillDuck::new, MobCategory.CREATURE).sized(0.5F, 0.7F).eyeHeight(0.6F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("brookbill_duck_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(BROOKBILL_DUCK).setId(EGG_KEY)));

	/** What a brookbill duck may stand on to spawn (written by tools/duck_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/brookbill_duck"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(BROOKBILL_DUCK, BrookbillDuck.createAttributes());
		SpawnPlacements.register(BROOKBILL_DUCK, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.BROOKBILL_DUCK, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.BROOKBILL_DUCK, BROOKBILL_DUCK);
		FieldGuide.add(new FieldGuide.Entry("wildercord:brookbill_duck", FieldGuide.Group.WILDLIFE, 0x7A5A3A));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
