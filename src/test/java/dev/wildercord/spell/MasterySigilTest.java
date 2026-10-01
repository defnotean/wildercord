package dev.wildercord.spell;

import dev.wildercord.spell.MasterySigil.Glyph;
import dev.wildercord.spell.MasterySigil.Stroke;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Personal sigils: the same for the same spell and owner, different between owners, readable, and varied. */
class MasterySigilTest {
	private static final UUID ANN = UUID.fromString("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0");
	private static final UUID BEN = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final String FIRE_BOLT = MasteryRules.key(java.util.List.of("wildercord:bolt", "wildercord:fire"));

	@Test
	void theSameSpellAndOwnerAlwaysMakeTheSameSigil() {
		long seed = MasterySigil.seed(ANN, FIRE_BOLT);
		assertEquals(seed, MasterySigil.seed(ANN, FIRE_BOLT));
		assertEquals(MasterySigil.glyph(seed), MasterySigil.glyph(seed));
		assertNotEquals(0L, seed);
		// Pinned, so a change that would redraw everyone's sigils is noticed.
		assertEquals(3926063885018608520L, seed);
		assertEquals(9, MasterySigil.glyph(seed).strokes().size());
		assertEquals(new MasterySigil.Stroke(0, 0, 0, 0.45F), MasterySigil.glyph(seed).strokes().getFirst());
	}

	@Test
	void anotherOwnerOrAnotherSpellMakesAnotherSigil() {
		assertNotEquals(MasterySigil.seed(ANN, FIRE_BOLT), MasterySigil.seed(BEN, FIRE_BOLT));
		assertNotEquals(MasterySigil.seed(ANN, FIRE_BOLT), MasterySigil.seed(ANN, MasteryRules.key(java.util.List.of("wildercord:bolt", "wildercord:frost"))));
		Set<Glyph> glyphs = new HashSet<>();
		java.util.Random random = new java.util.Random(7);
		for (int i = 0; i < 200; i++) {
			glyphs.add(MasterySigil.glyph(MasterySigil.seed(new UUID(random.nextLong(), random.nextLong()), FIRE_BOLT)));
		}
		assertTrue(glyphs.size() >= 150, "only " + glyphs.size() + " different sigils of 200");
	}

	@Test
	void everySigilIsAReadableMirroredMarkInsideItsCircle() {
		for (long seed = 1; seed <= 500; seed++) {
			Glyph glyph = MasterySigil.glyph(seed);
			assertTrue(glyph.strokes().size() >= 2 && glyph.strokes().size() <= 14, "seed " + seed + ": " + glyph.strokes().size() + " strokes");
			for (Stroke s : glyph.strokes()) {
				assertTrue(Math.hypot(s.x0(), s.y0()) <= 1.0001 && Math.hypot(s.x1(), s.y1()) <= 1.0001, "seed " + seed);
				// Mirrored left to right: every stroke has its reflection (a stroke on the middle line is its own).
				boolean mirrored = glyph.strokes().stream().anyMatch(m -> near(m.x0(), -s.x0()) && near(m.y0(), s.y0()) && near(m.x1(), -s.x1()) && near(m.y1(), s.y1())
					|| near(m.x0(), -s.x1()) && near(m.y0(), s.y1()) && near(m.x1(), -s.x0()) && near(m.y1(), s.y0()));
				assertTrue(mirrored, "seed " + seed + ": " + s + " has no mirror");
			}
			glyph.dots().forEach(d -> assertTrue(Math.hypot(d.x(), d.y()) + d.r() <= 1.05));
		}
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 1e-3F;
	}
}
