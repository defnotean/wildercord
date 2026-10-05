package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.client.combat.ArticulatedModelAccess;
import dev.wildercord.client.combat.ArticulatedRig;
import dev.wildercord.client.combat.ArticulatedViewModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Models keep both backends, but exactly one may own an accepted frame's body and hand sockets. */
@Mixin(PlayerModel.class)
public abstract class ArticulatedPlayerModelMixin implements ArticulatedModelAccess {
	@Unique private ArticulatedRig wildercord$rig;
	@Unique private ArticulatedViewModel wildercord$view;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void wildercord$createRig(ModelPart root, boolean slim, CallbackInfo ci) {
		wildercord$rig = new ArticulatedRig(slim, false);
		wildercord$rig.attach(root);
		wildercord$view = new ArticulatedViewModel(slim);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("HEAD"))
	private void wildercord$restoreBody(AvatarRenderState state, CallbackInfo ci) {
		if (wildercord$rig != null) wildercord$rig.restoreRigid((PlayerModel) (Object) this);
	}

	@Inject(method = "translateToHand(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
		at = @At("HEAD"), cancellable = true)
	private void wildercord$hand(AvatarRenderState state, HumanoidArm arm, PoseStack stack, CallbackInfo ci) {
		var frame = ArticulatedCombat.frame(state);
		if (frame == null || ((Object) this).getClass() != PlayerModel.class) return;
		ArticulatedCombat.translateHeld(wildercord$rig, (PlayerModel) (Object) this, state, arm, stack);
		ci.cancel();
	}

	@Override public ArticulatedRig wildercord$rig() { return wildercord$rig; }
	@Override public ArticulatedViewModel wildercord$viewModel() { return wildercord$view; }
}
