package dev.wildercord.gametest.stonehinge.peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/** Failure-only diagnostics: reporting and cleanup must never replace the original test failure. */
public final class StoneHingePeerFailure {
    private StoneHingePeerFailure() {}

    public static void report(Throwable original, Runnable report) {
        try { report.run(); }
        catch (Throwable reporting) { suppress(original, reporting); }
    }

    public static void cleanup(Throwable original, Runnable cleanup, Consumer<Throwable> report) {
        try { cleanup.run(); }
        catch (Throwable failure) {
            if (original != null) suppress(original, failure);
            else {
                report(failure, () -> report.accept(failure));
                throw failure;
            }
        }
    }

    private static void suppress(Throwable original, Throwable secondary) {
        if (original != secondary) original.addSuppressed(secondary);
    }

    public static void writeAtomic(Path destination, Path temporary, Properties value) throws IOException {
        if (Files.exists(destination) || Files.exists(temporary)) throw new IOException("Write-once native witness");
        var bytes = new ByteArrayOutputStream();
        value.store(bytes, "Bounded Stone Hinge diagnostic witness");
        if (bytes.size() > 65536) throw new IOException("Bounded native witness");
        Files.write(temporary, bytes.toByteArray(), StandardOpenOption.CREATE_NEW);
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
    }

    public static Map<String, String> describe(Throwable original) {
        var fields = new LinkedHashMap<String, String>();
        fields.put("error", bounded(original.toString()));
        var seen = new IdentityHashMap<Throwable, Boolean>();
        Throwable cause = original;
        int count = 0;
        while (cause != null && count < 4 && seen.put(cause, Boolean.TRUE) == null) {
            String prefix = "cause." + count++ + ".";
            fields.put(prefix + "class", bounded(cause.getClass().getName()));
            fields.put(prefix + "message", bounded(cause.getMessage()));
            StackTraceElement[] frames = cause.getStackTrace();
            int length = Math.min(frames.length, 6);
            fields.put(prefix + "frameCount", Integer.toString(length));
            fields.put(prefix + "framesTruncated", Boolean.toString(frames.length > length));
            for (int i = 0; i < length; i++) fields.put(prefix + "frame." + i, bounded(frames[i].toString()));
            cause = cause.getCause();
        }
        fields.put("causeCount", Integer.toString(count));
        fields.put("causesTruncated", Boolean.toString(cause != null));
        fields.put("suppressedCount", Integer.toString(original.getSuppressed().length));
        return fields;
    }

    private static String bounded(String value) {
        if (value == null) return "";
        return value.length() <= 256 ? value : value.substring(0, 253) + "...";
    }
}
