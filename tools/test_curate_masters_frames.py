"""Tempfile-only curator checks; optional receipt tests use Pillow, never native gameplay."""
import hashlib
import json
import os
import re
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import curate_masters_frames as curator


class CuratorFixture(unittest.TestCase):
    suite = "masters"

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = "build/run/clientGameTest/screenshots"
        self.marker = "review/current-run.json"
        self.output = "review/curated"
        self.identity = {"headSha": "a" * 40, "workflowSha": "b" * 40,
                         "runId": "120", "runAttempt": "2"}
        curator.prepare(self.root, self.source, self.marker, self.identity, suite=self.suite)
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
        return curator.curate(self.root, self.source, self.marker, self.output, self.identity,
                              suite=self.suite, **kwargs)


class CuratorTests(CuratorFixture):

    def test_missing_screenshots_produce_unavailable_manifest_without_failure(self):
        result = self.curate()
        self.assertEqual(result["status"], "unavailable")
        self.assertEqual(result["frames"], [])
        self.assertEqual(len(result["coverage"]), 32)
        self.assertTrue(all(row["missingCapturePhases"] == list(curator.PHASES)
                            for row in result["coverage"][:30]))
        self.assertEqual([row["missingCapturePhases"] for row in result["coverage"][30:]],
                         [list(beats) for beats in curator.NPC_BEATS.values()])
        self.assertTrue((self.root / self.output / "manifest.json").is_file())
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_exact_budget_includes_manifest_and_keeps_original_bytes_and_hash(self):
        for scene in curator.SCENES:
            for view in curator.VIEWS:
                for phase in curator.PHASES:
                    self.shot(f"{scene}_{view}_{phase}.png", size=300_000)
        result = self.curate()
        files = list((self.root / self.output).rglob("*"))
        size = sum(path.stat().st_size for path in files if path.is_file())
        self.assertLessEqual(size, 14_000_000)
        self.assertGreater(len(result["frames"]), 0)
        self.assertLess(len(result["frames"]), 120)
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

    def test_current_phase_selection_is_deterministic_and_does_not_relabel_old_samples(self):
        for phase in curator.PHASES:
            self.shot(f"masters_art_0_first_{phase}.png")
        self.shot("masters_art_0_first_frame_4.png")
        first = self.curate()
        second = curator.curate(self.root, self.source, self.marker, "review/another", self.identity)
        self.assertEqual(first, second)
        self.assertEqual([f["frameLabel"] for f in first["frames"]], list(curator.PHASES))
        active = first["frames"][1]
        self.assertEqual(active["phaseBasis"], "accepted_timeline_window_assertion")
        self.assertEqual(curator.describe("masters_art_0_first_frame_4.png")["phase"], "sample")
        self.assertEqual(curator.describe("masters_art_0_third_windup.png")["view"],
                         "local_owner_third_person_front")
        self.assertEqual(curator.describe("masters_art_0_third_back_windup.png")["view"],
                         "local_owner_third_person_back")

    def test_exact_scope_labels_and_absent_forms_are_not_invented(self):
        fixtures = {
            "masters_art_1_first_windup.png": ("shared_art", "owner_first_person", "windup"),
            "masters_style_kindling_draw_third_back_settled.png":
                ("first_form", "local_owner_third_person_back", "settled"),
            "masters_style_blossom_fall_first_recovery.png": ("second_form", "owner_first_person", "recovery"),
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
                         {"observer_client_of_player_combat", "live_master_ember_combat",
                          "exact_player_impact_phase", "human_multiplayer_duel"})
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

    def test_legacy_capture_indices_keep_their_original_bounded_sample_labels(self):
        for name in ("masters_art_0_first_frame_7.png", "masters_art_0_first_frame_00.png",
                     "masters_style_blossom_fall_first_frame_10.png"):
            self.assertIsNone(curator.describe(name))
        self.assertEqual(curator.describe("masters_art_0_first_frame_6.png")["phase"], "sample")
        self.assertEqual(curator.describe("masters_style_blossom_fall_first_frame_9.png")["phase"], "sample")


class NpcCuratorTests(CuratorFixture):
    def metadata(self, scene, phase):
        # Shape and values follow MastersNpcCaptureProbe.Evidence. These are
        # Python input-validation fixtures, never presented as native captures.
        gale = scene == "masters_npc_gale_crosswind"
        requested = curator.NPC_BEATS[scene][phase]
        attack, tell, recovery = (7, 22, 31) if gale else (8, 32, 39)
        position = {"x": 0, "y": 181, "z": 0}
        joint = {"x": .1, "y": .2, "z": .3}
        point = {"x": .4, "y": .5}
        warning = {"ray": {"from": position, "to": position, "renderedEnd": position,
                           "width": .1, "particleAge": 1, "lifetime": 5, "partial": .5},
                   "from": point, "to": point, "coloredBins": 8, "sampledBins": 16}
        count = 3 if phase == "reply_warning" else 0 if phase in ("release", "recovery") else 2 if gale else 1
        return {"name": f"{scene}_{phase}", "captureKind": "native_unpaused_server_ai",
                "participantKind": "consenting_fabric_fake_player", "observerKind": "real_client_spectator",
                "independentTrialPerFrame": True, "phase": phase, "requestedTick": requested,
                "width": 1280, "height": 720, "bodySubmits": 1, "modelPasses": 1,
                "renderedTimeline": {"clientGameTick": 100 + requested, "acceptedTick": 100, "attackId": attack,
                    "partial": .5, "age": requested + .5, "tell": tell, "active": 1, "recovery": recovery,
                    "fallbackRig": True, "frame": {"pose": {"weight": .9, "body": joint, "head": joint,
                        "sword": joint, "offhand": joint, "stance": .2, "bladeTilt": -30},
                        "leftHanded": False, "activation": 100, "move": attack}},
                "serverObservation": {"gameTick": 100 + requested, "acceptedTick": 100, "attackId": attack,
                    "age": requested, "noAi": False, "pending": requested < tell, "windup": requested < tell,
                    "guarding": not gale and 8 <= requested < 20, "participants": 1, "aura": 60 if gale else 56,
                    "targetHealth": 200 if requested < tell else 174 if gale else 169.2,
                    "masterPosition": position, "targetPosition": position},
                "modelPose": [.1] * 36, "camera": position, "renderedPosition": position,
                "bodyPoints": [point] * 9,
                "bodyPixels": {"nonBlackPixels": 100, "chromaticPixels": 50, "distinctColors": 12, "luminanceRange": 40},
                "warnings": [warning] * count}

    def npc(self, scene="masters_npc_gale_crosswind", phase="reply_warning", size=100):
        image = self.shot(f"{scene}_{phase}.png", size=size)
        sidecar = image.with_suffix(".json")
        self.write_metadata(sidecar, self.metadata(scene, phase))
        return image, sidecar

    def write_metadata(self, path, value):
        path.write_text(json.dumps(value, indent=2))
        os.utime(path, ns=(self.started + 1_000_000, self.started + 1_000_000))

    def test_exact_school_beats_are_whitelisted_and_other_npc_names_are_ignored(self):
        for scene, beats in curator.NPC_BEATS.items():
            for phase in beats:
                name = f"{scene}_{phase}.png"
                description = curator.describe(name)
                self.assertEqual((description["view"], description["captureKind"], description["phase"]),
                                 (curator.NPC_VIEW, "native_unpaused_server_ai", phase))
                self.assertEqual(description["sourceSuite"], curator.NPC_SUITE)
                self.assertEqual(description["phaseBasis"], "native_render_and_server_metadata_assertion")
                self.assertIsNone(curator.describe(name, suite="articulated"))
        for name in ("masters_npc_ember_release.png", "masters_npc_gale_crosswind_active.png",
                     "masters_npc_gale_crosswind_plant.png", "masters_npc_stone_fracture_step.png",
                     "masters_npc_gale_crosswind_frame_0.png", "masters_npc_stone_fracture_observer_release.png",
                     "master_model_pose.png"):
            self.assertIsNone(curator.describe(name))
            self.shot(name)
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 0)
        self.assertEqual(result["freshness"]["ignoredOutOfScopePngs"], 7)

    def test_priority_preserves_six_npc_beats_before_owner_breadth_and_counts_sidecars(self):
        for scene, beats in curator.NPC_BEATS.items():
            for phase in beats:
                self.npc(scene, phase, size=700_000)
        for scene in curator.SCENES:
            for view in curator.VIEWS:
                for phase in curator.PHASES:
                    self.shot(f"{scene}_{view}_{phase}.png", size=700_000)
        result = self.curate()
        expected = [(scene, phase) for phase in curator.NPC_PRIORITY for scene in curator.NPC_BEATS]
        self.assertEqual([(f["scene"], f["phase"]) for f in result["frames"][:6]], expected)
        self.assertEqual(result["selectedMetadataCount"], 6)
        self.assertTrue(any(frame["captureKind"] == "native_local_owner_combat" for frame in result["frames"]))
        self.assertEqual(result["selectedMetadataBytes"], sum(frame["metadata"]["bytes"] for frame in result["frames"] if "metadata" in frame))
        self.assertTrue(all(row["omittedForBudgetPhases"] == list(curator.NPC_BEATS[row["scene"]])[:2]
                            for row in result["coverage"][-2:]))
        files = [path for path in (self.root / self.output).rglob("*") if path.is_file()]
        self.assertLess(sum(path.stat().st_size for path in files), curator.BUDGET)
        archive = self.root / "curated.zip"
        with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in files:
                zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLess(archive.stat().st_size, 15_000_000)

    def test_small_matrix_is_deterministic_and_preserves_png_and_metadata_bytes(self):
        for scene, beats in curator.NPC_BEATS.items():
            for phase in beats:
                self.npc(scene, phase)
        self.shot("masters_art_0_first_active.png")
        result = self.curate()
        second = curator.curate(self.root, self.source, self.marker, "review/repeated", self.identity)
        self.assertEqual(result, second)
        self.assertEqual(result["selectedFrameCount"], 11)
        self.assertEqual(result["selectedMetadataCount"], 10)
        self.assertEqual(result["sourceSuites"], [curator.SUITE, curator.NPC_SUITE])
        for frame in result["frames"]:
            for entry in ([frame, frame["metadata"]] if "metadata" in frame else [frame]):
                original = (self.root / entry["sourcePath"]).read_bytes()
                self.assertEqual((self.root / self.output / entry["artifactPath"]).read_bytes(), original)
                self.assertEqual(entry["sha256"], hashlib.sha256(original).hexdigest())
                self.assertEqual(entry["bytes"], len(original))
            if "metadata" in frame:
                self.assertEqual(frame["renderedTimeline"]["age"], frame["requestedTick"] + .5)
                self.assertEqual(frame["participantKind"], "consenting_fabric_fake_player")
                self.assertTrue(frame["independentTrialPerFrame"])
            else:
                self.assertEqual(frame["phaseBasis"], "accepted_timeline_window_assertion")
        self.assertIn("not a human duel", result["basis"])
        self.assertIn("Synthetic model-injection", result["basis"])
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_budget_never_splits_an_npc_image_and_metadata_pair(self):
        image, sidecar = self.npc()
        cost = image.stat().st_size + sidecar.stat().st_size
        omitted = self.curate(budget=curator.MANIFEST_RESERVE + cost - 1)
        self.assertEqual(omitted["frames"], [])
        self.assertEqual(omitted["selectedMetadataBytes"], 0)
        row = omitted["coverage"][-2]
        self.assertEqual(row["capturedPhases"], ["reply_warning"])
        self.assertEqual(row["omittedForBudgetPhases"], ["reply_warning"])
        result = curator.curate(self.root, self.source, self.marker, "review/exact", self.identity,
                                budget=curator.MANIFEST_RESERVE + cost)
        self.assertEqual(result["selectedFrameCount"], 1)
        self.assertEqual(result["selectedPngBytes"] + result["selectedMetadataBytes"], cost)

    def test_full_owner_and_npc_matrix_fits_the_unchanged_manifest_reserve(self):
        for scene, beats in curator.NPC_BEATS.items():
            for phase in beats:
                self.npc(scene, phase)
        for scene in curator.SCENES:
            for view in curator.VIEWS:
                for phase in curator.PHASES:
                    self.shot(f"{scene}_{view}_{phase}.png")
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 130)
        self.assertEqual(result["limits"]["manifestReserveBytes"], 128_000)
        self.assertLessEqual((self.root / self.output / "manifest.json").stat().st_size, 128_000)
        self.assertTrue(all(not row["omittedForBudgetPhases"] for row in result["coverage"]))

    def test_partial_npc_coverage_never_substitutes_or_invents_missing_beats(self):
        self.npc(phase="gather")
        self.npc("masters_npc_stone_fracture", "release")
        self.shot("masters_art_0_first_active.png")
        result = self.curate()
        self.assertEqual([(f["scene"], f["phase"]) for f in result["frames"]], [
            ("masters_npc_stone_fracture", "release"), ("masters_art_0", "active"),
            ("masters_npc_gale_crosswind", "gather")])
        self.assertEqual(result["coverage"][-2]["missingCapturePhases"], ["step", "reply_warning", "release", "recovery"])
        self.assertEqual(result["coverage"][-1]["missingCapturePhases"], ["plant", "brace", "reply_warning", "recovery"])

    def test_preexisting_or_older_metadata_cannot_qualify_a_fresh_image(self):
        image, sidecar = self.npc()
        image.unlink()
        later = "review/later.json"
        curator.prepare(self.root, self.source, later, self.identity)
        self.started = json.loads((self.root / later).read_text())["startedNs"]
        self.shot(image.name)
        os.utime(sidecar, None)
        _, older = self.npc("masters_npc_stone_fracture", "release")
        os.utime(older, ns=(1, 1))
        result = curator.curate(self.root, self.source, later, self.output, self.identity)
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["freshness"]["excludedStaleNpcMetadata"], 2)

    def test_preexisting_or_older_npc_images_stay_excluded_with_fresh_metadata(self):
        image, _ = self.npc()
        later = "review/later.json"
        curator.prepare(self.root, self.source, later, self.identity)
        self.started = json.loads((self.root / later).read_text())["startedNs"]
        self.npc()
        older, _ = self.npc("masters_npc_stone_fracture", "release")
        os.utime(older, ns=(1, 1))
        result = curator.curate(self.root, self.source, later, self.output, self.identity)
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["freshness"]["excludedStalePngs"], 2)

    def test_missing_metadata_malformed_json_and_oversized_sidecars_fail_closed(self):
        _, sidecar = self.npc()
        for contents in (None, b"{invalid", b"[]", b" " * (curator.NPC_METADATA_LIMIT + 1), b'{"name":0,"name":1}'):
            with self.subTest(contents=None if contents is None else len(contents)):
                if contents is None:
                    sidecar.unlink()
                else:
                    sidecar.write_bytes(contents)
                with self.assertRaises((curator.EvidenceError, ValueError)):
                    self.curate()
                self.assertFalse((self.root / self.output).exists())

    def test_metadata_rejects_wrong_identity_phase_ai_and_render_evidence(self):
        scene, phase = "masters_npc_gale_crosswind", "reply_warning"
        _, sidecar = self.npc(scene, phase)
        mutations = (("name", "wrong"), ("phase", "release"), ("requestedTick", 17),
                     ("captureKind", "synthetic_model"), ("participantKind", "human"),
                     ("observerKind", "owner"), ("independentTrialPerFrame", False),
                     ("renderFailure", ""), ("renderFailure", True), ("bodySubmits", 0), ("modelPasses", 0),
                     ("modelPose", [.1] * 35), ("bodyPoints", []), ("warnings", []),
                     ("renderedTimeline.age", 17.5), ("renderedTimeline.partial", float("nan")),
                     ("renderedTimeline.attackId", 8), ("renderedTimeline.acceptedTick", 99),
                     ("renderedTimeline.clientGameTick", 120), ("renderedTimeline.fallbackRig", False),
                     ("renderedTimeline.frame.move", 8), ("renderedTimeline.frame.activation", 99),
                     ("renderedTimeline.frame.pose.weight", 0), ("serverObservation.noAi", True),
                     ("serverObservation.participants", 2), ("serverObservation.attackId", 8),
                     ("serverObservation.gameTick", 120), ("serverObservation.pending", False),
                     ("serverObservation.windup", False), ("serverObservation.guarding", True),
                     ("serverObservation.aura", 84), ("serverObservation.targetHealth", 174),
                     ("bodyPixels.nonBlackPixels", 0))
        for field, value in mutations:
            with self.subTest(field=field):
                evidence = self.metadata(scene, phase)
                parent = evidence
                keys = field.split(".")
                for key in keys[:-1]:
                    parent = parent[key]
                parent[keys[-1]] = value
                self.write_metadata(sidecar, evidence)
                with self.assertRaises(curator.EvidenceError):
                    self.curate()
                self.assertFalse((self.root / self.output).exists())

    def test_explicit_render_failure_preserves_valid_frames_and_records_a_bounded_exclusion(self):
        failed, sidecar = self.npc()
        evidence = json.loads(sidecar.read_text())
        evidence["renderFailure"] = "java.lang.AssertionError: Stage and other bodies must not occlude the master\n" * 50
        self.write_metadata(sidecar, evidence)
        valid, _ = self.npc("masters_npc_stone_fracture", "release")
        owner = self.shot("masters_art_0_first_active.png")
        result = self.curate()
        self.assertEqual({Path(frame["sourcePath"]).name for frame in result["frames"]}, {valid.name, owner.name})
        self.assertEqual(result["selectedMetadataCount"], 1)
        self.assertEqual(result["coverage"][-2]["capturedPhases"], [])
        self.assertIn("reply_warning", result["coverage"][-2]["missingCapturePhases"])
        self.assertEqual(len(result["excludedNpcCaptures"]), 1)
        exclusion = result["excludedNpcCaptures"][0]
        self.assertEqual(exclusion["reasonCode"], "native_render_failure")
        self.assertEqual(exclusion["sourcePath"], failed.relative_to(self.root).as_posix())
        self.assertEqual(exclusion["metadataSourcePath"], sidecar.relative_to(self.root).as_posix())
        self.assertEqual(exclusion["metadataSha256"], hashlib.sha256(sidecar.read_bytes()).hexdigest())
        self.assertEqual(len(exclusion["reason"]), curator.NPC_FAILURE_REASON_LIMIT)
        self.assertNotIn("\n", exclusion["reason"])
        self.assertFalse((self.root / self.output / "frames" / failed.name).exists())
        self.assertFalse((self.root / self.output / "frames" / sidecar.name).exists())
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_early_failed_native_render_may_omit_null_extraction_fields_without_becoming_accepted(self):
        _, sidecar = self.npc()
        evidence = json.loads(sidecar.read_text())
        for key in ("renderedTimeline", "modelPose", "renderedPosition"):
            del evidence[key]
        evidence.update(renderFailure="java.lang.AssertionError: Native NPC body submission and animated model passes must both occur",
                        bodySubmits=0, modelPasses=0, bodyPoints=[], warnings=[],
                        bodyPixels={"nonBlackPixels": 0, "chromaticPixels": 0, "distinctColors": 0, "luminanceRange": 0})
        self.write_metadata(sidecar, evidence)
        result = self.curate()
        self.assertEqual(result["status"], "unavailable")
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["selectedMetadataBytes"], 0)
        self.assertEqual(len(result["excludedNpcCaptures"]), 1)
        self.assertTrue(all(not row["capturedPhases"] for row in result["coverage"]))

    def test_failure_marker_does_not_excuse_malformed_identity_schema_or_non_finite_data(self):
        _, sidecar = self.npc()
        mutations = (("name", "../outside"), ("phase", "release"), ("requestedTick", 17),
                     ("renderFailure", " "), ("captureKind", "synthetic"), ("bodySubmits", "0"),
                     ("camera", None), ("modelPose", "missing"), ("bodyPoints", None),
                     ("warnings", {}), ("bodyPixels", []), ("renderedTimeline", []),
                     ("renderedTimeline.age", "16.5"), ("renderedTimeline.frame", []),
                     ("renderedTimeline.frame.pose.weight", "0.5"),
                     ("serverObservation", None), ("serverObservation.attackId", 8),
                     ("serverObservation.noAi", True), ("serverObservation.targetPosition", {}),
                     ("extra", float("inf")))
        for field, value in mutations:
            with self.subTest(field=field):
                evidence = self.metadata("masters_npc_gale_crosswind", "reply_warning")
                evidence["renderFailure"] = "java.lang.AssertionError: Failed native render"
                parent = evidence
                keys = field.split(".")
                for key in keys[:-1]:
                    parent = parent[key]
                parent[keys[-1]] = value
                self.write_metadata(sidecar, evidence)
                with self.assertRaises(curator.EvidenceError):
                    self.curate()
                self.assertFalse((self.root / self.output).exists())
        valid = self.metadata("masters_npc_gale_crosswind", "reply_warning")
        valid["renderFailure"] = "Failed native render"
        sidecar.write_text(json.dumps(valid).replace('"name":', '"name": "duplicate", "name":', 1))
        with self.assertRaisesRegex(curator.EvidenceError, "Duplicate"):
            self.curate()
        valid["extra"] = 1.0
        sidecar.write_text(json.dumps(valid).replace('"extra": 1.0', '"extra": 1e999'))
        with self.assertRaises(curator.EvidenceError):
            self.curate()

    def test_release_render_can_have_contemporaneous_pre_release_server_observation(self):
        # The server observation and actual render extraction are distinct. A
        # one-tick earlier observation must not be rewritten as release damage.
        _, sidecar = self.npc(phase="release")
        value = json.loads(sidecar.read_text())
        value["serverObservation"].update(gameTick=121, age=21, pending=True, windup=True, targetHealth=200)
        self.write_metadata(sidecar, value)
        result = self.curate()
        self.assertEqual(result["frames"][0]["renderedTimeline"]["age"], 22.5)
        self.assertEqual(result["frames"][0]["serverObservation"]["age"], 21)
        self.assertEqual(result["frames"][0]["serverObservation"]["targetHealth"], 200)

    def test_metadata_mutation_after_validation_is_rejected_even_with_same_size_and_mtime(self):
        _, sidecar = self.npc()
        select = curator.select_masters

        def mutate(groups, budget):
            selected = select(groups, budget)
            before = sidecar.stat()
            sidecar.write_bytes(sidecar.read_bytes().replace(b'"nonBlackPixels": 100', b'"nonBlackPixels": 101'))
            os.utime(sidecar, ns=(before.st_atime_ns, before.st_mtime_ns))
            return selected

        with patch.object(curator, "select_masters", side_effect=mutate):
            with self.assertRaisesRegex(curator.EvidenceError, "changed after validation"):
                self.curate()
        self.assertFalse((self.root / self.output).exists())

    def test_npc_sidecar_symlinks_and_duplicate_image_names_are_rejected(self):
        image, sidecar = self.npc()
        backup = self.root / "metadata.json"
        sidecar.rename(backup)
        sidecar.symlink_to(backup)
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            self.curate()
        sidecar.unlink()
        backup.rename(sidecar)
        self.shot(image.name, prefix="duplicate")
        # Put a complete sidecar beside the duplicate as well.
        duplicate = self.root / self.source / "duplicate" / sidecar.name
        self.write_metadata(duplicate, json.loads(sidecar.read_text()))
        with self.assertRaisesRegex(curator.EvidenceError, "Ambiguous duplicate"):
            self.curate()

    def test_old_marker_without_sidecar_inventory_cannot_authorize_npc_evidence(self):
        self.npc()
        stamp = json.loads((self.root / self.marker).read_text())
        del stamp["preexistingNpcMetadata"]
        (self.root / self.marker).write_text(json.dumps(stamp))
        with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
            self.curate()

    def test_new_fallback_framing_fields_cannot_opt_into_partial_warning_validation(self):
        _, sidecar = self.npc()
        value = json.loads(sidecar.read_text())
        value.update(framing="legacy_wide", fov=60, warningCoverage="full_lane")
        self.write_metadata(sidecar, value)
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 1)
        for changes in ({"framing": "body_close", "fov": 50, "warningCoverage": "visible_portion"},
                        {"warningCoverage": "visible_portion"}, {"fov": 50}):
            self.write_metadata(sidecar, {**value, **changes})
            with self.assertRaises(curator.EvidenceError):
                curator.curate(self.root, self.source, self.marker, "review/invalid", self.identity)
        value["warnings"][0].update(sampledBins=0, coloredBins=0, **{"from": None})
        self.write_metadata(sidecar, value)
        with self.assertRaises(curator.EvidenceError):
            curator.curate(self.root, self.source, self.marker, "review/invalid", self.identity)


class ArticulatedCuratorTests(CuratorFixture):
    suite = "articulated"

    def full_matrix(self, size=100):
        for filename, indices, has_idle in curator.articulated_capture_matrix():
            info = curator.describe(filename, suite=self.suite)
            prefix = filename.removesuffix(info["frameLabel"] + ".png")
            labels = [("sample_" if has_idle else "frame_") + str(index) for index in indices]
            if has_idle:
                labels += ["idle_before", "idle_after"]
            for label in labels:
                self.shot(prefix + label + ".png", size=size)

    def test_armor_and_hud_exact_fixture_labels_and_equipment_are_not_conflated(self):
        for equipment, expected in (("full", "netherite_full"), ("partial", "netherite_chestplate_and_leggings")):
            for hand in ("left", "right"):
                for view, owner_camera in curator.ARTICULATED_ARMOR_VIEWS.items():
                    for index in (2, 5):
                        name = f"articulated_armor_live_{equipment}_{hand}_{view}_frame_{index}.png"
                        info = curator.describe(name, suite=self.suite)
                        self.assertEqual((info["sourceSuite"], info["equipment"], info["hand"], info["view"]),
                                         (curator.ARTICULATED_ARMOR_SUITE, expected, hand, owner_camera))
                        self.assertEqual((info["armorEnchantment"], info["armorTrim"]), ("protection_iv", "gold_sentry"))
                        self.assertEqual((info["phase"], info["sampleIndex"], info["phaseBasis"]),
                                         ("sample", index, "filename_loop_index_only"))
                        self.assertIsNone(info["viewport"])
                        self.assertIsNone(info["uiScale"])
                        self.assertIsNone(curator.describe(name))
        for width, height, scale in ((854, 480, 2), (1280, 720, 3), (1280, 960, 4), (1920, 810, 3)):
            for equipment in ("skin", "netherite"):
                for hand in ("left", "right"):
                    for label in ("idle_before", "sample_0", "sample_9", "idle_after"):
                        name = f"articulated_hud_{width}x{height}_gui{scale}_{equipment}_{hand}_{label}.png"
                        info = curator.describe(name, suite=self.suite)
                        self.assertEqual(info["sourceSuite"], curator.ARTICULATED_HUD_SUITE)
                        self.assertEqual(info["equipment"], "skin" if equipment == "skin" else "netherite_chestplate")
                        self.assertEqual(info["armorEnchantment"], None if equipment == "skin" else "protection_iv")
                        self.assertIsNone(info["armorTrim"])
                        self.assertEqual(info["view"], "owner_first_person")
                        self.assertEqual(info["viewport"], {"width": width, "height": height})
                        self.assertEqual(info["uiScale"], scale)
                        self.assertTrue(info["hudVisible"])
                        self.assertEqual(info["phase"], "sample" if label.startswith("sample_") else label)
                        self.assertIsNone(curator.describe(name))

    def test_hud_viewport_whitelist_matches_native_fixture(self):
        fixture = curator.ROOT / "src/gametest/java/dev/wildercord/client/combat/ArticulatedFirstPersonCompositionTest.java"
        configured = re.findall(r"new Viewport\((\d+), (\d+), (\d+)\)", fixture.read_text())
        self.assertEqual(tuple(tuple(map(int, config)) for config in configured), curator.ARTICULATED_VIEWPORTS)

    def test_unknown_armor_frames_viewports_scales_and_phases_stay_excluded(self):
        for name in (
            "articulated_armor_live_full_left_third_frame_2.png",
            "articulated_armor_live_full_left_observer_frame_2.png",
            "articulated_armor_live_partial_right_first_frame_0.png",
            "articulated_armor_live_full_left_third_front_frame_8.png",
            "articulated_armor_live_full_left_third_back_frame_02.png",
            "articulated_armor_live_full_left_first_active.png",
            "articulated_armor_live_chest_left_first_frame_2.png",
            "articulated_hud_1280x720_gui2_skin_left_sample_0.png",
            "articulated_hud_1280x960_gui3_skin_left_sample_0.png",
            "articulated_hud_1920x1080_gui3_skin_left_sample_0.png",
            "articulated_hud_854x480_gui2_skin_left_sample_10.png",
            "articulated_hud_854x480_gui2_skin_left_sample_00.png",
            "articulated_hud_854x480_gui2_skin_left_active.png",
            "articulated_hud_854x480_gui2_full_left_idle_before.png",
            "articulated_hud_854x480_gui2_netherite_left_third_sample_0.png",
        ):
            with self.subTest(name=name):
                self.assertIsNone(curator.describe(name, suite=self.suite))
                self.shot(name)
        result = self.curate()
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["freshness"]["ignoredOutOfScopePngs"], 15)

    def test_full_small_matrix_is_deterministic_fits_manifest_and_preserves_all_bytes(self):
        self.full_matrix()
        before = {path: path.read_bytes() for path in (self.root / self.source).rglob("*.png")}
        result = self.curate()
        second = curator.curate(self.root, self.source, self.marker, "review/repeated", self.identity, suite=self.suite)
        self.assertEqual(result, second)
        self.assertEqual(result["selectedFrameCount"], 116)
        self.assertEqual(result["sourceSuites"], list(curator.ARTICULATED_SOURCE_SUITES))
        self.assertEqual({frame["sourceSuite"] for frame in result["frames"]}, set(curator.ARTICULATED_OWNER_SOURCE_SUITES))
        self.assertEqual(result["provenance"], self.identity)
        manifest_path = self.root / self.output / "manifest.json"
        self.assertLessEqual(manifest_path.stat().st_size, result["limits"]["manifestReserveBytes"])
        self.assertEqual(json.loads(manifest_path.read_text()), result)
        for frame in result["frames"]:
            original = before[self.root / frame["sourcePath"]]
            self.assertEqual((self.root / self.output / frame["artifactPath"]).read_bytes(), original)
            self.assertEqual(frame["sha256"], hashlib.sha256(original).hexdigest())
        self.assertEqual({path: path.read_bytes() for path in before}, before)
        self.assertTrue(all(not row["omittedForBudgetSamplePositions"] for row in result["coverage"]))

    def test_budget_prefers_body_full_front_back_and_hud_comparison_before_breadth(self):
        self.full_matrix(size=1_000_000)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 6_000_000)
        self.assertEqual({Path(frame["sourcePath"]).name for frame in result["frames"]}, {
            "articulated_live_left_first_frame_0.png", "articulated_live_left_third_frame_0.png",
            "articulated_armor_live_full_left_third_front_frame_2.png",
            "articulated_armor_live_full_left_third_back_frame_2.png",
            "articulated_hud_1280x720_gui3_skin_left_sample_5.png",
            "articulated_hud_1280x720_gui3_netherite_left_sample_5.png",
        })
        self.assertEqual(result["selectedPngBytes"], 6_000_000)
        self.assertLessEqual(sum(path.stat().st_size for path in (self.root / self.output).rglob("*") if path.is_file()),
                             result["limits"]["totalBytesLimit"])
        self.assertTrue(all(not row["missingSamplePositions"] for row in result["coverage"]))

    def test_default_budget_bounds_all_suites_and_preserves_core_comparisons(self):
        self.full_matrix(size=700_000)
        result = self.curate()
        files = [path for path in (self.root / self.output).rglob("*") if path.is_file()]
        self.assertLessEqual(sum(path.stat().st_size for path in files), 14_000_000)
        self.assertEqual(result["selectedFrameCount"], 18)
        for hand in ("left", "right"):
            selected = [frame for frame in result["frames"] if frame["hand"] == hand]
            self.assertEqual({frame["sourceSuite"] for frame in selected}, set(curator.ARTICULATED_OWNER_SOURCE_SUITES))
            self.assertTrue(any(frame["equipment"] == "netherite_full" and frame["view"].endswith("third_person_back")
                                for frame in selected))
            hud = [frame for frame in selected if frame["sourceSuite"] == curator.ARTICULATED_HUD_SUITE]
            self.assertEqual({frame["equipment"] for frame in hud}, {"skin", "netherite_chestplate"})

    def test_missing_counterparts_and_idle_frames_are_explicit_without_invented_samples(self):
        self.shot("articulated_armor_live_full_left_third_back_frame_5.png")
        self.shot("articulated_hud_1280x720_gui3_skin_left_sample_7.png")
        self.shot("articulated_hud_854x480_gui2_netherite_right_idle_after.png")
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 3)
        armor = next(row for row in result["coverage"] if row["scene"] == "articulated_armor_live_full_left"
                     and row["view"].endswith("third_person_back"))
        self.assertEqual(armor["uncapturedLoopIndices"], [2])
        self.assertEqual(armor["selectedFrameLabels"], ["frame_5"])
        hud = next(row for row in result["coverage"] if row["scene"] == "articulated_hud_854x480_gui2_netherite_right")
        self.assertEqual(hud["missingSamplePositions"], list(curator.SAMPLE_POSITIONS))
        self.assertEqual(hud["missingIdleLabels"], ["idle_before"])
        self.assertEqual(hud["capturedIdleLabels"], ["idle_after"])
        self.assertEqual(hud["selectedFrameLabels"], ["idle_after"])

    def test_hud_comparison_is_not_split_when_the_pair_exceeds_remaining_budget(self):
        for equipment in ("skin", "netherite"):
            self.shot(f"articulated_hud_1280x720_gui3_{equipment}_left_sample_2.png", size=100)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual(result["frames"], [])
        rows = [row for row in result["coverage"] if row["capturedSampleIndices"]]
        self.assertEqual(len(rows), 2)
        self.assertTrue(all(row["omittedForBudgetSamplePositions"] == list(curator.SAMPLE_POSITIONS) for row in rows))
        self.assertTrue(all(not row["missingSamplePositions"] for row in rows))

    def test_temporal_extras_do_not_split_a_skipped_body_armor_or_hud_pair(self):
        pairs = (
            ("articulated_live_left_first_frame_", "articulated_live_left_third_frame_", (0, 4)),
            ("articulated_armor_live_full_left_third_front_frame_",
             "articulated_armor_live_full_left_third_back_frame_", (2, 5)),
            ("articulated_hud_1280x720_gui3_skin_left_sample_",
             "articulated_hud_1280x720_gui3_netherite_left_sample_", (0, 2)),
        )
        for first, second, indices in pairs:
            for prefix in (first, second):
                for index in indices:
                    self.shot(f"{prefix}{index}.png")
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual(result["frames"], [])
        rows = [row for row in result["coverage"] if row["capturedSampleIndices"]]
        self.assertEqual(len(rows), 6)
        self.assertTrue(all(row["omittedForBudgetSamplePositions"] == list(curator.SAMPLE_POSITIONS) for row in rows))

    def test_absent_reference_viewport_uses_available_hud_pair_before_armor_breadth(self):
        self.full_matrix(size=100)
        for path in (self.root / self.source).glob("articulated_hud_1280x720_*.png"):
            path.unlink()
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 600)
        hud = [frame for frame in result["frames"] if frame["sourceSuite"] == curator.ARTICULATED_HUD_SUITE]
        self.assertEqual(len(hud), 2)
        self.assertEqual({frame["equipment"] for frame in hud}, {"skin", "netherite_chestplate"})
        self.assertTrue(all(frame["viewport"] == {"width": 854, "height": 480} and frame["hand"] == "left"
                            for frame in hud))
        missing = [row for row in result["coverage"] if "1280x720_gui3" in row["scene"]]
        self.assertTrue(all(row["missingSamplePositions"] == list(curator.SAMPLE_POSITIONS) for row in missing))

    def test_armor_and_hud_stale_paths_and_older_timestamps_are_rejected(self):
        prior = self.shot("articulated_armor_live_full_left_third_front_frame_2.png")
        marker = "review/later-run.json"
        curator.prepare(self.root, self.source, marker, self.identity, suite=self.suite)
        os.utime(prior, None)
        old = self.shot("articulated_hud_1280x720_gui3_netherite_left_sample_4.png")
        os.utime(old, ns=(1, 1))
        self.started = json.loads((self.root / marker).read_text())["startedNs"]
        fresh = self.shot("articulated_hud_1280x720_gui3_skin_left_idle_after.png")
        result = curator.curate(self.root, self.source, marker, self.output, self.identity, suite=self.suite)
        self.assertEqual(result["freshness"]["excludedStalePngs"], 2)
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [fresh.name])

    def test_only_real_supported_hand_view_and_loop_names_are_eligible(self):
        for hand in curator.ARTICULATED_HANDS:
            for view in curator.ARTICULATED_VIEWS:
                for index in range(9):
                    name = f"articulated_live_{hand}_{view}_frame_{index}.png"
                    info = curator.describe(name, suite=self.suite)
                    self.assertEqual((info["hand"], info["view"], info["sampleIndex"]),
                                     (hand, curator.ARTICULATED_VIEWS[view], index))
                    self.assertEqual(info["phase"], "sample")
                    self.assertEqual(info["phaseBasis"], "filename_loop_index_only")
                    self.assertIsNone(curator.describe(name))
        for name in ("articulated_live_left_first_frame_9.png", "articulated_live_right_third_frame_00.png",
                     "articulated_live_left_observer_frame_1.png", "articulated_live_left_first_windup.png",
                     "articulated_live_right_third_recovery.png", "articulated_left_first_frame_1.png",
                     "articulated_live_master_first_frame_1.png", "articulated_model_pose.png",
                     "articulated_hitstop_frame_1.png", "articulated_live_LEFT_first_frame_1.png",
                     "masters_art_0_first_frame_1.png"):
            with self.subTest(name=name):
                self.assertIsNone(curator.describe(name, suite=self.suite))

    def test_four_combinations_use_first_upper_median_and_last_available_samples(self):
        for hand in curator.ARTICULATED_HANDS:
            for view in curator.ARTICULATED_VIEWS:
                for index in (8, 1, 3, 6):
                    self.shot(f"articulated_live_{hand}_{view}_frame_{index}.png")
        result = self.curate()
        self.assertEqual(result["suiteGroup"], "articulated")
        self.assertEqual(result["suite"], curator.ARTICULATED_SUITE)
        self.assertEqual(result["provenance"], self.identity)
        self.assertEqual(result["selectedFrameCount"], 12)
        self.assertEqual(len(result["coverage"]), 32)
        self.assertEqual({(row["hand"], row["view"]) for row in result["coverage"]
                          if row["sourceSuite"] == curator.ARTICULATED_SUITE},
                         {(hand, view) for hand in curator.ARTICULATED_HANDS for view in curator.ARTICULATED_VIEWS.values()})
        for row in result["coverage"][:4]:
            self.assertEqual(row["capturedSampleIndices"], [1, 3, 6, 8])
            self.assertEqual(row["uncapturedLoopIndices"], [0, 2, 4, 5, 7])
            self.assertEqual(row["selectedFrameLabels"], ["frame_1", "frame_6", "frame_8"])
            self.assertEqual(row["availableSamplePositions"],
                             dict(zip(curator.SAMPLE_POSITIONS, ("frame_1", "frame_6", "frame_8"))))
            self.assertEqual(row["missingSamplePositions"], [])
            self.assertEqual(row["omittedForBudgetSamplePositions"], [])
        self.assertTrue(all(frame["captureKind"] == "native_local_owner_accepted_input_combat"
                            and frame["phase"] == "sample" for frame in result["frames"]))
        self.assertEqual(result["testVerdict"]["authoritativeArtifact"], "articulated-native-evidence")
        self.assertEqual(result["testVerdict"]["authoritativeManifest"], "articulated-native-manifest.json")
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])
        self.assertEqual({row["coverage"] for row in result["unavailableRequestedCoverage"]},
                         {"observer_client_of_player_combat", "human_multiplayer_duel", "synthetic_geometry_and_hitstop_screenshots",
                          "exact_player_impact_phase", "exact_player_recovery_phase"})
        second = curator.curate(self.root, self.source, self.marker, "review/another", self.identity,
                                suite=self.suite)
        self.assertEqual(result, second)

    def test_missing_captures_report_all_three_suites_and_exact_configurations(self):
        result = self.curate()
        self.assertEqual(result["status"], "unavailable")
        self.assertEqual(result["frames"], [])
        self.assertEqual(len(result["coverage"]), 32)
        for row in result["coverage"]:
            self.assertEqual(row["capturedSampleIndices"], [])
            expected = (list(range(9)) if row["sourceSuite"] == curator.ARTICULATED_SUITE else
                        [2, 5] if row["sourceSuite"] == curator.ARTICULATED_ARMOR_SUITE else list(range(10)))
            self.assertEqual(row["uncapturedLoopIndices"], expected)
            self.assertEqual(row["missingIdleLabels"],
                             ["idle_before", "idle_after"] if row["sourceSuite"] == curator.ARTICULATED_HUD_SUITE else [])
            self.assertEqual(row["missingSamplePositions"], list(curator.SAMPLE_POSITIONS))
        self.assertTrue((self.root / self.output / "manifest.json").is_file())

    def test_single_and_two_samples_are_not_duplicated_to_invent_a_triptych(self):
        self.shot("articulated_live_left_first_frame_2.png")
        self.shot("articulated_live_right_third_frame_1.png")
        self.shot("articulated_live_right_third_frame_7.png")
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 3)
        self.assertEqual(result["selectedPngBytes"], 300)
        self.assertEqual(result["frames"][0]["samplePositions"], list(curator.SAMPLE_POSITIONS))
        self.assertEqual(result["frames"][1]["samplePositions"], ["first_available"])
        self.assertEqual(result["frames"][2]["samplePositions"], ["middle_available", "last_available"])
        self.assertEqual(len(list((self.root / self.output / "frames").glob("*.png"))), 3)

    def test_articulated_budget_includes_manifest_and_preserves_bytes_and_hashes(self):
        for hand in curator.ARTICULATED_HANDS:
            for view in curator.ARTICULATED_VIEWS:
                for index in (0, 4, 8):
                    self.shot(f"articulated_live_{hand}_{view}_frame_{index}.png", size=1_200_000)
        result = self.curate()
        files = [path for path in (self.root / self.output).rglob("*") if path.is_file()]
        self.assertLessEqual(sum(path.stat().st_size for path in files), 14_000_000)
        self.assertEqual(result["selectedFrameCount"], 10)
        self.assertEqual(result["selectedPngBytes"], 12_000_000)
        self.assertTrue(any(row["omittedForBudgetSamplePositions"] for row in result["coverage"]))
        archive = self.root / "curated.zip"
        with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in files:
                zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLess(archive.stat().st_size, 15_000_000)
        for frame in result["frames"]:
            original = (self.root / frame["sourcePath"]).read_bytes()
            copied = (self.root / self.output / frame["artifactPath"]).read_bytes()
            self.assertEqual(original, copied)
            self.assertEqual(frame["sha256"], hashlib.sha256(original).hexdigest())
            self.assertEqual(frame["bytes"], len(original))

    def test_captured_but_unaffordable_sample_is_not_reported_as_missing(self):
        self.shot("articulated_live_left_first_frame_2.png")
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE)
        row = result["coverage"][0]
        self.assertEqual(result["status"], "unavailable")
        self.assertEqual(row["capturedSampleIndices"], [2])
        self.assertEqual(row["missingSamplePositions"], [])
        self.assertEqual(row["omittedForBudgetSamplePositions"], list(curator.SAMPLE_POSITIONS))

    def test_marker_is_bound_to_suite_and_cannot_mix_masters_with_articulated(self):
        stamp = json.loads((self.root / self.marker).read_text())
        self.assertEqual(stamp["suiteGroup"], self.suite)
        self.assertEqual(stamp["suite"], curator.ARTICULATED_SUITE)
        with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
            curator.curate(self.root, self.source, self.marker, self.output, self.identity)
        other_marker = "review/masters-run.json"
        curator.prepare(self.root, self.source, other_marker, self.identity)
        with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
            curator.curate(self.root, self.source, other_marker, self.output, self.identity, suite=self.suite)
        with self.assertRaises(FileExistsError):
            curator.prepare(self.root, self.source, self.marker, self.identity)
        # A marker without explicit suite identity must not silently mean Masters.
        for field in ("suiteGroup", "suite", "sourceSuites"):
            altered = {key: value for key, value in stamp.items() if key != field}
            (self.root / self.marker).write_text(json.dumps(altered))
            with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
                self.curate()

    def test_mixed_source_directory_never_admits_other_suite_or_synthetic_frames(self):
        articulated = "articulated_live_left_first_frame_1.png"
        masters = "masters_art_0_first_active.png"
        curator.prepare(self.root, self.source, "review/masters-run.json", self.identity)
        self.started = json.loads((self.root / "review/masters-run.json").read_text())["startedNs"]
        for name in (articulated, masters, "articulated_model_pose.png", "articulated_hitstop_frame_1.png"):
            self.shot(name)
        result = self.curate()
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [articulated])
        self.assertEqual(result["freshness"]["ignoredOutOfScopePngs"], 3)
        result = curator.curate(self.root, self.source, "review/masters-run.json", "review/masters",
                                self.identity)
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [masters])
        self.assertEqual(result["freshness"]["ignoredOutOfScopePngs"], 3)

    def test_articulated_rejects_prior_paths_old_mtimes_and_other_run_identity(self):
        prior = self.shot("articulated_live_left_first_frame_1.png")
        curator.prepare(self.root, self.source, "review/later-run.json", self.identity, suite=self.suite)
        old = self.shot("articulated_live_right_third_frame_1.png")
        os.utime(prior, None)
        os.utime(old, ns=(1, 1))
        result = curator.curate(self.root, self.source, "review/later-run.json", self.output,
                                self.identity, suite=self.suite)
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["freshness"]["excludedStalePngs"], 2)
        with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
            curator.curate(self.root, self.source, self.marker, "review/wrong-run",
                           {**self.identity, "runAttempt": "3"}, suite=self.suite)
        with self.assertRaisesRegex(curator.EvidenceError, "Output already exists"):
            self.curate()

    def test_articulated_rejects_symlinks_duplicates_and_invalid_pngs(self):
        original = self.shot("articulated_live_left_first_frame_1.png")
        duplicate = self.shot(original.name, prefix="nested")
        with self.assertRaisesRegex(curator.EvidenceError, "Ambiguous duplicate"):
            self.curate()
        duplicate.unlink()
        duplicate.symlink_to(original)
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            self.curate()
        duplicate.unlink()
        original.write_bytes(b"invalid PNG bytes")
        with self.assertRaisesRegex(curator.EvidenceError, "PNG signature"):
            self.curate()
        self.assertFalse((self.root / self.output).exists())

    def test_cli_prepare_and_curate_use_the_requested_suite(self):
        marker = "review/cli-run.json"
        output = "review/cli-output"
        with patch.object(curator, "ROOT", self.root), patch.object(curator, "provenance", return_value=self.identity):
            self.assertEqual(curator.main(["--suite", self.suite, "--prepare", "--marker", marker]), 0)
            self.started = json.loads((self.root / marker).read_text())["startedNs"]
            self.shot("articulated_live_right_first_frame_3.png")
            with patch("builtins.print"):
                self.assertEqual(curator.main(["--suite", self.suite, "--marker", marker, "--output", output]), 0)
        manifest = json.loads((self.root / output / "manifest.json").read_text())
        self.assertEqual(manifest["suite"], curator.ARTICULATED_SUITE)
        self.assertEqual(manifest["selectedFrameCount"], 1)


class SharedPlayerCuratorTests(CuratorFixture):
    suite = "articulated"

    def full_shared_matrix(self, size=100):
        for art in ("rising_break", "driving_cut"):
            for camera in ("third", "hud"):
                viewports = ((1280, 720, 3),) if camera == "third" else (
                    (854, 480, 2), (1280, 720, 3), (1280, 960, 4), (1920, 810, 3))
                for width, height, scale in viewports:
                    for gear in ("skin", "netherite"):
                        for hand in ("left", "right"):
                            for phase in ("windup", "active", "recovery"):
                                self.shot(f"articulated_shared_{art}_{camera}_{width}x{height}_gui{scale}_"
                                          f"{gear}_{hand}_requested_{phase}.png", size=size)

    def shared_shot(self, *, art="rising_break", camera="third", gear="skin", hand="left", phase="active", size=100):
        return self.shot(f"articulated_shared_{art}_{camera}_1280x720_gui3_{gear}_{hand}_requested_{phase}.png", size=size)

    def test_all_120_exact_names_remain_requested_phases_with_unknown_receipts(self):
        self.full_shared_matrix()
        result = self.curate()
        self.assertEqual(len(result["frames"]), 120)
        self.assertEqual(len(result["sharedPlayerCoverage"]), 40)
        for row in result["sharedPlayerCoverage"]:
            self.assertEqual(row["capturedRequestedPhases"], ["windup", "active", "recovery"])
            self.assertEqual(row["selectedFrameLabels"], ["requested_windup", "requested_active", "requested_recovery"])
            self.assertEqual(row["missingRequestedPhases"], [])
            self.assertEqual(row["omittedForBudgetRequestedPhases"], [])
            self.assertEqual(row["verifiedRenderedPhases"], [])
            self.assertEqual(row["renderedPhase"], "unknown")
        for frame in result["frames"]:
            filename = Path(frame["sourcePath"]).name
            self.assertEqual(frame["frameLabel"], "requested_" + frame["requestedPhase"])
            self.assertTrue(filename.endswith(frame["frameLabel"] + ".png"))
            self.assertEqual(frame["requestedPhaseBasis"], "filename_only")
            self.assertEqual(frame["phase"], "unknown")
            self.assertEqual(frame["renderedPhase"], "unknown")
            self.assertEqual(frame["phaseBasis"], "rendered_phase_unverified")
            self.assertEqual(frame["viewportBasis"], "filename_only")
            self.assertEqual(frame["exactImpactPixelCoverage"], "unverified")
            self.assertTrue(frame["nativePixelReviewRequired"])
            receipt = frame["preCaptureReceipt"]
            self.assertEqual(receipt["status"], "not_ingested")
            self.assertEqual(receipt["preCapturePhase"], "unknown")
            self.assertEqual(receipt["sourceArtifact"], "articulated-native-evidence")
            self.assertEqual(receipt["logRecord"], "ARTICULATED_SHARED_SAMPLE name=" + filename[:-4])
            for key in ("acceptedMove", "activation", "windup", "recovery", "actualSkin", "preCaptureAge"):
                self.assertIsNone(receipt[key])
            self.assertIsNone(curator.describe(filename))
        self.assertEqual(result["selectedMetadataCount"], 0)
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_camera_gear_and_fixture_details_have_separate_explicit_bases(self):
        for camera in ("third", "hud"):
            for gear in ("skin", "netherite"):
                path = self.shared_shot(camera=camera, gear=gear)
                info = curator.describe(path.name, suite=self.suite)
                self.assertEqual(info["equipment"], "skin" if gear == "skin" else
                                 "netherite_full" if camera == "third" else "netherite_chestplate")
                self.assertEqual(info["equipmentBasis"], "filename_and_native_fixture_contract")
                self.assertEqual(info["sourceSuite"], curator.ARTICULATED_SUITE if camera == "third" else curator.ARTICULATED_HUD_SUITE)
                self.assertEqual(info["view"], "local_owner_third_person_front" if camera == "third" else "owner_first_person")
                self.assertEqual(info["armorEnchantment"], None if gear == "skin" else "protection_iv")
                self.assertIsNone(info["armorTrim"])
                self.assertEqual(info["hudBasis"], "native_fixture_contract")
                self.assertEqual(info["heldItemBasis"], "native_fixture_contract")

    def test_unknown_names_unrequested_labels_and_third_viewports_are_out_of_scope(self):
        base = "articulated_shared_rising_break_third_1280x720_gui3_skin_left_requested_active.png"
        invalid = [base.replace(before, after) for before, after in (
            ("rising_break", "spellcut"), ("third", "third_back"), ("third", "first"),
            ("1280x720_gui3", "854x480_gui2"), ("gui3", "gui2"), ("skin", "full"),
            ("left", "LEFT"), ("requested_active", "active"), ("requested_active", "requested_idle"),
            ("requested_active", "frame_2"), ("requested_active", "pre_capture_active"),
            (".png", ".png.extra"))]
        invalid += [base.replace("third_1280x720_gui3", "hud_1920x1080_gui3")]
        for filename in invalid:
            with self.subTest(filename=filename):
                self.assertIsNone(curator.describe(filename, suite=self.suite))
        for filename in invalid[:-2]:
            self.shot(filename)
        self.assertEqual(self.curate()["frames"], [])

    def test_missing_requested_captures_remain_explicit_without_phase_substitution(self):
        self.shared_shot(phase="recovery")
        result = self.curate()
        captured = [row for row in result["sharedPlayerCoverage"] if row["capturedRequestedPhases"]]
        self.assertEqual(len(captured), 1)
        self.assertEqual(captured[0]["capturedRequestedPhases"], ["recovery"])
        self.assertEqual(captured[0]["missingRequestedPhases"], ["windup", "active"])
        self.assertEqual(captured[0]["verifiedRenderedPhases"], [])
        self.assertTrue(all(row["missingRequestedPhases"] == ["windup", "active", "recovery"]
                            for row in result["sharedPlayerCoverage"] if not row["capturedRequestedPhases"]))
        self.assertEqual(result["frames"][0]["phase"], "unknown")

    def test_four_art_camera_representatives_precede_old_owner_rows_under_budget(self):
        ArticulatedCuratorTests.full_matrix(self, size=1_000_000)
        self.full_shared_matrix(size=1_000_000)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 6_000_000)
        shared = [frame for frame in result["frames"] if frame.get("form") == "shared_player_art"]
        self.assertEqual([(frame["art"], frame["camera"]) for frame in shared], [
            ("rising_break", "third"), ("driving_cut", "third"), ("rising_break", "hud"), ("driving_cut", "hud")])
        self.assertTrue(all(frame["requestedPhase"] == "active" and frame["hand"] == "left"
                            and frame["equipment"] == "skin" and frame["viewport"] == {"width": 1280, "height": 720}
                            for frame in shared))
        old = [frame for frame in result["frames"] if frame not in shared]
        self.assertEqual({Path(frame["sourcePath"]).name for frame in old}, {
            "articulated_live_left_first_frame_0.png", "articulated_live_left_third_frame_0.png"})
        self.assertEqual(sum(len(row["omittedForBudgetRequestedPhases"]) for row in result["sharedPlayerCoverage"]), 116)
        self.assertTrue(all(not row["missingRequestedPhases"] for row in result["sharedPlayerCoverage"]))

    def test_unavailable_or_oversized_priority_uses_real_fallback_and_keeps_omission(self):
        oversized = self.shared_shot(size=curator.BUDGET + 1)
        fallback = self.shared_shot(camera="hud", art="driving_cut", hand="right", gear="netherite", phase="recovery")
        self.shared_shot(phase="windup")
        result = self.curate()
        self.assertEqual([frame["frameLabel"] for frame in result["frames"]], ["requested_windup", "requested_recovery"])
        self.assertIn(fallback.name, [Path(frame["sourcePath"]).name for frame in result["frames"]])
        row = next(row for row in result["sharedPlayerCoverage"] if row["scene"] == oversized.name.removesuffix("_requested_active.png"))
        self.assertEqual(row["capturedRequestedPhases"], ["windup", "active"])
        self.assertEqual(row["omittedForBudgetRequestedPhases"], ["active"])
        self.assertEqual(row["missingRequestedPhases"], ["recovery"])

    def test_freshness_duplicate_names_symlinks_and_signatures_apply_to_shared_captures(self):
        prior = self.shared_shot()
        marker = "review/second-run.json"
        curator.prepare(self.root, self.source, marker, self.identity, suite=self.suite)
        os.utime(prior, None)
        self.started = json.loads((self.root / marker).read_text())["startedNs"]
        old = self.shared_shot(phase="windup")
        os.utime(old, ns=(1, 1))
        fresh = self.shared_shot(phase="recovery")
        result = curator.curate(self.root, self.source, marker, "review/fresh", self.identity, suite=self.suite)
        self.assertEqual(result["freshness"]["excludedStalePngs"], 2)
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [fresh.name])
        duplicate = self.shot(fresh.name, prefix="duplicate")
        with self.assertRaisesRegex(curator.EvidenceError, "Ambiguous duplicate"):
            self.curate()
        duplicate.unlink()
        duplicate.symlink_to(fresh)
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            self.curate()
        duplicate.unlink()
        fresh.write_bytes(b"not a png")
        with self.assertRaisesRegex(curator.EvidenceError, "PNG signature"):
            self.curate()
        self.assertFalse((self.root / self.output).exists())

    def test_combined_full_matrix_is_deterministic_under_reserve_and_byte_identical(self):
        ArticulatedCuratorTests.full_matrix(self)
        self.full_shared_matrix()
        npc = ArticulatedNpcCuratorTests()
        npc.root, npc.source, npc.started = self.root, self.source, self.started
        npc.full_npc_matrix()
        before = {path: path.read_bytes() for path in (self.root / self.source).rglob("*") if path.is_file()}
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 251)
        self.assertEqual(result["selectedMetadataCount"], 15)
        self.assertEqual(len(result["coverage"]), 32)
        self.assertEqual(len(result["npcCoverage"]), 2)
        self.assertEqual(len(result["sharedPlayerCoverage"]), 40)
        repeat = curator.curate(self.root, self.source, self.marker, "review/repeated", self.identity, suite=self.suite)
        self.assertEqual(result, repeat)
        self.assertLess((self.root / self.output / "manifest.json").stat().st_size, curator.ARTICULATED_MANIFEST_RESERVE)
        self.assertLess(sum(path.stat().st_size for path in (self.root / self.output).rglob("*") if path.is_file()), 14_000_000)
        self.assertEqual(len({frame["sourcePath"] for frame in result["frames"]}), 251)
        for frame in result["frames"]:
            for entry in ([frame, frame["metadata"]] if "metadata" in frame else [frame]):
                original = before[self.root / entry["sourcePath"]]
                self.assertEqual((self.root / self.output / entry["artifactPath"]).read_bytes(), original)
                self.assertEqual(entry["sha256"], hashlib.sha256(original).hexdigest())
        self.assertEqual({path: path.read_bytes() for path in before}, before)

    def test_combined_large_matrix_preserves_budget_and_finalized_npc_priority(self):
        ArticulatedCuratorTests.full_matrix(self, size=500_000)
        self.full_shared_matrix(size=500_000)
        npc = ArticulatedNpcCuratorTests()
        npc.root, npc.source, npc.started = self.root, self.source, self.started
        npc.full_npc_matrix(size=100_000)
        result = self.curate()
        self.assertEqual([frame["phase"] for frame in result["frames"][:6]], [
            "reply_warning", "reply_warning", "release", "release", "recovery", "recovery"])
        self.assertTrue(all(frame["captureStatus"] == "passed" for frame in result["frames"][:6]))
        self.assertEqual({(frame["art"], frame["camera"]) for frame in result["frames"] if "art" in frame}, {
            (art, camera) for art in ("rising_break", "driving_cut") for camera in ("third", "hud")})
        self.assertTrue(any(row["omittedForBudgetRequestedPhases"] for row in result["sharedPlayerCoverage"]))
        self.assertLess(sum(path.stat().st_size for path in (self.root / self.output).rglob("*") if path.is_file()), 14_000_000)
        archive = self.root / "curated.zip"
        with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in (self.root / self.output).rglob("*"):
                if path.is_file():
                    zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLess(archive.stat().st_size, 15_000_000)


class FundedShellCuratorTests(CuratorFixture):
    suite = "articulated"

    def funded_shot(self, *, art="spellcut", camera="first_person", hand="right", gear="netherite", size=100):
        stage = 4 if hand == "right" else 5
        return self.shot(f"articulated_funded_shell_{art}_{camera}_{hand}_stage{stage}_{gear}.png", size=size)

    def full_funded_matrix(self, size=100):
        for art in ("spellcut", "rising_break", "driving_cut"):
            for camera in ("third_person_front", "third_person_back", "first_person"):
                for hand in ("right", "left"):
                    for gear in ("skin", "netherite"):
                        self.funded_shot(art=art, camera=camera, hand=hand, gear=gear, size=size)

    def test_all_36_exact_names_keep_fixture_facts_separate_from_unknown_observations(self):
        self.full_funded_matrix()
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 36)
        self.assertEqual(result["selectedMetadataCount"], 0)
        self.assertEqual(len(result["fundedShellCoverage"]), 36)
        self.assertEqual(len({row["expectedFilename"] for row in result["fundedShellCoverage"]}), 36)
        self.assertEqual(set(result["sourceSuites"]), {*curator.ARTICULATED_OWNER_SOURCE_SUITES,
                                                     curator.ARTICULATED_FUNDED_SHELL_SUITE})
        for row in result["fundedShellCoverage"]:
            self.assertTrue(row["captured"] and row["selected"])
            self.assertFalse(row["missingCapture"] or row["omittedForBudget"])
            self.assertEqual(row["renderedPhase"], "unknown")
            self.assertEqual(row["verifiedRenderedPhases"], [])
        for frame in result["frames"]:
            filename = Path(frame["sourcePath"]).name
            self.assertEqual(frame["sourceSuite"], curator.ARTICULATED_FUNDED_SHELL_SUITE)
            self.assertEqual(frame["form"], "funded_aura_shell")
            self.assertEqual((frame["stage"], frame["school"]), (4, "stone") if frame["hand"] == "right" else (5, "gale"))
            self.assertEqual(frame["initialAura"], 100)
            self.assertEqual(frame["initialAuraBasis"], "native_fixture_contract")
            self.assertEqual(frame["schoolBasis"], "native_fixture_contract")
            self.assertEqual(frame["stageBasis"], "filename_and_native_fixture_contract")
            armored = filename.endswith("_netherite.png")
            self.assertEqual(frame["equipment"], "netherite_full" if armored else "skin")
            self.assertEqual(frame["armorEnchantment"], "protection_iv" if armored else None)
            self.assertEqual(frame["armorTrim"], "gold_sentry" if armored else None)
            self.assertEqual((frame["mainHandItem"], frame["offHandItem"]), ("diamond_sword", "empty"))
            self.assertEqual(frame["heldItemBasis"], "native_fixture_contract")
            self.assertEqual(frame["hudBasis"], "native_fixture_contract")
            self.assertTrue(frame["hudVisible"])
            self.assertIsNone(frame["viewport"])
            self.assertIsNone(frame["uiScale"])
            self.assertEqual(frame["viewportBasis"], "not_encoded_in_filename")
            self.assertEqual((frame["phase"], frame["renderedPhase"]), ("unknown", "unknown"))
            self.assertEqual(frame["phaseBasis"], "rendered_phase_unverified")
            self.assertIsNone(frame["sampleIndex"])
            self.assertNotIn("requestedPhase", frame)
            self.assertNotIn("metadata", frame)
            self.assertTrue(frame["nativePixelReviewRequired"])
            self.assertEqual(frame["exactImpactPixelCoverage"], "unverified")
            receipt = frame["preCaptureReceipt"]
            self.assertEqual(receipt["status"], "not_ingested")
            self.assertEqual(receipt["sourceArtifact"], "articulated-native-evidence")
            self.assertEqual(receipt["logRecord"], "ARTICULATED_FUNDED_SHELL name=" + filename[:-4])
            self.assertEqual(receipt["preCapturePhase"], "unknown")
            for key in ("activation", "shellARGB", "actualSkin", "preCaptureAge"):
                self.assertIsNone(receipt[key])
            self.assertIsNone(curator.describe(filename))
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_impostor_stage_hand_move_equipment_and_camera_names_are_not_admitted(self):
        base = "articulated_funded_shell_spellcut_first_person_right_stage4_netherite.png"
        invalid = [base.replace(before, after) for before, after in (
            ("stage4", "stage5"), ("right", "left"), ("stage4", "stage04"), ("stage4", "stage6"),
            ("right", "RIGHT"), ("spellcut", "Spellcut"), ("spellcut", "kindling_draw"),
            ("first_person", "hud"), ("first_person", "first"), ("first_person", "third_front"),
            ("netherite", "full"), ("netherite", "skin_slim"), ("netherite", "netherite_requested_active"),
            ("funded_shell", "synthetic_shell"), (".png", "_frame_1.png"), (".png", ".png.extra"))]
        invalid += [base.replace("right_stage4", "left_stage4"), base.replace("right_stage4", "right_stage5")]
        for filename in invalid:
            with self.subTest(filename=filename):
                self.assertIsNone(curator.describe(filename, suite=self.suite))
            self.shot(filename)
        result = self.curate()
        self.assertEqual(result["frames"], [])
        self.assertTrue(all(row["missingCapture"] and not row["omittedForBudget"] for row in result["fundedShellCoverage"]))

    def test_missing_and_budget_omissions_are_distinct_for_every_slot(self):
        present = self.funded_shot()
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE)
        captured = [row for row in result["fundedShellCoverage"] if row["captured"]]
        self.assertEqual(len(captured), 1)
        self.assertEqual(captured[0]["expectedFilename"], present.name)
        self.assertTrue(captured[0]["omittedForBudget"])
        self.assertFalse(captured[0]["missingCapture"] or captured[0]["selected"])
        self.assertEqual(sum(row["missingCapture"] for row in result["fundedShellCoverage"]), 35)

    def test_native_logs_and_unbound_json_do_not_create_observed_receipts_or_phase_claims(self):
        image = self.funded_shot()
        image.with_suffix(".json").write_text(json.dumps({"actualSkin": "SLIM", "renderedPhase": "ACTIVE",
            "activation": 123, "shellARGB": "c0ff2040", "captureStatus": "passed"}))
        log = image.parent / "latest.log"
        log.write_text("ARTICULATED_FUNDED_SHELL name=" + image.stem
                       + " activation=123 shellGlow=c0ff2040 actualSkin=SLIM preCapturePhase=ACTIVE preCaptureAge=6.5")
        result = self.curate()
        receipt = result["frames"][0]["preCaptureReceipt"]
        self.assertIsNone(receipt["activation"])
        self.assertIsNone(receipt["actualSkin"])
        self.assertIsNone(receipt["shellARGB"])
        self.assertEqual(result["frames"][0]["renderedPhase"], "unknown")
        self.assertEqual(result["selectedMetadataCount"], 0)
        self.assertEqual(len(list((self.root / self.output / "frames").iterdir())), 1)

    def test_twelve_representatives_cover_requested_art_camera_gear_and_both_hands(self):
        self.full_funded_matrix(size=1_000_000)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 12_000_000)
        expected = {(art, "first_person", "netherite_full", hand)
                    for art in ("spellcut", "rising_break", "driving_cut") for hand in ("right", "left")}
        expected.update((art, "third_person_front", "skin", hand)
                        for art in ("spellcut", "driving_cut") for hand in ("right", "left"))
        expected.update(("rising_break", "third_person_back", "netherite_full", hand) for hand in ("right", "left"))
        self.assertEqual({(frame["art"], frame["camera"], frame["equipment"], frame["hand"])
                          for frame in result["frames"]}, expected)
        self.assertEqual(sum(row["omittedForBudget"] for row in result["fundedShellCoverage"]), 24)

    def test_funded_priority_pair_cannot_be_split_by_later_retry_but_missing_mate_is_not_invented(self):
        right = self.funded_shot()
        left = self.funded_shot(hand="left")
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual(result["frames"], [])
        self.assertEqual(sum(row["omittedForBudget"] for row in result["fundedShellCoverage"]), 2)
        right.unlink()
        result = curator.curate(self.root, self.source, self.marker, "review/missing-mate", self.identity,
                                suite=self.suite, budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [left.name])
        self.assertEqual(sum(row["missingCapture"] for row in result["fundedShellCoverage"]), 35)

    def test_budget_rounds_share_representatives_without_starving_old_comparison_pair(self):
        self.full_funded_matrix(size=1_000_000)
        SharedPlayerCuratorTests.full_shared_matrix(self, size=1_000_000)
        ArticulatedCuratorTests.full_matrix(self, size=1_000_000)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 6_000_000)
        funded = [frame for frame in result["frames"] if frame.get("form") == "funded_aura_shell"]
        shared = [frame for frame in result["frames"] if frame.get("form") == "shared_player_art"]
        old = [frame for frame in result["frames"] if frame not in funded + shared]
        self.assertEqual([(frame["art"], frame["camera"]) for frame in shared],
                         [("rising_break", "third"), ("driving_cut", "third")])
        self.assertEqual({(frame["art"], frame["camera"], frame["equipment"], frame["hand"]) for frame in funded},
                         {("spellcut", "first_person", "netherite_full", hand) for hand in ("right", "left")})
        self.assertEqual({Path(frame["sourcePath"]).name for frame in old},
                         {"articulated_live_left_first_frame_0.png", "articulated_live_left_third_frame_0.png"})
        self.assertEqual(sum(row["omittedForBudget"] for row in result["fundedShellCoverage"]), 34)
        self.assertFalse(any(row["missingCapture"] for row in result["fundedShellCoverage"]))

    def test_all_287_selected_records_fit_reserve_and_preserve_hashes_source_bytes_and_old_groups(self):
        self.full_funded_matrix()
        SharedPlayerCuratorTests.full_shared_matrix(self)
        ArticulatedCuratorTests.full_matrix(self)
        npc = ArticulatedNpcCuratorTests()
        npc.root, npc.source, npc.started = self.root, self.source, self.started
        npc.full_npc_matrix()
        before = {path: path.read_bytes() for path in (self.root / self.source).rglob("*") if path.is_file()}
        result = self.curate()
        self.assertEqual((result["selectedFrameCount"], result["selectedMetadataCount"]), (287, 15))
        self.assertEqual((len(result["coverage"]), len(result["npcCoverage"]), len(result["sharedPlayerCoverage"])), (32, 2, 40))
        self.assertEqual([frame["phase"] for frame in result["frames"][:6]],
                         ["reply_warning", "reply_warning", "release", "release", "recovery", "recovery"])
        self.assertTrue(all(frame["captureStatus"] == "passed" for frame in result["frames"][:6]))
        repeat = curator.curate(self.root, self.source, self.marker, "review/repeated", self.identity, suite=self.suite)
        self.assertEqual(result, repeat)
        manifest_size = (self.root / self.output / "manifest.json").stat().st_size
        self.assertGreater(manifest_size, 640_000)
        self.assertLess(manifest_size, curator.ARTICULATED_MANIFEST_RESERVE)
        self.assertEqual(result["limits"]["totalBytesLimit"], 14_000_000)
        self.assertEqual(len({frame["sourcePath"] for frame in result["frames"]}), 287)
        for frame in result["frames"]:
            for entry in ([frame, frame["metadata"]] if "metadata" in frame else [frame]):
                original = before[self.root / entry["sourcePath"]]
                self.assertEqual((self.root / self.output / entry["artifactPath"]).read_bytes(), original)
                self.assertEqual(entry["sha256"], hashlib.sha256(original).hexdigest())
        self.assertEqual({path: path.read_bytes() for path in before}, before)

    def test_large_combined_matrix_keeps_finalized_npc_priority_and_atomic_comparisons_below_cap(self):
        self.full_funded_matrix(size=500_000)
        SharedPlayerCuratorTests.full_shared_matrix(self, size=500_000)
        ArticulatedCuratorTests.full_matrix(self, size=500_000)
        npc = ArticulatedNpcCuratorTests()
        npc.root, npc.source, npc.started = self.root, self.source, self.started
        npc.full_npc_matrix(size=100_000)
        result = self.curate()
        self.assertEqual([frame["phase"] for frame in result["frames"][:6]],
                         ["reply_warning", "reply_warning", "release", "release", "recovery", "recovery"])
        self.assertTrue(all(frame["captureStatus"] == "passed" and "metadata" in frame for frame in result["frames"][:6]))
        old = [frame for frame in result["frames"] if frame["view"] != curator.NPC_VIEW and "form" not in frame]
        self.assertEqual({frame["sourceSuite"] for frame in old}, set(curator.ARTICULATED_OWNER_SOURCE_SUITES))
        for hand in ("left", "right"):
            for source in curator.ARTICULATED_OWNER_SOURCE_SUITES:
                self.assertIn(sum(frame["hand"] == hand and frame["sourceSuite"] == source for frame in old), (0, 2))
        funded = [frame for frame in result["frames"] if frame.get("form") == "funded_aura_shell"]
        self.assertGreaterEqual(len(funded), 2)
        for art, camera, equipment in curator.FUNDED_SHELL_PRIORITY:
            pair = [frame for frame in funded if frame["art"] == art and frame["camera"] == camera
                    and frame["equipment"] == ("netherite_full" if equipment == "netherite" else "skin")]
            self.assertIn(len(pair), (0, 2))
        self.assertTrue(any(row["omittedForBudget"] for row in result["fundedShellCoverage"]))
        self.assertTrue(any(row["omittedForBudgetRequestedPhases"] for row in result["sharedPlayerCoverage"]))
        files = [path for path in (self.root / self.output).rglob("*") if path.is_file()]
        self.assertLessEqual(sum(path.stat().st_size for path in files), 14_000_000)
        with zipfile.ZipFile(self.root / "curated.zip", "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in files:
                zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLess((self.root / "curated.zip").stat().st_size, 15_000_000)

    def test_funded_names_obey_existing_freshness_duplicate_signature_and_path_guards(self):
        prior = self.funded_shot()
        marker = "review/second-run.json"
        curator.prepare(self.root, self.source, marker, self.identity, suite=self.suite)
        self.started = json.loads((self.root / marker).read_text())["startedNs"]
        os.utime(prior, None)
        old = self.funded_shot(hand="left")
        os.utime(old, ns=(1, 1))
        fresh = self.funded_shot(art="driving_cut")
        result = curator.curate(self.root, self.source, marker, "review/fresh", self.identity, suite=self.suite)
        self.assertEqual(result["freshness"]["excludedStalePngs"], 2)
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [fresh.name])
        duplicate = self.shot(fresh.name, prefix="nested")
        with self.assertRaisesRegex(curator.EvidenceError, "Ambiguous duplicate"):
            self.curate()
        duplicate.unlink()
        duplicate.symlink_to(fresh)
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            self.curate()
        duplicate.unlink()
        fresh.write_bytes(b"not a png")
        with self.assertRaisesRegex(curator.EvidenceError, "PNG signature"):
            self.curate()
        self.assertFalse((self.root / self.output).exists())

    def test_three_suite_marker_cannot_claim_funded_suite_provenance(self):
        self.funded_shot()
        stamp = json.loads((self.root / self.marker).read_text())
        stamp["sourceSuites"] = list(curator.ARTICULATED_OWNER_SOURCE_SUITES)
        (self.root / self.marker).write_text(json.dumps(stamp))
        with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
            self.curate()


class OpeningStyleCuratorTests(CuratorFixture):
    suite = "articulated"

    def opening_shot(self, *, art="kindling_draw", camera="third", hand="left", gear="netherite",
                     shell="funded_shell", disabled=False, phase="active", size=100):
        return self.shot(f"articulated_opening_style_{art}_{camera}_{hand}_{gear}_{shell}"
                         f"{'_adapter_disabled' if disabled else ''}_requested_{phase}.png", size=size)

    def full_opening_matrix(self, size=100):
        # Mirror the producer's nested boolean loop independently of the allowlist.
        for art in ("kindling_draw", "frostbite"):
            for camera in ("first", "third"):
                for hand in ("right", "left"):
                    for armored in (False, True):
                        for shell in (False, True):
                            phases = ("windup", "active", "recovery") if armored == shell else ("active",)
                            for phase in phases:
                                self.opening_shot(art=art, camera=camera, hand=hand,
                                    gear="netherite" if armored else "skin",
                                    shell="funded_shell" if shell else "glow_shell_down", phase=phase, size=size)
                    self.opening_shot(art=art, camera=camera, hand=hand, disabled=True, size=size)

    def full_existing_matrix(self, size=100):
        ArticulatedCuratorTests.full_matrix(self, size=size)
        SharedPlayerCuratorTests.full_shared_matrix(self, size=size)
        FundedShellCuratorTests.full_funded_matrix(self, size=size)

    funded_shot = FundedShellCuratorTests.funded_shot

    def test_all_72_exact_trials_preserve_source_suites_and_unknown_observations(self):
        self.full_opening_matrix()
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 72)
        self.assertEqual(len(curator.OPENING_STYLE_SLOTS), 72)
        self.assertEqual(len(result["openingStyleCoverage"]), 72)
        self.assertEqual(len({row["expectedFilename"] for row in result["openingStyleCoverage"]}), 72)
        self.assertEqual(len(result["sourceSuites"]), 4)
        self.assertEqual(result["sourceSuites"], list(curator.ARTICULATED_SOURCE_SUITES))
        self.assertEqual(sum(row["sourceSuite"] == curator.ARTICULATED_SUITE
                             for row in result["openingStyleCoverage"]), 36)
        for row in result["openingStyleCoverage"]:
            self.assertTrue(row["captured"] and row["selected"])
            self.assertEqual(row["status"], "included")
            self.assertFalse(row["missingCapture"] or row["omittedForBudget"])
            self.assertEqual(row["renderedPhase"], "unknown")
            self.assertEqual(row["verifiedRenderedPhases"], [])
        for frame in result["frames"]:
            name = Path(frame["sourcePath"]).name
            self.assertIsNone(curator.describe(name))
            self.assertEqual(frame["sourceSuite"], curator.ARTICULATED_HUD_SUITE
                             if frame["camera"] == "first" else curator.ARTICULATED_SUITE)
            self.assertEqual(frame["form"], "opening_style")
            self.assertEqual(frame["view"], "owner_first_person" if frame["camera"] == "first"
                             else "local_owner_third_person_front")
            self.assertEqual((frame["viewport"], frame["uiScale"]), ({"width": 1280, "height": 720}, 3))
            self.assertEqual(frame["viewportBasis"], "native_fixture_contract")
            self.assertEqual((frame["mainHandItem"], frame["offHandItem"]), ("diamond_sword", "empty"))
            self.assertEqual(frame["captureKind"], "native_local_owner_accepted_sword_string")
            self.assertEqual(frame["expectedInput"], "swing swing low")
            self.assertEqual(frame["expectedMove"], 3 if frame["art"] == "kindling_draw" else 4)
            self.assertEqual((frame["expectedWindup"], frame["expectedRecovery"]), (6, 12))
            self.assertEqual(frame["inputBasis"], "native_fixture_contract")
            self.assertIsNone(frame["armorTrim"])
            self.assertEqual(frame["armorEnchantment"], "protection_iv" if frame["equipment"] == "netherite_full" else None)
            self.assertEqual(frame["shellAdapterEnabled"], "_adapter_disabled_" not in name)
            self.assertEqual(frame["frameLabel"], "requested_" + frame["requestedPhase"])
            self.assertEqual(frame["requestedPhaseBasis"], "filename_only")
            self.assertEqual((frame["phase"], frame["renderedPhase"]), ("unknown", "unknown"))
            self.assertEqual(frame["imageReceiptBinding"], "not_in_scope")
            self.assertTrue(frame["screenshotLatencyMayChangePhase"] and frame["nativePixelReviewRequired"])
            self.assertEqual(frame["exactImpactPixelCoverage"], "unverified")
            self.assertNotIn("captureStatus", frame)
            self.assertNotIn("metadata", frame)
            receipt = frame["preCaptureReceipt"]
            self.assertEqual(receipt["status"], "not_ingested")
            self.assertEqual(receipt["preCapturePhase"], "unknown")
            self.assertEqual(receipt["logRecord"], "ARTICULATED_OPENING_SAMPLE name=" + name[:-4])
            for key in ("actualSkin", "acceptedMove", "activation", "windup", "recovery", "preCaptureAge"):
                self.assertIsNone(receipt[key])
        self.assertEqual(result["selectedMetadataCount"], 0)
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_impostor_forms_widths_cameras_phases_and_unrequested_crossed_slots_are_excluded(self):
        base = "articulated_opening_style_kindling_draw_first_left_netherite_funded_shell_requested_active.png"
        invalid = [base.replace(before, after) for before, after in (
            ("kindling_draw", "rising_cinders"), ("kindling_draw", "rising_break"),
            ("first", "hud"), ("first", "third_back"), ("left", "LEFT"), ("left", "observer"),
            ("netherite", "partial"), ("netherite", "skin_slim"), ("funded_shell", "glow"),
            ("requested_active", "rendered_active"), ("requested_active", "requested_settled"),
            ("requested_active", "frame_0"), (".png", "_1280x720_gui3.png"), (".png", ".png.extra"))]
        invalid += [base.replace("funded_shell", "glow_shell_down").replace("active", "windup"),
                    base.replace("netherite", "skin").replace("active", "recovery"),
                    base.replace("requested_active", "adapter_disabled_requested_windup"),
                    base.replace("netherite", "skin").replace("requested_active", "adapter_disabled_requested_active")]
        for name in invalid:
            with self.subTest(filename=name):
                self.assertIsNone(curator.describe(name, suite=self.suite))
            self.shot(name)
        result = self.curate()
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["freshness"]["ignoredOutOfScopePngs"], sum(name.endswith(".png") for name in invalid))
        self.assertTrue(all(row["status"] == "not_captured" for row in result["openingStyleCoverage"]))

    def test_native_logs_and_unbound_json_cannot_invent_live_skin_acceptance_or_frame_binding(self):
        image = self.opening_shot(disabled=True)
        image.with_suffix(".json").write_text(json.dumps({"actualSkin": "SLIM", "captureStatus": "passed",
            "renderedPhase": "ACTIVE", "imageReceiptBinding": "bound", "acceptedMove": 3, "activation": 123}))
        (image.parent / "latest.log").write_text("ARTICULATED_OPENING_SAMPLE name=" + image.stem
            + " actualSkin=SLIM acceptedMove=3 activation=123 requestedPhase=ACTIVE preCapturePhase=ACTIVE"
            + " preCaptureAge=6.5 renderedPhase=unknown imageReceiptBinding=not_in_scope")
        result = self.curate()
        frame = result["frames"][0]
        self.assertFalse(frame["shellAdapterEnabled"])
        self.assertIsNone(frame["preCaptureReceipt"]["actualSkin"])
        self.assertIsNone(frame["preCaptureReceipt"]["acceptedMove"])
        self.assertEqual(frame["renderedPhase"], "unknown")
        self.assertEqual(frame["imageReceiptBinding"], "not_in_scope")
        self.assertNotIn("captureStatus", frame)
        self.assertEqual(result["selectedMetadataCount"], 0)
        self.assertEqual(len(list((self.root / self.output / "frames").iterdir())), 1)

    def test_missing_included_and_budget_omitted_slots_remain_distinct_without_failure_claims(self):
        present = self.opening_shot()
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE)
        captured = [row for row in result["openingStyleCoverage"] if row["captured"]]
        self.assertEqual(len(captured), 1)
        self.assertEqual(captured[0]["expectedFilename"], present.name)
        self.assertEqual(captured[0]["status"], "omitted_for_budget")
        self.assertTrue(captured[0]["omittedForBudget"])
        self.assertFalse(captured[0]["missingCapture"] or captured[0]["selected"])
        self.assertEqual(sum(row["missingCapture"] for row in result["openingStyleCoverage"]), 71)
        self.assertFalse(any("failed" in row for row in result["openingStyleCoverage"]))
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_priority_covers_both_forms_hands_body_and_first_person_funded_armor_active(self):
        self.full_opening_matrix(size=1_000_000)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 8_000_000)
        frames = result["frames"]
        self.assertEqual(len(frames), 8)
        self.assertEqual({(f["art"], f["camera"], f["hand"]) for f in frames},
            {(art, camera, hand) for art in ("kindling_draw", "frostbite")
             for camera in ("first", "third") for hand in ("left", "right")})
        self.assertTrue(all(f["requestedPhase"] == "active" and f["shellAdapterEnabled"]
            and f["equipment"] == "netherite_full" and f["shellConfiguration"] == "funded_shell" for f in frames))
        self.assertEqual(sum(row["omittedForBudget"] for row in result["openingStyleCoverage"]), 64)

    def test_old_pairs_and_new_pairs_share_budget_without_splitting_counterparts(self):
        self.full_opening_matrix(size=1_000_000)
        self.full_existing_matrix(size=1_000_000)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 7_000_000)
        opening = [f for f in result["frames"] if f.get("form") == "opening_style"]
        old = [f for f in result["frames"] if "form" not in f]
        self.assertEqual({(f["art"], f["camera"], f["hand"]) for f in opening},
                         {("kindling_draw", "third", hand) for hand in ("left", "right")})
        self.assertEqual({Path(f["sourcePath"]).name for f in old},
                         {"articulated_live_left_first_frame_0.png", "articulated_live_left_third_frame_0.png"})
        self.assertEqual(sum(f.get("form") == "shared_player_art" for f in result["frames"]), 1)
        self.assertEqual(sum(f.get("form") == "funded_aura_shell" for f in result["frames"]), 2)

    def test_large_combined_matrix_keeps_npc_old_proof_and_opening_active_representatives(self):
        self.full_opening_matrix(size=300_000)
        self.full_existing_matrix(size=300_000)
        npc = ArticulatedNpcCuratorTests()
        npc.root, npc.source, npc.started = self.root, self.source, self.started
        npc.full_npc_matrix(size=100_000)
        result = self.curate()
        self.assertEqual([f["phase"] for f in result["frames"][:6]],
                         ["reply_warning", "reply_warning", "release", "release", "recovery", "recovery"])
        self.assertTrue(all(f["captureStatus"] == "passed" for f in result["frames"][:6]))
        primary = [f for f in result["frames"] if f.get("form") == "opening_style"
                   and f["equipment"] == "netherite_full"]
        self.assertEqual({(f["art"], f["camera"], f["hand"]) for f in primary},
            {(art, camera, hand) for art in ("kindling_draw", "frostbite")
             for camera in ("first", "third") for hand in ("left", "right")})
        self.assertTrue(all(f["requestedPhase"] == "active" and f["shellAdapterEnabled"] for f in primary))
        old = [f for f in result["frames"] if "form" not in f]
        for hand in ("left", "right"):
            for source in curator.ARTICULATED_OWNER_SOURCE_SUITES:
                self.assertEqual(sum(f["hand"] == hand and f["sourceSuite"] == source for f in old), 2)
        self.assertEqual(sum(f.get("form") == "shared_player_art" for f in result["frames"]), 4)
        self.assertEqual(sum(f.get("form") == "funded_aura_shell" for f in result["frames"]), 12)
        self.assertTrue(all(row["captured"] for row in result["openingStyleCoverage"]))
        self.assertTrue(any(row["omittedForBudget"] for row in result["openingStyleCoverage"]))
        self.assertFalse(any(row["missingCapture"] for row in result["openingStyleCoverage"]))
        files = [p for p in (self.root / self.output).rglob("*") if p.is_file()]
        with zipfile.ZipFile(self.root / "curated.zip", "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in files:
                zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLessEqual((self.root / "curated.zip").stat().st_size, 14_000_000)

    def test_temporal_representatives_follow_active_pairs_when_budget_permits(self):
        self.full_opening_matrix(size=500_000)
        result = self.curate()
        self.assertEqual(len(result["frames"]), 25)
        self.assertTrue(all(f["requestedPhase"] == "active" for f in result["frames"][:16]))
        temporal = result["frames"][16:24]
        self.assertEqual({(f["art"], f["camera"], f["requestedPhase"]) for f in temporal},
            {(art, camera, phase) for art in ("kindling_draw", "frostbite")
             for camera in ("first", "third") for phase in ("windup", "recovery")})
        self.assertTrue(all(f["hand"] == "left" and f["equipment"] == "netherite_full"
                            and f["renderedPhase"] == "unknown" for f in temporal))

    def test_unaffordable_active_pair_is_not_retried_alone_and_missing_mate_is_not_invented(self):
        left = self.opening_shot()
        right = self.opening_shot(hand="right")
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual(result["frames"], [])
        self.assertEqual(sum(row["omittedForBudget"] for row in result["openingStyleCoverage"]), 2)
        right.unlink()
        result = curator.curate(self.root, self.source, self.marker, "review/one", self.identity,
                                suite=self.suite, budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual([Path(f["sourcePath"]).name for f in result["frames"]], [left.name])
        self.assertEqual(sum(row["missingCapture"] for row in result["openingStyleCoverage"]), 71)

    def test_temporal_comparison_is_atomic_and_missing_phase_stays_missing(self):
        windup = self.opening_shot(phase="windup")
        recovery = self.opening_shot(phase="recovery")
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual(result["frames"], [])
        recovery.unlink()
        result = curator.curate(self.root, self.source, self.marker, "review/one", self.identity,
                                suite=self.suite, budget=curator.ARTICULATED_MANIFEST_RESERVE + 100)
        self.assertEqual([Path(f["sourcePath"]).name for f in result["frames"]], [windup.name])
        self.assertEqual(result["frames"][0]["renderedPhase"], "unknown")

    def test_absent_opening_frames_preserve_existing_funded_shared_core_selection_order(self):
        self.full_existing_matrix(size=1_000_000)
        result = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + 6_000_000)
        self.assertEqual([Path(f["sourcePath"]).name for f in result["frames"]], [
            "articulated_shared_rising_break_third_1280x720_gui3_skin_left_requested_active.png",
            "articulated_shared_driving_cut_third_1280x720_gui3_skin_left_requested_active.png",
            "articulated_funded_shell_spellcut_first_person_right_stage4_netherite.png",
            "articulated_funded_shell_spellcut_first_person_left_stage5_netherite.png",
            "articulated_live_left_first_frame_0.png", "articulated_live_left_third_frame_0.png"])
        self.assertTrue(all(row["status"] == "not_captured" for row in result["openingStyleCoverage"]))
        self.assertEqual((len(result["sharedPlayerCoverage"]), len(result["fundedShellCoverage"])), (40, 36))

    def test_all_359_selected_records_and_stored_zip_fit_cap_without_altering_sources(self):
        self.full_opening_matrix()
        self.full_existing_matrix()
        npc = ArticulatedNpcCuratorTests()
        npc.root, npc.source, npc.started = self.root, self.source, self.started
        npc.full_npc_matrix()
        # Fill nearly the entire payload allowance with one eligible filename. The
        # bytes are a Python budget fixture, never a rendered/gameplay pass.
        source = self.root / self.source
        old = source / "articulated_opening_style_frostbite_first_right_skin_funded_shell_requested_active.png"
        extra = curator.BUDGET - curator.ARTICULATED_MANIFEST_RESERVE - sum(
            p.stat().st_size for p in source.iterdir() if p.is_file())
        old.write_bytes(old.read_bytes() + b"x" * extra)
        before = {path: hashlib.sha256(path.read_bytes()).hexdigest() for path in source.rglob("*") if path.is_file()}
        result = self.curate()
        self.assertEqual((result["selectedFrameCount"], result["selectedMetadataCount"]), (359, 15))
        self.assertEqual(len({f["sourcePath"] for f in result["frames"]}), 359)
        self.assertEqual([f["phase"] for f in result["frames"][:6]],
                         ["reply_warning", "reply_warning", "release", "release", "recovery", "recovery"])
        self.assertTrue(all(f["captureStatus"] == "passed" for f in result["frames"][:6]))
        self.assertEqual((len(result["coverage"]), len(result["sharedPlayerCoverage"]),
                          len(result["fundedShellCoverage"]), len(result["openingStyleCoverage"])), (32, 40, 36, 72))
        self.assertEqual(sum(row["captured"] and row["selected"] for row in result["openingStyleCoverage"]), 72)
        repeat = curator.curate(self.root, self.source, self.marker, "review/repeated", self.identity, suite=self.suite)
        self.assertEqual(result, repeat)
        manifest_size = (self.root / self.output / "manifest.json").stat().st_size
        self.assertGreater(manifest_size, 900_000)
        self.assertLess(manifest_size + result["limits"]["storedZipOverheadBytes"], curator.ARTICULATED_MANIFEST_RESERVE)
        files = [p for p in (self.root / self.output).rglob("*") if p.is_file()]
        self.assertLessEqual(sum(p.stat().st_size for p in files), 14_000_000)
        with zipfile.ZipFile(self.root / "curated.zip", "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in files:
                zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLessEqual((self.root / "curated.zip").stat().st_size, 14_000_000)
        self.assertEqual((self.root / "curated.zip").stat().st_size - sum(p.stat().st_size for p in files),
                         result["limits"]["storedZipOverheadBytes"])
        for frame in result["frames"]:
            for entry in ([frame, frame["metadata"]] if "metadata" in frame else [frame]):
                self.assertEqual(entry["sha256"], before[self.root / entry["sourcePath"]])
                self.assertEqual(hashlib.sha256((self.root / self.output / entry["artifactPath"]).read_bytes()).hexdigest(),
                                 entry["sha256"])
        self.assertEqual({path: hashlib.sha256(path.read_bytes()).hexdigest() for path in before}, before)

    def test_opening_frames_keep_freshness_duplicate_signature_and_path_guards(self):
        prior = self.opening_shot()
        marker = "review/later.json"
        curator.prepare(self.root, self.source, marker, self.identity, suite=self.suite)
        self.started = json.loads((self.root / marker).read_text())["startedNs"]
        os.utime(prior, None)
        old = self.opening_shot(hand="right")
        os.utime(old, ns=(1, 1))
        fresh = self.opening_shot(art="frostbite")
        result = curator.curate(self.root, self.source, marker, "review/fresh", self.identity, suite=self.suite)
        self.assertEqual(result["freshness"]["excludedStalePngs"], 2)
        self.assertEqual([Path(f["sourcePath"]).name for f in result["frames"]], [fresh.name])
        duplicate = self.shot(fresh.name, prefix="nested")
        with self.assertRaisesRegex(curator.EvidenceError, "Ambiguous duplicate"):
            self.curate()
        duplicate.unlink()
        duplicate.symlink_to(fresh)
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            self.curate()
        duplicate.unlink()
        fresh.write_bytes(b"not a png")
        with self.assertRaisesRegex(curator.EvidenceError, "PNG signature"):
            self.curate()
        self.assertFalse((self.root / self.output).exists())


class ArticulatedNpcCuratorTests(CuratorFixture):
    suite = "articulated"
    write_metadata = NpcCuratorTests.write_metadata

    def metadata(self, scene, phase):
        # Structural Python test inputs only. These manufactured receipts are
        # never native screenshots or a game/render/socket pass claim.
        original = scene.replace("articulated_npc_", "masters_npc_", 1)
        base_phase = phase if phase in curator.NPC_BEATS[original] else "reply_warning"
        value = NpcCuratorTests.metadata(self, original, base_phase)
        age = curator.ARTICULATED_NPC_BEATS[scene][phase]
        value.update(name=f"{scene}_{phase}", phase=phase, requestedTick=age, expectedBackend="segmented", captureStatus="passed")
        value["renderedTimeline"].update(clientGameTick=100 + age, age=age + .5, fallbackRig=False)
        value["serverObservation"].update(gameTick=100 + age, age=age)
        if phase == "step_last":
            value["warnings"] = value["warnings"][:2]
        value["modelPose"] = [.1] * 120
        step = original.endswith("gale_crosswind") and 8 <= age < 12
        value["articulatedFrame"] = {
            "activation": 100, "move": value["renderedTimeline"]["attackId"],
            "phase": "ACTIVE" if phase == "release" else "RECOVERY" if phase == "recovery" else "WINDUP",
            "weight": .9, "leftHanded": False, "scriptedFootwork": step,
            "horizontalVelocitySquared": .2025 if step else 0,
            "interpolatedTravelSquared": .06, "walkAnimationSpeed": .7 if original.endswith("gale_crosswind") else 0,
        }
        value["modelReceipts"] = [{"backend": "segmented", "segmentedRootVisible": True,
            "rigidPartsVisible": [False] * 6, "transformNames": list(curator.NPC_SEGMENTED_JOINTS),
            "transforms": list(value["modelPose"])}]
        matrix = [1.0 if i % 5 == 0 else 0.0 for i in range(16)]
        value["handReceipts"] = [{"hand": "RIGHT", "entryMatrix": matrix, "nativeHandMatrix": matrix,
            "resolvedItemMatrix": matrix, "expectedSocketItemMatrix": matrix,
            "hiltDistance": 0.0, "maximumMatrixError": 0.0}]
        close = phase != "reply_warning"
        value.update(framing="body_close" if close else "warning_lane", fov=50 if close else 60,
                     warningCoverage="visible_portion" if close else "full_lane",
                     warningValidation="diagnostic_only" if close else "full_lane_required",
                     bodyBounds={"minX": .2, "minY": .2, "maxX": .65, "maxY": .6 if close else .4,
                                 "vertexCount": 800, "allInFront": True, "wholeVisible": True},
                     bladeBounds={"minX": .35, "minY": .25, "maxX": .6, "maxY": .55,
                                  "vertexCount": 24, "allInFront": True, "wholeVisible": True},
                     bodyBoundsSource=curator.NPC_BODY_BOUNDS_SOURCE,
                     bladeBoundsSource=curator.NPC_BLADE_BOUNDS_SOURCE,
                     bodySubmitMatrix=matrix, viewRotationProjectionMatrix=matrix)
        return value

    def npc(self, scene="articulated_npc_gale_crosswind", phase="reply_warning", size=100):
        return NpcCuratorTests.npc(self, scene, phase, size)

    def full_npc_matrix(self, size=100):
        for scene, beats in curator.ARTICULATED_NPC_BEATS.items():
            for phase in beats:
                self.npc(scene, phase, size)

    def test_exact_articulated_namespace_and_boundary_beats_are_separate_from_fallback(self):
        for scene, beats in curator.ARTICULATED_NPC_BEATS.items():
            for phase in beats:
                name = f"{scene}_{phase}.png"
                self.assertIsNone(curator.describe(name))
                info = curator.describe(name, suite=self.suite)
                self.assertEqual((info["sourceSuite"], info["view"], info["phase"]),
                                 (curator.ARTICULATED_SUITE, curator.NPC_VIEW, phase))
        for name in ("articulated_npc_gale_crosswind_settle_12.png", "articulated_npc_gale_crosswind_settle_16.png",
                     "articulated_npc_stone_fracture_landed.png", "articulated_npc_gale_crosswind_frame_11.png",
                     "articulated_npc_ember_release.png", "masters_npc_gale_crosswind_release.png"):
            self.assertIsNone(curator.describe(name, suite=self.suite))
        absent = self.curate()
        self.assertEqual(absent["frames"], [])
        self.assertEqual([row["missingCapturePhases"] for row in absent["npcCoverage"]],
                         [list(beats) for beats in curator.ARTICULATED_NPC_BEATS.values()])

    def test_all_existing_and_npc_frames_fit_the_reserve_and_preserve_both_file_types(self):
        ArticulatedCuratorTests.full_matrix(self)
        self.full_npc_matrix()
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 131)
        self.assertEqual(result["selectedMetadataCount"], 15)
        self.assertEqual(len(result["coverage"]), 32)
        self.assertEqual(result["limits"]["manifestReserveBytes"], curator.ARTICULATED_MANIFEST_RESERVE)
        self.assertEqual(result["limits"]["totalBytesLimit"], 14_000_000)
        second = curator.curate(self.root, self.source, self.marker, "review/repeated", self.identity, suite=self.suite)
        self.assertEqual(result, second)
        for frame in result["frames"]:
            for entry in ([frame, frame["metadata"]] if "metadata" in frame else [frame]):
                original = (self.root / entry["sourcePath"]).read_bytes()
                self.assertEqual((self.root / self.output / entry["artifactPath"]).read_bytes(), original)
                self.assertEqual(entry["sha256"], hashlib.sha256(original).hexdigest())
        self.assertTrue(all(not row["missingCapturePhases"] and not row["omittedForBudgetPhases"] for row in result["npcCoverage"]))
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])
        self.assertIn("matrices are observed", result["basis"])

    def test_bounded_npc_priority_keeps_owner_armor_and_hud_pair_rules(self):
        ArticulatedCuratorTests.full_matrix(self, size=700_000)
        self.full_npc_matrix(size=700_000)
        result = self.curate()
        expected = [(scene, phase) for phase in curator.NPC_PRIORITY for scene in curator.ARTICULATED_NPC_BEATS]
        self.assertEqual([(f["scene"], f["phase"]) for f in result["frames"][:6]], expected)
        self.assertEqual(result["selectedMetadataCount"], 6)
        for hand in curator.ARTICULATED_HANDS:
            owner = [frame for frame in result["frames"] if frame.get("hand") == hand]
            self.assertEqual({frame["sourceSuite"] for frame in owner}, set(curator.ARTICULATED_OWNER_SOURCE_SUITES))
            self.assertEqual({frame["equipment"] for frame in owner if frame["sourceSuite"] == curator.ARTICULATED_HUD_SUITE},
                             {"skin", "netherite_chestplate"})
        files = [path for path in (self.root / self.output).rglob("*") if path.is_file()]
        self.assertLess(sum(path.stat().st_size for path in files), 14_000_000)
        archive = self.root / "curated.zip"
        with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in files:
                zipped.write(path, path.relative_to(self.root / self.output))
        self.assertLess(archive.stat().st_size, 15_000_000)

    def test_boundary_ages_keep_scripted_step_velocity_interpolation_and_eased_walk_distinct(self):
        for phase in ("step_last", "landed", "settle_13", "settle_14", "settle_15"):
            self.npc(phase=phase)
        result = self.curate()
        self.assertEqual([f["renderedTimeline"]["age"] for f in result["frames"]], [11.5, 12.5, 13.5, 14.5, 15.5])
        self.assertEqual([f["articulatedFrame"]["scriptedFootwork"] for f in result["frames"]], [True, False, False, False, False])
        for frame in result["frames"]:
            self.assertEqual(frame["articulatedFrame"]["interpolatedTravelSquared"], .06)
            self.assertEqual(frame["articulatedFrame"]["walkAnimationSpeed"], .7)
            self.assertEqual(frame["modelTransformCount"], 120)
            self.assertEqual(frame["modelReceiptCount"], 1)
            self.assertEqual(frame["handReceiptCount"], 1)
        self.assertEqual(result["frames"][1]["articulatedFrame"]["horizontalVelocitySquared"], 0)
        self.assertEqual(result["npcCoverage"][0]["capturedPhases"], ["step_last", "landed", "settle_13", "settle_14", "settle_15"])

    def test_png_and_receipt_bytes_are_one_indivisible_budget_unit(self):
        image, sidecar = self.npc()
        cost = image.stat().st_size + sidecar.stat().st_size
        rejected = self.curate(budget=curator.ARTICULATED_MANIFEST_RESERVE + cost - 1)
        self.assertEqual(rejected["frames"], [])
        self.assertEqual(rejected["npcCoverage"][0]["omittedForBudgetPhases"], ["reply_warning"])
        accepted = curator.curate(self.root, self.source, self.marker, "review/exact", self.identity,
                                 suite=self.suite, budget=curator.ARTICULATED_MANIFEST_RESERVE + cost)
        self.assertEqual(accepted["selectedFrameCount"], 1)
        self.assertEqual(accepted["selectedPngBytes"] + accepted["selectedMetadataBytes"], cost)

    def test_failed_rigid_backend_receipts_are_excluded_without_losing_owner_evidence(self):
        _, sidecar = self.npc(phase="landed")
        value = json.loads(sidecar.read_text())
        value["renderFailure"] = "java.lang.AssertionError: The requested backend must own the native school frame"
        value["captureStatus"] = "failed"
        value["renderedTimeline"]["fallbackRig"] = True
        value["modelPose"] = [.2] * 36
        value["modelReceipts"] = [{"backend": "rigid", "segmentedRootVisible": False, "rigidPartsVisible": [True] * 6,
                                   "transformNames": list(curator.NPC_RIGID_PARTS), "transforms": value["modelPose"]}]
        value["handReceipts"] = []
        self.write_metadata(sidecar, value)
        owner = self.shot("articulated_live_left_first_frame_0.png")
        result = self.curate()
        self.assertEqual([Path(f["sourcePath"]).name for f in result["frames"]], [owner.name])
        self.assertEqual(result["selectedMetadataCount"], 0)
        self.assertEqual(result["npcCoverage"][0]["capturedPhases"], [])
        self.assertEqual(result["excludedNpcCaptures"][0]["phase"], "landed")
        self.assertEqual(result["excludedNpcCaptures"][0]["reasonCode"], "native_render_failure")

    def test_inconsistent_backend_phase_joint_and_socket_receipts_cannot_be_accepted(self):
        _, sidecar = self.npc()
        mutations = (("expectedBackend", "rigid"), ("renderedTimeline.fallbackRig", True),
                     ("modelPose", [.1] * 36), ("modelReceipts", []), ("handReceipts", []),
                     ("articulatedFrame", None), ("articulatedFrame.activation", 99), ("articulatedFrame.move", 8),
                     ("articulatedFrame.phase", "ACTIVE"), ("articulatedFrame.scriptedFootwork", True),
                     ("articulatedFrame.horizontalVelocitySquared", -1), ("articulatedFrame.interpolatedTravelSquared", -1),
                     ("articulatedFrame.walkAnimationSpeed", "0.2"),
                     ("modelReceipts.0.transformNames", list(reversed(curator.NPC_SEGMENTED_JOINTS))),
                     ("modelReceipts.0.transforms", [.2] * 120), ("modelReceipts.0.rigidPartsVisible", [True] * 6),
                     ("modelReceipts.0.segmentedRootVisible", False),
                     ("handReceipts.0.hand", "LEFT"), ("handReceipts.0.entryMatrix", [1.0] * 15),
                     ("handReceipts.0.hiltDistance", .00001), ("handReceipts.0.maximumMatrixError", .00001),
                     ("handReceipts.0.resolvedItemMatrix", [0.0] * 16))
        for field, new in mutations:
            with self.subTest(field=field):
                value = self.metadata("articulated_npc_gale_crosswind", "reply_warning")
                node = value
                keys = field.split(".")
                for key in keys[:-1]:
                    node = node[int(key)] if isinstance(node, list) else node[key]
                node[keys[-1]] = new
                self.write_metadata(sidecar, value)
                with self.assertRaises(curator.EvidenceError):
                    self.curate()
                self.assertFalse((self.root / self.output).exists())

    def test_early_articulated_failure_allows_omitted_extraction_but_requires_receipt_schema(self):
        _, sidecar = self.npc()
        value = json.loads(sidecar.read_text())
        for key in ("renderedTimeline", "modelPose", "renderedPosition", "articulatedFrame"):
            del value[key]
        for key in ("bodySubmitMatrix", "viewRotationProjectionMatrix"):
            del value[key]
        for key in ("bodyBounds", "bladeBounds"):
            value[key] = {"minX": 0, "minY": 0, "maxX": 0, "maxY": 0, "vertexCount": 0,
                          "allInFront": False, "wholeVisible": False}
        value.update(captureStatus="failed", renderFailure="java.lang.AssertionError: Native NPC body submission and animated model passes must both occur",
                     modelPasses=0, bodySubmits=0, bodyPoints=[], warnings=[], modelReceipts=[], handReceipts=[],
                     bodyPixels={"nonBlackPixels": 0, "chromaticPixels": 0, "distinctColors": 0, "luminanceRange": 0})
        self.write_metadata(sidecar, value)
        result = self.curate()
        self.assertEqual(result["frames"], [])
        self.assertEqual(len(result["excludedNpcCaptures"]), 1)
        self.assertEqual(result["selectedMetadataCount"], 0)
        for field, malformed in (("modelReceipts", None), ("handReceipts", {}), ("expectedBackend", "rigid"),
                                 ("articulatedFrame", []), ("bodySubmitMatrix", [0.0] * 15), ("extra", float("inf"))):
            with self.subTest(field=field):
                invalid = {**value, field: malformed}
                self.write_metadata(sidecar, invalid)
                with self.assertRaises(curator.EvidenceError):
                    curator.curate(self.root, self.source, self.marker, "review/invalid", self.identity, suite=self.suite)

    def test_every_native_model_and_hand_pass_is_checked_and_retained_in_the_sidecar(self):
        _, sidecar = self.npc()
        value = json.loads(sidecar.read_text())
        value["modelPasses"] = 2
        value["modelReceipts"] *= 2
        value["handReceipts"] *= 2
        self.write_metadata(sidecar, value)
        result = self.curate()
        self.assertEqual(result["frames"][0]["modelReceiptCount"], 2)
        self.assertEqual(result["frames"][0]["handReceiptCount"], 2)
        for field in ("modelReceipts", "handReceipts"):
            invalid = json.loads(json.dumps(value))
            if field == "modelReceipts":
                invalid[field][1]["rigidPartsVisible"][0] = True
            else:
                invalid[field][1]["hiltDistance"] = .1
            self.write_metadata(sidecar, invalid)
            with self.assertRaises(curator.EvidenceError):
                curator.curate(self.root, self.source, self.marker, "review/invalid", self.identity, suite=self.suite)

    def test_close_framing_preserves_partial_warning_receipts_and_nullable_unclamped_endpoints(self):
        _, sidecar = self.npc(phase="landed")
        value = json.loads(sidecar.read_text())
        value["warnings"][0].update(sampledBins=3, coloredBins=1, to={"x": 1.4, "y": -.2})
        del value["warnings"][0]["from"]
        value["warnings"][1].update(sampledBins=0, coloredBins=0, **{"from": None, "to": None})
        # The close view's warning samples are explicitly diagnostic-only; an
        # actor can occlude a geometrically on-screen segment without a body failure.
        value["warnings"][2].update(sampledBins=2, coloredBins=0)
        self.write_metadata(sidecar, value)
        result = self.curate()
        frame = result["frames"][0]
        self.assertEqual((frame["framing"], frame["fov"], frame["warningCoverage"]), ("body_close", 50, "visible_portion"))
        self.assertEqual(frame["warningValidation"], "diagnostic_only")
        self.assertEqual(frame["bodyBounds"], value["bodyBounds"])
        self.assertEqual(frame["bladeBounds"], value["bladeBounds"])
        copied = self.root / self.output / frame["metadata"]["artifactPath"]
        self.assertEqual(copied.read_bytes(), sidecar.read_bytes())
        self.assertIn("not pixel segmentation", result["basis"])
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_close_frames_do_not_require_invented_offscreen_warning_segments(self):
        _, sidecar = self.npc(phase="step_last")
        value = json.loads(sidecar.read_text())
        value["warnings"] = []
        self.write_metadata(sidecar, value)
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 1)
        self.assertEqual(result["frames"][0]["warningCoverage"], "visible_portion")

    def test_framing_modes_bounds_provenance_and_projection_matrices_are_validated(self):
        _, sidecar = self.npc(phase="release")
        mutations = (("framing", "warning_lane"), ("fov", 60), ("fov", 50.0), ("warningCoverage", "full_lane"),
                     ("bodyBoundsSource", "authored_envelope"), ("bladeBoundsSource", "estimated_sword"),
                     ("bodyBounds", None), ("bladeBounds", {}), ("bodyBounds.vertexCount", 0),
                     ("bodyBounds.vertexCount", True), ("bodyBounds.allInFront", False),
                     ("bodyBounds.wholeVisible", False), ("bladeBounds.wholeVisible", False),
                     ("bodyBounds.minX", .03), ("bladeBounds.maxY", .97),
                     ("bodyBounds.minY", .5), ("bodyBounds.maxY", .4),
                     ("bodySubmitMatrix", [0.0] * 15), ("viewRotationProjectionMatrix", None))
        for field, replacement in mutations:
            with self.subTest(field=field, replacement=replacement):
                value = self.metadata("articulated_npc_gale_crosswind", "release")
                node = value
                keys = field.split(".")
                for key in keys[:-1]:
                    node = node[key]
                node[keys[-1]] = replacement
                self.write_metadata(sidecar, value)
                with self.assertRaises(curator.EvidenceError):
                    self.curate()

    def test_close_body_height_accepts_exact_180_pixels_but_rejects_smaller(self):
        _, sidecar = self.npc(phase="release")
        value = json.loads(sidecar.read_text())
        value["bodyBounds"].update(minY=.25, maxY=.5)
        self.write_metadata(sidecar, value)
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 1)
        value["bodyBounds"]["maxY"] = .499
        self.write_metadata(sidecar, value)
        with self.assertRaises(curator.EvidenceError):
            curator.curate(self.root, self.source, self.marker, "review/smaller", self.identity, suite=self.suite)

    def test_wide_warning_lane_keeps_full_endpoint_sample_and_color_contract(self):
        _, sidecar = self.npc()
        for field, replacement in (("from", None), ("to", {"x": 1.4, "y": .5}),
                                   ("sampledBins", 3), ("coloredBins", 0)):
            with self.subTest(field=field):
                value = self.metadata("articulated_npc_gale_crosswind", "reply_warning")
                value["warnings"][0][field] = replacement
                self.write_metadata(sidecar, value)
                with self.assertRaises(curator.EvidenceError):
                    self.curate()

    def test_partial_warning_counts_and_endpoint_schema_still_fail_closed(self):
        _, sidecar = self.npc(phase="landed")
        for updates in ({"sampledBins": 17}, {"sampledBins": -1}, {"sampledBins": True},
                        {"sampledBins": 3, "coloredBins": 4}, {"coloredBins": -1},
                        {"from": {"x": "offscreen", "y": .5}}, {"to": {}},
                        {"from": {"x": float("inf"), "y": .5}}):
            with self.subTest(updates=updates):
                value = self.metadata("articulated_npc_gale_crosswind", "landed")
                value["warnings"][0].update(updates)
                self.write_metadata(sidecar, value)
                with self.assertRaises(curator.EvidenceError):
                    self.curate()

    def test_failed_framing_bounds_are_excluded_but_malformed_bounds_are_not_excused(self):
        _, sidecar = self.npc(phase="landed")
        value = json.loads(sidecar.read_text())
        value["renderFailure"] = "java.lang.AssertionError: Native body geometry leaves the viewport"
        value["captureStatus"] = "failed"
        value["bodyBounds"].update(minX=-.1, wholeVisible=False)
        value["warnings"][0].update(sampledBins=0, coloredBins=0, **{"from": None, "to": None})
        self.write_metadata(sidecar, value)
        owner = self.shot("articulated_live_left_first_frame_0.png")
        result = self.curate()
        self.assertEqual([Path(f["sourcePath"]).name for f in result["frames"]], [owner.name])
        self.assertEqual(len(result["excludedNpcCaptures"]), 1)
        self.assertEqual(result["npcCoverage"][0]["capturedPhases"], [])
        value["bodyBounds"]["vertexCount"] = "unknown"
        self.write_metadata(sidecar, value)
        with self.assertRaises(curator.EvidenceError):
            curator.curate(self.root, self.source, self.marker, "review/malformed", self.identity, suite=self.suite)

    def test_old_metadata_and_old_marker_never_qualify_fresh_articulated_npc_images(self):
        image, sidecar = self.npc()
        image.unlink()
        marker = "review/later.json"
        curator.prepare(self.root, self.source, marker, self.identity, suite=self.suite)
        self.started = json.loads((self.root / marker).read_text())["startedNs"]
        self.npc()
        result = curator.curate(self.root, self.source, marker, self.output, self.identity, suite=self.suite)
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["freshness"]["excludedStaleNpcMetadata"], 1)
        stamp = json.loads((self.root / marker).read_text())
        del stamp["preexistingNpcMetadata"]
        (self.root / marker).write_text(json.dumps(stamp))
        with self.assertRaisesRegex(curator.EvidenceError, "does not match"):
            curator.curate(self.root, self.source, marker, "review/wrong", self.identity, suite=self.suite)

    def test_post_pixel_failure_is_excluded_while_valid_owner_and_npc_frames_survive(self):
        failed, sidecar = self.npc(phase="step")
        value = json.loads(sidecar.read_text())
        # Structural regression for public25bdaf76: the expected far Gale step
        # ray had 16 sampled bins and zero colored bins behind the actor's legs.
        # This is not a replacement or fabricated passing native capture.
        value["warnings"][1].update(sampledBins=16, coloredBins=0)
        value.update(captureStatus="failed", renderFailure=
                     "java.lang.AssertionError: An on-screen portion of an extracted warning must contain school-colored framebuffer pixels: "
                     "articulated_npc_gale_crosswind_step")
        self.write_metadata(sidecar, value)
        owner = self.shot("articulated_live_left_first_frame_0.png")
        valid, _ = self.npc("articulated_npc_stone_fracture", "release")
        result = self.curate()
        self.assertEqual({Path(frame["sourcePath"]).name for frame in result["frames"]}, {owner.name, valid.name})
        self.assertEqual(result["selectedMetadataCount"], 1)
        self.assertEqual(result["npcCoverage"][0]["capturedPhases"], [])
        self.assertEqual(result["excludedNpcCaptures"][0]["reasonCode"], "native_render_failure")
        self.assertIn("school-colored", result["excludedNpcCaptures"][0]["reason"])
        self.assertFalse((self.root / self.output / "frames" / failed.name).exists())
        self.assertFalse((self.root / self.output / "frames" / sidecar.name).exists())

    def test_legacy_null_failure_is_unfinalized_and_never_counted_as_passed_or_failed(self):
        _, sidecar = self.npc(phase="step")
        value = json.loads(sidecar.read_text())
        del value["captureStatus"]
        del value["warningValidation"]
        value["renderFailure"] = None
        value["warnings"][1].update(sampledBins=16, coloredBins=0)
        self.write_metadata(sidecar, value)
        owner = self.shot("articulated_live_left_first_frame_0.png")
        result = self.curate()
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [owner.name])
        self.assertEqual(result["selectedMetadataCount"], 0)
        self.assertEqual(result["npcCoverage"][0]["capturedPhases"], [])
        self.assertEqual(result["excludedNpcCaptures"][0]["reasonCode"], "native_capture_verdict_unavailable")
        self.assertIn("lacks a finalized", result["excludedNpcCaptures"][0]["reason"])
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])
        # Missing status does not excuse malformed legacy evidence.
        value["handReceipts"][0]["entryMatrix"] = []
        self.write_metadata(sidecar, value)
        with self.assertRaises(curator.EvidenceError):
            curator.curate(self.root, self.source, self.marker, "review/malformed", self.identity, suite=self.suite)

    def test_capture_verdict_rejects_invalid_or_contradictory_status(self):
        _, sidecar = self.npc(phase="step")
        for status, failure in ((None, None), (True, None), ("pending", None), ("failed", None),
                                ("failed", ""), ("passed", "Native pixel assertion failed")):
            with self.subTest(status=status, failure=failure):
                value = self.metadata("articulated_npc_gale_crosswind", "step")
                value.update(captureStatus=status, renderFailure=failure)
                self.write_metadata(sidecar, value)
                with self.assertRaises(curator.EvidenceError):
                    self.curate()
                self.assertFalse((self.root / self.output).exists())

    def test_unfinalized_legacy_pixel_failure_is_excluded_without_discarding_owner_evidence(self):
        _, sidecar = self.npc(phase="reply_warning")
        value = json.loads(sidecar.read_text())
        del value["captureStatus"]
        del value["warningValidation"]
        value["bodyPixels"].update(nonBlackPixels=0, distinctColors=0, luminanceRange=0)
        for warning in value["warnings"]:
            warning["coloredBins"] = 0
        self.write_metadata(sidecar, value)
        owner = self.shot("articulated_live_left_first_frame_0.png")
        result = self.curate()
        self.assertEqual([Path(frame["sourcePath"]).name for frame in result["frames"]], [owner.name])
        self.assertEqual(result["excludedNpcCaptures"][0]["reasonCode"], "native_capture_verdict_unavailable")
        for key in ("nonBlackPixels", "chromaticPixels", "distinctColors", "luminanceRange"):
            with self.subTest(negative_count=key):
                original = value["bodyPixels"][key]
                value["bodyPixels"][key] = -1
                self.write_metadata(sidecar, value)
                with self.assertRaises(curator.EvidenceError):
                    curator.curate(self.root, self.source, self.marker, "review/malformed", self.identity, suite=self.suite)
                value["bodyPixels"][key] = original

    def test_legacy_explicit_failure_still_excludes_without_inventing_a_finalized_pass(self):
        _, sidecar = self.npc(phase="step")
        value = json.loads(sidecar.read_text())
        del value["captureStatus"]
        value["renderFailure"] = "java.lang.AssertionError: Native body submission failed"
        self.write_metadata(sidecar, value)
        result = self.curate()
        self.assertEqual(result["frames"], [])
        self.assertEqual(result["excludedNpcCaptures"][0]["reasonCode"], "native_render_failure")

    def test_selected_articulated_npc_manifest_retains_final_per_capture_verdict(self):
        self.npc()
        result = self.curate()
        self.assertEqual(result["frames"][0]["captureStatus"], "passed")
        self.assertIn("finalized after native render and pixel checks", result["basis"])
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_close_body_pass_does_not_claim_warning_lane_acceptance(self):
        _, sidecar = self.npc(phase="step")
        value = json.loads(sidecar.read_text())
        for warning in value["warnings"]:
            warning.update(sampledBins=16, coloredBins=0)
        self.write_metadata(sidecar, value)
        self.npc(phase="reply_warning")
        result = self.curate()
        frames = {frame["phase"]: frame for frame in result["frames"]}
        self.assertEqual(frames["step"]["captureStatus"], "passed")
        self.assertEqual(frames["step"]["warningValidation"], "diagnostic_only")
        self.assertEqual(frames["reply_warning"]["warningValidation"], "full_lane_required")
        self.assertIn("do not establish warning-lane acceptance", result["basis"])
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_warning_validation_contract_is_required_and_cannot_be_swapped(self):
        for phase, invalid in (("step", "full_lane_required"), ("reply_warning", "diagnostic_only")):
            _, sidecar = self.npc(phase=phase)
            for replacement in (invalid, None, "unknown", False):
                with self.subTest(phase=phase, replacement=replacement):
                    value = self.metadata("articulated_npc_gale_crosswind", phase)
                    value["warningValidation"] = replacement
                    self.write_metadata(sidecar, value)
                    with self.assertRaises(curator.EvidenceError):
                        self.curate()
            value = self.metadata("articulated_npc_gale_crosswind", phase)
            del value["warningValidation"]
            self.write_metadata(sidecar, value)
            with self.assertRaises(curator.EvidenceError):
                self.curate()
            sidecar.unlink()
            sidecar.with_suffix(".png").unlink()

    def test_receipt_changes_after_validation_and_unsafe_sidecars_still_fail_closed(self):
        image, sidecar = self.npc()
        saved = sidecar.read_bytes()
        sidecar.unlink()
        sidecar.symlink_to(self.root / "outside")
        with self.assertRaisesRegex(curator.EvidenceError, "Symlink"):
            self.curate()
        sidecar.unlink()
        sidecar.write_bytes(saved)
        selector = curator.select_articulated

        def mutate(groups, budget):
            result = selector(groups, budget)
            before = sidecar.stat()
            sidecar.write_bytes(sidecar.read_bytes().replace(b'"nonBlackPixels": 100', b'"nonBlackPixels": 101'))
            os.utime(sidecar, ns=(before.st_atime_ns, before.st_mtime_ns))
            return result

        with patch.object(curator, "select_articulated", side_effect=mutate):
            with self.assertRaisesRegex(curator.EvidenceError, "changed after validation"):
                self.curate()
        self.assertFalse((self.root / self.output).exists())


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


class OpeningReceiptCuratorTests(CuratorFixture):
    """Synthetic wrapped receipts only; no Gradle, Java, or native process."""
    suite = "articulated"

    def build_report(self, *, exit_code=0, png_padding=0, **generation):
        # Optional receipt ingestion alone uses Pillow. Existing curator modes
        # retain their opaque-PNG/stdlib contract.
        import run_articulated_receipt_gate as gate
        from test_run_articulated_receipt_gate import NativeReceiptGateTests
        from client_suites import EXIT_PREFIX, SELECTION_PREFIX
        helper = NativeReceiptGateTests()
        helper.root = self.root
        helper.game = self.root / Path(self.source).parent
        helper.receipt_root = helper.game / "screenshots/articulated-shared-receipts"
        helper.pid = 1234
        destination = self.root / "review/receipt-bundle"

        def runner(command, repo, environment):
            helper.generate(environment)
            opening = helper.generate(environment, profile="opening", **generation)
            if png_padding:
                import struct
                import zlib
                kind, payload = b"npAd", b"x" * png_padding
                chunk = struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(payload, zlib.crc32(kind)))
                for record_path in opening.glob("*.json"):
                    record = json.loads(record_path.read_text())
                    path = helper.game / record["image"]["relativeImagePath"]
                    original = path.read_bytes()
                    data = original[:-12] + chunk + original[-12:]
                    path.write_bytes(data)
                    record["image"].update(pngBytes=len(data), pngSha256=hashlib.sha256(data).hexdigest())
                    record_path.write_text(json.dumps(record))
            (self.root / "native.log").write_text(SELECTION_PREFIX + json.dumps({"kind": "suite", "name": "articulated"})
                + "\n" + EXIT_PREFIX + str(exit_code) + "\n")
            return gate.NativeExit(exit_code, helper.pid)

        gate.run_gate(self.root, helper.game, destination, Path("native.log"), runner=runner,
                      commit_reader=lambda repo: self.identity["workflowSha"],
                      environment={"GITHUB_SHA": self.identity["workflowSha"], "GITHUB_RUN_ID": self.identity["runId"],
                                   "GITHUB_RUN_ATTEMPT": self.identity["runAttempt"]}, include_opening=True)
        self.report_path = destination / "opening-association-report.json"
        self.report = json.loads(self.report_path.read_text())
        self.shared_bytes = (destination / "association-report.json").read_bytes()
        return self.report_path.relative_to(self.root).as_posix()

    def save_report(self):
        self.report_path.write_text(json.dumps(self.report))

    def replace_raw_record(self, path, record):
        """Forge coherent package hashes; semantic checks must still reject it."""
        import time
        data = json.dumps(record).encode()
        path.write_bytes(data)
        (self.report_path.parent / "opening-receipts" / path.name).write_bytes(data)
        entry = {"name": path.name, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
        records = self.report["package"]["records"]
        records[:] = [item for item in records if item["name"] != path.name] + [entry]
        self.report["package"].update(recordCount=len(records), recordBytes=sum(item["bytes"] for item in records))
        tokens = {tuple((value["identity"][key] for key in ("runId", "captureSequence")))
                  for value in (json.loads(item.read_text()) for item in path.parent.glob("*.json"))}
        self.report["package"]["captureTokens"] = [{"runId": run, "captureSequence": sequence}
                                                   for run, sequence in sorted(tokens)]
        for observation in self.report["observations"]:
            if observation["receiptRelativePath"] == path.name:
                observation["receiptSha256"] = entry["sha256"]
                observation["pngSha256"] = record["image"]["pngSha256"]
        self.report["provenance"]["finishedNs"] = time.time_ns()
        self.save_report()

    def assert_unknown(self, result):
        self.assertEqual(result["openingReceiptEvidence"]["status"], "unavailable")
        self.assertFalse(result["openingReceiptEvidence"]["associationVerified"])
        self.assertLessEqual(len(result["openingReceiptEvidence"]["diagnostic"]), 500)
        for row in result["openingStyleCoverage"]:
            self.assertEqual(row["renderedPhase"], "unknown")
            self.assertEqual(row["verifiedRenderedPhases"], [])
            self.assertNotIn("openingRenderReceipt", row)
        for frame in result["frames"]:
            if frame.get("form") == "opening_style":
                self.assertEqual(frame["phase"], "unknown")
                self.assertNotIn("openingRenderReceipt", frame)
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_exact_current_report_enriches_all_72_without_changing_selection_or_pngs(self):
        report = self.build_report()
        baseline = self.curate()
        result = curator.curate(self.root, self.source, self.marker, "review/enriched", self.identity,
                                suite=self.suite, opening_receipt_report=report)
        self.assertEqual([item["sourcePath"] for item in baseline["frames"]],
                         [item["sourcePath"] for item in result["frames"]])
        self.assertEqual(result["openingReceiptEvidence"]["verifiedImages"], 72)
        self.assertTrue(result["openingReceiptEvidence"]["sourceGate"]["gatePassed"])
        self.assertEqual(result["selectedMetadataCount"], baseline["selectedMetadataCount"])
        for row in result["openingStyleCoverage"]:
            receipt = row["openingRenderReceipt"]
            self.assertEqual(receipt["trial"] + ".png", row["expectedFilename"])
            self.assertEqual(row["renderedPhase"], "FALLBACK" if "_adapter_disabled_" in row["expectedFilename"]
                             else row["requestedPhase"].upper())
            self.assertEqual(receipt["receiptSha256"], hashlib.sha256((self.root / receipt["receiptSourcePath"]).read_bytes()).hexdigest())
            self.assertEqual(receipt["mainHand"], "minecraft:diamond_sword:foil=false")
            self.assertIn(receipt["skinModel"], ("wide", "slim"))
            self.assertEqual(row["imageReceiptBinding"], "verified_native_readback")
        for frame in result["frames"]:
            self.assertEqual((self.root / frame["sourcePath"]).read_bytes(),
                             (self.root / "review/enriched" / frame["artifactPath"]).read_bytes())
            if frame.get("form") == "opening_style":
                self.assertEqual(frame["phaseBasis"], "native_render_receipt_readback")
                self.assertEqual(frame["preCaptureReceipt"]["status"], "not_ingested")
        self.assertEqual((self.report_path.parent / "association-report.json").read_bytes(), self.shared_bytes)
        self.assertFalse(result["testVerdict"]["establishedByThisArtifact"])

    def test_fallback_retains_raw_phase_and_cannot_supply_articulated_active_coverage(self):
        result = self.curate(opening_receipt_report=self.build_report())
        fallbacks = [frame for frame in result["frames"] if frame.get("expectedBackend") == "full_fallback"]
        self.assertEqual(len(fallbacks), 8)
        for frame in fallbacks:
            self.assertEqual((frame["phase"], frame["renderedPhase"], frame["rawAcceptedPhase"]), ("fallback", "FALLBACK", "ACTIVE"))
            self.assertFalse(frame["openingRenderReceipt"]["requestedPhaseObserved"])
            self.assertFalse(frame["openingRenderReceipt"]["unpausedPhaseCoverage"])
            self.assertTrue(frame["openingRenderReceipt"]["fallbackPhaseCoverage"])

    def test_phase_misses_and_native_failure_remain_visible_with_actual_associations(self):
        result = self.curate(opening_receipt_report=self.build_report(exit_code=9, active_miss=True))
        evidence = result["openingReceiptEvidence"]
        self.assertEqual(evidence["status"], "verified")
        gate = evidence["sourceGate"]
        self.assertFalse(gate["nativeSucceeded"] or gate["phaseCoverageVerified"] or gate["gatePassed"])
        self.assertTrue(gate["phaseCoverageMisses"])
        self.assertEqual(gate["errors"], self.report["errors"])
        active = [row for row in result["openingStyleCoverage"] if row["requestedPhase"] == "active"]
        self.assertTrue(all(row["rawAcceptedPhase"] == "RECOVERY" for row in active))
        self.assertTrue(all(row["renderedPhase"] == ("FALLBACK" if "_adapter_disabled_" in row["expectedFilename"] else "RECOVERY") for row in active))

    def test_all_72_verified_facts_survive_budget_omission_without_extra_files(self):
        report = self.build_report()
        budget = curator.ARTICULATED_MANIFEST_RESERVE + 500
        baseline = self.curate(budget=budget)
        result = curator.curate(self.root, self.source, self.marker, "review/enriched", self.identity, budget,
                                suite=self.suite, opening_receipt_report=report)
        self.assertEqual([item["sourcePath"] for item in baseline["frames"]],
                         [item["sourcePath"] for item in result["frames"]])
        rows = result["openingStyleCoverage"]
        self.assertEqual(sum(row["captured"] for row in rows), 72)
        self.assertTrue(any(row["omittedForBudget"] for row in rows))
        self.assertTrue(all("openingRenderReceipt" in row for row in rows))
        self.assertFalse(any(row["missingCapture"] for row in rows))
        files = [path for path in (self.root / "review/enriched").rglob("*") if path.is_file()]
        total = sum(path.stat().st_size for path in files)
        self.assertLessEqual(total + result["limits"]["storedZipOverheadBytes"], budget)
        self.assertEqual(len(files), result["selectedFrameCount"] + 1)

    def test_missing_report_preserves_captured_and_missing_distinction(self):
        name = next(iter(curator.OPENING_STYLE_SLOTS))
        self.shot(name)
        result = self.curate(opening_receipt_report="review/missing-opening-report.json")
        self.assert_unknown(result)
        self.assertEqual(sum(row["captured"] for row in result["openingStyleCoverage"]), 1)
        self.assertEqual(sum(row["missingCapture"] for row in result["openingStyleCoverage"]), 71)

    def test_verified_large_pngs_stay_byte_identical_below_14_million_including_manifest(self):
        report = self.build_report(png_padding=240_000)
        result = self.curate(opening_receipt_report=report)
        self.assertTrue(any(row["omittedForBudget"] for row in result["openingStyleCoverage"]))
        self.assertTrue(all("openingRenderReceipt" in row for row in result["openingStyleCoverage"]))
        output = self.root / self.output
        files = [path for path in output.rglob("*") if path.is_file()]
        self.assertLessEqual(sum(path.stat().st_size for path in files) + result["limits"]["storedZipOverheadBytes"], 14_000_000)
        for frame in result["frames"]:
            self.assertEqual((self.root / frame["sourcePath"]).read_bytes(), (output / frame["artifactPath"]).read_bytes())

    def test_missing_bound_png_remains_missing_without_accepting_other_report_claims(self):
        report = self.build_report()
        observation = self.report["observations"][0]
        (Path(self.report["gameDirectory"]) / observation["relativeImagePath"]).unlink()
        result = self.curate(opening_receipt_report=report)
        self.assert_unknown(result)
        self.assertEqual(sum(row["missingCapture"] for row in result["openingStyleCoverage"]), 1)
        self.assertEqual(sum(row["captured"] for row in result["openingStyleCoverage"]), 71)

    def test_report_claims_cannot_override_native_observations_or_gate(self):
        report = self.build_report(active_miss=True)
        changes = (
            lambda value: value.update(schemaVersion=1),
            lambda value: value.update(gatePassed=True),
            lambda value: value["observations"][0].update(skinModel="invented_skin"),
            lambda value: value["observations"][0].update(renderedPhase="invented_phase"),
            lambda value: value["observations"][0].update(receiptSha256="f" * 64),
            lambda value: value["observations"][0].update(pngSha256="f" * 64),
            lambda value: value["observations"][0].update(relativeImagePath=value["observations"][1]["relativeImagePath"]),
            lambda value: value["observations"].__setitem__(0, value["observations"][1]),
            lambda value: value["package"]["records"].append(value["package"]["records"][0]),
            lambda value: value["package"].update(complete=False),
        )
        original = self.report_path.read_text()
        for index, change in enumerate(changes):
            with self.subTest(change=index):
                self.report = json.loads(original)
                change(self.report)
                self.save_report()
                self.assert_unknown(curator.curate(self.root, self.source, self.marker, f"review/bad-{index}",
                    self.identity, suite=self.suite, opening_receipt_report=report))

    def test_old_wrong_root_commit_attempt_nonce_or_exit_are_rejected(self):
        import uuid
        report = self.build_report()
        original = self.report_path.read_text()
        changes = (
            lambda value: value["provenance"].update(startedNs=self.started - 1),
            lambda value: value["provenance"].update(checkout=str(self.root.parent)),
            lambda value: value["provenance"].update(sourceCommit="c" * 40),
            lambda value: value["provenance"].update(GITHUB_SHA="c" * 40),
            lambda value: value["provenance"].update(GITHUB_RUN_ID="121"),
            lambda value: value["provenance"].update(GITHUB_RUN_ATTEMPT="3"),
            lambda value: value["provenance"].update(launchNonce=str(uuid.uuid4())),
            lambda value: value["provenance"].update(nativeProcessExitConfirmed=False),
            lambda value: value["provenance"].update(nativeLogSha256="c" * 64),
            lambda value: value.update(gameDirectory=str(self.root)),
            lambda value: value.update(receiptDirectory=str(self.root)),
            lambda value: value["package"].update(sourceDirectory=str(self.root)),
            lambda value: value["package"].update(runId=str(uuid.uuid4())),
        )
        for index, change in enumerate(changes):
            with self.subTest(change=index):
                self.report = json.loads(original)
                change(self.report)
                self.save_report()
                self.assert_unknown(curator.curate(self.root, self.source, self.marker, f"review/bad-{index}",
                    self.identity, suite=self.suite, opening_receipt_report=report))

    def test_changed_png_is_not_hidden_by_unchanged_report(self):
        report = self.build_report()
        path = Path(self.report["gameDirectory"]) / self.report["observations"][0]["relativeImagePath"]
        data = bytearray(path.read_bytes())
        data[-8] ^= 1
        path.write_bytes(data)
        self.assert_unknown(self.curate(opening_receipt_report=report))

    def test_modified_packaged_record_invalidates_the_full_opening_mapping(self):
        report = self.build_report()
        record = self.report_path.parent / "opening-receipts" / self.report["package"]["records"][0]["name"]
        record.write_bytes(record.read_bytes() + b" ")
        self.assert_unknown(self.curate(opening_receipt_report=report))

    def test_late_record_cannot_hide_behind_successful_packaged_record(self):
        report = self.build_report()
        source = Path(self.report["receiptDirectory"])
        record = json.loads(next(source.glob("*.json")).read_text())
        record.update(verified=False, failures=["late_callback"])
        (source / "late-record.json").write_text(json.dumps(record))
        self.assert_unknown(self.curate(opening_receipt_report=report))

    def test_forged_matching_package_hashes_do_not_replace_raw_record_validation(self):
        report = self.build_report()
        path = Path(self.report["receiptDirectory"]) / self.report["package"]["records"][0]["name"]
        original_report, original_record = json.dumps(self.report), path.read_text()
        mutations = (
            lambda value: value.update(nativeLaunchNonce="unbound"),
            lambda value: value["copy"]["observation"].update(nativeLaunchNonce="unbound"),
            lambda value: value["copy"]["passes"][0]["attributes"].update(head="empty"),
            lambda value: value["copy"]["observation"].update(acceptedMove="1000"),
            lambda value: value["copy"]["passes"][0]["palette"].update(phase="NONE"),
        )
        for index, mutation in enumerate(mutations):
            with self.subTest(mutation=index):
                self.report = json.loads(original_report)
                record = json.loads(original_record)
                mutation(record)
                self.replace_raw_record(path, record)
                result = curator.curate(self.root, self.source, self.marker, f"review/bad-{index}",
                    self.identity, suite=self.suite, opening_receipt_report=report)
                self.assert_unknown(result)
                if index >= 2:
                    self.assertIn("native association recheck failed", result["openingReceiptEvidence"]["diagnostic"])

    def test_forged_png_hash_still_rechecks_original_crc_and_callback_pixels(self):
        report = self.build_report()
        source = Path(self.report["receiptDirectory"])
        receipt_path = source / self.report["observations"][0]["receiptRelativePath"]
        record = json.loads(receipt_path.read_text())
        image = Path(self.report["gameDirectory"]) / record["image"]["relativeImagePath"]
        data = bytearray(image.read_bytes())
        data[-8] ^= 1
        image.write_bytes(data)
        record["image"]["pngSha256"] = hashlib.sha256(data).hexdigest()
        self.replace_raw_record(receipt_path, record)
        result = self.curate(opening_receipt_report=report)
        self.assert_unknown(result)
        self.assertIn("PNG chunk CRC mismatch", result["openingReceiptEvidence"]["diagnostic"])

    def test_forged_complete_manifest_cannot_hide_duplicate_or_failed_late_tokens(self):
        report = self.build_report()
        source = Path(self.report["receiptDirectory"])
        record = json.loads(next(source.glob("*.json")).read_text())
        record.update(verified=False, failures=["late_callback"])
        self.replace_raw_record(source / "late-record.json", record)
        result = self.curate(opening_receipt_report=report)
        self.assert_unknown(result)
        self.assertIn("late-invalidated", result["openingReceiptEvidence"]["diagnostic"])

    def test_forged_report_cannot_reuse_a_shared_capture_token(self):
        report = self.build_report()
        path = Path(self.report["receiptDirectory"]) / self.report["observations"][0]["receiptRelativePath"]
        record = json.loads(path.read_text())
        record["identity"]["captureSequence"] = 1
        record["copy"]["identity"]["captureSequence"] = 1
        self.replace_raw_record(path, record)
        self.report["observations"][0]["captureSequence"] = 1
        self.save_report()
        result = self.curate(opening_receipt_report=report)
        self.assert_unknown(result)
        self.assertIn("already belongs to a shared record", result["openingReceiptEvidence"]["diagnostic"])

    def test_late_directory_change_during_copy_prevents_publication(self):
        report = self.build_report()
        source = Path(self.report["receiptDirectory"])
        original_digest = curator.digest
        changed = False

        def changed_while_copying(path):
            nonlocal changed
            if not changed:
                changed = True
                (source / "late-record.json").write_text("{}")
            return original_digest(path)

        with patch.object(curator, "digest", side_effect=changed_while_copying):
            with self.assertRaisesRegex(curator.EvidenceError, "Opening receipt evidence changed"):
                self.curate(opening_receipt_report=report)
        self.assertFalse((self.root / self.output).exists())

    def test_duplicate_report_json_keys_are_not_silently_overwritten(self):
        report = self.build_report()
        data = self.report_path.read_text()
        self.report_path.write_text('{"schemaVersion": 1,' + data[1:])
        result = self.curate(opening_receipt_report=report)
        self.assert_unknown(result)
        self.assertIn("duplicate JSON", result["openingReceiptEvidence"]["diagnostic"])

    def check_nested_evidence_remains_unknown(self, target):
        import time
        report = self.build_report()
        nested = b"[" * 10_000 + b"0" + b"]" * 10_000
        if target == "report":
            self.report_path.write_bytes(nested)
        else:
            root = Path(self.report["receiptDirectory"])
            if target == "shared":
                root = root.parent.parent / "articulated-shared-receipts" / root.name
            path = next(root.glob("*.json"))
            path.write_bytes(nested)
            if target == "opening":
                (self.report_path.parent / "opening-receipts" / path.name).write_bytes(nested)
                entry = next(item for item in self.report["package"]["records"] if item["name"] == path.name)
                entry.update(bytes=len(nested), sha256=hashlib.sha256(nested).hexdigest())
                self.report["package"]["recordBytes"] = sum(item["bytes"] for item in self.report["package"]["records"])
            self.report["provenance"]["finishedNs"] = time.time_ns()
            self.save_report()
        result = self.curate(opening_receipt_report=report)
        self.assert_unknown(result)
        self.assertGreater(len(result["frames"]), 0)
        self.assertIn("RecursionError", result["openingReceiptEvidence"]["diagnostic"])
        self.assertLessEqual(len(result["openingReceiptEvidence"]["diagnostic"]), 500)
        self.assertTrue((self.root / self.output / "manifest.json").is_file())

    def test_deeply_nested_optional_report_preserves_curation(self):
        self.check_nested_evidence_remains_unknown("report")

    def test_deeply_nested_original_and_packaged_receipt_preserves_curation(self):
        self.check_nested_evidence_remains_unknown("opening")

    def test_deeply_nested_shared_receipt_preserves_curation(self):
        self.check_nested_evidence_remains_unknown("shared")

    def test_original_association_failure_and_errors_stay_explicit(self):
        result = self.curate(opening_receipt_report=self.build_report(nonce="unbound"))
        self.assert_unknown(result)
        evidence = result["openingReceiptEvidence"]
        self.assertIn("associationVerified", evidence["unverifiedReportedFailures"])
        self.assertTrue(any("native child launch" in error for error in evidence["unverifiedReportedErrors"]))

    def test_exact_report_path_disallows_escaping_and_symlinks(self):
        report = self.build_report()
        (self.root / "linked-report.json").symlink_to(self.report_path)
        for index, path in enumerate((str(self.report_path), "../outside.json", "linked-report.json",
                                      report.replace("review/", "review/./"))):
            with self.subTest(path=path):
                self.assert_unknown(curator.curate(self.root, self.source, self.marker, f"review/bad-{index}",
                    self.identity, suite=self.suite, opening_receipt_report=path))

    def test_report_is_opt_in_and_other_modes_do_not_import_verifier(self):
        self.build_report()
        import builtins
        original_import = builtins.__import__

        def guarded(name, *args, **kwargs):
            if name in ("run_articulated_receipt_gate", "verify_articulated_render_receipts", "PIL"):
                raise AssertionError("default curation must not import receipt/decode tools")
            return original_import(name, *args, **kwargs)

        with patch("builtins.__import__", side_effect=guarded):
            result = self.curate()
        self.assertEqual(result["openingReceiptEvidence"]["status"], "not_requested")
        self.assertTrue(all(row["renderedPhase"] == "unknown" for row in result["openingStyleCoverage"]))


if __name__ == "__main__":
    unittest.main()
