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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * An old arch bridge over a steep creek, its middle fallen in. Jump the two-block gap, or drop into the deep
 * water and climb a bank ladder. A crenellated gatehouse arches over the near bridgehead, a lantern arch and a
 * watchtower guard the far one, and the old road runs out through a cleared margin on both banks.
 * The toll keeper's booth on the far side still holds the takings.
 */
public final class BrokenBridgePiece extends TravelPiece {
	/** The fallen span, in z: the deck is missing here. */
	public static final int GAP0 = 12, GAP1 = 13;

	/** Cleared ground kept on every side of the crossing. */
	static final int MARGIN = 5;

	public BrokenBridgePiece(int x, int y, int z, Direction facing) { super(TravelSites.BRIDGE_PIECE, x, y, z, 15, 14, 25, facing, MARGIN); }
	public BrokenBridgePiece(CompoundTag tag) { super(TravelSites.BRIDGE_PIECE, tag); }

	@Override protected int margin() { return MARGIN; }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		clearCanopy(level, bb);
		BlockState brick = b(Blocks.STONE_BRICKS), mossy = b(Blocks.MOSSY_STONE_BRICKS), cracked = b(Blocks.CRACKED_STONE_BRICKS);
		// Both banks levelled, the creek cut between them: deep enough to fall into safely.
		int m = MARGIN, x1 = 14 + m;
		footing(level, bb, -m, -m, x1, 7, GRASS, DIRT, 12);
		footing(level, bb, -m, 17, x1, 24 + m, GRASS, DIRT, 12);
		fill(level, bb, -m, -5, 8, x1, 0, 16, DIRT);
		fill(level, bb, -m, 1, 8, x1, 12, 16, AIR);
		fill(level, bb, 1 - m, -5, 9, x1 - 1, -5, 15, b(Blocks.GRAVEL));
		fill(level, bb, 1 - m, -4, 9, x1 - 1, -2, 15, b(Blocks.WATER));
		fill(level, bb, 1 - m, -1, 9, x1 - 1, 0, 15, AIR);
		// The old road out across both banks, worn to coarse dirt and gravel, with lantern posts at the clearing's edge.
		for (int z = -m; z <= 24 + m; z++) {
			if (z > 2 && z < 22) continue;
			for (int x = 6; x <= 8; x++) set(level, bb, noise(x, z) < 30 ? b(Blocks.COARSE_DIRT) : noise(x, z) < 70 ? b(Blocks.GRAVEL) : b(Blocks.DIRT_PATH), x, 0, z);
		}
		for (int z : new int[]{-m + 1, 23 + m}) for (int x : new int[]{5, 9}) {
			post(level, bb, b(Blocks.SPRUCE_FENCE), x, 2, z);
			set(level, bb, b(Blocks.LANTERN), x, 3, z);
		}
		// Fallen stones from the span and the old gate lie in the grass.
		for (int[] s : new int[][]{{-3, -2}, {16, 1}, {-2, 21}, {17, 26}, {13, -4}}) {
			set(level, bb, b(Blocks.MOSSY_COBBLESTONE), s[0], 1, s[1]);
			set(level, bb, noise(s[0], s[1]) < 50 ? b(Blocks.MOSSY_STONE_BRICK_SLAB) : cracked, s[0] + 1, 1, s[1]);
		}
		// Ferns along both banks and lily pads in the slack water by the walls.
		for (int[] r : new int[][]{{-m, -m, 3, 7}, {11, -m, x1, 7}, {-m, 17, 3, 24 + m}, {11, 17, x1, 24 + m}}) scatter(level, bb, r[0], 1, r[1], r[2], 1, r[3], b(Blocks.FERN), 12);
		for (int[] l : new int[][]{{2, 11}, {12, 12}, {1, 14}, {13, 10}}) set(level, bb, b(Blocks.LILY_PAD), l[0], -1, l[1]);
		// Abutments, the arch's two surviving piers, and the deck with its fallen middle.
		fill(level, bb, 5, -5, 4, 9, 1, 8, brick);
		fill(level, bb, 5, -5, 16, 9, 1, 20, brick);
		for (int x = 5; x <= 9; x++) for (int z = 4; z <= 20; z++) if (z <= 8 || z >= 16) fillColumnDown(level, brick, x, -6, z, bb);
		for (int z : new int[]{10, 15}) fill(level, bb, 6, -5, z, 8, 0, z, mossy);
		// The piers widened to the deck with pointed cutwaters, and the arch's springers under the deck.
		for (int z : new int[]{10, 15}) {
			for (int x : new int[]{5, 9}) fill(level, bb, x, -5, z, x, 0, z, noise(x, z) < 50 ? mossy : brick);
			fill(level, bb, 4, -5, z, 4, -1, z, mossy); fill(level, bb, 10, -5, z, 10, -1, z, mossy);
			set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.EAST, false), 4, 0, z);
			set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.WEST, false), 10, 0, z);
		}
		fill(level, bb, 5, 0, 9, 9, 0, 9, topSlab(Blocks.STONE_BRICK_SLAB));
		for (int x = 5; x <= 9; x++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), x, 0, 11);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), x, 0, 14);
		}
		for (int z = 9; z <= 15; z++) for (int x = 5; x <= 9; x++) {
			int n = noise(x, z);
			set(level, bb, z >= GAP0 && z <= GAP1 ? AIR : n < 20 ? cracked : n < 35 ? mossy : brick, x, 1, z);
		}
		fill(level, bb, 6, -4, GAP0, 8, -4, GAP1, b(Blocks.MOSSY_COBBLESTONE));
		set(level, bb, b(Blocks.MOSSY_COBBLESTONE), 7, -3, GAP0);
		set(level, bb, cracked, 5, -4, GAP1); set(level, bb, mossy, 9, -4, GAP0);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false), 8, -3, GAP1);
		for (int x = 6; x <= 8; x++) {
			set(level, bb, b(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH), x, 1, 3);
			set(level, bb, b(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH), x, 1, 21);
		}
		for (int z = 4; z <= 20; z++) for (int x : new int[]{5, 9}) {
			if (z >= GAP0 - 1 && z <= GAP1 + 1 || noise(x, 2, z) < 18) continue;
			set(level, bb, b(Blocks.STONE_BRICK_WALL), x, 2, z);
		}
		for (int[] l : new int[][]{{5, 4}, {9, 4}, {5, 20}}) { set(level, bb, b(Blocks.STONE_BRICK_WALL), l[0], 2, l[1]); set(level, bb, b(Blocks.LANTERN), l[0], 3, l[1]); }
		// The parapet stays open in front of the booth door.
		set(level, bb, AIR, 9, 2, 20);
		// A corbelled string course under both deck edges over the water, broken where the span fell.
		for (int z = 9; z <= 15; z++) {
			if (z >= GAP0 && z <= GAP1 || z == 10 || z == 15) continue;
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 4, 0, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 10, 0, z);
		}
		// Chiselled newel posts where the parapet meets each abutment.
		for (int z : new int[]{8, 16}) for (int x : new int[]{5, 9}) {
			set(level, bb, b(Blocks.CHISELED_STONE_BRICKS), x, 2, z);
			set(level, bb, b(Blocks.STONE_BRICK_SLAB), x, 3, z);
		}
		// Bridgehead piers by the water, lantern-topped, marking the crossing from far off.
		for (int x : new int[]{4, 10}) {
			post(level, bb, mossy, x, 0, 8);
			pillar(level, bb, x, 8, 1, 3, mossy, brick, b(Blocks.CHISELED_STONE_BRICKS));
			set(level, bb, b(Blocks.LANTERN), x, 4, 8);
		}
		// The near gatehouse: two crenellated towers with a vaulted arch carried over the deck between them.
		tower(level, bb, 2, 4, 9);
		tower(level, bb, 10, 4, 9);
		fill(level, bb, 5, 7, 4, 9, 8, 6, brick);
		for (int z = 4; z <= 6; z++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 5, 6, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 9, 6, z);
			for (int x : new int[]{6, 8}) set(level, bb, topSlab(Blocks.STONE_BRICK_SLAB), x, 6, z);
		}
		fill(level, bb, 5, 7, 4, 9, 7, 4, b(Blocks.CHISELED_STONE_BRICKS));
		for (int x : new int[]{5, 7, 9}) for (int z : new int[]{4, 6}) set(level, bb, mossy, x, 9, z);
		hangingLantern(level, bb, 7, 6, 5, 0, false);
		// The far bank: a lantern arch over the deck's end and a watchtower beside the road.
		for (int x : new int[]{4, 10}) {
			post(level, bb, mossy, x, 0, 16);
			pillar(level, bb, x, 16, 1, 6, mossy, brick, b(Blocks.CHISELED_STONE_BRICKS));
		}
		fill(level, bb, 4, 7, 16, 10, 7, 16, brick);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 5, 6, 16);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 9, 6, 16);
		for (int x : new int[]{4, 7, 10}) set(level, bb, b(Blocks.STONE_BRICK_WALL), x, 8, 16);
		for (int x : new int[]{4, 10}) set(level, bb, b(Blocks.LANTERN), x, 9, 16);
		hangingLantern(level, bb, 7, 6, 16, 0, false);
		tower(level, bb, 1, 18, 10);
		// Ladders up both banks for anyone who falls.
		for (int y = -4; y <= 0; y++) {
			set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH), 3, y, 9);
			set(level, bb, b(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH), 11, y, 15);
		}
		// The toll booth on the far bank: a stone sill course, log corners, a steep spruce roof and a lit doorway.
		room(level, bb, 10, 0, 18, 13, 4, 22, b(Blocks.SPRUCE_PLANKS));
		fill(level, bb, 10, 0, 18, 13, 0, 22, COBBLE);
		walls(level, bb, 10, 1, 18, 13, 1, 22, Palette.of(Blocks.COBBLESTONE, 3, Blocks.MOSSY_COBBLESTONE, 2));
		for (int[] c : new int[][]{{10, 18}, {13, 18}, {10, 22}, {13, 22}}) post(level, bb, b(Blocks.SPRUCE_LOG), c[0], 4, c[1]);
		fill(level, bb, 11, 4, 18, 12, 4, 18, b(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, bb, 11, 4, 22, 12, 4, 22, b(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		int ridge = gableRoof(level, bb, 10, 18, 13, 22, 5, true, Blocks.SPRUCE_STAIRS, b(Blocks.SPRUCE_PLANKS), Palette.of(Blocks.SPRUCE_PLANKS, 1));
		for (int x : new int[]{10, 13}) set(level, bb, b(Blocks.GLASS_PANE), x, ridge - 1, 20);
		door(level, bb, Blocks.SPRUCE_DOOR, 10, 1, 20, Direction.EAST);
		for (int z : new int[]{19, 21}) sconce(level, bb, 9, 3, z, Direction.WEST, false);
		set(level, bb, b(Blocks.GLASS_PANE), 13, 2, 20);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 14, 1, 20);
		set(level, bb, b(Blocks.BARREL), 14, 1, 18);
		set(level, bb, b(Blocks.BARREL), 14, 1, 22);
		hangingLantern(level, bb, 11, 3, 20, 0, false);
		chest(level, bb, 12, 1, 21, TravelSites.loot(TravelSites.BRIDGE), Direction.WEST, 3);
		lectern(level, bb, 12, 1, 19, Direction.WEST, TravelSites.book("travel_toll", "Toll Notice", "The Bridge Keeper"));
	}

	/** A 3x3 stone tower from the ground to {@code top}: arrow slits, a chiselled band, merlons and a brazier on the roof. */
	private void tower(WorldGenLevel level, BoundingBox bb, int x0, int z0, int top) {
		Palette stone = Palette.of(Blocks.STONE_BRICKS, 5, Blocks.MOSSY_STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 1);
		for (int x = x0; x <= x0 + 2; x++) for (int z = z0; z <= z0 + 2; z++) fillColumnDown(level, b(Blocks.STONE_BRICKS), x, 0, z, bb);
		walls(level, bb, x0, 1, z0, x0 + 2, top, z0 + 2, stone);
		fill(level, bb, x0, top - 2, z0, x0 + 2, top - 2, z0 + 2, b(Blocks.CHISELED_STONE_BRICKS));
		fill(level, bb, x0, top, z0, x0 + 2, top, z0 + 2, b(Blocks.STONE_BRICKS));
		for (int y : new int[]{3, top - 3}) {
			set(level, bb, b(Blocks.IRON_BARS), x0 + 1, y, z0);
			set(level, bb, b(Blocks.IRON_BARS), x0 + 1, y, z0 + 2);
		}
		crenellate(level, bb, x0, z0, x0 + 2, z0 + 2, top + 1, b(Blocks.MOSSY_STONE_BRICKS));
		set(level, bb, b(Blocks.CAMPFIRE), x0 + 1, top + 1, z0 + 1);
	}
}
