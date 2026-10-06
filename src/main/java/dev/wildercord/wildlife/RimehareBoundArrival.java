package dev.wildercord.wildlife;

import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

/** Recognizes a final native waypoint reached after travel, before navigation's next tick. */
final class RimehareBoundArrival {
	private RimehareBoundArrival() {}

	static boolean isAtFinalWaypoint(@Nullable Path path, double x, double y, double z,
		float horizontalTolerance, float verticalTolerance) {
		if (path == null || path.isDone() || path.getNextNodeIndex() != path.getNodeCount() - 1) return false;
		var node = path.getNextNodePos();
		// Match native ground following, including strict boundaries and the node's integer feet Y.
		return Math.abs(x - (node.getX() + .5)) < horizontalTolerance
			&& Math.abs(z - (node.getZ() + .5)) < horizontalTolerance
			&& Math.abs(y - node.getY()) < verticalTolerance;
	}
}
