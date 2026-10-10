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
 * The black bobcat ({@link BlackBobcat}): its entity type (as big as a polar bear), spawn egg, spawns in the dark forests and
 * old taigas ({@link WildlifeRules#BLACK_BOBCAT}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/bobcat_art.py.
 */
public final class BobcatContent {
	private BobcatContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("black_bobcat"));
	public static final EntityType<BlackBobcat> BLACK_BOBCAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(BlackBobcat::new, MobCategory.CREATURE).sized(1.3F, 1.6F).eyeHeight(1.35F).clientTrackingRange(10).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("black_bobcat_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(BLACK_BOBCAT).setId(EGG_KEY)));

	/** What a bobcat may stand on to spawn (written by tools/bobcat_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/black_bobcat"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(BLACK_BOBCAT, BlackBobcat.createAttributes());
		SpawnPlacements.register(BLACK_BOBCAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.BLACK_BOBCAT, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.BLACK_BOBCAT, BLACK_BOBCAT);
		FieldGuide.add(new FieldGuide.Entry("wildercord:black_bobcat", FieldGuide.Group.WILDLIFE, 0x3A3A48));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
