"""Record native gate outcomes with explicit scope, without counting opt-in skips as passes."""
import argparse
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

from client_suites import ROOT, select_entries, selection_issues


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, required=True, help="Log from run_client_ci.py with the same --suite/--shard selection")
    parser.add_argument("--output", type=Path, default=ROOT / "artifacts/review/test-manifest.json")
    selectors = parser.add_mutually_exclusive_group()
    selectors.add_argument("--shard", metavar="I/4", help="The log covers only this explicit CI shard (run_client_ci.py --shard)")
    selectors.add_argument("--suite", help="The log covers only this named focused group (run_client_ci.py --suite)")
    parser.add_argument("--gallery", action="store_true")
    parser.add_argument("--shaders", action="store_true")
    parser.add_argument("--showcase", action="store_true")
    parser.add_argument("--fireblood-shots", action="store_true")
    args = parser.parse_args(argv)
    try:
        selection = select_entries(suite=args.suite, shard=args.shard)
    except (ValueError, KeyError, OSError) as exc:
        parser.error(str(exc))
    issues = []
    required_part = selection["kind"] == "required-part"
    part = {}
    full_scope = selection["kind"] in ("full", "shard")
    full_evidence = {}
    profile = {"execution": "complete", "animationGallery": args.gallery, "shaders": args.shaders,
               "showcase": args.showcase, "firebloodShots": args.fireblood_shots}
    try:
        from full_client_ci import read_evidence, MAX_LOG_BYTES
        if required_part:
            from masters_required_ci import MAX_LOG_BYTES
        log = read_evidence(args.log, MAX_LOG_BYTES)
    except (OSError, ValueError) as exc:
        log = ""
        issues.append("Native execution is unverified: " + str(exc))
    issues.extend(selection_issues(log, selection))
    if required_part:
        from masters_required_ci import part_evidence
        part, part_issues = part_evidence(log, selection)
        issues.extend(part_issues)
    if full_scope:
        from full_client_ci import run_evidence
        full_evidence, full_issues = run_evidence(log, selection, profile)
        issues.extend(full_issues)
    successful = "BUILD SUCCESSFUL" in log and "BUILD FAILED" not in log and not issues
    optional = {
        "RunicAnimationGalleryTest": (args.gallery, "WILDERCORD_ANIMATION_GALLERY=1"),
        "WildercordShaderTest": (args.shaders, "Iris profile; checked separately with -Pshaders"),
        "WildercordShowcase": (args.showcase, "WILDERCORD_SHOWCASE=1; staged showcase"),
        "WildercordFireBloodShots": (args.fireblood_shots, "WILDERCORD_FIREBLOOD_SHOTS=1; additional close-ups"),
    }
    suites = []
    for entry in selection["entries"]:
        name = entry.rsplit(".", 1)[1]
        enabled, condition = optional.get(name, (True, None))
        suites.append({"suite": entry, "status": "passed" if successful and enabled else "skipped" if successful else "unverified",
                       **({"condition": condition} if condition else {})})
    if full_scope:
        from full_client_ci import suite_results
        suites = suite_results(selection, profile)
        if not successful:
            suites = [{**item, "status": "unverified"} for item in suites]
    units = {"suites": 0, "tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for report in (ROOT / "build/test-results/test").glob("TEST-*.xml"):
        root = ET.parse(report).getroot()
        units["suites"] += 1
        for key in ("tests", "failures", "errors", "skipped"):
            units[key] += int(root.get(key, "0"))
    focused = selection["kind"] == "suite"
    diagnostic = selection["kind"] == "diagnostic"
    manifest = {"fullClientGate": "unverified", "log": str(args.log.resolve()),
                **({"shard": selection["shard"]} if selection["kind"] == "shard" else {}),
                **({"focusedClientGate": "passed" if successful else "unverified"} if focused else {}),
                **({"focusedClientGate": "unverified", "diagnosticOutcome": "passed" if successful else "unverified"} if diagnostic else {}),
                **({**part, "focusedClientGate": "unverified", "requiredPartOutcome": "passed" if successful else "unverified"} if required_part else {}),
                **({**full_evidence, "shardOutcome" if selection["kind"] == "shard" else "fullRunOutcome": "passed" if successful else "unverified"} if full_scope else {}),
                "selection": selection,
                "verificationIssues": issues,
                "basis": ("One required Masters part only. All three matching passed parts are required for the 47-class focused gate; full client and manual visual acceptance remain unverified."
                          if required_part else "Diagnostic selection only. Neither full nor focused release acceptance is established by this run."
                          if diagnostic else "Named focused selection only, verified against the launcher's selection/exit and Gradle's processed descriptor evidence. "
                          "The full client gate and animation gallery are not established by this run. "
                          "Suite counts are not individual assertion counts." if focused else
                          "A run manifest covers only its own explicit group or full invocation. Only the separate exact all-four aggregate can establish fullClientGate. "
                          "Optional skips are not passed coverage; lifecycle completion and exact execution provenance are mandatory."),
                "counts": {status: sum(s["status"] == status for s in suites) for status in ("passed", "skipped", "unverified")},
                "unitTests": units, "clientSuites": suites}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(args.output), "scope": selection["kind"],
                      **({"suite": args.suite} if focused else {}),
                      "client": manifest["counts"], "unit": units}))
    # Missing/partial evidence fails every scope, including full/shard jobs.
    return 0 if successful else 1


if __name__ == "__main__":
    sys.exit(main())
