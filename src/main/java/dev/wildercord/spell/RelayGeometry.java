package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;

/** Conservative closed-cell coverage of one short ray, including grazed corners and grid boundaries. */
public final class RelayGeometry {
	private RelayGeometry() {}
	public record Cell(int x, int y, int z) {}
	public static List<Cell> cells(double ax, double ay, double az, double bx, double by, double bz) {
		double[] a = {ax, ay, az}, b = {bx, by, bz};
		for (int i = 0; i < 3; i++) if (!Double.isFinite(a[i]) || !Double.isFinite(b[i]) || Math.abs(a[i]) > 30_000_001 || Math.abs(b[i]) > 30_000_001) return List.of();
		double length = Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay)+(bz-az)*(bz-az));
		if (!Double.isFinite(length) || length > RelayRules.MAX_PATH + 1) return List.of();
		int[] lo = new int[3], hi = new int[3];
		for (int i = 0; i < 3; i++) {
			double min = Math.min(a[i], b[i]);
			lo[i] = (int)Math.floor(min) - (min == Math.floor(min) ? 1 : 0);
			hi[i] = (int)Math.floor(Math.max(a[i], b[i]));
		}
		List<Cell> cells = new ArrayList<>();
		// A length-limited segment gives a fixed worst-case search volume, independent of loaded entities/chunks.
		for (int x = lo[0]; x <= hi[0]; x++) for (int y = lo[1]; y <= hi[1]; y++) for (int z = lo[2]; z <= hi[2]; z++) {
			int[] cell = {x,y,z}; double enter = 0, leave = 1;
			for (int i = 0; i < 3 && enter <= leave; i++) {
				double delta = b[i] - a[i];
				if (delta == 0) { if (a[i] < cell[i] || a[i] > cell[i]+1) enter = 2; }
				else {
					double first = (cell[i]-a[i])/delta, second = (cell[i]+1-a[i])/delta;
					enter = Math.max(enter, Math.min(first, second)); leave = Math.min(leave, Math.max(first, second));
				}
			}
			if (enter <= leave) cells.add(new Cell(x,y,z));
		}
		return List.copyOf(cells);
	}
}
