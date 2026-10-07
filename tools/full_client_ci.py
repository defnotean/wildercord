"""Fail-closed full-client evidence. Only the all-four aggregate accepts the gate.

Native logs from older schemas are diagnostic only. Timings never prove coverage.
Optional profiles remain explicitly skipped, not passed; the required aggregate
includes the complete animation gallery but excludes Iris/showcase/extra shots.
"""
import argparse
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys
import uuid

from client_suites import (ROOT, SELECTION_PREFIX, DESCRIPTOR_PREFIX, EXIT_PREFIX,
                           select_entries, exact_json_marker, _unique_completion_object,
                           _whole_class_completion, scene_marker)
from masters_required_ci import selection_hash
from client_shard_plan import REQUIRED_PROFILE

PROVENANCE_PREFIX = "WILDERCORD_FULL_CLIENT "
PROFILE_PREFIX = "WILDERCORD_CLIENT_PROFILE "
OWNED_EXIT_PREFIX = "WILDERCORD_FULL_CLIENT_EXIT "
LAUNCH_ENV = "WILDERCORD_FULL_CLIENT_LAUNCH_ID"
MAX_LOG_BYTES = 32 * 1024 * 1024
MAX_MANIFEST_BYTES = 2 * 1024 * 1024
SHARDS = ("1/4", "2/4", "3/4", "4/4")
OPTIONAL = {
    "dev.wildercord.cast.RunicAnimationGalleryTest": ("animationGallery", "set WILDERCORD_ANIMATION_GALLERY=1 to capture the full authored gallery"),
    "dev.wildercord.gametest.WildercordShaderTest": ("shaders", "Iris is not installed in this test profile"),
    "dev.wildercord.gametest.WildercordShowcase": ("showcase", "set WILDERCORD_SHOWCASE=1 for the optional staged showcase"),
    "dev.wildercord.gametest.WildercordFireBloodShots": ("firebloodShots", "set WILDERCORD_FIREBLOOD_SHOTS=1 for the additional fire/blood close-up gallery"),
}


def read_evidence(path, limit):
    """No symlinks/devices/FIFOs, unbounded reads, or changed files."""
    fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    with os.fdopen(fd, "rb") as source:
        before = os.fstat(source.fileno())
        if not stat.S_ISREG(before.st_mode) or before.st_size > limit:
            raise ValueError("Evidence must be a bounded regular non-symlink file")
        raw = source.read(limit + 1)
        after = os.fstat(source.fileno())
    if len(raw) > limit or any(getattr(before, key) != getattr(after, key) for key in
                             ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns")):
        raise ValueError("Evidence changed during read or exceeded its size bound")
    return raw.decode("utf-8-sig")


def read_manifest(path):
    data = json.loads(read_evidence(path, MAX_MANIFEST_BYTES), object_pairs_hook=_unique_completion_object)
    if not isinstance(data, dict):
        raise ValueError("Shard manifest must be an object")
    return data


def validate_profile(profile):
    expected = {**REQUIRED_PROFILE}
    if not isinstance(profile, dict) or set(profile) != set(expected):
        raise ValueError("Missing or foreign execution profile fields")
    if profile["execution"] != "complete" or any(type(profile[key]) is not bool for key in expected if key != "execution"):
        raise ValueError("A complete explicit execution profile is required")
    # Showcase is an alternate execution mode: its presence skips ordinary tests.
    if profile["showcase"]:
        raise ValueError("Showcase cannot establish complete client execution")
    return profile


def execution_profile(env, *, shaders=False):
    allowed = {"WILDERCORD_ANIMATION_GALLERY", "WILDERCORD_FIREBLOOD_SHOTS",
               "WILDERCORD_MOON_RECEIPT_NONCE", "WILDERCORD_SHARED_RECEIPT_NONCE", LAUNCH_ENV}
    injected = {"JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "GRADLE_OPTS", "JAVA_OPTS"}
    narrowed = sorted(key for key in env if (key.startswith("WILDERCORD_") and key not in allowed) or key in injected)
    if narrowed:
        raise ValueError("Complete client execution forbids environment overrides: " + ", ".join(narrowed))
    return validate_profile({"execution": "complete", "animationGallery": env.get("WILDERCORD_ANIMATION_GALLERY") == "1",
                             "shaders": shaders, "showcase": False,
                             "firebloodShots": env.get("WILDERCORD_FIREBLOOD_SHOTS") == "1"})


def provenance(selection, identity, profile, launch_id):
    if selection.get("kind") not in ("full", "shard") or not exact_json_marker(json.dumps(selection),
            select_entries(shard=selection.get("shard"))):
        raise ValueError("Full-client selection must match the exact current full roster or shard")
    if set(identity) != {"headSha", "checkoutSha", "workflowSha", "runId", "runAttempt", "job"}:
        raise ValueError("Missing or foreign full-client provenance fields")
    if any(not isinstance(identity[key], str) or not re.fullmatch(r"[0-9a-f]{40}", identity[key])
           for key in ("headSha", "checkoutSha", "workflowSha")):
        raise ValueError("Full-client evidence requires exact full source/workflow SHAs")
    if identity["workflowSha"] != identity["checkoutSha"] or identity["job"] != "game-tests" or any(
            not isinstance(identity[key], str) or not re.fullmatch(r"[1-9][0-9]*", identity[key])
            for key in ("runId", "runAttempt")):
        raise ValueError("Full-client checkout, workflow, run, attempt or job mismatch")
    if not isinstance(launch_id, str) or str(uuid.UUID(launch_id)) != launch_id:
        raise ValueError("Full-client evidence requires a canonical owned-launch UUID")
    return {"schemaVersion": 3, "gate": "full-client", **identity, "launchId": launch_id,
            "selectionSha256": selection_hash(selection), "fullSelectionSha256": selection_hash(select_entries()),
            "plan": selection["plan"], "profile": validate_profile(profile)}


def current_identity(env):
    def git(*args):
        return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()
    if git("status", "--porcelain", "--untracked-files=no"):
        raise ValueError("Full-client checkout has tracked source changes")
    return {"headSha": env.get("FULL_CLIENT_HEAD_SHA"), "checkoutSha": git("rev-parse", "HEAD"),
            "workflowSha": env.get("GITHUB_SHA"), "runId": env.get("GITHUB_RUN_ID"),
            "runAttempt": env.get("GITHUB_RUN_ATTEMPT"), "job": env.get("GITHUB_JOB")}


def suite_results(selection, profile):
    return [{"suite": entry, "status": "skipped" if entry in OPTIONAL and not profile[OPTIONAL[entry][0]] else "passed"}
            for entry in selection["entries"]]


def run_evidence(log, selection, profile, *, identity=None):
    issues, found, completed = [], None, []
    launch_id = None
    lines = log.splitlines()
    prefixes = (SELECTION_PREFIX, PROVENANCE_PREFIX, DESCRIPTOR_PREFIX, PROFILE_PREFIX, EXIT_PREFIX, OWNED_EXIT_PREFIX)
    for line in lines:
        if any(prefix.rstrip() in line for prefix in prefixes) and not any(line.startswith(prefix) for prefix in prefixes):
            issues.append("Malformed or embedded full-client launcher marker")
    try:
        markers = [line[len(PROVENANCE_PREFIX):] for line in lines if line.startswith(PROVENANCE_PREFIX)]
        if len(markers) != 1:
            raise ValueError("Exactly one full-client provenance marker is required")
        found = json.loads(markers[0], object_pairs_hook=_unique_completion_object)
        expected = provenance(selection, current_identity(os.environ) if identity is None else identity,
                              profile, found["launchId"])
        if not exact_json_marker(markers[0], expected):
            raise ValueError("Full-client source/run/attempt/roster/profile provenance mismatch")
        launch_id = expected["launchId"]
        owned_exit = {"launchId": launch_id, "exitCode": 0}
        for prefix, value in ((SELECTION_PREFIX, selection), (DESCRIPTOR_PREFIX, selection),
                              (PROFILE_PREFIX, {"launchId": expected["launchId"], "profile": profile}),
                              (OWNED_EXIT_PREFIX, owned_exit)):
            markers = [line[len(prefix):] for line in lines if line.startswith(prefix)]
            if len(markers) != 1 or not exact_json_marker(markers[0], value):
                raise ValueError("Missing, duplicate or mismatched " + prefix.strip())
    except (ValueError, KeyError, TypeError, AttributeError, OSError, subprocess.CalledProcessError) as exc:
        issues.append("Full-client evidence is unverified: " + str(exc))
    completed, completion_issues = _whole_class_completion(log, selection["entries"], "Full-client selection", launch_id=launch_id)
    issues.extend(completion_issues)
    # Bind every selected class inside this one descriptor/profile/owned-exit boundary.
    boundaries = []
    for line in lines:
        for prefix, name in ((SELECTION_PREFIX, "selection"), (PROVENANCE_PREFIX, "provenance"),
                             (DESCRIPTOR_PREFIX, "descriptor"), (PROFILE_PREFIX, "profile"),
                             (EXIT_PREFIX, "exit"), (OWNED_EXIT_PREFIX, "owned-exit")):
            if line.startswith(prefix):
                boundaries.append(name)
        try:
            if scene_marker(line, launch_id=launch_id) is not None:
                boundaries.append("scene")
        except (ValueError, KeyError, TypeError, OverflowError):
            boundaries.append("invalid-scene")
        if re.fullmatch(r"BUILD SUCCESSFUL(?: in .+)?", line):
            boundaries.append("build")
    expected_boundaries = ["selection", "provenance", "descriptor", "profile"] + ["scene"] * (4 * selection["count"]) + ["build", "exit", "owned-exit"]
    if boundaries != expected_boundaries or [line for line in lines if line.startswith(EXIT_PREFIX)] != [EXIT_PREFIX + "0"]:
        issues.append("Full-client evidence lacks ordered complete launch/lifecycle/success boundaries")
    if "BUILD FAILED" in log or "WILDERCORD_NATIVE_REQUEST " in log or "WILDERCORD_MASTERS_PART " in log:
        issues.append("Failed or borrowed focused/diagnostic evidence cannot establish full-client execution")
    # Only exact documented optional skip messages are allowed, in their owning run phase.
    expected_skips = {entry: OPTIONAL[entry][1] for entry in selection["entries"]
                      if entry in OPTIONAL and not profile[OPTIONAL[entry][0]]}
    observed_skips, current = [], None
    for line in lines:
        try:
            event = scene_marker(line, launch_id=launch_id)
            if event is not None:
                current = event["suite"] if event["phase"] == "run" else None
        except (ValueError, KeyError, TypeError, OverflowError):
            current = None
        if "[TEST SKIP]" in line:
            message = line.split("[TEST SKIP]", 1)[1].strip()
            expected_message = (current.rsplit(".", 1)[-1] + ": " + expected_skips[current]) if current in expected_skips else None
            if message != expected_message:
                issues.append("Unexpected, required, or out-of-lifecycle suite skip")
            observed_skips.append(current)
    if observed_skips != list(expected_skips):
        issues.append("Optional skips must be explicit, exact, once, and in selected class order")
    return {"schemaVersion": 3, "scope": "full-client-shard" if selection["kind"] == "shard" else "full-client-run",
            "provenance": found, "completedEntries": completed, "lifecycleLaunchId": launch_id, "profile": profile}, issues


def validate_aggregate(manifests, *, head_sha, workflow_sha, run_id, run_attempt, shards_result="success", download_result="success"):
    issues, seen, entries, launches = [], set(), [], set()
    if len(manifests) != 4:
        issues.append("Exactly four shard manifests are required")
    if download_result != "success":
        issues.append("Trusted same-run artifact download must succeed")
    if shards_result != "success":
        issues.append("All four shard jobs, manifest writes and evidence uploads must succeed")
    identity = {"headSha": head_sha, "checkoutSha": workflow_sha, "workflowSha": workflow_sha,
                "runId": run_id, "runAttempt": run_attempt, "job": "game-tests"}
    for manifest in manifests:
        try:
            shard = manifest["shard"]
            if shard not in SHARDS or shard in seen:
                raise ValueError("Missing, foreign or duplicate full-client shard")
            seen.add(shard)
            selection = select_entries(shard=shard)
            launch = manifest["provenance"]["launchId"]
            if launch in launches:
                raise ValueError("Shard manifests must own distinct native launches")
            launches.add(launch)
            expected = provenance(selection, identity, REQUIRED_PROFILE, launch)
            results = suite_results(selection, REQUIRED_PROFILE)
            exact = {"schemaVersion": 3, "scope": "full-client-shard", "shard": shard,
                     "selection": selection, "provenance": expected, "profile": REQUIRED_PROFILE,
                     "shardOutcome": "passed", "fullClientGate": "unverified", "verificationIssues": [],
                     "completedEntries": selection["entries"], "lifecycleLaunchId": launch, "clientSuites": results,
                     "counts": {status: sum(item["status"] == status for item in results)
                                for status in ("passed", "skipped", "unverified")}}
            if not exact_json_marker(json.dumps({key: manifest.get(key) for key in exact}), exact):
                raise ValueError("Shard is incomplete, stale, failed, or has mismatched source/run/attempt/profile/scope")
            if any(key in manifest for key in ("focusedClientGate", "diagnosticOutcome", "requiredPartOutcome", "fullRunOutcome")):
                raise ValueError("Borrowed focused/diagnostic/full-run evidence cannot certify a shard")
            entries.extend(selection["entries"])
        except (ValueError, KeyError, TypeError, AttributeError) as exc:
            issues.append(str(exc))
    full = select_entries()
    if seen != set(SHARDS) or len(entries) != full["count"] or len(set(entries)) != full["count"] or set(entries) != set(full["entries"]):
        issues.append("All four shards must prove the exact disjoint current full-roster union")
    results = suite_results(full, REQUIRED_PROFILE) if not issues else []
    return {"schemaVersion": 3, "scope": "full-client-aggregate", "fullClientGate": "unverified" if issues else "passed",
            "selection": full, "fullSelectionSha256": selection_hash(full), "profile": REQUIRED_PROFILE,
            "plan": full["plan"],
            **identity, "shards": list(SHARDS), "verificationIssues": issues, "clientSuites": results,
            "basis": "All four matching completed shards and successful evidence uploads are mandatory. Optional skipped profiles are not passed coverage. Manual visual acceptance is separate."}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    inputs = parser.add_mutually_exclusive_group(required=True)
    inputs.add_argument("--manifest", type=Path, action="append")
    inputs.add_argument("--artifact-root", type=Path)
    for name in ("head-sha", "workflow-sha", "run-id", "run-attempt"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--shards-result", required=True)
    parser.add_argument("--download-result", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    errors, manifests = [], []
    paths = args.manifest or []
    if args.artifact_root is not None:
        # download-artifact keeps one exact same-run artifact directory per shard.
        for shard in range(1, 5):
            paths.append(args.artifact_root / f"full-client-shard-{shard}-{args.run_id}-{args.run_attempt}" / "test-manifest.json")
        if args.artifact_root.is_symlink():
            errors.append("Artifact root cannot be a symlink")
        if args.artifact_root.exists() and sorted(p.name for p in args.artifact_root.iterdir()) != sorted(p.parent.name for p in paths):
            errors.append("Missing or foreign shard artifact directory")
    for path in paths:
        try:
            if path.parent.is_symlink():
                raise ValueError("Shard artifact directory cannot be a symlink")
            manifests.append(read_manifest(path))
        except (ValueError, OSError) as exc:
            errors.append(f"Unreadable shard evidence {path.name}: {exc}")
    result = validate_aggregate(manifests, head_sha=args.head_sha, workflow_sha=args.workflow_sha,
                                run_id=args.run_id, run_attempt=args.run_attempt, shards_result=args.shards_result,
                                download_result=args.download_result)
    result["verificationIssues"].extend(errors)
    if result["verificationIssues"]:
        result["fullClientGate"], result["clientSuites"] = "unverified", []
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"fullClientGate": result["fullClientGate"], "verificationIssues": result["verificationIssues"]}))
    return 1 if result["verificationIssues"] else 0


if __name__ == "__main__":
    sys.exit(main())
