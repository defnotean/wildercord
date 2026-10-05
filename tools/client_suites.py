"""Shared native CI selection and evidence helpers; no Minecraft or third-party imports."""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "tools/client_suite_catalog.json"
DESCRIPTOR = ROOT / "src/gametest/resources/fabric.mod.json"
SELECTION_PREFIX = "WILDERCORD_CLIENT_SELECTION "
DESCRIPTOR_PREFIX = "WILDERCORD_CLIENT_DESCRIPTOR "
EXIT_PREFIX = "WILDERCORD_CLIENT_EXIT_CODE "
REQUEST_PREFIX = "WILDERCORD_NATIVE_REQUEST "


def parse_shard(value):
    if not re.fullmatch(r"[0-9]+/[0-9]+", value):
        raise ValueError("--shard must look like 2/4")
    shard, shards = map(int, value.split("/"))
    if not 1 <= shard <= shards:
        raise ValueError("--shard must have 1 <= I <= N")
    return shard, shards


def validate_entries(entries, label):
    if not isinstance(entries, list) or not entries or any(
            not isinstance(entry, str) or not entry.strip() for entry in entries):
        raise ValueError(f"{label} must contain a nonempty list of entrypoint names")
    if len(set(entries)) != len(entries):
        raise ValueError(f"{label} contains duplicate entrypoints")
    return entries


def select_entries(*, suite=None, shard=None, descriptor=DESCRIPTOR, catalog=CATALOG):
    if suite is not None and shard is not None:
        raise ValueError("--suite and --shard cannot be combined")
    entries = validate_entries(json.loads(descriptor.read_text(encoding="utf-8"))
                               ["entrypoints"]["fabric-client-gametest"], "Full descriptor")
    if suite is not None:
        groups = json.loads(catalog.read_text(encoding="utf-8"))
        if suite not in groups:
            raise ValueError(f"Unknown client suite {suite!r}; choose from {', '.join(groups)}")
        group = groups[suite]
        selected = validate_entries(group["entries"], f"Suite {suite}")
        expected = group["expectedCount"]
        if type(expected) is not int or expected < 1 or len(selected) != expected:
            raise ValueError(f"Suite {suite} must contain exactly {expected} entrypoints")
        missing = [entry for entry in selected if entry not in entries]
        if missing:
            raise ValueError(f"Suite {suite} has unregistered entrypoints: {', '.join(missing)}")
        purpose = group.get("purpose", "release")
        if purpose not in ("release", "diagnostic"):
            raise ValueError(f"Suite {suite} has an unknown purpose")
        return {"kind": "diagnostic" if purpose == "diagnostic" else "suite",
                "name": suite, "count": len(selected), "entries": selected}
    if shard is not None:
        index, total = parse_shard(shard)
        # Same contiguous split as build.gradle's ciShard selector.
        selected = entries[len(entries) * (index - 1) // total:len(entries) * index // total]
        return {"kind": "shard", "shard": f"{index}/{total}",
                "count": len(selected), "entries": selected}
    return {"kind": "full", "count": len(entries), "entries": entries}


def selection_issues(log, selection):
    """Focused evidence must come from this launcher's matching, completed invocation.

    Legacy full/shard logs remain supported, but a focused log can never be relabeled
    as a full/shard pass. Catalog changes also invalidate stale focused evidence.
    """
    issues = []
    for prefix, label in ((SELECTION_PREFIX, "Launcher selection"),
                          (DESCRIPTOR_PREFIX, "Processed descriptor")):
        markers = [line[len(prefix):] for line in log.splitlines() if line.startswith(prefix)]
        if markers:
            try:
                matches = len(markers) == 1 and json.loads(markers[0]) == selection
            except json.JSONDecodeError:
                matches = False
            if not matches:
                issues.append(f"{label} evidence does not match the requested selection")
        elif selection["kind"] in ("suite", "diagnostic"):
            issues.append(f"Focused run is missing {label.lower()} evidence")
    if selection["kind"] in ("suite", "diagnostic"):
        exits = [line[len(EXIT_PREFIX):] for line in log.splitlines() if line.startswith(EXIT_PREFIX)]
        if exits != ["0"]:
            issues.append("Focused run has no single successful launcher exit")
    return issues
