package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RimehareBoundArrivalTest {
	private static final float HORIZONTAL = .75F - .45F / 2;
	private static final float VERTICAL = 1;

	@Test
	void theLoggedSlabArrivalDoesNotNeedAnotherBound() {
		Path path = atLastNode(true);
		assertTrue(reached(path, .5039762109022629, 100.5, -11.532875655621492));
		assertFalse(path.isDone(), "Only the next native navigation tick consumes the reached waypoint");
		assertEquals(1, path.getNextNodeIndex());
		assertEquals(new BlockPos(0, 101, -13), path.getTarget());
	}

	@Test
	void intermediateWaypointsKeepNormalBounding() {
		Path path = route(true);
		assertFalse(reached(path, .5, 101, -10.5));
		assertFalse(reached(path, .5, 100.5, -11.5), "Being near the end cannot skip an intermediate waypoint");
		assertEquals(0, path.getNextNodeIndex());
	}

	@Test
	void missingAndConsumedPathsCannotCreateArrivalIntent() {
		assertFalse(reached(null, .5, 100.5, -11.5));
		Path path = atLastNode(true);
		path.advance();
		assertFalse(reached(path, .5, 100.5, -11.5));
		assertFalse(reached(new Path(List.of(), new BlockPos(0, 101, -13), false), .5, 100.5, -11.5));
	}

	@Test
	void aPartialPathsActualEndStillMustNotStartAnExtraBound() {
		Path path = atLastNode(false);
		assertTrue(reached(path, .5, 100.5, -11.5));
		assertFalse(path.canReach(), "Recognizing the final waypoint does not claim the requested target was reached");
		assertFalse(path.isDone());
	}

	@Test
	void horizontalArrivalUsesBothStrictNativeBoundaries() {
		Path path = atLastNode(true);
		for (int sign : new int[] {-1, 1}) {
			assertTrue(reached(path, .5 + sign * (HORIZONTAL - .000001), 100.5, -11.5));
			assertFalse(reached(path, .5 + sign * HORIZONTAL, 100.5, -11.5));
			assertFalse(reached(path, .5 + sign * (HORIZONTAL + .000001), 100.5, -11.5));
			assertTrue(reached(path, .5, 100.5, -11.5 + sign * (HORIZONTAL - .000001)));
			assertFalse(reached(path, .5, 100.5, -11.5 + sign * HORIZONTAL));
			assertFalse(reached(path, .5, 100.5, -11.5 + sign * (HORIZONTAL + .000001)));
		}
	}

	@Test
	void horizontalArrivalUsesTheNativeSquareRatherThanARadius() {
		Path path = atLastNode(true);
		for (int xSign : new int[] {-1, 1}) for (int zSign : new int[] {-1, 1}) {
			assertTrue(reached(path, .5 + xSign * (HORIZONTAL - .000001), 100.5,
				-11.5 + zSign * (HORIZONTAL - .000001)));
		}
	}

	@Test
	void verticalArrivalUsesTheIntegerNodeHeightAndStrictBoundary() {
		Path path = atLastNode(true);
		for (int sign : new int[] {-1, 1}) {
			assertTrue(reached(path, .5, 101 + sign * (VERTICAL - .000001), -11.5));
			assertFalse(reached(path, .5, 101 + sign * VERTICAL, -11.5));
			assertFalse(reached(path, .5, 101 + sign * (VERTICAL + .000001), -11.5));
		}
	}

	private static boolean reached(Path path, double x, double y, double z) {
		return RimehareBoundArrival.isAtFinalWaypoint(path, x, y, z, HORIZONTAL, VERTICAL);
	}

	private static Path route(boolean canReach) {
		return new Path(List.of(new Node(0, 101, -11), new Node(0, 101, -12)), new BlockPos(0, 101, -13), canReach);
	}

	private static Path atLastNode(boolean canReach) {
		Path path = route(canReach);
		path.advance();
		return path;
	}
}
