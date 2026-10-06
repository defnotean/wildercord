"""Compile and exercise the real pure-JDK gametest watchdog, without launching Minecraft."""
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class NativeJvmTests(unittest.TestCase):
    def test_self_jvm_watchdog(self):
        java_home = os.environ.get("JAVA_HOME")
        suffix = ".exe" if os.name == "nt" else ""
        javac = str(Path(java_home) / "bin" / ("javac" + suffix)) if java_home else shutil.which("javac")
        java = str(Path(java_home) / "bin" / ("java" + suffix)) if java_home else shutil.which("java")
        self.assertTrue(javac and java, "The project's Java 25 JDK is required")
        with tempfile.TemporaryDirectory() as temp:
            result = subprocess.run([javac, "--release", "25", "-d", temp,
                                     str(ROOT / "src/gametest/java/dev/wildercord/gametest/NativeJvmDiagnostics.java"),
                                     str(ROOT / "tools/tests/NativeJvmDiagnosticsTest.java")],
                                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout)
            result = subprocess.run([java, "-cp", temp, "dev.wildercord.gametest.NativeJvmDiagnosticsTest",
                                     str(Path(temp) / "evidence")],
                                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertIn("checks passed", result.stdout)

    def test_activation_job_ids_and_thresholds_match_current_workflow(self):
        workflow = (ROOT / ".github/workflows/build.yml").read_text()
        for job, limit in (("masters-native", 90), ("articulated-native", 60), ("game-tests", 180), ("native-diagnostic", 60)):
            # Job fields have four spaces, so inspect until the next two-space job key.
            section = workflow.split(f"\n  {job}:\n", 1)[1]
            section = re.split(r"\n  [a-z][a-z-]*:\n", section, maxsplit=1)[0]
            self.assertIn(f"timeout-minutes: {limit}", section)
        from native_ci_diagnostics import snapshot_thresholds
        self.assertEqual(snapshot_thresholds({"kind": "suite", "name": "masters"}), (1800, 2700, 4500))
        self.assertEqual(snapshot_thresholds({"kind": "suite"}), (1800, 2700))
        self.assertEqual(snapshot_thresholds({"kind": "shard"}), (5400, 9000))


if __name__ == "__main__":
    unittest.main()
