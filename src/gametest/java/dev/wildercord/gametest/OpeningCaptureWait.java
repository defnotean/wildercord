package dev.wildercord.gametest;

import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/** GameTest-only wall-clock synchronization. Waiting never dispatches a Fabric task or advances a game tick. */
public final class OpeningCaptureWait {
	private static final long TIMEOUT_NANOS = 1_000_000_000L;
	private static final long POLL_NANOS = 1_000_000L;
	private static final ThreadLocal<CleanupScope> CLEANUP = new ThreadLocal<>();
	private OpeningCaptureWait() {}

	public record Identity(UUID ownerUuid, int owner, int acceptedOwner, int move, long activation, long gameTick) {}
	public record Snapshot(Identity identity, boolean observedHold, long expiresByNanos) {}
	public record Result(boolean observedHold, int clockPolls, long elapsedNanos) {}
	public static final class InterruptedCapture extends AssertionError {
		private InterruptedCapture() { super("Opening capture wait interrupted", new InterruptedException("Opening capture wait interrupted")); }
	}
	private static final class CleanupScope {
		InterruptedCapture interruption;
		boolean interrupted() {
			if (Thread.interrupted() && interruption == null) interruption = new InterruptedCapture();
			return interruption != null;
		}
	}

	/**
	 * Cancellation remains a terminal failure, with the interrupt consumed through fixture cleanup
	 * and Fabric's subsequent shutdown handoff. Restoring the bit here would abandon that handoff's ack.
	 */
	public static void withCleanup(Runnable capture) {
		if (CLEANUP.get() != null) throw new IllegalStateException("Opening capture cleanup scopes cannot overlap");
		CleanupScope scope = new CleanupScope(); CLEANUP.set(scope);
		try {
			capture.run();
			if (scope.interruption != null) throw scope.interruption;
		} catch (RuntimeException | Error failure) {
			if (scope.interruption != null && failure != scope.interruption) {
				scope.interruption.addSuppressed(failure);
				throw scope.interruption;
			}
			throw failure;
		} finally {
			CLEANUP.remove();
			if (scope.interruption != null && Thread.interrupted())
				scope.interruption.addSuppressed(new InterruptedException("Additional interruption during opening capture cleanup"));
		}
	}

	public static Result await(Snapshot snapshot) {
		return await(snapshot, System::nanoTime, LockSupport::parkNanos);
	}
	static Result await(Snapshot snapshot, LongSupplier clock, LongConsumer park) {
		CleanupScope scope = CLEANUP.get();
		if (scope == null) throw new IllegalStateException("Opening capture wait requires its outer cleanup scope");
		if (scope.interrupted()) throw scope.interruption;
		try { return await(snapshot, clock, park, scope::interrupted); }
		catch (InterruptedCapture failure) { throw scope.interruption == null ? failure : scope.interruption; }
	}

	/** This must run in the existing final client preflight, before arming the single real screenshot. */
	public static void requireReady(Snapshot expected, Identity actual, boolean holding) {
		if (!expected.identity().equals(actual))
			throw new AssertionError("Opening capture identity or tick changed while waiting: expected=" + expected.identity() + " actual=" + actual);
		if (holding) throw new AssertionError("Opening hit-stop is still active or renewed before capture: " + actual);
	}

	/** Injectable clocks exercise deadline and interruption behavior without sleeping or running Minecraft. */
	static Result await(Snapshot snapshot, LongSupplier clock, LongConsumer park, BooleanSupplier interrupted) {
		Identity expected = snapshot.identity();
		if (expected == null || expected.ownerUuid() == null || expected.owner() != expected.acceptedOwner())
			throw new AssertionError("Opening capture has no exact accepted owner: " + expected);
		long start = clock.getAsLong();
		int polls = 0;
		while (true) {
			if (interrupted.getAsBoolean()) throw new InterruptedCapture();
			long now = clock.getAsLong(), elapsed = now - start; polls++;
			if (elapsed >= TIMEOUT_NANOS) throw new AssertionError("Opening hit-stop did not expire within one second: " + expected);
			long remaining = snapshot.expiresByNanos() - now;
			if (!snapshot.observedHold() || remaining <= 0) return new Result(snapshot.observedHold(), polls, elapsed);
			park.accept(Math.min(Math.min(POLL_NANOS, remaining), TIMEOUT_NANOS - elapsed));
		}
	}
}
