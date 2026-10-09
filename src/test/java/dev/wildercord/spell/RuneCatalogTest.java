package dev.wildercord.spell;

import static dev.wildercord.spell.RuneCatalog.Use.*;
import static org.junit.jupiter.api.Assertions.*;

import dev.wildercord.spell.RuneCatalog.Filter;
import dev.wildercord.spell.RuneCatalog.Show;
import dev.wildercord.spell.RuneCatalog.Sort;
import dev.wildercord.spell.RuneCatalog.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The rune catalog: what each rune is for, and searching, filtering, sorting and paging a list of runes. */
class RuneCatalogTest {
	private static RuneDef rune(String path) {
		return Runes.get("wildercord:" + path).orElseThrow(() -> new AssertionError("no rune " + path));
	}

	/** A player who knows {@code known}, wears a Cord that holds tier 1 and 2, and is still reading Heal. */
	private static View view(Set<String> known) {
		return new View(RuneDef::name, r -> r.family().name() + " " + r.category() + " " + r.element(), RuneDef::description,
			r -> known.contains(r.path()), r -> r.tier() <= 2, r -> r.path().equals("heal"));
	}

	@Test
	void everyEffectHasAUse() {
		for (RuneDef rune : Runes.all()) {
			if (rune.family() == RuneFamily.EFFECT) {
				assertFalse(RuneCatalog.uses(rune).isEmpty(), rune.id() + " should be for something");
			}
		}
	}

	@Test
	void everyUseHasRunes() {
		for (RuneCatalog.Use use : RuneCatalog.Use.values()) {
			assertTrue(Runes.all().stream().filter(r -> RuneCatalog.uses(r).contains(use)).count() >= 10, use + " should hold a fair few runes");
		}
	}

	@Test
	void runesAreSortedByWhatTheyDo() {
		assertTrue(RuneCatalog.uses(rune("sow")).contains(FARMING));
		assertTrue(RuneCatalog.uses(rune("harvest")).contains(FARMING));
		assertTrue(RuneCatalog.uses(rune("angler_lure")).contains(FISHING));
		assertTrue(RuneCatalog.uses(rune("vein")).contains(MINING));
		assertTrue(RuneCatalog.uses(rune("break")).contains(MINING));
		assertTrue(RuneCatalog.uses(rune("polish")).contains(BUILDING));
		assertTrue(RuneCatalog.uses(rune("village_sense")).contains(EXPLORING));
		assertTrue(RuneCatalog.uses(rune("grapple")).contains(TRAVEL));
		assertTrue(RuneCatalog.uses(rune("heal")).contains(SUPPORT));
		assertTrue(RuneCatalog.uses(rune("fire")).contains(COMBAT));
		assertFalse(RuneCatalog.uses(rune("fire")).contains(FARMING), "a fighting rune isn't for farming");
		assertFalse(RuneCatalog.uses(rune("heal")).contains(COMBAT));
		// The hearth runes carry their trade.
		assertEquals(Set.of(FISHING), RuneCatalog.uses(rune("on_catch")));
		assertEquals(Set.of(MINING), RuneCatalog.uses(rune("on_mine")));
		assertTrue(RuneCatalog.uses(rune("replanting")).contains(FARMING));
		// Shapes: field shapes by what they lay out, creature pickers by who they pick, the rest reach enemies.
		assertTrue(RuneCatalog.uses(rune("furrow")).contains(FARMING));
		assertTrue(RuneCatalog.uses(rune("shaft")).contains(MINING));
		assertTrue(RuneCatalog.uses(rune("nursery")).contains(FARMING));
		assertTrue(RuneCatalog.uses(rune("bolt")).contains(COMBAT));
		// Core modifiers and links fit any spell.
		assertTrue(RuneCatalog.uses(rune("delay")).isEmpty());
		assertTrue(RuneCatalog.uses(rune("split")).isEmpty());
	}

	@Test
	void passiveFollowsThePassivesPage() {
		for (RuneDef rune : Runes.all()) {
			assertEquals(Passives.allowed(rune), RuneCatalog.uses(rune).contains(PASSIVE), rune.id());
		}
	}

	@Test
	void filtersNarrowTheList() {
		List<RuneDef> all = new ArrayList<>(Runes.all());
		Set<String> known = Set.of("fire", "heal", "sow", "vein", "bolt", "amplify", "delay", "on_catch", "angler_lure");
		View view = view(known);
		List<RuneDef> mine = RuneCatalog.select(all, Filter.NONE, view);
		assertEquals(known.size(), mine.size(), "by default only known runes are listed");
		assertEquals(all.size(), RuneCatalog.select(all, Filter.NONE.withShow(Show.ALL), view).size());

		assertEquals(List.of(rune("bolt")), RuneCatalog.select(all, Filter.NONE.withFamily(RuneFamily.SHAPE), view));
		assertEquals(List.of(rune("fire")), RuneCatalog.select(all, Filter.NONE.withElement("fire"), view));
		assertTrue(RuneCatalog.select(all, Filter.NONE.withElement("none"), view).stream().allMatch(r -> r.element().isEmpty()));
		assertEquals(List.of(rune("angler_lure"), rune("on_catch")), RuneCatalog.select(all, Filter.NONE.withUse(FISHING).withSort(Sort.NAME), view));
		assertEquals(List.of(rune("heal")), RuneCatalog.select(all, Filter.NONE.withShow(Show.READING), view));
		assertFalse(RuneCatalog.select(all, Filter.NONE.withShow(Show.READY), view).stream().anyMatch(r -> r.tier() > 2));
	}

	@Test
	void searchReadsOnlyWhatIsReadable() {
		List<RuneDef> all = List.of(rune("fire"), rune("heal"), rune("vein"));
		Set<String> known = Set.of("fire", "heal", "vein");
		View hidden = new View(RuneDef::name, r -> "", r -> "a hint", r -> known.contains(r.path()), r -> true, r -> false);
		View shown = new View(RuneDef::name, r -> "", RuneDef::description, r -> known.contains(r.path()), r -> true, r -> false);
		assertEquals(List.of(rune("vein")), RuneCatalog.select(all, Filter.NONE.withQuery("VEIN"), hidden), "names always match");
		assertTrue(RuneCatalog.select(all, Filter.NONE.withQuery("ore"), hidden).isEmpty(), "unread text is not searched");
		assertEquals(List.of(rune("vein")), RuneCatalog.select(all, Filter.NONE.withQuery("pickaxe"), shown));
		assertTrue(RuneCatalog.select(all, Filter.NONE.withQuery("vein zzzz"), shown).isEmpty(), "every word must match");
		assertTrue(RuneCatalog.select(all, Filter.NONE.withQuery("ore"), shown).isEmpty(), "short words don't search the text");
	}

	@Test
	void namesStartingWithTheSearchComeFirst() {
		List<RuneDef> all = List.of(rune("firestorm"), rune("ember"), rune("fire"));
		View view = new View(RuneDef::name, r -> "", r -> "fire", r -> true, r -> true, r -> false);
		List<RuneDef> out = RuneCatalog.select(all, Filter.NONE.withQuery("fire").withSort(Sort.NAME), view);
		assertEquals(List.of(rune("fire"), rune("firestorm"), rune("ember")), out);
	}

	@Test
	void sortsAreStable() {
		List<RuneDef> all = new ArrayList<>(Runes.all());
		View view = view(Set.of());
		for (Sort sort : Sort.values()) {
			List<RuneDef> a = RuneCatalog.select(all, Filter.NONE.withShow(Show.ALL).withSort(sort), view);
			java.util.Collections.reverse(all);
			List<RuneDef> b = RuneCatalog.select(all, Filter.NONE.withShow(Show.ALL).withSort(sort), view);
			assertEquals(a, b, sort + " should not depend on the input order");
		}
		List<RuneDef> byTier = RuneCatalog.select(all, Filter.NONE.withShow(Show.ALL).withSort(Sort.TIER), view);
		for (int i = 1; i < byTier.size(); i++) {
			assertTrue(byTier.get(i - 1).tier() <= byTier.get(i).tier());
		}
		List<RuneDef> byGroup = RuneCatalog.select(all, Filter.NONE.withShow(Show.ALL), view);
		for (int i = 1; i < byGroup.size(); i++) {
			assertTrue(byGroup.get(i - 1).family().ordinal() <= byGroup.get(i).family().ordinal());
		}
	}

	@Test
	void paging() {
		List<Integer> items = List.of(0, 1, 2, 3, 4, 5, 6);
		assertEquals(3, RuneCatalog.pages(7, 3));
		assertEquals(1, RuneCatalog.pages(0, 3), "an empty list is still one page");
		assertEquals(List.of(3, 4, 5), RuneCatalog.page(items, 1, 3));
		assertEquals(List.of(6), RuneCatalog.page(items, 2, 3));
		assertEquals(List.of(6), RuneCatalog.page(items, 99, 3), "a page past the end shows the last");
		assertEquals(List.of(0, 1, 2), RuneCatalog.page(items, -4, 3));
		assertEquals(List.of(), RuneCatalog.page(List.of(), 0, 3));
		assertEquals(5, RuneCatalog.pages(5, 0), "a page holds at least one");
		assertEquals(List.of(1), RuneCatalog.page(items, 1, 0));
	}

	@Test
	void elementsAreListedInOrder() {
		assertEquals(List.of("fire", "life", "none"), RuneCatalog.elements(List.of(rune("heal"), rune("bolt"), rune("fire"), rune("ember"))));
	}
}
