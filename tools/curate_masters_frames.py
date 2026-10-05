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
ARTICULATED_MANIFEST_RESERVE = 256_000
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
SUITE = "dev.wildercord.gametest.WildercordMastersArtsPresentationTest"
ARTICULATED_SUITE = "dev.wildercord.client.combat.ArticulatedCombatPresentationTest"
ARTICULATED_ARMOR_SUITE = "dev.wildercord.client.combat.ArticulatedArmorPresentationTest"
ARTICULATED_HUD_SUITE = "dev.wildercord.client.combat.ArticulatedFirstPersonCompositionTest"
ARTICULATED_SOURCE_SUITES = (ARTICULATED_SUITE, ARTICULATED_ARMOR_SUITE, ARTICULATED_HUD_SUITE)
SUITES = {"masters": SUITE, "articulated": ARTICULATED_SUITE}
ARTICULATED_HANDS = ("left", "right")
SAMPLE_POSITIONS = ("first_available", "middle_available", "last_available")
ARTICULATED_PATTERN = re.compile(r"articulated_live_(left|right)_(first|third)_frame_([0-8])\.png\Z")
ARTICULATED_ARMOR_PATTERN = re.compile(
    r"articulated_armor_live_(full|partial)_(left|right)_(first|third_front|third_back)_frame_([25])\.png\Z")
# Exact configurations in ArticulatedFirstPersonCompositionTest, not arbitrary resolutions/scales.
ARTICULATED_VIEWPORTS = ((854, 480, 2), (1280, 720, 3), (1280, 960, 4), (1920, 810, 3))
HUD_REFERENCE_VIEWPORT = (1280, 720, 3)
ARTICULATED_HUD_PATTERN = re.compile(
    r"articulated_hud_(" + "|".join(f"{w}x{h}_gui{s}" for w, h, s in ARTICULATED_VIEWPORTS)
    + r")_(netherite|skin)_(left|right)_(idle_before|sample_([0-9])|idle_after)\.png\Z")
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
VIEWS = {"first": "owner_first_person", "third_back": "local_owner_third_person_back"}
ARTICULATED_VIEWS = {"first": "owner_first_person", "third": "local_owner_third_person_front"}
ARTICULATED_ARMOR_VIEWS = {"first": "owner_first_person", "third_front": "local_owner_third_person_front",
                          "third_back": "local_owner_third_person_back"}
PHASES = ("windup", "active", "recovery", "settled")
PATTERN = re.compile(r"(masters_art_[012]|masters_style_(?:"
                     + "|".join(FIRST_FORMS + SECOND_FORMS)
                     + r"))_(first|third_back|third)_(windup|active|recovery|settled|frame_([0-9]+))\.png\Z")


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
                             **({"sourceSuites": list(ARTICULATED_SOURCE_SUITES)} if suite == "articulated" else {}),
                             "screenshots": Path(screenshots).as_posix(),
                             "startedNs": time.time_ns(), "preexistingPngs": prior}))


def describe_articulated(filename):
    common = {"mainHandItem": "diamond_sword", "offHandItem": "empty", "viewport": None,
              "uiScale": None, "hudVisible": None, "viewportBasis": "not_encoded_in_filename",
              "armorEnchantment": None, "armorTrim": None}
    match = ARTICULATED_PATTERN.fullmatch(filename)
    if match:
        hand, view, frame = match.groups()
        return {**common, "sourceSuite": ARTICULATED_SUITE, "equipment": "skin",
                "scene": "articulated_live_" + hand, "hand": hand, "view": ARTICULATED_VIEWS[view],
                "captureKind": "native_local_owner_accepted_input_combat",
                "frameLabel": "frame_" + frame, "phase": "sample", "sampleIndex": int(frame),
                "phaseBasis": "filename_loop_index_only"}
    match = ARTICULATED_ARMOR_PATTERN.fullmatch(filename)
    if match:
        equipment, hand, view, frame = match.groups()
        return {**common, "sourceSuite": ARTICULATED_ARMOR_SUITE,
                "equipment": "netherite_full" if equipment == "full" else "netherite_chestplate_and_leggings",
                "armorEnchantment": "protection_iv", "armorTrim": "gold_sentry",
                "scene": f"articulated_armor_live_{equipment}_{hand}", "hand": hand,
                "view": ARTICULATED_ARMOR_VIEWS[view],
                "captureKind": "native_local_owner_accepted_input_combat",
                "frameLabel": "frame_" + frame, "phase": "sample", "sampleIndex": int(frame),
                "phaseBasis": "filename_loop_index_only"}
    match = ARTICULATED_HUD_PATTERN.fullmatch(filename)
    if match:
        viewport, equipment, hand, label, sample = match.groups()
        width, height, scale = next(config for config in ARTICULATED_VIEWPORTS
                                    if f"{config[0]}x{config[1]}_gui{config[2]}" == viewport)
        return {**common, "sourceSuite": ARTICULATED_HUD_SUITE,
                "equipment": "netherite_chestplate" if equipment == "netherite" else "skin",
                "armorEnchantment": "protection_iv" if equipment == "netherite" else None,
                "scene": f"articulated_hud_{viewport}_{equipment}_{hand}", "hand": hand,
                "view": "owner_first_person", "viewport": {"width": width, "height": height},
                "uiScale": scale, "hudVisible": True, "viewportBasis": "native_fixture_assertion",
                "captureKind": "native_local_owner_accepted_input_combat" if sample is not None else "native_local_owner_idle",
                "frameLabel": label, "phase": "sample" if sample is not None else label,
                "sampleIndex": int(sample) if sample is not None else None,
                "phaseBasis": "filename_loop_index_only" if sample is not None else "explicit_filename_label"}
    return None


def describe(filename, *, suite="masters"):
    suite_name(suite)
    if suite == "articulated":
        return describe_articulated(filename)
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
    return {"scene": scene, "form": form, "view": ({**ARTICULATED_VIEWS, **VIEWS})[view],
            "captureKind": "native_local_owner_combat", "frameLabel": label,
            "phase": "sample" if frame is not None else label,
            "sampleIndex": int(frame) if frame is not None else None,
            "phaseBasis": "filename_loop_index_only" if frame is not None else
                "accepted_timeline_window_assertion" if label in ("active", "recovery") else "explicit_filename_label"}


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
    # Complete per-view phase sets first, in stable scene/view order. If a set
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
                candidate = options[0]
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


def articulated_capture_matrix():
    """Name only configurations that the three existing native suites can capture."""
    for hand in ARTICULATED_HANDS:
        for view in ARTICULATED_VIEWS:
            yield f"articulated_live_{hand}_{view}_frame_0.png", range(9), False
    for equipment in ("full", "partial"):
        for hand in ARTICULATED_HANDS:
            for view in ARTICULATED_ARMOR_VIEWS:
                yield f"articulated_armor_live_{equipment}_{hand}_{view}_frame_2.png", (2, 5), False
    for width, height, scale in ARTICULATED_VIEWPORTS:
        for equipment in ("skin", "netherite"):
            for hand in ARTICULATED_HANDS:
                yield f"articulated_hud_{width}x{height}_gui{scale}_{equipment}_{hand}_sample_0.png", range(10), True


def select_articulated(groups, budget):
    remaining = budget - ARTICULATED_MANIFEST_RESERVE
    rows = []
    for filename, expected_indices, has_idle in articulated_capture_matrix():
        info = describe_articulated(filename)
        scene, view = info["scene"], info["view"]
        options = sorted(groups.get((scene, view, "sample"), []),
                         key=lambda item: (item["sampleIndex"], item["sourcePath"]))
        positions = {}
        candidates = {}
        # Several available-sample positions can describe the same authentic PNG.
        if options:
            for position, candidate in zip(SAMPLE_POSITIONS,
                                           (options[0], options[len(options) // 2], options[-1])):
                positions[position] = candidate["frameLabel"]
                candidates.setdefault(candidate["frameLabel"], {**candidate, "samplePositions": []})["samplePositions"].append(position)
        idle_labels = ("idle_before", "idle_after") if has_idle else ()
        idle = {label: groups.get((scene, view, label), []) for label in idle_labels}
        for label, frames in idle.items():
            if frames:
                candidates[label] = {**frames[0], "samplePositions": []}
        indices = [item["sampleIndex"] for item in options]
        coverage = {key: info[key] for key in ("scene", "hand", "view", "sourceSuite", "equipment",
                                              "armorEnchantment", "armorTrim", "viewport", "uiScale",
                                              "hudVisible", "viewportBasis")}
        coverage.update(capturedSampleIndices=indices,
                        uncapturedLoopIndices=[index for index in expected_indices if index not in indices],
                        availableSamplePositions=positions, selectedFrameLabels=[],
                        missingSamplePositions=[] if options else list(SAMPLE_POSITIONS),
                        omittedForBudgetSamplePositions=[],
                        capturedIdleLabels=[label for label in idle_labels if idle[label]],
                        missingIdleLabels=[label for label in idle_labels if not idle[label]],
                        omittedForBudgetIdleLabels=[])
        representative = positions.get("middle_available" if has_idle else "first_available")
        rows.append({"info": info, "coverage": coverage, "candidates": candidates,
                     "representative": candidates.get(representative)})

    selected_paths = set()

    def choose(candidates):
        nonlocal remaining
        available = {frame["sourcePath"]: frame for frame in candidates
                     if frame is not None and frame["sourcePath"] not in selected_paths}
        cost = sum(frame["bytes"] for frame in available.values())
        if cost <= remaining:
            selected_paths.update(available)
            remaining -= cost

    def matches_viewport(row, viewport):
        width, height, scale = viewport
        return (row["info"]["sourceSuite"] == ARTICULATED_HUD_SUITE
                and row["info"]["viewport"] == {"width": width, "height": height}
                and row["info"]["uiScale"] == scale)

    # Prefer a complete skin/chestplate combat comparison at the reference size.
    # If captures are absent there, use an available exact fixture configuration
    # before spending the budget on armor breadth. Never invent its counterpart.
    viewport_order = (HUD_REFERENCE_VIEWPORT, *(viewport for viewport in ARTICULATED_VIEWPORTS
                                               if viewport != HUD_REFERENCE_VIEWPORT))
    primary_hud = []
    for hand in ARTICULATED_HANDS:
        pairs = [[row for row in rows if row["info"]["hand"] == hand and matches_viewport(row, viewport)]
                 for viewport in viewport_order]
        primary_hud.extend(max(pairs, key=lambda pair: (
            sum(row["representative"] is not None for row in pair),
            sum(bool(row["candidates"]) for row in pair))))

    # Establish original body/hand, full-armor front/back, and skin/armor HUD
    # comparisons before extra temporal samples, partial sets or other viewports.
    # A present comparison pair is charged together so budget cannot split it.
    core = []
    core_pairs = []
    for hand in ARTICULATED_HANDS:
        for source_suite in ARTICULATED_SOURCE_SUITES:
            pair = [row for row in rows if row["info"]["hand"] == hand
                    and row["info"]["sourceSuite"] == source_suite
                    and (source_suite == ARTICULATED_SUITE
                         or source_suite == ARTICULATED_ARMOR_SUITE
                         and row["info"]["equipment"] == "netherite_full"
                         and row["info"]["view"] != "owner_first_person"
                         or row in primary_hud)]
            core.extend(pair)
            core_pairs.append(pair)
            choose(row["representative"] for row in pair)
    # Add the chosen viewport's idle-before/after skin/armor comparisons first.
    for hand in ARTICULATED_HANDS:
        for label in ("idle_before", "idle_after"):
            choose(row["candidates"].get(label) for row in core
                   if row in primary_hud and row["info"]["hand"] == hand)
    # Broaden time samples and then configurations in stable matrix order. Do not
    # retry a skipped representative alone and silently split a budget-limited pair.
    for pair in core_pairs:
        if any(row["representative"] is not None
               and row["representative"]["sourcePath"] not in selected_paths for row in pair):
            continue
        for row in pair:
            for candidate in row["candidates"].values():
                if candidate is not row["representative"] and candidate["phase"] == "sample":
                    choose([candidate])
    for row in rows:
        if row not in core:
            for candidate in row["candidates"].values():
                choose([candidate])

    selected = []
    for row in rows:
        for frame in row["candidates"].values():
            if frame["sourcePath"] in selected_paths:
                selected.append(frame)
                row["coverage"]["selectedFrameLabels"].append(frame["frameLabel"])
            elif frame["phase"] == "sample":
                row["coverage"]["omittedForBudgetSamplePositions"].extend(frame["samplePositions"])
            else:
                row["coverage"]["omittedForBudgetIdleLabels"].append(frame["frameLabel"])
    return selected, [row["coverage"] for row in rows]


def curate(root, screenshots, marker, output, identity, budget=BUDGET, *, suite="masters"):
    entrypoint = suite_name(suite)
    manifest_reserve = ARTICULATED_MANIFEST_RESERVE if suite == "articulated" else MANIFEST_RESERVE
    if not manifest_reserve <= budget <= BUDGET:
        raise EvidenceError(f"Budget must be between {manifest_reserve} and {BUDGET} bytes")
    destination = safe_path(root, output)
    source = safe_path(root, screenshots)
    if destination.exists():
        raise EvidenceError("Output already exists; refusing stale or mixed-run artifacts")
    if destination.is_relative_to(source) or source.is_relative_to(destination):
        raise EvidenceError("Source and output directories must be separate")
    stamp = json.loads(safe_path(root, marker).read_text(encoding="utf-8"))
    if (stamp.get("schemaVersion") != 1 or stamp.get("provenance") != identity
            or stamp.get("suiteGroup") != suite or stamp.get("suite") != entrypoint
            or (suite == "articulated" and stamp.get("sourceSuites") != list(ARTICULATED_SOURCE_SUITES))
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
        "limits": {"totalBytesLimit": budget, "manifestReserveBytes": manifest_reserve,
                   "archiveHeadroomBelow15MB": 15_000_000 - budget},
        "basis": "Byte-identical source PNGs from local-owner singleplayer captures. "
                 "First-person and back third-person are the same owning client, never an observer. "
                 "Windup, active and recovery filenames are checked against the accepted timeline by the native suite; "
                 "active is its release window, not proof of the exact rendered server impact. Presence is not a gameplay pass.",
        "testVerdict": {"establishedByThisArtifact": False,
                        "authoritativeArtifact": f"{suite}-native-evidence",
                        "authoritativeManifest": f"{suite}-native-manifest.json",
                        "note": "Existing native test manifest and all suite/shard gates are unchanged."},
        "unavailableRequestedCoverage": [
            {"coverage": "observer_client", "reason": "This suite has no observer-client screenshot capture."},
            {"coverage": "live_master_ember_combat", "reason": "EmberAfterburnChecks has no screenshot calls. "
             "MasterModelPresentationTest is synthetic model inspection, not live Master Ember combat."},
            {"coverage": "exact_impact_phase", "reason": "The accepted timeline release window does not prove the exact rendered server impact."},
        ],
        "selectionPolicy": "Stable scene order; owner first-person then local-owner back third-person; "
                           "windup, active release window, recovery, settled; skip frames exceeding remaining budget. "
                           "Only implemented first/second-form names and shared arts are eligible. "
                           "Left-turn, cancelled, help, synthetic model and other screenshots remain in the full artifact.",
        "selectedPngBytes": sum(frame["bytes"] for frame in selected),
        "selectedFrameCount": len(selected), "coverage": coverage, "frames": selected,
    }
    if suite == "articulated":
        manifest.update({
            "sourceSuites": list(ARTICULATED_SOURCE_SUITES),
            "basis": "Byte-identical native PNGs from the three existing articulated suites. Combat samples "
                     "follow real Spellcut input and its server-accepted timeline; HUD idle-before/after "
                     "captures are explicitly idle. Left/right denote the local owner's configured main hand; "
                     "all first-person and front/back third-person views share that owning singleplayer client, "
                     "never an observer. Equipment and HUD viewport labels describe native fixture assertions, "
                     "not image analysis; unencoded viewport/UI settings remain unknown. Loop indices and "
                     "first/middle/last available samples prove neither exact impact nor recovery boundaries; "
                     "skin/armor samples are not guaranteed to depict the same animation age. "
                     "Presence is not a gameplay or native pixel-review pass.",
            "unavailableRequestedCoverage": [
                {"coverage": "observer_client", "reason": "This suite has no observer-client screenshot capture."},
                {"coverage": "live_npc_combat", "reason": "The Master renderer bridge is probed synthetically; "
                 "there is no live NPC attack screenshot capture."},
                {"coverage": "synthetic_geometry_and_hitstop_screenshots", "reason": "Synthetic geometry, "
                 "socket and hit-stop checks do not take screenshots; inspect the independent native verdict."},
                {"coverage": "exact_impact_phase", "reason": "Capture loop indices do not prove exact impact timing."},
                {"coverage": "exact_recovery_phase", "reason": "Last available capture is not a verified recovery boundary."},
            ],
            "selectionPolicy": "Left then right: original body/hand first-available pair, full enchanted armor "
                               "front/back first-available pair, 1280x720/gui3 HUD skin/chestplate upper-median pair. "
                               "Prefer the HUD viewport with the most available counterparts, breaking ties "
                               "by reference size then fixture order; missing reference captures use this fallback. "
                               "Each core/idle comparison pair is budgeted together; missing counterparts remain missing. "
                               "Then chosen HUD idle-before/after pairs, core temporal samples, full-armor "
                               "first-person, partial armor, and other exact fixture viewports in matrix order. "
                               "Only first/upper-median/last available combat samples and explicit HUD idle "
                               "captures are copied, each once. Armor capture indices are only 2 and 5; "
                               "original samples are 0..8 and HUD samples 0..9. Uncaptured indices are not "
                               "required captures. The full evidence artifact and native verdicts are unchanged.",
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
        if len(data) > manifest_reserve or sum(item["bytes"] for item in selected) + len(data) > budget:
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
