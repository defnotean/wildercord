package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Retains the original native Submit identity; normal and outline constructors stay unchanged. */
@Mixin(SubmitNodeCollection.class)
public abstract class CounterPeerNativeSubmitMixin {
	@WrapOperation(method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V",
		at = @At(value = "NEW", target = "(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/model/Model;Ljava/lang/Object;IIILnet/minecraft/client/renderer/texture/UvMapping;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;"), require = 2, expect = 2, allow = 2)
	private ModelFeatureRenderer.Submit<?> captureNativeSubmit(RenderType material, PoseStack.Pose pose, Model<?> model, Object state,
		int light, int overlay, int tint, UvMapping uv, PoseStack.Pose decal, Operation<ModelFeatureRenderer.Submit<?>> original) {
		var node = original.call(material, pose, model, state, light, overlay, tint, uv, decal);
		CounterPeerRenderProbe.passive(() -> CounterPeerRenderProbe.nativeSubmitted(node, node.model(), node.state(), node.renderType(), node.pose().pose().get(new float[16]), material.isOutline()));
		return node;
	}
}
