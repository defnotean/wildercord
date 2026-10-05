package dev.wildercord.aura.world;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/** Negative controls for the test harness allowance; no Minecraft entity, clock or ticket is modified. */
final class MasterChunkMaintenanceBudgetChecks {
	static void run() {
		long[] now = {100};
		var gate = new MasterChunkMaintenanceBudget.Gate(() -> now[0]);
		Object level = new Object(), anotherLevel = new Object();
		var pending = new AtomicBoolean(true);
		var nativeAllowed = new AtomicBoolean(false);
		var nativeCalls = new AtomicInteger();
		BooleanSupplier original = () -> { nativeCalls.incrementAndGet(); return nativeAllowed.get(); };
		check(gate.supplement(level, original) == original, "Inactive scope preserves the original supplier itself");
		var first = gate.open(level, pending::get);
		check(gate.supplement(anotherLevel, original) == original, "A different level receives no supplemental budget");
		BooleanSupplier admitted = gate.supplement(level, original);
		check(admitted.getAsBoolean() && nativeCalls.get() == 1, "An active scope supplements one denied native query");
		now[0] += MasterChunkMaintenanceBudget.SLICE_NANOS - 1;
		check(admitted.getAsBoolean(), "The fixed invocation budget remains open just before five milliseconds");
		now[0]++;
		check(!admitted.getAsBoolean(), "Exactly five milliseconds expires supplemental admission");
		nativeAllowed.set(true);
		check(admitted.getAsBoolean(), "An expired supplement never denies the original native supplier");
		nativeAllowed.set(false);
		BooleanSupplier freshInvocation = gate.supplement(level, original);
		check(freshInvocation.getAsBoolean(), "A later ordinary maintenance invocation gets its own fixed slice");
		pending.set(false);
		check(!freshInvocation.getAsBoolean() && gate.supplement(level, original) == original,
			"Observing the real unload-completion condition revokes both retained and future supplements");
		pending.set(true);
		BooleanSupplier stale = gate.supplement(level, original);
		var replacement = gate.open(level, () -> true);
		check(!stale.getAsBoolean(), "A replaced scope cannot use a previously issued allowance");
		gate.close(first);
		BooleanSupplier current = gate.supplement(level, original);
		check(current.getAsBoolean(), "Closing an old scope cannot close its replacement");
		gate.close(replacement);
		check(!current.getAsBoolean() && gate.supplement(level, original) == original,
			"Clearing the active scope revokes retained and future supplements");
		nativeAllowed.set(true);
		int before = nativeCalls.get();
		check(current.getAsBoolean() && nativeCalls.get() == before + 1,
			"Cleared scope still queries and preserves the original supplier exactly once");
		check(first.nativeDenials > 0 && first.supplementalGrants > 0 && first.maintenanceCalls > 0,
			"Budget receipts distinguish native denials from supplemental admissions");
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	public static void main(String[] args) { run(); }
}
