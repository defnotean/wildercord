package dev.wildercord.trials;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.compat.McWorldgen;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import dev.wildercord.world.biome.AstralLeyGladeClimate;
import dev.wildercord.world.biome.CinderCalderaClimate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Registers custom living-biome flora, resources, creatures, features, and Sanctum Wayfinder. */
public final class BiomeTrialContent {
	private BiomeTrialContent() {}

	private static ResourceKey<Block> blockKey(String id) {
		return ResourceKey.create(Registries.BLOCK, Wildercord.id(id));
	}

	private static ResourceKey<Item> itemKey(String id) {
		return ResourceKey.create(Registries.ITEM, Wildercord.id(id));
	}

	private static Item registerItem(String id, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = itemKey(id);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	public static final LeybloomClusterBlock LEYBLOOM_CLUSTER = Registry.register(
		BuiltInRegistries.BLOCK, blockKey("leybloom_cluster"),
		new LeybloomClusterBlock(BlockBehaviour.Properties.of()
			.setId(blockKey("leybloom_cluster"))
			.mapColor(MapColor.COLOR_CYAN)
			.strength(0.8F)
			.noOcclusion()
			.sound(SoundType.AMETHYST_CLUSTER)
			.lightLevel(s -> 11)));

	public static final CalderaSlagVentBlock CALDERA_SLAG_VENT = Registry.register(
		BuiltInRegistries.BLOCK, blockKey("caldera_slag_vent"),
		new CalderaSlagVentBlock(BlockBehaviour.Properties.of()
			.setId(blockKey("caldera_slag_vent"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(1.8F)
			.noOcclusion()
			.sound(SoundType.BASALT)
			.lightLevel(s -> 9)));

	public static final Item LEYBLOOM_CLUSTER_ITEM = registerItem("leybloom_cluster",
		p -> new BlockItem(LEYBLOOM_CLUSTER, p.useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON)));
	public static final Item CALDERA_SLAG_VENT_ITEM = registerItem("caldera_slag_vent",
		p -> new BlockItem(CALDERA_SLAG_VENT, p.useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON)));

	public static final Item ASTRAL_LEY_SHARD = registerItem("astral_ley_shard",
		p -> new AuraWorld.Lore("astral_ley_shard", p.stacksTo(64).rarity(Rarity.UNCOMMON)));
	public static final Item CALDERA_SLAG_CORE = registerItem("caldera_slag_core",
		p -> new AuraWorld.Lore("caldera_slag_core", p.stacksTo(64).rarity(Rarity.UNCOMMON)));
	public static final Item SANCTUM_WAYFINDER = registerItem("sanctum_wayfinder",
		p -> new SanctumWayfinderItem(p.stacksTo(1).rarity(Rarity.RARE)));

	private static final ResourceKey<EntityType<?>> MOTH_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("leyweaver_moth"));
	public static final EntityType<LeyweaverMoth> LEYWEAVER_MOTH = Registry.register(
		BuiltInRegistries.ENTITY_TYPE, MOTH_KEY,
		EntityType.Builder.of(LeyweaverMoth::new, MobCategory.CREATURE)
			.sized(0.75F, 0.85F)
			.eyeHeight(0.55F)
			.clientTrackingRange(8)
			.build(MOTH_KEY));

	private static final ResourceKey<EntityType<?>> HOUND_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("caldera_cinderhound"));
	public static final EntityType<CalderaCinderhound> CALDERA_CINDERHOUND = Registry.register(
		BuiltInRegistries.ENTITY_TYPE, HOUND_KEY,
		EntityType.Builder.of(CalderaCinderhound::new, MobCategory.MONSTER)
			.sized(0.95F, 0.95F)
			.eyeHeight(0.72F)
			.clientTrackingRange(8)
			.fireImmune()
			.build(HOUND_KEY));

	public static final Item LEYWEAVER_MOTH_SPAWN_EGG = registerItem("leyweaver_moth_spawn_egg",
		p -> new SpawnEggItem(p.spawnEgg(LEYWEAVER_MOTH)));
	public static final Item CALDERA_CINDERHOUND_SPAWN_EGG = registerItem("caldera_cinderhound_spawn_egg",
		p -> new SpawnEggItem(p.spawnEgg(CALDERA_CINDERHOUND)));

	public static void init() {
		McWorldgen.registerFeature("ley_spire_patch", new LeySpireFeature());
		McWorldgen.registerFeature("caldera_fumarole_patch", new CalderaFumaroleFeature());

		FabricDefaultAttributeRegistry.register(LEYWEAVER_MOTH, LeyweaverMoth.createAttributes());
		FabricDefaultAttributeRegistry.register(CALDERA_CINDERHOUND, CalderaCinderhound.createAttributes());

		SpawnPlacements.register(LEYWEAVER_MOTH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			Mob::checkMobSpawnRules);
		SpawnPlacements.register(CALDERA_CINDERHOUND, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, rand) ->
				level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, rand));

		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(Set.of(AstralLeyGladeClimate.ASTRAL_LEY_GLADE)),
			GenerationStep.Decoration.SURFACE_STRUCTURES,
			ResourceKey.create(Registries.PLACED_FEATURE, Wildercord.id("ley_spire_patch")));
		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(Set.of(CinderCalderaClimate.CINDER_CALDERA)),
			GenerationStep.Decoration.SURFACE_STRUCTURES,
			ResourceKey.create(Registries.PLACED_FEATURE, Wildercord.id("caldera_fumarole_patch")));

		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(Set.of(AstralLeyGladeClimate.ASTRAL_LEY_GLADE)),
			MobCategory.CREATURE, LEYWEAVER_MOTH, 8, 1, 3);
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(Set.of(CinderCalderaClimate.CINDER_CALDERA)),
			MobCategory.MONSTER, CALDERA_CINDERHOUND, 12, 1, 2);

		FieldGuide.add(new FieldGuide.Entry("wildercord:leyweaver_moth", FieldGuide.Group.WILDLIFE, 0x7CF5E0));
		FieldGuide.add(new FieldGuide.Entry("wildercord:caldera_cinderhound", FieldGuide.Group.MONSTER, 0xFF7B3B));

		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(out -> List.of(
				LEYBLOOM_CLUSTER_ITEM, CALDERA_SLAG_VENT_ITEM,
				ASTRAL_LEY_SHARD, CALDERA_SLAG_CORE, SANCTUM_WAYFINDER,
				LEYWEAVER_MOTH_SPAWN_EGG, CALDERA_CINDERHOUND_SPAWN_EGG
			).forEach(out::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
			out.accept(LEYWEAVER_MOTH_SPAWN_EGG);
			out.accept(CALDERA_CINDERHOUND_SPAWN_EGG);
		});
	}
}
