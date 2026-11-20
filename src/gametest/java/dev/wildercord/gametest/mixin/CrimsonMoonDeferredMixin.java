package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Distinguish the actual deferred geometry preparation from setup calls for attached layers/items. */
@Mixin(ModelFeatureRenderer.class)
public abstract class CrimsonMoonDeferredMixin {
	@WrapOperation(method = "renderModel(Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$moon$deferred(Model<?> model, Object state, Operation<Void> original) {
		var call = CrimsonMoonRenderProbe.deferredEnter(model, state);
		try { original.call(model, state); }
		finally { CrimsonMoonRenderProbe.deferredLeave(call); }
	}
}
