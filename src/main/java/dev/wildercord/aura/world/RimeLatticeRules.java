package dev.wildercord.aura.world;

/**
 * Rime's signature: a freezing ground lattice. Three times the Master marks the cell a challenger stands on and its four
 * neighbours (a plus of frost); twelve ticks later the marked cells freeze. Keep moving: a diagonal step, any two-cell
 * move, or a jump at the freeze clears it. Repeated pulses are capped per challenger.
 */
public final class RimeLatticeRules {
	private RimeLatticeRules() {}
	public static final int PULSES = 3, MARK = 12, TELL = 20, INTERVAL = 16, RECOVERY = 16, WARNING_REFRESH = 4;
	public static final int END = TELL + INTERVAL * (PULSES - 1) + RECOVERY;
	public static final double COST = 26, DAMAGE = 12, CAP = 30, CELL = 1.5, NEAR = 2, FAR = 7;
	public static final double LOW = -.5, HIGH = .6;

	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == ElementalMasters.RIME && ElementalMasters.eligible(school, sequence, aura, now, readyAt, COST)
			&& Double.isFinite(distance) && distance >= NEAR && distance <= FAR && Double.isFinite(height) && Math.abs(height) <= 1;
	}

	/** Ticks after admission on which pulse {@code pulse} freezes; the mark is laid {@link #MARK} ticks earlier. */
	public static int freezeAt(int pulse) { return TELL + pulse * INTERVAL; }
	public static int markAt(int pulse) { return freezeAt(pulse) - MARK; }

	/** The pulse marked on this tick, or -1. */
	public static int markingPulse(int age) {
		for (int pulse = 0; pulse < PULSES; pulse++) if (age == markAt(pulse)) return pulse;
		return -1;
	}

	/** The pulse that freezes on this tick, or -1. */
	public static int freezingPulse(int age) {
		for (int pulse = 0; pulse < PULSES; pulse++) if (age == freezeAt(pulse)) return pulse;
		return -1;
	}

	/** Grid cell index of an offset from the lattice origin. */
	public static int cell(double offset) { return (int) Math.floor(offset / CELL); }

	/** Whether cell (x, z) belongs to the plus centred on the marked cell. */
	public static boolean marked(int markX, int markZ, int x, int z) {
		return x == markX && Math.abs(z - markZ) <= 1 || z == markZ && Math.abs(x - markX) <= 1;
	}

	/** Feet at offset (x, height, z) from the origin when a plus marked at offset (markX, markZ) freezes. */
	public static boolean hits(double markX, double markZ, double x, double z, double height) {
		if (!Double.isFinite(markX) || !Double.isFinite(markZ) || !Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(height))
			return false;
		return height >= LOW && height <= HIGH && marked(cell(markX), cell(markZ), cell(x), cell(z));
	}

	/** What one freeze deals to a challenger who has already taken {@code taken} from this lattice. */
	public static double damage(double taken) {
		return Double.isFinite(taken) ? Math.max(0, Math.min(DAMAGE, CAP - Math.max(0, taken))) : 0;
	}
}
