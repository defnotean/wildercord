package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** A shepherd's stone hut on the hillside beside a fenced pen of sheep, with a loom, a smoker and a gate to shut. */
public final class ShepherdPiece extends FarmPiece {
	public ShepherdPiece(int x, int z, Direction facing) { super(FarmSites.SHEPHERD, x, 0, z, 21, 10, 21, facing); }
	public ShepherdPiece(CompoundTag tag) { super(FarmSites.SHEPHERD, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		fill(level, bb, 5, 0, 0, 5, 0, 11, s(Blocks.DIRT_PATH));
		fill(level, bb, 6, 0, 3, 14, 0, 3, s(Blocks.DIRT_PATH));
		// The hut: a stone course, spruce above, a gable roof running across.
		fill(level, bb, 2, 0, 12, 8, 0, 18, s(Blocks.COBBLESTONE));
		walls(level, bb, 2, 1, 12, 8, 1, 18, s(Blocks.COBBLESTONE));
		walls(level, bb, 2, 2, 12, 8, 3, 18, s(Blocks.SPRUCE_PLANKS));
		for (int x : new int[]{2, 8}) for (int z : new int[]{12, 18}) fill(level, bb, x, 1, z, x, 3, z, s(Blocks.SPRUCE_LOG));
		for (int i = 0; i < 4; i++) {
			int y = 4 + i;
			fill(level, bb, 1, y, 11 + i, 9, y, 11 + i, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
			fill(level, bb, 1, y, 19 - i, 9, y, 19 - i, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
			fill(level, bb, 2, y, 12 + i, 2, y, 18 - i, s(Blocks.SPRUCE_PLANKS));
			fill(level, bb, 8, y, 12 + i, 8, y, 18 - i, s(Blocks.SPRUCE_PLANKS));
		}
		fill(level, bb, 1, 7, 15, 9, 7, 15, s(Blocks.SPRUCE_PLANKS));
		fill(level, bb, 1, 8, 15, 9, 8, 15, s(Blocks.SPRUCE_SLAB));
		set(level, bb, s(Blocks.COBBLESTONE_WALL), 7, 8, 15);
		set(level, bb, s(Blocks.CAMPFIRE), 7, 9, 15);
		set(level, bb, s(Blocks.GLASS), 2, 2, 15);
		set(level, bb, s(Blocks.GLASS), 8, 2, 15);
		set(level, bb, s(Blocks.GLASS), 5, 2, 18);
		door(level, bb, Blocks.SPRUCE_DOOR, 5, 1, 12, Direction.NORTH, true);
		set(level, bb, facing(Blocks.LOOM, Direction.EAST), 3, 1, 14);
		set(level, bb, facing(Blocks.SMOKER, Direction.EAST), 3, 1, 16);
		chest(level, bb, 7, 1, 17, FarmSites.SHEPHERD_HUT, Direction.WEST, 1);
		set(level, bb, s(Blocks.HAY_BLOCK), 7, 1, 13);
		set(level, bb, s(Blocks.WOOL.white()), 3, 1, 17);
		set(level, bb, s(Blocks.CARPET.white()), 5, 1, 16);
		set(level, bb, hangingLantern(), 5, 6, 15);
		// The pen, its gate shut, with hay, a trough and the flock.
		walls(level, bb, 10, 1, 4, 19, 1, 18, s(Blocks.OAK_FENCE));
		set(level, bb, facing(Blocks.OAK_FENCE_GATE, Direction.SOUTH), 14, 1, 4);
		set(level, bb, s(Blocks.HAY_BLOCK), 18, 1, 17);
		set(level, bb, s(Blocks.HAY_BLOCK), 17, 1, 17);
		set(level, bb, s(Blocks.HAY_BLOCK), 18, 2, 17);
		fill(level, bb, 11, 0, 15, 11, 0, 17, s(Blocks.WATER));
		for (int x = 12; x <= 18; x++) for (int z = 5; z <= 17; z++) if (noise(x, z) < 15) set(level, bb, s(Blocks.SHORT_GRASS), x, 1, z);
		sentry(level, bb, EntityTypes.SHEEP, 13, 1, 9);
		sentry(level, bb, EntityTypes.SHEEP, 16, 1, 12);
		sentry(level, bb, EntityTypes.SHEEP, 14, 1, 14);
	}
}
