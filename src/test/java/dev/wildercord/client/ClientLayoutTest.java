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

	@Test
	void subtitlesRiseOverTheSpellPanelOnlyWhenTheyMeet() {
		// 240 tall: the panel's name row at 198 (panel at 208), the lowest subtitle's backdrop down to 210.
		int top = 240 - 32 - 10;
		int lift = SpellHud.subtitleLift(240, 300, 400, top);
		assertEquals(13, lift);
		assertTrue(240 - 30 - lift < top, "risen clear of the name row");
		// Raised over an attack indicator, it rises further.
		assertEquals(37, SpellHud.subtitleLift(240, 300, 400, top - 24));
		// A narrow subtitle right of the panel, or no panel at all, stays where it is.
		assertEquals(0, SpellHud.subtitleLift(240, 400, 400, top));
		assertEquals(0, SpellHud.subtitleLift(240, 300, 400, -1));
	}

	@Test
	void theWaypointLineMovesBelowBossBarsItWouldRunUnder() {
		// Plenty of room beside the bars (from 149 on a GUI 480 across): it stays in the corner.
		assertEquals(6, WaypointHud.lineY(480, 270, 120, 2));
		assertEquals(6, WaypointHud.lineY(320, 240, 200, 0));
		// On a GUI 320 across the bars start at 69: a long line goes below them.
		assertEquals(22, WaypointHud.lineY(320, 240, 120, 1));
		assertEquals(41, WaypointHud.lineY(320, 240, 120, 2));
		// Never lower than the bars the overlay actually draws (it stops a third of the way down).
		assertEquals(WaypointHud.lineY(320, 240, 120, 4), WaypointHud.lineY(320, 240, 120, 9));
	}
}
