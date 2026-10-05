#!/usr/bin/env python3
"""Verify passive native receipts and their unchanged PNGs; never a visual-quality verdict.

All JSON records for a capture token are authoritative, including late/failed records.
Use one selected native run directory and its original game directory/artifact equivalent.
The native process must be finished; a live scan cannot rule out future late diagnostics.
"""
from __future__ import annotations

import argparse
from collections import defaultdict
import hashlib
import io
import json
from pathlib import Path
import struct

from PIL import Image


PIXEL_FORMAT = "argb32-be-row-major-v1"
STAGES = ["requested", "extracted", "rendered", "copy-enqueued", "image-received", "image-written", "receipt-verified"]


def validate_native_record(record: dict, pixels: dict) -> tuple[dict, bool, bool]:
    """Recheck evidence, including claims emitted by the writer; 'verified' alone is not evidence."""
    if record["schemaVersion"] != 1 or record["origin"] != "fabric_test_screenshot" or record["stages"] != STAGES:
        raise ValueError("missing native schema/origin/stages")
    trial = record["identity"]["trial"]
    requested = trial.rsplit("_requested_", 1)[-1].upper() if "_requested_" in trial else "unknown"
    if record["identity"]["requestedPhase"] != requested:
        raise ValueError("requested phase differs from the unchanged trial name")
    capture = record["copy"]
    extraction, render, copied = (capture[key] for key in ("extractionSequence", "renderSequence", "copySequence"))
    if not all(type(value) is int for value in (extraction, render, copied)) or not 0 < extraction < render < copied:
        raise ValueError("missing or unordered extraction/render/copy identities")
    target = capture["target"]
    observation = capture["observation"]
    if target["width"] != pixels["width"] or target["height"] != pixels["height"] or target["mipLevel"] != 0:
        raise ValueError("copied target dimensions/mip disagree with image")
    for target_key, observed_key in (("targetGeneration", "renderTargetGeneration"), ("textureGeneration", "renderTextureGeneration")):
        if type(target[target_key]) is not int or target[target_key] <= 0 or str(target[target_key]) != observation[observed_key]:
            raise ValueError("rendered and copied target generations disagree")
    if any(observation[key] not in ("true", "false") for key in ("firstPerson", "paused", "frozen")):
        raise ValueError("unknown camera/pause/freeze state")
    first = observation["firstPerson"] == "true"
    submit_kind, deferred_kind = ("view_submit", "view_deferred") if first else ("body_submit", "body_deferred")
    passes = capture["passes"]
    submitted = [entry for entry in passes if entry["kind"] == submit_kind]
    deferred = [entry for entry in passes if entry["kind"] == deferred_kind]
    if len(submitted) != 1 or not deferred:
        raise ValueError("missing one actual submission and matching deferred preparation")
    if first and len([entry for entry in passes if entry["kind"] == "view_item"]) != 1:
        raise ValueError("missing one actual main-hand item submission")
    allowed = {"view_submit", "view_deferred", "view_attachment", "view_item", "body_submit", "body_deferred", "body_attachment"}
    for entry in passes:
        palette = entry["palette"]
        if entry["kind"] not in allowed or palette is None:
            raise ValueError("unknown pass or missing palette")
        if str(entry["owner"]) != observation["ownerId"] or palette["master"] or str(palette["activation"]) != observation["acceptedStartTick"] or str(palette["move"]) != observation["acceptedMove"]:
            raise ValueError("wrong owner or accepted activation")
        if palette["phase"] not in ("NONE", "WINDUP", "ACTIVE", "RECOVERY"):
            raise ValueError("unknown observed palette phase")
        if type(entry["stateIdentity"]) is not int or entry["stateIdentity"] <= 0 or type(entry["modelIdentity"]) is not int or entry["modelIdentity"] <= 0:
            raise ValueError("missing native state/model identity")
    base = submitted[0]
    for entry in deferred:
        if entry["stateIdentity"] != base["stateIdentity"] or entry["modelIdentity"] != base["modelIdentity"] or entry["palette"] != base["palette"]:
            raise ValueError("deferred state/model/palette differs from actual submission")
        if entry["attributes"].get("segmentedVisible") != "true":
            raise ValueError("missing visible segmented backend")
        for field in ("skinModel", "skinTexture", "rigWidth"):
            if entry["attributes"].get(field) in (None, "", "unknown") or entry["attributes"][field] != base["attributes"].get(field):
                raise ValueError("missing or conflicting actual skin appearance")
    if first:
        item = next(entry for entry in passes if entry["kind"] == "view_item")
        if any(item[field] != base[field] for field in ("stateIdentity", "modelIdentity", "palette")):
            raise ValueError("main-hand item differs from submitted view")
    rendered = deferred[0]["palette"]["phase"]
    if rendered != record["renderedPhase"]:
        raise ValueError("reported rendered phase conflicts with deferred palette")
    matched = record["identity"]["requestedPhase"] == rendered
    unpaused = matched and observation["paused"] == "false" and observation["frozen"] == "false"
    if record["requestedPhaseObserved"] is not matched or record["unpausedPhaseCoverage"] is not unpaused:
        raise ValueError("reported phase/pause coverage conflicts with native evidence")
    return deferred[0], matched, unpaused


def image_evidence(data: bytes) -> dict:
    with Image.open(io.BytesIO(data)) as source:
        if source.format != "PNG":
            raise ValueError("bound image is not PNG")
        image = source.convert("RGBA")
        rgba = image.tobytes()
        argb = bytearray(len(rgba))
        argb[0::4], argb[1::4], argb[2::4], argb[3::4] = rgba[3::4], rgba[0::4], rgba[1::4], rgba[2::4]
        digest = hashlib.sha256(PIXEL_FORMAT.encode("ascii") + b"\0" + struct.pack(">II", *image.size) + argb)
        return {"width": image.width, "height": image.height, "format": PIXEL_FORMAT, "sha256": digest.hexdigest()}


def verify(receipts: Path, game_dir: Path, expected_count: int, expected_trials: set[str] | None = None) -> dict:
    if expected_count <= 0:
        raise ValueError("a positive expected trial count is required to reject missing receipts")
    groups = defaultdict(list)
    errors = []
    for path in sorted(receipts.glob("*.json")):
        try:
            record = json.loads(path.read_text())
            identity = record["identity"]
            key = (identity["runId"], identity["captureSequence"])
            if not identity["trial"].startswith("articulated_shared_"):
                raise ValueError("out-of-scope trial")
            groups[key].append((path, record))
        except (ValueError, KeyError, TypeError) as failure:
            errors.append(f"{path.name}: malformed receipt: {failure}")
    if not groups:
        errors.append("no capture receipts")
    if len({key[0] for key in groups}) > 1:
        errors.append("select exactly one native run directory")
    trials = {}
    observations = []
    image_owners = {}
    for key, records in groups.items():
        identities = [record["identity"] for _, record in records]
        if any(identity != identities[0] for identity in identities[1:]):
            errors.append(f"{key}: conflicting token identities")
        trial = identities[0]["trial"]
        if trial in trials:
            errors.append(f"{trial}: multiple capture tokens; retry cannot silently satisfy coverage")
        trials[trial] = key
        # A late callback revokes an older immutable success record. Never read '*-receipt-*' alone.
        if any(not record.get("verified") or record.get("failures") for _, record in records):
            errors.append(f"{trial}: failed, incomplete or late-invalidated receipt")
            continue
        verified = [record for _, record in records if "receipt-verified" in record.get("stages", [])]
        if len(verified) != 1:
            errors.append(f"{trial}: expected one verified terminal association")
            continue
        record = verified[0]
        try:
            image = record["image"]
            relative = Path(image["relativeImagePath"])
            path = (game_dir / relative).resolve()
            # Local validation must not follow a forged artifact path outside the selected game tree.
            if relative.is_absolute() or not path.is_relative_to(game_dir.resolve()):
                raise ValueError("image path escapes selected game directory")
            if path in image_owners:
                raise ValueError(f"same PNG is already associated with {image_owners[path]}")
            image_owners[path] = trial
            data = path.read_bytes()
            if len(data) != image["pngBytes"] or hashlib.sha256(data).hexdigest() != image["pngSha256"]:
                raise ValueError("PNG bytes changed or filename was reused")
            pixels = image_evidence(data)
            if pixels != image["decodedPixels"] or pixels != record["callbackPixels"]:
                raise ValueError("callback and unchanged decoded PNG hashes differ")
            if record["copy"]["identity"] != record["identity"]:
                raise ValueError("copy belongs to another capture")
            if record.get("exactImpactPixelCoverage") != "unverified" or not record.get("nativePixelReviewRequired"):
                raise ValueError("receipt overstates native pixel proof")
            observed, matched, unpaused = validate_native_record(record, pixels)
            observations.append({"trial": trial, "requestedPhase": record["identity"]["requestedPhase"],
                                 "renderedPhase": record["renderedPhase"], "requestedPhaseObserved": matched,
                                 "unpausedPhaseCoverage": unpaused, "backend": observed["kind"],
                                 "move": observed["palette"]["move"], "leftHanded": observed["palette"]["leftHanded"],
                                 "skinModel": observed["attributes"]["skinModel"],
                                 "skinTexture": observed["attributes"]["skinTexture"],
                                 "rigWidth": observed["attributes"]["rigWidth"], "pngSha256": image["pngSha256"]})
        except (OSError, ValueError, KeyError, TypeError) as failure:
            errors.append(f"{trial}: {failure}")
    if len(trials) != expected_count:
        errors.append(f"expected {expected_count} distinct trials, observed {len(trials)}")
    if expected_trials is not None and set(trials) != expected_trials:
        errors.append(f"trial manifest mismatch: missing={sorted(expected_trials - set(trials))}, unexpected={sorted(set(trials) - expected_trials)}")
    return {"associationVerified": not errors, "expectedTrials": expected_count, "observedTrials": len(trials),
            "verifiedImages": len(observations), "observations": observations, "errors": errors,
            "nativePixelReviewRequired": True, "exactImpactPixelCoverage": "unverified"}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--receipts", type=Path, required=True, help="One run's complete receipts directory, including late/failed files")
    parser.add_argument("--game-dir", type=Path, required=True)
    parser.add_argument("--expected-count", type=int, required=True, help="24 for shared body, 96 for shared HUD, 120 if both ran")
    parser.add_argument("--expected-trials-file", type=Path, help="Optional exact planned trial names, one per line")
    args = parser.parse_args()
    expected = set(args.expected_trials_file.read_text().splitlines()) if args.expected_trials_file else None
    report = verify(args.receipts, args.game_dir, args.expected_count, expected)
    print(json.dumps(report, indent=2))
    raise SystemExit(0 if report["associationVerified"] else 1)


if __name__ == "__main__":
    main()
