package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps primary and auxiliary native preparations distinct through their actual draw. */
@Mixin(ModelFeatureRenderer.class)
public abstract class CounterPeerDeferredMixin {
	@WrapMethod(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V", require = 1, expect = 1, allow = 1)
	private void prepareNativeBody(ModelFeatureRenderer.Submit<?> node, Operation<Void> original) {
		var call = CounterPeerRenderProbe.passive(() -> CounterPeerRenderProbe.deferredEnter(node, node.model(), node.state(), node.renderType(), node.pose().pose().get(new float[16])), null);
		boolean completed = false;
		try { original.call(node); completed = true; }
		finally { boolean done = completed; CounterPeerRenderProbe.passive(() -> CounterPeerRenderProbe.deferredLeave(call, done)); }
	}
	@WrapOperation(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), require = 1, expect = 1, allow = 1)
	private void drawnNativeBody(Model<?> model, PoseStack pose, VertexConsumer vertices, int light, int overlay, int tint,
		Operation<Void> original, @Local(argsOnly = true) ModelFeatureRenderer.Submit<?> node) {
		float[] root = CounterPeerRenderProbe.passive(() -> pose.last().pose().get(new float[16]), null);
		original.call(model, pose, vertices, light, overlay, tint);
		CounterPeerRenderProbe.passive(() -> CounterPeerRenderProbe.bodyDrawn(node, model, node.state(), root));
	}
}
