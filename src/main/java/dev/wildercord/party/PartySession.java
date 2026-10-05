package dev.wildercord.party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Server-session names observed on authenticated players, with the same lifetime as their use. */
final class PartySession {
	final PartyRules rules = new PartyRules();
	private final Map<UUID, String> names = new HashMap<>();

	void remember(UUID player, String name) {
		names.put(player, name);
	}

	String name(UUID player) {
		return names.getOrDefault(player, player.toString());
	}

	void disconnect(UUID player) {
		// Cancel invitations first; membership and its authenticated offline name survive reconnect.
		rules.disconnect(player);
		if (!rules.references(player)) names.remove(player);
	}

	void prune(long now, Predicate<UUID> online) {
		rules.prune(now);
		// Also collects offline names after kick/disband, once command feedback has used them.
		names.keySet().removeIf(player -> !online.test(player) && !rules.references(player));
	}
}
