package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A mossy walled orchard of oak, cherry and azalea round an overgrown rune garden. The garden's keeper buried a cache
 * under a mound of roots beside the old pedestal; the sign says where to dig. A cider-press house with a smoking chimney
 * stands in the back corner, and the wild wood is cleared back from the wall so the orchard shows from afar.
 */
public final class OrchardPiece extends FarmPiece {
	public OrchardPiece(int x, int z, Direction facing) { super(FarmSites.ORCHARD, x, 0, z, 25, 13, 25, facing); }
	public OrchardPiece(CompoundTag tag) { super(FarmSites.ORCHARD, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		clearCanopy(level, bb);
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		// The wall: two high, crumbled in places, coped where it still stands, a gate in the front.
		for (int x = 0; x <= 24; x++) for (int z = 0; z <= 24; z++) {
			if (x != 0 && x != 24 && z != 0 && z != 24) continue;
			if (z == 0 && x >= 11 && x <= 13) continue;
			int n = noise(x, z);
			if (n >= 8) set(level, bb, s(n < 40 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE), x, 1, z);
			if (n >= 25) set(level, bb, s(n < 60 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE), x, 2, z);
			if (n >= 45) set(level, bb, s(n < 70 ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.COBBLESTONE_SLAB), x, 3, z);
		}
		// Stone piers along the wall, lanterns on the corners.
		Palette pier = Palette.of(Blocks.MOSSY_STONE_BRICKS, 5, Blocks.STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 2);
		var chiseled = s(Blocks.CHISELED_STONE_BRICKS);
		var lantern = s(Blocks.LANTERN);
		for (int[] c : new int[][]{{0, 0}, {24, 0}, {0, 24}, {24, 24}}) {
			fill(level, bb, c[0], 1, c[1], c[0], 3, c[1], pier);
			set(level, bb, chiseled, c[0], 4, c[1]);
			set(level, bb, lantern, c[0], 5, c[1]);
		}
		for (int[] p : new int[][]{{6, 0}, {18, 0}, {6, 24}, {12, 24}, {18, 24}, {0, 6}, {0, 12}, {0, 18}, {24, 6}, {24, 12}, {24, 18}}) {
			fill(level, bb, p[0], 1, p[1], p[0], 3, p[1], pier);
			set(level, bb, s(Blocks.MOSSY_STONE_BRICK_SLAB), p[0], 4, p[1]);
		}
		// The gatehouse: two piers under a stone lintel and a little pitched roof, a lantern in the arch.
		for (int x : new int[]{10, 14}) {
			fill(level, bb, x, 1, 0, x, 5, 1, pier);
			set(level, bb, chiseled, x, 3, 0);
		}
		for (int z = 0; z <= 1; z++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 11, 5, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 13, 5, z);
		}
		fill(level, bb, 9, 6, 0, 15, 6, 1, s(Blocks.STONE_BRICKS));
		for (int x = 9; x <= 15; x++) {
			set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.NORTH, false), x, 7, 0);
			set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.SOUTH, false), x, 7, 1);
		}
		fill(level, bb, 11, 8, 0, 13, 8, 1, s(Blocks.MOSSY_STONE_BRICKS));
		for (int z = 0; z <= 1; z++) {
			set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.EAST, false), 10, 8, z);
			set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.WEST, false), 14, 8, z);
		}
		set(level, bb, chiseled, 12, 9, 0);
		set(level, bb, lantern, 12, 10, 0);
		hangingLantern(level, bb, 12, 5, 0, 0, false);
		Block banner = Blocks.WALL_BANNER.pick(DyeColor.GREEN);
		banner(level, bb, banner, 10, 4, 2, Direction.NORTH);
		banner(level, bb, banner, 14, 4, 2, Direction.NORTH);
		// The path in, flagged at its edges and lit by posts.
		fill(level, bb, 12, 0, 0, 12, 0, 8, s(Blocks.DIRT_PATH));
		for (int z = 1; z <= 8; z++) for (int x : new int[]{11, 13}) set(level, bb, s(noise(x, z) < 50 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS), x, 0, z);
		// The trees, kept in leaf.
		for (int x : new int[]{4, 8, 16, 20}) for (int z : new int[]{4, 8, 16, 20}) {
			if (x == 8 && z == 4 || x == 16 && z == 4 || x >= 16 && z >= 16 && !(x == 16 && z == 16)) continue;
			int kind = noise(x * 3, z * 5) % 3;
			BlockState log = s(kind == 1 ? Blocks.CHERRY_LOG : Blocks.OAK_LOG);
			BlockState leaf = leaves(kind == 0 ? Blocks.OAK_LEAVES : kind == 1 ? Blocks.CHERRY_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES);
			tree(level, bb, x, 1, z, 4, log, leaf);
		}
		for (int x = 1; x <= 23; x++) for (int z = 1; z <= 23; z++) {
			if (x >= 9 && x <= 15 && z >= 9 && z <= 15 || x >= 11 && x <= 13 && z < 9 || x >= 16 && z >= 17) continue;
			int n = noise(x + 7, z);
			if (n < 10) set(level, bb, s(Blocks.SHORT_GRASS), x, 1, z);
			else if (n < 16) set(level, bb, s(Blocks.LEAF_LITTER), x, 1, z);
		}
		for (int z : new int[]{4, 7}) for (int x : new int[]{10, 14}) {
			set(level, bb, s(Blocks.MOSSY_STONE_BRICK_WALL), x, 1, z);
			set(level, bb, lantern, x, 2, z);
		}
		// The rune garden: a low ring of old brick, moss and petals, a pedestal, and the cache under the roots.
		fill(level, bb, 9, 0, 9, 15, 0, 15, s(Blocks.MOSS_BLOCK));
		walls(level, bb, 9, 1, 9, 15, 1, 15, s(Blocks.MOSSY_STONE_BRICKS));
		walls(level, bb, 9, 2, 9, 15, 2, 15, s(Blocks.MOSSY_STONE_BRICK_SLAB));
		set(level, bb, AIR, 12, 1, 9);
		set(level, bb, AIR, 12, 2, 9);
		for (int x = 10; x <= 14; x++) for (int z = 10; z <= 14; z++) {
			int n = noise(x, 3, z);
			if (n < 25) set(level, bb, s(Blocks.PINK_PETALS), x, 1, z);
			else if (n < 40) set(level, bb, s(Blocks.FERN), x, 1, z);
			else if (n < 50) set(level, bb, s(Blocks.FLOWERING_AZALEA), x, 1, z);
		}
		// A pergola over the ring: stone posts at the corners, timber beams draped in blossom, a lantern over the pedestal.
		var beam = s(Blocks.STRIPPED_OAK_LOG);
		for (int x : new int[]{9, 15}) for (int z : new int[]{9, 15}) {
			fill(level, bb, x, 2, z, x, 4, z, s(Blocks.STONE_BRICKS));
			set(level, bb, chiseled, x, 5, z);
		}
		for (int x : new int[]{11, 13}) set(level, bb, s(Blocks.MOSSY_STONE_BRICK_WALL), x, 2, 9);
		fill(level, bb, 10, 5, 9, 14, 5, 9, beam); fill(level, bb, 10, 5, 15, 14, 5, 15, beam);
		fill(level, bb, 9, 5, 10, 9, 5, 14, beam); fill(level, bb, 15, 5, 10, 15, 5, 14, beam);
		fill(level, bb, 10, 5, 12, 14, 5, 12, beam);
		for (int x = 9; x <= 15; x++) for (int z = 9; z <= 15; z++) {
			boolean edge = x == 9 || x == 15 || z == 9 || z == 15;
			if (edge && noise(x, 6, z) < 45) set(level, bb, leaves(noise(z, 6, x) < 50 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.CHERRY_LEAVES), x, 6, z);
		}
		hangingLantern(level, bb, 12, 4, 12, 0, false);
		hangingLantern(level, bb, 12, 4, 9, 0, false);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 12, 1, 12);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 12, 2, 12);
		chest(level, bb, 12, 0, 14, FarmSites.ORCHARD_CACHE, Direction.SOUTH, 2);
		set(level, bb, s(Blocks.ROOTED_DIRT), 12, 1, 14);
		set(level, bb, s(Blocks.MOSS_CARPET), 11, 1, 14);
		set(level, bb, s(Blocks.MOSS_CARPET), 13, 1, 14);
		sign(level, bb, standingSign(), 12, 1, 10, "farm_orchard", 3);
		// The cider-press house in the back corner: a cobbled sill, timber walls under a steep spruce gable, a smoking chimney.
		fill(level, bb, 17, 0, 18, 23, 0, 23, s(Blocks.SPRUCE_PLANKS));
		walls(level, bb, 17, 1, 18, 23, 1, 23, s(Blocks.MOSSY_COBBLESTONE));
		walls(level, bb, 17, 2, 18, 23, 4, 23, s(Blocks.OAK_PLANKS));
		for (int x : new int[]{17, 23}) for (int z : new int[]{18, 23}) fill(level, bb, x, 1, z, x, 4, z, s(Blocks.STRIPPED_SPRUCE_LOG));
		gableRoof(level, bb, 17, 18, 23, 23, 5, false, Blocks.SPRUCE_STAIRS, s(Blocks.SPRUCE_PLANKS), Palette.of(Blocks.OAK_PLANKS, 3, Blocks.STRIPPED_OAK_LOG, 1));
		set(level, bb, s(Blocks.GLASS_PANE), 20, 6, 18);
		set(level, bb, s(Blocks.GLASS_PANE), 20, 6, 23);
		for (int y = 2; y <= 3; y++) {
			for (int z = 20; z <= 21; z++) { set(level, bb, s(Blocks.GLASS_PANE), 17, y, z); set(level, bb, s(Blocks.GLASS_PANE), 23, y, z); }
			set(level, bb, s(Blocks.GLASS_PANE), 18, y, 18);
			set(level, bb, s(Blocks.GLASS_PANE), 22, y, 18);
		}
		door(level, bb, Blocks.SPRUCE_DOOR, 20, 1, 18, Direction.SOUTH, false);
		for (int x : new int[]{19, 21}) set(level, bb, s(Blocks.OAK_FENCE), x, 1, 17);
		for (int x : new int[]{19, 21}) set(level, bb, lantern, x, 2, 17);
		set(level, bb, s(Blocks.FLOWER_POT), 18, 1, 17);
		set(level, bb, s(Blocks.POTTED_RED_TULIP), 22, 1, 17);
		fill(level, bb, 22, 2, 22, 22, 10, 22, s(Blocks.COBBLESTONE));
		set(level, bb, s(Blocks.SMOKER).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 22, 1, 22);
		set(level, bb, s(Blocks.CAMPFIRE), 22, 11, 22);
		// Inside: the press (a cauldron under a piston), casks of cider and baskets of windfalls.
		set(level, bb, s(Blocks.CAULDRON), 18, 1, 22);
		set(level, bb, s(Blocks.PISTON).setValue(BlockStateProperties.FACING, Direction.DOWN), 18, 3, 22);
		set(level, bb, s(Blocks.OAK_FENCE), 18, 2, 22);
		for (int x = 19; x <= 21; x++) set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.NORTH), x, 1, 22);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.NORTH), 20, 2, 22);
		set(level, bb, s(Blocks.COMPOSTER), 22, 1, 19);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 18, 1, 19);
		hangingLantern(level, bb, 20, 8, 20, 2, false);
		// The fruit stand by the gate.
		fill(level, bb, 15, 1, 2, 15, 2, 2, s(Blocks.OAK_FENCE));
		fill(level, bb, 18, 1, 2, 18, 2, 2, s(Blocks.OAK_FENCE));
		fill(level, bb, 15, 3, 2, 18, 3, 4, s(Blocks.SPRUCE_SLAB));
		chest(level, bb, 16, 1, 3, FarmSites.ORCHARD_STAND, Direction.SOUTH, 1);
		set(level, bb, s(Blocks.COMPOSTER), 17, 1, 3);
		set(level, bb, s(Blocks.PUMPKIN), 16, 1, 4);
		set(level, bb, s(Blocks.MELON), 17, 1, 4);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 18, 1, 4);
		// A striped awning over the stand, a lit post and the overflow stacked beside it.
		for (int x = 15; x <= 18; x++) set(level, bb, s(x % 2 == 0 ? Blocks.WOOL.pick(DyeColor.RED) : Blocks.WOOL.white()), x, 3, 1);
		set(level, bb, s(Blocks.OAK_FENCE), 19, 1, 2); set(level, bb, s(Blocks.LANTERN), 19, 2, 2);
		set(level, bb, s(Blocks.HAY_BLOCK), 19, 1, 4); set(level, bb, s(Blocks.PUMPKIN), 19, 1, 3);
		set(level, bb, s(Blocks.MELON), 14, 1, 3);
	}
}
