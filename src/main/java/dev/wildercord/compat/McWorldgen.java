package dev.wildercord.compat;

import com.mojang.datafixers.util.Pair;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Version seam for worldgen feature registration, biome spawn additions, biome lookup and structure queries.
 */
public final class McWorldgen {
	private McWorldgen() {}

	/** Registers a zero-config {@link SimpleWorldFeature} under {@code wildercord:<path>}. */
	public static void registerFeature(String path, SimpleWorldFeature feature) {
		Registry.register(BuiltInRegistries.FEATURE, Wildercord.id(path), feature);
	}

	/** Adds a mob spawn entry via Fabric biome modification API. */
	public static void addSpawn(BiomeModificationContext context, MobCategory category, EntityType<?> type,
			int minGroup, int maxGroup, int weight) {
		addSpawn(context.getSpawnSettings(), category, type, minGroup, maxGroup, weight);
	}

	/** Adds a mob spawn entry directly onto a {@link BiomeModificationContext.SpawnSettingsContext}. */
	public static void addSpawn(BiomeModificationContext.SpawnSettingsContext spawns, MobCategory category,
			EntityType<?> type, int minGroup, int maxGroup, int weight) {
		spawns.addSpawn(category, new MobSpawnSettings.SpawnerData(type, minGroup, maxGroup), weight);
	}

	/** Returns natural spawn entity types registered in {@code biome} for {@code category}. */
	public static List<EntityType<?>> biomeSpawnTypes(Holder<Biome> biome, MobCategory category) {
		MobSpawnSettings settings = biome.value().getMobSettings();
		List<EntityType<?>> types = new ArrayList<>();
		settings.getMobs(category).unwrap().forEach(w -> types.add(w.value().type()));
		return types;
	}

	/** Optional {@code Biomes.DAPPLED_FOREST} key when present in the active MC version (empty in 26.2). */
	public static Optional<ResourceKey<Biome>> dappledForestBiome() {
		return Optional.empty();
	}

	/** Returns an unmodifiable set combining {@code base} with {@code extra} if present. */
	@SafeVarargs
	public static Set<ResourceKey<Biome>> biomeSetWithOptional(Set<ResourceKey<Biome>> base, Optional<ResourceKey<Biome>>... extras) {
		Set<ResourceKey<Biome>> combined = new HashSet<>(base);
		for (Optional<ResourceKey<Biome>> extra : extras) {
			extra.ifPresent(combined::add);
		}
		return Set.copyOf(combined);
	}

	/**
	 * Quick precheck for whether a valid structure biome exists at the chunk center surface.
	 * Keeping this outside {@code ArchiveStructure.findGenerationPoint} preserves a single
	 * {@code getFirstOccupiedHeight} call site for test Mixins.
	 */
	public static boolean canValidBiomeExistAtChunkCenter(Structure.GenerationContext context) {
		int cx = context.chunkPos().getMiddleBlockX();
		int cz = context.chunkPos().getMiddleBlockZ();
		int surface = context.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(), context.randomState());
		Holder<Biome> centerBiome = context.biomeSource().getNoiseBiome(
			QuartPos.fromBlock(cx), QuartPos.fromBlock(surface), QuartPos.fromBlock(cz),
			context.randomState().sampler());
		return context.validBiome().test(centerBiome);
	}

	/** Looks up the biome at block coordinates inside structure generation. */
	public static Holder<Biome> getNoiseBiome(Structure.GenerationContext context, int blockX, int blockY, int blockZ) {
		return context.biomeSource().getNoiseBiome(
			QuartPos.fromBlock(blockX), QuartPos.fromBlock(blockY), QuartPos.fromBlock(blockZ),
			context.randomState().sampler());
	}

	/** Lists structure starts matching {@code predicate} in chunk {@code (chunkX, chunkZ)}. */
	public static List<StructureStart> startsForStructure(StructureManager manager, int chunkX, int chunkZ, Predicate<Structure> predicate) {
		return manager.startsForStructure(new ChunkPos(chunkX, chunkZ), predicate);
	}

	/**
	 * Whether {@code pos} sits inside a worldgen region's writable area. 26.1.2 has no
	 * {@code isWithinWriteZone}: vanilla allows writes within one chunk of the region centre.
	 */
	public static boolean withinWriteZone(net.minecraft.world.level.WorldGenLevel level, BlockPos pos) {
		if (!(level instanceof net.minecraft.server.level.WorldGenRegion region)) {
			return true;
		}
		ChunkPos centre = region.getCenter();
		// ChunkPos is a record on 26.1.2 with private fields; compare via the block-coordinate getters.
		return Math.abs((pos.getX() >> 4) - (centre.getMinBlockX() >> 4)) <= 1
			&& Math.abs((pos.getZ() >> 4) - (centre.getMinBlockZ() >> 4)) <= 1;
	}

	/** Lists structure starts matching {@code structure} at {@code chunk}. */
	public static List<StructureStart> startsForStructure(StructureManager manager, ChunkPos chunk, Structure structure) {
		return manager.startsForStructure(chunk, s -> s == structure);
	}

	/** Finds the nearest structure matching a {@link TagKey} or {@link HolderSet}. */
	@SuppressWarnings("unchecked")
	public static  BlockPos findNearestStructure(ServerLevel level, Object target, BlockPos origin,
			int radiusCells, boolean skipKnown) {
		if (target instanceof TagKey<?> tag) {
			return level.findNearestMapStructure((TagKey<Structure>) tag, origin, radiusCells, skipKnown);
		}
		return Optional.ofNullable(level.getChunkSource().getGenerator().findNearestMapStructure(
			level, (HolderSet<Structure>) target, origin, radiusCells, skipKnown))
			.map(Pair::getFirst)
			.orElse(null);
	}
}
