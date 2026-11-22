package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Inert unless the exact Fabric shared-player screenshot call has armed its synchronous scope.
 * 1.21.4 has no CommandEncoder: the readback is NativeImage.downloadTexture inside
 * Screenshot.takeScreenshot, and that call is the copy stage. */
@Mixin(Screenshot.class)
public abstract class CounterPeerCopyMixin {
	@WrapOperation(method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lcom/mojang/blaze3d/platform/NativeImage;",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;downloadTexture(IZ)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$counter$copy(NativeImage image, int mip, boolean useSync, Operation<Void> original,
			@Local(argsOnly = true) RenderTarget target) {
		CounterPeerRenderProbe.copyEnqueued(target.getColorTextureId(), mip, target);
		original.call(image, mip, useSync);
	}
}
