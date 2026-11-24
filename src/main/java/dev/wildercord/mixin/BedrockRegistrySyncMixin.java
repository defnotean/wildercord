package dev.wildercord.mixin;

import dev.wildercord.bedrock.BedrockPlayers;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allows Bedrock/Console players connected through Geyser & Floodgate to complete configuration
 * without being disconnected for lacking Fabric Registry Sync packets. Java clients still run full
 * Fabric Registry Sync unchanged.
 */
@Mixin(value = RegistrySyncManager.class, remap = false)
public final class BedrockRegistrySyncMixin {
	@Inject(method = "configureClient", at = @At("HEAD"), cancellable = true)
	private static void wildercord$skipBedrockRegistrySync(
		ServerConfigurationPacketListenerImpl handler,
		MinecraftServer server,
		CallbackInfo ci
	) {
		if (BedrockPlayers.isBedrockSession(handler, server)) {
			ci.cancel();
		}
	}
}
