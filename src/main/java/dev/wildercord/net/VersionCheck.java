package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
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

/**
 * Tells a player whose Wildercord doesn't match the server's which version to install. A rune one
 * version knows and the other doesn't shows up as a Silent Rune, so a mismatch is worth saying out loud
 * on joining rather than leaving people to wonder. A client new enough to hear the server's version
 * compares the two itself; an older one, which can't, is told by the server.
 */
public final class VersionCheck {
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

	/** This game's Wildercord version, as the mod list shows it ("0.4.1-alpha+mc26.3"). */
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
			"This server runs Wildercord %s, and you have %s. Runes one of them doesn't know show as Silent Runes: install %s to match.",
			server, mine, server).withStyle(ChatFormatting.YELLOW);
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(ServerVersion.TYPE, ServerVersion.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			String mine = version();
			if (ServerPlayNetworking.canSend(handler.player, ServerVersion.TYPE)) {
				sender.sendPacket(new ServerVersion(mine));
			} else if (ServerPlayNetworking.canSend(handler.player, Config.Sync.TYPE)) {
				// Wildercord, but a version from before this check: older than the server's, so say it from here.
				handler.player.sendSystemMessage(Component.translatableWithFallback("message.wildercord.version_old",
					"This server runs Wildercord %s, newer than yours. Update to %s, or runes your version doesn't know will show as Silent Runes.",
					mine, mine).withStyle(ChatFormatting.YELLOW));
			}
		});
	}
}
