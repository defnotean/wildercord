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
from test_verify_articulated_render_receipts import opening_fixture


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

    def generate(self, environment, *, nonce=None, phase_miss=False, active_miss=False, paused=False, frozen=False, count=None, profile="shared", run_id=None):
        if run_id is None:
            run_id = next(self.receipt_root.iterdir()).name if profile == "opening" and self.receipt_root.exists() else str(uuid.uuid4())
        root = self.receipt_root if profile == "shared" else self.game / "screenshots/articulated-opening-receipts"
        directory = root / run_id
        directory.mkdir(parents=True)
        png = self.game / "screenshots/template.png"
        Image.new("RGBA", (2, 2), (18, 52, 86, 255)).save(png)
        image_bytes = png.read_bytes()
        pixels = image_evidence(image_bytes)
        launch_nonce = environment[gate.NONCE_ENV] if nonce is None else nonce
        trials = gate.planned_trials() if profile == "shared" else gate.planned_opening_trials()
        for sequence, trial in enumerate(sorted(trials)[:count], start=1 if profile == "shared" else 121):
            requested = trial.rsplit("_requested_", 1)[1].upper()
            phase = "ACTIVE" if phase_miss else requested
            if active_miss and requested == "ACTIVE":
                phase = "RECOVERY"
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
                      "renderedPhase": phase, "requestedPhaseObserved": requested == phase, "unpausedPhaseCoverage": requested == phase and not paused and not frozen,
                      "nativePixelReviewRequired": True, "exactImpactPixelCoverage": "unverified", "callbackPixels": pixels,
                      "image": {"returnedPath": str(image), "relativeImagePath": str(image.relative_to(self.game)),
                                "pngBytes": len(image_bytes), "pngSha256": hashlib.sha256(image_bytes).hexdigest(), "decodedPixels": pixels},
                      "copy": {"identity": identity, "extractionSequence": 10000 + 3 * sequence,
                               "renderSequence": 10001 + 3 * sequence, "copySequence": 10002 + 3 * sequence,
                               "target": {"targetGeneration": 1, "textureGeneration": 2, "width": 2, "height": 2, "mipLevel": 0},
                               "observation": {"nativeLaunchNonce": launch_nonce, "firstPerson": str(first).lower(), "paused": str(paused).lower(), "frozen": str(frozen).lower(),
                                               "ownerId": "7", "acceptedStartTick": "42", "acceptedMove": str(move), "renderTargetGeneration": "1", "renderTextureGeneration": "2"},
                               "passes": passes}}
            if profile == "opening":
                record = opening_fixture(record, trial)
                if phase_miss or (active_miss and requested == "ACTIVE"):
                    for entry in record["copy"]["passes"]:
                        entry["palette"]["phase"] = phase
                        entry["attributes"]["rawAcceptedPhase"] = phase
                    fallback = record["expectedBackend"] == "full_fallback"
                    record.update(rawAcceptedPhase=phase, rawRequestedPhaseObserved=phase == requested,
                                  renderedPhase="FALLBACK" if fallback else phase,
                                  requestedPhaseObserved=not fallback and phase == requested,
                                  unpausedPhaseCoverage=not fallback and phase == requested and not paused and not frozen)
            (directory / f"{sequence:06d}-receipt-1.json").write_text(json.dumps(record))
        png.unlink()
        return directory

    def run_case(self, write=None, exit_code=0, environment=None, include_opening=False):
        def runner(command, repo, environment):
            self.assertEqual(command[2:4], ["--suite", "articulated"])
            self.assertEqual(Path(command[1]), self.root / "tools/run_client_ci.py")
            self.assertFalse((self.output / "association-report.json").exists(), "gate cannot verify before child exit")
            if write:
                write(environment)
            else:
                self.generate(environment)
                if include_opening:
                    self.generate(environment, profile="opening")
            (self.root / "native.log").write_text(SELECTION_PREFIX + json.dumps({"kind": "suite", "name": "articulated"}) + "\n" + EXIT_PREFIX + str(exit_code) + "\n")
            return gate.NativeExit(exit_code, self.pid)
        return gate.run_gate(self.root, self.game, self.output, Path("native.log"), runner=runner,
                             commit_reader=lambda repo: "test-head", environment={} if environment is None else environment, include_opening=include_opening)

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
        self.assertTrue(report["phaseCoverageVerified"])
        self.assertEqual(report["phaseCoverageMisses"], [])
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
        self.assertEqual(self.run_case(lambda env: self.generate(env, phase_miss=True)), 1)
        report = self.report()
        self.assertFalse(report["gatePassed"])
        self.assertFalse(report["phaseCoverageVerified"])
        self.assertTrue(report["associationVerified"])
        self.assertTrue(report["nativeSucceeded"])
        self.assertEqual(report["verifiedImages"], 120)
        self.assertEqual(report["package"]["recordCount"], 120)
        self.assertEqual(len(report["phaseCoverageMisses"]), 80)
        self.assertEqual(sum("phase coverage miss" in error for error in report["errors"]), 80)
        observations = report["observations"]
        self.assertEqual(sum(not entry["requestedPhaseObserved"] for entry in observations), 80)
        self.assertEqual({entry["renderedPhase"] for entry in observations}, {"ACTIVE"})

    def test_measured_active_to_recovery_miss_fails_without_losing_valid_associations(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env, active_miss=True)), 1)
        report = self.report()
        self.assertTrue(report["associationVerified"])
        self.assertTrue(report["nativeSucceeded"])
        self.assertTrue(report["package"]["complete"])
        self.assertEqual(report["verifiedImages"], 120)
        self.assertEqual(len(report["phaseCoverageMisses"]), 40)
        self.assertEqual({(entry["requestedPhase"], entry["renderedPhase"]) for entry in report["phaseCoverageMisses"]}, {("ACTIVE", "RECOVERY")})
        self.assertFalse(report["gatePassed"])
        self.assertFalse(report["phaseCoverageVerified"])

    def test_paused_phase_matches_do_not_satisfy_final_gate(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env, paused=True)), 1)
        self.assertTrue(self.report()["associationVerified"])
        self.assertEqual(len(self.report()["phaseCoverageMisses"]), 120)
        self.assertTrue(all(entry["requestedPhaseObserved"] for entry in self.report()["phaseCoverageMisses"]))
        self.assertTrue(all("paused or frozen" in error for error in self.report()["errors"]))

    def test_frozen_phase_matches_do_not_satisfy_final_gate(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env, frozen=True)), 1)
        self.assertTrue(self.report()["associationVerified"])
        self.assertEqual(len(self.report()["phaseCoverageMisses"]), 120)
        self.assertFalse(self.report()["phaseCoverageVerified"])

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
        self.assertFalse(report["phaseCoverageVerified"])
        self.assertTrue(report["package"]["complete"])
        self.assertTrue(report["errorDetailsBounded"])
        self.assertLessEqual((self.output / "association-report.json").stat().st_size, gate.MAX_REPORT_BYTES)


    def opening_report(self):
        return json.loads((self.output / "opening-association-report.json").read_text())

    def test_exact_independent_opening_matrix(self):
        shared = gate.planned_trials()
        opening = gate.planned_opening_trials()
        self.assertEqual(len(shared), 120)
        self.assertEqual(len(opening), 72)
        self.assertFalse(shared & opening)
        self.assertEqual(sum("_first_" in trial for trial in opening), 36)
        self.assertEqual(sum("_third_" in trial for trial in opening), 36)
        self.assertEqual(sum("_adapter_disabled_" in trial for trial in opening), 8)
        self.assertIn("articulated_opening_style_kindling_draw_first_left_netherite_funded_shell_adapter_disabled_requested_active", opening)
        self.assertNotIn("articulated_opening_style_frostbite_third_right_skin_funded_shell_requested_windup", opening)

    def test_include_opening_packages_separate72_from_one_exact_native_launch(self):
        with mock.patch.object(self, "generate", wraps=self.generate) as generated:
            self.assertEqual(self.run_case(include_opening=True), 0)
        self.assertEqual(generated.call_count, 2)
        shared, opening = self.report(), self.opening_report()
        self.assertEqual(shared["expectedTrials"], 120)
        self.assertEqual(shared["verifiedImages"], 120)
        self.assertTrue(shared["gatePassed"])
        self.assertEqual(opening["expectedTrials"], 72)
        self.assertEqual(opening["verifiedImages"], 72)
        self.assertTrue(opening["gatePassed"])
        self.assertTrue(opening["phaseCoverageVerified"])
        self.assertTrue(opening["fallbackPhaseCoverageVerified"])
        self.assertEqual(shared["provenance"], opening["provenance"])
        shared_tokens = {(token["runId"], token["captureSequence"]) for token in shared["package"]["captureTokens"]}
        opening_tokens = {(token["runId"], token["captureSequence"]) for token in opening["package"]["captureTokens"]}
        self.assertEqual(len(shared_tokens), 120)
        self.assertEqual(len(opening_tokens), 72)
        self.assertFalse(shared_tokens & opening_tokens)
        self.assertEqual({entry["captureSequence"] for entry in opening["observations"]}, set(range(121, 193)))
        self.assertEqual(shared["provenance"]["command"][2:4], ["--suite", "articulated"])
        self.assertEqual(len(list((self.output / "opening-receipts").glob("*.json"))), 72)
        self.assertFalse(list(self.output.rglob("*.png")))
        self.assertLessEqual(sum(path.stat().st_size for path in self.output.rglob("*") if path.is_file()), gate.MAX_METADATA_BYTES)
        for observed in opening["observations"]:
            self.assertEqual(hashlib.sha256((self.game / observed["relativeImagePath"]).read_bytes()).hexdigest(), observed["pngSha256"])
            sidecar = Path(opening["receiptDirectory"]) / observed["receiptRelativePath"]
            self.assertEqual(hashlib.sha256(sidecar.read_bytes()).hexdigest(), observed["receiptSha256"])

    def test_default_shared_gate_ignores_opening_without_changing_planned_proof(self):
        def both(env):
            self.generate(env)
            self.generate(env, profile="opening", count=1)
        self.assertEqual(self.run_case(both), 0)
        self.assertEqual(self.report()["expectedTrials"], 120)
        self.assertFalse((self.output / "opening-association-report.json").exists())

    def test_missing_opening_keeps_passing_shared_proof_but_fails_optional_gate(self):
        self.assertEqual(self.run_case(lambda env: self.generate(env), include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        self.assertFalse(self.opening_report()["gatePassed"])
        self.assertFalse(self.opening_report()["package"]["complete"])

    def test_opening_success_cannot_hide_shared_missing_trial(self):
        def missing(env):
            self.generate(env, count=119)
            self.generate(env, profile="opening")
        self.assertEqual(self.run_case(missing, include_opening=True), 1)
        self.assertFalse(self.report()["gatePassed"])
        self.assertTrue(self.opening_report()["gatePassed"])

    def test_opening_native_failure_survives_both_valid_associations(self):
        self.assertEqual(self.run_case(exit_code=9, include_opening=True), 1)
        for report in (self.report(), self.opening_report()):
            self.assertTrue(report["associationVerified"])
            self.assertFalse(report["nativeSucceeded"])
            self.assertFalse(report["gatePassed"])
            self.assertTrue(any("exit code 9" in error for error in report["errors"]))

    def test_opening_phase_misses_fail_both_truthful_coverage_dimensions(self):
        def misses(env):
            self.generate(env)
            self.generate(env, profile="opening", active_miss=True)
        self.assertEqual(self.run_case(misses, include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        report = self.opening_report()
        self.assertTrue(report["associationVerified"])
        self.assertFalse(report["phaseCoverageVerified"])
        self.assertFalse(report["fallbackPhaseCoverageVerified"])
        self.assertEqual(len(report["phaseCoverageMisses"]), 32)
        self.assertEqual(len(report["fallbackPhaseCoverageMisses"]), 8)
        self.assertTrue(all(entry["renderedPhase"] == "FALLBACK" for entry in report["fallbackPhaseCoverageMisses"]))

    def test_opening_stale_root_prevents_native_launch(self):
        self.generate({gate.NONCE_ENV: "old"}, profile="opening", count=1)
        never = mock.Mock(side_effect=AssertionError("stale opening root must prevent launch"))
        result = gate.run_gate(self.root, self.game, self.output, Path("native.log"), runner=never,
                               commit_reader=lambda repo: "test-head", environment={}, include_opening=True)
        self.assertEqual(result, 1)
        never.assert_not_called()
        self.assertTrue(any("preexisting opening" in error for error in self.opening_report()["errors"]))

    def test_opening_fresh_files_with_copied_nonce_fail_only_opening(self):
        def copied(env):
            self.generate(env)
            self.generate(env, profile="opening", nonce="copied")
        self.assertEqual(self.run_case(copied, include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        self.assertFalse(self.opening_report()["associationVerified"])
        self.assertTrue(self.opening_report()["package"]["complete"])

    def test_aggregate_budget_covers_both_complete_receipt_sets_and_reports(self):
        sizes = {}
        def measured(env):
            for profile in ("shared", "opening"):
                directory = self.generate(env, profile=profile)
                sizes[profile] = sum(path.stat().st_size for path in directory.glob("*.json"))
            # Allow all shared bytes and only half the complete opening set.
            limited = 2 * gate.MAX_REPORT_BYTES + sizes["shared"] + sizes["opening"] // 2
            self.budget_patch = mock.patch.object(gate, "MAX_METADATA_BYTES", limited)
            self.budget_patch.start()
        try:
            self.assertEqual(self.run_case(measured, include_opening=True), 1)
            self.assertTrue(self.report()["gatePassed"])
            self.assertFalse(self.opening_report()["package"]["complete"])
            self.assertTrue(any("budget" in error for error in self.opening_report()["errors"]))
            self.assertLessEqual(sum(path.stat().st_size for path in self.output.rglob("*") if path.is_file()), gate.MAX_METADATA_BYTES)
        finally:
            if hasattr(self, "budget_patch"):
                self.budget_patch.stop()

    def test_opening_symlink_and_hardlink_images_fail_association(self):
        def linked(env):
            self.generate(env)
            directory = self.generate(env, profile="opening")
            for index, path in enumerate(sorted(directory.glob("*.json"))[:2]):
                record = json.loads(path.read_text())
                image = self.game / record["image"]["relativeImagePath"]
                original = image.with_suffix(".original")
                image.rename(original)
                if index == 0:
                    image.symlink_to(original.name)
                else:
                    os.link(original, image)
        self.assertEqual(self.run_case(linked, include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        self.assertFalse(self.opening_report()["associationVerified"])


    def test_shared_png_cannot_double_as_an_opening_capture(self):
        def reused(env):
            shared = self.generate(env)
            opening = self.generate(env, profile="opening")
            original = json.loads(next(shared.glob("*.json")).read_text())
            path = next(opening.glob("*.json"))
            record = json.loads(path.read_text())
            record["image"] = original["image"]
            path.write_text(json.dumps(record))
        self.assertEqual(self.run_case(reused, include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        self.assertFalse(self.opening_report()["associationVerified"])
        self.assertTrue(any("already belong to shared" in error for error in self.opening_report()["errors"]))

    def test_opening_run_uuid_must_match_same_native_shared_run(self):
        def mismatched(env):
            self.generate(env)
            self.generate(env, profile="opening", run_id=str(uuid.uuid4()))
        self.assertEqual(self.run_case(mismatched, include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        self.assertFalse(self.opening_report()["associationVerified"])
        self.assertTrue(any("run UUID differs" in error for error in self.opening_report()["errors"]))

    def test_opening_capture_token_cannot_duplicate_shared_token(self):
        def duplicate(env):
            self.generate(env)
            opening = self.generate(env, profile="opening")
            path = next(opening.glob("*.json"))
            record = json.loads(path.read_text())
            record["identity"]["captureSequence"] = 1
            record["copy"]["identity"]["captureSequence"] = 1
            path.write_text(json.dumps(record))
        self.assertEqual(self.run_case(duplicate, include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        self.assertEqual(self.opening_report()["verifiedImages"], 72)
        self.assertFalse(self.opening_report()["associationVerified"])
        self.assertFalse(self.opening_report()["gatePassed"])
        self.assertTrue(any("capture tokens already belong" in error for error in self.opening_report()["errors"]))

    def test_failed_shared_record_still_reserves_its_capture_token(self):
        def duplicate(env):
            shared = self.generate(env)
            self.generate(env, profile="opening")
            path = next(shared.glob("*.json"))
            record = json.loads(path.read_text())
            record["identity"]["captureSequence"] = 121
            record.update(verified=False, failures=["original_operation_failed"], copy=None, stages=["requested"])
            path.write_text(json.dumps(record))
        self.assertEqual(self.run_case(duplicate, include_opening=True), 1)
        self.assertFalse(self.report()["gatePassed"])
        self.assertEqual(self.report()["verifiedImages"], 119)
        self.assertEqual(self.opening_report()["verifiedImages"], 72)
        self.assertFalse(self.opening_report()["associationVerified"])
        self.assertTrue(any("capture tokens already belong" in error for error in self.opening_report()["errors"]))

    def test_late_opening_record_cannot_claim_shared_token(self):
        def duplicate(env):
            self.generate(env)
            opening = self.generate(env, profile="opening")
            record = json.loads(next(opening.glob("*.json")).read_text())
            record["identity"]["captureSequence"] = 1
            record.update(verified=False, failures=["late_callback"], copy=None, stages=["requested"])
            (opening / "000001-late-2.json").write_text(json.dumps(record))
        self.assertEqual(self.run_case(duplicate, include_opening=True), 1)
        self.assertTrue(self.report()["gatePassed"])
        self.assertEqual(self.opening_report()["package"]["recordCount"], 73)
        self.assertTrue(any("capture tokens already belong" in error for error in self.opening_report()["errors"]))


if __name__ == "__main__":
    unittest.main()
