package dev.wildercord.aura;

/** One counter can reserve only the existing finite clash which actually accepted it. Server ticks never reset. */
final class EarnedCounterReservation {
	enum Take { VALID, STALE, UNAVAILABLE }
	private Object clash;
	private long heldAt;
	private boolean taken;
	void hold(Object key, long now) {
		if (key == null || clash != null || taken) throw new IllegalStateException("A counter may enter only one clash");
		clash = key; heldAt = now;
	}
	Take take(Object key, long now) {
		if (taken || key != clash) return Take.UNAVAILABLE;
		taken = true;
		return clash != null && (now < heldAt || now - heldAt > ClashRules.serverLength()) ? Take.STALE : Take.VALID;
	}
}
