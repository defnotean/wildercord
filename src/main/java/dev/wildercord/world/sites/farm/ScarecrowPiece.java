package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A field gone wild under four scarecrows, the farmhouse behind it fallen in. By day it is quiet. After dark the dead
 * come up out of the furrows (the site's own spawn list), so the sign at the door asks you not to linger.
 */
public final class ScarecrowPiece extends FarmPiece {
	public ScarecrowPiece(int x, int z, Direction facing) { super(FarmSites.SCARECROW, x, 0, z, 23, 9, 23, facing); }
	public ScarecrowPiece(CompoundTag tag) { super(FarmSites.SCARECROW, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		// The overgrown field.
		for (int x = 1; x <= 21; x++) for (int z = 1; z <= 21; z++) {
			if (x >= 14 && z >= 12 || x == 17) continue;
			int n = noise(x, z);
			if (n < 45) {
				set(level, bb, farmland(false), x, 0, z);
				set(level, bb, crop(Blocks.WHEAT, 7), x, 1, z);
			} else if (n < 70) {
				set(level, bb, s(Blocks.COARSE_DIRT), x, 0, z);
				if (n < 52) set(level, bb, s(Blocks.DEAD_BUSH), x, 1, z);
				else if (n < 58) set(level, bb, s(Blocks.PUMPKIN), x, 1, z);
				else if (n < 62) set(level, bb, s(Blocks.MELON), x, 1, z);
			} else if (n < 85) set(level, bb, s(Blocks.TALL_DRY_GRASS), x, 1, z);
		}
		fill(level, bb, 17, 0, 0, 17, 0, 13, s(Blocks.COARSE_DIRT));
		for (int[] p : new int[][]{{4, 5}, {10, 5}, {4, 15}, {10, 15}}) {
			set(level, bb, s(Blocks.COARSE_DIRT), p[0], 0, p[1]);
			for (int dx = -1; dx <= 1; dx++) for (int dy = 1; dy <= 3; dy++) set(level, bb, AIR, p[0] + dx, dy, p[1]);
			scarecrow(level, bb, p[0], 1, p[1], Direction.SOUTH);
		}
		// The farmhouse, roof half gone.
		fill(level, bb, 14, 0, 14, 21, 0, 21, s(Blocks.OAK_PLANKS));
		for (int x = 14; x <= 21; x++) for (int z = 14; z <= 21; z++) {
			if (x != 14 && x != 21 && z != 14 && z != 21) {
				if (noise(x, 5, z) < 45) set(level, bb, s(Blocks.SPRUCE_SLAB), x, 5, z);
				if (noise(x, 2, z) < 12) set(level, bb, s(Blocks.COBWEB), x, 3, z);
				continue;
			}
			set(level, bb, s(Blocks.COBBLESTONE), x, 1, z);
			for (int y = 2; y <= 4; y++) if (y < 3 || noise(x, y, z) >= 35) set(level, bb, s(noise(x, y + 9, z) < 30 ? Blocks.MOSSY_COBBLESTONE : Blocks.OAK_PLANKS), x, y, z);
		}
		for (int x : new int[]{14, 21}) for (int z : new int[]{14, 21}) fill(level, bb, x, 1, z, x, 4, z, s(Blocks.OAK_LOG));
		fill(level, bb, 17, 1, 14, 17, 3, 14, AIR);
		chest(level, bb, 20, 1, 20, FarmSites.SCARECROW_HOUSE, Direction.WEST, 1);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 15, 1, 20);
		set(level, bb, s(Blocks.COMPOSTER), 15, 1, 15);
		set(level, bb, s(Blocks.COBWEB), 20, 2, 15);
		set(level, bb, s(Blocks.HAY_BLOCK), 20, 1, 15);
		sign(level, bb, standingSign(), 16, 1, 12, "farm_scarecrow", 2);
	}
}
