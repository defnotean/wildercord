package dev.wildercord.wildlife;

import org.jspecify.annotations.Nullable;

/** A bounded search of exposed column tops. No randomness, movement, or chunk requests. */
final class MoonreedSearch {
	private MoonreedSearch() {}
	static final int RADIUS = 3, DROP = 4, MAX_CANDIDATES = 49;

	record Cell(int x, int y, int z) {}

	/** Loaded means completed FULL residency, not ticket eligibility. Reads must not load or join absent data. */
	interface View {
		boolean loaded(int x, int z);
		int surfaceHeight(int x, int z);
		boolean bud(int x, int y, int z);
		boolean water(int x, int y, int z);
		boolean raining(int x, int y, int z);
	}

	static @Nullable Cell find(View world, int x, int y, int z, long time) {
		if (!WetlandRules.night(time)) return null;
		Cell oldBand = null, lowerBand = null;
		for (int dz = -RADIUS; dz <= RADIUS; dz++) {
			for (int dx = -RADIUS; dx <= RADIUS; dx++) {
				int cx = x + dx, cz = z + dz;
				if (!world.loaded(cx, cz)) continue;
				int top = world.surfaceHeight(cx, cz) - 1;
				if (top < y - DROP || top > y + 1 || !world.bud(cx, top, cz) || !moist(world, cx, top, cz)) continue;
				Cell at = new Cell(cx, top, cz);
				if (top >= y - 1) {
					if (before(at, oldBand)) oldBand = at;
				} else if (before(at, lowerBand)) lowerBand = at;
			}
			// Later Z rows cannot precede an old-band match in this completed row.
			if (oldBand != null) return oldBand;
		}
		return lowerBand;
	}

	/** betweenClosed visits X fastest, then Y, then Z; preserve its first old-band match. */
	private static boolean before(Cell at, @Nullable Cell previous) {
		return previous == null || at.z < previous.z || at.z == previous.z
			&& (at.y < previous.y || at.y == previous.y && at.x < previous.x);
	}

	/** Loaded water or rain is sufficient. Unknown water in absent neighbors is deferred, never loaded. */
	private static boolean moist(View world, int x, int y, int z) {
		for (int direction = 0; direction < 4; direction++) {
			int nx = x + (direction == 0 ? -1 : direction == 1 ? 1 : 0);
			int nz = z + (direction == 2 ? -1 : direction == 3 ? 1 : 0);
			if (!world.loaded(nx, nz)) continue;
			for (int below = 0; below <= 2; below++) if (world.water(nx, y - below, nz)) return true;
		}
		return world.raining(x, y, z);
	}
}
