"""Record full-client gate outcomes without counting opt-in skips as passes."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, required=True, help="Log from the full descriptor (or one --shard of it), without focused/tail selectors")
    parser.add_argument("--output", type=Path, default=ROOT / "artifacts/review/test-manifest.json")
    parser.add_argument("--shard", metavar="I/N", help="The log covers only this contiguous CI shard (run_client_ci.py --shard)")
    parser.add_argument("--gallery", action="store_true")
    parser.add_argument("--shaders", action="store_true")
    parser.add_argument("--showcase", action="store_true")
    parser.add_argument("--fireblood-shots", action="store_true")
    args = parser.parse_args()
    log = args.log.read_text(encoding="utf-8-sig", errors="replace")
    successful = "BUILD SUCCESSFUL" in log and "BUILD FAILED" not in log
    descriptor = json.loads((ROOT / "src/gametest/resources/fabric.mod.json").read_text())
    optional = {
        "RunicAnimationGalleryTest": (args.gallery, "WILDERCORD_ANIMATION_GALLERY=1"),
        "WildercordShaderTest": (args.shaders, "Iris profile; checked separately with -Pshaders"),
        "WildercordShowcase": (args.showcase, "WILDERCORD_SHOWCASE=1; staged showcase"),
        "WildercordFireBloodShots": (args.fireblood_shots, "WILDERCORD_FIREBLOOD_SHOTS=1; additional close-ups"),
    }
    entries = descriptor["entrypoints"]["fabric-client-gametest"]
    shard_label = None
    if args.shard:
        shard, _, shards = args.shard.partition("/")
        shard, shards = int(shard), int(shards)
        # Same contiguous split as build.gradle's ciShard selector.
        entries = entries[len(entries) * (shard - 1) // shards:len(entries) * shard // shards]
        shard_label = f"{shard}/{shards}"
    suites = []
    for entry in entries:
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
    manifest = {"fullClientGate": "passed" if successful else "unverified", "log": str(args.log.resolve()),
                **({"shard": shard_label} if shard_label else {}),
                "basis": "Full-descriptor Gradle outcome plus declared optional flags; suite counts are not individual assertion counts. A shard manifest covers only its own contiguous part of the descriptor. Do not use this tool with focused, tail, tour-only or showcase-only runs.",
                "counts": {status: sum(s["status"] == status for s in suites) for status in ("passed", "skipped", "unverified")},
                "unitTests": units, "clientSuites": suites}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps({"output": str(args.output), "client": manifest["counts"], "unit": units}))
