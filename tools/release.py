"""Cut a Wildercord release: bump the version, build, smoke, tag, upload and stage.

Run on the VPS from the CI checkout (C:\\wildercord-ci\\repo) with the same environment as
run_vps_checks.py (JAVA_HOME, GRADLE_USER_HOME, the venv on PATH). Steps run in order and print
what they do; --dry-run prints the plan without touching anything, and the skip flags let a
partly finished release be resumed by hand.

  1. Guard: right computer, clean tree, docs/releases/<version>.md present (the CurseForge
     changelog reads it) and the version different from gradle.properties.
  2. Bump mod_version, commit "Release <version>", tag v<version>, push branch and tag.
  3. Build the jar and sources with Gradle.
  4. Run the release-smoke suite (--skip-smoke to skip; it is the pre-release check).
  5. Upload to CurseForge (jar, then sources parented to it) and Modrinth (--skip-upload).
     Tokens come from the process environment, as the upload tools want them.
  6. Stage the jar for the live server: write <jar>.sha256 first, then the jar, into
     <server-root>\\staging; the watchdog installs it while nobody is playing (--no-stage skips).

Usage: python tools/release.py --version 0.15.0-alpha [--computer WIN-I2GBVRRD872]
       [--skip-smoke] [--skip-upload] [--no-stage] [--dry-run] [--server-root C:\\wildercord]
"""

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSION_SHAPE = re.compile(r"\d+\.\d+\.\d+-alpha(\.\d+)?")


def parse_property(text, key):
    match = re.search(rf"^{key}=(.+)$", text, re.MULTILINE)
    if not match:
        sys.exit(f"gradle.properties has no {key}")
    return match.group(1).strip()


def bumped(text, version):
    """gradle.properties text with mod_version set to the new release version."""
    if not VERSION_SHAPE.fullmatch(version):
        sys.exit(f"{version} is not shaped like 0.15.0-alpha")
    if parse_property(text, "mod_version") == version:
        sys.exit(f"gradle.properties is already {version}; nothing to release")
    return re.sub(r"^mod_version=.+$", f"mod_version={version}", text, count=1, flags=re.MULTILINE)


def git(*args, capture=False):
    command = ["git", *args]
    print("+ " + " ".join(command), flush=True)
    return subprocess.run(command, check=True, capture_output=capture, text=True)


def sha256_of(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def stage(jar, staging):
    """Put the jar where the watchdog will install it: the .sha256 first, so a half-written
    jar is never a jar with a matching hash."""
    staging = Path(staging)
    staging.mkdir(parents=True, exist_ok=True)
    jar = Path(jar)
    (staging / f"{jar.name}.sha256").write_text(sha256_of(jar) + "\n", encoding="ascii")
    shutil.copy2(jar, staging / jar.name)
    return staging / jar.name


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--computer", default="WIN-I2GBVRRD872")
    parser.add_argument("--server-root", default=r"C:\wildercord")
    parser.add_argument("--skip-smoke", action="store_true")
    parser.add_argument("--skip-upload", action="store_true")
    parser.add_argument("--no-stage", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)

    if os.environ.get("COMPUTERNAME", "").casefold() != args.computer.casefold():
        sys.exit(f"this is not {args.computer}; releases run on the VPS")
    notes = ROOT / "docs/releases" / f"{args.version}.md"
    if not notes.exists():
        sys.exit(f"{notes} is missing: write the release notes first (CurseForge sends them as the changelog)")
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    new_properties = bumped(properties, args.version)
    minecraft = parse_property(properties, "minecraft_version")
    jar_name = f"wildercord-{args.version}+mc{minecraft}.jar"
    sources_name = jar_name.replace(".jar", "-sources.jar")

    def step(number, name, action):
        print(f"[{number}] {name}", flush=True)
        if not args.dry_run:
            action()

    def do_bump():
        (ROOT / "gradle.properties").write_text(new_properties, encoding="utf-8")
        git("add", "gradle.properties")
        git("commit", "-m", f"Release {args.version}")
        git("tag", f"v{args.version}")
        branch = git("rev-parse", "--abbrev-ref", "HEAD", capture=True).stdout.strip()
        git("push", "origin", branch)
        git("push", "origin", f"v{args.version}")

    def do_build():
        subprocess.run([str(ROOT / "gradlew.bat"), "assemble", "--console=plain"], check=True, cwd=ROOT)
        for name in (jar_name, sources_name):
            if not (ROOT / "build/libs" / name).exists():
                sys.exit(f"the build did not produce build/libs/{name}")

    def do_smoke():
        head = git("rev-parse", "HEAD", capture=True).stdout.strip()
        subprocess.run([sys.executable, "tools/run_vps_checks.py", "suite", "--suite", "release-smoke",
                        "--batch", "1", "--head", head, "--computer", args.computer,
                        "--accepted-eula", str(Path(args.server_root) / "eula.txt")], check=True, cwd=ROOT)

    def do_upload():
        jar = ROOT / "build/libs" / jar_name
        sources = ROOT / "build/libs" / sources_name
        curse = subprocess.run([sys.executable, "tools/curseforge_upload.py", str(jar)],
                               check=True, cwd=ROOT, capture_output=True, text=True)
        file_id = json.loads(curse.stdout.strip().splitlines()[-1])["fileID"]
        subprocess.run([sys.executable, "tools/curseforge_upload.py", str(sources),
                        "--parent", str(file_id), "--version", args.version], check=True, cwd=ROOT)
        subprocess.run([sys.executable, "tools/modrinth_upload.py", str(jar), "--sources", str(sources)],
                       check=True, cwd=ROOT)

    def do_stage():
        staged = stage(ROOT / "build/libs" / jar_name, Path(args.server_root) / "staging")
        print(f"staged {staged}; the watchdog installs it while nobody is playing", flush=True)

    step(1, f"bump to {args.version}, commit, tag v{args.version}, push", do_bump)
    step(2, f"build {jar_name} and sources", do_build)
    if args.skip_smoke:
        print("[3] release-smoke skipped", flush=True)
    else:
        step(3, "release-smoke suite", do_smoke)
    if args.skip_upload:
        print("[4] uploads skipped", flush=True)
    else:
        step(4, "upload to CurseForge and Modrinth", do_upload)
    if args.no_stage:
        print("[5] staging skipped", flush=True)
    else:
        step(5, f"stage into {Path(args.server_root) / 'staging'}", do_stage)

    print(f"{'dry-run: planned' if args.dry_run else 'released'} {args.version}", flush=True)
    if not args.dry_run:
        print(f"sha256 {sha256_of(ROOT / 'build/libs' / jar_name)}", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
