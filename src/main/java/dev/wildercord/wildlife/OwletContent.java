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
 * The hushwing owlet ({@link HushwingOwlet}): its entity type, spawn egg, spawns under the pale oaks ({@link WildlifeRules#HUSHWING_OWLET},
 * held to the shaded rule, since no light gets through the pale garden), and its page in the Grimoire's field guide. Art
 * and data are written by tools/owlet_art.py.
 */
public final class OwletContent {
	private OwletContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("hushwing_owlet"));
	public static final EntityType<HushwingOwlet> HUSHWING_OWLET = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(HushwingOwlet::new, MobCategory.CREATURE).sized(0.45F, 0.7F).eyeHeight(0.55F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("hushwing_owlet_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(HUSHWING_OWLET).setId(EGG_KEY)));

	/** What a hushwing owlet may stand on to spawn (written by tools/owlet_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/hushwing_owlet"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(HUSHWING_OWLET, HushwingOwlet.createAttributes());
		SpawnPlacements.register(HUSHWING_OWLET, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.shaded(WildlifeRules.HUSHWING_OWLET, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.HUSHWING_OWLET, HUSHWING_OWLET);
		FieldGuide.add(new FieldGuide.Entry("wildercord:hushwing_owlet", FieldGuide.Group.WILDLIFE, 0xC8C2B4));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
