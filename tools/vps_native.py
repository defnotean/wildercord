"""VPS native selection, owned launch markers and strict completed-run manifests.

No process is launched here. The Windows runner owns the child and records its
actual exit; Gradle and Minecraft must supply descriptor/profile/lifecycle proof.
"""
import hashlib
import json
import re
import uuid

import client_suites as suites
import full_client_ci as full
from vps_identity import make_identity

SUITES = ("release-smoke",)
PREFIX = "WILDERCORD_VPS_NATIVE "


def prepare(*, head, computer, batch, attempt, launch, environment, suite=None, shard=None):
    if shard is not None or suite not in SUITES:
        raise ValueError("Select the complete release-smoke suite")
    selection = suites.select_entries(suite=suite, shard=shard)
    if str(uuid.UUID(launch)) != launch:
        raise ValueError("VPS launch must own a canonical UUID")
    identity = make_identity(head, computer, batch, attempt,
        "smoke-native")
    child = dict(environment)
    child[full.LAUNCH_ENV] = launch
    profile = None
    flags = ["-PciSuite=" + suite]
    proof, prefix = {"schemaVersion": 1, **identity, "launchId": launch,
                     "selectionSha256": hashlib.sha256(json.dumps(selection, sort_keys=True, separators=(",", ":")).encode()).hexdigest()}, PREFIX
    preamble = [suites.SELECTION_PREFIX + json.dumps(selection, sort_keys=True),
                prefix + json.dumps(proof, sort_keys=True)]
    return {"selection": selection, "identity": identity, "launch": launch, "profile": profile,
            "proof": proof, "prefix": prefix, "preamble": preamble, "flags": flags,
            "environment": child, "moonReceipt": None}


def exit_markers(plan, code):
    return [suites.EXIT_PREFIX + str(code), full.OWNED_EXIT_PREFIX + json.dumps(
        {"launchId": plan["launch"], "exitCode": code}, sort_keys=True)]


def group_boundaries(log, plan):
    """Every non-shard class must finish inside this one owned invocation."""
    selection, launch = plan["selection"], plan["launch"]
    issues = []
    expected_markers = [(suites.SELECTION_PREFIX, selection), (plan["prefix"], plan["proof"]),
                        (suites.DESCRIPTOR_PREFIX, selection),
                        (full.OWNED_EXIT_PREFIX, {"launchId": launch, "exitCode": 0})]
    prefixes = [prefix for prefix, _ in expected_markers] + [suites.EXIT_PREFIX]
    for prefix, value in expected_markers:
        markers = [line[len(prefix):] for line in log.splitlines() if line.startswith(prefix)]
        try:
            matches = len(markers) == 1 and suites.exact_json_marker(markers[0], value)
        except (ValueError, TypeError):
            matches = False
        if not matches:
            issues.append("Missing or mismatched VPS marker: " + prefix.strip())
    boundaries = []
    for line in log.splitlines():
        if any(prefix.rstrip() in line for prefix in prefixes) and not any(line.startswith(prefix) for prefix in prefixes):
            issues.append("Malformed or embedded VPS launch marker")
        for prefix in prefixes:
            if line.startswith(prefix):
                boundaries.append(prefix)
        try:
            if suites.scene_marker(line, launch_id=launch) is not None:
                boundaries.append("scene")
        except (ValueError, KeyError, TypeError, OverflowError):
            boundaries.append("invalid-scene")
        if re.fullmatch(r"BUILD SUCCESSFUL(?: in .+)?", line):
            boundaries.append("build")
    expected = [prefix for prefix, _ in expected_markers[:3]] + ["scene"] * (selection["count"] * 4)
    expected += ["build", suites.EXIT_PREFIX, full.OWNED_EXIT_PREFIX]
    if boundaries != expected:
        issues.append("VPS group lacks ordered complete owned launch/lifecycle/exit boundaries")
    if "[TEST SKIP]" in log or "BUILD FAILED" in log or suites.REQUEST_PREFIX in log or full.PROVENANCE_PREFIX in log:
        issues.append("Failed, skipped or borrowed evidence cannot pass the VPS group")
    return issues


def manifest(log, plan):
    selection, identity, launch = plan["selection"], plan["identity"], plan["launch"]
    issues = suites.selection_issues(log, selection, launch_id=launch)
    issues.extend(group_boundaries(log, plan))
    completed, extra = suites._whole_class_completion(log, selection["entries"], "VPS smoke", launch_id=launch)
    proof = {"schemaVersion": 1, "scope": "vps-release-smoke", "provenance": plan["proof"],
             "completedEntries": completed}
    outcome = "focusedClientGate"
    results = [{"suite": entry, "status": "passed"} for entry in selection["entries"]]
    issues.extend(extra)
    if issues:
        results = [{**item, "status": "unverified"} for item in results]
    return {**proof, "fullClientGate": "unverified", outcome: "unverified" if issues else "passed",
            "selection": selection, "verificationIssues": issues, "clientSuites": results,
            "counts": {status: sum(item["status"] == status for item in results)
                       for status in ("passed", "skipped", "unverified")}}
