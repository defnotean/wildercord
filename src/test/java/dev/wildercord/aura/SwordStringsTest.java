package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static dev.wildercord.aura.SwordString.Token.COUNTER;
import static dev.wildercord.aura.SwordString.Token.FULL;
import static dev.wildercord.aura.SwordString.Token.LEAP;
import static dev.wildercord.aura.SwordString.Token.LOW;
import static dev.wildercord.aura.SwordString.Token.RUN;
import static dev.wildercord.aura.SwordString.Token.STEP;
import static dev.wildercord.aura.SwordString.Token.SWING;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Sword strings, the pure parts: the grammar ({@link SwordString}), the windows and prices ({@link StringRules}), the reader
 * ({@link StringReader}: windows, cues, matching, which art wins, falling through, refusals, fumbles, lapses) and the registry's
 * conflict check ({@link AuraApi#conflicts}).
 */
class SwordStringsTest {
	/** An art for the reader: an id, a string and a stage. */
	private record Art(String id, SwordString string, int stage) implements StringReader.Spelled {
		static Art of(String id, String string, int stage) {
			return new Art(id, SwordString.parse(string), stage);
		}
	}

	private static final Art FIRST = Art.of("first", "swing swing low", AuraRules.GLOW);
	private static final Art SECOND = Art.of("second", "leap low", AuraRules.FLOW);
	private static final Art THIRD = Art.of("third", "counter", AuraRules.EDGE);
	private static final Art FOURTH = Art.of("fourth", "step", AuraRules.FORM);
	private static final Art FINAL = Art.of("final", "full full full low", AuraRules.SOVEREIGN);
	private static final List<Art> ALL = List.of(FIRST, SECOND, THIRD, FOURTH, FINAL);

	/** A sword: ready for a full swing 11 ticks after the last. */
	private static final int SWORD = StringRules.recover(12.5);
	private static final int W = StringRules.WINDOW;

	private static int m(SwordString.Token... kinds) {
		return SwordString.Token.marks(kinds);
	}

	// ------------------------------------------------------------------ the grammar

	@Test
	void stringsAreWrittenAndReadInTokenIds() {
		SwordString s = SwordString.parse("swing swing low");
		assertEquals(List.of(SWING, SWING, LOW), s.tokens());
		assertEquals("swing swing low", s.text());
		assertEquals(SwordString.parse("full full full low"), SwordString.parse(" Full,FULL , full  low "), "case, commas and spaces don't matter");
		assertEquals(s, SwordString.parse(s.text()), "written out, it reads back the same");
		for (SwordString.Token t : SwordString.Token.values()) {
			assertEquals(t, SwordString.Token.byId(t.id).orElseThrow());
		}
		assertThrows(IllegalArgumentException.class, () -> SwordString.parse("swing slash"), "an unknown swing");
		assertThrows(IllegalArgumentException.class, () -> SwordString.parse(""), "an empty string");
		assertThrows(IllegalArgumentException.class, () -> SwordString.parse("swing swing swing swing swing swing swing"), "seven is too many");
		assertEquals(SwordString.MAX_LENGTH, SwordString.parse("swing swing swing swing swing swing").length());
	}

	@Test
	void aSwingFitsEveryTokenItHasAMarkFor() {
		int fullLow = m(FULL, LOW);
		assertTrue(SWING.fits(fullLow) && FULL.fits(fullLow) && LOW.fits(fullLow));
		assertFalse(LEAP.fits(fullLow) || RUN.fits(fullLow) || COUNTER.fits(fullLow) || STEP.fits(fullLow));
		assertTrue(SWING.fits(0), "a plain swing fits anything, even with no marks");
		assertEquals(COUNTER, SwordString.Token.shown(m(FULL, LOW, COUNTER)), "a counter shows as a counter first");
		assertEquals(LOW, SwordString.Token.shown(m(FULL, LOW)));
		assertEquals(FULL, SwordString.Token.shown(m(FULL)));
		assertEquals(SWING, SwordString.Token.shown(m()));
		assertTrue(SWING.coveredBy(LOW) && LOW.coveredBy(LOW) && !LOW.coveredBy(FULL));
	}

	@Test
	void aStringFitsTheEndOfTheSwings() {
		SwordString first = SwordString.parse("swing swing low");
		assertTrue(first.endsIn(new int[] {m(FULL), m(), m(LOW)}));
		assertTrue(first.endsIn(new int[] {m(LEAP), m(RUN), m(FULL), m(LOW, FULL)}), "earlier swings don't matter, nor extra marks");
		assertFalse(first.endsIn(new int[] {m(), m(LOW), m()}), "the low swing must come last");
		assertFalse(first.endsIn(new int[] {m(), m(LOW)}), "too few swings");
		assertTrue(first.fits(new int[] {m(), m(), m(LOW)}));
		assertFalse(first.fits(new int[] {m(), m(), m(), m(LOW)}), "fits asks for exactly its own swings");
		assertEquals(2, first.weight());
		assertEquals(LOW, first.last());
		assertEquals(5, SwordString.parse("full full full low").weight());
		assertEquals(4, SwordString.parse("counter").weight());
	}

	@Test
	void aShorterStringPlayedOnTheWayCutsALongerOneShort() {
		SwordString two = SwordString.parse("swing swing");
		SwordString first = SwordString.parse("swing swing low");
		SwordString last = SwordString.parse("full full full low");
		assertTrue(first.cutBy(two), "swing swing goes off before the low swing comes");
		assertTrue(last.cutBy(two), "a plain swing string is played by any swings");
		assertFalse(last.cutBy(first), "full swings aren't low, so the First Art waits for the Final Art's last swing");
		assertFalse(first.cutBy(first), "a string doesn't cut itself");
		assertTrue(SwordString.parse("low swing low").cutBy(SwordString.parse("low")));
		assertTrue(SwordString.parse("full low swing").cutBy(SwordString.parse("swing low")), "it can complete in the middle");
		assertFalse(SwordString.parse("low full").cutBy(SwordString.parse("full")), "only before the last swing");
	}

	@Test
	void theFiveArtsStringsNeverGetInEachOthersWay() {
		List<SwordString> five = List.of(PlaceholderArts.FIRST_STRING, PlaceholderArts.SECOND_STRING, PlaceholderArts.THIRD_STRING,
			PlaceholderArts.FOURTH_STRING, PlaceholderArts.FINAL_STRING);
		assertEquals(5, Set.copyOf(five).size(), "five different strings");
		for (SwordString a : five) {
			for (SwordString b : five) {
				assertFalse(a != b && a.cutBy(b), "'" + b + "' cuts '" + a + "' short");
			}
		}
		assertEquals("swing swing low", PlaceholderArts.FIRST_STRING.text());
		assertEquals("leap low", PlaceholderArts.SECOND_STRING.text());
		assertEquals("counter", PlaceholderArts.THIRD_STRING.text());
		assertEquals("step", PlaceholderArts.FOURTH_STRING.text());
		assertEquals("full full full low", PlaceholderArts.FINAL_STRING.text());
	}

	// ------------------------------------------------------------------ windows

	@Test
	void aStringIsPacedToItsBlade() {
		assertEquals(11, StringRules.recover(12.5), "a sword (attack speed 1.6)");
		assertEquals(20, StringRules.recover(20 / 0.9), "an iron axe (0.9)");
		assertEquals(30, StringRules.recover(20 / 0.6), "a mace (0.6)");
		assertEquals(4, StringRules.recover(5), "a bare fist (4)");
		assertEquals(0, StringRules.recover(0));
		assertEquals(StringRules.MAX_RECOVER, StringRules.recover(1000), "a very slow weapon still keeps a string in reach");
		// At the recover tick the swing really is full, a tick sooner it isn't.
		for (double delay : new double[] {5, 10, 12.5, 16.7, 20 / 0.9, 25, 20 / 0.6}) {
			int r = StringRules.recover(delay);
			assertTrue((r + 0.5) / delay >= AuraRules.FULL_SWING - 1e-9, "full at " + r + " for " + delay);
			assertTrue(r == 0 || (r - 1 + 0.5) / delay < AuraRules.FULL_SWING, "not full at " + (r - 1) + " for " + delay);
		}
		assertEquals(W + SWORD, StringRules.window(W, SWORD));
		assertEquals(10, StringRules.windowTicks(0.5));
		assertEquals(2, StringRules.windowTicks(0.01), "held to a tenth of a second at least");
		assertEquals(40, StringRules.windowTicks(9), "and two seconds at most");
		assertEquals(3L * (W + SWORD), StringRules.span(4, W, SWORD));
		assertEquals(0L, StringRules.span(1, W, SWORD), "a one-swing string takes no time");
		assertTrue(StringRules.window(W, SWORD) <= 25, "a sword's string never asks for more than a second and a quarter between swings");
		assertTrue(StringRules.window(W, 0) >= 5, "nor less than a quarter of a second");
	}

	// ------------------------------------------------------------------ the reader

	/** A reader for a sword, every art usable unless told otherwise. */
	private static StringReader<Art> reader() {
		return new StringReader<>(W);
	}

	private static StringReader.Event swing(StringReader<Art> r, long at, int marks) {
		return r.stroke(at, marks, SWORD, ALL, a -> true);
	}

	@SuppressWarnings("unchecked")
	private static Art completed(StringReader.Event e) {
		assertInstanceOf(StringReader.Completed.class, e, "expected a completed string, got " + e);
		return (Art) ((StringReader.Completed<Art>) e).art();
	}

	@Test
	void swingSwingLowCompletesTheFirstArt() {
		StringReader<Art> r = reader();
		assertInstanceOf(StringReader.Landed.class, swing(r, 100, m(FULL)));
		assertInstanceOf(StringReader.Landed.class, swing(r, 112, m(FULL)));
		assertEquals(2, r.chain().size());
		StringReader.Event e = swing(r, 120, m(LOW));
		assertSame(FIRST, completed(e));
		assertEquals(3, ((StringReader.Completed<?>) e).strokes().size(), "its own three swings");
		assertTrue(r.chain().isEmpty(), "a completed string uses its swings up");
		assertInstanceOf(StringReader.Landed.class, swing(r, 125, m(LOW)), "the next string starts afresh");
	}

	@Test
	void eachSwingMustComeWithinItsWindowOfTheBladeBeingReady() {
		int window = StringRules.window(W, SWORD);
		StringReader<Art> r = reader();
		swing(r, 0, m(FULL));
		swing(r, window, m(FULL));
		assertSame(FIRST, completed(swing(r, 2L * window, m(LOW))), "right at the edge of each window still counts");

		r = reader();
		swing(r, 0, m(FULL));
		swing(r, window, m(FULL));
		assertNull(r.tick(2L * window), "not yet run out");
		assertInstanceOf(StringReader.Lapsed.class, r.tick(2L * window + 1), "a tick past the window it lapses");
		assertTrue(r.chain().isEmpty());
		assertNull(r.tick(2L * window + 2), "it lapses once");

		// A slower blade waits longer: an axe's full swings keep its string going where a sword's would have run out.
		int axe = StringRules.recover(20 / 0.9);
		StringReader<Art> slow = reader();
		for (int i = 0; i < 3; i++) {
			slow.stroke(i * (long) (axe + W), m(FULL), axe, ALL, a -> true);
		}
		StringReader.Event e = slow.stroke(3L * (axe + W), m(LOW), axe, ALL, a -> true);
		assertSame(FINAL, completed(e), "three full axe swings and a low one, each as soon as the axe allows plus the window");
	}

	@Test
	void aDeliberateReleaseThatComesTooLateIsAFumble() {
		int window = StringRules.window(W, SWORD);
		StringReader<Art> r = reader();
		swing(r, 0, m(FULL));
		swing(r, 10, m(FULL));
		assertInstanceOf(StringReader.Lapsed.class, r.tick(11 + window));
		StringReader.Event late = swing(r, 12 + window, m(LOW));
		assertInstanceOf(StringReader.Fumbled.class, late, "the low swing would have finished the First Art");
		assertSame(FIRST, ((StringReader.Fumbled<?>) late).art());
		assertEquals(3, ((StringReader.Fumbled<?>) late).strokes().size());
		assertTrue(r.chain().isEmpty(), "a fumble uses the late swing up");

		// Late without a tick in between (the reader sees the lapse at the swing itself): the same.
		r = reader();
		swing(r, 0, m(FULL));
		swing(r, 10, m(FULL));
		assertInstanceOf(StringReader.Fumbled.class, swing(r, 10 + window + 3, m(LOW)));

		// Much later is simply a new string.
		r = reader();
		swing(r, 0, m(FULL));
		swing(r, 10, m(FULL));
		assertInstanceOf(StringReader.Landed.class, swing(r, 10 + window + StringRules.LATE_GRACE + 1, m(LOW)));

		// A late plain swing isn't a fumble: nothing deliberate was meant.
		Art plain = Art.of("plain", "full full swing", AuraRules.GLOW);
		r = reader();
		r.stroke(0, m(FULL), SWORD, List.of(plain), a -> true);
		r.stroke(12, m(FULL), SWORD, List.of(plain), a -> true);
		assertInstanceOf(StringReader.Landed.class, r.stroke(13 + window, m(), SWORD, List.of(plain), a -> true));
	}

	@Test
	void theStringThatAsksTheMostWins() {
		// A leaping swing then a low one, after another swing: both the First and the Second Art fit; the Second asks more.
		StringReader<Art> r = reader();
		swing(r, 0, m(FULL));
		swing(r, 8, m(LEAP, FULL));
		assertSame(SECOND, completed(swing(r, 16, m(LOW))));
		// Three full swings and a low one: the First Art fits its end too, but the Final Art asks more.
		r = reader();
		swing(r, 0, m(FULL));
		swing(r, 12, m(FULL));
		swing(r, 24, m(FULL));
		assertSame(FINAL, completed(swing(r, 30, m(LOW))));
		// A counter is a low swing too (the guard holds while sneaking): the counter, the heavier release, wins.
		r = reader();
		swing(r, 0, m(FULL));
		swing(r, 12, m(FULL));
		r.cue(StringReader.Cue.GUARD, 20);
		assertSame(THIRD, completed(swing(r, 24, m(LOW, FULL))));
		// The tie-breaks, in order: the last token's weight, the whole weight, the length, the stage, then the first given.
		Art a = Art.of("a", "swing low", AuraRules.GLOW);
		Art b = Art.of("b", "full low", AuraRules.GLOW);
		Art c = Art.of("c", "swing swing low", AuraRules.GLOW);
		Art d = Art.of("d", "swing swing low", AuraRules.FORM);
		Art e = Art.of("e", "swing swing low", AuraRules.FORM);
		assertSame(b, StringReader.best(List.of(a, b)), "heavier string");
		assertSame(c, StringReader.best(List.of(a, c)), "longer");
		assertSame(d, StringReader.best(List.of(c, d)), "higher stage");
		assertSame(d, StringReader.best(List.of(d, e)), "the first given");
		assertSame(THIRD, StringReader.best(List.of(FINAL, THIRD)), "a counter's release beats any low swing, however long the string");
	}

	@Test
	void anArtThatCantGoFallsThroughToAnotherTheSameSwingsSpell() {
		StringReader<Art> r = reader();
		Predicate<Art> notFinal = a -> a != FINAL;
		r.stroke(0, m(FULL), SWORD, ALL, notFinal);
		r.stroke(12, m(FULL), SWORD, ALL, notFinal);
		r.stroke(24, m(FULL), SWORD, ALL, notFinal);
		assertSame(FIRST, completed(r.stroke(30, m(LOW), SWORD, ALL, notFinal)), "the Final Art resting, the First Art goes");
		// And when nothing that fits can go, the best is refused, the swings used up.
		r = reader();
		r.stroke(0, m(), SWORD, ALL, a -> false);
		r.stroke(5, m(), SWORD, ALL, a -> false);
		StringReader.Event e = r.stroke(10, m(LOW), SWORD, ALL, a -> false);
		assertInstanceOf(StringReader.Refused.class, e);
		assertSame(FIRST, ((StringReader.Refused<?>) e).art());
		assertTrue(r.chain().isEmpty());
	}

	@Test
	void onlyTheArtsTheReaderIsGivenAreRead() {
		// A Glow swordsman has only the First Art: three full swings and a low one are the First Art to them.
		StringReader<Art> r = reader();
		List<Art> glow = List.of(FIRST);
		r.stroke(0, m(FULL), SWORD, glow, a -> true);
		r.stroke(12, m(FULL), SWORD, glow, a -> true);
		assertSame(FIRST, completed(r.stroke(24, m(FULL, LOW), SWORD, glow, a -> true)));
		// With nothing to read, every swing just lands.
		r = reader();
		for (int i = 0; i < 10; i++) {
			assertInstanceOf(StringReader.Landed.class, r.stroke(i * 5L, m(LOW, LEAP), SWORD, List.of(), a -> true));
		}
		assertEquals(StringRules.CHAIN, r.chain().size(), "it remembers the last six swings and no more");
	}

	@Test
	void aGuardOrAStepMarksTheFirstSwingAfterItAndKeepsTheStringGoing() {
		StringReader<Art> r = reader();
		r.cue(StringReader.Cue.GUARD, 100);
		assertEquals(COUNTER.bit(), r.cueMarks(100 + StringRules.COUNTER_TICKS));
		assertEquals(0, r.cueMarks(101 + StringRules.COUNTER_TICKS), "its moment passes");
		assertSame(THIRD, completed(swing(r, 110, m(FULL))), "a swing straight after a perfect guard is a counter");

		// Too long after it, a swing is only a swing.
		r = reader();
		r.cue(StringReader.Cue.GUARD, 100);
		r.tick(100 + StringRules.COUNTER_TICKS + 1);
		assertInstanceOf(StringReader.Landed.class, swing(r, 100 + StringRules.COUNTER_TICKS + 1, m(FULL)));

		// Only the first swing after it: a second swing in the same moment isn't a counter.
		Art doubleCounter = Art.of("two", "counter counter", AuraRules.EDGE);
		r = reader();
		r.cue(StringReader.Cue.GUARD, 0);
		r.stroke(2, m(), SWORD, List.of(doubleCounter), a -> true);
		assertTrue(COUNTER.fits(r.chain().getLast().marks()), "the first swing is a counter");
		assertInstanceOf(StringReader.Landed.class, r.stroke(4, m(), SWORD, List.of(doubleCounter), a -> true));
		assertFalse(COUNTER.fits(r.chain().getLast().marks()), "the second swing is plain");

		// An Aura Step: the first swing after it is a step cut.
		r = reader();
		r.cue(StringReader.Cue.STEP, 50);
		assertSame(FOURTH, completed(swing(r, 50 + StringRules.STEP_CUT_TICKS, m())));

		// A guard in the middle of a string keeps it open: a string written round a counter can be played.
		Art riposte = Art.of("riposte", "full counter", AuraRules.EDGE);
		r = reader();
		r.stroke(0, m(FULL), SWORD, List.of(riposte), a -> true);
		// Raised just before the window closes, the guard keeps the string open for the counter's moment.
		long guard = StringRules.window(W, SWORD) - 1;
		r.cue(StringReader.Cue.GUARD, guard);
		assertTrue(r.deadline() >= guard + StringRules.COUNTER_TICKS);
		assertSame(riposte, completed(r.stroke(guard + 10, m(), SWORD, List.of(riposte), a -> true)));
	}

	@Test
	void aCounterAfterAStringRanOutIsACounterNotAFumble() {
		// A one-swing art is never fumbled: the swing that would be late plays it afresh.
		Art counter = Art.of("counter", "counter", AuraRules.EDGE);
		StringReader<Art> r = reader();
		long window = StringRules.window(W, SWORD);
		r.stroke(0, m(), SWORD, List.of(counter), a -> true);
		r.tick(window + 1);
		r.cue(StringReader.Cue.GUARD, window + 1);
		assertSame(counter, completed(r.stroke(window + 2, m(), SWORD, List.of(counter), a -> true)));
		// And a late plain swing with only a one-swing art about just lands.
		r = reader();
		r.stroke(0, m(), SWORD, List.of(counter), a -> true);
		r.tick(window + 1);
		assertInstanceOf(StringReader.Landed.class, r.stroke(window + 2, m(), SWORD, List.of(counter), a -> true));
	}

	@Test
	void forgettingClearsEverything() {
		StringReader<Art> r = reader();
		swing(r, 0, m());
		r.cue(StringReader.Cue.STEP, 1);
		r.reset();
		assertTrue(r.chain().isEmpty());
		assertEquals(Long.MIN_VALUE, r.deadline());
		assertEquals(0, r.cueMarks(2));
		r.window(4);
		assertEquals(4, r.window());
		r.window(-3);
		assertEquals(1, r.window(), "a window is a tick at least");
	}

	@Test
	void fightingAtASwordsPaceKeepsAStringGoingAndAPauseBreaksIt() {
		// Full swings as soon as the sword allows, then a breath: the string runs out within a second and a quarter of the last swing.
		StringReader<Art> r = reader();
		List<StringReader.Event> events = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			events.add(swing(r, i * 13L, m(FULL)));
		}
		assertTrue(events.stream().allMatch(e -> e instanceof StringReader.Landed), "plain full swings set nothing off: " + events);
		long lastSwing = 4 * 13L;
		StringReader.Event lapsed = null;
		long t = lastSwing;
		while (lapsed == null && t < lastSwing + 40) {
			lapsed = r.tick(++t);
		}
		assertInstanceOf(StringReader.Lapsed.class, lapsed);
		assertTrue(t - lastSwing <= 25, "ran out after " + (t - lastSwing) + " ticks");
	}

	// ------------------------------------------------------------------ the placeholder arts' numbers

	@Test
	void theFinalArtWaitsOnAFullPool() {
		assertTrue(StringRules.poolFull(160, 160));
		assertTrue(StringRules.poolFull(158, 160), "the string's own coated swings spend a little on the way");
		assertTrue(StringRules.poolFull(144, 160));
		assertFalse(StringRules.poolFull(143, 160));
		assertFalse(StringRules.poolFull(0, 0), "nothing to fill");
	}

	@Test
	void anArcOpensInFront() {
		assertTrue(StringRules.inArc(0, 2, 0, 1, 3.5, 130), "straight ahead");
		assertTrue(StringRules.inArc(1.5, 1.5, 0, 1, 3.5, 130), "ahead and to the side");
		assertFalse(StringRules.inArc(2, -0.5, 0, 1, 3.5, 130), "to the side and a little behind");
		assertFalse(StringRules.inArc(0, 4, 0, 1, 3.5, 130), "too far");
		assertTrue(StringRules.inArc(0.2, -0.2, 0, 1, 3.5, 130), "standing right on top of you");
	}

	@Test
	void thePlaceholderArtsArePricedUnderTheSlashForWhatTheyReach() {
		assertTrue(StringRules.FIRST_COST < AuraRules.SLASH_COST && StringRules.FIRST_FACTOR < AuraRules.SLASH_FACTOR);
		assertTrue(StringRules.SECOND_COST < AuraRules.SLASH_COST && StringRules.THIRD_COST < AuraRules.SLASH_COST);
		assertTrue(StringRules.FOURTH_COST < AuraRules.SLASH_COST, "the step's own price comes first");
		assertTrue(StringRules.FINAL_COST <= AuraRules.DOMINION_COST && StringRules.FINAL_COOLDOWN >= 20 * 20, "the Final Art is rare");
		assertTrue(StringRules.FIRST_COST <= AuraRules.capacity(AuraRules.GLOW) / 3.0, "a Glow pool plays the First Art three times");
		assertTrue(StringRules.FINAL_COST < AuraRules.capacity(AuraRules.SOVEREIGN) * StringRules.FINAL_POOL);
		for (int cooldown : new int[] {StringRules.FIRST_COOLDOWN, StringRules.SECOND_COOLDOWN, StringRules.THIRD_COOLDOWN, StringRules.FOURTH_COOLDOWN}) {
			assertTrue(cooldown > AuraRules.SLASH_COOLDOWN, "an art rests longer than the slash");
		}
	}

	// ------------------------------------------------------------------ the registry

	@Test
	void artsAreRegisteredReplacedAndCheckedForConflicts() {
		PlaceholderArts.register();
		try {
			for (String id : PlaceholderArts.IDS) {
				assertTrue(AuraApi.string(id).isPresent(), id);
			}
			List<AuraApi.StringArt> all = AuraApi.strings();
			for (int i = 1; i < all.size(); i++) {
				assertTrue(all.get(i - 1).stage() <= all.get(i).stage(), "by stage");
			}
			AuraApi.StringArt first = AuraApi.string(PlaceholderArts.FIRST).orElseThrow();
			assertEquals(AuraRules.GLOW, first.stage());
			assertEquals(StringRules.FIRST_COST, first.cost(), 1e-9);
			assertEquals("aura.wildercord.art.first_art", first.nameKey());
			assertEquals(AuraApi.FINAL_GATE, AuraApi.string(PlaceholderArts.FINAL).orElseThrow().condition(), "the Final Art waits on the gate every Final Art shares");
			assertEquals("message.wildercord.aura.art.full_pool", PlaceholderArts.FULL_POOL.hintKey());
			assertEquals("message.wildercord.aura.art.condition", AuraApi.ArtCondition.ALWAYS.hintKey());

			// None of the five gets in another's way.
			for (String id : PlaceholderArts.IDS) {
				AuraApi.StringArt art = AuraApi.string(id).orElseThrow();
				assertTrue(AuraApi.conflicts(art.string(), id).isEmpty(), id + " meets " + AuraApi.conflicts(art.string(), id));
			}
			// What a writing screen would warn of: a string another art already uses, one cut short by an art's, and one that cuts an art's.
			assertEquals(List.of(PlaceholderArts.FIRST), ids(AuraApi.conflicts(SwordString.parse("swing swing low"), "")));
			assertEquals(List.of(PlaceholderArts.THIRD), ids(AuraApi.conflicts(SwordString.parse("counter full"), "")));
			assertTrue(ids(AuraApi.conflicts(SwordString.parse("swing swing"), "")).contains(PlaceholderArts.FIRST));
			assertTrue(AuraApi.conflicts(SwordString.parse("run run low"), "").isEmpty(), "a running string is free");

			// Replaced by id, and taken out.
			AuraApi.StringArt replacement = AuraApi.StringArt.of("apitest:first", "run run low", AuraRules.GLOW, 5, 40, (p, c) -> true);
			AuraApi.registerString(replacement);
			assertSame(replacement, AuraApi.string("apitest:first").orElseThrow());
			assertEquals("aura.wildercord.art.apitest.first", replacement.nameKey());
			assertTrue(AuraApi.unregisterString("apitest:first"));
			assertFalse(AuraApi.unregisterString("apitest:first"));
		} finally {
			AuraApi.unregisterString("apitest:first");
		}
	}

	@Test
	void anArtIsHeldToSenseOnTheWayIn() {
		assertThrows(IllegalArgumentException.class, () -> new AuraApi.StringArt(" ", SwordString.parse("low"), 1, 1, 1, null, null, null));
		assertThrows(IllegalArgumentException.class, () -> new AuraApi.StringArt("x", null, 1, 1, 1, null, null, null));
		AuraApi.StringArt art = new AuraApi.StringArt("x", SwordString.parse("low"), 0, -3, -5, null, null, null);
		assertEquals(AuraRules.GLOW, art.stage(), "an art opens at Glow at the earliest");
		assertEquals(0, art.cost(), 1e-9);
		assertEquals(0, art.cooldownTicks());
		assertSame(AuraApi.ArtCondition.ALWAYS, art.condition());
		assertEquals(AuraRules.SOVEREIGN, new AuraApi.StringArt("y", SwordString.parse("low"), 9, 1, 1, null, null, null).stage());
		AuraApi.StringContext context = new AuraApi.StringContext(art, List.of(m(FULL), m(LOW, FULL)), null, 0);
		assertTrue(context.released(LOW) && context.released(FULL) && !context.released(LEAP));
	}

	private static List<String> ids(List<AuraApi.StringArt> arts) {
		return arts.stream().map(AuraApi.StringArt::id).toList();
	}
}
