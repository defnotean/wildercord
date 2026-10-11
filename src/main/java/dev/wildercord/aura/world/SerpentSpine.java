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
	static final double[] COIL_TOP = {0, 6.6, 3.0};
	static final double COIL_TOP_R = 3.0, COIL_FOOT_R = 3.8, COIL_FOOT_H = 0.7;
	/** Share of an uncoil spent lifting the neck off the trunk before the body starts to slide. */
	static final float LIFT = 0.3F;

	/** Its half-thickness at its thickest: broader than a player is wide. */
	public static final double GIRTH = 0.86;

	/** The body's half-thickness at joint {@code i}: a slim neck swelling to the mid-body and tapering to the tail's tip. */
	public static double radius(int i) {
		double s = i / (double) (JOINTS - 1);
		if (s < 0.25) return 0.55 + (GIRTH - 0.55) * smooth(s / 0.25);
		if (s < 0.5) return GIRTH;
		return 0.1 + (GIRTH - 0.1) * (1 - smooth((s - 0.5) / 0.5));
	}

	/** Where the reared neck meets the ground, behind its feet: every pose of the neck ends here. */
	static final double[] BASE = {0, 0.75, -1.4};
	/** The reared neck in the body frame, head to where the body meets the ground: a cobra's S. */
	public static final double[][] FIGHT_NECK = {{0, 4.3, 1.3}, {0, 2.6, 2.2}, {0, 0.9, 0.4}, BASE};
	/** The sleeping neck in the trunk frame, the head drooping out over the herb, down to the top of the coil. */
	static final double[][] SLEEP_NECK = {{0, 4.4, 5.6}, {0, 5.6, 5.3}, {-1.0, 6.6, 3.0}, COIL_TOP};

	/** How many joints the reared neck holds; the rest of the body trails behind its last. */
	public static int neckJoints() {
		return (int) Math.floor(length(dense(FIGHT_NECK)) / SEG) + 1;
	}

	/** The reared neck's joints in the body frame. */
	public static double[][] fightNeck() {
		return resample(dense(FIGHT_NECK), 0, neckJoints());
	}

	/**
	 * A pose of the reared neck: its four control points in the body frame (head first, ending at {@link #BASE}), how far the head
	 * is pitched down in degrees ({@code NaN} to look at its prey instead), and how far its jaw hangs open (0 shut, 1 wide).
	 */
	public record Pose(double[][] neck, double pitch, double jaw) {}

	static final Pose WATCHING = new Pose(FIGHT_NECK, Double.NaN, 0.12);
	/** A bite: drawn back and up with the jaws gaping, then snapped forward and down at whoever is in front. */
	static final Pose DRAWN = new Pose(new double[][]{{0, 4.6, -0.3}, {0, 3.4, 0.9}, {0, 1.0, 0.9}, BASE}, 10, 0.75);
	static final Pose BITE = new Pose(new double[][]{{0, 1.7, 4.3}, {0, 3.4, 2.4}, {0, 1.1, 0.2}, BASE}, 45, 0.05);
	/** A lunge: wound back low like a spring, then flung out flat along the ground. */
	static final Pose WOUND = new Pose(new double[][]{{0, 2.4, -0.6}, {0, 3.6, 1.6}, {0, 0.9, 0.9}, BASE}, 0, 0.3);
	static final Pose FLUNG = new Pose(new double[][]{{0, 1.3, 4.4}, {0, 1.7, 2.6}, {0, 0.8, 0.8}, BASE}, 20, 0.85);
	/** The grab: its head swings wide to one side while the coils open; then it looms over whoever it holds. */
	static final Pose GRAB = new Pose(new double[][]{{1.4, 2.2, 2.6}, {1.2, 3.2, 1.4}, {0, 1.0, 0.6}, BASE}, 30, 0.4);
	static final Pose LOOM = new Pose(new double[][]{{0, 3.5, 2.6}, {0, 3.8, 0.4}, {0, 1.0, -0.2}, BASE}, 50, 0.2);
	/** Slipping a blow: the neck ducks low and aside. */
	static final Pose DUCK = new Pose(new double[][]{{-0.8, 3.0, 1.8}, {-0.6, 2.2, 2.2}, {0, 0.9, 0.4}, BASE}, 0, 0.12);
	/** Spent: reared high and leaning back, the pale belly bare, panting. */
	static final Pose SPENT = new Pose(new double[][]{{0, 5.1, 0.3}, {0, 3.9, 1.0}, {0, 1.1, 0.7}, BASE}, -25, 0.35);
	/** The dive: it rears to its full height and hisses, then pitches nose first into the water. */
	static final Pose TOWERING = new Pose(new double[][]{{0, 5.3, 0.6}, {0, 3.6, 1.6}, {0, 1.0, 0.6}, BASE}, -30, 0.9);
	static final Pose PLUNGE = new Pose(new double[][]{{0, 1.0, 3.6}, {0, 2.4, 2.4}, {0, 1.0, 0.6}, BASE}, 60, 0.2);
	/** Under the water, stretched out swimming. */
	static final Pose SWIMMING = new Pose(new double[][]{{0, 0.7, 4.9}, {0, 0.7, 3.0}, {0, 0.75, 1.0}, BASE}, 0, 0.05);
	/** The slam: braced high while the tail comes over, then thrown forward as it lands. */
	static final Pose BRACED = new Pose(new double[][]{{0, 4.9, -0.1}, {0, 3.7, 1.0}, {0, 1.0, 0.8}, BASE}, -10, 0.5);
	static final Pose THROWN = new Pose(new double[][]{{0, 3.6, 2.2}, {0, 2.4, 2.4}, {0, 0.9, 0.4}, BASE}, 25, 0.6);

	/**
	 * The neck's pose {@code el} ticks into {@code move}, whose tell is {@code tell} ticks. These are targets: the renderer eases
	 * toward each, fast enough that a bite still lands as the blow does.
	 */
	public static Pose pose(int move, float el, int tell) {
		return switch (move) {
			// The snap begins a tick before the bite lands, so the jaws meet as the damage does.
			case SerpentRules.STRIKE -> el < tell - 1 ? DRAWN : BITE;
			case SerpentRules.LUNGE -> el <= SerpentRules.LUNGE_TELL ? WOUND : FLUNG;
			case SerpentRules.CONSTRICT -> GRAB;
			case SerpentRules.SQUEEZE -> LOOM;
			case SerpentRules.SIDESTEP -> DUCK;
			case SerpentRules.RECOVER -> SPENT;
			case SerpentRules.DIVE -> el < SerpentRules.DIVE_TICKS / 2F ? TOWERING : PLUNGE;
			case SerpentRules.SUBMERGED -> SWIMMING;
			// Out of the water it climbs nose up, then comes down on its prey.
			case SerpentRules.BURST -> new Pose(FLUNG.neck(), -30 + 60 * Math.min(1, el / SerpentRules.BURST_TICKS), 0.9);
			case SerpentRules.TAIL_SLAM -> el < tell ? BRACED : THROWN;
			default -> WATCHING;
		};
	}

	/** The pose's neck as {@link #neckJoints()} joints in the body frame. */
	public static double[][] neck(double[][] controls) {
		return resample(dense(controls), 0, neckJoints());
	}

	/** Joints from the neck's base on that the squeeze wraps round its prey. */
	public static final int WRAP = 22;
	/** The wrap's turns round its prey, its radius, and the heights its coils climb between. */
	static final double WRAP_TURNS = 1.6, WRAP_R = 1.45, WRAP_LOW = 0.55, WRAP_HIGH = 2.3;
	/** Where it holds its prey, ahead of its feet. */
	public static final double GRIP = 2.2;
	private static double[][] wrap;

	/** The coils wound round whoever it holds at {@link #GRIP}, in the body frame: from the neck's base round and up its prey. */
	public static double[][] wrap() {
		if (wrap == null) {
			List<double[]> path = new ArrayList<>();
			path.add(BASE.clone());
			int steps = 96;
			for (int i = 0; i <= steps; i++) {
				double u = i / (double) steps, a = Math.PI + WRAP_TURNS * 2 * Math.PI * u;
				path.add(new double[]{WRAP_R * Math.sin(a), WRAP_LOW + (WRAP_HIGH - WRAP_LOW) * u, GRIP + WRAP_R * Math.cos(a)});
			}
			wrap = resample(path, 0, WRAP + 1);
		}
		return wrap;
	}

	/** The slam's body: it gathers into a loop on the ground behind and beside it, ending where the tail leaves the ground. */
	static final double[][] GATHERED = {BASE, {0.4, 0.7, -6.6}, {7.2, 0.7, -6.6}, {5.6, 0.7, 0.2}};
	/** The tail lying on, lifted high behind it, and brought down in front of it, each from where it leaves the ground. */
	static final double[][] TAIL_LYING = {{5.4, 0.7, 2.6}, {4.4, 0.7, 4.6}, {3.0, 0.7, 6.6}},
		TAIL_LIFTED = {{5.8, 4.0, -0.2}, {5.4, 7.5, -1.6}, {4.4, 9.5, -3.2}},
		TAIL_DOWN = {{4.8, 5.5, 1.4}, {1.6, 3.4, 1.6}, {0.7, 0.3, 3.4}};
	/** Share of the slam's tell spent lifting the tail; the rest swings it over. */
	static final double TAIL_LIFT = 0.65;

	/**
	 * The body behind the neck during a slam, {@code el} ticks into a tell of {@code tell}, in the body frame: from {@link #BASE} to
	 * the tail's tip, as {@link #JOINTS} - {@link #neckJoints()} + 1 joints. It gathers into a loop beside itself, lifts its tail
	 * high behind, then brings it over and down so it strikes the ground {@link SerpentRules#SLAM_AHEAD} in front of its feet as
	 * the shock lands; what tail is left lies along the ground beyond.
	 */
	public static double[][] slam(float el, int tell) {
		double p = Math.max(0, el / (double) tell);
		double lift = smooth(p / TAIL_LIFT), swing = smooth((p - TAIL_LIFT) / (1 - TAIL_LIFT));
		double[][] c = new double[4][];
		c[0] = GATHERED[3];
		for (int k = 0; k < 3; k++) {
			double[] up = lerp(TAIL_LYING[k], TAIL_LIFTED[k], lift), over = lerp(up, TAIL_DOWN[k], swing);
			// It comes over in an arc, not straight through its own body.
			over[1] += Math.sin(Math.PI * swing) * (k + 1) * 0.6;
			c[k + 1] = over;
		}
		List<double[]> path = dense(GATHERED);
		List<double[]> tail = dense(c);
		path.addAll(tail.subList(1, tail.size()));
		// Brought down, the rest of the tail runs on along the ground instead of into it.
		if (swing > 0.5) {
			double[] tip = c[3];
			path.add(new double[]{tip[0] * 0.5, tip[1], tip[2] + 8});
		}
		return resample(path, 0, JOINTS - neckJoints() + 1);
	}

	private static double[] lerp(double[] a, double[] b, double t) {
		return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
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
		List<double[]> climb = dense(new double[][]{base, {base[0] - facing[0] * 1.2, base[1] + 1.4, base[2] - facing[2] * 1.2},
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
