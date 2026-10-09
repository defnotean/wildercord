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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * A small rune library under a steep slate roof. Two shelf walls, one along a red carpet and one along a blue one.
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
		footing(level, bb, 0, 0, 14, 15, b(Blocks.STONE_BRICKS), COBBLE, 16);
		for (int x = 5; x <= 9; x++) set(level, bb, b(Blocks.GRAVEL), x, 0, 0);
		room(level, bb, 0, 0, 1, 14, 7, 15, b(Blocks.STONE_BRICKS));
		fill(level, bb, 1, 0, 2, 13, 0, 14, b(Blocks.DARK_OAK_PLANKS));
		fill(level, bb, 1, 7, 2, 13, 7, 14, b(Blocks.DARK_OAK_PLANKS));
		gable(level, bb, 0, 14, 1, 15, 7, Blocks.DEEPSLATE_TILE_STAIRS, b(Blocks.DEEPSLATE_TILES), b(Blocks.STONE_BRICKS));
		door(level, bb, Blocks.DARK_OAK_DOOR, 7, 1, 1, Direction.NORTH);
		for (int z : new int[]{5, 8, 11}) { fill(level, bb, 0, 4, z, 0, 5, z, b(Blocks.GLASS_PANE)); fill(level, bb, 14, 4, z, 14, 5, z, b(Blocks.GLASS_PANE)); }
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
		int v = variant();
		lectern(level, bb, 7, 1, 5, Direction.SOUTH, TravelSites.written("A Reader's Riddle", "The Librarian",
			List.of(Component.translatable("book.wildercord.travel_riddle." + v), Component.translatable("book.wildercord.travel_riddle.end"))));
		lectern(level, bb, 7, 1, 11, Direction.SOUTH, TravelSites.book("travel_catalog", "Reading the Catalog", "The Librarian"));
		int[] n = nook();
		set(level, bb, AIR, n[0], 1, n[2]);
		chest(level, bb, n[0], 1, n[2], TravelSites.loot(TravelSites.LIBRARY), n[0] == 1 ? Direction.EAST : Direction.WEST, 4);
	}
}
