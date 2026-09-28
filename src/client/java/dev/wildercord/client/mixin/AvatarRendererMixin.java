package dev.wildercord.client.mixin;

import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player charging a spell holds both hands out in front, pushing the magic circle open (the pose
 * of drawing a bow, which raises both arms to follow the head). Everyone around sees it.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$castingPose(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
		if (avatar.hasAttached(WildercordAttachments.CHARGE)) {
			if (state.mainArm == HumanoidArm.LEFT) {
				state.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
			} else {
				state.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
			}
		}
	}
}
