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
import net.minecraft.world.level.block.CandleBlock;
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
		BlockState beamX = b(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		BlockState beamZ = b(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		BlockState pane = b(Blocks.GLASS_PANE);
		Palette plinth = Palette.of(Blocks.COBBLESTONE, 5, Blocks.MOSSY_COBBLESTONE, 2, Blocks.STONE_BRICKS, 2);
		Palette boards = Palette.of(Blocks.BIRCH_PLANKS, 1);
		footing(level, bb, 0, 0, 10, 10, GRASS, DIRT, 10);
		footing(level, bb, 1, 1, 9, 9, COBBLE, COBBLE, 0);
		for (int x = 4; x <= 6; x++) set(level, bb, b(Blocks.DIRT_PATH), x, 0, 0);
		// A fieldstone plinth under pale birch boards, framed in oak with a dark spruce wall plate.
		room(level, bb, 1, 0, 1, 9, 4, 9, b(Blocks.BIRCH_PLANKS));
		walls(level, bb, 1, 0, 1, 9, 1, 9, plinth);
		fill(level, bb, 2, 0, 2, 8, 0, 8, b(Blocks.OAK_PLANKS));
		for (int[] c : new int[][]{{1, 1}, {9, 1}, {1, 9}, {9, 9}, {1, 5}, {9, 5}}) fill(level, bb, c[0], 1, c[1], c[0], 3, c[1], log);
		fill(level, bb, 1, 4, 1, 9, 4, 1, beamX); fill(level, bb, 1, 4, 9, 9, 4, 9, beamX);
		fill(level, bb, 1, 4, 2, 1, 4, 8, beamZ); fill(level, bb, 9, 4, 2, 9, 4, 8, beamZ);
		// A steep spruce roof with birch gables, a round of glass in each.
		gableRoof(level, bb, 1, 1, 9, 9, 5, true, Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_PLANKS), boards);
		for (int x : new int[]{1, 9}) { set(level, bb, pane, x, 6, 5); set(level, bb, log, x, 5, 5); }
		// The door between two sconces, the windows with oak sills and a pot of fern on each.
		door(level, bb, Blocks.BIRCH_DOOR, 5, 1, 1, Direction.NORTH);
		for (int x : new int[]{4, 6}) sconce(level, bb, x, 3, 0, Direction.SOUTH, false);
		for (int x : new int[]{3, 7}) {
			fill(level, bb, x, 2, 1, x, 3, 1, pane);
			set(level, bb, stair(Blocks.OAK_STAIRS, Direction.NORTH, true), x, 1, 0);
			set(level, bb, b(Blocks.POTTED_FERN), x, 2, 0);
		}
		for (int z : new int[]{4, 6}) {
			fill(level, bb, 9, 2, z, 9, 3, z, pane);
			set(level, bb, stair(Blocks.OAK_STAIRS, Direction.WEST, true), 10, 1, z);
		}
		// A lamp post at the path and a crate of rolled charts by the east wall.
		fill(level, bb, 10, 1, 0, 10, 2, 0, b(Blocks.OAK_FENCE));
		set(level, bb, b(Blocks.LANTERN), 10, 3, 0);
		set(level, bb, b(Blocks.BARREL), 10, 1, 8); set(level, bb, b(Blocks.BARREL), 10, 1, 9);
		// Inside: a beam across the ceiling, a blue rug, a second shelf and a candle at the drawing table.
		fill(level, bb, 2, 4, 5, 8, 4, 5, beamX);
		fill(level, bb, 3, 1, 3, 6, 1, 6, b(Blocks.CARPET.pick(DyeColor.LIGHT_BLUE)));
		set(level, bb, b(Blocks.BOOKSHELF), 8, 2, 8);
		set(level, bb, b(Blocks.POTTED_FERN), 8, 3, 8);
		set(level, bb, b(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 2, 2, 8);
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
