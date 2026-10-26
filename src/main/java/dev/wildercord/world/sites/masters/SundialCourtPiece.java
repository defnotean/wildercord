package dev.wildercord.world.sites.masters;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** A desert sundial court of the Dune or Hourglass school: a gnomon obelisk in a ring of hour stones, and a sealed shrine behind. */
public final class SundialCourtPiece extends MasterSitePiece {
	public SundialCourtPiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.SUNDIAL_COURT, x, y, z, 19, 16, 21, facing, alt); }
	public SundialCourtPiece(CompoundTag tag) { super(MasterSites.SUNDIAL_COURT, tag); }
	@Override public String site() { return "master_sundial_court"; }
	@Override public List<String> schools() { return List.of("dune", "hourglass"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState wall = alt ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.CUT_SANDSTONE), stone = alt ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.CHISELED_SANDSTONE);
		BlockState cap = alt ? s(Blocks.GOLD_BLOCK) : s(Blocks.CHISELED_RED_SANDSTONE), ring = alt ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.SMOOTH_RED_SANDSTONE);
		BlockState slab = alt ? s(Blocks.QUARTZ_SLAB) : s(Blocks.SMOOTH_SANDSTONE_SLAB);
		Palette masonry = alt ? Palette.of(Blocks.QUARTZ_BRICKS, 6, Blocks.SMOOTH_QUARTZ, 2, Blocks.CALCITE, 1)
			: Palette.of(Blocks.CUT_SANDSTONE, 6, Blocks.SANDSTONE, 3, Blocks.SMOOTH_SANDSTONE, 2);
		Block trim = alt ? Blocks.SMOOTH_QUARTZ_STAIRS : Blocks.SMOOTH_SANDSTONE_STAIRS;
		Block accent = alt ? Blocks.QUARTZ_STAIRS : Blocks.SMOOTH_RED_SANDSTONE_STAIRS;
		pad(level, bb, 0, 0, 18, 20, 15, s(Blocks.SMOOTH_SANDSTONE), s(Blocks.SANDSTONE));
		for (int x = 0; x <= 18; x++) for (int z = 0; z <= 20; z++) {
			double d = dist(x, z, 9, 9);
			if (d > 7.5 && d <= 8.5) set(level, bb, ring, x, 0, z);
			else if (d > 9.5 && noise(x, z) < 15) set(level, bb, s(Blocks.SAND), x, 1, z);
		}
		fill(level, bb, 8, 0, 0, 10, 0, 1, ring);
		// The hour stones, all but the one where the path comes in (and the one the shrine stands on), each capped.
		for (int k = 0; k < 12; k++) {
			if (k == 9) continue;
			int x = 9 + (int) Math.round(8 * Math.cos(Math.PI * k / 6)), z = 9 + (int) Math.round(8 * Math.sin(Math.PI * k / 6));
			fill(level, bb, x, 1, z, x, 2, z, stone);
			set(level, bb, slab, x, 3, z);
		}
		// The gnomon on a stepped base, and its shadow line.
		stairRing(level, bb, 7, 7, 11, 11, 1, trim, stone);
		fill(level, bb, 8, 1, 8, 10, 2, 10, wall);
		for (int[] c : new int[][]{{8, 8}, {10, 8}, {8, 10}, {10, 10}}) set(level, bb, stone, c[0], 2, c[1]);
		fill(level, bb, 9, 3, 9, 9, 12, 9, stone);
		set(level, bb, cap, 9, 13, 9);
		fill(level, bb, 9, 0, 11, 9, 0, 16, s(Blocks.DYED_TERRACOTTA.brown()));
		// A low court wall with lantern posts, and a gate where the path comes in.
		for (int x = 1; x <= 17; x++) {
			if (x >= 7 && x <= 11) continue;
			set(level, bb, wall, x, 1, 0); set(level, bb, slab, x, 2, 0);
			if (x <= 4 || x >= 14) { set(level, bb, wall, x, 1, 20); set(level, bb, slab, x, 2, 20); }
		}
		for (int z = 1; z <= 19; z++) {
			boolean post = z % 5 == 0;
			for (int x : new int[]{0, 18}) {
				if (post) { fill(level, bb, x, 1, z, x, 2, z, stone); set(level, bb, s(Blocks.LANTERN), x, 3, z); }
				else { set(level, bb, wall, x, 1, z); set(level, bb, slab, x, 2, z); }
			}
		}
		for (int x : new int[]{7, 11}) { fill(level, bb, x, 1, 0, x, 3, 0, stone); set(level, bb, cap, x, 4, 0); set(level, bb, s(Blocks.LANTERN), x, 5, 0); }
		for (int[] c : new int[][]{{0, 0}, {18, 0}, {0, 20}, {18, 20}}) { pillar(level, bb, c[0], c[1], 1, 4, stone, wall, cap); set(level, bb, s(Blocks.LANTERN), c[0], 5, c[1]); }
		// The shrine: a stepped roof, a cornice and pilastered flanks.
		room(level, bb, 5, 0, 15, 13, 6, 20, wall);
		walls(level, bb, 5, 1, 15, 13, 5, 20, masonry);
		stairRing(level, bb, 5, 15, 13, 20, 7, trim, stone);
		fill(level, bb, 6, 7, 16, 12, 7, 19, wall);
		stairRing(level, bb, 6, 16, 12, 19, 8, trim, stone);
		fill(level, bb, 7, 8, 17, 11, 8, 18, wall);
		set(level, bb, cap, 9, 9, 17); set(level, bb, cap, 9, 9, 18);
		fill(level, bb, 6, 0, 16, 12, 0, 19, ring);
		for (int z = 16; z <= 20; z++) {
			boolean pier = z == 17 || z == 20;
			if (pier) { fill(level, bb, 4, 1, z, 4, 5, z, stone); fill(level, bb, 14, 1, z, 14, 5, z, stone); }
			else { set(level, bb, stair(trim, Direction.EAST, false), 4, 1, z); set(level, bb, stair(trim, Direction.WEST, false), 14, 1, z); }
			set(level, bb, stair(accent, Direction.EAST, true), 4, 6, z); set(level, bb, stair(accent, Direction.WEST, true), 14, 6, z);
		}
		// Inside: two columns, a sun set in the ceiling and a runner to the reward.
		for (int x : new int[]{7, 11}) pillar(level, bb, x, 17, 1, 5, ring, stone, ring);
		set(level, bb, s(Blocks.GLOWSTONE), 9, 6, 17);
		fill(level, bb, 9, 1, 16, 9, 1, 17, Blocks.CARPET.pick(alt ? DyeColor.YELLOW : DyeColor.ORANGE).defaultBlockState());
		// A pylon gateway: two battered towers flank the sealed door under a carved lintel with a sun disc.
		for (int x0 : new int[]{4, 11}) {
			int x1 = x0 + 3;
			fill(level, bb, x0, 1, 14, x1, 9, 15, masonry);
			fill(level, bb, x0, 5, 14, x1, 5, 14, stone); fill(level, bb, x0, 8, 14, x1, 8, 14, stone);
			for (int x = x0; x <= x1; x++) { set(level, bb, stair(trim, Direction.NORTH, false), x, 1, 13); set(level, bb, stair(accent, Direction.NORTH, true), x, 9, 13); }
			int out = x0 == 4 ? 3 : 15, in = x0 == 4 ? 8 : 10;
			Direction outFace = x0 == 4 ? Direction.EAST : Direction.WEST, inFace = x0 == 4 ? Direction.WEST : Direction.EAST;
			for (int z = 14; z <= 15; z++) {
				set(level, bb, stair(trim, outFace, false), out, 1, z);
				set(level, bb, stair(accent, outFace, true), out, 9, z); set(level, bb, stair(accent, inFace, true), in, 9, z);
			}
			set(level, bb, stone, out, 1, 13); set(level, bb, stone, out, 9, 13); set(level, bb, stone, in, 9, 13);
			fill(level, bb, x0, 10, 14, x1, 10, 15, ring);
		}
		set(level, bb, s(Blocks.LANTERN), 4, 11, 14); set(level, bb, s(Blocks.LANTERN), 14, 11, 14);
		fill(level, bb, 8, 4, 14, 10, 4, 14, stone);
		set(level, bb, wall, 8, 5, 14); set(level, bb, cap, 9, 5, 14); set(level, bb, wall, 10, 5, 14);
		Block banner = Blocks.WALL_BANNER.pick(alt ? DyeColor.YELLOW : DyeColor.ORANGE);
		for (int x : new int[]{5, 6, 12, 13}) banner(level, bb, banner, x, 7, 13, Direction.SOUTH);
		var e = element(school());
		sealDoorAcross(level, bb, 15, 8, 10, 1, 3, e, e);
		set(level, bb, s(Blocks.LANTERN), 6, 1, 19); set(level, bb, s(Blocks.LANTERN), 12, 1, 19);
		chest(level, bb, 9, 1, 18, rewardLoot(), Direction.SOUTH, 2);
		// Practice and the lectern.
		var post = alt ? s(Blocks.BIRCH_FENCE) : s(Blocks.ACACIA_FENCE);
		dummy(level, bb, 3, 1, 4, post); dummy(level, bb, 15, 1, 4, post);
		target(level, bb, 4, 1, 12, post); target(level, bb, 14, 1, 12, post);
		lectern(level, bb, 6, 1, 6);
		chest(level, bb, 12, 1, 5, MasterSites.supplies(site()), Direction.SOUTH, 1);
	}

	/** A course of stairs round a rectangle at {@code y}, each rising toward its middle, corners solid. */
	private void stairRing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y, Block stairs, BlockState corner) {
		for (int x = x0 + 1; x < x1; x++) {
			set(level, bb, stair(stairs, Direction.NORTH, false), x, y, z0);
			set(level, bb, stair(stairs, Direction.SOUTH, false), x, y, z1);
		}
		for (int z = z0 + 1; z < z1; z++) {
			set(level, bb, stair(stairs, Direction.EAST, false), x0, y, z);
			set(level, bb, stair(stairs, Direction.WEST, false), x1, y, z);
		}
		for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) set(level, bb, corner, c[0], y, c[1]);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(5, 0, 15, 13, 6, 20)); }
}
