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

	// 1.21.8 hands the model an arm and a stack only; the state it was posed for stays on the model.
	@Inject(method = "translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
	private void wildercord$beforeNativeHand(HumanoidArm arm, PoseStack stack, CallbackInfo ci) {
		MastersNpcCaptureProbe.beforeHand(((MasterModel) (Object) this).posedRenderState(), arm, stack);
	}

	@Inject(method = "translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("RETURN"))
	private void wildercord$afterNativeHand(HumanoidArm arm, PoseStack stack, CallbackInfo ci) {
		MasterModel model = (MasterModel) (Object) this;
		MastersNpcCaptureProbe.hand(model, model.posedRenderState(), arm, stack);
	}
}
