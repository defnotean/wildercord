package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Actual nonempty baked-quads submission after both native display transformations. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class BraceNullItemDrawMixin {
	// 1.21.8: the outer state is field_55345, there is one ItemTransform, and the
	// render type is an argument of submitItem; the local pose is the PoseStack itself.
	@Shadow @Final private ItemStackRenderState field_55345;
	@Shadow private net.minecraft.client.resources.model.BakedModel model;
	@Shadow private net.minecraft.client.renderer.RenderType renderType;
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullDraw(ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int light, int overlay,
		int[] tints, net.minecraft.client.resources.model.BakedModel baked, net.minecraft.client.renderer.RenderType material, ItemStackRenderState.FoilType foil, Operation<Void> original) {
		var actual = new Matrix4f(pose.last().pose());
		var display = baked == null ? null : baked.getTransforms().getTransform(context);
		var quads = baked == null ? java.util.List.<BakedQuad>of() : baked.getQuads(null, null, 0L);
		original.call(context, pose, buffer, light, overlay, tints, baked, material, foil);
		BraceNullItemDrawProbe.drawn(field_55345, pose, buffer, context, display, new Matrix4f(pose.last().pose()), quads, actual);
	}
}
