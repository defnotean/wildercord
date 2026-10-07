"""Shared native CI selection and evidence helpers; no Minecraft or third-party imports."""
import json
import math
from pathlib import Path
import re

from native_ci_diagnostics import SCENE_PREFIX
from client_shard_plan import selection as full_selection, read_json

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
GALE_BALLISTIC_ENTRY = "dev.wildercord.aura.world.GaleVaultBallisticTest"
GALE_BALLISTIC_SUITE = "diagnostic-gale-vault-ballistic"
GALE_BALLISTIC_PREFIX = "GALE_VAULT_BALLISTIC_COMPLETE "
GALE_BALLISTIC_RESULT = ("native_physics=true ordinary_attack=false selector_integration=false "
                         "payment=false damage=false accepted_attack_count_unchanged=true cleanup=complete")

STONE_VELOCITY_ENTRY = "dev.wildercord.aura.StoneHingeVelocityComparisonTest"
REED_REFUGE_ENTRY = "dev.wildercord.wildlife.ReedRefugeTest"
MOVEMENT_FOUNDATION_ENTRIES = (STONE_VELOCITY_ENTRY, REED_REFUGE_ENTRY)
STONE_VELOCITY_PREFIX = "STONE_HINGE_VELOCITY_COMPARISON "
STONE_VELOCITY_RESULT = ("owner_cases=15 owner_gate=observed peer_gate=NOT_PROVEN latency_gate=NOT_PROVEN "
                         "admission_gate=NOT_PROVEN movement_gate=NOT_PROVEN gameplay_enabled=false "
                         "conditional_aura=20 conditional_shared_rest_ticks=120 conditional_brace_ticks=6 conditional_catch_ticks=12")
STONE_MARCH_ENTRIES = (
    'dev.wildercord.aura.world.StoneMarchTest',
    'dev.wildercord.aura.world.StoneMarchPresentationTest',
    'dev.wildercord.aura.world.StoneMarchOpponentViewTest',
)
MASTERS_PART_NAMES = ("masters-core", "masters-march-mechanics", "masters-march-visuals")
STONE_MARCH_VISUAL_ENTRIES = (
    'dev.wildercord.aura.world.StoneMarchPresentationTest',
    'dev.wildercord.aura.world.StoneMarchOpponentViewTest',
)
STONE_MARCH_DIAGNOSTICS = {
    "stone-fault-march-visuals": STONE_MARCH_VISUAL_ENTRIES,
    "stone-fault-march-presentation": STONE_MARCH_VISUAL_ENTRIES[:1],
    "stone-fault-march-opponent": STONE_MARCH_VISUAL_ENTRIES[1:],
}


def masters_parts(groups):
    """Validate the exact disjoint 44/1/2 partition of the expanded 47-class roster."""
    full = validate_entries(groups["masters"]["entries"], "Masters roster")
    if (groups["masters"].get("purpose", "release") != "release"
            or type(groups["masters"]["expectedCount"]) is not int
            or groups["masters"]["expectedCount"] != 47 or len(full) != 47
            or [entry for entry in full if entry in STONE_MARCH_ENTRIES] != list(STONE_MARCH_ENTRIES)
            or full[-1] != "dev.wildercord.gametest.WildercordMastersArtsPresentationTest"):
        raise ValueError("Required Masters parts require the ordered 47-class release roster")
    expected = dict(zip(MASTERS_PART_NAMES, (
        [entry for entry in full if entry not in STONE_MARCH_ENTRIES],
        list(STONE_MARCH_ENTRIES[:1]), list(STONE_MARCH_VISUAL_ENTRIES))))
    for name, entries in expected.items():
        group = groups[name]
        if (group.get("purpose") != "required-part" or group["entries"] != entries
                or type(group["expectedCount"]) is not int or group["expectedCount"] != len(entries)):
            raise ValueError("Required Masters parts must retain the exact disjoint ordered 44/1/2 partition")
    return expected


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
    data = (json.loads(descriptor.read_text(encoding="utf-8")) if suite is not None else read_json(descriptor))
    entries = validate_entries(data["entrypoints"]["fabric-client-gametest"], "Full descriptor")
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
        if purpose not in ("release", "diagnostic", "required-part"):
            raise ValueError(f"Suite {suite} has an unknown purpose")
        if purpose == "required-part" and suite not in MASTERS_PART_NAMES:
            raise ValueError("Unknown required Masters part")
        if suite in MASTERS_PART_NAMES:
            masters_parts(groups)
        if suite == "diagnostic-life-excise" and (
                purpose != "diagnostic" or selected != list(LIFE_EXCISE_ENTRIES)):
            raise ValueError("Life32 requires all four unchanged generic suites and the separate Excise ordinary class in order")
        if suite == "diagnostic-progression-feasibility" and (
                purpose != "diagnostic" or selected != list(PROGRESSION_ENTRIES)):
            raise ValueError("Progression feasibility requires exactly both complete diagnostic classes in order")
        if suite == "diagnostic-stone-hinge-owner-negative" and (
                purpose != "diagnostic" or selected != list(STONE_OWNER_NEGATIVE_ENTRIES)):
            raise ValueError("Stone owner negative control requires its exact complete diagnostic class")
        if suite == GALE_BALLISTIC_SUITE and (
                purpose != "diagnostic" or selected != [GALE_BALLISTIC_ENTRY]):
            raise ValueError("Gale Vault ballistic requires its exact complete diagnostic class")
        if suite == "diagnostic-movement-foundations" and (
                purpose != "diagnostic" or selected != list(MOVEMENT_FOUNDATION_ENTRIES)):
            raise ValueError("Movement foundations requires exactly both whole diagnostic classes in order")
        if suite == "diagnostic-excise" and (purpose != "diagnostic" or selected != list(EXCISE_ENTRIES)):
            raise ValueError("Excise diagnostic requires its exact complete registered class")
        if suite == "stone-fault-march" and (purpose != "diagnostic" or selected != list(STONE_MARCH_ENTRIES)):
            raise ValueError("Stone Fault March requires exactly three complete diagnostic classes in order")
        if suite in STONE_MARCH_DIAGNOSTICS and (
                purpose != "diagnostic" or selected != list(STONE_MARCH_DIAGNOSTICS[suite])):
            raise ValueError("Stone Fault March diagnostic requires exactly its complete selected classes in order")
        return {"kind": purpose if purpose in ("diagnostic", "required-part") else "suite",
                "name": suite, "count": len(selected), "entries": selected}
    if shard is not None:
        parse_shard(shard)
    return full_selection(entries, shard=shard)


def selection_issues(log, selection):
    """Focused evidence must come from this launcher's matching, completed invocation.

    Full/shard evidence is also fail-closed. Catalog changes invalidate stale evidence;
    whole-client acceptance additionally requires the separate all-four aggregate.
    """
    issues = []
    for prefix, label in ((SELECTION_PREFIX, "Launcher selection"),
                          (DESCRIPTOR_PREFIX, "Processed descriptor")):
        markers = [line[len(prefix):] for line in log.splitlines() if line.startswith(prefix)]
        if markers:
            try:
                matches = len(markers) == 1 and (
                    exact_json_marker(markers[0], selection)
                    if selection.get("name") in (*STONE_MARCH_DIAGNOSTICS, GALE_BALLISTIC_SUITE) or selection["kind"] in ("required-part", "full", "shard")
                    else json.loads(markers[0]) == selection)
            except ValueError:
                matches = False
            if not matches:
                issues.append(f"{label} evidence does not match the requested selection")
        else:
            issues.append(f"Run is missing {label.lower()} evidence")
    if selection["kind"] in ("suite", "diagnostic", "required-part", "full", "shard"):
        exits = [line[len(EXIT_PREFIX):] for line in log.splitlines() if line.startswith(EXIT_PREFIX)]
        if exits != ["0"]:
            issues.append("Run has no single successful launcher exit")
    if selection.get("name") == "diagnostic-progression-feasibility":
        _, completion_issues = progression_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-reweave-player":
        _, completion_issues = reweave_player_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-stone-hinge-owner-negative":
        _, completion_issues = stone_owner_negative_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == GALE_BALLISTIC_SUITE:
        _, completion_issues = gale_ballistic_completion(log)
        issues.extend(completion_issues)
        _, seed_issues = whole_class_seeds(log, [GALE_BALLISTIC_ENTRY], "Gale Vault ballistic")
        issues.extend(seed_issues)
    if selection.get("name") == "diagnostic-movement-foundations":
        _, completion_issues = movement_foundations_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-excise":
        _, completion_issues = excise_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "diagnostic-life-excise":
        _, completion_issues = life_excise_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") == "stone-fault-march":
        _, completion_issues = stone_march_completion(log)
        issues.extend(completion_issues)
    if selection.get("name") in STONE_MARCH_DIAGNOSTICS:
        _, completion_issues = _whole_class_completion(log, selection["entries"], "Stone Fault March diagnostic")
        issues.extend(completion_issues)
        _, seed_issues = stone_march_seeds(log, selection["entries"])
        issues.extend(seed_issues)
    if selection["kind"] == "required-part":
        _, completion_issues = _whole_class_completion(log, selection["entries"], "Required Masters part")
        issues.extend(completion_issues)
        if selection["name"] != "masters-core":
            _, seed_issues = stone_march_seeds(log, selection["entries"])
            issues.extend(seed_issues)
    return issues


def _unique_completion_object(pairs):
    marker = {}
    for key, value in pairs:
        if key in marker:
            raise ValueError("Duplicate completion field")
        marker[key] = value
    return marker


def exact_json_marker(raw, expected):
    """Reject duplicate fields, non-finite numbers and bool/number type coercion."""
    observed = json.loads(raw, object_pairs_hook=_unique_completion_object)
    return (json.dumps(observed, sort_keys=True, allow_nan=False)
            == json.dumps(expected, sort_keys=True, allow_nan=False))


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


def gale_ballistic_completion(log):
    """Native feasibility needs the final marker after the class's own cleanup.

    Fabric's separate cleanup/return must still follow; an earlier physics summary
    or a successful process exit cannot stand in for either completion boundary.
    """
    completed, issues = _whole_class_completion(log, [GALE_BALLISTIC_ENTRY], "Gale Vault ballistic")
    markers = [line.split(GALE_BALLISTIC_PREFIX, 1)[1].strip()
               for line in log.splitlines() if GALE_BALLISTIC_PREFIX in line]
    if markers != [GALE_BALLISTIC_RESULT]:
        issues.append("Gale Vault ballistic requires exactly its final physics-only cleanup result")
    boundaries = []
    for line in log.splitlines():
        if line.startswith(SELECTION_PREFIX):
            boundaries.append("selection")
        if line.startswith(DESCRIPTOR_PREFIX):
            boundaries.append("descriptor")
        try:
            if scene_marker(line) is not None:
                boundaries.append("scene")
        except (ValueError, KeyError, TypeError, OverflowError):
            boundaries.append("invalid-scene")
        if "WILDERCORD_NATIVE_WORLD " in line:
            boundaries.append("seed")
        if GALE_BALLISTIC_PREFIX in line:
            boundaries.append("terminal")
        if line.startswith("BUILD SUCCESSFUL"):
            boundaries.append("build")
        if line.startswith(EXIT_PREFIX):
            boundaries.append("exit")
    if boundaries != ["selection", "descriptor", "scene", "scene", "seed", "terminal", "scene", "scene", "build", "exit"]:
        issues.append("Gale Vault ballistic requires ordered selection, native run/seed/terminal, runner cleanup/return and successful process completion")
    return completed, issues


def excise_completion(log):
    """Require ordinary study/input/lifecycle cleanup and full return; no partial pass."""
    return _whole_class_completion(log, EXCISE_ENTRIES, "Excise player")


def life_excise_completion(log):
    """Generic Life coverage and the custom held route must all cleanly return."""
    return _whole_class_completion(log, LIFE_EXCISE_ENTRIES, "Life and Excise")


def stone_march_completion(log):
    return _whole_class_completion(log, STONE_MARCH_ENTRIES, "Stone Fault March")


def stone_march_visual_completion(log):
    """Two whole visual classes only; never certify mechanics or release acceptance."""
    return _whole_class_completion(log, STONE_MARCH_VISUAL_ENTRIES, "Stone Fault March visuals")


def scene_marker(line, *, launch_id=None):
    """Parse only native Test-thread stdout or the explicit plain fixture form.

    The reserved token without its required space/envelope is malformed evidence,
    never unrelated output. A copied/quoted historical line cannot become native
    completion just because it contains a correctly shaped JSON suffix.
    """
    token = SCENE_PREFIX.rstrip()
    if token not in line:
        return None
    envelope = r"(?:\[(?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]\] \[Test thread/INFO\] \(Minecraft\) \[STDOUT\]: )?"
    match = re.fullmatch(envelope + re.escape(SCENE_PREFIX) + r"(\{.*\})", line)
    if match is None:
        raise ValueError("Malformed native lifecycle envelope")
    marker = json.loads(match.group(1), object_pairs_hook=_unique_completion_object)
    fields = {"suite", "event", "phase", "elapsedSeconds", "sceneElapsedSeconds"}
    if launch_id is not None:
        fields.add("launchId")
    if not isinstance(marker, dict) or set(marker) != fields:
        raise ValueError("Malformed native lifecycle fields")
    if launch_id is not None and marker["launchId"] != launch_id:
        raise ValueError("Native lifecycle belongs to another owned launch")
    if any(not isinstance(marker[key], str) or not marker[key] for key in ("suite", "event", "phase")):
        raise ValueError("Malformed native lifecycle identity")
    if any(type(marker[key]) not in (float, int) or not math.isfinite(marker[key]) or marker[key] < 0
           for key in ("elapsedSeconds", "sceneElapsedSeconds")):
        raise ValueError("Malformed native lifecycle timing")
    return marker


def _whole_class_completion(log, entries, label, *, launch_id=None):
    events, issues = [], []
    for line in log.splitlines():
        try:
            marker = scene_marker(line, launch_id=launch_id)
            if marker is not None:
                events.append((marker["suite"], marker["event"], marker["phase"]))
        except (ValueError, KeyError, TypeError, OverflowError):
            issues.append(f"Malformed {label.lower()} completion evidence")
    expected = [(entry, event, phase) for entry in entries
                for event, phase in (("start", "setup"), ("phase", "run"),
                                     ("phase", "cleanup"), ("end", "returned"))]
    if events != expected:
        issues.append(f"{label} requires exactly its completed whole classes in order")
    return [entry for entry in entries if (entry, "end", "returned") in events], issues


def stone_march_seeds(log, entries):
    """Each selected whole March class owns exactly one original signed-long seed."""
    return whole_class_seeds(log, entries, "March scope")


def whole_class_seeds(log, entries, label):
    """Observe exact original world identities, never override the fixture RNG."""
    found, issues = {}, []
    prefix = "WILDERCORD_NATIVE_WORLD "
    for line in log.splitlines():
        if prefix not in line:
            continue
        try:
            marker = json.loads(line.split(prefix, 1)[1], object_pairs_hook=_unique_completion_object)
            if not isinstance(marker, dict) or set(marker) != {"suite", "seed"}:
                raise ValueError()
            entry, seed = marker["suite"], marker["seed"]
            if (not isinstance(entry, str) or entry not in entries or entry in found
                    or not isinstance(seed, str) or not re.fullmatch(r"-?[0-9]{1,19}", seed)
                    or not -(2 ** 63) <= int(seed) < 2 ** 63):
                raise ValueError()
            found[entry] = [seed]
        except (ValueError, KeyError, TypeError):
            issues.append(f"Invalid or duplicate {label.lower()} world seed")
    if list(found) != list(entries):
        issues.append(f"{label} requires exactly its original world seeds in class order")
    return found, issues
