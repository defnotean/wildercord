"""Execute the committed CI setup body with isolated fake package commands only."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[1]
PACKAGES = ["xvfb", "libgl1-mesa-dri", "libegl1", "mesa-utils"]
FAKE = r'''
import json, os, sys
from pathlib import Path
name = Path(sys.argv[0]).name
args = sys.argv[1:]
with open(os.environ["NATIVE_SETUP_TRACE"], "a") as stream:
    stream.write(json.dumps([name, args]) + "\n")
mode = os.environ.get("NATIVE_SETUP_MODE", "success")
if name == "sudo":
    assert args.pop(0) == "-n"
    os.execvp(args[0], args)
elif name == "timeout":
    assert args.pop(0) == "--kill-after=15s"
    duration = args.pop(0)
    action = "update" if args[-1] == "update" else "install"
    assert duration == ("300s" if action == "update" else "180s")
    if mode == action + "-timeout":
        sys.exit(124)
    assert args[0] == "apt-get"
    os.execvp(args[0], args)
elif name == "apt-get":
    action = "update" if args[-1] == "update" else "install"
    expected = ["-o", "Acquire::Retries=2", "-o", "Acquire::http::Timeout=30",
                "-o", "Acquire::https::Timeout=30"]
    expected += (["--error-on=any", "update"] if action == "update" else
                 ["install", "-y", "xvfb", "libgl1-mesa-dri", "libegl1", "mesa-utils"])
    assert args == expected, args
    if mode == action + "-failure":
        sys.exit(100)
elif name == "dpkg-query":
    assert args[0] == "--show" and len(args) == 3
    package = args[-1]
    assert package in ["xvfb", "libgl1-mesa-dri", "libegl1", "mesa-utils"]
    if mode == "query-failure":
        sys.exit(1)
    if "Status-Status" in args[1]:
        print("unpacked" if mode == "missing-" + package else "installed")
    else:
        assert args[1] == "--showformat=${Version}"
        if mode != "empty-version":
            print("24.04-fixture")
else:
    raise AssertionError("The setup must only locate, never launch, display executables")
'''


def display_steps():
    workflow = (ROOT / ".github/workflows/build.yml").read_text()
    jobs = re.split(r"(?=^  [a-z][a-z-]*:\s*$)", workflow, flags=re.MULTILINE)
    found = {}
    for job in jobs:
        match = re.match(r"  ([a-z][a-z-]*):\n", job)
        if not match:
            continue
        for step in re.split(r"(?=^      - )", job, flags=re.MULTILINE)[1:]:
            if re.match(r"      - name: Install (?:virtual display|existing virtual-display dependencies)", step):
                if match[1] in found:
                    raise AssertionError("Duplicate display install step")
                found[match[1]] = step.rstrip()
    return found


class NativeDisplaySetupTests(unittest.TestCase):
    def script(self):
        steps = display_steps()
        self.assertEqual(set(steps), {"game-tests", "masters-native", "connected-combat-native",
                                     "articulated-native", "native-diagnostic"})
        scripts = []
        for job, step in steps.items():
            self.assertIn("\n        timeout-minutes: 10\n", step)
            self.assertNotIn("continue-on-error", step)
            if job == "native-diagnostic":
                self.assertIn("\n        if: steps.request.outputs.enabled == 'true'\n", step)
            else:
                self.assertNotIn("\n        if:", step)
            body = step.split("\n        run: |\n", 1)[1]
            scripts.append(textwrap.dedent(body))
        self.assertTrue(all(script == scripts[0] for script in scripts))
        self.assertTrue(scripts[0].startswith("set -euo pipefail\n"))
        return scripts[0]

    def run_setup(self, mode="success", absent=None):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            trace = root / "calls.jsonl"
            for name in ["sudo", "timeout", "apt-get", "dpkg-query", "Xvfb", "glxinfo"]:
                if name == absent:
                    continue
                path = root / name
                path.write_text("#!" + sys.executable + "\n" + FAKE)
                path.chmod(0o700)
            result = subprocess.run(["/bin/bash", "-c", self.script()],
                                    env={**os.environ, "PATH": str(root),
                                         "NATIVE_SETUP_TRACE": str(trace), "NATIVE_SETUP_MODE": mode},
                                    text=True, capture_output=True, timeout=10)
            calls = [json.loads(line) for line in trace.read_text().splitlines()] if trace.exists() else []
            return result, calls

    def test_all_native_jobs_install_and_verify_exact_packages(self):
        result, calls = self.run_setup()
        self.assertEqual(result.returncode, 0, result.stderr)
        apt = [args for name, args in calls if name == "apt-get"]
        self.assertEqual(len(apt), 2)
        self.assertEqual(apt[0][-1], "update")
        self.assertEqual(apt[1][-4:], PACKAGES)
        verified = [args[-1] for name, args in calls if name == "dpkg-query" and "Status-Status" in args[1]]
        self.assertEqual(verified, PACKAGES)
        for package in PACKAGES:
            self.assertIn(package + "=24.04-fixture", result.stdout)
        self.assertFalse(any(name in ("Xvfb", "glxinfo") for name, _ in calls))

    def test_package_failures_and_timeouts_never_continue_as_success(self):
        for mode, code in [("update-failure", 100), ("install-failure", 100),
                           ("update-timeout", 124), ("install-timeout", 124)]:
            with self.subTest(mode=mode):
                result, calls = self.run_setup(mode)
                self.assertEqual(result.returncode, code, result.stderr)
                self.assertFalse(any(name == "dpkg-query" for name, _ in calls))
                if mode.startswith("update"):
                    self.assertFalse(any(name == "apt-get" and "install" in args for name, args in calls))

    def test_missing_or_unverifiable_package_refuses_setup(self):
        for mode in [*("missing-" + package for package in PACKAGES), "query-failure", "empty-version"]:
            with self.subTest(mode=mode):
                result, _ = self.run_setup(mode)
                self.assertNotEqual(result.returncode, 0)

    def test_missing_display_executable_refuses_setup(self):
        for executable in ["Xvfb", "glxinfo"]:
            with self.subTest(executable=executable):
                result, _ = self.run_setup(absent=executable)
                self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()
