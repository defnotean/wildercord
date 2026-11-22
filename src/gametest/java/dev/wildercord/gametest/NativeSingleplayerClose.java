package dev.wildercord.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;

/** GameTest-only adaptation of the Fabric close handshake. 1.21.3 has no `postRunTasks()`; the pump is the event loop `pollTask` loop, which is what 1.21.4 runs there. */
public final class NativeSingleplayerClose {
	private static final NativeHaltHandshake.Gate GATE = new NativeHaltHandshake.Gate();
	private static volatile Receipt last;
	private NativeSingleplayerClose() { }
	private record Target(IntegratedServer server, Thread renderThread) { }
	public record Receipt(int submissions, int pumps, boolean completed) { }

	public static void close(ClientGameTestContext context, MinecraftServer expectedServer, Runnable original) {
		ThreadingImpl.checkOnGametestThread("nativeSingleplayerClose");
		Target target = context.computeOnClient(mc -> new Target(mc.getSingleplayerServer(), Thread.currentThread()));
		if (target.server == null) { original.run(); return; }
		if (target.server != expectedServer) throw new IllegalStateException("Closing a different native singleplayer world");
		last = null;
		try (var scope = GATE.open(target.server, target.renderThread)) {
			original.run();
			last = new Receipt(scope.submissions(), scope.pumps(), scope.completed());
		}
	}

	/** Return false only for an unrelated native call, which the exact-call mixin must forward unchanged. */
	public static boolean execute(IntegratedServer server, Runnable task) {
		if (server.isSameThread()) return false;
		var scope = GATE.claim(server, ThreadingImpl.getCurrentPhase() == ThreadingImpl.PHASE_TICK
			&& ThreadingImpl.taskToRun == null);
		if (scope == null) return false;
		Minecraft client = Minecraft.getInstance();
		scope.execute(task, server::submit, () -> { while (client.pollTask()) { } }, () -> {
			if (!ThreadingImpl.isGameCrashed() && ThreadingImpl.isServerRunning) return null;
			Throwable recorded = ThreadingImpl.testFailureException;
			return recorded != null ? recorded : new IllegalStateException("Native server stopped before its shutdown task completed");
		});
		return true;
	}

	public static Receipt lastReceipt() { return last; }
	public static boolean isActive() { return GATE.isActive(); }
}
