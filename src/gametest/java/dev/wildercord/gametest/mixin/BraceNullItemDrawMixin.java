package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
	// 1.21.11: the outer state is field_55345, there is one ItemTransform, and the
	// render type is an argument of submitItem; the local pose is the PoseStack itself.
	@Shadow @Final private ItemStackRenderState field_55345;
	@Shadow private ItemTransform transform;
	@Shadow private net.minecraft.client.renderer.RenderType renderType;
	@WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullDraw(SubmitNodeCollector collector, PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline,
		int[] tints, List<BakedQuad> quads, net.minecraft.client.renderer.RenderType material, ItemStackRenderState.FoilType foil, Operation<Void> original) {
		var actual = new Matrix4f(pose.last().pose());
		original.call(collector, pose, context, light, overlay, outline, tints, quads, material, foil);
		BraceNullItemDrawProbe.drawn(field_55345, pose, collector, context, transform, new Matrix4f(pose.last().pose()), quads, actual);
	}
}
