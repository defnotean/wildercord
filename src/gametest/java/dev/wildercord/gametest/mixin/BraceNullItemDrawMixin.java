package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Actual nonempty baked-quads submission after both native display transformations. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class BraceNullItemDrawMixin {
	@Shadow @Final private ItemStackRenderState this$0;
	@Shadow private ItemTransform itemTransform;
	@Shadow @Final private Matrix4f localTransform;
	@WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILnet/minecraft/client/resources/model/geometry/ItemQuads;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullDraw(SubmitNodeCollector collector, PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline,
		int[] tints, ItemQuads quads, ItemStackRenderState.FoilType foil, Operation<Void> original) {
		var actual = new Matrix4f(pose.last().pose());
		original.call(collector, pose, context, light, overlay, outline, tints, quads, foil);
		BraceNullItemDrawProbe.drawn(this$0, pose, collector, context, itemTransform, localTransform, quads, actual);
	}
}
