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
		clearCanopy(level, bb);
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		// The terraces: a worn stone lip at the front of each, grass on top, carved piers at the ends.
		Palette lip = Palette.of(Blocks.MOSSY_STONE_BRICKS, 5, Blocks.STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 1);
		var chiseled = s(Blocks.CHISELED_STONE_BRICKS);
		var lantern = s(Blocks.LANTERN);
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
		for (int x : new int[]{0, 18}) {
			fill(level, bb, x, 1, 6, x, 2, 6, chiseled); set(level, bb, lantern, x, 3, 6);
			fill(level, bb, x, 2, 12, x, 3, 12, chiseled); set(level, bb, lantern, x, 4, 12);
		}
		// Lantern posts on the lips either side of each flight of steps.
		for (int x : new int[]{8, 10}) {
			set(level, bb, s(Blocks.STONE_BRICK_WALL), x, 2, 6); set(level, bb, lantern, x, 3, 6);
			set(level, bb, s(Blocks.STONE_BRICK_WALL), x, 3, 12); set(level, bb, lantern, x, 4, 12);
		}
		// A flowering hedge along the front, broken by a timber gateway over the path.
		for (int x = 0; x <= 18; x++) {
			if (x >= 6 && x <= 12) continue;
			set(level, bb, leaves(noise(x, 0) < 45 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES), x, 1, 0);
		}
		var post = s(Blocks.STRIPPED_OAK_LOG);
		for (int x : new int[]{7, 11}) { set(level, bb, s(Blocks.STONE_BRICKS), x, 1, 0); fill(level, bb, x, 2, 0, x, 4, 0, post); }
		fill(level, bb, 7, 5, 0, 11, 5, 0, s(Blocks.OAK_PLANKS));
		set(level, bb, stair(Blocks.OAK_STAIRS, Direction.EAST, true), 6, 5, 0); set(level, bb, stair(Blocks.OAK_STAIRS, Direction.WEST, true), 12, 5, 0);
		fill(level, bb, 6, 6, 0, 12, 6, 0, s(Blocks.SPRUCE_SLAB));
		set(level, bb, s(Blocks.HONEYCOMB_BLOCK), 9, 6, 0);
		hangingLantern(level, bb, 9, 4, 0, 0, false);
		// A stone drinking basin for the bees by the lower hives.
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3), 16, 1, 3);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3), 2, 1, 3);
		// Two rows of hives, two bees in each, every hive under a little slab hood.
		for (int x : new int[]{3, 6, 12, 15}) {
			hive(level, bb, Blocks.BEEHIVE, x, 2, 8, Direction.SOUTH, 2);
			hive(level, bb, Blocks.BEEHIVE, x, 3, 13, Direction.SOUTH, 2);
			set(level, bb, s(Blocks.SPRUCE_SLAB), x, 3, 8);
			set(level, bb, s(Blocks.SPRUCE_SLAB), x, 4, 13);
		}
		// The keeper's shed: a timber frame on a stone footing, plank walls with windows, a steep gable roof.
		for (int x : new int[]{5, 13}) for (int z : new int[]{15, 18}) fill(level, bb, x, 3, z, x, 5, z, post);
		fill(level, bb, 5, 2, 18, 13, 2, 18, s(Blocks.STONE_BRICKS));
		fill(level, bb, 6, 3, 18, 12, 5, 18, s(Blocks.OAK_PLANKS));
		set(level, bb, s(Blocks.GLASS), 7, 4, 18); set(level, bb, s(Blocks.GLASS), 11, 4, 18);
		for (int x : new int[]{5, 13}) {
			fill(level, bb, x, 3, 16, x, 5, 17, s(Blocks.OAK_PLANKS));
			set(level, bb, s(Blocks.GLASS), x, 4, 16);
		}
		set(level, bb, s(Blocks.POTTED_RED_TULIP), 4, 3, 16); set(level, bb, s(Blocks.POTTED_RED_TULIP), 14, 3, 16);
		for (int x = 6; x <= 12; x++) set(level, bb, stair(Blocks.OAK_STAIRS, Direction.NORTH, true), x, 5, 15);
		Block roof = Blocks.SPRUCE_STAIRS;
		for (int x = 4; x <= 14; x++) {
			set(level, bb, stair(roof, Direction.NORTH, false), x, 6, 14);
			set(level, bb, stair(roof, Direction.NORTH, false), x, 7, 15);
			set(level, bb, s(Blocks.SPRUCE_PLANKS), x, 7, 16);
			set(level, bb, s(Blocks.SPRUCE_SLAB), x, 8, 16);
			set(level, bb, stair(roof, Direction.SOUTH, false), x, 7, 17);
			set(level, bb, stair(roof, Direction.SOUTH, false), x, 6, 18);
		}
		for (int x : new int[]{5, 13}) fill(level, bb, x, 6, 15, x, 6, 17, s(Blocks.OAK_PLANKS));
		hangingLantern(level, bb, 7, 6, 16, 0, false); hangingLantern(level, bb, 11, 6, 16, 0, false);
		hangingLantern(level, bb, 5, 5, 14, 0, false); hangingLantern(level, bb, 13, 5, 14, 0, false);
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
