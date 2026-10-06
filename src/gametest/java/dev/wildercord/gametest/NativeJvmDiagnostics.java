package dev.wildercord.gametest;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

/** Read-only self-JVM evidence, present only in the client gametest source set. */
public final class NativeJvmDiagnostics {
	static final int MAX_THREADS = 4;
	static final int MAX_SERVER_THREADS = 2;
	static final int MAX_DISCOVERY_IDS = 256;
	static final int MAX_DEPTH = 64;
	static final int MAX_BYTES = 64 * 1024;
	private static final Scene STOPPED = new Scene("none", "stopped");
	private static volatile Session active;

	private NativeJvmDiagnostics() { }

	enum Mode {
		DISABLED(new long[0]), FOCUSED(new long[] {1800, 2700}), MASTERS(new long[] {1800, 2700, 4500}), FULL(new long[] {5400, 9000});
		final long[] seconds;
		Mode(long[] seconds) { this.seconds = seconds; }
	}

	static Mode mode(String ci, String job) {
		if (!"true".equals(ci) || job == null) return Mode.DISABLED;
		return switch (job) {
			case "masters-native" -> Mode.MASTERS;
			case "articulated-native", "native-diagnostic" -> Mode.FOCUSED;
			case "game-tests" -> Mode.FULL;
			default -> Mode.DISABLED;
		};
	}

	/** Called by the pinned runner on the render thread, before it starts the test thread. */
	public static void start() {
		try {
			stop();
			// Read only these two non-sensitive activation selectors. Never enumerate or log env.
			Mode mode = mode(System.getenv("CI"), System.getenv("GITHUB_JOB"));
			if (mode == Mode.DISABLED) return;
			Session session = new Session(mode, Thread.currentThread().threadId(), System::nanoTime,
				new FileSnapshots(Path.of("logs", "ci-diagnostics")));
			active = session;
			session.start();
		} catch (Throwable ignored) {
			stop(); // Diagnostics must not replace even an Error thrown by the actual test.
		}
	}

	public static void testThread() {
		try {
			Session session = active;
			if (session != null) session.testThreadId = Thread.currentThread().threadId();
		} catch (Throwable ignored) { }
	}

	public static void scene(String suite, String phase) {
		try {
			Session session = active;
			if (session != null) session.scene(suite, phase);
		} catch (Throwable ignored) { }
	}

	public static void stop() {
		try {
			Session session = active;
			active = null;
			if (session != null) session.close();
		} catch (Throwable ignored) { }
	}

	// Only primitive IDs and immutable strings cross threads; no contexts, worlds or entities.
	record Scene(String suite, String phase) { }

	@FunctionalInterface
	interface SnapshotSink {
		void write(Session session, long threshold, long elapsedNanos) throws Exception;
	}

	static final class Session {
		final Mode mode;
		final long renderThreadId;
		final LongSupplier clock;
		final SnapshotSink sink;
		final long started;
		volatile long testThreadId;
		volatile Scene scene = new Scene("none", "runner-start");
		volatile boolean stopped;
		Thread worker;
		int next;

		Session(Mode mode, long renderThreadId, LongSupplier clock, SnapshotSink sink) {
			this.mode = mode;
			this.renderThreadId = renderThreadId;
			this.clock = clock;
			this.sink = sink;
			this.started = clock.getAsLong();
		}

		void scene(String suite, String phase) {
			if (stopped) return;
			String boundedPhase = switch (phase) {
				case "setup", "run", "cleanup" -> phase;
				default -> "between-scenes";
			};
			scene = new Scene("between-scenes".equals(boundedPhase) ? "none" : token(suite, 240), boundedPhase);
		}

		void start() {
			worker = Thread.ofPlatform().name("wildercord-native-snapshots").daemon()
				.inheritInheritableThreadLocals(false).unstarted(this::watch);
			worker.setContextClassLoader(null);
			worker.start();
		}

		private void watch() {
			try {
				while (!stopped && next < mode.seconds.length) {
					long now = clock.getAsLong();
					poll(now);
					if (!stopped && next < mode.seconds.length) {
						long remaining = TimeUnit.SECONDS.toNanos(mode.seconds[next]) - (clock.getAsLong() - started);
						if (remaining > 0) LockSupport.parkNanos(this, remaining);
					}
				}
			} catch (Throwable ignored) {
				// No retries, rethrows, test-thread interruption or process termination.
			} finally {
				try { close(); } catch (Throwable ignored) { }
			}
		}

		void poll(long now) {
			long elapsed = now - started; // monotonic; subtraction also handles nanoTime wraparound
			while (!stopped && next < mode.seconds.length
					&& elapsed >= TimeUnit.SECONDS.toNanos(mode.seconds[next])) {
				long threshold = mode.seconds[next++]; // consume before I/O, including failures
				try {
					sink.write(this, threshold, elapsed);
				} catch (Throwable ignored) { }
			}
		}

		void close() {
			stopped = true;
			testThreadId = 0;
			scene = STOPPED;
			if (worker != null && worker != Thread.currentThread()) worker.interrupt();
			// Never join: a blocked diagnostic filesystem must not hold up runner teardown.
		}
	}

	static final class FileSnapshots implements SnapshotSink {
		private final Path parent;
		private Path directory;

		FileSnapshots(Path parent) { this.parent = parent; }

		@Override
		public void write(Session session, long threshold, long elapsedNanos) throws Exception {
			String snapshot = snapshot(ManagementFactory.getThreadMXBean(), session, threshold, elapsedNanos);
			if (session.stopped) return;
			if (directory == null) {
				Files.createDirectories(parent);
				directory = Files.createTempDirectory(parent, "jvm-");
			}
			Files.writeString(directory.resolve("snapshot-" + threshold + ".txt"), snapshot,
				StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		}
	}

	static String snapshot(ThreadMXBean bean, Session session, long threshold, long elapsedNanos) {
		Scene scene = session.scene;
		BoundedText out = new BoundedText();
		out.add("WILDERCORD_NATIVE_JVM_SNAPSHOT observational-only\n");
		out.add("mode=" + session.mode.name() + " thresholdSeconds=" + threshold
			+ " elapsedSeconds=" + TimeUnit.NANOSECONDS.toSeconds(elapsedNanos) + "\n");
		out.add("suite=" + scene.suite + " phase=" + scene.phase + "\n");
		Map<Long, String> selected = new LinkedHashMap<>();
		if (session.testThreadId > 0) selected.put(session.testThreadId, "test");
		if (session.renderThreadId > 0) selected.putIfAbsent(session.renderThreadId, "render");
		// Names are inspected only at depth zero, and never serialized. There is no all-stack dump.
		long[] ids = bean.getAllThreadIds();
		Arrays.sort(ids);
		if (ids.length > MAX_DISCOVERY_IDS) out.add("serverDiscovery=limited\n");
		int servers = 0;
		for (int i = 0; i < Math.min(ids.length, MAX_DISCOVERY_IDS)
				&& selected.size() < MAX_THREADS && servers < MAX_SERVER_THREADS; i++) {
			if (selected.containsKey(ids[i])) continue;
			ThreadInfo info = bean.getThreadInfo(ids[i], 0);
			if (info != null && "Server thread".equals(info.getThreadName())) {
				selected.put(ids[i], "server");
				servers++;
			}
		}
		for (Map.Entry<Long, String> entry : selected.entrySet()) {
			ThreadInfo info = bean.getThreadInfo(entry.getKey(), MAX_DEPTH);
			out.add("thread role=" + entry.getValue() + " id=" + entry.getKey()
				+ " state=" + (info == null ? "unavailable" : info.getThreadState()) + "\n");
			if (info == null) continue;
			// Emit only symbols and line numbers, never ThreadInfo.toString(), lock values or locals.
			for (StackTraceElement frame : info.getStackTrace()) {
				out.add("  at " + token(frame.getClassName(), 240) + "." + token(frame.getMethodName(), 160)
					+ ":" + frame.getLineNumber() + (frame.isNativeMethod() ? " native" : "") + "\n");
			}
		}
		return out.toString();
	}

	static String token(String value, int limit) {
		if (value == null) return "none";
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < Math.min(value.length(), limit); i++) {
			char c = value.charAt(i);
			out.append(c >= 33 && c <= 126 ? c : '_');
		}
		return out.toString();
	}

	static final class BoundedText {
		private static final String TRUNCATED = "\n[byte limit]\n";
		private final StringBuilder text = new StringBuilder();
		private boolean truncated;
		void add(String line) {
			if (truncated) return;
			int remaining = MAX_BYTES - TRUNCATED.length() - text.length();
			if (line.length() <= remaining) text.append(line);
			else {
				text.append(line, 0, remaining).append(TRUNCATED);
				truncated = true;
			}
		}
		@Override public String toString() { return text.toString(); }
	}
}
