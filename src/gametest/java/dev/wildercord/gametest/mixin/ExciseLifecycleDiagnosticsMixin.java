package dev.wildercord.gametest.mixin;

import dev.wildercord.cast.ExciseCasting;
import dev.wildercord.gametest.ExciseLifecycleProbe;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ExciseCasting.class, remap = false)
public abstract class ExciseLifecycleDiagnosticsMixin {
	@Inject(method = "retire", at = @At("HEAD"), require = 1, allow = 1)
	private static void wildercord$beforeRetire(ServerPlayer player, CallbackInfo ci) {
		ExciseLifecycleProbe.record("retire_begin", player);
	}
	@Inject(method = "retire", at = @At("RETURN"), require = 1, allow = 1)
	private static void wildercord$afterRetire(ServerPlayer player, CallbackInfo ci) {
		ExciseLifecycleProbe.record("retire_end", player);
	}
}
