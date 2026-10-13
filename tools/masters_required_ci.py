"""Exact, bounded evidence for the three mandatory parts of Masters 47.

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
import subprocess
import sys
import uuid

from client_suites import (ROOT, MASTERS_PART_NAMES, select_entries, exact_json_marker,
                           _unique_completion_object, _whole_class_completion, stone_march_seeds)
from native_evidence import read_text
from vps_identity import is_vps, extension

PART_PREFIX = "WILDERCORD_MASTERS_PART "
MAX_MANIFEST_BYTES = 1024 * 1024
MAX_LOG_BYTES = 16 * 1024 * 1024


def selection_hash(selection):
    return hashlib.sha256(json.dumps(selection, sort_keys=True, separators=(",", ":"),
                                     allow_nan=False).encode()).hexdigest()


def provenance(selection, identity, launch_id=None):
    if selection["name"] not in MASTERS_PART_NAMES or not exact_json_marker(
            json.dumps(selection), select_entries(suite=selection["name"])):
        raise ValueError("Required part selection must match the exact current catalog")
    vps = is_vps(identity)
    if any(not isinstance(identity[key], str) or not re.fullmatch(r"[0-9a-f]{40}", identity[key])
           for key in ("headSha", "checkoutSha", "workflowSha")):
        raise ValueError("Required parts need full exact source and workflow SHAs")
    if (identity["workflowSha"] != identity["checkoutSha"] or identity["job"] != "masters-native"
            or any(not isinstance(identity[key], str) or not re.fullmatch(r"[1-9][0-9]*", identity[key])
                   for key in ("runId", "runAttempt"))):
        raise ValueError("Required parts need the exact candidate, run, attempt and job")
    if vps and (not isinstance(launch_id, str) or str(uuid.UUID(launch_id)) != launch_id):
        raise ValueError("VPS Masters part requires its owned-launch UUID")
    if not vps and launch_id is not None:
        raise ValueError("Legacy Masters provenance cannot borrow a VPS launch")
    return {"schemaVersion": 2 if vps else 1, "gate": "masters", "part": selection["name"],
            **({"launchId": launch_id} if vps else {}),
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


def part_evidence(log, selection, env=None, *, identity=None, launch_id=None):
    issues = []
    expected = None
    try:
        expected = (launch_provenance(selection, os.environ if env is None else env) if identity is None else
                    provenance(selection, identity, launch_id))
        markers = [line[len(PART_PREFIX):] for line in log.splitlines() if line.startswith(PART_PREFIX)]
        if len(markers) != 1 or not exact_json_marker(markers[0], expected):
            issues.append("Required part head/run/attempt/selection provenance does not match this invocation")
    except (ValueError, OSError, subprocess.CalledProcessError) as exc:
        issues.append("Required part provenance is unverified: " + str(exc))
    completed, completion_issues = _whole_class_completion(log, selection["entries"], "Required Masters part", launch_id=launch_id)
    issues.extend(completion_issues)
    seeds = {}
    if selection["name"] != "masters-core":
        seeds, seed_issues = stone_march_seeds(log, selection["entries"])
        issues.extend(seed_issues)
    return {"schemaVersion": 2 if identity is not None and identity.get("executor") == "windows-vps" else 1,
            "scope": "required-masters-part", "requiredGate": "masters",
            "part": selection["name"], "provenance": expected,
            "completedEntries": completed, "observedWorldSeeds": seeds}, issues


def validate_aggregate(manifests, *, head_sha, workflow_sha, run_id, run_attempt, executor=None, computer=None):
    """Fail closed on missing/duplicate parts, stale identities and borrowed scopes."""
    issues, seen, all_entries, launches = [], set(), [], set()
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
            launch = identity.get("launchId") if executor == "windows-vps" else None
            if executor == "windows-vps":
                if launch in launches:
                    raise ValueError("VPS Masters parts must own distinct launches")
                launches.add(launch)
            expected = provenance(selection, {"headSha": head_sha, "checkoutSha": workflow_sha,
                "workflowSha": workflow_sha, "runId": run_id,
                "runAttempt": run_attempt, "job": "masters-native", **extension(executor, computer)}, launch)
            if identity["workflowSha"] != workflow_sha or not exact_json_marker(json.dumps(identity), expected):
                raise ValueError("Required parts have stale or mismatched source/run/attempt/selection provenance")
            exact_fields = {"schemaVersion": expected["schemaVersion"], "scope": "required-masters-part", "requiredGate": "masters",
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
        except (ValueError, KeyError, TypeError, AttributeError) as exc:
            issues.append(str(exc))
    full = select_entries(suite="masters")
    if seen != set(MASTERS_PART_NAMES) or len(all_entries) != 47 or len(set(all_entries)) != 47 or set(all_entries) != set(full["entries"]):
        issues.append("Required parts do not prove the exact disjoint 47-class union")
    return {"schemaVersion": 2 if executor == "windows-vps" else 1, "scope": "required-masters-aggregate", "fullClientGate": "unverified",
            "focusedClientGate": "unverified" if issues else "passed", "selection": full,
            "headSha": head_sha, "workflowSha": workflow_sha, "runId": run_id, "runAttempt": run_attempt, **extension(executor, computer),
            "parts": list(MASTERS_PART_NAMES), "verificationIssues": issues,
            "basis": "All three required Masters parts of one exact candidate/run/attempt are mandatory. "
                     "Full four-shard, articulated and connected gates and manual visual acceptance remain separate."}


def read_manifest(path):
    try:
        raw = read_text(path, MAX_MANIFEST_BYTES)
    except OSError as exc:
        raise ValueError("Required part manifest must be a readable regular non-symlink file") from exc
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
    parser.add_argument("--executor", choices=("windows-vps",))
    parser.add_argument("--computer")
    args = parser.parse_args(argv)
    try:
        if len(args.manifest) != 3:
            raise ValueError("Provide exactly three --manifest inputs")
        result = validate_aggregate([read_manifest(path) for path in args.manifest],
                                    head_sha=args.head_sha, workflow_sha=args.workflow_sha, run_id=args.run_id, run_attempt=args.run_attempt,
                                    executor=args.executor, computer=args.computer)
    except (ValueError, OSError) as exc:
        parser.error(str(exc))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result))
    return 1 if result["verificationIssues"] else 0


if __name__ == "__main__":
    sys.exit(main())
