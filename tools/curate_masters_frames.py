"""Copy a bounded, byte-identical subset of this run's native suite screenshots.

Run --prepare before the native suites and run again afterwards with the same
--marker and --suite (Masters by default). This report is visual evidence only:
test_manifest.py owns gate status.
No images are rendered, resized, decoded, or re-encoded here.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import time


ROOT = Path(__file__).resolve().parents[1]
BUDGET = 14_000_000
MANIFEST_RESERVE = 128_000
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
SUITE = "dev.wildercord.gametest.WildercordMastersArtsPresentationTest"
ARTICULATED_SUITE = "dev.wildercord.client.combat.ArticulatedCombatPresentationTest"
SUITES = {"masters": SUITE, "articulated": ARTICULATED_SUITE}
ARTICULATED_HANDS = ("left", "right")
SAMPLE_POSITIONS = ("first_available", "middle_available", "last_available")
ARTICULATED_PATTERN = re.compile(r"articulated_live_(left|right)_(first|third)_frame_([0-8])\.png\Z")
FIRST_FORMS = (
    "kindling_draw", "frostbite", "crackle", "cutting_breeze", "rockbreaker",
    "thorn_lash", "void_cut", "star_needle", "echo_cut", "bloodletting",
)
SECOND_FORMS = ("rising_cinders", "blossom_fall")
# Start with one shared art and both implemented second forms, then broaden.
SCENES = ("masters_art_0", "masters_style_kindling_draw",
          "masters_style_rising_cinders", "masters_style_blossom_fall",
          "masters_art_1", "masters_art_2",
          *("masters_style_" + art for art in FIRST_FORMS[1:]))
VIEWS = {"first": "owner_first_person", "third": "local_owner_third_person_front"}
PHASES = ("windup", "sample", "settled")
PATTERN = re.compile(r"(masters_art_[012]|masters_style_(?:"
                     + "|".join(FIRST_FORMS + SECOND_FORMS)
                     + r"))_(first|third)_(windup|settled|frame_([0-9]+))\.png\Z")


class EvidenceError(ValueError):
    """Do not publish an artifact whose identity or paths cannot be established."""


def safe_path(root, relative):
    """Require a repository-relative path, without links or traversal components."""
    relative = Path(relative)
    if relative.is_absolute() or ".." in relative.parts or not relative.parts:
        raise EvidenceError(f"Unsafe relative path: {relative}")
    root = root.resolve(strict=True)
    target = root / relative
    for part in (target, *target.parents):
        if part == root:
            break
        if part.is_symlink():
            raise EvidenceError(f"Symlink is not allowed: {relative}")
    if not target.resolve().is_relative_to(root):
        raise EvidenceError(f"Path escapes the repository: {relative}")
    return target


def source_files(root, relative):
    source = safe_path(root, relative)
    if not source.exists():
        return []
    if not source.is_dir():
        raise EvidenceError("Screenshot root is not a directory")
    result = []
    for directory, dirs, files in os.walk(source, followlinks=False):
        for name in sorted(dirs + files):
            path = safe_path(root, (Path(directory) / name).relative_to(root))
            mode = path.stat().st_mode
            if not (stat.S_ISDIR(mode) or stat.S_ISREG(mode)):
                raise EvidenceError("Non-regular screenshot source")
            if stat.S_ISREG(mode) and path.suffix.lower() == ".png":
                result.append(path)
    return sorted(result)


def provenance(root, environ):
    """Read exact PR head identity from the event, separately from checkout SHA."""
    required = ("GITHUB_SHA", "GITHUB_REPOSITORY", "GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT")
    if any(not environ.get(key) for key in required):
        raise EvidenceError("GitHub run identity is missing")
    sha = environ["GITHUB_SHA"]
    repository = environ["GITHUB_REPOSITORY"]
    if not re.fullmatch(r"[0-9a-fA-F]{40}", sha):
        raise EvidenceError("GITHUB_SHA is not an exact commit SHA")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise EvidenceError("Invalid GitHub repository identity")
    if any(not re.fullmatch(r"[1-9][0-9]*", environ[key])
           for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT")):
        raise EvidenceError("Invalid GitHub run identity")
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    if actual.lower() != sha.lower():
        raise EvidenceError("Checked-out HEAD does not match GITHUB_SHA")
    head = sha
    event_name = environ.get("GITHUB_EVENT_NAME", "")
    if event_name.startswith("pull_request"):
        event_path = environ.get("GITHUB_EVENT_PATH")
        if not event_path:
            raise EvidenceError("PR event is missing; cannot substitute the merge SHA for its head")
        event = json.loads(Path(event_path).read_text(encoding="utf-8"))
        head = event.get("pull_request", {}).get("head", {}).get("sha")
        if not isinstance(head, str) or not re.fullmatch(r"[0-9a-fA-F]{40}", head):
            raise EvidenceError("PR event does not contain an exact head SHA")
    elif event_name != "push":
        raise EvidenceError("Only push or pull_request run provenance is supported")
    base = f"https://github.com/{repository}"
    run = f"{base}/actions/runs/{environ['GITHUB_RUN_ID']}"
    return {"repository": repository, "eventName": event_name, "headSha": head,
            "workflowSha": sha, "checkedOutSha": actual,
            "runId": environ["GITHUB_RUN_ID"], "runAttempt": environ["GITHUB_RUN_ATTEMPT"],
            "runUrl": run, "runAttemptUrl": f"{run}/attempts/{environ['GITHUB_RUN_ATTEMPT']}",
            "headCommitUrl": f"{base}/commit/{head}", "workflowCommitUrl": f"{base}/commit/{sha}"}


def encode(value):
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def suite_name(suite):
    if suite not in SUITES:
        raise EvidenceError(f"Unsupported screenshot suite: {suite}")
    return SUITES[suite]


def prepare(root, screenshots, marker, identity, *, suite="masters"):
    entrypoint = suite_name(suite)
    marker_path = safe_path(root, marker)
    prior = [path.relative_to(root).as_posix() for path in source_files(root, screenshots)]
    marker_path.parent.mkdir(parents=True, exist_ok=True)
    # Never overwrite a marker from a prior attempt.
    with marker_path.open("xb") as handle:
        handle.write(encode({"schemaVersion": 1, "provenance": identity,
                             "suiteGroup": suite, "suite": entrypoint,
                             "screenshots": Path(screenshots).as_posix(),
                             "startedNs": time.time_ns(), "preexistingPngs": prior}))


def describe(filename, *, suite="masters"):
    suite_name(suite)
    if suite == "articulated":
        match = ARTICULATED_PATTERN.fullmatch(filename)
        if not match:
            return None
        hand, view, frame = match.groups()
        return {"scene": "articulated_live_" + hand, "hand": hand, "view": VIEWS[view],
                "captureKind": "native_local_owner_accepted_input_combat",
                "frameLabel": "frame_" + frame, "phase": "sample", "sampleIndex": int(frame),
                "phaseBasis": "filename_loop_index_only"}
    match = PATTERN.fullmatch(filename)
    if not match:
        return None
    scene, view, label, frame = match.groups()
    if frame is not None and (str(int(frame)) != frame
                              or int(frame) >= (7 if scene.startswith("masters_art_") else 10)):
        return None
    form = ("shared_art" if scene.startswith("masters_art_") else
            "second_form" if scene.removeprefix("masters_style_") in SECOND_FORMS else "first_form")
    # Loop counters do not prove an impact, release, or recovery phase.
    return {"scene": scene, "form": form, "view": VIEWS[view],
            "captureKind": "native_local_owner_combat", "frameLabel": label,
            "phase": "sample" if frame is not None else label,
            "sampleIndex": int(frame) if frame is not None else None,
            "phaseBasis": "filename_loop_index_only" if frame is not None else "explicit_filename_label"}


def digest(path):
    checksum = hashlib.sha256()
    with path.open("rb") as handle:
        if handle.read(8) != PNG_SIGNATURE:
            raise EvidenceError(f"Selected source lacks a PNG signature: {path.name}")
        handle.seek(0)
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            checksum.update(chunk)
    return checksum.hexdigest()


def unchanged(before, after):
    # Reading may change access time; content/inode metadata must stay unchanged.
    return all(getattr(before, name) == getattr(after, name)
               for name in ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns"))


def select_masters(groups, budget):
    selected = []
    coverage = []
    remaining = budget - MANIFEST_RESERVE
    # Complete per-view triptychs first, in stable scene/view order. If a triptych
    # cannot fit, keep whichever of its authentic frames still fit the budget.
    for scene in SCENES:
        for view in VIEWS.values():
            captured = []
            chosen = []
            missing = []
            omitted = []
            for phase in PHASES:
                options = groups.get((scene, view, phase), [])
                if not options:
                    missing.append(phase)
                    continue
                captured.append(phase)
                options.sort(key=lambda item: (item["sampleIndex"] or 0, item["sourcePath"]))
                candidate = options[len(options) // 2] if phase == "sample" else options[0]
                if candidate["bytes"] > remaining:
                    omitted.append(phase)
                    continue
                selected.append(candidate)
                chosen.append(candidate["frameLabel"])
                remaining -= candidate["bytes"]
            coverage.append({"scene": scene, "view": view, "capturedPhases": captured,
                             "selectedFrameLabels": chosen, "missingCapturePhases": missing,
                             "omittedForBudgetPhases": omitted})
    return selected, coverage


def select_articulated(groups, budget):
    selected = []
    coverage = []
    remaining = budget - MANIFEST_RESERVE
    for hand in ARTICULATED_HANDS:
        scene = "articulated_live_" + hand
        for view in VIEWS.values():
            options = sorted(groups.get((scene, view, "sample"), []),
                             key=lambda item: (item["sampleIndex"], item["sourcePath"]))
            chosen = []
            omitted = []
            positions = {}
            # One or two actual captures may serve multiple sample positions.
            # Copy and charge each original PNG only once; do not invent frames.
            candidates = {}
            if options:
                for position, candidate in zip(SAMPLE_POSITIONS,
                                               (options[0], options[len(options) // 2], options[-1])):
                    positions[position] = candidate["frameLabel"]
                    candidates.setdefault(candidate["frameLabel"], (candidate, []))[1].append(position)
            for candidate, sample_positions in candidates.values():
                if candidate["bytes"] > remaining:
                    omitted.extend(sample_positions)
                    continue
                selected.append({**candidate, "samplePositions": sample_positions})
                chosen.append(candidate["frameLabel"])
                remaining -= candidate["bytes"]
            indices = [item["sampleIndex"] for item in options]
            coverage.append({"scene": scene, "hand": hand, "view": view,
                             "capturedSampleIndices": indices,
                             "uncapturedLoopIndices": [index for index in range(9) if index not in indices],
                             "availableSamplePositions": positions, "selectedFrameLabels": chosen,
                             "missingSamplePositions": [] if options else list(SAMPLE_POSITIONS),
                             "omittedForBudgetSamplePositions": omitted})
    return selected, coverage


def curate(root, screenshots, marker, output, identity, budget=BUDGET, *, suite="masters"):
    entrypoint = suite_name(suite)
    if not MANIFEST_RESERVE <= budget <= BUDGET:
        raise EvidenceError(f"Budget must be between {MANIFEST_RESERVE} and {BUDGET} bytes")
    destination = safe_path(root, output)
    source = safe_path(root, screenshots)
    if destination.exists():
        raise EvidenceError("Output already exists; refusing stale or mixed-run artifacts")
    if destination.is_relative_to(source) or source.is_relative_to(destination):
        raise EvidenceError("Source and output directories must be separate")
    stamp = json.loads(safe_path(root, marker).read_text(encoding="utf-8"))
    if (stamp.get("schemaVersion") != 1 or stamp.get("provenance") != identity
            or stamp.get("suiteGroup") != suite or stamp.get("suite") != entrypoint
            or stamp.get("screenshots") != Path(screenshots).as_posix()
            or type(stamp.get("startedNs")) is not int
            or not isinstance(stamp.get("preexistingPngs"), list)
            or any(not isinstance(item, str) for item in stamp["preexistingPngs"])):
        raise EvidenceError("Freshness marker does not match this run, suite and screenshot root")
    old_paths = set(stamp["preexistingPngs"])
    groups = {}
    stale = ignored = 0
    names = set()
    for path in source_files(root, screenshots):
        info = describe(path.name, suite=suite)
        if info is None:
            ignored += 1
            continue
        relative = path.relative_to(root).as_posix()
        if relative in old_paths or path.stat().st_mtime_ns < stamp["startedNs"]:
            stale += 1
            continue
        if path.name in names:
            raise EvidenceError(f"Ambiguous duplicate screenshot name: {path.name}")
        names.add(path.name)
        # Oversized candidates remain visible as captured but cannot be selected.
        info.update(sourcePath=relative, artifactPath="frames/" + path.name, bytes=path.stat().st_size)
        groups.setdefault((info["scene"], info["view"], info["phase"]), []).append(info)
    selector = select_articulated if suite == "articulated" else select_masters
    selected, coverage = selector(groups, budget)
    manifest = {
        "schemaVersion": 1, "artifactKind": "curated_native_visual_evidence_only",
        "status": "available" if selected else "unavailable", "provenance": identity,
        "suiteGroup": suite, "suite": entrypoint, "sourceRoot": Path(screenshots).as_posix(),
        "freshness": {"method": "pre_run_marker; reject all preexisting paths and older mtimes",
                      "excludedStalePngs": stale, "ignoredOutOfScopePngs": ignored},
        "limits": {"totalBytesLimit": budget, "manifestReserveBytes": MANIFEST_RESERVE,
                   "archiveHeadroomBelow15MB": 15_000_000 - budget},
        "basis": "Byte-identical source PNGs from local-owner singleplayer captures. "
                 "First-person and front third-person are the same owning client, never an observer. "
                 "windup/settled are source filename labels; frame_N is only a timeline sample, "
                 "not an exact impact or phase boundary. Presence is not a gameplay pass.",
        "testVerdict": {"establishedByThisArtifact": False,
                        "authoritativeArtifact": f"{suite}-native-evidence",
                        "authoritativeManifest": f"{suite}-native-manifest.json",
                        "note": "Existing native test manifest and all suite/shard gates are unchanged."},
        "unavailableRequestedCoverage": [
            {"coverage": "observer_client", "reason": "This suite has no observer-client screenshot capture."},
            {"coverage": "live_master_ember_combat", "reason": "EmberAfterburnChecks has no screenshot calls. "
             "MasterModelPresentationTest is synthetic model inspection, not live Master Ember combat."},
            {"coverage": "exact_impact_phase", "reason": "Capture loop indices do not prove exact impact timing."},
        ],
        "selectionPolicy": "Stable scene order; owner first-person then local-owner front third-person; "
                           "windup, median available frame index, settled; skip frames exceeding remaining budget. "
                           "Only implemented first/second-form names and shared arts are eligible. "
                           "Left-turn, cancelled, help, synthetic model and other screenshots remain in the full artifact.",
        "selectedPngBytes": sum(frame["bytes"] for frame in selected),
        "selectedFrameCount": len(selected), "coverage": coverage, "frames": selected,
    }
    if suite == "articulated":
        manifest.update({
            "basis": "Byte-identical source PNGs from real Spellcut input and its server-accepted timeline. "
                     "Left/right denote the local owner's configured main hand; first-person and front "
                     "third-person share that owning singleplayer client, never an observer. Screenshots "
                     "exist only when the articulated body backend owns the sampled state. Loop indices "
                     "and first/middle/last available samples prove neither exact impact nor recovery "
                     "boundaries. Presence is not a gameplay pass.",
            "unavailableRequestedCoverage": [
                {"coverage": "observer_client", "reason": "This suite has no observer-client screenshot capture."},
                {"coverage": "live_npc_combat", "reason": "The Master renderer bridge is probed synthetically; "
                 "there is no live NPC attack screenshot capture."},
                {"coverage": "synthetic_geometry_and_hitstop_screenshots", "reason": "Synthetic geometry, "
                 "socket and hit-stop checks do not take screenshots; inspect the independent native verdict."},
                {"coverage": "exact_impact_phase", "reason": "Capture loop indices do not prove exact impact timing."},
                {"coverage": "exact_recovery_phase", "reason": "Last available capture is not a verified recovery boundary."},
            ],
            "selectionPolicy": "Left then right main hand; owner first-person then local-owner front third-person; "
                               "first, middle (upper median), and last available frame index from 0..8. "
                               "Copy each original only once even if it serves multiple sample positions; "
                               "skip candidates exceeding remaining budget. Uncaptured loop indices are "
                               "reported without implying every index must be captured. Other screenshots "
                               "remain in the full artifact.",
        })
    if not selected:
        manifest["unavailableReason"] = "No fresh eligible native PNG fits the budget; inspect coverage and full evidence."
    destination.parent.mkdir(parents=True, exist_ok=True)
    # Publish only a complete, verified directory. Never reuse old output files.
    with tempfile.TemporaryDirectory(prefix=f".{suite}-curation-", dir=destination.parent) as temp:
        staging = Path(temp) / "artifact"
        (staging / "frames").mkdir(parents=True)
        for frame in selected:
            path = safe_path(root, frame["sourcePath"])
            before = path.stat()
            if before.st_size != frame["bytes"] or before.st_mtime_ns < stamp["startedNs"]:
                raise EvidenceError("Source changed during curation")
            checksum = digest(path)
            copied = staging / frame["artifactPath"]
            shutil.copyfile(path, copied)
            after = safe_path(root, frame["sourcePath"]).stat()
            if (not unchanged(before, after) or copied.stat().st_size != frame["bytes"] or digest(copied) != checksum):
                raise EvidenceError("Source changed while being copied")
            frame["sha256"] = checksum
        data = encode(manifest)
        if len(data) > MANIFEST_RESERVE or sum(item["bytes"] for item in selected) + len(data) > budget:
            raise EvidenceError("Final artifact exceeds its byte budget")
        (staging / "manifest.json").write_bytes(data)
        # Explicit second check prevents accidental merging with an existing output.
        if destination.exists():
            raise EvidenceError("Output appeared during curation")
        staging.rename(destination)
    return manifest


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--suite", choices=tuple(SUITES), default="masters")
    parser.add_argument("--screenshots", default="build/run/clientGameTest/screenshots")
    parser.add_argument("--marker", required=True, help="Unique repository-relative pre-run marker path")
    parser.add_argument("--output", help="New repository-relative curated artifact directory")
    parser.add_argument("--budget", type=int, default=BUDGET)
    args = parser.parse_args(argv)
    if not args.prepare and not args.output:
        parser.error("--output is required when curating")
    try:
        identity = provenance(ROOT, os.environ)
        if args.prepare:
            prepare(ROOT, args.screenshots, args.marker, identity, suite=args.suite)
        else:
            result = curate(ROOT, args.screenshots, args.marker, args.output, identity, args.budget, suite=args.suite)
            print(json.dumps({"output": args.output, "status": result["status"], "frames": len(result["frames"])}))
    except (EvidenceError, OSError, ValueError, subprocess.SubprocessError) as exc:
        print(f"Visual evidence unavailable: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
