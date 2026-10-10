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
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * The 0.13 tameable predators, kin to the black bobcat ({@link PredatorRules}): the {@link FrostLynx} and the
 * {@link DuneCougar}, with their entity types, spawn eggs, spawns and field-guide pages. Art and data are written by
 * tools/predator_art.py.
 */
public final class PredatorContent {
	private PredatorContent() {}

	public static final EntityType<FrostLynx> FROST_LYNX = entity("frost_lynx",
		EntityType.Builder.of(FrostLynx::new, MobCategory.CREATURE).sized(1.15F, 1.4F).eyeHeight(1.2F).clientTrackingRange(10));
	public static final EntityType<DuneCougar> DUNE_COUGAR = entity("dune_cougar",
		EntityType.Builder.of(DuneCougar::new, MobCategory.CREATURE).sized(1.3F, 1.6F).eyeHeight(1.35F).clientTrackingRange(10));

	public static final Item LYNX_EGG = egg("frost_lynx", FROST_LYNX);
	public static final Item COUGAR_EGG = egg("dune_cougar", DUNE_COUGAR);

	/** What each may stand on to spawn (written by tools/predator_art.py). */
	public static final TagKey<Block> LYNX_GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/frost_lynx"));
	public static final TagKey<Block> COUGAR_GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/dune_cougar"));

	private static <T extends BlackBobcat> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item egg(String name, EntityType<?> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(name + "_spawn_egg"));
		return Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key)));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(FROST_LYNX, FrostLynx.createAttributes());
		FabricDefaultAttributeRegistry.register(DUNE_COUGAR, DuneCougar.createAttributes());
		SpawnPlacements.register(FROST_LYNX, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.FROST_LYNX, type, LYNX_GROUND, level, reason, pos, random));
		SpawnPlacements.register(DUNE_COUGAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.DUNE_COUGAR, type, COUGAR_GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.FROST_LYNX, FROST_LYNX);
		WildlifeSpawns.join(WildlifeRules.DUNE_COUGAR, DUNE_COUGAR);
		FieldGuide.add(new FieldGuide.Entry("wildercord:frost_lynx", FieldGuide.Group.WILDLIFE, 0xB8B4AC));
		FieldGuide.add(new FieldGuide.Entry("wildercord:dune_cougar", FieldGuide.Group.WILDLIFE, 0xB8864E));
		for (ResourceKey<CreativeModeTab> tab : List.of(CreativeModeTabs.SPAWN_EGGS,
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))) {
			CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
				output.accept(LYNX_EGG);
				output.accept(COUGAR_EGG);
			});
		}
	}
}
