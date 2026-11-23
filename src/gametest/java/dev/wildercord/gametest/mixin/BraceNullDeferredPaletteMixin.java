package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps primary and auxiliary native preparations distinct through their actual draw.
 * 1.21.8 has no submit node: the model drawing itself is the native body.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class BraceNullDeferredPaletteMixin {
	@WrapOperation(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), require = 1, expect = 1, allow = 1)
	private void prepareNativeBody(EntityModel<?> model, PoseStack pose, VertexConsumer vertices,
			int light, int overlay, int tint, Operation<Void> original,
			@Local(argsOnly = true) LivingEntityRenderState state) {
		float[] root = BraceNullCaptureProbe.passive(() -> pose.last().pose().get(new float[16]), null);
		BraceNullCaptureProbe.Deferred call = BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.deferredEnter(model, model, state, null, root), null);
		boolean completed = false;
		try {
			original.call(model, pose, vertices, light, overlay, tint);
			completed = true;
		} finally {
			boolean done = completed;
			BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.deferredLeave(call, done));
			BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.bodyDrawn(model, model, state, root));
		}
	}
}
