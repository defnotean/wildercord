package dev.wildercord.gametest.stonehinge.peer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Exercises the actual failure helper without Minecraft, sockets, or replacing a native assertion. */
public final class StoneHingePeerFailureTest {
    private static int checks;
    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
    private static Throwable caught(Runnable action) {
        try { action.run(); } catch (Throwable failure) { return failure; }
        throw new AssertionError("Expected failure");
    }
    private static final class Server implements AutoCloseable {
        final List<String> events;
        final AssertionError closeFailure;
        Server(List<String> events, AssertionError failure) { this.events = events; closeFailure = failure; }
        public void close() {
            check(events.contains("natural-witness") && events.contains("host-witness"), "Both witnesses precede native close");
            events.add("server-close"); throw closeFailure;
        }
    }
    public static void main(String[] args) throws Exception {
        preservesNestedFailure(); cleanupAndReentry(); boundedCauses(); atomicWitness(Path.of(args[0]));
        System.out.println("Stone Hinge peer failure: " + checks + " checks passed");
    }
    private static void preservesNestedFailure() {
        var original = new AssertionError("owner wait", new IllegalStateException("predicate"));
        var reporting = new AssertionError("report failed");
        var cleanup = new AssertionError("Master cleanup failed");
        var closing = new AssertionError("native close failed");
        var events = new ArrayList<String>();
        Throwable result = caught(() -> {
            try (var server = new Server(events, closing)) {
                try {
                    Throwable primary = null;
                    try { throw original; }
                    catch (Throwable failure) {
                        primary = failure;
                        StoneHingePeerFailure.report(failure, () -> { events.add("natural-witness"); throw reporting; });
                        throw failure;
                    } finally {
                        StoneHingePeerFailure.cleanup(primary, () -> { events.add("natural-cleanup"); throw cleanup; }, failure -> {
                            throw new AssertionError("Existing original must remain primary");
                        });
                    }
                } catch (Throwable failure) {
                    check(failure == original, "Host receives exact original object");
                    StoneHingePeerFailure.report(failure, () -> events.add("host-witness"));
                    throw failure;
                }
            }
        });
        check(result == original && result.getCause() == original.getCause(), "Original Throwable and cause survive all scopes");
        check(List.of(result.getSuppressed()).equals(List.of(reporting, cleanup, closing)), "Reporting, cleanup and close are suppressed in order");
        check(events.equals(List.of("natural-witness", "natural-cleanup", "host-witness", "server-close")), "Witness precedes cleanup and server close");
    }
    private static void cleanupAndReentry() {
        var events = new ArrayList<String>();
        for (int run = 0; run < 2; run++) {
            var primary = new AssertionError("original-" + run);
            StoneHingePeerFailure.report(primary, () -> {
                StoneHingePeerFailure.report(primary, () -> { throw primary; });
                events.add("nested");
            });
            check(primary.getSuppressed().length == 0, "Self-reporting never self-suppresses or poisons next run");
            StoneHingePeerFailure.cleanup(primary, () -> events.add("clean"), failure -> events.add("unexpected"));
            var cleanup = new AssertionError("cleanup-" + run);
            var reporting = new AssertionError("cleanup report");
            check(caught(() -> StoneHingePeerFailure.cleanup(null, () -> { throw cleanup; }, failure -> {
                check(failure == cleanup, "Cleanup-only failure is reported with original identity");
                throw reporting;
            })) == cleanup, "Cleanup-only failure retains identity");
            check(List.of(cleanup.getSuppressed()).equals(List.of(reporting)), "Cleanup reporting failure is suppressed");
        }
        StoneHingePeerFailure.cleanup(null, () -> events.add("success"), failure -> events.add("unexpected"));
        check(events.equals(List.of("nested", "clean", "nested", "clean", "success")), "Clean and reentered calls share no failure state");
    }
    private static void boundedCauses() {
        var first = new AssertionError("x".repeat(100000));
        Throwable current = first;
        for (int i = 0; i < 10; i++) {
            var next = new IllegalStateException("cause".repeat(10000));
            current.initCause(next); current = next;
        }
        var frames = new StackTraceElement[100];
        java.util.Arrays.fill(frames, new StackTraceElement("class".repeat(100), "method".repeat(100), "file".repeat(100), 99));
        first.setStackTrace(frames);
        var fields = StoneHingePeerFailure.describe(first);
        check(fields.get("causeCount").equals("4") && fields.get("causesTruncated").equals("true"), "Cause traversal is bounded");
        check(fields.get("cause.0.frameCount").equals("6") && fields.get("cause.0.framesTruncated").equals("true"), "Stack traversal is bounded");
        check(fields.values().stream().allMatch(value -> value.length() <= 256), "Every message and frame is bounded");
        var a = new IllegalStateException("a"); var b = new IllegalStateException("b");
        a.initCause(b); b.initCause(a);
        check(StoneHingePeerFailure.describe(a).get("causeCount").equals("2"), "Cyclic causes terminate");
    }
    private static void atomicWitness(Path directory) throws Exception {
        Path target = directory.resolve("host-failure.properties"), temporary = directory.resolve("host-failure.tmp");
        Properties value = new Properties(); value.putAll(StoneHingePeerFailure.describe(new AssertionError("original")));
        value.setProperty("nonce", "nonce-test"); value.setProperty("role", "host"); value.setProperty("pid", "123");
        value.setProperty("case", "NATURAL_MASTER"); value.setProperty("caseIndex", "16");
        value.setProperty("phase", "owner-motion-wait");
        value.setProperty("expectedMotion", "(0.0, 0.36080000519752503, -0.4000000059604645)");
        value.setProperty("actualOriginalTrackerRaw", "(0.0, 0.2751840104494096, -0.2184000286221508)");
        StoneHingePeerFailure.writeAtomic(target, temporary, value);
        check(Files.isRegularFile(target) && !Files.exists(temporary), "Atomic publication leaves complete target only");
        Properties restored = new Properties(); try (var input = Files.newInputStream(target)) { restored.load(input); }
        check(restored.equals(value), "Witness roundtrip retains original error, identity, phase and distinct vectors");
        byte[] original = Files.readAllBytes(target);
        try { StoneHingePeerFailure.writeAtomic(target, temporary, new Properties()); throw new AssertionError("Overwrote original"); }
        catch (java.io.IOException expected) { check(java.util.Arrays.equals(original, Files.readAllBytes(target)), "Repeated reporting cannot replace original witness"); }
        Path oversized = directory.resolve("oversized.properties");
        value.setProperty("oversized", "x".repeat(65537));
        try { StoneHingePeerFailure.writeAtomic(oversized, temporary, value); throw new AssertionError("Oversized witness allowed"); }
        catch (java.io.IOException expected) { check(!Files.exists(oversized) && !Files.exists(temporary), "Oversized reporting publishes no partial file"); }
        Path missing = directory.resolve("missing/host-failure.properties");
        try { StoneHingePeerFailure.writeAtomic(missing, directory.resolve("missing/host-failure.tmp"), new Properties()); throw new AssertionError("Missing directory allowed"); }
        catch (java.io.IOException expected) { check(!Files.exists(missing), "Failed reporting does not manufacture success"); }
    }
}
