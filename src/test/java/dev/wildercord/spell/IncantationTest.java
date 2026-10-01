package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Every rune's syllable: made the same way every time, readable, and never shared within the roster. */
class IncantationTest {
	@Test
	void everyRuneOfTheRosterHasAReadableSyllableOfItsOwn() {
		Map<String, String> seen = new HashMap<>();
		for (RuneDef rune : Runes.all()) {
			String s = Incantation.syllable(rune.id());
			assertNotNull(s, rune.id());
			assertTrue(s.matches("[a-z]{2,5}"), rune.id() + " says \"" + s + "\"");
			assertTrue(s.matches(".*[aeiouy].*"), rune.id() + " says \"" + s + "\", which has no vowel to say");
			String before = seen.put(s, rune.id());
			assertNull(before, "\"" + s + "\" is both " + before + " and " + rune.id());
		}
		assertTrue(seen.size() >= 300, "the whole roster: " + seen.size());
	}

	@Test
	void theSameRuneAlwaysSaysTheSame() {
		for (RuneDef rune : Runes.all()) {
			assertEquals(Incantation.syllable(rune.id()), Incantation.syllable(rune.id()));
		}
		// The lexicon made again from the same roster agrees with itself, rune for rune.
		Map<String, String> again = Incantation.build(Runes.all());
		for (RuneDef rune : Runes.all()) {
			assertEquals(Incantation.syllable(rune.id()), again.get(rune.id()), rune.id());
		}
		// And the generator is a pure function of the id.
		assertEquals(Incantation.sound("ember", 3), Incantation.sound("ember", 3));
	}

	@Test
	void theBestKnownRunesHaveTheirHandTunedSounds() {
		assertEquals("vo", Incantation.syllable(Runes.BOLT.id()));
		assertEquals("ign", Incantation.syllable(Runes.FIRE.id()));
		assertEquals("hrim", Incantation.syllable(Runes.FROST.id()));
		assertEquals("zar", Incantation.syllable(Runes.SHOCK.id()));
		assertEquals("mae", Incantation.syllable(Runes.HEAL.id()));
		assertEquals("sei", Incantation.syllable(Runes.SPLIT_MOD.id()));
	}

	@Test
	void aSpellsIncantationIsItsRunesSyllablesInOrder() {
		List<String> spell = new ArrayList<>(List.of(Runes.BOLT.id(), Runes.FIRE.id(), Runes.SPLIT_MOD.id()));
		assertEquals(List.of("vo", "ign", "sei"), Incantation.of(spell));
		assertEquals("vo ign sei", Incantation.line(spell));
		assertEquals(List.of(), Incantation.of(List.of()));
	}

	@Test
	void aRuneTheRosterDoesntKnowStillGetsASyllableFromItsId() {
		String s = Incantation.syllable("someaddon:moonbeam");
		assertTrue(s.matches("[a-z]{2,5}"), s);
		assertEquals(s, Incantation.syllable("someaddon:moonbeam"));
	}
}
