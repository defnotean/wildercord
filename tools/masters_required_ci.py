"""Exact, bounded evidence for the three mandatory parts of Masters 44.

Part manifests never establish focused/full success alone. This offline validator
requires all three passed parts of one head, workflow run and attempt. It does not
create another CI job or replace any required GitHub check.
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

from client_suites import (ROOT, MASTERS_PART_NAMES, select_entries, exact_json_marker,
                           _unique_completion_object, _whole_class_completion, stone_march_seeds)

PART_PREFIX = "WILDERCORD_MASTERS_PART "
MAX_MANIFEST_BYTES = 1024 * 1024
MAX_LOG_BYTES = 16 * 1024 * 1024


def selection_hash(selection):
    return hashlib.sha256(json.dumps(selection, sort_keys=True, separators=(",", ":"),
                                     allow_nan=False).encode()).hexdigest()


def provenance(selection, identity):
    if selection["name"] not in MASTERS_PART_NAMES or not exact_json_marker(
            json.dumps(selection), select_entries(suite=selection["name"])):
        raise ValueError("Required part selection must match the exact current catalog")
    if set(identity) != {"headSha", "checkoutSha", "workflowSha", "runId", "runAttempt", "job"}:
        raise ValueError("Missing or unknown required-part provenance fields")
    if any(not isinstance(identity[key], str) or not re.fullmatch(r"[0-9a-f]{40}", identity[key])
           for key in ("headSha", "checkoutSha", "workflowSha")):
        raise ValueError("Required parts need full exact source and workflow SHAs")
    if (identity["workflowSha"] != identity["checkoutSha"] or identity["job"] != "masters-native"
            or any(not isinstance(identity[key], str) or not re.fullmatch(r"[1-9][0-9]*", identity[key])
                   for key in ("runId", "runAttempt"))):
        raise ValueError("Required parts need the exact candidate, run, attempt and job")
    return {"schemaVersion": 1, "gate": "masters", "part": selection["name"],
            **identity, "selectionSha256": selection_hash(selection),
            "mastersSelectionSha256": selection_hash(select_entries(suite="masters"))}


def launch_provenance(selection, env):
    def git(*args):
        return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()
    if git("status", "--porcelain", "--untracked-files=no"):
        raise ValueError("Required Masters part checkout has tracked source changes")
    return provenance(selection, {"headSha": env.get("MASTERS_HEAD_SHA"),
                                  "checkoutSha": git("rev-parse", "HEAD"),
                                  "workflowSha": env.get("GITHUB_SHA"),
                                  "runId": env.get("GITHUB_RUN_ID"),
                                  "runAttempt": env.get("GITHUB_RUN_ATTEMPT"),
                                  "job": env.get("GITHUB_JOB")})


def part_evidence(log, selection, env=None):
    issues = []
    expected = None
    try:
        expected = launch_provenance(selection, os.environ if env is None else env)
        markers = [line[len(PART_PREFIX):] for line in log.splitlines() if line.startswith(PART_PREFIX)]
        if len(markers) != 1 or not exact_json_marker(markers[0], expected):
            issues.append("Required part head/run/attempt/selection provenance does not match this invocation")
    except (ValueError, OSError, subprocess.CalledProcessError) as exc:
        issues.append("Required part provenance is unverified: " + str(exc))
    completed, completion_issues = _whole_class_completion(log, selection["entries"], "Required Masters part")
    issues.extend(completion_issues)
    seeds = {}
    if selection["name"] != "masters-core":
        seeds, seed_issues = stone_march_seeds(log, selection["entries"])
        issues.extend(seed_issues)
    return {"schemaVersion": 1, "scope": "required-masters-part", "requiredGate": "masters",
            "part": selection["name"], "provenance": expected,
            "completedEntries": completed, "observedWorldSeeds": seeds}, issues


def validate_aggregate(manifests, *, head_sha, workflow_sha, run_id, run_attempt):
    """Fail closed on missing/duplicate parts, stale identities and borrowed scopes."""
    issues, seen, all_entries = [], set(), []
    if len(manifests) != 3:
        issues.append("Exactly three required Masters part manifests are required")
    for manifest in manifests:
        try:
            part = manifest["part"]
            if part not in MASTERS_PART_NAMES or part in seen:
                raise ValueError("Missing, foreign or duplicate required Masters part")
            seen.add(part)
            selection = select_entries(suite=part)
            identity = manifest["provenance"]
            expected = provenance(selection, {"headSha": head_sha, "checkoutSha": workflow_sha,
                "workflowSha": workflow_sha, "runId": run_id,
                "runAttempt": run_attempt, "job": "masters-native"})
            if identity["workflowSha"] != workflow_sha or not exact_json_marker(json.dumps(identity), expected):
                raise ValueError("Required parts have stale or mismatched source/run/attempt/selection provenance")
            exact_fields = {"schemaVersion": 1, "scope": "required-masters-part", "requiredGate": "masters",
                "part": part, "selection": selection, "requiredPartOutcome": "passed",
                "fullClientGate": "unverified", "focusedClientGate": "unverified", "verificationIssues": [],
                "completedEntries": selection["entries"],
                "counts": {"passed": selection["count"], "skipped": 0, "unverified": 0},
                "clientSuites": [{"suite": entry, "status": "passed"} for entry in selection["entries"]]}
            if not exact_json_marker(json.dumps({key: manifest.get(key) for key in exact_fields}), exact_fields):
                raise ValueError("Required part is incomplete, failed or has the wrong evidence scope")
            seeds = manifest["observedWorldSeeds"]
            if part == "masters-core":
                if seeds != {}:
                    raise ValueError("Core part cannot claim March seed evidence")
            else:
                if not isinstance(seeds, dict) or list(seeds) != selection["entries"]:
                    raise ValueError("Required March part has missing, foreign or reordered seeds")
                seed_log = ""
                for entry, values in seeds.items():
                    if not isinstance(values, list) or len(values) != 1:
                        raise ValueError("Required March part must have exactly one seed per class")
                    seed_log += "WILDERCORD_NATIVE_WORLD " + json.dumps({"suite": entry, "seed": values[0]}) + "\n"
                if stone_march_seeds(seed_log, selection["entries"])[1]:
                    raise ValueError("Invalid required March seed evidence")
            all_entries.extend(selection["entries"])
        except (ValueError, KeyError, TypeError) as exc:
            issues.append(str(exc))
    full = select_entries(suite="masters")
    if seen != set(MASTERS_PART_NAMES) or len(all_entries) != 44 or len(set(all_entries)) != 44 or set(all_entries) != set(full["entries"]):
        issues.append("Required parts do not prove the exact disjoint 44-class union")
    return {"schemaVersion": 1, "scope": "required-masters-aggregate", "fullClientGate": "unverified",
            "focusedClientGate": "unverified" if issues else "passed", "selection": full,
            "headSha": head_sha, "workflowSha": workflow_sha, "runId": run_id, "runAttempt": run_attempt,
            "parts": list(MASTERS_PART_NAMES), "verificationIssues": issues,
            "basis": "All three required Masters parts of one exact candidate/run/attempt are mandatory. "
                     "Full four-shard, articulated and connected gates and manual visual acceptance remain separate."}


def read_manifest(path):
    try:
        descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    except OSError as exc:
        raise ValueError("Required part manifest must be a readable regular non-symlink file") from exc
    with os.fdopen(descriptor, "rb") as source:
        before = os.fstat(source.fileno())
        if not stat.S_ISREG(before.st_mode) or before.st_size > MAX_MANIFEST_BYTES:
            raise ValueError("Required part manifest is not a bounded regular file")
        raw = source.read(MAX_MANIFEST_BYTES + 1)
        after = os.fstat(source.fileno())
    if (len(raw) > MAX_MANIFEST_BYTES or any(getattr(before, key) != getattr(after, key)
            for key in ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns"))):
        raise ValueError("Required part manifest changed or exceeded its bounded size")
    result = json.loads(raw, object_pairs_hook=_unique_completion_object)
    if not isinstance(result, dict):
        raise ValueError("Required part manifest must be an object")
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, action="append", required=True)
    parser.add_argument("--head-sha", required=True)
    parser.add_argument("--workflow-sha", required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--run-attempt", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        if len(args.manifest) != 3:
            raise ValueError("Provide exactly three --manifest inputs")
        result = validate_aggregate([read_manifest(path) for path in args.manifest],
                                    head_sha=args.head_sha, workflow_sha=args.workflow_sha, run_id=args.run_id, run_attempt=args.run_attempt)
    except (ValueError, OSError) as exc:
        parser.error(str(exc))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result))
    return 1 if result["verificationIssues"] else 0


if __name__ == "__main__":
    sys.exit(main())
