"""The committed icon is exactly what tools/make_icon.py draws (no hand edits)."""

from pathlib import Path
import hashlib
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "tools/make_icon.py"
ICON = ROOT / "src/main/resources/assets/wildercord/icon.png"


class MakeIconTest(unittest.TestCase):
    def test_the_committed_icon_is_the_scripts_own_output(self):
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.run([sys.executable, str(SCRIPT), "--store", tmp], check=True, cwd=ROOT,
                           capture_output=True)
            fresh = Path(tmp) / "wildercord-icon-640.png"
            self.assertTrue(fresh.exists())
            self.assertEqual(hashlib.sha256(ICON.read_bytes()).hexdigest(),
                             hashlib.sha256(fresh.read_bytes()).hexdigest(),
                             "icon.png was not produced by tools/make_icon.py; run it and commit")


if __name__ == "__main__":
    unittest.main()
