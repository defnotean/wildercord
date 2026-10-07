package dev.wildercord.pet;

import java.util.UUID;

/** Missing one durable identity record is uncertainty, never a new-companion invitation. */
final class CinnamonIdentityRules {
	enum Status { NEW, REGISTERED, UNCERTAIN }
	private CinnamonIdentityRules() {}
	static boolean mayRetire(UUID body, UUID journalIdentity, UUID ownerMarker, boolean retired) {
		return retired && body != null && body.equals(journalIdentity) && ownerMarker != null && ownerMarker.equals(journalIdentity);
	}
	static Status status(UUID ownerMarker, UUID journalIdentity, boolean quarantinedOwner) {
		if (journalIdentity == null) return ownerMarker == null && !quarantinedOwner ? Status.NEW : Status.UNCERTAIN;
		return ownerMarker == null || ownerMarker.equals(journalIdentity) ? Status.REGISTERED : Status.UNCERTAIN;
	}
}
