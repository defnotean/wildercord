package dev.wildercord.cast;

/**
 * The pure rules behind the fused effects of flame and stone ({@link FusedFlame}), with no Minecraft types so they're
 * unit-tested: when Everburn's own burn falls, which enemy each of Starfire's motes seeks, how high Monolith throws,
 * and how fast the pulls draw a creature in.
 */
public final class FusedFlameRules {
	private FusedFlameRules() {}

	/**
	 * Vanilla fire burns a creature when its fire ticks left reach a multiple of 20. Everburn burns when they're this
	 * much past one: 9 ticks after the fire's own and 11 before the next, both clear of the half-second hurt cooldown
	 * (which a hit checks before it counts down for the tick), so both land.
	 */
	public static final int EVERBURN_BEAT = 11;

	/** Ticks from now until Everburn's next burn on a creature with {@code fireTicks} left (0: now). */
	public static int everburnDue(int fireTicks) {
		return Math.floorMod(fireTicks - EVERBURN_BEAT, 20);
	}

	/**
	 * Which of {@code marks} enemies each of {@code motes} motes seeks: in turn, so every enemy gets one before any
	 * gets two, and a lone enemy gets them all. -1 for every mote when there's no one to seek.
	 */
	public static int[] moteMarks(int marks, int motes) {
		int[] out = new int[Math.max(0, motes)];
		for (int i = 0; i < out.length; i++) {
			out[i] = marks <= 0 ? -1 : i % marks;
		}
		return out;
	}

	/** Monolith's throw: the upward speed (blocks a tick) that carries a creature about 3 blocks up. */
	public static final double MONOLITH_LIFT = 0.69;

	/** How high a creature thrown straight up at {@code speed} rises, under vanilla gravity (0.08) and air drag (0.98). */
	public static double rise(double speed) {
		double height = 0;
		double v = speed;
		for (int tick = 0; tick < 200 && v > 0; tick++) {
			height += v;
			v = (v - 0.08) * 0.98;
		}
		return height;
	}

	/** Hellmouth's drag, each quarter second, on a creature {@code distance} from the pit's middle. */
	public static double hellmouthDrag(double distance) {
		return Math.min(0.3, 0.06 + distance * 0.07);
	}

	/** Magnetize's draw, every fifth of a second, on a creature {@code distance} from the magnet. */
	public static double magnetDraw(double distance) {
		return Math.min(0.28, 0.05 + distance * 0.05);
	}

	/**
	 * Sinkhole's drag, every other tick, on a creature {@code distance} from the middle: quick from the edge, slowing
	 * as it nears, and always short of the middle, so nothing is carried past it.
	 */
	public static double sinkholeDrag(double distance) {
		return Math.min(0.45, Math.max(0.0, distance) * 0.3);
	}
}
