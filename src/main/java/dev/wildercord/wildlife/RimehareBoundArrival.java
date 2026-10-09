package dev.wildercord.wildlife;

import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

/** Recognizes a final native waypoint reached after travel, before navigation's next tick. */
final class RimehareBoundArrival {
	private static final double MIN_VELOCITY = .003;
	private static final int MAX_COAST_TICKS = 256;

	private RimehareBoundArrival() {}

	/** Read-only prediction: ordinary navigation resumes if native coasting cannot settle promptly. */
	static boolean canCoastToFinalWaypoint(@Nullable Path path, double x, double y, double z,
		double velocityX, double velocityZ, float horizontalTolerance, float verticalTolerance, float drag) {
		if (path == null || path.isDone() || path.getNextNodeIndex() != path.getNodeCount() - 1
			|| !Float.isFinite(horizontalTolerance) || horizontalTolerance <= 0
			|| !Float.isFinite(verticalTolerance) || verticalTolerance <= 0
			|| !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
			|| !Double.isFinite(velocityX) || !Double.isFinite(velocityZ)
			|| !Float.isFinite(drag) || drag < 0 || drag >= 1
			|| Math.abs(y - path.getNextNodePos().getY()) >= verticalTolerance
			|| Math.abs(velocityX) < MIN_VELOCITY && Math.abs(velocityZ) < MIN_VELOCITY) return false;
		for (int tick = 0; tick <= MAX_COAST_TICKS; tick++) {
			// LivingEntity clips each axis before travel; the current velocity moves before drag.
			if (Math.abs(velocityX) < MIN_VELOCITY) velocityX = 0;
			if (Math.abs(velocityZ) < MIN_VELOCITY) velocityZ = 0;
			if (velocityX == 0 && velocityZ == 0)
				return isAtFinalWaypoint(path, x, y, z, horizontalTolerance, verticalTolerance);
			if (tick == MAX_COAST_TICKS) return false;
			x += velocityX;
			z += velocityZ;
			velocityX *= drag;
			velocityZ *= drag;
		}
		return false;
	}

	/** Match LivingEntity's float ground friction and horizontal air drag, including both attributes. */
	static float groundDrag(float blockFriction, double frictionModifier, double airDragModifier) {
		float groundModifier = (float) frictionModifier, airModifier = (float) airDragModifier;
		if (!Float.isFinite(blockFriction) || !Float.isFinite(groundModifier) || !Float.isFinite(airModifier)) return Float.NaN;
		return Mth.clamp(1F - (1F - blockFriction) * groundModifier, 0F, 1F)
			* Mth.clamp(1F - (1F - .91F) * airModifier, 0F, 1F);
	}

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
