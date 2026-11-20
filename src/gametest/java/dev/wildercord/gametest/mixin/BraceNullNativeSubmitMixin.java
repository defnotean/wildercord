package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Retains the original native submit node identity; the 26.1.2 record carries no render type, so the collector's own is used. */
@Mixin(SubmitNodeCollection.class)
public abstract class BraceNullNativeSubmitMixin {
	@WrapOperation(method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
		at = @At(value = "NEW", target = "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/model/Model;Ljava/lang/Object;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;"), require = 1, expect = 1, allow = 1)
	private SubmitNodeStorage.ModelSubmit<?> captureNativeSubmit(PoseStack.Pose pose, Model<?> model, Object state,
		int light, int overlay, int tint, TextureAtlasSprite uv, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling,
		Operation<SubmitNodeStorage.ModelSubmit<?>> original, @Local(argsOnly = true) RenderType material) {
		var node = original.call(pose, model, state, light, overlay, tint, uv, outlineColor, crumbling);
		BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.nativeSubmitted(node, node.model(), node.state(), material, node.pose().pose().get(new float[16]), material.isOutline()));
		return node;
	}
}
