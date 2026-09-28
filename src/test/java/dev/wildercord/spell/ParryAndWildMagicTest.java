package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/** A Shield's parry window and counter-burst, and wild magic's chance, table and rune rewrites. */
class ParryAndWildMagicTest {
	@Test
	void parryWindowIsSevenTicksBeforeTheSpellArrives() {
		assertEquals(7, Parry.WINDOW);
		assertTrue(Parry.timed(100, 100), "raised the same tick the spell arrives");
		assertTrue(Parry.timed(100, 107), "raised 7 ticks before");
		assertFalse(Parry.timed(100, 108), "raised 8 ticks before is too early");
		assertFalse(Parry.timed(100, 99), "a Shield raised after the spell arrived can't have parried it");
		assertFalse(Parry.timed(-1, 5), "never raised by hand");
	}

	@Test
	void aSpellAlreadyOnItsWayIsParriedHoweverSlow() {
		assertTrue(Parry.parries(100, 160, true), "primed: raised while the spell was already closing in");
		assertFalse(Parry.parries(100, 160, false));
		assertTrue(Parry.parries(100, 103, false));
	}

	@Test
	void counterBurstIsHalfTheSpellsWorthAndCapped() {
		assertEquals(2.0, Parry.counter(0), 1e-9);
		assertEquals(0.5 * (4 + 0.4 * 12), Parry.counter(12), 1e-9);
		assertTrue(Parry.counter(1000) <= Parry.MAX_COUNTER);
		assertEquals(Parry.counter(-5), Parry.counter(0), 1e-9);
		assertTrue(Parry.counter(20) > Parry.counter(10), "a costlier spell is answered harder");
	}

	@Test
	void onlyAnArrivingSpellCanBeParriedNotLingeringDamage() {
		assertTrue(Parry.parriable(false));
		assertFalse(Parry.parriable(true), "a burn ticking on is blocked, never parried");
	}

	@Test
	void surgeChanceGrowsWithHowFarPastYourManaItWent() {
		assertEquals(WildMagic.BASE_CHANCE, WildMagic.chance(40, 41, 100), 0.01);
		assertEquals(WildMagic.BASE_CHANCE + 0.15, WildMagic.chance(0, 100, 100), 1e-9);
		assertEquals(WildMagic.MAX_CHANCE, WildMagic.chance(0, 200, 100), 1e-9);
		assertEquals(WildMagic.MAX_CHANCE, WildMagic.chance(0, 10_000, 100), 1e-9);
		assertTrue(WildMagic.chance(10, 90, 100) > WildMagic.chance(50, 90, 100));
		assertEquals(WildMagic.BASE_CHANCE, WildMagic.chance(80, 50, 100), 1e-9, "never below the base");
	}

	@Test
	void surgeTableHasEveryOutcomeByWeight() {
		assertTrue(WildMagic.Surge.values().length >= 12);
		Set<String> ids = new HashSet<>();
		for (WildMagic.Surge surge : WildMagic.Surge.values()) {
			assertTrue(surge.weight > 0, surge.id);
			assertTrue(ids.add(surge.id), surge.id);
			assertEquals("message.wildercord.surge." + surge.id, surge.key());
		}
		// Sweeping the roll from 0 to 1 lands on each outcome in proportion to its weight.
		Map<WildMagic.Surge, Integer> counts = new EnumMap<>(WildMagic.Surge.class);
		int n = 100_000;
		for (int i = 0; i < n; i++) {
			counts.merge(WildMagic.pick(i / (double) n, true), 1, Integer::sum);
		}
		int total = 0;
		for (WildMagic.Surge surge : WildMagic.Surge.values()) {
			total += surge.weight;
		}
		for (WildMagic.Surge surge : WildMagic.Surge.values()) {
			assertEquals(surge.weight / (double) total, counts.getOrDefault(surge, 0) / (double) n, 0.002, surge.id);
		}
		assertEquals(WildMagic.Surge.values()[0], WildMagic.pick(0, true));
		assertNotNull(WildMagic.pick(1.0, true));
		assertNotNull(WildMagic.pick(-3, false));
	}

	@Test
	void secretSpellsNeverGetRewritten() {
		for (int i = 0; i < 1000; i++) {
			assertFalse(WildMagic.pick(i / 1000.0, false).rewrites);
		}
	}

	@Test
	void elementSwapKeepsTheShapeAndChangesTheElement() {
		List<String> elements = WildMagic.elements();
		assertTrue(elements.size() >= 4, elements.toString());
		assertEquals("fire", WildMagic.elementOf(List.of(BOLT, FIRE)));
		assertEquals("", WildMagic.elementOf(List.of(SELF, HEAL)));
		for (String element : elements) {
			List<RuneDef> swapped = WildMagic.swapElement(List.of(BOLT, FIRE, AMPLIFY), element, 3);
			assertEquals(3, swapped.size());
			assertEquals(BOLT, swapped.get(0));
			assertEquals(AMPLIFY, swapped.get(2));
			assertEquals(element, swapped.get(1).element());
			assertEquals(EffectKind.HARMFUL, swapped.get(1).kind());
			assertFalse(Runes.innate(swapped.get(1)));
			assertFalse(SpellCompiler.compile(swapped).isEmpty(), element);
		}
		// Helpful effects stay as they are.
		assertEquals(List.of(SELF, HEAL), WildMagic.swapElement(List.of(SELF, HEAL), "frost", 0));
	}

	@Test
	void grandWidensEveryRadiusTwice() {
		List<RuneDef> grand = WildMagic.grand(List.of(BURST, FIRE));
		assertNotNull(grand);
		assertEquals(List.of(BURST, WIDEN, WIDEN, FIRE), grand);
		SpellCompiler.Compiled compiled = SpellCompiler.compile(grand);
		assertFalse(compiled.isEmpty());
		assertTrue(compiled.warnings().isEmpty(), compiled.warnings().toString());
		assertNull(WildMagic.grand(List.of(SELF, HEAL)), "nothing to widen");
	}
}
