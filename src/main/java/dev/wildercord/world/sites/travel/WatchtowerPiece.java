package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A stone watchtower seen from far off. A ladder climbs three floors to a crenellated top with a cold signal
 * fire: light it and its smoke column marks the road for miles. The watch chest waits beside it.
 */
public final class WatchtowerPiece extends TravelPiece {
	public static final int TOP = 18;

	public WatchtowerPiece(int x, int y, int z, Direction facing) { super(TravelSites.TOWER_PIECE, x, y, z, 11, 26, 11, facing); }
	public WatchtowerPiece(CompoundTag tag) { super(TravelSites.TOWER_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState brick = b(Blocks.STONE_BRICKS), deep = b(Blocks.POLISHED_DEEPSLATE);
		Palette shaft = Palette.of(Blocks.STONE_BRICKS, 10, Blocks.MOSSY_STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 2);
		Palette plinth = Palette.of(Blocks.DEEPSLATE_BRICKS, 6, Blocks.COBBLED_DEEPSLATE, 2, Blocks.CRACKED_DEEPSLATE_BRICKS, 1);
		footing(level, bb, 0, 0, 10, 10, b(Blocks.COBBLESTONE), COBBLE, 25);
		// Hollow shaft on a dark battered plinth.
		for (int y = 0; y <= TOP; y++) for (int x = 2; x <= 8; x++) for (int z = 2; z <= 8; z++) {
			boolean shell = x == 2 || x == 8 || z == 2 || z == 8 || y == 0 || y == TOP;
			set(level, bb, !shell ? AIR : y <= 2 ? plinth.at(x, y, z) : shaft.at(x, y, z), x, y, z);
		}
		walls(level, bb, 1, 0, 1, 9, 1, 9, plinth);
		ring(level, bb, 1, 2, Blocks.DEEPSLATE_BRICK_STAIRS, false, true);
		fill(level, bb, 3, 0, 3, 7, 0, 7, b(Blocks.SPRUCE_PLANKS));
		// Two landings, a string course outside each, and hooded window slits on every floor.
		for (int y : new int[]{6, 12}) {
			fill(level, bb, 3, y, 3, 7, y, 7, b(Blocks.SPRUCE_PLANKS));
			ring(level, bb, 1, y, Blocks.STONE_BRICK_STAIRS, true, true);
		}
		for (int y : new int[]{3, 9, 15}) {
			fill(level, bb, 5, y, 2, 5, y + 1, 2, AIR); fill(level, bb, 3, y, 8, 3, y + 1, 8, AIR);
			fill(level, bb, 2, y, 5, 2, y + 1, 5, AIR); fill(level, bb, 8, y, 5, 8, y + 1, 5, AIR);
			if (y > 3) set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), 5, y + 2, 1);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), 3, y + 2, 9);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 1, y + 2, 5);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 9, y + 2, 5);
		}
		// A porch: the plinth opens for the door between two lantern-topped piers under a stone lintel.
		fill(level, bb, 5, 1, 1, 5, 2, 1, AIR);
		for (int x : new int[]{4, 6}) { fill(level, bb, x, 1, 1, x, 2, 1, deep); set(level, bb, b(Blocks.LANTERN), x, 3, 1); }
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), 5, 3, 1);
		door(level, bb, Blocks.SPRUCE_DOOR, 5, 1, 2, Direction.NORTH);
		for (int y = 1; y <= TOP; y++) set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH), 5, y, 7);
		for (int y : new int[]{4, 10, 16}) set(level, bb, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 4, y + 1, 4);
		// The Road Wardens' colours on the face that looks down the road.
		for (int x : new int[]{4, 6}) banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.BLUE), x, 14, 1, Direction.SOUTH);
		// The top: an overhanging floor on a corbel course, crenellations, the cold signal fire and the watch chest.
		ring(level, bb, 1, TOP - 1, Blocks.STONE_BRICK_STAIRS, true, false);
		fill(level, bb, 1, TOP, 1, 9, TOP, 9, shaft);
		set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH), 5, TOP, 7);
		// A full parapet, so nobody steps off by accident, with merlons along it and lanterns on the corner merlons.
		walls(level, bb, 1, TOP + 1, 1, 9, TOP + 1, 9, shaft);
		crenellate(level, bb, 1, 1, 9, 9, TOP + 2, brick);
		for (int[] c : new int[][]{{1, 1}, {9, 1}, {1, 9}, {9, 9}}) {
			set(level, bb, b(Blocks.CHISELED_STONE_BRICKS), c[0], TOP + 2, c[1]);
			set(level, bb, b(Blocks.LANTERN), c[0], TOP + 3, c[1]);
		}
		// A watch shelter over the back of the roof, clear of the fire so its smoke still rises.
		for (int[] c : new int[][]{{2, 5}, {8, 5}, {2, 8}, {8, 8}}) fill(level, bb, c[0], TOP + 1, c[1], c[0], TOP + 3, c[1], b(Blocks.STRIPPED_SPRUCE_LOG));
		hipRoof(level, bb, 2, 5, 8, 8, TOP + 4, Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_PLANKS), b(Blocks.SPRUCE_SLAB));
		hangingLantern(level, bb, 5, TOP + 4, 6, 1, false);
		set(level, bb, b(Blocks.HAY_BLOCK), 5, TOP, 4);
		set(level, bb, b(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.SIGNAL_FIRE, true), 5, TOP + 1, 4);
		chest(level, bb, 7, TOP + 1, 7, TravelSites.loot(TravelSites.TOWER), Direction.WEST, 2);
		lectern(level, bb, 3, TOP + 1, 7, Direction.EAST, TravelSites.book("travel_watch", "Watch Orders", "The Road Wardens"));
	}

	/**
	 * A course of stairs around the shaft {@code inset} blocks from the box edge at {@code y}, each climbing toward the
	 * shaft: right way up for a battered plinth, upside down for a ledge or corbel. Corners are solid unless {@code solidCorners} is false.
	 */
	private void ring(WorldGenLevel level, BoundingBox bb, int inset, int y, net.minecraft.world.level.block.Block stairs, boolean top, boolean solidCorners) {
		int lo = inset, hi = 10 - inset;
		for (int i = lo + 1; i < hi; i++) {
			set(level, bb, stair(stairs, Direction.NORTH, top), i, y, lo);
			set(level, bb, stair(stairs, Direction.SOUTH, top), i, y, hi);
			set(level, bb, stair(stairs, Direction.EAST, top), lo, y, i);
			set(level, bb, stair(stairs, Direction.WEST, top), hi, y, i);
		}
		BlockState corner = solidCorners ? b(Blocks.DEEPSLATE_BRICKS) : b(Blocks.STONE_BRICKS);
		for (int[] c : new int[][]{{lo, lo}, {hi, lo}, {lo, hi}, {hi, hi}}) set(level, bb, corner, c[0], y, c[1]);
	}
}
