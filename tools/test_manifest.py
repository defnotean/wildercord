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
    selectors.add_argument("--shard", metavar="I/N", help="The log covers only this contiguous CI shard (run_client_ci.py --shard)")
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
    try:
        if required_part:
            from masters_required_ci import MAX_LOG_BYTES
            if args.log.is_symlink() or not args.log.is_file():
                raise ValueError("Required part log must be a regular non-symlink file")
            with args.log.open("rb") as source:
                raw = source.read(MAX_LOG_BYTES + 1)
            if len(raw) > MAX_LOG_BYTES:
                raise ValueError("Required part log exceeds bounded evidence size")
            log = raw.decode("utf-8-sig", errors="replace")
        else:
            log = args.log.read_text(encoding="utf-8-sig", errors="replace")
    except (FileNotFoundError, ValueError) as exc:
        log = ""
        issues.append("Native execution is unverified: " + str(exc))
    issues.extend(selection_issues(log, selection))
    if required_part:
        from masters_required_ci import part_evidence
        part, part_issues = part_evidence(log, selection)
        issues.extend(part_issues)
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
    units = {"suites": 0, "tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for report in (ROOT / "build/test-results/test").glob("TEST-*.xml"):
        root = ET.parse(report).getroot()
        units["suites"] += 1
        for key in ("tests", "failures", "errors", "skipped"):
            units[key] += int(root.get(key, "0"))
    focused = selection["kind"] == "suite"
    diagnostic = selection["kind"] == "diagnostic"
    manifest = {"fullClientGate": "passed" if successful and not (focused or diagnostic or required_part) else "unverified", "log": str(args.log.resolve()),
                **({"shard": selection["shard"]} if selection["kind"] == "shard" else {}),
                **({"focusedClientGate": "passed" if successful else "unverified"} if focused else {}),
                **({"focusedClientGate": "unverified", "diagnosticOutcome": "passed" if successful else "unverified"} if diagnostic else {}),
                **({**part, "focusedClientGate": "unverified", "requiredPartOutcome": "passed" if successful else "unverified"} if required_part else {}),
                "selection": selection,
                "verificationIssues": issues,
                "basis": ("One required Masters part only. All three matching passed parts are required for the 44-class focused gate; full client and manual visual acceptance remain unverified."
                          if required_part else "Diagnostic selection only. Neither full nor focused release acceptance is established by this run."
                          if diagnostic else "Named focused selection only, verified against the launcher's selection/exit and Gradle's processed descriptor evidence. "
                          "The full client gate and animation gallery are not established by this run. "
                          "Suite counts are not individual assertion counts." if focused else
                          "Full-descriptor Gradle outcome plus declared optional flags; suite counts are not individual assertion counts. "
                          "A shard manifest covers only its own contiguous part of the descriptor. "
                          "Uncatalogued focused, tail, tour-only or showcase-only runs are unsupported."),
                "counts": {status: sum(s["status"] == status for s in suites) for status in ("passed", "skipped", "unverified")},
                "unitTests": units, "clientSuites": suites}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(args.output), "scope": selection["kind"],
                      **({"suite": args.suite} if focused else {}),
                      "client": manifest["counts"], "unit": units}))
    # The focused job must not turn green if execution or scope evidence is missing.
    # Keep the existing full/shard report-only exit behavior unchanged.
    return 1 if (focused or diagnostic or required_part) and not successful else 0


if __name__ == "__main__":
    sys.exit(main())
