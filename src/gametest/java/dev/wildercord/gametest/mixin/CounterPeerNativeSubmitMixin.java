package dev.wildercord.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Retains the original native submit node identity. 1.21.8 builds no node: the model drawing itself
 * is the submission, so the model stands in for the node and the buffer's render type is unknown here.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class CounterPeerNativeSubmitMixin {
	@Shadow @Final protected EntityModel<?> model;

	@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
	private void captureNativeSubmit(LivingEntityRenderState state, PoseStack pose,
			net.minecraft.client.renderer.MultiBufferSource buffer, int light, CallbackInfo ci) {
		EntityModel<?> drawn = model;
		float[] root = CounterPeerRenderProbe.passive(() -> pose.last().pose().get(new float[16]), null);
		CounterPeerRenderProbe.passive(() -> CounterPeerRenderProbe.nativeSubmitted(drawn, drawn, state, null, root, false));
	}
}
