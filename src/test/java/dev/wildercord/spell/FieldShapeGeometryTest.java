package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The shapes pack's pure side: 41 shapes, their block patterns, caps, numbers and Codex text. */
class FieldShapeGeometryTest {
	private static final int[][] FACINGS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	@Test
	void fortyOneShapesEachRegisteredAsAShapeWithItsCategory() {
		assertEquals(41, FieldShapeGeometry.PATHS.size());
		assertEquals(41, new HashSet<>(FieldShapeGeometry.PATHS).size(), "no path twice");
		assertEquals(Set.copyOf(FieldShapeGeometry.PATHS), FieldShapeGeometry.SPECS.keySet());
		long useful = 0;
		for (String path : FieldShapeGeometry.PATHS) {
			RuneDef rune = Runes.get("wildercord:" + path).orElseThrow(() -> new AssertionError(path));
			assertEquals(RuneFamily.SHAPE, rune.family(), path);
			FieldShapeGeometry.Spec spec = FieldShapeGeometry.spec(path);
			assertEquals(spec.category(), RuneCategories.categoryFor(path, RuneFamily.SHAPE), path);
			assertTrue(rune.tier() >= 1 && rune.tier() <= 3, path);
			assertTrue(rune.cost() > 0, path);
			assertFalse(rune.description().isBlank(), path);
			assertEquals(spec.category(), rune.category(), path);
			assertEquals(spec.strength(), SpellNumbers.shapeStrength(rune), 1e-9, path);
			assertTrue(spec.strength() > 0 && spec.strength() <= 1.2, path);
			if (spec.category().equals("field")) useful++;
		}
		assertTrue(useful >= 25, "at least 25 of them work the world as well as the fight");
	}

	@Test
	void onlyThePacksOwnIdsAreHandled() {
		assertEquals("furrow", FieldShapeGeometry.path("wildercord:furrow"));
		assertEquals("", FieldShapeGeometry.path("someaddon:furrow"));
		assertFalse(FieldShapeGeometry.handles(FieldShapeGeometry.path("someaddon:furrow")));
		assertFalse(FieldShapeGeometry.handles("bolt"));
		assertNull(FieldShapeGeometry.strength("bolt"));
		assertNull(FieldShapeGeometry.phrase("bolt", 1.0));
	}

	@Test
	void everyFieldStaysWithinTheBlockBudgetAtAnySize() {
		for (String path : FieldShapeGeometry.PATHS) {
			boolean kin = FieldShapeGeometry.spec(path).anchor() == FieldShapeGeometry.Anchor.SELF;
			for (double radius : new double[] {0.1, 0.5, 1.0, 1.5, 2.25, 5.0}) {
				for (int[] f : FACINGS) {
					List<FieldShapeGeometry.Cell> cells = FieldShapeGeometry.cells(path, f[0], f[1], radius);
					assertEquals(cells.size(), new HashSet<>(cells).size(), path + " repeats a block");
					if (kin) {
						assertTrue(cells.isEmpty(), path + " is a kin shape and has no blocks");
						continue;
					}
					assertFalse(cells.isEmpty(), path);
					if (!path.equals("lodeseek") && !path.equals("shoreline")) {
						assertTrue(cells.size() <= FieldShapeGeometry.MAX_CELLS, path + " " + cells.size());
					}
				}
			}
			assertNotNull(FieldShapeGeometry.phrase(path, 1.0), path);
			assertTrue(FieldShapeGeometry.limit(path) <= FieldShapeGeometry.MAX_CELLS);
		}
		assertEquals(FieldShapeGeometry.LODE_MAX, FieldShapeGeometry.limit("lodeseek"));
	}

	@Test
	void furrowRunsAheadAndShaftGoesStraightDown() {
		List<FieldShapeGeometry.Cell> furrow = FieldShapeGeometry.cells("furrow", 0, 1, 1.0);
		assertEquals(9, furrow.size());
		for (int i = 0; i < 9; i++) assertEquals(new FieldShapeGeometry.Cell(0, 0, i + 1), furrow.get(i));
		List<FieldShapeGeometry.Cell> shaft = FieldShapeGeometry.cells("shaft", 1, 0, 1.0);
		assertEquals(8, shaft.size());
		for (FieldShapeGeometry.Cell c : shaft) {
			assertEquals(0, c.x());
			assertEquals(0, c.z());
			assertTrue(c.y() <= 0);
		}
		// Widen makes it longer, Focus shorter; never under one block.
		assertEquals(18, FieldShapeGeometry.cells("shaft", 1, 0, 2.25).size());
		assertEquals(4, FieldShapeGeometry.cells("shaft", 1, 0, 0.1).size());
	}

	@Test
	void stairwellDescendsOneBlockPerStepWithHeadroom() {
		List<FieldShapeGeometry.Cell> stairs = FieldShapeGeometry.cells("stairwell", -1, 0, 1.0);
		assertEquals(24, stairs.size());
		for (int i = 1; i <= 8; i++) {
			for (int h = 0; h < 3; h++) {
				assertTrue(stairs.contains(new FieldShapeGeometry.Cell(-i, 1 - i + h, 0)), "step " + i + " height " + h);
			}
		}
		assertTrue(stairs.stream().allMatch(c -> c.z() == 0 && c.x() < 0), "laid along the facing only");
	}

	@Test
	void perimeterIsAnOutlineWithAnEmptyMiddle() {
		List<FieldShapeGeometry.Cell> ring = FieldShapeGeometry.cells("perimeter", 1, 0, 1.0);
		assertEquals(24, ring.size());
		assertTrue(ring.stream().allMatch(c -> Math.max(Math.abs(c.x()), Math.abs(c.z())) == 3 && c.y() == 0));
	}

	@Test
	void patternsTurnWithTheCasterAndCardinalPicksTheNearestAxis() {
		assertArrayEquals(new int[] {1, 0}, FieldShapeGeometry.cardinal(0.9, 0.3));
		assertArrayEquals(new int[] {0, -1}, FieldShapeGeometry.cardinal(0.2, -0.9));
		assertArrayEquals(new int[] {-1, 0}, FieldShapeGeometry.cardinal(-1, 1));
		// Seam lies across the facing: facing +z, it runs along x.
		assertTrue(FieldShapeGeometry.cells("seam", 0, 1, 1.0).stream().allMatch(c -> c.z() == 0 && c.y() == 0));
		assertTrue(FieldShapeGeometry.cells("seam", 1, 0, 1.0).stream().allMatch(c -> c.x() == 0 && c.y() == 0));
		assertEquals(7, FieldShapeGeometry.cells("seam", 1, 0, 1.0).size());
	}

	@Test
	void squaresAndSlabsHaveTheirSizes() {
		assertEquals(9, FieldShapeGeometry.cells("plot", 1, 0, 1.0).size());
		assertEquals(25, FieldShapeGeometry.cells("seedbed", 1, 0, 1.0).size());
		assertEquals(18, FieldShapeGeometry.cells("pit", 1, 0, 1.0).size());
		assertEquals(27, FieldShapeGeometry.cells("vault", 1, 0, 1.0).size());
		assertEquals(17, FieldShapeGeometry.cells("crossway", 1, 0, 1.0).size());
		assertEquals(5, FieldShapeGeometry.cells("lamplit", 1, 0, 1.0).size());
		assertEquals(13, FieldShapeGeometry.cells("lattice", 1, 0, 1.0).size());
		assertTrue(FieldShapeGeometry.cells("canopy", 1, 0, 1.0).stream().allMatch(c -> c.y() == 3));
		assertTrue(FieldShapeGeometry.cells("dome", 1, 0, 1.0).stream().allMatch(c -> c.y() >= 1));
	}

	@Test
	void kinReachScalesAndStaysBounded() {
		assertEquals(8, FieldShapeGeometry.reach("herd", 1.0), 1e-9);
		assertEquals(8 * FieldShapeGeometry.MAX_SCALE, FieldShapeGeometry.reach("herd", 10), 1e-9);
		assertEquals(8 * FieldShapeGeometry.MIN_SCALE, FieldShapeGeometry.reach("herd", 0.01), 1e-9);
	}
}
