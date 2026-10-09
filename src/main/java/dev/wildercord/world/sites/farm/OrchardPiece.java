package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A mossy walled orchard of oak, cherry and azalea round an overgrown rune garden. The garden's keeper buried a cache
 * under a mound of roots beside the old pedestal; the sign says where to dig.
 */
public final class OrchardPiece extends FarmPiece {
	public OrchardPiece(int x, int z, Direction facing) { super(FarmSites.ORCHARD, x, 0, z, 25, 9, 25, facing); }
	public OrchardPiece(CompoundTag tag) { super(FarmSites.ORCHARD, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		// The wall: two high, crumbled in places, a gate in the front.
		for (int x = 0; x <= 24; x++) for (int z = 0; z <= 24; z++) {
			if (x != 0 && x != 24 && z != 0 && z != 24) continue;
			if (z == 0 && x >= 11 && x <= 13) continue;
			int n = noise(x, z);
			if (n >= 8) set(level, bb, s(n < 40 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE), x, 1, z);
			if (n >= 25) set(level, bb, s(n < 60 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE), x, 2, z);
		}
		fill(level, bb, 12, 0, 0, 12, 0, 8, s(Blocks.DIRT_PATH));
		// The trees, kept in leaf.
		for (int x : new int[]{4, 8, 16, 20}) for (int z : new int[]{4, 8, 16, 20}) {
			if (x == 8 && z == 4 || x == 16 && z == 4) continue;
			int kind = noise(x * 3, z * 5) % 3;
			BlockState log = s(kind == 1 ? Blocks.CHERRY_LOG : Blocks.OAK_LOG);
			BlockState leaf = leaves(kind == 0 ? Blocks.OAK_LEAVES : kind == 1 ? Blocks.CHERRY_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES);
			tree(level, bb, x, 1, z, 4, log, leaf);
		}
		for (int x = 1; x <= 23; x++) for (int z = 1; z <= 23; z++) {
			if (x >= 9 && x <= 15 && z >= 9 && z <= 15 || x == 12 && z < 9) continue;
			int n = noise(x + 7, z);
			if (n < 10) set(level, bb, s(Blocks.SHORT_GRASS), x, 1, z);
			else if (n < 16) set(level, bb, s(Blocks.LEAF_LITTER), x, 1, z);
		}
		// The rune garden: a low ring of old brick, moss and petals, a pedestal, and the cache under the roots.
		fill(level, bb, 9, 0, 9, 15, 0, 15, s(Blocks.MOSS_BLOCK));
		walls(level, bb, 9, 1, 9, 15, 1, 15, s(Blocks.MOSSY_STONE_BRICKS));
		set(level, bb, AIR, 12, 1, 9);
		for (int x = 10; x <= 14; x++) for (int z = 10; z <= 14; z++) {
			int n = noise(x, 3, z);
			if (n < 25) set(level, bb, s(Blocks.PINK_PETALS), x, 1, z);
			else if (n < 40) set(level, bb, s(Blocks.FERN), x, 1, z);
			else if (n < 50) set(level, bb, s(Blocks.FLOWERING_AZALEA), x, 1, z);
		}
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 12, 1, 12);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 12, 2, 12);
		chest(level, bb, 12, 0, 14, FarmSites.ORCHARD_CACHE, Direction.SOUTH, 2);
		set(level, bb, s(Blocks.ROOTED_DIRT), 12, 1, 14);
		set(level, bb, s(Blocks.MOSS_CARPET), 11, 1, 14);
		set(level, bb, s(Blocks.MOSS_CARPET), 13, 1, 14);
		sign(level, bb, standingSign(), 12, 1, 10, "farm_orchard", 3);
		// The fruit stand by the gate.
		fill(level, bb, 15, 1, 2, 15, 2, 2, s(Blocks.OAK_FENCE));
		fill(level, bb, 18, 1, 2, 18, 2, 2, s(Blocks.OAK_FENCE));
		fill(level, bb, 15, 3, 2, 18, 3, 4, s(Blocks.SPRUCE_SLAB));
		chest(level, bb, 16, 1, 3, FarmSites.ORCHARD_STAND, Direction.SOUTH, 1);
		set(level, bb, s(Blocks.COMPOSTER), 17, 1, 3);
		set(level, bb, s(Blocks.PUMPKIN), 16, 1, 4);
		set(level, bb, s(Blocks.MELON), 17, 1, 4);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 18, 1, 4);
	}
}
