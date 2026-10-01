package dev.wildercord.monster;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterRulesTest {
	@Test
	void sixMonstersEachWithItsOwnIdAndAModestWeight() {
		Set<String> ids = new HashSet<>();
		for (MonsterRules.Kind kind : MonsterRules.ALL) {
			assertTrue(ids.add(kind.id), kind.id + " is listed twice");
			// A zombie or a skeleton weighs about 100: these should be a meeting now and then, never a crowd.
			assertTrue(kind.weight >= 5 && kind.weight <= 20, kind + " weighs " + kind.weight);
			assertTrue(kind.min >= 1 && kind.max >= kind.min && kind.max <= 2, kind + " comes in packs of " + kind.min + "-" + kind.max);
		}
		assertEquals(6, ids.size());
	}

	@Test
	void theSpawnRateScalesWeightsAndNeverRoundsThemAwayUnlessItIsNothing() {
		assertEquals(10, MonsterRules.weight(10, 1.0));
		assertEquals(20, MonsterRules.weight(10, 2.0));
		assertEquals(5, MonsterRules.weight(10, 0.5));
		assertEquals(1, MonsterRules.weight(10, 0.01), "a tiny rate still lets them spawn, rarely");
		assertEquals(0, MonsterRules.weight(10, 0.0));
		assertEquals(0, MonsterRules.weight(10, -1.0));
		assertEquals(0, MonsterRules.weight(10, Double.NaN));
		assertEquals(40, MonsterRules.weight(10, 50.0), "held to four times");
	}

	@Test
	void vinesHoldLongerOnHarderDifficulties() {
		assertTrue(MonsterRules.rootTicks(1) < MonsterRules.rootTicks(2));
		assertTrue(MonsterRules.rootTicks(2) < MonsterRules.rootTicks(3));
		assertEquals(30, MonsterRules.rootTicks(2), "a second and a half on Normal");
		assertTrue(MonsterRules.rootTicks(3) <= 40, "never more than two seconds");
	}

	@Test
	void aGloomstalkerHidesOnlyInTheDarkUnmarkedAndAlone() {
		assertTrue(MonsterRules.veiled(0, false, false, 10, false));
		assertTrue(MonsterRules.veiled(MonsterRules.REVEAL_LIGHT - 1, false, false, 10, false));
		assertFalse(MonsterRules.veiled(MonsterRules.REVEAL_LIGHT, false, false, 10, false), "a torch's glow shows it");
		assertFalse(MonsterRules.veiled(0, true, false, 10, false), "glowing shows it");
		assertFalse(MonsterRules.veiled(0, false, true, 10, false), "a light spell or a wound shows it for a while");
		assertFalse(MonsterRules.veiled(0, false, false, 2.5, false), "up close it shows");
		assertFalse(MonsterRules.veiled(0, false, false, 10, true), "mid-pounce it shows");
		for (String light : new String[] {"fire", "storm", "arcane", "life"}) {
			assertTrue(MonsterRules.revealing(light), light);
		}
		for (String dark : new String[] {"void", "frost", "earth", "wind", "time", "blood", ""}) {
			assertFalse(MonsterRules.revealing(dark), dark);
		}
		assertEquals(1.0F, MonsterRules.opacity(0), 1e-6);
		assertTrue(MonsterRules.opacity(1) > 0.05F && MonsterRules.opacity(1) < 0.2F, "hidden, still a ripple in the dark");
		assertEquals(MonsterRules.opacity(1), MonsterRules.opacity(3), 1e-6);
	}

	@Test
	void aCurledGeodeCrawlerShrugsOffBlowsButCracksUnderTheRightOne() {
		assertEquals(2.0F, MonsterRules.curled(10, false), 1e-6);
		assertEquals(12.5F, MonsterRules.curled(10, true), 1e-6);
		assertTrue(MonsterRules.curled(10, true) > 10 * 5 * MonsterRules.CURLED_SHARE);
	}

	@Test
	void aManaOozeFillsGrowsAndSplitsWhenTooFull() {
		MonsterRules.Feed sip = MonsterRules.feed(2, 0, 5);
		assertEquals(2, sip.size());
		assertEquals(5, sip.fullness(), 1e-6);
		assertFalse(sip.split());
		MonsterRules.Feed grow = MonsterRules.feed(2, 12, 6);
		assertEquals(3, grow.size(), "full, it grows a size");
		assertEquals(0, grow.fullness(), 1e-6, "and starts empty again");
		MonsterRules.Feed burst = MonsterRules.feed(MonsterRules.MAX_OOZE, MonsterRules.oozeCapacity(MonsterRules.MAX_OOZE) - 1, 3);
		assertTrue(burst.split(), "full at its biggest, it bursts");
		assertEquals(MonsterRules.MAX_OOZE / 2, burst.size());
		assertEquals(0, MonsterRules.feed(1, 0, -4).fullness(), 1e-6, "nothing negative is drunk");
		for (int size = 1; size < MonsterRules.MAX_OOZE; size++) {
			assertTrue(MonsterRules.oozeHealth(size) < MonsterRules.oozeHealth(size + 1));
			assertTrue(MonsterRules.oozeCapacity(size) < MonsterRules.oozeCapacity(size + 1));
		}
		assertEquals(MonsterRules.oozeHealth(MonsterRules.MAX_OOZE), MonsterRules.oozeHealth(99), "no bigger than its biggest");
	}

	@Test
	void aBubbleLandsWhereItWasAimed() {
		double dx = 9.0;
		double dy = -1.5;
		double dz = -4.0;
		int ticks = MonsterRules.bubbleFlight(Math.sqrt(dx * dx + dz * dz));
		assertTrue(ticks >= 12 && ticks <= 34, "slow enough to see coming and to shoot down: " + ticks);
		double[] v = MonsterRules.lob(dx, dy, dz, MonsterRules.BUBBLE_GRAVITY, ticks);
		// Flown the way the bubble flies: move, then fall a little faster.
		double x = 0;
		double y = 0;
		double z = 0;
		double vy = v[1];
		for (int i = 0; i < ticks; i++) {
			x += v[0];
			y += vy;
			z += v[2];
			vy -= MonsterRules.BUBBLE_GRAVITY;
		}
		assertEquals(dx, x, 1e-9);
		assertEquals(dy, y, 1e-9);
		assertEquals(dz, z, 1e-9);
		assertTrue(v[1] > 0, "it's lobbed upward, in an arc");
	}
}
