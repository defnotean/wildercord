package dev.wildercord.pet;

import java.util.UUID;

/** CPU-testable admission for the single temporary recovery ticket shared by every owner and dimension. */
final class CinnamonRecoveryRules {
	static final int TICKET_TICKS = 200, TICKET_RADIUS = 2, WHISTLE_TICKS = 200;
	static final int FOLLOW_DISTANCE = 32;
	private UUID owner;
	private long until;
	boolean acquire(UUID candidate, long now) {
		if (candidate == null || owner != null) return false;
		owner = candidate; until = now + TICKET_TICKS; return true;
	}
	boolean heldBy(UUID candidate) { return owner != null && owner.equals(candidate); }
	boolean expired(long now) { return owner != null && now >= until; }
	long deadline() { return until; }
	void clear() { owner = null; until = 0; }
}
