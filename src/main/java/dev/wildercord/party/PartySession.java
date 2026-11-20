package dev.wildercord.party;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Server-session names observed on authenticated players, with the same lifetime as their use. */
final class PartySession {
	final PartyRules rules = new PartyRules();
	private final Map<UUID, String> names = new HashMap<>();

	void remember(UUID player, String name) {
		names.put(player, name);
		rules.reconnect(player);
	}

	String name(UUID player) {
		return names.getOrDefault(player, player.toString());
	}

	void disconnect(UUID player) {
		// Cancel invitations first; membership and its authenticated offline name survive reconnect.
		rules.disconnect(player);
		dropUnreferenced(player);
	}

	void disconnect(UUID player, long now) {
		rules.disconnect(player, now);
		dropUnreferenced(player);
	}

	private void dropUnreferenced(UUID player) {
		if (!rules.references(player)) names.remove(player);
	}

	List<PartyRules.Promotion> prune(long now, Predicate<UUID> online) {
		List<PartyRules.Promotion> promotions = rules.prune(now, online);
		// Also collects offline names after kick/disband, once command feedback has used them.
		names.keySet().removeIf(player -> !online.test(player) && !rules.references(player));
		return promotions;
	}
}
