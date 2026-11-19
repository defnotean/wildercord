package dev.wildercord;

/** Game-time arithmetic that survives the sentinel a fresh state starts with. */
public final class Ticks {
	private Ticks() {}

	/** A field that has never been timed. Never subtract it raw ({@code now - NEVER} overflows): ask {@link #since}. */
	public static final long NEVER = Long.MIN_VALUE / 2;

	/**
	 * Ticks since {@code then}. A mark of {@link #NEVER}, one from before a restart (the clock moved back
	 * under it) or one old enough to overflow the subtraction all read as "long past", so idle and expiry
	 * checks pass instead of sticking on a sentinel that can never be caught up to. A map that must read a
	 * stale mark as <em>fresh</em> should clear on stop through {@link ServerState} rather than lean on this.
	 */
	public static long since(long then, long now) {
		if (then <= NEVER) {
			return Long.MAX_VALUE;
		}
		long delta = now - then;
		return delta < 0 ? Long.MAX_VALUE : delta;
	}
}
