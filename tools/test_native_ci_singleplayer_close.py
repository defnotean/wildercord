"""Compile the actual close gate and check its source-set and exact native call boundaries."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/gametest/java/dev/wildercord/gametest"


class NativeSingleplayerCloseTests(unittest.TestCase):
    def test_handshake_contract(self):
        home = os.environ.get("JAVA_HOME")
        suffix = ".exe" if os.name == "nt" else ""
        java = str(Path(home) / "bin" / ("java" + suffix)) if home else shutil.which("java")
        javac = str(Path(home) / "bin" / ("javac" + suffix)) if home else shutil.which("javac")
        self.assertTrue(java and javac, "The project's Java 25 JDK is required")
        with tempfile.TemporaryDirectory() as temp:
            compile_result = subprocess.run([javac, "--release", "25", "-d", temp,
                str(SOURCE / "NativeHaltHandshake.java"), str(ROOT / "tools/tests/NativeHaltHandshakeTest.java")],
                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
            self.assertEqual(compile_result.returncode, 0, compile_result.stdout)
            result = subprocess.run([java, "-cp", temp, "dev.wildercord.gametest.NativeHaltHandshakeTest"],
                text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout)
            self.assertIn("checks passed", result.stdout)

    def test_only_test_descriptor_enables_the_exact_hook(self):
        name = "native-singleplayer-close-gametest.mixins.json"
        test_descriptor = json.loads((ROOT / "src/gametest/resources/fabric.mod.json").read_text())
        self.assertEqual(test_descriptor["mixins"].count(name), 1)
        for source_set in ("main", "client"):
            for path in (ROOT / "src" / source_set).rglob("*"):
                if path.is_file() and path.suffix in (".java", ".json"):
                    self.assertNotIn("NativeSingleplayerClose", path.read_text(), str(path))
                    self.assertNotIn(name, path.read_text(), str(path))
        hook = (SOURCE / "mixin/NativeIntegratedServerHaltMixin.java").read_text()
        self.assertIn('method = "halt(Z)V"', hook)
        self.assertIn('Lnet/minecraft/client/server/IntegratedServer;executeBlocking(Ljava/lang/Runnable;)V', hook)
        self.assertIn('require = 1, allow = 1', hook)
        self.assertIn('original.call(server, task)', hook)
        bridge = (SOURCE / "mixin/NativeClientPhaseAccess.java").read_text()
        self.assertIn('priority = 900', bridge)
        self.assertIn('@Invoker(value = "postRunTasks", remap = false)', bridge)

    def test_no_timeout_or_forced_phase_and_native_acceptance_is_retained(self):
        gate = (SOURCE / "NativeHaltHandshake.java").read_text()
        adapter = (SOURCE / "NativeSingleplayerClose.java").read_text()
        for forbidden in ("orTimeout", "completeOnTimeout", "forceTermination", "arriveAndDeregister", "enterPhase(", "Thread.sleep", "nanoTime", "complete(null)"):
            self.assertNotIn(forbidden, gate + adapter)
        self.assertIn('try (var scope = GATE.open', adapter)
        self.assertIn('future.join()', gate)
        checks = (SOURCE / "NativeSingleplayerCloseChecks.java").read_text()
        self.assertIn('new boolean[] {false, true}', checks)
        self.assertIn('save.open()', checks)
        self.assertIn('original.isShutdown()', checks)
        relay = (ROOT / "src/gametest/java/dev/wildercord/cast/RelayCircleTest.java").read_text()
        self.assertLess(relay.index('NativeSingleplayerCloseChecks.verify'), relay.index('RelayLessonChecks.run'))
        lesson = (ROOT / "src/gametest/java/dev/wildercord/content/RelayLessonChecks.java").read_text()
        self.assertIn('reconnect(context)', lesson)
        self.assertIn('connection.close()', lesson)


if __name__ == "__main__":
    unittest.main()
