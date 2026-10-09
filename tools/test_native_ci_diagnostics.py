"""Native diagnostics ownership, timing and failure isolation; no Minecraft launch."""
from contextlib import nullcontext, redirect_stdout
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from unittest.mock import Mock, patch

import client_suites
import native_ci_diagnostics as diagnostics
import run_client_ci
import test_client_ci


class ProcTree:
    def __init__(self, path):
        self.path = path

    def process(self, pid, parent, *, group=100, session=100, started=1, executable="/jdk/bin/java", children=()):
        root = self.path / str(pid)
        root.mkdir(exist_ok=True)
        # stat fields 3..22; comm deliberately includes whitespace and parentheses.
        fields = ["S", str(parent), str(group), str(session)] + ["0"] * 15 + [str(started)]
        (root / "stat").write_text(f"{pid} (java worker (test)) " + " ".join(fields))
        if not (root / "exe").is_symlink():
            (root / "exe").symlink_to(executable)
        task = root / "task" / str(pid)
        task.mkdir(parents=True, exist_ok=True)
        (task / "children").write_text(" ".join(map(str, children)))
        return diagnostics.process_identity(pid, self.path)


class SnapshotBudgetTests(unittest.TestCase):
    def test_expanded_masters_gets_bounded_late_snapshot_without_changing_other_jobs(self):
        self.assertEqual(diagnostics.snapshot_thresholds({"kind": "suite", "name": "masters"}), (1800, 2700, 4500))
        self.assertEqual(diagnostics.snapshot_thresholds({"kind": "suite", "name": "articulated"}), (1800, 2700))
        self.assertEqual(diagnostics.snapshot_thresholds({"kind": "diagnostic", "name": "diagnostic-progression-feasibility"}), (1800, 2700))
        self.assertEqual(diagnostics.snapshot_thresholds({"kind": "shard"}), (5400, 9000))


class OwnershipTests(unittest.TestCase):
    def test_only_java_descendants_in_original_group_and_session(self):
        with tempfile.TemporaryDirectory() as temp:
            tree = ProcTree(Path(temp))
            root = tree.process(100, 9, executable="/usr/bin/sh", children=[101, 102, 103, 104])
            expected = tree.process(101, 100, children=[105])
            tree.process(102, 100, group=102)
            tree.process(103, 100, session=103)
            tree.process(104, 99)  # Reused child PID no longer belongs to this parent.
            tree.process(105, 101, executable="/usr/bin/Xvfb", children=[106])
            nested = tree.process(106, 105)
            tree.process(999, 1)  # Must never be inspected even with a matching group.
            original = diagnostics.process_identity
            with patch.object(diagnostics, "process_identity", wraps=original) as read:
                self.assertEqual(diagnostics.owned_java_processes(root, tree.path), [expected, nested])
            self.assertNotIn(999, [call.args[0] for call in read.call_args_list])

    def test_descendants_forked_from_nonleader_thread_are_found(self):
        with tempfile.TemporaryDirectory() as temp:
            tree = ProcTree(Path(temp))
            root = tree.process(100, 9, executable="/usr/bin/sh")
            task = tree.path / "100/task/150"
            task.mkdir()
            (task / "children").write_text("101")
            expected = tree.process(101, 100)
            self.assertEqual(diagnostics.owned_java_processes(root, tree.path), [expected])

    def test_stale_root_identity_prevents_all_discovery(self):
        with tempfile.TemporaryDirectory() as temp:
            tree = ProcTree(Path(temp))
            root = tree.process(100, 9, children=[101])
            tree.process(101, 100)
            tree.process(100, 9, started=2, children=[101])
            self.assertEqual(diagnostics.owned_java_processes(root, tree.path), [])

    def test_capture_rechecks_owned_identity_without_starting_a_command(self):
        target = diagnostics.ProcessIdentity(101, 100, 100, 100, 1)
        with patch.object(diagnostics, "owned_java_processes", return_value=[]), \
                patch.object(diagnostics.subprocess, "Popen") as popen:
            result = diagnostics.capture_thread_stack(target, None, Path("unused"), threading.Event())
        self.assertEqual(result, "ownership-changed")
        popen.assert_not_called()

    def test_inaccessible_descendant_links_fail_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            tree = ProcTree(Path(temp))
            root = tree.process(100, 9, children=[101])
            tree.process(101, 100)
            (tree.path / "100/task/100/children").unlink()
            self.assertEqual(diagnostics.owned_java_processes(root, tree.path), [])

    def test_target_cap_is_four(self):
        with tempfile.TemporaryDirectory() as temp:
            tree = ProcTree(Path(temp))
            root = tree.process(100, 9, children=range(101, 111))
            for pid in range(101, 111):
                tree.process(pid, 100)
            self.assertEqual(len(diagnostics.owned_java_processes(root, tree.path)), diagnostics.MAX_TARGETS)


class ProgressTests(unittest.TestCase):
    def marker(self, action, phase, suite="selected.Test"):
        return diagnostics.SCENE_PREFIX + json.dumps({"event": action, "suite": suite, "phase": phase})

    def test_elapsed_phase_and_quiet_time_do_not_require_ticks(self):
        progress = diagnostics.SceneProgress(["selected.Test"], 10)
        progress.observe(self.marker("start", "setup"), 12)
        progress.observe(self.marker("phase", "run"), 13)
        progress.observe("an ordinary last log line", 14)
        context = progress.context(100)
        self.assertEqual(context["currentSuite"], "selected.Test")
        self.assertEqual(context["phase"], "run")
        self.assertEqual(context["elapsedSeconds"], 90)
        self.assertEqual(context["sceneElapsedSeconds"], 88)
        self.assertEqual(context["outputQuietSeconds"], 86)
        progress.observe(self.marker("phase", "cleanup"), 101)
        progress.observe(self.marker("end", "threw"), 102)
        context = progress.context(103)
        self.assertIsNone(context["currentSuite"])
        self.assertEqual(context["lastScene"], {"suite": "selected.Test", "outcome": "threw"})

    def test_malformed_unknown_and_out_of_sequence_markers_are_ignored(self):
        progress = diagnostics.SceneProgress(["selected.Test"], 0)
        for line in (diagnostics.SCENE_PREFIX + "[]", diagnostics.SCENE_PREFIX + "not-json",
                     diagnostics.SCENE_PREFIX + '{"suite": []}', self.marker("start", "setup", "unknown.Test"),
                     self.marker("end", "returned"), self.marker("phase", "cleanup")):
            progress.observe(line, 1)
            self.assertIsNone(progress.suite)
            self.assertEqual(progress.phase, "gradle/startup")

    def test_markers_cannot_create_a_gameplay_pass(self):
        selection = client_suites.select_entries(suite="masters")
        log = self.marker("start", "setup", selection["entries"][0]) + "\n"
        log += self.marker("end", "returned", selection["entries"][0]) + "\n"
        manifest = test_client_ci.ManifestTests().manifest(log, "--suite", "masters")
        self.assertEqual(manifest["focusedClientGate"], "unverified")


class WatchdogTests(unittest.TestCase):
    def watch(self, record, *, kind="suite", clock=lambda: 0):
        process = Mock(pid=100)
        process.poll.return_value = None
        with patch.object(diagnostics, "process_identity", return_value=None):
            return diagnostics.NativeDiagnostics(process, {"kind": kind, "entries": ["selected.Test"]}, record, clock=clock)

    def test_existing_job_limits_keep_headroom_and_each_threshold_runs_once(self):
        for kind, expected, timeout in (("suite", (1800, 2700), 3600), ("shard", (5400, 9000), 10800), ("full", (5400, 9000), 10800)):
            messages = []
            watcher = self.watch(messages.append, kind=kind)
            self.assertEqual(watcher.thresholds, expected)
            self.assertLess(max(expected), timeout)
            with patch.object(diagnostics, "owned_java_processes", return_value=[]) as discover:
                watcher.poll(expected[0] - 1)
                discover.assert_not_called()
                watcher.poll(expected[0])
                watcher.poll(expected[0] + 1)
                watcher.poll(expected[1])
                watcher.poll(timeout + 1)
                self.assertEqual(discover.call_count, 2)
            events = [json.loads(line.removeprefix(diagnostics.DIAGNOSTIC_PREFIX)) for line in messages]
            self.assertEqual(len([event for event in events if event["event"] == "snapshot-start"]), 2)

    def test_background_watchdog_runs_with_no_stdout_or_game_ticks(self):
        notified = threading.Event()
        messages = []
        def record(line):
            messages.append(line)
            if '"event": "snapshot-unavailable"' in line:
                notified.set()
        watcher = self.watch(record, clock=lambda: 1801)
        watcher.started = 0
        with patch.object(diagnostics, "owned_java_processes", return_value=[]):
            watcher.start()
            try:
                self.assertTrue(notified.wait(3), "Independent watchdog never reported a silent process")
            finally:
                watcher.close()
        self.assertTrue(any('"event": "heartbeat"' in line for line in messages))

    def test_launcher_preserves_nonzero_exit_despite_diagnostic_failure(self):
        with tempfile.TemporaryDirectory() as temp:
            attempted = threading.Event()
            finished = threading.Event()
            def fail_discovery(root):
                attempted.set()
                raise OSError("unavailable")
            def silent_output():
                self.assertTrue(attempted.wait(3))
                yield "BUILD FAILED\n"
            process = Mock(pid=100, stdout=silent_output())
            def finish():
                finished.set()
                return 7
            process.wait.side_effect = finish
            process.poll.side_effect = lambda: 7 if finished.is_set() else None
            with patch.object(run_client_ci.subprocess, "Popen", return_value=process), \
                    patch.object(diagnostics, "snapshot_thresholds", return_value=(0,)), \
                    patch.object(diagnostics, "owned_java_processes", side_effect=fail_discovery) as discover, \
                    redirect_stdout(io.StringIO()):
                status = run_client_ci.main(["--suite", "masters", "--log", str(Path(temp) / "run.log")])
            discover.assert_called_once()
            self.assertEqual(status, 7)
            log = (Path(temp) / "run.log").read_text()
            self.assertIn('"event": "diagnostic-unavailable"', log)
            self.assertIn(client_suites.EXIT_PREFIX + "7", log)

    def test_unavailable_background_thread_preserves_launcher_result(self):
        with tempfile.TemporaryDirectory() as temp:
            process = Mock(pid=100, stdout=io.StringIO("BUILD SUCCESSFUL\n"))
            process.wait.return_value = 0
            process.poll.return_value = 0
            with patch.object(run_client_ci.subprocess, "Popen", return_value=process), \
                    patch.object(diagnostics.threading.Thread, "start", side_effect=RuntimeError), \
                    redirect_stdout(io.StringIO()):
                result = run_client_ci.main(["--suite", "masters", "--log", str(Path(temp) / "run.log")])
            self.assertEqual(result, 0)
            log = (Path(temp) / "run.log").read_text()
            self.assertIn('"event": "diagnostic-unavailable"', log)
            self.assertIn(client_suites.EXIT_PREFIX + "0", log)

    def test_initialization_observation_and_close_failures_cannot_mask_test_exit(self):
        for stage in ("construct", "start", "observe", "close"):
            with self.subTest(stage=stage), tempfile.TemporaryDirectory() as temp:
                process = Mock(pid=100, stdout=io.StringIO("BUILD FAILED\n"))
                process.wait.return_value = 7
                process.poll.return_value = 7
                watcher = Mock()
                if stage != "construct":
                    getattr(watcher, stage).side_effect = RuntimeError("unavailable")
                with patch.object(run_client_ci.subprocess, "Popen", return_value=process), \
                        patch.object(run_client_ci, "NativeDiagnostics", return_value=watcher,
                                     side_effect=RuntimeError("unavailable") if stage == "construct" else None), \
                        redirect_stdout(io.StringIO()):
                    result = run_client_ci.main(["--suite", "masters", "--log", str(Path(temp) / "run.log")])
                self.assertEqual(result, 7)
                log = (Path(temp) / "run.log").read_text()
                self.assertIn('"event": "diagnostic-unavailable"', log)
                self.assertIn(client_suites.EXIT_PREFIX + "7", log)

    def test_stop_event_suppresses_snapshots(self):
        watcher = self.watch(lambda message: None)
        watcher.stop.set()
        with patch.object(diagnostics, "owned_java_processes") as discover:
            watcher.poll(99999)
            discover.assert_not_called()


class CaptureTests(unittest.TestCase):
    def capture(self, script, **changes):
        # A private fake JDK exercises timeout/output behavior with no JVM or unrelated PID.
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            jdk = root / "jdk/bin"
            jdk.mkdir(parents=True)
            jcmd = jdk / "jcmd"
            jcmd.write_text(f"#!{sys.executable}\n" + script)
            jcmd.chmod(0o700)
            tree = ProcTree(root / "proc")
            tree.path.mkdir()
            leader = tree.process(100, 9, executable="/usr/bin/sh", children=[101])
            target = tree.process(101, 100, executable=str(jdk / "java"))
            output = root / "stacks.txt"
            with patch.multiple(diagnostics, **changes) if changes else nullcontext():
                result = diagnostics.capture_thread_stack(target, leader, output, threading.Event(), tree.path)
            return result, output.read_bytes()

    def test_attach_only_requests_thread_stacks(self):
        result, output = self.capture("import sys\nprint('|'.join(sys.argv[1:]))\n")
        self.assertEqual(result, "complete")
        self.assertEqual(output, b"101|Thread.print|-l\n")

    def test_attach_timeout_is_bounded_without_stopping_target(self):
        started = time.monotonic()
        result, _ = self.capture("import time\ntime.sleep(60)\n", ATTACH_SECONDS=0.15)
        self.assertEqual(result, "attach-timeout")
        self.assertLess(time.monotonic() - started, 3)

    def test_stack_output_is_capped_and_command_is_reaped(self):
        result, output = self.capture("import sys\nsys.stdout.write('x' * 1000000)\nsys.stdout.flush()\n", MAX_STACK_BYTES=2048)
        self.assertEqual(result, "stack-byte-limit")
        self.assertEqual(len(output), 2048)


if __name__ == "__main__":
    unittest.main()
