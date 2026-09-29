package dev.wildercord.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The client's layout arithmetic: where the spell HUD goes, and how big a tooltip may grow. */
class ClientLayoutTest {
	/** The HUD at its narrowest with a three-digit mana count: the bottom row (78) plus its margins. */
	private static final int NARROWEST = 28 + 78 + 4;
	private static final int INDICATOR = 23;
	private static final int OFFHAND = 29;

	@Test
	void hudMovesAsideWhenThereIsRoom() {
		// 1920x1080 at the automatic GUI scale (4): room beside the attack indicator.
		SpellHud.Place place = SpellHud.place(480, INDICATOR, NARROWEST);
		assertFalse(place.raised());
		assertEquals(240 + 91 + 5 + INDICATOR, place.x());
	}

	@Test
	void hudSitsOnTopOfTheIndicatorWhenOnlyTheHotbarLeavesRoom() {
		// 1280x720 or 2560x1440 at the automatic GUI scale (426 across): it used to be tucked over the indicator.
		SpellHud.Place place = SpellHud.place(426, INDICATOR, NARROWEST);
		assertTrue(place.raised());
		assertEquals(213 + 91 + 5, place.x());
		// 1366x768 (455 across) with a left-handed player's off-hand slot on the right.
		place = SpellHud.place(455, OFFHAND, NARROWEST);
		assertTrue(place.raised());
		assertEquals(227 + 91 + 5, place.x());
	}

	@Test
	void hudNeverRisesWithNothingBesideTheHotbar() {
		for (int width = 320; width <= 1000; width++) {
			SpellHud.Place place = SpellHud.place(width, 0, NARROWEST);
			assertFalse(place.raised(), "raised at " + width);
			assertEquals(width / 2 + 96, place.x());
		}
	}

	@Test
	void hudTooNarrowEvenBesideTheHotbarStaysDown() {
		// 800x600 at GUI scale 2: no room anywhere, so it's left to be tucked into the corner.
		SpellHud.Place place = SpellHud.place(400, INDICATOR, NARROWEST);
		assertFalse(place.raised());
	}

	@Test
	void tooltipsKeepAReadableWidthAndFitTheScreen() {
		assertEquals(280, Tooltips.maxWidth(960));
		assertEquals(280, Tooltips.maxWidth(427));
		assertEquals(192, Tooltips.maxWidth(200));
		for (int width = 110; width <= 2000; width++) {
			assertTrue(Tooltips.maxWidth(width) <= width - 8, "too wide for a screen " + width + " across");
		}
	}

	@Test
	void tooltipsFitTheScreenTall() {
		// 240 tall (the smallest a GUI gets): 23 lines of 10, and the frame, fit.
		assertEquals(23, Tooltips.maxLines(240));
		assertTrue(Tooltips.maxLines(240) * 10 - 2 + 8 <= 240);
		assertEquals(53, Tooltips.maxLines(540));
		assertEquals(2, Tooltips.maxLines(10));
	}
}
