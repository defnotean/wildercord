package dev.wildercord.wildlife;

/** A bound's recovery counts complete grounded navigation ticks, never flight or the landing tick. */
final class RimehareBoundCooldown {
	private int remaining;

	boolean tick(boolean groundedBeforeTravel, boolean groundedAfterTravel) {
		if (!groundedBeforeTravel || !groundedAfterTravel) return false;
		if (remaining > 0) remaining--;
		return remaining == 0;
	}

	/** A full ground-input step must not jump across an entire cell between native waypoint samples. */
	boolean shouldBrake(double horizontalSpeed, double movementSpeed) {
		// Vanilla normalizes movement input above one, and otherwise applies speed to that input again.
		double inputImpulse = movementSpeed * Math.min(movementSpeed, 1.0);
		return remaining > 0 && horizontalSpeed + inputImpulse > 1.0;
	}

	void launched(int groundedTicks) {
		remaining = groundedTicks;
	}
}
