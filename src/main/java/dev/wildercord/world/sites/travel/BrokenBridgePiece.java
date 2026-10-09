package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * An old arch bridge over a steep creek, its middle fallen in. Jump the two-block gap, or drop into the deep
 * water and climb a bank ladder. The toll keeper's booth on the far side still holds the takings.
 */
public final class BrokenBridgePiece extends TravelPiece {
	/** The fallen span, in z: the deck is missing here. */
	public static final int GAP0 = 12, GAP1 = 13;

	public BrokenBridgePiece(int x, int y, int z, Direction facing) { super(TravelSites.BRIDGE_PIECE, x, y, z, 15, 10, 25, facing); }
	public BrokenBridgePiece(CompoundTag tag) { super(TravelSites.BRIDGE_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState brick = b(Blocks.STONE_BRICKS), mossy = b(Blocks.MOSSY_STONE_BRICKS), cracked = b(Blocks.CRACKED_STONE_BRICKS);
		// Both banks levelled, the creek cut between them: deep enough to fall into safely.
		footing(level, bb, 0, 0, 14, 7, GRASS, DIRT, 9);
		footing(level, bb, 0, 17, 14, 24, GRASS, DIRT, 9);
		fill(level, bb, 0, -5, 8, 14, 0, 16, DIRT);
		fill(level, bb, 0, 1, 8, 14, 9, 16, AIR);
		fill(level, bb, 1, -5, 9, 13, -5, 15, b(Blocks.GRAVEL));
		fill(level, bb, 1, -4, 9, 13, -2, 15, b(Blocks.WATER));
		fill(level, bb, 1, -1, 9, 13, 0, 15, AIR);
		for (int z : new int[]{0, 1, 2, 22, 23, 24}) for (int x = 6; x <= 8; x++) set(level, bb, noise(x, z) < 30 ? b(Blocks.COARSE_DIRT) : b(Blocks.GRAVEL), x, 0, z);
		// Abutments, the arch's two surviving piers, and the deck with its fallen middle.
		fill(level, bb, 5, -5, 4, 9, 1, 8, brick);
		fill(level, bb, 5, -5, 16, 9, 1, 20, brick);
		for (int x = 5; x <= 9; x++) for (int z = 4; z <= 20; z++) if (z <= 8 || z >= 16) fillColumnDown(level, brick, x, -6, z, bb);
		for (int z : new int[]{10, 15}) fill(level, bb, 6, -5, z, 8, 0, z, mossy);
		for (int z = 9; z <= 15; z++) for (int x = 5; x <= 9; x++) {
			int n = noise(x, z);
			set(level, bb, z >= GAP0 && z <= GAP1 ? AIR : n < 20 ? cracked : n < 35 ? mossy : brick, x, 1, z);
		}
		fill(level, bb, 6, -4, GAP0, 8, -4, GAP1, b(Blocks.MOSSY_COBBLESTONE));
		set(level, bb, b(Blocks.MOSSY_COBBLESTONE), 7, -3, GAP0);
		for (int x = 6; x <= 8; x++) {
			set(level, bb, b(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH), x, 1, 3);
			set(level, bb, b(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH), x, 1, 21);
		}
		for (int z = 4; z <= 20; z++) for (int x : new int[]{5, 9}) {
			if (z >= GAP0 - 1 && z <= GAP1 + 1 || noise(x, 2, z) < 18) continue;
			set(level, bb, b(Blocks.STONE_BRICK_WALL), x, 2, z);
		}
		for (int[] l : new int[][]{{5, 4}, {9, 4}, {5, 20}, {9, 20}}) { set(level, bb, b(Blocks.STONE_BRICK_WALL), l[0], 2, l[1]); set(level, bb, b(Blocks.LANTERN), l[0], 3, l[1]); }
		// Ladders up both banks for anyone who falls.
		for (int y = -4; y <= 0; y++) {
			set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH), 3, y, 9);
			set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH), 11, y, 15);
		}
		// The toll booth on the far bank.
		room(level, bb, 10, 0, 18, 13, 4, 22, b(Blocks.SPRUCE_PLANKS));
		fill(level, bb, 10, 0, 18, 13, 0, 22, COBBLE);
		gable(level, bb, 10, 13, 18, 22, 4, Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_PLANKS), b(Blocks.SPRUCE_PLANKS));
		door(level, bb, Blocks.SPRUCE_DOOR, 10, 1, 20, Direction.EAST);
		set(level, bb, b(Blocks.GLASS_PANE), 13, 2, 20);
		chest(level, bb, 12, 1, 21, TravelSites.loot(TravelSites.BRIDGE), Direction.WEST, 3);
		lectern(level, bb, 12, 1, 19, Direction.WEST, TravelSites.book("travel_toll", "Toll Notice", "The Bridge Keeper"));
	}
}
