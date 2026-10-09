"""Authoritative full-client selection, shared by Python and Gradle preflight.

The checked-in plan is a reviewed scheduling contract, not a timing prediction.
No automatic balancing, unassigned-class append, or equal-count fallback exists.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys

PLAN = Path(__file__).resolve().with_name("client_shard_plan.json")
MAX_CONFIG_BYTES = 128 * 1024
PLAN_ID = "full-client-explicit-v2-counter-lifetimes"
PLAN_SHA256 = "d81692710775b8ce351c63ea357e6e56a643b138879962f7d97e6aa1b7fbe460"
ROSTER_SHA256 = "af359daf9aa00cf6ca2ceca38c6b7eb6239b37b10e521c4e43ae2ce43be38e9c"
COUNTS = (86, 86, 85, 84)
REQUIRED_PROFILE = {"execution": "complete", "animationGallery": True,
                    "shaders": False, "showcase": False, "firebloodShots": False}
REQUIRED_BLOCKS = {
    "relay": ["dev.wildercord.cast.RelayCircleTest", "dev.wildercord.cast.RelayLifetimeTest",
              "dev.wildercord.cast.RelayDefenceTest", "dev.wildercord.party.RelayImpactTest"],
    "wetland-garden-terrain": ["dev.wildercord.wildlife.WetlandGardenTest",
        "dev.wildercord.wildlife.WetlandGenerationScopeTest", "dev.wildercord.wildlife.WetlandPlacementContractTest",
        "dev.wildercord.wildlife.WetlandTerrainAbsenceTest", "dev.wildercord.wildlife.WetlandTerrainTest"],
    "march": ["dev.wildercord.aura.world.StoneMarchTest", "dev.wildercord.aura.world.StoneMarchPresentationTest",
              "dev.wildercord.aura.world.StoneMarchOpponentViewTest"],
    "settings-articulated": ["dev.wildercord.client.CombatPresentationSettingsTest",
        "dev.wildercord.client.combat.ArticulatedCombatPresentationTest",
        "dev.wildercord.client.combat.ArticulatedArmorPresentationTest",
        "dev.wildercord.world.upgrade.UpgradeRecoveryTest",
        "dev.wildercord.client.combat.ArticulatedFirstPersonCompositionTest",
        "dev.wildercord.client.combat.ArticulatedAuraShellPresentationTest",
        "dev.wildercord.client.combat.CrimsonMoonCaptureTest"],
    "wall-turn": ["dev.wildercord.aura.WallTurnLessonTest", "dev.wildercord.aura.WallTurnSafetyTest",
                  "dev.wildercord.aura.WallTurnCommitmentTest"],
    "earned-counter-lifetimes": ["dev.wildercord.aura.EarnedCounterAcceptanceTest",
        "dev.wildercord.aura.MirrorRiposteReleasedOwnerTest", "dev.wildercord.aura.UnmovedNullAcceptanceTest",
        "dev.wildercord.aura.arts.ArtWardsHardeningTest"],
}


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False)


def digest(value):
    return hashlib.sha256(canonical(value).encode("utf-8")).hexdigest()


def unique_fields(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate configuration field: " + key)
        result[key] = value
    return result


def parse_json(raw):
    def invalid_constant(value):
        raise ValueError("Non-finite configuration number: " + value)
    return json.loads(raw, object_pairs_hook=unique_fields, parse_constant=invalid_constant)


def read_json(path):
    """Portable bounded configuration read; reject links, devices and changed files.

    POSIX additionally prevents link traversal and blocking at open. Platforms
    without those flags still check the path/opened file identity before reading
    and recheck both identities afterward. Runtime evidence uses its own stricter
    Linux-only reader; this reader is for checked-in source and resource inputs.
    """
    before_path = os.lstat(path)
    if not stat.S_ISREG(before_path.st_mode) or before_path.st_size > MAX_CONFIG_BYTES:
        raise ValueError("Configuration must be a bounded regular non-symlink file")
    flags = os.O_RDONLY
    for name in ("O_NOFOLLOW", "O_NONBLOCK", "O_BINARY"):
        flags |= getattr(os, name, 0) or 0
    fd = os.open(path, flags)
    with os.fdopen(fd, "rb") as source:
        before = os.fstat(source.fileno())
        if not stat.S_ISREG(before.st_mode) or file_identity(before_path) != file_identity(before):
            raise ValueError("Configuration path changed before read")
        raw = source.read(MAX_CONFIG_BYTES + 1)
        after = os.fstat(source.fileno())
    after_path = os.lstat(path)
    if (len(raw) > MAX_CONFIG_BYTES or not stat.S_ISREG(after_path.st_mode)
            or file_identity(before) != file_identity(after)
            or file_identity(after) != file_identity(after_path)):
        raise ValueError("Configuration changed during read or exceeded its size bound")
    return parse_json(raw.decode("utf-8"))


def file_identity(info):
    # Windows reports creation time from lstat but change time from fstat, so st_ctime_ns never matches there.
    keys = ("st_dev", "st_ino", "st_size", "st_mtime_ns") + (() if os.name == "nt" else ("st_ctime_ns",))
    return tuple(getattr(info, key) for key in keys)


def entry_list(entries, label):
    if not isinstance(entries, list) or not entries or any(
            not isinstance(entry, str) or not re.fullmatch(r"[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+", entry, re.ASCII)
            for entry in entries):
        raise ValueError(label + " requires nonempty fully qualified class names")
    if len(set(entries)) != len(entries):
        raise ValueError(label + " contains duplicate entrypoints")
    return entries


def validate_plan(entries, plan):
    """Validate structure, exact roster/order/blocks, then the pinned plan identity."""
    entry_list(entries, "Full descriptor")
    fields = {"schemaVersion", "planId", "shardCount", "expectedCount", "orderedRosterSha256",
              "executionProfile", "requiredBlocks", "groups"}
    if not isinstance(plan, dict) or set(plan) != fields:
        raise ValueError("Missing or foreign shard plan fields")
    for name, expected in (("schemaVersion", 1), ("shardCount", 4), ("expectedCount", sum(COUNTS))):
        if type(plan[name]) is not int or plan[name] != expected:
            raise ValueError("Invalid shard plan " + name)
    if plan["planId"] != PLAN_ID or plan["orderedRosterSha256"] != ROSTER_SHA256:
        raise ValueError("Stale or foreign shard plan identity")
    if len(entries) != plan["expectedCount"] or digest(entries) != ROSTER_SHA256:
        raise ValueError("Full descriptor roster/order changed; explicitly review and repin the shard plan")
    if canonical(plan["executionProfile"]) != canonical(REQUIRED_PROFILE):
        raise ValueError("Shard plan requires the exact complete execution profile")
    blocks = [{"id": name, "entries": block} for name, block in REQUIRED_BLOCKS.items()]
    if canonical(plan["requiredBlocks"]) != canonical(blocks):
        raise ValueError("Shard plan must retain every exact required co-location/order block")
    groups = plan["groups"]
    if not isinstance(groups, list) or len(groups) != 4:
        raise ValueError("Exactly four shard groups are required")
    flattened = []
    for index, group in enumerate(groups, 1):
        if not isinstance(group, dict) or set(group) != {"id", "expectedCount", "entries"}:
            raise ValueError("Missing or foreign shard group fields")
        if type(group["id"]) is not int or group["id"] != index:
            raise ValueError("Shard group IDs must be exactly 1, 2, 3, 4 in order")
        selected = entry_list(group["entries"], "Shard group")
        if type(group["expectedCount"]) is not int or group["expectedCount"] != COUNTS[index - 1] or len(selected) != group["expectedCount"]:
            raise ValueError("Shard group has an incorrect expected count")
        if any(entry not in entries for entry in selected):
            raise ValueError("Shard group contains foreign entrypoints")
        membership = set(selected)
        if selected != [entry for entry in entries if entry in membership]:
            raise ValueError("Shard group must retain descriptor-relative order")
        flattened.extend(selected)
    if len(flattened) != len(entries) or len(set(flattened)) != len(entries) or set(flattened) != set(entries):
        raise ValueError("Shard groups must prove the exact disjoint full-roster union")
    for name, block in REQUIRED_BLOCKS.items():
        owners = [group["entries"] for group in groups if block[0] in group["entries"]]
        owner = owners[0]
        offset = owner.index(block[0])
        if owner[offset:offset + len(block)] != block:
            raise ValueError("Required contiguous co-location/order block was split: " + name)
    if digest(plan) != PLAN_SHA256:
        raise ValueError("Shard plan hash changed; review and repin the exact plan, never silently rebalance")
    return plan


def selection(entries, *, shard=None, plan_path=PLAN):
    plan = validate_plan(entries, read_json(plan_path))
    identity = {"schemaVersion": plan["schemaVersion"], "planId": plan["planId"],
                "planSha256": PLAN_SHA256, "orderedRosterSha256": ROSTER_SHA256,
                "executionProfile": plan["executionProfile"]}
    if shard is None:
        return {"kind": "full", "count": len(entries), "entries": entries, "plan": identity}
    if not isinstance(shard, str) or not re.fullmatch(r"[1-4]/4", shard):
        raise ValueError("Explicit full-client plan requires a canonical shard 1/4, 2/4, 3/4 or 4/4")
    group = plan["groups"][int(shard[0]) - 1]
    return {"kind": "shard", "shard": shard, "count": group["expectedCount"],
            "entries": group["entries"], "plan": identity}


def verify_processed(expected, path):
    """Reject stale resources and malformed raw JSON even on an UP-TO-DATE launch."""
    processed = read_json(path)
    try:
        entries = processed["entrypoints"]["fabric-client-gametest"]
        identity = processed["custom"]["wildercord:clientShardPlan"]
    except (KeyError, TypeError) as exc:
        raise ValueError("Processed descriptor is missing its full-client entries or plan identity") from exc
    if canonical(entries) != canonical(expected["entries"]):
        raise ValueError("Processed client descriptor does not match the exact full/shard source selection")
    if canonical(identity) != canonical(expected["plan"]):
        raise ValueError("Processed client descriptor has a missing or stale full-client plan identity")


def descriptor_entries(path):
    descriptor = read_json(path)
    try:
        return entry_list(descriptor["entrypoints"]["fabric-client-gametest"], "Full descriptor")
    except (KeyError, TypeError) as exc:
        raise ValueError("Source descriptor is missing its full-client entrypoints") from exc


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--shard")
    parser.add_argument("--plan", type=Path, default=PLAN)
    parser.add_argument("--processed-descriptor", type=Path)
    parser.add_argument("--source-descriptor", type=Path)
    args = parser.parse_args(argv)
    try:
        raw = sys.stdin.buffer.read(MAX_CONFIG_BYTES + 1)
        if len(raw) > MAX_CONFIG_BYTES:
            raise ValueError("Descriptor input exceeds its size bound")
        entries = parse_json(raw.decode("utf-8"))
        if args.source_descriptor is not None and canonical(entries) != canonical(descriptor_entries(args.source_descriptor)):
            raise ValueError("Gradle source entries differ from the strict raw source descriptor")
        result = selection(entries, shard=args.shard, plan_path=args.plan)
        if args.processed_descriptor is not None:
            verify_processed(result, args.processed_descriptor)
    except (ValueError, OSError, TypeError, RecursionError) as exc:
        parser.error(str(exc))
    print(canonical(result))
    return 0


if __name__ == "__main__":
    sys.exit(main())
