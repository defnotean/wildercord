package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Reading runes: the three stages, the hints every rune shows unread, and the veiled numbers of a glimpse. */
class RuneReadingTest {
	@Test
	void progressMovesARuneThroughTheStages() {
		assertEquals(RuneReading.Stage.UNDERSTOOD, RuneReading.stage((Integer) null), "a rune nobody is reading is understood");
		assertEquals(RuneReading.Stage.UNREAD, RuneReading.stage(0));
		assertEquals(RuneReading.Stage.GLIMPSED, RuneReading.stage(RuneReading.GLIMPSED_AT));
		assertEquals(RuneReading.Stage.GLIMPSED, RuneReading.stage(RuneReading.UNDERSTOOD_AT - 1));
		assertEquals(RuneReading.Stage.UNDERSTOOD, RuneReading.stage(RuneReading.UNDERSTOOD_AT));
		// The first cast glimpses it; three casts that land understand it.
		int progress = RuneReading.add(0, RuneReading.CAST);
		assertEquals(RuneReading.Stage.GLIMPSED, RuneReading.stage(progress));
		progress = RuneReading.add(progress, RuneReading.LANDED);
		progress = RuneReading.add(progress, RuneReading.CAST + RuneReading.LANDED);
		assertEquals(RuneReading.Stage.GLIMPSED, RuneReading.stage(progress));
		progress = RuneReading.add(progress, RuneReading.CAST + RuneReading.LANDED);
		assertEquals(RuneReading.Stage.UNDERSTOOD, RuneReading.stage(progress));
		assertEquals(RuneReading.UNDERSTOOD_AT, RuneReading.add(RuneReading.UNDERSTOOD_AT, 9), "capped");
	}

	@Test
	void runesKnownBeforeReadingExistedAreUnderstood() {
		// A player from before this version knows Fire but has no reading of it: understood, never asked to read it again.
		assertEquals(RuneReading.Stage.UNDERSTOOD, RuneReading.stage(Runes.FIRE, id -> true, Map.of()));
		// One just learned is being read from nothing.
		assertEquals(RuneReading.Stage.UNREAD, RuneReading.stage(Runes.FIRE, id -> true, Map.of(Runes.FIRE.id(), 0)));
		// One not known at all only ever shows its hint (a rune item's tooltip, say).
		assertEquals(RuneReading.Stage.UNREAD, RuneReading.stage(Runes.FIRE, id -> false, Map.of()));
	}

	@Test
	void knotsAndWeavesFollowWhatsInside() {
		RuneDef knot = Runes.get(Knots.id(List.of(Runes.BOLT, Runes.FIRE), "")).orElseThrow();
		assertEquals(RuneReading.Stage.UNDERSTOOD, RuneReading.stage(knot, id -> true, Map.of(Runes.FIRE.id(), 0)), "a Knot lists its runes");
		RuneDef weave = WovenRunes.bind(Runes.FIRE, Runes.FROST);
		assertEquals(RuneReading.Stage.UNDERSTOOD, RuneReading.stage(weave, id -> true, Map.of()));
		assertEquals(RuneReading.Stage.GLIMPSED, RuneReading.stage(weave, id -> true, Map.of(Runes.FROST.id(), 2)));
		assertEquals(RuneReading.Stage.UNREAD, RuneReading.stage(weave, id -> true, Map.of(Runes.FROST.id(), 2, Runes.FIRE.id(), 0)));
		assertFalse(RuneReading.tracked(knot));
		assertFalse(RuneReading.tracked(weave));
		assertTrue(RuneReading.tracked(Runes.FIRE));
	}

	@Test
	void everyRuneHasAHintThatGivesNoNumbersAway() {
		Set<String> effectHints = new HashSet<>();
		int effects = 0;
		for (RuneDef rune : Runes.all()) {
			if (rune == Runes.TRIGGER) {
				continue;
			}
			String hint = RuneHints.hint(rune);
			assertFalse(hint.isBlank(), rune.name());
			assertTrue(hint.endsWith("."), rune.name() + ": " + hint);
			assertFalse(hint.matches(".*\\d.*"), rune.name() + "'s hint has a number in it: " + hint);
			assertNotEquals(rune.description(), hint, rune.name());
			assertTrue(hint.length() < rune.description().length() + 140, rune.name() + "'s hint runs long: " + hint);
			assertEquals(Character.toUpperCase(hint.charAt(0)), hint.charAt(0), rune.name() + ": " + hint);
			if (rune.family() == RuneFamily.EFFECT) {
				effects++;
				effectHints.add(hint);
			}
		}
		// Not samey: almost every effect reads differently from the rest.
		assertTrue(effectHints.size() >= effects * 0.85, effectHints.size() + " different hints among " + effects + " effects");
	}

	@Test
	void theWrittenHintsAreForRealRunes() {
		for (String path : RuneHints.WRITTEN.keySet()) {
			assertTrue(Runes.get("wildercord:" + path).isPresent(), path);
		}
		assertEquals(RuneHints.WRITTEN.get("fire"), RuneHints.hint(Runes.FIRE));
		assertTrue(RuneHints.hint(Runes.KINDLING).startsWith("It woke in your heart"), "innate runes say where they came from");
		assertTrue(RuneHints.hint(Runes.FIRESTORM).startsWith("Two magics run together"), "fused runes say so");
		assertTrue(RuneHints.FEEL.keySet().containsAll(Set.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood")));
	}

	@Test
	void aGlimpseVeilsEveryNumber() {
		List<RuneHints.Piece> pieces = RuneHints.glimpse("5 fire damage and sets alight for 6 seconds.");
		assertEquals(List.of(new RuneHints.Piece("5", true), new RuneHints.Piece(" fire damage and sets alight for ", false), new RuneHints.Piece("6", true),
			new RuneHints.Piece(" seconds.", false)), pieces);
		assertEquals("? damage, Speed ? for ? (?)", RuneHints.glimpseText("12 damage, Speed III for 2.5 (40%)", "?"));
		for (RuneDef rune : Runes.all()) {
			String glimpse = RuneHints.glimpseText(rune.description(), "?");
			assertFalse(glimpse.matches(".*\\d.*"), rune.name() + ": " + glimpse);
			StringBuilder whole = new StringBuilder();
			RuneHints.glimpse(rune.description()).forEach(piece -> whole.append(piece.text()));
			assertEquals(rune.description(), whole.toString(), "the pieces put back together are the text: " + rune.name());
		}
	}
}
