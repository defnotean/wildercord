package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The glyph a caster may trace to steady a charge: a single unbroken line of three to five straight
 * strokes between nine points (eight round a ring and its centre), drawn round the crosshair while the
 * spell charges. It is worked out from the spell's runes alone, so a spell always has the same glyph
 * on every client, and a longer spell draws more strokes. It is deliberately plain: a few lines to
 * follow, not an ornament.
 *
 * <p>Points are in the glyph's own square, -1 to 1 on each axis, y up; the ring has radius
 * {@link #RING}. {@link #score} says how well a traced path followed it, 0 to 1: how much of the
 * glyph the path went along ({@code coverage}), and how much of the path stayed on the glyph
 * ({@code precision}). Pure, so the client's scoring and the tests are the same code.</p>
 */
public final class TraceGlyph {
	private TraceGlyph() {}

	/** The radius of the ring the glyph's points sit on. */
	public static final double RING = 0.8;
	/** The fewest and most strokes a glyph has. */
	public static final int MIN_STROKES = 3;
	public static final int MAX_STROKES = 5;
	/** Points the glyph and the path are each measured at. */
	private static final int SAMPLES = 48;
	/** How many times the glyph's length a path may run (shaky hands, going back over a stroke) before it counts as scribbling. */
	public static final double LENGTH_ALLOWANCE = 2.0;
	/** A path shorter than this (in glyph units) wasn't a trace at all. */
	public static final double MIN_PATH = 0.5;

	/**
	 * How much help tracing gets, from the client's settings: how far off the line still counts as on
	 * it ({@code tolerance}, glyph units), and how strongly the cursor is drawn back onto the line each
	 * time it moves ({@code pull}, the share of the way).
	 */
	public enum Assist {
		NONE(0.14, 0.0),
		LIGHT(0.2, 0.2),
		STRONG(0.28, 0.45);

		public final double tolerance;
		public final double pull;

		Assist(double tolerance, double pull) {
			this.tolerance = tolerance;
			this.pull = pull;
		}

		public Assist next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	/** The nine points: the ring's eight, clockwise from the top, then the centre. */
	private static final double[][] NODES = new double[9][];

	static {
		for (int k = 0; k < 8; k++) {
			double a = Math.PI / 2 - k * Math.PI / 4;
			NODES[k] = new double[] {RING * Math.cos(a), RING * Math.sin(a)};
		}
		NODES[8] = new double[] {0, 0};
	}

	/** How many strokes the glyph of a spell of {@code runes} runes has: one per rune, three to five. */
	public static int strokes(int runes) {
		return Math.max(MIN_STROKES, Math.min(MAX_STROKES, runes));
	}

	/** The glyph of a spell, as its corner points in order (strokes + 1 of them). Empty for no runes. */
	public static List<double[]> of(List<String> runeIds) {
		List<double[]> points = new ArrayList<>();
		if (runeIds == null || runeIds.isEmpty()) {
			return points;
		}
		long state = seed(runeIds);
		int strokes = strokes(runeIds.size());
		int at = (int) Math.floorMod(next(state), 9L);
		state = mix(state);
		points.add(NODES[at].clone());
		java.util.Set<Integer> used = new java.util.HashSet<>();
		int previous = -1;
		for (int s = 0; s < strokes; s++) {
			List<Integer> options = new ArrayList<>();
			for (int n = 0; n < 9; n++) {
				if (n == at || n == previous || used.contains(edge(at, n)) || distance(NODES[at], NODES[n]) < 0.55) {
					continue;
				}
				options.add(n);
			}
			if (options.isEmpty()) {
				// Boxed in (never with nine points and five strokes, but cheap to be sure): go back through the centre.
				options.add(at == 8 ? 0 : 8);
			}
			int to = options.get((int) Math.floorMod(next(state), (long) options.size()));
			state = mix(state);
			used.add(edge(at, to));
			previous = at;
			at = to;
			points.add(NODES[at].clone());
		}
		return points;
	}

	private static int edge(int a, int b) {
		return Math.min(a, b) * 16 + Math.max(a, b);
	}

	/** A stable 64-bit hash of the runes in order (FNV-1a over their ids). */
	static long seed(List<String> runeIds) {
		long h = 0xcbf29ce484222325L;
		for (String id : runeIds) {
			for (byte b : (id + "|").getBytes(StandardCharsets.UTF_8)) {
				h ^= b & 0xFF;
				h *= 0x100000001b3L;
			}
		}
		return mix(h);
	}

	private static long mix(long z) {
		z += 0x9e3779b97f4a7c15L;
		z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
		z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
		return z ^ (z >>> 31);
	}

	private static long next(long state) {
		return mix(state) >>> 1;
	}

	// ------------------------------------------------------------------ geometry

	static double distance(double[] a, double[] b) {
		return Math.hypot(a[0] - b[0], a[1] - b[1]);
	}

	/** The total length of a path. */
	public static double length(List<double[]> path) {
		double total = 0;
		for (int i = 1; i < path.size(); i++) {
			total += distance(path.get(i - 1), path.get(i));
		}
		return total;
	}

	/** The point on a path nearest {@code p} (the path's only point, if it has one). */
	public static double[] nearest(double[] p, List<double[]> path) {
		if (path.size() == 1) {
			return path.getFirst().clone();
		}
		double best = Double.POSITIVE_INFINITY;
		double[] found = new double[] {p[0], p[1]};
		for (int i = 1; i < path.size(); i++) {
			double[] q = nearestOnSegment(p, path.get(i - 1), path.get(i));
			double d = distance(p, q);
			if (d < best) {
				best = d;
				found = q;
			}
		}
		return found;
	}

	/** How far {@code p} is from a path. */
	public static double distanceTo(double[] p, List<double[]> path) {
		return path.isEmpty() ? Double.POSITIVE_INFINITY : distance(p, nearest(p, path));
	}

	private static double[] nearestOnSegment(double[] p, double[] a, double[] b) {
		double dx = b[0] - a[0];
		double dy = b[1] - a[1];
		double len2 = dx * dx + dy * dy;
		double t = len2 <= 1e-12 ? 0 : Math.max(0, Math.min(1, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / len2));
		return new double[] {a[0] + dx * t, a[1] + dy * t};
	}

	/** {@code n} points spread evenly along a path by length (its first point {@code n} times if it has no length). */
	public static List<double[]> resample(List<double[]> path, int n) {
		List<double[]> out = new ArrayList<>(n);
		if (path.isEmpty() || n <= 0) {
			return out;
		}
		double total = length(path);
		if (total <= 1e-9 || path.size() == 1) {
			for (int i = 0; i < n; i++) {
				out.add(path.getFirst().clone());
			}
			return out;
		}
		int seg = 1;
		double walked = 0;
		for (int i = 0; i < n; i++) {
			double want = total * i / Math.max(1, n - 1);
			while (seg < path.size() - 1 && walked + distance(path.get(seg - 1), path.get(seg)) < want) {
				walked += distance(path.get(seg - 1), path.get(seg));
				seg++;
			}
			double[] a = path.get(seg - 1);
			double[] b = path.get(seg);
			double len = distance(a, b);
			double t = len <= 1e-12 ? 0 : Math.max(0, Math.min(1, (want - walked) / len));
			out.add(new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
		}
		return out;
	}

	// ------------------------------------------------------------------ scoring

	/**
	 * How well {@code path} traced {@code glyph}, 0 to 1. Each sample of the glyph counts as covered when
	 * the path passed within {@code tolerance} of it (fading to nothing at half as far again), each sample of the
	 * path as precise when it stayed that close to the glyph; the score is the coverage, weighed down by
	 * imprecision: {@code coverage × (0.4 + 0.6 × precision)}, and held back for a path more than
	 * {@link #LENGTH_ALLOWANCE} times as long as the glyph (a scribble over everything). A path too short to
	 * be a trace scores 0, which is also what not tracing at all scores.
	 */
	public static double score(List<double[]> glyph, List<double[]> path, double tolerance) {
		if (glyph.size() < 2 || path.size() < 2 || length(path) < MIN_PATH || tolerance <= 0) {
			return 0;
		}
		List<double[]> g = resample(glyph, SAMPLES);
		List<double[]> t = resample(path, SAMPLES);
		double coverage = 0;
		for (double[] p : g) {
			coverage += soft(distanceTo(p, path), tolerance);
		}
		coverage /= g.size();
		double precision = 0;
		for (double[] p : t) {
			precision += soft(distanceTo(p, glyph), tolerance);
		}
		precision /= t.size();
		// Scribbling over everything covers the glyph too: a path far longer than the glyph is held back.
		double economy = Math.min(1, LENGTH_ALLOWANCE * length(glyph) / length(path));
		return Math.max(0, Math.min(1, coverage * (0.4 + 0.6 * precision) * economy));
	}

	private static double soft(double d, double tolerance) {
		return d <= tolerance ? 1 : Math.max(0, 1 - (d - tolerance) / (0.5 * tolerance));
	}

	/**
	 * Where a cursor at {@code p} ends up with an assist pulling it {@code pull} of the way toward the
	 * nearest point of the glyph (no pull: where it is).
	 */
	public static double[] assisted(double[] p, List<double[]> glyph, double pull) {
		if (pull <= 0 || glyph.isEmpty()) {
			return new double[] {p[0], p[1]};
		}
		double[] q = nearest(p, glyph);
		double k = Math.min(1, pull);
		return new double[] {p[0] + (q[0] - p[0]) * k, p[1] + (q[1] - p[1]) * k};
	}
}
