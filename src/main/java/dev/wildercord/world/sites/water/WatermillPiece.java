package dev.wildercord.world.sites.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A miller's house on a river bank, its wheel turning in the stream behind it. The miller still works there. In its
 * own frame the house stands on the bank at z 1-8 (the door at z 1), its floor at y {@link #BASE}; the wheel stands
 * upright in the river at z 11, its axle running back into the house.
 */
public class WatermillPiece extends WaterPiece {
	public static final int WIDTH = 15, HEIGHT = 22, DEPTH = 15;
	public static final int BASE = 8;
	static final int WHEEL_Z = 11;

	private static final BlockState PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState LOG = Blocks.SPRUCE_LOG.defaultBlockState();
	private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
	private static final BlockState AXLE = LOG.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);

	public WatermillPiece(int x, int y, int z, Direction facing) {
		super(WaterSites.WATERMILL, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public WatermillPiece(CompoundTag tag) {
		super(WaterSites.WATERMILL, tag);
	}

	/**
	 * A bank with river water at least a block deep behind it and the house's ground fairly level: tried each way
	 * round from several spots in the chunk.
	 */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		int sea = c.chunkGenerator().getSeaLevel();
		java.util.List<Direction> facings = facings(c);
		for (int[] off : OFFSETS) {
			for (Direction facing : facings) {
				WatermillPiece piece = new WatermillPiece(chunk.getMinBlockX() + off[0], 0, chunk.getMinBlockZ() + off[1], facing);
				BlockPos house = piece.getWorldPos(7, 0, 4);
				BlockPos river = piece.getWorldPos(7, 0, 12);
				if (!land(c, house, sea - 1, sea + 5) || !water(c, river, 1, 64)
						|| !water(c, piece.getWorldPos(4, 0, 12), 1, 64) && !water(c, piece.getWorldPos(10, 0, 12), 1, 64)) {
					continue;
				}
				int ground = surface(c, house) - 1;
				boolean level = true;
				for (int[] at : new int[][] {{2, 1}, {12, 1}, {2, 8}, {12, 8}}) {
					int h = surface(c, piece.getWorldPos(at[0], 0, at[1])) - 1;
					level &= h >= ground - 5 && h <= ground + 3;
				}
				if (!level) {
					continue;
				}
				piece.move(0, ground - BASE, 0);
				return stub(piece, new BlockPos(river.getX(), sea - 1, river.getZ()));
			}
		}
		return Optional.empty();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		// The river's surface in the piece's frame; the wheel's hub stands two over it.
		int water = generator.getSeaLevel() - 1 - boundingBox.minY();
		int hub = water + 2;
		clear(level, bb, 1, BASE + 1, 0, 13, HEIGHT - 1, 9);
		footing(level, bb, COBBLE, 2, 1, 12, 8, BASE);
		fill(level, bb, 3, BASE, 2, 11, BASE, 7, PLANKS);
		for (int x = 6; x <= 8; x++) {
			post(level, bb, Blocks.DIRT.defaultBlockState(), x, 0, BASE - 1);
			set(level, bb, Blocks.DIRT_PATH.defaultBlockState(), x, BASE, 0);
			clear(level, bb, x, BASE + 1, 0, x, BASE + 3, 0);
		}
		house(level, bb, hub);
		wheel(level, bb, water, hub);
	}

	private void house(WorldGenLevel level, BoundingBox bb, int hub) {
		fill(level, bb, 2, BASE + 1, 1, 12, BASE + 1, 8, COBBLE);
		fill(level, bb, 2, BASE + 2, 1, 12, BASE + 4, 8, PLANKS);
		fill(level, bb, 3, BASE + 1, 2, 11, BASE + 4, 7, AIR);
		for (int[] c : new int[][] {{2, 1}, {12, 1}, {2, 8}, {12, 8}}) {
			fill(level, bb, c[0], BASE + 1, c[1], c[0], BASE + 4, c[1], LOG);
		}
		fill(level, bb, 2, BASE + 5, 1, 12, BASE + 5, 8, PLANKS);
		gable(level, bb, 1, 13, 0, 9, BASE + 5, Blocks.SPRUCE_STAIRS, PLANKS, PLANKS, 1, 8);
		fill(level, bb, 7, BASE + 1, 1, 7, BASE + 2, 1, AIR);
		for (int[] w : new int[][] {{2, 4}, {12, 4}, {4, 8}, {10, 8}, {4, 1}, {10, 1}}) {
			set(level, bb, Blocks.GLASS.defaultBlockState(), w[0], BASE + 3, w[1]);
		}
		// The mill's work: a grindstone, a stonecutter, grain and flour.
		set(level, bb, Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR), 4, BASE + 1, 3);
		set(level, bb, Blocks.STONECUTTER.defaultBlockState(), 4, BASE + 1, 5);
		set(level, bb, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, BASE + 1, 7);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), 10, BASE + 1, 7);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), 11, BASE + 1, 7);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), 11, BASE + 2, 7);
		set(level, bb, Blocks.COMPOSTER.defaultBlockState(), 10, BASE + 1, 2);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 11, BASE + 1, 2);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 11, BASE + 1, 3);
		chest(level, bb, 11, BASE + 1, 5, WaterSites.WATERMILL_LOOT, Direction.WEST, 303);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, BASE + 4, 4);
		// The axle comes in through the back wall to a hub over the millstone.
		for (int z = 7; z <= WHEEL_Z - 1; z++) {
			if (z > 7 || hub > BASE && hub <= BASE + 4) {
				set(level, bb, AXLE, 7, hub, z);
			}
		}
		spawn(level, bb, EntityTypes.VILLAGER, 7, BASE + 1, 4, 0);
	}

	/** The wheel, upright in the river: a rim of stripped logs, eight spokes, paddles that dip into the stream. */
	private void wheel(WorldGenLevel level, BoundingBox bb, int water, int hub) {
		clear(level, bb, 2, water + 1, 9, 12, Math.min(HEIGHT - 1, hub + 5), 13);
		BlockState rim = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		for (int x = 3; x <= 11; x++) {
			for (int y = hub - 4; y <= hub + 4; y++) {
				int dx = x - 7, dy = y - hub;
				double d = Math.sqrt(dx * dx + dy * dy);
				if (Math.abs(d - 3.8) < 0.6) {
					set(level, bb, rim, x, y, WHEEL_Z);
					if (dx == 0 || dy == 0) {
						// A paddle either side of the rim at each quarter.
						wet(level, bb, Blocks.SPRUCE_SLAB.defaultBlockState(), x, y, WHEEL_Z - 1);
						wet(level, bb, Blocks.SPRUCE_SLAB.defaultBlockState(), x, y, WHEEL_Z + 1);
					}
				} else if (d < 3.3 && (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy))) {
					set(level, bb, d < 0.5 ? AXLE : Blocks.SPRUCE_PLANKS.defaultBlockState(), x, y, WHEEL_Z);
				}
			}
		}
	}
}
