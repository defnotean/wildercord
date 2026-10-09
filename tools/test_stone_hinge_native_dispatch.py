"""Execute native-dispatch observer contract tests; this is not a Minecraft gameplay test."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class NativeDispatchTests(unittest.TestCase):
    def test_native_sequence_evidence_and_latched_negative_cases(self):
        home = os.environ.get("JAVA_HOME")
        suffix = ".exe" if os.name == "nt" else ""
        java = str(Path(home) / "bin" / ("java" + suffix)) if home else shutil.which("java")
        javac = str(Path(home) / "bin" / ("javac" + suffix)) if home else shutil.which("javac")
        self.assertTrue(java and javac, "The project's Java 25 JDK is required")
        with tempfile.TemporaryDirectory() as temp:
            compile_result = subprocess.run(
                [javac, "--release", "25", "-d", temp,
                 str(ROOT / "src/gametest/java/dev/wildercord/gametest/stonehinge/peer/StoneHingeNativeDispatch.java"),
                 str(ROOT / "tools/tests/StoneHingeNativeDispatchTest.java"),
                 str(ROOT / "tools/tests/StoneHingeOwnerReceiptTest.java")],
                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
            self.assertEqual(compile_result.returncode, 0, compile_result.stdout)
            for test in ("StoneHingeNativeDispatchTest", "StoneHingeOwnerReceiptTest"):
                result = subprocess.run(
                    [java, "-cp", temp, "dev.wildercord.gametest.stonehinge.peer." + test],
                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)
                self.assertEqual(result.returncode, 0, result.stdout)
                self.assertRegex(result.stdout, r"\d+ checks passed; \d+ rejected scenarios")
                self.assertIn("pure-JDK sequencing only; no Minecraft gameplay claim", result.stdout)


if __name__ == "__main__":
    unittest.main()
