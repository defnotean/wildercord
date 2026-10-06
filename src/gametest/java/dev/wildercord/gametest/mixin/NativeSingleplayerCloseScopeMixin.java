package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.NativeSingleplayerClose;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.impl.client.gametest.context.TestSingleplayerContextImpl;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

/** Scope is the actual context's original server, retained only in the test context that already owns it. */
@Mixin(value = TestSingleplayerContextImpl.class, remap = false)
public abstract class NativeSingleplayerCloseScopeMixin {
	@Unique private ClientGameTestContext wildercord$context;
	@Unique private MinecraftServer wildercord$server;

	@Inject(method = "<init>", at = @At("RETURN"), require = 1, allow = 1)
	private void wildercord$captureServer(ClientGameTestContext context, TestWorldSave worldSave,
			MinecraftServer server, CallbackInfo ci) {
		wildercord$context = context; wildercord$server = server;
	}

	@WrapMethod(method = "close()V", require = 1, allow = 1)
	private void wildercord$scopeClose(Operation<Void> original) {
		NativeSingleplayerClose.close(wildercord$context, wildercord$server, () -> original.call());
	}
}
