package dev.wildercord.client.fx;

import dev.wildercord.aura.AuraFxRules;

import java.util.HashMap;
import java.util.Map;

/**
 * Aura's hit-stop, on the client only: as a heavy blow lands, the striker and the one struck hold still on this screen for a few
 * frames (milliseconds of real time, {@link AuraFxRules.Weight#hitStop}), then carry on. Nothing in the game waits: the world ticks
 * on and only how the two are drawn holds.
 *
 * <p>1.21.1 keeps the timing (who is held, for how long, and how many stops have run) but not the freeze itself: holding a pose
 * over an entity's look is done from its render state, and the render-state client is dropped here. The bookkeeping stays so the
 * settings screen and the game tests see the same behaviour as on the newer versions.</p>
 */
public final class HitStop {
	private HitStop() {}

	/** A held entity: until when (nanoseconds). */
	private static final class Held {
		final long until;

		Held(long until) {
			this.until = until;
		}
	}

	private static final Map<Integer, Held> HELD = new HashMap<>();
	private static long lastStart = Long.MIN_VALUE / 4;
	private static int stops;

	/** Holds these entities (by network id; -1 is skipped) for {@code millis} of real time, unless a hold began just now. */
	public static void hold(int millis, int... entities) {
		if (millis <= 0) {
			return;
		}
		long now = System.nanoTime();
		HELD.values().removeIf(held -> now >= held.until);
		if ((now - lastStart) / 1_000_000L < AuraFxRules.HIT_STOP_GAP) {
			return;
		}
		lastStart = now;
		stops++;
		long until = now + millis * 1_000_000L;
		for (int id : entities) {
			if (id >= 0) {
				HELD.put(id, new Held(until));
			}
		}
	}

	/** Whether anything is held now. */
	public static boolean holding() {
		long now = System.nanoTime();
		HELD.values().removeIf(h -> now >= h.until);
		return !HELD.isEmpty();
	}

	/** Lets go of everything (leaving a world). */
	public static void clear() {
		HELD.clear();
		lastStart = Long.MIN_VALUE / 4;
	}

	/** How many hit-stops began since the game started (the game tests read it). */
	public static int stops() {
		return stops;
	}
}
