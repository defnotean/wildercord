"""VPS native selection, owned launch markers and strict completed-run manifests.

No process is launched here. The Windows runner owns the child and records its
actual exit; Gradle and Minecraft must supply descriptor/profile/lifecycle proof.
"""
import json
import re
import uuid

import client_suites as suites
import full_client_ci as full
import masters_required_ci as masters
from run_client_ci import MOON_ENTRY, MOON_NONCE
from vps_identity import make_identity

SUITES = (*suites.MASTERS_PART_NAMES, "articulated", "release-smoke")
PREFIX = "WILDERCORD_VPS_NATIVE "


def prepare(*, head, computer, batch, attempt, launch, environment, suite=None, shard=None):
    if (suite is None) == (shard is None) or (suite is not None and suite not in SUITES):
        raise ValueError("Select one complete supported VPS suite or full-client shard")
    selection = suites.select_entries(suite=suite, shard=shard)
    if str(uuid.UUID(launch)) != launch:
        raise ValueError("VPS launch must own a canonical UUID")
    identity = make_identity(head, computer, batch, attempt,
        "game-tests" if shard else "masters-native" if suite in suites.MASTERS_PART_NAMES else "articulated-native" if suite == "articulated" else "smoke-native")
    child = dict(environment)
    child[full.LAUNCH_ENV] = launch
    profile = None
    if shard:
        child["WILDERCORD_ANIMATION_GALLERY"] = "1"
        profile = full.execution_profile(child)
        proof = full.provenance(selection, identity, profile, launch)
        prefix = full.PROVENANCE_PREFIX
        number, count = shard.split("/")
        flags = ["-PciShard=" + number, "-PciShards=" + count]
    else:
        flags = ["-PciSuite=" + suite]
        if suite in suites.MASTERS_PART_NAMES:
            proof, prefix = masters.provenance(selection, identity, launch), masters.PART_PREFIX
        else:
            proof, prefix = {"schemaVersion": 1, **identity, "launchId": launch,
                             "selectionSha256": masters.selection_hash(selection)}, PREFIX
    preamble = [suites.SELECTION_PREFIX + json.dumps(selection, sort_keys=True),
                prefix + json.dumps(proof, sort_keys=True)]
    moon = None
    if MOON_ENTRY in selection["entries"]:
        nonce = str(uuid.uuid4())
        child[MOON_NONCE] = nonce
        # Explicit VPS receipt. Historical GitHub frame packages are not borrowed.
        moon = {"schemaVersion": 2, "purpose": "owner-fp-native-diagnostic",
                **identity, "nonce": nonce, "launchId": launch, "selection": selection,
                "remoteObserverCoverage": False, "serverReleaseFrameCorrespondenceVerified": False}
        preamble.append("WILDERCORD_MOON_OWNER_LAUNCH " + json.dumps(moon, sort_keys=True))
    return {"selection": selection, "identity": identity, "launch": launch, "profile": profile,
            "proof": proof, "prefix": prefix, "preamble": preamble, "flags": flags,
            "environment": child, "moonReceipt": moon}


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
    if selection["kind"] == "shard":
        proof, extra = full.run_evidence(log, selection, plan["profile"], identity=identity)
        if not suites.exact_json_marker(json.dumps(proof["provenance"]), plan["proof"]):
            extra.append("Full-client log belongs to a different owned VPS launch")
        proof.update(shard=selection["shard"])
        outcome = "shardOutcome"
        results = full.suite_results(selection, plan["profile"])
    else:
        issues.extend(group_boundaries(log, plan))
        if selection["kind"] == "required-part":
            proof, extra = masters.part_evidence(log, selection, identity=identity, launch_id=launch)
            proof["focusedClientGate"] = "unverified"
            outcome = "requiredPartOutcome"
        else:
            completed, extra = suites._whole_class_completion(log, selection["entries"], "VPS articulated", launch_id=launch)
            proof = {"schemaVersion": 1, "scope": "vps-articulated-suite", "provenance": plan["proof"],
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
