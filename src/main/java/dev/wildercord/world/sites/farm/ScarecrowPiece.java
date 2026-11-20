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
 * A field gone wild under four scarecrows and a great one over the middle, the farmhouse behind it fallen in and the
 * grain silo by the gate open to the sky. By day it is quiet. After dark the dead come up out of the furrows (the site's
 * own spawn list), so the sign at the door asks you not to linger.
 */
public final class ScarecrowPiece extends FarmPiece {
	public ScarecrowPiece(int x, int z, Direction facing) { super(FarmSites.SCARECROW, x, 0, z, 23, 14, 23, facing); }
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
		// What is left of the field fence: odd posts standing in the grass.
		for (int i = 1; i <= 21; i++) {
			if ((i < 16 || i > 18) && noise(i, 0, 7) < 40) set(level, bb, s(Blocks.OAK_FENCE), i, 1, 0);
			if (noise(0, i, 7) < 40) set(level, bb, s(Blocks.OAK_FENCE), 0, 1, i);
			if (i < 13 && noise(i, 22, 7) < 40) set(level, bb, s(Blocks.OAK_FENCE), i, 1, 22);
			if (i < 13 && noise(22, i, 7) < 40) set(level, bb, s(Blocks.OAK_FENCE), 22, 1, i);
		}
		// The old field gate: one post still up under a broken crossbar and a soul lantern, the other leaning short.
		var post = s(Blocks.SPRUCE_LOG);
		fill(level, bb, 16, 1, 0, 16, 4, 0, post);
		fill(level, bb, 18, 1, 0, 18, 2, 0, post);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 17, 4, 0);
		hangingLantern(level, bb, 17, 3, 0, 0, true);
		set(level, bb, s(Blocks.SPRUCE_SLAB), 19, 1, 1); set(level, bb, s(Blocks.SPRUCE_SLAB), 20, 1, 1);
		for (int[] p : new int[][]{{4, 5}, {10, 5}, {4, 15}, {10, 15}}) {
			set(level, bb, s(Blocks.COARSE_DIRT), p[0], 0, p[1]);
			for (int dx = -1; dx <= 1; dx++) for (int dy = 1; dy <= 3; dy++) set(level, bb, AIR, p[0] + dx, dy, p[1]);
			scarecrow(level, bb, p[0], 1, p[1], Direction.SOUTH);
		}
		// A dead oak in the middle of the field, webs in its crook.
		var dead = s(Blocks.DARK_OAK_LOG);
		fill(level, bb, 7, 1, 10, 7, 5, 10, dead);
		for (int[] b : new int[][]{{6, 4, 10}, {5, 5, 10}, {8, 5, 10}, {9, 6, 10}, {7, 6, 9}, {7, 7, 8}, {7, 5, 11}, {4, 6, 10}}) set(level, bb, dead, b[0], b[1], b[2]);
		set(level, bb, s(Blocks.COBWEB), 6, 5, 10); set(level, bb, s(Blocks.COBWEB), 8, 6, 10);
		set(level, bb, s(Blocks.COARSE_DIRT), 7, 0, 10);
		// The great scarecrow over the middle of the field: a tall post and crossbar, a straw body in a ragged coat and a
		// lantern-lit head, a soul lantern swinging from one arm. It is the first thing seen across the furrows.
		set(level, bb, s(Blocks.COARSE_DIRT), 11, 0, 10);
		fill(level, bb, 10, 1, 9, 12, 8, 11, AIR);
		fill(level, bb, 11, 1, 10, 11, 4, 10, s(Blocks.STRIPPED_SPRUCE_LOG));
		fill(level, bb, 11, 5, 10, 11, 6, 10, s(Blocks.HAY_BLOCK));
		set(level, bb, s(Blocks.BROWN_WOOL), 11, 5, 9);
		set(level, bb, facing(Blocks.JACK_O_LANTERN, Direction.SOUTH), 11, 7, 10);
		set(level, bb, s(Blocks.BROWN_CARPET), 11, 8, 10);
		for (int x : new int[]{9, 10, 12, 13}) set(level, bb, s(Blocks.SPRUCE_FENCE), x, 6, 10);
		set(level, bb, s(Blocks.SPRUCE_FENCE), 13, 5, 10);
		hangingLantern(level, bb, 13, 4, 10, 0, true);
		set(level, bb, s(Blocks.BROWN_WOOL), 9, 5, 10);
		// The old grain silo by the gate: a round fieldstone tower open to the sky, its top broken off at odd heights.
		Palette silo = Palette.of(Blocks.COBBLESTONE, 4, Blocks.MOSSY_COBBLESTONE, 3, Blocks.ANDESITE, 1);
		for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
			int x = 20 + dx, z = 5 + dz, d2 = dx * dx + dz * dz;
			set(level, bb, s(Blocks.COARSE_DIRT), x, 0, z);
			fill(level, bb, x, 1, z, x, 12, z, AIR);
			if (d2 < 3 || d2 > 5) continue;
			int top = 6 + noise(x, 11, z) % 6;
			fill(level, bb, x, 1, z, x, top, z, silo);
			set(level, bb, s(Blocks.STRIPPED_SPRUCE_LOG), x, 4, z);
			if (top >= 8) set(level, bb, s(Blocks.STRIPPED_SPRUCE_LOG), x, 8, z);
		}
		fill(level, bb, 18, 1, 5, 18, 2, 5, AIR);
		set(level, bb, s(Blocks.IRON_BARS), 22, 3, 5);
		set(level, bb, s(Blocks.IRON_BARS), 20, 3, 7);
		fill(level, bb, 19, 1, 5, 21, 1, 6, s(Blocks.HAY_BLOCK));
		set(level, bb, s(Blocks.HAY_BLOCK), 20, 2, 6);
		set(level, bb, s(Blocks.COBWEB), 20, 3, 4);
		set(level, bb, s(Blocks.COBWEB), 19, 5, 5);
		// Unmarked graves at the bottom of the field, where the furrows give up their dead.
		for (int x : new int[]{2, 4, 6, 8}) {
			set(level, bb, s(noise(x, 9) < 50 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL), x, 1, 21);
			fill(level, bb, x, 0, 19, x, 0, 20, s(Blocks.COARSE_DIRT));
			set(level, bb, AIR, x, 1, 20);
			set(level, bb, noise(x, 1, 19) < 50 ? s(Blocks.CANDLE) : s(Blocks.DEAD_BUSH), x, 1, 19);
		}
		set(level, bb, s(Blocks.HAY_BLOCK), 12, 1, 19); set(level, bb, s(Blocks.HAY_BLOCK), 12, 1, 20); set(level, bb, s(Blocks.HAY_BLOCK), 13, 1, 20);
		// The farmhouse: two storeys once, its upper walls broken and the front of its roof down to bare rafters.
		fill(level, bb, 14, 0, 14, 21, 0, 21, s(Blocks.OAK_PLANKS));
		for (int x = 14; x <= 21; x++) for (int z = 14; z <= 21; z++) {
			if (x != 14 && x != 21 && z != 14 && z != 21) {
				if (noise(x, 5, z) < 45) set(level, bb, s(Blocks.SPRUCE_SLAB), x, 5, z);
				if (noise(x, 2, z) < 12) set(level, bb, s(Blocks.COBWEB), x, 3, z);
				continue;
			}
			set(level, bb, s(Blocks.COBBLESTONE), x, 1, z);
			for (int y = 2; y <= 4; y++) if (y < 3 || noise(x, y, z) >= 35) set(level, bb, s(noise(x, y + 9, z) < 30 ? Blocks.MOSSY_COBBLESTONE : Blocks.OAK_PLANKS), x, y, z);
			for (int y = 5; y <= 7; y++) if (noise(x, y + 20, z) >= (z == 14 ? 55 : 30)) set(level, bb, s(noise(x, y + 3, z) < 25 ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_PLANKS), x, y, z);
		}
		for (int x : new int[]{14, 21}) for (int z : new int[]{14, 21}) fill(level, bb, x, 1, z, x, 7, z, s(Blocks.OAK_LOG));
		fill(level, bb, 16, 4, 14, 18, 4, 14, s(Blocks.STRIPPED_SPRUCE_LOG));
		// The roof: whole over the back rooms, gaping rafters over the front, the back gable still standing.
		for (int i = 0; i <= 4; i++) {
			int y = 8 + i;
			for (int z = 13; z <= 22; z++) {
				if (z < 17 && noise(i, y, z) < 55 && z != 13) continue;
				set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false), 13 + i, y, z);
				set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 22 - i, y, z);
			}
			if (i < 4) {
				fill(level, bb, 14 + i, y, 21, 21 - i, y, 21, s(Blocks.SPRUCE_PLANKS));
				for (int x = 14 + i; x <= 21 - i; x++) if (noise(x, y, 14) >= 60) set(level, bb, s(Blocks.SPRUCE_PLANKS), x, y, 14);
			}
		}
		set(level, bb, s(Blocks.COBWEB), 17, 9, 18); set(level, bb, s(Blocks.COBWEB), 19, 6, 16);
		// A tall fieldstone chimney up the east side, cold now.
		Palette chimney = Palette.of(Blocks.COBBLESTONE, 4, Blocks.MOSSY_COBBLESTONE, 4, Blocks.CRACKED_STONE_BRICKS, 1);
		fill(level, bb, 22, 0, 17, 22, 11, 18, chimney);
		set(level, bb, s(Blocks.CRACKED_STONE_BRICKS), 22, 12, 17);
		// The porch, sagging: two posts, one snapped, and a holed slab roof.
		fill(level, bb, 15, 1, 13, 15, 3, 13, s(Blocks.STRIPPED_SPRUCE_LOG));
		fill(level, bb, 19, 1, 13, 19, 2, 13, s(Blocks.STRIPPED_SPRUCE_LOG));
		for (int x = 14; x <= 20; x++) for (int z = 12; z <= 13; z++) if (noise(x, 4, z) >= 35) set(level, bb, s(Blocks.SPRUCE_SLAB), x, 4, z);
		set(level, bb, s(Blocks.SPRUCE_SLAB), 20, 1, 12);
		fill(level, bb, 17, 1, 14, 17, 3, 14, AIR);
		chest(level, bb, 20, 1, 20, FarmSites.SCARECROW_HOUSE, Direction.WEST, 1);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 15, 1, 20);
		set(level, bb, s(Blocks.COMPOSTER), 15, 1, 15);
		set(level, bb, s(Blocks.COBWEB), 20, 2, 15);
		set(level, bb, s(Blocks.HAY_BLOCK), 20, 1, 15);
		sign(level, bb, standingSign(), 16, 1, 12, "farm_scarecrow", 2);
	}
}
