package dev.wildercord.gametest.mixin;

import dev.wildercord.client.auraworld.AuraFighterRenderState;
import dev.wildercord.client.auraworld.MasterModel;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MasterModel.class)
public abstract class MastersNpcModelProbeMixin {
	@Inject(method = "setupAnim(Ldev/wildercord/client/auraworld/AuraFighterRenderState;)V", at = @At("TAIL"))
	private void wildercord$nativeModel(AuraFighterRenderState state, CallbackInfo ci) {
		MastersNpcCaptureProbe.model((MasterModel) (Object) this, state);
	}
}
