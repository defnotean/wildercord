package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Distinguishes real deferred body geometry from pre-render attachment setup calls. */
@Mixin(ModelFeatureRenderer.class)
public abstract class BraceNullDeferredPaletteMixin {
	@WrapOperation(method = "prepareModel(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullDeferred(Model<?> model, Object state, Operation<Void> original) {
		var call = BraceNullCaptureProbe.deferredEnter(model, state);
		try { original.call(model, state); }
		finally { BraceNullCaptureProbe.deferredLeave(call); }
	}
}
