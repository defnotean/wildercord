package dev.wildercord.spell;

import java.util.Map;

/**
 * Keeps the spells' per-creature timer maps from growing for ever: once one outgrows {@link #SOFT_CAP} entries, those
 * whose time is over are dropped on the next write. Below the cap nothing is touched, so a small server pays nothing.
 */
public final class StatePrune {
	private StatePrune() {}

	/** Entries a timer map may hold before a write sweeps it. */
	public static final int SOFT_CAP = 256;

	/** Drops deadlines already past {@code now} once {@code until} outgrows the cap; how many went. */
	public static <K> int expired(Map<K, Long> until, long now) {
		int before = until.size();
		if (before <= SOFT_CAP) {
			return 0;
		}
		until.values().removeIf(u -> u == null || u < now);
		return before - until.size();
	}

	/** Drops start times whose {@code rest} has run (or that lie in the future: a stale world clock) once {@code since} outgrows the cap. */
	public static <K> int rested(Map<K, Long> since, long now, long rest) {
		int before = since.size();
		if (before <= SOFT_CAP) {
			return 0;
		}
		since.values().removeIf(at -> at == null || now - at >= rest || at > now);
		return before - since.size();
	}
}
