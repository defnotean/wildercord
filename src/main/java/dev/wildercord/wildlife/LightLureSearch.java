package dev.wildercord.wildlife;

/** A bounded local light gradient; unavailable or non-air cells are never traversed. */
final class LightLureSearch {
	static final int STEPS = 8;
	static final int MAX_READS = 1 + 6 * STEPS;
	private static final int[][] NEIGHBORS = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};

	@FunctionalInterface interface Cells {
		/** Real block light in resident air, or -1 when the cell is unavailable or not air. */
		int airLight(int x, int y, int z);
	}
	record Candidate(int x, int y, int z, int light) {}
	private LightLureSearch() {}

	static Candidate find(Cells cells, int x, int y, int z, int minimum) {
		int light = cells.airLight(x, y, z);
		if (light < 0) return null;
		Candidate current = new Candidate(x, y, z, light);
		for (int step = 0; step < STEPS; step++) {
			Candidate next = current;
			for (int[] offset : NEIGHBORS) {
				int nx = current.x + offset[0], ny = current.y + offset[1], nz = current.z + offset[2];
				if (Math.abs(nx - x) > 8 || Math.abs(ny - y) > 4 || Math.abs(nz - z) > 8) continue;
				int value = cells.airLight(nx, ny, nz);
				if (value > next.light) next = new Candidate(nx, ny, nz, value);
			}
			if (next == current) break;
			current = next;
		}
		return current.light >= minimum ? current : null;
	}
}
