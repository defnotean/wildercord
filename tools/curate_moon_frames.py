"""Preserve <14 MB of original paired/owner Moon diagnostics and exact provenance.

No resizing, retiming, image generation or visual acceptance. Complete native
artifacts remain authoritative; omitted/rejected/missing evidence is explicit.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import uuid

from native import launch_two_clients as s
from native import run_paired_matrix as matrix
from verify_articulated_render_receipts import image_evidence

ROOT = s.ROOT
BUDGET = 14_000_000
RESERVE = 128_000
MAX_RECEIPT = 512_000
MAX_LOG = 64 * 1024 * 1024
PREFIX = "crimson_moon_owner_"
MARKER = re.compile(r"CRIMSON_MOON_OWNER_RECEIPT name=(\S+) verified=(true|false) actualSourceAge=(\S+) receipt=(\S+) phaseBasis=authored_post_hitstop_source_palette serverReleaseFrameCorrespondenceVerified=false pixelQualityReviewed=false")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def canonical_nonce(value):
    s.require(isinstance(value, str) and str(uuid.UUID(value)) == value, "Missing canonical launch nonce")
    return value


def receipt_from_log(game, log, name):
    s.require(log.is_file() and not log.is_symlink() and log.stat().st_size <= MAX_LOG, "Missing or oversized current native log")
    matches = [m for m in MARKER.finditer(log.read_text(errors="replace")) if m[1] == name]
    s.require(len(matches) == 1, "Need exactly one current log receipt marker for " + name)
    value = Path(matches[0][4])
    s.require(value.is_absolute() and value.is_relative_to(game), "Log receipt is outside the selected game directory")
    relative = value.relative_to(game)
    s.require(len(relative.parts) == 4 and relative.parts[:2] == ("screenshots", "crimson-moon-owner-receipts")
              and re.fullmatch(r"[0-9]{6}\.json", relative.name), "Unexpected Moon receipt path")
    canonical_nonce(relative.parts[2])
    return matrix.safe_path(game, relative), matches[0][2] == "true"


def frame(game, log, name, nonce, skin, *, paired=False):
    """Require current nonce/name and original bytes even for rejected diagnostics."""
    receipt, logged_verified = receipt_from_log(game, log, name)
    record = matrix.read_json(receipt, MAX_RECEIPT)
    s.require(record.get("schemaVersion") == (3 if paired else 2) and record.get("launchNonce") == nonce,
              "Receipt is not from the current owned launch")
    expected = record.get("expected", {})
    s.require(expected.get("name") == name and str(expected.get("skin", "")).lower() == skin
              and expected.get("view") == ("remote" if "_remote_" in name else "fp")
              and expected.get("mode") == "articulated" and expected.get("hand") == "RIGHT"
              and expected.get("armor") is True and expected.get("shell") is True
              and expected.get("phase") == "ACTIVE" and expected.get("age") == 10,
              "Receipt changed the fixed name, skin, action or appearance")
    s.require(type(expected.get("activation")) is int and type(expected.get("owner")) is int
              and expected["owner"] > 0 and isinstance(expected.get("uuid"), str), "Missing actual accepted-action identity")
    s.require(record.get("serverReleaseFrameCorrespondenceVerified") is False and record.get("pixelQualityReviewed") is False,
              "Receipt overstates server-frame or visual proof")
    png = matrix.safe_path(game, Path("screenshots") / (name + ".png"))
    s.require(png.is_file() and png.stat().st_size <= 64 * 1024 * 1024, "Missing or oversized original PNG")
    data = png.read_bytes()
    pixels = image_evidence(data)  # Includes all PNG chunk CRCs; never re-encodes.
    image = record.get("image", {})
    s.require(image.get("relativeImagePath") == str(png.relative_to(game))
              and image.get("pngBytes") == len(data) and image.get("pngSha256") == sha(data)
              and image.get("decodedPixels") == pixels and record.get("callbackPixels") == pixels,
              "Original PNG/hash/callback binding is missing or changed")
    receipt_bytes = receipt.read_bytes()
    observation = (record.get("copy") or {}).get("observations", {})
    return {"name": name, "nonce": nonce, "skin": skin, "action": expected,
            "actualSourceAge": observation.get("actualSourceAge"),
            "pngSha256": sha(data), "pngBytes": len(data), "decodedPixels": pixels,
            "allPngChunkCrcsVerified": True, "receiptSha256": sha(receipt_bytes),
            "receiptBytes": len(receipt_bytes), "originalPng": str(png.relative_to(game)),
            "originalReceipt": str(receipt.relative_to(game)),
            "recordReportsVerified": record.get("verified") is True,
            "logReportsVerified": logged_verified,
            "nativeRejections": record.get("failures", []),
            "serverReleaseFrameCorrespondenceVerified": False, "pixelQualityReviewed": False}, data, receipt_bytes


def package(root, output, report, candidates):
    out = matrix.safe_path(root, output)
    s.require(not out.exists(), "Curated output must be fresh")
    out.mkdir(parents=True)
    total = 0
    report.update(schemaVersion=1, budgetBytes=BUDGET, byteIdenticalOriginals=True,
                  serverReleaseFrameCorrespondenceVerified=False, pixelQualityReviewed=False, frames=[])
    for item, png, receipt in candidates:
        item = dict(item)
        if total + len(png) + len(receipt) >= BUDGET - RESERVE:
            item.update(included=False, omission="Original bytes exceed remaining bounded artifact budget; retained in full native artifact")
        else:
            stem = item["group"] + "-" + item["role"]
            label = "machine-verified" if item.get("causalGroupVerified") else "diagnostic"
            png_name, receipt_name = f"{label}-{stem}.png", f"{label}-{stem}.json"
            (out / png_name).write_bytes(png)
            (out / receipt_name).write_bytes(receipt)
            s.require(sha((out / png_name).read_bytes()) == item["pngSha256"]
                      and sha((out / receipt_name).read_bytes()) == item["receiptSha256"], "Copied original evidence changed")
            item.update(included=True, png=png_name, receipt=receipt_name)
            total += len(png) + len(receipt)
        report["frames"].append(item)
    report["originalBytesIncluded"] = total
    data = matrix.encode(report)
    s.require(len(data) < RESERVE and total + len(data) < BUDGET, "Bounded Moon manifest exceeds artifact budget")
    (out / "manifest.json").write_bytes(data)
    return report


def paired(root, source, output):
    source = matrix.safe_path(root, source)
    current = matrix.provenance(root)
    report = {"purpose": "paired-moon-original-frames", "provenance": current,
              "fullArtifact": "connected-combat-native-" + (current["runId"] or "local") + "-" + (current["runAttempt"] or "0"),
              "matrixVerified": False, "errors": [], "missing": []}
    candidates, nonces = [], set()
    matrix_result = matrix.read_json(source / "matrix-result.json")
    s.require(matrix_result.get("provenance") == current, "Matrix belongs to a different exact head/run")
    groups = matrix_result.get("groups", [])
    s.require(len(groups) == len(matrix.GROUPS)
              and all(all(actual.get(k) == value for k, value in expected.items()) for actual, expected in zip(groups, matrix.GROUPS)),
              "Missing or changed fixed cast/Moon group ledger")
    passed = 0
    for entry, group in zip(groups, matrix.GROUPS):
        base = matrix.safe_path(source, group["id"])
        verified = False
        try:
            result = matrix.validate_group(base, group, current, nonces)
            s.require(entry.get("status") == "passed" and entry.get("nonce") == result["nonce"]
                      and entry.get("runIdentity") == result["runIdentity"]
                      and entry.get("resultSha256") == s.digest(base / "result.json"), "Matrix summary does not bind the exact result")
            launch_path = source / (group["suite"] + "-launch.json")
            launch = matrix.read_json(launch_path)
            s.require(s.digest(launch_path) == result["launchSha256"]
                      and s.digest(source / (group["suite"] + "-descriptor.json")) == result["descriptorSha256"]
                      and launch.get("descriptorSha256") == result["descriptorSha256"], "Export/descriptor provenance changed")
            verified = True
            passed += 1
        except (ValueError, KeyError, TypeError, OSError) as error:
            report["errors"].append({"group": group["id"], "error": str(error)[:500]})
        if group["suite"] != s.MOON_SUITE:
            continue
        try:
            proof = matrix.read_json(base / "launch-proof.json")
            nonce = canonical_nonce(proof.get("nonce"))
            for key in ("sourceHead", "checkoutSha", "prHeadSha"):
                s.require(proof.get(key) == current[key], "Diagnostic launch changed " + key)
            selection = matrix.selected(group)
            for key in ("suite", "profile", "cases", "case", "expectedSkin", "observerAngle"):
                s.require(proof.get(key) == selection[key], "Diagnostic launch changed selected " + key)
            s.require(proof.get("ci") == {"GITHUB_RUN_ID": current["runId"], "GITHUB_RUN_ATTEMPT": current["runAttempt"], "GITHUB_JOB": current["job"]}
                      and proof.get("runIdentity") == "-".join([current["runId"] or "local", current["runAttempt"] or "0", nonce]), "Diagnostic launch changed run identity")
            for role, view in (("peer", "remote"), ("host", "fp")):
                name = PREFIX + view + "_" + selection["expectedSkin"] + "_" + group["observerAngle"] + "_" + s.MOON_CASE
                try:
                    item, png, receipt = frame(base / role, base / (role + ".log"), name, nonce, selection["expectedSkin"], paired=True)
                    causal = verified
                    if causal:
                        try:
                            witness = s.read_properties(base / "ipc" / (role + "-case-00-observed.properties"))
                            bindings = {"receiptRelativePath": item["originalReceipt"], "receiptSha256": item["receiptSha256"],
                                        "pngRelativePath": item["originalPng"], "pngSha256": item["pngSha256"],
                                        "callbackPixelSha256": item["decodedPixels"]["sha256"], "screenshotName": name}
                            s.require(all(witness.get(k) == value for k, value in bindings.items())
                                      and item["recordReportsVerified"] and item["logReportsVerified"] and not item["nativeRejections"],
                                      "Logged receipt is not the exact validated role image witness")
                        except (ValueError, KeyError, TypeError, OSError) as error:
                            causal = False
                            report["errors"].append({"group": group["id"], "role": role, "error": str(error)[:500]})
                    item.update(group=group["id"], role=role, observerAngle=group["observerAngle"],
                                runIdentity=proof["runIdentity"], causalGroupVerified=causal,
                                releaseImageDamageOrderVerified=causal, diagnostic=not causal)
                    candidates.append((item, png, receipt))
                except (ValueError, KeyError, TypeError, OSError) as error:
                    report["missing"].append({"group": group["id"], "role": role, "error": str(error)[:500]})
        except (ValueError, KeyError, TypeError, OSError) as error:
            report["missing"].append({"group": group["id"], "error": str(error)[:500]})
    report["matrixVerified"] = (matrix_result.get("status") == "passed" and passed == len(matrix.GROUPS)
                                and len(candidates) == 8 and not report["missing"]
                                and all(item["causalGroupVerified"] for item, _, _ in candidates)
                                and matrix_result.get("serverReleaseFrameCorrespondenceVerified") is False
                                and matrix_result.get("pixelQualityReviewed") is False
                                and matrix_result.get("releaseImageDamageOrderVerified") is True)
    report["matrixResultSha256"] = s.digest(source / "matrix-result.json")
    # Cover each observer angle/skin first, then owner FP. Originals that do not
    # fit stay in the complete artifact, with their hashes/paths in this index.
    candidates.sort(key=lambda entry: (entry[0]["role"] != "peer", entry[0]["group"]))
    return package(root, output, report, candidates)


def owner(root, output, log=Path("articulated-native.log")):
    from curate_masters_frames import provenance
    identity = provenance(root, os.environ)
    launch_path = matrix.safe_path(root, Path("artifacts/review/moon-owner-launch.json"))
    launch = matrix.read_json(launch_path)
    nonce = canonical_nonce(launch.get("nonce"))
    s.require(launch.get("schemaVersion") == 1 and launch.get("purpose") == "owner-fp-native-diagnostic"
              and launch.get("checkoutSha") == identity["checkedOutSha"] and launch.get("runId") == identity["runId"]
              and launch.get("runAttempt") == identity["runAttempt"] and launch.get("remoteObserverCoverage") is False
              and launch.get("serverReleaseFrameCorrespondenceVerified") is False, "Owner launch belongs to a different head/run or proof scope")
    s.require("dev.wildercord.client.combat.CrimsonMoonCaptureTest" in launch.get("selection", {}).get("entries", []), "Owner capture was not selected")
    game = matrix.safe_path(root, Path("build/run/clientGameTest"))
    candidates, errors = [], []
    current_log = matrix.safe_path(root, log)
    for skin in ("wide", "slim"):
        name = PREFIX + "fp_" + skin + "_" + s.MOON_CASE
        try:
            item, png, receipt = frame(game, current_log, name, nonce, skin)
            item.update(group="owner-fp", role=skin, observerAngle=None, causalGroupVerified=False,
                        releaseImageDamageOrderVerified=False, remoteObserverCoverage=False, diagnostic=True)
            candidates.append((item, png, receipt))
        except (ValueError, KeyError, TypeError, OSError) as error:
            errors.append({"skin": skin, "error": str(error)[:500]})
    s.require(len(candidates) <= 1, "The bounded owner diagnostic admits one actual original skin/case")
    return package(root, output, {"purpose": "owner-fp-native-diagnostic", "provenance": identity,
                   "launchNonce": nonce, "launchSha256": s.digest(launch_path), "remoteObserverCoverage": False,
                   "releaseImageDamageOrderVerified": False, "matrixVerified": False, "errors": errors,
                   "fullArtifact": "articulated-native-evidence"}, candidates)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--owner", action="store_true")
    parser.add_argument("--source", type=Path, default=Path("build/native/paired-matrix"))
    parser.add_argument("--output", type=Path, required=True)
    options = parser.parse_args(argv)
    if options.owner:
        return owner(ROOT, options.output)
    return paired(ROOT, options.source, options.output)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError) as error:
        print("Moon original-frame curation failed: " + str(error), file=sys.stderr)
        raise SystemExit(1)
