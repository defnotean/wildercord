package dev.wildercord.world.dungeons;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The biome's own trees stay out of the dungeons. Dungeons are built before the world's trees grow, so a forest's
 * oaks and birches used to sprout in the middle of a boss arena, where the ward then kept anyone from cutting them
 * down. Now a tree (or fallen log) whose roots would land in a dungeon's footprint, or close enough for its canopy to
 * hang into it, doesn't grow; and the trees already grown in dungeons built before this are cleared once, the first
 * time all of a dungeon is loaded.
 *
 * <p>A dungeon's own trees are left alone: every leaf a dungeon places is persistent, and its dead trees, beams and
 * posts never stand on dirt under the biome's (decaying) leaves. Only a log-and-leaf tree that grew there is cut.</p>
 */
public final class ArenaTrees {
	/** How far past a dungeon's edge a tree's roots must be for its canopy not to hang into it. */
	public static final int OVERHANG = 4;
	/** How far below a dungeon's floor a tree may root and still grow up into it. */
	public static final int BELOW = 8;
	/** The biggest tree the sweep cuts: anything larger isn't a tree the biome grew. */
	public static final int MOST_LOGS = 160;

	/** Dungeon pieces swept in one tick at most, so a burst of loading chunks doesn't stall the server. */
	private static final int SWEEPS_PER_TICK = 1;

	/** Chunks with a dungeon in them, loaded since the last tick, waiting to have their dungeons swept. */
	private static final Set<Map.Entry<ResourceKey<Level>, Long>> WAITING = new LinkedHashSet<>();

	private ArenaTrees() {
	}

	public static void init() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, fresh) -> {
			if (chunk.getAllReferences().keySet().stream().anyMatch(ArenaTrees::dungeon)) {
				WAITING.add(Map.entry(level.dimension(), chunk.getPos().pack()));
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int swept = 0;
			for (var it = WAITING.iterator(); it.hasNext() && swept < SWEEPS_PER_TICK; ) {
				var waiting = it.next();
				ServerLevel level = server.getLevel(waiting.getKey());
				ChunkPos at = ChunkPos.unpack(waiting.getValue());
				var chunk = level == null ? null : level.getChunkSource().getChunkNow(at.x(), at.z());
				if (chunk == null) {
					it.remove();
					continue;
				}
				Swept record = level.getDataStorage().computeIfAbsent(Swept.TYPE);
				boolean later = false;
				for (BoundingBox box : piecesIn(level, chunk)) {
					if (record.boxes.contains(box)) {
						continue;
					}
					if (swept >= SWEEPS_PER_TICK) {
						later = true;
						break;
					}
					if (!loaded(level, box)) {
						// The rest of it is swept when the chunk that completes it loads.
						continue;
					}
					int cut = sweep(level, box);
					if (cut > 0) {
						Wildercord.LOGGER.info("Cleared {} blocks of trees out of the dungeon at {} {} {}", cut, box.minX(), box.minY(), box.minZ());
					}
					record.boxes.add(box);
					record.setDirty();
					swept++;
				}
				if (!later) {
					it.remove();
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WAITING.clear());
	}

	/** The dungeon pieces whose trees have been cleared, so each is swept once. */
	static final class Swept extends SavedData {
		private static final Codec<Swept> CODEC = RecordCodecBuilder.create(i -> i.group(
			BoundingBox.CODEC.listOf().optionalFieldOf("swept", List.of()).forGetter(w -> new ArrayList<>(w.boxes))
		).apply(i, Swept::new));
		static final SavedDataType<Swept> TYPE = new SavedDataType<>(Wildercord.id("arena_trees"), Swept::new, CODEC, null);
		final Set<BoundingBox> boxes = new HashSet<>();

		Swept() {
		}

		private Swept(List<BoundingBox> boxes) {
			this.boxes.addAll(boxes);
		}
	}

	/** Whether a tree rooted at {@code origin} would reach into {@code box}. A tree rooted above its top is clear of it. */
	public static boolean reaches(BoundingBox box, BlockPos origin) {
		return origin.getX() >= box.minX() - OVERHANG && origin.getX() <= box.maxX() + OVERHANG
			&& origin.getZ() >= box.minZ() - OVERHANG && origin.getZ() <= box.maxZ() + OVERHANG
			&& origin.getY() >= box.minY() - BELOW && origin.getY() <= box.maxY();
	}

	/** Whether a tree about to be placed at {@code origin} would grow into a dungeon. */
	public static boolean blocksTree(WorldGenLevel level, BlockPos origin) {
		StructureManager structures;
		if (level instanceof WorldGenRegion region) {
			structures = region.getLevel().structureManager().forWorldGenRegion(region);
		} else if (level instanceof ServerLevel server) {
			structures = server.structureManager();
		} else {
			return false;
		}
		// A piece within OVERHANG of the roots has a reference in one of these chunks, all inside a feature's region.
		int minX = (origin.getX() - OVERHANG) >> 4, maxX = (origin.getX() + OVERHANG) >> 4;
		int minZ = (origin.getZ() - OVERHANG) >> 4, maxZ = (origin.getZ() + OVERHANG) >> 4;
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				for (StructureStart start : structures.startsForStructure(cx, cz, ArenaTrees::dungeon)) {
					for (StructurePiece piece : start.getPieces()) {
						if (piece instanceof DungeonPiece && reaches(piece.getBoundingBox(), origin)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	static boolean dungeon(Structure structure) {
		return structure.type() == DungeonWorldgen.DUNGEON;
	}

	/**
	 * The footprints of the dungeon pieces that reach {@code chunk}, or nothing if a dungeon's start isn't loaded (it
	 * is asked again when more of the world loads).
	 */
	static List<BoundingBox> piecesIn(ServerLevel level, ChunkAccess chunk) {
		List<BoundingBox> boxes = new ArrayList<>();
		for (var entry : chunk.getAllReferences().entrySet()) {
			if (!dungeon(entry.getKey())) {
				continue;
			}
			for (long packed : entry.getValue()) {
				ChunkPos at = ChunkPos.unpack(packed);
				var startChunk = level.getChunkSource().getChunkNow(at.x(), at.z());
				StructureStart start = startChunk == null ? null : startChunk.getStartForStructure(entry.getKey());
				if (start == null || !start.isValid()) {
					continue;
				}
				for (StructurePiece piece : start.getPieces()) {
					if (piece instanceof DungeonPiece && !boxes.contains(piece.getBoundingBox())) {
						boxes.add(piece.getBoundingBox());
					}
				}
			}
		}
		return boxes;
	}

	/** Whether every chunk {@code box} covers is loaded, so it can be swept whole. */
	static boolean loaded(ServerLevel level, BoundingBox box) {
		for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
			for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
				if (level.getChunkSource().getChunkNow(cx, cz) == null) {
					return false;
				}
			}
		}
		return true;
	}

	/** Cuts down the trees that grew inside {@code box}. Returns how many blocks went. */
	public static int sweep(ServerLevel level, BoundingBox box) {
		List<BlockPos> wildLeaves = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (wildLeaf(level.getBlockState(pos))) {
				wildLeaves.add(pos.immutable());
			}
		}
		Set<BlockPos> seen = new HashSet<>();
		List<BlockPos> logs = new ArrayList<>();
		for (BlockPos leaf : wildLeaves) {
			for (BlockPos near : BlockPos.betweenClosed(leaf.offset(-1, -1, -1), leaf.offset(1, 1, 1))) {
				if (box.isInside(near) && !seen.contains(near) && level.getBlockState(near).is(BlockTags.LOGS)) {
					List<BlockPos> tree = tree(level, box, near.immutable(), seen);
					if (tree != null) {
						logs.addAll(tree);
					}
				}
			}
		}
		for (BlockPos pos : logs) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (BlockPos pos : wildLeaves) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		return logs.size() + wildLeaves.size();
	}

	/** A leaf the world grew: it decays once its tree is gone. Every leaf a dungeon places is persistent. */
	static boolean wildLeaf(BlockState state) {
		return state.getBlock() instanceof LeavesBlock && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
	}

	/**
	 * The logs joined to {@code seed} inside {@code box}, if they make a tree the biome grew: rooted in soil (grass, dirt, podzol, moss, mud), under no
	 * placed (persistent) leaf, and no bigger than a tree gets. Null for anything else, such as a dungeon's beam.
	 */
	private static List<BlockPos> tree(ServerLevel level, BoundingBox box, BlockPos seed, Set<BlockPos> seen) {
		List<BlockPos> logs = new ArrayList<>();
		ArrayDeque<BlockPos> open = new ArrayDeque<>();
		open.add(seed);
		seen.add(seed);
		boolean rooted = false, kept = false;
		while (!open.isEmpty()) {
			BlockPos log = open.poll();
			logs.add(log);
			rooted |= level.getBlockState(log.below()).is(BlockTags.SUBSTRATE_OVERWORLD);
			for (BlockPos near : BlockPos.betweenClosed(log.offset(-1, -1, -1), log.offset(1, 1, 1))) {
				BlockState state = level.getBlockState(near);
				if (state.getBlock() instanceof LeavesBlock && !wildLeaf(state)) {
					kept = true;
				} else if (box.isInside(near) && state.is(BlockTags.LOGS) && seen.add(near.immutable())) {
					open.add(near.immutable());
				}
			}
		}
		return rooted && !kept && logs.size() <= MOST_LOGS ? logs : null;
	}
}
