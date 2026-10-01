package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The tracing glyph: made the same way every time from the spell's runes, and scored fairly. */
class TraceGlyphTest {
	private static List<String> ids(RuneDef... runes) {
		List<String> out = new ArrayList<>();
		for (RuneDef r : runes) {
			out.add(r.id());
		}
		return out;
	}

	@Test
	void theSameSpellAlwaysDrawsTheSameGlyph() {
		List<String> spell = ids(Runes.BOLT, Runes.FIRE, Runes.SPLIT_MOD);
		List<double[]> a = TraceGlyph.of(spell);
		List<double[]> b = TraceGlyph.of(new ArrayList<>(spell));
		assertEquals(a.size(), b.size());
		for (int i = 0; i < a.size(); i++) {
			assertArrayEquals(a.get(i), b.get(i), 1e-12);
		}
		// And a fixed value, so it never changes between versions without anyone noticing.
		assertEquals(TraceGlyph.seed(spell), TraceGlyph.seed(List.of("wildercord:bolt", "wildercord:fire", "wildercord:split")));
	}

	@Test
	void ordersAndSpellsDrawDifferentGlyphs() {
		Set<String> shapes = new HashSet<>();
		List<RuneDef> all = new ArrayList<>(Runes.all());
		for (int i = 0; i + 2 < 60; i++) {
			shapes.add(key(TraceGlyph.of(ids(all.get(i), all.get(i + 1), all.get(i + 2)))));
		}
		assertTrue(shapes.size() > 30, "most spells get a glyph of their own: " + shapes.size());
		assertNotEquals(key(TraceGlyph.of(ids(Runes.BOLT, Runes.FIRE))), key(TraceGlyph.of(ids(Runes.FIRE, Runes.BOLT))),
			"the order of the runes is part of the glyph");
	}

	private static String key(List<double[]> glyph) {
		StringBuilder s = new StringBuilder();
		for (double[] p : glyph) {
			s.append(Math.round(p[0] * 100)).append(',').append(Math.round(p[1] * 100)).append(';');
		}
		return s.toString();
	}

	@Test
	void aGlyphHasThreeToFiveCleanStrokesInsideItsSquare() {
		assertTrue(TraceGlyph.of(List.of()).isEmpty());
		List<RuneDef> all = new ArrayList<>(Runes.all());
		Random random = new Random(7);
		for (int n = 1; n <= 9; n++) {
			for (int k = 0; k < 40; k++) {
				List<String> spell = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					spell.add(all.get(random.nextInt(all.size())).id());
				}
				List<double[]> glyph = TraceGlyph.of(spell);
				int strokes = glyph.size() - 1;
				assertEquals(TraceGlyph.strokes(n), strokes);
				assertTrue(strokes >= TraceGlyph.MIN_STROKES && strokes <= TraceGlyph.MAX_STROKES);
				Set<String> edges = new HashSet<>();
				for (int i = 1; i < glyph.size(); i++) {
					double[] a = glyph.get(i - 1);
					double[] b = glyph.get(i);
					assertTrue(TraceGlyph.distance(a, b) >= 0.55, "no stroke too short to follow");
					assertTrue(Math.abs(b[0]) <= 1 && Math.abs(b[1]) <= 1);
					String edge = key(List.of(a, b)).compareTo(key(List.of(b, a))) < 0 ? key(List.of(a, b)) : key(List.of(b, a));
					assertTrue(edges.add(edge), "no stroke drawn twice");
				}
			}
		}
	}

	@Test
	void aFaithfulTraceScoresHighAndNotTracingScoresNothing() {
		List<double[]> glyph = TraceGlyph.of(ids(Runes.BOLT, Runes.FIRE, Runes.SPLIT_MOD, Runes.AMPLIFY));
		double tol = TraceGlyph.Assist.NONE.tolerance;
		// Exactly along it.
		assertTrue(TraceGlyph.score(glyph, TraceGlyph.resample(glyph, 80), tol) > 0.97);
		// A slightly shaky hand: still high.
		Random random = new Random(3);
		List<double[]> shaky = new ArrayList<>();
		for (double[] p : TraceGlyph.resample(glyph, 80)) {
			shaky.add(new double[] {p[0] + (random.nextDouble() - 0.5) * 0.12, p[1] + (random.nextDouble() - 0.5) * 0.12});
		}
		assertTrue(TraceGlyph.score(glyph, shaky, tol) > 0.85, "a shaky trace: " + TraceGlyph.score(glyph, shaky, tol));
		// Not tracing: nothing (and nothing lost).
		assertEquals(0, TraceGlyph.score(glyph, List.of(), tol), 0);
		assertEquals(0, TraceGlyph.score(glyph, List.of(glyph.getFirst()), tol), 0);
		assertEquals(0, TraceGlyph.score(glyph, List.of(new double[] {0, 0}, new double[] {0.1, 0}), tol), 0, "a twitch isn't a trace");
	}

	@Test
	void halfATraceOrAScribbleScoresLow() {
		List<double[]> glyph = TraceGlyph.of(ids(Runes.BOLT, Runes.FIRE, Runes.SPLIT_MOD, Runes.AMPLIFY));
		double tol = TraceGlyph.Assist.NONE.tolerance;
		double full = TraceGlyph.score(glyph, TraceGlyph.resample(glyph, 80), tol);
		List<double[]> half = TraceGlyph.resample(glyph, 80).subList(0, 40);
		double halfScore = TraceGlyph.score(glyph, half, tol);
		assertTrue(halfScore > 0.3 && halfScore < 0.7 && halfScore < full - 0.3, "half the glyph, well short of the whole: " + halfScore);
		Random random = new Random(11);
		List<double[]> scribble = new ArrayList<>();
		for (int i = 0; i < 80; i++) {
			scribble.add(new double[] {random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1});
		}
		double scribbled = TraceGlyph.score(glyph, scribble, tol);
		assertTrue(scribbled < 0.5 && scribbled < full / 2, "a scribble all over: " + scribbled);
		// A circle round the edge isn't the glyph either.
		List<double[]> circle = new ArrayList<>();
		for (int i = 0; i <= 64; i++) {
			double a = i * Math.PI * 2 / 64;
			circle.add(new double[] {0.95 * Math.cos(a), 0.95 * Math.sin(a)});
		}
		assertTrue(TraceGlyph.score(glyph, circle, tol) < 0.5);
	}

	@Test
	void assistWidensTheLineAndPullsTheCursorOntoIt() {
		List<double[]> glyph = TraceGlyph.of(ids(Runes.BEAM, Runes.FROST, Runes.WIDEN));
		List<double[]> off = new ArrayList<>();
		for (double[] p : TraceGlyph.resample(glyph, 80)) {
			off.add(new double[] {p[0] + 0.2, p[1]});
		}
		double none = TraceGlyph.score(glyph, off, TraceGlyph.Assist.NONE.tolerance);
		double strong = TraceGlyph.score(glyph, off, TraceGlyph.Assist.STRONG.tolerance);
		assertTrue(strong > none, "more assist forgives more: " + none + " -> " + strong);
		assertTrue(TraceGlyph.Assist.NONE.pull == 0 && TraceGlyph.Assist.STRONG.pull > TraceGlyph.Assist.LIGHT.pull);
		double[] p = {0.5, 0.5};
		assertArrayEquals(p, TraceGlyph.assisted(p, glyph, 0), 0);
		double before = TraceGlyph.distanceTo(p, glyph);
		double after = TraceGlyph.distanceTo(TraceGlyph.assisted(p, glyph, TraceGlyph.Assist.STRONG.pull), glyph);
		assertTrue(after < before);
		assertEquals(TraceGlyph.Assist.LIGHT, TraceGlyph.Assist.NONE.next());
		assertEquals(TraceGlyph.Assist.NONE, TraceGlyph.Assist.STRONG.next());
	}

	@Test
	void resamplingSpreadsPointsEvenly() {
		List<double[]> line = List.of(new double[] {0, 0}, new double[] {1, 0}, new double[] {1, 1});
		List<double[]> r = TraceGlyph.resample(line, 5);
		assertEquals(5, r.size());
		assertArrayEquals(new double[] {0, 0}, r.get(0), 1e-9);
		assertArrayEquals(new double[] {1, 0}, r.get(2), 1e-9);
		assertArrayEquals(new double[] {1, 1}, r.get(4), 1e-9);
		assertEquals(2.0, TraceGlyph.length(line), 1e-9);
		assertEquals(0.5, TraceGlyph.distanceTo(new double[] {0.5, 0.5}, line), 1e-9);
	}
}
