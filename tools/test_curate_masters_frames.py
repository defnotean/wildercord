"""Stdlib, tempfile-only curator checks; never launches Java or native gameplay."""
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
        self.assertEqual({frame["sourceSuite"] for frame in result["frames"]}, set(curator.ARTICULATED_SOURCE_SUITES))
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
        self.assertEqual(result["selectedFrameCount"], 19)
        for hand in ("left", "right"):
            selected = [frame for frame in result["frames"] if frame["hand"] == hand]
            self.assertEqual({frame["sourceSuite"] for frame in selected}, set(curator.ARTICULATED_SOURCE_SUITES))
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
        self.assertEqual(result["selectedFrameCount"], 11)
        self.assertEqual(result["selectedPngBytes"], 13_200_000)
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
        value.update(name=f"{scene}_{phase}", phase=phase, requestedTick=age, expectedBackend="segmented")
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

    def test_all_existing_and_npc_frames_fit_the_unchanged_reserve_and_preserve_both_file_types(self):
        ArticulatedCuratorTests.full_matrix(self)
        self.full_npc_matrix()
        result = self.curate()
        self.assertEqual(result["selectedFrameCount"], 131)
        self.assertEqual(result["selectedMetadataCount"], 15)
        self.assertEqual(len(result["coverage"]), 32)
        self.assertEqual(result["limits"]["manifestReserveBytes"], 256_000)
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
            self.assertEqual({frame["sourceSuite"] for frame in owner}, set(curator.ARTICULATED_SOURCE_SUITES))
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
        value.update(renderFailure="java.lang.AssertionError: Native NPC body submission and animated model passes must both occur",
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
        # Captured rays may include old segments that are not part of the native
        # expected-ray color assertion. The curator cannot invent that identity.
        value["warnings"][2].update(sampledBins=2, coloredBins=0)
        self.write_metadata(sidecar, value)
        result = self.curate()
        frame = result["frames"][0]
        self.assertEqual((frame["framing"], frame["fov"], frame["warningCoverage"]), ("body_close", 50, "visible_portion"))
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


if __name__ == "__main__":
    unittest.main()
