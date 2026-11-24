package dev.wildercord.bedrock;

import dev.wildercord.mixin.ServerCommonPacketListenerImplAccessor;
import io.netty.channel.local.LocalAddress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

/**
 * Identifies active Bedrock / Console sessions connected through Geyser or Floodgate while ensuring
 * Java-only environments never load optional Geyser/Floodgate classes.
 */
public final class BedrockPlayers {
	private BedrockPlayers() {}

	public static boolean isBedrock(Player player) {
		return player != null && isBedrock(player.getUUID());
	}

	public static boolean isBedrock(UUID uuid) {
		if (uuid == null || !BedrockBridge.isAnyLoaded()) {
			return false;
		}
		return ApiHooks.isBedrockUuid(uuid);
	}

	/**
	 * Used during the configuration phase before a ServerPlayer exists so Geyser/Floodgate players
	 * are not kicked by Fabric Registry Sync.
	 */
	public static boolean isBedrockSession(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
		if (handler == null || !BedrockBridge.isAnyLoaded()) {
			return false;
		}
		UUID uuid = handler.getOwner().id();
		if (isBedrock(uuid)) {
			return true;
		}
		if (BedrockBridge.isGeyserLoaded() && server != null && !server.isSingleplayerOwner(new NameAndId(handler.getOwner()))) {
			var conn = ((ServerCommonPacketListenerImplAccessor) handler).wildercord$connection();
			return conn != null && conn.getRemoteAddress() instanceof LocalAddress;
		}
		return false;
	}

	private static final class ApiHooks {
		static boolean isBedrockUuid(UUID uuid) {
			if (BedrockBridge.isFloodgateLoaded()) {
				try {
					FloodgateApi api = FloodgateApi.getInstance();
					if (api != null && (api.isFloodgateId(uuid) || api.isFloodgatePlayer(uuid))) {
						return true;
					}
				} catch (Throwable ignored) {
				}
			}
			if (BedrockBridge.isGeyserLoaded()) {
				try {
					GeyserApi geyser = GeyserApi.api();
					if (geyser != null && geyser.connectionByUuid(uuid) != null) {
						return true;
					}
				} catch (Throwable ignored) {
				}
			}
			return false;
		}
	}
}
