package dev.wildercord.cast;

/** Standalone negative controls for the passive identity/time fence; never registered as a gallery case. */
public final class StasisDeliveryProbeScopeChecks {
    public static void main(String[] args) {
        Object owner = new Object(), world = new Object();
        var scope = new StasisDeliveryProbe.Scope(owner, world, 4974);
        check(scope.accepts(owner, world, 4974), "opening tick is observed");
        check(scope.accepts(owner, world, 4983), "tenth existing tick is observed");
        check(!scope.accepts(owner, world, 4973), "earlier ticks are excluded");
        check(!scope.accepts(owner, world, 4984), "eleventh tick is excluded");
        check(!scope.accepts(new Object(), world, 4979), "other owner/body is excluded");
        check(!scope.accepts(owner, new Object(), 4979), "other world is excluded");
        scope.closed = true;
        check(!scope.accepts(owner, world, 4979), "closed scope is inactive even inside its time window");
        int[] calls = {0};
        StasisDeliveryProbe.beam(null, null, null, null, () -> calls[0]++);
        check(calls[0] == 1, "inactive observer calls original exactly once");
        RuntimeException expected = new RuntimeException("original failure");
        try {
            StasisDeliveryProbe.beam(null, null, null, null, () -> { calls[0]++; throw expected; });
            throw new AssertionError("original failure was swallowed");
        } catch (RuntimeException actual) { check(actual == expected, "original failure is preserved"); }
        check(calls[0] == 2, "throwing original was not retried");
        StasisDeliveryProbe.clipped(null);
        StasisDeliveryProbe.hit(null, null);
        System.out.println("Stasis delivery scope: 12 controls passed");
    }
    private static void check(boolean ok, String detail) { if (!ok) throw new AssertionError(detail); }
}
