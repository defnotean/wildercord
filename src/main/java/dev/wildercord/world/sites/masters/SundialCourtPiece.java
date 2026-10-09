package dev.wildercord.world.sites.masters;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
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
		pad(level, bb, 0, 0, 18, 20, 15, s(Blocks.SMOOTH_SANDSTONE), s(Blocks.SANDSTONE));
		for (int x = 0; x <= 18; x++) for (int z = 0; z <= 20; z++) {
			double d = dist(x, z, 9, 9);
			if (d > 7.5 && d <= 8.5) set(level, bb, ring, x, 0, z);
			else if (d > 9.5 && noise(x, z) < 15) set(level, bb, s(Blocks.SAND), x, 1, z);
		}
		// The hour stones, all but the one where the path comes in (and the one the shrine stands on).
		for (int k = 0; k < 12; k++) {
			if (k == 9) continue;
			int x = 9 + (int) Math.round(8 * Math.cos(Math.PI * k / 6)), z = 9 + (int) Math.round(8 * Math.sin(Math.PI * k / 6));
			fill(level, bb, x, 1, z, x, 2, z, stone);
		}
		// The gnomon and its shadow line.
		fill(level, bb, 8, 1, 8, 10, 2, 10, wall);
		fill(level, bb, 9, 3, 9, 9, 12, 9, stone);
		set(level, bb, cap, 9, 13, 9);
		fill(level, bb, 9, 0, 11, 9, 0, 16, s(Blocks.DYED_TERRACOTTA.brown()));
		// The shrine: sealed with the school's element, its reward inside.
		room(level, bb, 5, 0, 15, 13, 6, 20, wall);
		fill(level, bb, 6, 7, 16, 12, 7, 19, wall);
		fill(level, bb, 6, 0, 16, 12, 0, 19, ring);
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

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(5, 0, 15, 13, 6, 20)); }
}
