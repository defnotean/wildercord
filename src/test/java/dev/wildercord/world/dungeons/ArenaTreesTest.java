package dev.wildercord.world.dungeons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Which trees would grow into a dungeon, and so never grow. */
class ArenaTreesTest {
	private static final BoundingBox ARENA = new BoundingBox(0, 64, 0, 40, 80, 40);

	@Test
	void aTreeRootedInTheArenaReachesIt() {
		assertTrue(ArenaTrees.reaches(ARENA, new BlockPos(20, 65, 20)));
		assertTrue(ArenaTrees.reaches(ARENA, new BlockPos(0, 64, 40)), "On its very edge");
	}

	@Test
	void aTreeJustOutsideStillHangsItsCanopyIn() {
		assertTrue(ArenaTrees.reaches(ARENA, new BlockPos(-ArenaTrees.OVERHANG, 70, 20)));
		assertTrue(ArenaTrees.reaches(ARENA, new BlockPos(20, 70, 40 + ArenaTrees.OVERHANG)));
	}

	@Test
	void aTreeFarEnoughAwayGrows() {
		assertFalse(ArenaTrees.reaches(ARENA, new BlockPos(-ArenaTrees.OVERHANG - 1, 70, 20)));
		assertFalse(ArenaTrees.reaches(ARENA, new BlockPos(20, 70, 41 + ArenaTrees.OVERHANG)));
	}

	@Test
	void aTreeOnTopOfABuriedDungeonGrows() {
		assertFalse(ArenaTrees.reaches(ARENA, new BlockPos(20, 81, 20)), "Rooted above its roof");
		assertTrue(ArenaTrees.reaches(ARENA, new BlockPos(20, 80, 20)), "Rooted on its top layer");
	}

	@Test
	void aTreeRootedJustUnderTheFloorStillGrowsIntoIt() {
		assertTrue(ArenaTrees.reaches(ARENA, new BlockPos(20, 64 - ArenaTrees.BELOW, 20)));
		assertFalse(ArenaTrees.reaches(ARENA, new BlockPos(20, 63 - ArenaTrees.BELOW, 20)), "Deep in a cave under it");
	}
}
