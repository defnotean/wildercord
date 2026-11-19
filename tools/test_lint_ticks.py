"""The tick-sentinel lint flags a raw Long.MIN_VALUE that is subtracted without a guard (no network needed)."""

from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parent / "lint_ticks.py"


def run(root):
    return subprocess.run([sys.executable, "-I", str(SCRIPT), str(root)],
                          capture_output=True, text=True, timeout=60)


class LintTicksTest(unittest.TestCase):
    def java(self, root, body):
        src = root / "src/main/java/dev/wildercord"
        src.mkdir(parents=True)
        (src / "Sample.java").write_text(
            "package dev.wildercord;\npublic class Sample {\n" + body + "\n}\n", encoding="utf-8")

    def test_a_bare_sentinel_subtracted_is_flagged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.java(root, "long hitTick = Long.MIN_VALUE;\nboolean idle(long now) { return now - hitTick >= 200; }")
            run_ = run(root)
            self.assertEqual(1, run_.returncode, run_.stdout + run_.stderr)
            self.assertIn("hitTick", run_.stdout)

    def test_an_equality_guard_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.java(root, "long hitTick = Long.MIN_VALUE;\nboolean idle(long now) { return hitTick == Long.MIN_VALUE || now - hitTick >= 200; }")
            run_ = run(root)
            self.assertEqual(0, run_.returncode, run_.stdout + run_.stderr)

    def test_a_halved_sentinel_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.java(root, "long hitTick = Long.MIN_VALUE / 2;\nboolean idle(long now) { return now - hitTick >= 200; }")
            self.assertEqual(0, run(root).returncode)

    def test_ticks_since_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.java(root, "long hitTick = Long.MIN_VALUE;\nboolean idle(long now) { return Ticks.since(hitTick, now) >= 200; }")
            self.assertEqual(0, run(root).returncode)

    def test_prose_hyphenation_is_not_a_subtraction(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.java(root, "// the per-tick voice limit\nlong tick = Long.MIN_VALUE;\nvoid reset(long now) { tick = now; }")
            self.assertEqual(0, run(root).returncode)

    def test_ticks_ok_exempts_a_line(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.java(root, "long hitTick = Long.MIN_VALUE; // ticks-ok\nboolean idle(long now) { return now - hitTick >= 200; }")
            self.assertEqual(0, run(root).returncode)

    def test_a_missing_directory_is_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            run_ = run(Path(tmp) / "nope")
            self.assertEqual(2, run_.returncode)
            self.assertIn("not a directory", run_.stderr)


if __name__ == "__main__":
    unittest.main()
