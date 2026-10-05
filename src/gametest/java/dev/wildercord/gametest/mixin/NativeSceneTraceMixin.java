package dev.wildercord.gametest.mixin;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-mod-only tracing of Fabric's pinned runner; never wraps or replaces a test. */
@Mixin(value = FabricClientGameTestRunner.class, remap = false)
public abstract class NativeSceneTraceMixin {
	@Shadow public static EntrypointContainer<FabricClientGameTest> currentlyRunningGameTest;
	@Unique private static long wildercord$traceStarted;
	@Unique private static long wildercord$sceneStarted;

	// Fabric client gametest 6.0.7: assign entry, setup, runTest, cleanup, clear;
	// the catch-all also clears the entry before rethrowing the original throwable.
	@Inject(method = "lambda$start$0(Ljava/util/List;)V", at = @At(value = "FIELD",
			target = "Lnet/fabricmc/fabric/impl/client/gametest/FabricClientGameTestRunner;currentlyRunningGameTest:Lnet/fabricmc/loader/api/entrypoint/EntrypointContainer;",
			opcode = Opcodes.PUTSTATIC, ordinal = 0, shift = At.Shift.AFTER), require = 1, allow = 1)
	private static void wildercord$sceneStart(CallbackInfo ci) {
		wildercord$sceneStarted = System.nanoTime();
		if (wildercord$traceStarted == 0) wildercord$traceStarted = wildercord$sceneStarted;
		wildercord$trace("start", "setup");
	}

	@Inject(method = "lambda$start$0(Ljava/util/List;)V", at = @At(value = "INVOKE",
			target = "Lnet/fabricmc/fabric/api/client/gametest/v1/FabricClientGameTest;runTest(Lnet/fabricmc/fabric/api/client/gametest/v1/context/ClientGameTestContext;)V"), require = 1, allow = 1)
	private static void wildercord$sceneRun(CallbackInfo ci) {
		wildercord$trace("phase", "run");
	}

	@Inject(method = "lambda$start$0(Ljava/util/List;)V", at = @At(value = "INVOKE",
			target = "Lnet/fabricmc/fabric/impl/client/gametest/FabricClientGameTestRunner;setupAndCheckFinalGameTestState(Lnet/fabricmc/fabric/impl/client/gametest/context/ClientGameTestContextImpl;)V"), require = 1, allow = 1)
	private static void wildercord$sceneCleanup(CallbackInfo ci) {
		wildercord$trace("phase", "cleanup");
	}

	@Inject(method = "lambda$start$0(Ljava/util/List;)V", at = @At(value = "FIELD",
			target = "Lnet/fabricmc/fabric/impl/client/gametest/FabricClientGameTestRunner;currentlyRunningGameTest:Lnet/fabricmc/loader/api/entrypoint/EntrypointContainer;",
			opcode = Opcodes.PUTSTATIC, ordinal = 1), require = 1, allow = 1)
	private static void wildercord$sceneReturned(CallbackInfo ci) {
		wildercord$trace("end", "returned");
	}

	@Inject(method = "lambda$start$0(Ljava/util/List;)V", at = @At(value = "FIELD",
			target = "Lnet/fabricmc/fabric/impl/client/gametest/FabricClientGameTestRunner;currentlyRunningGameTest:Lnet/fabricmc/loader/api/entrypoint/EntrypointContainer;",
			opcode = Opcodes.PUTSTATIC, ordinal = 2), require = 1, allow = 1)
	private static void wildercord$sceneThrew(CallbackInfo ci) {
		wildercord$trace("end", "threw");
	}

	@Unique
	private static void wildercord$trace(String event, String phase) {
		// No exception text, context objects, world data, environment or VM settings.
		// A returned entry can have been an intentional optional skip; it is not a pass.
		try {
			long now = System.nanoTime();
			JsonObject marker = new JsonObject();
			marker.addProperty("event", event);
			marker.addProperty("phase", phase);
			marker.addProperty("suite", currentlyRunningGameTest.getDefinition());
			marker.addProperty("elapsedSeconds", (now - wildercord$traceStarted) / 1_000_000_000.0);
			marker.addProperty("sceneElapsedSeconds", (now - wildercord$sceneStarted) / 1_000_000_000.0);
			System.out.println("WILDERCORD_NATIVE_SCENE " + marker);
		} catch (RuntimeException ignored) {
			// Logging cannot replace the original test exception or result.
		}
	}
}
