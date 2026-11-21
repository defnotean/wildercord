package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.gametest.ArticulatedSharedRenderProbe;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import dev.wildercord.gametest.MastersCaptureProbe;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.8 has no submit queue: the body draw is the model's own renderToBuffer call. */
@Mixin(LivingEntityRenderer.class)
public abstract class MastersBodyRenderProbeMixin {
	@WrapOperation(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$sharedBody(EntityModel<?, ?> model, PoseStack pose, VertexConsumer vertices,
			int light, int overlay, int color, Operation<Void> original,
			@Local(argsOnly = true) LivingEntityRenderState state) {
		var call = BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.submissionBegin(model, state, pose, null), null);
		boolean completed = false;
		try {
			original.call(model, pose, vertices, light, overlay, color);
			ArticulatedSharedRenderProbe.bodySubmitted(model, state);
			BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.submitted(model, state, pose));
			completed = true;
		} finally {
			boolean done = completed;
			BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.submissionEnd(call, done));
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
	private void wildercord$submittedBody(LivingEntityRenderState state, PoseStack pose,
			net.minecraft.client.renderer.MultiBufferSource buffer, int light, CallbackInfo ci) {
		MastersNpcCaptureProbe.body(state, pose);
		if (state instanceof PlayerRenderState avatar && !avatar.isInvisible && !avatar.isInvisibleToPlayer && !avatar.isSpectator)
			MastersCaptureProbe.body(avatar.id);
	}
}
