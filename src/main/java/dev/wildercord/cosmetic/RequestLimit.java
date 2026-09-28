package dev.wildercord.cosmetic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How often each player may ask for something (no Minecraft types, so it's unit-tested): at most
 * {@code max} requests in any {@code window} ticks, the rest dropped. Keeps a client that spams the
 * Cosmetics page's requests from making the server re-sync a style to everyone around many times a
 * tick. Not thread-safe; the server keeps one.
 */
public final class RequestLimit {
	private final int max;
	private final int window;
	private final Map<UUID, long[]> recent = new HashMap<>();

	public RequestLimit(int max, int window) {
		if (max < 1 || window < 1) {
			throw new IllegalArgumentException("a limit needs at least one request in at least one tick");
		}
		this.max = max;
		this.window = window;
	}

	/** Whether this player's request at {@code now} (in ticks) is allowed; an allowed one counts toward the limit. */
	public boolean allow(UUID player, long now) {
		long[] times = recent.computeIfAbsent(player, id -> {
			long[] fresh = new long[max];
			java.util.Arrays.fill(fresh, Long.MIN_VALUE);
			return fresh;
		});
		// The oldest of the last {@code max} requests: once it's out of the window, there's room for one more.
		int oldest = 0;
		for (int i = 1; i < times.length; i++) {
			if (times[i] < times[oldest]) {
				oldest = i;
			}
		}
		if (times[oldest] != Long.MIN_VALUE && now - times[oldest] < window) {
			return false;
		}
		times[oldest] = now;
		return true;
	}

	/** Forgets a player (they left). */
	public void forget(UUID player) {
		recent.remove(player);
	}

	public void clear() {
		recent.clear();
	}
}
