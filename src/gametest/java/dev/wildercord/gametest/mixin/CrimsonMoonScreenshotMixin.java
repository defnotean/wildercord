package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
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

/** Exact audited Fabric 6.0.7 call sites. Missing hooks must fail application, never silently degrade. */
@Mixin(value = ClientGameTestContextImpl.class, remap = false)
public abstract class CrimsonMoonScreenshotMixin {
	@WrapMethod(method = "takeScreenshot(Lnet/fabricmc/fabric/api/client/gametest/v1/screenshot/TestScreenshotOptions;)Ljava/nio/file/Path;", require = 1, expect = 1, allow = 1)
	private Path wildercord$moon$receipt(TestScreenshotOptions options, Operation<Path> original) {
		var token = CrimsonMoonRenderProbe.begin(options);
		if (token == null) return original.call(options);
		try {
			Path path;
			try { path = original.call(options); }
			catch (Throwable failure) { CrimsonMoonRenderProbe.failed(token, failure); throw failure; }
			CrimsonMoonRenderProbe.success(token, path);
			return path;
		} finally { CrimsonMoonRenderProbe.unregister(options, token); }
	}

	@WrapMethod(method = "lambda$doTakeScreenshot$2(Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;Ljava/util/function/Function;Lnet/minecraft/client/Minecraft;)Ljava/util/concurrent/CompletableFuture;", require = 1, expect = 1, allow = 1)
	private static CompletableFuture<?> wildercord$moon$scope(TestScreenshotCommonOptionsImpl<?> options,
			Function<NativeImage, ?> save, Minecraft mc, Operation<CompletableFuture<?>> original) {
		var scope = CrimsonMoonRenderProbe.enter(options, mc);
		try { return original.call(options, save, mc); }
		finally { CrimsonMoonRenderProbe.leave(scope); }
	}

	@WrapOperation(method = "lambda$doTakeScreenshot$2(Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;Ljava/util/function/Function;Lnet/minecraft/client/Minecraft;)Ljava/util/concurrent/CompletableFuture;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;extract(Lnet/minecraft/client/DeltaTracker;Z)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$moon$extract(GameRenderer renderer, DeltaTracker delta, boolean renderLevel, Operation<Void> original) {
		CrimsonMoonRenderProbe.extractBegin(delta);
		boolean completed = false;
		try { original.call(renderer, delta, renderLevel); completed = true; }
		finally { CrimsonMoonRenderProbe.extractEnd(completed); }
	}

	@WrapOperation(method = "lambda$doTakeScreenshot$2(Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;Ljava/util/function/Function;Lnet/minecraft/client/Minecraft;)Ljava/util/concurrent/CompletableFuture;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render()V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$moon$render(GameRenderer renderer, Operation<Void> original) {
		CrimsonMoonRenderProbe.renderBegin();
		boolean completed = false;
		try { original.call(renderer); completed = true; }
		finally { CrimsonMoonRenderProbe.renderEnd(completed); }
	}

	@WrapOperation(method = "lambda$doTakeScreenshot$2(Lnet/fabricmc/fabric/impl/client/gametest/screenshot/TestScreenshotCommonOptionsImpl;Ljava/util/function/Function;Lnet/minecraft/client/Minecraft;)Ljava/util/concurrent/CompletableFuture;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$moon$readback(RenderTarget target, Consumer<NativeImage> consumer, Operation<Void> original) {
		CrimsonMoonRenderProbe.screenshot(target, consumer, (unchangedTarget, wrappedConsumer) -> original.call(unchangedTarget, wrappedConsumer));
	}
}
