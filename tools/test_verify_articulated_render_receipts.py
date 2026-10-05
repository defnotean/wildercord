import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from PIL import Image

from verify_articulated_render_receipts import STAGES, image_evidence, verify


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


if __name__ == "__main__":
    unittest.main()
