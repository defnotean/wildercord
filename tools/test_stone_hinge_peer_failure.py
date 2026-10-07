"""Run actual pure-JDK failure handling and check its native scope boundaries."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class PeerFailureTests(unittest.TestCase):
    def test_original_identity_cleanup_atomic_witness_and_reentry(self):
        home = os.environ.get("JAVA_HOME")
        suffix = ".exe" if os.name == "nt" else ""
        java = str(Path(home) / "bin" / ("java" + suffix)) if home else shutil.which("java")
        javac = str(Path(home) / "bin" / ("javac" + suffix)) if home else shutil.which("javac")
        self.assertTrue(java and javac, "The project's Java 25 JDK is required")
        with tempfile.TemporaryDirectory() as temp:
            result = subprocess.run([javac, "--release", "25", "-d", temp,
                str(ROOT / "src/gametest/java/dev/wildercord/gametest/stonehinge/peer/StoneHingePeerFailure.java"),
                str(ROOT / "tools/tests/StoneHingePeerFailureTest.java")],
                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout)
            result = subprocess.run([java, "-cp", temp,
                "dev.wildercord.gametest.stonehinge.peer.StoneHingePeerFailureTest", temp],
                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertIn("checks passed", result.stdout)

    def test_early_native_catch_and_original_motion_predicates(self):
        cases = (ROOT / "src/gametest/java/dev/wildercord/aura/StoneHingePeerCases.java").read_text()
        natural = cases.split("private Result naturalMaster(", 1)[1].split("private void logNaturalPhase", 1)[0]
        self.assertLess(natural.index("reportFailure(Case.NATURAL_MASTER, phase, failure)"),
                        natural.index("} finally {"))
        self.assertIn("running.expectMotion(outcome.motion());", natural)
        self.assertIn("context.waitFor(mc -> running.expectedOwnerMotion() != null, 80);", natural)
        self.assertIn("world.waitFor(server -> running.hasOwnerPositionAfterMotion(), 80);", natural)
        owner = (ROOT / "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeOwnerProbe.java").read_text()
        self.assertIn("expectedMotion.equals(packet.movement())", owner)
        self.assertIn("sent != null && sent.expected && after != null", owner)
        host = (ROOT / "src/gametest/java/dev/wildercord/aura/StoneHingePeerTransportTest.java").read_text()
        body = host.split("try (var server = c.worldBuilder().createServer(settings)) {", 1)[1].split("private void peer(", 1)[0]
        self.assertIn("} catch (Throwable failure) {", body)
        self.assertIn("failure(phase, failure, Map.of());\n                throw failure;\n            }\n        }", body)
        self.assertIn('if (exists(role + "-failure.properties")) return;', host)
        self.assertIn('value.putAll(identity)', host)
        self.assertIn('value.setProperty("role", role)', host)
        self.assertIn('value.setProperty("pid", Long.toString(ProcessHandle.current().pid()))', host)


if __name__ == "__main__":
    unittest.main()
