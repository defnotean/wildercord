"""Shared native CI selection and evidence helpers; no Minecraft or third-party imports."""
import json
import math
from pathlib import Path
import re

from native_ci_diagnostics import SCENE_PREFIX

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "tools/client_suite_catalog.json"
DESCRIPTOR = ROOT / "src/gametest/resources/fabric.mod.json"
SELECTION_PREFIX = "WILDERCORD_CLIENT_SELECTION "
DESCRIPTOR_PREFIX = "WILDERCORD_CLIENT_DESCRIPTOR "
EXIT_PREFIX = "WILDERCORD_CLIENT_EXIT_CODE "
REQUEST_PREFIX = "WILDERCORD_NATIVE_REQUEST "
PROGRESSION_ENTRIES = ("dev.wildercord.cast.ReweaveFeasibilityTest",
                       "dev.wildercord.aura.StoneHingeFeasibilityTest")
REWEAVE_PLAYER_ENTRIES = ("dev.wildercord.cast.ReweavePlayableTest",)
EXCISE_ENTRIES = ("dev.wildercord.cast.ExcisePlayableTest",)
STONE_OWNER_NEGATIVE_ENTRIES = ("dev.wildercord.aura.StoneHingeStationaryOwnerNegativeTest",)
STONE_OWNER_NEGATIVE_PREFIX = "STONE_HINGE_OWNER_NEGATIVE "
STONE_OWNER_NEGATIVE_RESULT = ("expected_incompatibility=observed movement_gate=NOT_PROVEN "
                               "gameplay_enabled=false conditional_shared_rest_ticks=120")

STONE_VELOCITY_ENTRY = "dev.wildercord.aura.StoneHingeVelocityComparisonTest"
REED_REFUGE_ENTRY = "dev.wildercord.wildlife.ReedRefugeTest"
MOVEMENT_FOUNDATION_ENTRIES = (STONE_VELOCITY_ENTRY, REED_REFUGE_ENTRY)
STONE_VELOCITY_PREFIX = "STONE_HINGE_VELOCITY_COMPARISON "
STONE_VELOCITY_RESULT = ("owner_cases=15 owner_gate=observed peer_gate=NOT_PROVEN latency_gate=NOT_PROVEN "
                         "admission_gate=NOT_PROVEN movement_gate=NOT_PROVEN gameplay_enabled=false "
                         "conditional_aura=20 conditional_shared_rest_ticks=120 conditional_brace_ticks=6 conditional_catch_ticks=12")
LIFE_EXCISE_ENTRIES = (
    "dev.wildercord.client.fx.LifeRecipeTest",
    "dev.wildercord.client.fx.LifeFormationTest",
    "dev.wildercord.client.fx.LifeFlightTest",
    "dev.wildercord.cast.LifeOutcomeRecipeTest",
    "dev.wildercord.cast.ExcisePlayableTest",
)



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
    if any(entry in entries for entry in LIFE_EXCISE_ENTRIES[:-1]) and LIFE_EXCISE_ENTRIES[-1] not in entries:
        raise ValueError("Life32 requires its separate Excise ordinary class in the full descriptor; generic31 is not complete coverage")
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
        if suite == "diagnostic-life-excise" and (
                purpose != "diagnostic" or selected != list(LIFE_EXCISE_ENTRIES)):
            raise ValueError("Life32 requires all four unchanged generic suites and the separate Excise ordinary class in order")
        if suite == "diagnostic-progression-feasibility" and (
                purpose != "diagnostic" or selected != list(PROGRESSION_ENTRIES)):
            raise ValueError("Progression feasibility requires exactly both complete diagnostic classes in order")
        if suite == "diagnostic-stone-hinge-owner-negative" and (
                purpose != "diagnostic" or selected != list(STONE_OWNER_NEGATIVE_ENTRIES)):
            raise ValueError("Stone owner negative control requires its exact complete diagnostic class")
        if suite == "diagnostic-movement-foundations" and (
                purpose != "diagnostic" or selected != list(MOVEMENT_FOUNDATION_ENTRIES)):
            raise ValueError("Movement foundations requires exactly both whole diagnostic classes in order")
        if suite == "diagnostic-excise" and (purpose != "diagnostic" or selected != list(EXCISE_ENTRIES)):
            raise ValueError("Excise diagnostic requires its exact complete registered class")
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
    if selection.get("name") == "diagnostic-progression-feasibility":
        _, completion_issues = progression_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-reweave-player":
        _, completion_issues = reweave_player_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-stone-hinge-owner-negative":
        _, completion_issues = stone_owner_negative_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-movement-foundations":
        _, completion_issues = movement_foundations_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-excise":
        _, completion_issues = excise_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-life-excise":
        _, completion_issues = life_excise_completion(log)
        issues.extend(completion_issues)
    return issues


def _unique_completion_object(pairs):
    marker = {}
    for key, value in pairs:
        if key in marker:
            raise ValueError("Duplicate completion field")
        marker[key] = value
    return marker


def progression_completion(log):
    """Require the pinned runner's setup/run/cleanup/return for both whole classes."""
    return _whole_class_completion(log, PROGRESSION_ENTRIES, "Progression feasibility")


def reweave_player_completion(log):
    """Require the whole ordinary learn/equip/input/lifecycle class to clean up and return."""
    return _whole_class_completion(log, REWEAVE_PLAYER_ENTRIES, "Reweave player")


def stone_owner_negative_completion(log):
    """A completed negative control cannot become evidence of available movement."""
    completed, issues = _whole_class_completion(log, STONE_OWNER_NEGATIVE_ENTRIES, "Stone owner negative control")
    markers = [line.split(STONE_OWNER_NEGATIVE_PREFIX, 1)[1].strip()
               for line in log.splitlines() if STONE_OWNER_NEGATIVE_PREFIX in line]
    if markers != [STONE_OWNER_NEGATIVE_RESULT]:
        issues.append("Stone owner negative control requires exactly its unchanged NOT_PROVEN result")
    return completed, issues


def movement_foundations_completion(log):
    """Require the complete refuge lifecycle and bounded owner-only velocity proof."""
    completed, issues = _whole_class_completion(log, MOVEMENT_FOUNDATION_ENTRIES, "Movement foundations")
    markers = [line.split(STONE_VELOCITY_PREFIX, 1)[1].strip()
               for line in log.splitlines() if STONE_VELOCITY_PREFIX in line]
    if markers != [STONE_VELOCITY_RESULT]:
        issues.append("Movement foundations requires the exact fifteen-case owner result with remaining gates NOT_PROVEN")
    return completed, issues


def excise_completion(log):
    """Require ordinary study/input/lifecycle cleanup and full return; no partial pass."""
    return _whole_class_completion(log, EXCISE_ENTRIES, "Excise player")


def life_excise_completion(log):
    """Generic Life coverage and the custom held route must all cleanly return."""
    return _whole_class_completion(log, LIFE_EXCISE_ENTRIES, "Life and Excise")


def _whole_class_completion(log, entries, label):
    events, issues = [], []
    for line in log.splitlines():
        if SCENE_PREFIX not in line:
            continue
        try:
            marker = json.loads(line.split(SCENE_PREFIX, 1)[1], object_pairs_hook=_unique_completion_object)
            if not isinstance(marker, dict) or set(marker) != {
                    "suite", "event", "phase", "elapsedSeconds", "sceneElapsedSeconds"}:
                raise ValueError()
            if any(type(marker[key]) not in (float, int) or not math.isfinite(marker[key]) or marker[key] < 0
                   for key in ("elapsedSeconds", "sceneElapsedSeconds")):
                raise ValueError()
            events.append((marker["suite"], marker["event"], marker["phase"]))
        except (ValueError, KeyError, TypeError, OverflowError):
            issues.append(f"Malformed {label.lower()} completion evidence")
    expected = [(entry, event, phase) for entry in entries
                for event, phase in (("start", "setup"), ("phase", "run"),
                                     ("phase", "cleanup"), ("end", "returned"))]
    if events != expected:
        issues.append(f"{label} requires exactly its completed whole classes in order")
    return [entry for entry in entries if (entry, "end", "returned") in events], issues
