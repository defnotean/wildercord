package dev.wildercord.net;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Ensures all WilderCord {@link ServerPlayConnectionEvents#DISCONNECT} listeners run on the owning
 * {@link MinecraftServer} thread.
 *
 * <p>Fabric invokes {@code ServerPlayConnectionEvents.DISCONNECT} from {@code Connection.channelInactive},
 * which runs on the client {@code Render thread} for singleplayer {@code LocalChannel}s and on Netty IO
 * threads for remote sockets. Mutating {@link ServerPlayer} attachments or world state off-thread races
 * {@code PlayerDataStorage.save(Player)} and, under {@code PlayerAttachmentThreadDiagnosticsMixin},
 * aborts {@code AbstractNetworkAddon.handleDisconnect()} before {@code endSession()} can release the
 * {@code ServerPlayNetworkAddon} from {@code GlobalReceiverRegistry.trackedAddons}.
 */
public final class ServerDisconnects {
	private static final List<ServerPlayConnectionEvents.Disconnect> LISTENERS = new CopyOnWriteArrayList<>();
	private static final Set<ServerGamePacketListenerImpl> PENDING =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	private static boolean initialized;

	private ServerDisconnects() {
	}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		ServerPlayConnectionEvents.DISCONNECT.register(ServerDisconnects::onDisconnect);
		ServerLifecycleEvents.SERVER_STOPPING.register(ServerDisconnects::flushAll);
	}

	public static void register(ServerPlayConnectionEvents.Disconnect listener) {
		init();
		LISTENERS.add(listener);
	}

	private static void onDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server) {
		if (handler == null || server == null) {
			return;
		}
		if (server.isSameThread()) {
			PENDING.remove(handler);
			dispatch(handler, server);
		} else {
			PENDING.add(handler);
			server.execute(() -> flush(handler, server));
		}
	}

	public static void flush(ServerGamePacketListenerImpl handler, MinecraftServer server) {
		if (handler != null && server != null && server.isSameThread() && PENDING.remove(handler)) {
			dispatch(handler, server);
		}
	}

	public static void flushPlayer(ServerPlayer player) {
		if (player != null && player.connection != null && player.level() != null) {
			flush(player.connection, player.level().getServer());
		}
	}

	public static void flushAll(MinecraftServer server) {
		if (server == null || !server.isSameThread() || server.getPlayerList() == null) {
			return;
		}
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			flushPlayer(player);
		}
	}

	private static void dispatch(ServerGamePacketListenerImpl handler, MinecraftServer server) {
		for (ServerPlayConnectionEvents.Disconnect listener : LISTENERS) {
			// One listener failing must not cost the rest their cleanup (a leaver's state would linger).
			try {
				listener.onPlayDisconnect(handler, server);
			} catch (RuntimeException e) {
				dev.wildercord.Wildercord.LOGGER.error("A disconnect listener ({}) failed", listener.getClass().getName(), e);
			}
		}
	}
}
