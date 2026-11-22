package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl;
import net.fabricmc.fabric.impl.client.gametest.screenshot.TestScreenshotCommonOptionsImpl;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/** Exact audited Fabric 4.1.1 (1.21.4) call sites: doTakeScreenshot drives render and the
 * NativeImage-returning Screenshot.takeScreenshot. Missing hooks must fail application, never
 * silently degrade. */
@Mixin(value = ClientGameTestContextImpl.class, remap = false)
public abstract class CounterPeerScreenshotMixin {
	@WrapMethod(method = "takeScreenshot(Lnet/fabricmc/fabric/api/client/gametest/v1/screenshot/TestScreenshotOptions;)Ljava/nio/file/Path;", require = 1, expect = 1, allow = 1)
	private Path wildercord$counter$receipt(TestScreenshotOptions options, Operation<Path> original) {
		var token = CounterPeerRenderProbe.begin(options);
		if (token == null) return original.call(options);
		try {
			Path path;
			try { path = original.call(options); }
			catch (Throwable failure) { CounterPeerRenderProbe.failed(token, failure); throw failure; }
			CounterPeerRenderProbe.success(token, path);
			return path;
		} finally { CounterPeerRenderProbe.unregister(options, token); }
	}

	@WrapMethod(method = "doTakeScreenshot(Lnet/minecraft/client/Minecraft;Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;)Lcom/mojang/blaze3d/platform/NativeImage;", require = 1, expect = 1, allow = 1)
	private static NativeImage wildercord$counter$scope(Minecraft mc, TestScreenshotCommonOptionsImpl<?> options, Operation<NativeImage> original) {
		var scope = CounterPeerRenderProbe.enter(options, mc);
		try { return original.call(mc, options); }
		finally { CounterPeerRenderProbe.leave(scope); }
	}

	@WrapOperation(method = "doTakeScreenshot(Lnet/minecraft/client/Minecraft;Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;)Lcom/mojang/blaze3d/platform/NativeImage;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$counter$extract(GameRenderer renderer, DeltaTracker delta, boolean renderLevel, Operation<Void> original) {
		CounterPeerRenderProbe.extractBegin(delta);
		boolean completed = false;
		try { original.call(renderer, delta, renderLevel); completed = true; }
		finally { CounterPeerRenderProbe.extractEnd(completed); }
	}

	@WrapOperation(method = "doTakeScreenshot(Lnet/minecraft/client/Minecraft;Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;)Lcom/mojang/blaze3d/platform/NativeImage;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$counter$render(GameRenderer renderer, DeltaTracker delta, boolean renderLevel, Operation<Void> original) {
		CounterPeerRenderProbe.renderBegin();
		boolean completed = false;
		try { original.call(renderer, delta, renderLevel); completed = true; }
		finally { CounterPeerRenderProbe.renderEnd(completed); }
	}

	@WrapOperation(method = "doTakeScreenshot(Lnet/minecraft/client/Minecraft;Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;)Lcom/mojang/blaze3d/platform/NativeImage;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lcom/mojang/blaze3d/platform/NativeImage;"), require = 1, expect = 1, allow = 1)
	private static NativeImage wildercord$counter$readback(RenderTarget target, Operation<NativeImage> original) {
		NativeImage[] captured = new NativeImage[1];
		CounterPeerRenderProbe.screenshot(target, image -> captured[0] = image,
			(unchangedTarget, wrappedConsumer) -> wrappedConsumer.accept(original.call(unchangedTarget)));
		return captured[0];
	}
}
