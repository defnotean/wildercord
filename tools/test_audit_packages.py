"""Adversarial archive tests; fixtures are synthetic, not gameplay evidence."""
import contextlib
import copy
import io
import json
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import warnings
import zipfile

import audit_packages as audit
import build_profiles


class PackageFixture(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.props = {"mod_version": "1.2.3-dev", "minecraft_version": "26.3", "loader_version": "0.19.5",
                      "java_version": "25", "loom_version": "1.18.2", "fabric_api_version": "0.161.0+26.3", "sodium_version": "mc26.3-0.9.2-fabric",
                      "iris_version": "1.11.6+26.3-fabric"}
        self.version = "1.2.3-dev+mc26.3"
        self.mod = {"schemaVersion": 1, "id": "wildercord", "version": "${version}", "name": "Wildercord",
                    "license": "MIT", "environment": "*", "icon": "assets/wildercord/icon.png",
                    "depends": {"fabricloader": ">=0.19.5", "minecraft": "~26.3", "java": ">=25", "fabric-api": ">=0.161.0+26.3"},
                    "entrypoints": {"main": ["dev.wildercord.Main"], "client": ["dev.wildercord.client.Client"]},
                    "mixins": ["wildercord.mixins.json", {"config": "wildercord.client.mixins.json", "environment": "client"}]}
        self.write("gradle.properties", "\n".join(f"{key}={value}" for key, value in self.props.items()))
        self.write("LICENSE", "MIT fixture notice\n")
        self.write_json("src/main/resources/fabric.mod.json", self.mod)
        self.write("src/main/resources/assets/wildercord/icon.png", b"fixture icon")
        self.write("src/main/resources/data/wildercord/recipe/fixture.json", b'{"type":"fixture"}')
        for source, package, key in (("main", "dev.wildercord.mixin", "mixins"), ("client", "dev.wildercord.client.mixin", "client")):
            name = "wildercord" + (".client" if source == "client" else "") + ".mixins.json"
            self.write_json(f"src/{source}/resources/{name}", {"required": True, "package": package, "compatibilityLevel": "JAVA_25", key: ["FixtureMixin"]})
            if source == "client":
                self.write("build/resources/client/" + name, (self.root / f"src/client/resources/{name}").read_bytes())
        for source, names in (("main", ("dev/wildercord/Main.class", "dev/wildercord/mixin/FixtureMixin.class")),
                              ("client", ("dev/wildercord/client/Client.class", "dev/wildercord/client/mixin/FixtureMixin.class"))):
            for name in names:
                self.write("build/classes/java/" + source + "/" + name, b"\xca\xfe\xba\xbe\x00\x00\x00\x45fixture class")
        self.lock = {"minecraft": "26.3", "fabric_loader": "0.19.5", "dependencies": []}
        for project, property_name in (("fabric-api", "fabric_api_version"), ("sodium", "sodium_version"), ("iris", "iris_version")):
            self.lock["dependencies"].append({"project": project, "version": self.props[property_name], "version_id": project + "-id",
                "filename": project + ".jar", "url": "https://cdn.modrinth.com/data/fixture/versions/" + project + "/" + project + ".jar",
                "size": 123, "hashes": {"sha1": "a" * 40, "sha512": "b" * 128}, "game_versions": ["26.3"],
                "loaders": ["fabric"], "required_versions": ["sodium-id"] if project == "iris" else []})
        self.write_json("profiles/dependencies.lock.json", self.lock)
        self.write("profiles/README.md", "Fixture profile documentation\n")
        for profile in audit.PROFILES:
            self.write_json("profiles/" + profile + "/config/wildercord-visuals.json", {"profile": profile})
        self.jar = self.root / "build/libs" / ("wildercord-" + self.version + ".jar")
        self.jar.parent.mkdir(parents=True)
        entries = {"LICENSE": (self.root / "LICENSE").read_bytes()}
        for relative in ("src/main/resources", "src/client/resources", "build/classes/java/main", "build/classes/java/client"):
            base = self.root / relative
            entries.update({path.relative_to(base).as_posix(): path.read_bytes() for path in base.rglob("*") if path.is_file()})
        entries["fabric.mod.json"] = json.dumps({**self.mod, "version": self.version}).encode()
        self.manifest = {"Manifest-Version": "1.0", "Fabric-Mapping-Namespace": "official",
            "Fabric-Loom-Split-Environment": "true", "Fabric-Minecraft-Version": "26.3", "Fabric-Loader-Version": "0.19.5",
            "Fabric-Loom-Version": "1.18.2", "Fabric-Gradle-Version": "9.7.1", "Fabric-Mixin-Version": "variable-tool-version",
            "Fabric-Loom-Client-Only-Entries": ";".join(name for name in entries if name.startswith("dev/wildercord/client/")
                                                      or name == "wildercord.client.mixins.json")}
        entries["META-INF/MANIFEST.MF"] = self.manifest_bytes()
        self.zip_write(self.jar, entries)
        self.profiles = self.root / "artifacts/profiles"
        with patch.object(build_profiles, "ROOT", self.root), contextlib.redirect_stdout(io.StringIO()):
            for profile in audit.PROFILES:
                build_profiles.build(profile, self.jar, self.profiles)

    def write(self, name, data):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data.encode() if isinstance(data, str) else data)
        return path

    def write_json(self, name, value):
        return self.write(name, json.dumps(value))

    def zip_write(self, path, entries):
        with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for name, data in entries.items():
                archive.writestr(name, data)

    def manifest_bytes(self):
        lines = []
        for key, value in self.manifest.items():
            remaining = f"{key}: {value}".encode()
            lines.append(remaining[:70])
            remaining = remaining[70:]
            while remaining:
                lines.append(b" " + remaining[:69])
                remaining = remaining[69:]
        return b"\r\n".join(lines) + b"\r\n\r\n"

    def rewrite(self, path, updates=None, remove=()):
        with zipfile.ZipFile(path) as archive:
            entries = {name: archive.read(name) for name in archive.namelist() if name not in remove}
        entries.update(updates or {})
        self.zip_write(path, entries)

    def assert_rejected(self, pattern=None):
        with self.assertRaises((audit.AuditError, zipfile.BadZipFile)) as caught:
            audit.audit(self.root)
        if pattern:
            self.assertIn(pattern, str(caught.exception))

    def profile_path(self, profile="cinematic"):
        return self.profiles / f"wildercord-{profile}-{self.version}.mrpack"

    def alter_index(self, change):
        path = self.profile_path()
        with zipfile.ZipFile(path) as archive:
            index = json.loads(archive.read("modrinth.index.json"))
        change(index)
        self.rewrite(path, {"modrinth.index.json": json.dumps(index).encode()})

    def test_current_builder_profiles_and_all_source_sets_pass(self):
        result, jar = audit.audit(self.root)
        self.assertEqual(jar, self.jar)
        self.assertEqual(result["jar"]["main_classes"], 2)
        self.assertEqual(result["jar"]["client_classes"], 2)
        self.assertEqual(result["jar"]["client_resources"], 1)
        self.assertEqual(result["jar"]["mixins"], 2)
        self.assertEqual([len(profile["pinned_dependencies"]) for profile in result["profiles"]], [2, 2, 3])
        self.assertTrue(all(profile["embedded_mod_sha256"] == result["jar"]["sha256"] for profile in result["profiles"]))

    def test_resources_not_just_manifest_are_compared(self):
        self.rewrite(self.jar, {"data/wildercord/recipe/fixture.json": b'{"type":"changed"}'})
        self.assert_rejected("Packaged bytes differ")

    def test_missing_client_resource_fails(self):
        self.rewrite(self.jar, remove=("wildercord.client.mixins.json",))
        self.assert_rejected("Unexpected/missing")

    def test_missing_or_changed_license_fails(self):
        self.rewrite(self.jar, {"LICENSE": b"different notice"})
        self.assert_rejected("LICENSE")

    def test_metadata_dependency_or_version_tampering_fails(self):
        mod = {**self.mod, "version": self.version, "depends": {"minecraft": "*"}}
        self.rewrite(self.jar, {"fabric.mod.json": json.dumps(mod).encode()})
        self.assert_rejected("metadata differs")

    def test_duplicate_json_keys_fail(self):
        self.rewrite(self.jar, {"fabric.mod.json": b'{"id":"wildercord","id":"other"}'})
        self.assert_rejected("Duplicate JSON key")

    def test_missing_entrypoint_even_if_compiled_inventory_agrees_fails(self):
        name = "dev/wildercord/Main.class"
        (self.root / "build/classes/java/main" / name).unlink()
        self.rewrite(self.jar, remove=(name,))
        self.assert_rejected("entrypoint class")

    def test_missing_mixin_even_if_compiled_inventory_agrees_fails(self):
        name = "dev/wildercord/client/mixin/FixtureMixin.class"
        (self.root / "build/classes/java/client" / name).unlink()
        self.manifest["Fabric-Loom-Client-Only-Entries"] = ";".join(
            entry for entry in self.manifest["Fabric-Loom-Client-Only-Entries"].split(";") if entry != name)
        self.rewrite(self.jar, {"META-INF/MANIFEST.MF": self.manifest_bytes()}, remove=(name,))
        self.assert_rejected("mixin class")

    def test_changed_compiled_bytes_fail(self):
        self.rewrite(self.jar, {"dev/wildercord/Main.class": b"bad class"})
        self.assert_rejected("Packaged bytes differ")

    def test_preview_or_wrong_java_target_fails(self):
        data = b"\xca\xfe\xba\xbe\xff\xff\x00\x45fixture class"
        name = "dev/wildercord/Main.class"
        self.write("build/classes/java/main/" + name, data)
        self.rewrite(self.jar, {name: data})
        self.assert_rejected("Java target")

    def test_foreign_runtime_nested_jar_and_private_files_fail(self):
        for name in ("net/minecraft/Main.class", "net/fabricmc/Loader.class", "org/thirdparty/Lib.class", "META-INF/jars/extra.jar",
                     "dev/wildercord/gametest/Fixture.class", ".env", "logs/latest.log", ".aws/credentials", "extra.txt"):
            with self.subTest(name=name):
                self.rewrite(self.jar, {name: b"unexpected fixture"})
                self.assert_rejected()
                self.rewrite(self.jar, remove=(name,))

    def test_empty_private_directory_fails(self):
        self.rewrite(self.jar, {".git/": b""})
        self.assert_rejected()

    def test_runtime_manifest_agent_fails(self):
        self.manifest["Premain-Class"] = "dev.wildercord.Main"
        self.rewrite(self.jar, {"META-INF/MANIFEST.MF": self.manifest_bytes()})
        self.assert_rejected("launcher/agent")

    def test_manifest_mapping_environment_and_version_pins_are_checked(self):
        for key in ("Fabric-Mapping-Namespace", "Fabric-Loom-Split-Environment", "Fabric-Minecraft-Version",
                    "Fabric-Loader-Version", "Fabric-Loom-Version"):
            original = self.manifest[key]
            with self.subTest(key=key):
                self.manifest[key] = "wrong"
                self.rewrite(self.jar, {"META-INF/MANIFEST.MF": self.manifest_bytes()})
                self.assert_rejected(key.lower())
                del self.manifest[key]
                self.rewrite(self.jar, {"META-INF/MANIFEST.MF": self.manifest_bytes()})
                self.assert_rejected(key.lower())
                self.manifest[key] = original

    def test_manifest_client_inventory_rejects_bogus_missing_extra_duplicate(self):
        entries = self.manifest["Fabric-Loom-Client-Only-Entries"].split(";")
        for value in ("bogus.class", ";".join(entries[:-1]), ";".join(entries + ["dev/wildercord/Main.class"]),
                      ";".join(entries + entries[:1])):
            with self.subTest(value=value):
                self.manifest["Fabric-Loom-Client-Only-Entries"] = value
                self.rewrite(self.jar, {"META-INF/MANIFEST.MF": self.manifest_bytes()})
                self.assert_rejected("client-only entries")

    def test_manifest_continuations_and_variable_metadata_pass(self):
        self.manifest["Fabric-Gradle-Version"] = "variable"
        self.manifest["Fabric-Mixin-Compile-Extensions-Version"] = "also-variable"
        self.manifest["Fabric-Loom-Client-Only-Entries"] = ";".join(reversed(
            self.manifest["Fabric-Loom-Client-Only-Entries"].split(";")))
        data = self.manifest_bytes()
        self.assertIn(b"\r\n ", data)
        self.rewrite(self.jar, {"META-INF/MANIFEST.MF": data})
        result = audit.check_jar(self.root, self.jar, self.props)
        self.assertEqual(result["manifest_client_entries"], 3)
        self.assertEqual(result["manifest_pins"]["fabric-mapping-namespace"], "official")

    def test_manifest_duplicate_case_insensitive_attribute_fails(self):
        self.manifest["fabric-loom-split-environment"] = "false"
        self.rewrite(self.jar, {"META-INF/MANIFEST.MF": self.manifest_bytes()})
        self.assert_rejected("Duplicate manifest attribute")

    def test_processed_client_resources_are_reconciled(self):
        self.write("build/resources/client/wildercord.client.mixins.json", b"wrong")
        self.assert_rejected("Processed client resources")

    def test_missing_and_ambiguous_distributable_fail(self):
        other = self.jar.with_name("wildercord-dev.jar")
        other.write_bytes(self.jar.read_bytes())
        self.assert_rejected("exactly one")
        other.unlink()
        self.jar.unlink()
        self.assert_rejected("exactly one")

    def test_sources_jar_is_never_selected(self):
        self.jar.with_name(self.jar.stem + "-sources.jar").write_bytes(b"source fixture")
        result, selected = audit.audit(self.root)
        self.assertEqual(selected, self.jar)
        self.assertTrue(result["jar"]["within_32_mib_raw_file_limit"])

    def test_profile_missing_or_extra_fails(self):
        self.profile_path().unlink()
        self.assert_rejected("launcher profiles")

    def test_embedded_mod_bytes_must_match_selected_jar(self):
        self.rewrite(self.profile_path(), {"overrides/mods/" + self.jar.name: b"wrong jar"})
        self.assert_rejected("Profile bytes differ")

    def test_profile_configs_and_options_are_exact(self):
        self.rewrite(self.profile_path(), {"client-overrides/options.txt": b"renderDistance:32\n"})
        self.assert_rejected("Profile bytes differ")

    def test_profile_cannot_bundle_a_dependency(self):
        self.rewrite(self.profile_path(), {"overrides/mods/iris.jar": b"dependency"})
        self.assert_rejected("Unexpected/missing")

    def test_profile_metadata_version_is_checked(self):
        self.alter_index(lambda index: index.update(versionId="stale-version"))
        self.assert_rejected("Profile metadata")

    def test_every_pin_field_is_reconciled(self):
        original = self.profile_path().read_bytes()
        for key, value in (("hashes", {"sha1": "0" * 40, "sha512": "0" * 128}), ("fileSize", 999),
                           ("downloads", ["https://untrusted.invalid/iris.jar"]), ("env", {"client": "optional"}),
                           ("path", "mods/other.jar")):
            with self.subTest(key=key):
                self.alter_index(lambda index: index["files"][0].update({key: value}))
                self.assert_rejected("entries differ")
                self.profile_path().write_bytes(original)

    def test_duplicate_pin_entries_fail(self):
        self.alter_index(lambda index: index["files"].__setitem__(1, copy.deepcopy(index["files"][0])))
        self.assert_rejected("entries differ")

    def test_lock_runtime_and_gradle_must_agree(self):
        self.lock["minecraft"] = "old"
        self.write_json("profiles/dependencies.lock.json", self.lock)
        self.assert_rejected("runtime pins")

    def test_lock_dependency_closure_is_checked(self):
        self.lock["dependencies"][-1]["required_versions"] = ["missing-id"]
        self.write_json("profiles/dependencies.lock.json", self.lock)
        self.assert_rejected("Incompatible dependency")

    def test_success_report_and_staged_jar_are_exact(self):
        report = self.root / "report.json"
        destination = self.root / "dist"
        with patch.object(audit, "provenance", return_value={"git_commit": "fixture"}), contextlib.redirect_stdout(io.StringIO()):
            code = audit.main(["--root", str(self.root), "--output", str(report), "--distributable-directory", str(destination)])
        result = json.loads(report.read_text())
        self.assertEqual(code, 0)
        self.assertEqual(result["status"], "passed")
        self.assertEqual(result["scope"], "packaging_only")
        self.assertEqual(result["gameplay_readiness"], "not_assessed")
        self.assertEqual((destination / self.jar.name).read_bytes(), self.jar.read_bytes())
        self.assertLess(report.stat().st_size, 10000)

    def test_failure_report_replaces_stale_pass_and_does_not_stage(self):
        report = self.write_json("report.json", {"status": "passed"})
        destination = self.root / "dist"
        self.rewrite(self.jar, {"unexpected.txt": b"fixture"})
        with patch.object(audit, "provenance", return_value={}), contextlib.redirect_stdout(io.StringIO()):
            code = audit.main(["--root", str(self.root), "--output", str(report), "--distributable-directory", str(destination)])
        self.assertEqual(code, 1)
        self.assertEqual(json.loads(report.read_text())["status"], "failed")
        self.assertFalse(destination.exists())


class ArchiveBoundaryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "input.zip"

    def make(self, names=("a.txt",), data=b"fixture", compression=zipfile.ZIP_DEFLATED):
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(self.path, "w", compression=compression) as archive:
                for name in names:
                    archive.writestr(name, data)

    def check(self):
        with audit.checked_archive(self.path) as archive:
            return audit.archive_digests(archive)

    def test_valid_directory_and_file(self):
        with zipfile.ZipFile(self.path, "w") as archive:
            archive.writestr("parent/", b"")
            archive.writestr("parent/a.txt", b"fixture")
        self.assertEqual(set(self.check()), {"parent/a.txt"})

    def test_traversal_and_noncanonical_names_rejected(self):
        for name in ("../a", "/a", "C:/a", "a\\b", "a//b", "a/./b", "a/../b", "a. ", "NUL.txt", "a\x01b"):
            with self.subTest(name=name):
                self.make((name,))
                with self.assertRaises(audit.AuditError):
                    self.check()

    def test_duplicate_case_collision_and_file_parent_rejected(self):
        for names in (("a", "a"), ("a", "A"), ("a", "a/b"), ("a", "a/")):
            with self.subTest(names=names):
                self.make(names)
                with self.assertRaises(audit.AuditError):
                    self.check()

    def test_null_filename_in_central_directory_rejected(self):
        self.make(("aXb",))
        data = self.path.read_bytes().replace(b"aXb", b"a\x00b")
        self.path.write_bytes(data)
        with self.assertRaisesRegex(audit.AuditError, "Truncated"):
            self.check()

    def test_symlinks_rejected(self):
        with zipfile.ZipFile(self.path, "w") as archive:
            info = zipfile.ZipInfo("link")
            info.create_system = 3
            info.external_attr = 0o120777 << 16
            archive.writestr(info, b"target")
        with self.assertRaisesRegex(audit.AuditError, "Non-regular"):
            self.check()

    def test_bad_crc_rejected(self):
        self.make(data=b"fixture", compression=zipfile.ZIP_STORED)
        self.path.write_bytes(self.path.read_bytes().replace(b"fixture", b"corrupt"))
        with self.assertRaises(zipfile.BadZipFile):
            self.check()

    def test_expansion_and_per_entry_bounds_rejected(self):
        self.make(data=b"A" * 2 * audit.MIB)
        with self.assertRaisesRegex(audit.AuditError, "ratio"):
            self.check()
        self.make()
        with patch.object(audit, "MAX_MEMBER", 3), self.assertRaisesRegex(audit.AuditError, "member"):
            self.check()
        with patch.object(audit, "MAX_EXPANDED", 3), self.assertRaisesRegex(audit.AuditError, "expanded"):
            self.check()

    def test_directory_bounds_apply_before_zipfile_allocation(self):
        self.make()
        for name, limit in (("MAX_ENTRIES", 0), ("MAX_CENTRAL_DIRECTORY", 1), ("MAX_ARCHIVE", 10)):
            with self.subTest(name=name), patch.object(audit, name, limit), patch.object(audit.zipfile, "ZipFile") as opened:
                with self.assertRaises(audit.AuditError):
                    self.check()
                opened.assert_not_called()

    def test_forged_directory_entry_count_fails_before_allocation(self):
        self.make(("a", "b"))
        data = bytearray(self.path.read_bytes())
        struct.pack_into("<HH", data, len(data) - 22 + 8, 1, 1)
        self.path.write_bytes(data)
        with patch.object(audit.zipfile, "ZipFile") as opened:
            with self.assertRaisesRegex(audit.AuditError, "count mismatch"):
                self.check()
            opened.assert_not_called()

    def test_trailing_truncated_and_split_archives_rejected(self):
        self.make()
        original = self.path.read_bytes()
        split = bytearray(original)
        struct.pack_into("<H", split, len(split) - 22 + 4, 1)
        for data in (original + b"extra", original[:-3], bytes(split)):
            with self.subTest(size=len(data)):
                self.path.write_bytes(data)
                with self.assertRaises(audit.AuditError):
                    self.check()

    def test_encryption_and_unsupported_compression_rejected(self):
        self.make()
        data = bytearray(self.path.read_bytes())
        cd = data.index(b"PK\x01\x02")
        struct.pack_into("<H", data, cd + 8, 1)
        self.path.write_bytes(data)
        with self.assertRaisesRegex(audit.AuditError, "Encrypted"):
            self.check()
        self.make(compression=zipfile.ZIP_BZIP2)
        with self.assertRaisesRegex(audit.AuditError, "compression|extraction version"):
            self.check()

    def test_small_force_zip64_member_is_rejected(self):
        with zipfile.ZipFile(self.path, "w") as archive:
            with archive.open("fixture.txt", "w", force_zip64=True) as member:
                member.write(b"fixture")
        with self.assertRaisesRegex(audit.AuditError, "ZIP64"):
            self.check()
        # A forged ordinary central header must not hide local ZIP64 usage.
        data = bytearray(self.path.read_bytes())
        cd = data.index(b"PK\x01\x02")
        struct.pack_into("<H", data, cd + 6, 20)
        self.path.write_bytes(data)
        with self.assertRaisesRegex(audit.AuditError, "ZIP64.*local"):
            self.check()

    def test_zip64_local_size_sentinel_and_extra_field_are_rejected(self):
        self.make()
        data = bytearray(self.path.read_bytes())
        struct.pack_into("<I", data, 18, 0xffffffff)
        self.path.write_bytes(data)
        with self.assertRaisesRegex(audit.AuditError, "ZIP64 local sizes"):
            self.check()
        # ZIP64 extras are invalid here even without a large size or version45.
        info = zipfile.ZipInfo("fixture.txt")
        info.extra = struct.pack("<HHQQ", 1, 16, 7, 7)
        with zipfile.ZipFile(self.path, "w") as archive:
            archive.writestr(info, b"fixture")
        with self.assertRaisesRegex(audit.AuditError, "ZIP64 extra"):
            self.check()
        data = bytearray(self.path.read_bytes())
        cd = data.index(b"PK\x01\x02")
        name_size = struct.unpack_from("<H", data, cd + 28)[0]
        # Retain only the local ZIP64 extra by renaming its central tag.
        struct.pack_into("<H", data, cd + 46 + name_size, 0xcafe)
        self.path.write_bytes(data)
        with self.assertRaisesRegex(audit.AuditError, "ZIP64 extra"):
            self.check()


if __name__ == "__main__":
    unittest.main()
