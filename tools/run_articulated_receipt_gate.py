#!/usr/bin/env python3
"""Launch the existing articulated native suite, then verify and package bounded receipt metadata.

The real native launcher is fixed; this command has no mode that accepts an old marker or log.
It never copies/re-encodes PNGs, and all full native evidence remains in its original location.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import time
import uuid

from client_suites import EXIT_PREFIX, SELECTION_PREFIX
from verify_articulated_render_receipts import verify


REPO = Path(__file__).resolve().parents[1]
NONCE_ENV = "WILDERCORD_SHARED_RECEIPT_NONCE"
# Bound uncompressed metadata well below the 32 MiB review transport limit.
MAX_RECORD_BYTES = 128 * 1024
MAX_RECORDS = 256
MAX_METADATA_BYTES = 24 * 1024 * 1024
MAX_REPORT_BYTES = 1024 * 1024


def planned_trials() -> set[str]:
    """Exact unchanged ArticulatedSharedPlayerChecks matrix: 24 body and 96 HUD trials."""
    result = set()
    for backend in ("third", "hud"):
        viewports = [(1280, 720, 3)] if backend == "third" else [(854, 480, 2), (1280, 720, 3), (1280, 960, 4), (1920, 810, 3)]
        for width, height, gui in viewports:
            for armor in ("skin", "netherite"):
                for hand in ("left", "right"):
                    for move in ("rising_break", "driving_cut"):
                        for phase in ("windup", "active", "recovery"):
                            result.add(f"articulated_shared_{move}_{backend}_{width}x{height}_gui{gui}_{armor}_{hand}_requested_{phase}")
    return result


@dataclass(frozen=True)
class NativeExit:
    returncode: int
    pid: int


def launch_native(command: list[str], repo: Path, environment: dict[str, str]) -> NativeExit:
    # run_client_ci waits for its native process and retains its original failure/cleanup behavior.
    process = subprocess.Popen(command, cwd=repo, env=environment)
    return NativeExit(process.wait(), process.pid)


def source_commit(repo: Path) -> str:
    return subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo, text=True).strip()


def confirm_native_exit(log: Path, native: NativeExit, started_ns: int, finished_ns: int) -> dict:
    """A killed launcher is not evidence its separately grouped native child has finished."""
    info = log.lstat()
    if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1 or not started_ns <= info.st_mtime_ns <= finished_ns:
        raise ValueError("native completion log is missing, linked, stale or still changing")
    exits, selections = [], []
    digest = hashlib.sha256()
    with log.open("rb") as stream:
        for line in stream:
            digest.update(line)
            if line.startswith(EXIT_PREFIX.encode()):
                exits.append(line.decode().strip()[len(EXIT_PREFIX):])
            if line.startswith(SELECTION_PREFIX.encode()):
                selections.append(json.loads(line[len(SELECTION_PREFIX):]))
    if native.returncode < 0 or exits != [str(native.returncode)]:
        raise ValueError("owned launcher exited without matching native-process completion evidence")
    if len(selections) != 1 or selections[0].get("kind") != "suite" or selections[0].get("name") != "articulated":
        raise ValueError("native completion log has a different or ambiguous suite selection")
    if log.stat().st_mtime_ns != info.st_mtime_ns or log.stat().st_size != info.st_size:
        raise ValueError("native log changed after launcher exit")
    return {"nativeProcessExitConfirmed": True, "nativeLog": str(log), "nativeLogBytes": info.st_size,
            "nativeLogSha256": digest.hexdigest()}


def bounded_entries(directory: Path, limit: int) -> list[Path]:
    if not directory.exists():
        return []
    result = []
    for entry in directory.iterdir():
        result.append(entry)
        if len(result) > limit:
            raise ValueError(f"too many entries in {directory.name}; limit={limit}")
    return sorted(result)


def contained(path: Path, root: Path) -> Path:
    """Reject symlinks and outside paths before reading or writing an evidence tree."""
    absolute = Path(os.path.abspath(path))
    root = root.resolve()
    if not absolute.is_relative_to(root):
        raise ValueError(f"path is outside selected checkout: {path}")
    current = root
    for component in absolute.relative_to(root).parts:
        current /= component
        if current.is_symlink():
            raise ValueError(f"symlink evidence path: {current}")
    return absolute


def snapshot_records(directory: Path, started_ns: int, finished_ns: int) -> dict[str, tuple]:
    entries = bounded_entries(directory, MAX_RECORDS)
    result = {}
    for path in entries:
        info = path.lstat()
        if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1 or path.suffix != ".json":
            raise ValueError(f"not an independent regular JSON record: {path.name}")
        if not 0 < info.st_size <= MAX_RECORD_BYTES:
            raise ValueError(f"record exceeds bounded size: {path.name}")
        if not started_ns <= info.st_mtime_ns <= finished_ns or not started_ns <= info.st_ctime_ns <= finished_ns:
            raise ValueError(f"stale or post-exit record: {path.name}")
        result[path.name] = (info.st_dev, info.st_ino, info.st_size, info.st_mtime_ns, info.st_ctime_ns)
    if not result:
        raise ValueError("native receipt directory is empty")
    return result


def directory_identity(info) -> tuple:
    return info.st_dev, info.st_ino, info.st_mode, info.st_mtime_ns, info.st_ctime_ns


def package_run(receipt_root: Path, game_dir: Path, output: Path, nonce: str,
                started_ns: int, finished_ns: int) -> tuple[dict, dict]:
    entries = bounded_entries(receipt_root, 2)
    if len(entries) != 1:
        raise ValueError(f"expected exactly one fresh native receipt run; found {len(entries)}")
    selected = entries[0]
    if selected.is_symlink() or not selected.is_dir() or str(uuid.UUID(selected.name)) != selected.name:
        raise ValueError("receipt run is not a canonical UUID directory")
    directory_stat = selected.stat()
    if not started_ns <= directory_stat.st_mtime_ns <= finished_ns or not started_ns <= directory_stat.st_ctime_ns <= finished_ns:
        raise ValueError("receipt run directory is stale or still changing after process exit")
    snapshot = snapshot_records(selected, started_ns, finished_ns)
    raw_records = []
    total = 0
    provenance_errors = []
    for name, info in snapshot.items():
        data = (selected / name).read_bytes()
        total += len(data)
        if len(data) != info[2] or total > MAX_METADATA_BYTES - MAX_REPORT_BYTES:
            raise ValueError("complete receipt metadata exceeds bounded artifact budget")
        raw_records.append((name, data))
        try:
            record = json.loads(data)
            if record["identity"]["runId"] != selected.name:
                raise ValueError("record has copied or conflicting run identity")
            if record.get("nativeLaunchNonce") != nonce:
                raise ValueError("record does not belong to this native child launch")
            if record.get("copy") is not None and record["copy"]["observation"].get("nativeLaunchNonce") != nonce:
                raise ValueError("copy does not belong to this native child launch")
        except (ValueError, KeyError, TypeError, AttributeError, IndexError) as failure:
            provenance_errors.append(f"{name}: {failure}")
    try:
        association = verify(selected, game_dir, 120, planned_trials())
    except (OSError, ValueError, KeyError, TypeError, AttributeError, IndexError) as failure:
        association = {"associationVerified": False, "errors": [f"receipt verification failed: {failure}"]}
    association["errors"].extend(provenance_errors)
    if provenance_errors:
        association["associationVerified"] = False
    # No waiting or retry: a changing directory means the process/snapshot is not sealed.
    if snapshot_records(selected, started_ns, finished_ns) != snapshot:
        raise ValueError("native receipt records changed while verifying completed run")
    if bounded_entries(receipt_root, 2) != entries or directory_identity(selected.stat()) != directory_identity(directory_stat):
        raise ValueError("native receipt run directories changed after process exit")
    destination = output / "receipts"
    destination.mkdir()
    copied = []
    for name, data in raw_records:
        (destination / name).write_bytes(data)
        copied.append({"name": name, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
    package = {"runId": selected.name, "sourceDirectory": str(selected), "complete": True,
               "recordCount": len(copied), "recordBytes": total, "records": copied}
    return association, package


def run_gate(repo: Path, game_dir: Path, output: Path, log: Path, *, runner=launch_native,
             commit_reader=source_commit, environment=None) -> int:
    repo = repo.resolve()
    game_dir, output, log = (contained(path if path.is_absolute() else repo / path, repo) for path in (game_dir, output, log))
    if output.is_relative_to(game_dir) or game_dir.is_relative_to(output):
        raise ValueError("metadata output must be separate from the complete native game/evidence directory")
    if log.is_relative_to(output) or output.is_relative_to(log) or log.is_relative_to(game_dir):
        raise ValueError("native log must be separate from bounded metadata and original game/evidence paths")
    if log.exists():
        raise ValueError("native log already exists; preserve prior evidence and choose a fresh log path")
    if output.exists():
        raise ValueError("output directory already exists; prior provenance cannot be reused")
    output.mkdir(parents=True)
    environment = dict(os.environ if environment is None else environment)
    nonce = str(uuid.uuid4())
    report = {"schemaVersion": 1, "associationVerified": False, "phaseCoverageVerified": False,
              "phaseCoverageMisses": [], "nativeSucceeded": False,
              "expectedTrials": 120, "plannedTrials": sorted(planned_trials()), "nativePixelReviewRequired": True, "exactImpactPixelCoverage": "unverified",
              "provenance": {"launchNonce": nonce, "checkout": str(repo), "gameDirectory": str(game_dir)},
              "package": {"complete": False, "recordCount": 0, "recordBytes": 0}, "errors": []}
    returncode = 1
    try:
        report["provenance"]["sourceCommit"] = commit_reader(repo)
        for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "GITHUB_SHA", "GITHUB_JOB"):
            if key in environment:
                report["provenance"][key] = environment[key]
        if environment.get("GITHUB_SHA") and environment["GITHUB_SHA"] != report["provenance"]["sourceCommit"]:
            raise ValueError("checkout commit differs from current CI provenance")
        receipt_root = contained(game_dir / "screenshots/articulated-shared-receipts", repo)
        if bounded_entries(receipt_root, 2):
            raise ValueError("preexisting receipt run directory; stale evidence is preserved and rejected")
        command = [sys.executable, str(repo / "tools/run_client_ci.py"), "--suite", "articulated", "--log", str(log)]
        report["provenance"]["command"] = command
        environment[NONCE_ENV] = nonce
        started_ns = time.time_ns()
        report["provenance"]["startedNs"] = started_ns
        native = runner(command, repo, environment)
        finished_ns = time.time_ns()
        report["provenance"].update(finishedNs=finished_ns, waitedForExit=True, launcherPid=native.pid, nativeExitCode=native.returncode)
        report["nativeSucceeded"] = native.returncode == 0
        contained(receipt_root, repo); contained(log, repo); contained(output, repo)
        report["provenance"].update(confirm_native_exit(log, native, started_ns, finished_ns))
        # package_run is unreachable until the owned launcher has exited.
        association, package = package_run(receipt_root, game_dir, output, nonce, started_ns, finished_ns)
        report.update(association)
        report["package"] = package
        # Association remains truthful even when the unchanged PNG captured the wrong phase.
        # The final gate additionally requires every planned phase, unpaused and unfrozen.
        observations = report.get("observations", [])
        misses = [entry for entry in observations if not entry["unpausedPhaseCoverage"]]
        report["phaseCoverageMisses"] = [{key: entry[key] for key in
                                        ("trial", "requestedPhase", "renderedPhase", "requestedPhaseObserved", "unpausedPhaseCoverage")}
                                       for entry in misses]
        report["phaseCoverageVerified"] = report["associationVerified"] and len(observations) == 120 and not misses
        for entry in misses:
            reason = "requested phase differs from rendered phase" if not entry["requestedPhaseObserved"] else "capture was paused or frozen"
            report["errors"].append(f"{entry['trial']}: phase coverage miss: requested={entry['requestedPhase']} rendered={entry['renderedPhase']}; {reason}")
        if native.returncode:
            report["errors"].append(f"original native launcher failed with exit code {native.returncode}")
        returncode = 0 if report["associationVerified"] and report["phaseCoverageVerified"] and report["nativeSucceeded"] else 1
    except Exception as failure:
        report["errors"].append(f"{type(failure).__name__}: {failure}")
    report["gatePassed"] = returncode == 0
    data = (json.dumps(report, indent=2, sort_keys=True) + "\n").encode()
    if len(data) > MAX_REPORT_BYTES:
        # Keep a useful failure report and complete raw records rather than leaving an ambiguous bundle.
        report.pop("observations", None)
        report["omittedReportFields"] = ["observations"]
        report["associationVerified"] = report["phaseCoverageVerified"] = report["gatePassed"] = False
        report["errorCountBeforeBounding"] = len(report["errors"])
        report["errorDetailsBounded"] = True
        report["errors"] = [str(error)[:2048] for error in report["errors"][:32]]
        report["errors"].append("association report exceeds bounded metadata budget; full raw receipt records are preserved")
        for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "GITHUB_SHA", "GITHUB_JOB"):
            if key in report["provenance"]:
                report["provenance"][key] = str(report["provenance"][key])[:256]
        returncode = 1
        data = (json.dumps(report, indent=2, sort_keys=True) + "\n").encode()
        if len(data) > MAX_REPORT_BYTES:
            # Defensive last resort: failure remains explicit even for pathological external strings.
            report = {"schemaVersion": 1, "gatePassed": False, "associationVerified": False, "phaseCoverageVerified": False,
                      "nativeSucceeded": report["nativeSucceeded"], "nativePixelReviewRequired": True,
                      "exactImpactPixelCoverage": "unverified", "launchNonce": nonce,
                      "package": {key: report["package"].get(key) for key in ("complete", "recordCount", "recordBytes")},
                      "packageManifestOmitted": True,
                      "errors": ["association report exceeded metadata budget; copied complete raw records and original native evidence remain available"]}
            data = (json.dumps(report, indent=2, sort_keys=True) + "\n").encode()
    (output / "association-report.json").write_bytes(data)
    print(f"ARTICULATED_RECEIPT_GATE passed={report['gatePassed']} report={output / 'association-report.json'}")
    return returncode


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-dir", type=Path, default=Path("build/run/clientGameTest"))
    parser.add_argument("--output", type=Path, required=True, help="New bounded metadata artifact directory")
    parser.add_argument("--log", type=Path, default=Path("articulated-native.log"))
    args = parser.parse_args(argv)
    try:
        return run_gate(REPO, args.game_dir, args.output, args.log)
    except (OSError, ValueError) as failure:
        print(f"ARTICULATED_RECEIPT_GATE failed: {failure}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
