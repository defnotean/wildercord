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
 * The cinnabar panda ({@link CinnabarPanda}): its entity type, spawn egg, spawns in the cherry groves, bamboo jungles and old-growth
 * taigas ({@link WildlifeRules#CINNABAR_PANDA}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/red_panda_art.py.
 */
public final class RedPandaContent {
	private RedPandaContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("cinnabar_panda"));
	public static final EntityType<CinnabarPanda> CINNABAR_PANDA = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(CinnabarPanda::new, MobCategory.CREATURE).sized(0.6F, 0.6F).eyeHeight(0.5F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("cinnabar_panda_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(CINNABAR_PANDA).setId(EGG_KEY)));

	/** What a cinnabar panda may stand on to spawn (written by tools/red_panda_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/cinnabar_panda"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(CINNABAR_PANDA, CinnabarPanda.createAttributes());
		SpawnPlacements.register(CINNABAR_PANDA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.CINNABAR_PANDA, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.CINNABAR_PANDA, CINNABAR_PANDA);
		FieldGuide.add(new FieldGuide.Entry("wildercord:cinnabar_panda", FieldGuide.Group.WILDLIFE, 0xC0502A));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
