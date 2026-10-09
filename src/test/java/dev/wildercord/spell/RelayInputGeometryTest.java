package dev.wildercord.spell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelayInputGeometryTest {
	@Test void repeatedOrReorderedPacketsCannotCommitHeldInput() {
		var edges = new RelayInputRules.Edges();
		assertTrue(edges.accept(0, 4, 10));
		assertFalse(edges.accept(0, 4, 11));
		assertFalse(edges.accept(0, 5, 11));
		assertFalse(edges.accept(1, 3, 11));
		assertFalse(edges.accept(0, 6, 12));
		assertTrue(edges.accept(1, 7, 12));
		assertTrue(edges.accept(0, 8, 13));
		assertTrue(edges.accept(1, 9, 13));
		assertFalse(edges.accept(0, 10, 13));
		assertTrue(edges.accept(0, 11, 14));
	}
	@Test void malformedActionsCannotResetFreshness() {
		var edges = new RelayInputRules.Edges();
		assertTrue(edges.accept(0, 1, 1));
		assertFalse(edges.accept(100, 2, 2));
		assertFalse(edges.accept(1, -4, 2));
		assertFalse(edges.accept(0, 3, 3));
		assertTrue(edges.accept(2, 4, 3));
		assertFalse(edges.accept(2, 3, 4));
		assertTrue(edges.accept(0, 5, 4));
	}
	@Test void tinyDiagonalCornerCannotSkipAnUnloadedOrWardedCell() {
		var forward = RelayGeometry.cells(.2, 1.2, .2001, 4.2, 1.2, 4.2001);
		assertTrue(forward.contains(new RelayGeometry.Cell(0,1,1)), "The thin crossed wedge is covered");
		var backwards = RelayGeometry.cells(4.2, 1.2, 4.2001, .2, 1.2, .2001);
		assertEquals(new java.util.HashSet<>(forward), new java.util.HashSet<>(backwards));
	}
	@Test void exactGridBoundaryIncludesBothSidesAndRefusesUnboundedGeometry() {
		var cells = RelayGeometry.cells(0, 2, 0, 0, 2, 16);
		assertTrue(cells.contains(new RelayGeometry.Cell(-1,1,4)));
		assertTrue(cells.contains(new RelayGeometry.Cell(0,2,4)));
		assertTrue(cells.size() < 100);
		assertTrue(RelayGeometry.cells(0,0,0,100,0,0).isEmpty());
		assertTrue(RelayGeometry.cells(Double.NaN,0,0,1,0,0).isEmpty());
	}
}
