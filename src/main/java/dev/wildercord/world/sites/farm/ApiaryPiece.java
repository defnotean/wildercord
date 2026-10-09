package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Three flowering terraces of beehives climbing to the keeper's shed, where a smoking campfire calms the hive hung over
 * it. A second fire on the roof sends up the smoke column that is the first thing a traveller sees.
 */
public final class ApiaryPiece extends FarmPiece {
	private static final Block[] FLOWERS = {Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.DANDELION, Blocks.ALLIUM, Blocks.POPPY, Blocks.AZURE_BLUET};

	public ApiaryPiece(int x, int z, Direction facing) { super(FarmSites.APIARY, x, 0, z, 19, 10, 19, facing); }
	public ApiaryPiece(CompoundTag tag) { super(FarmSites.APIARY, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		// The terraces: a stone lip at the front of each, grass on top.
		var lip = s(Blocks.MOSSY_STONE_BRICKS);
		fill(level, bb, 0, 0, 6, 18, 0, 18, s(Blocks.DIRT));
		fill(level, bb, 0, 1, 6, 18, 1, 18, s(Blocks.GRASS_BLOCK));
		fill(level, bb, 0, 1, 12, 18, 1, 18, s(Blocks.DIRT));
		fill(level, bb, 0, 2, 12, 18, 2, 18, s(Blocks.GRASS_BLOCK));
		fill(level, bb, 0, 1, 6, 18, 1, 6, lip);
		fill(level, bb, 0, 2, 12, 18, 2, 12, lip);
		fill(level, bb, 9, 0, 0, 9, 0, 5, s(Blocks.DIRT_PATH));
		set(level, bb, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), 9, 1, 6);
		fill(level, bb, 9, 1, 7, 9, 1, 11, s(Blocks.DIRT_PATH));
		set(level, bb, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), 9, 2, 12);
		fill(level, bb, 9, 2, 13, 9, 2, 14, s(Blocks.DIRT_PATH));
		// Flowers everywhere the bees can reach.
		for (int x = 0; x <= 18; x++) for (int z = 0; z <= 18; z++) {
			if (x == 9 || z == 6 || z == 12 || z >= 14 && x >= 5 && x <= 13) continue;
			int y = z < 6 ? 1 : z < 12 ? 2 : 3;
			int n = noise(x, z);
			if (n < 35) set(level, bb, s(FLOWERS[n % FLOWERS.length]), x, y, z);
			else if (n < 50) set(level, bb, s(Blocks.WILDFLOWERS), x, y, z);
		}
		// Two rows of hives, two bees in each.
		for (int x : new int[]{3, 6, 12, 15}) {
			hive(level, bb, Blocks.BEEHIVE, x, 2, 8, Direction.SOUTH, 2);
			hive(level, bb, Blocks.BEEHIVE, x, 3, 13, Direction.SOUTH, 2);
		}
		// The keeper's shed: a signal campfire on hay sends smoke up past the hive hung over it.
		var post = s(Blocks.STRIPPED_OAK_LOG);
		for (int x : new int[]{5, 13}) for (int z : new int[]{15, 18}) fill(level, bb, x, 3, z, x, 5, z, post);
		fill(level, bb, 6, 3, 18, 12, 5, 18, s(Blocks.OAK_PLANKS));
		fill(level, bb, 4, 6, 14, 14, 6, 18, s(Blocks.OAK_SLAB));
		fill(level, bb, 5, 2, 15, 13, 2, 17, s(Blocks.OAK_PLANKS));
		set(level, bb, s(Blocks.HAY_BLOCK), 9, 2, 17);
		set(level, bb, s(Blocks.CAMPFIRE).setValue(BlockStateProperties.SIGNAL_FIRE, true), 9, 3, 17);
		hive(level, bb, Blocks.BEEHIVE, 9, 5, 17, Direction.SOUTH, 3);
		set(level, bb, s(Blocks.HAY_BLOCK), 13, 6, 18);
		set(level, bb, s(Blocks.CAMPFIRE).setValue(BlockStateProperties.SIGNAL_FIRE, true), 13, 7, 18);
		chest(level, bb, 6, 3, 17, FarmSites.APIARY_SHED, Direction.SOUTH, 1);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 12, 3, 17);
		set(level, bb, s(Blocks.HONEY_BLOCK), 12, 3, 16);
		set(level, bb, s(Blocks.COMPOSTER), 6, 3, 15);
		sign(level, bb, standingSign(), 11, 3, 15, "farm_apiary", 3);
	}
}
