package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.wildercord.gametest.ArticulatedSharedRenderProbe;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.Consumer;

/** Inert unless the exact Fabric shared-player screenshot call has armed its synchronous scope. */
@Mixin(Screenshot.class)
public abstract class ArticulatedSharedCopyMixin {
	@WrapOperation(method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;copyTextureToBuffer(Lcom/mojang/blaze3d/textures/GpuTexture;Lcom/mojang/blaze3d/buffers/GpuBuffer;ILjava/lang/Runnable;I)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$copy(CommandEncoder encoder, GpuTexture texture, GpuBuffer buffer,
			int offset, Runnable callback, int mip, Operation<Void> original,
			@Local(argsOnly = true) RenderTarget target, @Local(argsOnly = true) Consumer<NativeImage> consumer) {
		ArticulatedSharedRenderProbe.copyEnqueued(texture, mip, target, consumer);
		original.call(encoder, texture, buffer, offset, callback, mip);
	}
}
