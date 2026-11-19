package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The registry that keeps per-session maps from outliving their world must actually clear them. */
class ServerStateRegistryTest {
	@Test
	void registeredMapsClearOnStopAndDisconnect() {
		Map<UUID, String> picks = new HashMap<>();
		ServerState.resetOnStop(picks);
		ServerState.forgetOnDisconnect(picks);
		UUID player = UUID.randomUUID();

		picks.put(player, "hearty stew");
		ServerState.runDisconnect(player);
		assertTrue(picks.isEmpty(), "a leaver's entry should go with them");

		picks.put(player, "hearty stew");
		ServerState.runResets();
		assertTrue(picks.isEmpty(), "the world's picks should not outlive it");
	}
}
