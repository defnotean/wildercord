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
 * The glowberry dormouse ({@link GlowberryDormouse}): its entity type, spawn egg, spawns on the moss of the lush caves
 * ({@link WildlifeRules#GLOWBERRY_DORMOUSE}, held to the cave rule rather than the daylight one), and its page in the
 * Grimoire's field guide. Art and data are written by tools/dormouse_art.py.
 */
public final class DormouseContent {
	private DormouseContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("glowberry_dormouse"));
	public static final EntityType<GlowberryDormouse> GLOWBERRY_DORMOUSE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(GlowberryDormouse::new, MobCategory.CREATURE).sized(0.4F, 0.4F).eyeHeight(0.3F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("glowberry_dormouse_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(GLOWBERRY_DORMOUSE).setId(EGG_KEY)));

	/** What a glowberry dormouse may stand on to spawn (written by tools/dormouse_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/glowberry_dormouse"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(GLOWBERRY_DORMOUSE, GlowberryDormouse.createAttributes());
		SpawnPlacements.register(GLOWBERRY_DORMOUSE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.cave(WildlifeRules.GLOWBERRY_DORMOUSE, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.GLOWBERRY_DORMOUSE, GLOWBERRY_DORMOUSE);
		FieldGuide.add(new FieldGuide.Entry("wildercord:glowberry_dormouse", FieldGuide.Group.WILDLIFE, 0xE8A43C));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
