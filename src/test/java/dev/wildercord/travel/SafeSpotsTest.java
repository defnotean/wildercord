package dev.wildercord.travel;

import dev.wildercord.travel.SafeSpots.Cell;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SafeSpotsTest {
	/** A little world: open air everywhere, a flat floor at y = 63 unless told otherwise. */
	private static class World implements SafeSpots.Grid {
		final Map<Long, Cell> cells = new HashMap<>();
		int floorY = 63;
		Cell floor = Cell.FLOOR;

		World set(int x, int y, int z, Cell cell) {
			cells.put(key(x, y, z), cell);
			return this;
		}

		@Override
		public Cell cell(int x, int y, int z) {
			Cell set = cells.get(key(x, y, z));
			if (set != null) {
				return set;
			}
			return y == floorY ? floor : y < floorY ? Cell.FLOOR : Cell.OPEN;
		}

		private static long key(int x, int y, int z) {
			return ((long) x & 0xFFFFF) << 40 | ((long) y & 0xFFFFF) << 20 | ((long) z & 0xFFFFF);
		}
	}

	@Test
	void standingOnFloorInOpenAirIsSafe() {
		World world = new World();
		assertTrue(SafeSpots.standable(world, 0, 64, 0, false));
		assertTrue(SafeSpots.standable(world, 0, 64, 0, true));
		assertFalse(SafeSpots.standable(world, 0, 65, 0, false), "nothing under your feet");
		assertFalse(SafeSpots.standable(world, 0, 63, 0, false), "inside the floor");
	}

	@Test
	void headroomAndDangerMatter() {
		World world = new World().set(0, 65, 0, Cell.FLOOR);
		assertFalse(SafeSpots.standable(world, 0, 64, 0, false), "no room for your head");
		world = new World().set(0, 63, 0, Cell.DANGER);
		assertFalse(SafeSpots.standable(world, 0, 64, 0, false), "standing on magma or in lava");
		world = new World().set(0, 64, 0, Cell.DANGER);
		assertFalse(SafeSpots.standable(world, 0, 64, 0, false), "feet in fire");
	}

	@Test
	void waterAndLeavesAreOnlyRefusedWhenStrict() {
		World shallow = new World().set(0, 64, 0, Cell.WATER);
		assertTrue(SafeSpots.standable(shallow, 0, 64, 0, false), "wading is fine for a home");
		assertFalse(SafeSpots.standable(shallow, 0, 64, 0, true), "a random teleport never lands in water");
		World deep = new World().set(0, 64, 0, Cell.WATER).set(0, 65, 0, Cell.WATER);
		assertFalse(SafeSpots.standable(deep, 0, 64, 0, false), "head under water");
		World canopy = new World();
		canopy.floor = Cell.LEAVES;
		assertTrue(SafeSpots.standable(canopy, 0, 64, 0, false));
		assertFalse(SafeSpots.standable(canopy, 0, 64, 0, true), "never on top of a tree");
	}

	@Test
	void aSpotThatIsFineIsKept() {
		assertArrayEquals(new int[] {5, 64, -3}, SafeSpots.find(new World(), 5, 64, -3, false));
	}

	@Test
	void aBlockPlacedOnAHomeLandsYouBesideIt() {
		World world = new World().set(0, 64, 0, Cell.FLOOR);
		// Right beside it is just as close, and level ground: that wins over climbing on top.
		int[] found = SafeSpots.find(world, 0, 64, 0, false);
		assertNotNull(found);
		assertEquals(64, found[1]);
		assertEquals(1, Math.abs(found[0]) + Math.abs(found[2]));
	}

	@Test
	void aRemovedFloorDropsYouToTheGroundBelow() {
		World world = new World();
		world.floorY = 58;
		// Saved standing at 64, the floor now at 58: land on it, straight down.
		assertArrayEquals(new int[] {0, 59, 0}, SafeSpots.find(world, 0, 64, 0, false));
	}

	@Test
	void lavaEverywhereNearbyIsRefused() {
		World world = new World();
		world.floor = Cell.DANGER;
		for (int y = 55; y < 63; y++) {
			for (int x = -4; x <= 4; x++) {
				for (int z = -4; z <= 4; z++) {
					world.set(x, y, z, Cell.DANGER);
				}
			}
		}
		assertNull(SafeSpots.find(world, 0, 64, 0, false));
	}

	@Test
	void theWorldGetsTheLastWord() {
		World world = new World() {
			@Override
			public boolean fits(int x, int y, int z) {
				// Say something big (a boat, the world border) is in the way everywhere but x = 2.
				return x == 2;
			}
		};
		int[] found = SafeSpots.find(world, 0, 64, 0, false);
		assertNotNull(found);
		assertEquals(2, found[0]);
		assertEquals(64, found[1]);
	}
}
