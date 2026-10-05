"""Copy a bounded, byte-identical subset of this run's native suite screenshots.

Run --prepare before the native suites and run again afterwards with the same
--marker and --suite (Masters by default). This report is visual evidence only:
test_manifest.py owns gate status.
No images are rendered, resized, decoded, or re-encoded here.
"""
import argparse
import hashlib
import json
import math
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
# Includes all legacy/NPC and 120 shared-player capture records and omissions.
ARTICULATED_MANIFEST_RESERVE = 640_000
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
SUITE = "dev.wildercord.gametest.WildercordMastersArtsPresentationTest"
NPC_SUITE = "dev.wildercord.client.auraworld.MasterModelPresentationTest"
NPC_VIEW = "real_client_spectator_observing_npc"
NPC_METADATA_LIMIT = 64_000
NPC_FAILURE_REASON_LIMIT = 240
# Exact beats emitted by MasterSchoolMotionChecks, each in a fresh natural trial.
NPC_BEATS = {
    "masters_npc_gale_crosswind": {"gather": 4, "step": 9, "reply_warning": 16, "release": 22, "recovery": 38},
    "masters_npc_stone_fracture": {"plant": 4, "brace": 12, "reply_warning": 25, "release": 32, "recovery": 52},
}
NPC_PRIORITY = ("reply_warning", "release", "recovery")
ARTICULATED_SUITE = "dev.wildercord.client.combat.ArticulatedCombatPresentationTest"
ARTICULATED_ARMOR_SUITE = "dev.wildercord.client.combat.ArticulatedArmorPresentationTest"
ARTICULATED_HUD_SUITE = "dev.wildercord.client.combat.ArticulatedFirstPersonCompositionTest"
ARTICULATED_SOURCE_SUITES = (ARTICULATED_SUITE, ARTICULATED_ARMOR_SUITE, ARTICULATED_HUD_SUITE)
ARTICULATED_NPC_BEATS = {
    "articulated_npc_gale_crosswind": {"gather": 4, "step": 9, "step_last": 11, "landed": 12,
        "settle_13": 13, "settle_14": 14, "settle_15": 15, "reply_warning": 16, "release": 22, "recovery": 38},
    "articulated_npc_stone_fracture": dict(NPC_BEATS["masters_npc_stone_fracture"]),
}
NPC_SEGMENTED_JOINTS = (
    "PELVIS", "SPINE", "CHEST", "HEAD", "RIGHT_SHOULDER", "RIGHT_UPPER_ARM", "RIGHT_FOREARM", "RIGHT_HAND", "RIGHT_SOCKET",
    "LEFT_SHOULDER", "LEFT_UPPER_ARM", "LEFT_FOREARM", "LEFT_HAND", "LEFT_SOCKET",
    "RIGHT_THIGH", "RIGHT_SHIN", "RIGHT_FOOT", "LEFT_THIGH", "LEFT_SHIN", "LEFT_FOOT",
)
NPC_RIGID_PARTS = ("body", "head", "right_arm", "left_arm", "right_leg", "left_leg")
NPC_BODY_BOUNDS_SOURCE = "visible_native_model_cube_vertices_with_body_submit_matrix"
NPC_BLADE_BOUNDS_SOURCE = "resolved_native_item_extents_with_hand_receipt_and_vanilla_adult_offsets"
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
SHARED_PLAYER_ARTS = ("rising_break", "driving_cut")
SHARED_REQUESTED_PHASES = ("windup", "active", "recovery")
SHARED_PHASE_PRIORITY = ("active", "windup", "recovery")
SHARED_PLAYER_PATTERN = re.compile(
    r"articulated_shared_(rising_break|driving_cut)_(third|hud)_("
    + "|".join(f"{w}x{h}_gui{s}" for w, h, s in ARTICULATED_VIEWPORTS)
    + r")_(skin|netherite)_(left|right)_requested_(windup|active|recovery)\.png\Z")
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


def source_files(root, relative, suffixes=(".png",)):
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
            if stat.S_ISREG(mode) and path.suffix.lower() in suffixes:
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
    prior = source_files(root, screenshots, (".png", ".json"))
    marker_path.parent.mkdir(parents=True, exist_ok=True)
    # Never overwrite a marker from a prior attempt.
    with marker_path.open("xb") as handle:
        handle.write(encode({"schemaVersion": 1, "provenance": identity,
                             "suiteGroup": suite, "suite": entrypoint,
                             **({"sourceSuites": list(ARTICULATED_SOURCE_SUITES)} if suite == "articulated" else {}),
                             "screenshots": Path(screenshots).as_posix(),
                             "startedNs": time.time_ns(),
                             "preexistingPngs": [path.relative_to(root).as_posix() for path in prior
                                                 if path.suffix.lower() == ".png"],
                             "preexistingNpcMetadata": [path.relative_to(root).as_posix() for path in prior
                                                        if path.suffix.lower() == ".json"]}))


def describe_npc(filename, beats, source_suite):
    for scene, phases in beats.items():
        for phase in phases:
            if filename == f"{scene}_{phase}.png":
                return {"scene": scene, "form": "npc_school_form", "sourceSuite": source_suite,
                        "view": NPC_VIEW, "captureKind": "native_unpaused_server_ai",
                        "frameLabel": phase, "phase": phase, "sampleIndex": None,
                        "phaseBasis": "native_render_and_server_metadata_assertion"}
    return None


def describe_articulated(filename):
    npc = describe_npc(filename, ARTICULATED_NPC_BEATS, ARTICULATED_SUITE)
    if npc is not None:
        return npc
    common = {"mainHandItem": "diamond_sword", "offHandItem": "empty", "viewport": None,
              "uiScale": None, "hudVisible": None, "viewportBasis": "not_encoded_in_filename",
              "armorEnchantment": None, "armorTrim": None}
    match = SHARED_PLAYER_PATTERN.fullmatch(filename)
    if match:
        art, camera, viewport, equipment, hand, requested_phase = match.groups()
        width, height, scale = next(config for config in ARTICULATED_VIEWPORTS
                                    if f"{config[0]}x{config[1]}_gui{config[2]}" == viewport)
        if camera == "third" and (width, height, scale) != HUD_REFERENCE_VIEWPORT:
            return None
        return {**common, "sourceSuite": ARTICULATED_SUITE if camera == "third" else ARTICULATED_HUD_SUITE,
                "form": "shared_player_art", "art": art, "camera": camera,
                "scene": f"articulated_shared_{art}_{camera}_{viewport}_{equipment}_{hand}",
                "hand": hand, "view": "local_owner_third_person_front" if camera == "third" else "owner_first_person",
                "equipment": "skin" if equipment == "skin" else
                    "netherite_full" if camera == "third" else "netherite_chestplate",
                "armorEnchantment": "protection_iv" if equipment == "netherite" else None,
                "equipmentBasis": "filename_and_native_fixture_contract",
                "heldItemBasis": "native_fixture_contract",
                "viewport": {"width": width, "height": height}, "uiScale": scale,
                "viewportBasis": "filename_only", "hudVisible": True, "hudBasis": "native_fixture_contract",
                "captureKind": "native_local_owner_accepted_input_combat",
                "frameLabel": "requested_" + requested_phase, "requestedPhase": requested_phase,
                "requestedPhaseBasis": "filename_only", "sampleIndex": None,
                "phase": "unknown", "renderedPhase": "unknown", "phaseBasis": "rendered_phase_unverified",
                "nativePixelReviewRequired": True, "exactImpactPixelCoverage": "unverified",
                "preCaptureReceipt": {"status": "not_ingested", "acceptedMove": None, "activation": None,
                    "windup": None, "recovery": None, "actualSkin": None, "preCaptureAge": None,
                    "preCapturePhase": "unknown", "sourceArtifact": "articulated-native-evidence",
                    "logRecord": "ARTICULATED_SHARED_SAMPLE name=" + filename.removesuffix(".png")}}
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
    npc = describe_npc(filename, NPC_BEATS, NPC_SUITE)
    if npc is not None:
        return npc
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


def read_npc_metadata(root, path, info, stamp):
    """Bind exact native render metadata to its fresh PNG; never infer it from a name alone."""
    relative = path.with_suffix(".json").relative_to(root).as_posix()
    sidecar = safe_path(root, relative)
    if not sidecar.is_file():
        raise EvidenceError(f"NPC capture lacks its metadata: {path.name}")
    before = sidecar.stat()
    if relative in stamp["preexistingNpcMetadata"] or before.st_mtime_ns < stamp["startedNs"]:
        return None
    if not 0 < before.st_size <= NPC_METADATA_LIMIT:
        raise EvidenceError(f"NPC metadata exceeds its bounded size: {sidecar.name}")
    with sidecar.open("rb") as handle:
        raw = handle.read(NPC_METADATA_LIMIT + 1)
    if len(raw) != before.st_size or not unchanged(before, sidecar.stat()):
        raise EvidenceError("NPC metadata changed while being read")

    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise EvidenceError(f"Duplicate NPC metadata key: {key}")
            result[key] = value
        return result

    def finite(value):
        return type(value) is int or type(value) is float and math.isfinite(value)

    def vector(value):
        return isinstance(value, dict) and all(finite(value.get(axis)) for axis in ("x", "y", "z"))

    def point(value):
        return isinstance(value, dict) and all(finite(value.get(axis)) and 0 < value[axis] < 1 for axis in ("x", "y"))

    def require(ok):
        if not ok:
            raise EvidenceError(f"NPC metadata does not match the native fixture: {sidecar.name}")

    def exclude(reason_code, reason):
        return {"excludedNpcCapture": {
            "sourcePath": path.relative_to(root).as_posix(), "metadataSourcePath": relative,
            "metadataSha256": hashlib.sha256(raw).hexdigest(), "scene": scene, "phase": phase,
            "reasonCode": reason_code, "reason": " ".join(reason.split())[:NPC_FAILURE_REASON_LIMIT]}}

    def invalid_constant(value):
        raise EvidenceError(f"Non-finite NPC metadata value: {value}")

    def finite_tree(value):
        if isinstance(value, dict):
            return all(finite_tree(item) for item in value.values())
        if isinstance(value, list):
            return all(finite_tree(item) for item in value)
        return type(value) is not float or math.isfinite(value)

    def pose_schema(pose):
        return (isinstance(pose, dict)
                and all(vector(pose.get(joint)) for joint in ("body", "head", "sword", "offhand"))
                and all(finite(pose.get(key)) for key in ("weight", "stance", "bladeTilt")))

    def numbers(value, size):
        return isinstance(value, list) and len(value) == size and all(finite(item) for item in value)

    def framing_metadata(value, failed):
        if not articulated:
            # Old fallback captures predate these additive fields. New fallback
            # records still cannot opt into close/partial warning semantics.
            if any(key in value for key in ("framing", "fov", "warningCoverage")):
                require(value.get("framing") == "legacy_wide" and type(value.get("fov")) is int
                        and value["fov"] == 60 and value.get("warningCoverage") == "full_lane")
            if "warningValidation" in value:
                require(value["warningValidation"] == "full_lane_required")
            return
        require(value.get("framing") == ("body_close" if close_body else "warning_lane")
                and type(value.get("fov")) is int and value["fov"] == (50 if close_body else 60)
                and value.get("warningCoverage") == ("visible_portion" if close_body else "full_lane")
                and value.get("bodyBoundsSource") == NPC_BODY_BOUNDS_SOURCE
                and value.get("bladeBoundsSource") == NPC_BLADE_BOUNDS_SOURCE)
        # Unfinalized legacy records may predate this field, but can never be
        # accepted. A finalized receipt must state its warning contract exactly.
        if "captureStatus" in value or "warningValidation" in value:
            require(value.get("warningValidation") == ("diagnostic_only" if close_body else "full_lane_required"))
        for key in ("bodyBounds", "bladeBounds"):
            bounds = value.get(key)
            require(isinstance(bounds, dict) and all(finite(bounds.get(field)) for field in ("minX", "minY", "maxX", "maxY"))
                    and bounds["minX"] <= bounds["maxX"] and bounds["minY"] <= bounds["maxY"]
                    and type(bounds.get("vertexCount")) is int and bounds["vertexCount"] >= 0
                    and type(bounds.get("allInFront")) is bool and type(bounds.get("wholeVisible")) is bool)
            enclosed = (bounds["vertexCount"] > 0 and bounds["allInFront"]
                        and bounds["minX"] > .03 and bounds["minY"] > .03
                        and bounds["maxX"] < .97 and bounds["maxY"] < .97)
            require(not bounds["wholeVisible"] or enclosed)
            if not failed:
                require(enclosed and bounds["wholeVisible"])
        for key in ("bodySubmitMatrix", "viewRotationProjectionMatrix"):
            require(failed and value.get(key) is None or numbers(value.get(key), 16))
        if not failed and close_body:
            require((value["bodyBounds"]["maxY"] - value["bodyBounds"]["minY"]) * value["height"] >= 180)

    def warning_schema(warning):
        def endpoint(value):
            if close_body:
                # These projections are intentionally unclamped; Gson omits null
                # endpoints behind the camera or outside the native depth range.
                return value is None or isinstance(value, dict) and all(finite(value.get(axis)) for axis in ("x", "y"))
            return point(value)

        require(isinstance(warning, dict) and endpoint(warning.get("from")) and endpoint(warning.get("to"))
                and type(warning.get("coloredBins")) is int and type(warning.get("sampledBins")) is int
                and 0 <= warning["coloredBins"] <= warning["sampledBins"] <= 16
                and (close_body or warning["sampledBins"] == 16))
        ray = warning.get("ray")
        require(isinstance(ray, dict) and all(vector(ray.get(key)) for key in ("from", "to", "renderedEnd"))
                and all(finite(ray.get(key)) for key in ("width", "partial"))
                and all(type(ray.get(key)) is int for key in ("particleAge", "lifetime")))

    def articulated_receipts(value, failed):
        require(value.get("expectedBackend") == "segmented")
        candidate = value.get("articulatedFrame")
        if candidate is not None:
            require(isinstance(candidate, dict)
                    and all(type(candidate.get(key)) is int for key in ("activation", "move"))
                    and candidate.get("phase") in ("NONE", "WINDUP", "ACTIVE", "RECOVERY")
                    and all(type(candidate.get(key)) is bool for key in ("leftHanded", "scriptedFootwork"))
                    and all(finite(candidate.get(key)) for key in
                            ("weight", "horizontalVelocitySquared", "interpolatedTravelSquared", "walkAnimationSpeed")))
        receipts, hands = value.get("modelReceipts"), value.get("handReceipts")
        require(isinstance(receipts, list) and isinstance(hands, list))
        for receipt in receipts:
            require(isinstance(receipt, dict) and receipt.get("backend") in ("segmented", "rigid")
                    and type(receipt.get("segmentedRootVisible")) is bool
                    and isinstance(receipt.get("rigidPartsVisible"), list) and len(receipt["rigidPartsVisible"]) == 6
                    and all(type(visible) is bool for visible in receipt["rigidPartsVisible"]))
            names = NPC_SEGMENTED_JOINTS if receipt["backend"] == "segmented" else NPC_RIGID_PARTS
            require(receipt.get("transformNames") == list(names) and numbers(receipt.get("transforms"), len(names) * 6))
        for hand in hands:
            require(isinstance(hand, dict) and hand.get("hand") in ("LEFT", "RIGHT")
                    and all(numbers(hand.get(key), 16) for key in
                            ("entryMatrix", "nativeHandMatrix", "resolvedItemMatrix", "expectedSocketItemMatrix"))
                    and all(finite(hand.get(key)) and hand[key] >= 0 for key in ("hiltDistance", "maximumMatrixError")))
        if failed:
            # A failed receipt may describe rigid ownership or a rejected candidate.
            # Keep these as diagnostics; never promote them to accepted footage.
            return
        require(candidate is not None and receipts and hands and len(receipts) == value["modelPasses"])
        timeline = value["renderedTimeline"]
        expected_phase = "ACTIVE" if phase == "release" else "RECOVERY" if phase == "recovery" else "WINDUP"
        require(candidate["activation"] == timeline["acceptedTick"] and candidate["move"] == attack
                and candidate["leftHanded"] == timeline["frame"]["leftHanded"]
                and candidate["phase"] == expected_phase and 0 < candidate["weight"] <= 1
                and candidate["scriptedFootwork"] is (gale and 8 <= timeline["age"] < 12)
                and candidate["horizontalVelocitySquared"] >= 0 and candidate["interpolatedTravelSquared"] >= 0
                and candidate["walkAnimationSpeed"] >= 0)
        require(all(receipt["backend"] == "segmented" and receipt["segmentedRootVisible"] is True
                    and not any(receipt["rigidPartsVisible"]) for receipt in receipts)
                and value["modelPose"] == receipts[-1]["transforms"])
        for hand in hands:
            require(hand["hand"] == ("LEFT" if candidate["leftHanded"] else "RIGHT")
                    and hand["hiltDistance"] < .00001 and hand["maximumMatrixError"] < .00001
                    and max(abs(actual - expected) for actual, expected in
                            zip(hand["resolvedItemMatrix"], hand["expectedSocketItemMatrix"])) < .00001)

    def failed_render_schema(value):
        # Gson omits null extraction fields after an early render assertion.
        # Validate their producer schema when present, without requiring the
        # successful render assertions that this record explicitly failed.
        require(all(type(value.get(key)) is int and value[key] >= 0 for key in ("bodySubmits", "modelPasses")))
        require(vector(value.get("camera")))
        require(value.get("renderedPosition") is None or vector(value["renderedPosition"]))
        model = value.get("modelPose")
        require(model is None or any(numbers(model, size) for size in ((36, 120) if articulated else (36,))))
        timeline = value.get("renderedTimeline")
        if timeline is not None:
            require(isinstance(timeline, dict)
                    and all(type(timeline.get(key)) is int for key in
                            ("clientGameTick", "acceptedTick", "attackId", "tell", "active", "recovery"))
                    and all(finite(timeline.get(key)) for key in ("partial", "age"))
                    and type(timeline.get("fallbackRig")) is bool)
            frame = timeline.get("frame")
            require(frame is None or isinstance(frame, dict)
                    and type(frame.get("move")) is int and type(frame.get("activation")) is int
                    and type(frame.get("leftHanded")) is bool and pose_schema(frame.get("pose")))
        points, pixels, warnings = value.get("bodyPoints"), value.get("bodyPixels"), value.get("warnings")
        require(isinstance(points, list) and len(points) <= 9 and all(point(item) for item in points))
        require(isinstance(pixels, dict) and all(type(pixels.get(key)) is int and pixels[key] >= 0 for key in
                ("nonBlackPixels", "chromaticPixels", "distinctColors", "luminanceRange")))
        require(isinstance(warnings, list))
        for warning in warnings:
            warning_schema(warning)

    evidence = json.loads(raw, object_pairs_hook=unique_object, parse_constant=invalid_constant)
    require(isinstance(evidence, dict) and finite_tree(evidence))
    scene, phase = info["scene"], info["phase"]
    articulated = scene in ARTICULATED_NPC_BEATS
    close_body = articulated and phase != "reply_warning"
    requested = (ARTICULATED_NPC_BEATS if articulated else NPC_BEATS)[scene][phase]
    gale = scene.endswith("gale_crosswind")
    attack, tell, recovery = (7, 22, 31) if gale else (8, 32, 39)
    require(evidence.get("name") == path.stem and evidence.get("phase") == phase
            and evidence.get("captureKind") == "native_unpaused_server_ai"
            and evidence.get("participantKind") == "consenting_fabric_fake_player"
            and evidence.get("observerKind") == "real_client_spectator"
            and evidence.get("independentTrialPerFrame") is True
            and type(evidence.get("requestedTick")) is int and evidence["requestedTick"] == requested)
    failure = evidence.get("renderFailure")
    require(failure is None or isinstance(failure, str) and bool(failure.strip()))
    if articulated and "captureStatus" in evidence:
        require(evidence["captureStatus"] in ("passed", "failed")
                and (evidence["captureStatus"] == "failed") == (failure is not None))
    require(all(type(evidence.get(key)) is int and evidence[key] > 0 for key in ("width", "height")))
    server = evidence.get("serverObservation")
    require(isinstance(server, dict) and all(type(server.get(key)) is int for key in
            ("gameTick", "acceptedTick", "attackId", "participants")))
    require(server["attackId"] == attack and server["acceptedTick"] >= 0
            and finite(server.get("age")) and server["age"] == server["gameTick"] - server["acceptedTick"] >= 0
            and all(vector(server.get(key)) for key in ("masterPosition", "targetPosition")))
    require(server.get("noAi") is False and server["participants"] == 1
            and server.get("pending") is (server["age"] < tell)
            and server.get("windup") is (server["age"] < tell)
            and server.get("guarding") is (not gale and 8 <= server["age"] < 20)
            and finite(server.get("aura")) and abs(server["aura"] - (60 if gale else 56)) < .01
            and finite(server.get("targetHealth"))
            and abs(server["targetHealth"] - (200 if server["age"] < tell else 174 if gale else 169.2)) < .02)
    framing_metadata(evidence, failed=failure is not None)
    if failure is not None:
        failed_render_schema(evidence)
        if articulated:
            articulated_receipts(evidence, failed=True)
        return exclude("native_render_failure", failure)
    require(all(type(evidence.get(key)) is int and evidence[key] > 0
                for key in ("width", "height", "bodySubmits", "modelPasses")))
    timeline, server = evidence.get("renderedTimeline"), evidence.get("serverObservation")
    require(isinstance(timeline, dict) and isinstance(server, dict))
    require(all(type(timeline.get(key)) is int for key in ("clientGameTick", "acceptedTick", "attackId", "tell", "active", "recovery"))
            and all(type(server.get(key)) is int for key in ("gameTick", "acceptedTick", "attackId", "participants")))
    require(timeline["attackId"] == server["attackId"] == attack
            and timeline["acceptedTick"] == server["acceptedTick"] >= 0
            and (timeline["tell"], timeline["active"], timeline["recovery"]) == (tell, 1, recovery)
            and timeline.get("fallbackRig") is (not articulated)
            and finite(timeline.get("partial")) and timeline["partial"] == .5
            and finite(timeline.get("age")) and requested <= timeline["age"] < requested + 1
            and timeline["age"] == timeline["clientGameTick"] - timeline["acceptedTick"] + timeline["partial"]
            and abs(timeline["clientGameTick"] - server["gameTick"]) <= 1
            and finite(server.get("age")) and server["age"] == server["gameTick"] - server["acceptedTick"])
    frame = timeline.get("frame")
    require(isinstance(frame, dict) and type(frame.get("move")) is int and frame["move"] == attack
            and type(frame.get("activation")) is int and frame["activation"] == timeline["acceptedTick"]
            and type(frame.get("leftHanded")) is bool and isinstance(frame.get("pose"), dict))
    pose = frame["pose"]
    require(all(vector(pose.get(joint)) for joint in ("body", "head", "sword", "offhand"))
            and all(finite(pose.get(key)) for key in ("weight", "stance", "bladeTilt"))
            and 0 < pose["weight"] <= 1)
    require(numbers(evidence.get("modelPose"), 120 if articulated else 36)
            and all(vector(evidence.get(key)) for key in ("camera", "renderedPosition"))
            and all(vector(server.get(key)) for key in ("masterPosition", "targetPosition")))
    pixels = evidence.get("bodyPixels")
    require(isinstance(pixels, dict) and all(type(pixels.get(key)) is int and pixels[key] >= 0 for key in
            ("nonBlackPixels", "chromaticPixels", "distinctColors", "luminanceRange")))
    unfinalized = articulated and "captureStatus" not in evidence
    if not unfinalized:
        require(pixels["nonBlackPixels"] >= 48 and pixels["distinctColors"] >= 8 and pixels["luminanceRange"] >= 24)
    points = evidence.get("bodyPoints")
    require(isinstance(points, list) and len(points) == 9 and all(point(value) for value in points))
    warnings = evidence.get("warnings")
    required_warnings = 0 if requested >= tell else 3 if requested >= (12 if gale else 20) else 2 if gale else 1
    require(isinstance(warnings, list))
    for warning in warnings:
        warning_schema(warning)
    if not close_body and not unfinalized:
        require(sum(warning["coloredBins"] >= 4 for warning in warnings) >= required_warnings)
    # Close views retain warning rays as diagnostics only. Bodies can occlude
    # geometrically on-screen segments, so this evidence cannot establish lane
    # acceptance; that remains the separate wide reply-warning capture's job.
    if articulated:
        articulated_receipts(evidence, failed=False)
        # Older producers wrote JSON before pixel assertions. A missing failure
        # field therefore cannot establish that this individual capture passed.
        # Validate legacy structure first, then keep it out of accepted coverage.
        if unfinalized:
            return exclude("native_capture_verdict_unavailable",
                           "Legacy NPC receipt lacks a finalized native capture verdict; pixel checks may have failed after the JSON write.")
        info.update(captureStatus=evidence["captureStatus"], expectedBackend="segmented", articulatedFrame=evidence["articulatedFrame"],
                    modelReceiptCount=len(evidence["modelReceipts"]), handReceiptCount=len(evidence["handReceipts"]),
                    modelTransformCount=120,
                    **{key: evidence[key] for key in ("framing", "fov", "warningCoverage", "warningValidation", "bodyBounds", "bladeBounds",
                                                     "bodyBoundsSource", "bladeBoundsSource")})
    info.update(participantKind=evidence["participantKind"], observerKind=evidence["observerKind"],
                independentTrialPerFrame=True, requestedTick=requested,
                renderedTimeline=timeline, serverObservation=server,
                viewport={"width": evidence["width"], "height": evidence["height"]},
                viewportBasis="native_framebuffer_metadata",
                metadata={"sourcePath": relative, "artifactPath": "frames/" + sidecar.name,
                          "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()})
    return info


def frame_bytes(frame):
    return frame["bytes"] + frame.get("metadata", {}).get("bytes", 0)


def select_masters(groups, budget):
    selected = []
    coverage = []
    remaining = budget - MANIFEST_RESERVE
    selected_paths = set()

    def choose(candidate):
        nonlocal remaining
        if frame_bytes(candidate) > remaining:
            return False
        selected.append(candidate)
        selected_paths.add(candidate["sourcePath"])
        remaining -= frame_bytes(candidate)
        return True

    # At most six representatives precede owner breadth. Missing beats are never
    # substituted, and each image/sidecar pair is an indivisible budget unit.
    for phase in NPC_PRIORITY:
        for scene in NPC_BEATS:
            options = groups.get((scene, NPC_VIEW, phase), [])
            if options:
                choose(options[0])
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
                if not choose(candidate):
                    omitted.append(phase)
                    continue
                chosen.append(candidate["frameLabel"])
            coverage.append({"scene": scene, "view": view, "capturedPhases": captured,
                             "selectedFrameLabels": chosen, "missingCapturePhases": missing,
                             "omittedForBudgetPhases": omitted})
    for scene, beats in NPC_BEATS.items():
        captured, chosen, missing, omitted = [], [], [], []
        for phase in beats:
            options = groups.get((scene, NPC_VIEW, phase), [])
            if not options:
                missing.append(phase)
                continue
            captured.append(phase)
            candidate = options[0]
            if candidate["sourcePath"] in selected_paths or (phase not in NPC_PRIORITY and choose(candidate)):
                chosen.append(phase)
            else:
                omitted.append(phase)
        coverage.append({"scene": scene, "view": NPC_VIEW, "sourceSuite": NPC_SUITE,
                         "capturedPhases": captured, "selectedFrameLabels": chosen,
                         "missingCapturePhases": missing, "omittedForBudgetPhases": omitted})
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


def articulated_npc_candidates(groups, phases):
    for phase in phases:
        for scene, beats in ARTICULATED_NPC_BEATS.items():
            if phase in beats:
                yield from groups.get((scene, NPC_VIEW, phase), [])


def articulated_npc_coverage(groups, selected):
    paths = {frame["sourcePath"] for frame in selected}
    result = []
    for scene, beats in ARTICULATED_NPC_BEATS.items():
        captured = [phase for phase in beats if groups.get((scene, NPC_VIEW, phase))]
        chosen = [phase for phase in captured if groups[(scene, NPC_VIEW, phase)][0]["sourcePath"] in paths]
        result.append({"scene": scene, "view": NPC_VIEW, "sourceSuite": ARTICULATED_SUITE,
                       "expectedBackend": "segmented", "capturedPhases": captured, "selectedFrameLabels": chosen,
                       "missingCapturePhases": [phase for phase in beats if phase not in captured],
                       "omittedForBudgetPhases": [phase for phase in captured if phase not in chosen]})
    return result


def shared_player_capture_matrix():
    """The 40 filename configurations each request three independent captures."""
    for art in SHARED_PLAYER_ARTS:
        for camera in ("third", "hud"):
            for width, height, scale in ((HUD_REFERENCE_VIEWPORT,) if camera == "third" else ARTICULATED_VIEWPORTS):
                for equipment in ("skin", "netherite"):
                    for hand in ARTICULATED_HANDS:
                        yield (f"articulated_shared_{art}_{camera}_{width}x{height}_gui{scale}_"
                               f"{equipment}_{hand}_requested_windup.png")


def shared_player_rows(groups):
    rows = []
    for filename in shared_player_capture_matrix():
        info = describe_articulated(filename)
        candidates = {frame["requestedPhase"]: frame
                      for frame in groups.get((info["scene"], info["view"], "unknown"), [])}
        rows.append({"info": info, "candidates": candidates})
    return rows


def shared_player_coverage(groups, selected):
    selected_paths = {frame["sourcePath"] for frame in selected}
    coverage = []
    for row in shared_player_rows(groups):
        info, candidates = row["info"], row["candidates"]
        captured = [phase for phase in SHARED_REQUESTED_PHASES if phase in candidates]
        chosen = [phase for phase in captured if candidates[phase]["sourcePath"] in selected_paths]
        coverage.append({**{key: info[key] for key in (
            "scene", "art", "camera", "hand", "view", "sourceSuite", "equipment", "equipmentBasis",
            "armorEnchantment", "armorTrim", "viewport", "uiScale", "viewportBasis", "hudVisible", "hudBasis")},
            "expectedRequestedPhases": list(SHARED_REQUESTED_PHASES), "capturedRequestedPhases": captured,
            "selectedFrameLabels": ["requested_" + phase for phase in chosen],
            "missingRequestedPhases": [phase for phase in SHARED_REQUESTED_PHASES if phase not in candidates],
            "omittedForBudgetRequestedPhases": [phase for phase in captured if phase not in chosen],
            "renderedPhase": "unknown", "verifiedRenderedPhases": [], "nativePixelReviewRequired": True,
            "preCaptureReceiptStatus": "not_ingested"})
    return coverage


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
        cost = sum(frame_bytes(frame) for frame in available.values())
        if cost <= remaining:
            selected_paths.update(available)
            remaining -= cost

    # Six actual NPC representatives precede owner breadth, each with its sidecar.
    # Owner comparison pairs retain their existing ordering and atomic budgeting.
    npc_priority = list(articulated_npc_candidates(groups, NPC_PRIORITY))
    for candidate in npc_priority:
        choose([candidate])

    # Give each new art/camera one opportunity before the old owner matrix can
    # consume the budget. Prefer requested active, reference viewport, skin, left
    # hand, but use another authentic candidate when an earlier one cannot fit.
    shared_rows = shared_player_rows(groups)
    shared_priority = []
    for camera in ("third", "hud"):
        for art in SHARED_PLAYER_ARTS:
            candidates = [frame for row in shared_rows
                          if row["info"]["art"] == art and row["info"]["camera"] == camera
                          for frame in row["candidates"].values()]
            candidates.sort(key=lambda frame: (
                SHARED_PHASE_PRIORITY.index(frame["requestedPhase"]),
                (frame["viewport"]["width"], frame["viewport"]["height"], frame["uiScale"]) != HUD_REFERENCE_VIEWPORT,
                frame["equipment"] != "skin", ARTICULATED_HANDS.index(frame["hand"]), frame["sourcePath"]))
            for candidate in candidates:
                choose([candidate])
                if candidate["sourcePath"] in selected_paths:
                    shared_priority.append(candidate)
                    break

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
    # Broaden shared-art requests evenly across both arts before spending on old
    # temporal extras. This is request coverage, never a rendered phase verdict.
    shared_extras = []
    for phase in SHARED_PHASE_PRIORITY:
        per_art = [[row["candidates"][phase] for row in shared_rows
                    if row["info"]["art"] == art and phase in row["candidates"]]
                   for art in SHARED_PLAYER_ARTS]
        for index in range(max(map(len, per_art), default=0)):
            for candidates in per_art:
                if index < len(candidates):
                    candidate = candidates[index]
                    if candidate not in shared_priority:
                        shared_extras.append(candidate)
                        choose([candidate])
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

    # Boundary and early-school beats come after the existing owner/armor/HUD
    # selection. Their exact labels and ages remain in sidecars and coverage.
    extra_phases = dict.fromkeys(phase for beats in ARTICULATED_NPC_BEATS.values()
                                for phase in beats if phase not in NPC_PRIORITY)
    npc_extras = list(articulated_npc_candidates(groups, extra_phases))
    for candidate in npc_extras:
        choose([candidate])
    selected = [frame for frame in npc_priority if frame["sourcePath"] in selected_paths]
    selected.extend(shared_priority)
    for row in rows:
        for frame in row["candidates"].values():
            if frame["sourcePath"] in selected_paths:
                selected.append(frame)
                row["coverage"]["selectedFrameLabels"].append(frame["frameLabel"])
            elif frame["phase"] == "sample":
                row["coverage"]["omittedForBudgetSamplePositions"].extend(frame["samplePositions"])
            else:
                row["coverage"]["omittedForBudgetIdleLabels"].append(frame["frameLabel"])
    selected.extend(frame for frame in shared_extras if frame["sourcePath"] in selected_paths)
    selected.extend(frame for frame in npc_extras if frame["sourcePath"] in selected_paths)
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
            or any(not isinstance(item, str) for item in stamp["preexistingPngs"])
            or not isinstance(stamp.get("preexistingNpcMetadata"), list)
            or any(not isinstance(item, str) for item in stamp["preexistingNpcMetadata"])):
        raise EvidenceError("Freshness marker does not match this run, suite and screenshot root")
    old_paths = set(stamp["preexistingPngs"])
    groups = {}
    excluded_npc = []
    stale = ignored = stale_metadata = 0
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
        if info["view"] == NPC_VIEW:
            metadata = read_npc_metadata(root, path, info, stamp)
            if metadata is None:
                stale_metadata += 1
                continue
            if "excludedNpcCapture" in metadata:
                excluded_npc.append(metadata["excludedNpcCapture"])
                continue
        groups.setdefault((info["scene"], info["view"], info["phase"]), []).append(info)
    selector = select_articulated if suite == "articulated" else select_masters
    selected, coverage = selector(groups, budget)
    manifest = {
        "schemaVersion": 1, "artifactKind": "curated_native_visual_evidence_only",
        "status": "available" if selected else "unavailable", "provenance": identity,
        "suiteGroup": suite, "suite": entrypoint, "sourceRoot": Path(screenshots).as_posix(),
        "freshness": {"method": "pre_run_marker; reject all preexisting paths and older mtimes",
                      "excludedStalePngs": stale, "ignoredOutOfScopePngs": ignored,
                      "excludedStaleNpcMetadata": stale_metadata},
        "limits": {"totalBytesLimit": budget, "manifestReserveBytes": manifest_reserve,
                   "archiveHeadroomBelow15MB": 15_000_000 - budget},
        "basis": "Byte-identical source PNGs. Player first-person and back third-person captures share the "
                 "owning singleplayer client; their active label is an accepted release window, not exact render-phase evidence. "
                 "Separate Gale/Stone NPC captures use an unpaused real client spectator and a consenting Fabric FakePlayer, "
                 "not a human duel. Their fresh JSON sidecars record actual native model extraction age, server AI observation "
                 "and the accepted attack. Each NPC image is a separate natural trial, not consecutive footage. "
                 "Synthetic model-injection checks in MasterModelPresentationTest remain distinct and are not selected. "
                 "Presence or metadata validation is not a gameplay or native pixel-review pass.",
        "testVerdict": {"establishedByThisArtifact": False,
                        "authoritativeArtifact": f"{suite}-native-evidence",
                        "authoritativeManifest": f"{suite}-native-manifest.json",
                        "note": "Existing native test manifest and all suite/shard gates are unchanged."},
        "unavailableRequestedCoverage": [
            {"coverage": "observer_client_of_player_combat", "reason": "Player combat images use only the owning client; "
             "the separate spectator fixture observes NPC Gale/Stone combat."},
            {"coverage": "live_master_ember_combat", "reason": "EmberAfterburnChecks has no screenshot calls. "
             "The live NPC fixture covers Gale/Stone; its earlier model-injection checks are synthetic."},
            {"coverage": "exact_player_impact_phase", "reason": "The owner accepted release window does not prove the exact rendered impact. "
             "NPC extraction age is recorded separately; a render sample does not itself establish a server damage verdict."},
            {"coverage": "human_multiplayer_duel", "reason": "The NPC challenger is a consenting Fabric FakePlayer with a real client spectator."},
        ],
        "selectionPolicy": "First up to six native NPC representatives: reply_warning, release, recovery, Gale then Stone per beat. "
                           "Each validated PNG/JSON pair is budgeted together. Then stable player scene order; "
                           "owner first-person then local-owner back third-person; "
                           "windup, active release window, recovery, settled; skip frames exceeding remaining budget. "
                           "Then NPC gather/step and plant/brace. Missing NPC beats are never invented or substituted. "
                           "Explicit native render failures are excluded with bounded reasons, never accepted as coverage. "
                           "Only exact implemented owner and NPC fixture names are eligible. "
                           "Left-turn, cancelled, help, synthetic model and other screenshots remain in the full artifact.",
        "selectedPngBytes": sum(frame["bytes"] for frame in selected),
        "excludedNpcCaptures": excluded_npc,
        "selectedMetadataBytes": sum(frame.get("metadata", {}).get("bytes", 0) for frame in selected),
        "selectedMetadataCount": sum("metadata" in frame for frame in selected),
        "selectedFrameCount": len(selected), "coverage": coverage, "frames": selected,
    }
    if suite == "masters":
        manifest.update(sourceSuites=[SUITE, NPC_SUITE])
    if suite == "articulated":
        manifest.update({
            "sourceSuites": list(ARTICULATED_SOURCE_SUITES),
            "npcCoverage": articulated_npc_coverage(groups, selected),
            "sharedPlayerCoverage": shared_player_coverage(groups, selected),
            "basis": "Byte-identical native PNGs from the three existing articulated suites. Original combat samples "
                     "follow real Spellcut input and its server-accepted timeline; HUD idle-before/after "
                     "captures are explicitly idle. Left/right denote the local owner's configured main hand; "
                     "all first-person and front/back third-person views share that owning singleplayer client, "
                     "never an observer of player combat. Separate opt-in Gale/Stone NPC captures use a real client spectator "
                     "and a consenting Fabric FakePlayer, not a human duel. Each NPC image is an independent natural trial. "
                     "Its copied JSON records the actual extraction age, 20-joint model passes and native held-item calls. "
                     "Entry/hand matrices are observed; resolved/expected item matrices apply fixed vanilla item offsets. "
                     "Synced horizontal velocity, position interpolation and eased walk speed remain separate measurements. "
                     "NPC reply warnings retain a wide full-lane view; other beats use close body framing and report "
                     "only visible warning portions, with unclamped or absent projected endpoints. Close warning samples "
                     "are diagnostic-only and do not establish warning-lane acceptance; only wide warning-lane captures "
                     "enforce expected segment/color coverage. Body/blade bounds are geometric enclosure "
                     "receipts, not pixel segmentation or proof against self-occlusion. "
                     "Selected NPC sidecars require a per-capture passed status finalized after native render and pixel checks; "
                     "legacy receipts without that verdict are excluded as unfinalized, not declared failed. "
                     "Synthetic model/socket checks are distinct from these native NPC captures. "
                     "Equipment and HUD viewport labels describe native fixture assertions, "
                     "not image analysis; unencoded viewport/UI settings remain unknown. Loop indices and "
                     "first/middle/last available samples prove neither exact impact nor recovery boundaries; "
                     "skin/armor samples are not guaranteed to depict the same animation age. "
                     "New Rising Break/Driving Cut filenames record requested phases only; their rendered phase remains unknown. "
                     "Their camera/viewport/hand facts come from filenames and equipment/HUD details from the fixture contract. "
                     "ARTICULATED_SHARED_SAMPLE log receipts remain in full evidence and are not ingested here; "
                     "accepted activation, actual skin and pre-capture phase/age are unknown in this artifact. "
                     "Even a matching pre-capture log receipt would not establish the framebuffer phase. "
                     "Presence is not a gameplay or native pixel-review pass.",
            "unavailableRequestedCoverage": [
                {"coverage": "observer_client_of_player_combat", "reason": "Player images use the owner; the separate spectator fixture observes NPC Gale/Stone combat."},
                {"coverage": "human_multiplayer_duel", "reason": "The NPC challenger is a consenting Fabric FakePlayer with a real client spectator."},
                {"coverage": "synthetic_geometry_and_hitstop_screenshots", "reason": "Synthetic geometry, "
                 "socket and hit-stop checks do not take screenshots; inspect the independent native verdict."},
                {"coverage": "exact_player_impact_phase", "reason": "Player capture loop indices and shared-art requested/pre-capture phases do not prove exact framebuffer impact timing; NPC render age is recorded separately."},
                {"coverage": "exact_player_recovery_phase", "reason": "Last available or requested-recovery player captures are not verified rendered recovery boundaries."},
            ],
            "selectionPolicy": "First up to six validated NPC PNG/JSON pairs: reply_warning, release, recovery, Gale then Stone per beat. "
                               "Missing or explicitly failed native NPC frames are never accepted or substituted. "
                               "Then one available shared-player representative per camera and art, third then HUD, Rising then Driving; "
                               "prefer requested active then windup/recovery, reference viewport, skin and left hand, skipping over-budget candidates. "
                               "Then left then right: original body/hand first-available pair, full enchanted armor "
                               "front/back first-available pair, 1280x720/gui3 HUD skin/chestplate upper-median pair. "
                               "Prefer the HUD viewport with the most available counterparts, breaking ties "
                               "by reference size then fixture order; missing reference captures use this fallback. "
                               "Each core/idle comparison pair is budgeted together; missing counterparts remain missing. "
                               "After the old core comparisons, broaden the shared-player requests, active then windup/recovery, "
                               "alternating arts within each phase in stable configuration order. All 120 requested trial slots "
                               "have explicit capture/omission coverage, separate from rendered phases. "
                               "Then chosen HUD idle-before/after pairs, core temporal samples, full-armor "
                               "first-person, partial armor, and other exact fixture viewports in matrix order. "
                               "For the original matrices, only first/upper-median/last available combat samples and explicit HUD idle "
                               "captures are copied, each once. Armor capture indices are only 2 and 5; "
                               "original samples are 0..8 and HUD samples 0..9. Uncaptured indices are not "
                               "required captures. Finally exact early-school and Gale age11..15 boundary captures when present. "
                               "NPC receipt files share the existing total byte cap and are copied unchanged. "
                               "The full evidence artifact and native verdicts are unchanged.",
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
            if "metadata" in frame:
                metadata = frame["metadata"]
                path = safe_path(root, metadata["sourcePath"])
                before = path.stat()
                if before.st_size != metadata["bytes"] or before.st_mtime_ns < stamp["startedNs"]:
                    raise EvidenceError("NPC metadata changed during curation")
                copied = staging / metadata["artifactPath"]
                shutil.copyfile(path, copied)
                if (not unchanged(before, safe_path(root, metadata["sourcePath"]).stat())
                        or copied.stat().st_size != metadata["bytes"]
                        or hashlib.sha256(copied.read_bytes()).hexdigest() != metadata["sha256"]):
                    raise EvidenceError("NPC metadata changed after validation")
        data = encode(manifest)
        if len(data) > manifest_reserve or sum(frame_bytes(item) for item in selected) + len(data) > budget:
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
