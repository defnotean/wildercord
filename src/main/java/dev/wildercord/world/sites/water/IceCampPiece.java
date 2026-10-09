package dev.wildercord.world.sites.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * An ice fishing camp on a frozen lake: a floor of packed ice laid over the water, two shanties each roofed over its
 * own hole (so the hole never freezes), a hide tent with the camp's sled chest, and soul fire, which warms without
 * melting the ice. In its own frame the camp is centred on x 8, z 8; the water's surface is at y {@link #BASE}.
 */
public class IceCampPiece extends WaterPiece {
	public static final int WIDTH = 17, HEIGHT = 14, DEPTH = 17;
	public static final int BASE = 6;
	static final int CX = 8, CZ = 8;
	/** The two shanties' holes: open water under a roof. */
	static final int[][] HOLES = {{3, 4}, {13, 4}};

	private static final BlockState PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState ICE = Blocks.PACKED_ICE.defaultBlockState();

	public IceCampPiece(int x, int y, int z, Direction facing) {
		super(WaterSites.ICE_CAMP, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public IceCampPiece(CompoundTag tag) {
		super(WaterSites.ICE_CAMP, tag);
	}

	/** Open water deep enough to fish, all across the camp. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		IceCampPiece piece = new IceCampPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		int sea = c.chunkGenerator().getSeaLevel();
		BlockPos mid = piece.getWorldPos(CX, 0, CZ);
		if (!water(c, mid, 3, 255)) {
			return Optional.empty();
		}
		for (int[] at : new int[][] {{1, 8}, {15, 8}, {8, 1}, {8, 15}, {3, 4}, {13, 4}}) {
			if (!water(c, piece.getWorldPos(at[0], 0, at[1]), 2, 255)) {
				return Optional.empty();
			}
		}
		piece.move(0, sea - 1 - BASE, 0);
		return stub(piece, new BlockPos(mid.getX(), sea - 1, mid.getZ()));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				if (dist(x, z, CX, CZ) > 7.5) {
					continue;
				}
				set(level, bb, ICE, x, BASE, z);
				clear(level, bb, x, BASE + 1, z, x, HEIGHT - 1, z);
				if (noise(x, z) < 30) {
					set(level, bb, Blocks.SNOW.defaultBlockState(), x, BASE + 1, z);
				}
			}
		}
		// The fire ring, with logs to sit on.
		clear(level, bb, 5, BASE + 1, 6, 11, BASE + 1, 10);
		set(level, bb, Blocks.SOUL_CAMPFIRE.defaultBlockState(), CX, BASE + 1, CZ);
		BlockState bench = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		set(level, bb, bench, 6, BASE + 1, CZ);
		set(level, bb, bench, 10, BASE + 1, CZ);
		for (int[] at : new int[][] {{8, 1}, {1, 8}, {15, 8}, {13, 12}}) {
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), at[0], BASE + 1, at[1]);
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState(), at[0], BASE + 2, at[1]);
		}
		shanty(level, bb, 2, false);
		shanty(level, bb, 11, true);
		tent(level, bb);
	}

	/** A shanty 4 wide from {@code x0}, its door toward the fire, its hole under its roof. */
	private void shanty(WorldGenLevel level, BoundingBox bb, int x0, boolean east) {
		int x1 = x0 + 3;
		fill(level, bb, x0, BASE + 1, 3, x1, BASE + 3, 6, PLANKS);
		fill(level, bb, x0 + 1, BASE + 1, 4, x1 - 1, BASE + 3, 5, AIR);
		fill(level, bb, x0, BASE + 4, 3, x1, BASE + 4, 6, Blocks.SPRUCE_SLAB.defaultBlockState());
		int door = east ? x0 : x1;
		fill(level, bb, door, BASE + 1, 4, door, BASE + 2, 4, AIR);
		int hole = east ? x1 - 1 : x0 + 1, seat = east ? x0 + 1 : x1 - 1;
		set(level, bb, Blocks.WATER.defaultBlockState(), hole, BASE, 4);
		set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), hole, BASE + 3, 4);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), hole, BASE + 1, 5);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, east ? Direction.WEST : Direction.EAST), seat, BASE + 1, 5);
		set(level, bb, Blocks.GLASS.defaultBlockState(), east ? x1 : x0, BASE + 2, 4);
	}

	/** A hide tent, an A-frame of wool open toward the fire, with the sled chest at its back. */
	private void tent(WorldGenLevel level, BoundingBox bb) {
		var wool = Blocks.WOOL.brown().defaultBlockState();
		var stairs = Blocks.WOOL_STAIRS.brown();
		for (int z = 12; z <= 15; z++) {
			set(level, bb, stair(stairs, Direction.EAST), 6, BASE + 1, z);
			set(level, bb, stair(stairs, Direction.WEST), 10, BASE + 1, z);
			set(level, bb, stair(stairs, Direction.EAST), 7, BASE + 2, z);
			set(level, bb, stair(stairs, Direction.WEST), 9, BASE + 2, z);
			set(level, bb, wool, 8, BASE + 3, z);
		}
		clear(level, bb, 7, BASE + 1, 12, 9, BASE + 1, 14);
		clear(level, bb, 8, BASE + 2, 12, 8, BASE + 2, 14);
		fill(level, bb, 7, BASE + 1, 15, 9, BASE + 1, 15, wool);
		set(level, bb, wool, 8, BASE + 2, 15);
		set(level, bb, Blocks.CARPET.red().defaultBlockState(), 7, BASE + 1, 13);
		set(level, bb, Blocks.CARPET.red().defaultBlockState(), 9, BASE + 1, 13);
		chest(level, bb, 8, BASE + 1, 14, WaterSites.ICE_CAMP_LOOT, Direction.SOUTH, 304);
	}
}
