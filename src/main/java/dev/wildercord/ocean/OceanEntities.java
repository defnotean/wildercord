package dev.wildercord.ocean;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Registers all Deep Ocean Expansion EntityType, default attributes, and water spawn placements.
 */
public final class OceanEntities {
	private OceanEntities() {}

	private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static final EntityType<BarnacleHulkDredger> BARNACLE_HULK_DREDGER = register(
		"barnacle_hulk_dredger",
		EntityType.Builder.of(BarnacleHulkDredger::new, MobCategory.MONSTER).sized(1.0F, 2.3F)
	);

	public static final EntityType<SirenVeilJelly> SIREN_VEIL_JELLY = register(
		"siren_veil_jelly",
		EntityType.Builder.of(SirenVeilJelly::new, MobCategory.MONSTER).sized(0.95F, 1.4F)
	);

	public static final EntityType<CoralSpineMako> CORAL_SPINE_MAKO = register(
		"coral_spine_mako",
		EntityType.Builder.of(CoralSpineMako::new, MobCategory.MONSTER).sized(1.15F, 0.75F)
	);

	public static final EntityType<DreadNautilusSentinel> DREAD_NAUTILUS_SENTINEL = register(
		"dread_nautilus_sentinel",
		EntityType.Builder.of(DreadNautilusSentinel::new, MobCategory.MONSTER).sized(1.3F, 1.3F)
	);

	public static final EntityType<ThalassorBoss> THALASSOR = register(
		"thalassor",
		EntityType.Builder.of(ThalassorBoss::new, MobCategory.MONSTER).sized(3.2F, 2.2F).fireImmune()
	);

	public static final EntityType<BrineMerchant> BRINE_MERCHANT = register(
		"brine_merchant",
		EntityType.Builder.of(BrineMerchant::new, MobCategory.CREATURE).sized(0.6F, 1.95F)
	);

	public static final EntityType<LumiSeahorse> LUMI_SEAHORSE = register(
		"lumi_seahorse",
		EntityType.Builder.of(LumiSeahorse::new, MobCategory.WATER_CREATURE).sized(0.85F, 1.45F)
	);

	public static final EntityType<PearlPup> PEARL_PUP = register(
		"pearl_pup",
		EntityType.Builder.of(PearlPup::new, MobCategory.WATER_CREATURE).sized(0.7F, 0.55F)
	);

	public static final EntityType<SingingMoonWhale> SINGING_MOON_WHALE = register(
		"singing_moon_whale",
		EntityType.Builder.of(SingingMoonWhale::new, MobCategory.WATER_CREATURE).sized(3.4F, 2.0F)
	);

	public static final EntityType<ThrownHarpoon> THROWN_HARPOON = register(
		"thrown_harpoon",
		EntityType.Builder.<ThrownHarpoon>of(ThrownHarpoon::new, MobCategory.MISC)
			.sized(0.5F, 0.5F)
			.clientTrackingRange(4)
			.updateInterval(10)
	);

	public static void init() {
		FabricDefaultAttributeRegistry.register(BARNACLE_HULK_DREDGER, BarnacleHulkDredger.createAttributes());
		FabricDefaultAttributeRegistry.register(SIREN_VEIL_JELLY, SirenVeilJelly.createAttributes());
		FabricDefaultAttributeRegistry.register(CORAL_SPINE_MAKO, CoralSpineMako.createAttributes());
		FabricDefaultAttributeRegistry.register(DREAD_NAUTILUS_SENTINEL, DreadNautilusSentinel.createAttributes());
		FabricDefaultAttributeRegistry.register(THALASSOR, ThalassorBoss.createAttributes());
		FabricDefaultAttributeRegistry.register(BRINE_MERCHANT, BrineMerchant.createAttributes());
		FabricDefaultAttributeRegistry.register(LUMI_SEAHORSE, LumiSeahorse.createAttributes());
		FabricDefaultAttributeRegistry.register(PEARL_PUP, PearlPup.createAttributes());
		FabricDefaultAttributeRegistry.register(SINGING_MOON_WHALE, SingingMoonWhale.createAttributes());

		registerMonsterWaterSpawn(BARNACLE_HULK_DREDGER);
		registerMonsterWaterSpawn(SIREN_VEIL_JELLY);
		registerMonsterWaterSpawn(CORAL_SPINE_MAKO);
		registerMonsterWaterSpawn(DREAD_NAUTILUS_SENTINEL);
		registerFriendlyWaterSpawn(LUMI_SEAHORSE);
		registerFriendlyWaterSpawn(PEARL_PUP);
		registerFriendlyWaterSpawn(SINGING_MOON_WHALE);
	}

	private static <T extends Mob> void registerMonsterWaterSpawn(EntityType<T> type) {
		SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
			(t, level, reason, pos, random) ->
				(MobSpawnType.isSpawner(reason) || reason != MobSpawnType.NATURAL
					|| (level.getDifficulty() != Difficulty.PEACEFUL
						&& level.getFluidState(pos).is(FluidTags.WATER)
						&& pos.getY() < level.getSeaLevel() - 6)));
	}

	private static <T extends Mob> void registerFriendlyWaterSpawn(EntityType<T> type) {
		SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
			(t, level, reason, pos, random) ->
				level.getFluidState(pos).is(FluidTags.WATER)
					&& (MobSpawnType.isSpawner(reason) || pos.getY() < level.getSeaLevel()));
	}
}
