package dev.wildercord.world.sites.wilds;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * An open cherry-wood pavilion under falling petals, built to watch the sun come up. Its shrine always stands on the
 * side where the sun rises, whichever way the pavilion turned; an Arcane and Life seal faces the pavilion, and a lamp
 * on the shrine's roof lights by day.
 */
public final class DawnPavilionPiece extends WildsPiece {
	public DawnPavilionPiece(int x, int z, Direction facing) { super(WildsSites.DAWN_PAVILION, x, z, 19, 12, 19, facing); }
	public DawnPavilionPiece(CompoundTag tag) { super(WildsSites.DAWN_PAVILION, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new DawnPavilionPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 6);
	}
	static final int CENTRE = 9;

	/** The shrine's local box: k 6 to 9 steps toward sunrise from the centre, two either side. */
	int[] shrine() {
		int[] d = towardSunrise();
		int ax = CENTRE + 6 * d[0], bx = CENTRE + 9 * d[0], az = CENTRE + 6 * d[1], bz = CENTRE + 9 * d[1];
		if (d[0] == 0) { ax = CENTRE - 2; bx = CENTRE + 2; } else { az = CENTRE - 2; bz = CENTRE + 2; }
		return new int[]{Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)};
	}

	/** Local x, z at k steps toward sunrise and j across. */
	private int[] at(int k, int j) {
		int[] d = towardSunrise();
		return new int[]{CENTRE + k * d[0] + j * d[1], CENTRE + k * d[1] + j * d[0]};
	}

	private static BlockState edge(Block stair, Direction inward) { return s(stair).setValue(StairBlock.FACING, inward); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		footing(level, bb, 0, 0, 18, 18, s(Blocks.DIRT));
		fill(level, bb, 0, 0, 0, 18, 0, 18, s(Blocks.GRASS_BLOCK));
		clear(level, bb, 0, 1, 0, 18, 11, 18);
		int[] box = shrine();
		for (int x = 0; x <= 18; x++) for (int z = 0; z <= 18; z++) {
			boolean inShrine = x >= box[0] && x <= box[2] && z >= box[1] && z <= box[3];
			if ((x < 3 || x > 15 || z < 3 || z > 15) && !inShrine && noise(x, z) < 35) set(level, bb, s(Blocks.PINK_PETALS), x, 1, z);
		}
		// The platform, four posts and a stepped roof.
		fill(level, bb, 3, 0, 3, 15, 0, 15, s(Blocks.CHERRY_PLANKS));
		fill(level, bb, 7, 0, 7, 11, 0, 11, s(Blocks.STRIPPED_CHERRY_LOG));
		for (int[] p : new int[][]{{4, 4}, {14, 4}, {4, 14}, {14, 14}}) fill(level, bb, p[0], 1, p[1], p[0], 5, p[1], s(Blocks.CHERRY_LOG));
		int[][] tiers = {{3, 15}, {5, 13}, {7, 11}};
		for (int t = 0; t < tiers.length; t++) {
			int a = tiers[t][0], b = tiers[t][1], y = 6 + t;
			fill(level, bb, a, y, a, b, y, b, s(Blocks.CHERRY_PLANKS));
			fill(level, bb, a + 1, y, a, b - 1, y, a, edge(Blocks.CHERRY_STAIRS, Direction.NORTH));
			fill(level, bb, a + 1, y, b, b - 1, y, b, edge(Blocks.CHERRY_STAIRS, Direction.SOUTH));
			fill(level, bb, a, y, a + 1, a, y, b - 1, edge(Blocks.CHERRY_STAIRS, Direction.EAST));
			fill(level, bb, b, y, a + 1, b, y, b - 1, edge(Blocks.CHERRY_STAIRS, Direction.WEST));
		}
		set(level, bb, s(Blocks.CHERRY_PLANKS), 9, 9, 9);
		set(level, bb, s(Blocks.CHERRY_SLAB), 9, 10, 9);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 9, 5, 9);
		for (int[] p : new int[][]{{4, 4}, {14, 4}, {4, 14}, {14, 14}}) set(level, bb, s(Blocks.CHERRY_LEAVES).setValue(LeavesBlock.PERSISTENT, true), p[0], 6, p[1]);
		chest(level, bb, 5, 1, 4, WildsSites.DAWN_HALL, Direction.NORTH, 1);
		set(level, bb, s(Blocks.FLOWER_POT), 13, 1, 5);
		// The sunrise shrine.
		fill(level, bb, box[0], 0, box[1], box[2], 4, box[3], s(Blocks.CALCITE));
		for (int k = 7; k <= 8; k++) for (int j = -1; j <= 1; j++) for (int y = 1; y <= 3; y++) { int[] p = at(k, j); set(level, bb, AIR, p[0], y, p[1]); }
		for (int j = -1; j <= 1; j++) for (int y = 2; y <= 3; y++) { int[] p = at(9, j); set(level, bb, Blocks.STAINED_GLASS.orange().defaultBlockState(), p[0], y, p[1]); }
		int[] door = at(6, 0), d = towardSunrise();
		if (d[0] == 0) sealDoorAcross(level, bb, door[1], CENTRE - 1, CENTRE + 1, 1, 3, RuneSealBlock.Element.ARCANE, RuneSealBlock.Element.LIFE);
		else sealDoorAlong(level, bb, door[0], CENTRE - 1, CENTRE + 1, 1, 3, RuneSealBlock.Element.ARCANE, RuneSealBlock.Element.LIFE);
		int[] lamp = at(8, 0), chest = at(8, 0), glow = at(7, 0);
		set(level, bb, s(Blocks.REDSTONE_LAMP), lamp[0], 4, lamp[1]);
		set(level, bb, s(Blocks.DAYLIGHT_DETECTOR), lamp[0], 5, lamp[1]);
		set(level, bb, s(Blocks.PEARLESCENT_FROGLIGHT), glow[0], 0, glow[1]);
		chest(level, bb, chest[0], 1, chest[1], WildsSites.DAWN_VAULT, Direction.NORTH, 2);
		DungeonWards.remember(level, this);
	}

	/** The local spot of the shrine's chest (for the game test). */
	int[] shrineChest() { return at(8, 0); }

	@Override public List<BoundingBox> wardedBoxes() { int[] b = shrine(); return List.of(worldBox(b[0], 0, b[1], b[2], 4, b[3])); }
}
