package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * A spell's personal sigil, the pure part: a small glyph worked out from the spell's runes and its owner, the same
 * every time and on every machine, drawn at the centre of the spell's circle, in the Cord screen and on an inscribed
 * scroll. Two players threading the same runes get different sigils; the same player always gets the same one.
 *
 * <p>The glyph is strokes between points of two rings and the centre, mirrored left to right so it reads as a mark
 * someone made rather than noise, with a dot or two and sometimes a small ring. Coordinates are within the unit circle
 * (y up).</p>
 */
public final class MasterySigil {
	private MasterySigil() {}

	/** One straight stroke, from (x0, y0) to (x1, y1). */
	public record Stroke(float x0, float y0, float x1, float y1) {}

	/** A small ring of radius {@code r} around (x, y). */
	public record Dot(float x, float y, float r) {}

	/** A whole sigil. */
	public record Glyph(List<Stroke> strokes, List<Dot> dots) {
		public Glyph {
			strokes = List.copyOf(strokes);
			dots = List.copyOf(dots);
		}
	}

	/** The inner and outer rings the strokes run between, as fractions of the sigil's radius. */
	public static final float INNER = 0.45F;
	public static final float OUTER = 0.92F;

	/** The seed of {@code owner}'s sigil for the spell keyed {@code key} (see {@link MasteryRules#key}); never 0. */
	public static long seed(UUID owner, String key) {
		long h = MasteryRules.hash(key);
		h ^= mix(owner.getMostSignificantBits());
		h = mix(h ^ Long.rotateLeft(owner.getLeastSignificantBits(), 17));
		return h == 0 ? 1 : h;
	}

	private static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** The glyph for {@code seed}. */
	public static Glyph glyph(long seed) {
		SplittableRandom random = new SplittableRandom(seed);
		int[] counts = {5, 6, 8};
		int m = counts[random.nextInt(counts.length)];
		float[][] inner = ring(m, INNER);
		float[][] outer = ring(m, OUTER);
		// Edges on the right half (point 0 is at the top; k and m-k mirror each other), each added with its mirror.
		Set<String> seen = new LinkedHashSet<>();
		List<Stroke> strokes = new ArrayList<>();
		// A spine down the middle, so every sigil stands on something.
		switch (random.nextInt(3)) {
			case 0 -> add(strokes, seen, 0, 0, inner[0]);
			case 1 -> add(strokes, seen, inner[0], outer[0]);
			default -> {
				if (m % 2 == 0) {
					add(strokes, seen, outer[0], outer[m / 2]);
				} else {
					add(strokes, seen, 0, 0, outer[0]);
				}
			}
		}
		int half = m / 2;
		int wanted = 2 + random.nextInt(3);
		for (int tries = 0; tries < 24 && wanted > 0; tries++) {
			int k = random.nextInt(half + 1);
			int next = (k + 1) % m;
			float[] a;
			float[] b;
			switch (random.nextInt(5)) {
				case 0 -> {
					a = new float[] {0, 0};
					b = inner[k];
				}
				case 1 -> {
					a = inner[k];
					b = outer[k];
				}
				case 2 -> {
					a = inner[k];
					b = inner[next];
				}
				case 3 -> {
					a = outer[k];
					b = outer[next];
				}
				default -> {
					a = outer[k];
					b = inner[next];
				}
			}
			int before = strokes.size();
			add(strokes, seen, a, b);
			add(strokes, seen, mirror(a), mirror(b));
			if (strokes.size() > before) {
				wanted--;
			}
		}
		List<Dot> dots = new ArrayList<>();
		int dotted = random.nextInt(3);
		for (int i = 0; i < dotted; i++) {
			float[] at = outer[1 + random.nextInt(Math.max(1, half - 1))];
			dots.add(new Dot(at[0], at[1], 0.08F));
			if (Math.abs(at[0]) > 1e-4F) {
				dots.add(new Dot(-at[0], at[1], 0.08F));
			}
		}
		if (random.nextInt(3) == 0) {
			dots.add(new Dot(0, 0, 0.2F));
		}
		return new Glyph(strokes, dots.stream().distinct().toList());
	}

	/** {@code m} points on a ring of {@code radius}, the first at the top, going round. */
	private static float[][] ring(int m, float radius) {
		float[][] points = new float[m][];
		for (int k = 0; k < m; k++) {
			double a = Math.PI / 2 - 2 * Math.PI * k / m;
			points[k] = new float[] {round((float) (Math.cos(a) * radius)), round((float) (Math.sin(a) * radius))};
		}
		return points;
	}

	private static float round(float v) {
		return Math.round(v * 10000F) / 10000F;
	}

	private static float[] mirror(float[] p) {
		return new float[] {p[0] == 0 ? 0 : -p[0], p[1]};
	}

	private static void add(List<Stroke> strokes, Set<String> seen, float x0, float y0, float[] b) {
		add(strokes, seen, new float[] {x0, y0}, b);
	}

	private static void add(List<Stroke> strokes, Set<String> seen, float[] a, float[] b) {
		if (Math.abs(a[0] - b[0]) < 1e-4F && Math.abs(a[1] - b[1]) < 1e-4F) {
			return;
		}
		String forward = a[0] + "," + a[1] + ">" + b[0] + "," + b[1];
		String back = b[0] + "," + b[1] + ">" + a[0] + "," + a[1];
		if (seen.contains(forward) || seen.contains(back)) {
			return;
		}
		seen.add(forward);
		strokes.add(new Stroke(a[0], a[1], b[0], b[1]));
	}
}
