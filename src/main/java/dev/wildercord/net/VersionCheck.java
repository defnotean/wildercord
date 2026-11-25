package dev.wildercord.net;

import dev.wildercord.ServerState;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Tells a player whose Wildercord doesn't match the server's which version to install. A rune one
 * version knows and the other doesn't shows up as a Silent Rune, and older clients can miss newer UI,
 * abilities, or synced attachments, so a mismatch is surfaced on join on both client and server.
 */
public final class VersionCheck {
	private static final int ACK_TIMEOUT_TICKS = 80;
	private static final Map<UUID, Integer> AWAITING_ACK = new HashMap<>();

	private VersionCheck() {}

	/** Server to client: the server's Wildercord version, sent on joining. */
	public record ServerVersion(String version) implements CustomPacketPayload {
		public static final Type<ServerVersion> TYPE = new Type<>(Wildercord.id("server_version"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ServerVersion> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(64), ServerVersion::version, ServerVersion::new).cast();

		@Override
		public Type<ServerVersion> type() {
			return TYPE;
		}
	}

	/** Client to server: the client's Wildercord version, returned once {@link ServerVersion} is received. */
	public record ClientVersion(String version) implements CustomPacketPayload {
		public static final Type<ClientVersion> TYPE = new Type<>(Wildercord.id("client_version"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ClientVersion> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(64), ClientVersion::version, ClientVersion::new).cast();

		@Override
		public Type<ClientVersion> type() {
			return TYPE;
		}
	}

	/** This game's Wildercord version, as the mod list shows it ("0.16.1-alpha+mc26.3"). */
	public static String version() {
		return FabricLoader.getInstance().getModContainer(Wildercord.MOD_ID)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
			.orElse("?");
	}

	/** What a client says when its version and the server's differ (null when they match). */
	public static Component mismatch(String server, String mine) {
		if (server.equals(mine)) {
			return null;
		}
		return Component.translatableWithFallback("message.wildercord.version_mismatch",
			"This server runs Wildercord %s, and you have %s. Update to %s to avoid missing UI, attachments, abilities, or Silent Runes.",
			server, mine, server).withStyle(ChatFormatting.YELLOW);
	}

	/** Warning sent from the server when an older Wildercord client cannot complete the version handshake. */
	public static Component olderClientWarning(String serverVersion) {
		return Component.translatableWithFallback("message.wildercord.version_old",
			"Your Wildercord client is older than the server (%s) — update to %s to avoid missing UI, attachments, or abilities.",
			serverVersion, serverVersion).withStyle(ChatFormatting.YELLOW);
	}

	public static void init() {
		ServerState.forgetOnDisconnect(AWAITING_ACK);
		ServerState.resetOnStop(AWAITING_ACK);
		PayloadTypeRegistry.clientboundPlay().register(ServerVersion.TYPE, ServerVersion.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ClientVersion.TYPE, ClientVersion.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ClientVersion.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			AWAITING_ACK.remove(player.getUUID());
			String mine = version();
			if (!payload.version().equals(mine)) {
				Wildercord.LOGGER.warn("Player {} connected with Wildercord {} (server runs {})",
					player.getName().getString(), payload.version(), mine);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			String mine = version();
			ServerPlayer player = handler.player;
			if (ServerPlayNetworking.canSend(player, ServerVersion.TYPE)) {
				sender.sendPacket(new ServerVersion(mine));
				AWAITING_ACK.put(player.getUUID(), ACK_TIMEOUT_TICKS);
			} else if (ServerPlayNetworking.canSend(player, Config.Sync.TYPE)) {
				// Wildercord, but a version from before this check: older than the server's, so say it from here.
				Wildercord.LOGGER.warn("Player {} joined with pre-handshake Wildercord client (server runs {})",
					player.getName().getString(), mine);
				player.sendSystemMessage(olderClientWarning(mine));
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (AWAITING_ACK.isEmpty()) {
				return;
			}
			String mine = version();
			Iterator<Map.Entry<UUID, Integer>> it = AWAITING_ACK.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Integer> entry = it.next();
				int remaining = entry.getValue() - 1;
				if (remaining > 0) {
					entry.setValue(remaining);
					continue;
				}
				it.remove();
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player != null) {
					Wildercord.LOGGER.warn("Player {} did not answer version handshake; client predates Wildercord {} client ack",
						player.getName().getString(), mine);
					player.sendSystemMessage(olderClientWarning(mine));
				}
			}
		});
	}
}
