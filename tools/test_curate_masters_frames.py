"""Stdlib, tempfile-only curator checks; never launches Java or native gameplay."""
import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import curate_masters_frames as curator


class CuratorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = "build/run/clientGameTest/screenshots"
        self.marker = "review/current-run.json"
        self.output = "review/curated"
        self.identity = {"headSha": "a" * 40, "workflowSha": "b" * 40,
                         "runId": "120", "runAttempt": "2"}
        curator.prepare(self.root, self.source, self.marker, self.identity)
        self.started = json.loads((self.root / self.marker).read_text())["startedNs"]

    def shot(self, name, size=100, *, prefix=""):
        path = self.root / self.source / prefix / name
        path.parent.mkdir(parents=True, exist_ok=True)
        # A signature and opaque bytes suffice: production intentionally never
        # decodes images, and this fixture makes byte budgets easy to inspect.
        path.write_bytes(curator.PNG_SIGNATURE + b"x" * (size - 8))
        os.utime(path, ns=(self.started + 1_000_000, self.started + 1_000_000))
        return path

    def curate(self, **kwargs):
        return curator.curate(self.root, self.source, self.marker, self.output, self.identity, **kwargs)

    def test_missing_screenshots_produce_unavailable_manifest_without_failure(self):
        result = self.curate()
        self.assertEqual(result["status"], "unavailable")
        self.assertEqual(result["frames"], [])
        self.assertEqual(len(result["coverage"]), 30)
        self.assertTrue(all(row["missingCapturePhases"] == list(curator.PHASES)
                            for row in result["coverage"]))
        self.assertTrue((self.root / self.output / "manifest.json").is_file())
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_exact_budget_includes_manifest_and_keeps_original_bytes_and_hash(self):
        for scene in curator.SCENES:
            for view in curator.VIEWS:
                for phase in ("windup", "frame_2", "settled"):
                    self.shot(f"{scene}_{view}_{phase}.png", size=300_000)
        result = self.curate()
        files = list((self.root / self.output).rglob("*"))
        size = sum(path.stat().st_size for path in files if path.is_file())
        self.assertLessEqual(size, 14_000_000)
        self.assertGreater(len(result["frames"]), 0)
        self.assertLess(len(result["frames"]), 90)
        self.assertTrue(any(row["omittedForBudgetPhases"] for row in result["coverage"]))
        self.assertEqual(result["selectedPngBytes"], sum(frame["bytes"] for frame in result["frames"]))
        # Even an uncompressed ZIP retains comfortable headroom below 15 MB.
        archive = self.root / "curated.zip"
        with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in files:
                if path.is_file():
                    zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLess(archive.stat().st_size, 15_000_000)
        for frame in result["frames"]:
            original = (self.root / frame["sourcePath"]).read_bytes()
            copied = (self.root / self.output / frame["artifactPath"]).read_bytes()
            self.assertEqual(original, copied)
            self.assertEqual(frame["sha256"], hashlib.sha256(original).hexdigest())
            self.assertEqual(frame["bytes"], len(original))

    def test_oversized_source_is_reported_but_not_copied(self):
        self.shot("masters_art_0_first_windup.png", size=curator.BUDGET + 1)
        result = self.curate()
        self.assertEqual(result["status"], "unavailable")
        row = result["coverage"][0]
        self.assertEqual(row["capturedPhases"], ["windup"])
        self.assertEqual(row["omittedForBudgetPhases"], ["windup"])

    def test_selection_is_deterministic_and_uses_median_captured_sample_index(self):
        for index in (6, 0, 2, 4):
            self.shot(f"masters_art_0_first_frame_{index}.png")
        self.shot("masters_style_rising_cinders_third_windup.png")
        first = self.curate()
        second = curator.curate(self.root, self.source, self.marker, "review/another", self.identity)
        self.assertEqual(first, second)
        frame = first["frames"][0]
        self.assertEqual(frame["frameLabel"], "frame_4")
        self.assertEqual(frame["phase"], "sample")
        self.assertEqual(frame["phaseBasis"], "filename_loop_index_only")
        self.assertEqual(frame["sampleIndex"], 4)
        self.assertEqual((self.root / self.output / "manifest.json").read_bytes(),
                         (self.root / "review/another/manifest.json").read_bytes())

    def test_exact_scope_labels_and_absent_forms_are_not_invented(self):
        fixtures = {
            "masters_art_1_first_windup.png": ("shared_art", "owner_first_person", "windup"),
            "masters_style_kindling_draw_third_settled.png":
                ("first_form", "local_owner_third_person_front", "settled"),
            "masters_style_blossom_fall_first_frame_7.png": ("second_form", "owner_first_person", "sample"),
        }
        for name in fixtures:
            self.shot(name)
        for name in ("master_ember_windup.png", "masters_style_kindling_draw_observer_frame_1.png",
                     "masters_style_unknown_first_windup.png", "master_model_pose.png",
                     "masters_style_blossom_fall_left_turn_first_frame_1.png",
                     "masters_style_rising_cinders_cancelled_neutral.png", "masters_arts_help_scale_1.png"):
            self.shot(name)
        result = self.curate()
        self.assertEqual(len(result["frames"]), len(fixtures))
        for frame in result["frames"]:
            self.assertEqual((frame["form"], frame["view"], frame["phase"]),
                             fixtures[Path(frame["sourcePath"]).name])
            self.assertEqual(frame["captureKind"], "native_local_owner_combat")
        self.assertEqual({row["coverage"] for row in result["unavailableRequestedCoverage"]},
                         {"observer_client", "live_master_ember_combat", "exact_impact_phase"})
        rising = [row for row in result["coverage"] if "rising_cinders" in row["scene"]]
        self.assertTrue(all(row["missingCapturePhases"] == list(curator.PHASES) for row in rising))
        self.assertEqual(result["freshness"]["ignoredOutOfScopePngs"], 7)

    def test_preexisting_and_old_timestamp_sources_are_rejected_even_after_touch(self):
        prior = self.shot("masters_art_0_first_windup.png")
        other_marker = "review/later-run.json"
        curator.prepare(self.root, self.source, other_marker, self.identity)
        old = self.shot("masters_art_0_first_settled.png")
        os.utime(old, ns=(1, 1))
        os.utime(prior, None)
        result = curator.curate(self.root, self.source, other_marker, self.output, self.identity)
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["freshness"]["excludedStalePngs"], 2)

    def test_run_marker_mismatch_and_reuse_are_rejected(self):
        with self.assertRaises(FileExistsError):
            curator.prepare(self.root, self.source, self.marker, self.identity)
        wrong = {**self.identity, "runAttempt": "3"}
        with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
            curator.curate(self.root, self.source, self.marker, self.output, wrong)
        self.curate()
        with self.assertRaisesRegex(curator.EvidenceError, "Output already exists"):
            self.curate()

    def test_path_traversal_and_absolute_paths_are_rejected(self):
        for path in ("../outside", "review/../../outside", str(self.root / "absolute")):
            with self.subTest(path=path), self.assertRaises(curator.EvidenceError):
                curator.safe_path(self.root, path)
        with self.assertRaisesRegex(curator.EvidenceError, "must be separate"):
            curator.curate(self.root, self.source, self.marker, self.source + "/out", self.identity)

    def test_source_and_ancestor_symlinks_are_rejected(self):
        outside = self.root / "outside"
        outside.mkdir()
        (outside / "real.png").write_bytes(curator.PNG_SIGNATURE)
        path = self.root / self.source
        path.mkdir(parents=True)
        (path / "masters_art_0_first_windup.png").symlink_to(outside / "real.png")
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            self.curate()
        (self.root / "linked").symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            curator.safe_path(self.root, "linked/nested/output")
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            curator.source_files(self.root, "linked")

    def test_duplicate_basename_and_invalid_png_signature_fail_closed(self):
        first = self.shot("masters_art_0_first_windup.png")
        duplicate = self.shot(first.name, prefix="duplicate")
        with self.assertRaisesRegex(curator.EvidenceError, "Ambiguous duplicate"):
            self.curate()
        duplicate.unlink()
        first.write_bytes(b"not a png image")
        with self.assertRaisesRegex(curator.EvidenceError, "PNG signature"):
            self.curate()
        self.assertFalse((self.root / self.output).exists())

    def test_invalid_budget_cannot_exceed_promised_limit(self):
        for budget in (0, curator.MANIFEST_RESERVE - 1, curator.BUDGET + 1):
            with self.subTest(budget=budget), self.assertRaises(curator.EvidenceError):
                self.curate(budget=budget)

    def test_only_capture_loop_indices_present_in_the_source_are_eligible(self):
        for name in ("masters_art_0_first_frame_7.png", "masters_art_0_first_frame_00.png",
                     "masters_style_blossom_fall_first_frame_10.png"):
            self.assertIsNone(curator.describe(name))
        self.assertEqual(curator.describe("masters_art_0_first_frame_6.png")["phase"], "sample")
        self.assertEqual(curator.describe("masters_style_blossom_fall_first_frame_9.png")["phase"], "sample")


class ProvenanceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.event = self.root / "event.json"
        self.event.write_text(json.dumps({"pull_request": {"head": {"sha": "a" * 40}}}))
        self.environment = {"GITHUB_SHA": "b" * 40, "GITHUB_REPOSITORY": "owner/repo",
                            "GITHUB_RUN_ID": "120", "GITHUB_RUN_ATTEMPT": "2",
                            "GITHUB_EVENT_NAME": "pull_request", "GITHUB_EVENT_PATH": str(self.event)}

    def provenance(self, **overrides):
        with patch.object(curator.subprocess, "check_output", return_value="b" * 40 + "\n"):
            return curator.provenance(self.root, {**self.environment, **overrides})

    def test_pr_head_and_workflow_merge_sha_are_distinct_and_exact(self):
        value = self.provenance()
        self.assertEqual(value["headSha"], "a" * 40)
        self.assertEqual(value["workflowSha"], "b" * 40)
        self.assertEqual(value["checkedOutSha"], "b" * 40)
        self.assertEqual(value["runId"], "120")
        self.assertEqual(value["runAttempt"], "2")
        self.assertEqual(value["runAttemptUrl"], "https://github.com/owner/repo/actions/runs/120/attempts/2")
        self.assertEqual(value["headCommitUrl"], "https://github.com/owner/repo/commit/" + "a" * 40)

    def test_pr_event_cannot_fallback_to_merge_sha(self):
        with self.assertRaises(curator.EvidenceError):
            self.provenance(GITHUB_EVENT_PATH="")
        self.event.write_text("{}")
        with self.assertRaises(curator.EvidenceError):
            self.provenance()

    def test_push_uses_exact_checkout_commit(self):
        value = self.provenance(GITHUB_EVENT_NAME="push")
        self.assertEqual(value["headSha"], value["workflowSha"])

    def test_missing_or_mismatched_identity_cannot_be_published(self):
        for overrides in ({"GITHUB_RUN_ID": ""}, {"GITHUB_SHA": "main"},
                          {"GITHUB_SHA": "c" * 40}, {"GITHUB_RUN_ATTEMPT": "0"},
                          {"GITHUB_REPOSITORY": "../../other"}, {"GITHUB_EVENT_NAME": "workflow_dispatch"}):
            with self.subTest(overrides=overrides), self.assertRaises(curator.EvidenceError):
                self.provenance(**overrides)


if __name__ == "__main__":
    unittest.main()
