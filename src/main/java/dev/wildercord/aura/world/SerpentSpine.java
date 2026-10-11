package dev.wildercord.aura.world;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rootcoil Serpent's body as a chain of joints, head first, {@link #SEG} apart: its girth along the chain, the pose it
 * sleeps in coiled round its trunk, the path it slides down to the island, and the reared neck it fights with. Pure geometry
 * (points are {x, y, z} arrays) so the renderer and the tests share it.
 *
 * <p>Two frames are used. The <em>trunk frame</em> has its origin at the trunk's foot with +Z toward the herb (the serpent's
 * rest yaw). The <em>body frame</em> has its origin at the serpent's feet with +Z the way it faces.
 */
public final class SerpentSpine {
	private SerpentSpine() {}

	/** Joints along the body, head first, and the spacing between them: thirty blocks of serpent. */
	public static final int JOINTS = 41;
	public static final double SEG = 0.75;
	public static final double LENGTH = (JOINTS - 1) * SEG;

	/** The top of the coil, where the neck leaves the trunk, and the coil's radius and height from there to its foot. */
	static final double[] COIL_TOP = {0, 6.6, 2.7};
	static final double COIL_TOP_R = 2.7, COIL_FOOT_R = 3.4, COIL_FOOT_H = 0.7;
	/** Share of an uncoil spent lifting the neck off the trunk before the body starts to slide. */
	static final float LIFT = 0.3F;

	/** The body's half-thickness at joint {@code i}: a slim neck swelling to the mid-body and tapering to the tail's tip. */
	public static double radius(int i) {
		double s = i / (double) (JOINTS - 1);
		if (s < 0.25) return 0.42 + (0.66 - 0.42) * smooth(s / 0.25);
		if (s < 0.5) return 0.66;
		return 0.08 + (0.66 - 0.08) * (1 - smooth((s - 0.5) / 0.5));
	}

	/** The reared neck in the body frame, head to where the body meets the ground: a cobra's S. */
	public static final double[][] FIGHT_NECK = {{0, 2.5, 1.1}, {0, 1.4, 1.4}, {0, 0.55, 0.2}, {0, 0.55, -1.0}};
	/** The sleeping neck in the trunk frame, the head drooping out over the herb, down to the top of the coil. */
	static final double[][] SLEEP_NECK = {{0, 4.4, 5.2}, {0, 5.6, 5.0}, {-1.0, 6.6, 2.7}, COIL_TOP};

	/** How many joints the reared neck holds; the rest of the body trails behind its last. */
	public static int neckJoints() {
		return (int) Math.floor(length(dense(FIGHT_NECK)) / SEG) + 1;
	}

	/** The reared neck's joints in the body frame. */
	public static double[][] fightNeck() {
		return resample(dense(FIGHT_NECK), 0, neckJoints());
	}

	private static double[][] coiled;

	/** The sleeping pose in the trunk frame: the neck, then the coil spiralling down the trunk to the tail. */
	public static double[][] coiled() {
		if (coiled == null) {
			List<double[]> path = dense(SLEEP_NECK);
			double neck = length(path);
			List<double[]> coil = helix(LENGTH - neck + SEG);
			path.addAll(coil.subList(1, coil.size()));
			coiled = resample(path, 0, JOINTS);
		}
		return coiled;
	}

	/**
	 * The body in the trunk frame {@code progress} of the way through its uncoil (0 coiled, 1 landed), landing at {@code landing}
	 * (also in the trunk frame). First the neck lifts off the trunk toward the landing; then the body slides head first down
	 * a path through the landing and back up round the coil, so it never leaves the line it is moving along.
	 */
	public static double[][] uncoil(float progress, double[] landing) {
		float t = Math.max(0, Math.min(1, progress));
		List<double[]> path = slide(landing);
		double sleepNeck = length(dense(SLEEP_NECK));
		double approach = slideApproach(landing);
		double furthest = Math.max(0, approach - sleepNeck);
		if (t < LIFT) {
			double[][] from = coiled(), to = resample(path, furthest, JOINTS);
			// A straight blend of two chains pinches the joints together; laying the blend back out restores the spacing.
			return resample(List.of(blend(from, to, smooth(t / LIFT))), 0, JOINTS);
		}
		return resample(path, furthest * (1 - smooth((t - LIFT) / (1 - LIFT))), JOINTS);
	}

	/** The slide path: the reared neck at the landing, the climb back to the coil's top, and the coil. */
	static List<double[]> slide(double[] landing) {
		List<double[]> path = approach(landing);
		List<double[]> coil = helix(LENGTH + SEG);
		path.addAll(coil.subList(1, coil.size()));
		return path;
	}

	/** How far along the slide path the coil's top lies. */
	static double slideApproach(double[] landing) {
		return length(approach(landing));
	}

	/** The slide path up to the coil's top: the reared neck at the landing, then the climb back to the trunk. */
	private static List<double[]> approach(double[] landing) {
		List<double[]> path = landed(landing);
		double[] base = path.get(path.size() - 1), facing = facing(landing);
		List<double[]> climb = dense(new double[][]{base, {base[0] - facing[0] * 0.6, base[1] + 2.5, base[2] - facing[2] * 0.6},
			{COIL_TOP[0] - 1.2, COIL_TOP[1], COIL_TOP[2]}, COIL_TOP});
		path.addAll(climb.subList(1, climb.size()));
		return path;
	}

	/** The reared neck placed at the landing, facing away from the trunk, in the trunk frame. */
	static List<double[]> landed(double[] landing) {
		double[] f = facing(landing);
		List<double[]> out = new ArrayList<>();
		for (double[] p : dense(FIGHT_NECK)) out.add(new double[]{landing[0] + p[2] * f[0] + p[0] * f[2], landing[1] + p[1], landing[2] + p[2] * f[2] - p[0] * f[0]});
		return out;
	}

	/** Out from the trunk toward the landing, flat; straight toward the herb if the landing is the trunk itself. */
	static double[] facing(double[] landing) {
		double h = Math.hypot(landing[0], landing[2]);
		return h < 1e-6 ? new double[]{0, 0, 1} : new double[]{landing[0] / h, 0, landing[2] / h};
	}

	/** The coil from its top down round the trunk, {@code length} long. */
	static List<double[]> helix(double length) {
		double lo = 0.1, hi = 40;
		for (int i = 0; i < 50; i++) {
			double mid = (lo + hi) / 2;
			if (length(helixPoints(mid)) < length) lo = mid;
			else hi = mid;
		}
		return helixPoints(hi);
	}

	private static List<double[]> helixPoints(double turn) {
		List<double[]> out = new ArrayList<>();
		int steps = Math.max(16, (int) (turn * 24));
		for (int i = 0; i <= steps; i++) {
			double u = i / (double) steps, a = turn * u;
			double r = COIL_TOP_R + (COIL_FOOT_R - COIL_TOP_R) * u, h = COIL_TOP[1] + (COIL_FOOT_H - COIL_TOP[1]) * u;
			out.add(new double[]{r * Math.sin(a), h, r * Math.cos(a)});
		}
		return out;
	}

	/** A cubic Bezier through four control points, as a fine polyline. */
	public static List<double[]> dense(double[][] c) {
		List<double[]> out = new ArrayList<>();
		for (int i = 0; i <= 32; i++) {
			double t = i / 32.0, a = (1 - t) * (1 - t) * (1 - t), b = 3 * (1 - t) * (1 - t) * t, d = 3 * (1 - t) * t * t, e = t * t * t;
			out.add(new double[]{a * c[0][0] + b * c[1][0] + d * c[2][0] + e * c[3][0], a * c[0][1] + b * c[1][1] + d * c[2][1] + e * c[3][1],
				a * c[0][2] + b * c[1][2] + d * c[2][2] + e * c[3][2]});
		}
		return out;
	}

	public static double length(List<double[]> path) {
		double total = 0;
		for (int i = 1; i < path.size(); i++) total += distance(path.get(i - 1), path.get(i));
		return total;
	}

	/** {@code count} points {@link #SEG} apart along a polyline, from {@code start} along it; past its end it runs straight on. */
	public static double[][] resample(List<double[]> path, double start, int count) {
		double[][] out = new double[count][];
		int seg = 1;
		double before = 0;
		for (int i = 0; i < count; i++) {
			double want = start + i * SEG;
			while (seg < path.size() - 1 && before + distance(path.get(seg - 1), path.get(seg)) < want) {
				before += distance(path.get(seg - 1), path.get(seg));
				seg++;
			}
			double[] a = path.get(seg - 1), b = path.get(seg);
			double span = distance(a, b), t = span < 1e-9 ? 0 : (want - before) / span;
			out[i] = new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
		}
		return out;
	}

	public static double[][] blend(double[][] from, double[][] to, double t) {
		double[][] out = new double[from.length][];
		for (int i = 0; i < from.length; i++)
			out[i] = new double[]{from[i][0] + (to[i][0] - from[i][0]) * t, from[i][1] + (to[i][1] - from[i][1]) * t, from[i][2] + (to[i][2] - from[i][2]) * t};
		return out;
	}

	public static double distance(double[] a, double[] b) {
		double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
		return Math.sqrt(x * x + y * y + z * z);
	}

	/** A point in the trunk (or body) frame turned to world offsets for a yaw: +Z becomes the way that yaw faces. */
	public static double[] toWorld(double[] p, float yaw) {
		double r = Math.toRadians(yaw), fx = -Math.sin(r), fz = Math.cos(r);
		return new double[]{p[2] * fx + p[0] * fz, p[1], p[2] * fz - p[0] * fx};
	}

	/** A world offset turned into the frame a yaw faces along: the inverse of {@link #toWorld}. */
	public static double[] fromWorld(double[] w, float yaw) {
		double r = Math.toRadians(yaw), fx = -Math.sin(r), fz = Math.cos(r);
		return new double[]{w[0] * fz - w[2] * fx, w[1], w[0] * fx + w[2] * fz};
	}

	static double smooth(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}
}
