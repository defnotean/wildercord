package dev.wildercord;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Static maps that hold per-session state must not outlive the world they were filled in: a stale pick,
 * greeting or game time carried into the next world reads to the player as a bug they cannot explain.
 * Register each map once here and it is cleared when the server stops; maps keyed by player can also
 * drop the leaver's entry on disconnect, which runs on the server thread through
 * {@link dev.wildercord.net.ServerDisconnects}.
 */
public final class ServerState {
	private static final List<Map<?, ?>> ON_STOP = new CopyOnWriteArrayList<>();
	private static final List<Map<UUID, ?>> ON_DISCONNECT = new CopyOnWriteArrayList<>();
	private static boolean initialised;

	private ServerState() {}

	/** Clear these maps whenever the server stops. */
	@SafeVarargs
	public static void resetOnStop(Map<?, ?>... maps) {
		init();
		for (Map<?, ?> map : maps) {
			ON_STOP.add(map);
		}
	}

	/** Drop the player's entry from these maps whenever they disconnect. */
	@SafeVarargs
	public static <V> void forgetOnDisconnect(Map<UUID, V>... maps) {
		init();
		for (Map<UUID, ?> map : maps) {
			ON_DISCONNECT.add(map);
		}
	}

	private static synchronized void init() {
		if (initialised) {
			return;
		}
		initialised = true;
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> runResets());
		dev.wildercord.net.ServerDisconnects.register((handler, server) -> runDisconnect(handler.player.getUUID()));
	}

	/** Clear every registered map. The stop hook and the unit test both come through here. */
	static void runResets() {
		for (Map<?, ?> map : ON_STOP) {
			map.clear();
		}
	}

	/** Drop the player's entry from every registered map. The disconnect hook and the unit test both come through here. */
	static void runDisconnect(UUID player) {
		for (Map<UUID, ?> map : ON_DISCONNECT) {
			map.remove(player);
		}
	}
}
