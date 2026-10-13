package dev.wildercord.cast;

import static dev.wildercord.cast.ArchivistRules.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchivistRulesTest {
	/** How long since everything, long enough that nothing is resting. */
	private static long[] rested() {
		long[] since = new long[RESTS];
		Arrays.fill(since, 100_000L);
		return since;
	}

	/** The weight {@link ArchivistRules#weights} gives {@code option}, with three spells to hand. */
	private static double weight(int option, int phase, double distance, boolean behind, double height, boolean sight, boolean silenced,
			long[] since) {
		int[] options = options(phase, 3);
		double[] weights = weights(phase, 3, distance, behind, height, sight, 1, silenced, since);
		for (int i = 0; i < options.length; i++) {
			if (options[i] == option) {
				return weights[i];
			}
		}
		throw new AssertionError("no option " + option);
	}

	@Test void itLearnsTwoMoreTricksWithEachRewrite() {
		for (int move : new int[] {TOMES, QUILL, RECITE, SNAP}) {
			assertTrue(unlocked(move, 1));
		}
		assertFalse(unlocked(ERASE, 1));
		assertFalse(unlocked(BIND, 1));
		assertTrue(unlocked(ERASE, 2));
		assertTrue(unlocked(BIND, 2));
		assertFalse(unlocked(BOOKMARK, 2));
		assertFalse(unlocked(HUSH, 2));
		assertTrue(unlocked(BOOKMARK, 3));
		assertTrue(unlocked(HUSH, 3));
		for (int open : new int[] {NONE, SHUT, BLANK, LEAFING, SUNFALL}) {
			assertFalse(unlocked(open, 3));
		}
		assertEquals(0, weight(BOOKMARK, 2, 10, false, 0, true, false, rested()));
		assertTrue(weight(BOOKMARK, 3, 10, false, 0, true, false, rested()) > 0);
	}

	@Test void everyTrickAndOpeningIsNamedAndToldFairly() {
		for (int move = 1; move < MOVES; move++) {
			assertNotNull(key(move), "move " + move);
		}
		assertNull(key(NONE));
		for (int move : PICKS) {
			assertTrue(tell(move, false) >= 10 && tell(move, false) <= 18, "tell " + move);
			assertTrue(tell(move, true) < tell(move, false), "chained tell " + move);
			assertTrue(tell(move, true) >= 9, "chained tell " + move);
			int lock = lock(move);
			assertTrue(lock == 0 || lock >= 3 && lock <= 6, "lock " + move);
			assertTrue(lock < tell(move, false));
			assertTrue(after(move) > 0);
		}
		assertEquals(HUSH_HOLD, after(HUSH));
	}

	@Test void itsSpellsWantSightAndAnUnsealedCord() {
		assertTrue(weight(SPELL, 1, 8, false, 0, true, false, rested()) > 0);
		assertEquals(0, weight(SPELL, 1, 8, false, 0, false, false, rested()));
		assertEquals(0, weight(SPELL + 2, 2, 8, false, 0, true, true, rested()));
		assertEquals(0, weight(SPELL, 1, 30, false, 0, true, false, rested()));
		assertTrue(weight(SPELL, 1, 8, false, 0, true, false, rested()) > weight(SPELL, 2, 8, false, 0, true, false, rested()));
		// Sealed, its tricks still work.
		assertTrue(weight(SNAP, 1, 2, false, 0, true, true, rested()) > 0);
	}

	@Test void theSecretComesOnlyInItsLastCord() {
		assertEquals(0, weight(SUNFALL, 2, 8, false, 0, true, false, rested()));
		assertTrue(weight(SUNFALL, 3, 8, false, 0, true, false, rested()) > 0);
		assertEquals(0, weight(SUNFALL, 3, 8, false, 0, true, true, rested()));
		long[] since = rested();
		since[SUNFALL] = SUNFALL_REST * 2 / 3 - 1;
		assertEquals(0, weight(SUNFALL, 3, 8, false, 0, true, false, since));
	}

	@Test void aTrickStillRestingCantBeChosenAndItsLastCordRestsLess() {
		long[] since = rested();
		since[QUILL] = rest(QUILL, 1) - 1;
		assertEquals(0, weight(QUILL, 1, 8, false, 0, true, false, since));
		since[QUILL] = rest(QUILL, 1);
		assertTrue(weight(QUILL, 1, 8, false, 0, true, false, since) > 0);
		for (int move : PICKS) {
			assertTrue(rest(move, 3) < rest(move, 2), "rest " + move);
			assertEquals(rest(move, 1), rest(move, 2));
		}
		assertTrue(pause(TOMES, 3, 0.99) < pause(TOMES, 1, 0));
		assertTrue(pause(SPELL, 1, 0) > pause(TOMES, 1, 0));
		assertEquals(pause(SUNFALL, 2, 0.5), pause(SPELL + 1, 2, 0.5));
		assertTrue(scatter(2) > scatter(1));
		assertTrue(quillLines(2) > quillLines(1));
	}

	@Test void itClapsAtThoseCloseAndMostOfAllThoseBehindIt() {
		assertTrue(want(SNAP, 2, true, 0, true, 1) > want(SNAP, 2, false, 0, true, 1));
		assertTrue(want(SNAP, 2, false, 0, true, 1) > want(QUILL, 2, false, 0, true, 1));
		assertTrue(want(SNAP, 2, false, 0, true, 1) > want(TOMES, 2, false, 0, true, 1));
		assertEquals(0, want(SNAP, 10, false, 0, true, 1));
		assertTrue(want(QUILL, 8, false, 0, true, 1) > want(SNAP, 8, false, 0, true, 1));
	}

	@Test void itAnswersThoseAboveItWithWhatFallsOrReaches() {
		assertEquals(0, want(QUILL, 6, false, 4, true, 1));
		assertEquals(0, want(RECITE, 6, false, 4, true, 1));
		assertEquals(0, want(SNAP, 2, false, 4, true, 1));
		assertTrue(want(TOMES, 3, false, 4, true, 1) > want(TOMES, 3, false, 0, true, 1));
		assertTrue(want(BIND, 3, false, 4, true, 1) > want(BIND, 3, false, 0, true, 1));
		assertTrue(want(BOOKMARK, 3, false, 4, true, 1) > want(BOOKMARK, 3, false, 0, true, 1));
	}

	@Test void itCantWipeWhatIsBehindItNorBindWhatItCantSee() {
		assertEquals(0, want(ERASE, 5, true, 0, true, 1));
		assertTrue(want(ERASE, 5, false, 0, true, 1) > 0);
		assertEquals(0, want(BIND, 10, false, 0, false, 1));
		assertEquals(0, want(QUILL, 8, false, 0, false, 1));
		assertTrue(want(BIND, 10, false, 0, true, 3) > want(BIND, 10, false, 0, true, 1));
		assertTrue(want(RECITE, 6, false, 0, true, 3) > want(RECITE, 6, false, 0, true, 1));
		assertTrue(want(HUSH, 3, true, 0, true, 1) > want(HUSH, 10, false, 0, true, 1));
		assertEquals(0, want(QUILL, 25, false, 0, true, 1));
	}

	@Test void inItsLastCordSomeTricksRunStraightIntoAnother() {
		assertEquals(-1, chain(QUILL, 10, true, CHAIN_CHANCE));
		assertEquals(TOMES, chain(QUILL, 10, true, 0));
		assertEquals(SNAP, chain(QUILL, 3, true, 0));
		assertEquals(-1, chain(QUILL, 5, true, 0));
		assertEquals(QUILL, chain(RECITE, 5, true, 0));
		assertEquals(TOMES, chain(RECITE, 5, false, 0));
		assertEquals(BOOKMARK, chain(TOMES, 5, true, 0));
		assertEquals(TOMES, chain(HUSH, 5, true, 0));
		for (int move : new int[] {SNAP, ERASE, BOOKMARK, BIND, SHUT, BLANK, LEAFING}) {
			assertEquals(-1, chain(move, 5, true, 0), "chain " + move);
		}
		for (int move : PICKS) {
			assertNotEquals(move, chain(move, 3, true, 0));
			assertNotEquals(move, chain(move, 10, true, 0));
		}
	}

	@Test void threeTricksLeaveItOpenAndOpenItTakesMore() {
		assertEquals(SHUT, opening(SNAP));
		assertEquals(BLANK, opening(ERASE));
		assertEquals(LEAFING, opening(BOOKMARK));
		for (int move : new int[] {TOMES, QUILL, RECITE, BIND, HUSH}) {
			assertEquals(NONE, opening(move));
			assertEquals(1.0F, taken(move));
			assertFalse(open(move));
		}
		for (int state : new int[] {SHUT, BLANK, LEAFING}) {
			assertTrue(open(state));
			assertTrue(taken(state) > 1.0F);
			assertTrue(openFor(state) >= 20);
			assertEquals(0, tell(state, false));
		}
	}

	@Test void itsLinesAndRingsAndMarksCatchOnlyWhereTheyLie() {
		assertTrue(lane(6, 0.5, 0, 1, 12));
		assertFalse(lane(6, 2, 0, 1, 12));
		assertFalse(lane(13, 0, 0, 1, 12));
		assertFalse(lane(-2, 0, 0, 1, 12));
		assertFalse(lane(6, 0, 3, 1, 12));
		assertEquals(1, quillFront(0));
		assertEquals(QUILL_REACH, quillFront(QUILL_GROW));
		assertEquals(QUILL_REACH, quillFront(QUILL_GROW * 4));
		assertEquals(RECITE_REACH, wave(1000, RECITE_SPEED));
		// A ring catches those standing in its way but passes under those who jump it.
		assertTrue(crosses(5, 4.5, 5, 0));
		assertFalse(crosses(5, 4.5, 5, 1.2));
		assertFalse(crosses(8, 4.5, 5, 0));
		assertTrue(inMark(1, 1, 0, TOMES_RADIUS));
		assertFalse(inMark(2, 1, 0, TOMES_RADIUS));
		assertFalse(inMark(0, 0, 4, TOMES_RADIUS));
		assertTrue(snapHits(4, 0));
		assertFalse(snapHits(5.5, 0));
		assertFalse(snapHits(2, 4));
	}

	@Test void theWipingIsAConeBeforeItWidenedInItsLastCord() {
		assertTrue(cone(5, 0, 0, 2));
		assertFalse(cone(-5, 0, 0, 2));
		assertFalse(cone(1, 0, 0, 2));
		assertFalse(cone(11, 0, 0, 2));
		double side = 5 * Math.tan(Math.toRadians(35));
		assertFalse(cone(5, side, 0, 2));
		assertTrue(cone(5, side, 0, 3));
		assertFalse(cone(5, 0, 3.5, 3));
	}

	@Test void theHushTheArenaAndTheBookmarkKeepTheirBounds() {
		assertFalse(moved(0.5, 0.5));
		assertTrue(moved(1, 0.5));
		assertArrayEquals(new double[] {3, 4}, within(3, 4), 1e-9);
		double[] far = within(30, 40);
		assertEquals(ARENA, Math.sqrt(far[0] * far[0] + far[1] * far[1]), 1e-9);
		assertEquals(0.75, far[0] / far[1], 1e-9);
		assertArrayEquals(new double[] {BOOKMARK_CREEP, 0}, creep(0, 0, 5, 0), 1e-9);
		assertArrayEquals(new double[] {0.05, 0}, creep(0, 0, 0.05, 0), 1e-9);
		assertEquals(-170, wrap(190), 1e-9);
		assertEquals(170, wrap(-190), 1e-9);
		assertEquals(0, wrap(720), 1e-9);
		assertTrue(behind(0, 180));
		assertTrue(behind(170, -40));
		assertFalse(behind(0, 90));
		assertFalse(behind(350, 20));
	}

	@Test void overALongFightItNeverRepeatsItselfAndUsesEverythingItKnows() {
		Random random = new Random(7);
		for (int phase = 1; phase <= 3; phase++) {
			MoveDeck deck = new MoveDeck(MEMORY);
			long[] lastMade = new long[RESTS];
			Arrays.fill(lastMade, -100_000L);
			Set<Integer> made = new HashSet<>();
			long now = 0;
			int previous = -1;
			for (int turn = 0; turn < 600; turn++) {
				double distance = random.nextDouble() * 18;
				boolean behind = random.nextDouble() < 0.25;
				double height = random.nextDouble() < 0.15 ? 4 : 0;
				boolean sight = random.nextDouble() < 0.85;
				int crowd = 1 + random.nextInt(3);
				boolean silenced = random.nextDouble() < 0.1;
				long[] since = new long[RESTS];
				for (int i = 0; i < RESTS; i++) {
					since[i] = now - lastMade[i];
				}
				int[] options = options(phase, 3);
				double[] weights = weights(phase, 3, distance, behind, height, sight, crowd, silenced, since);
				boolean other = false;
				for (int i = 0; i < options.length; i++) {
					if (weights[i] > 0 && options[i] != previous) {
						other = true;
					}
				}
				int choice = choose(deck, phase, 3, distance, behind, height, sight, crowd, silenced, since, random.nextDouble());
				if (choice < 0) {
					now += 10;
					continue;
				}
				if (other) {
					assertNotEquals(previous, choice, "phase " + phase + " turn " + turn);
				}
				deck.played(choice);
				made.add(choice);
				previous = choice;
				if (choice >= MOVES) {
					if (choice == SUNFALL) {
						lastMade[SUNFALL] = now;
					}
					now += 20 + pause(choice, phase, random.nextDouble());
					continue;
				}
				assertTrue(unlocked(choice, phase), "phase " + phase + " made " + choice);
				lastMade[choice] = now;
				now += tell(choice, false) + after(choice) + openFor(opening(choice));
				int next = phase >= 3 ? chain(choice, distance, sight, random.nextDouble()) : -1;
				if (next > 0 && unlocked(next, phase) && now - lastMade[next] >= rest(next, phase) / 2) {
					assertNotEquals(choice, next);
					deck.played(next);
					made.add(next);
					previous = next;
					lastMade[next] = now;
					now += tell(next, true) + after(next) + openFor(opening(next));
				}
				now += pause(choice, phase, random.nextDouble());
			}
			for (int move : PICKS) {
				assertEquals(unlocked(move, phase), made.contains(move), "phase " + phase + " move " + move);
			}
			assertEquals(phase >= 3, made.contains(SUNFALL), "phase " + phase);
			assertTrue(made.contains(SPELL) && made.contains(SPELL + 1) && made.contains(SPELL + 2));
		}
	}
}
