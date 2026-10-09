package dev.wildercord.aura.world;

/**
 * Thunder's signature: a delayed chain lightning. The Master plants three conductors (one under the challenger, one to each
 * side) and the bolt jumps rod to rod. Each strike then leaps from anyone it hits to anyone standing close to them.
 * Leave a rod's circle before it is struck, and do not crowd a struck ally. Each challenger is struck at most once.
 */
public final class ThunderChainRules {
	private ThunderChainRules() {}
	public static final int RODS = 3, TELL = 36, HOP = 6, RECOVERY = 16, WARNING_REFRESH = 4;
	public static final int END = TELL + HOP * (RODS - 1) + RECOVERY;
	public static final double COST = 26, DAMAGE = 24, ROD = 1.6, JUMP = 2.5, SPREAD = 3.5, NEAR = 2, FAR = 8;
	public static final double LOW = -.5, HIGH = 2.5;

	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == ElementalMasters.THUNDER && ElementalMasters.eligible(school, sequence, aura, now, readyAt, COST)
			&& Double.isFinite(distance) && distance >= NEAR && distance <= FAR && Double.isFinite(height) && Math.abs(height) <= 1;
	}

	public static int strikeAt(int rod) { return TELL + rod * HOP; }

	/** The rod struck on this tick, or -1. */
	public static int strikingRod(int age) {
		for (int rod = 0; rod < RODS; rod++) if (age == strikeAt(rod)) return rod;
		return -1;
	}

	/** Conductor positions (x, z): the challenger's spot, then one either side across the Master's aim (ax, az). */
	public static double[][] rods(double targetX, double targetZ, double ax, double az) {
		double length = Math.sqrt(ax * ax + az * az);
		double sx = length > 1e-6 ? -az / length : 1, sz = length > 1e-6 ? ax / length : 0;
		return new double[][] {{targetX, targetZ}, {targetX + sx * SPREAD, targetZ + sz * SPREAD}, {targetX - sx * SPREAD, targetZ - sz * SPREAD}};
	}

	public static boolean nearRod(double rodX, double rodZ, double x, double z, double height) {
		if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(height)) return false;
		double dx = x - rodX, dz = z - rodZ;
		return dx * dx + dz * dz <= ROD * ROD && height >= LOW && height <= HIGH;
	}

	/**
	 * Who one rod's bolt reaches: everyone within the rod's circle, then (breadth first) everyone within {@link #JUMP} of
	 * someone already struck. {@code spent} challengers were struck by an earlier rod: they neither take nor carry it again.
	 */
	public static boolean[] chain(double rodX, double rodZ, double[] xs, double[] zs, double[] heights, boolean[] spent) {
		int n = xs.length;
		boolean[] struck = new boolean[n];
		var queue = new java.util.ArrayDeque<Integer>();
		for (int i = 0; i < n; i++) if (!spent[i] && nearRod(rodX, rodZ, xs[i], zs[i], heights[i])) { struck[i] = true; queue.add(i); }
		while (!queue.isEmpty()) {
			int from = queue.poll();
			for (int i = 0; i < n; i++) {
				if (struck[i] || spent[i] || !Double.isFinite(xs[i]) || !Double.isFinite(zs[i]) || !Double.isFinite(heights[i])) continue;
				double dx = xs[i] - xs[from], dz = zs[i] - zs[from];
				if (dx * dx + dz * dz <= JUMP * JUMP && Math.abs(heights[i] - heights[from]) <= HIGH) { struck[i] = true; queue.add(i); }
			}
		}
		return struck;
	}
}
