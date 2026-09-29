package dev.wildercord.client;

import dev.wildercord.net.VersionCheck;
import dev.wildercord.net.WildercordNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The client's half of the version check: a word in chat when the server's Wildercord differs from
 * ours. A server new enough says its version as we join; one from before the check says nothing, so a
 * server that has Wildercord but stays quiet for a few seconds is older than us.
 */
public final class VersionWatch {
	private VersionWatch() {}

	/** How long a server gets to say its version before it counts as older than the check, in ticks. */
	private static final int WAIT = 100;

	private static int waiting = -1;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(VersionCheck.ServerVersion.TYPE, (payload, context) -> {
			waiting = -1;
			Component warning = VersionCheck.mismatch(payload.version(), VersionCheck.version());
			if (warning != null && context.player() != null) {
				context.player().sendSystemMessage(warning);
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			waiting = ClientPlayNetworking.canSend(WildercordNetworking.CastSpell.TYPE) ? WAIT : -1);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> waiting = -1);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (waiting < 0 || --waiting > 0) {
				return;
			}
			waiting = -1;
			if (client.player != null) {
				String mine = VersionCheck.version();
				client.player.sendSystemMessage(Component.translatableWithFallback("message.wildercord.version_server_old",
					"This server runs an older Wildercord than yours (%s). Runes it doesn't know show as Silent Runes: the server needs updating, or install the version it runs.",
					mine).withStyle(ChatFormatting.YELLOW));
			}
		});
	}
}
