package dev.wildercord.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Two weaves of ley lines, and the places of power where they cross. */
class LeyCrossingTest {
	@Test
	void everyWorldHasCrossingsAFewHundredBlocksApart() {
		for (long world : new long[] {12345L, 1L, -42L, 987654321L}) {
			long seed = LeyLines.seedOf(world);
			int found = 0;
			for (int cx = -64; cx < 64; cx += 1) {
				for (int cz = -64; cz < 64; cz += 4) {
					if (LeyLines.crossingIn(seed, cx, cz) != null) {
						found++;
					}
				}
			}
			// 128 by 32 chunks sampled (2048 by 512 blocks, a quarter of the chunks in it): some, never a carpet.
			assertTrue(found >= 1 && found < 120, "crossings in a 2048-block strip for world " + world + ": " + found);
		}
	}

	@Test
	void aCrossingIsOnALineOfEachWeave() {
		long seed = LeyLines.seedOf(12345L);
		double[] heart = LeyLines.nearestCrossing(seed, 0, 0, 3000);
		assertNotNull(heart, "a crossing within 3000 blocks of spawn");
		double x = heart[0];
		double z = heart[1];
		assertTrue(LeyLines.atCrossing(seed, x, z));
		assertTrue(LeyLines.first(seed, x, z) >= LeyLines.CROSSING && LeyLines.second(seed, x, z) >= LeyLines.CROSSING);
		assertTrue(LeyLines.strength(seed, x, z) >= LeyLines.CROSSING, "and so on a ley line");
		assertEquals(LeyLines.crossing(seed, x, z), heart[2], 1e-9);
		// Its heart is the strongest spot near it, and a few blocks off it's no crossing at all.
		boolean leaves = false;
		for (int d = 4; d <= 24 && !leaves; d += 2) {
			leaves = !LeyLines.atCrossing(seed, x + d, z) || !LeyLines.atCrossing(seed, x, z + d);
		}
		assertTrue(leaves, "a crossing is a place, not a region");
		// Worked out the same every time, on the server and on every client.
		assertArrayEquals(heart, LeyLines.nearestCrossing(seed, 0, 0, 3000));
	}

	@Test
	void theFirstWeaveIsTheLinesAsTheyWere() {
		long seed = LeyLines.seedOf(777L);
		for (int x = -900; x < 900; x += 37) {
			for (int z = -900; z < 900; z += 41) {
				double s = LeyLines.strength(seed, x, z);
				assertTrue(s >= LeyLines.first(seed, x, z) && s >= LeyLines.second(seed, x, z));
				assertTrue(LeyLines.crossing(seed, x, z) <= s);
			}
		}
	}

	@Test
	void noCrossingIsFoundWhereNoneIsNear() {
		long seed = LeyLines.seedOf(12345L);
		// Somewhere with no line of the second weave for 40 blocks around has no crossing near it.
		for (int x = -4000; x < 4000; x += 160) {
			boolean quiet = true;
			for (int dx = -40; dx <= 40 && quiet; dx += 4) {
				for (int dz = -40; dz <= 40 && quiet; dz += 4) {
					quiet = LeyLines.second(seed, x + dx, 500 + dz) == 0;
				}
			}
			if (quiet) {
				assertNull(LeyLines.nearestCrossing(seed, x, 500, 24));
				return;
			}
		}
		fail("expected some quiet ground in 8000 blocks");
	}
}
