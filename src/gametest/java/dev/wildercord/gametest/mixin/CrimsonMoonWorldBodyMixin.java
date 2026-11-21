package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Exact vanilla body material and root submitted for the actual connected remote actor.
 * 1.21.8 has no submit queue: the body draw is the model's own renderToBuffer call.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class CrimsonMoonWorldBodyMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), require = 1, expect = 1, allow = 1)
    private void moon$body(EntityModel<?, ?> model, PoseStack pose, VertexConsumer vertices, int light, int overlay, int color,
        Operation<Void> original, @Local(argsOnly = true) LivingEntityRenderState state) {
        var root = new org.joml.Matrix4f(pose.last().pose());
        original.call(model, pose, vertices, light, overlay, color);
        CrimsonMoonRenderProbe.worldSubmitted(model, state, root, null);
    }
}
