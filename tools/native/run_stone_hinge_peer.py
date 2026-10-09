"""Three fixed, sequential Stone Hinge transport diagnostics in native-diagnostic.

Only the exported, fingerprinted Java/Fabric command may run. Each fresh pair
uses two retained literal IPv4 loopback relays. A passing result requires the
original motion/owner/tracker/peer chain, interpolation, four drained byte
streams, two zero exits, frozen inputs, and clean owned-resource teardown.
Disposable game state lives in work/; upload only the bounded evidence folders.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
from dataclasses import asdict
import json
import hashlib
import struct
import math
import os
from pathlib import Path
import re
import signal
import stat
import subprocess
import sys
import time
import uuid

try:
    from . import launch_two_clients as s, run_paired_matrix as matrix, stone_hinge_relay as relay
except ImportError:
    import launch_two_clients as s
    import run_paired_matrix as matrix
    import stone_hinge_relay as relay

ROOT = s.ROOT
SUITE = "stone-hinge-peer"
CONTRACT = "src/gametest/resources/stone-hinge-peer-native-contract.json"
ENTRYPOINT = "dev.wildercord.aura.StoneHingePeerTransportTest"
MIXINS = ("stone-hinge-proof-gametest.mixins.json", "stone-hinge-peer-gametest.mixins.json")
CASES = ("ORDINARY", "RIGHT", "LEFT", "PARTIAL_RESISTANCE", "ABSORPTION", "WALL_CLIPPED",
         "REAR", "FULL_RESISTANCE", "ZERO_WOUND", "INVULNERABLE", "CALLER_KNOCKBACK",
         "CLOSE_AURA", "INTERVENING_IMPULSE", "FALLING_ORDINARY", "FALLING_REFUSAL",
         "NONZERO_RETENTION", "NATURAL_MASTER")
DELAYED_CASES = ("ORDINARY", "RIGHT", "LEFT", "WALL_CLIPPED", "FALLING_ORDINARY", "FALLING_REFUSAL")
PROFILES = ("transparent", "symmetric", "asymmetric")
DELAYS = ((0, 0, 0, 0), (50, 50, 50, 50), (100, 25, 25, 100))
TOTAL_SECONDS, EXPORT_SECONDS, PROFILE_SECONDS, VALIDATION_SECONDS, CLEANUP_SECONDS = 1200, 180, 300, 60, 60
MAX_JSON = 262144
MAX_EVIDENCE = 16 * 1024 * 1024
IDENTITY = (*s.IDENTITY, "profile")
FLAGS = {"gameplayEnabled": False, "ordinaryInputProven": False, "admissionProven": False,
         "paidInputProven": False, "wholeJvmEgressIsolationProven": False}
TERMINALS = {"host-stone-hinge-passed.properties": "host", "peer-stone-hinge-passed.properties": "peer",
             "peer-disconnected.properties": "peer", "disconnect-peer.properties": "host"}
QUANTIZATION_SQUARED = 3.0 / 4096**2
PACKET_KINDS = {"Pos", "PosRot", "ClientboundEntityPositionSyncPacket"}


def roster(profile):
    s.require(profile in PROFILES, "Only the three fixed Stone Hinge profiles are allowed")
    return list(CASES if profile == "transparent" else DELAYED_CASES)


def selection(profile):
    return {"suite": SUITE, "profile": profile, "contract": CONTRACT, "entrypoint": ENTRYPOINT,
            "cases": roster(profile), "profiles": s.PROFILES, "mixins": MIXINS, "maxTimeoutSeconds": PROFILE_SECONDS}


def load_contract(root):
    value = read_json(root / CONTRACT)
    expected = {"schemaVersion": 1, "suite": SUITE, "entrypoint": ENTRYPOINT, "expectedCount": 29,
                "profiles": [{"id": name, "delayMs": list(delay), "cases": roster(name)}
                             for name, delay in zip(PROFILES, DELAYS)],
                "limits": {"maxJvms": 2, "maxHeapMiBPerJvm": 2048, "maxCases": 29,
                           "maxTimeoutSeconds": TOTAL_SECONDS, "maxProfileSeconds": PROFILE_SECONDS,
                           "maxProfileEvidenceBytes": MAX_EVIDENCE}, **FLAGS}
    s.require(value == expected, "Stone Hinge requires its complete fixed 17/6/6 source contract")
    s.require(tuple(relay.PROFILES) == PROFILES and tuple(relay.PROFILES.values()) == DELAYS,
              "Relay delays differ from the source contract")
    return value


def read_json(path, maximum=MAX_JSON):
    def unique(pairs):
        value = {}
        for key, item in pairs:
            s.require(key not in value, "Duplicate JSON key: " + key)
            value[key] = item
        return value
    s.require(path.is_file() and not path.is_symlink() and path.stat().st_size <= maximum,
              "Missing, linked or oversized JSON evidence: " + str(path))
    def bad_constant(value):
        raise ValueError("Nonfinite JSON constant: " + value)
    with path.open("rb") as stream:
        data = stream.read(maximum + 1)
    s.require(len(data) <= maximum, "JSON evidence grew beyond its bound")
    # JSON permits the integer spelling -0; retain its IEEE sign before structural comparisons.
    return json.loads(data, object_pairs_hook=unique, parse_constant=bad_constant,
                      parse_int=lambda token: -0.0 if token == "-0" else int(token))


def validate_launch(launch, path, root):
    load_contract(root)
    s.require(launch.get("javaVersion") == 25 and type(launch["javaVersion"]) is int,
              "Export must attest the fingerprinted Java 25 toolchain")
    s.require(launch.get("maxTimeoutSeconds") == PROFILE_SECONDS, "Export must bind the 300-second profile ceiling")
    return s.validate_launch(launch, path, root, selection(PROFILES[0]))


def provenance(root, environ=None):
    env = os.environ if environ is None else environ
    head = s.source_head(root)
    pr = env.get("WILDERCORD_PR_HEAD_SHA", "")
    s.require(re.fullmatch(r"[a-f0-9]{40}", head) and (not pr or re.fullmatch(r"[a-f0-9]{40}", pr)), "Invalid exact source/PR head")
    result = {"sourceHead": head, "checkoutSha": head, "prHeadSha": pr,
              "runId": env.get("GITHUB_RUN_ID"), "runAttempt": env.get("GITHUB_RUN_ATTEMPT"),
              "job": env.get("GITHUB_JOB"), "repository": env.get("GITHUB_REPOSITORY"), "eventName": env.get("GITHUB_EVENT_NAME"),
              "workflowSha": env.get("GITHUB_SHA"), "pullRequest": env.get("NATIVE_DIAGNOSTIC_PR")}
    if env.get("GITHUB_ACTIONS") == "true":
        # This existing job checks out the PR head; GITHUB_SHA remains GitHub's
        # synthetic merge commit and is a separate workflow association.
        s.require(env.get("NATIVE_DIAGNOSTIC_HEAD_SHA") == head == pr, "Diagnostic checkout/export must match the explicitly requested PR head")
        s.require(re.fullmatch(r"[a-f0-9]{40}", result["workflowSha"] or ""), "Missing workflow SHA association")
        s.require(all(re.fullmatch(r"[1-9][0-9]*", result[key] or "") for key in ("runId", "runAttempt", "pullRequest")), "Missing CI run/attempt/PR")
        s.require(result["job"] == "native-diagnostic", "Stone Hinge may run only in the existing native-diagnostic CI job")
        s.require(re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", result["repository"] or ""), "Missing CI repository")
        s.require(result["eventName"] == "pull_request", "Existing native diagnostic requires a pull_request event")
        event = read_json(Path(env.get("GITHUB_EVENT_PATH", "")), 8 * 1024 * 1024)
        s.require(event.get("pull_request", {}).get("head", {}).get("sha") == pr
                  and event.get("number") == int(result["pullRequest"]), "PR head/number differs from workflow event")
        result["runUrl"] = f"https://github.com/{result['repository']}/actions/runs/{result['runId']}/attempts/{result['runAttempt']}"
    return result


def integer(value, label, minimum=0, maximum=2**63 - 1):
    s.require(type(value) is int and minimum <= value <= maximum, "Invalid " + label)
    return value


def number(value, label):
    s.require(type(value) in (int, float) and math.isfinite(value), "Nonfinite or nonnumeric " + label)
    return value


def vector(value, label):
    s.require(isinstance(value, dict) and set(value) == {"x", "y", "z"}, "Invalid vector " + label)
    return tuple(number(value[key], label + "." + key) for key in ("x", "y", "z"))


def exact(left, right):
    """JSON numeric equality matching finite Java record values, including IEEE signed zero."""
    if isinstance(left, dict) and isinstance(right, dict):
        return left.keys() == right.keys() and all(exact(left[key], right[key]) for key in left)
    if isinstance(left, (list, tuple)) and type(left) is type(right):
        return len(left) == len(right) and all(exact(a, b) for a, b in zip(left, right))
    if type(left) in (int, float) and type(right) in (int, float):
        return left == right and (left != 0 or math.copysign(1, left) == math.copysign(1, right))
    return type(left) is type(right) and left == right


def distance(left, right):
    return sum((a - b)**2 for a, b in zip(vector(left, "left"), vector(right, "right")))


def sha(value, label):
    s.require(isinstance(value, str) and re.fullmatch(r"[a-f0-9]{64}", value), "Invalid SHA-256 " + label)
    return value


def clock_proof(value):
    s.require(isinstance(value, dict), "Missing native clock evidence")
    observations = integer(value.get("observations"), "clock observations", 4)
    transitions = integer(value.get("tickTransitions"), "native tick transitions", 3, observations - 1)
    integer(value.get("nativeTimeResyncs"), "native time resync count", 0, transitions)
    integer(value.get("firstTick"), "first native tick")
    integer(value.get("lastTick"), "last native tick")
    integer(value.get("maximumGapNanos"), "native clock maximum gap", 1, 2_000_000_000)


def pose(value):
    s.require(isinstance(value, dict) and set(value) == {"yaw", "pitch", "headYaw", "bodyYaw", "stance"},
              "Missing or invalid immutable native pose snapshot")
    for key in ("yaw", "pitch", "headYaw", "bodyYaw"):
        number(value[key], "native pose " + key)
    s.require(isinstance(value["stance"], str) and re.fullmatch(r"[A-Z][A-Z_]*", value["stance"]), "Invalid native Pose value")


def body(value, entity):
    s.require(isinstance(value, dict) and value.get("entity") == entity, "Owner event belongs to a different body")
    vector(value.get("position"), "owner position"); vector(value.get("motion"), "owner motion")
    number(value.get("fall"), "owner fall"); number(value.get("health"), "owner health")
    integer(value.get("tick"), "owner tick")
    pose(value.get("pose"))
    s.require(all(type(value.get(k)) is bool for k in ("grounded", "neutral", "horizontalCollision")), "Invalid owner body flags")


def validate_natural_dispatch(identity, chain, events, motion):
    proof = chain.get("naturalDispatch")
    if identity["name"] != "NATURAL_MASTER":
        s.require(proof is None, "Only natural Master may use native-step qualification")
        return
    s.require(isinstance(proof, dict) and isinstance(motion, dict), "Natural Master lacks its original native-step dispatch proof")
    s.require(proof.get("completed") is True and all(type(proof.get(k)) is int and proof[k] == 1
              for k in ("stepCount", "trackerCount", "motionOrdinal")), "Natural dispatch is not one step, next tracker and first motion")
    receipt = proof.get("ownerReceipt")
    s.require(receipt is None or isinstance(receipt, dict), "Invalid queued owner receipt")
    frames = dict(proof)
    if receipt is not None:
        frames.update(handlerEntry=receipt.get("handlerEntry"), handlerReturn=receipt.get("handlerReturn"))
    stages = (("release", "natural-release"), ("beforeStep", "natural-step-start"),
              ("afterStep", "natural-step-end"), ("listenerExit", "natural-listener-end"))
    if receipt is not None:
        stages += (("handlerEntry", "natural-owner-packet-start"), ("handlerReturn", "natural-owner-packet-end"))
    stages += (("trackerEntry", "natural-tracker-start"), ("send", "natural-motion-start"),
              ("sendExit", "natural-motion-end"), ("trackerExit", "natural-tracker-end"))
    previous = -1
    source = None
    for name, kind in stages:
        frame = frames.get(name)
        s.require(isinstance(frame, dict) and frame.get("valid") is True and frame.get("awaitingTeleport") is False,
                  "Natural dispatch has replaced or corrected native identity")
        s.require(frame.get("ownerEntity") == identity["ownerEntity"] and frame.get("ownerUuid") == identity["ownerUuid"],
                  "Natural dispatch changed owner generation")
        current_source = (integer(frame.get("sourceEntity"), "natural source entity", 1), frame.get("sourceUuid"))
        s.require(isinstance(current_source[1], str) and str(uuid.UUID(current_source[1])) == current_source[1]
                  and current_source[0] not in (identity["ownerEntity"], identity["peerEntity"])
                  and current_source[1] not in (identity["ownerUuid"], identity["peerUuid"]), "Invalid natural Master source")
        s.require(source is None or source == current_source, "Natural Master source changed during dispatch")
        source = current_source
        tick = integer(frame.get("gameTick"), "natural game tick")
        server_tick = integer(frame.get("serverTick"), "natural server tick")
        index = integer(frame.get("eventIndex"), "natural event index", previous + 1, len(events) - 1)
        pose(frame.get("pose"))
        snapshot = frame.get("body")
        s.require(isinstance(snapshot, dict), "Missing natural body snapshot")
        for key in ("position", "motion"):
            vector(snapshot.get(key), "natural " + key)
        for key in ("fall", "health", "absorption"):
            number(snapshot.get(key), "natural " + key)
        s.require(snapshot.get("neutral") is True and all(type(snapshot.get(k)) is bool
                  for k in ("grounded", "needsSync", "syncVelocity", "collision")), "Invalid natural native state")
        event = events[index]
        s.require(event["kind"] == kind and event["data"] == f"source={source[1]} serverTick={server_tick}",
                  "Natural snapshot does not identify its original operation event")
        expected_body = {k: snapshot[k] for k in ("position", "motion", "fall", "grounded", "neutral", "health")}
        expected_body.update(entity=identity["ownerEntity"], tick=tick, horizontalCollision=snapshot["collision"], pose=frame["pose"])
        s.require(exact(event.get("after"), expected_body) and sum(e["kind"] == kind for e in events) == 1,
                  "Natural snapshot body or single-operation count differs from ledger")
        previous = index
    release, before, after, listener, tracker, sent, sent_exit, tracker_exit = (proof[name] for name in
        ("release", "beforeStep", "afterStep", "listenerExit", "trackerEntry", "send", "sendExit", "trackerExit"))
    tracker_index = integer(proof.get("trackerWitnessIndex"), "retained tracker event index", 0, release["eventIndex"] - 1)
    tracker_id = proof.get("trackerIdentity")
    s.require(isinstance(tracker_id, str) and re.fullmatch(r"[0-9]{1,10}", tracker_id), "Missing retained original tracker identity")
    tracker_event = events[tracker_index]
    s.require(tracker_event["kind"] == "natural-tracker-identity"
              and tracker_event["data"] == f"tracker={tracker_id} owner={identity['ownerUuid']}"
              and tracker_event.get("after", {}).get("entity") == identity["ownerEntity"]
              and tracker_event["after"]["tick"] <= release["gameTick"]
              and sum(e["kind"] == "natural-tracker-identity" for e in events) == 1,
              "Original owner tracker identity was not retained before release")
    for frame in (before, after, listener):
        s.require((frame["gameTick"], frame["serverTick"]) == (release["gameTick"], release["serverTick"]),
                  "Native release and its one physics step have different clocks")
    for frame in (tracker, sent, sent_exit, tracker_exit):
        s.require((frame["gameTick"], frame["serverTick"]) == (release["gameTick"] + 1, release["serverTick"] + 1),
                  "Natural motion is not in the very next native tracker tick")
    s.require(exact(before["body"], release["body"]) and exact(before["pose"], release["pose"]), "Immediate strike state changed before original physics")
    s.require(all(exact(after["body"][k], release["body"][k]) for k in ("health", "absorption")), "Intervening native wound during physics")
    s.require(exact(listener["body"], {**after["body"], "position": before["body"]["position"]}) and exact(listener["pose"], after["pose"]),
              "Native listener exit changed more than its position restoration")
    expected_tracker = validate_owner_receipt(identity, receipt, events, release, listener, tracker)
    expected_pose = listener["pose"] if receipt is None else receipt["handlerReturn"]["pose"]
    s.require(exact(tracker["body"], expected_tracker) and exact(tracker["pose"], expected_pose) and tracker["body"]["syncVelocity"] is True,
              "Native physics state did not reach its original tracker intact")
    s.require(exact(sent["body"], {**tracker["body"], "needsSync": False, "syncVelocity": False}) and exact(sent["pose"], tracker["pose"]),
              "Original tracker changed more than native synchronization flags")
    s.require(exact(sent_exit["body"], tracker_exit["body"]) and exact(sent_exit["body"], sent["body"])
              and exact(sent_exit["pose"], tracker_exit["pose"]) and exact(sent_exit["pose"], sent["pose"]), "Owner state changed while original send/tracker completed")
    s.require(motion["sendOrdinal"] == proof["motionOrdinal"] and exact(motion["raw"], sent["body"]["motion"])
              and sent["eventIndex"] < motion["sendStartIndex"], "One native physics step is not bound to this exact owner motion")
    entries = [e for e in events if e["kind"] == "natural-listener-start"]
    s.require(len(entries) == 1, "Missing or repeated original listener scope")
    entry, end = entries[0], events[tracker_exit["eventIndex"]]
    s.require(release["eventIndex"] < entry["index"] < before["eventIndex"]
              and exact(entry.get("after"), events[release["eventIndex"]]["after"])
              and entry["data"] == events[release["eventIndex"]]["data"], "Original listener did not begin at the retained release state")
    sends = [e for e in events if e["kind"] in ("server-motion-start", "server-motion-sent")
             and e["index"] < end["index"]]
    s.require([e["kind"] for e in sends] == ["server-motion-start", "server-motion-sent"]
              and sends[0]["index"] == motion["sendStartIndex"] and sends[1]["index"] > sends[0]["index"]
              and sends[0]["index"] < sent_exit["eventIndex"] < sends[1]["index"]
              and all(e["data"].startswith("ordinal=1 manual=false ") for e in sends)
              and exact(end.get("after"), events[sent["eventIndex"]]["after"])
              and end["data"] == events[sent["eventIndex"]]["data"], "The selected original tracker did not complete exactly one bound motion send")


def validate_owner_receipt(identity, receipt, events, release, listener, tracker):
    """The optional receipt explains one exact native ground-bit update; it never masks a body field."""
    handler_events = [event for event in events if event["kind"] in
                      ("natural-owner-packet-start", "natural-owner-packet-end")]
    incoming = [event for event in events if event["kind"] == "server-owner-packet-received"
                and release["eventIndex"] < event["index"] < tracker["eventIndex"]]
    if receipt is None:
        s.require(not handler_events and not incoming, "Unattested owner packet before native tracker")
        return listener["body"]
    entry, returned = receipt["handlerEntry"], receipt["handlerReturn"]
    s.require([event["kind"] for event in handler_events] == ["natural-owner-packet-start", "natural-owner-packet-end"]
              and len(incoming) == 1 and listener["eventIndex"] < incoming[0]["index"] < entry["eventIndex"]
              < returned["eventIndex"] < tracker["eventIndex"], "Not exactly one READY owner handler before tracker")
    for frame in (entry, returned):
        s.require((frame["gameTick"], frame["serverTick"]) == (release["gameTick"] + 1, release["serverTick"] + 1),
                  "Queued owner handler has the wrong native clock")
    original, received = receipt.get("originalSend"), receipt.get("received")
    s.require(isinstance(original, dict) and isinstance(received, dict), "Missing original FIFO send or decoded receipt")
    ordinal = integer(original.get("ordinal"), "queued owner ordinal", 1)
    start = integer(original.get("sentIndex"), "queued owner send index", 0, release["eventIndex"] - 1)
    completed = integer(original.get("completedIndex"), "queued owner send completion", start + 1, release["eventIndex"] - 1)
    s.require(original.get("sameSenderAndConnection") is True and original.get("ownerEntity") == identity["ownerEntity"]
              and original.get("ownerUuid") == identity["ownerUuid"], "Queued packet changed sender or connection")
    snapshot, packet = original.get("body"), original.get("packet")
    body(snapshot, identity["ownerEntity"])
    s.require(isinstance(packet, dict) and exact(packet, received) and packet.get("type") == "Pos"
              and packet.get("rotation") is False and exact(packet.get("yaw"), 0.0) and exact(packet.get("pitch"), 0.0)
              and packet.get("grounded") is True and packet.get("collision") is False,
              "Queued receipt is not the exact original rotation-free grounded Pos")
    s.require(snapshot["neutral"] is True and snapshot["grounded"] is True and snapshot["horizontalCollision"] is False
              and exact(snapshot["position"], packet.get("position")) and exact(snapshot["position"], release["body"]["position"])
              and snapshot["tick"] <= release["gameTick"], "Queued owner snapshot or original coordinates changed")
    sha(packet.get("sha256"), "queued Pos native codec")
    for key in ("yaw", "pitch"):
        number(packet.get(key), "queued Pos " + key)
        s.require(math.copysign(1, packet[key]) == 1, "Absent packet rotation retains positive zero")
    # The pinned native Pos codec writes three network-order doubles and its packed flags byte.
    expected_digest = hashlib.sha256(struct.pack(">dddB", *(packet["position"][axis] for axis in ("x", "y", "z")), 1)).hexdigest()
    s.require(packet["sha256"] == expected_digest, "Queued Pos digest differs from its decoded native payload")
    sent_event, completed_event = events[start], events[completed]
    s.require(sent_event["kind"] == "client-owner-position-sent" and exact(sent_event.get("after"), snapshot)
              and completed_event["kind"] == "client-owner-position-send-complete"
              and completed_event["data"] == f"ordinal={ordinal}" and exact(completed_event.get("after"), snapshot),
              "Original send lacks its exact completed pre-release ledger witness")
    for event in (sent_event, incoming[0]):
        match = re.fullmatch(r"ordinal=(\d+) PositionKey\[type=Pos, x=([^,]+), y=([^,]+), z=([^,]+), "
                             r"rotation=false, yaw=NaN, pitch=NaN, ground=true, collision=false\] sha256=([0-9a-f]{64})", event["data"])
        s.require(match is not None and int(match[1]) == ordinal and match[5] == packet["sha256"],
                  "Queued Pos sender/receiver key or digest differs from original ledger")
        decoded = {axis: float(match[i + 2]) for i, axis in enumerate(("x", "y", "z"))}
        s.require(exact(decoded, packet["position"]), "Decoded original queued coordinates differ from ledger")
    queue = []
    last_send = 0
    for event in events[:entry["eventIndex"]]:
        if event["kind"] not in ("client-owner-position-sent", "server-owner-packet-received"):
            continue
        match = re.match(r"ordinal=(\d+) ", event["data"])
        s.require(match is not None, "Missing FIFO owner packet ordinal")
        value = int(match[1])
        if event["kind"] == "client-owner-position-sent":
            s.require(value == last_send + 1, "Noncontiguous original owner send")
            last_send = value
            queue.append(value)
        elif value:
            s.require(queue and queue[0] == value, "Owner receipt searched past the original FIFO head")
            queue.pop(0)
    s.require(exact(entry["body"], listener["body"]) and exact(entry["pose"], listener["pose"]), "Whole saved native body changed before queued handler")
    s.require(exact(returned["body"], {**entry["body"], "grounded": packet["grounded"]}) and exact(returned["pose"], entry["pose"]),
              "Queued handler changed more than its exact packet ground bit")
    return returned["body"]


def validate_reports(case_identity, host, peer, moved):
    """Recheck every native chain link without trusting Java's passed witness."""
    s.require(type(moved) is bool, "Missing explicit movement expectation")
    for report in (host, peer):
        s.require(isinstance(report, dict) and report.get("identity") == case_identity,
                  "Packet report identity differs from armed native bodies")
        s.require(report.get("correction") is False and report.get("error") is None, "Native correction/observation failure")
        clock_proof(report.get("clock")); vector(report.get("finalPosition"), "final position")
        packets = report.get("packets")
        s.require(isinstance(packets, list) and len(packets) <= 256, "Unbounded native packet ledger")
        for index, packet in enumerate(packets, 1):
            s.require(packet.get("ordinal") == index and type(packet["ordinal"]) is int, "Noncontiguous packet ordinals")
            s.require(packet.get("kind") in PACKET_KINDS, "Unrecognized native position packet")
            sha(packet.get("sha256"), "encoded packet"); vector(packet.get("target"), "packet target")
            vector(packet.get("body"), "packet body"); integer(packet.get("tick"), "packet native tick")
            s.require(type(packet.get("interpolating")) is bool, "Missing native interpolation state")
    chain = host.get("ownerChain")
    s.require(isinstance(chain, dict) and peer.get("ownerChain") is None, "Only host may attest the owner/server chain")
    events, positions, motion = chain.get("events"), chain.get("positions"), chain.get("motion")
    s.require(isinstance(events, list) and 1 <= len(events) <= 256 and isinstance(positions, list) and len(positions) <= 256,
              "Missing or unbounded native owner ledger")
    for index, event in enumerate(events):
        s.require(event.get("index") == index and type(event["index"]) is int and isinstance(event.get("kind"), str)
                  and isinstance(event.get("data"), str), "Invalid owner event index/data")
        s.require("correction" not in event["kind"], "Correction found in original owner ledger")
        for field in ("before", "after"):
            if event.get(field) is not None:
                body(event[field], case_identity["ownerEntity"])
        if event["kind"] == "client-tick":
            s.require(event.get("after", {}).get("neutral") is True, "Owner input was not neutral")
        if event["kind"] in {"server-motion-start", "server-motion-sent"}:
            s.require(re.match(r"ordinal=[1-9][0-9]* manual=false ", event["data"]), "Manual or malformed motion dispatch")
    s.require(not moved or isinstance(motion, dict), "Moved case lacks original native motion")
    if motion is not None:
        s.require(isinstance(motion, dict) and motion.get("completed") is True and motion.get("manual") is False
                  and motion.get("originalTracker") is True, "Motion was not completed by the original native tracker")
        ordinal = integer(motion.get("sendOrdinal"), "motion send ordinal", 1)
        applied = integer(motion.get("appliedIndex"), "motion applied index", 0, len(events) - 1)
        started = integer(motion.get("sendStartIndex"), "motion dispatch start index", 0, applied - 1)
        vector(motion.get("raw"), "raw motion"); vector(motion.get("wire"), "encoded motion"); vector(motion.get("applied"), "applied motion")
        s.require(exact(motion["wire"], motion["applied"]), "Owner did not apply exact encoded native motion")
        event = events[applied]
        s.require(event["kind"] == "client-motion-processed" and event["data"].startswith(f"sendOrdinal={ordinal} ")
                  and exact(event.get("after", {}).get("motion"), motion["applied"]), "Motion application does not match retained owner event")
        s.require(events[started]["kind"] == "server-motion-start"
                  and events[started]["data"].startswith(f"ordinal={ordinal} manual=false "), "Native motion dispatch did not precede owner application")
        # send() completion is recorded on the server thread after the native
        # call returns; a fast client can apply its bytes before that log event.
        s.require(any(e["kind"] == "server-motion-sent" and e["data"].startswith(f"ordinal={ordinal} manual=false ")
                      for e in events), "Native motion dispatch lacks its completed send event")
    validate_natural_dispatch(case_identity, chain, events, motion)
    linked = {}
    previous_send, previous_accept = 0, -1
    for position in positions:
        s.require(motion is not None and position.get("completed") is True and position.get("motionOrdinal") == motion["sendOrdinal"],
                  "Owner position is not linked to the completed native motion")
        send = integer(position.get("sendOrdinal"), "position send ordinal", previous_send + 1)
        sent = integer(position.get("sentIndex"), "position sent index", motion["appliedIndex"] + 1, len(events) - 1)
        accepted = integer(position.get("acceptanceIndex"), "position acceptance index", max(sent + 1, previous_accept + 1), len(events) - 1)
        for name in ("requested", "sentPosition", "acceptedPosition"):
            vector(position.get(name), name)
        s.require(exact(position["requested"], position["sentPosition"]) and exact(position["requested"], position["acceptedPosition"]), "Native server did not accept exact genuine owner position")
        sent_event, accepted_event = events[sent], events[accepted]
        s.require(sent_event["kind"] == "client-owner-position-sent" and sent_event["data"].startswith(f"ordinal={send} ")
                  and accepted_event["kind"] == "server-owner-position-processed" and accepted_event["data"].startswith(f"sendOrdinal={send} "),
                  "Position chain indexes do not identify its original send/accept events")
        s.require(exact(sent_event.get("after", {}).get("position"), position["sentPosition"])
                  and exact(accepted_event.get("after", {}).get("position"), position["acceptedPosition"]), "Position event bodies differ from structured chain")
        for name, event in (("ownerTick", sent_event), ("serverTick", accepted_event)):
            integer(position.get(name), name)
            s.require(event["after"]["tick"] == position[name], "Position chain native tick mismatch")
        linked[(accepted, send)] = position
        previous_send, previous_accept = send, accepted
    s.require(not moved or positions, "Moved case lacks genuine accepted owner positions")
    for packet in peer["packets"]:
        s.require(packet.get("originalTracker") is False and packet.get("ownerAcceptanceIndex") == -1
                  and packet.get("ownerSendOrdinal") == -1, "Peer cannot claim server-local chain indexes")
    interpolation = peer.get("interpolation")
    s.require(isinstance(interpolation, list) and len(interpolation) <= 256 and host.get("interpolation", []) == [], "Invalid passive interpolation ledger")
    for step in interpolation:
        ordinal = integer(step.get("packetOrdinal"), "interpolation packet ordinal", 1, len(peer["packets"]))
        packet = peer["packets"][ordinal - 1]
        # Delayed native time-sync packets may move client game time backwards.
        # Causality is carried by the retained packet ordinal, not wall time.
        integer(step.get("tick"), "interpolation native tick")
        s.require(packet["interpolating"] is True and exact(step.get("target"), packet["target"]), "Interpolation is not tied to its native packet target")
        s.require(distance(step.get("before"), step.get("after")) > 1E-12
                  and distance(step["after"], step["target"]) < distance(step["before"], step["target"]), "Peer did not move toward its native interpolation target")
    next_peer, matched, previous_accept = 0, [], -1
    for packet in host["packets"]:
        s.require(packet.get("originalTracker") is True, "Server packet was not the original tracker delivery")
        accepted = integer(packet.get("ownerAcceptanceIndex"), "tracker acceptance index", previous_accept)
        send = integer(packet.get("ownerSendOrdinal"), "tracker owner-send ordinal", 1)
        position = linked.get((accepted, send))
        s.require(position is not None and packet["tick"] >= position["serverTick"], "Tracker packet does not follow its linked native owner acceptance")
        s.require(distance(packet["target"], position["acceptedPosition"]) <= QUANTIZATION_SQUARED
                  and exact(packet["body"], position["acceptedPosition"]), "Original tracker target/body differs from accepted owner position")
        while next_peer < len(peer["packets"]):
            candidate = peer["packets"][next_peer]; next_peer += 1
            if packet["kind"] == candidate["kind"] and packet["sha256"] == candidate["sha256"] and distance(packet["target"], candidate["target"]) <= QUANTIZATION_SQUARED:
                matched.append(candidate["ordinal"]); break
        else:
            raise ValueError("Peer did not process every original encoded tracker packet in order")
        previous_accept = accepted
    s.require(not moved or host["packets"] and any(step["packetOrdinal"] in matched for step in interpolation),
              "Moved case lacks original tracker delivery and native peer interpolation")
    s.require(distance(host["finalPosition"], peer["finalPosition"]) < .002**2, "Peer final position did not converge to native server position")


def witness(ipc, filename, role, identity, pids):
    values = s.read_properties(ipc / filename)
    for key, value in {**identity, "role": role, "pid": str(pids[role])}.items():
        s.require(values.get(key) == value, filename + " changed " + key)
    return values


def endpoint(value, label):
    s.require(isinstance(value, (list, tuple)) and len(value) == 2 and value[0] == "127.0.0.1", "Non-loopback " + label)
    return [value[0], integer(value[1], label + " port", 1, 65535)]


def property_endpoint(values, prefix):
    return endpoint([values.get(prefix + "Host"), s.decimal(values.get(prefix + "Port"), prefix + " port", True)], prefix)


def validate_connections(ipc, identity, pids, report):
    ready = witness(ipc, "server-ready.properties", "host", identity, pids)
    backend = relay.BackendIdentity.from_ready(ready, expected_nonce=identity["nonce"], expected_host_pid=pids["host"])
    s.require(report.get("identity") == asdict(backend), "Relay backend identity changed")
    supervisor = witness(ipc, "relay-ready.properties", "supervisor", identity, pids)
    s.require(supervisor.get("hostPid") == str(pids["host"]) and supervisor.get("peerPid") == str(pids["peer"]), "Relay witness changed owned JVMs")
    server = witness(ipc, "server-connected.properties", "host", identity, pids)
    connections = report.get("connections", {})
    s.require(set(connections) == {"owner", "observer"}, "Missing both retained relay TCP connection tuples")
    entities = {}
    for role, owner in (("host", "owner"), ("peer", "observer")):
        client = witness(ipc, role + "-connected.properties", role, identity, pids)
        pair = connections[owner]
        s.require(pair.get("connected") is True, "Relay backend never connected")
        tuples = {key: endpoint(pair.get(key), key) for key in ("clientLocal", "clientRemote", "backendLocal", "backendRemote")}
        s.require(tuples["clientRemote"] == property_endpoint(client, "clientLocal")
                  and tuples["clientLocal"] == property_endpoint(client, "clientRemote"), "Client receipt is not the relay's accepted TCP stream")
        s.require(tuples["backendRemote"] == ["127.0.0.1", backend.backend_port]
                  and tuples["backendLocal"] == property_endpoint(server, owner + "BackendRemote"), "Server receipt is not the relay's connected backend stream")
        port = report.get(owner + "Port")
        s.require(tuples["clientLocal"] == ["127.0.0.1", port] and supervisor.get(owner + "Port") == str(port), "Relay listener identity differs")
        entities[role] = s.decimal(client.get("entity"), "native entity", True)
        s.require(server.get(owner + "Entity") == str(entities[role]), "Server/client native entity identities differ")
    s.require(entities["host"] != entities["peer"], "Native owner and observer must be distinct bodies")
    return entities


def validate_relay(report, identity, pids):
    s.require(report.get("status") == "passed" and report.get("closed") is True
              and report.get("failure") is None and report.get("cleanupErrors") == [], "Relay did not close cleanly")
    s.require(report.get("profile") == identity["profile"] and report.get("backendAddress") == "127.0.0.1", "Relay profile/address changed")
    s.require(report.get("limits") == asdict(relay.Limits()) and report.get("ownedSocketsHigh") == 6, "Relay resource ceilings changed")
    s.require(report.get("delaysMs") == dict(zip(relay.DIRECTION_NAMES, relay.PROFILES[identity["profile"]])), "Relay delays changed")
    s.require(report.get("identity", {}).get("nonce") == identity["nonce"] and report["identity"].get("host_pid") == pids["host"], "Relay changed its owned host identity")
    ports = [integer(report.get(k), k, 1, 65535) for k in ("ownerPort", "observerPort")]
    ports.append(integer(report["identity"].get("backend_port"), "backend port", 1, 65535))
    s.require(len(set(ports)) == 3, "Relay and native backend ports overlap")
    directions = report.get("directions", {})
    s.require(set(directions) == set(relay.DIRECTION_NAMES), "Missing one of four whole connection streams")
    total = 0
    for name, stream in directions.items():
        count = integer(stream.get("readBytes"), name + " read bytes", 1, relay.Limits().stream_bytes)
        total += count
        s.require(stream.get("writtenBytes") == count and stream.get("readOffsetRange") == stream.get("writeOffsetRange") == [0, count], "Stream counts/offsets differ")
        s.require(all(stream.get(key) is True for key in ("connected", "offsetsContiguous", "eof", "writeHalfClosed")), "Stream lacks contiguous drained orderly EOF")
        s.require(all(stream.get(key) == 0 for key in ("queuedBytes", "bufferedBytes")), "Relay retained undrained bytes")
        s.require(sha(stream.get("readSha256"), name) == sha(stream.get("writeSha256"), name), "Whole-stream hashes differ")
        sha(stream.get("eventsSha256"), name + " event ledger")
        integer(stream.get("readChunks"), "read chunks", 1); integer(stream.get("writeCalls"), "write calls", 1)
        integer(stream.get("queueHighBytes"), "queue high bytes", 1, relay.Limits().queue_bytes)
        integer(stream.get("queueHighChunks"), "queue high chunks", 1, relay.Limits().queue_chunks)
        delay = report["delaysMs"][name]
        s.require(stream.get("delayMs") == delay, "Per-stream delay differs")
        low = number(stream.get("residenceMinSeconds"), "minimum residence")
        high = number(stream.get("residenceMaxSeconds"), "maximum residence")
        mean = number(stream.get("residenceByteMeanSeconds"), "mean residence")
        late = number(stream.get("releaseLatenessMaxSeconds"), "release lateness")
        s.require(delay / 1000 - 1E-9 <= low <= mean + 1E-9 and mean <= high + 1E-9
                  and high <= delay / 1000 + relay.Limits().lateness_seconds + 1E-9
                  and 0 <= late <= relay.Limits().lateness_seconds, "Relay violated configured monotonic delay/release ceiling")
    s.require(report.get("totalReadBytes") == total <= relay.Limits().total_stream_bytes
              and report.get("totalQueuedBytes") == report.get("totalBufferedBytes") == 0, "Aggregate relay byte accounting differs")
    integer(report.get("queueHighBytes"), "aggregate queue high bytes", 1, relay.Limits().total_queue_bytes)


def validate_witnesses(ipc, identity, pids, relay_report):
    s.require(set(pids) == {"host", "peer", "supervisor"} and len(set(pids.values())) == 3, "Exactly two distinct owned JVMs and their supervisor required")
    validate_relay(relay_report, identity, pids)
    entities = validate_connections(ipc, identity, pids, relay_report)
    names = set(TERMINALS) | {"server-ready.properties", "relay-ready.properties", "host-connected.properties", "peer-connected.properties", "server-connected.properties"}
    cases = roster(identity["profile"])
    for filename, role in TERMINALS.items():
        s.require(witness(ipc, filename, role, identity, pids).get("cases") == ",".join(cases), "Terminal omitted/reordered fixed cases")
    for index, name in enumerate(cases):
        stem = f"case-{index:02d}"
        paths = {kind: ipc / (stem + "-" + kind + ".json") for kind in ("identity", "host", "peer")}
        names.update(path.name for path in paths.values())
        expected = {"nonce": identity["nonce"], "profile": identity["profile"], "name": name,
                    "ownerUuid": identity["hostUuid"], "ownerEntity": entities["host"], "ownerGeneration": 1,
                    "peerUuid": identity["peerUuid"], "peerEntity": entities["peer"], "peerGeneration": 1}
        values = {kind: read_json(path) for kind, path in paths.items()}
        s.require(values["identity"] == expected, "Case identity differs from exact roster/current body generation")
        hashes = {kind: s.digest(path) for kind, path in paths.items()}
        moved = name not in {"FULL_RESISTANCE", "INVULNERABLE"}
        for phase, role, fields in (
                ("arm", "host", {"identitySha256": hashes["identity"]}),
                ("armed", "peer", {"identitySha256": hashes["identity"]}),
                ("ready", "host", {"reportSha256": hashes["host"], "moved": str(moved).lower()}),
                ("seen", "peer", {"reportSha256": hashes["peer"]}),
                ("passed", "host", {"hostReportSha256": hashes["host"], "peerReportSha256": hashes["peer"]})):
            filename = stem + "-" + phase + ".properties"; names.add(filename)
            value = witness(ipc, filename, role, identity, pids)
            s.require(all(value.get(key) == expected_value for key, expected_value in {"case": name, **fields}.items()), "Case handshake/hash binding differs: " + filename)
        validate_reports(expected, values["host"], values["peer"], moved)
    s.require({p.name for p in ipc.iterdir()} == names, "Unexpected, missing or unfinished IPC artifacts")
    return {name: s.digest(ipc / name) for name in sorted(names)}


def evidence_files(base, *, active_profile=None):
    """Bound live writes or strictly enumerate quiescent evidence for hashing.

    Java publishes write-once IPC files by moving a sibling .tmp atomically.
    A live walk can enumerate that temporary name just before it disappears.
    Accept only this known publication, with its regular destination present;
    final scans must never forgive a missing entry. No payloads are read here.
    """
    phase = "live" if active_profile is not None else "final"
    publications = {}
    if active_profile is not None:
        names = set(TERMINALS) | {"server-ready.properties", "server-connected.properties",
                "host-connected.properties", "peer-connected.properties", "host-failure.properties", "peer-failure.properties"}
        for index in range(len(roster(active_profile))):
            names.update(f"case-{index:02d}-{suffix}" for suffix in (
                "identity.json", "host.json", "peer.json", "arm.properties", "armed.properties",
                "ready.properties", "seen.properties", "passed.properties"))
        publications = {(base / "ipc" / name).with_suffix(".tmp"): base / "ipc" / name for name in names}

    root_fd, ipc_fd, walk_stream = None, None, None

    def same_directory(named, retained, path):
        s.require(stat.S_ISDIR(named.st_mode) and not stat.S_ISLNK(named.st_mode)
                  and (named.st_dev, named.st_ino) == (retained.st_dev, retained.st_ino),
                  f"Changed or linked {phase} evidence directory: {path}")

    def check_pinned_directories():
        if root_fd is not None:
            same_directory(base.lstat(), os.fstat(root_fd), base)
        if ipc_fd is not None:
            same_directory(os.stat("ipc", dir_fd=root_fd, follow_symlinks=False), os.fstat(ipc_fd), base / "ipc")

    def metadata(path, entry_fd=None):
        if entry_fd is not None:
            check_pinned_directories()
            try:
                return os.stat(path.name, dir_fd=entry_fd, follow_symlinks=False)
            finally:
                check_pinned_directories()
        return path.lstat()

    def inspect(path, *, directory=False, entry_fd=None):
        original = path
        try:
            try:
                info = metadata(path, entry_fd)
            except FileNotFoundError:
                if directory or path not in publications:
                    raise
                path = publications[path]
                info = metadata(path, entry_fd)
        except OSError as error:
            raise ValueError(f"Cannot inspect {phase} evidence path {original} (checking {path}): {error}") from error
        label = f"{phase} evidence path: {path}"
        s.require(not stat.S_ISLNK(info.st_mode), "Linked " + label)
        s.require(stat.S_ISDIR(info.st_mode) if directory else stat.S_ISREG(info.st_mode),
                  ("Non-directory " if directory else "Non-regular ") + label)
        return path, info

    def walk_error(error):
        raise ValueError(f"Cannot walk {phase} evidence path {error.filename}: {error}") from error

    total, files, sizes = 0, {}, {}
    _, root_info = inspect(base, directory=True)
    try:
        if active_profile is not None:
            flags = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW | os.O_CLOEXEC
            root_fd = os.open(base, flags)
            same_directory(root_info, os.fstat(root_fd), base)
            check_pinned_directories()
            ipc_info = os.stat("ipc", dir_fd=root_fd, follow_symlinks=False)
            s.require(not stat.S_ISLNK(ipc_info.st_mode), f"Linked {phase} evidence path: {base / 'ipc'}")
            ipc_fd = os.open("ipc", flags, dir_fd=root_fd)
            same_directory(ipc_info, os.fstat(ipc_fd), base / "ipc")
            check_pinned_directories()
        if active_profile is not None:
            walk_stream = os.fwalk(".", dir_fd=root_fd, follow_symlinks=False, onerror=walk_error)
            rows = ((base / directory, children, names, descriptor)
                    for directory, children, names, descriptor in walk_stream)
        else:
            rows = ((Path(directory), children, names, None)
                    for directory, children, names in os.walk(base, followlinks=False, onerror=walk_error))
        for directory, children, names, entry_fd in rows:
            check_pinned_directories()
            if entry_fd is None:
                inspect(directory, directory=True)
            else:
                s.require(stat.S_ISDIR(os.fstat(entry_fd).st_mode), f"Non-directory {phase} evidence path: {directory}")
            for name in children:
                inspect(directory / name, directory=True, entry_fd=entry_fd)
            for name in names:
                path, info = inspect(directory / name, entry_fd=entry_fd)
                relative = path.relative_to(base).as_posix()
                # A directory snapshot can contain both sides of a rename. Count
                # the published file once, including when it was not in that snapshot.
                total += info.st_size - sizes.get(relative, 0)
                sizes[relative] = info.st_size
                s.require(total <= MAX_EVIDENCE, f"Aggregate {phase} profile evidence (including logs) exceeded 16 MiB: {path}")
                files[relative] = path
                s.require(len(files) <= 1024, f"Unbounded {phase} evidence file count: {path}")
        check_pinned_directories()
        return total, files
    except OSError as error:
        raise ValueError(f"Cannot inspect {phase} evidence path {base}: {error}") from error
    finally:
        try:
            if walk_stream is not None:
                walk_stream.close()
        finally:
            try:
                if ipc_fd is not None:
                    os.close(ipc_fd)
            finally:
                if root_fd is not None:
                    os.close(root_fd)


def artifact_hashes(base):
    _, files = evidence_files(base)
    return {name: s.digest(path) for name, path in sorted(files.items()) if name != "result.json"}


def write_properties(path, values):
    s.require(not path.exists(), "Supervisor witness must be fresh")
    for key, value in values.items():
        s.require(re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", key) and isinstance(value, str)
                  and not any(c in value for c in "\r\n\\"), "Unsafe supervisor property")
    temporary = path.with_suffix(".tmp")
    with temporary.open("x", encoding="ascii") as output:
        output.write("".join(key + "=" + value + "\n" for key, value in sorted(values.items())))
    temporary.replace(path)


def supervise(base, ipc, identity, jobs, listeners, deadline, *, clock=time.monotonic, sleeper=time.sleep, relay_factory=relay.StoneHingeRelay):
    """Fast bounded process/relay loop, independently testable without sockets."""
    pids = {role: process.pid for role, process in jobs}
    s.require(set(pids) == {"host", "peer"} and len(set(pids.values())) == 2, "Exactly two owned Java processes required")
    owned_relay = None
    try:
        while True:
            now = clock()
            s.require(math.isfinite(now) and now < deadline, "Stone Hinge profile elapsed deadline expired")
            evidence_files(base, active_profile=identity["profile"])
            states = {role: process.poll() for role, process in jobs}
            s.require(not any((ipc / (role + "-failure.properties")).exists() for role in pids), "Native client published a failure witness")
            for role, state in states.items():
                s.require(state in (None, 0), "Owned native client failed: " + role + " exit=" + str(state))
                s.require(state is None or (owned_relay is not None and (ipc / (role + "-stone-hinge-passed.properties")).is_file()), "Owned native client exited before its complete terminal witness")
            if owned_relay is None and (ipc / "server-ready.properties").exists():
                s.require(states["host"] is None, "Owned host exited before server-ready validation")
                ready = witness(ipc, "server-ready.properties", "host", identity, pids)
                backend = relay.BackendIdentity.from_ready(ready, expected_nonce=identity["nonce"], expected_host_pid=pids["host"])
                s.require(dict(jobs)["host"].poll() is None, "Owned host died before relay backend binding")
                owned_relay = relay_factory(backend, identity["profile"], listeners=listeners)
                endpoints = owned_relay.endpoints
                write_properties(ipc / "relay-ready.properties", {**identity, "role": "supervisor", "pid": str(os.getpid()),
                    "hostPid": str(pids["host"]), "peerPid": str(pids["peer"]), "ownerPort": str(endpoints["owner"][1]), "observerPort": str(endpoints["observer"][1])})
            if owned_relay is not None:
                owned_relay.pump(min(.01, max(0.0, deadline - clock())))
                if all(code == 0 for code in states.values()) and owned_relay.complete:
                    owned_relay.assert_complete()
                    return owned_relay
            else:
                sleeper(min(.01, max(0.0, deadline - clock())))
    except BaseException:
        if owned_relay is not None:
            owned_relay.close()
            matrix.write_report(base / "relay.json", owned_relay.report())
        raise


@contextmanager
def cleanup_signals():
    handlers = {sig: signal.signal(sig, signal.SIG_IGN) for sig in (signal.SIGALRM, signal.SIGINT, signal.SIGTERM)}
    try:
        yield
    finally:
        for sig, handler in handlers.items():
            signal.signal(sig, handler)


@contextmanager
def stage_guard(deadline):
    """Tighten the active POSIX alarm without losing the enclosing deadline."""
    started = time.monotonic()
    seconds = deadline - started
    s.require(math.isfinite(seconds) and seconds > 0, "Stone Hinge stage budget exhausted")
    previous, interval = signal.getitimer(signal.ITIMER_REAL)
    s.require(previous > 0 and interval == 0, "A stage requires the outer elapsed guard")
    signal.setitimer(signal.ITIMER_REAL, min(previous, seconds))
    try:
        yield
    finally:
        # Even an alarm ignored during owned cleanup is rearmed against the
        # original absolute deadline, rather than silently extending the job.
        signal.setitimer(signal.ITIMER_REAL, max(.000001, previous - (time.monotonic() - started)))


def run_profile(root, base, work, launch, launch_hash, profile, source, eula, deadline, used_ports):
    started = time.monotonic()
    profile_deadline = min(deadline, started + PROFILE_SECONDS)
    active_deadline = profile_deadline - 20  # Existing owned terminate/wait/kill helper has a 20-second ceiling.
    timeout = math.floor(profile_deadline - started)
    s.require(timeout >= 60, "Insufficient remaining budget for another native profile")
    base.mkdir(); work.mkdir(parents=True); ipc = base / "ipc"; ipc.mkdir()
    nonce = str(uuid.uuid4())
    identity = {key: launch[key] for key in ("sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256")}
    identity.update(nonce=nonce, suite=SUITE, profile=profile, hostUuid=s.PROFILES["host"][1], peerUuid=s.PROFILES["peer"][1],
                    runIdentity="-".join([source["runId"] or "local", source["runAttempt"] or "0", nonce]))
    report = {"schemaVersion": 1, "status": "failed", **identity, **FLAGS, "cases": roster(profile),
              "timeoutSeconds": timeout, "heapMiBPerJvm": 2048, "javaVersion": 25, "launcherPid": os.getpid(),
              "launchSha256": launch_hash, "runtimeSha256": launch["runtimeSha256"], "provenance": source, "processes": {}, "cleanupErrors": []}
    jobs, logs, listeners, owned_relay = [], [], {}, None
    try:
        listeners = relay.reserve_listeners()
        ports = {socket.getsockname()[1] for socket in listeners.values()}
        s.require(len(ports) == 2 and not ports.intersection(used_ports), "Every profile needs fresh retained relay listener ports")
        used_ports.update(ports)
        matrix.write_report(base / "launch-proof.json", {**report, "status": "started"})
        for role in ("host", "peer"):
            s.require(time.monotonic() < active_deadline, "Profile deadline expired before launch")
            game = work / role; game.mkdir(); (game / "tmp").mkdir(); (game / "cache").mkdir()
            if role == "host":
                (game / "eula.txt").write_bytes(eula)
            argfile = game / "java.args"
            argfile.write_text("\n".join(s.java_quote(arg) for arg in s.command(launch, role, game, ipc, identity, timeout, selection(profile))) + "\n", encoding="utf-8")
            log = (base / (role + ".log")).open("xb"); logs.append(log)
            process = subprocess.Popen([launch["java"], "@" + str(argfile)], cwd=game, env=s.environment(launch, game),
                                       stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, shell=False)
            jobs.append((role, process))
        owned_relay = supervise(base, ipc, identity, jobs, listeners, active_deadline)
        backend_port = owned_relay.identity.backend_port
        s.require(backend_port not in used_ports, "Every profile needs a fresh actual native backend port")
        used_ports.add(backend_port)
        report["nativeCompleted"] = True
    except BaseException as error:
        report["error"] = type(error).__name__ + ": " + str(error)[:2048]
        raise
    finally:
        with cleanup_signals():
            report["cleanupErrors"] = s.stop_owned(jobs)
            for log in logs:
                log.close()
            if owned_relay is not None:
                owned_relay.close()
                matrix.write_report(base / "relay.json", owned_relay.report())
            else:
                for listener in listeners.values():
                    try:
                        listener.close()
                    except OSError as error:
                        report["cleanupErrors"].append("listener close: " + str(error))
            report["processes"] = {role: {"pid": process.pid, "exit": process.returncode} for role, process in jobs}
            report["seconds"] = round(time.monotonic() - started, 6)
            matrix.write_report(base / "result.json", report)
    try:
        s.require(not report["cleanupErrors"] and all(p["exit"] == 0 for p in report["processes"].values()), "Owned Java cleanup/exits were not clean")
        s.require(time.monotonic() <= profile_deadline, "Profile including cleanup exceeded 300 seconds")
        pids = {role: process.pid for role, process in jobs}; pids["supervisor"] = os.getpid()
        report["witnesses"] = validate_witnesses(ipc, identity, pids, read_json(base / "relay.json"))
        report["artifacts"] = artifact_hashes(base)
        s.require(time.monotonic() <= profile_deadline, "Profile validation exceeded its elapsed ceiling")
        report["status"] = "passed"
        report["seconds"] = round(time.monotonic() - started, 6)
        matrix.write_report(base / "result.json", report)
        evidence_files(base)
    except BaseException as error:
        report.update(status="failed", error=type(error).__name__ + ": " + str(error)[:2048])
        matrix.write_report(base / "result.json", report)
        raise
    return report


def validate_profile(base, expected, launch, launch_hash, source, seen):
    result = read_json(base / "result.json", 2 * 1024 * 1024)
    s.require(result == expected and result.get("status") == "passed" and result.get("cleanupErrors") == [], "Profile result changed or failed")
    identity = {key: result[key] for key in IDENTITY}
    nonce = identity["nonce"]
    s.require(str(uuid.UUID(nonce)) == nonce and nonce not in seen, "Profile nonce is invalid or reused")
    seen.add(nonce)
    s.require(identity["suite"] == SUITE and identity["profile"] == base.name
              and result.get("cases") == roster(base.name), "Profile suite/roster changed")
    s.require(identity["runIdentity"] == "-".join([source["runId"] or "local", source["runAttempt"] or "0", nonce]), "Run/nonce identity differs")
    s.require(all(identity[key] == launch[key] for key in ("sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256")), "Profile source/descriptor changed")
    s.require(result.get("launchSha256") == launch_hash and result.get("runtimeSha256") == launch["runtimeSha256"]
              and result.get("provenance") == source and all(result.get(key) is value for key, value in FLAGS.items()), "Profile frozen inputs/proof scope changed")
    s.require(result.get("heapMiBPerJvm") == 2048 and result.get("javaVersion") == 25
              and 60 <= integer(result.get("timeoutSeconds"), "profile timeout") <= PROFILE_SECONDS
              and 0 <= number(result.get("seconds"), "profile elapsed") <= result["timeoutSeconds"], "Profile resource budget differs")
    processes = result.get("processes", {})
    s.require(set(processes) == {"host", "peer"} and all(p.get("exit") == 0 for p in processes.values()), "Both actual JVMs must exit zero")
    pids = {role: integer(p.get("pid"), "owned Java PID", 1) for role, p in processes.items()}
    pids["supervisor"] = integer(result.get("launcherPid"), "supervisor PID", 1)
    proof = read_json(base / "launch-proof.json", 2 * 1024 * 1024)
    s.require(proof.get("status") == "started" and all(proof.get(key) == result[key] for key in (*IDENTITY, "cases", "runtimeSha256", "provenance", "launchSha256", "timeoutSeconds", "heapMiBPerJvm", "javaVersion", "launcherPid")), "Launch proof differs from final result")
    s.require(result.get("witnesses") == validate_witnesses(base / "ipc", identity, pids, read_json(base / "relay.json")), "Final native witness ledger changed")
    s.require(result.get("artifacts") == artifact_hashes(base), "Profile evidence/log bytes changed after cleanup")


def run(options, root=ROOT):
    root = root.resolve()
    s.require(type(options.total_timeout) is int and 1 <= options.total_timeout <= TOTAL_SECONDS, "Total elapsed ceiling must be 1..1200 seconds")
    candidate = options.output if options.output.is_absolute() else root / options.output
    for path in (candidate, *candidate.parents):
        s.require(not path.is_symlink(), "Output cannot traverse a symlink")
        if path == root:
            break
    base = s.output_path(candidate, root)
    s.require(not base.exists(), "Stone Hinge output must be fresh")
    s.ignored_output(base, root)
    started = time.monotonic(); total_deadline = started + options.total_timeout
    work_deadline = total_deadline - CLEANUP_SECONDS
    base.mkdir(parents=True)
    report = {"schemaVersion": 1, "suite": SUITE, "status": "failed", **FLAGS, "totalTimeoutSeconds": options.total_timeout,
              "exportCeilingSeconds": EXPORT_SECONDS, "profileCeilingSeconds": PROFILE_SECONDS,
              "finalValidationCeilingSeconds": VALIDATION_SECONDS, "reservedCleanupSeconds": CLEANUP_SECONDS,
              "maxConcurrentJvms": 2, "heapMiBPerJvm": 2048, "profiles": []}
    path = base / "matrix-result.json"; matrix.write_report(path, report)
    handlers = {}
    try:
        def interrupted(signum, frame):
            raise InterruptedError("Stone Hinge supervisor received signal " + str(signum))
        for sig in (signal.SIGINT, signal.SIGTERM):
            handlers[sig] = signal.signal(sig, interrupted)
        with matrix.elapsed_guard(matrix.remaining(work_deadline)), s.supervisor_lock(root):
            export_started = time.monotonic()
            # The export allocation covers provenance, contract/EULA, Gradle,
            # runtime fingerprints, and descriptor preflight together.
            with stage_guard(min(work_deadline, export_started + EXPORT_SECONDS)):
                report["provenance"] = source = provenance(root)
                load_contract(root)
                eula, report["eulaSource"] = s.accepted_eula(options.accepted_eula, root)
                launch_path = base / "stone-hinge-peer-launch.json"
                # Export helper owns its process group; reserve its ten-second reap.
                matrix.export(root, SUITE, launch_path, base / "export.log", min(work_deadline, export_started + EXPORT_SECONDS - 10))
                matrix.remaining(work_deadline)
                launch = read_json(launch_path, 2 * 1024 * 1024)
                launch_hash = validate_launch(launch, launch_path, root)
                report.update(launchSha256=launch_hash, descriptorSha256=launch["descriptorSha256"], runtimeSha256=launch["runtimeSha256"])
                (base / "stone-hinge-peer-descriptor.json").write_bytes(Path(launch["descriptor"]).read_bytes())
                for item in launch["runtimeSha256"]:
                    runtime = Path(item).resolve()
                    s.require(base != runtime and base not in runtime.parents and runtime not in base.parents, "Diagnostic output overlaps runtime inputs")
            s.require(time.monotonic() - export_started <= EXPORT_SECONDS, "Export/preflight including cleanup exceeded 180 seconds")
            report["exportSeconds"] = round(time.monotonic() - export_started, 6)
            used_ports = set()
            for profile in PROFILES:
                with stage_guard(min(work_deadline - VALIDATION_SECONDS, time.monotonic() + PROFILE_SECONDS)):
                    result = run_profile(root, base / profile, base / "work" / profile, launch, launch_hash, profile, source,
                                         eula, work_deadline - VALIDATION_SECONDS, used_ports)
                report["profiles"].append({"profile": profile, "status": "passed", "nonce": result["nonce"], "resultSha256": s.digest(base / profile / "result.json")})
                matrix.write_report(path, report)
            validation_started = time.monotonic()
            s.require(validation_started < work_deadline, "No final validation budget remains")
            # Recheck all exact completed files and every exported runtime fingerprint.
            seen = set()
            with stage_guard(min(work_deadline, validation_started + VALIDATION_SECONDS)):
                for entry in report["profiles"]:
                    profile_base = base / entry["profile"]
                    s.require(entry["resultSha256"] == s.digest(profile_base / "result.json"), "Completed profile result changed")
                    result = read_json(profile_base / "result.json", 2 * 1024 * 1024)
                    validate_profile(profile_base, result, launch, launch_hash, source, seen)
                s.require(validate_launch(launch, launch_path, root) == launch_hash and provenance(root) == source, "Source/runtime/provenance changed during diagnostic")
            s.require(time.monotonic() - validation_started <= VALIDATION_SECONDS and time.monotonic() < work_deadline,
                      "Final validation exceeded its 60-second elapsed ceiling")
            s.require(len(seen) == 3 and [entry["profile"] for entry in report["profiles"]] == list(PROFILES), "All three fresh sequential profiles are required")
            report.update(status="passed", finalValidationSeconds=round(time.monotonic() - validation_started, 6))
    except BaseException as error:
        report.update(status="failed", error=type(error).__name__ + ": " + str(error)[:2048])
        raise
    finally:
        with cleanup_signals():
            report["seconds"] = round(time.monotonic() - started, 6)
            if time.monotonic() > total_deadline:
                report.update(status="failed", error="Total elapsed budget including cleanup exceeded")
            matrix.write_report(path, report)
        for sig, handler in handlers.items():
            signal.signal(sig, handler)
    s.require(report["status"] == "passed", "Stone Hinge diagnostic failed; retained matrix-result.json explains why")
    return report


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path("build/native/stone-hinge-peer"))
    parser.add_argument("--total-timeout", type=int, default=TOTAL_SECONDS)
    parser.add_argument("--accepted-eula", type=Path)
    options = parser.parse_args(argv)
    if not 1 <= options.total_timeout <= TOTAL_SECONDS:
        parser.error("Total elapsed budget must be 1..1200 seconds")
    return options


if __name__ == "__main__":
    try:
        run(parse_args())
    except (ValueError, RuntimeError, OSError, subprocess.SubprocessError) as error:
        print("Stone Hinge peer diagnostic failed: " + str(error), file=sys.stderr)
        raise SystemExit(1)
