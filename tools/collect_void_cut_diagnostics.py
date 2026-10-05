"""Preserve six exact Void Cut native files, including potentially rejected frames.

Run --prepare before the existing native command, then collect with that marker.
This independent diagnostic artifact never establishes test or animation acceptance.
No source file is changed, parsed as capture evidence, or re-encoded.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys
import tempfile
import time


ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path("build/run/clientGameTest/screenshots")
# Leave headroom for the manifest and ZIP headers below the 14 MB download limit.
BYTE_LIMIT = 13_900_000
REPORT_RESERVE = 16_384
MARKER_LIMIT = 16_384
# Prefer the failure's active receipt and buffer if all six files cannot fit.
FILES = tuple(f"masters_style_void_cut_first_{phase}.{extension}"
              for phase in ("active", "windup", "recovery")
              for extension in ("json", "png"))
JOB_GROUPS = ("masters-native", *(f"game-tests-{shard}-of-4" for shard in range(1, 5)))


class DiagnosticError(ValueError):
    """Cannot establish bounded, fresh, unambiguous diagnostic inputs."""


def encode(value):
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def safe_path(root, relative):
    relative = Path(relative)
    if relative.is_absolute() or not relative.parts or ".." in relative.parts:
        raise DiagnosticError("Paths must be repository-relative without traversal")
    root = root.resolve(strict=True)
    target = root / relative
    for component in (target, *target.parents):
        if component == root:
            break
        if component.is_symlink():
            raise DiagnosticError("Symlinks are not diagnostic inputs or outputs")
    if not target.resolve().is_relative_to(root):
        raise DiagnosticError("Path escapes repository")
    return target


def provenance(root, environ, job_group):
    if job_group not in JOB_GROUPS:
        raise DiagnosticError("Unsupported native job group")
    expected_job = "masters-native" if job_group == "masters-native" else "game-tests"
    if environ.get("GITHUB_JOB") != expected_job:
        raise DiagnosticError("Diagnostic group does not match the current GitHub job")
    sha = environ.get("GITHUB_SHA", "")
    repository = environ.get("GITHUB_REPOSITORY", "")
    if not re.fullmatch(r"[0-9a-fA-F]{40}", sha):
        raise DiagnosticError("Missing exact source commit")
    if (not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}", repository)
            or repository.rsplit("/", 1)[-1] in (".", "..")):
        raise DiagnosticError("Missing repository identity")
    for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT"):
        if not re.fullmatch(r"[1-9][0-9]{0,19}", environ.get(key, "")):
            raise DiagnosticError("Missing run/attempt identity")
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    if sha.lower() != actual.lower():
        raise DiagnosticError("Checked-out commit does not match GITHUB_SHA")
    # A PR workflow can check out a merge commit. Record it separately from its head.
    event_name, head = environ.get("GITHUB_EVENT_NAME"), sha
    if event_name == "pull_request":
        event_path = environ.get("GITHUB_EVENT_PATH")
        if not event_path:
            raise DiagnosticError("Missing PR event for source head identity")
        event = json.loads(Path(event_path).read_text(encoding="utf-8"))
        head = event.get("pull_request", {}).get("head", {}).get("sha")
        if not isinstance(head, str) or not re.fullmatch(r"[0-9a-fA-F]{40}", head):
            raise DiagnosticError("Missing exact PR head commit")
    elif event_name != "push":
        raise DiagnosticError("Unsupported GitHub event")
    return {"repository": repository, "sourceCommit": actual, "headCommit": head,
            "eventName": event_name, "runId": environ["GITHUB_RUN_ID"],
            "runAttempt": environ["GITHUB_RUN_ATTEMPT"], "job": expected_job,
            "jobGroup": job_group,
            "runAttemptUrl": f"https://github.com/{repository}/actions/runs/"
                             f"{environ['GITHUB_RUN_ID']}/attempts/{environ['GITHUB_RUN_ATTEMPT']}"}


def separated_paths(root, marker, output=None):
    source = safe_path(root, SOURCE)
    marker_path = safe_path(root, marker)
    if marker_path.is_relative_to(source) or source.is_relative_to(marker_path):
        raise DiagnosticError("Marker must be separate from screenshots")
    if output is None:
        return source, marker_path
    destination = safe_path(root, output)
    for path in (source, marker_path):
        if destination.is_relative_to(path) or path.is_relative_to(destination):
            raise DiagnosticError("Output must be separate from screenshots and marker")
    if destination.exists():
        raise DiagnosticError("Never reuse an existing diagnostic output")
    return source, marker_path, destination


def prepare(root, marker, identity):
    _, marker_path = separated_paths(root, marker)
    # Only exact direct children are inspected; do not traverse the full gallery.
    preexisting = [name for name in FILES if safe_path(root, SOURCE / name).exists()]
    stamp = {"schemaVersion": 1, "provenance": identity, "source": SOURCE.as_posix(),
             "startedNs": time.time_ns(), "preexistingFiles": preexisting}
    data = encode(stamp)
    if len(data) > MARKER_LIMIT:
        raise DiagnosticError("Preparation marker exceeds byte limit")
    marker_path.parent.mkdir(parents=True, exist_ok=True)
    # A second invocation cannot quietly adopt an earlier launch's marker.
    with marker_path.open("xb") as handle:
        handle.write(data)


def read_marker(root, marker, identity, ended_ns):
    path = safe_path(root, marker)
    try:
        descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    except FileNotFoundError:
        return None
    with os.fdopen(descriptor, "rb") as handle:
        before = os.fstat(handle.fileno())
        if not stat.S_ISREG(before.st_mode) or before.st_size > MARKER_LIMIT:
            raise DiagnosticError("Invalid preparation marker")
        data = handle.read(MARKER_LIMIT + 1)
        after = os.fstat(handle.fileno())
    if not unchanged(before, after) or len(data) > MARKER_LIMIT:
        raise DiagnosticError("Preparation marker changed or exceeded byte limit")
    stamp = json.loads(data)
    if (not isinstance(stamp, dict) or stamp.get("schemaVersion") != 1
            or stamp.get("provenance") != identity or stamp.get("source") != SOURCE.as_posix()
            or type(stamp.get("startedNs")) is not int
            or not 0 < stamp["startedNs"] <= ended_ns
            or not isinstance(stamp.get("preexistingFiles"), list)
            or any(name not in FILES for name in stamp["preexistingFiles"])):
        raise DiagnosticError("Preparation marker does not match this job/run/attempt")
    return stamp


def unchanged(before, after):
    return all(getattr(before, key) == getattr(after, key) for key in
               ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns"))


def copy_file(root, name, staging, stamp, ended_ns, remaining):
    path = safe_path(root, SOURCE / name)
    row = {"name": name, "sourcePath": (SOURCE / name).as_posix()}
    try:
        descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    except FileNotFoundError:
        return {**row, "status": "missing"}
    with os.fdopen(descriptor, "rb") as handle:
        before = os.fstat(handle.fileno())
        if not stat.S_ISREG(before.st_mode):
            raise DiagnosticError("Only regular diagnostic files are allowed")
        if stamp is None:
            return {**row, "status": "freshness-unavailable"}
        if name in stamp["preexistingFiles"]:
            return {**row, "status": "preexisting"}
        if not (stamp["startedNs"] <= before.st_mtime_ns <= ended_ns
                and stamp["startedNs"] <= before.st_ctime_ns <= ended_ns):
            return {**row, "status": "outside-current-run-window"}
        row["bytes"] = before.st_size
        if before.st_size > remaining:
            return {**row, "status": "omitted-for-byte-limit"}
        checksum, size = hashlib.sha256(), 0
        copied = staging / name
        with copied.open("xb") as output:
            while data := handle.read(min(64 * 1024, remaining - size + 1)):
                size += len(data)
                if size > remaining:
                    raise DiagnosticError("Diagnostic source grew beyond byte limit")
                output.write(data)
                checksum.update(data)
        after = os.fstat(handle.fileno())
    current = safe_path(root, SOURCE / name).stat()
    if not unchanged(before, after) or not unchanged(after, current) or size != before.st_size:
        raise DiagnosticError("Diagnostic source changed while being copied")
    if copied.stat().st_size != size or hashlib.sha256(copied.read_bytes()).hexdigest() != checksum.hexdigest():
        raise DiagnosticError("Diagnostic copy differs from source bytes")
    return {**row, "status": "included", "artifactPath": name, "sha256": checksum.hexdigest()}


def collect(root, marker, output, identity, *, budget=BYTE_LIMIT):
    if not REPORT_RESERVE <= budget <= BYTE_LIMIT:
        raise DiagnosticError("Invalid diagnostic byte limit")
    _, _, destination = separated_paths(root, marker, output)
    ended_ns = time.time_ns()
    stamp = read_marker(root, marker, identity, ended_ns)
    report = {
        "schemaVersion": 1, "artifactKind": "void-cut-native-diagnostics",
        "provenance": identity, "byteLimit": budget,
        "basis": "Byte-identical diagnostics; may include rejected or incomplete native frames. "
                 "Filename phases are labels only. Neither file presence nor sidecar values establish "
                 "accepted animation coverage or any test verdict. Inspect the unchanged full evidence.",
        "testVerdictEstablished": False, "acceptedAnimationCoverageEstablished": False,
        "freshness": {"status": "prepared" if stamp else "missing-preparation-marker",
                      "startedNs": stamp["startedNs"] if stamp else None, "collectedNs": ended_ns,
                      "policy": "Exclude every preexisting name, then require mtime and ctime within this run window."},
        "selectionPolicy": "Exact six direct filenames only; active, windup, recovery; JSON before PNG. "
                           "Each file is independent, and missing counterparts remain missing.",
    }
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".void-cut-diagnostics-", dir=destination.parent) as temp:
        staging = Path(temp) / "artifact"
        staging.mkdir()
        rows, total = [], 0
        for name in FILES:
            row = copy_file(root, name, staging, stamp, ended_ns, budget - REPORT_RESERVE - total)
            rows.append(row)
            if row["status"] == "included":
                total += row["bytes"]
        report.update(files=rows, selectedBytes=total,
                      status="diagnostics-available" if any(row["status"] == "included" for row in rows)
                             else "diagnostics-unavailable",
                      missingFiles=[row["name"] for row in rows if row["status"] == "missing"])
        data = encode(report)
        if len(data) > REPORT_RESERVE or total + len(data) > budget:
            raise DiagnosticError("Diagnostic artifact exceeds byte limit")
        (staging / "manifest.json").write_bytes(data)
        separated_paths(root, marker, output)
        staging.rename(destination)
    return report


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--job-group", choices=JOB_GROUPS, required=True)
    parser.add_argument("--marker", required=True)
    parser.add_argument("--output")
    args = parser.parse_args(argv)
    if not args.prepare and not args.output:
        parser.error("--output is required when collecting")
    try:
        identity = provenance(ROOT, os.environ, args.job_group)
        if args.prepare:
            prepare(ROOT, args.marker, identity)
        else:
            report = collect(ROOT, args.marker, args.output, identity)
            print(json.dumps({"output": args.output, "status": report["status"],
                              "selectedBytes": report["selectedBytes"]}))
    except (DiagnosticError, OSError, ValueError, subprocess.SubprocessError) as exc:
        print(f"Void Cut diagnostics unavailable: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
