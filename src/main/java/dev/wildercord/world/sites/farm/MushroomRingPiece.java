package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A round pit sunk four blocks into the forest floor, ringed in glass under a dark oak eave, where mushrooms are
 * grown on mycelium round one giant red cap. Steps lead down from the south.
 */
public final class MushroomRingPiece extends FarmPiece {
	private static final int C = 10;

	public MushroomRingPiece(int x, int z, Direction facing) { super(FarmSites.MUSHROOM_RING, x, 0, z, 21, 13, 21, facing); }
	public MushroomRingPiece(CompoundTag tag) { super(FarmSites.MUSHROOM_RING, tag); }

	@Override
	protected int ground() {
		return 4;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.PODZOL), 5);
		var wall = s(Blocks.MOSSY_STONE_BRICKS);
		var shingle = s(Blocks.DARK_OAK_PLANKS);
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			double d = dist(x, z, C, C);
			if (d <= 8.5) {
				// The pit: open down to its bed.
				fill(level, bb, x, 1, z, x, 4, z, AIR);
				int n = noise(x, z);
				set(level, bb, s(n < 55 ? Blocks.MYCELIUM : n < 85 ? Blocks.PODZOL : Blocks.COARSE_DIRT), x, 0, z);
				if (d > 3 && n < 18) set(level, bb, s(Blocks.RED_MUSHROOM), x, 1, z);
				else if (d > 3 && n < 40) set(level, bb, s(Blocks.BROWN_MUSHROOM), x, 1, z);
			} else if (d <= 10.5) {
				fill(level, bb, x, 0, z, x, 3, z, wall);
				set(level, bb, s(Blocks.STONE_BRICKS), x, 4, z);
				if (d > 9.5) fill(level, bb, x, 5, z, x, 7, z, s(Blocks.GLASS));
			} else if (noise(x, z) < 22) {
				// Mushrooms coming up through the podzol round the outside.
				set(level, bb, s(noise(z, x) < 50 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM), x, 5, z);
			}
			// A stepped cone of dark oak shingles rising to an open eye over the giant cap.
			if (d > 4.5 && d <= 10.5) {
				int k = Math.min(3, (int) ((10.5 - d) / 1.5));
				double outer = 10.5 - 1.5 * k;
				int dx = x - C, dz = z - C;
				Direction in = Math.abs(dx) >= Math.abs(dz) ? (dx < 0 ? Direction.EAST : Direction.WEST) : (dz < 0 ? Direction.NORTH : Direction.SOUTH);
				set(level, bb, d > outer - 0.8 ? stair(Blocks.DARK_OAK_STAIRS, in, false) : shingle, x, 8 + k, z);
				if (k == 3 && d <= 5.5) set(level, bb, s(Blocks.MUSHROOM_STEM), x, 12, z);
			}
		}
		// Posts round the glass, and lights let into the pit wall.
		for (int[] p : new int[][]{{C, 0}, {C, 20}, {0, C}, {20, C}, {3, 3}, {17, 3}, {3, 17}, {17, 17}}) {
			if (p[1] == 0) continue;
			fill(level, bb, p[0], 5, p[1], p[0], 7, p[1], s(Blocks.DARK_OAK_LOG));
		}
		for (int[] p : new int[][]{{C, 19}, {1, C}, {19, C}, {3, 4}, {17, 4}, {3, 16}, {17, 16}}) set(level, bb, s(Blocks.SHROOMLIGHT), p[0], 2, p[1]);
		// The way in: a gap in the glass between two posts under a timber arch, and steps down the south wall.
		fill(level, bb, 9, 5, 0, 11, 7, 0, AIR);
		for (int x : new int[]{8, 12}) fill(level, bb, x, 5, 0, x, 7, 0, s(Blocks.DARK_OAK_LOG));
		set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, true), 9, 7, 0);
		set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, true), 11, 7, 0);
		hangingLantern(level, bb, 10, 7, 0, 0, false);
		for (int i = 0; i < 4; i++) {
			fill(level, bb, 9, 3 - i, 1 + i, 11, 3 - i, 1 + i, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
			fill(level, bb, 9, 4 - i, 1 + i, 11, 7, 1 + i, AIR);
			if (i > 0) fill(level, bb, 9, 0, 1 + i, 11, 2 - i, 1 + i, wall);
		}
		fill(level, bb, 9, 0, 5, 11, 0, 8, s(Blocks.COARSE_DIRT));
		fill(level, bb, 9, 1, 5, 11, 1, 8, AIR);
		// Lanterns hung from the cone over the gallery.
		for (int[] p : new int[][]{{C, 2}, {C, 18}, {2, C}, {18, C}}) hangingLantern(level, bb, p[0], 8, p[1], 0, false);
		// Toadstools in the four corners outside the ring.
		for (int[] t : new int[][]{{1, 1, 0}, {19, 1, 1}, {1, 19, 1}, {19, 19, 0}}) {
			int tx = t[0], tz = t[1];
			var cap = s(t[2] == 0 ? Blocks.RED_MUSHROOM_BLOCK : Blocks.BROWN_MUSHROOM_BLOCK);
			fill(level, bb, tx, 5, tz, tx, 6, tz, s(Blocks.MUSHROOM_STEM));
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(level, bb, cap, tx + dx, 7, tz + dz);
			set(level, bb, cap, tx, 8, tz);
		}
		// The giant cap at the heart of the ring, domed and glowing under its gills.
		fill(level, bb, C, 0, C, C, 7, C, s(Blocks.MUSHROOM_STEM));
		for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
			if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
			set(level, bb, s(Blocks.RED_MUSHROOM_BLOCK), C + dx, 8, C + dz);
			if (Math.abs(dx) == 2 || Math.abs(dz) == 2) set(level, bb, s(Blocks.RED_MUSHROOM_BLOCK), C + dx, 7, C + dz);
			if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) set(level, bb, s(Blocks.RED_MUSHROOM_BLOCK), C + dx, 9, C + dz);
		}
		for (int[] g : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) set(level, bb, s(Blocks.SHROOMLIGHT), C + g[0], 7, C + g[1]);
		// The grower's corner: a cauldron, a smoker, a composter and the cellar chest.
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3), 5, 1, 10);
		set(level, bb, facing(Blocks.SMOKER, Direction.WEST), 15, 1, 10);
		set(level, bb, s(Blocks.COMPOSTER), 6, 1, 6);
		set(level, bb, s(Blocks.COMPOSTER), 14, 1, 6);
		chest(level, bb, C, 1, 16, FarmSites.MUSHROOM_CELLAR, Direction.SOUTH, 1);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 9, 1, 16);
	}
}
