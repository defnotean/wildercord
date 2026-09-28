package dev.wildercord.familiar;

import dev.wildercord.familiar.WispRules.Outcome;
import dev.wildercord.familiar.WispRules.Step;
import dev.wildercord.familiar.WispRules.Taming;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WispRulesTest {
	private static final String ALEX = "alex";
	private static final String SAM = "sam";

	@Test
	void threeOfferingsOfItsOwnElementBondIt() {
		Step one = WispRules.offer(Taming.NONE, ALEX, "fire", "fire", 100);
		assertEquals(Outcome.PROGRESS, one.outcome());
		assertEquals(1, one.taming().hits());
		Step two = WispRules.offer(one.taming(), ALEX, "fire", "fire", 140);
		assertEquals(Outcome.PROGRESS, two.outcome());
		assertEquals(2, two.taming().hits());
		Step three = WispRules.offer(two.taming(), ALEX, "fire", "fire", 180);
		assertEquals(Outcome.BONDED, three.outcome());
		assertEquals(WispRules.TAMING_HITS, three.taming().hits());
	}

	@Test
	void aFullLanternTakesNoMore() {
		assertTrue(WispRules.canBond(0));
		assertTrue(WispRules.canBond(WispRules.MAX_BONDS - 1));
		assertFalse(WispRules.canBond(WispRules.MAX_BONDS), "at the cap, a wisp takes no offering (and no food is eaten)");
	}

	@Test
	void oneCastCountsOnce() {
		Step one = WispRules.offer(Taming.NONE, ALEX, "frost", "frost", 100);
		// A Zone pulsing, or two effects of one spell, a few ticks apart.
		Step again = WispRules.offer(one.taming(), ALEX, "frost", "frost", 100 + WispRules.MIN_GAP_TICKS - 1);
		assertEquals(Outcome.IGNORED, again.outcome());
		assertEquals(1, again.taming().hits());
		Step later = WispRules.offer(again.taming(), ALEX, "frost", "frost", 100 + WispRules.MIN_GAP_TICKS);
		assertEquals(2, later.taming().hits());
	}

	@Test
	void theWrongElementSpooksItAndItForgets() {
		Step one = WispRules.offer(Taming.NONE, ALEX, "storm", "storm", 0);
		Step two = WispRules.offer(one.taming(), ALEX, "storm", "storm", 40);
		Step wrong = WispRules.offer(two.taming(), ALEX, "fire", "storm", 80);
		assertEquals(Outcome.SPOOKED, wrong.outcome());
		assertEquals(0, wrong.taming().hits());
		assertEquals(1, WispRules.offer(wrong.taming(), ALEX, "storm", "storm", 120).taming().hits());
	}

	@Test
	void anotherPlayerStartsOverAndAMinuteIsForgotten() {
		Step alex = WispRules.offer(WispRules.offer(Taming.NONE, ALEX, "life", "life", 0).taming(), ALEX, "life", "life", 40);
		assertEquals(2, alex.taming().hits());
		Step sam = WispRules.offer(alex.taming(), SAM, "life", "life", 60);
		assertEquals(1, sam.taming().hits());
		assertEquals(SAM, sam.taming().player());
		Step late = WispRules.offer(alex.taming(), ALEX, "life", "life", 40 + WispRules.FORGET_TICKS + 1);
		assertEquals(1, late.taming().hits());
		assertTrue(WispRules.forgotten(alex.taming(), 40 + WispRules.FORGET_TICKS + 1));
		assertFalse(WispRules.forgotten(alex.taming(), 40 + WispRules.FORGET_TICKS));
	}

	@Test
	void spellsWithNoElementDoNothing() {
		Step none = WispRules.offer(Taming.NONE, ALEX, "", "void", 10);
		assertEquals(Outcome.IGNORED, none.outcome());
		assertEquals(Taming.NONE, none.taming());
	}

	@Test
	void everyElementHasItsOwnFood() {
		assertEquals(8, WispRules.ELEMENTS.size());
		for (String element : WispRules.ELEMENTS) {
			String food = WispRules.food(element);
			assertFalse(food.isEmpty(), element);
			assertEquals(element, WispRules.foodElement(food));
		}
		assertEquals("", WispRules.foodElement("minecraft:dirt"));
	}

	@Test
	void levelsComeSlowly() {
		assertEquals(1, WispRules.level(0));
		assertEquals(1, WispRules.level(39));
		assertEquals(2, WispRules.level(40));
		assertEquals(2, WispRules.level(149));
		assertEquals(3, WispRules.level(150));
		assertEquals(3, WispRules.level(100000));
		assertEquals(40, WispRules.nextLevelAt(0));
		assertEquals(150, WispRules.nextLevelAt(40));
		assertEquals(-1, WispRules.nextLevelAt(150));
		assertEquals(1, WispRules.killXp(false));
		assertEquals(20, WispRules.killXp(true));
	}

	@Test
	void higherLevelsHelpMoreAndSooner() {
		assertEquals(0.10F, WispRules.regenBonus(1), 1e-6);
		assertEquals(0.20F, WispRules.regenBonus(3), 1e-6);
		for (int level = 1; level < WispRules.MAX_LEVEL; level++) {
			assertTrue(WispRules.regenBonus(level + 1) > WispRules.regenBonus(level));
			assertTrue(WispRules.helpInterval(level + 1) < WispRules.helpInterval(level));
			assertTrue(WispRules.helpPower(level + 1) > WispRules.helpPower(level));
			assertTrue(WispRules.regenBonus(level) >= 0.10F && WispRules.regenBonus(level) <= 0.20F);
		}
		// Out of range levels are clamped rather than thrown.
		assertEquals(WispRules.helpInterval(1), WispRules.helpInterval(0));
		assertEquals(WispRules.helpInterval(3), WispRules.helpInterval(9));
	}

	@Test
	void theMenagerieNeedsEveryElement() {
		assertFalse(WispRules.menagerie(List.of("fire", "frost")));
		assertTrue(WispRules.menagerie(WispRules.ELEMENTS));
		assertTrue(WispRules.menagerie(List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "fire")));
	}
}
