package dev.wildercord.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.config.Config;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Where the monsters of the wilds come from: each joins the monsters of its biomes (a few in fifty spawns where it lives,
 * among the zombies and skeletons, inside the game's own monster cap) and keeps to its own ground through a spawn rule:
 * <ul>
 *   <li><b>Bramblewalkers</b>: the forests' floors at night (most of all dark forests and the pale garden);</li>
 *   <li><b>Gloomstalkers</b>: dark forests and the pale garden at night, and the deep caves below 0 anywhere;</li>
 *   <li><b>Thunderwing Harpies</b>: the bare peaks and windswept hills, 90 and up, at night or in a storm;</li>
 *   <li><b>Geode Crawlers</b>: caves below 50, mostly near amethyst geodes;</li>
 *   <li><b>Bog Witch-Frogs</b>: swamps and mangrove swamps at night;</li>
 *   <li><b>Mana Oozes</b>: the deep caves below 0, or caves below 40 under a ley line;</li>
 *   <li><b>Strawmen</b>: out in the open plains and meadows at night, one at a time;</li>
 *   <li><b>Dunestingers</b>: out on the deserts and badlands at night, a few together, buried where there is sand.</li>
 *   <li><b>Gulper Pitchers</b>: rooted on the jungle floor at night, one by one.</li>
 *   <li><b>Cackling Hyenas</b>: out on the savannas at night, in packs of three to five.</li>
 *   <li><b>Pitchback Beetles</b>: under the spruces of the taiga at night, one or two.</li>
 *   <li><b>Rimecoat Wolverines</b>: out on the snowy plains, the ice spikes and the snowy taiga at night, alone.</li>
 *   <li><b>Snapjaw Turtles</b>: on the riverbed in the dark, rivers and frozen rivers, alone.</li>
 *   <li><b>Reefgrasp Octopuses</b>: on the seafloor in the dark, the oceans but the frozen ones, alone.</li>
 *   <li><b>Foxfire Kitsunes</b>: among the cherry groves at night, alone.</li>
 *   <li><b>Cragcleaver Rams</b>: out on the windswept hills and the snowy slopes at night, alone.</li>
 * </ul>
 * All need the dark monsters need, none spawns on Peaceful (their types say so), and the server's {@code monsters} settings
 * can stop any of them: the switches at once, through the rules here, the rate when a world loads (it sets the weights).
 */
public final class MonsterSpawns {
	private MonsterSpawns() {}

	private static final Set<ResourceKey<Biome>> FORESTS = Set.of(Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
		Biomes.DAPPLED_FOREST, Biomes.DARK_FOREST, Biomes.PALE_GARDEN, Biomes.SNOWY_TAIGA, Biomes.GROVE);
	/** Where the woods are darkest: more Bramblewalkers, and Gloomstalkers on the surface. */
	private static final Set<ResourceKey<Biome>> DARK_WOODS = Set.of(Biomes.DARK_FOREST, Biomes.PALE_GARDEN);
	private static final Set<ResourceKey<Biome>> PEAKS = Set.of(Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES,
		Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST);
	private static final Set<ResourceKey<Biome>> SWAMPS = Set.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
	private static final Set<ResourceKey<Biome>> FIELDS = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
	/** A Strawman's spawn weight, and its id among the server's switches. */
	private static final int STRAWMAN_WEIGHT = 12;
	private static final String STRAWMAN = "strawman";
	private static final Set<ResourceKey<Biome>> DESERTS = Set.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
	/** A Dunestinger's spawn weight, and its id among the server's switches. */
	private static final int DUNESTINGER_WEIGHT = 14;
	private static final String DUNESTINGER = "dunestinger";
	private static final Set<ResourceKey<Biome>> JUNGLES = Set.of(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);
	/** A Gulper Pitcher's spawn weight, and its id among the server's switches. */
	private static final int GULPER_PITCHER_WEIGHT = 18;
	private static final String GULPER_PITCHER = "gulper_pitcher";
	private static final Set<ResourceKey<Biome>> SAVANNAS = Set.of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
	/** A Cackling Hyena pack's spawn weight, and its id among the server's switches. */
	private static final int CACKLING_HYENA_WEIGHT = 16;
	private static final String CACKLING_HYENA = "cackling_hyena";
	private static final Set<ResourceKey<Biome>> TAIGAS = Set.of(Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
	/** A Pitchback Beetle's spawn weight, and its id among the server's switches. */
	private static final int PITCHBACK_BEETLE_WEIGHT = 18;
	private static final String PITCHBACK_BEETLE = "pitchback_beetle";
	private static final Set<ResourceKey<Biome>> SNOWS = Set.of(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA);
	/** A Rimecoat Wolverine's spawn weight, and its id among the server's switches. */
	private static final int RIMECOAT_WOLVERINE_WEIGHT = 16;
	private static final String RIMECOAT_WOLVERINE = "rimecoat_wolverine";
	private static final Set<ResourceKey<Biome>> RIVERS = Set.of(Biomes.RIVER, Biomes.FROZEN_RIVER);
	/** A Snapjaw Turtle's spawn weight, and its id among the server's switches. */
	private static final int SNAPJAW_TURTLE_WEIGHT = 12;
	private static final String SNAPJAW_TURTLE = "snapjaw_turtle";
	private static final Set<ResourceKey<Biome>> SEAS = Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.LUKEWARM_OCEAN,
		Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN);
	/** A Reefgrasp Octopus's spawn weight, and its id among the server's switches. */
	private static final int REEFGRASP_OCTOPUS_WEIGHT = 15;
	private static final String REEFGRASP_OCTOPUS = "reefgrasp_octopus";
	private static final Set<ResourceKey<Biome>> CHERRY = Set.of(Biomes.CHERRY_GROVE);
	/** A Foxfire Kitsune's spawn weight, and its id among the server's switches. */
	private static final int FOXFIRE_KITSUNE_WEIGHT = 20;
	private static final String FOXFIRE_KITSUNE = "foxfire_kitsune";
	private static final Set<ResourceKey<Biome>> CRAGS = Set.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
		Biomes.SNOWY_SLOPES);
	/** A Cragcleaver Ram's spawn weight, and its id among the server's switches. */
	private static final int CRAGCLEAVER_RAM_WEIGHT = 16;
	private static final String CRAGCLEAVER_RAM = "cragcleaver_ram";
	/**
	 * Overworld biomes left alone underground: the deep dark (nothing spawns there on purpose; a lone entry would fill it) and
	 * the mushroom fields (no monsters, ever).
	 */
	private static final Set<ResourceKey<Biome>> QUIET = Set.of(Biomes.DEEP_DARK, Biomes.MUSHROOM_FIELDS);

	public static void init() {
		SpawnPlacements.register(MonsterContent.BRAMBLEWALKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, MonsterRules.Kind.BRAMBLEWALKER)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos)));
		SpawnPlacements.register(MonsterContent.GLOOMSTALKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, MonsterRules.Kind.GLOOMSTALKER)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || deep(level, pos)
					|| level.canSeeSky(pos) && inside(level, pos, DARK_WOODS)));
		SpawnPlacements.register(MonsterContent.THUNDERWING_HARPY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, MonsterRules.Kind.THUNDERWING_HARPY)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
				&& (EntitySpawnReason.isSpawner(reason) || pos.getY() >= MonsterRules.HARPY_MIN_Y && level.canSeeSky(pos)));
		SpawnPlacements.register(MonsterContent.GEODE_CRAWLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, MonsterRules.Kind.GEODE_CRAWLER)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason)
					|| pos.getY() < MonsterRules.CRAWLER_Y && !level.canSeeSky(pos)
						&& (nearAmethyst(level, pos, random) || random.nextDouble() < MonsterRules.CRAWLER_AWAY_FROM_GEODES)));
		SpawnPlacements.register(MonsterContent.BOG_WITCH_FROG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, MonsterRules.Kind.BOG_WITCH_FROG)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos)));
		SpawnPlacements.register(MonsterContent.MANA_OOZE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, MonsterRules.Kind.MANA_OOZE)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || deep(level, pos)
					|| pos.getY() < MonsterRules.LEY_OOZE_Y && !level.canSeeSky(pos) && onLey(level, pos)));
		SpawnPlacements.register(MonsterContent.STRAWMAN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, STRAWMAN)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos)));
		SpawnPlacements.register(MonsterContent.DUNESTINGER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, DUNESTINGER)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos)));
		SpawnPlacements.register(MonsterContent.GULPER_PITCHER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, GULPER_PITCHER)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || pos.getY() >= level.getSeaLevel()));
		SpawnPlacements.register(MonsterContent.CACKLING_HYENA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, CACKLING_HYENA)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos)));
		SpawnPlacements.register(MonsterContent.PITCHBACK_BEETLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, PITCHBACK_BEETLE)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || pos.getY() >= level.getSeaLevel()));
		SpawnPlacements.register(MonsterContent.RIMECOAT_WOLVERINE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, RIMECOAT_WOLVERINE)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || pos.getY() >= level.getSeaLevel()));
		SpawnPlacements.register(MonsterContent.SNAPJAW_TURTLE, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
			(type, level, reason, pos, random) -> allowed(reason, SNAPJAW_TURTLE)
				&& level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
				&& level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) && !level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER)
				&& (EntitySpawnReason.isSpawner(reason) || (pos.getY() < level.getSeaLevel() && Monster.isDarkEnoughToSpawn(level, pos, random))));
		SpawnPlacements.register(MonsterContent.REEFGRASP_OCTOPUS, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
			(type, level, reason, pos, random) -> allowed(reason, REEFGRASP_OCTOPUS)
				&& level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
				&& level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) && !level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER)
				&& (EntitySpawnReason.isSpawner(reason) || (pos.getY() < level.getSeaLevel() && Monster.isDarkEnoughToSpawn(level, pos, random))));
		SpawnPlacements.register(MonsterContent.FOXFIRE_KITSUNE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, FOXFIRE_KITSUNE)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos)));
		SpawnPlacements.register(MonsterContent.CRAGCLEAVER_RAM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> allowed(reason, CRAGCLEAVER_RAM)
				&& Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && (EntitySpawnReason.isSpawner(reason) || level.canSeeSky(pos)));

		// Their places among each biome's monsters. Read as each world loads, so the spawn rate counts from then.
		// Every overworld biome, by the vanilla tag as well as by where they generate, so a world whose overworld makes only a few
		// (a superflat one) still knows them all.
		BiomeModifications.create(Wildercord.id("monsters")).add(ModificationPhase.ADDITIONS,
			BiomeSelectors.foundInOverworld().or(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD)), MonsterSpawns::add);
	}

	private static void add(BiomeSelectionContext biome, BiomeModificationContext context) {
		ResourceKey<Biome> key = biome.getBiomeKey();
		double rate = Config.get().monsters().spawnRate();
		BiomeModificationContext.MobSpawnSettingsContext spawns = context.getMobSpawnSettings();
		if (FORESTS.contains(key)) {
			add(spawns, MonsterRules.Kind.BRAMBLEWALKER, DARK_WOODS.contains(key) ? 1.4 : 1.0, rate);
		}
		if (PEAKS.contains(key)) {
			add(spawns, MonsterRules.Kind.THUNDERWING_HARPY, 1.0, rate);
		}
		if (SWAMPS.contains(key)) {
			add(spawns, MonsterRules.Kind.BOG_WITCH_FROG, 1.0, rate);
		}
		if (FIELDS.contains(key)) {
			int weight = MonsterRules.weight(STRAWMAN_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.STRAWMAN, UniformInt.of(1, 1)), weight);
			}
		}
		if (DESERTS.contains(key)) {
			int weight = MonsterRules.weight(DUNESTINGER_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.DUNESTINGER, UniformInt.of(2, 4)), weight);
			}
		}
		if (JUNGLES.contains(key)) {
			int weight = MonsterRules.weight(GULPER_PITCHER_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.GULPER_PITCHER, UniformInt.of(1, 1)), weight);
			}
		}
		if (SAVANNAS.contains(key)) {
			int weight = MonsterRules.weight(CACKLING_HYENA_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.CACKLING_HYENA, UniformInt.of(3, 5)), weight);
			}
		}
		if (TAIGAS.contains(key)) {
			int weight = MonsterRules.weight(PITCHBACK_BEETLE_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.PITCHBACK_BEETLE, UniformInt.of(1, 2)), weight);
			}
		}
		if (SNOWS.contains(key)) {
			int weight = MonsterRules.weight(RIMECOAT_WOLVERINE_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.RIMECOAT_WOLVERINE, UniformInt.of(1, 1)), weight);
			}
		}
		if (RIVERS.contains(key)) {
			int weight = MonsterRules.weight(SNAPJAW_TURTLE_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.SNAPJAW_TURTLE, UniformInt.of(1, 1)), weight);
			}
		}
		if (SEAS.contains(key)) {
			int weight = MonsterRules.weight(REEFGRASP_OCTOPUS_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.REEFGRASP_OCTOPUS, UniformInt.of(1, 1)), weight);
			}
		}
		if (CHERRY.contains(key)) {
			int weight = MonsterRules.weight(FOXFIRE_KITSUNE_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.FOXFIRE_KITSUNE, UniformInt.of(1, 1)), weight);
			}
		}
		if (CRAGS.contains(key)) {
			int weight = MonsterRules.weight(CRAGCLEAVER_RAM_WEIGHT, rate);
			if (weight > 0) {
				spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.CRAGCLEAVER_RAM, UniformInt.of(1, 1)), weight);
			}
		}
		if (!QUIET.contains(key)) {
			add(spawns, MonsterRules.Kind.GLOOMSTALKER, DARK_WOODS.contains(key) ? 1.25 : 0.75, rate);
			add(spawns, MonsterRules.Kind.GEODE_CRAWLER, 1.0, rate);
			add(spawns, MonsterRules.Kind.MANA_OOZE, 1.0, rate);
		}
	}

	private static void add(BiomeModificationContext.MobSpawnSettingsContext spawns, MonsterRules.Kind kind, double share, double rate) {
		int weight = MonsterRules.weight((int) Math.round(kind.weight * share), rate);
		if (weight > 0) {
			spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(MonsterContent.type(kind), UniformInt.of(kind.min, kind.max)), weight);
		}
	}

	/** Natural spawns (not spawners, eggs or commands) ask the server's switches. */
	private static boolean allowed(EntitySpawnReason reason, MonsterRules.Kind kind) {
		return allowed(reason, kind.id);
	}

	private static boolean allowed(EntitySpawnReason reason, String id) {
		if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
			return true;
		}
		return Config.get().monsters().spawns(id);
	}

	private static boolean deep(ServerLevelAccessor level, BlockPos pos) {
		return pos.getY() < MonsterRules.DEEP_Y && !level.canSeeSky(pos);
	}

	private static boolean inside(ServerLevelAccessor level, BlockPos pos, Set<ResourceKey<Biome>> biomes) {
		Holder<Biome> biome = level.getBiome(pos);
		for (ResourceKey<Biome> key : biomes) {
			if (biome.is(key)) {
				return true;
			}
		}
		return false;
	}

	private static boolean onLey(ServerLevelAccessor level, BlockPos pos) {
		return LeyLines.strength(LeyWalker.seed(level.getLevel()), pos.getX() + 0.5, pos.getZ() + 0.5) >= LeyWalker.ON_LINE;
	}

	/** Whether amethyst (or a geode's calcite shell) lies within about ten blocks: a few dozen blocks looked at, at random. */
	private static boolean nearAmethyst(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int i = 0; i < 40; i++) {
			at.set(pos.getX() + random.nextInt(21) - 10, pos.getY() + random.nextInt(13) - 6, pos.getZ() + random.nextInt(21) - 10);
			if (!level.hasChunkAt(at)) {
				continue;
			}
			BlockState state = level.getBlockState(at);
			if (state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST) || state.is(Blocks.CALCITE) || state.is(Blocks.AMETHYST_CLUSTER)) {
				return true;
			}
		}
		return false;
	}

	/** The monsters a biome spawns (its natural spawns, as the world reads them): for the tests, and the guide's numbers. */
	public static List<EntityType<?>> monstersIn(Holder<Biome> biome) {
		MobSpawnSettings settings = biome.value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		List<EntityType<?>> types = new ArrayList<>();
		settings.getMobsToSpawn(MobCategory.MONSTER).unwrap().forEach(w -> types.add(w.value().type()));
		return types;
	}

	/** Whether one of the six is in the biome's monsters. */
	public static boolean spawnsIn(Holder<Biome> biome, EntityType<? extends Mob> type) {
		return monstersIn(biome).contains(type);
	}

	/** For readers of the rules above: which of the six are ever meant to spawn in a biome. */
	public static Predicate<ResourceKey<Biome>> meantFor(MonsterRules.Kind kind) {
		return switch (kind) {
			case BRAMBLEWALKER -> FORESTS::contains;
			case THUNDERWING_HARPY -> PEAKS::contains;
			case BOG_WITCH_FROG -> SWAMPS::contains;
			default -> key -> !QUIET.contains(key);
		};
	}
}
