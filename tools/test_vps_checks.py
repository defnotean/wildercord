"""VPS result rejection and actual Windows-owned process cleanup tests."""
import ctypes
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import run_vps_checks as runner

SUITE = "dev.wildercord.cast.NextCounterTest"
NONCE = "a23e5eea-b323-4d92-8554-0f1e9e93caf8"
DESCRIPTOR = {"entrypoints": {"fabric-client-gametest": [SUITE]}}


def log_fixture():
    lines = []
    for event, phase in (("start", "setup"), ("phase", "run"), ("phase", "cleanup"), ("end", "returned")):
        lines.append("WILDERCORD_NATIVE_SCENE " + json.dumps({"event": event, "phase": phase,
            "suite": SUITE, "launchId": NONCE, "elapsedSeconds": 1, "sceneElapsedSeconds": 1}))
    return "\n".join(lines + ["BUILD SUCCESSFUL in 30s"]) + "\n"


class VpsEvidenceTests(unittest.TestCase):
    def test_only_whole_selected_class_with_owned_nonce_passes(self):
        self.assertEqual(runner.native_evidence(log_fixture(), SUITE, NONCE, DESCRIPTOR),
                         {"completedEntries": [SUITE], "verificationIssues": []})

    def test_rejects_missing_reordered_foreign_duplicated_or_skipped_gameplay(self):
        lines = log_fixture().splitlines()
        samples = ["BUILD SUCCESSFUL\n", "\n".join(lines[:-2] + lines[-1:]),
                   "\n".join([lines[1], lines[0], *lines[2:]]), log_fixture() + lines[0],
                   log_fixture().replace(NONCE, "e23e5eea-b323-4d92-8554-0f1e9e93caf8"),
                   log_fixture().replace(SUITE, SUITE + "Other"),
                   log_fixture() + "[TEST SKIP] filtered case\n",
                   log_fixture() + "BUILD FAILED\n", log_fixture() + "BUILD SUCCESSFUL\n",
                   "\n".join([lines[-1], *lines[:-1]])]
        for log in samples:
            with self.subTest(log=log):
                self.assertTrue(runner.native_evidence(log, SUITE, NONCE, DESCRIPTOR)["verificationIssues"])

    def test_processed_descriptor_must_match_exact_class(self):
        for descriptor in ({}, {"entrypoints": {"fabric-client-gametest": []}},
                           {"entrypoints": {"fabric-client-gametest": [SUITE, SUITE]}}):
            self.assertTrue(runner.native_evidence(log_fixture(), SUITE, NONCE, descriptor)["verificationIssues"])

    def test_exact_clean_source_is_required(self):
        with patch.object(runner, "git", side_effect=["a" * 40, ""]):
            self.assertEqual(runner.source_identity("a" * 40), "a" * 40)
        for observed in (["b" * 40], ["a" * 40, " M build.gradle"]):
            with patch.object(runner, "git", side_effect=observed), self.assertRaises(ValueError):
                runner.source_identity("a" * 40)
        with self.assertRaises(ValueError):
            runner.source_identity("main")

    def test_units_require_real_reports_and_zero_failures_errors_or_skips(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaises(ValueError):
                runner.unit_results(root)
            report = root / "TEST-fixture.xml"
            report.write_text('<testsuite tests="7" failures="0" errors="0" skipped="0"/>')
            self.assertEqual(runner.unit_results(root), {"tests": 7, "failures": 0, "errors": 0, "skipped": 0})
            for field in ("failures", "errors", "skipped"):
                report.write_text('<testsuite tests="7" failures="0" errors="0" skipped="0"/>'.replace(field + '="0"', field + '="1"'))
                with self.assertRaises(ValueError):
                    runner.unit_results(root)


@unittest.skipUnless(os.name == "nt", "Actual Windows job ownership requires Windows")
class WindowsOwnershipTests(unittest.TestCase):
    def check_cleanup(self, interrupted):
        from ctypes import wintypes as w
        kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        kernel.OpenProcess.argtypes = [w.DWORD, w.BOOL, w.DWORD]
        kernel.OpenProcess.restype = w.HANDLE
        kernel.WaitForSingleObject.argtypes = [w.HANDLE, w.DWORD]
        kernel.WaitForSingleObject.restype = w.DWORD
        kernel.CloseHandle.argtypes = [w.HANDLE]
        kernel.CloseHandle.restype = w.BOOL
        # An unrelated sibling remains alive while the job owner's descendants die.
        sibling = subprocess.Popen([sys.executable, "-c", "import time; time.sleep(30)"],
                                   creationflags=subprocess.CREATE_NO_WINDOW)
        code = "\n".join([
            "import sys, subprocess", "sys.path.insert(0, 'tools')",
            "from run_vps_checks import own_windows_job", "own_windows_job()",
            "child = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'], creationflags=subprocess.CREATE_NO_WINDOW)",
            "print(child.pid, flush=True)", "sys.stdin.read(1)"])
        owner = subprocess.Popen([sys.executable, "-c", code], cwd=runner.ROOT, stdin=subprocess.PIPE,
                                 stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
                                 creationflags=subprocess.CREATE_NO_WINDOW)
        handle = None
        try:
            line = owner.stdout.readline().strip()
            self.assertTrue(line.isdecimal(), owner.stderr.read() if owner.poll() is not None else line)
            handle = kernel.OpenProcess(0x100000, False, int(line))  # SYNCHRONIZE
            self.assertTrue(handle, ctypes.get_last_error())
            self.assertEqual(kernel.WaitForSingleObject(handle, 0), 258)  # WAIT_TIMEOUT: alive
            if interrupted:
                owner.terminate()
            else:
                owner.stdin.write("x"); owner.stdin.flush()
            owner.wait(timeout=10)
            if not interrupted:
                self.assertEqual(owner.returncode, 0, owner.stderr.read())
            self.assertEqual(kernel.WaitForSingleObject(handle, 5000), 0, "Owned child survived its runner")
            self.assertIsNone(sibling.poll(), "Unrelated sibling must remain running")
        finally:
            if handle:
                kernel.CloseHandle(handle)
            for process in (owner, sibling):
                if process.poll() is None:
                    process.terminate()
                process.wait(timeout=5)
            for stream in (owner.stdin, owner.stdout, owner.stderr):
                stream.close()

    def test_normal_exit_reaps_only_owned_descendants(self):
        self.check_cleanup(False)

    def test_interrupted_runner_reaps_only_owned_descendants(self):
        self.check_cleanup(True)


if __name__ == "__main__":
    unittest.main()
