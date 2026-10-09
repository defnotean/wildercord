package dev.wildercord.spell;

import static org.junit.jupiter.api.Assertions.*;

import dev.wildercord.spell.RuneCompanions.Group;
import dev.wildercord.spell.RuneCompanions.Suggestions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** "Goes well with": every suggestion is one the spell compiler accepts, drawn only from the runes offered. */
class RuneCompanionsTest {
	private static final List<RuneDef> ALL = new ArrayList<>(Runes.all());

	private static RuneDef rune(String path) {
		return Runes.get("wildercord:" + path).orElseThrow(() -> new AssertionError("no rune " + path));
	}

	private static Optional<Group> group(Suggestions s, RuneFamily family) {
		return s.groups().stream().filter(g -> g.family() == family).findFirst();
	}

	private static List<RuneDef> every(RuneDef rune, RuneFamily family) {
		return group(RuneCompanions.suggest(rune, ALL, Integer.MAX_VALUE), family).map(Group::runes).orElse(List.of());
	}

	@Test
	void everySuggestedModifierAttachesToTheEffect() {
		int checked = 0;
		for (RuneDef effect : ALL) {
			if (effect.family() != RuneFamily.EFFECT) {
				continue;
			}
			for (RuneDef mod : every(effect, RuneFamily.MODIFIER)) {
				SpellCompiler.Compiled c = SpellCompiler.compile(List.of(Runes.TOUCH, effect, mod));
				assertEquals(1, c.attachedTo()[2], mod.name() + " on " + effect.name() + ": " + c.warnings());
				checked++;
			}
		}
		assertTrue(checked > 1000, "should check plenty of pairs, checked " + checked);
	}

	@Test
	void everySuggestedModifierAttachesToTheShape() {
		for (RuneDef shape : ALL) {
			if (shape.family() != RuneFamily.SHAPE) {
				continue;
			}
			for (RuneDef mod : every(shape, RuneFamily.MODIFIER)) {
				SpellCompiler.Compiled c = SpellCompiler.compile(List.of(shape, mod, Runes.FIRE));
				assertEquals(0, c.attachedTo()[1], mod.name() + " on " + shape.name() + ": " + c.warnings());
			}
		}
	}

	@Test
	void aModifierSuggestsWhatItFits() {
		for (RuneDef mod : ALL) {
			if (mod.family() != RuneFamily.MODIFIER) {
				continue;
			}
			for (RuneDef target : every(mod, RuneFamily.EFFECT)) {
				assertTrue(target.has(mod.needs()), mod.name() + " suggested for " + target.name());
			}
			for (RuneDef target : every(mod, RuneFamily.SHAPE)) {
				assertTrue(target.has(mod.needs()), mod.name() + " suggested for " + target.name());
			}
		}
		assertTrue(RuneCompanions.suggest(rune("frugal"), ALL, 6).anyEffect(), "Frugal fits every effect");
		assertTrue(every(rune("pierce"), RuneFamily.SHAPE).contains(rune("bolt")));
		assertFalse(every(rune("pierce"), RuneFamily.SHAPE).contains(rune("self")));
	}

	@Test
	void shapesReachWhoTheEffectIsFor() {
		List<RuneDef> forHeal = every(rune("heal"), RuneFamily.SHAPE);
		assertTrue(forHeal.contains(rune("self")));
		assertFalse(forHeal.contains(rune("bolt")), "a heal isn't thrown at enemies");
		List<RuneDef> forFire = every(rune("fire"), RuneFamily.SHAPE);
		assertTrue(forFire.contains(rune("bolt")));
		assertFalse(forFire.contains(rune("self")), "fire isn't cast on yourself");
		assertFalse(forFire.contains(rune("nursery")), "fire isn't for young animals");
		assertTrue(every(rune("sow"), RuneFamily.SHAPE).contains(rune("furrow")), "a crop row for sowing");
		assertEquals(rune("furrow"), every(rune("sow"), RuneFamily.SHAPE).getFirst(), "a shape that shares the use comes first");
	}

	@Test
	void linksShareAUse() {
		assertTrue(every(rune("angler_lure"), RuneFamily.LINK).contains(rune("on_catch")));
		assertTrue(every(rune("vein"), RuneFamily.LINK).contains(rune("on_mine")));
		assertFalse(every(rune("vein"), RuneFamily.LINK).contains(rune("on_catch")));
		assertTrue(every(rune("on_catch"), RuneFamily.EFFECT).contains(rune("angler_lure")));
		assertTrue(RuneCompanions.suggest(rune("delay"), ALL, 6).anyEffect(), "Delay fits any spell");
	}

	@Test
	void onlyRunesOfferedAreSuggested() {
		List<RuneDef> pool = List.of(rune("fire"), rune("bolt"), rune("self"), rune("amplify"), rune("pierce"));
		Suggestions s = RuneCompanions.suggest(rune("fire"), pool, 6);
		for (Group g : s.groups()) {
			assertTrue(pool.containsAll(g.runes()), g.toString());
			assertFalse(g.runes().contains(rune("fire")), "a rune doesn't go with itself");
		}
		assertEquals(List.of(rune("bolt")), group(s, RuneFamily.SHAPE).orElseThrow().runes());
		assertEquals(List.of(rune("amplify")), group(s, RuneFamily.MODIFIER).orElseThrow().runes(), "pierce goes on the shape, not the effect");
		assertTrue(RuneCompanions.suggest(rune("fire"), List.of(), 6).groups().isEmpty());
	}

	@Test
	void aLessonRuneGoesOnlyWithItsPartner() {
		Suggestions excise = RuneCompanions.suggest(Runes.EXCISE, ALL, 6);
		assertTrue(excise.fixed());
		assertEquals(List.of(Runes.BEAM), group(excise, RuneFamily.SHAPE).orElseThrow().runes());
		assertTrue(group(excise, RuneFamily.MODIFIER).isEmpty(), "nothing may be added to a lesson spell");
		assertTrue(every(Runes.BEAM, RuneFamily.EFFECT).contains(Runes.EXCISE), "Beam is Excise's partner");
		assertEquals(java.util.Set.of(Runes.HARM, Runes.FROST, Runes.SHOCK), java.util.Set.copyOf(every(Runes.RELAY, RuneFamily.EFFECT)));
		assertTrue(every(Runes.HARM, RuneFamily.SHAPE).contains(Runes.RELAY));
	}

	@Test
	void theLimitCountsTheRest() {
		Suggestions s = RuneCompanions.suggest(rune("fire"), ALL, 3);
		Group shapes = group(s, RuneFamily.SHAPE).orElseThrow();
		assertEquals(3, shapes.runes().size());
		assertEquals(every(rune("fire"), RuneFamily.SHAPE).size() - 3, shapes.more());
		assertEquals(s, RuneCompanions.suggest(rune("fire"), ALL, 3), "the same every time");
	}
}
