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
import net.minecraft.world.level.block.RotatedPillarBlock;
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
	private static final BlockState BEAM_X = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
	private static final BlockState BEAM_Z = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
	/** The smokehouse's sooty fieldstone. */
	private static final Palette STONE = Palette.of(Blocks.COBBLESTONE, 6, Blocks.MOSSY_COBBLESTONE, 3, Blocks.STONE_BRICKS, 2);

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
		// Sill beams from stilt to stilt under the deck.
		for (int z = 2; z <= 14; z++) {
			if (z != 5 && z != 9 && z != 13) {
				for (int x : new int[] {2, 7, 12}) {
					set(level, bb, BEAM_Z, x, DECK - 1, z);
				}
			}
		}
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
		// Lamp standards at the deck's corners and log gateposts either side of the steps.
		for (int x : new int[] {2, 12}) {
			for (int z : new int[] {1, 15}) {
				set(level, bb, FENCE, x, DECK + 2, z);
				set(level, bb, Blocks.LANTERN.defaultBlockState(), x, DECK + 3, z);
			}
		}
		for (int x : new int[] {5, 9}) {
			fill(level, bb, x, DECK + 1, 1, x, DECK + 2, 1, LOG);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, DECK + 3, 1);
		}
		hut(level, bb);
		smokehouse(level, bb);
	}

	private void hut(WorldGenLevel level, BoundingBox bb) {
		// A mud-brick sill course, plank walls, log corners and a stripped log plate under the roof.
		fill(level, bb, 3, DECK + 1, 4, 11, DECK + 1, 10, Blocks.MUD_BRICKS.defaultBlockState());
		fill(level, bb, 3, DECK + 2, 4, 11, DECK + 4, 10, PLANKS);
		fill(level, bb, 4, DECK + 1, 5, 10, DECK + 4, 9, AIR);
		fill(level, bb, 4, DECK + 4, 4, 10, DECK + 4, 4, BEAM_X);
		fill(level, bb, 4, DECK + 4, 10, 10, DECK + 4, 10, BEAM_X);
		fill(level, bb, 3, DECK + 4, 5, 3, DECK + 4, 9, BEAM_Z);
		fill(level, bb, 11, DECK + 4, 5, 11, DECK + 4, 9, BEAM_Z);
		for (int[] c : new int[][] {{3, 4}, {11, 4}, {3, 10}, {11, 10}}) {
			fill(level, bb, c[0], DECK + 1, c[1], c[0], DECK + 4, c[1], LOG);
		}
		fill(level, bb, 3, DECK + 5, 4, 11, DECK + 5, 10, PLANKS);
		// A dark oak roof with its eaves carried on brackets over the side walks.
		gable(level, bb, 2, 12, 3, 11, DECK + 5, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS.defaultBlockState(), PLANKS, 4, 10);
		for (int z = 3; z <= 11; z += 2) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 2, DECK + 4, z);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 12, DECK + 4, z);
		}
		// A king post and two small windows in the front gable.
		fill(level, bb, 7, DECK + 6, 4, 7, DECK + 9, 4, LOG);
		for (int x : new int[] {6, 8}) {
			set(level, bb, Blocks.GLASS.defaultBlockState(), x, DECK + 7, 4);
		}
		fill(level, bb, 7, DECK + 1, 4, 7, DECK + 2, 4, AIR);
		for (int[] w : new int[][] {{3, 7}, {11, 7}, {5, 4}, {9, 4}}) {
			set(level, bb, Blocks.GLASS.defaultBlockState(), w[0], DECK + 2, w[1]);
		}
		// A hood course over the door and front windows, a lantern under it and window boxes below.
		for (int x = 4; x <= 10; x++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), x, DECK + 3, 3);
		}
		hangingLantern(level, bb, 6, DECK + 2, 3, 0, false);
		for (int x : new int[] {5, 9}) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), x, DECK + 1, 3);
		}
		set(level, bb, Blocks.POTTED_ALLIUM.defaultBlockState(), 5, DECK + 2, 3);
		set(level, bb, Blocks.FLOWER_POT.defaultBlockState(), 9, DECK + 2, 3);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 2, DECK + 3, 7);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 12, DECK + 3, 7);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, DECK + 4, 7);
		set(level, bb, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.EAST), 4, DECK + 1, 9);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 4, DECK + 1, 8);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 4, DECK + 2, 8);
		set(level, bb, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 4, DECK + 1, 5);
		set(level, bb, Blocks.CRAFTING_TABLE.defaultBlockState(), 10, DECK + 1, 9);
		chest(level, bb, 10, DECK + 1, 5, WaterSites.SMOKEHOUSE_LOOT, Direction.WEST, 301);
		// A rug, a candle on the bench, kelp drying from the plate and a wall light either side.
		fill(level, bb, 6, DECK + 1, 6, 8, DECK + 1, 8, Blocks.CARPET.brown().defaultBlockState());
		set(level, bb, Blocks.CANDLE.defaultBlockState(), 10, DECK + 2, 9);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), 10, DECK + 1, 7);
		for (int x : new int[] {5, 9}) {
			set(level, bb, Blocks.DRIED_KELP_BLOCK.defaultBlockState(), x, DECK + 4, 9);
		}
		sconce(level, bb, 4, DECK + 3, 7, Direction.EAST, false);
		sconce(level, bb, 10, DECK + 3, 7, Direction.WEST, false);
		// The way through to the smokehouse.
		fill(level, bb, 7, DECK + 1, 10, 7, DECK + 2, 10, AIR);
	}

	private void smokehouse(WorldGenLevel level, BoundingBox bb) {
		// Fieldstone walls on dressed quoins, under a stepped stone roof.
		fill(level, bb, 4, DECK + 1, 11, 10, DECK + 4, 14, STONE);
		fill(level, bb, 5, DECK + 1, 12, 9, DECK + 4, 13, AIR);
		for (int x : new int[] {4, 10}) {
			for (int z : new int[] {11, 14}) {
				fill(level, bb, x, DECK + 1, z, x, DECK + 4, z, Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		fill(level, bb, 4, DECK + 5, 11, 10, DECK + 5, 11, Blocks.STONE_BRICKS.defaultBlockState());
		for (int z = 12; z <= 13; z++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false), 4, DECK + 5, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false), 10, DECK + 5, z);
		}
		for (int x = 5; x <= 9; x++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false), x, DECK + 5, 14);
		}
		set(level, bb, Blocks.STONE_BRICKS.defaultBlockState(), 4, DECK + 5, 14);
		set(level, bb, Blocks.STONE_BRICKS.defaultBlockState(), 10, DECK + 5, 14);
		fill(level, bb, 5, DECK + 5, 12, 9, DECK + 5, 13, STONE);
		fill(level, bb, 5, DECK + 6, 12, 9, DECK + 6, 13, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
		fill(level, bb, 7, DECK + 1, 11, 7, DECK + 2, 11, AIR);
		// Barred smoke vents in each side wall.
		for (int x : new int[] {4, 10}) {
			set(level, bb, Blocks.IRON_BARS.defaultBlockState(), x, DECK + 3, 12);
			set(level, bb, Blocks.IRON_BARS.defaultBlockState(), x, DECK + 3, 13);
		}
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState(), 5, DECK + 1, 13);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState(), 9, DECK + 1, 13);
		for (int x : new int[] {6, 8}) {
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), x, DECK + 4, 12);
			set(level, bb, Blocks.DRIED_KELP_BLOCK.defaultBlockState(), x, DECK + 3, 12);
		}
		set(level, bb, Blocks.SMOKER.defaultBlockState().setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, Direction.SOUTH), 7, DECK + 1, 13);
		// The chimney: a signal fire on hay, its smoke a column seen from far off, the stack capped by a corbelled collar.
		fill(level, bb, 7, DECK + 4, 13, 7, DECK + 7, 13, Blocks.STONE_BRICKS.defaultBlockState());
		set(level, bb, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 7, DECK + 6, 13);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 6, DECK + 7, 13);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 8, DECK + 7, 13);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), 7, DECK + 7, 12);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), 7, DECK + 7, 14);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), 7, DECK + 8, 13);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true), 7, DECK + 9, 13);
		// Firewood and salt barrels stacked along the back walks.
		set(level, bb, LOG.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 3, DECK + 1, 13);
		set(level, bb, LOG.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 3, DECK + 1, 14);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 11, DECK + 1, 14);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.WEST), 11, DECK + 1, 13);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 11, DECK + 2, 14);
	}
}
