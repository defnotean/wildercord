package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.NativeSingleplayerClose;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FabricMC/fabric-api#5623: halt joins before Fabric's existing disconnect busy-wait hook is reachable. */
@Mixin(IntegratedServer.class)
public abstract class NativeIntegratedServerHaltMixin {
	@WrapOperation(method = "halt(Z)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/server/IntegratedServer;executeBlocking(Ljava/lang/Runnable;)V"), require = 1, allow = 1)
	private void wildercord$awaitNativeShutdown(IntegratedServer server, Runnable task, Operation<Void> original) {
		if (!NativeSingleplayerClose.execute(server, task)) original.call(server, task);
	}
}
