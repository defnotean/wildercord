"""The CurseForge upload refuses a file whose SHA-256 is already in the upload ledger (no network or requests needed)."""

import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parent / "curseforge_upload.py"


class CurseforgeUploadTest(unittest.TestCase):
    def test_a_recorded_file_is_refused_before_the_token_is_read(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "tools").mkdir()
            shutil.copy(SCRIPT, root / "tools/curseforge_upload.py")
            jar = root / "wildercord.jar"
            jar.write_bytes(b"not really a jar")
            (root / "docs/releases").mkdir(parents=True)
            digest = hashlib.sha256(jar.read_bytes()).hexdigest()
            (root / "docs/releases/curseforge-uploads.json").write_text(json.dumps(
                [{"project": 1716203, "fileID": 42, "file": jar.name, "sha256": digest}]), encoding="utf-8")
            env = {k: v for k, v in os.environ.items() if k != "CURSEFORGE_TOKEN"}
            run = subprocess.run([sys.executable, "-I", str(root / "tools/curseforge_upload.py"), str(jar)],
                                 capture_output=True, text=True, env=env, timeout=60)
            self.assertNotEqual(0, run.returncode)
            self.assertIn("already uploaded as file 42", run.stderr)

            # --again gets past the guard and stops at the missing token instead.
            run = subprocess.run([sys.executable, "-I", str(root / "tools/curseforge_upload.py"), str(jar), "--again"],
                                 capture_output=True, text=True, env=env, timeout=60)
            self.assertIn("CURSEFORGE_TOKEN is required", run.stderr)


if __name__ == "__main__":
    unittest.main()
