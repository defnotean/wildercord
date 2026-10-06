package dev.wildercord.gametest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded stand-alone scheduler controls exercise the actual gate, never launch Minecraft. */
public final class NativeHaltHandshakeTest {
	private static int checks;
	private static void check(boolean value, String message) {
		checks++;
		if (!value) throw new AssertionError(message);
	}
	private static Throwable caught(Runnable action) {
		try { action.run(); } catch (Throwable failure) { return failure; }
		throw new AssertionError("Expected failure");
	}
	private static void await(CountDownLatch latch) {
		try { if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Control thread did not arrive"); }
		catch (InterruptedException failure) { throw new AssertionError(failure); }
	}
	private static void join(Thread thread) throws Exception {
		thread.join(5000);
		check(!thread.isAlive(), "Control thread terminated");
	}

	public static void main(String[] args) throws Exception {
		completion(); failures(); isolation(); reentrancyAndLateCallbacks(); primaryAndSuppressed(); threePartyCycle();
		System.out.println("Native halt handshake: " + checks + " checks passed");
	}

	private static void completion() {
		for (int delay : new int[] {0, 1, 3}) {
			var gate = new NativeHaltHandshake.Gate(); Object server = new Object();
			AtomicInteger calls = new AtomicInteger(), pumps = new AtomicInteger(), submissions = new AtomicInteger();
			Runnable nativeTask = calls::incrementAndGet;
			CompletableFuture<Void> future = new CompletableFuture<>();
			try (var scope = gate.open(server, Thread.currentThread())) {
				check(gate.claim(server, true) == scope, "Exact scope claimed");
				scope.execute(nativeTask, supplied -> {
					check(supplied == nativeTask, "Submit receives original native Runnable identity"); submissions.incrementAndGet();
					if (delay == 0) { supplied.run(); future.complete(null); }
					return future;
				}, () -> { if (pumps.incrementAndGet() == delay) { nativeTask.run(); future.complete(null); } }, () -> null);
				check(scope.submissions() == 1 && submissions.get() == 1 && calls.get() == 1, "One submission and one actual task execution");
				check(scope.pumps() == delay && pumps.get() == delay && scope.completed(), "Immediate/delayed exact pump count");
			}
			check(!gate.isActive(), "Completion disarms close");
		}
	}

	private static void failures() {
		for (boolean delayed : new boolean[] {false, true}) {
			var gate = new NativeHaltHandshake.Gate(); Object server = new Object();
			var cause = new AssertionError("native removal failed");
			var original = new CompletionException("native completion", cause);
			var future = new CompletableFuture<Void>();
			try (var scope = gate.open(server, Thread.currentThread())) {
				gate.claim(server, true);
				Throwable failure = caught(() -> scope.execute(() -> { }, task -> {
					if (!delayed) future.completeExceptionally(original);
					return future;
				}, () -> future.completeExceptionally(original), () -> null));
				check(failure == original && failure.getCause() == cause, "Join preserves original exceptional completion and cause");
				check(!scope.completed() && scope.submissions() == 1, "Failed native completion is never marked successful");
			}
			check(!gate.isActive(), "Exceptional completion disarms close");
		}
		for (String stage : List.of("submit", "pump", "crash")) {
			var gate = new NativeHaltHandshake.Gate(); Object server = new Object();
			var original = new AssertionError(stage); var future = new CompletableFuture<Void>();
			try (var scope = gate.open(server, Thread.currentThread())) {
				gate.claim(server, true);
				Throwable failure = caught(() -> scope.execute(() -> { }, task -> {
					if (stage.equals("submit")) throw original; return future;
				}, () -> { throw original; }, () -> stage.equals("crash") ? original : null));
				check(failure == original, "Exact " + stage + " failure retained");
				check(!future.isDone() && !scope.completed(), "No cancelled or fabricated completion for " + stage);
			}
			check(!gate.isActive(), "Failed " + stage + " disarms close");
		}
	}

	private static void isolation() throws Exception {
		var gate = new NativeHaltHandshake.Gate(); Object server = new Object();
		check(gate.claim(server, true) == null, "Inactive call remains vanilla");
		try (var scope = gate.open(server, Thread.currentThread())) {
			check(gate.claim(new Object(), true) == null, "Wrong server remains vanilla");
			AtomicReference<Object> otherClaim = new AtomicReference<>();
			Thread other = Thread.ofPlatform().start(() -> otherClaim.set(gate.claim(server, true)));
			join(other); check(otherClaim.get() == null, "Wrong thread remains vanilla without consuming scope");
			AtomicReference<Throwable> badClose = new AtomicReference<>();
			Thread wrongOwner = Thread.ofPlatform().start(() -> badClose.set(caught(scope::close)));
			join(wrongOwner); check(badClose.get() instanceof IllegalStateException && gate.isActive(), "Wrong thread cannot disarm close");
			check(caught(() -> gate.open(server, Thread.currentThread())) instanceof IllegalStateException, "Nested close rejected");
			check(caught(() -> gate.claim(server, false)) instanceof IllegalStateException, "Wrong phase refused before submission");
			check(gate.claim(server, true) == scope, "Failed mismatches do not consume correct claim");
		}
		check(!gate.isActive(), "Isolation controls leave no scope");
	}

	private static void reentrancyAndLateCallbacks() {
		var gate = new NativeHaltHandshake.Gate(); Object server = new Object();
		var old = gate.open(server, Thread.currentThread());
		var future = new CompletableFuture<Void>(); AtomicInteger calls = new AtomicInteger();
		try (old) {
			gate.claim(server, true);
			old.execute(calls::incrementAndGet, task -> future, () -> {
				check(caught(() -> gate.claim(server, true)) instanceof IllegalStateException, "Reentrant halt cannot resubmit");
				check(caught(() -> old.execute(() -> { }, task -> future, () -> { }, () -> null)) instanceof IllegalStateException,
					"Reentrant callback cannot execute twice");
				calls.incrementAndGet(); future.complete(null);
			}, () -> null);
			check(calls.get() == 1 && old.submissions() == 1, "Reentrant refusal retains one native execution");
		}
		try (var next = gate.open(server, Thread.currentThread())) {
			old.close();
			check(gate.isActive(), "Late old close cannot clear newer scope");
			check(caught(() -> old.execute(() -> { }, task -> future, () -> { }, () -> null)) instanceof IllegalStateException,
				"Late callback cannot submit into newer scope");
			check(gate.claim(server, true) == next, "New scope can still claim its own halt");
		}
		var unused = gate.open(server, Thread.currentThread()); gate.claim(server, true); unused.close();
		check(caught(() -> unused.execute(() -> { }, task -> future, () -> { }, () -> null)) instanceof IllegalStateException,
			"Claimed callback arriving after close is refused before submission");
		check(unused.submissions() == 0 && gate.claim(server, true) == null, "Late refusal leaves native inactive path available");
		var endsDuringPump = gate.open(server, Thread.currentThread()); gate.claim(server, true);
		var late = new CompletableFuture<Void>();
		check(caught(() -> endsDuringPump.execute(() -> { }, task -> late, () -> {
			endsDuringPump.close(); late.complete(null);
		}, () -> null)) instanceof IllegalStateException, "Late successful future cannot make an ended close successful");
		check(!endsDuringPump.completed() && !gate.isActive(), "Ended scope remains disarmed after late completion");
	}

	private static void primaryAndSuppressed() throws Exception {
		for (boolean cleanupFails : new boolean[] {false, true}) {
			var gate = new NativeHaltHandshake.Gate(); Object server = new Object();
			var primary = new AssertionError("body"); var secondary = new CompletionException(new AssertionError("cleanup"));
			AutoCloseable world = () -> {
				try (var scope = gate.open(server, Thread.currentThread())) {
					gate.claim(server, true);
					scope.execute(() -> { }, task -> cleanupFails ? CompletableFuture.failedFuture(secondary) : CompletableFuture.completedFuture(null),
						() -> { throw new AssertionError("Unexpected pump"); }, () -> null);
				}
			};
			try (world) { throw primary; }
			catch (AssertionError failure) {
				check(failure == primary, "Try-with-resources retains exact primary failure");
				check(failure.getSuppressed().length == (cleanupFails ? 1 : 0), "Cleanup suppression count retained");
				if (cleanupFails) check(failure.getSuppressed()[0] == secondary, "Exact native close exception remains suppressed");
			}
			check(!gate.isActive(), "Primary/suppressed failure leaves no active scope");
		}
	}

	private static void threePartyCycle() throws Exception {
		Phaser phases = new Phaser(3); CountDownLatch waiting = new CountDownLatch(2), executed = new CountDownLatch(1);
		var tasks = new ConcurrentLinkedQueue<Runnable>(); var workerFailure = new AtomicReference<Throwable>();
		var ranOn = new AtomicReference<Thread>(); AtomicInteger executions = new AtomicInteger();
		var future = new CompletableFuture<Void>();
		Runnable arrive = () -> {
			int phase = phases.arrive(); waiting.countDown();
			try { phases.awaitAdvanceInterruptibly(phase, 5, TimeUnit.SECONDS); }
			catch (Exception failure) { throw new AssertionError(failure); }
			phases.arriveAndAwaitAdvance();
		};
		Thread server = Thread.ofPlatform().daemon().name("contract server").start(() -> {
			try { arrive.run(); tasks.remove().run(); }
			catch (Throwable failure) { workerFailure.set(failure); }
			finally { executed.countDown(); }
		});
		Thread test = Thread.ofPlatform().daemon().name("contract test").start(() -> {
			try { arrive.run(); } catch (Throwable failure) { workerFailure.set(failure); }
		});
		try {
			await(waiting);
			check(phases.getRegisteredParties() == 3 && phases.getUnarrivedParties() == 1, "Server and test are already waiting for render at TEST");
			var gate = new NativeHaltHandshake.Gate(); Object identity = new Object();
			Runnable original = () -> { ranOn.set(Thread.currentThread()); executions.incrementAndGet(); };
			try (var scope = gate.open(identity, Thread.currentThread())) {
				gate.claim(identity, true);
				scope.execute(original, supplied -> {
					check(supplied == original && !future.isDone(), "Queue the exact native task after the other parties are parked");
					tasks.add(() -> { supplied.run(); future.complete(null); });
					check(!future.isDone() && phases.getUnarrivedParties() == 1, "Legacy join here would form the confirmed cycle");
					return future;
				}, () -> {
					phases.arriveAndAwaitAdvance(); // Existing pump enters TEST.
					phases.arriveAndAwaitAdvance(); // Existing pump returns to TICK.
					await(executed); // Test-only scheduling control; no wait/timeout is added to the actual adapter.
				}, () -> workerFailure.get());
				check(scope.pumps() == 1 && scope.completed(), "One full phase cycle releases actual queued work");
			}
			check(executions.get() == 1 && ranOn.get() == server && tasks.isEmpty(), "Native task executes once on the original server thread");
			check(workerFailure.get() == null, "Neither scheduler participant failed");
		} finally {
			// Only failure cleanup of this isolated test scheduler, never a game or production Phaser.
			phases.forceTermination(); join(server); join(test);
		}
	}
}
