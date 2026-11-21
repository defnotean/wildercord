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
 * Distinguish the actual deferred geometry preparation from setup calls for attached layers/items.
 * 1.21.8 has no submit queue: the model's own draw is the deferred step.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class CrimsonMoonDeferredMixin {
	@WrapOperation(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$moon$deferred(EntityModel<?, ?> model, PoseStack pose, VertexConsumer vertices,
			int light, int overlay, int tint, Operation<Void> original,
			@Local(argsOnly = true) LivingEntityRenderState state) {
		var call = CrimsonMoonRenderProbe.deferredEnter(model, state);
		try { original.call(model, pose, vertices, light, overlay, tint); }
		finally { CrimsonMoonRenderProbe.deferredLeave(call); }
	}
}
