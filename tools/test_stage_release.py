"""Unit tests for tools/stage_release.py (manifest validation, dirty gate check, and synthetic bundle staging)."""
from __future__ import annotations

import contextlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

TOOLS_DIR = Path(__file__).resolve().parent
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

import audit_packages
import stage_release as release


class StageReleaseToolTest(unittest.TestCase):
    def test_repo_gradle_properties_validates_semver(self):
        props = release.parse_properties(TOOLS_DIR.parent)
        self.assertRegex(props["mod_version"], release.VERSION_RE)
        self.assertTrue(props["minecraft_version"])

    def test_dirty_tree_rejected_without_allow_dirty(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "gradle.properties").write_text("mod_version=0.17.0-alpha\nminecraft_version=26.3\n", encoding="utf-8")
            with patch.object(release, "_git_output", return_value=" M gradle.properties"):
                with self.assertRaises(release.ReleaseError):
                    release.run_local_gates(root, allow_dirty=False)

    def test_stage_release_bundle_synthetic_fixture(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            props = {
                "mod_version": "0.17.0-alpha",
                "minecraft_version": "26.3",
                "loader_version": "0.19.5",
                "java_version": "25",
                "loom_version": "1.18.2",
                "fabric_api_version": "0.161.0+26.3",
                "sodium_version": "mc26.3-0.9.2-fabric",
                "iris_version": "1.11.6+26.3-fabric",
            }
            (root / "gradle.properties").write_text("\n".join(f"{k}={v}" for k, v in props.items()), encoding="utf-8")
            (root / "LICENSE").write_text("MIT\n", encoding="utf-8")
            mod_meta = {
                "schemaVersion": 1,
                "id": "wildercord",
                "version": "${version}",
                "name": "Wildercord",
                "license": "MIT",
                "environment": "*",
                "icon": "assets/wildercord/icon.png",
                "depends": {"fabricloader": ">=0.19.5", "minecraft": "~26.3", "java": ">=25", "fabric-api": ">=0.161.0+26.3"},
                "entrypoints": {"main": ["dev.wildercord.Main"], "client": ["dev.wildercord.client.Client"]},
                "mixins": ["wildercord.mixins.json", {"config": "wildercord.client.mixins.json", "environment": "client"}],
            }
            for rel, payload in (
                ("src/main/resources/fabric.mod.json", json.dumps(mod_meta).encode()),
                ("src/main/resources/assets/wildercord/icon.png", b"icon"),
                ("src/main/resources/data/wildercord/recipe/fixture.json", b'{"type":"fixture"}'),
                ("src/main/resources/wildercord.mixins.json", b'{"required":true,"package":"dev.wildercord.mixin","compatibilityLevel":"JAVA_25","mixins":["FixtureMixin"]}'),
                ("src/client/resources/wildercord.client.mixins.json", b'{"required":true,"package":"dev.wildercord.client.mixin","compatibilityLevel":"JAVA_25","client":["FixtureMixin"]}'),
                ("build/resources/client/wildercord.client.mixins.json", b'{"required":true,"package":"dev.wildercord.client.mixin","compatibilityLevel":"JAVA_25","client":["FixtureMixin"]}'),
                ("build/classes/java/main/dev/wildercord/Main.class", b"\xca\xfe\xba\xbe\x00\x00\x00\x45"),
                ("build/classes/java/main/dev/wildercord/mixin/FixtureMixin.class", b"\xca\xfe\xba\xbe\x00\x00\x00\x45"),
                ("build/classes/java/client/dev/wildercord/client/Client.class", b"\xca\xfe\xba\xbe\x00\x00\x00\x45"),
                ("build/classes/java/client/dev/wildercord/client/mixin/FixtureMixin.class", b"\xca\xfe\xba\xbe\x00\x00\x00\x45"),
                ("profiles/README.md", b"Profiles\n"),
            ):
                p = root / rel
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_bytes(payload)

            lock = {"minecraft": "26.3", "fabric_loader": "0.19.5", "dependencies": []}
            for project, prop_key in (("fabric-api", "fabric_api_version"), ("sodium", "sodium_version"), ("iris", "iris_version")):
                lock["dependencies"].append({
                    "project": project,
                    "version": props[prop_key],
                    "version_id": project + "-id",
                    "filename": project + ".jar",
                    "url": "https://cdn.modrinth.com/data/fixture/versions/" + project + "/" + project + ".jar",
                    "size": 123,
                    "hashes": {"sha1": "a" * 40, "sha512": "b" * 128},
                    "game_versions": ["26.3"],
                    "loaders": ["fabric"],
                    "required_versions": ["sodium-id"] if project == "iris" else [],
                })
            (root / "profiles" / "dependencies.lock.json").write_text(json.dumps(lock), encoding="utf-8")
            for profile in audit_packages.PROFILES:
                cfg = root / "profiles" / profile / "config" / "wildercord-visuals.json"
                cfg.parent.mkdir(parents=True, exist_ok=True)
                cfg.write_text(json.dumps({"profile": profile}), encoding="utf-8")

            version = "0.17.0-alpha+mc26.3"
            jar = root / "build" / "libs" / f"wildercord-{version}.jar"
            jar.parent.mkdir(parents=True, exist_ok=True)
            entries = {"LICENSE": (root / "LICENSE").read_bytes()}
            for rel in ("src/main/resources", "src/client/resources", "build/classes/java/main", "build/classes/java/client"):
                base = root / rel
                for path in base.rglob("*"):
                    if path.is_file():
                        entries[path.relative_to(base).as_posix()] = path.read_bytes()
            entries["fabric.mod.json"] = json.dumps({**mod_meta, "version": version}).encode()
            manifest_lines = [
                "Manifest-Version: 1.0",
                "Fabric-Mapping-Namespace: official",
                "Fabric-Loom-Split-Environment: true",
                "Fabric-Minecraft-Version: 26.3",
                "Fabric-Loader-Version: 0.19.5",
                "Fabric-Loom-Version: 1.18.2",
                "Fabric-Gradle-Version: 9.7.1",
                "Fabric-Mixin-Version: fixture",
                "Fabric-Loom-Client-Only-Entries: " + ";".join(
                    n for n in sorted(entries) if n.startswith("dev/wildercord/client/") or n == "wildercord.client.mixins.json"
                ),
            ]
            entries["META-INF/MANIFEST.MF"] = ("\r\n".join(manifest_lines) + "\r\n\r\n").encode()
            with zipfile.ZipFile(jar, "w", compression=zipfile.ZIP_DEFLATED) as zf:
                for k, v in sorted(entries.items()):
                    zf.writestr(k, v)

            out = root / "staged"
            with patch.object(release, "_git_output", return_value="deadbeef" * 5), \
                 patch.object(audit_packages, "provenance", return_value={"git_commit": "deadbeef" * 5, "git_tree": "0" * 40, "worktree_dirty": False, "github": {}}), \
                 contextlib.redirect_stdout(io.StringIO()):
                bundle = release.stage_release_bundle(root, out, jar=jar)

            self.assertEqual(bundle["version"], "0.17.0-alpha")
            self.assertTrue(bundle["ready_for_manual_publish"])
            self.assertFalse(bundle["published"])
            self.assertTrue((out / "SHA256SUMS.txt").is_file())
            self.assertTrue((out / "release-manifest.json").is_file())
            self.assertEqual(len(bundle["profiles"]), 3)


if __name__ == "__main__":
    unittest.main()
