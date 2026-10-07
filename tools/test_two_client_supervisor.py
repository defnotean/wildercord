"""Bounded supervisor regression tests; no Minecraft process is started."""
import ast
import contextlib
import copy
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import types
import unittest
from unittest.mock import patch
import uuid
from PIL import Image
from verify_articulated_render_receipts import image_evidence

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("supervisor", ROOT / "tools/native/launch_two_clients.py")
supervisor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(supervisor)
CONTRACT = json.loads((ROOT / supervisor.CONTRACT).read_text())
ORIGINAL_CASES = [
    spell + "_" + case
    for spell in ("BREAK_CAST", "DRIVING_CUT")
    for case in ("HEALTH", "FULL_ABSORPTION", "MANA_SKIN", "REVERSAL", "TOTEM", "GUARD", "STEP", "WARD",
                 "RESISTANCE", "REJECTED", "REPLACED", "EQUAL_TOKEN", "NEW_CHARGE", "IDLE", "WINDUP_REPLACEMENT")
] + ["SHARED_BREAK_CAST_TO_DRIVING_CUT", "SHARED_DRIVING_CUT_TO_BREAK_CAST", "IDLE_SEAL_RECOVERY",
     "CHARGED_SEAL_RECOVERY", "NONPLAYER"]
COUNTER_CASES = ["COUNTER_QUIETUS_PAID_BOLT", "COUNTER_QUIETUS_THIRTEEN_EXISTING", "COUNTER_REFLECTED_RESPAWN_NULLCATCH"]
PRESERVED_CASES = ORIGINAL_CASES + COUNTER_CASES
SPECTATOR_CASES = ["SPECTATOR_BOLT_VENOM", "SPECTATOR_SPARK_VENOM", "SPECTATOR_RAY_VENOM", "SPECTATOR_TOUCH_VENOM"]
PRIOR_CASES = PRESERVED_CASES + SPECTATOR_CASES
LIFE_CASES = ["LIFE_SELF_HEAL_FIRST_PERSON", "LIFE_SELF_HEAL_THIRD_PERSON", "LIFE_SELF_HEAL_CAMERA_TRANSITIONS", "LIFE_SELF_SECOND_WIND_REDUCED_FLASH"]
ORIGINAL_46_CASES = PRIOR_CASES + LIFE_CASES
COUNTER_VISUAL_CASES = [
    "COUNTER_UNMOVED_CLASSIC_RIGHT", "COUNTER_UNMOVED_CLASSIC_LEFT",
    "COUNTER_UNMOVED_ARTICULATED_RIGHT", "COUNTER_UNMOVED_ARTICULATED_LEFT",
    "COUNTER_NULL_PARRY_CLASSIC_RIGHT", "COUNTER_NULL_PARRY_CLASSIC_LEFT",
    "COUNTER_NULL_PARRY_ARTICULATED_RIGHT", "COUNTER_NULL_PARRY_ARTICULATED_LEFT",
]
EXPECTED_CASES = ORIGINAL_46_CASES + COUNTER_VISUAL_CASES


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data))


def write_life_witnesses(ipc, identity, pids):
    """Synthetic validator inputs only; never launch Minecraft or claim native acceptance."""
    for index, case in enumerate(LIFE_CASES, 42):
        role_fields = {"lifeSource": identity["hostUuid"], "lifeViewerUuid": identity["peerUuid"],
                       "lifeOwnerEntity": "1", "lifeViewerEntity": "2"}
        full, minimal = (3, 3) if case == LIFE_CASES[-1] else (11, 7)
        own = 0 if case == LIFE_CASES[0] else full
        support = "1" if case == LIFE_CASES[-1] else "0"
        ready = {"actorEntity": "1", "recipientEntity": "2", "caseLife": case, "lifeMoment": "APPLY",
                 "lifeSource": identity["hostUuid"], "lifeOwnerSource": identity["hostUuid"],
                 "lifeFullPieces": str(full), "lifeMinimalPieces": str(minimal), "lifeOwnerPieces": str(own),
                 "lifeOwnerUnique": str(own), "lifeOwnerExtracted": str(own), "lifeOwnerCameraNative": "true",
                 "lifeOwnerTime": support, "lifeOwnerReducedSamples": support}
        seen = {"actorEntity": "1", "recipientEntity": "2", "lifePeerSource": identity["hostUuid"],
                "lifePeerPieces": str(minimal), "lifePeerUnique": str(minimal), "lifePeerExtracted": str(minimal),
                "lifePeerCameraNative": "true", "lifePeerTime": support, "lifePeerReducedSamples": support}
        phases = (("prepare", "host", role_fields), ("armed", "peer", {**role_fields, "lifeCameraNative": "true"}),
                  ("captured", "peer", {**role_fields, "lifeCameraNative": "true", "lifeCapture": "resident"}),
                  ("ready", "host", ready), ("seen", "peer", seen), ("passed", "host", {}))
        for phase, role, fields in phases:
            values = {**identity, "role": role, "pid": str(pids[role]), "case": case, **fields}
            (ipc / f"case-{index:02d}-{phase}.properties").write_text("\n".join(key + "=" + value for key, value in values.items()) + "\n")


class SyntheticCounterEvidence:
    """Tiny generated validator inputs, not screenshots from native JVMs or visual acceptance."""
    def __init__(self, ipc, identity, pids, skin="wide"):
        self.ipc, self.identity, self.pids = ipc, identity, pids
        self.values, self.records = {}, {}
        for index, case in enumerate(COUNTER_VISUAL_CASES, 46):
            tick = 100 + (index - 46) * 200
            action = {**supervisor.counter_case(case), "actorEntity": "11", "observerEntity": "22",
                      "actorUuid": identity["hostUuid"], "observerUuid": identity["peerUuid"]}
            action = {key: str(value) for key, value in action.items()}
            common = {**action, "case": case, "skin": skin, "cameraNative": "true"}
            self.write(f"case-{index:02d}-prepare", "host", {**common, "skin": skin, "cameraNative": "true"})
            self.write(f"case-{index:02d}-armed", "peer", {**common, "skin": skin, "cameraNative": "true"})
            self.write(f"case-{index:02d}-clock-initial", "peer", {**common, "clockClientTick": str(tick - 10)})
            clock = {**common, "clockRendezvousTick": str(tick - 5), "clockInitialServerTick": str(tick - 5), "clockInitialSha256": supervisor.digest(ipc / f"case-{index:02d}-clock-initial.properties")}
            self.write(f"case-{index:02d}-clock-rendezvous", "host", {**clock, "clockServerTick": str(tick - 5)})
            clock["clockRendezvousSha256"] = supervisor.digest(ipc / f"case-{index:02d}-clock-rendezvous.properties")
            self.write(f"case-{index:02d}-clock-ack", "peer", {**clock, "clockClientTick": str(tick - 5)})
            self.write(f"case-{index:02d}-clock-ready", "host", {**clock, "clockServerTick": str(tick - 5), "clockClientTick": str(tick - 5),
                       "clockAckSha256": supervisor.digest(ipc / f"case-{index:02d}-clock-ack.properties")})
            self.values[f"case-{index:02d}-armed"]["clockReadySha256"] = supervisor.digest(ipc / f"case-{index:02d}-clock-ready.properties")
            self.flush(f"case-{index:02d}-armed")
            generation = identity["nonce"] + f":case-{index:02d}"
            admission = {
                "admissionSchemaVersion": "1", "admissionGeneration": generation,
                "admissionPeerArmedSha256": supervisor.digest(ipc / f"case-{index:02d}-armed.properties"),
                "admissionArt": "unmoved" if action["move"] == "24" else "null_parry",
                "admissionSentMarks": "[35]", "admissionRequestMarks": "[35]", "admissionPerformMarks": "[39]",
                "admissionServerBodyUuid": identity["hostUuid"], "admissionServerBodyEntity": "11", "admissionServerLevel": "minecraft:overworld",
                "admissionServerConnectionIdentity": "1111", "admissionClientBodyUuid": identity["hostUuid"],
                "admissionClientBodyEntity": "11", "admissionClientLevel": "minecraft:overworld", "admissionClientConnectionIdentity": "2222",
                "admissionBusy": "false", "admissionExciseBlocking": "false", "admissionPerformed": "true", "admissionCheck": "ACCEPTED",
                "admissionRefusal": "NONE", "admissionObserverFailure": "NONE", "admissionNativeFailure": "NONE",
                "admissionTrace": f"generation={generation}, sends=1, requests=1, check=ACCEPTED, refusal=NONE, payments=1, sources=1, returns=1, committed=true",
                "admissionArmedTick": str(tick - 1), "admissionSentTick": str(tick), "admissionRequestTick": str(tick), "admissionReturnTick": str(tick),
                "admissionPaidTick": str(tick), "admissionPaid": "8.0", "admissionCost": "8.0", "admissionAuraBefore": "19.0",
                "admissionAuraAfter": "11.0", "admissionSpendLeft": "11.0", "admissionRest": str(tick + 80), "admissionBacklash": "false",
                "admissionOriginalBody": "true", "admissionPendingAtEntry": "NONE", "admissionReturnCommitted": "true",
                "admissionActionIdentity": str(1000+index), "admissionSourceActionIdentity": str(1000+index), "admissionReturnActionIdentity": str(1000+index),
                "admissionSourceEntity": "11", "admissionSourceMove": action["move"], "admissionSourceStartTick": str(tick),
                "admissionSourceWindup": action["windup"], "admissionSourceRecovery": action["recovery"], "admissionSourceYaw": "0.0", "admissionSourcePitch": "0.0",
            }
            self.write(f"case-{index:02d}-admission", "host", {**common, **admission})
            admission_sha = supervisor.digest(ipc / f"case-{index:02d}-admission.properties")
            self.write(f"case-{index:02d}-accepted", "host", {**common, **admission, "admissionReceiptSha256": admission_sha, "acceptedTick": str(tick), "caughtTick": str(tick - 1), "payments": "1", "paid": "8.0", "restUntil": str(tick + 80),
                       "clockRendezvousTick": str(tick - 5), "clockReadySha256": supervisor.digest(ipc / f"case-{index:02d}-clock-ready.properties"),
                       "caughtAttackerUuid": "00000000-0000-4000-8000-000000000003", "armedSha256": supervisor.digest(ipc / f"case-{index:02d}-armed.properties")})
            for phase, role in (("ready", "host"), ("seen", "peer"), ("passed", "host")):
                self.write(f"case-{index:02d}-{phase}", role, {**common, "releaseTick": str(tick + int(action["windup"])),
                           "payments": "1", "completions": "1", "restUntil": str(tick + 80), "counterTargetUuid": "00000000-0000-4000-8000-000000000003",
                           "counterTargetEntity": "33", "counterTargetHealthBefore": "200.0", "counterTargetHealth": "185.0",
                           "directPrimaryHits": "1", "primaryHitTick": str(tick + int(action["windup"]))})
            accepted_sha = supervisor.digest(ipc / f"case-{index:02d}-accepted.properties")
            w, r, move = (int(action[key]) for key in ("windup", "recovery", "move"))
            ages = (w / 2, w + .25, w + 1.25, w + r / 2 + .25)
            for role in ("host", "peer"):
                game = ipc.parent / role; game.mkdir(exist_ok=True)
                for phase, age in zip(supervisor.COUNTER_PHASES, ages):
                    key = f"{role}-case-{index:02d}-{phase}-observed"
                    name = "counter_peer_" + ("unmoved" if move == 24 else "null_parry") + "_" + action["mode"] + "_" + action["hand"].lower() + "_" + role + "_" + phase.lower()
                    png = game / (name + ".png")
                    Image.new("RGBA", (2, 2), (index * 4, int(age * 8), 100 if role == "host" else 200, 255)).save(png)
                    data = png.read_bytes(); pixels = image_evidence(data)
                    frame = {"activation": tick, "move": move, "left": action["hand"] == "LEFT", "master": False, "yaw": 0., "pitch": 0., "tilt": 0.,
                             "footwork": False, "velocity": "n/a", "pose": "a" * 64}
                    art = action["mode"] == "articulated"
                    palette = {"activation": tick, "move": move, "phase": phase, "age": age, "classic": frame}
                    if art:
                        palette["articulated"] = {**frame, "velocity": "NaN", "pose": ("RECOVERY" if phase == "FOLLOW" else phase) + ":" + "b" * 64 + ":" + "c" * 64}
                    material = 5 if art or role == "peer" else 0
                    binding = {"source": 1, "state": 2, "model": 3, "item": 4, "skinMaterial": material, "owner": 11, "uuid": identity["hostUuid"],
                               "skin": skin.upper(), "texture": "minecraft:skin", "hand": action["hand"], "palette": palette, "matched": True}
                    kinds = (["body_submit", "body", "world_item"] if role == "peer" else ["view_submit", "view_deferred", "view_item"] if art else ["classic_transform", "native_item"])
                    shapes = {"classic_transform": ("observed_classic_before_after", 16), "native_item": ("actual_native_item_matrix", 16),
                              "view_submit": ("view_root_matrix", 16), "view_deferred": ("deferred_view_locals", 180), "view_item": ("actual_socket_item_matrix", 16),
                              "body_submit": ("world_root_identity", 17), "body": ("deferred_body_locals_and_retained_outer_root", 197 if art else 71),
                              "world_item": ("actual_world_socket_item_matrix", 16)}
                    passes = []
                    for kind in kinds:
                        operation, count = shapes[kind]
                        passes.append({"kind": kind, "binding": {**copy.deepcopy(binding), "item": 4 if role == "peer" or "item" in kind else 0},
                                       "geometry": {"operation": operation, "expected": [0.] * count, "actual": [0.] * count, "maxError": 0., "matched": True}, "segmented": art, "rigid": not art})
                    side = -1 if action["hand"] == "LEFT" else 1
                    draw = {"binding": copy.deepcopy(passes[-1]["binding"]), "geometry": {"operation":"actual_displayed_item_matrix", "expected":[0.]*16, "actual":[0.]*16, "maxError":0., "matched":True},
                            "displayContext": ("FIRST_PERSON_" if role == "host" else "THIRD_PERSON_") + action["hand"] + "_HAND", "quads": 12,
                            "anchorKind":"stock_diamond_hilt", "anchorExpected":[0.,0.,0.], "anchorActual":[0.,0.,0.],
                            "displayRotation":[0.,-90.*side,(25. if role=="host" else 55.)*side],
                            "displayTranslation":[1.13/16,3.2/16,1.13/16] if role=="host" else [0.,4./16,.5/16],
                            "displayScale":[.68 if role=="host" else .85]*3,
                            "displayLocal":[1.,0.,0.,0.,0.,1.,0.,0.,0.,0.,1.,0.,0.,0.,0.,1.],
                            "emittedQuadPositions":[0.] * 144,"emittedQuadSha256":hashlib.sha256(bytes(144*4)).hexdigest(),"stackIdentity":6,"collectorIdentity":7,"entryStackIdentity":6,"entryCollectorIdentity":7}
                    observation = {"owner": identity["hostUuid"], "observerUuid": identity["hostUuid"] if role == "host" else identity["peerUuid"],
                                   "observerEntity": "11" if role == "host" else "22", "observerCoverage": "false" if role == "host" else "true", "pairedRole": role,
                                   "sourceHead": identity["sourceHead"], "runIdentity": identity["runIdentity"], "acceptedReceiptSha256": accepted_sha,
                                   "acceptedSourceIdentity":str(index*100+5), "acceptedReadSequence": str(index*100+6), "sourceFrameSequence": str(index*100+7), "actualSourceAge": str(age), "connectedSkinTexture": "minecraft:skin", "connectedSkinModel": skin.upper(),
                                   "originalSkinMaterialIdentity": str(material), "submittedSkinMaterialIdentity": str(material), "ordinaryHandAdmission": "true", "handEquipKnown": "true",
                                   "handSameItem": "true", "handEquipping": "false", "worldHandEligible": "true"}
                    observation.update({"receivedSource" + key: admission["admissionSource" + key] for key in supervisor.COUNTER_SOURCE_FIELDS})
                    observation.update(admissionReceiptSha256=admission_sha, admissionGeneration=generation,
                                       receivedOwnerUuid=identity["hostUuid"], receivedOwnerEntity="11", receivedLevel="minecraft:overworld",
                                       receivedConnectionIdentity="2222" if role == "host" else "3333", tick=str(tick + int(age)))
                    record = {"schemaVersion": 1, "launchNonce": identity["nonce"], "verified": True, "failures": [], "scopeCleanupVerified": True,
                              "acceptanceObservedBeforeSource": True, "phaseBasis": supervisor.COUNTER_PHASE_BASIS, "pixelQualityReviewed": False, "serverReleaseFrameCorrespondenceVerified": False,
                              "expected": {"name": name, "view": "fp" if role == "host" else "remote", "owner": 11, "uuid": identity["hostUuid"], "activation": tick,
                                           "mode": action["mode"], "hand": action["hand"], "skin": skin, "armor": False, "shell": False, "phase": phase, "move": move, "windup": w, "recovery": r},
                              "copy": {"observations": observation, "passes": passes, "draws": [draw], "extractSequence": index*100+8, "renderSequence": index*100+9, "copySequence": index*100+10, "width": 2, "height": 2},
                              "image": {"relativeImagePath": png.name, "pngBytes": len(data), "pngSha256": hashlib.sha256(data).hexdigest(), "decodedPixels": pixels}, "callbackPixels": pixels}
                    self.records[key] = record
                    self.write(f"{role}-case-{index:02d}-{phase}-rendered", role, {**common, "acceptedTick": str(tick), "phase": phase, "renderedBeforeReadback": "true"})
                    self.write(key, role, {**common, "admissionReceiptSha256": admission_sha, "admissionGeneration": generation, "acceptedTick": str(tick), "acceptedReceiptSha256": accepted_sha, "phase": phase, "view": record["expected"]["view"], "skin": skin,
                                          "screenshotName": name, "actualSourceAge": str(age), "receiptRelativePath": name + ".json", "pngRelativePath": png.name,
                                          "pngSha256": record["image"]["pngSha256"], "callbackPixelSha256": pixels["sha256"], "receiptSha256": "pending",
                                          "scopeCleanupVerified": "true", "acceptanceObservedBeforeSource": "true", "pixelQualityReviewed": "false"})
                    self.rebind(key)

    def write(self, name, role, fields):
        self.values[name] = {**self.identity, "role": role, "pid": str(self.pids[role]), **fields}
        self.flush(name)

    def flush(self, name):
        def escape(value):
            return str(value).replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=").replace("\n", "\\n").replace("\r", "\\r")
        (self.ipc / (name + ".properties")).write_text("".join(key + "=" + escape(value) + "\n" for key, value in self.values[name].items()), encoding="iso-8859-1")

    def rebind(self, key):
        values = self.values[key]
        path = self.ipc.parent / values["role"] / values["receiptRelativePath"]
        write_json(path, self.records[key])
        values["receiptSha256"] = supervisor.digest(path)
        self.flush(key)


class PropertiesReaderTests(unittest.TestCase):
    def test_java_physical_boundaries_and_store_escapes_are_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "native.properties"
            for separator in (b"\n", b"\r", b"\r\n"):
                path.write_bytes(separator.join((b"# Java Properties.store", b"plain=ACCEPTED", b"empty=",
                    b"escaped=\\ leading\\:value\\=\\\\\\t\\n\\r\\f\\#\\!\\u0085\\u000B\\u000C\\u001C\\u001D\\u001E", b"")))
                properties = supervisor.read_properties(path)
                self.assertEqual(set(properties), {"plain", "empty", "escaped"})
                self.assertEqual(properties["plain"], "ACCEPTED")
                self.assertEqual(properties["empty"], "")
                self.assertEqual(supervisor.counter_property(properties["escaped"]),
                                 " leading:value=\\\t\n\r\f#!\x85\x0b\x0c\x1c\x1d\x1e")

    def test_raw_controls_noncanonical_keys_and_continuations_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "native.properties"
            controls = [code for code in range(32) if code not in (10, 13)] + list(range(127, 160))
            mutations = [b"first=good" + bytes([code]) + b"second=bad\n" for code in controls]
            mutations += [b" first=bad\n", b"first =bad\n", b"first:ignored=bad\n", b"fi\\u0072st=bad\n",
                          b"first=\\\nsecond=bad\n", b"first= unescaped leading space\n", b"first=\\q\n", b"first=\\uBAD\n"]
            for raw in mutations:
                path.write_bytes(raw)
                with self.subTest(raw=raw), self.assertRaises(ValueError): supervisor.read_properties(path)


class Fixture(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name).resolve()
        (self.root / ".gitignore").write_text("build/\nbuild-alt/\n.gradle/\n")
        write_json(self.root / supervisor.CONTRACT, CONTRACT)
        for mixin in supervisor.COUNTER_MIXINS:
            write_json(self.root / "src/gametest/resources" / mixin, {"required": True, "client": ["SyntheticCounterHook"]})
        (self.root / "source.txt").write_text("reviewed source")
        subprocess.run(["git", "init", "-q", str(self.root)], check=True)
        for args in (("add", "."), ("-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "-qm", "fixture")):
            subprocess.run(["git", "-C", str(self.root), *args], check=True)
        self.head = supervisor.source_head(self.root)
        self.descriptor = self.root / "build/resources/gametest/fabric.mod.json"
        write_json(self.descriptor, {"entrypoints": {"fabric-client-gametest": [supervisor.ENTRYPOINT]}, "mixins": list(supervisor.COUNTER_MIXINS)})
        for mixin in supervisor.COUNTER_MIXINS:
            shutil.copyfile(self.root / "src/gametest/resources" / mixin, self.descriptor.parent / mixin)
        shutil.copyfile(self.root / supervisor.CONTRACT, self.descriptor.parent / Path(supervisor.CONTRACT).name)
        self.java = self.root / "build/toolchain/bin/java"
        self.java.parent.mkdir(parents=True)
        self.java.write_text("test input only; never executed")
        self.config = self.root / ".gradle/loom-cache/launch.cfg"
        self.config.parent.mkdir(parents=True)
        self.config.write_text("commonProperties\n\tfabric.development=true\nclientArgs\n")
        self.classes = self.root / "build/classes/java/gametest"
        self.classes.mkdir(parents=True)
        (self.classes / "Fixture.class").write_bytes(b"compiled fixture")
        self.launch = {
            "schemaVersion": 2, "suite": "cast-receipt", "sourceHead": self.head, "checkoutSha": self.head, "prHeadSha": "", "project": str(self.root),
            "descriptor": str(self.descriptor), "descriptorSha256": supervisor.digest(self.descriptor),
            "contract": supervisor.CONTRACT, "contractSha256": supervisor.digest(self.root / supervisor.CONTRACT),
            "java": str(self.java), "main": supervisor.MAIN,
            "classpath": [str(self.descriptor.parent), str(self.classes)],
            "jvm": ["-Dfabric.dli.env=client", "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient", "-Dfabric.dli.config=" + str(self.config), "-Xmx8G"],
            "args": ["--gameDir", str(self.root / "build/old-game")], "environment": {"ALSOFT_DRIVERS": "null"},
            "runtimeSha256": {str(p): supervisor.digest(p) for p in (self.java, self.descriptor.parent, self.classes, self.config)},
        }
        self.launch_path = self.root / "build/native/launch.json"
        write_json(self.launch_path, self.launch)
        java_patch = patch.object(supervisor, "configured_java", return_value=self.java)
        java_patch.start(); self.addCleanup(java_patch.stop)
        self.identity = {"nonce": "fresh-nonce", "suite": "cast-receipt", "sourceHead": self.head, "checkoutSha": self.head, "prHeadSha": "",
                         "descriptorSha256": self.launch["descriptorSha256"], "hostUuid": supervisor.PROFILES["host"][1],
                         "peerUuid": supervisor.PROFILES["peer"][1], "runIdentity": "123-2-fresh-nonce"}

    def validate(self):
        return supervisor.validate_launch(self.launch, self.launch_path, self.root)

    def update_config(self, text):
        self.config.write_text(text)
        self.launch["runtimeSha256"][str(self.config)] = supervisor.digest(self.config)

    def options(self):
        return types.SimpleNamespace(launch=self.launch_path, output=self.root / "build/native/run", suite="cast-receipt", profile="aura", timeout=1, accepted_eula=None)


class ContractTests(Fixture):
    def test_exact_54_case_roster_retains_original_46_and_offline_profiles(self):
        contract = supervisor.load_contract(self.root)
        cases = contract["cases"]
        self.assertEqual(len(cases), 54)
        self.assertEqual(cases, EXPECTED_CASES)
        self.assertEqual(cases[:35], ORIGINAL_CASES)
        self.assertEqual(cases[:38], PRESERVED_CASES)
        self.assertEqual(cases[35:38], COUNTER_CASES)
        self.assertEqual(cases[38:42], SPECTATOR_CASES)
        self.assertEqual(cases[:42], PRIOR_CASES)
        self.assertEqual(cases[42:46], LIFE_CASES)
        self.assertEqual(cases[:46], ORIGINAL_46_CASES)
        self.assertEqual(cases[46:], COUNTER_VISUAL_CASES)
        self.assertEqual(supervisor.LIFE_CASES, tuple(LIFE_CASES))
        self.assertEqual(contract["expectedCount"], 54)
        self.assertEqual(contract["limits"], {"maxJvms": 2, "maxHeapMiBPerJvm": 2048, "maxCases": 54, "maxTimeoutSeconds": 900})
        for name, expected in supervisor.PROFILES.values():
            raw = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
            raw[6] = raw[6] & 15 | 48; raw[8] = raw[8] & 63 | 128
            self.assertEqual(str(uuid.UUID(bytes=bytes(raw))), expected)
        self.assertNotEqual(supervisor.PROFILES["host"][1], supervisor.PROFILES["peer"][1])

    def test_exporter_binds_the_same_ordered_roster(self):
        exporter = (ROOT / "tools/native/export_two_client_launch.init.gradle").read_text()
        roster = re.search(r"def expectedCases = (\[.*?\])", exporter, re.DOTALL)
        self.assertIsNotNone(roster)
        self.assertEqual(ast.literal_eval(roster[1]), EXPECTED_CASES)
        self.assertIn("contract.cases != expectedCases", exporter)

    def test_every_case_is_mandatory_and_cannot_be_substituted(self):
        for index, case in enumerate(EXPECTED_CASES):
            for replacement in ([], ["FOREIGN_CASE"], [EXPECTED_CASES[(index + 1) % len(EXPECTED_CASES)]]):
                value = copy.deepcopy(CONTRACT)
                value["cases"][index:index + 1] = replacement
                # A consistent count must not legitimize dropping any preserved or spectator case.
                value["expectedCount"] = len(value["cases"])
                value["limits"]["maxCases"] = len(value["cases"])
                write_json(self.root / supervisor.CONTRACT, value)
                with self.subTest(case=case, replacement=replacement), self.assertRaisesRegex(ValueError, "exact ordered 54-case roster"):
                    supervisor.load_contract(self.root)

    def test_original_counter_spectator_and_boundary_order_cannot_change(self):
        for first, second in ((0, 1), (34, 35), (35, 36), (36, 37), (37, 38), (38, 39), (40, 41), (41, 42), (42, 43), (44, 45), (45, 46), (46, 47), (49, 50), (52, 53)):
            value = copy.deepcopy(CONTRACT)
            value["cases"][first], value["cases"][second] = value["cases"][second], value["cases"][first]
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(first=first, second=second), self.assertRaisesRegex(ValueError, "exact ordered 54-case roster"):
                supervisor.load_contract(self.root)

    def test_legacy_count_invalid_rosters_and_relaxed_limits_are_rejected(self):
        mutations = [("expectedCount", 46), ("cases", ORIGINAL_46_CASES), ("expectedCount", 35), ("expectedCount", 38), ("expectedCount", 42), ("cases", PRIOR_CASES), ("cases", ORIGINAL_CASES), ("cases", PRESERVED_CASES), ("cases", None),
                     ("cases", [None] + EXPECTED_CASES[1:]), ("cases", [{}] + EXPECTED_CASES[1:])]
        for field, replacement in mutations:
            value = copy.deepcopy(CONTRACT)
            value[field] = replacement
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(field=field, replacement=replacement), self.assertRaisesRegex(ValueError, "exact ordered 54-case roster"):
                supervisor.load_contract(self.root)
        for field, replacement in (("maxJvms", 3), ("maxHeapMiBPerJvm", 4096), ("maxCases", 35), ("maxCases", 38), ("maxCases", 42), ("maxCases", 46), ("maxTimeoutSeconds", 901)):
            value = copy.deepcopy(CONTRACT)
            value["limits"][field] = replacement
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(limit=field), self.assertRaisesRegex(ValueError, "resource limits"):
                supervisor.load_contract(self.root)

    def test_contract_cannot_change_profiles_count_or_witness_paths(self):
        for field in ("peerProfile", "profiles", "cases", "terminalWitnesses"):
            value = copy.deepcopy(CONTRACT)
            if field == "peerProfile":
                value[field]["offlineUuid"] = "foreign"
            elif field == "profiles":
                value[field]["aura"]["username"] = "foreign"
            else:
                value[field].append("EXTRA")
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(field=field), self.assertRaises(ValueError):
                supervisor.load_contract(self.root)

    def test_cli_rejects_legacy_flags_arbitrary_commands_and_incomplete_moon_selection(self):
        base = ["build/launch.json", "--output", "build/out", "--suite", "cast-receipt", "--profile", "aura"]
        for extra in (["--require-unity"], ["--command", "sh"], ["--java", "sh"], ["--case", "anything"], ["--suite", "moon"]):
            with contextlib.redirect_stderr(io.StringIO()), self.subTest(extra=extra), self.assertRaises(SystemExit):
                supervisor.parse_args(base + extra)


class ProvenanceTests(Fixture):
    def test_clean_exact_export_is_accepted(self):
        self.assertEqual(len(self.validate()), 64)

    def test_dirty_tracked_and_untracked_source_are_rejected(self):
        (self.root / "source.txt").write_text("unreviewed")
        with self.assertRaisesRegex(ValueError, "clean checkout"):
            self.validate()
        (self.root / "source.txt").write_text("reviewed source")
        (self.root / "untracked.java").write_text("new source")
        with self.assertRaisesRegex(ValueError, "clean checkout"):
            self.validate()

    def test_checkout_and_pr_heads_are_distinct_and_bound(self):
        pr_head = "a" * 40
        self.launch["prHeadSha"] = pr_head
        with patch.dict(os.environ, {"WILDERCORD_PR_HEAD_SHA": pr_head}):
            self.validate()
        with patch.dict(os.environ, {"WILDERCORD_PR_HEAD_SHA": "b" * 40}):
            with self.assertRaisesRegex(ValueError, "explicit workflow metadata"):
                self.validate()
        self.launch["checkoutSha"] = pr_head
        with self.assertRaisesRegex(ValueError, "Checkout association"):
            self.validate()

    def test_missing_or_invalid_pr_association_is_rejected(self):
        for bad in (None, "main", "a" * 39, "A" * 40):
            self.launch["prHeadSha"] = bad
            with self.subTest(prHeadSha=bad), self.assertRaisesRegex(ValueError, "Malformed PR-head"):
                self.validate()
        self.launch["prHeadSha"] = ""
        with patch.dict(os.environ, {"WILDERCORD_PR_HEAD_SHA": ""}):
            self.validate()

    def test_wrong_head_suite_project_main_or_java_are_rejected(self):
        for key, bad in (("schemaVersion", 1), ("sourceHead", "0" * 40), ("suite", "moon"), ("project", str(self.root.parent)), ("main", "foreign.Main"), ("java", "/bin/sh")):
            old = self.launch[key]; self.launch[key] = bad
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.validate()
            self.launch[key] = old

    def test_altered_descriptor_or_compiled_classes_are_rejected(self):
        original = self.descriptor.read_bytes()
        self.descriptor.write_bytes(original + b" ")
        with self.assertRaisesRegex(ValueError, "descriptor changed"):
            self.validate()
        self.descriptor.write_bytes(original)
        (self.classes / "Fixture.class").write_bytes(b"other runtime bytes")
        with self.assertRaisesRegex(ValueError, "Runtime input changed"):
            self.validate()

    def test_rehashed_descriptor_cannot_add_another_entrypoint(self):
        write_json(self.descriptor, {"entrypoints": {"fabric-client-gametest": [supervisor.ENTRYPOINT, "foreign.Main"]}})
        self.launch["descriptorSha256"] = supervisor.digest(self.descriptor)
        with self.assertRaisesRegex(ValueError, "exactly"):
            self.validate()

    def test_processed_contract_must_equal_source_contract(self):
        target = self.descriptor.parent / Path(supervisor.CONTRACT).name
        target.write_text("{}")
        with self.assertRaisesRegex(ValueError, "Processed contract"):
            self.validate()

    def test_contract_path_and_hash_remain_bound_to_source(self):
        for key, bad in (("contract", "build/alternate-contract.json"), ("contractSha256", "0" * 64)):
            original = self.launch[key]
            self.launch[key] = bad
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, "Source contract changed"):
                self.validate()
            self.launch[key] = original

    def test_java_and_launch_config_bytes_remain_hash_bound(self):
        for path in (self.java, self.config):
            original = path.read_bytes()
            path.write_bytes(original + b"\n")
            with self.subTest(path=path.name), self.assertRaisesRegex(ValueError, "Runtime input changed"):
                self.validate()
            path.write_bytes(original)

    def test_jvm_and_environment_injection_is_rejected(self):
        for argument in ("-javaagent:/tmp/agent.jar", "@/tmp/args", "-jar", "-Djava.system.class.loader=foreign.Loader", "-Dwildercord.mp.role=peer", "-Dfabric.addMods=/tmp/foreign.jar"):
            self.launch["jvm"].append(argument)
            with self.subTest(argument=argument), self.assertRaises(ValueError):
                self.validate()
            self.launch["jvm"].pop()
        self.launch["environment"] = {"JAVA_TOOL_OPTIONS": "foreign"}
        with self.assertRaisesRegex(ValueError, "environment"):
            self.validate()

    def test_hidden_loom_config_cannot_override_role_game_dir_or_connect_target(self):
        for config in ("commonProperties\n\twildercord.mp.role=peer\n", "clientArgs\n\t--server\n\tremote.invalid\n", "clientArgs\n\t--gameDir\n\t" + str(self.root / "build/other") + "\n", "clientProperties\n\tjava.library.path=/tmp/foreign\n"):
            self.update_config(config)
            with self.subTest(config=config), self.assertRaises(ValueError):
                self.validate()

    def test_native_paths_introduced_by_config_are_hash_bound(self):
        native = self.root / "build/natives"; native.mkdir()
        library = native / "library.so"; library.write_bytes(b"original")
        self.update_config("clientProperties\n\tjava.library.path=" + str(native) + "\n")
        with self.assertRaisesRegex(ValueError, "every runtime input"):
            self.validate()
        self.launch["runtimeSha256"][str(native)] = supervisor.digest(native)
        self.validate()
        library.write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "Runtime input changed"):
            self.validate()

    def test_remap_classpath_contents_cannot_escape_allowed_roots(self):
        remap = self.root / "build/remap.txt"; remap.write_text("/tmp/foreign.jar")
        self.update_config("commonProperties\n\tfabric.remapClasspathFile=" + str(remap) + "\n")
        self.launch["runtimeSha256"][str(remap)] = supervisor.digest(remap)
        with self.assertRaisesRegex(ValueError, "escapes"):
            self.validate()

    def test_real_loom118_java25_flags_and_config_are_accepted(self):
        self.launch["jvm"] += ["-XX:StackShadowPages=32", "--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED"]
        jar = self.root / "build/common.jar"; jar.write_bytes(b"test game jar")
        self.update_config("commonProperties\n\tfabric.development=true\n\tfabric.defaultModDistributionNamespace=official\n\tfabric.defaultMixinRemapType=static\n\tfabric.gameJarPath=" + str(jar) + "\nclientArgs\n\t--assetIndex\n\t26.3\n\t--assetsDir\n\t" + str(self.root / "build/assets") + "\n")
        self.launch["runtimeSha256"][str(jar)] = supervisor.digest(jar)
        self.validate()

    def test_unknown_arguments_and_unbounded_dimensions_are_rejected(self):
        for args in (["--server", "remote.invalid"], ["--gameDir", "/tmp/foreign"], ["--gameDir"], ["--width", "9000"]):
            self.launch["args"] = args
            with self.subTest(args=args), self.assertRaises(ValueError):
                self.validate()

    def test_fingerprints_must_cover_every_input(self):
        del self.launch["runtimeSha256"][str(self.java)]
        with self.assertRaisesRegex(ValueError, "every runtime input"):
            self.validate()

    def test_output_paths_cannot_escape_or_dirty_source(self):
        with self.assertRaises(ValueError):
            supervisor.output_path(self.root / "src/output", self.root)
        target = self.root / "build/escape"; target.symlink_to(self.root.parent, target_is_directory=True)
        with self.assertRaises(ValueError):
            supervisor.output_path(target / "output", self.root)
        with self.assertRaisesRegex(ValueError, "Git-ignored"):
            supervisor.ignored_output(self.root / "artifacts/unignored", self.root)

    def test_exclusive_lock_and_lock_path_confinement(self):
        with supervisor.supervisor_lock(self.root):
            with self.assertRaisesRegex(RuntimeError, "Another"):
                with supervisor.supervisor_lock(self.root):
                    self.fail("Concurrent lock acquired")
        with supervisor.supervisor_lock(self.root):
            pass
        shutil.rmtree(self.root / ".gradle")
        (self.root / ".gradle").symlink_to(self.root.parent, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "escapes"):
            with supervisor.supervisor_lock(self.root):
                self.fail("Escaping lock acquired")

    def test_eula_requires_prior_file_or_both_explicit_ci_flags(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(ValueError):
                supervisor.accepted_eula(None, self.root)
            os.environ["CI_MINECRAFT_EULA_ACCEPTED"] = "true"
            with self.assertRaises(ValueError):
                supervisor.accepted_eula(None, self.root)
            os.environ["GITHUB_ACTIONS"] = "true"
            self.assertIn(b"eula=true", supervisor.accepted_eula(None, self.root)[0])
        eula = self.root / "build/eula.txt"; eula.write_text("eula=false")
        with self.assertRaises(ValueError):
            supervisor.accepted_eula(eula, self.root)
        eula.write_text("# Previously accepted\neula=true\n")
        self.assertEqual(supervisor.accepted_eula(eula, self.root)[0], eula.read_bytes())

    def test_owned_command_replaces_heap_profiles_and_game_directory(self):
        for role, (name, native_uuid) in supervisor.PROFILES.items():
            game = self.root / "build/run" / role
            args = supervisor.command(self.launch, role, game, game.parent / "ipc", self.identity, 120)
            self.assertEqual([arg for arg in args if arg.startswith("-Xmx")], ["-Xmx2G"])
            for flag, expected in (("--username", name), ("--uuid", native_uuid), ("--gameDir", str(game)), ("--accessToken", "0")):
                self.assertEqual(args[args.index(flag) + 1], expected)
            self.assertIn("-Dwildercord.mp.timeoutSeconds=120", args)
            self.assertIn("-Djava.io.tmpdir=" + str(game / "tmp"), args)

    def test_ambient_java_injection_is_removed_and_caches_are_owned(self):
        game = self.root / "build/run/host"
        with patch.dict(os.environ, {"JAVA_TOOL_OPTIONS": "bad", "JDK_JAVA_OPTIONS": "bad", "WILDERCORD_CASE": "bad", "LD_PRELOAD": "bad", "DISPLAY": ":99"}):
            env = supervisor.environment(self.launch, game)
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "WILDERCORD_CASE", "LD_PRELOAD"):
            self.assertNotIn(key, env)
        self.assertEqual(env["DISPLAY"], ":99")
        self.assertEqual(env["TMPDIR"], str(game / "tmp"))
        self.assertEqual(env["XDG_CACHE_HOME"], str(game / "cache"))


class WitnessTests(Fixture):
    def setUp(self):
        super().setUp()
        self.ipc = self.root / "build/ipc"; self.ipc.mkdir()
        self.jobs = [("host", types.SimpleNamespace(pid=1001)), ("peer", types.SimpleNamespace(pid=1002))]
        for filename, role in supervisor.TERMINALS.items():
            values = {**self.identity, "role": role, "pid": "1001" if role == "host" else "1002", "cases": ",".join(CONTRACT["cases"])}
            (self.ipc / filename).write_text("# Java properties\n" + "\n".join(key + "=" + value for key, value in values.items()) + "\n")

        write_life_witnesses(self.ipc, self.identity, {role: process.pid for role, process in self.jobs})
        self.counter = SyntheticCounterEvidence(self.ipc, self.identity, {role: process.pid for role, process in self.jobs})

    def validate_witnesses(self):
        return supervisor.validate_witnesses(self.ipc, self.identity, CONTRACT["cases"], self.jobs)

    def test_all_three_exact_witnesses_are_required(self):
        self.assertEqual(len(self.validate_witnesses()), 3)
        (self.ipc / "peer-disconnected.properties").unlink()
        with self.assertRaisesRegex(ValueError, "Missing"):
            self.validate_witnesses()

    def test_wrong_identity_role_foreign_pid_partial_cases_and_duplicates_fail(self):
        target = self.ipc / "peer-cast-receipt-passed.properties"
        original = target.read_text()
        for key in (*self.identity, "role", "pid", "cases"):
            target.write_text("\n".join(key + "=wrong" if line.startswith(key + "=") else line for line in original.splitlines()))
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, "mismatched " + key):
                self.validate_witnesses()
        target.write_text(original + "nonce=fresh-nonce\n")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.validate_witnesses()

    def test_every_terminal_rejects_legacy_missing_replaced_or_reordered_added_cases(self):
        bad_rosters = [ORIGINAL_46_CASES, ORIGINAL_CASES, PRESERVED_CASES, PRIOR_CASES, EXPECTED_CASES + ["EXTRA_CASE"],
                       ORIGINAL_CASES + list(reversed(COUNTER_CASES)) + SPECTATOR_CASES,
                       PRESERVED_CASES + list(reversed(SPECTATOR_CASES))]
        for index in range(35, 54):
            bad_rosters.append(EXPECTED_CASES[:index] + EXPECTED_CASES[index + 1:])
            bad_rosters.append(EXPECTED_CASES[:index] + ["FOREIGN_CASE"] + EXPECTED_CASES[index + 1:])
        for filename in supervisor.TERMINALS:
            target = self.ipc / filename
            original = target.read_text()
            for cases in bad_rosters:
                target.write_text("\n".join("cases=" + ",".join(cases) if line.startswith("cases=") else line for line in original.splitlines()))
                with self.subTest(filename=filename, cases=cases), self.assertRaisesRegex(ValueError, "mismatched cases"):
                    self.validate_witnesses()
            target.write_text(original)
        self.assertEqual(len(self.validate_witnesses()), 3)

    def test_identical_pids_and_symlink_witnesses_fail(self):
        self.jobs[1][1].pid = 1001
        with self.assertRaisesRegex(ValueError, "distinct"):
            self.validate_witnesses()
        self.jobs[1][1].pid = 1002
        target = self.ipc / "peer-disconnected.properties"
        target.rename(self.ipc / "saved.properties")
        target.symlink_to(self.ipc / "saved.properties")
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.validate_witnesses()

    def test_every_new_life_handshake_phase_is_required(self):
        for index in range(42, 46):
            for phase in ("prepare", "armed", "captured", "ready", "seen", "passed"):
                target = self.ipc / f"case-{index:02d}-{phase}.properties"
                original = target.read_text(); target.unlink()
                with self.subTest(index=index, phase=phase), self.assertRaisesRegex(ValueError, "Missing"):
                    self.validate_witnesses()
                target.write_text(original)

    def test_life_roles_sources_camera_and_unique_measurements_fail_closed(self):
        mutations = [("prepare", "lifeSource", self.identity["peerUuid"]),
                     ("prepare", "lifeViewerUuid", self.identity["hostUuid"]), ("prepare", "lifeOwnerEntity", "2"),
                     ("armed", "role", "host"), ("armed", "lifeSource", ""), ("armed", "lifeCameraNative", "false"),
                     ("captured", "lifeSource", "not-a-uuid"), ("captured", "lifeCapture", "queued"),
                     ("ready", "role", "peer"), ("ready", "lifeSource", self.identity["peerUuid"]),
                     ("ready", "lifeOwnerSource", self.identity["peerUuid"]), ("ready", "lifeMoment", "PULSE"),
                     ("ready", "lifeOwnerPieces", "1"), ("ready", "lifeFullPieces", "129"),
                     ("seen", "lifePeerSource", self.identity["peerUuid"]), ("seen", "lifePeerCameraNative", "yes"),
                     ("seen", "lifePeerUnique", "8"), ("seen", "lifePeerPieces", "07"),
                     ("seen", "lifePeerExtracted", "0"), ("passed", "role", "peer")]
        for phase, key, value in mutations:
            target = self.ipc / f"case-42-{phase}.properties"; original = target.read_text()
            target.write_text("\n".join(key + "=" + value if line.startswith(key + "=") else line for line in original.splitlines()))
            with self.subTest(phase=phase, key=key, value=value), self.assertRaises(ValueError):
                self.validate_witnesses()
            target.write_text(original)

    def test_reduced_flash_requires_independent_time_and_alpha_samples(self):
        for phase, prefix in (("ready", "lifeOwner"), ("seen", "lifePeer")):
            for suffix in ("Time", "ReducedSamples"):
                target = self.ipc / f"case-45-{phase}.properties"; original = target.read_text(); key = prefix + suffix
                target.write_text("\n".join(key + "=0" if line.startswith(key + "=") else line for line in original.splitlines()))
                with self.subTest(phase=phase, key=key), self.assertRaisesRegex(ValueError, "Time support"):
                    self.validate_witnesses()
                target.write_text(original)


class CounterSelectionTests(Fixture):
    def test_fixed_counter_profiles_preserve_aura_and_bound_full_roster(self):
        self.assertEqual(supervisor.selection()["profiles"], supervisor.PROFILES)
        for profile, (name, offline, skin) in supervisor.COUNTER_PROFILES.items():
            selected = supervisor.selection(supervisor.SUITE, profile)
            self.assertEqual(selected["cases"], EXPECTED_CASES)
            self.assertEqual(selected["maxTimeoutSeconds"], 900)
            self.assertEqual(selected["expectedSkin"], skin)
            self.assertEqual(selected["profiles"]["peer"], supervisor.PROFILES["peer"])
            raw = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
            raw[6] = raw[6] & 15 | 48; raw[8] = raw[8] & 63 | 128
            self.assertEqual(str(uuid.UUID(bytes=bytes(raw))), offline)
            args = supervisor.command(self.launch, "host", self.root / "build/game", self.root / "build/ipc", self.identity, 900, selected)
            self.assertEqual(args[args.index("--username") + 1], name)
            self.assertEqual(args[args.index("--uuid") + 1], offline)
            self.assertIn("-Dwildercord.counter.expectedSkin=" + skin, args)
            self.assertEqual([arg for arg in args if arg.startswith("-Xmx")], ["-Xmx2G"])
            cli = ["build/launch.json", "--output", "build/run", "--suite", "cast-receipt", "--profile", profile]
            self.assertEqual(supervisor.parse_args(cli).timeout, 900)
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                supervisor.parse_args(cli + ["--timeout", "901"])
            with self.assertRaises(ValueError):
                supervisor.selection("moon", profile, supervisor.MOON_CASE, "front_oblique")
        with self.assertRaises(ValueError):
            supervisor.selection(supervisor.SUITE, "arbitrary")

    def test_counter_hooks_must_exist_once_and_match_exported_bytes(self):
        self.validate()
        original = json.loads(self.descriptor.read_text())
        for mixins in ([], list(supervisor.COUNTER_MIXINS) * 2):
            write_json(self.descriptor, {**original, "mixins": mixins})
            self.launch["descriptorSha256"] = supervisor.digest(self.descriptor)
            with self.assertRaisesRegex(ValueError, "registered exactly once"):
                self.validate()
        write_json(self.descriptor, original)
        self.launch["descriptorSha256"] = supervisor.digest(self.descriptor)
        (self.descriptor.parent / supervisor.COUNTER_MIXINS[0]).write_text("changed")
        with self.assertRaisesRegex(ValueError, "hook descriptor changed"):
            self.validate()

    def test_contract_cannot_drop_counter_phase_profile_union_or_hooks(self):
        for field, value in (("casePrefixCount", 45), ("phases", ["ACTIVE"]), ("requiredProfiles", ["aura-wide"])):
            contract = copy.deepcopy(CONTRACT); contract["counterVisuals"][field] = value
            write_json(self.root / supervisor.CONTRACT, contract)
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, "phase/profile union"):
                supervisor.load_contract(self.root)
        contract = copy.deepcopy(CONTRACT); contract["requiredMixins"] = []
        write_json(self.root / supervisor.CONTRACT, contract)
        with self.assertRaisesRegex(ValueError, "passive hooks"):
            supervisor.load_contract(self.root)

    def test_contract_requires_complete_admission_schema_and_original_budget(self):
        for field, value in (("schemaVersion", 2), ("exactWitnessCount", 7), ("requiredProperties", []),
                             ("acceptedDigestProperty", "optional"), ("receivedSourceProperties", ["receivedSourceMove"]),
                             ("sharedInitialNativeTickBudget", 13), ("nativeStatus", "verified")):
            contract = copy.deepcopy(CONTRACT); contract["counterVisuals"]["admission"][field] = value
            write_json(self.root / supervisor.CONTRACT, contract)
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, "admission proof closure"):
                supervisor.load_contract(self.root)

    def test_counter_nonce_is_fresh_generated_input_and_does_not_activate_moon(self):
        nonce = "00000000-0000-4000-8000-000000000001"
        with patch.dict(os.environ, {"WILDERCORD_COUNTER_RECEIPT_NONCE": "stale", "WILDERCORD_MOON_RECEIPT_NONCE": "foreign"}):
            self.assertNotIn("WILDERCORD_COUNTER_RECEIPT_NONCE", supervisor.environment(self.launch, self.root))
            env = supervisor.environment(self.launch, self.root, counter_nonce=nonce)
            self.assertEqual(env["WILDERCORD_COUNTER_RECEIPT_NONCE"], nonce)
            self.assertNotIn("WILDERCORD_MOON_RECEIPT_NONCE", env)
        with self.assertRaises(ValueError):
            supervisor.environment(self.launch, self.root, counter_nonce="not-a-nonce")


class CounterEvidenceTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(); self.addCleanup(temp.cleanup)
        self.base = Path(temp.name).resolve(); self.ipc = self.base / "ipc"; self.ipc.mkdir()
        self.identity = {"nonce": "00000000-0000-4000-8000-000000000001", "suite": supervisor.SUITE,
                         "sourceHead": "a" * 40, "checkoutSha": "a" * 40, "prHeadSha": "", "descriptorSha256": "b" * 64,
                         "hostUuid": supervisor.PROFILES["host"][1], "peerUuid": supervisor.PROFILES["peer"][1], "runIdentity": "synthetic-only-1"}
        self.pids = {"host": 111, "peer": 222}
        self.synthetic = SyntheticCounterEvidence(self.ipc, self.identity, self.pids)

    def verify(self):
        supervisor.validate_counter_witnesses(self.ipc, self.identity, self.pids)

    def mutate_record(self, key, path, value):
        original = copy.deepcopy(self.synthetic.records[key])
        target = self.synthetic.records[key]
        for component in path[:-1]:
            target = target[component]
        target[path[-1]] = value
        self.synthetic.rebind(key)
        try:
            with self.subTest(key=key, path=path, value=value), self.assertRaises(ValueError):
                self.verify()
        finally:
            self.synthetic.records[key] = original; self.synthetic.rebind(key)

    def test_complete_synthetic_geometry_and_pixel_bindings_pass_without_native_claim(self):
        self.verify()
        self.assertEqual(len(self.synthetic.records), 64)
        self.assertEqual(len(list(self.ipc.glob("*-admission.properties"))), 8)
        # Exercise the exact escaping emitted by Properties.store, including trace '=' and generation ':'.
        data = (self.ipc / "case-46-admission.properties").read_text()
        self.assertIn("generation\\=", data)
        self.assertIn("\\:case-46", data)
        self.assertTrue(all(record["pixelQualityReviewed"] is False for record in self.synthetic.records.values()))

    def rehash_counter_accepted(self, index=46):
        accepted_sha = supervisor.digest(self.ipc / f"case-{index:02d}-accepted.properties")
        for role in ("host", "peer"):
            for phase in supervisor.COUNTER_PHASES:
                key = f"{role}-case-{index:02d}-{phase}-observed"
                self.synthetic.values[key]["acceptedReceiptSha256"] = accepted_sha
                self.synthetic.records[key]["copy"]["observations"]["acceptedReceiptSha256"] = accepted_sha
                self.synthetic.rebind(key)

    def rehash_counter_admission(self, index=46, copy_fields=False):
        prefix = f"case-{index:02d}-"
        admission, accepted = (self.synthetic.values[prefix + name] for name in ("admission", "accepted"))
        self.synthetic.flush(prefix + "admission")
        if copy_fields:
            accepted.update({key: value for key, value in admission.items() if key.startswith("admission")})
        admission_sha = supervisor.digest(self.ipc / (prefix + "admission.properties"))
        accepted["admissionReceiptSha256"] = admission_sha; self.synthetic.flush(prefix + "accepted")
        for role in ("host", "peer"):
            for phase in supervisor.COUNTER_PHASES:
                key = f"{role}-case-{index:02d}-{phase}-observed"
                binding = {"admissionReceiptSha256": admission_sha, "admissionGeneration": accepted["admissionGeneration"]}
                self.synthetic.values[key].update(binding)
                self.synthetic.records[key]["copy"]["observations"].update(binding)
        self.rehash_counter_accepted(index)

    def rehash_raw_admission(self, raw, index=46):
        """Keep deliberately mutated physical bytes; refresh every downstream digest."""
        path = self.ipc / f"case-{index:02d}-admission.properties"; path.write_bytes(raw)
        sha = supervisor.digest(path); key = f"case-{index:02d}-accepted"
        self.synthetic.values[key]["admissionReceiptSha256"] = sha; self.synthetic.flush(key)
        for role in ("host", "peer"):
            for phase in supervisor.COUNTER_PHASES:
                key = f"{role}-case-{index:02d}-{phase}-observed"
                self.synthetic.values[key]["admissionReceiptSha256"] = sha
                self.synthetic.records[key]["copy"]["observations"]["admissionReceiptSha256"] = sha
        self.rehash_counter_accepted(index)

    def test_rehashed_java_parser_boundary_and_key_divergence_fails_closed(self):
        original = (self.ipc / "case-46-admission.properties").read_bytes()
        needle = b"admissionCheck=ACCEPTED\n"
        self.assertEqual(original.count(needle), 1)
        mutations = [original.replace(needle, b"admissionCheck=ACCEPTED" + bytes([code]))
                     for code in (0x85, 0x0b, 0x0c, 0x1c, 0x1d, 0x1e)]
        # Java would normalize these keys or continue the preceding logical value.
        mutations += [original + extra for extra in (b"admission\\u0043heck=NO_AURA\n", b" admissionCheck=NO_AURA\n",
                      b"admissionCheck:ignored=NO_AURA\n", b"ignored=continued\\\nadmissionCheck=NO_AURA\n")]
        for raw in mutations:
            self.rehash_raw_admission(raw)
            with self.subTest(mutant=raw[-100:]), self.assertRaises(ValueError): self.verify()
        for separator in (b"\n", b"\r", b"\r\n"):
            self.rehash_raw_admission(original.replace(b"\n", separator)); self.verify()
        self.rehash_raw_admission(original); self.verify()

    def test_every_native_admission_field_is_required_even_after_rehashing(self):
        original = self.synthetic.values["case-46-admission"].copy()
        for field in supervisor.COUNTER_ADMISSION_FIELDS:
            del self.synthetic.values["case-46-admission"][field]
            self.rehash_counter_admission()
            with self.subTest(field=field), self.assertRaises(ValueError): self.verify()
            self.synthetic.values["case-46-admission"] = original.copy()
        self.rehash_counter_admission(); self.verify()

    def test_rehashed_failed_native_admission_cannot_be_rescued_by_accepted_or_images(self):
        original = self.synthetic.values["case-46-admission"].copy()
        mutations = {
            "SchemaVersion": "2", "Generation": self.identity["nonce"] + ":case-47", "PeerArmedSha256": "f"*64,
            "Art": "null_parry", "SentMarks": "[3]", "RequestMarks": "[39]", "PerformMarks": "[3]",
            "ServerBodyUuid": self.identity["peerUuid"], "ServerBodyEntity": "22", "ServerLevel": "minecraft:the_nether",
            "ClientBodyUuid": self.identity["peerUuid"], "ClientBodyEntity": "22", "ClientLevel": "minecraft:the_nether",
            "ServerConnectionIdentity": "0", "ClientConnectionIdentity": "9999", "Busy": "true", "ExciseBlocking": "true",
            "Performed": "false", "Check": "NO_AURA", "Refusal": "NO_AURA", "ObserverFailure": "failed", "NativeFailure": "thrown",
            "Trace": original["admissionTrace"].replace("sends=1", "sends=2"), "ArmedTick": "101", "SentTick": "1000",
            "RequestTick": "101", "ReturnTick": "101", "PaidTick": "99", "Paid": "7.0", "Cost": "9.0",
            "AuraBefore": "18.0", "AuraAfter": "12.0", "SpendLeft": "12.0", "Rest": "100", "Backlash": "true",
            "OriginalBody": "false", "PendingAtEntry": "1234", "ActionIdentity": "0", "SourceActionIdentity": "9999",
            "ReturnActionIdentity": "9999", "ReturnCommitted": "false", "SourceEntity": "22", "SourceMove": "25",
            "SourceStartTick": "99", "SourceWindup": "7", "SourceRecovery": "17", "SourceYaw": "30.0", "SourcePitch": "10.0",
        }
        self.assertEqual({"admission" + key for key in mutations}, set(supervisor.COUNTER_ADMISSION_FIELDS))
        for suffix, bad in mutations.items():
            self.synthetic.values["case-46-admission"]["admission" + suffix] = bad
            self.rehash_counter_admission(copy_fields=True)
            with self.subTest(field=suffix), self.assertRaises(ValueError): self.verify()
            self.synthetic.values["case-46-admission"] = original.copy()
        self.rehash_counter_admission(copy_fields=True); self.verify()

    def test_accepted_only_contradictions_and_bogus_admission_digest_fail_after_downstream_rehash(self):
        key = "case-46-accepted"; original = self.synthetic.values[key].copy()
        for field, bad in (("admissionReceiptSha256", "f"*64), ("admissionCheck", "NO_AURA"),
                           ("admissionPerformed", "false"), ("admissionRefusal", "NO_AURA"),
                           ("admissionSourceYaw", "90.0"), ("admissionReturnActionIdentity", "9999")):
            self.synthetic.values[key][field] = bad; self.synthetic.flush(key); self.rehash_counter_accepted()
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, "contradicts immutable"): self.verify()
            self.synthetic.values[key] = original.copy()
        self.synthetic.flush(key); self.rehash_counter_accepted(); self.verify()

    def test_missing_extra_foreign_dangling_and_linked_admission_records_fail(self):
        path = self.ipc / "case-46-admission.properties"; original = path.read_bytes()
        path.unlink()
        with self.assertRaises(ValueError): self.verify()
        path.symlink_to(self.base / "missing-admission")
        with self.assertRaises(ValueError): self.verify()
        path.unlink(); path.write_bytes(original)
        alias = self.base / "linked-witness"; os.link(path, alias)
        with self.assertRaisesRegex(ValueError, "Hard-linked"): self.verify()
        alias.unlink()
        for name in ("case-45-admission.properties", "case-54-admission.properties", "peer-case-46-admission.properties",
                     "case-46-admission.tmp", "case-46-admission.properties.backup"):
            extra = self.ipc / name; extra.write_bytes(original)
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, "extra counter admission"): self.verify()
            extra.unlink()
        for field, value in (("nonce", "stale"), ("pid", "222"), ("role", "peer"), ("case", supervisor.COUNTER_CASES[1])):
            old = self.synthetic.values["case-46-admission"][field]
            self.synthetic.values["case-46-admission"][field] = value; self.rehash_counter_admission(copy_fields=True)
            with self.subTest(field=field), self.assertRaises(ValueError): self.verify()
            self.synthetic.values["case-46-admission"][field] = old
        self.rehash_counter_admission(copy_fields=True); self.verify()

    def test_rehashed_actual_owner_peer_sources_must_match_every_native_admission_field(self):
        for role in ("host", "peer"):
            key = role + "-case-46-ACTIVE-observed"
            changes = {"Entity": "22", "Move": "25", "StartTick": "99", "Windup": "7", "Recovery": "17", "Yaw": "30.0", "Pitch": "10.0"}
            for field, value in changes.items():
                self.mutate_record(key, ("copy", "observations", "receivedSource" + field), value)
            for field, value in (("admissionReceiptSha256", "f"*64), ("admissionGeneration", "stale"),
                                 ("receivedOwnerUuid", self.identity["peerUuid"]), ("receivedOwnerEntity", "22"),
                                 ("receivedLevel", "minecraft:the_nether"), ("receivedConnectionIdentity", "9999")):
                self.mutate_record(key, ("copy", "observations", field), value)
        self.verify()

    def test_replayed_case_action_and_malformed_admission_properties_fail(self):
        key = "case-47-accepted"; old = self.synthetic.values[key]["acceptedTick"]
        self.synthetic.values[key]["acceptedTick"] = "100"; self.synthetic.flush(key); self.rehash_counter_accepted(47)
        with self.assertRaisesRegex(ValueError, "replays or overlaps"): self.verify()
        self.synthetic.values[key]["acceptedTick"] = old; self.synthetic.flush(key); self.rehash_counter_accepted(47)
        path = self.ipc / "case-46-admission.properties"; original = path.read_bytes()
        for extra in (b"admissionCheck=NO_AURA\n", b"foreignAdmission=true\n", b"bad=trailing\\\n", b"bad=\\uNOTA\n"):
            path.write_bytes(original + extra)
            with self.subTest(extra=extra), self.assertRaises(ValueError): self.verify()
        path.write_bytes(original); self.verify()

    def test_missing_or_forged_deep_displayed_item_draw_fails_before_acceptance(self):
        for key in ("host-case-46-ACTIVE-observed", "peer-case-46-ACTIVE-observed", "host-case-48-ACTIVE-observed"):
            self.mutate_record(key, ("copy","draws"), [])
            for path, value in [(("binding","item"),999),(("binding","source"),999),(("binding","palette","activation"),101),
                                (("displayContext",),"FIXED"),(("quads",),0),(("anchorKind",),"guessed"),
                                (("geometry","actual",0),1.),(("geometry","actual"),[]),(("geometry","matched"),False),
                                (("anchorActual",0),1.),(("anchorActual",),[0.,0.]),(("displayRotation",1),0.),
                                (("displayScale",0),1.),(("displayLocal",12),1.),(("emittedQuadPositions",0),1.),
                                (("emittedQuadSha256",),"a"*64),(("emittedQuadPositions",),[]),(("stackIdentity",),0),(("collectorIdentity",),0),(("stackIdentity",),999),(("collectorIdentity",),999)]:
                self.mutate_record(key,("copy","draws",0,*path),value)

    def test_counter_png_receipt_and_witness_hardlinks_are_rejected(self):
        key="host-case-46-ACTIVE-observed"; values=self.synthetic.values[key]
        targets=[self.base/"host"/values[field] for field in ("pngRelativePath","receiptRelativePath")]
        targets.append(self.ipc/"case-46-armed.properties")
        for target in targets:
            alias=target.with_name(target.name+".hardlink")
            os.link(target,alias)
            self.assertEqual(target.stat().st_nlink,2)
            with self.subTest(target=target),self.assertRaisesRegex(ValueError,"Hard-linked"):self.verify()
            alias.unlink()
        self.verify()

    def test_any_post_success_rejection_artifact_invalidates_prior_good_images(self):
        self.verify()
        for name in ("host-counter-render-failure.properties","peer-counter-render-failure.properties",
                     "foreign-counter-render-failure.properties","host-counter-render-failure-partial.tmp"):
            marker=self.ipc/name
            for content in ("", "rejected=true\n", "nonce=foreign\nrejected=false\n"):
                marker.write_text(content)
                with self.subTest(name=name,content=content),self.assertRaisesRegex(ValueError,"rejection artifact"):self.verify()
                marker.unlink()
            marker.symlink_to(self.ipc/"missing-rejection-target")
            with self.assertRaisesRegex(ValueError,"rejection artifact"):self.verify()
            marker.unlink()
            marker.mkdir()
            with self.assertRaisesRegex(ValueError,"rejection artifact"):self.verify()
            marker.rmdir()
        self.verify()

    def test_equal_valued_source_replacement_or_case_reuse_is_rejected(self):
        for role in ("host","peer"):
            key=f"{role}-case-46-ACTIVE-observed"
            old=int(self.synthetic.records[key]["copy"]["observations"]["acceptedSourceIdentity"])
            self.mutate_record(key,("copy","observations","acceptedSourceIdentity"),str(old-1))
        # Whole later-case reuse cannot evade the within-case comparison.
        for phase in supervisor.COUNTER_PHASES:
            key=f"host-case-47-{phase}-observed"
            self.synthetic.records[key]["copy"]["observations"]["acceptedSourceIdentity"]="4605";self.synthetic.rebind(key)
        with self.assertRaisesRegex(ValueError,"reused by another case"):self.verify()

    def test_every_counter_handshake_and_owner_peer_phase_is_required(self):
        for key in self.synthetic.values:
            target = self.ipc / (key + ".properties"); original = target.read_bytes(); target.unlink()
            with self.subTest(witness=key), self.assertRaisesRegex(ValueError, "Missing"):
                self.verify()
            target.write_bytes(original)
        self.verify()

    def rehash_counter_clock_chain(self, index=46):
        prefix = f"case-{index:02d}-"
        values = self.synthetic.values
        sha = lambda phase: supervisor.digest(self.ipc / (prefix + phase + ".properties"))
        for phase in ("clock-rendezvous", "clock-ack", "clock-ready"):
            current = values[prefix + phase]
            current["clockInitialSha256"] = sha("clock-initial")
            if phase in ("clock-ack", "clock-ready"):
                current["clockRendezvousSha256"] = sha("clock-rendezvous")
            if phase == "clock-ready":
                current["clockAckSha256"] = sha("clock-ack")
            self.synthetic.flush(prefix + phase)
        values[prefix + "armed"]["clockReadySha256"] = sha("clock-ready"); self.synthetic.flush(prefix + "armed")
        accepted = values[prefix + "accepted"]
        accepted["clockReadySha256"] = sha("clock-ready"); accepted["armedSha256"] = sha("armed"); self.synthetic.flush(prefix + "accepted")
        values[prefix + "admission"]["admissionPeerArmedSha256"] = sha("armed")
        self.rehash_counter_admission(index, copy_fields=True)

    def test_clock_chain_and_render_barriers_reject_stale_or_rehashed_contradictions(self):
        mutations = [("clock-initial", "clockClientTick", "96"), ("clock-rendezvous", "clockInitialServerTick", "96"),
                     ("clock-rendezvous", "clockServerTick", "94"), ("clock-rendezvous", "clockRendezvousTick", "94"),
                     ("clock-ack", "clockClientTick", "96"), ("clock-ack", "clockRendezvousTick", "96"),
                     ("clock-ack", "clockInitialServerTick", "94"), ("clock-ready", "clockServerTick", "96"),
                     ("clock-ready", "clockClientTick", "94"), ("clock-ready", "clockInitialServerTick", "94")]
        for phase, field, bad in mutations:
            key = "case-46-" + phase; old = self.synthetic.values[key][field]
            self.synthetic.values[key][field] = bad; self.synthetic.flush(key); self.rehash_counter_clock_chain()
            with self.subTest(phase=phase, field=field), self.assertRaises(ValueError): self.verify()
            self.synthetic.values[key][field] = old; self.synthetic.flush(key); self.rehash_counter_clock_chain()
        for phase in ("clock-initial", "clock-rendezvous", "clock-ack", "clock-ready"):
            path = self.ipc / ("case-46-" + phase + ".properties"); data = path.read_bytes(); path.write_bytes(data + b"# changed immutable bytes\n")
            with self.subTest(phase=phase), self.assertRaises(ValueError): self.verify()
            path.write_bytes(data)
        for role in ("host", "peer"):
            for phase in supervisor.COUNTER_PHASES:
                key = f"{role}-case-46-{phase}-rendered"
                self.synthetic.values[key]["renderedBeforeReadback"] = "false"; self.synthetic.flush(key)
                with self.subTest(role=role, phase=phase), self.assertRaisesRegex(ValueError, "render barrier"): self.verify()
                self.synthetic.values[key]["renderedBeforeReadback"] = "true"; self.synthetic.flush(key)
        # Latest mutable pacing samples are not substitutes for the immutable chain.
        (self.ipc / "case-46-clock-latest.properties").write_text("clockServerTick=999999\n")
        self.verify()

    def test_handshake_identity_actions_admission_and_cameras_fail_closed(self):
        mutations = [("case-46-prepare", key, "wrong") for key in (*self.identity, "role", "pid", "case", "actorUuid", "actorEntity", "observerUuid", "observerEntity", "move", "windup", "recovery", "mode", "hand", "skin", "cameraNative")]
        mutations += [("case-46-armed", "cameraNative", "false"), ("case-46-armed", "skin", "slim"),
                      ("case-46-accepted", "payments", "2"), ("case-46-accepted", "paid", "0"), ("case-46-accepted", "paid", "NaN"),
                      ("case-46-accepted", "caughtTick", "101"), ("case-46-accepted", "caughtTick", "83"),
                      ("case-46-accepted", "caughtAttackerUuid", self.identity["hostUuid"]), ("case-46-accepted", "armedSha256", "f" * 64),
                      ("case-46-ready", "observerUuid", self.identity["hostUuid"]), ("case-46-seen", "mode", "articulated"),
                      ("case-46-passed", "hand", "LEFT"), ("host-case-46-WINDUP-observed", "acceptedReceiptSha256", "f" * 64),
                      ("peer-case-46-WINDUP-observed", "view", "fp"), ("host-case-46-WINDUP-observed", "pixelQualityReviewed", "true")]
        for key, field, bad in mutations:
            original = self.synthetic.values[key].copy(); self.synthetic.values[key][field] = bad; self.synthetic.flush(key)
            with self.subTest(witness=key, field=field, value=bad), self.assertRaises(ValueError):
                self.verify()
            self.synthetic.values[key] = original; self.synthetic.flush(key)
        self.verify()

    def test_each_real_release_target_outcome_field_must_agree_across_three_roles(self):
        changes = {"releaseTick": "107", "payments": "2", "completions": "2", "restUntil": "181", "counterTargetUuid": "00000000-0000-4000-8000-000000000004",
                   "counterTargetEntity": "34", "counterTargetHealthBefore": "199.0", "counterTargetHealth": "184.0", "directPrimaryHits": "2", "primaryHitTick": "105"}
        for phase in ("ready", "seen", "passed"):
            key = "case-46-" + phase
            for field, bad in changes.items():
                old = self.synthetic.values[key][field]; self.synthetic.values[key][field] = bad; self.synthetic.flush(key)
                with self.subTest(phase=phase, field=field), self.assertRaisesRegex(ValueError, "outcome disagree"): self.verify()
                self.synthetic.values[key][field] = old; self.synthetic.flush(key)
        # Even a consistently rewritten three-role ledger cannot replace gameplay measurements.
        for field, bad in (("releaseTick", "105"), ("payments", "0"), ("completions", "2"), ("directPrimaryHits", "0"), ("primaryHitTick", "105"),
                           ("counterTargetUuid", self.identity["peerUuid"]), ("counterTargetEntity", "11"), ("counterTargetEntity", "0"),
                           ("counterTargetHealthBefore", "201.0"), ("counterTargetHealth", "0"), ("counterTargetHealth", "200"), ("counterTargetHealth", "NaN")):
            old = self.synthetic.values["case-46-ready"][field]
            for phase in ("ready", "seen", "passed"):
                key = "case-46-" + phase; self.synthetic.values[key][field] = bad; self.synthetic.flush(key)
            with self.subTest(field=field, bad=bad), self.assertRaises(ValueError): self.verify()
            for phase in ("ready", "seen", "passed"):
                key = "case-46-" + phase; self.synthetic.values[key][field] = old; self.synthetic.flush(key)
        self.verify()

    def test_rehashed_metadata_and_actual_source_sequences_are_not_proof(self):
        key = "host-case-48-ACTIVE-observed"
        mutations = [(('schemaVersion',), 3), (('verified',), False), (('failures',), ["rejected"]), (('launchNonce',), "old"),
                     (('scopeCleanupVerified',), False), (('acceptanceObservedBeforeSource',), False), (('phaseBasis',), "requested_age"),
                     (('pixelQualityReviewed',), True), (('serverReleaseFrameCorrespondenceVerified',), True),
                     (('expected', 'armor'), True), (('expected', 'shell'), True), (('expected', 'owner'), 22),
                     (('expected', 'activation'), 99), (('expected', 'phase'), "RECOVERY"), (('expected', 'hand'), "LEFT"),
                     (('expected', 'skin'), "slim"), (('expected', 'name'), "foreign_capture"),
                     (('copy', 'observations', 'sourceHead'), "c" * 40), (('copy', 'observations', 'runIdentity'), "other-run"),
                     (('copy', 'observations', 'owner'), self.identity['peerUuid']), (('copy', 'observations', 'observerUuid'), self.identity['peerUuid']),
                     (('copy', 'observations', 'acceptedReceiptSha256'), "f" * 64), (('copy', 'observations', 'acceptedReadSequence'), "7"),
                     (('copy', 'observations', 'sourceFrameSequence'), "8"), (('copy', 'observations', 'actualSourceAge'), "7.0"),
                     (('copy', 'extractSequence'), 9), (('copy', 'renderSequence'), 10), (('copy', 'copySequence'), 9),
                     (('copy', 'observations', 'ordinaryHandAdmission'), "false"), (('copy', 'observations', 'handEquipping'), "true"),
                     (('copy', 'observations', 'connectedSkinTexture'), "foreign:skin"), (('copy', 'observations', 'submittedSkinMaterialIdentity'), "7")]
        for path, value in mutations:
            self.mutate_record(key, path, value)
        self.verify()

    def test_all_role_backend_passes_require_exact_union_and_full_numeric_geometry(self):
        for key in ("host-case-46-ACTIVE-observed", "peer-case-46-ACTIVE-observed", "host-case-48-ACTIVE-observed", "peer-case-48-ACTIVE-observed"):
            passes = self.synthetic.records[key]["copy"]["passes"]
            for malformed in ([], passes[:-1], passes + [passes[0]], [passes[0]] * len(passes)):
                self.mutate_record(key, ("copy", "passes"), malformed)
            for path, value in [(('kind',), 'label_only'), (('segmented',), not passes[0]['segmented']),
                                (('binding', 'source'), 99), (('binding', 'state'), 99), (('binding', 'model'), 99),
                                (('binding', 'skinMaterial'), 99), (('binding', 'hand'), 'LEFT'), (('binding', 'uuid'), self.identity['peerUuid']),
                                (('binding', 'palette', 'activation'), 99), (('binding', 'palette', 'move'), 19),
                                (('binding', 'palette', 'age'), 9), (('binding', 'palette', 'classic', 'left'), True),
                                (('binding', 'palette', 'classic', 'pose'), 'phase-label'), (('binding', 'palette', 'classic', 'master'), True),
                                (('geometry', 'operation'), 'label_only'), (('geometry', 'expected'), [0]), (('geometry', 'matched'), False),
                                (('geometry', 'actual', 0), .5), (('geometry', 'actual', 0), float('nan')), (('geometry', 'actual', 0), '0'),
                                (('geometry', 'maxError'), .1)]:
                self.mutate_record(key, ('copy', 'passes', 0, *path), value)
        self.mutate_record("host-case-46-ACTIVE-observed", ('copy','passes',0,'binding','palette','articulated'), {})
        self.mutate_record("peer-case-48-ACTIVE-observed", ('copy','passes',0,'binding','palette','articulated','pose'), 'WINDUP:'+'b'*64+':'+'c'*64)
        self.mutate_record("peer-case-48-FOLLOW-observed", ('copy','passes',0,'binding','palette','articulated','pose'), 'FOLLOW:'+'b'*64+':'+'c'*64)

    def test_rehashed_json_requires_typed_objects_and_no_duplicate_properties(self):
        key = "host-case-48-ACTIVE-observed"
        for path, value in [(('schemaVersion',), True), (('expected',), None), (('copy',), []),
                            (('copy', 'observations'), None), (('copy', 'passes', 0, 'binding'), []),
                            (('copy', 'passes', 0, 'binding', 'palette'), None),
                            (('copy', 'passes', 0, 'geometry'), None), (('image',), [])]:
            self.mutate_record(key, path, value)
        values = self.synthetic.values[key]
        path = self.base / values['role'] / values['receiptRelativePath']
        original = path.read_text()
        for text in ('[]', original[:-1] + ',"verified":true}'):
            path.write_text(text); values['receiptSha256'] = supervisor.digest(path); self.synthetic.flush(key)
            with self.assertRaises(ValueError): self.verify()
        self.synthetic.rebind(key)
        self.verify()

    def test_actual_phase_windows_have_exact_boundaries_for_both_counters(self):
        for move, windup, recovery in ((24, 6, 16), (25, 4, 14)):
            for age, phase in ((-1, "NONE"), (0, "WINDUP"), (windup - .001, "WINDUP"), (windup, "ACTIVE"),
                               (windup + .999, "ACTIVE"), (windup + 1, "FOLLOW"), (windup + recovery / 2 - .001, "FOLLOW"),
                               (windup + recovery / 2, "RECOVERY"), (windup + recovery - .001, "RECOVERY"), (windup + recovery, "NONE")):
                self.assertEqual(supervisor.counter_phase(move, age), phase)
            for age in ("NaN", "Infinity", True):
                with self.assertRaises(ValueError):
                    supervisor.counter_phase(move, age)
        key = "host-case-46-ACTIVE-observed"
        original = self.synthetic.values[key].copy()
        for age in ("5.999", "7", "22", "NaN"):
            self.synthetic.values[key]["actualSourceAge"] = age; self.synthetic.flush(key)
            with self.assertRaises(ValueError): self.verify()
        self.synthetic.values[key] = original; self.synthetic.flush(key)

    def test_image_bytes_callback_pixels_dimensions_and_evidence_paths_fail_closed(self):
        key = "host-case-46-WINDUP-observed"
        for path, value in [(('callbackPixels','sha256'), 'f'*64), (('image','decodedPixels','sha256'), 'f'*64),
                            (('image','pngSha256'), 'f'*64), (('image','pngBytes'), 1), (('copy','width'), 3)]:
            self.mutate_record(key, path, value)
        values = self.synthetic.values[key]; png = self.base/'host'/values['pngRelativePath']; original = png.read_bytes()
        png.write_bytes(original+b'changed')
        with self.assertRaisesRegex(ValueError, 'PNG bytes changed'): self.verify()
        png.write_bytes(original)
        receipt = self.base/'host'/values['receiptRelativePath']; data = receipt.read_bytes(); receipt.write_bytes(data+b' ')
        with self.assertRaisesRegex(ValueError, 'receipt bytes changed'): self.verify()
        receipt.write_bytes(data)
        for field in ('receiptRelativePath','pngRelativePath'):
            old = values[field]
            for bad in ('../host/'+old, '/tmp/foreign', ''):
                values[field] = bad; self.synthetic.flush(key)
                with self.assertRaises(ValueError): self.verify()
            values[field] = old; self.synthetic.flush(key)
        saved = png.with_suffix('.saved'); png.rename(saved); png.symlink_to(saved)
        with self.assertRaisesRegex(ValueError, 'Symlink'): self.verify()


class CounterProfileUnionTests(unittest.TestCase):
    """Aggregate acceptance is exercised only against explicitly synthetic local fixtures."""
    def setUp(self):
        temp = tempfile.TemporaryDirectory(); self.addCleanup(temp.cleanup)
        self.base = Path(temp.name).resolve(); self.paths = []; self.reports = []
        for index, (profile, (_, owner, skin)) in enumerate(supervisor.COUNTER_PROFILES.items(), 1):
            base = self.base/profile; ipc = base/'ipc'; ipc.mkdir(parents=True)
            identity = {'nonce': f'00000000-0000-4000-8000-{index:012d}', 'suite': supervisor.SUITE,
                        'sourceHead':'a'*40,'checkoutSha':'a'*40,'prHeadSha':'','descriptorSha256':'b'*64,
                        'hostUuid':owner,'peerUuid':supervisor.PROFILES['peer'][1],'runIdentity':f'synthetic-only-{index}'}
            pids = {'host':100+index*2,'peer':101+index*2}
            for filename, role in supervisor.TERMINALS.items():
                values={**identity,'role':role,'pid':str(pids[role]),'cases':','.join(EXPECTED_CASES)}
                (ipc/filename).write_text(''.join(key+'='+value+'\n' for key,value in values.items()))
            write_life_witnesses(ipc, identity, pids)
            SyntheticCounterEvidence(ipc, identity, pids, skin)
            report={**identity,'suite':supervisor.SUITE,'profile':profile,'status':'passed','cases':EXPECTED_CASES,'expectedSkin':skin,
                    'counterOwnerPeerGeometryVerified':True,'phaseBasis':supervisor.COUNTER_PHASE_BASIS,'counterPhaseCaptures':64,
                    'pixelQualityReviewed':False,'serverReleaseFrameCorrespondenceVerified':False,'timeoutSeconds':900,
                    'heapMiBPerJvm':2048,'seconds':1.,'processes':{role:{'pid':pid,'exit':0} for role,pid in pids.items()}}
            write_json(base/'result.json',report); self.paths.append(base); self.reports.append(report)

    def test_exact_original_width_union_reopens_both_full_runs(self):
        value=supervisor.validate_counter_profile_union(self.paths)
        self.assertEqual(value['cases'],EXPECTED_CASES)
        self.assertEqual(value['counterPhaseCaptures'],128)
        self.assertEqual(value['profiles'],['aura-wide','aura-slim'])
        self.assertFalse(value['pixelQualityReviewed'])
        self.assertFalse(value['serverReleaseFrameCorrespondenceVerified'])
        supervisor.validate_counter_profile_union(list(reversed(self.paths)))

    def test_union_refuses_shortened_full_profile_caps_and_late_rejection(self):
        for seconds in (1,60,899,901):
            old=self.reports[0]["timeoutSeconds"];self.reports[0]["timeoutSeconds"]=seconds
            write_json(self.paths[0]/"result.json",self.reports[0])
            with self.subTest(seconds=seconds),self.assertRaisesRegex(ValueError,"resource limits"):
                supervisor.validate_counter_profile_union(self.paths)
            self.reports[0]["timeoutSeconds"]=old;write_json(self.paths[0]/"result.json",self.reports[0])
        supervisor.validate_counter_profile_union(self.paths)
        marker=self.paths[1]/"ipc"/"peer-counter-render-failure.properties";marker.write_text("late swallowed duplicate")
        with self.assertRaisesRegex(ValueError,"rejection artifact"):supervisor.validate_counter_profile_union(self.paths)

    def test_missing_duplicate_foreign_and_partial_union_is_rejected(self):
        for paths in ([],self.paths[:1],self.paths+self.paths[:1],[self.paths[0]]*2):
            with self.subTest(paths=paths),self.assertRaises(ValueError): supervisor.validate_counter_profile_union(paths)
        for field,bad in [('profile','aura'),('status','failed'),('cases',ORIGINAL_46_CASES),('cases',EXPECTED_CASES+['EXTRA']),
                          ('expectedSkin','wide'),('sourceHead','c'*40),('checkoutSha','c'*40),('descriptorSha256','c'*64),
                          ('prHeadSha','d'*40),('nonce',self.reports[0]['nonce']),('runIdentity',self.reports[0]['runIdentity']),
                          ('counterOwnerPeerGeometryVerified',False),('counterPhaseCaptures',63),('pixelQualityReviewed',True),
                          ('serverReleaseFrameCorrespondenceVerified',True),('phaseBasis','requested_age'),('heapMiBPerJvm',4096),
                          ('timeoutSeconds',901),('seconds',901),('cleanupErrors',['failed']),('processes',{'host':{'pid':102,'exit':0}})]:
            report=copy.deepcopy(self.reports[1]);report[field]=bad;write_json(self.paths[1]/'result.json',report)
            with self.subTest(field=field,bad=bad),self.assertRaises(ValueError):supervisor.validate_counter_profile_union(self.paths)
        write_json(self.paths[1]/'result.json',self.reports[1])
        supervisor.validate_counter_profile_union(self.paths)

    def test_passed_manifest_cannot_replace_any_terminal_life_counter_or_png_evidence(self):
        base=self.paths[1]
        targets=[base/'ipc'/next(iter(supervisor.TERMINALS)),base/'ipc/case-42-armed.properties',
                 base/'ipc/case-46-accepted.properties',base/'ipc/peer-case-53-RECOVERY-observed.properties',
                 next((base/'peer').glob('*.png'))]
        for target in targets:
            original=target.read_bytes();target.unlink()
            with self.subTest(target=target),self.assertRaises(ValueError):supervisor.validate_counter_profile_union(self.paths)
            target.write_bytes(original)
        supervisor.validate_counter_profile_union(self.paths)


class FakeProcess:
    def __init__(self, pid, code=None, ignore_terminate=False):
        self.pid, self.returncode = pid, code
        self.ignore_terminate = ignore_terminate
        self.terminated = self.killed = False

    def poll(self):
        return self.returncode

    def terminate(self):
        self.terminated = True
        if not self.ignore_terminate:
            self.returncode = -15

    def kill(self):
        self.killed = True; self.returncode = -9

    def wait(self, timeout):
        if self.returncode is None:
            raise subprocess.TimeoutExpired("owned client", timeout)
        return self.returncode


class LifecycleTests(Fixture):
    def fake_run(self, processes, publish=False, failure=None):
        options = self.options()
        launched = []
        def spawn(argv, **kwargs):
            self.assertFalse(kwargs["shell"])
            self.assertEqual(argv[0], str(self.java)); self.assertEqual(len(argv), 2)
            self.assertEqual(kwargs["stdin"], subprocess.DEVNULL)
            kwargs["stdout"].write(b"owned diagnostic\n")
            proof = json.loads((options.output / "launch-proof.json").read_text())
            self.assertEqual(kwargs["env"]["WILDERCORD_COUNTER_RECEIPT_NONCE"], proof["nonce"])
            self.assertNotIn("WILDERCORD_MOON_RECEIPT_NONCE", kwargs["env"])
            if failure is not None and len(launched) == 1:
                raise failure
            process = processes[len(launched)]; launched.append(process)
            if len(launched) == 2 and publish:
                proof = json.loads((options.output / "launch-proof.json").read_text())
                identity = {key: proof[key] for key in supervisor.IDENTITY}
                for filename, role in supervisor.TERMINALS.items():
                    values = {**identity, "role": role, "pid": str(processes[0 if role == "host" else 1].pid), "cases": ",".join(CONTRACT["cases"])}
                    (options.output / "ipc" / filename).write_text("\n".join(key + "=" + value for key, value in values.items()))
                write_life_witnesses(options.output / "ipc", identity, {"host": processes[0].pid, "peer": processes[1].pid})
                SyntheticCounterEvidence(options.output / "ipc", identity, {"host": processes[0].pid, "peer": processes[1].pid})
            return process
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "true", "CI_MINECRAFT_EULA_ACCEPTED": "true", "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2"}), patch.object(supervisor, "ignored_output"), patch.object(supervisor, "source_head", return_value=self.head), patch.object(supervisor, "validate_launch", return_value="a" * 64), patch.object(supervisor.subprocess, "Popen", side_effect=spawn):
            return supervisor.run(options, self.root)

    def test_complete_pair_preserves_exact_ci_identity_and_both_logs(self):
        processes = [FakeProcess(1101, 0), FakeProcess(1102, 0)]
        report = self.fake_run(processes, publish=True)
        self.assertEqual(report["status"], "passed")
        self.assertEqual(report["cases"], EXPECTED_CASES)
        self.assertTrue(report["runIdentity"].startswith("123-2-"))
        self.assertTrue(report["counterOwnerPeerGeometryVerified"])
        self.assertEqual(report["counterPhaseCaptures"], 64)
        self.assertEqual(report["phaseBasis"], supervisor.COUNTER_PHASE_BASIS)
        self.assertFalse(report["pixelQualityReviewed"])
        self.assertFalse(report["serverReleaseFrameCorrespondenceVerified"])
        self.assertEqual(len(report["processes"]), 2)
        for role in ("host", "peer"):
            self.assertEqual((self.options().output / (role + ".log")).read_text(), "owned diagnostic\n")
        self.assertFalse(any(process.terminated for process in processes))

    def test_missing_witness_preserves_failure_result_and_logs(self):
        with self.assertRaises(ValueError):
            self.fake_run([FakeProcess(1101, 0), FakeProcess(1102, 0)])
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["status"], "failed")
        self.assertIn("witness", report["error"])
        self.assertTrue((self.options().output / "host.log").is_file())

    def test_preflight_failure_preserves_result_without_starting_java(self):
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "false", "CI_MINECRAFT_EULA_ACCEPTED": "false"}):
            with self.assertRaisesRegex(ValueError, "EULA"):
                supervisor.run(self.options(), self.root)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["stage"], "preflight")
        self.assertEqual(report["processes"], {})
        self.assertEqual(report["status"], "failed")

    def test_legacy_roster_fails_preflight_before_starting_java(self):
        for roster in (ORIGINAL_CASES, PRESERVED_CASES, PRIOR_CASES, ORIGINAL_46_CASES):
            with self.subTest(legacy_count=len(roster)):
                value = copy.deepcopy(CONTRACT)
                value["cases"] = roster
                value["expectedCount"] = value["limits"]["maxCases"] = len(roster)
                write_json(self.root / supervisor.CONTRACT, value)
                options = self.options()
                options.output = self.root / "build/native" / ("legacy-" + str(len(roster)))
                with patch.object(supervisor, "ignored_output"), patch.object(supervisor.subprocess, "Popen") as spawn, self.assertRaisesRegex(ValueError, "exact ordered 54-case roster"):
                    supervisor.run(options, self.root)
                spawn.assert_not_called()
                report = json.loads((options.output / "result.json").read_text())
                self.assertEqual(report["stage"], "preflight")
                self.assertEqual(report["processes"], {})
                self.assertEqual(report["status"], "failed")

    def test_peer_start_failure_stops_only_the_started_owned_host(self):
        host, unrelated = FakeProcess(1101), FakeProcess(9999)
        with self.assertRaises(OSError):
            self.fake_run([host, unrelated], failure=OSError("peer start failed"))
        self.assertTrue(host.terminated)
        self.assertFalse(unrelated.terminated)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(set(report["processes"]), {"host"})

    def test_nonzero_exit_stops_other_owned_child(self):
        host, peer = FakeProcess(1101, 3), FakeProcess(1102)
        with self.assertRaisesRegex(ValueError, "process failed"):
            self.fake_run([host, peer])
        self.assertFalse(host.terminated)
        self.assertTrue(peer.terminated)

    def test_deadline_kills_resistant_owned_process_and_retains_failure(self):
        host, peer, unrelated = FakeProcess(1101, ignore_terminate=True), FakeProcess(1102), FakeProcess(9999)
        with patch.object(supervisor.time, "sleep"), patch.object(supervisor.time, "monotonic", side_effect=[0, 0, 0, 2, 3]):
            with self.assertRaises(TimeoutError):
                self.fake_run([host, peer])
        self.assertTrue(host.terminated and host.killed)
        self.assertTrue(peer.terminated)
        self.assertFalse(unrelated.terminated or unrelated.killed)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertIn("TimeoutError", report["error"])

    def test_late_zero_exit_is_not_accepted_after_the_deadline(self):
        with patch.object(supervisor.time, "monotonic", side_effect=[0, 0, 0, 2, 3]):
            with self.assertRaises(TimeoutError):
                self.fake_run([FakeProcess(1101, 0), FakeProcess(1102, 0)], publish=True)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["status"], "failed")

    def test_expired_preflight_budget_never_starts_a_client(self):
        with patch.object(supervisor.time, "monotonic", side_effect=[0, 2, 3]):
            with self.assertRaises(TimeoutError):
                self.fake_run([])
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["processes"], {})

    def test_invalid_deadlines_or_reused_output_fail_before_spawning(self):
        options = self.options()
        for timeout in (0, -1, 901):
            options.timeout = timeout
            with patch.object(supervisor.subprocess, "Popen") as spawn, self.assertRaises(ValueError):
                supervisor.run(options, self.root)
            spawn.assert_not_called()
        options.timeout = 10
        options.output.mkdir(parents=True)
        with patch.object(supervisor.subprocess, "Popen") as spawn, self.assertRaisesRegex(ValueError, "fresh"):
            supervisor.run(options, self.root)
        spawn.assert_not_called()


if __name__ == "__main__":
    unittest.main()
