package dev.wildercord.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Pure helper contracts. No rendered phase, receipt or pixel acceptance is asserted here. */
public final class OpeningCaptureWaitChecks {
	private static final OpeningCaptureWait.Identity EXPECTED = new OpeningCaptureWait.Identity(new UUID(1, 2), 75, 75, 3, 380, 386);
	private static int checks;
	private OpeningCaptureWaitChecks() {}

	public static void main(String[] args) {
		AtomicLong clock = new AtomicLong(); AtomicInteger parks = new AtomicInteger();
		var immediate = OpeningCaptureWait.await(snapshot(false, 0), clock::get, nanos -> parks.incrementAndGet(), () -> false);
		check(!immediate.observedHold() && immediate.clockPolls() == 1 && immediate.elapsedNanos() == 0 && parks.get() == 0,
			"No hold returns without parking");
		var alreadyExpired = OpeningCaptureWait.await(snapshot(true, 0), clock::get, nanos -> parks.incrementAndGet(), () -> false);
		check(alreadyExpired.observedHold() && alreadyExpired.elapsedNanos() == 0 && parks.get() == 0, "Already elapsed hold bound returns immediately");
		var expired = OpeningCaptureWait.await(snapshot(true, 2_000_000L), clock::get,
			nanos -> { parks.incrementAndGet(); clock.addAndGet(nanos); }, () -> false);
		check(expired.observedHold() && expired.clockPolls() == 3 && expired.elapsedNanos() == 2_000_000L && parks.get() == 2,
			"Only the monotonic clock is polled until the expiry bound, without a Fabric observer");
		clock.set(0);
		fails("within one second", () -> OpeningCaptureWait.await(snapshot(true, 2_000_000_000L), clock::get, clock::addAndGet, () -> false));
		check(clock.get() == 1_000_000_000L, "A bad expiry bound still stops at the finite one-second deadline");
		clock.set(0);
		fails("within one second", () -> OpeningCaptureWait.await(snapshot(true, 2_000_000L), clock::get,
			nanos -> clock.set(1_000_000_000L), () -> false));

		for (var changed : new OpeningCaptureWait.Identity[] {
			new OpeningCaptureWait.Identity(new UUID(1, 3), 75, 75, 3, 380, 386),
			new OpeningCaptureWait.Identity(EXPECTED.ownerUuid(), 76, 75, 3, 380, 386),
			new OpeningCaptureWait.Identity(EXPECTED.ownerUuid(), 75, 76, 3, 380, 386),
			new OpeningCaptureWait.Identity(EXPECTED.ownerUuid(), 75, 75, 4, 380, 386),
			new OpeningCaptureWait.Identity(EXPECTED.ownerUuid(), 75, 75, 3, 381, 386),
			new OpeningCaptureWait.Identity(EXPECTED.ownerUuid(), 75, 75, 3, 380, 387), null
		}) fails("identity or tick changed", () -> OpeningCaptureWait.requireReady(snapshot(true, 0), changed, false));
		fails("still active or renewed", () -> OpeningCaptureWait.requireReady(snapshot(true, 0), EXPECTED, true));
		OpeningCaptureWait.requireReady(snapshot(true, 0), EXPECTED, false);
		check(true, "Final real preflight must establish unchanged identity and actual natural expiry");
		fails("no exact accepted owner", () -> OpeningCaptureWait.await(new OpeningCaptureWait.Snapshot(
			new OpeningCaptureWait.Identity(EXPECTED.ownerUuid(), 75, 76, 3, 380, 386), false, 0), () -> 0, nanos -> {}, () -> false));

		signalChecks();
		for (boolean signal : new boolean[] {false, true}) for (boolean before : new boolean[] {false, true}) for (boolean cleanupFails : new boolean[] {false, true})
			interruptedCleanup(signal, before, cleanupFails);
		System.out.println("Opening capture wait checks passed: " + checks);
	}

	private static void signalChecks() {
		AtomicLong clock = new AtomicLong(); AtomicInteger parks = new AtomicInteger();
		OpeningCaptureWait.withCleanup(() -> {
			OpeningCaptureWait.awaitSignal(() -> true, 3_000_000L, "ready receipt", clock::get, nanos -> parks.incrementAndGet());
			check(parks.get() == 0, "Existing genuine signal does not park");
			OpeningCaptureWait.awaitSignal(() -> parks.get() == 2, 3_000_000L, "other JVM publishes", clock::get,
				nanos -> { parks.incrementAndGet(); clock.addAndGet(nanos); });
			check(parks.get() == 2 && clock.get() == 2_000_000L, "Signal waits use only bounded wall-clock polling");
			fails("Finite native handshake expired", () -> OpeningCaptureWait.awaitSignal(() -> false, 3_000_000L,
				"absent receipt", clock::get, clock::addAndGet));
			check(clock.get() == 3_000_000L, "Missing receipt cannot extend the existing launch deadline");
			RuntimeException peerFailure = new RuntimeException("peer failed");
			try { OpeningCaptureWait.awaitSignal(() -> { throw peerFailure; }, 4_000_000L, "peer receipt", clock::get, clock::addAndGet); }
			catch (RuntimeException failure) { check(failure == peerFailure, "Peer failure is propagated unchanged"); return; }
			throw new AssertionError("Peer failure was swallowed");
		});
	}
	private static void interruptedCleanup(boolean signal, boolean before, boolean cleanupFails) {
		List<String> order = new ArrayList<>(); AtomicLong clock = new AtomicLong();
		AssertionError[] original = new AssertionError[1]; RuntimeException secondary = new RuntimeException("cleanup failure");
		try {
			OpeningCaptureWait.withCleanup(() -> {
				try {
					try {
						if (before) Thread.currentThread().interrupt();
						try {
							if (signal) OpeningCaptureWait.awaitSignal(() -> false, 10_000_000L, "peer receipt", clock::get, nanos -> Thread.currentThread().interrupt());
							else OpeningCaptureWait.await(snapshot(true, 10_000_000L), clock::get, nanos -> Thread.currentThread().interrupt());
						}
						catch (OpeningCaptureWait.InterruptedCapture failure) { original[0] = failure; throw failure; }
						throw new AssertionError("Interrupted wait returned normally");
					} finally {
						check(!Thread.currentThread().isInterrupted(), "Trial cleanup starts with interruption deferred");
						order.add("keys"); order.add("target/cast");
						if (cleanupFails) throw secondary;
					}
				} finally {
					check(!Thread.currentThread().isInterrupted(), "World close and option restoration keep the cancellation in the failure");
					order.add("world-close"); order.add("options");
				}
			});
			throw new AssertionError("Missing original interruption");
		} catch (OpeningCaptureWait.InterruptedCapture failure) {
			check(failure == original[0], "Original interruption stays primary");
			check(failure.getCause() instanceof InterruptedException, "The primary failure retains the cancellation cause");
			check(!cleanupFails || List.of(failure.getSuppressed()).contains(secondary), "Secondary cleanup failure remains attached");
			check(!Thread.currentThread().isInterrupted(), "Terminal Fabric shutdown can safely dispatch after the failure boundary");
			check(order.equals(List.of("keys", "target/cast", "world-close", "options")), "Every cleanup runs in order before the terminal failure");
		}
	}
	private static OpeningCaptureWait.Snapshot snapshot(boolean held, long expiresBy) { return new OpeningCaptureWait.Snapshot(EXPECTED, held, expiresBy); }
	private static void fails(String message, Runnable action) {
		try { action.run(); } catch (AssertionError failure) {
			check(failure.getMessage() != null && failure.getMessage().contains(message), "Expected refusal: " + message + "; got " + failure);
			return;
		}
		throw new AssertionError("Missing refusal: " + message);
	}
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
}
