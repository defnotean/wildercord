package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Keeps the entire public rune roster usable as the catalogue grows. */
class EveryRuneCompilationTest {
	@Test
	void everyRuneCanBeResolvedAndUsedInItsRole() {
		List<String> failures = new ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			try {
				assertSame(rune, Runes.get(rune.id()).orElseThrow());
				List<RuneDef> spell = switch (rune.family()) {
					case SHAPE -> List.of(rune, Runes.HARM);
					case EFFECT -> List.of(Runes.SELF, rune);
					case LINK -> List.of(Runes.SELF, Runes.HEAL, rune, Runes.SELF, Runes.HARM);
					case MODIFIER -> modifierSpell(rune);
					case KNOT -> throw new AssertionError("A dynamic Knot should not be in the static roster");
				};
				SpellCompiler.Compiled compiled = SpellCompiler.compile(spell);
				assertFalse(compiled.isEmpty());
				assertFalse(compiled.lines().isEmpty());
				assertTrue(Double.isFinite(compiled.cost()) && compiled.cost() >= 0);
				if (rune.family() == RuneFamily.MODIFIER) {
					assertTrue(compiled.attachedTo()[spell.indexOf(rune)] >= 0, "modifier did not attach");
				}
			} catch (Throwable failure) {
				failures.add(rune.id() + ": " + failure);
			}
		}
		assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
	}

	private static List<RuneDef> modifierSpell(RuneDef modifier) {
		RuneDef host = Runes.all().stream()
			.filter(r -> r.family() == RuneFamily.SHAPE || r.family() == RuneFamily.EFFECT)
			.filter(r -> r.has(modifier.needs()))
			.findFirst().orElseThrow(() -> new AssertionError("No host for " + modifier.id()));
		return host.family() == RuneFamily.SHAPE
			? List.of(host, modifier, Runes.HARM)
			: List.of(Runes.SELF, host, modifier);
	}

	@Test
	void previouslyUncoveredModifiersAndLinksHaveTheirPromisedPlans() {
		SpellCompiler.Compiled broad = SpellCompiler.compile(List.of(Runes.BURST, Runes.HARM));
		SpellCompiler.Compiled focused = SpellCompiler.compile(List.of(Runes.BURST, Runes.FOCUS_MOD, Runes.HARM));
		assertEquals(0, focused.attachedTo()[1]);
		assertEquals(SpellNumbers.groupPower(broad.root().groups.getFirst()) * 1.5,
			SpellNumbers.groupPower(focused.root().groups.getFirst()), 1e-9);
		assertEquals(SpellNumbers.burstRadius(broad.root().groups.getFirst()) * 0.5,
			SpellNumbers.burstRadius(focused.root().groups.getFirst()), 1e-9);

		SpellCompiler.Compiled plain = SpellCompiler.compile(List.of(Runes.SELF, Runes.HARM));
		SpellCompiler.Compiled overcharged = SpellCompiler.compile(List.of(Runes.SELF, Runes.HARM, Runes.OVERCHARGE_MOD));
		assertEquals(1, overcharged.attachedTo()[2]);
		assertEquals(plain.cost() * 2.6 - Runes.SELF.cost() * 1.6, overcharged.cost(), 1e-9);
		assertEquals(SpellNumbers.power(plain.root().groups.getFirst().effects.getFirst()) * 2.5,
			SpellNumbers.power(overcharged.root().groups.getFirst().effects.getFirst()), 1e-9);

		for (RuneDef link : List.of(Runes.IF_SNEAKING, Runes.ON_LOW_HEALTH)) {
			SpellCompiler.Compiled conditional = SpellCompiler.compile(List.of(link, Runes.HEAL));
			assertSame(link, conditional.root().link.link);
			assertSame(link == Runes.IF_SNEAKING ? Runes.SELF : Runes.TRIGGER,
				conditional.root().link.next.implicitShape);
		}
	}
}
