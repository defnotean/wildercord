package dev.wildercord.aura.world;

import net.minecraft.server.level.ServerLevel;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** A test-only allowance for the ordinary native maintenance call, never a second tick or a synthetic unload. */
public final class MasterChunkMaintenanceBudget {
	static final long SLICE_NANOS = 5_000_000;
	private static final Gate ACTIVE = new Gate(System::nanoTime);
	private MasterChunkMaintenanceBudget() {}

	static Scope open(ServerLevel level, BooleanSupplier pending) { return ACTIVE.open(level, pending); }
	static void close(Scope scope) { ACTIVE.close(scope); }
	public static BooleanSupplier supplement(ServerLevel level, BooleanSupplier original) { return ACTIVE.supplement(level, original); }

	static final class Scope {
		final Object level;
		final BooleanSupplier pending;
		long maintenanceCalls, nativeDenials, supplementalGrants;
		Scope(Object level, BooleanSupplier pending) { this.level = level; this.pending = pending; }
	}

	/** Identity and an injected monotonic clock let the native fixture check stale scopes and exact expiry deterministically. */
	static final class Gate {
		private final LongSupplier clock;
		private volatile Scope active; // Failure cleanup may run on the test thread before the server's next tick.
		Gate(LongSupplier clock) { this.clock = clock; }
		Scope open(Object level, BooleanSupplier pending) { return active = new Scope(level, pending); }
		void close(Scope scope) { if (active == scope) active = null; }

		BooleanSupplier supplement(Object level, BooleanSupplier original) {
			Scope scope = active;
			if (scope == null || scope.level != level || !scope.pending.getAsBoolean()) return original;
			long began = clock.getAsLong();
			scope.maintenanceCalls++;
			return () -> {
				// Always preserve native permission, including after this supplement expires or its scope closes.
				if (original.getAsBoolean()) return true;
				scope.nativeDenials++;
				if (active != scope || !scope.pending.getAsBoolean()) return false;
				long elapsed = clock.getAsLong() - began;
				boolean allowed = elapsed >= 0 && elapsed < SLICE_NANOS;
				if (allowed) scope.supplementalGrants++;
				return allowed;
			};
		}
	}
}
