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
 * The sunwatch meerkat ({@link SunwatchMeerkat}): its entity type, spawn egg, spawns in mobs in the deserts and savannas
 * ({@link WildlifeRules#SUNWATCH_MEERKAT}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/meerkat_art.py.
 */
public final class MeerkatContent {
	private MeerkatContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("sunwatch_meerkat"));
	public static final EntityType<SunwatchMeerkat> SUNWATCH_MEERKAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(SunwatchMeerkat::new, MobCategory.CREATURE).sized(0.5F, 0.55F).eyeHeight(0.45F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("sunwatch_meerkat_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(SUNWATCH_MEERKAT).setId(EGG_KEY)));

	/** What a sunwatch meerkat may stand on to spawn (written by tools/meerkat_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/sunwatch_meerkat"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(SUNWATCH_MEERKAT, SunwatchMeerkat.createAttributes());
		SpawnPlacements.register(SUNWATCH_MEERKAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.SUNWATCH_MEERKAT, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.SUNWATCH_MEERKAT, SUNWATCH_MEERKAT);
		FieldGuide.add(new FieldGuide.Entry("wildercord:sunwatch_meerkat", FieldGuide.Group.WILDLIFE, 0xC8A06A));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
