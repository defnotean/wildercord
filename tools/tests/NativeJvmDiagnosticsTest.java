package dev.wildercord.gametest;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone JDK harness: tests the actual gametest helper without Minecraft or extra dependencies. */
public final class NativeJvmDiagnosticsTest {
	private static int checks;
	private static void check(boolean condition, String message) {
		checks++;
		if (!condition) throw new AssertionError(message);
	}

	public static void main(String[] args) throws Exception {
		activation();
		thresholds();
		failures();
		scopeAndBounds();
		files(Path.of(args[0]));
		backgroundLifecycle();
		System.out.println("Native JVM diagnostics: " + checks + " checks passed");
	}

	private static void activation() {
		for (String job : new String[] {"masters-native", "articulated-native"}) {
			check(NativeJvmDiagnostics.mode("true", job) == NativeJvmDiagnostics.Mode.FOCUSED, "focused job");
			for (String ci : new String[] {null, "", "false", "TRUE", "1"}) {
				check(NativeJvmDiagnostics.mode(ci, job) == NativeJvmDiagnostics.Mode.DISABLED, "fail closed without exact CI");
			}
		}
		check(NativeJvmDiagnostics.mode("true", "game-tests") == NativeJvmDiagnostics.Mode.FULL, "full job");
		for (String job : new String[] {null, "", "build", "masters-native-extra", "game-tests (1/4)"}) {
			check(NativeJvmDiagnostics.mode("true", job) == NativeJvmDiagnostics.Mode.DISABLED, "unknown job disabled");
		}
	}

	private static void thresholds() {
		for (var mode : new NativeJvmDiagnostics.Mode[] {NativeJvmDiagnostics.Mode.FOCUSED, NativeJvmDiagnostics.Mode.FULL}) {
			// Deliberately cross the signed nanoTime wraparound boundary.
			long start = Long.MAX_VALUE - TimeUnit.SECONDS.toNanos(10);
			List<Long> calls = new ArrayList<>();
			var session = new NativeJvmDiagnostics.Session(mode, 1, () -> start,
				(s, threshold, elapsed) -> calls.add(threshold));
			session.scene("dev.wildercord.SomeTest", "setup");
			long first = TimeUnit.SECONDS.toNanos(mode.seconds[0]);
			session.poll(start + first - 1);
			check(calls.isEmpty(), "no early snapshot");
			session.poll(start + first);
			session.poll(start + first);
			check(calls.equals(List.of(mode.seconds[0])), "one first threshold");
			session.scene("dev.wildercord.OtherTest", "run");
			session.poll(start + TimeUnit.SECONDS.toNanos(mode.seconds[1]));
			session.poll(start + TimeUnit.DAYS.toNanos(1));
			check(calls.equals(List.of(mode.seconds[0], mode.seconds[1])), "two total across scene transitions");
			session.close();
			session.scene("dev.wildercord.LateTest", "setup");
			check(session.scene.suite().equals("none") && session.testThreadId == 0, "close clears scene and IDs");
		}
		var mode = NativeJvmDiagnostics.Mode.FOCUSED;
		List<Long> calls = new ArrayList<>();
		var closed = new NativeJvmDiagnostics.Session(mode, 1, () -> 0, (s, t, e) -> calls.add(t));
		closed.close();
		closed.poll(TimeUnit.DAYS.toNanos(1));
		check(calls.isEmpty(), "completed runner cannot snapshot");
		var stopsDuringCapture = new NativeJvmDiagnostics.Session(mode, 1, () -> 0,
			(s, t, e) -> { calls.add(t); s.close(); });
		stopsDuringCapture.poll(TimeUnit.DAYS.toNanos(1));
		check(calls.equals(List.of(1800L)), "stop during first capture suppresses second overdue snapshot");
	}

	private static void failures() {
		List<Long> calls = new ArrayList<>();
		var session = new NativeJvmDiagnostics.Session(NativeJvmDiagnostics.Mode.FOCUSED, 1, () -> 0,
			(s, threshold, elapsed) -> {
				calls.add(threshold);
				if (calls.size() == 1) throw new IOException("must not become test result");
				throw new AssertionError("must not replace original test failure");
			});
		AssertionError original = new AssertionError("original");
		try {
			try { throw original; }
			finally { session.poll(TimeUnit.DAYS.toNanos(1)); session.close(); }
		} catch (AssertionError caught) {
			check(caught == original, "original throwable identity survives diagnostic IOException and Error");
		}
		session.poll(TimeUnit.DAYS.toNanos(2));
		check(calls.equals(List.of(1800L, 2700L)), "failed snapshots are consumed, never retried");
	}

	private static Thread parked(String name, CountDownLatch ready, CountDownLatch release) {
		return Thread.ofPlatform().name(name).daemon().start(() -> {
			ready.countDown();
			try { release.await(); } catch (InterruptedException ignored) { }
		});
	}

	private static void scopeAndBounds() throws Exception {
		CountDownLatch ready = new CountDownLatch(6), release = new CountDownLatch(1);
		Thread test = parked("private-test-name", ready, release);
		Thread render = parked("private-render-name", ready, release);
		Thread unrelated = parked("unrelated-private-worker", ready, release);
		List<Thread> servers = new ArrayList<>();
		for (int i = 0; i < 3; i++) servers.add(parked("Server thread", ready, release));
		check(ready.await(3, TimeUnit.SECONDS), "fixture threads started");
		try {
			var session = new NativeJvmDiagnostics.Session(NativeJvmDiagnostics.Mode.FOCUSED,
				render.threadId(), () -> 0, (s, t, e) -> { });
			session.testThreadId = test.threadId();
			session.scene("dev.wildercord.SomeTest", "cleanup");
			ThreadMXBean real = ManagementFactory.getThreadMXBean();
			List<Long> stackRequests = new ArrayList<>();
			ThreadMXBean observed = (ThreadMXBean) Proxy.newProxyInstance(ThreadMXBean.class.getClassLoader(),
				new Class<?>[] {ThreadMXBean.class}, (proxy, method, args) -> {
					if (method.getName().equals("getAllThreadIds")) return real.getAllThreadIds();
					check(method.getName().equals("getThreadInfo") && args.length == 2, "only scoped ThreadInfo API used");
					int depth = (int) args[1];
					check(depth == 0 || depth == NativeJvmDiagnostics.MAX_DEPTH, "bounded stack depth");
					if (depth > 0) stackRequests.add((long) args[0]);
					return method.invoke(real, args);
				});
			String text = NativeJvmDiagnostics.snapshot(observed, session, 1800, TimeUnit.SECONDS.toNanos(1800));
			check(stackRequests.size() == 4 && stackRequests.get(0) == test.threadId()
				&& stackRequests.get(1) == render.threadId(), "primary threads first and four-thread cap");
			check(!stackRequests.contains(unrelated.threadId()), "unrelated worker stack never requested");
			check(text.contains("role=test") && text.contains("role=render") && text.contains("role=server"), "all expected roles");
			check(text.contains("CountDownLatch.await") && text.contains("phase=cleanup"), "real wait and runner phase retained");
			check(!text.contains("private-") && !text.contains("unrelated-private-worker") && !text.contains("Server thread"), "raw names never emitted");
			check(text.length() <= NativeJvmDiagnostics.MAX_BYTES && text.chars().allMatch(c -> c < 128), "ASCII byte bound");
			stackRequests.clear();
			session.testThreadId = 0;
			NativeJvmDiagnostics.snapshot(observed, session, 1800, 0);
			check(stackRequests.size() == 3, "server cap remains two before test thread starts");
			session.testThreadId = Long.MAX_VALUE;
			String gone = NativeJvmDiagnostics.snapshot(real, session, 2700, 0);
			check(gone.contains("id=" + Long.MAX_VALUE + " state=unavailable"), "ended thread tolerated");
			var bounded = new NativeJvmDiagnostics.BoundedText();
			bounded.add("x".repeat(NativeJvmDiagnostics.MAX_BYTES * 2));
			bounded.add("never appended");
			check(bounded.toString().length() == NativeJvmDiagnostics.MAX_BYTES
				&& bounded.toString().endsWith("[byte limit]\n"), "hard byte cap with truncation marker");
			check(NativeJvmDiagnostics.token("unsafe\n\u1234".repeat(100), 240).length() == 240, "individual symbol cap");
			AtomicLong namesInspected = new AtomicLong();
			ThreadMXBean crowded = (ThreadMXBean) Proxy.newProxyInstance(ThreadMXBean.class.getClassLoader(),
				new Class<?>[] {ThreadMXBean.class}, (proxy, method, args) -> {
					if (method.getName().equals("getAllThreadIds")) return java.util.stream.LongStream.range(10000, 11000).toArray();
					if ((int) args[1] == 0) { namesInspected.incrementAndGet(); return null; }
					return method.invoke(real, args);
				});
			String limited = NativeJvmDiagnostics.snapshot(crowded, session, 1800, 0);
			check(namesInspected.get() == 256 && limited.contains("serverDiscovery=limited"), "large JVM discovery remains bounded");
		} finally {
			release.countDown();
			for (Thread thread : servers) thread.join(1000);
			test.join(1000); render.join(1000); unrelated.join(1000);
		}
	}

	private static void files(Path root) throws Exception {
		Files.createDirectories(root);
		var writer = new NativeJvmDiagnostics.FileSnapshots(root.resolve("logs/ci-diagnostics"));
		var session = new NativeJvmDiagnostics.Session(NativeJvmDiagnostics.Mode.FOCUSED,
			Thread.currentThread().threadId(), () -> 0, writer);
		check(!Files.exists(root.resolve("logs")), "no diagnostic directories before threshold");
		session.poll(TimeUnit.DAYS.toNanos(1));
		session.poll(TimeUnit.DAYS.toNanos(2));
		try (var paths = Files.walk(root)) {
			List<Path> files = paths.filter(Files::isRegularFile).toList();
			check(files.size() == 2, "exactly two snapshot files");
			for (Path file : files) check(Files.size(file) <= NativeJvmDiagnostics.MAX_BYTES, "file byte bound");
		}
		Path block = root.resolve("not-a-directory");
		Files.writeString(block, "unchanged");
		var broken = new NativeJvmDiagnostics.Session(NativeJvmDiagnostics.Mode.FOCUSED,
			Thread.currentThread().threadId(), () -> 0, new NativeJvmDiagnostics.FileSnapshots(block));
		broken.poll(TimeUnit.DAYS.toNanos(1));
		check(broken.next == 2 && Files.readString(block).equals("unchanged"), "filesystem failure leaves existing file intact");
	}

	private static void backgroundLifecycle() throws Exception {
		InheritableThreadLocal<Object> inherited = new InheritableThreadLocal<>();
		inherited.set(new Object());
		AtomicLong now = new AtomicLong();
		CountDownLatch captured = new CountDownLatch(1), release = new CountDownLatch(1);
		AtomicReference<String> failure = new AtomicReference<>();
		var session = new NativeJvmDiagnostics.Session(NativeJvmDiagnostics.Mode.FOCUSED, 1, now::get,
			(s, t, e) -> {
				if (!Thread.currentThread().isDaemon() || inherited.get() != null
						|| Thread.currentThread().getContextClassLoader() != null) failure.set("retained caller state");
				captured.countDown();
				// Simulate I/O which ignores interruption; runner teardown still must never join it.
				while (release.getCount() > 0) {
					try { release.await(); } catch (InterruptedException ignored) { }
				}
			});
		now.set(TimeUnit.SECONDS.toNanos(1800));
		session.start();
		try {
			check(captured.await(3, TimeUnit.SECONDS), "background captures without ticks or output");
			long before = System.nanoTime();
			session.close();
			check(System.nanoTime() - before < TimeUnit.SECONDS.toNanos(1), "teardown does not wait for blocked diagnostics");
			check(failure.get() == null && session.worker.isDaemon(), "daemon does not inherit caller locals or classloader");
		} finally {
			release.countDown();
			session.close();
			session.worker.join(3000);
			inherited.remove();
		}
		check(!session.worker.isAlive(), "worker exits after teardown");
		var sleeper = new NativeJvmDiagnostics.Session(NativeJvmDiagnostics.Mode.FULL, 1, System::nanoTime, (s, t, e) -> { });
		sleeper.start();
		sleeper.close();
		sleeper.worker.join(3000);
		check(!sleeper.worker.isAlive(), "long deadline sleep is interrupted by teardown");
	}
}
