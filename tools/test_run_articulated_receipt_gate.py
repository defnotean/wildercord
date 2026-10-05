"""Synthetic launcher/file-contract tests. These never launch Minecraft or establish native coverage."""
import copy
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest import mock
import uuid

from PIL import Image

import run_articulated_receipt_gate as gate
from client_suites import EXIT_PREFIX, SELECTION_PREFIX
from verify_articulated_render_receipts import STAGES, image_evidence


class NativeReceiptGateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.game = self.root / "build/run/clientGameTest"
        self.output = self.root / "artifacts/receipts"
        self.receipt_root = self.game / "screenshots/articulated-shared-receipts"
        self.pid = 1234

    def tearDown(self):
        self.temp.cleanup()

    def generate(self, environment, *, nonce=None, phase_miss=False, count=120):
        run_id = str(uuid.uuid4())
        directory = self.receipt_root / run_id
        directory.mkdir(parents=True)
        png = self.game / "screenshots/template.png"
        Image.new("RGBA", (2, 2), (18, 52, 86, 255)).save(png)
        image_bytes = png.read_bytes()
        pixels = image_evidence(image_bytes)
        launch_nonce = environment[gate.NONCE_ENV] if nonce is None else nonce
        for sequence, trial in enumerate(sorted(gate.planned_trials())[:count], start=1):
            requested = trial.rsplit("_requested_", 1)[1].upper()
            phase = "ACTIVE" if phase_miss else requested
            first = "_hud_" in trial
            model_id = sequence + 1000
            state_id = sequence + 2000
            move = 1 if "_rising_break_" in trial else 2
            appearance = {"segmentedVisible": "true", "skinModel": "wide", "skinTexture": "synthetic-test-only", "rigWidth": "wide"}
            palette = {"activation": 42, "move": move, "phase": phase, "weight": 1, "leftHanded": "_left_" in trial,
                       "master": False, "localsSha256": "test-only"}
            base = {"kind": "view_submit" if first else "body_submit", "stateIdentity": state_id,
                    "modelIdentity": model_id, "owner": 7, "palette": palette, "attributes": appearance}
            deferred = copy.deepcopy(base)
            deferred["kind"] = "view_deferred" if first else "body_deferred"
            passes = [base, deferred]
            if first:
                item = copy.deepcopy(base)
                item["kind"] = "view_item"
                passes.append(item)
            image = self.game / "screenshots" / (trial + ".png")
            image.write_bytes(image_bytes)
            identity = {"runId": run_id, "captureSequence": sequence, "trial": trial, "requestedPhase": requested}
            record = {"schemaVersion": 1, "origin": "fabric_test_screenshot", "identity": identity,
                      "nativeLaunchNonce": launch_nonce, "verified": True, "failures": [], "stages": STAGES,
                      "renderedPhase": phase, "requestedPhaseObserved": requested == phase, "unpausedPhaseCoverage": requested == phase,
                      "nativePixelReviewRequired": True, "exactImpactPixelCoverage": "unverified", "callbackPixels": pixels,
                      "image": {"returnedPath": str(image), "relativeImagePath": str(image.relative_to(self.game)),
                                "pngBytes": len(image_bytes), "pngSha256": hashlib.sha256(image_bytes).hexdigest(), "decodedPixels": pixels},
                      "copy": {"identity": identity, "extractionSequence": 10000 + 3 * sequence,
                               "renderSequence": 10001 + 3 * sequence, "copySequence": 10002 + 3 * sequence,
                               "target": {"targetGeneration": 1, "textureGeneration": 2, "width": 2, "height": 2, "mipLevel": 0},
                               "observation": {"nativeLaunchNonce": launch_nonce, "firstPerson": str(first).lower(), "paused": "false", "frozen": "false",
                                               "ownerId": "7", "acceptedStartTick": "42", "acceptedMove": str(move), "renderTargetGeneration": "1", "renderTextureGeneration": "2"},
                               "passes": passes}}
            (directory / f"{sequence:06d}-receipt-1.json").write_text(json.dumps(record))
        png.unlink()
        return directory

    def run_case(self, write=None, exit_code=0, environment=None):
        def runner(command, repo, environment):
            self.assertEqual(command[2:4], ["--suite", "articulated"])
            self.assertEqual(Path(command[1]), self.root / "tools/run_client_ci.py")
            self.assertFalse((self.output / "association-report.json").exists(), "gate cannot verify before child exit")
            if write:
                write(environment)
            else:
                self.generate(environment)
            (self.root / "native.log").write_text(SELECTION_PREFIX + json.dumps({"kind": "suite", "name": "articulated"}) + "\n" + EXIT_PREFIX + str(exit_code) + "\n")
            return gate.NativeExit(exit_code, self.pid)
        return gate.run_gate(self.root, self.game, self.output, Path("native.log"), runner=runner,
                             commit_reader=lambda repo: "test-head", environment={} if environment is None else environment)

    def report(self):
        return json.loads((self.output / "association-report.json").read_text())

    def test_exact_planned_matrix(self):
        trials = gate.planned_trials()
        self.assertEqual(len(trials), 120)
        self.assertEqual(sum("_third_" in trial for trial in trials), 24)
        self.assertEqual(sum("_hud_" in trial for trial in trials), 96)
        self.assertIn("articulated_shared_driving_cut_hud_1920x810_gui3_netherite_right_requested_active", trials)
        self.assertIn("articulated_shared_rising_break_third_1280x720_gui3_skin_left_requested_recovery", trials)

    def test_completed_child_packages_every_receipt_without_pngs(self):
        self.assertEqual(self.run_case(), 0)
        report = self.report()
        self.assertTrue(report["gatePassed"])
        self.assertTrue(report["provenance"]["waitedForExit"])
        self.assertEqual(report["verifiedImages"], 120)
        self.assertEqual(report["package"]["recordCount"], 120)
        self.assertFalse(list(self.output.rglob("*.png")))
        for entry in report["package"]["records"]:
            copied = self.output / "receipts" / entry["name"]
            original = Path(report["package"]["sourceDirectory"]) / entry["name"]
            self.assertEqual(copied.read_bytes(), original.read_bytes())
        self.assertLessEqual(sum(path.stat().st_size for path in self.output.rglob("*") if path.is_file()), gate.MAX_METADATA_BYTES)
        self.assertEqual(len(list((self.game / "screenshots").glob("*.png"))), 120)

    def test_phase_misses_remain_explicit(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env, phase_miss=True)), 0)
        observations = self.report()["observations"]
        self.assertEqual(sum(not entry["requestedPhaseObserved"] for entry in observations), 80)
        self.assertEqual({entry["renderedPhase"] for entry in observations}, {"ACTIVE"})

    def test_native_failure_remains_nonzero_with_complete_receipts(self):
        self.assertEqual(self.run_case(exit_code=9), 1)
        self.assertFalse(self.report()["nativeSucceeded"])
        self.assertTrue(self.report()["package"]["complete"])

    def test_missing_records_rejected(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env, count=119)), 1)
        self.assertTrue(self.report()["package"]["complete"])
        self.assertEqual(self.report()["observedTrials"], 119)

    def test_missing_run_directory_rejected(self):
        self.assertEqual(self.run_case(lambda env: None), 1)
        self.assertTrue(any("exactly one" in error for error in self.report()["errors"]))

    def test_multiple_run_directories_rejected(self):
        def two(env):
            self.generate(env)
            self.generate(env, count=1)
        self.assertEqual(self.run_case(two), 1)
        self.assertFalse(self.report()["package"]["complete"])

    def test_preexisting_run_rejected_before_launch(self):
        self.generate({gate.NONCE_ENV: "old"}, count=1)
        never = mock.Mock(side_effect=AssertionError("must not launch with stale evidence"))
        result = gate.run_gate(self.root, self.game, self.output, Path("native.log"), runner=never,
                               commit_reader=lambda repo: "test-head", environment={})
        self.assertEqual(result, 1)
        never.assert_not_called()
        self.assertTrue(any("preexisting" in error for error in self.report()["errors"]))

    def test_copied_provenance_rejected_despite_fresh_times(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env, nonce="copied-old-launch")), 1)
        self.assertTrue(self.report()["package"]["complete"])
        self.assertTrue(any("this native child launch" in error for error in self.report()["errors"]))

    def test_unwrapped_provenance_cannot_claim_wrapped_acceptance(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env, nonce="unbound")), 1)
        self.assertFalse(self.report()["associationVerified"])

    def test_stale_record_times_rejected(self):
        def stale(env):
            directory = self.generate(env)
            path = next(directory.glob("*.json"))
            os.utime(path, ns=(1, 1))
        self.assertEqual(self.run_case(stale), 1)
        self.assertTrue(any("stale" in error for error in self.report()["errors"]))

    def test_late_failed_record_is_packaged_and_invalidates_capture(self):
        def late(env):
            directory = self.generate(env)
            source = next(directory.glob("*.json"))
            record = json.loads(source.read_text())
            record.update(verified=False, failures=["late_callback"])
            (directory / "000001-late-2.json").write_text(json.dumps(record))
        self.assertEqual(self.run_case(late), 1)
        self.assertEqual(self.report()["package"]["recordCount"], 121)

    def test_failed_before_copy_record_is_preserved(self):
        def failed(env):
            directory = self.generate(env)
            path = next(directory.glob("*.json"))
            record = json.loads(path.read_text())
            record.update(verified=False, failures=["original_failure"], copy=None, stages=["requested"])
            path.write_text(json.dumps(record))
        self.assertEqual(self.run_case(failed), 1)
        self.assertTrue(self.report()["package"]["complete"])

    def test_oversized_metadata_fails_without_truncating_source(self):
        def oversized(env):
            directory = self.generate(env)
            path = next(directory.glob("*.json"))
            path.write_bytes(b" " * (gate.MAX_RECORD_BYTES + 1))
        self.assertEqual(self.run_case(oversized), 1)
        self.assertFalse(self.report()["package"]["complete"])
        self.assertTrue(any(path.stat().st_size > gate.MAX_RECORD_BYTES for path in self.receipt_root.rglob("*.json")))

    def test_conflicting_ci_commit_rejected_before_launch(self):
        self.assertEqual(self.run_case(environment={"GITHUB_SHA": "old-head"}), 1)
        self.assertNotIn("waitedForExit", self.report()["provenance"])

    def test_test_module_import_has_no_subprocess_side_effect(self):
        path = Path(__file__).with_name("test_articulated_render_receipts.py")
        spec = importlib.util.spec_from_file_location("receipt_import_check", path)
        module = importlib.util.module_from_spec(spec)
        with mock.patch("subprocess.run", side_effect=AssertionError("import launched subprocess")):
            spec.loader.exec_module(module)
        self.assertTrue(callable(module.run_pure_checks))

    def test_killed_launcher_does_not_prove_native_exit(self):
        self.assertEqual(self.run_case(exit_code=-15), 1)
        self.assertNotIn("nativeProcessExitConfirmed", self.report()["provenance"])
        self.assertFalse(self.report()["package"]["complete"])

    def test_malformed_container_becomes_bounded_failure_report(self):
        def malformed(env):
            directory = self.generate(env)
            path = next(directory.glob("*.json"))
            record = json.loads(path.read_text())
            record["copy"]["observation"] = []
            path.write_text(json.dumps(record))
        self.assertEqual(self.run_case(malformed), 1)
        self.assertTrue(self.report()["package"]["complete"])

    def test_run_directory_added_during_verification_is_rejected(self):
        original = gate.verify
        def changed(*args, **kwargs):
            result = original(*args, **kwargs)
            (self.receipt_root / str(uuid.uuid4())).mkdir()
            return result
        with mock.patch.object(gate, "verify", changed):
            self.assertEqual(self.run_case(), 1)
        self.assertTrue(any("directories changed" in error for error in self.report()["errors"]))

    def test_log_cannot_expand_metadata_artifact_or_overwrite_native_evidence(self):
        for log in (self.output / "huge-native.log", self.game / "screenshots/actual.png"):
            with self.assertRaisesRegex(ValueError, "log must be separate"):
                gate.run_gate(self.root, self.game, self.output, log)

    def test_existing_log_is_preserved(self):
        log = self.root / "native.log"
        log.write_text("prior full native evidence")
        with self.assertRaisesRegex(ValueError, "log already exists"):
            gate.run_gate(self.root, self.game, self.output, log)
        self.assertEqual(log.read_text(), "prior full native evidence")

    def test_output_cannot_overlap_original_native_evidence(self):
        with self.assertRaisesRegex(ValueError, "metadata output must be separate"):
            gate.run_gate(self.root, self.game, self.game / "screenshots/bounded", self.root / "native.log")

    def test_oversized_summary_keeps_bounded_failure_report_and_full_records(self):
        def oversized(env):
            directory = self.generate(env)
            for path in directory.glob("*.json"):
                record = json.loads(path.read_text())
                for entry in record["copy"]["passes"]:
                    entry["attributes"]["skinTexture"] = "x" * 14000
                path.write_text(json.dumps(record))
        self.assertEqual(self.run_case(oversized), 1)
        report = self.report()
        self.assertFalse(report["gatePassed"])
        self.assertTrue(report["package"]["complete"])
        self.assertEqual(report["package"]["recordCount"], 120)
        self.assertLessEqual((self.output / "association-report.json").stat().st_size, gate.MAX_REPORT_BYTES)

    def test_missing_terminal_marker_never_verifies_or_packages_live_child(self):
        def runner(command, repo, environment):
            self.generate(environment)
            (self.root / "native.log").write_text(SELECTION_PREFIX + json.dumps({"kind": "suite", "name": "articulated"}) + "\n")
            return gate.NativeExit(0, self.pid)
        with mock.patch.object(gate, "verify", side_effect=AssertionError("verification before confirmed native exit")):
            result = gate.run_gate(self.root, self.game, self.output, Path("native.log"), runner=runner,
                                   commit_reader=lambda repo: "test-head", environment={})
        self.assertEqual(result, 1)
        self.assertFalse(self.report()["package"]["complete"])

    def test_oversized_error_details_cannot_remove_failure_report(self):
        def oversized(env):
            directory = self.generate(env)
            for number, path in enumerate(directory.glob("*.json")):
                record = json.loads(path.read_text())
                trial = "articulated_shared_" + "x" * 10000 + str(number) + "_requested_active"
                record["identity"]["trial"] = trial
                record["copy"]["identity"]["trial"] = trial
                path.write_text(json.dumps(record))
        self.assertEqual(self.run_case(oversized), 1)
        report = self.report()
        self.assertFalse(report["gatePassed"])
        self.assertTrue(report["package"]["complete"])
        self.assertTrue(report["errorDetailsBounded"])
        self.assertLessEqual((self.output / "association-report.json").stat().st_size, gate.MAX_REPORT_BYTES)


if __name__ == "__main__":
    unittest.main()
