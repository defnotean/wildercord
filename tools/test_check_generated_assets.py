"""A failed generated-asset check leaves every generated file exactly as it found it."""

from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parent / "check_generated_assets.py"

# Stands in for generate_assets.py: rewrites one file, removes one and adds one.
FAKE_GENERATOR = """from pathlib import Path
root = Path(__file__).resolve().parent.parent
(root / "src/main/resources/kept.json").write_text('{"drifted": true}')
(root / "src/main/resources/removed.json").unlink()
(root / "src/main/resources/assets/added.json").write_text("{}")
"""


class CheckGeneratedAssetsTest(unittest.TestCase):
    def test_a_mismatch_fails_and_puts_every_file_back(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "tools").mkdir()
            shutil.copy(SCRIPT, root / "tools/check_generated_assets.py")
            (root / "tools/generate_assets.py").write_text(FAKE_GENERATOR)
            resources = root / "src/main/resources"
            (resources / "assets").mkdir(parents=True)
            (resources / "kept.json").write_bytes(b'{"drifted": false}
')
            (resources / "removed.json").write_bytes(b"[]")
            (root / "docs").mkdir()
            (root / "docs/RECIPES.md").write_bytes(b"# Recipes
")
            before = {p.relative_to(root).as_posix(): p.read_bytes() for p in root.rglob("*")
                      if p.is_file() and p.relative_to(root).parts[0] != "tools"}

            run = subprocess.run([sys.executable, "-I", str(root / "tools/check_generated_assets.py")],
                                 capture_output=True, text=True, timeout=60)
            self.assertEqual(1, run.returncode, run.stderr)
            for name in ("kept.json", "removed.json", "assets/added.json"):
                self.assertIn("src/main/resources/" + name, run.stderr)

            after = {p.relative_to(root).as_posix(): p.read_bytes() for p in root.rglob("*")
                     if p.is_file() and p.relative_to(root).parts[0] != "tools"}
            self.assertEqual(before, after)


if __name__ == "__main__":
    unittest.main()
