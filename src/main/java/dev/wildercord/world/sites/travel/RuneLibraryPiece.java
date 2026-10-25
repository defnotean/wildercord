package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
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

import java.util.List;

/**
 * A rune library: a stone hall under a steep slate roof whose gable fronts the road over a dark portal. Two shelf
 * walls, one along a red carpet and one along a blue one.
 * The riddle on the front lectern names a carpet and a shelf column; the lowest book there hides a reading nook
 * with the library's chest. The second lectern teaches the Rune Catalog's Travel and Exploring filters.
 */
public final class RuneLibraryPiece extends TravelPiece {
	/** Per layout: which wall (true: the blue carpet's) and which shelf column, counted from the door, hides the nook. */
	static final boolean[] BLUE = {true, false, true};
	static final int[] COLUMN = {3, 5, 8};

	public RuneLibraryPiece(int x, int y, int z, Direction facing) { super(TravelSites.LIBRARY_PIECE, x, y, z, 15, 17, 17, facing); }
	public RuneLibraryPiece(CompoundTag tag) { super(TravelSites.LIBRARY_PIECE, tag); }

	/** The nook's chest, in local coordinates. */
	public int[] nook() { int v = variant(); return new int[]{BLUE[v] ? 13 : 1, 1, 2 + COLUMN[v]}; }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState deep = b(Blocks.POLISHED_DEEPSLATE), chisel = b(Blocks.CHISELED_DEEPSLATE), pane = b(Blocks.GLASS_PANE);
		BlockState beamX = b(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		Palette plinth = Palette.of(Blocks.DEEPSLATE_BRICKS, 6, Blocks.COBBLED_DEEPSLATE, 2, Blocks.CRACKED_DEEPSLATE_BRICKS, 1);
		Palette stone = Palette.of(Blocks.STONE_BRICKS, 10, Blocks.MOSSY_STONE_BRICKS, 2, Blocks.CRACKED_STONE_BRICKS, 2);
		footing(level, bb, 0, 0, 14, 16, b(Blocks.STONE_BRICKS), COBBLE, 16);
		for (int x = 5; x <= 9; x++) set(level, bb, b(Blocks.GRAVEL), x, 0, 0);
		// A stone hall on a dark plinth, its walls banded at the shelf tops and the eaves.
		room(level, bb, 0, 0, 1, 14, 7, 15, b(Blocks.STONE_BRICKS));
		walls(level, bb, 0, 2, 1, 14, 7, 15, stone);
		walls(level, bb, 0, 0, 1, 14, 1, 15, plinth);
		walls(level, bb, 0, 4, 1, 14, 4, 15, Palette.of(Blocks.POLISHED_DEEPSLATE, 1));
		fill(level, bb, 1, 0, 2, 13, 0, 14, b(Blocks.DARK_OAK_PLANKS));
		// The ceiling opens into the roof; tie beams carry the hanging lanterns.
		fill(level, bb, 1, 7, 2, 13, 7, 14, AIR);
		for (int z : new int[]{4, 8, 12}) fill(level, bb, 1, 7, z, 13, 7, z, beamX);
		for (int[] c : new int[][]{{0, 1}, {14, 1}, {0, 15}, {14, 15}}) pillar(level, bb, c[0], c[1], 0, 7, chisel, deep, chisel);
		// A steep slate roof with its gable over the door, so the front reads as a pediment.
		int ridge = gableRoof(level, bb, 1, 1, 13, 15, 8, false, Blocks.DEEPSLATE_TILE_STAIRS, b(Blocks.DEEPSLATE_TILES), stone);
		for (int z : new int[]{4, 8, 12}) fill(level, bb, 7, 8, z, 7, ridge - 1, z, b(Blocks.DARK_OAK_LOG));
		for (int z : new int[]{6, 10}) hangingLantern(level, bb, 7, ridge - 1, z, 4, false);
		// Rose windows in both gables.
		for (int z : new int[]{1, 15}) {
			fill(level, bb, 6, 10, z, 8, 12, z, pane);
			for (int[] c : new int[][]{{7, 9}, {7, 13}, {5, 11}, {9, 11}}) set(level, bb, pane, c[0], c[1], z);
			set(level, bb, chisel, 7, 11, z);
			set(level, bb, chisel, 7, 14, z);
		}
		// A portal: dark columns, a lintel and a small pediment around the door, a lantern hung in it.
		for (int x : new int[]{5, 9}) { set(level, bb, deep, x, 0, 0); pillar(level, bb, x, 0, 1, 4, chisel, deep, chisel); }
		for (int x = 6; x <= 8; x++) set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 4, 0);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, false), 5, 5, 0);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, false), 9, 5, 0);
		fill(level, bb, 6, 5, 0, 8, 5, 0, b(Blocks.DEEPSLATE_BRICKS));
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, false), 6, 6, 0);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, false), 8, 6, 0);
		set(level, bb, chisel, 7, 6, 0);
		set(level, bb, b(Blocks.DEEPSLATE_BRICK_SLAB), 7, 7, 0);
		hangingLantern(level, bb, 7, 3, 0, 0, false);
		door(level, bb, Blocks.DARK_OAK_DOOR, 7, 1, 1, Direction.NORTH);
		// Tall front windows with sills and hoods, and the side windows framed the same way.
		for (int x : new int[]{3, 11}) {
			fill(level, bb, x, 2, 1, x, 3, 1, pane);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 1, 0);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 4, 0);
			set(level, bb, b(Blocks.POTTED_FERN), x, 2, 0);
		}
		for (int z : new int[]{5, 8, 11}) {
			fill(level, bb, 0, 4, z, 0, 5, z, pane); fill(level, bb, 14, 4, z, 14, 5, z, pane);
			set(level, bb, chisel, 0, 6, z); set(level, bb, chisel, 14, 6, z);
		}
		for (int z : new int[]{3, 13}) { pillar(level, bb, 0, z, 0, 6, chisel, deep, deep); pillar(level, bb, 14, z, 0, 6, chisel, deep, deep); }
		// A tall lancet over the back shelves.
		fill(level, bb, 7, 3, 15, 7, 6, 15, pane);
		fill(level, bb, 6, 5, 15, 8, 5, 15, pane);
		// Shelf walls with solid backing; carpets mark which wall is which.
		for (int x : new int[]{1, 13}) fill(level, bb, x, 1, 3, x, 3, 13, b(Blocks.DARK_OAK_PLANKS));
		for (int x : new int[]{2, 12}) fill(level, bb, x, 1, 3, x, 3, 13, b(Blocks.BOOKSHELF));
		fill(level, bb, 3, 0, 3, 3, 0, 13, b(Blocks.WOOL.pick(DyeColor.RED)));
		fill(level, bb, 11, 0, 3, 11, 0, 13, b(Blocks.WOOL.pick(DyeColor.BLUE)));
		fill(level, bb, 3, 1, 3, 3, 1, 13, b(Blocks.CARPET.pick(DyeColor.RED)));
		fill(level, bb, 11, 1, 3, 11, 1, 13, b(Blocks.CARPET.pick(DyeColor.BLUE)));
		// Reading tables between the lecterns.
		for (int z : new int[]{7, 9}) for (int x : new int[]{6, 8}) {
			set(level, bb, b(Blocks.DARK_OAK_FENCE), x, 1, z);
			set(level, bb, b(Blocks.DARK_OAK_PRESSURE_PLATE), x, 2, z);
		}
		for (int[] l : new int[][]{{5, 8}, {9, 8}, {7, 4}, {7, 12}}) set(level, bb, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), l[0], 6, l[1]);
		// A back shelf under the lancet, candles along the shelf tops, the wall colours and a fern by the door.
		fill(level, bb, 4, 1, 14, 6, 2, 14, b(Blocks.BOOKSHELF));
		fill(level, bb, 8, 1, 14, 10, 2, 14, b(Blocks.BOOKSHELF));
		for (int x : new int[]{4, 10}) set(level, bb, b(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), x, 3, 14);
		for (int z : new int[]{4, 8, 12}) for (int x : new int[]{2, 12})
			set(level, bb, b(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 1 + (z + x) % 3).setValue(CandleBlock.LIT, true), x, 4, z);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), 4, 5, 14, Direction.SOUTH);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.BLUE), 10, 5, 14, Direction.SOUTH);
		for (int x : new int[]{2, 12}) set(level, bb, b(Blocks.POTTED_FERN), x, 1, 2);
		int v = variant();
		lectern(level, bb, 7, 1, 5, Direction.SOUTH, TravelSites.written("A Reader's Riddle", "The Librarian",
			List.of(Component.translatable("book.wildercord.travel_riddle." + v), Component.translatable("book.wildercord.travel_riddle.end"))));
		lectern(level, bb, 7, 1, 11, Direction.SOUTH, TravelSites.book("travel_catalog", "Reading the Catalog", "The Librarian"));
		int[] n = nook();
		set(level, bb, AIR, n[0], 1, n[2]);
		chest(level, bb, n[0], 1, n[2], TravelSites.loot(TravelSites.LIBRARY), n[0] == 1 ? Direction.EAST : Direction.WEST, 4);
	}
}
