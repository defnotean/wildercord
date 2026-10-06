import copy
import hashlib
import json
from pathlib import Path
import tempfile
import struct
import uuid
import zlib
from unittest import mock
import unittest

from PIL import Image

import verify_articulated_render_receipts as verifier
from verify_articulated_render_receipts import STAGES, image_evidence, verify


def opening_fixture(source, trial):
    """Synthetic schema-2 artifact only; this helper never establishes native coverage."""
    record = copy.deepcopy(source)
    art, camera, hand, armor, shell, disabled, requested = verifier.OPENING_TRIAL.fullmatch(trial).groups()
    first, fallback = camera == "first", bool(disabled)
    move = 3 if art == "kindling_draw" else 4
    phase = requested.upper()
    identity = record["identity"]
    identity.update(trial=trial, requestedPhase=phase)
    record["copy"]["identity"] = copy.deepcopy(identity)
    owner = "0fde6de4-aa93-4e34-b9bf-abbf393da2ad"
    backend = "full_fallback" if fallback else "segmented"
    observation = record["copy"]["observation"]
    observation.update(firstPerson=str(first).lower(), acceptedEntity="7", ownerUuid=owner,
                       expectedOwnerUuid=owner, expectedOwnerId="7", acceptedMove=str(move), expectedMove=str(move),
                       expectedStartTick="42", openingArmed="true", expectedBackend=backend,
                       shellAdapterEnabled=str(not fallback).lower())
    base = copy.deepcopy(record["copy"]["passes"][0])
    base.update(kind="view_fallback_submit" if first and fallback else "view_submit" if first else "body_submit",
                stateIdentity=100, modelIdentity=200)
    base["palette"].update(move=move, phase=phase, leftHanded=hand == "left", localsSha256="a" * 64)
    attrs = base["attributes"]
    attrs.update(ownerUuid=owner, avatarStateIdentity="100", extractedStateIdentity="100", postHitStopExtractionMatched="true",
                 mainArm=hand.upper(), mainHand="minecraft:diamond_sword:foil=false", shellGlowPresent=str(shell == "funded_shell").lower(),
                 shellAdapterEnabled=str(not fallback).lower(), rawAcceptedPhase=phase, rawActivation="42", rawMove=str(move),
                 rawLeftHanded=str(hand == "left").lower(), rawPaletteSha256="a" * 64,
                 fallbackFrameCompatible=str(not fallback).lower(),
                 segmentedVisible=str(not fallback).lower(), rigidVisible=str(fallback).lower(), segmentedRootVisible=str(not fallback).lower())
    for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("legs", "leggings"), ("feet", "boots")):
        attrs[slot] = f"minecraft:netherite_{piece}:foil=true" if armor == "netherite" else "empty"
    passes = [base]
    if first and fallback:
        attrs.update(itemStateIdentity="300", fallbackFrameCompatible="false")
    else:
        deferred = copy.deepcopy(base)
        deferred["kind"] = "view_deferred" if first else "body_deferred"
        deferred["attributes"]["rigPaletteSha256" if first else "actualPaletteSha256"] = "b" * 64
        passes.append(deferred)
    if first:
        item = copy.deepcopy(base)
        item["kind"] = "view_fallback_item" if fallback else "view_item"
        item["attributes"]["itemStateIdentity"] = "300"
        passes.append(item)
    record["copy"]["passes"] = passes
    record.update(schemaVersion=2, expectedBackend=backend, rawAcceptedPhase=phase, rawRequestedPhaseObserved=True,
                  renderedPhase="FALLBACK" if fallback else phase, requestedPhaseObserved=not fallback,
                  unpausedPhaseCoverage=not fallback and observation["paused"] == "false" and observation["frozen"] == "false")
    return record


class ReceiptArtifactTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.receipts = self.root / "receipts"
        self.receipts.mkdir()
        self.image_path = self.root / "articulated_shared_trial_requested_windup.png"
        image = Image.new("RGBA", (2, 1))
        image.putdata([(0x12, 0x34, 0x56, 255), (0xAB, 0xCD, 0xEF, 255)])
        image.save(self.image_path)
        data = self.image_path.read_bytes()
        pixels = image_evidence(data)
        identity = {"runId": "run-a", "captureSequence": 1, "trial": self.image_path.stem, "requestedPhase": "WINDUP"}
        self.record = {"schemaVersion": 1, "origin": "fabric_test_screenshot", "identity": identity, "verified": True, "failures": [], "stages": STAGES,
                       "renderedPhase": "ACTIVE", "requestedPhaseObserved": False, "unpausedPhaseCoverage": False,
                       "exactImpactPixelCoverage": "unverified", "nativePixelReviewRequired": True,
                       "image": {"relativeImagePath": self.image_path.name, "pngBytes": len(data),
                                 "pngSha256": hashlib.sha256(data).hexdigest(), "decodedPixels": pixels}, "callbackPixels": pixels,
                       "copy": {"identity": identity, "extractionSequence": 10, "renderSequence": 20, "copySequence": 30,
                                "target": {"targetGeneration": 1, "textureGeneration": 2, "width": 2, "height": 1, "mipLevel": 0},
                                "observation": {"firstPerson": "false", "paused": "false", "frozen": "false", "ownerId": "7", "acceptedStartTick": "42", "acceptedMove": "1", "renderTargetGeneration": "1", "renderTextureGeneration": "2"},
                                "passes": [{"kind": "body_deferred", "stateIdentity": 100, "modelIdentity": 200, "owner": 7,
                                "palette": {"phase": "ACTIVE", "activation": 42, "move": 1, "weight": 1, "leftHanded": False, "master": False, "localsSha256": "observed-palette"},
                                "attributes": {"segmentedVisible": "true", "skinModel": "wide", "skinTexture": "actual-texture", "rigWidth": "wide"}}]}}
        submission = copy.deepcopy(self.record["copy"]["passes"][0])
        submission["kind"] = "body_submit"
        self.record["copy"]["passes"].insert(0, submission)
        self.write("000001-receipt-1.json", self.record)

    def tearDown(self):
        self.temp.cleanup()

    def write(self, name, record):
        (self.receipts / name).write_text(json.dumps(record))

    def result(self, count=1, expected=None):
        return verify(self.receipts, self.root, count, expected)

    def test_preserves_phase_miss_and_observed_skin(self):
        report = self.result()
        self.assertTrue(report["associationVerified"])
        self.assertEqual(report["observations"][0]["renderedPhase"], "ACTIVE")
        self.assertFalse(report["observations"][0]["requestedPhaseObserved"])
        self.assertEqual({x["skinModel"] for x in report["observations"]}, {"wide"})

    def test_late_failure_revokes_earlier_verified_file(self):
        late = copy.deepcopy(self.record)
        late.update(verified=False, failures=["late_callback"])
        self.write("000001-late-2.json", late)
        self.assertFalse(self.result()["associationVerified"])
        self.assertEqual(self.result()["verifiedImages"], 0)

    def test_expected_count_rejects_missing_receipt(self):
        self.assertFalse(self.result(2)["associationVerified"])

    def test_expected_manifest_rejects_wrong_same_count(self):
        self.assertFalse(self.result(expected={"articulated_shared_other"})["associationVerified"])

    def test_repeated_trial_cannot_hide_retry(self):
        other = copy.deepcopy(self.record)
        other["identity"]["captureSequence"] = 2
        other["copy"]["identity"]["captureSequence"] = 2
        self.write("000002-receipt-1.json", other)
        self.assertFalse(self.result()["associationVerified"])

    def test_reused_filename_rejects_original_receipt(self):
        Image.new("RGBA", (2, 1), (9, 8, 7, 255)).save(self.image_path)
        self.assertFalse(self.result()["associationVerified"])

    def test_missing_image_does_not_accept_hash_record(self):
        self.image_path.unlink()
        self.assertFalse(self.result()["associationVerified"])

    def test_callback_pixel_hash_must_match(self):
        self.record["callbackPixels"]["sha256"] = "bad"
        self.write("000001-receipt-1.json", self.record)
        self.assertFalse(self.result()["associationVerified"])

    def test_path_escape_rejected(self):
        self.record["image"]["relativeImagePath"] = "../outside.png"
        self.write("000001-receipt-1.json", self.record)
        self.assertFalse(self.result()["associationVerified"])

    def test_no_receipts_rejected(self):
        (self.receipts / "000001-receipt-1.json").unlink()
        self.assertFalse(self.result()["associationVerified"])

    def test_false_requested_phase_claim_rejected(self):
        self.record["requestedPhaseObserved"] = True
        self.write("000001-receipt-1.json", self.record)
        self.assertFalse(self.result()["associationVerified"])

    def test_false_unpaused_claim_rejected(self):
        self.record["identity"]["requestedPhase"] = "ACTIVE"
        self.record["identity"]["trial"] = "articulated_shared_trial_requested_active"
        self.record["requestedPhaseObserved"] = True
        self.record["unpausedPhaseCoverage"] = True
        self.record["copy"]["observation"].update(paused="true", frozen="true")
        self.write("000001-receipt-1.json", self.record)
        result = self.result()
        self.assertFalse(result["associationVerified"])
        self.assertTrue(any("phase/pause coverage" in error for error in result["errors"]), result["errors"])

    def test_wrong_owner_and_activation_rejected(self):
        self.record["copy"]["passes"][1]["owner"] = 99
        self.record["copy"]["passes"][1]["palette"]["activation"] = 900
        self.write("000001-receipt-1.json", self.record)
        self.assertFalse(self.result()["associationVerified"])

    def test_missing_actual_submission_rejected(self):
        self.record["copy"]["passes"].pop(0)
        self.write("000001-receipt-1.json", self.record)
        self.assertFalse(self.result()["associationVerified"])

    def test_missing_stage_or_copy_identity_rejected(self):
        self.record["copy"]["copySequence"] = 0
        self.record["stages"] = ["receipt-verified"]
        self.write("000001-receipt-1.json", self.record)
        self.assertFalse(self.result()["associationVerified"])

    def test_changed_target_generation_rejected(self):
        self.record["copy"]["target"]["textureGeneration"] = 9
        self.write("000001-receipt-1.json", self.record)
        self.assertFalse(self.result()["associationVerified"])

    def test_one_png_cannot_supply_two_capture_tokens(self):
        other = copy.deepcopy(self.record)
        other["identity"]["captureSequence"] = 2
        other["copy"]["identity"]["captureSequence"] = 2
        other["identity"]["trial"] = "articulated_shared_another_requested_windup"
        other["copy"]["identity"]["trial"] = "articulated_shared_another_requested_windup"
        self.write("000002-receipt-1.json", other)
        self.assertFalse(self.result(2)["associationVerified"])


class OpeningReceiptArtifactTests(unittest.TestCase):
    write = ReceiptArtifactTests.write
    tearDown = ReceiptArtifactTests.tearDown

    def setUp(self):
        ReceiptArtifactTests.setUp(self)
        self.source = copy.deepcopy(self.record)
        self.source["identity"]["runId"] = str(uuid.uuid4())
        self.make()

    def make(self, first=False, fallback=False):
        trial = "articulated_opening_style_kindling_draw_" + ("first" if first else "third") + "_right_netherite_funded_shell" + ("_adapter_disabled" if fallback else "") + "_requested_active"
        self.record = opening_fixture(self.source, trial)
        self.write("000001-receipt-1.json", self.record)

    def result(self):
        self.write("000001-receipt-1.json", self.record)
        return verify(self.receipts, self.root, 1, profile="opening")

    def test_both_backends_and_full_fallbacks_preserve_honest_phase_fields(self):
        for first in (False, True):
            for fallback in (False, True):
                with self.subTest(first=first, fallback=fallback):
                    self.make(first, fallback)
                    result = self.result()
                    self.assertTrue(result["associationVerified"], result["errors"])
                    observation = result["observations"][0]
                    self.assertEqual(observation["renderedPhase"], "FALLBACK" if fallback else "ACTIVE")
                    self.assertEqual(observation["unpausedPhaseCoverage"], not fallback)
                    self.assertEqual(observation["fallbackPhaseCoverage"], fallback)
                    self.assertEqual(observation["rawAcceptedPhase"], "ACTIVE")

    def test_schema_profiles_cannot_cross_satisfy_coverage(self):
        self.assertFalse(verify(self.receipts, self.root, 1)["associationVerified"])
        self.record["schemaVersion"] = 1
        self.assertFalse(self.result()["associationVerified"])

    def test_first_view_locals_can_differ_from_exact_raw_body_extraction(self):
        self.make(first=True)
        for entry in self.record["copy"]["passes"]:
            entry["palette"]["localsSha256"] = "d" * 64
        self.assertTrue(self.result()["associationVerified"])

    def test_opening_identity_equipment_shell_and_backend_attacks(self):
        attacks = [
            ("owner", lambda r: r["copy"]["observation"].update(expectedOwnerUuid=str(uuid.uuid4()))),
            ("entity", lambda r: r["copy"]["observation"].update(acceptedEntity="88")),
            ("move", lambda r: r["copy"]["observation"].update(expectedMove="4")),
            ("replacement", lambda r: r["copy"]["observation"].update(expectedStartTick="43")),
            ("unarmed", lambda r: r["copy"]["observation"].update(openingArmed="false")),
            ("camera", lambda r: r["copy"]["observation"].update(firstPerson="true")),
            ("missing extraction", lambda r: r["copy"]["passes"][1]["attributes"].update(extractedStateIdentity="99")),
            ("wrong raw pose", lambda r: r["copy"]["passes"][1]["attributes"].update(rawPaletteSha256="c" * 64)),
            ("missing actual pose", lambda r: r["copy"]["passes"][1]["attributes"].pop("actualPaletteSha256")),
            ("hand", lambda r: r["copy"]["passes"][1]["palette"].update(leftHanded=True)),
            ("equipment", lambda r: [e["attributes"].update(chest="empty") for e in r["copy"]["passes"]]),
            ("shell", lambda r: [e["attributes"].update(shellGlowPresent="false") for e in r["copy"]["passes"]]),
            ("backend", lambda r: r.update(expectedBackend="full_fallback")),
            ("unknown pass", lambda r: r["copy"]["passes"][1].update(kind="invented_deferred")),
            ("phase lie", lambda r: r.update(rawAcceptedPhase="WINDUP")),
            ("forged freeze", lambda r: r["copy"]["observation"].update(frozen="true")),
        ]
        for label, attack in attacks:
            with self.subTest(attack=label):
                self.make()
                attack(self.record)
                self.assertFalse(self.result()["associationVerified"])

    def test_fallback_cannot_claim_articulated_phase_or_keep_partial_body(self):
        for attrs in ({"rigidVisible": "false"}, {"segmentedRootVisible": "true"}, {"segmentedVisible": "true"}):
            self.make(fallback=True)
            self.record["copy"]["passes"][1]["attributes"].update(attrs)
            self.assertFalse(self.result()["associationVerified"])
        self.make(fallback=True)
        self.record.update(renderedPhase="ACTIVE", requestedPhaseObserved=True, unpausedPhaseCoverage=True)
        self.assertFalse(self.result()["associationVerified"])

    def test_fallback_view_requires_exact_actual_item_and_no_invented_arm(self):
        for attack in (
            lambda r: r["copy"]["passes"][1]["attributes"].update(itemStateIdentity="999"),
            lambda r: r["copy"]["passes"].pop(),
            lambda r: r["copy"]["passes"][1].update(kind="view_deferred"),
            lambda r: r["copy"]["passes"][0]["attributes"].update(fallbackFrameCompatible="true"),
        ):
            self.make(first=True, fallback=True)
            attack(self.record)
            self.assertFalse(self.result()["associationVerified"])

    def test_valid_association_keeps_raw_phase_miss_and_paused_fallback_miss(self):
        self.make(fallback=True)
        for entry in self.record["copy"]["passes"]:
            entry["palette"]["phase"] = "RECOVERY"
            entry["attributes"]["rawAcceptedPhase"] = "RECOVERY"
        self.record.update(rawAcceptedPhase="RECOVERY", rawRequestedPhaseObserved=False)
        result = self.result()
        self.assertTrue(result["associationVerified"], result["errors"])
        self.assertFalse(result["observations"][0]["fallbackPhaseCoverage"])
        self.make(first=True, fallback=True)
        self.record["copy"]["observation"]["paused"] = "true"
        self.assertFalse(self.result()["observations"][0]["fallbackPhaseCoverage"])

    def test_observation_binds_exact_original_image_and_sidecar_paths_and_hashes(self):
        result = self.result()["observations"][0]
        self.assertEqual(result["relativeImagePath"], self.image_path.name)
        self.assertEqual(result["pngSha256"], hashlib.sha256(self.image_path.read_bytes()).hexdigest())
        self.assertEqual(result["receiptRelativePath"], "000001-receipt-1.json")
        self.assertEqual(result["receiptSha256"], hashlib.sha256((self.receipts / "000001-receipt-1.json").read_bytes()).hexdigest())

    def test_external_packaged_receipts_verify_against_original_game_images(self):
        with tempfile.TemporaryDirectory() as external_directory:
            external = Path(external_directory)
            (external / "000001-receipt-1.json").write_bytes((self.receipts / "000001-receipt-1.json").read_bytes())
            result = verify(external, self.root, 1, profile="opening")
            self.assertTrue(result["associationVerified"], result["errors"])
            self.assertEqual(result["receiptDirectory"], str(external))

    def test_conflicting_repeated_deferred_geometry_hashes_rejected(self):
        repeated = copy.deepcopy(self.record["copy"]["passes"][1])
        self.record["copy"]["passes"].append(repeated)
        self.assertTrue(self.result()["associationVerified"])
        repeated["attributes"]["actualPaletteSha256"] = "c" * 64
        self.assertFalse(self.result()["associationVerified"])

    def test_duplicate_item_and_late_terminal_records_are_rejected(self):
        self.make(first=True, fallback=True)
        self.record["copy"]["passes"].append(copy.deepcopy(self.record["copy"]["passes"][1]))
        self.assertFalse(self.result()["associationVerified"])
        self.make()
        late = copy.deepcopy(self.record)
        late.update(verified=False, failures=["late_callback"])
        self.write("000001-late-2.json", late)
        self.assertFalse(self.result()["associationVerified"])


class PngIntegrityTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / "image.png"
        Image.new("RGBA", (2, 2), (20, 40, 60, 255)).save(self.path)
        self.data = self.path.read_bytes()

    def tearDown(self):
        self.temp.cleanup()

    def test_corrupt_idat_crc_rejected_even_when_png_pixels_decode(self):
        data = bytearray(self.data)
        at = data.index(b"IDAT")
        length = struct.unpack_from(">I", data, at - 4)[0]
        data[at + 4 + length] ^= 1
        with self.assertRaisesRegex(ValueError, "CRC"):
            image_evidence(bytes(data))

    def test_all_ancillary_crc_and_trailing_bytes_are_checked(self):
        chunk = struct.pack(">I", 1) + b"tEXt" + b"x" + b"\0" * 4
        for data in (self.data[:-12] + chunk + self.data[-12:], self.data + b"extra", self.data[:-1]):
            with self.assertRaises(ValueError):
                image_evidence(data)

    def test_dimensions_and_byte_budget_checked_before_decode(self):
        data = bytearray(self.data)
        struct.pack_into(">II", data, 16, 100000, 100000)
        struct.pack_into(">I", data, 29, zlib.crc32(data[12:29]))
        with mock.patch.object(verifier.Image, "open", side_effect=AssertionError("decoder must not run")):
            with self.assertRaisesRegex(ValueError, "dimensions"):
                image_evidence(bytes(data))
            with mock.patch.object(verifier, "MAX_PNG_BYTES", len(self.data) - 1):
                with self.assertRaisesRegex(ValueError, "bounded"):
                    image_evidence(self.data)


if __name__ == "__main__":
    unittest.main()
