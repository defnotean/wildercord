package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class LightLureSearchTest {
	private record Cell(int x, int y, int z) {}
	private static final class Field implements LightLureSearch.Cells {
		final Map<Cell, Integer> lamps = new HashMap<>();
		final Set<Cell> blocked = new HashSet<>();
		final List<Cell> reads = new ArrayList<>();
		int lastResidentX = 100;
		Field lamp(int x, int y, int z, int light) { lamps.put(new Cell(x, y, z), light); return this; }
		public int airLight(int x, int y, int z) {
			Cell at = new Cell(x, y, z); reads.add(at);
			if (x > lastResidentX || blocked.contains(at) || lamps.containsKey(at)) return -1;
			int value = 0;
			for (var lamp : lamps.entrySet()) {
				Cell source = lamp.getKey();
				value = Math.max(value, lamp.getValue() - Math.abs(x - source.x) - Math.abs(y - source.y) - Math.abs(z - source.z));
			}
			return value;
		}
	}
	private static LightLureSearch.Candidate find(Field field, int x, int y, int z) {
		var found = LightLureSearch.find(field, x, y, z, 10);
		assertTrue(field.reads.size() <= 49, "Bounded current-cell and neighbor reads");
		assertTrue(field.reads.stream().allMatch(p -> Math.abs(p.x - x) <= 8 && Math.abs(p.y - y) <= 4 && Math.abs(p.z - z) <= 8));
		return found;
	}
	@Test void realFailureGeometryFindsLanternFromCurrentLightInsteadOfLuckySamples() {
		var field = new Field().lamp(30, 101, -1, 15);
		var found = find(field, 33, 101, -1);
		assertEquals(new LightLureSearch.Candidate(31, 101, -1, 14), found);
		field = new Field().lamp(30, 101, -1, 15);
		found = find(field, 36, 102, 0);
		assertNotNull(found); assertEquals(14, found.light());
	}
	@Test void darknessAndBelowThresholdLightNeverAcquire() {
		assertNull(find(new Field(), 0, 0, 0));
		assertNull(find(new Field().lamp(3, 0, 0, 10), 0, 0, 0), "Emitter itself is not air; surrounding light stays below ten");
	}
	@Test void brighterLocalGradientWinsWithMultipleSources() {
		var field = new Field().lamp(-3, 0, 0, 12).lamp(3, 0, 0, 15);
		assertEquals(new LightLureSearch.Candidate(2, 0, 0, 14), find(field, 0, 0, 0));
	}
	@Test void blockedCellIsNeverAPathAndDoesNotCauseAPlateauLoop() {
		var field = new Field().lamp(3, 0, 0, 15);
		field.blocked.add(new Cell(1, 0, 0));
		assertEquals(new LightLureSearch.Candidate(0, 0, 0, 12), find(field, 0, 0, 0));
		assertEquals(7, field.reads.size(), "No repeated traversal of a local maximum");
		field = new Field(); field.blocked.add(new Cell(0, 0, 0));
		assertNull(find(field, 0, 0, 0)); assertEquals(1, field.reads.size());
	}
	@Test void unloadedEdgeDefersTheUnavailableBrighterCell() {
		var field = new Field().lamp(4, 0, 0, 15); field.lastResidentX = 1;
		assertEquals(new LightLureSearch.Candidate(1, 0, 0, 12), find(field, 0, 0, 0));
		assertTrue(field.reads.stream().noneMatch(p -> p.x > 2), "Never step into the unavailable edge to inspect beyond it");
	}
	@Test void longGradientsStopAtEightStepsAndVerticalEnvelope() {
		var field = new Field().lamp(14, 0, 0, 15);
		assertNull(find(field, 0, 0, 0));
		assertEquals(49, field.reads.size());
		field = new Field().lamp(0, 7, 0, 15);
		assertEquals(new LightLureSearch.Candidate(0, 4, 0, 12), find(field, 0, 0, 0));
	}
	@Test void tiesAreStableAndNeitherLevelNorDecreasingLightCanExtendTheSearch() {
		var field = new Field().lamp(-2, 0, 0, 15).lamp(2, 0, 0, 15);
		assertEquals(new LightLureSearch.Candidate(-1, 0, 0, 14), find(field, 0, 0, 0));
		assertEquals(13, field.reads.size());
	}
}
