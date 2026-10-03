package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RuneChoreographyTest {
	@Test
	void everyCastableRuneHasItsOwnAuthoredSequenceAndEmblem() {
		Set<String> roster = new HashSet<>();
		Set<RuneChoreography.Sequence> sequences = new HashSet<>();
		for (RuneDef rune : Runes.all()) {
			if (rune.family() != RuneFamily.SHAPE && rune.family() != RuneFamily.EFFECT) continue;
			roster.add(rune.path());
			RuneChoreography.Sequence sequence = RuneChoreography.of(rune);
			assertTrue(sequences.add(sequence), rune.id() + " reuses another complete animation");
			assertNotNull(RuneChoreographyTest.class.getResource(
				"/assets/wildercord/textures/particle/circle/" + rune.path() + "_mark.png"),
				rune.id() + " needs its own illustrated emblem");
		}
		assertEquals(298, roster.size());
		assertEquals(roster, RuneChoreography.all().keySet(), "scripts must be kept in sync with the rune roster");
	}

	@Test
	void familiarSpellsTellDifferentVisualStories() {
		assertEquals(new RuneChoreography.Sequence(RuneChoreography.Gesture.FLAME,
			RuneChoreography.Gesture.FORK, RuneChoreography.Gesture.BURST), RuneChoreography.of(Runes.FIRE));
		assertEquals(new RuneChoreography.Sequence(RuneChoreography.Gesture.HEART,
			RuneChoreography.Gesture.PETAL, RuneChoreography.Gesture.HALO), RuneChoreography.of(Runes.HEAL));
		assertEquals(new RuneChoreography.Sequence(RuneChoreography.Gesture.GATE,
			RuneChoreography.Gesture.STEP, RuneChoreography.Gesture.FLARE), RuneChoreography.of(Runes.BLINK));
	}

	@Test
	void addonRunesHaveASafeVisualFallback() {
		RuneDef addon = new RuneDef("example:flare", "Flare", RuneFamily.EFFECT, 1, 3, 1,
			"fire", EffectKind.HARMFUL, Set.of(), "", "A test effect", "damage");
		assertNotNull(RuneChoreography.of(addon));
	}
}
