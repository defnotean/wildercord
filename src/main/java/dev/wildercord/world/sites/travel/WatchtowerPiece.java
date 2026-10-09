package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
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

	public WatchtowerPiece(int x, int y, int z, Direction facing) { super(TravelSites.TOWER_PIECE, x, y, z, 11, 23, 11, facing); }
	public WatchtowerPiece(CompoundTag tag) { super(TravelSites.TOWER_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState brick = b(Blocks.STONE_BRICKS), mossy = b(Blocks.MOSSY_STONE_BRICKS), cracked = b(Blocks.CRACKED_STONE_BRICKS);
		footing(level, bb, 1, 0, 9, 9, b(Blocks.COBBLESTONE), COBBLE, 22);
		// Hollow shaft, weathered higher on the windward side.
		for (int y = 0; y <= TOP; y++) for (int x = 2; x <= 8; x++) for (int z = 2; z <= 8; z++) {
			if (x != 2 && x != 8 && z != 2 && z != 8 && y != 0 && y != TOP) { set(level, bb, AIR, x, y, z); continue; }
			int n = noise(x, y, z);
			set(level, bb, n < 14 ? mossy : n < 22 ? cracked : brick, x, y, z);
		}
		fill(level, bb, 3, 0, 3, 7, 0, 7, b(Blocks.SPRUCE_PLANKS));
		// Two landings and window slits on every floor.
		for (int y : new int[]{6, 12}) fill(level, bb, 3, y, 3, 7, y, 7, b(Blocks.SPRUCE_PLANKS));
		for (int y : new int[]{3, 9, 15}) {
			fill(level, bb, 5, y, 2, 5, y + 1, 2, AIR); fill(level, bb, 3, y, 8, 3, y + 1, 8, AIR);
			fill(level, bb, 2, y, 5, 2, y + 1, 5, AIR); fill(level, bb, 8, y, 5, 8, y + 1, 5, AIR);
		}
		door(level, bb, Blocks.SPRUCE_DOOR, 5, 1, 2, Direction.NORTH);
		for (int y = 1; y <= TOP; y++) set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH), 5, y, 7);
		for (int y : new int[]{4, 10, 16}) set(level, bb, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 4, y + 1, 4);
		// The top: an overhanging floor, crenellations, the cold signal fire and the watch chest.
		fill(level, bb, 1, TOP, 1, 9, TOP, 9, brick);
		set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH), 5, TOP, 7);
		// A full parapet, so nobody steps off by accident; the corners stand taller.
		for (int x = 1; x <= 9; x++) for (int z = 1; z <= 9; z++)
			if (x == 1 || x == 9 || z == 1 || z == 9) set(level, bb, b(Blocks.STONE_BRICK_WALL), x, TOP + 1, z);
		for (int[] c : new int[][]{{1, 1}, {9, 1}, {1, 9}, {9, 9}}) fill(level, bb, c[0], TOP + 1, c[1], c[0], TOP + 2, c[1], brick);
		set(level, bb, b(Blocks.HAY_BLOCK), 5, TOP, 4);
		set(level, bb, b(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.SIGNAL_FIRE, true), 5, TOP + 1, 4);
		chest(level, bb, 7, TOP + 1, 7, TravelSites.loot(TravelSites.TOWER), Direction.WEST, 2);
		lectern(level, bb, 3, TOP + 1, 7, Direction.EAST, TravelSites.book("travel_watch", "Watch Orders", "The Road Wardens"));
	}
}
