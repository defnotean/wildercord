"""Unit tests for tools/port_helper.py (offline API mocks + synthetic git/JAR checks)."""
from contextlib import redirect_stderr, redirect_stdout
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))
import port_helper
import verify_port


def find_javac():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        for name in ("javac.exe", "javac"):
            cand = Path(java_home) / "bin" / name
            if cand.is_file():
                return str(cand)
    return shutil.which("javac")


JAVAC = find_javac()
JAVAP = verify_port.find_javap()

SHA1_A = "a" * 40
SHA1_B = "b" * 40
SHA1_C = "c" * 40
SHA1_D = "d" * 40
SHA512_A = "1" * 128
SHA512_B = "2" * 128
SHA512_C = "3" * 128
SHA512_D = "4" * 128


class _FakeHttpResponse:
    def __init__(self, payload):
        self._data = json.dumps(payload).encode("utf-8")

    def read(self):
        return self._data

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False


def _modrinth_version(vid, vnum, vtype, filename, url_pid, mc, sha1, sha512, deps=()):
    return {
        "id": vid,
        "version_number": vnum,
        "version_type": vtype,
        "game_versions": [mc],
        "loaders": ["fabric"],
        "dependencies": list(deps),
        "files": [{
            "primary": True,
            "filename": filename,
            "url": f"https://cdn.modrinth.com/data/{url_pid}/versions/{vid}/{filename}",
            "size": 123456,
            "hashes": {"sha1": sha1, "sha512": sha512},
        }],
    }


class PortHelperOfflineTests(unittest.TestCase):
    def test_sync_deps_solves_iris_required_sodium_and_updates_descriptors(self):
        mc = "1.21.11"
        responses = {
            f"https://meta.fabricmc.net/v2/versions/loader/{mc}": [
                {"loader": {"version": "0.18.4", "stable": False}},
                {"loader": {"version": "0.18.2", "stable": True}},
            ],
            "P7dR8mSH": [
                _modrinth_version("6qAuTtLR", "0.141.6+1.21.11", "release", "fabric-api-0.141.6.jar", "P7dR8mSH", mc, SHA1_A, SHA512_A),
            ],
            "AANobbMI": [
                _modrinth_version("RB7CDjTS", "mc1.21.11-0.8.15-beta.1-fabric", "beta", "sodium-beta.jar", "AANobbMI", mc, SHA1_B, SHA512_B),
                _modrinth_version("rkdTcxoT", "mc1.21.11-0.8.14-fabric", "release", "sodium-0.8.14.jar", "AANobbMI", mc, SHA1_C, SHA512_C),
            ],
            "YL57xq9U": [
                _modrinth_version(
                    "ka9PDlsN", "1.10.8+1.21.11-fabric", "release", "iris-1.10.8.jar", "YL57xq9U", mc, SHA1_D, SHA512_D,
                    deps=[{"project_id": "AANobbMI", "version_id": "rkdTcxoT", "dependency_type": "required"}],
                ),
            ],
        }

        def fake_urlopen(req, timeout=30):
            url = req.full_url if hasattr(req, "full_url") else str(req)
            if url in responses:
                return _FakeHttpResponse(responses[url])
            for key, val in responses.items():
                if f"/project/{key}/version" in url:
                    return _FakeHttpResponse(val)
            raise AssertionError(f"Unexpected URL: {url}")

        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            (repo / "gradle.properties").write_text(
                "# Header comment\nminecraft_version=26.2\nloader_version=0.19.5\njava_version=25\n"
                "fabric_api_version=old\niris_version=old\nsodium_version=old\n",
                encoding="utf-8",
            )
            fmod = repo / "src/main/resources/fabric.mod.json"
            fmod.parent.mkdir(parents=True, exist_ok=True)
            fmod.write_text(json.dumps({
                "schemaVersion": 1, "id": "wildercord", "version": "${version}",
                "depends": {"fabricloader": ">=0.19.5", "minecraft": "~26.2", "java": ">=25", "fabric-api": ">=old"},
            }, indent="\t"), encoding="utf-8")
            m1 = repo / "src/main/resources/wildercord.mixins.json"
            m1.write_text('{\n\t"required": true,\n\t"package": "dev.wildercord.mixin",\n\t"compatibilityLevel": "JAVA_25"\n}\n', encoding="utf-8")
            m2 = repo / "src/gametest/resources/sample.mixins.json"
            m2.parent.mkdir(parents=True, exist_ok=True)
            m2.write_text('{\n  "required": true,\n  "package": "dev.wildercord.gametest.mixin",\n  "compatibilityLevel": "JAVA_25",\n  "injectors": {"defaultRequire": 1}\n}\n', encoding="utf-8")
            (repo / "profiles").mkdir(parents=True, exist_ok=True)
            (repo / "profiles/dependencies.lock.json").write_text("{}", encoding="utf-8")

            with patch("urllib.request.urlopen", side_effect=fake_urlopen):
                with redirect_stdout(io.StringIO()):
                    rc = port_helper.main(["sync-deps", "--root", str(repo), "--mc", mc, "--write"])
            self.assertEqual(rc, 0)
            self.assertEqual(verify_port.verify_descriptor_alignment(repo), [])

            lock = json.loads((repo / "profiles/dependencies.lock.json").read_text(encoding="utf-8"))
            by_proj = {e["project"]: e for e in lock["dependencies"]}
            self.assertEqual(lock["minecraft"], "1.21.11")
            self.assertEqual(lock["fabric_loader"], "0.18.2")
            self.assertEqual(by_proj["sodium"]["version_id"], "rkdTcxoT")
            self.assertEqual(by_proj["iris"]["required_versions"], ["rkdTcxoT"])
            self.assertEqual(verify_port.read_gradle_properties(repo)["java_version"], "21")
            self.assertIn("\n\t", fmod.read_text(encoding="utf-8"))
            self.assertIn('"injectors": {"defaultRequire": 1}', m2.read_text(encoding="utf-8"))
            self.assertNotIn("\r\n", m1.read_bytes().decode("utf-8"))

    def test_diff_surface_enforces_four_surfaces(self):
        if shutil.which("git") is None:
            self.skipTest("git not available")
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            for args in (
                ["init"],
                ["config", "user.name", "Tester"],
                ["config", "user.email", "tester@example.com"],
            ):
                subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True)
            (repo / "gradle.properties").write_text("minecraft_version=26.2\n", encoding="utf-8")
            compat = repo / "src/main/java/dev/wildercord/compat/McEntities.java"
            compat.parent.mkdir(parents=True, exist_ok=True)
            compat.write_text("class McEntities {}\n", encoding="utf-8")
            loot = repo / "src/main/java/dev/wildercord/WildercordLoot.java"
            loot.write_text("class WildercordLoot {}\n", encoding="utf-8")
            subprocess.run(["git", "-C", str(repo), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(repo), "commit", "-m", "base"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(repo), "branch", "base"], check=True, capture_output=True)

            compat.write_text("class McEntities { int ok; }\n", encoding="utf-8")
            (repo / "gradle.properties").write_text("minecraft_version=26.1.2\n", encoding="utf-8")
            subprocess.run(["git", "-C", str(repo), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(repo), "commit", "-m", "seam"], check=True, capture_output=True)
            with redirect_stdout(io.StringIO()):
                self.assertEqual(port_helper.main(["diff-surface", "--root", str(repo), "--base", "base"]), 0)

            loot.write_text("class WildercordLoot { int leaked; }\n", encoding="utf-8")
            subprocess.run(["git", "-C", str(repo), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(repo), "commit", "-m", "leak"], check=True, capture_output=True)
            with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
                self.assertEqual(port_helper.main(["diff-surface", "--root", str(repo), "--base", "base"]), 1)
                self.assertEqual(
                    port_helper.main(["diff-surface", "--root", str(repo), "--base", "base", "--allow-identifier-migration"]),
                    0,
                )


@unittest.skipUnless(JAVAC and JAVAP, "Requires JDK javac and javap")
class PortHelperInspectorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls._tmp = tempfile.TemporaryDirectory()
        tmp = Path(cls._tmp.name)
        src_dir = tmp / "src"
        cls_dir = tmp / "cls"
        cls_dir.mkdir(parents=True, exist_ok=True)
        (src_dir / "net/minecraft/world").mkdir(parents=True, exist_ok=True)
        (src_dir / "net/minecraft/world/Helper.java").write_text(
            "package net.minecraft.world;\n"
            "public class Helper {\n"
            "    public int alphaCount;\n"
            "    public void ping(int x) {}\n"
            "    public void pong() {}\n"
            "}\n",
            encoding="utf-8",
        )
        p = src_dir / "net/minecraft/world/Helper.java"
        proc = subprocess.run([JAVAC, "-g", "-d", str(cls_dir), str(p)], capture_output=True, text=True)
        if proc.returncode != 0:
            raise RuntimeError(proc.stderr)
        cls.jar = tmp / "mc.jar"
        with zipfile.ZipFile(cls.jar, "w") as zf:
            for c in cls_dir.rglob("*.class"):
                zf.write(c, c.relative_to(cls_dir).as_posix())
        cls.repo = tmp / "repo"
        cls.repo.mkdir()
        (cls.repo / "gradle.properties").write_text("minecraft_version=26.2\n", encoding="utf-8")

    @classmethod
    def tearDownClass(cls):
        cls._tmp.cleanup()

    def test_search_and_inspect_with_member_filter(self):
        out = io.StringIO()
        with redirect_stdout(out):
            rc = port_helper.main(["search-class", "--root", str(self.repo), "--mc-jar", str(self.jar), "helper"])
        self.assertEqual(rc, 0)
        self.assertIn("net.minecraft.world.Helper", out.getvalue())

        out_inspect = io.StringIO()
        with redirect_stdout(out_inspect):
            rc = port_helper.main([
                "inspect", "--root", str(self.repo), "--mc-jar", str(self.jar),
                "--member", "ping", "--code", "--supertypes", "Helper",
            ])
        self.assertEqual(rc, 0)
        text = out_inspect.getvalue()
        self.assertIn("Supertypes: net.minecraft.world.Helper", text)
        self.assertIn("ping(int)", text)
        self.assertNotIn("pong()", text)


if __name__ == "__main__":
    unittest.main()
