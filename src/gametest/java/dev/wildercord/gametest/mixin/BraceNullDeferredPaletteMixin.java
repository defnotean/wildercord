package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps primary and auxiliary native preparations distinct through their actual draw. */
@Mixin(ModelFeatureRenderer.class)
public abstract class BraceNullDeferredPaletteMixin {
	@WrapMethod(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", require = 1, expect = 1, allow = 1)
	private void prepareNativeBody(SubmitNodeStorage.ModelSubmit<?> node, RenderType renderType, VertexConsumer vertices,
			OutlineBufferSource outline, MultiBufferSource.BufferSource buffer, Operation<Void> original) {
		var call = BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.deferredEnter(node, node.model(), node.state(), renderType, node.pose().pose().get(new float[16])), null);
		boolean completed = false;
		try { original.call(node, renderType, vertices, outline, buffer); completed = true; }
		finally { boolean done = completed; BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.deferredLeave(call, done)); }
	}
	@WrapOperation(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), require = 1, expect = 3, allow = 3)
	private void drawnNativeBody(Model<?> model, PoseStack pose, VertexConsumer vertices, int light, int overlay, int tint,
		Operation<Void> original, @Local(argsOnly = true) SubmitNodeStorage.ModelSubmit<?> node) {
		float[] root = BraceNullCaptureProbe.passive(() -> pose.last().pose().get(new float[16]), null);
		original.call(model, pose, vertices, light, overlay, tint);
		BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.bodyDrawn(node, model, node.state(), root));
	}
}
