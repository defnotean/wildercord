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
 * The tussock pika ({@link TussockPika}): its entity type, spawn egg, spawns on the meadows and high stony slopes
 * ({@link WildlifeRules#TUSSOCK_PIKA}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/pika_art.py.
 */
public final class PikaContent {
	private PikaContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("tussock_pika"));
	public static final EntityType<TussockPika> TUSSOCK_PIKA = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(TussockPika::new, MobCategory.CREATURE).sized(0.45F, 0.4F).eyeHeight(0.3F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("tussock_pika_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(TUSSOCK_PIKA).setId(EGG_KEY)));

	/** What a tussock pika may stand on to spawn (written by tools/pika_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/tussock_pika"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(TUSSOCK_PIKA, TussockPika.createAttributes());
		SpawnPlacements.register(TUSSOCK_PIKA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.TUSSOCK_PIKA, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.TUSSOCK_PIKA, TUSSOCK_PIKA);
		FieldGuide.add(new FieldGuide.Entry("wildercord:tussock_pika", FieldGuide.Group.WILDLIFE, 0x9C8A70));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
