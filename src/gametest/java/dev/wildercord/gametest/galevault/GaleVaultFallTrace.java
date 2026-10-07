package dev.wildercord.gametest.galevault;

/** Pure test-only observation boundary. Native operations run once; observer failures are deferred out of physics. */
public final class GaleVaultFallTrace<R> {
    @FunctionalInterface public interface Original<T> { T call() throws Throwable; }
    @FunctionalInterface public interface Before<R> { R read(long invocation) throws Throwable; }
    @FunctionalInterface public interface After<R> { void read(R receipt, boolean returned) throws Throwable; }
    @FunctionalInterface public interface Returned<R> { void read(R receipt) throws Throwable; }
    private static final class Frame<R> {
        final long invocation;
        final Frame<R> parent;
        R receipt;
        boolean callbackObserved;
        Frame(long invocation, Frame<R> parent) { this.invocation = invocation; this.parent = parent; }
    }
    private Frame<R> current;
    private Throwable observerFailure;
    private long invocation;

    /** Wrap the existing native method, including its exceptional exit; never synthesize another native call. */
    public <T> T scope(Original<T> original, Returned<R> afterReturn) throws Throwable {
        Frame<R> frame = null;
        if (observerFailure == null) {
            try { frame = new Frame<>(++invocation, current); current = frame; }
            catch (Throwable failure) { latch(failure); }
        }
        boolean returned = false;
        try {
            T result = original.call();
            returned = true;
            return result;
        } finally {
            if (frame != null) {
                try {
                    if (returned && observerFailure == null) {
                        try { afterReturn.read(frame.receipt); }
                        catch (Throwable failure) { latch(failure); }
                    }
                } finally { current = frame.parent; }
            }
        }
    }
    /** The callback receipt belongs to the exact current method frame, never to the most recent global receipt. */
    public <T> T callback(Original<T> original, Before<R> before, After<R> after) throws Throwable {
        Frame<R> frame = current;
        R receipt = null;
        if (observerFailure == null && frame != null) {
            try {
                if (frame.callbackObserved) throw new IllegalStateException("Duplicate callback in one native fall-check invocation");
                frame.callbackObserved = true;
                receipt = before.read(frame.invocation);
                frame.receipt = receipt;
            } catch (Throwable failure) { latch(failure); }
        }
        boolean returned = false;
        try {
            T result = original.call();
            returned = true;
            return result;
        } finally {
            if (observerFailure == null && frame != null) {
                try { after.read(receipt, returned); }
                catch (Throwable failure) { latch(failure); }
            }
        }
    }
    private void latch(Throwable failure) { if (observerFailure == null) observerFailure = failure; }
    public void recordFailure(Throwable failure) { latch(failure); }
    public Throwable failure() { return observerFailure; }
    public boolean idle() { return current == null; }
    /** Only the GameTest driver may call this between native physics invocations. */
    public void checkOutsidePhysics() {
        if (current != null) throw new AssertionError("Passive native fall observation still has an open invocation");
        if (observerFailure != null) throw new AssertionError("Passive native fall observer failed", observerFailure);
    }
}
