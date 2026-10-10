package dev.wildercord.wildlife;

import dev.wildercord.config.Config;
import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.MobSpawnSettings;
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
			.clientTrackingRange(10).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("ridgeback_stag_spawn_egg"));
	public static final Item EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(RIDGEBACK_STAG).setId(EGG_KEY)));

	public static void init() {
		FabricDefaultAttributeRegistry.register(RIDGEBACK_STAG, RidgebackStag.createAttributes());
		SpawnPlacements.register(RIDGEBACK_STAG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
		BiomeModifications.create(Wildercord.id("wildlife/ridgeback_stag")).add(ModificationPhase.ADDITIONS,
			BiomeSelectors.includeByKey(BIOMES.stream().map(id -> ResourceKey.create(Registries.BIOME, net.minecraft.resources.Identifier.parse(id))).toList()),
			context -> {
				if (Config.get().wildlife().enabled() && Config.get().wildlife().spawnMultiplier() > 0) {
					context.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,
						new MobSpawnSettings.SpawnerData(RIDGEBACK_STAG, UniformInt.of(MIN_GROUP, MAX_GROUP)), WEIGHT);
				}
			});
		FieldGuide.add(new FieldGuide.Entry("wildercord:ridgeback_stag", FieldGuide.Group.WILDLIFE, 0x8A5A34));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(EGG));
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"))).register(output -> output.accept(EGG));
	}
}
