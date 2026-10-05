package dev.wildercord.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.auraworld.AuraFighterRenderState;
import dev.wildercord.client.auraworld.MasterModel;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MasterModel.class)
public abstract class MastersNpcModelProbeMixin {
	// The articulated backend exits setupAnim early, so TAIL would only observe rigid fallback.
	@Inject(method = "setupAnim(Ldev/wildercord/client/auraworld/AuraFighterRenderState;)V", at = @At("RETURN"))
	private void wildercord$nativeModel(AuraFighterRenderState state, CallbackInfo ci) {
		MastersNpcCaptureProbe.model((MasterModel) (Object) this, state);
	}

	@Inject(method = "translateToHand(Ldev/wildercord/client/auraworld/AuraFighterRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
	private void wildercord$beforeNativeHand(AuraFighterRenderState state, HumanoidArm arm, PoseStack stack, CallbackInfo ci) {
		MastersNpcCaptureProbe.beforeHand(state, arm, stack);
	}

	@Inject(method = "translateToHand(Ldev/wildercord/client/auraworld/AuraFighterRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("RETURN"))
	private void wildercord$afterNativeHand(AuraFighterRenderState state, HumanoidArm arm, PoseStack stack, CallbackInfo ci) {
		MastersNpcCaptureProbe.hand((MasterModel) (Object) this, state, arm, stack);
	}
}
