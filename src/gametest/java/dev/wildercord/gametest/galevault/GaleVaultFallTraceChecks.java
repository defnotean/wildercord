package dev.wildercord.gametest.galevault;

import java.util.ArrayList;
import java.util.List;

/** Runs against the exact observation helper, without Minecraft, synthetic fall calls or damage. */
public final class GaleVaultFallTraceChecks {
    private GaleVaultFallTraceChecks() {}
    public static void main(String[] args) { verify(); System.out.println("GALE_FALL_TRACE_NEGATIVES passed=12 native_physics=false"); }
    public static void verify() {
        try {
            for (boolean beforeFailure : new boolean[]{true, false}) for (boolean nativeFailure : new boolean[]{false, true})
                observerFailure(beforeFailure, nativeFailure);
            returnObserverFailure();
            nested(true); nested(false); nestedFailure();
            unscoped(); duplicate(); disabledAfterFailure(); nullReceipt();
        } catch (Throwable failure) { throw new AssertionError("Passive fall observation isolation check failed", failure); }
    }
    private static void observerFailure(boolean beforeFailure, boolean nativeFailure) throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); var diagnostic = new AssertionError("observer");
        var nativeError = new AssertionError("native"); Object expected = new Object(); int[] calls = {0};
        Throwable thrown = null; Object result = null;
        try {
            result = trace.scope(() -> trace.callback(() -> { calls[0]++; if (nativeFailure) throw nativeError; return expected; },
                id -> { if (beforeFailure) throw diagnostic; return "receipt"; },
                (receipt, returned) -> { if (!beforeFailure) throw diagnostic; }), receipt -> {});
        } catch (Throwable failure) { thrown = failure; }
        check(calls[0] == 1, "Observer failure cannot skip or duplicate the original");
        check(nativeFailure ? thrown == nativeError : thrown == null && result == expected, "Native result or Throwable identity is unchanged");
        check(trace.failure() == diagnostic && trace.idle(), "Observer error is latched and frames close");
        try { trace.checkOutsidePhysics(); throw new AssertionError("Expected deferred diagnostic failure"); }
        catch (AssertionError failure) { check(failure.getCause() == diagnostic, "Diagnostic fails only at the outside check"); }
    }
    private static void returnObserverFailure() throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); var error = new Error("reset observer"); Object value = new Object();
        check(trace.scope(() -> value, receipt -> { throw error; }) == value, "Reset observation cannot replace the native return");
        check(trace.failure() == error && trace.idle(), "Reset observer error is latched");
    }
    private static void nested(boolean innerCallback) throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); List<String> returns = new ArrayList<>(); List<Long> ids = new ArrayList<>(); int[] calls = {0};
        trace.scope(() -> trace.callback(() -> {
            calls[0]++;
            trace.scope(() -> {
                if (innerCallback) return trace.callback(() -> { calls[0]++; return null; }, id -> { ids.add(id); return "inner"; }, (r, ok) -> check("inner".equals(r) && ok, "Inner callback token"));
                return null;
            }, receipt -> returns.add(receipt));
            return null;
        }, id -> { ids.add(id); return "outer"; }, (r, ok) -> check("outer".equals(r) && ok, "Outer callback token survives reentry")), receipt -> returns.add(receipt));
        check(returns.size() == 2 && (innerCallback ? "inner".equals(returns.get(0)) : returns.get(0) == null)
            && "outer".equals(returns.get(1)), "Each return observes its exact invocation, including an inner call with no callback");
        check(calls[0] == (innerCallback ? 2 : 1) && (!innerCallback || !ids.get(0).equals(ids.get(1))), "Nested originals and invocation IDs are distinct");
        trace.checkOutsidePhysics();
    }
    private static void nestedFailure() throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); var nativeError = new Error("nested native"); int[] resets = {0};
        try {
            trace.scope(() -> trace.callback(() -> trace.scope(() -> { throw nativeError; }, r -> resets[0]++),
                id -> "outer", (r, returned) -> check(!returned, "Exceptional callback is recorded as exceptional")), r -> resets[0]++);
            throw new AssertionError("Expected original nested exception");
        } catch (Throwable failure) { check(failure == nativeError, "Nested native Throwable is not wrapped or replaced"); }
        check(resets[0] == 0 && trace.idle(), "Exceptional native returns cannot fabricate reset completion"); trace.checkOutsidePhysics();
    }
    private static void unscoped() throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); int[] observed = {0}, calls = {0};
        trace.callback(() -> { calls[0]++; return null; }, id -> { observed[0]++; return "bad"; }, (r, ok) -> observed[0]++);
        check(calls[0] == 1 && observed[0] == 0, "No original invocation means no borrowed observation frame"); trace.checkOutsidePhysics();
    }
    private static void duplicate() throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); int[] calls = {0};
        trace.scope(() -> {
            for (int i = 0; i < 2; i++) trace.callback(() -> { calls[0]++; return null; }, id -> "receipt", (r, ok) -> {});
            return null;
        }, receipt -> { throw new AssertionError("Ambiguous frame must not be reported"); });
        check(calls[0] == 2 && trace.failure() instanceof IllegalStateException && trace.idle(), "Duplicate observation fails closed without skipping originals");
    }
    private static void disabledAfterFailure() throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); var error = new Error("observer"); int[] calls = {0}, observations = {0};
        for (int i = 0; i < 2; i++) trace.scope(() -> trace.callback(() -> { calls[0]++; return null; },
            id -> { observations[0]++; throw error; }, (r, ok) -> { throw new AssertionError("Failed observation must stay disabled"); }), r -> {});
        check(calls[0] == 2 && observations[0] == 1 && trace.failure() == error && trace.idle(), "A latched observer stays disabled while native originals continue");
    }
    private static void nullReceipt() throws Throwable {
        var trace = new GaleVaultFallTrace<String>(); int[] calls = {0};
        trace.scope(() -> trace.callback(() -> { calls[0]++; return null; }, id -> null, (r, ok) -> check(r == null && ok, "Refused callback receipt remains null")),
            receipt -> check(receipt == null, "No receipt is borrowed from another event"));
        check(calls[0] == 1, "Refusing observation does not suppress the original"); trace.checkOutsidePhysics();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
