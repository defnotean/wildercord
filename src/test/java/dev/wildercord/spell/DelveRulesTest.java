package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The delving pack's pure rules (DelveRules) and its place in the roster. */
class DelveRulesTest {
	@Test
	void theRosterHasFortyEightDistinctNonOffensiveRunes() {
		assertEquals(48, DelveRules.PATHS.size());
		assertEquals(48, new HashSet<>(DelveRules.PATHS).size());
		Set<String> taken = Set.of("excavate", "tunnel", "vein", "smelt", "prospect", "chisel", "span", "fell", "break", "light", "glimmer", "prune",
			"collect", "harvest", "rampart", "basinfill", "galvanize", "strata_rise", "shard_compass", "pocket_current", "cinder_sieve", "night_seam",
			"wind_steps");
		for (String path : DelveRules.PATHS) {
			assertFalse(taken.contains(path), path);
			RuneDef rune = Runes.get("wildercord:" + path).orElse(null);
			assertNotNull(rune, path);
			assertEquals(RuneFamily.EFFECT, rune.family(), path);
			assertNotEquals(EffectKind.HARMFUL, rune.kind(), path + " must not be offensive");
			assertNotEquals("damage", rune.category(), path);
			assertTrue(rune.tier() >= 1 && rune.tier() <= 4, path);
		}
	}

	@Test
	void radiusGrowsWithScaleButNeverPastTheCap() {
		assertEquals(2, DelveRules.radius(2, 1.0));
		assertEquals(3, DelveRules.radius(2, 1.5));
		assertEquals(DelveRules.RADIUS_CAP, DelveRules.radius(6, 10.0));
		assertEquals(1, DelveRules.radius(2, 0.0));
		assertEquals(1, DelveRules.radius(2, -3.0));
	}

	@Test
	void stairsAreThreeCellsAStepAndGoTheRightWay() {
		List<int[]> down = DelveRules.stairCells(5, true);
		List<int[]> up = DelveRules.stairCells(5, false);
		assertEquals(15, down.size());
		assertEquals(15, up.size());
		// The lowest cell of each step going down drops one a step; going up it climbs one a step.
		for (int i = 0; i < 5; i++) {
			assertArrayEquals(new int[] {i + 1, -i}, down.get(i * 3));
			assertArrayEquals(new int[] {i + 1, i + 1}, up.get(i * 3));
		}
		// Never more than the cast's block budget for both together.
		assertTrue(down.size() + up.size() <= 32);
	}

	@Test
	void oreKindsComeFromTheBlockId() {
		assertEquals("iron", DelveRules.oreKind("iron_ore"));
		assertEquals("iron", DelveRules.oreKind("deepslate_iron_ore"));
		assertEquals("gold", DelveRules.oreKind("nether_gold_ore"));
		assertEquals("quartz", DelveRules.oreKind("nether_quartz_ore"));
		assertEquals("debris", DelveRules.oreKind("ancient_debris"));
		assertNull(DelveRules.oreKind("stone"));
		assertNull(DelveRules.oreKind("raw_iron_block"));
		assertNull(DelveRules.oreKind("_ore"));
	}

	@Test
	void aTallyIsBiggestFirstAndAtMostFiveKinds() {
		Map<String, Integer> counts = new LinkedHashMap<>();
		counts.put("coal", 2);
		counts.put("iron", 3);
		counts.put("copper", 2);
		assertEquals("3 iron, 2 coal, 2 copper", DelveRules.tally(counts));
		assertEquals("none", DelveRules.tally(Map.of()));
		Map<String, Integer> many = new LinkedHashMap<>();
		for (String k : List.of("a", "b", "c", "d", "e", "f")) {
			many.put(k, 1);
		}
		assertEquals("1 a, 1 b, 1 c, 1 d, 1 e", DelveRules.tally(many));
	}

	@Test
	void fortuneNeverPassesVanillasThree() {
		assertEquals(1, DelveRules.fortune(false, 0));
		assertEquals(2, DelveRules.fortune(false, 1));
		assertEquals(2, DelveRules.fortune(true, 0));
		assertEquals(3, DelveRules.fortune(true, 1));
		assertEquals(3, DelveRules.fortune(true, 5));
	}

	@Test
	void packingCountsWholeNinesOnly() {
		assertEquals(0, DelveRules.packs(8));
		assertEquals(1, DelveRules.packs(9));
		assertEquals(2, DelveRules.packs(26));
		assertEquals(DelveRules.PACK_MAX, DelveRules.packs(64 * 36));
		assertEquals(0, DelveRules.packs(-4));
	}

	@Test
	void mendingSpendsOnlyWhatTheWearNeeds() {
		// Iron pickaxe: 250 durability, a quarter is 62 a material.
		assertEquals(1, DelveRules.mendUnits(10, 250, 5));
		assertEquals(2, DelveRules.mendUnits(100, 250, 5));
		assertEquals(4, DelveRules.mendUnits(249, 250, 64));
		assertEquals(1, DelveRules.mendUnits(249, 250, 1));
		assertEquals(0, DelveRules.mendUnits(0, 250, 5));
		assertEquals(0, DelveRules.mendUnits(10, 250, 0));
		assertEquals(0, DelveRules.mended(10, 250, 1));
		assertEquals(38, DelveRules.mended(100, 250, 1));
		// Never more spent than the wear asks: one material short of full is never mended past full.
		assertTrue(DelveRules.mended(200, 250, DelveRules.mendUnits(200, 250, 64)) == 0);
	}

	@Test
	void stiltsAndBridgesAreBounded() {
		assertEquals(0, DelveRules.stiltHeight(0));
		assertEquals(3, DelveRules.stiltHeight(3));
		assertEquals(DelveRules.STILT_MAX, DelveRules.stiltHeight(40));
		assertEquals(4, DelveRules.bridgeLength(4, 64));
		assertEquals(2, DelveRules.bridgeLength(10, 2));
		assertEquals(DelveRules.BRIDGE_MAX, DelveRules.bridgeLength(99, 99));
	}

	@Test
	void torchesKeepTheirSpacing() {
		List<int[]> chosen = new ArrayList<>();
		chosen.add(new int[] {0, 64, 0});
		assertFalse(DelveRules.spaced(chosen, new int[] {3, 64, 3}, 5));
		assertTrue(DelveRules.spaced(chosen, new int[] {5, 64, 0}, 5));
		assertTrue(DelveRules.spaced(List.of(), new int[] {0, 0, 0}, 5));
	}

	@Test
	void lumensHangOneEveryFourBlocksAtMostSix() {
		assertEquals(1, DelveRules.lumens(0.5));
		assertEquals(1, DelveRules.lumens(4));
		assertEquals(2, DelveRules.lumens(5));
		assertEquals(DelveRules.LUMEN_MAX, DelveRules.lumens(200));
	}

	@Test
	void aMarkCallsYouBackOnlyFromNearEnough() {
		assertTrue(DelveRules.markReach(100, 10, 50));
		assertTrue(DelveRules.markReach(DelveRules.MARK_RANGE, 0, 0));
		assertFalse(DelveRules.markReach(DelveRules.MARK_RANGE, 1, 0));
	}

	@Test
	void stacksSplitWithoutLosingOrMakingAnything() {
		assertEquals(List.of(64, 64, 2), DelveRules.stacks(130, 64));
		assertEquals(List.of(16), DelveRules.stacks(16, 16));
		assertEquals(List.of(), DelveRules.stacks(0, 64));
		assertEquals(List.of(), DelveRules.stacks(10, 0));
		assertEquals(1000, DelveRules.stacks(1000, 64).stream().mapToInt(Integer::intValue).sum());
	}

	@Test
	void everyAreaEditFitsTheCastBudget() {
		int budget = 32;
		assertTrue(DelveRules.STAIR_STEPS * 3 <= budget);
		assertTrue(DelveRules.SHAFT_DEPTH <= budget);
		assertTrue(DelveRules.ORE_PLUCK_MAX <= budget);
		assertTrue(DelveRules.MOTHERLODE_MAX <= budget);
		assertTrue(DelveRules.SEAL_MAX <= budget);
		assertTrue(DelveRules.FILL_MAX <= budget);
		assertTrue(DelveRules.SET_MAX <= budget);
		assertTrue(27 <= budget, "Deepway's 3x3x3 road");
		assertTrue(DelveRules.FLOOR_SIDE * DelveRules.FLOOR_SIDE <= budget);
		assertTrue(DelveRules.TORCH_MAX <= budget && DelveRules.LUMEN_MAX <= budget && DelveRules.SWITCH_MAX <= budget);
	}
}
