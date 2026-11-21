package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.MethodSources;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

import java.util.Random;

/**
 * Where fallen knights rise: around a player in a place where old fights were fought, the structures in the
 * {@code #wildercord:knight_haunts} tag (strongholds, ancient cities and the expeditions, so a data pack can add more) and the
 * spawner rooms of dungeons (a spawner on cobblestone near). Every ten seconds, a chance; never with
 * {@link WildercordConfig.AuraWorldSettings#maxKnightsNearby} already about, never on Peaceful, never in the light or close
 * to a player, and always inside the same place. A knight's rank and method follow where it rose: a dungeon's are rank 1 and
 * any method; a stronghold's rank 2; an ancient city's or an expedition's rank 3, each favouring the methods its own chests do
 * ({@link MethodSources}).
 */
public final class KnightSpawner {
	private KnightSpawner() {}

	/** The structures knights haunt. */
	public static final TagKey<Structure> HAUNTS = TagKey.create(Registries.STRUCTURE, Wildercord.id("knight_haunts"));

	/** A haunted place: the rank of what rises there, the source whose weights pick its method, and its bounds. */
	public record Haunt(int rank, String source, @Nullable StructureStart start) {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(KnightSpawner::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % AuraWorldRules.KNIGHT_PERIOD != 0) {
			return;
		}
		WildercordConfig.AuraWorldSettings settings = Config.get().auraWorld();
		if (!settings.knightsSpawn()) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
				continue;
			}
			RandomSource random = level.getRandom();
			for (ServerPlayer player : level.players()) {
				if (player.isSpectator() || random.nextDouble() >= AuraWorldRules.chance(AuraWorldRules.KNIGHT_CHANCE, settings.knightSpawnRate())) {
					continue;
				}
				Haunt haunt = haunt(level, player.blockPosition());
				if (haunt == null || nearby(level, player.blockPosition()) >= settings.maxKnightsNearby()) {
					continue;
				}
				spawnIn(level, player.blockPosition(), haunt, random);
			}
		}
	}

	/** Knights within {@link AuraWorldRules#KNIGHT_CROWD} blocks of {@code pos}. */
	public static int nearby(ServerLevel level, BlockPos pos) {
		double r = AuraWorldRules.KNIGHT_CROWD;
		return level.getEntities(AuraWorld.FALLEN_KNIGHT, k -> k.isAlive() && k.blockPosition().distSqr(pos) < r * r).size();
	}

	/** The haunted place {@code pos} is in, or null. */
	public static @Nullable Haunt haunt(ServerLevel level, BlockPos pos) {
		StructureStart start = level.structureManager().getStructureWithPieceAt(pos, HAUNTS);
		if (start.isValid()) {
			Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
			String path = id == null ? "" : id.getPath();
			boolean stronghold = path.equals("stronghold");
			// A structure's own chests' source when it has one (the ancient city's, each expedition's), else any method.
			String source = MethodSources.byId(path).isPresent() ? path : "";
			return new Haunt(AuraWorldRules.knightRank(!stronghold, stronghold), source, start);
		}
		return spawnerRoom(level, pos) ? new Haunt(AuraWorldRules.knightRank(false, false), "", null) : null;
	}

	/** Whether a dungeon's spawner (on cobblestone or mossy cobblestone) is within ten blocks. */
	static boolean spawnerRoom(ServerLevel level, BlockPos pos) {
		ChunkPos centre = ChunkPos.containing(pos);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!level.hasChunk(centre.x() + dx, centre.z() + dz)) {
					continue;
				}
				LevelChunk chunk = level.getChunk(centre.x() + dx, centre.z() + dz);
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof SpawnerBlockEntity && entity.getBlockPos().distSqr(pos) < 100) {
						BlockState floor = level.getBlockState(entity.getBlockPos().below());
						if (floor.is(Blocks.COBBLESTONE) || floor.is(Blocks.MOSSY_COBBLESTONE)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/** Raises a knight 8 to 20 blocks from {@code near}, somewhere dark inside the same place. Returns it, or null. */
	public static @Nullable FallenKnight spawnIn(ServerLevel level, BlockPos near, Haunt haunt, RandomSource random) {
		for (int i = 0; i < 16; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 8 + random.nextDouble() * 12;
			BlockPos column = near.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
			BlockPos at = floor(level, column);
			if (at == null || level.getMaxLocalRawBrightness(at) > 9 || level.getNearestPlayer(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 7, false) != null) {
				continue;
			}
			if (haunt.start() != null ? !level.structureManager().getStructureWithPieceAt(at, HAUNTS).isValid() : !spawnerRoom(level, at)) {
				continue;
			}
			return raise(level, at, haunt, random);
		}
		return null;
	}

	/** A knight of {@code haunt}'s rank and one of its methods, standing at {@code at}. */
	public static @Nullable FallenKnight raise(ServerLevel level, BlockPos at, Haunt haunt, RandomSource random) {
		FallenKnight knight = AuraWorld.FALLEN_KNIGHT.create(level, EntitySpawnReason.NATURAL);
		if (knight == null) {
			return null;
		}
		knight.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
		knight.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, null);
		knight.setRank(haunt.rank());
		knight.setMethod(method(haunt.source(), random));
		level.addFreshEntityWithPassengers(knight);
		return knight;
	}

	/** A method drawn by a source's weights, or any built-in one. */
	static BreathingMethod method(String source, RandomSource random) {
		Random draw = new Random(random.nextLong());
		return MethodSources.byId(source).flatMap(s -> s.draw(draw)).flatMap(BreathingMethods::byId)
			.orElseGet(() -> BreathingMethods.BUILT_IN.get(draw.nextInt(BreathingMethods.BUILT_IN.size())));
	}

	/** A floor to stand on in a column, within four blocks above or below {@code column}'s height. */
	static @Nullable BlockPos floor(ServerLevel level, BlockPos column) {
		if (!level.hasChunkAt(column)) {
			return null;
		}
		for (int dy = 4; dy >= -4; dy--) {
			BlockPos at = column.above(dy);
			BlockPos below = at.below();
			if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir()
				&& level.getFluidState(at).isEmpty()) {
				return at;
			}
		}
		return null;
	}
}
