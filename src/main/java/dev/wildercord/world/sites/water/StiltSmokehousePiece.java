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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A fisher's hut on stilts over a swamp or a river, with a smokehouse behind it whose chimney sends up a signal
 * fire's tall smoke, seen from far off. In its own frame: the water's surface is at y {@link #BASE}, the deck one
 * over it, the landing steps at z 0, the hut at z 4-10, the smokehouse at z 11-14.
 */
public class StiltSmokehousePiece extends WaterPiece {
	public static final int WIDTH = 15, HEIGHT = 21, DEPTH = 17;
	public static final int BASE = 8;
	/** The deck: you stand at {@code DECK + 1}. */
	public static final int DECK = BASE + 1;

	private static final BlockState PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState LOG = Blocks.SPRUCE_LOG.defaultBlockState();
	private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
	private static final BlockState FENCE = Blocks.SPRUCE_FENCE.defaultBlockState();

	public StiltSmokehousePiece(int x, int y, int z, Direction facing) {
		super(WaterSites.STILT_SMOKEHOUSE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public StiltSmokehousePiece(CompoundTag tag) {
		super(WaterSites.STILT_SMOKEHOUSE, tag);
	}

	/** Shallow open water under the middle, and no corner standing high out of it. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		StiltSmokehousePiece piece = new StiltSmokehousePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		int sea = c.chunkGenerator().getSeaLevel();
		BlockPos mid = piece.getWorldPos(7, 0, 8);
		if (!water(c, mid, 1, 6)) {
			return Optional.empty();
		}
		for (int[] at : new int[][] {{2, 1}, {12, 1}, {2, 15}, {12, 15}, {7, 0}}) {
			BlockPos q = piece.getWorldPos(at[0], 0, at[1]);
			if (surface(c, q) > sea + 1 || floor(c, q) < sea - BASE) {
				return Optional.empty();
			}
		}
		piece.move(0, sea - 1 - BASE, 0);
		return stub(piece, new BlockPos(mid.getX(), sea - 1, mid.getZ()));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		clear(level, bb, 2, DECK + 1, 0, 12, HEIGHT - 1, 15);
		// Stilts down to the mud, then the deck over them.
		for (int x : new int[] {2, 7, 12}) {
			for (int z : new int[] {1, 5, 9, 13, 15}) {
				post(level, bb, LOG, x, z, DECK - 1);
			}
		}
		fill(level, bb, 2, DECK, 1, 12, DECK, 15, PLANKS);
		// Landing steps up out of the water.
		for (int x = 6; x <= 8; x++) {
			post(level, bb, LOG, x, 0, BASE - 1);
			wet(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH), x, BASE, 0);
		}
		// Rails, with a gap for the steps.
		for (int z = 1; z <= 15; z++) {
			set(level, bb, FENCE, 2, DECK + 1, z);
			set(level, bb, FENCE, 12, DECK + 1, z);
		}
		for (int x = 2; x <= 12; x++) {
			set(level, bb, FENCE, x, DECK + 1, 15);
			if (x < 6 || x > 8) {
				set(level, bb, FENCE, x, DECK + 1, 1);
			}
		}
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 2, DECK + 2, 1);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 12, DECK + 2, 1);
		hut(level, bb);
		smokehouse(level, bb);
	}

	private void hut(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 3, DECK + 1, 4, 11, DECK + 4, 10, PLANKS);
		fill(level, bb, 4, DECK + 1, 5, 10, DECK + 4, 9, AIR);
		for (int[] c : new int[][] {{3, 4}, {11, 4}, {3, 10}, {11, 10}}) {
			fill(level, bb, c[0], DECK + 1, c[1], c[0], DECK + 4, c[1], LOG);
		}
		fill(level, bb, 3, DECK + 5, 4, 11, DECK + 5, 10, PLANKS);
		gable(level, bb, 2, 12, 3, 11, DECK + 5, Blocks.SPRUCE_STAIRS, PLANKS, PLANKS, 4, 10);
		fill(level, bb, 7, DECK + 1, 4, 7, DECK + 2, 4, AIR);
		for (int[] w : new int[][] {{3, 7}, {11, 7}, {5, 4}, {9, 4}}) {
			set(level, bb, Blocks.GLASS.defaultBlockState(), w[0], DECK + 2, w[1]);
		}
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, DECK + 4, 7);
		set(level, bb, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.EAST), 4, DECK + 1, 9);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 4, DECK + 1, 8);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 4, DECK + 2, 8);
		set(level, bb, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 4, DECK + 1, 5);
		set(level, bb, Blocks.CRAFTING_TABLE.defaultBlockState(), 10, DECK + 1, 9);
		chest(level, bb, 10, DECK + 1, 5, WaterSites.SMOKEHOUSE_LOOT, Direction.WEST, 301);
		// The way through to the smokehouse.
		fill(level, bb, 7, DECK + 1, 10, 7, DECK + 2, 10, AIR);
	}

	private void smokehouse(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 4, DECK + 1, 11, 10, DECK + 4, 14, COBBLE);
		fill(level, bb, 5, DECK + 1, 12, 9, DECK + 4, 13, AIR);
		fill(level, bb, 4, DECK + 5, 11, 10, DECK + 5, 14, Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
		fill(level, bb, 7, DECK + 1, 11, 7, DECK + 2, 11, AIR);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState(), 5, DECK + 1, 13);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState(), 9, DECK + 1, 13);
		for (int x : new int[] {6, 8}) {
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), x, DECK + 4, 12);
			set(level, bb, Blocks.DRIED_KELP_BLOCK.defaultBlockState(), x, DECK + 3, 12);
		}
		set(level, bb, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.SOUTH), 7, DECK + 1, 13);
		// The chimney: a signal fire on hay, its smoke a column seen from far off.
		fill(level, bb, 7, DECK + 4, 13, 7, DECK + 7, 13, COBBLE);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), 7, DECK + 8, 13);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true), 7, DECK + 9, 13);
	}
}
