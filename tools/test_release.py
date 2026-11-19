"""The release orchestrator plans the right steps and refuses a bad candidate (no network needed)."""

from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import release  # noqa: E402

SCRIPT = Path(__file__).resolve().parent / "release.py"


def make_repo(root):
    (root / "tools").mkdir(parents=True)
    shutil.copy(SCRIPT, root / "tools/release.py")
    (root / "gradle.properties").write_text("org.gradle.caching=true\nmod_version=0.14.0-alpha\nminecraft_version=26.3\n", encoding="utf-8")
    (root / "docs/releases").mkdir(parents=True)
    (root / "docs/releases/0.15.0-alpha.md").write_text("# 0.15.0-alpha\n", encoding="utf-8")
    for command in (["git", "init", "-b", "main"], ["git", "add", "-A"],
                    ["git", "-c", "user.name=t", "-c", "user.email=t@example.invalid", "commit", "-m", "seed"]):
        subprocess.run(command, check=True, cwd=root, capture_output=True)


def run_release(root, extra):
    env = {"COMPUTERNAME": "TESTVPS", "PATH": shutil.os.environ.get("PATH", "")}
    return subprocess.run([sys.executable, "-I", str(root / "tools/release.py"),
                           "--computer", "TESTVPS", *extra],
                          capture_output=True, text=True, cwd=root, env=env, timeout=120)


class ReleaseTest(unittest.TestCase):
    def test_bump_replaces_only_the_version(self):
        text = "org.gradle.caching=true\nmod_version=0.14.0-alpha\n"
        out = release.bumped(text, "0.15.0-alpha")
        self.assertIn("mod_version=0.15.0-alpha", out)
        self.assertIn("org.gradle.caching=true", out)

    def test_the_same_version_is_refused(self):
        with self.assertRaises(SystemExit):
            release.bumped("mod_version=0.15.0-alpha\n", "0.15.0-alpha")

    def test_a_bad_shape_is_refused(self):
        with self.assertRaises(SystemExit):
            release.bumped("mod_version=0.14.0-alpha\n", "15")

    def test_staging_writes_the_sidecar_the_watchdog_expects(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            jar = root / "wildercord-0.15.0-alpha+mc26.3.jar"
            jar.write_bytes(b"not really a jar")
            staged = release.stage(jar, root / "staging")
            self.assertTrue(staged.exists())
            sidecar = Path(str(staged) + ".sha256")
            import hashlib
            self.assertEqual(hashlib.sha256(b"not really a jar").hexdigest(),
                             sidecar.read_text(encoding="ascii").strip())

    def test_dry_run_plans_every_step_in_order(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            make_repo(root)
            run = run_release(root, ["--version", "0.15.0-alpha", "--dry-run"])
            self.assertEqual(0, run.returncode, run.stdout + run.stderr)
            order = [line for line in run.stdout.splitlines() if line.startswith("[")]
            self.assertEqual(5, len(order), run.stdout)
            self.assertIn("[1] bump to 0.15.0-alpha", order[0])
            self.assertIn("[2] build wildercord-0.15.0-alpha+mc26.3.jar", order[1])
            self.assertIn("[3] release-smoke", order[2])
            self.assertIn("[4] upload", order[3])
            self.assertIn("[5] stage", order[4])
            # Nothing was touched.
            self.assertIn("mod_version=0.14.0-alpha", (root / "gradle.properties").read_text(encoding="utf-8"))
            self.assertEqual("main", subprocess.run(["git", "rev-parse", "--abbrev-ref", "HEAD"],
                                                    cwd=root, capture_output=True, text=True).stdout.strip())

    def test_missing_release_notes_stop_the_run(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            make_repo(root)
            (root / "docs/releases/0.15.0-alpha.md").unlink()
            run = run_release(root, ["--version", "0.15.0-alpha", "--dry-run"])
            self.assertNotEqual(0, run.returncode)
            self.assertIn("release notes", run.stderr)

    def test_the_wrong_computer_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            make_repo(root)
            run = subprocess.run([sys.executable, "-I", str(root / "tools/release.py"),
                                  "--version", "0.15.0-alpha", "--computer", "SOMETHING-ELSE"],
                                 capture_output=True, text=True, cwd=root,
                                 env={"COMPUTERNAME": "TESTVPS", "PATH": shutil.os.environ.get("PATH", "")}, timeout=120)
            self.assertNotEqual(0, run.returncode)
            self.assertIn("releases run on the VPS", run.stderr)


if __name__ == "__main__":
    unittest.main()
