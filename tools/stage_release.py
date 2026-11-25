"""Release candidate verifier and local bundle stager (never tags, pushes, or publishes externally)."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

TOOLS_DIR = Path(__file__).resolve().parent
ROOT = TOOLS_DIR.parent
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

import audit_packages
import build_profiles

VERSION_RE = re.compile(r"^\d+\.\d+\.\d+(-[a-z0-9.]+)?$")


class ReleaseError(ValueError):
    pass


def parse_properties(root: Path) -> dict[str, str]:
    props_path = root / "gradle.properties"
    if not props_path.is_file():
        raise ReleaseError(f"Missing gradle.properties at {props_path}")
    props: dict[str, str] = {}
    for line in props_path.read_text(encoding="utf-8-sig").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        props[k.strip()] = v.strip()
    mod_version = props.get("mod_version", "")
    mc_version = props.get("minecraft_version", "")
    if not VERSION_RE.fullmatch(mod_version):
        raise ReleaseError(f"Invalid mod_version in gradle.properties: {mod_version!r}")
    if not mc_version:
        raise ReleaseError("Missing minecraft_version in gradle.properties")
    return props


def _git_output(root: Path, *args: str) -> str:
    return subprocess.run(
        ["git", "-C", str(root), *args],
        check=True,
        capture_output=True,
        text=True,
        timeout=15,
    ).stdout.strip()


def run_local_gates(root: Path, allow_dirty: bool = False) -> dict[str, object]:
    props = parse_properties(root)
    dirty = bool(_git_output(root, "status", "--porcelain", "--untracked-files=normal"))
    if dirty and not allow_dirty:
        raise ReleaseError("Git worktree is dirty; commit/clean changes or pass --allow-dirty")

    checks = [
        ("check_generated_assets", [sys.executable, str(root / "tools" / "check_generated_assets.py")]),
        ("player_docs", [sys.executable, str(root / "tools" / "player_docs.py"), "--check"]),
        ("lint_ticks", [sys.executable, str(root / "tools" / "lint_ticks.py"), "src/main/java"]),
    ]
    results = {}
    for name, cmd in checks:
        proc = subprocess.run(cmd, cwd=root, capture_output=True, text=True, timeout=180)
        if proc.returncode != 0:
            detail = (proc.stderr or proc.stdout or "").strip()
            raise ReleaseError(f"Local gate '{name}' failed (exit {proc.returncode}): {detail[-800:]}")
        results[name] = "passed"
    return {
        "version": props["mod_version"],
        "minecraft_version": props["minecraft_version"],
        "git_commit": _git_output(root, "rev-parse", "HEAD"),
        "worktree_dirty": dirty,
        "gates": results,
    }


def _sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        while chunk := fh.read(65536):
            h.update(chunk)
    return h.hexdigest()


def _resolve_jar(root: Path, jar: Path | None) -> Path:
    if jar is not None:
        return jar
    candidates = sorted(
        p for p in (root / "build" / "libs").glob("*.jar")
        if p.is_file() and not p.name.endswith("-sources.jar")
    )
    if len(candidates) != 1:
        raise ReleaseError(f"Expected exactly 1 non-sources JAR in {root / 'build/libs'}, found {len(candidates)}")
    return candidates[0]


def stage_release_bundle(root: Path, output_dir: Path, jar: Path | None = None) -> dict[str, object]:
    props = parse_properties(root)
    resolved_jar = _resolve_jar(root, jar)
    if not resolved_jar.is_file():
        raise ReleaseError(f"Missing JAR: {resolved_jar}")

    profiles_dir = output_dir / "profiles"
    jar_dir = output_dir / "jar"
    profiles_dir.mkdir(parents=True, exist_ok=True)
    jar_dir.mkdir(parents=True, exist_ok=True)

    built_profiles: list[Path] = []
    prev_profiles_root = build_profiles.ROOT
    build_profiles.ROOT = root
    try:
        for profile in build_profiles.PROFILES:
            built_profiles.append(build_profiles.build(profile, resolved_jar, profiles_dir))
    finally:
        build_profiles.ROOT = prev_profiles_root

    audit_report, audited_jar = audit_packages.audit(root, jar=resolved_jar, profile_directory=profiles_dir)
    if not audit_report["jar"].get("within_32_mib_raw_file_limit", False):
        raise ReleaseError(f"JAR exceeds 32 MiB raw file limit: {audit_report['jar']['size_bytes']} bytes")

    staged_jar = jar_dir / audited_jar.name
    shutil.copyfile(audited_jar, staged_jar)
    if _sha256_file(staged_jar) != audit_report["jar"]["sha256"]:
        raise ReleaseError("Staged JAR SHA256 mismatch")

    files_to_sum = [staged_jar, *sorted(built_profiles)]
    sum_lines = [f"{_sha256_file(p)}  {p.relative_to(output_dir).as_posix()}" for p in files_to_sum]
    (output_dir / "SHA256SUMS.txt").write_text("\n".join(sum_lines) + "\n", encoding="utf-8", newline="\n")

    manifest = {
        "schema_version": 1,
        "version": props["mod_version"],
        "minecraft_version": props["minecraft_version"],
        "git_commit": _git_output(root, "rev-parse", "HEAD"),
        "jar": {
            "path": staged_jar.relative_to(output_dir).as_posix(),
            "size_bytes": audit_report["jar"]["size_bytes"],
            "sha256": audit_report["jar"]["sha256"],
            "within_32_mib_raw_file_limit": audit_report["jar"]["within_32_mib_raw_file_limit"],
        },
        "profiles": [
            {
                "profile": entry["profile"],
                "path": f"profiles/{entry['file']}",
                "size_bytes": entry["size_bytes"],
                "sha256": entry["sha256"],
            }
            for entry in audit_report["profiles"]
        ],
        "ready_for_manual_publish": True,
        "published": False,
    }
    (output_dir / "release-manifest.json").write_text(
        json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n"
    )
    return manifest


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--check", action="store_true", help="Run non-mutating release preflight gates")
    parser.add_argument("--prepare", action="store_true", help="Build .mrpack profiles, audit packaging, and stage bundle")
    parser.add_argument("--output", type=Path, default=ROOT / "artifacts" / "release" / "latest")
    parser.add_argument("--jar", type=Path, default=None)
    parser.add_argument("--allow-dirty", action="store_true")
    args = parser.parse_args(argv)

    if not args.check and not args.prepare:
        args.check = True

    try:
        root = args.root.resolve()
        gate_summary = run_local_gates(root, allow_dirty=args.allow_dirty)
        if args.prepare:
            manifest = stage_release_bundle(root, args.output.resolve(), jar=args.jar)
            print(json.dumps({"gates": gate_summary, "bundle": manifest}, indent=2))
        else:
            print(json.dumps({"gates": gate_summary}, indent=2))
        return 0
    except (ReleaseError, audit_packages.AuditError, OSError, ValueError, subprocess.SubprocessError) as exc:
        print(f"Release check failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
