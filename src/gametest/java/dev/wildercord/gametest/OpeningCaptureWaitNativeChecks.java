package dev.wildercord.gametest;

import dev.wildercord.aura.MastersArts;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual Fabric handoffs and real world close; synthetic interruption, never screenshot evidence. */
public final class OpeningCaptureWaitNativeChecks {
	private OpeningCaptureWaitNativeChecks() {}
	private record Keys(boolean attack, boolean shift) {}

	public static void verify(ClientGameTestContext context) {
		for (boolean cleanupFails : new boolean[] {false, true}) {
			Keys saved = context.computeOnClient(mc -> new Keys(mc.options.keyAttack.isDown(), mc.options.keyShift.isDown()));
			List<String> order = new ArrayList<>(); AtomicInteger completed = new AtomicInteger();
			OpeningCaptureWait.InterruptedCapture[] original = new OpeningCaptureWait.InterruptedCapture[1];
			RuntimeException secondary = new RuntimeException("deliberate native cleanup contract failure");
			try {
				OpeningCaptureWait.withCleanup(() -> {
					try {
						try (var world = context.worldBuilder().create()) {
							var identity = context.computeOnClient(mc -> new OpeningCaptureWait.Identity(
								mc.player.getUUID(), mc.player.getId(), mc.player.getId(), 3, 0, mc.level.getGameTime()));
							try {
								Thread.currentThread().interrupt();
								try { OpeningCaptureWait.await(new OpeningCaptureWait.Snapshot(identity, false, System.nanoTime())); }
								catch (OpeningCaptureWait.InterruptedCapture failure) { original[0] = failure; throw failure; }
								throw new AssertionError("Interrupted native contract returned normally");
							} finally {
								check(!Thread.currentThread().isInterrupted(), "Native trial cleanup sees the deferred interrupt");
								context.getInput().releaseKey(o -> o.keyAttack); context.getInput().releaseKey(o -> o.keyShift);
								context.runOnClient(mc -> { order.add("keys"); completed.incrementAndGet(); });
								world.getServer().runOnServer(server -> {
									MastersArts.cancel(server.getPlayerList().getPlayers().getFirst());
									order.add("cast-cleanup"); completed.incrementAndGet();
									if (cleanupFails) throw secondary;
								});
							}
						}
					} finally {
						check(!Thread.currentThread().isInterrupted(), "Actual world.close finishes with cancellation retained as failure");
						context.runOnClient(mc -> {
							check(mc.level == null && !ThreadingImpl.isServerRunning, "Real singleplayer world closed during interrupted cleanup");
							order.add("world-closed"); completed.incrementAndGet();
							mc.options.keyAttack.setDown(saved.attack()); mc.options.keyShift.setDown(saved.shift());
							order.add("options-restored"); completed.incrementAndGet();
						});
					}
				});
				throw new AssertionError("Missing native interruption refusal");
			} catch (OpeningCaptureWait.InterruptedCapture failure) {
				check(failure == original[0], "Native cleanup preserves the exact original interruption");
				check(failure.getCause() instanceof InterruptedException, "Native cleanup preserves the original cancellation cause");
				check(!cleanupFails || List.of(failure.getSuppressed()).contains(secondary), "Native cleanup failure is retained as suppressed evidence");
				check(!Thread.currentThread().isInterrupted(), "Cancellation failure leaves terminal Fabric shutdown safe to dispatch");
				check(completed.get() == 4 && order.equals(List.of("keys", "cast-cleanup", "world-closed", "options-restored")),
					"All native cleanup callbacks complete in order, exactly once");
				check(ThreadingImpl.taskToRun == null, "Interrupted wait leaves no published Fabric task behind");
			}
			// Same real handoff used by ThreadingImpl's terminal Minecraft.stop path; this
			// contract's callback is a sentinel so the remaining native tests can continue.
			ThreadingImpl.runOnClient(() -> completed.incrementAndGet());
			check(completed.get() == 5 && ThreadingImpl.taskToRun == null && ThreadingImpl.TEST_SEMAPHORE.availablePermits() == 0,
				"Terminal-shaped native handoff consumes its own acknowledgement without manually clearing the interrupt");
		}
		System.out.println("ARTICULATED_SYNTHETIC_INTERRUPT_CLEANUP passed: native dispatch, world close, primary failure; screenshotEvidence=false");
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
