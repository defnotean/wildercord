package dev.wildercord.spell;

import dev.wildercord.world.LeyLines;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/** Spell names, secret spells, spell codes, leaning, ley lines, readable spell circles and the new breakthroughs. */
class DiscoveryTest {
	@Test
	void spellsGetReadableNames() {
		assertEquals("Frost Bolt", SpellNames.auto(List.of(BOLT, FROST)));
		assertEquals("Splitting Fire Bolt", SpellNames.auto(List.of(BOLT, FIRE, SPLIT_MOD)));
		assertEquals("Swift", SpellNames.auto(List.of(SWIFT)));
		assertEquals("Stoneskin & Leap", SpellNames.auto(List.of(SELF, STONESKIN, LEAP)));
		assertEquals("Arcane Bolt › Blasting Burst", SpellNames.auto(List.of(BOLT, HARM, ON_HIT, BURST, EXPLODE)));
		for (RuneDef shape : Runes.all()) {
			if (shape.family() == RuneFamily.SHAPE && shape != TRIGGER) {
				String name = SpellNames.auto(List.of(shape, HARM, AMPLIFY, SPLIT_MOD));
				assertFalse(name.isBlank(), shape.name());
				assertTrue(name.length() <= SpellNames.MAX_LENGTH, name);
			}
		}
		assertEquals("Hello", SpellNames.clean("  Hello§c "));
		assertEquals(SpellNames.MAX_LENGTH, SpellNames.clean("x".repeat(80)).length());
	}

	@Test
	void secretsAreValidDistinctSpells() {
		Set<List<RuneDef>> seen = new HashSet<>();
		Set<String> ids = new HashSet<>();
		for (Secrets.Secret secret : Secrets.ALL) {
			assertTrue(seen.add(secret.runes()), secret.id() + " duplicates another sequence");
			assertTrue(ids.add(secret.id()), secret.id());
			SpellCompiler.Compiled compiled = SpellCompiler.compile(secret.runes());
			assertFalse(compiled.isEmpty(), secret.id());
			assertTrue(compiled.warnings().isEmpty(), secret.id() + ": " + compiled.warnings());
			assertEquals(secret, Secrets.match(secret.runes()).orElseThrow());
			assertFalse(secret.riddle().isBlank());
			for (RuneDef rune : secret.runes()) {
				assertFalse(Runes.innate(rune), secret.id() + " must be reachable without an innate rune");
			}
		}
		// Only the exact sequence counts: one rune more, less or moved is an ordinary spell.
		assertTrue(Secrets.match(List.of(BOLT, FROST, SHOCK, FROST)).isEmpty());
		assertTrue(Secrets.match(List.of(BOLT, FROST, FROST, SHOCK, AMPLIFY)).isEmpty());
		assertTrue(Secrets.match(List.of(BOLT, FROST, FROST)).isEmpty());
	}

	@Test
	void spellCodesRoundTrip() {
		List<String> ids = List.of(BOLT.id(), FROST.id(), SPLIT_MOD.id(), "otheraddon:spark");
		String code = SpellCodes.encode(ids);
		assertEquals("wc:bolt.frost.split.otheraddon~spark", code);
		assertEquals(ids, SpellCodes.decode(code));
		assertEquals("wc:bolt.frost.split.otheraddon~spark", SpellCodes.find("try this: wc:bolt.frost.split.otheraddon~spark !"));
		assertNull(SpellCodes.find("no code here"));
		assertTrue(SpellCodes.decode("wc:").isEmpty());
	}

	@Test
	void leaningNeedsAClearFavourite() {
		// Leaning follows the deepest affinity: level I at least, and a quarter ahead of the next.
		int first = PlayerAffinity.threshold(1);
		assertEquals("", Leaning.of(Map.of()));
		assertEquals("", Leaning.of(Map.of("fire", first - 1)));
		assertEquals("fire", Leaning.of(Map.of("fire", first)));
		assertEquals("fire", Leaning.of(Map.of("fire", 600, "frost", 300)));
		assertEquals("", Leaning.of(Map.of("fire", 600, "frost", 550)));
	}

	@Test
	void innateRunesAreSeparate() {
		assertEquals(10, Runes.INNATE.size());
		for (RuneDef rune : Runes.INNATE) {
			assertEquals("innate", rune.category(), rune.id());
			assertEquals(RuneFamily.EFFECT, rune.family());
		}
	}

	@Test
	void leyLinesAreThinRareAndStable() {
		long seed = LeyLines.seedOf(12345L);
		assertEquals(seed, LeyLines.seedOf(12345L));
		assertNotEquals(seed, LeyLines.seedOf(12346L));
		int on = 0;
		int samples = 0;
		for (int x = -2000; x < 2000; x += 7) {
			for (int z = -2000; z < 2000; z += 7) {
				samples++;
				double s = LeyLines.strength(seed, x, z);
				assertTrue(s >= 0 && s <= 1);
				if (s >= 0.45) {
					on++;
				}
			}
		}
		double share = on / (double) samples;
		assertTrue(share > 0.002 && share < 0.06, "share of ground on a ley line: " + share);
	}

	@Test
	void breakthroughsAskForFeats() {
		assertTrue(Circles.requirements(3).contains(new Circles.Requirement(Circles.Need.REACTIONS, 1)));
		assertTrue(Circles.requirements(5).contains(Circles.Requirement.feat(Feats.LONG_SPELL_KILL)));
		assertTrue(Circles.requirements(7).contains(Circles.Requirement.feat(Feats.RHYTHM)));
		assertTrue(Circles.requirements(8).contains(Circles.Requirement.feat(Feats.ARCHIVIST)));
		for (int n = 2; n <= Circles.MAX; n++) {
			for (Circles.Requirement r : Circles.requirements(n)) {
				if (r.need() == Circles.Need.FEAT) {
					assertFalse(Feats.feat(r.feat()).description().isEmpty(), r.feat());
				}
			}
		}
		assertEquals(400, Feats.reward("secret:sunfall"));
		assertEquals(2, Feats.count(List.of("secret:a", "feat:b", "secret:c"), "secret:"));
	}

	@Test
	void spellCirclesCanBeRead() {
		for (int n = 1; n <= SpellSigil.MAX_RUNES; n++) {
			int p = SpellSigil.points(n);
			int q = SpellSigil.step(p);
			// A real star with a point for every rune: {p/q}, never a plain polygon.
			assertTrue(p >= n, "points for " + n + " runes");
			assertTrue(q >= 2 && 2 * q < p, "{" + p + "/" + q + "} is a star");
			// Each rune on its own point, the first at the top.
			Set<Integer> used = new HashSet<>();
			for (int i = 0; i < n; i++) {
				assertTrue(used.add(SpellSigil.pointOf(i, n)), "rune " + i + " of " + n + " shares a point");
			}
			assertEquals(0, SpellSigil.pointOf(0, n));
			// Roundels never touch each other, the pattern band or the inner ring.
			float s = SpellSigil.roundel(n);
			assertTrue(2 * s < 2 * SpellSigil.STAR * Math.sin(Math.PI / p), "roundels touch at " + n + " runes");
			assertTrue(SpellSigil.STAR + s < SpellSigil.PATTERN - SpellSigil.PATTERN_HEIGHT / 2, "roundel meets the pattern band at " + n);
			assertTrue(SpellSigil.STAR - s > SpellSigil.INNER, "roundel meets the inner ring at " + n);
		}
		// The bands sit inside one another without touching.
		assertTrue(SpellSigil.SCRIPT + SpellSigil.SCRIPT_HEIGHT / 2 < SpellSigil.FRAME_INNER);
		assertTrue(SpellSigil.SCRIPT - SpellSigil.SCRIPT_HEIGHT / 2 > SpellSigil.SCRIPT_INNER - 0.01F);
		assertTrue(SpellSigil.PATTERN + SpellSigil.PATTERN_HEIGHT / 2 < SpellSigil.SCRIPT_INNER);
		assertTrue(SpellSigil.SEAL / 2 < SpellSigil.MEDALLION);
	}
}
