"""Synthetic/fake-process Stone Hinge supervisor tests. Never open sockets or run Java."""
import copy
from contextlib import contextmanager
from dataclasses import asdict
import json
import os
from pathlib import Path
import signal
import subprocess
import tempfile
import types
import unittest
from unittest.mock import patch

from native import run_stone_hinge_peer as gate
from test_two_client_supervisor import Fixture as LaunchFixture


def vec(x=0, y=1, z=0):
    return {"x": x, "y": y, "z": z}


def body(position=None, motion=None, tick=10):
    return {"position": position or vec(), "motion": motion or vec(0, .4, .2), "tick": tick,
            "entity": 10, "fall": 0, "grounded": False, "neutral": True, "health": 192, "horizontalCollision": False}


def case_identity(identity, name):
    return {"nonce": identity["nonce"], "profile": identity["profile"], "name": name,
            "ownerUuid": identity["hostUuid"], "ownerEntity": 10, "ownerGeneration": 1,
            "peerUuid": identity["peerUuid"], "peerEntity": 20, "peerGeneration": 1}


def native_reports(identity, moved=True):
    position, motion = vec(1, 2, 3), vec(0, .4, .2)
    events = [
        {"index": 0, "kind": "server-motion-start", "data": "ordinal=1 manual=false raw=example wire=example", "after": body()},
        {"index": 1, "kind": "server-motion-sent", "data": "ordinal=1 manual=false raw=example wire=example", "after": body()},
        {"index": 2, "kind": "client-motion-processed", "data": "sendOrdinal=1 payload=example", "after": body(motion=motion)},
        {"index": 3, "kind": "client-owner-position-sent", "data": "ordinal=1 example", "after": body(position)},
        {"index": 4, "kind": "server-owner-position-processed", "data": "sendOrdinal=1 requested=example", "after": body(position)},
        {"index": 5, "kind": "client-tick", "data": "", "after": body(position)},
    ]
    chain = {"motion": {"sendOrdinal": 1, "raw": motion, "wire": motion, "applied": motion, "sendStartIndex": 0, "appliedIndex": 2,
                        "manual": False, "originalTracker": True, "completed": True},
             "positions": [{"sendOrdinal": 1, "sentIndex": 3, "acceptanceIndex": 4, "motionOrdinal": 1,
                            "requested": position, "sentPosition": position, "acceptedPosition": position,
                            "ownerTick": 10, "serverTick": 10, "completed": True}], "events": events}
    packet = {"ordinal": 1, "kind": "Pos", "sha256": "a" * 64, "target": position, "body": position,
              "tick": 10, "ownerAcceptanceIndex": 4, "ownerSendOrdinal": 1, "originalTracker": True, "interpolating": False}
    clock = {"observations": 20, "firstTick": 10, "lastTick": 29, "tickTransitions": 19,
             "nativeTimeResyncs": 0, "maximumGapNanos": 50_000_000}
    host = {"identity": identity, "packets": [packet], "clock": clock, "finalPosition": position,
            "correction": False, "ownerChain": chain}
    peer = {"identity": copy.deepcopy(identity), "packets": [{**packet, "ownerAcceptanceIndex": -1,
            "ownerSendOrdinal": -1, "originalTracker": False, "interpolating": True}],
            "clock": copy.deepcopy(clock), "finalPosition": position, "correction": False,
            "interpolation": [{"packetOrdinal": 1, "tick": 11, "before": vec(0, 2, 3), "after": vec(.5, 2, 3), "target": position}]}
    if not moved:
        host["packets"] = []; host["ownerChain"] = {"events": [events[5] | {"index": 0}], "positions": []}
        peer["packets"] = []; peer["interpolation"] = []
    return copy.deepcopy(host), copy.deepcopy(peer)


def relay_report(identity, pids):
    streams = {}
    for name, delay in zip(gate.relay.DIRECTION_NAMES, gate.relay.PROFILES[identity["profile"]]):
        streams[name] = {"connected": True, "delayMs": delay, "readBytes": 10, "writtenBytes": 10,
                         "readOffsetRange": [0, 10], "writeOffsetRange": [0, 10], "offsetsContiguous": True,
                         "readSha256": "b" * 64, "writeSha256": "b" * 64, "eventsSha256": "c" * 64,
                         "readChunks": 1, "writeCalls": 1, "queuedBytes": 0, "bufferedBytes": 0,
                         "queueHighBytes": 10, "queueHighChunks": 1, "eof": True, "writeHalfClosed": True,
                         "backpressureEvents": 0, "backpressureMaxSeconds": 0,
                         "residenceMinSeconds": delay / 1000, "residenceMaxSeconds": delay / 1000,
                         "residenceByteMeanSeconds": delay / 1000, "releaseLatenessMaxSeconds": 0}
    return {"schemaVersion": 1, "status": "passed", "identity": {"nonce": identity["nonce"], "host_pid": pids["host"], "backend_port": 33003},
            "profile": identity["profile"], "delaysMs": dict(zip(gate.relay.DIRECTION_NAMES, gate.relay.PROFILES[identity["profile"]])),
            "ownerPort": 33001, "observerPort": 33002, "backendAddress": "127.0.0.1", "limits": asdict(gate.relay.Limits()),
            "totalReadBytes": 40, "totalQueuedBytes": 0, "totalBufferedBytes": 0, "queueHighBytes": 40,
            "ownedSocketsHigh": 6, "closed": True, "failure": None, "cleanupErrors": [], "directions": streams,
            "connections": {role: {"connected": True, "clientLocal": ["127.0.0.1", listen], "clientRemote": ["127.0.0.1", client],
                                    "backendLocal": ["127.0.0.1", back], "backendRemote": ["127.0.0.1", 33003]}
                            for role, listen, client, back in (("owner", 33001, 33004, 33006), ("observer", 33002, 33005, 33007))}}


class EvidenceFixture(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(); self.addCleanup(temp.cleanup)
        self.base = Path(temp.name); self.ipc = self.base / "ipc"; self.ipc.mkdir()
        self.identity = {"nonce": "ed43d7c7-f1a4-4ac1-a48e-7e9853d78141", "suite": gate.SUITE, "sourceHead": "1" * 40,
                         "checkoutSha": "1" * 40, "prHeadSha": "", "descriptorSha256": "2" * 64,
                         "hostUuid": gate.s.PROFILES["host"][1], "peerUuid": gate.s.PROFILES["peer"][1],
                         "runIdentity": "local-0-ed43d7c7-f1a4-4ac1-a48e-7e9853d78141", "profile": "transparent"}
        self.pids = {"host": 100, "peer": 200, "supervisor": os.getpid()}

    def properties(self, filename, role, fields=None):
        values = {**self.identity, "role": role, "pid": str(self.pids[role]), **(fields or {})}
        (self.ipc / filename).write_text("".join(key + "=" + str(value) + "\n" for key, value in values.items()))

    def write_witnesses(self):
        report = relay_report(self.identity, self.pids)
        self.properties("server-ready.properties", "host", {"port": "33003"})
        self.properties("relay-ready.properties", "supervisor", {"ownerPort": "33001", "observerPort": "33002", "hostPid": "100", "peerPid": "200"})
        for role, port, remote, entity in (("host", "33004", "33001", "10"), ("peer", "33005", "33002", "20")):
            self.properties(role + "-connected.properties", role, {"clientLocalHost": "127.0.0.1", "clientLocalPort": port,
                "clientRemoteHost": "127.0.0.1", "clientRemotePort": remote, "entity": entity})
        self.properties("server-connected.properties", "host", {"ownerBackendRemoteHost": "127.0.0.1", "ownerBackendRemotePort": "33006",
            "observerBackendRemoteHost": "127.0.0.1", "observerBackendRemotePort": "33007", "ownerEntity": "10", "observerEntity": "20"})
        for filename, role in gate.TERMINALS.items():
            self.properties(filename, role, {"cases": ",".join(gate.roster(self.identity["profile"]))})
        for index, name in enumerate(gate.roster(self.identity["profile"])):
            stem = f"case-{index:02d}"
            ident = case_identity(self.identity, name)
            moved = name not in {"FULL_RESISTANCE", "INVULNERABLE"}
            host, peer = native_reports(ident, moved)
            hashes = {}
            for kind, value in (("identity", ident), ("host", host), ("peer", peer)):
                path = self.ipc / (stem + "-" + kind + ".json")
                path.write_text(json.dumps(value)); hashes[kind] = gate.s.digest(path)
            for phase, role, fields in (
                    ("arm", "host", {"identitySha256": hashes["identity"]}),
                    ("armed", "peer", {"identitySha256": hashes["identity"]}),
                    ("ready", "host", {"reportSha256": hashes["host"], "moved": str(moved).lower()}),
                    ("seen", "peer", {"reportSha256": hashes["peer"]}),
                    ("passed", "host", {"hostReportSha256": hashes["host"], "peerReportSha256": hashes["peer"]})):
                self.properties(stem + "-" + phase + ".properties", role, {"case": name, **fields})
        return report


class NativeChainTests(EvidenceFixture):
    def test_complete_chain_and_native_interpolation_are_independently_accepted(self):
        identity = case_identity(self.identity, "RIGHT")
        gate.validate_reports(identity, *native_reports(identity), True)

    def test_no_motion_invulnerability_control_is_valid(self):
        identity = case_identity(self.identity, "INVULNERABLE")
        gate.validate_reports(identity, *native_reports(identity, False), False)

    def test_chain_falsifications_fail_even_with_well_formed_json(self):
        changes = (
            ("ownerChain", "motion", "manual", True), ("ownerChain", "motion", "originalTracker", False),
            ("ownerChain", "motion", "completed", False), ("ownerChain", "motion", "applied", vec(4, 5, 6)),
            ("ownerChain", "motion", "sendStartIndex", 3), ("ownerChain", "motion", "sendStartIndex", 1),
            ("ownerChain", "motion", "appliedIndex", 0), ("ownerChain", "positions", 0, "completed", False),
            ("ownerChain", "positions", 0, "motionOrdinal", 2), ("ownerChain", "positions", 0, "sentIndex", 0),
            ("ownerChain", "positions", 0, "acceptanceIndex", 2), ("ownerChain", "positions", 0, "acceptedPosition", vec(9, 9, 9)),
            ("ownerChain", "positions", 0, "ownerTick", 99), ("ownerChain", "events", 3, "kind", "server-correction-sent"),
            ("ownerChain", "events", 5, "after", "neutral", False), ("ownerChain", "events", 2, "after", "entity", 999),
            ("ownerChain", "events", 0, "data", "ordinal=2 manual=false forged"),
            ("ownerChain", "events", 1, "kind", "uncompleted-motion"),
            ("ownerChain", "events", 2, "data", "ordinal=99 forged"), ("packets", 0, "originalTracker", False),
            ("packets", 0, "ownerAcceptanceIndex", 2), ("packets", 0, "target", vec(99, 99, 99)),
            ("packets", 0, "body", vec(99, 99, 99)), ("packets", 0, "sha256", "f" * 64),
            ("clock", "observations", 3), ("clock", "tickTransitions", 2), ("clock", "maximumGapNanos", 2_000_000_001),
            ("correction", True), ("error", "passive failure"), ("identity", "ownerGeneration", 2),
            ("finalPosition", "x", float("nan")), ("packets", 0, "ordinal", True),
        )
        identity = case_identity(self.identity, "RIGHT")
        for change in changes:
            with self.subTest(change=change):
                host, peer = native_reports(identity)
                value = host
                for key in change[:-2]: value = value[key]
                value[change[-2]] = change[-1]
                with self.assertRaises(ValueError): gate.validate_reports(identity, host, peer, True)

    def test_peer_interpolation_and_encoded_packet_falsifications_fail(self):
        changes = (("interpolation", []), ("interpolation", 0, "packetOrdinal", 2),
                   ("interpolation", 0, "target", vec(99, 99, 99)), ("interpolation", 0, "after", vec(-1, 2, 3)),
                   ("packets", 0, "interpolating", False), ("packets", 0, "sha256", "f" * 64),
                   ("packets", 0, "target", vec(9, 9, 9)), ("packets", 0, "originalTracker", True),
                   ("finalPosition", vec(1.003, 2, 3)))
        identity = case_identity(self.identity, "RIGHT")
        for change in changes:
            with self.subTest(change=change):
                host, peer = native_reports(identity)
                value = peer
                for key in change[:-2]: value = value[key]
                value[change[-2]] = change[-1]
                with self.assertRaises(ValueError): gate.validate_reports(identity, host, peer, True)

    def test_peer_quantization_tolerance_is_bounded(self):
        identity = case_identity(self.identity, "RIGHT")
        host, peer = native_reports(identity)
        peer["packets"][0]["target"]["x"] += 1 / 4096
        peer["interpolation"][0]["target"] = peer["packets"][0]["target"]
        gate.validate_reports(identity, host, peer, True)
        peer["packets"][0]["target"]["x"] += .001
        with self.assertRaises(ValueError): gate.validate_reports(identity, host, peer, True)

    def test_native_time_resync_does_not_replace_packet_causality(self):
        identity = case_identity(self.identity, "RIGHT")
        host, peer = native_reports(identity)
        peer["clock"].update(lastTick=5, nativeTimeResyncs=1)
        peer["interpolation"][0]["tick"] = 5
        gate.validate_reports(identity, host, peer, True)

    def test_transparent_owner_may_apply_before_server_logs_send_completion(self):
        identity = case_identity(self.identity, "RIGHT")
        host, peer = native_reports(identity)
        events = host["ownerChain"]["events"]
        events[1], events[2] = events[2] | {"index": 1}, events[1] | {"index": 2}
        host["ownerChain"]["motion"]["appliedIndex"] = 1
        gate.validate_reports(identity, host, peer, True)
        self.assertEqual([event["kind"] for event in events[:3]],
                         ["server-motion-start", "client-motion-processed", "server-motion-sent"])

    def test_same_encoded_packets_in_wrong_order_are_rejected(self):
        identity = case_identity(self.identity, "RIGHT")
        host, peer = native_reports(identity)
        host["packets"].append(host["packets"][0] | {"ordinal": 2, "sha256": "d" * 64})
        peer["packets"].append(peer["packets"][0] | {"ordinal": 2, "sha256": "d" * 64})
        gate.validate_reports(identity, host, peer, True)
        peer["packets"][0]["sha256"], peer["packets"][1]["sha256"] = peer["packets"][1]["sha256"], peer["packets"][0]["sha256"]
        with self.assertRaises(ValueError): gate.validate_reports(identity, host, peer, True)


class WitnessTests(EvidenceFixture):
    def test_all_three_exact_case_rosters_and_sha_ledgers(self):
        for profile, count in (("transparent", 17), ("symmetric", 6), ("asymmetric", 6)):
            with self.subTest(profile=profile):
                for path in self.ipc.iterdir(): path.unlink()
                self.identity["profile"] = profile
                report = self.write_witnesses()
                ledger = gate.validate_witnesses(self.ipc, self.identity, self.pids, report)
                self.assertEqual(len(ledger), 9 + count * 8)

    def test_every_case_stage_is_mandatory_and_exact(self):
        report = self.write_witnesses()
        for suffix in ("arm.properties", "armed.properties", "ready.properties", "seen.properties", "passed.properties", "identity.json", "host.json", "peer.json"):
            path = self.ipc / ("case-00-" + suffix); saved = path.read_bytes(); path.unlink()
            with self.subTest(suffix=suffix), self.assertRaises(ValueError): gate.validate_witnesses(self.ipc, self.identity, self.pids, report)
            path.write_bytes(saved)

    def test_identity_pid_profile_role_and_hash_changes_are_rejected(self):
        report = self.write_witnesses()
        path = self.ipc / "case-00-armed.properties"; saved = path.read_text()
        for key, value in (("nonce", "stale"), ("profile", "asymmetric"), ("pid", "101"), ("role", "host"), ("identitySha256", "9" * 64), ("case", "LEFT")):
            with self.subTest(key=key):
                path.write_text("\n".join(key + "=" + value if line.startswith(key + "=") else line for line in saved.splitlines()))
                with self.assertRaises(ValueError): gate.validate_witnesses(self.ipc, self.identity, self.pids, report)
        path.write_text(saved)

    def test_extra_stale_case_and_unfinished_tmp_are_rejected(self):
        report = self.write_witnesses()
        for name in ("case-17-passed.properties", "case-00-host.tmp"):
            path = self.ipc / name; path.write_text("stale")
            with self.assertRaises(ValueError): gate.validate_witnesses(self.ipc, self.identity, self.pids, report)
            path.unlink()

    def test_native_connection_tuple_falsifications_are_rejected(self):
        original = self.write_witnesses()
        for key in ("clientLocal", "clientRemote", "backendLocal", "backendRemote"):
            for role in ("owner", "observer"):
                value = copy.deepcopy(original); value["connections"][role][key][1] += 100
                with self.subTest(key=key, role=role), self.assertRaises(ValueError): gate.validate_connections(self.ipc, self.identity, self.pids, value)

    def test_four_streams_cannot_be_empty_unfinished_corrupted_or_early(self):
        original = relay_report(self.identity, self.pids)
        for name in gate.relay.DIRECTION_NAMES:
            for key, value in (("readBytes", 0), ("writtenBytes", 9), ("writeSha256", "e" * 64), ("eof", False),
                               ("writeHalfClosed", False), ("queuedBytes", 1), ("offsetsContiguous", False), ("releaseLatenessMaxSeconds", .251)):
                report = copy.deepcopy(original); report["directions"][name][key] = value
                with self.subTest(name=name, key=key), self.assertRaises(ValueError): gate.validate_relay(report, self.identity, self.pids)
        for key, value in (("closed", False), ("failure", {"code": "reset"}), ("cleanupErrors", ["close"]), ("profile", "symmetric"), ("ownedSocketsHigh", 7)):
            report = copy.deepcopy(original); report[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError): gate.validate_relay(report, self.identity, self.pids)

    def test_all_evidence_and_logs_are_size_bounded_before_hashing(self):
        (self.base / "host.log").write_bytes(b"log")
        (self.base / "peer.log").write_bytes(b"log")
        with patch.object(gate, "MAX_EVIDENCE", 5), self.assertRaises(ValueError): gate.evidence_files(self.base)
        with patch.object(gate, "MAX_EVIDENCE", 6): self.assertEqual(gate.evidence_files(self.base)[0], 6)

    def test_json_duplicate_keys_nonfinite_values_oversize_and_links_fail(self):
        path = self.ipc / "input.json"
        for value in ('{"x":1,"x":2}', '{"x":NaN}', " " * (gate.MAX_JSON + 1)):
            path.write_text(value)
            with self.assertRaises(ValueError): gate.read_json(path)
        path.unlink(); path.symlink_to(self.base / "host.log")
        with self.assertRaises(ValueError): gate.read_json(path)
        with self.assertRaises(ValueError): gate.evidence_files(self.base)


class EvidenceScanTests(EvidenceFixture):
    @contextmanager
    def after_enumeration(self, action):
        """Run a real filesystem mutation after a walk captured IPC names."""
        walk, fwalk = os.walk, os.fwalk
        def scan(*args, **kwargs):
            for directory, children, names in walk(*args, **kwargs):
                if Path(directory) == self.ipc:
                    action(names)
                yield directory, children, names
        def pinned_scan(*args, **kwargs):
            for directory, children, names, descriptor in fwalk(*args, **kwargs):
                if self.base / directory == self.ipc:
                    action(names)
                yield directory, children, names, descriptor
        with patch.object(gate.os, "walk", scan), patch.object(gate.os, "fwalk", pinned_scan):
            yield

    def test_live_atomic_publication_is_counted_even_if_destination_was_not_enumerated(self):
        for filename in ("case-04-passed.properties", "case-00-host.json", "host-failure.properties"):
            with self.subTest(filename=filename):
                destination = self.ipc / filename; temporary = destination.with_suffix(".tmp")
                temporary.write_bytes(b"receipt")
                def publish(names):
                    self.assertIn(temporary.name, names)
                    self.assertNotIn(destination.name, names)
                    temporary.replace(destination)
                with self.after_enumeration(publish):
                    total, files = gate.evidence_files(self.base, active_profile="transparent")
                self.assertEqual(total, 7)
                self.assertEqual(files, {"ipc/" + filename: destination})
                self.assertEqual(gate.artifact_hashes(self.base), {"ipc/" + filename: gate.s.digest(destination)})
                destination.unlink()

    def test_snapshot_containing_both_rename_names_counts_published_bytes_once(self):
        destination = self.ipc / "case-00-host.json"; temporary = destination.with_suffix(".tmp")
        temporary.write_bytes(b"receipt")
        def publish(names):
            temporary.replace(destination)
            names.append(destination.name)
        with self.after_enumeration(publish), patch.object(gate, "MAX_EVIDENCE", 7):
            total, files = gate.evidence_files(self.base, active_profile="transparent")
        self.assertEqual((total, len(files)), (7, 1))

    def test_live_metadata_uses_one_nofollow_stat_even_if_publication_follows_it(self):
        destination = self.ipc / "case-00-host.json"; temporary = destination.with_suffix(".tmp")
        temporary.write_bytes(b"receipt")
        native_stat = os.stat
        def publish(path, *args, **kwargs):
            info = native_stat(path, *args, **kwargs)
            if path == temporary.name and kwargs.get("dir_fd") is not None:
                self.assertIs(kwargs.get("follow_symlinks"), False)
                temporary.replace(destination)
            return info
        with patch.object(gate.os, "stat", publish):
            self.assertEqual(gate.evidence_files(self.base, active_profile="transparent")[0], 7)
        self.assertEqual(gate.evidence_files(self.base)[1], {"ipc/case-00-host.json": destination})

    def test_live_parent_swaps_never_follow_outside_metadata_and_close_owned_descriptors(self):
        for parent_kind in ("ipc", "base"):
            for missing in (False, True):
                with self.subTest(parent=parent_kind, missing=missing), tempfile.TemporaryDirectory() as outside_name:
                    outside = Path(outside_name)
                    (outside / "case-00-host.json").write_bytes(b"outside must not be inspected")
                    temporary = self.ipc / "case-00-host.tmp"; temporary.write_bytes(b"receipt")
                    parent = self.ipc if parent_kind == "ipc" else self.base
                    parked = parent.with_name(parent.name + "-retired")
                    native_stat, native_open = os.stat, os.open
                    opened, swapped, outside_reads = [], [], []
                    outside_identity = (outside.stat().st_dev, outside.stat().st_ino)
                    def retain_open(*args, **kwargs):
                        descriptor = native_open(*args, **kwargs); opened.append(descriptor); return descriptor
                    def swap(path, *args, **kwargs):
                        descriptor = kwargs.get("dir_fd")
                        if descriptor is not None:
                            info = os.fstat(descriptor)
                            if (info.st_dev, info.st_ino) == outside_identity: outside_reads.append(path)
                        if path == temporary.name and descriptor is not None and not swapped:
                            info = native_stat(path, *args, **kwargs)
                            parent.rename(parked); parent.symlink_to(outside, target_is_directory=True); swapped.append(True)
                            if missing: raise FileNotFoundError(2, "published while parent changed", str(temporary))
                            return info
                        return native_stat(path, *args, **kwargs)
                    try:
                        with patch.object(gate.os, "stat", swap), patch.object(gate.os, "open", retain_open):
                            with self.assertRaisesRegex(ValueError, "Changed or linked live evidence directory"):
                                gate.evidence_files(self.base, active_profile="transparent")
                        self.assertTrue(swapped); self.assertEqual(outside_reads, [])
                        self.assertGreaterEqual(len(opened), 2)
                        self.assertLessEqual(len(opened), 4)
                        for descriptor in opened:
                            with self.assertRaises(OSError): os.fstat(descriptor)
                    finally:
                        if swapped: parent.unlink(); parked.rename(parent)
                        temporary.unlink()

    def test_live_walk_uses_retained_root_before_enumerating_names(self):
        with tempfile.TemporaryDirectory() as outside_name:
            outside = Path(outside_name); (outside / "foreign").write_bytes(b"outside")
            parked = self.base.with_name(self.base.name + "-retired")
            native_scandir = os.scandir; swapped, listed = [], []
            def swap_before_enumeration(directory):
                if not swapped:
                    self.assertIsInstance(directory, int)
                    self.base.rename(parked); self.base.symlink_to(outside, target_is_directory=True); swapped.append(True)
                stream = native_scandir(directory)
                class ObservedEntries:
                    def __iter__(self): return self
                    def __next__(self):
                        entry = next(stream); listed.append(entry.name); return entry
                    def __enter__(self): return self
                    def __exit__(self, *args): stream.close()
                    def close(self): stream.close()
                return ObservedEntries()
            try:
                with patch.object(gate.os, "scandir", swap_before_enumeration):
                    with self.assertRaisesRegex(ValueError, "Changed or linked live evidence directory"):
                        gate.evidence_files(self.base, active_profile="transparent")
                self.assertTrue(swapped); self.assertNotIn("foreign", listed)
            finally:
                if swapped: self.base.unlink(); parked.rename(self.base)

    def test_final_scan_rejects_even_a_recognized_atomic_publication(self):
        destination = self.ipc / "case-00-host.json"; temporary = destination.with_suffix(".tmp")
        temporary.write_bytes(b"receipt")
        with self.after_enumeration(lambda names: temporary.replace(destination)):
            with self.assertRaisesRegex(ValueError, "final evidence path .*case-00-host.tmp"):
                gate.artifact_hashes(self.base)

    def test_live_scan_requires_recognized_temporary_and_present_destination(self):
        for filename in ("case-00-host.tmp", "case-00-host.json", "unrecognized.tmp", "case-06-host.tmp"):
            with self.subTest(filename=filename):
                path = self.ipc / filename; path.write_bytes(b"receipt")
                with self.after_enumeration(lambda names: path.unlink()):
                    with self.assertRaisesRegex(ValueError, "live evidence path .*" + filename):
                        gate.evidence_files(self.base, active_profile="symmetric")

    def test_non_ipc_temporary_and_disappearing_log_are_never_ignored(self):
        fwalk = os.fwalk
        for filename in ("case-00-host.tmp", "host.log"):
            with self.subTest(filename=filename):
                path = self.base / filename; path.write_bytes(b"receipt")
                destination = path.with_suffix(".json")
                def scan(*args, **kwargs):
                    for row in fwalk(*args, **kwargs):
                        if self.base / row[0] == self.base:
                            path.replace(destination)
                        yield row
                with patch.object(gate.os, "fwalk", scan):
                    with self.assertRaisesRegex(ValueError, "live evidence path .*" + filename):
                        gate.evidence_files(self.base, active_profile="transparent")
                destination.unlink()

    def test_symlink_fifo_and_directory_replacements_fail_in_both_phases(self):
        for active_profile in (None, "transparent"):
            for kind in ("symlink", "fifo", "directory"):
                with self.subTest(active_profile=active_profile, kind=kind):
                    path = self.ipc / "case-00-host.tmp"; path.write_bytes(b"receipt")
                    def replace(names):
                        path.unlink()
                        if kind == "symlink": path.symlink_to(self.base / "absent")
                        elif kind == "fifo": os.mkfifo(path)
                        else: path.mkdir()
                    with self.after_enumeration(replace):
                        with self.assertRaisesRegex(ValueError, "(Linked|Non-regular) .* evidence path: .*case-00-host.tmp"):
                            gate.evidence_files(self.base, active_profile=active_profile)
                    if path.is_dir(): path.rmdir()
                    else: path.unlink()

    def test_renamed_destination_must_itself_be_regular_and_unlinked(self):
        destination = self.ipc / "case-00-host.json"; temporary = destination.with_suffix(".tmp")
        for kind in ("symlink", "fifo", "directory"):
            with self.subTest(kind=kind):
                temporary.write_bytes(b"receipt")
                def replace(names):
                    temporary.unlink()
                    if kind == "symlink": destination.symlink_to(self.base / "absent")
                    elif kind == "fifo": os.mkfifo(destination)
                    else: destination.mkdir()
                with self.after_enumeration(replace):
                    with self.assertRaisesRegex(ValueError, "(Linked|Non-regular) live evidence path: .*case-00-host.json"):
                        gate.evidence_files(self.base, active_profile="transparent")
                if destination.is_dir(): destination.rmdir()
                else: destination.unlink()

    def test_missing_or_linked_root_and_subdirectories_fail_in_both_phases(self):
        for active_profile in (None, "transparent"):
            with self.subTest(active_profile=active_profile):
                with self.assertRaisesRegex(ValueError, "evidence path .*absent"):
                    gate.evidence_files(self.base / "absent", active_profile=active_profile)
                linked = self.base / "linked"; linked.symlink_to(self.ipc, target_is_directory=True)
                with self.assertRaisesRegex(ValueError, "Linked .* evidence path: .*linked"):
                    gate.evidence_files(linked, active_profile=active_profile)
                with self.assertRaisesRegex(ValueError, "Linked .* evidence path: .*linked"):
                    gate.evidence_files(self.base, active_profile=active_profile)
                linked.unlink()

    def test_walk_errors_are_not_silently_ignored(self):
        def fail(base, *, followlinks, onerror):
            onerror(PermissionError(13, "denied", str(self.ipc)))
            return iter(())
        with patch.object(gate.os, "walk", fail):
            with self.assertRaisesRegex(ValueError, "Cannot walk final evidence path .*ipc"):
                gate.evidence_files(self.base)

    def test_temporary_and_published_bytes_keep_the_exact_16_mib_cap(self):
        path = self.ipc / "case-00-host.tmp"
        for active_profile in (None, "transparent"):
            with self.subTest(active_profile=active_profile):
                with path.open("wb") as stream: stream.truncate(16 * 1024 * 1024)
                self.assertEqual(gate.evidence_files(self.base, active_profile=active_profile)[0], 16 * 1024 * 1024)
                with path.open("ab") as stream: stream.write(b"x")
                with self.assertRaisesRegex(ValueError, "exceeded 16 MiB: .*case-00-host.tmp"):
                    gate.evidence_files(self.base, active_profile=active_profile)
        destination = path.with_suffix(".json")
        with self.after_enumeration(lambda names: path.replace(destination)):
            with self.assertRaisesRegex(ValueError, "exceeded 16 MiB: .*case-00-host.json"):
                gate.evidence_files(self.base, active_profile="transparent")

    def test_live_and_final_file_counts_remain_bounded(self):
        for index in range(1024): (self.ipc / str(index)).touch()
        for active_profile in (None, "transparent"):
            with self.subTest(active_profile=active_profile):
                self.assertEqual(len(gate.evidence_files(self.base, active_profile=active_profile)[1]), 1024)
                extra = self.ipc / "extra"; extra.touch()
                with self.assertRaisesRegex(ValueError, "Unbounded .* evidence file count: "):
                    gate.evidence_files(self.base, active_profile=active_profile)
                extra.unlink()


class FakeClock:
    def __init__(self): self.now = 0.0
    def __call__(self): return self.now
    def sleep(self, seconds): self.now += seconds


class FakeProcess:
    def __init__(self, pid, status=None): self.pid, self.returncode, self.terminated, self.killed = pid, status, False, False
    def poll(self): return self.returncode
    def terminate(self): self.terminated = True; self.returncode = -15
    def kill(self): self.killed = True; self.returncode = -9
    def wait(self, timeout): return self.returncode


class FakeRelay:
    def __init__(self, identity, profile, *, listeners):
        self.identity, self.profile, self.listeners = identity, profile, listeners
        self.endpoints = {"owner": ("127.0.0.1", 33001), "observer": ("127.0.0.1", 33002)}
        self.complete, self.closed, self.calls = False, False, []
    def pump(self, timeout): self.calls.append(timeout); return self.complete
    def close(self): self.closed = True
    def assert_complete(self):
        if not self.complete: raise ValueError("not drained")
    def report(self): return {"closed": self.closed, "status": "failed"}


class LifecycleTests(EvidenceFixture):
    def drive(self, processes, factory=FakeRelay, seconds=.1):
        clock = FakeClock()
        return gate.supervise(self.base, self.ipc, self.identity, list(zip(("host", "peer"), processes)), {}, seconds,
                              clock=clock, sleeper=clock.sleep, relay_factory=factory)

    def test_deadline_before_ready_fails_without_creating_backend(self):
        factory = unittest.mock.Mock()
        with self.assertRaisesRegex(ValueError, "deadline"): self.drive([FakeProcess(100), FakeProcess(200)], factory)
        factory.assert_not_called()

    def test_supervision_survives_atomic_publication_but_still_enforces_deadline(self):
        temporary = self.ipc / "case-00-host.tmp"; temporary.write_bytes(b"receipt")
        fwalk = os.fwalk
        def publish(*args, **kwargs):
            for row in fwalk(*args, **kwargs):
                if self.base / row[0] == self.ipc and temporary.name in row[2]:
                    temporary.replace(temporary.with_suffix(".json"))
                yield row
        with patch.object(gate.os, "fwalk", publish):
            with self.assertRaisesRegex(ValueError, "elapsed deadline expired"):
                self.drive([FakeProcess(100), FakeProcess(200)])

    def test_host_must_remain_owned_and_alive_at_ready(self):
        self.properties("server-ready.properties", "host", {"port": "33003"})
        for process in (FakeProcess(100, 0), FakeProcess(100, 2)):
            factory = unittest.mock.Mock()
            with self.assertRaises(ValueError): self.drive([process, FakeProcess(200)], factory)
            factory.assert_not_called()

    def test_wrong_ready_identity_cannot_create_backend(self):
        self.properties("server-ready.properties", "host", {"port": "33003", "nonce": "foreign"})
        factory = unittest.mock.Mock()
        with self.assertRaises(ValueError): self.drive([FakeProcess(100), FakeProcess(200)], factory)
        factory.assert_not_called()

    def test_normal_exit_needs_complete_witnesses_and_drained_relay(self):
        self.properties("server-ready.properties", "host", {"port": "33003"})
        processes = [FakeProcess(100), FakeProcess(200)]; made = []
        def factory(*args, **kwargs):
            current = FakeRelay(*args, **kwargs); made.append(current)
            def pump(timeout):
                current.calls.append(timeout)
                for role, process in zip(("host", "peer"), processes):
                    process.returncode = 0; self.properties(role + "-stone-hinge-passed.properties", role)
                current.complete = True
            current.pump = pump
            return current
        result = self.drive(processes, factory)
        self.assertIs(result, made[0]); self.assertTrue(result.complete)
        self.assertTrue(all(0 <= timeout <= .01 for timeout in result.calls))
        witness = gate.s.read_properties(self.ipc / "relay-ready.properties")
        self.assertEqual(witness["pid"], str(os.getpid())); self.assertEqual(witness["hostPid"], "100")

    def test_failure_after_relay_start_closes_and_retains_failed_report(self):
        self.properties("server-ready.properties", "host", {"port": "33003"})
        processes = [FakeProcess(100), FakeProcess(200)]; made = []
        def factory(*args, **kwargs):
            current = FakeRelay(*args, **kwargs); made.append(current)
            def pump(timeout): processes[1].returncode = 7
            current.pump = pump; return current
        with self.assertRaisesRegex(ValueError, "exit=7"): self.drive(processes, factory)
        self.assertTrue(made[0].closed)
        self.assertEqual(gate.read_json(self.base / "relay.json")["status"], "failed")

    def test_failure_witness_stops_process_loop(self):
        self.properties("peer-failure.properties", "peer", {"error": "failure"})
        with self.assertRaisesRegex(ValueError, "failure witness"): self.drive([FakeProcess(100), FakeProcess(200)])

    def test_second_spawn_failure_cleans_only_owned_first_process(self):
        process = FakeProcess(100)
        sockets = [types.SimpleNamespace(getsockname=lambda: ("127.0.0.1", port), close=unittest.mock.Mock()) for port in (33001, 33002)]
        # Bind lambda values explicitly so the two retained fake endpoints stay distinct.
        sockets[0].getsockname = lambda: ("127.0.0.1", 33001)
        source = {"runId": None, "runAttempt": None}
        launch = {key: self.identity[key] for key in ("sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256")}
        launch.update(java="/never/java", runtimeSha256={})
        target = self.base / "transparent"
        with patch.object(gate.relay, "reserve_listeners", return_value=dict(zip(("owner", "observer"), sockets))), \
             patch.object(gate.s, "command", return_value=["fixture"]), patch.object(gate.s, "environment", return_value={}), \
             patch.object(gate.subprocess, "Popen", side_effect=[process, OSError("fake spawn failure")]):
            with self.assertRaisesRegex(OSError, "fake spawn"):
                gate.run_profile(self.base, target, self.base / "work", launch, "a" * 64, "transparent", source, b"eula=true", gate.time.monotonic() + 300, set())
        self.assertTrue(process.terminated)
        self.assertTrue(all(sock.close.called for sock in sockets))
        result = gate.read_json(target / "result.json")
        self.assertEqual(result["status"], "failed"); self.assertEqual(set(result["processes"]), {"host"})

    def test_signal_cleanup_restores_previous_handlers(self):
        before = {sig: signal.getsignal(sig) for sig in (signal.SIGALRM, signal.SIGINT, signal.SIGTERM)}
        with gate.cleanup_signals():
            self.assertTrue(all(signal.getsignal(sig) == signal.SIG_IGN for sig in before))
        self.assertEqual(before, {sig: signal.getsignal(sig) for sig in before})

    def test_nested_deadline_restores_remaining_outer_budget_after_exception(self):
        with patch.object(gate.signal, "getitimer", return_value=(100, 0)), \
             patch.object(gate.signal, "setitimer") as timer, patch.object(gate.time, "monotonic", side_effect=[10, 15]):
            with self.assertRaisesRegex(RuntimeError, "stage failed"):
                with gate.stage_guard(30): raise RuntimeError("stage failed")
        self.assertEqual(timer.call_args_list, [unittest.mock.call(signal.ITIMER_REAL, 20), unittest.mock.call(signal.ITIMER_REAL, 95)])

    def test_expired_outer_deadline_is_rearmed_after_masked_cleanup(self):
        with patch.object(gate.signal, "getitimer", return_value=(5, 0)), \
             patch.object(gate.signal, "setitimer") as timer, patch.object(gate.time, "monotonic", side_effect=[10, 20]):
            with gate.stage_guard(30): pass
        self.assertEqual(timer.call_args_list[-1], unittest.mock.call(signal.ITIMER_REAL, .000001))


class FinalResultTests(EvidenceFixture):
    def setUp(self):
        super().setUp()
        profile = self.base / "transparent"; profile.mkdir()
        self.ipc.rmdir(); self.base = profile; self.ipc = profile / "ipc"; self.ipc.mkdir()
        self.source = {"sourceHead": self.identity["sourceHead"], "checkoutSha": self.identity["checkoutSha"], "prHeadSha": "",
                       "runId": None, "runAttempt": None, "job": None, "repository": None, "eventName": None}
        self.launch = {key: self.identity[key] for key in ("sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256")}
        self.launch["runtimeSha256"] = {"/fixture/java": "9" * 64}
        self.launch_hash = "8" * 64
        relay_value = self.write_witnesses()
        gate.matrix.write_report(self.base / "relay.json", relay_value)
        (self.base / "host.log").write_text("retained host log\n")
        (self.base / "peer.log").write_text("retained peer log\n")
        self.result = {"schemaVersion": 1, "status": "passed", **self.identity, **gate.FLAGS,
                       "cases": gate.roster("transparent"), "timeoutSeconds": 300, "heapMiBPerJvm": 2048, "javaVersion": 25,
                       "launcherPid": os.getpid(), "launchSha256": self.launch_hash, "runtimeSha256": self.launch["runtimeSha256"],
                       "provenance": self.source, "cleanupErrors": [], "seconds": 2,
                       "processes": {role: {"pid": self.pids[role], "exit": 0} for role in ("host", "peer")}}
        gate.matrix.write_report(self.base / "launch-proof.json", self.result | {"status": "started"})
        self.result["witnesses"] = gate.validate_witnesses(self.ipc, self.identity, self.pids, relay_value)
        self.result["artifacts"] = gate.artifact_hashes(self.base)
        gate.matrix.write_report(self.base / "result.json", self.result)

    def validate(self, seen=None):
        gate.validate_profile(self.base, self.result, self.launch, self.launch_hash, self.source, set() if seen is None else seen)

    def test_full_frozen_profile_validates_and_reused_nonce_fails(self):
        seen = set(); self.validate(seen)
        self.assertEqual(seen, {self.identity["nonce"]})
        with self.assertRaises(ValueError): self.validate(seen)

    def test_late_log_or_native_witness_mutation_cannot_preserve_success(self):
        for path in (self.base / "host.log", self.ipc / "case-00-host.json"):
            original = path.read_bytes(); path.write_bytes(original + b" ")
            with self.subTest(path=path.name), self.assertRaises(ValueError): self.validate()
            path.write_bytes(original)

    def test_final_missing_log_or_native_witness_cannot_preserve_success(self):
        for path in (self.base / "host.log", self.ipc / "case-00-host.json"):
            original = path.read_bytes(); path.unlink()
            with self.subTest(path=path.name), self.assertRaises(ValueError): self.validate()
            path.write_bytes(original)

    def test_passing_report_cannot_claim_failed_cleanup_or_nonzero_exit(self):
        original = copy.deepcopy(self.result)
        mutations = (("cleanupErrors", ["unreaped peer"]), ("processes", "peer", "exit", 1),
                     ("gameplayEnabled", True), ("heapMiBPerJvm", 4096), ("seconds", 301),
                     ("descriptorSha256", "7" * 64), ("runtimeSha256", {"/fixture/java": "7" * 64}))
        for mutation in mutations:
            self.result = copy.deepcopy(original)
            value = self.result
            for key in mutation[:-2]: value = value[key]
            value[mutation[-2]] = mutation[-1]
            gate.matrix.write_report(self.base / "result.json", self.result)
            with self.subTest(mutation=mutation), self.assertRaises(ValueError): self.validate()


class LaunchTests(LaunchFixture):
    def setUp(self):
        super().setUp()
        self.contract = json.loads((gate.ROOT / gate.CONTRACT).read_text())
        (self.root / gate.CONTRACT).write_text(json.dumps(self.contract))
        self.descriptor.write_text(json.dumps({"entrypoints": {"fabric-client-gametest": [gate.ENTRYPOINT]}, "mixins": list(gate.MIXINS)}))
        (self.descriptor.parent / Path(gate.CONTRACT).name).write_text(json.dumps(self.contract))
        for name in gate.MIXINS:
            value = (gate.ROOT / "src/gametest/resources" / name).read_bytes()
            (self.root / "src/gametest/resources" / name).write_bytes(value)
            (self.descriptor.parent / name).write_bytes(value)
        subprocess.run(["git", "-C", str(self.root), "add", "src"], check=True)
        subprocess.run(["git", "-C", str(self.root), "-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "-qm", "stone fixture"], check=True)
        head = gate.s.source_head(self.root)
        self.launch.update(suite=gate.SUITE, sourceHead=head, checkoutSha=head, javaVersion=25, maxTimeoutSeconds=300,
                           descriptorSha256=gate.s.digest(self.descriptor), contract=gate.CONTRACT, contractSha256=gate.s.digest(self.root / gate.CONTRACT))
        self.launch["runtimeSha256"] = {name: gate.s.digest(name) for name in self.launch["runtimeSha256"]}
        self.launch_path.write_text(json.dumps(self.launch))
        java_patch = patch.object(gate.s, "configured_java", return_value=self.java)
        java_patch.start(); self.addCleanup(java_patch.stop)

    def test_exact_stone_launch_reuses_runtime_command_validator(self):
        self.assertEqual(gate.validate_launch(self.launch, self.launch_path, self.root), gate.s.digest(self.launch_path))

    def test_java_version_timeout_mixin_and_runtime_tampering_fail(self):
        for key, value in (("javaVersion", 24), ("maxTimeoutSeconds", 301), ("suite", "moon"), ("main", "OtherMain")):
            launch = copy.deepcopy(self.launch); launch[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError): gate.validate_launch(launch, self.launch_path, self.root)
        self.java.write_text("changed executable")
        with self.assertRaises(ValueError): gate.validate_launch(self.launch, self.launch_path, self.root)

    def test_command_forces_two_gib_offline_identity_and_explicit_profile(self):
        identity = {"profile": "asymmetric", "nonce": "abc"}
        result = gate.s.command(self.launch, "host", self.root / "build/game", self.root / "build/ipc", identity, 300, gate.selection("asymmetric"))
        self.assertIn("-Xmx2G", result); self.assertNotIn("-Xmx8G", result)
        self.assertIn("-Dwildercord.mp.profile=asymmetric", result)
        self.assertEqual(result[result.index("--accessToken") + 1], "0")

    def test_only_existing_diagnostic_ci_job_and_exact_checkout_are_accepted(self):
        event = self.root / "build/event.json"
        event.write_text(json.dumps({"number": 42, "pull_request": {"head": {"sha": self.launch["sourceHead"]}}}))
        env = {"GITHUB_ACTIONS": "true", "GITHUB_SHA": "a" * 40, "GITHUB_RUN_ID": "12", "GITHUB_RUN_ATTEMPT": "2",
               "GITHUB_JOB": "native-diagnostic", "GITHUB_REPOSITORY": "owner/repo", "GITHUB_EVENT_NAME": "pull_request",
               "NATIVE_DIAGNOSTIC_HEAD_SHA": self.launch["sourceHead"], "WILDERCORD_PR_HEAD_SHA": self.launch["sourceHead"],
               "NATIVE_DIAGNOSTIC_PR": "42", "GITHUB_EVENT_PATH": str(event)}
        result = gate.provenance(self.root, env)
        self.assertEqual(result["job"], "native-diagnostic")
        self.assertEqual(result["workflowSha"], "a" * 40)
        self.assertNotEqual(result["workflowSha"], result["checkoutSha"])
        for key, value in (("GITHUB_JOB", "new-job"), ("NATIVE_DIAGNOSTIC_HEAD_SHA", "a" * 40),
                           ("WILDERCORD_PR_HEAD_SHA", ""), ("GITHUB_SHA", "malformed"), ("GITHUB_EVENT_NAME", "workflow_dispatch"),
                           ("NATIVE_DIAGNOSTIC_PR", "43"), ("GITHUB_RUN_ATTEMPT", "0")):
            with self.subTest(key=key), self.assertRaises(ValueError): gate.provenance(self.root, env | {key: value})
        event.write_text(json.dumps({"number": 42, "pull_request": {"head": {"sha": "f" * 40}}}))
        with self.assertRaises(ValueError): gate.provenance(self.root, env)

    def test_changed_contract_cannot_drop_cases_or_change_delays(self):
        for key in ("profiles", "limits", "gameplayEnabled"):
            value = copy.deepcopy(self.contract)
            if key == "profiles": value[key][0]["cases"].pop()
            elif key == "limits": value[key]["maxProfileSeconds"] = 301
            else: value[key] = True
            (self.root / gate.CONTRACT).write_text(json.dumps(value))
            with self.subTest(key=key), self.assertRaises(ValueError): gate.load_contract(self.root)

    def test_cli_has_no_arbitrary_command_profile_case_or_suite_selection(self):
        for args in (("--profile", "transparent"), ("--suite", "moon"), ("--case", "RIGHT"), ("--command", "anything"), ("--total-timeout", "1201")):
            with self.subTest(args=args), patch("sys.stderr"), self.assertRaises(SystemExit): gate.parse_args(args)
        self.assertEqual(gate.parse_args([]).total_timeout, 1200)


if __name__ == "__main__":
    unittest.main()
