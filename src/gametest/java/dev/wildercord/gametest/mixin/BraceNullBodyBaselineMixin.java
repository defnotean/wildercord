package dev.wildercord.gametest.mixin;

import dev.wildercord.client.MastersArtPose;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Capture the real untouched vanilla palette, never replay the production adapter as an oracle. */
@Mixin(MastersArtPose.class)
public abstract class BraceNullBodyBaselineMixin {
	@Inject(method = "apply", at = @At("HEAD"), require = 1)
	private static void wildercord$braceNullBaseline(PlayerModel model, PlayerRenderState state, CallbackInfoReturnable<Boolean> ci) {
		BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.bodyBefore(model, state));
	}
}
