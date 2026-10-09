package dev.wildercord.gametest.mixin;

import dev.wildercord.aura.world.StoneMarchPulseProbe;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads the original return before pulse victim selection/damage; never cancels or replaces it. */
@Mixin(targets = "dev.wildercord.aura.world.StoneMarch", remap = false)
public abstract class StoneMarchPulseProbeMixin {
	@Inject(method = "canHit(Lnet/minecraft/server/level/ServerPlayer;I)Z", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
	private void wildercord$originalMarchEligibility(ServerPlayer player, int pulse, CallbackInfoReturnable<Boolean> result) {
		StoneMarchPulseProbe.originalReturn(this, player, pulse, result.getReturnValueZ());
	}
}
