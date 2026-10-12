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
 * The brook otter ({@link BrookOtter}): its entity type, spawn egg, spawns along the rivers and in the swamps
 * ({@link WildlifeRules#BROOK_OTTER}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/otter_art.py.
 */
public final class OtterContent {
	private OtterContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("brook_otter"));
	public static final EntityType<BrookOtter> BROOK_OTTER = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(BrookOtter::new, MobCategory.CREATURE).sized(0.6F, 0.5F).eyeHeight(0.4F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("brook_otter_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(BROOK_OTTER).setId(EGG_KEY)));

	/** What an otter may stand on to spawn (written by tools/otter_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/brook_otter"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(BROOK_OTTER, BrookOtter.createAttributes());
		SpawnPlacements.register(BROOK_OTTER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.BROOK_OTTER, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.BROOK_OTTER, BROOK_OTTER);
		FieldGuide.add(new FieldGuide.Entry("wildercord:brook_otter", FieldGuide.Group.WILDLIFE, 0x7A5434));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
