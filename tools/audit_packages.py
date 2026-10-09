"""Audit built mod/profile bytes, never gameplay readiness (Python stdlib only).

Run after ./gradlew build and tools/build_profiles.py. Archives are never extracted.
Limits are intentionally fixed: ZIP64, encrypted, exotic, or oversized inputs fail
closed. A passing report attests to this checkout and its compiled outputs, not to
launcher import, game behavior, the safety of downloaded dependencies, or a release.
"""
import argparse
from contextlib import contextmanager
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import struct
import subprocess
import unicodedata
import zipfile
import zlib

ROOT = Path(__file__).resolve().parents[1]
PROFILES = ("performance", "balanced", "cinematic")
MIB = 1024 * 1024
MAX_ARCHIVE = 256 * MIB
MAX_CENTRAL_DIRECTORY = 16 * MIB
MAX_ENTRIES = 32768
MAX_MEMBER = 128 * MIB
MAX_EXPANDED = 512 * MIB
MAX_JSON = MIB
DOWNLOAD_LIMIT = 32 * MIB
BLOCK = 64 * 1024


class AuditError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise AuditError(message)


def digest_stream(stream, limit):
    digest = hashlib.sha256()
    size = 0
    while chunk := stream.read(BLOCK):
        size += len(chunk)
        require(size <= limit, "Expanded input exceeds byte limit")
        digest.update(chunk)
    return {"size_bytes": size, "sha256": digest.hexdigest()}


def digest_file(path, limit=MAX_MEMBER):
    require(path.is_file() and not path.is_symlink(), f"Missing or non-regular file: {path.name}")
    require(path.stat().st_size <= limit, f"File exceeds byte limit: {path.name}")
    with path.open("rb") as stream:
        return digest_stream(stream, limit)


def read_file(path, limit=MAX_JSON):
    digest_file(path, limit)
    with path.open("rb") as stream:
        data = stream.read(limit + 1)
    require(len(data) <= limit, "Input exceeds byte limit")
    return data


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"Duplicate JSON key: {key}")
        result[key] = value
    return result


def parse_json(data):
    require(len(data) <= MAX_JSON, "JSON exceeds byte limit")
    value = json.loads(data.decode("utf-8-sig"), object_pairs_hook=unique_object,
                       parse_constant=lambda value: (_ for _ in ()).throw(AuditError("Non-finite JSON number")))
    require(isinstance(value, dict), "Expected a JSON object")
    return value


def safe_name(name):
    require(isinstance(name, str) and 0 < len(name.encode("utf-8")) <= 512, "Invalid archive path length")
    require(unicodedata.normalize("NFC", name) == name, f"Noncanonical archive path: {name!r}")
    require(not name.startswith("/") and "\\" not in name and ":" not in name,
            f"Unsafe archive path: {name!r}")
    require(not any(ord(char) < 32 or ord(char) == 127 for char in name), "Control character in archive path")
    parts = name.rstrip("/").split("/")
    require(all(part not in ("", ".", "..") and not part.endswith((" ", ".")) for part in parts),
            f"Noncanonical archive path: {name!r}")
    require(not any(re.fullmatch(r"(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\..*)?", part) for part in parts),
            f"Reserved archive path: {name!r}")
    return "/".join(parts).casefold()


def reject_development_file(name):
    parts = name.casefold().split("/")
    forbidden = {".git", ".github", ".gradle", ".idea", ".vscode", "__pycache__", "gametest",
                 "test", "tests", "logs", "crash-reports", ".aws", ".ssh", "node_modules"}
    require(not forbidden.intersection(parts), f"Development/private file in package: {name}")
    require(not any(part.startswith(".env") or part in {"credentials", "credentials.json", "secrets.json"}
                    for part in parts), f"Private file in package: {name}")
    require(not name.casefold().endswith((".java", ".py", ".pyc", ".log", ".hprof", ".jfr", ".pem", ".key")),
            f"Development/private file in package: {name}")


def check_zip_extra(data):
    cursor = 0
    while cursor < len(data):
        require(cursor + 4 <= len(data), "Malformed ZIP extra field")
        kind, size = struct.unpack_from("<HH", data, cursor)
        require(kind != 1, "ZIP64 extra fields are unsupported")
        cursor += 4 + size
        require(cursor <= len(data), "Malformed ZIP extra field")


def zip_preflight(path):
    """Bound the directory before ZipFile allocates one object per entry."""
    require(path.is_file() and not path.is_symlink(), f"Missing or non-regular archive: {path.name}")
    size = path.stat().st_size
    require(22 <= size <= MAX_ARCHIVE, f"Archive size outside limits: {path.name}")
    with path.open("rb") as stream:
        stream.seek(max(0, size - 65557))
        tail = stream.read(65557)
    position = tail.rfind(b"PK\x05\x06")
    require(position >= 0 and len(tail) - position >= 22, "Missing ZIP end record")
    _, disk, cd_disk, disk_entries, entries, cd_size, cd_offset, comment = struct.unpack_from("<4s4H2IH", tail, position)
    require(position + 22 + comment == len(tail), "Trailing or malformed ZIP data")
    require(disk == cd_disk == 0 and disk_entries == entries, "Split ZIP archives are unsupported")
    require(0 < entries <= MAX_ENTRIES and cd_size <= MAX_CENTRAL_DIRECTORY, "ZIP directory exceeds limits")
    require(cd_offset + cd_size == size - len(tail) + position, "ZIP64, prefixed or malformed ZIP directory")
    with path.open("rb") as stream:
        stream.seek(cd_offset)
        directory = stream.read(cd_size)
    cursor = count = 0
    while cursor < len(directory):
        require(cursor + 46 <= len(directory) and directory[cursor:cursor + 4] == b"PK\x01\x02",
                "Malformed ZIP central directory")
        name_size, extra_size, comment_size = struct.unpack_from("<3H", directory, cursor + 28)
        require(struct.unpack_from("<H", directory, cursor + 6)[0] <= 20,
                "ZIP64 or unsupported ZIP extraction version")
        require(0xffffffff not in struct.unpack_from("<II", directory, cursor + 20)
                and struct.unpack_from("<I", directory, cursor + 42)[0] != 0xffffffff,
                "ZIP64 member sizes/offsets are unsupported")
        require(struct.unpack_from("<H", directory, cursor + 34)[0] == 0, "Split/ZIP64 member disk")
        extra_start = cursor + 46 + name_size
        require(extra_start + extra_size <= len(directory), "Truncated ZIP extra field")
        check_zip_extra(directory[extra_start:extra_start + extra_size])
        cursor += 46 + name_size + extra_size + comment_size
        count += 1
        require(count <= MAX_ENTRIES and cursor <= len(directory), "ZIP directory exceeds limits")
    require(count == entries and cursor == cd_size, "ZIP directory entry count mismatch")
    return entries, cd_offset


def check_local_headers(path, infos, cd_offset):
    # Small members can opt into ZIP64 using only local headers; EOCD and
    # central-directory limits alone do not exclude that representation.
    with path.open("rb") as stream:
        for info in infos:
            stream.seek(info.header_offset)
            header = stream.read(30)
            require(len(header) == 30 and header[:4] == b"PK\x03\x04", "Malformed ZIP local header")
            require(struct.unpack_from("<H", header, 4)[0] <= 20,
                    "ZIP64 or unsupported ZIP local extraction version")
            require(0xffffffff not in struct.unpack_from("<II", header, 18), "ZIP64 local sizes are unsupported")
            name_size, extra_size = struct.unpack_from("<HH", header, 26)
            require(info.header_offset + 30 + name_size + extra_size <= cd_offset, "Local header overlaps ZIP directory")
            stream.seek(name_size, 1)
            extra = stream.read(extra_size)
            require(len(extra) == extra_size, "Truncated ZIP local extra field")
            check_zip_extra(extra)


@contextmanager
def checked_archive(path):
    entries, cd_offset = zip_preflight(path)
    with zipfile.ZipFile(path) as archive:
        infos = archive.infolist()
        require(len(infos) == entries, "ZIP entry count mismatch")
        paths, files, offsets = set(), set(), set()
        expanded = 0
        for info in infos:
            require(info.orig_filename == info.filename, "Truncated ZIP filename")
            key = safe_name(info.filename)
            require(key not in paths, f"Duplicate/colliding ZIP path: {info.filename}")
            paths.add(key)
            if not info.is_dir():
                files.add(key)
            mode = stat.S_IFMT(info.external_attr >> 16)
            require(mode in (0, stat.S_IFREG, stat.S_IFDIR), f"Non-regular ZIP entry: {info.filename}")
            require(mode != stat.S_IFDIR or info.is_dir(), "Directory mode/name mismatch")
            require(not info.flag_bits & 1, "Encrypted ZIP entry")
            require(info.compress_type in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED), "Unsupported ZIP compression")
            require(0 <= info.file_size <= MAX_MEMBER and info.compress_size <= MAX_ARCHIVE, "ZIP member exceeds limits")
            require(info.file_size <= max(1, info.compress_size) * 1000, "ZIP expansion ratio exceeds limit")
            require(not info.is_dir() or info.file_size == 0, "Nonempty ZIP directory")
            require(0 <= info.header_offset < cd_offset and info.header_offset not in offsets, "Invalid ZIP member offset")
            offsets.add(info.header_offset)
            expanded += info.file_size
        require(min(offsets) == 0, "Prefixed ZIP archives are unsupported")
        require(expanded <= MAX_EXPANDED, "ZIP expanded size exceeds limit")
        for key in paths:
            parts = key.split("/")
            require(not any("/".join(parts[:i]) in files for i in range(1, len(parts))), "ZIP file/directory collision")
        check_local_headers(path, infos, cd_offset)
        yield archive


def archive_digests(archive):
    result = {}
    for info in archive.infolist():
        # Reading every entry also checks local headers, overlap, decompression, and CRC.
        with archive.open(info) as stream:
            digest = digest_stream(stream, min(info.file_size, MAX_MEMBER))
        require(digest["size_bytes"] == info.file_size, "ZIP member size mismatch")
        if not info.is_dir():
            result[info.filename] = digest
    return result


def archive_json(archive, name):
    require(archive.getinfo(name).file_size <= MAX_JSON, f"JSON exceeds byte limit: {name}")
    return parse_json(archive.read(name))


def check_members(archive, actual, expected):
    require(set(actual) == set(expected),
            "Unexpected/missing archive members: " + repr(sorted(set(actual) ^ set(expected))[:10]))
    parents = {name[:index] for name in expected for index, char in enumerate(name) if char == "/"}
    for info in archive.infolist():
        reject_development_file(info.filename)
        require(not info.is_dir() or info.filename.rstrip("/") in parents,
                f"Unexpected archive directory: {info.filename}")


def properties(root):
    result = {}
    for line in read_file(root / "gradle.properties").decode("utf-8").splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            require(key.strip() not in result, "Duplicate Gradle property")
            result[key.strip()] = value.strip()
    return result


def inventory(directory):
    require(directory.is_dir() and not directory.is_symlink(), f"Missing build/source directory: {directory}")
    result = {}
    total = 0
    for path in sorted(directory.rglob("*")):
        require(not path.is_symlink(), f"Symlink in expected inputs: {path.name}")
        if path.is_file():
            name = path.relative_to(directory).as_posix()
            safe_name(name)
            reject_development_file(name)
            result[name] = digest_file(path)
            total += result[name]["size_bytes"]
            require(len(result) <= MAX_ENTRIES and total <= MAX_EXPANDED, "Expected input inventory exceeds limits")
    require(bool(result), f"Empty build/source directory: {directory}")
    return result


def bytes_digest(data):
    return {"size_bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}


def manifest_attributes(data):
    """Parse case-insensitive main attributes after byte-level line unfolding."""
    require(len(data) <= MAX_JSON, "Manifest exceeds byte limit")
    unfolded = []
    for line in data.replace(b"\r\n", b"\n").split(b"\n"):
        require(b"\r" not in line and b"\x00" not in line, "Invalid manifest control character")
        if line.startswith(b" "):
            require(bool(unfolded) and bool(unfolded[-1]), "Orphan manifest continuation")
            unfolded[-1].append(line[1:])
        else:
            unfolded.append([line] if line else [])
    sections = [{}]
    for fragments in unfolded:
        line = b"".join(fragments)
        if not line:
            if sections[-1]:
                sections.append({})
            continue
        key, separator, value = line.partition(b": ")
        require(bool(separator) and re.fullmatch(rb"[A-Za-z0-9_-]{1,70}", key), "Malformed manifest attribute")
        key = key.decode("ascii").lower()
        require(key not in sections[-1], f"Duplicate manifest attribute: {key}")
        sections[-1][key] = value.decode("utf-8")
    return sections[0]


def check_manifest(data, props, client_entries):
    # Loom v1.18.2 ManifestModificationAction/ClientEntriesService define these
    # for this non-remapped, split-source build. Other tool-version fields vary.
    attributes = manifest_attributes(data)
    pinned = {"manifest-version": "1.0", "fabric-mapping-namespace": "official",
              "fabric-loom-split-environment": "true", "fabric-minecraft-version": props["minecraft_version"],
              "fabric-loader-version": props["loader_version"], "fabric-loom-version": props["loom_version"]}
    for key, value in pinned.items():
        require(attributes.get(key) == value, f"JAR manifest disagrees with pinned build: {key}")
    require(not {"class-path", "main-class", "premain-class", "agent-class", "launcher-agent-class"} & attributes.keys(),
            "Unexpected runtime launcher/agent in JAR manifest")
    declared = attributes.get("fabric-loom-client-only-entries", "").split(";")
    require(len(declared) == len(set(declared)) and set(declared) == client_entries,
            "JAR manifest client-only entries differ from client build outputs")
    return pinned


def check_jar(root, jar, props):
    version = props["mod_version"] + "+mc" + props["minecraft_version"]
    require(jar.name == "wildercord-" + version + ".jar", "Unexpected distributable JAR filename")
    source_mod = parse_json(read_file(root / "src/main/resources/fabric.mod.json"))
    expected_mod = {**source_mod, "version": version}
    require(source_mod["version"] == "${version}", "Unsupported source version template")
    require(expected_mod["schemaVersion"] == 1 and expected_mod["id"] == "wildercord"
            and expected_mod["environment"] == "*" and expected_mod["license"] == "MIT", "Unexpected mod identity/license")
    require(expected_mod["depends"] == {"fabricloader": ">=" + props["loader_version"],
            "minecraft": "~" + props["minecraft_version"], "java": ">=" + props["java_version"],
            "fabric-api": ">=" + props["fabric_api_version"]}, "Descriptor dependencies disagree with Gradle properties")
    require(not expected_mod.get("jars"), "Bundled dependency JARs are not allowed")
    expected = {}
    counts = {}
    client_entries = set()
    for kind, relative in (("main_resources", "src/main/resources"), ("client_resources", "src/client/resources"),
                           ("main_classes", "build/classes/java/main"), ("client_classes", "build/classes/java/client")):
        entries = inventory(root / relative)
        require(not expected.keys() & entries.keys(), "Duplicate main/client build inputs")
        if kind.endswith("classes"):
            require(all(name.startswith("dev/wildercord/") and name.endswith(".class") for name in entries),
                    "Unexpected compiled output; only mod runtime classes may be packaged")
        expected.update(entries)
        counts[kind] = len(entries)
        if kind.startswith("client_"):
            client_entries.update(entries)
        if kind == "client_resources":
            require(inventory(root / "build/resources/client") == entries,
                    "Processed client resources differ from source inputs")
    expected.pop("fabric.mod.json")  # Expanded JSON is compared structurally below.
    expected["LICENSE"] = digest_file(root / "LICENSE", MAX_JSON)
    with checked_archive(jar) as archive:
        actual = archive_digests(archive)
        check_members(archive, actual, set(expected) | {"fabric.mod.json", "META-INF/MANIFEST.MF"})
        require(archive_json(archive, "fabric.mod.json") == expected_mod, "Packaged mod metadata differs from source/version")
        require(archive.getinfo("META-INF/MANIFEST.MF").file_size <= MAX_JSON, "Manifest exceeds byte limit")
        manifest = check_manifest(archive.read("META-INF/MANIFEST.MF"), props, client_entries)
        for name, digest in expected.items():
            require(actual[name] == digest, f"Packaged bytes differ from expected input: {name}")
            if name.endswith(".class"):
                with archive.open(name) as stream:
                    header = stream.read(8)
                require(len(header) == 8 and header[:4] == b"\xca\xfe\xba\xbe"
                        and struct.unpack(">HH", header[4:]) == (0, int(props["java_version"]) + 44),
                        f"Invalid/preview/wrong Java target class: {name}")
        require(expected_mod["icon"] in actual, "Missing mod icon")
        require(set(expected_mod["entrypoints"]) == {"main", "client"}, "Unexpected entrypoint environments")
        entrypoint_count = 0
        for values in expected_mod["entrypoints"].values():
            require(isinstance(values, list) and bool(values), "Missing entrypoints")
            for value in values:
                require(isinstance(value, str) and value.replace(".", "/") + ".class" in actual, "Missing entrypoint class")
                entrypoint_count += 1
        mixin_count = 0
        for declaration in expected_mod["mixins"]:
            name = declaration if isinstance(declaration, str) else declaration["config"]
            config = archive_json(archive, name)
            require(config["compatibilityLevel"] == "JAVA_" + props["java_version"], "Mixin Java target mismatch")
            for environment in ("mixins", "client", "server"):
                for mixin in config.get(environment, []):
                    path = (config["package"] + "." + mixin).replace(".", "/") + ".class"
                    require(path in actual, f"Missing mixin class: {path}")
                    mixin_count += 1
        require(mixin_count > 0, "No runtime mixins checked")
    return {"file": jar.name, **digest_file(jar, MAX_ARCHIVE), "version": version, **counts,
            "entrypoints": entrypoint_count, "mixins": mixin_count,
            "dependencies": expected_mod["depends"], "license_sha256": expected["LICENSE"]["sha256"],
            "manifest_pins": manifest, "manifest_client_entries": len(client_entries),
            "input_inventory_sha256": hashlib.sha256(json.dumps(expected, sort_keys=True).encode()).hexdigest()}


def check_profiles(root, directory, jar, jar_result, props):
    lock_path = root / "profiles/dependencies.lock.json"
    lock = parse_json(read_file(lock_path))
    require(lock["minecraft"] == props["minecraft_version"] and lock["fabric_loader"] == props["loader_version"],
            "Profile runtime pins disagree with Gradle properties")
    pins = {entry["project"]: entry for entry in lock["dependencies"]}
    require(len(pins) == len(lock["dependencies"]) and set(pins) == {"fabric-api", "sodium", "iris"}, "Unexpected/duplicate dependency pins")
    for project, property_name in (("fabric-api", "fabric_api_version"), ("sodium", "sodium_version"), ("iris", "iris_version")):
        require(pins[project]["version"] == props[property_name], f"Dependency pin disagrees with Gradle: {project}")
    expected_names = {f"wildercord-{profile}-{jar_result['version']}.mrpack" for profile in PROFILES}
    require(directory.is_dir() and {path.name for path in directory.glob("*.mrpack")} == expected_names,
            "Missing or unexpected launcher profiles")
    reports = []
    for profile in PROFILES:
        path = directory / f"wildercord-{profile}-{jar_result['version']}.mrpack"
        selected = {"fabric-api", "sodium"} | ({"iris"} if profile == "cinematic" else set())
        selected_ids = {pins[project]["version_id"] for project in selected}
        expected_files = {}
        for project in sorted(selected):
            pin = pins[project]
            safe_name(pin["filename"])
            require("/" not in pin["filename"] and pin["filename"].endswith(".jar"), "Unsafe dependency filename")
            require(lock["minecraft"] in pin["game_versions"] and "fabric" in pin["loaders"]
                    and set(pin["required_versions"]).issubset(selected_ids), f"Incompatible dependency pin: {project}")
            require(set(pin["hashes"]) == {"sha1", "sha512"}
                    and re.fullmatch(r"[0-9a-f]{40}", pin["hashes"]["sha1"])
                    and re.fullmatch(r"[0-9a-f]{128}", pin["hashes"]["sha512"]), "Invalid pinned dependency hashes")
            require(type(pin["size"]) is int and 0 < pin["size"] <= MAX_MEMBER, "Invalid pinned dependency size")
            require(pin["url"].startswith("https://cdn.modrinth.com/data/"), "Unexpected dependency download host")
            name = "mods/" + pin["filename"]
            require(name not in expected_files, "Colliding pinned dependency filenames")
            expected_files[name] = {"path": name, "hashes": pin["hashes"], "downloads": [pin["url"]],
                "fileSize": pin["size"], "env": {"client": "required", "server": "required" if project == "fabric-api" else "unsupported"}}
        jar_member = "overrides/mods/" + jar.name
        config_member = "client-overrides/config/wildercord-visuals.json"
        expected = {jar_member: {key: jar_result[key] for key in ("size_bytes", "sha256")},
                    config_member: digest_file(root / "profiles" / profile / "config/wildercord-visuals.json", MAX_JSON),
                    "client-overrides/options.txt": bytes_digest(("renderDistance:" + ("10" if profile == "performance" else "12")
                        + "\nsimulationDistance:6\nmaxFps:120\n").encode()),
                    "overrides/WILDERCORD-PROFILE.md": digest_file(root / "profiles/README.md", MAX_JSON)}
        with checked_archive(path) as archive:
            actual = archive_digests(archive)
            check_members(archive, actual, set(expected) | {"modrinth.index.json"})
            for name, digest in expected.items():
                require(actual[name] == digest, f"Profile bytes differ from pinned input: {profile}/{name}")
            archive_json(archive, config_member)
            manifest = archive_json(archive, "modrinth.index.json")
            require(manifest == {"formatVersion": 1, "game": "minecraft", "versionId": jar_result["version"] + "-" + profile,
                "name": "WilderCord " + profile.title(),
                "summary": "WilderCord with pinned Fabric dependencies and " + profile + " visuals",
                "files": manifest.get("files"), "dependencies": {"minecraft": lock["minecraft"], "fabric-loader": lock["fabric_loader"]}},
                "Profile metadata differs from expected contract")
            files = manifest["files"]
            require(isinstance(files, list) and len(files) == len(expected_files), "Unexpected profile dependency count")
            indexed = {entry["path"]: entry for entry in files}
            require(len(indexed) == len(files) and indexed == expected_files, "Profile dependency entries differ from lock")
        reports.append({"profile": profile, "file": path.name, **digest_file(path, MAX_ARCHIVE),
                        "pinned_dependencies": sorted(selected), "embedded_mod_sha256": jar_result["sha256"]})
    return reports


def provenance(root):
    def git(*args):
        return subprocess.run(["git", "-C", str(root), *args], check=True, capture_output=True,
                              text=True, timeout=10).stdout.strip()
    return {"git_commit": git("rev-parse", "HEAD"), "git_tree": git("rev-parse", "HEAD^{tree}"),
            "worktree_dirty": bool(git("status", "--porcelain", "--untracked-files=normal")),
            "github": {key: os.environ[key] for key in ("GITHUB_REPOSITORY", "GITHUB_SHA", "GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT") if key in os.environ}}


def audit(root, jar=None, profile_directory=None):
    props = properties(root)
    version = props["mod_version"] + "+mc" + props["minecraft_version"]
    if jar is None:
        candidates = sorted(path for path in (root / "build/libs").glob("*.jar") if not path.name.endswith("-sources.jar"))
        require(len(candidates) == 1, "Expected exactly one distributable JAR (excluding sources)")
        jar = candidates[0]
    jar_result = check_jar(root, jar, props)
    require(jar_result["version"] == version, "Mod version mismatch")
    profiles = check_profiles(root, profile_directory or root / "artifacts/profiles", jar, jar_result, props)
    jar_result["within_32_mib_raw_file_limit"] = jar_result["size_bytes"] <= DOWNLOAD_LIMIT
    return {"jar": jar_result, "profiles": profiles,
            "dependency_lock_sha256": digest_file(root / "profiles/dependencies.lock.json", MAX_JSON)["sha256"],
            "artifact_download_note": "The 32 MiB comparison is for the raw JAR. CI artifact ZIP size is not measured here.",
            "private_data_check": "Member allowlists and known credential/development paths only; not a general secret-content scan.",
            "dependency_downloads": "Manifest pins checked; remote dependency bytes were not downloaded or audited."}, jar


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--profiles", type=Path)
    parser.add_argument("--output", type=Path, default=ROOT / "artifacts/review/packaging-report.json")
    parser.add_argument("--distributable-directory", type=Path)
    args = parser.parse_args(argv)
    report = {"schema_version": 1, "status": "failed", "scope": "packaging_only", "gameplay_readiness": "not_assessed",
              "limits": {"archive_bytes": MAX_ARCHIVE, "member_bytes": MAX_MEMBER, "expanded_bytes": MAX_EXPANDED,
                         "entries": MAX_ENTRIES, "central_directory_bytes": MAX_CENTRAL_DIRECTORY, "json_bytes": MAX_JSON}}
    try:
        report["provenance"] = provenance(args.root)
        result, jar = audit(args.root, args.jar, args.profiles)
        report.update(result)
        if args.distributable_directory:
            destination = args.distributable_directory
            require(not destination.exists() or not any(destination.iterdir()), "Distributable directory must be empty")
            destination.mkdir(parents=True, exist_ok=True)
            staged = destination / jar.name
            shutil.copyfile(jar, staged)
            require(digest_file(staged, MAX_ARCHIVE) == {key: result["jar"][key] for key in ("size_bytes", "sha256")},
                    "Staged JAR differs from verified JAR")
        report["status"] = "passed"
    except (AuditError, OSError, ValueError, KeyError, TypeError, AttributeError, RecursionError,
            zipfile.BadZipFile, zlib.error, EOFError, NotImplementedError, RuntimeError, subprocess.SubprocessError) as error:
        report["error"] = f"{type(error).__name__}: {str(error)[:1000]}"
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Packaging audit {report['status']}; gameplay readiness not assessed. Report: {args.output}")
    if report["status"] == "failed":
        print(report["error"])
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
