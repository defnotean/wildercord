package dev.wildercord.wildlife;

import dev.wildercord.config.Config;
import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Mounts (0.12 "Tempering"): the {@link RidgebackStag}, its entity type, spawn egg, spawns on the plains, meadows and savannas
 * (while wildlife spawns are on), and its page in the field guide. Art and data are written by tools/mount_art.py.
 */
public final class MountContent {
	private MountContent() {}

	/** Where ridgebacks roam, and how many come together. */
	public static final List<String> BIOMES = List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
		"minecraft:savanna", "minecraft:savanna_plateau");
	public static final int WEIGHT = 4, MIN_GROUP = 2, MAX_GROUP = 4;

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("ridgeback_stag"));
	public static final EntityType<RidgebackStag> RIDGEBACK_STAG = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.of(RidgebackStag::new, MobCategory.CREATURE).sized(1.3F, 1.6F).eyeHeight(1.5F).passengerAttachments(1.2F)
			.clientTrackingRange(10).build(KEY.location().toString()));

	private static final ResourceKey<Item> BRIDLE_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("skyray_bridle"));
	/** The reward for the Tenth Circle's tribulation: calls your skyray down. */
	public static final Item SKYRAY_BRIDLE = Registry.register(BuiltInRegistries.ITEM, BRIDLE_KEY,
		new SkyrayBridleItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("ridgeback_stag_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		dev.wildercord.compat.McSpawnEgg.create(RIDGEBACK_STAG, new Item.Properties()));

	/** What each may stand on to spawn (written by tools/mount_art.py). */
	public static final TagKey<Block> TURTLE_GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/reefback_turtle"));
	public static final TagKey<Block> MOLE_GROUND = TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/delver_mole"));

	public static void init() {
		FabricDefaultAttributeRegistry.register(REEFBACK_TURTLE, ReefbackTurtle.createAttributes());
		FabricDefaultAttributeRegistry.register(DELVER_MOLE, DelverMole.createAttributes());
		SpawnPlacements.register(REEFBACK_TURTLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.REEFBACK_TURTLE, type, TURTLE_GROUND, level, reason, pos, random));
		SpawnPlacements.register(DELVER_MOLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> WildlifeSpawns.creature(WildlifeRules.DELVER_MOLE, type, MOLE_GROUND, level, reason, pos, random));
		WildlifeSpawns.join(WildlifeRules.REEFBACK_TURTLE, REEFBACK_TURTLE);
		WildlifeSpawns.join(WildlifeRules.DELVER_MOLE, DELVER_MOLE);
		FieldGuide.add(new FieldGuide.Entry("wildercord:reefback_turtle", FieldGuide.Group.WILDLIFE, 0x3E8A78));
		FieldGuide.add(new FieldGuide.Entry("wildercord:delver_mole", FieldGuide.Group.WILDLIFE, 0x4A3A44));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
			output.accept(TURTLE_EGG);
			output.accept(MOLE_EGG);
		});
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> {
			output.accept(TURTLE_EGG);
			output.accept(MOLE_EGG);
		});
		FabricDefaultAttributeRegistry.register(RIDGEBACK_STAG, RidgebackStag.createAttributes());
		FabricDefaultAttributeRegistry.register(BONDED_SKYRAY, BondedSkyray.createAttributes());
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(SKYRAY_BRIDLE));
		SpawnPlacements.register(RIDGEBACK_STAG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
		BiomeModifications.create(Wildercord.id("wildlife/ridgeback_stag")).add(ModificationPhase.ADDITIONS,
			BiomeSelectors.includeByKey(BIOMES.stream().map(id -> ResourceKey.create(Registries.BIOME, net.minecraft.resources.ResourceLocation.parse(id))).toList()),
			context -> {
				if (Config.get().wildlife().enabled() && Config.get().wildlife().spawnMultiplier() > 0) {
					dev.wildercord.compat.McWorldgen.addSpawn(context.getSpawnSettings(), MobCategory.CREATURE, RIDGEBACK_STAG, MIN_GROUP, MAX_GROUP, WEIGHT);
				}
			});
		FieldGuide.add(new FieldGuide.Entry("wildercord:ridgeback_stag", FieldGuide.Group.WILDLIFE, 0x8A5A34));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
		MountBonds.init();
	}
}
