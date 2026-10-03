"""Verify a built review JAR against processed resources, compiled classes and unit reports.

Run after Gradle build; this checks the named artifact, not the newest file in build/libs.
It does not substitute for gameplay, visual or multiplayer evidence.
"""
import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def verify(jar):
    counts = {"resources": 0, "classes": 0}
    with zipfile.ZipFile(jar) as archive:
        bad = archive.testzip()
        if bad:
            raise ValueError(f"Corrupt ZIP entry: {bad}")
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate ZIP entries")
        expected_classes = set()
        for directory, kind in [(ROOT / "build/resources/main", "resources"),
                                (ROOT / "build/classes/java/main", "classes"),
                                (ROOT / "build/classes/java/client", "classes")]:
            if not directory.is_dir():
                raise ValueError(f"Missing build output: {directory}")
            for source in sorted(directory.rglob("*")):
                if not source.is_file():
                    continue
                entry = source.relative_to(directory).as_posix()
                if archive.read(entry) != source.read_bytes():
                    raise ValueError(f"Packaged bytes differ from tested output: {entry}")
                counts[kind] += 1
                if kind == "classes":
                    expected_classes.add(entry)
        packaged_classes = {n for n in names if n.endswith(".class") and n.startswith("dev/wildercord/")}
        if packaged_classes != expected_classes:
            raise ValueError(f"Unexpected/missing mod classes: {sorted(packaged_classes ^ expected_classes)}")
        counts["source_version"] = json.loads(archive.read("fabric.mod.json"))["version"]
    reports = sorted((ROOT / "build/test-results/test").glob("TEST-*.xml"))
    if not reports:
        raise ValueError("No unit test reports")
    tests = dict.fromkeys(["tests", "failures", "errors", "skipped"], 0)
    for report in reports:
        suite = ET.parse(report).getroot()
        for key in tests:
            tests[key] += int(suite.get(key, 0))
    if tests["failures"] or tests["errors"]:
        raise ValueError(f"Unit failures: {tests}")
    return {"jar": jar.name, "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
            **counts, "unit_tests": tests}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = verify(args.jar.resolve())
    text = json.dumps(result, indent=2) + "\n"
    if args.output:
        args.output.write_text(text, encoding="utf-8", newline="\n")
    print(text, end="")
