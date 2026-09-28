package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers behind the fused runes of flame and stone: Everburn's beat, Starfire's marks, Monolith's throw, the pulls. */
class FusedFlameRulesTest {
	/** Vanilla fire burns when a creature's fire ticks left are a multiple of 20 (and counts them down after). */
	private static boolean vanillaBurns(int fireTicks) {
		return fireTicks % 20 == 0;
	}

	@Test
	void everburnBurnsOnTheHalfBeatOfVanillaFire() {
		// Walk a 5-second fire down tick by tick, as the game would: Everburn burns as often as the fire itself does.
		int ours = 0;
		int theirs = 0;
		int lastOurs = -100;
		int lastTheirs = -100;
		for (int left = 100, tick = 0; left > 0; left--, tick++) {
			if (vanillaBurns(left)) {
				theirs++;
				// Never inside the half-second cooldown our last burn left: vanilla's burn always lands.
				assertTrue(tick - lastOurs > 10, "vanilla's burn at " + tick + " falls inside our cooldown (ours at " + lastOurs + ")");
				lastTheirs = tick;
			}
			// Everburn checks at the end of the tick, when the fire has counted down once more.
			if (FusedFlameRules.everburnDue(left - 1) == 0) {
				ours++;
				assertTrue(tick - lastTheirs >= 5, "our burn at " + tick + " sits right on vanilla's (at " + lastTheirs + ")");
				lastOurs = tick;
			}
		}
		assertEquals(5, theirs);
		assertEquals(5, ours, "5 seconds of Everburn should burn 5 more times: 1 more damage a second");
	}

	@Test
	void everburnDueCountsDownToItsBeat() {
		assertEquals(0, FusedFlameRules.everburnDue(FusedFlameRules.EVERBURN_BEAT));
		assertEquals(0, FusedFlameRules.everburnDue(91));
		assertEquals(1, FusedFlameRules.everburnDue(92));
		assertEquals(19, FusedFlameRules.everburnDue(90));
		for (int left = 1; left <= 400; left++) {
			int due = FusedFlameRules.everburnDue(left);
			assertTrue(due >= 0 && due < 20);
			assertEquals(0, FusedFlameRules.everburnDue(left - due), "waiting " + due + " from " + left + " should reach the beat");
		}
	}

	@Test
	void everyEnemyGetsAMoteBeforeAnyGetsTwo() {
		assertArrayEquals(new int[] {0, 1, 2, 3, 4}, FusedFlameRules.moteMarks(5, 5));
		assertArrayEquals(new int[] {0, 1, 2, 0, 1}, FusedFlameRules.moteMarks(3, 5));
		assertArrayEquals(new int[] {0, 0, 0, 0, 0}, FusedFlameRules.moteMarks(1, 5));
		assertArrayEquals(new int[] {-1, -1, -1, -1, -1}, FusedFlameRules.moteMarks(0, 5));
		for (int marks = 1; marks <= 5; marks++) {
			int[] counts = new int[marks];
			for (int m : FusedFlameRules.moteMarks(marks, 5)) {
				counts[m]++;
			}
			int min = Integer.MAX_VALUE;
			int max = 0;
			for (int c : counts) {
				min = Math.min(min, c);
				max = Math.max(max, c);
			}
			assertTrue(max - min <= 1, "motes should be shared out evenly among " + marks);
		}
	}

	@Test
	void monolithThrowsAboutThreeBlocksUp() {
		double rise = FusedFlameRules.rise(FusedFlameRules.MONOLITH_LIFT);
		assertTrue(rise > 2.85 && rise < 3.2, "Monolith should throw a creature about 3 blocks up (it rises " + rise + ")");
		// Landing from there costs nothing: falls of 3 blocks or less don't hurt.
		assertTrue(rise < 3.25);
	}

	@Test
	void pullsAreGentleAndNeverOvershoot() {
		for (double d = 0; d <= 8; d += 0.05) {
			assertTrue(FusedFlameRules.hellmouthDrag(d) <= 0.3);
			assertTrue(FusedFlameRules.magnetDraw(d) <= 0.28);
			assertTrue(FusedFlameRules.sinkholeDrag(d) <= 0.45);
			// Each drag is less than the way left: nothing is carried past the middle and slammed into the far side.
			assertTrue(FusedFlameRules.sinkholeDrag(d) <= d);
			if (d > 0.35) {
				assertTrue(FusedFlameRules.hellmouthDrag(d) < d * 0.9);
			}
		}
		// Stronger from further off.
		assertTrue(FusedFlameRules.hellmouthDrag(3) > FusedFlameRules.hellmouthDrag(1));
		assertTrue(FusedFlameRules.magnetDraw(4) > FusedFlameRules.magnetDraw(1));
	}
}
