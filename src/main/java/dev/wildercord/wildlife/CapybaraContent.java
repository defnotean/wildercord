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
 * The mirebath capybara ({@link MirebathCapybara}): its entity type, spawn egg, spawns in the swamps and jungles
 * ({@link WildlifeRules#MIREBATH_CAPYBARA}), and its page in the Grimoire's field guide. Art and data are written by
 * tools/capybara_art.py.
 */
public final class CapybaraContent {
	private CapybaraContent() {}

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("mirebath_capybara"));
	public static final EntityType<MirebathCapybara> MIREBATH_CAPYBARA = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(MirebathCapybara::new, MobCategory.CREATURE).sized(0.85F, 0.75F).eyeHeight(0.6F).clientTrackingRange(8).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("mirebath_capybara_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(MIREBATH_CAPYBARA).setId(EGG_KEY)));

	/** What a mirebath capybara may stand on to spawn (written by tools/capybara_art.py). */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/mirebath_capybara"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(MIREBATH_CAPYBARA, MirebathCapybara.createAttributes());
		SpawnPlacements.register(MIREBATH_CAPYBARA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.MIREBATH_CAPYBARA, type, GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.MIREBATH_CAPYBARA, MIREBATH_CAPYBARA);
		FieldGuide.add(new FieldGuide.Entry("wildercord:mirebath_capybara", FieldGuide.Group.WILDLIFE, 0xA8744A));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
