package dev.wildercord.gametest;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/** One real close owns one native shutdown task; waiting participates in the existing client phases. */
public final class NativeHaltHandshake {
	private NativeHaltHandshake() { }

	public static final class Gate {
		private final AtomicReference<Scope> active = new AtomicReference<>();

		public Scope open(Object server, Thread renderThread) {
			Scope scope = new Scope(this, Objects.requireNonNull(server), Thread.currentThread(),
				Objects.requireNonNull(renderThread));
			if (!active.compareAndSet(null, scope)) throw new IllegalStateException("A native singleplayer close is already active");
			return scope;
		}

		/** Mismatched calls retain vanilla behavior; an already claimed matching call must never submit twice. */
		public Scope claim(Object server, boolean tickPhase) {
			Scope scope = active.get();
			if (scope == null || scope.server != server || scope.renderThread != Thread.currentThread()) return null;
			synchronized (scope) {
				if (active.get() != scope || scope.closed) return null;
				if (!tickPhase) throw new IllegalStateException("Native singleplayer halt was not deferred to the tick phase");
				if (scope.claimed) throw new IllegalStateException("Reentrant or repeated native singleplayer halt");
				scope.claimed = true;
				return scope;
			}
		}

		public boolean isActive() { return active.get() != null; }
	}

	public static final class Scope implements AutoCloseable {
		private final Gate gate;
		private final Object server;
		private final Thread ownerThread, renderThread;
		private boolean claimed, started;
		private volatile boolean closed;
		private volatile int submissions, pumps;
		private volatile boolean completed;

		private Scope(Gate gate, Object server, Thread ownerThread, Thread renderThread) {
			this.gate = gate; this.server = server; this.ownerThread = ownerThread; this.renderThread = renderThread;
		}

		public void execute(Runnable original, Function<Runnable, CompletableFuture<Void>> submit,
				Runnable phasePump, Supplier<Throwable> unavailable) {
			synchronized (this) {
				checkLive();
				if (!claimed || started || Thread.currentThread() != renderThread)
					throw new IllegalStateException("Native shutdown task is outside its claimed render-thread close");
				started = true;
			}
			// The submitted object is the original vanilla Runnable. Neither retry nor replacement is permitted.
			submissions++;
			CompletableFuture<Void> future = Objects.requireNonNull(submit.apply(Objects.requireNonNull(original)));
			while (!future.isDone()) {
				checkLive();
				Throwable failure = unavailable.get();
				if (failure != null) NativeHaltHandshake.<RuntimeException>raise(failure);
				pumps++;
				phasePump.run();
			}
			// Retain precisely the future and join semantics used by vanilla executeBlocking, including its exception.
			future.join();
			checkLive();
			completed = true;
		}

		private void checkLive() {
			if (closed || gate.active.get() != this) throw new IllegalStateException("Native singleplayer close scope has ended");
		}

		public int submissions() { return submissions; }
		public int pumps() { return pumps; }
		public boolean completed() { return completed; }

		@Override public void close() {
			if (Thread.currentThread() != ownerThread) throw new IllegalStateException("Only the test thread can end its native close scope");
			synchronized (this) {
				closed = true;
				gate.active.compareAndSet(this, null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void raise(Throwable failure) throws E { throw (E) failure; }
}
