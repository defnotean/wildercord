"""Scoped March visuals must never borrow mechanics, partial or release evidence."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import client_suites as suites
import run_native_diagnostic_ci as diagnostic
import test_native_diagnostic_ci as existing


CASE = "stone-fault-march-visuals"
ENTRIES = ["dev.wildercord.aura.world.StoneMarchPresentationTest",
           "dev.wildercord.aura.world.StoneMarchOpponentViewTest"]


class MarchVisualDiagnosticTests(unittest.TestCase):
    fixture = existing.EvidenceTests.fixture
    log = existing.EvidenceTests.log
    collect = existing.EvidenceTests.collect
    assert_manifest_cannot_relabel_diagnostic = existing.EvidenceTests.assert_manifest_cannot_relabel_diagnostic
    assert_collect_preserves_evidence = existing.EvidenceTests.assert_collect_preserves_evidence

    def test_fixed_two_whole_classes_preserve_all_three_required_release_classes(self):
        self.assertEqual(diagnostic.CASES[CASE], CASE)
        self.assertEqual(list(suites.STONE_MARCH_VISUAL_ENTRIES), ENTRIES)
        self.assertEqual(suites.select_entries(suite=CASE), {
            "kind": "diagnostic", "name": CASE, "count": 2, "entries": ENTRIES})
        self.assertEqual(diagnostic.SEEDS[CASE], dict.fromkeys(ENTRIES))
        all_three = ["dev.wildercord.aura.world.StoneMarchTest", *ENTRIES]
        for selected in (suites.select_entries(suite="masters"), suites.select_entries()):
            self.assertEqual([entry for entry in selected["entries"] if entry in all_three], all_three)
        self.assertEqual(suites.select_entries(suite="masters")["count"], 44)
        self.assertEqual(suites.select_entries(suite="stone-fault-march")["entries"], all_three)

    def test_catalog_rejects_old_three_partial_reordered_foreign_and_release_scope(self):
        for update in ({"entries": list(suites.STONE_MARCH_ENTRIES), "expectedCount": 3},
                       {"entries": ENTRIES[:1], "expectedCount": 1},
                       {"entries": ENTRIES[::-1]}, {"entries": [ENTRIES[0]] * 2},
                       {"entries": [ENTRIES[0], "foreign.Class"]},
                       {"entries": [ENTRIES[0], ENTRIES[1] + "#subset"]},
                       {"purpose": "release"}, {"expectedCount": True}):
            with self.subTest(update=update), tempfile.TemporaryDirectory() as temp:
                catalog = json.loads(suites.CATALOG.read_text())
                catalog[CASE].update(update)
                path = Path(temp) / "catalog.json"
                path.write_text(json.dumps(catalog))
                with self.assertRaises(ValueError):
                    suites.select_entries(suite=CASE, catalog=path)
        # The existing generic registered-class rule still applies to this selector.
        with tempfile.TemporaryDirectory() as temp:
            descriptor = json.loads(suites.DESCRIPTOR.read_text())
            descriptor["entrypoints"]["fabric-client-gametest"].remove(ENTRIES[1])
            path = Path(temp) / "fabric.mod.json"
            path.write_text(json.dumps(descriptor))
            with self.assertRaisesRegex(ValueError, "unregistered entrypoints"):
                suites.select_entries(suite=CASE, descriptor=path)

    def test_current_rejects_forged_selection_before_source_inventory_or_launch(self):
        data = self.fixture(CASE)
        for changes in ({"entries": list(suites.STONE_MARCH_ENTRIES), "count": 3},
                        {"entries": ENTRIES[:1], "count": 1}, {"entries": ENTRIES[::-1]},
                        {"kind": "suite"}, {"name": "stone-fault-march"}):
            with self.subTest(changes=changes), tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                request = root / diagnostic.REQUEST
                request.parent.mkdir()
                request.write_text(json.dumps(data["request"]))
                with patch.object(diagnostic, "ROOT", root), \
                        patch.object(diagnostic, "identity", return_value={"headSha": "b" * 40, "observedLiveHeadSha": "b" * 40}), \
                        patch.object(diagnostic, "git", side_effect=["b" * 40 + " " + "a" * 40, diagnostic.REQUEST]), \
                        patch.object(diagnostic, "select_entries", return_value={**data["selection"], **changes}), \
                        patch.object(diagnostic, "digest") as digest, self.assertRaises(ValueError):
                    diagnostic.current({})
                digest.assert_not_called()

    def test_complete_ordered_native_receipts_only(self):
        data = self.fixture(CASE)
        good = self.log(data)
        self.assertEqual(self.collect(data, good)["completedEntries"], ENTRIES)
        lines = good.splitlines()
        for index, line in enumerate(lines):
            if diagnostic.SEED_PREFIX in line or diagnostic.SCENE_PREFIX in line:
                for bad in ("\n".join(lines[:index] + lines[index + 1:]), good + line + "\n"):
                    with self.subTest(marker=line):
                        self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")
        for prefix in (diagnostic.SEED_PREFIX, diagnostic.SCENE_PREFIX):
            receipts = [line for line in lines if prefix in line]
            remaining = [line for line in lines if prefix not in line]
            reordered = "\n".join(remaining + receipts[::-1])
            self.assertEqual(self.collect(data, reordered)["diagnosticOutcome"], "unverified")
        for old, new in (("returned", "threw"), ("cleanup", "run"),
                         (ENTRIES[0], "foreign.Class")):
            self.assertEqual(self.collect(data, good.replace(old, new))["diagnosticOutcome"], "unverified")

    def test_old_three_class_evidence_cannot_be_relabelled_as_visuals(self):
        data = self.fixture(CASE)
        old_data = self.fixture("stone-fault-march")
        old = self.log(old_data)
        forged = old.replace(json.dumps(old_data["selection"]), json.dumps(data["selection"]))
        forged = forged.replace(json.dumps(diagnostic.receipt(old_data)), json.dumps(diagnostic.receipt(data)))
        for log in (old, forged):
            self.assertEqual(self.collect(data, log)["diagnosticOutcome"], "unverified")
        self.assert_manifest_cannot_relabel_diagnostic(CASE)

    def test_malformed_duplicate_fields_and_out_of_range_seeds_cannot_pass(self):
        data = self.fixture(CASE)
        good = self.log(data)
        marker = diagnostic.SEED_PREFIX + json.dumps({"suite": ENTRIES[0], "seed": "1"})
        for value in ("{", "[]", "null", json.dumps({"suite": ENTRIES[0], "seed": True}),
                      json.dumps({"suite": ENTRIES[0], "seed": "1", "override": True}),
                      json.dumps({"suite": ENTRIES[0], "seed": str(2 ** 63)}),
                      json.dumps({"suite": ENTRIES[0], "seed": str(-2 ** 63 - 1)}),
                      '{"suite":"' + ENTRIES[0] + '","seed":"1","seed":"1"}'):
            with self.subTest(value=value):
                self.assertEqual(self.collect(data, good.replace(marker, diagnostic.SEED_PREFIX + value))["diagnosticOutcome"], "unverified")
        for prefix in (suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX, suites.REQUEST_PREFIX, diagnostic.SCENE_PREFIX):
            line = next(line for line in good.splitlines() if line.startswith(prefix))
            payload = json.loads(line[len(prefix):])
            key = next(iter(payload))
            duplicate = line[:-1] + ", " + json.dumps(key) + ": " + json.dumps(payload[key]) + "}"
            self.assertEqual(self.collect(data, good.replace(line, duplicate))["diagnosticOutcome"], "unverified")
        scene = next(line for line in good.splitlines() if line.startswith(diagnostic.SCENE_PREFIX))
        for value in ("true", "NaN", "Infinity", "-1"):
            bad = good.replace(scene, scene.replace('"elapsedSeconds": 1.0', '"elapsedSeconds": ' + value))
            self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")

    def test_source_head_run_attempt_configuration_and_inventory_are_bound(self):
        data = self.fixture(CASE)
        for section, field, value in (("request", "sourceSha", "d" * 40),
                                      ("provenance", "headSha", "d" * 40),
                                      ("provenance", "runId", "999"),
                                      ("provenance", "runAttempt", "2"),
                                      ("configuration", "nativeStepTimeoutMinutes", 55),
                                      ("sourceFilesSha256", "fixture.java", "d" * 64)):
            stale = copy.deepcopy(data)
            stale[section][field] = value
            with self.subTest(field=field):
                self.assertEqual(self.collect(data, self.log(stale))["diagnosticOutcome"], "unverified")
        request = data["request"]
        identity = {"headSha": "b" * 40, "observedLiveHeadSha": "b" * 40}
        self.assertEqual(diagnostic.request_state(request, identity, ["c" * 40], [diagnostic.REQUEST]), "inactive-stale-request")
        self.assertEqual(diagnostic.request_state(request, {**identity, "observedLiveHeadSha": "c" * 40},
                                                 ["a" * 40], [diagnostic.REQUEST]), "inactive-stale-pr-head")
        # Real temporary Git source/request commits exercise preparation, hashes,
        # stale launch refusal, and preserving immutable evidence at collection.
        self.assert_collect_preserves_evidence(CASE)

    def test_receipt_numeric_types_cannot_impersonate_exact_source_configuration(self):
        data = self.fixture(CASE)
        data["configuration"] = {"nativeStepTimeoutMinutes": 50, "automaticRetries": 0}
        good = self.log(data)
        for section, field, value in (("request", "schemaVersion", True),
                                      ("request", "schemaVersion", 1.0),
                                      ("configuration", "nativeStepTimeoutMinutes", 50.0),
                                      ("configuration", "automaticRetries", False)):
            forged = copy.deepcopy(data)
            forged[section][field] = value
            with self.subTest(field=field, value=value):
                self.assertEqual(self.collect(data, self.log(forged))["diagnosticOutcome"], "unverified")
        for prefix in (suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX):
            line = prefix + json.dumps(data["selection"])
            forged = good.replace(line, line.replace('"count": 2', '"count": 2.0'))
            self.assertEqual(self.collect(data, forged)["diagnosticOutcome"], "unverified")
        self.assertEqual(self.collect(data, good)["diagnosticOutcome"], "passed")

    def test_source_inventory_binds_original_capture_and_visual_dependencies(self):
        files = diagnostic.CASE_FILES[CASE]
        self.assertTrue(set(diagnostic.CASE_FILES["stone-fault-march"]).issubset(files))
        for path in files:
            self.assertTrue((suites.ROOT / path).is_file(), path)
        config = "src/gametest/resources/masters-capture-gametest.mixins.json"
        self.assertIn(config, files)
        mixins = json.loads((suites.ROOT / config).read_text())
        for name in mixins["client"]:
            self.assertIn("src/gametest/java/dev/wildercord/gametest/mixin/" + name + ".java", files)

    def test_existing_nine_frame_upload_works_without_case_specific_guard_or_new_paths(self):
        workflow = (suites.ROOT / ".github/workflows/build.yml").read_text()
        job = workflow.split("\n  native-diagnostic:\n", 1)[1]
        self.assertIn("    timeout-minutes: 60\n", job)
        self.assertIn("        timeout-minutes: 50\n", job)
        step = job.split("      - name: Preserve original March diagnostic frames (not release or visual acceptance)\n", 1)[1].split("      - name:", 1)[0]
        names = ("rigid_front_gather", "rigid_front_first_strike", "rigid_front_extraction", "rigid_wide_warning",
                 "segmented_front_first_strike", "segmented_wide_spent_first", "segmented_wide_spent_all",
                 "opponent_segmented_fov90_normal_inward_warning", "opponent_segmented_fov90_reduced_inward_warning")
        base = "build/run/clientGameTest/screenshots/stone_fault_march_"
        images = [base + name + ".png" for name in names]
        paths = step.split("          path: |\n", 1)[1].split("          if-no-files-found:", 1)[0]
        self.assertEqual([line.strip() for line in paths.splitlines()], [
            base + name + suffix for name in names for suffix in (".png", ".json")] + [
            diagnostic.OUTPUT + "/request-provenance.json", diagnostic.OUTPUT + "/diagnostic-result.json"])
        self.assertIn("if: always() && steps.request.outputs.enabled == 'true' && hashFiles("
                      + ", ".join("'" + path + "'" for path in images) + ") != ''", step)
        self.assertIn("name: stone-march-native-diagnostic-${{ github.event.pull_request.head.sha }}-${{ github.run_id }}-${{ github.run_attempt }}", step)
        self.assertNotIn("run:", step)


if __name__ == "__main__":
    unittest.main()
