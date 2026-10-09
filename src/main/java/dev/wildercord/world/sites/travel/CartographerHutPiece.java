package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A surveyor's hut with a flag on a tall pole. Maps and a compass hang on the walls, the survey notes lie on a
 * lectern, and the desk chest keeps a map that points to another roadside site.
 */
public final class CartographerHutPiece extends TravelPiece {
	public CartographerHutPiece(int x, int y, int z, Direction facing) { super(TravelSites.CARTOGRAPHER_PIECE, x, y, z, 11, 11, 11, facing); }
	public CartographerHutPiece(CompoundTag tag) { super(TravelSites.CARTOGRAPHER_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState log = b(Blocks.STRIPPED_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		footing(level, bb, 0, 0, 10, 10, GRASS, DIRT, 10);
		footing(level, bb, 1, 1, 9, 9, COBBLE, COBBLE, 0);
		for (int x = 4; x <= 6; x++) set(level, bb, b(Blocks.DIRT_PATH), x, 0, 0);
		room(level, bb, 1, 0, 1, 9, 4, 9, b(Blocks.BIRCH_PLANKS));
		fill(level, bb, 2, 0, 2, 8, 0, 8, b(Blocks.OAK_PLANKS));
		for (int[] c : new int[][]{{1, 1}, {9, 1}, {1, 9}, {9, 9}}) fill(level, bb, c[0], 1, c[1], c[0], 3, c[1], log);
		gable(level, bb, 1, 9, 1, 9, 4, Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_PLANKS), b(Blocks.BIRCH_PLANKS));
		door(level, bb, Blocks.BIRCH_DOOR, 5, 1, 1, Direction.NORTH);
		for (int z : new int[]{4, 6}) set(level, bb, b(Blocks.GLASS_PANE), 9, 2, z);
		fill(level, bb, 3, 2, 1, 3, 2, 1, b(Blocks.GLASS_PANE)); fill(level, bb, 7, 2, 1, 7, 2, 1, b(Blocks.GLASS_PANE));
		// The work: a cartography table, the desk chest, the survey notes, a lantern.
		set(level, bb, b(Blocks.CARTOGRAPHY_TABLE), 2, 1, 8);
		set(level, bb, b(Blocks.BOOKSHELF), 8, 1, 8);
		chest(level, bb, 5, 1, 8, TravelSites.loot(TravelSites.CARTOGRAPHER), Direction.SOUTH, 8);
		lectern(level, bb, 7, 1, 5, Direction.WEST, TravelSites.book("travel_survey", "Survey Notes", "The Surveyor"));
		set(level, bb, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 5, 3, 5);
		// Maps and a compass on the walls.
		frame(level, bb, 2, 2, 3, Direction.EAST, new ItemStack(Items.MAP));
		frame(level, bb, 2, 2, 5, Direction.EAST, new ItemStack(Items.COMPASS));
		frame(level, bb, 2, 3, 4, Direction.EAST, new ItemStack(Items.MAP));
		frame(level, bb, 4, 3, 8, Direction.SOUTH, new ItemStack(Items.MAP));
		frame(level, bb, 6, 3, 8, Direction.SOUTH, new ItemStack(Items.PAPER));
		// The flag pole at the front corner, seen over the treetops.
		footing(level, bb, 0, 0, 0, 0, COBBLE, COBBLE, 0);
		fill(level, bb, 0, 1, 0, 0, 10, 0, b(Blocks.OAK_FENCE));
		fill(level, bb, 1, 8, 0, 2, 9, 0, b(Blocks.WOOL.pick(DyeColor.LIGHT_BLUE)));
	}
}
