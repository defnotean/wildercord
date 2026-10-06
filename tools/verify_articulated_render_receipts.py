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
import math
import os
from pathlib import Path
import re
import stat
import struct
import uuid
import zlib

from PIL import Image


PIXEL_FORMAT = "argb32-be-row-major-v1"
STAGES = ["requested", "extracted", "rendered", "copy-enqueued", "image-received", "image-written", "receipt-verified"]
MAX_RECORD_BYTES = 128 * 1024
MAX_RECORDS = 256
MAX_RECEIPT_BYTES = 23 * 1024 * 1024
MAX_PNG_BYTES = 64 * 1024 * 1024
MAX_IMAGE_DIMENSION = 8192
MAX_IMAGE_PIXELS = 16 * 1024 * 1024


def bounded_file(path: Path, root: Path, limit: int) -> bytes:
    """Read one independent, bounded regular file without following evidence symlinks."""
    path = Path(os.path.abspath(path))
    root = root.resolve()
    if not path.is_relative_to(root):
        raise ValueError("evidence path escapes selected game directory")
    current = root
    for component in path.relative_to(root).parts:
        current /= component
        if current.is_symlink():
            raise ValueError("symlink evidence path")
    before = path.lstat()
    if not stat.S_ISREG(before.st_mode) or before.st_nlink != 1 or not 0 < before.st_size <= limit:
        raise ValueError("evidence file is linked, non-regular, empty or exceeds bounded size")
    with path.open("rb") as stream:
        opened = os.fstat(stream.fileno())
        if (opened.st_dev, opened.st_ino) != (before.st_dev, before.st_ino):
            raise ValueError("evidence file replaced before read")
        data = stream.read(limit + 1)
    after = path.lstat()
    fields = ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns", "st_nlink")
    if len(data) != before.st_size or any(getattr(before, field) != getattr(after, field) for field in fields):
        raise ValueError("evidence file changed while reading")
    return data


def relative_evidence_path(value: str) -> Path:
    path = Path(value)
    if not isinstance(value, str) or not value or "\\" in value or path.is_absolute() or path.as_posix() != value or any(part in (".", "..") for part in value.split("/")):
        raise ValueError("image path escapes selected game directory or is not canonical")
    return path


def validate_native_record(record: dict, pixels: dict, profile="shared") -> tuple[dict, bool, bool]:
    """Recheck evidence, including claims emitted by the writer; 'verified' alone is not evidence."""
    if type(record["schemaVersion"]) is not int or record["schemaVersion"] != (1 if profile == "shared" else 2) or record["origin"] != "fabric_test_screenshot" or record["stages"] != STAGES:
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
    if profile == "opening":
        return validate_opening_record(record)
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


OPENING_TRIAL = re.compile(r"articulated_opening_style_(kindling_draw|frostbite)_(first|third)_(left|right)_(skin|netherite)_(glow_shell_down|funded_shell)(_adapter_disabled)?_requested_(windup|active|recovery)")
SHA256 = re.compile(r"[0-9a-f]{64}")
OPENING_APPEARANCE = ("skinModel", "skinTexture", "rigWidth", "ownerUuid", "avatarStateIdentity", "extractedStateIdentity",
                      "postHitStopExtractionMatched", "mainArm", "mainHand", "head", "chest", "legs", "feet",
                      "shellGlowPresent", "shellAdapterEnabled", "rawAcceptedPhase", "rawActivation", "rawMove", "rawLeftHanded", "rawPaletteSha256")


def positive_identity(value) -> bool:
    return isinstance(value, str) and value.isascii() and value.isdecimal() and str(int(value)) == value and int(value) > 0


def validate_opening_record(record: dict) -> tuple[dict, bool, bool]:
    """Validate opening schema independently, including the complete vanilla fallback."""
    identity, capture = record["identity"], record["copy"]
    observation = capture["observation"]
    trial = OPENING_TRIAL.fullmatch(identity["trial"])
    if trial is None:
        raise ValueError("unknown opening trial identity")
    art, camera, hand, armor, shell, disabled, phase = trial.groups()
    fallback, first = disabled is not None, camera == "first"
    if ((armor == "netherite") != (shell == "funded_shell") and phase != "active") or (fallback and (armor, shell, phase) != ("netherite", "funded_shell", "active")):
        raise ValueError("opening trial is outside the exact native matrix")
    expected = "full_fallback" if fallback else "segmented"
    move = 3 if art == "kindling_draw" else 4
    if observation["firstPerson"] != str(first).lower() or record["expectedBackend"] != expected or observation["expectedBackend"] != expected:
        raise ValueError("opening camera/backend conflicts with exact trial")
    if observation["openingArmed"] != "true":
        raise ValueError("opening receipt lacks accepted action arming")
    if str(uuid.UUID(identity["runId"])) != identity["runId"]:
        raise ValueError("opening run identity is not a canonical UUID")
    owner = observation["ownerUuid"]
    if str(uuid.UUID(owner)) != owner or observation["expectedOwnerUuid"] != owner:
        raise ValueError("opening owner UUID conflicts with armed owner")
    if not positive_identity(observation["ownerId"]) or any(observation[key] != observation["ownerId"] for key in ("expectedOwnerId", "acceptedEntity")):
        raise ValueError("opening accepted entity conflicts with owner")
    if observation["acceptedMove"] != str(move) or observation["expectedMove"] != str(move) or not positive_identity(observation["acceptedStartTick"]) or observation["acceptedStartTick"] != observation["expectedStartTick"]:
        raise ValueError("opening accepted action conflicts with armed action or trial")
    if observation["shellAdapterEnabled"] != str(not fallback).lower():
        raise ValueError("opening adapter state conflicts with trial")
    passes = capture["passes"]
    submit_kind = "view_fallback_submit" if first and fallback else "view_submit" if first else "body_submit"
    deferred_kind = "view_deferred" if first else "body_deferred"
    allowed = {"view_fallback_submit", "view_fallback_item"} if first and fallback else {f"{'view' if first else 'body'}_{kind}" for kind in ("submit", "deferred", "attachment")}
    if first and not fallback:
        allowed.add("view_item")
    submitted = [entry for entry in passes if entry["kind"] == submit_kind]
    deferred = [entry for entry in passes if entry["kind"] == deferred_kind]
    if len(submitted) != 1 or (not (first and fallback) and not deferred):
        raise ValueError("missing one actual opening submission and required deferred preparation")
    base = submitted[0]
    if first:
        item_kind = "view_fallback_item" if fallback else "view_item"
        items = [entry for entry in passes if entry["kind"] == item_kind]
        if len(items) != 1:
            raise ValueError("missing one actual opening main-hand item submission")
        item = items[0]
        if any(item[field] != base[field] for field in ("stateIdentity", "modelIdentity", "palette")) or not positive_identity(item["attributes"].get("itemStateIdentity")):
            raise ValueError("opening item differs from actual submitted view")
        if fallback and item["attributes"]["itemStateIdentity"] != base["attributes"].get("itemStateIdentity"):
            raise ValueError("fallback item state differs from actual vanilla submission")
    expected_equipment = {"head": "helmet", "chest": "chestplate", "legs": "leggings", "feet": "boots"}
    for entry in passes:
        attrs, palette = entry["attributes"], entry["palette"]
        if entry["kind"] not in allowed or palette is None or str(entry["owner"]) != observation["ownerId"]:
            raise ValueError("unknown opening pass, missing palette or wrong owner")
        if any(type(entry[field]) is not int or entry[field] <= 0 for field in ("stateIdentity", "modelIdentity")):
            raise ValueError("missing opening native state/model identity")
        if any(attrs.get(field) in (None, "", "unknown") or attrs[field] != base["attributes"].get(field) for field in OPENING_APPEARANCE):
            raise ValueError("missing or conflicting opening appearance/action attributes")
        if attrs["ownerUuid"] != owner or not positive_identity(attrs["avatarStateIdentity"]) or attrs["avatarStateIdentity"] != attrs["extractedStateIdentity"] or attrs["postHitStopExtractionMatched"] != "true":
            raise ValueError("opening state lacks exact post-HitStop owner extraction")
        if not first and str(entry["stateIdentity"]) != attrs["avatarStateIdentity"]:
            raise ValueError("opening body state differs from extracted avatar")
        if palette["master"] is not False or type(palette["leftHanded"]) is not bool or palette["leftHanded"] != (hand == "left") or type(palette["move"]) is not int or palette["move"] != move or type(palette["activation"]) is not int or str(palette["activation"]) != observation["acceptedStartTick"]:
            raise ValueError("opening palette conflicts with exact accepted action/hand")
        if palette["phase"] not in ("NONE", "WINDUP", "ACTIVE", "RECOVERY") or type(palette["weight"]) not in (int, float) or not math.isfinite(palette["weight"]) or not 0 <= palette["weight"] <= 1 or not SHA256.fullmatch(palette["localsSha256"]):
            raise ValueError("invalid raw opening palette")
        raw_fields = {"rawAcceptedPhase": palette["phase"], "rawActivation": str(palette["activation"]), "rawMove": str(move), "rawLeftHanded": str(palette["leftHanded"]).lower()}
        # The articulated first-person view transforms the raw body locals.
        # Raw extraction and submitted view hashes have distinct meanings.
        if not first or fallback:
            raw_fields["rawPaletteSha256"] = palette["localsSha256"]
        if not SHA256.fullmatch(attrs["rawPaletteSha256"]) or any(attrs[field] != value for field, value in raw_fields.items()):
            raise ValueError("opening raw extraction palette conflicts with submitted palette")
        if attrs["mainArm"] != hand.upper() or attrs["mainHand"] != "minecraft:diamond_sword:foil=false":
            raise ValueError("opening held item or hand conflicts with trial")
        for slot, piece in expected_equipment.items():
            wanted = f"minecraft:netherite_{piece}:foil=true" if armor == "netherite" else "empty"
            if attrs[slot] != wanted:
                raise ValueError("opening actual equipment conflicts with trial")
        if attrs["shellGlowPresent"] != str(shell == "funded_shell").lower() or attrs["shellAdapterEnabled"] != str(not fallback).lower():
            raise ValueError("opening actual shell state conflicts with trial")
        if first and fallback and (attrs.get("segmentedVisible") != "false" or attrs.get("fallbackFrameCompatible") != "false"):
            raise ValueError("fallback view retains articulated compatibility or visibility")
    for entry in deferred:
        attrs = entry["attributes"]
        if any(entry[field] != base[field] for field in ("stateIdentity", "modelIdentity", "palette")):
            raise ValueError("opening deferred state/model/palette differs from submission")
        if attrs.get("segmentedVisible") != str(not fallback).lower():
            raise ValueError("opening visible backend conflicts with trial")
        if fallback and (attrs.get("rigidVisible") != "true" or attrs.get("segmentedRootVisible") != "false" or attrs.get("fallbackFrameCompatible") != "false"):
            raise ValueError("fallback body is not complete or still contains segmented geometry")
        hash_key = "rigPaletteSha256" if first else "actualPaletteSha256"
        if not SHA256.fullmatch(attrs.get(hash_key, "")) or attrs[hash_key] != deferred[0]["attributes"].get(hash_key):
            raise ValueError("missing installed opening geometry palette hash")
    raw = base["palette"]["phase"]
    rendered = "FALLBACK" if fallback else raw
    matched = not fallback and raw == identity["requestedPhase"]
    unpaused = matched and observation["paused"] == "false" and observation["frozen"] == "false"
    if record["renderedPhase"] != rendered or record["rawAcceptedPhase"] != raw or record["rawRequestedPhaseObserved"] is not (raw == identity["requestedPhase"]) or record["requestedPhaseObserved"] is not matched or record["unpausedPhaseCoverage"] is not unpaused:
        raise ValueError("opening reported phase/pause coverage conflicts with native evidence")
    return (base if first and fallback else deferred[0]), matched, unpaused


def image_evidence(data: bytes) -> dict:
    # Pillow does not reliably check every IDAT CRC while decoding. Check the
    # original stream, including ancillary chunks, before allocating pixels.
    if len(data) > MAX_PNG_BYTES or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("bound image is not a bounded PNG")
    offset, chunks, saw_data, ended = 8, 0, False, False
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValueError("truncated PNG chunk")
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4:offset + 8]
        end = offset + 12 + length
        if end > len(data):
            raise ValueError("PNG chunk exceeds original bytes")
        payload = memoryview(data)[offset + 8:offset + 8 + length]
        expected_crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        if zlib.crc32(payload, zlib.crc32(kind)) != expected_crc:
            raise ValueError("PNG chunk CRC mismatch")
        if chunks == 0:
            if kind != b"IHDR" or length != 13:
                raise ValueError("PNG has no canonical first IHDR")
            width, height = struct.unpack_from(">II", payload)
            if not 0 < width <= MAX_IMAGE_DIMENSION or not 0 < height <= MAX_IMAGE_DIMENSION or width * height > MAX_IMAGE_PIXELS:
                raise ValueError("PNG dimensions exceed safe decode bounds")
        elif kind == b"IHDR":
            raise ValueError("PNG contains duplicate IHDR")
        if kind in (b"acTL", b"fcTL", b"fdAT"):
            raise ValueError("animated PNG is not one native screenshot")
        saw_data |= kind == b"IDAT"
        chunks += 1
        offset = end
        if kind == b"IEND":
            if length != 0 or not saw_data or offset != len(data):
                raise ValueError("invalid PNG end or trailing bytes")
            ended = True
            break
    if not ended:
        raise ValueError("PNG has no complete IEND")
    with Image.open(io.BytesIO(data)) as source:
        if source.format != "PNG":
            raise ValueError("bound image is not PNG")
        image = source.convert("RGBA")
        rgba = image.tobytes()
        argb = bytearray(len(rgba))
        argb[0::4], argb[1::4], argb[2::4], argb[3::4] = rgba[3::4], rgba[0::4], rgba[1::4], rgba[2::4]
        digest = hashlib.sha256(PIXEL_FORMAT.encode("ascii") + b"\0" + struct.pack(">II", *image.size) + argb)
        return {"width": image.width, "height": image.height, "format": PIXEL_FORMAT, "sha256": digest.hexdigest()}


def verify(receipts: Path, game_dir: Path, expected_count: int, expected_trials: set[str] | None = None, *, profile: str = "shared") -> dict:
    if expected_count <= 0:
        raise ValueError("a positive expected trial count is required to reject missing receipts")
    if profile not in ("shared", "opening"):
        raise ValueError("unknown receipt profile")
    receipts = Path(os.path.abspath(receipts))
    game_dir = Path(os.path.abspath(game_dir))
    if receipts != receipts.resolve():
        raise ValueError("symlink receipt directory")
    groups = defaultdict(list)
    errors = []
    entries = []
    if receipts.exists():
        for path in receipts.iterdir():
            entries.append(path)
            if len(entries) > MAX_RECORDS:
                raise ValueError("too many native receipt records")
    total = 0
    for path in sorted(entries):
        try:
            if path.suffix != ".json":
                raise ValueError("non-JSON evidence in receipt directory")
            data = bounded_file(path, receipts, MAX_RECORD_BYTES)
            total += len(data)
            if total > MAX_RECEIPT_BYTES:
                raise ValueError("native receipt metadata exceeds bounded size")
            record = json.loads(data)
            identity = record["identity"]
            key = (identity["runId"], identity["captureSequence"])
            if not isinstance(key[0], str) or not key[0] or type(key[1]) is not int or key[1] <= 0:
                raise ValueError("invalid capture identity")
            if not identity["trial"].startswith("articulated_shared_" if profile == "shared" else "articulated_opening_style_"):
                raise ValueError("out-of-scope trial")
            groups[key].append((path, record, hashlib.sha256(data).hexdigest()))
        except (OSError, ValueError, KeyError, TypeError, AttributeError, IndexError) as failure:
            errors.append(f"{path.name}: malformed receipt: {failure}")
    if not groups:
        errors.append("no capture receipts")
    if len({key[0] for key in groups}) > 1:
        errors.append("select exactly one native run directory")
    trials = {}
    observations = []
    image_owners = {}
    for key, records in groups.items():
        identities = [record["identity"] for _, record, _ in records]
        if any(identity != identities[0] for identity in identities[1:]):
            errors.append(f"{key}: conflicting token identities")
        trial = identities[0]["trial"]
        if trial in trials:
            errors.append(f"{trial}: multiple capture tokens; retry cannot silently satisfy coverage")
        trials[trial] = key
        # A late callback revokes an older immutable success record. Never read '*-receipt-*' alone.
        if any(record.get("verified") is not True or record.get("failures") != [] for _, record, _ in records):
            errors.append(f"{trial}: failed, incomplete or late-invalidated receipt")
            continue
        verified = [(path, record, digest) for path, record, digest in records if "receipt-verified" in record.get("stages", [])]
        if len(verified) != 1:
            errors.append(f"{trial}: expected one verified terminal association")
            continue
        receipt_path, record, receipt_digest = verified[0]
        try:
            image = record["image"]
            relative = relative_evidence_path(image["relativeImagePath"])
            path = game_dir / relative
            # Local validation must not follow a forged artifact path outside the selected game tree.
            if relative.is_absolute() or not path.resolve().is_relative_to(game_dir.resolve()):
                raise ValueError("image path escapes selected game directory")
            if path in image_owners:
                raise ValueError(f"same PNG is already associated with {image_owners[path]}")
            image_owners[path] = trial
            data = bounded_file(path, game_dir, MAX_PNG_BYTES)
            if len(data) != image["pngBytes"] or hashlib.sha256(data).hexdigest() != image["pngSha256"]:
                raise ValueError("PNG bytes changed or filename was reused")
            pixels = image_evidence(data)
            if pixels != image["decodedPixels"] or pixels != record["callbackPixels"]:
                raise ValueError("callback and unchanged decoded PNG hashes differ")
            if record["copy"]["identity"] != record["identity"]:
                raise ValueError("copy belongs to another capture")
            if record.get("exactImpactPixelCoverage") != "unverified" or record.get("nativePixelReviewRequired") is not True:
                raise ValueError("receipt overstates native pixel proof")
            observed, matched, unpaused = validate_native_record(record, pixels, profile)
            observations.append({"trial": trial, "runId": record["identity"]["runId"],
                                 "captureSequence": record["identity"]["captureSequence"],
                                 "requestedPhase": record["identity"]["requestedPhase"],
                                 "renderedPhase": record["renderedPhase"], "requestedPhaseObserved": matched,
                                 "unpausedPhaseCoverage": unpaused, "backend": observed["kind"],
                                 "move": observed["palette"]["move"], "leftHanded": observed["palette"]["leftHanded"],
                                 "skinModel": observed["attributes"]["skinModel"],
                                 "skinTexture": observed["attributes"]["skinTexture"],
                                 "rigWidth": observed["attributes"]["rigWidth"],
                                 "relativeImagePath": relative.as_posix(), "pngBytes": len(data), "pngSha256": image["pngSha256"],
                                 "receiptRelativePath": receipt_path.relative_to(receipts).as_posix(),
                                 "receiptSha256": receipt_digest})
            if profile == "opening":
                native = record["copy"]["observation"]
                observations[-1].update(expectedBackend=record["expectedBackend"], rawAcceptedPhase=record["rawAcceptedPhase"],
                                        rawRequestedPhaseObserved=record["rawRequestedPhaseObserved"],
                                        fallbackPhaseCoverage=record["expectedBackend"] == "full_fallback" and record["rawRequestedPhaseObserved"]
                                        and native["paused"] == "false" and native["frozen"] == "false",
                                        ownerUuid=native["ownerUuid"], acceptedStartTick=native["acceptedStartTick"],
                                        mainArm=observed["attributes"]["mainArm"],
                                        shellGlowPresent=observed["attributes"]["shellGlowPresent"],
                                        shellAdapterEnabled=observed["attributes"]["shellAdapterEnabled"],
                                        bannerClassification=native.get("bannerClassification", "unclassified:not_observed"),
                                        **{field: observed["attributes"][field] for field in ("head", "chest", "legs", "feet", "mainHand")})
        except (OSError, ValueError, KeyError, TypeError, AttributeError, IndexError) as failure:
            errors.append(f"{trial}: {failure}")
    if len(trials) != expected_count:
        errors.append(f"expected {expected_count} distinct trials, observed {len(trials)}")
    if expected_trials is not None and set(trials) != expected_trials:
        errors.append(f"trial manifest mismatch: missing={sorted(expected_trials - set(trials))}, unexpected={sorted(set(trials) - expected_trials)}")
    return {"associationVerified": not errors, "expectedTrials": expected_count, "observedTrials": len(trials),
            "receiptDirectory": str(receipts), "gameDirectory": str(game_dir),
            "verifiedImages": len(observations), "observations": observations, "errors": errors,
            "nativePixelReviewRequired": True, "exactImpactPixelCoverage": "unverified"}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--receipts", type=Path, required=True, help="One run's complete receipts directory, including late/failed files")
    parser.add_argument("--game-dir", type=Path, required=True)
    parser.add_argument("--expected-count", type=int, required=True, help="24 shared body, 96 shared HUD, 120 combined shared, or 72 opening")
    parser.add_argument("--profile", choices=("shared", "opening"), default="shared", help="Opening requires separate schema-2 native receipts")
    parser.add_argument("--expected-trials-file", type=Path, help="Optional exact planned trial names, one per line")
    args = parser.parse_args()
    expected = set(args.expected_trials_file.read_text().splitlines()) if args.expected_trials_file else None
    report = verify(args.receipts, args.game_dir, args.expected_count, expected, profile=args.profile)
    print(json.dumps(report, indent=2))
    raise SystemExit(0 if report["associationVerified"] else 1)


if __name__ == "__main__":
    main()
