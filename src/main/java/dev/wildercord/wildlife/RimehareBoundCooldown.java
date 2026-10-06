package dev.wildercord.wildlife;

/** A bound's recovery counts complete grounded navigation ticks, never flight or the landing tick. */
final class RimehareBoundCooldown {
	private int remaining;

	boolean tick(boolean groundedBeforeTravel, boolean groundedAfterTravel) {
		if (!groundedBeforeTravel || !groundedAfterTravel) return false;
		if (remaining > 0) remaining--;
		return remaining == 0;
	}

	void launched(int groundedTicks) {
		remaining = groundedTicks;
	}
}
