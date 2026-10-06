"""Reject arbitrary requests, stale revisions, mixed logs and release-gate relabeling."""
from contextlib import redirect_stderr, redirect_stdout
import copy
import io
import json
import os
import shutil
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import Mock, patch

import client_suites as suites
import native_ci_diagnostics
import run_client_ci
import run_native_diagnostic_ci as diagnostic
import test_manifest


class RequestTests(unittest.TestCase):
    def request(self, **changes):
        return {"schemaVersion": 1, "case": "wetland", "sourceSha": "a" * 40, **changes}

    def test_unrelated_pr_updates_cannot_cancel_admitted_diagnostic(self):
        workflow = (suites.ROOT / ".github/workflows/build.yml").read_text()
        diagnostic_job = workflow.split("\n  native-diagnostic:\n", 1)[1]
        self.assertIn("    concurrency:\n      group: native-diagnostic-${{ github.repository }}-${{ github.event.pull_request.number }}\n      cancel-in-progress: false\n", diagnostic_job)
        self.assertNotIn("cancel-in-progress: true", diagnostic_job)

    def test_stasis_diagnostic_selects_complete_original_gallery_and_requires_seed(self):
        selected = suites.select_entries(suite="diagnostic-stasis-gallery")
        self.assertEqual(selected["count"], 1)
        self.assertEqual(selected["entries"], ["dev.wildercord.gametest.WildercordScreenshots"])
        self.assertEqual(selected["kind"], "diagnostic")
        self.assertEqual(diagnostic.CASES["stasis-gallery"], "diagnostic-stasis-gallery")
        self.assertEqual(diagnostic.SEEDS["stasis-gallery"],
                         {"dev.wildercord.gametest.WildercordScreenshots": None})
        source = (suites.ROOT / "src/gametest/java/dev/wildercord/gametest/WildercordScreenshots.java").read_text()
        self.assertIn("server.overworld().getSeed()", source)
        self.assertIn("castEverything(context, world)", source)
        self.assertIn("mechanicsChecks(context, world)", source)

    def test_request_accepts_only_fixed_cases_or_disabled(self):
        for case in (*diagnostic.CASES, None):
            data = self.request(case=case, sourceSha="a" * 40 if case else None)
            self.assertEqual(diagnostic.parse_request(json.dumps(data)), data)

    def test_kiln_diagnostic_upload_keeps_early_rigid_and_segmented_pairs_with_own_provenance(self):
        workflow = (suites.ROOT / ".github/workflows/build.yml").read_text()
        diagnostic_job = workflow.split("\n  native-diagnostic:\n", 1)[1]
        step = diagnostic_job.split("      - name: Preserve original Kiln diagnostic frames (not release or visual acceptance)\n", 1)[1].split("      - name:", 1)[0]
        names = ("ember_kiln_rigid_front_coil", "ember_kiln_rigid_front_release",
                 "ember_kiln_rigid_front_recovery", "ember_kiln_rigid_wide_warning",
                 "ember_kiln_segmented_front_coil", "ember_kiln_segmented_front_release",
                 "ember_kiln_segmented_front_recovery", "ember_kiln_segmented_wide_warning",
                 "ember_kiln_opponent_segmented_fov90_normal_inward_warning",
                 "ember_kiln_opponent_segmented_fov90_reduced_inward_warning")
        images = ["build/run/clientGameTest/screenshots/" + name + ".png" for name in names]
        pairs = ["build/run/clientGameTest/screenshots/" + name + suffix
                 for name in names for suffix in (".png", ".json")]
        paths = step.split("          path: |\n", 1)[1].split("          if-no-files-found:", 1)[0]
        self.assertEqual([line.strip() for line in paths.splitlines()], pairs + [
            diagnostic.OUTPUT + "/request-provenance.json", diagnostic.OUTPUT + "/diagnostic-result.json"])
        self.assertIn("if: always() && steps.request.outputs.enabled == 'true' && hashFiles("
                      + ", ".join("'" + path + "'" for path in images) + ") != ''", step)
        self.assertIn("uses: actions/upload-artifact@v7", step)
        self.assertIn("name: ember-kiln-native-diagnostic-${{ github.event.pull_request.head.sha }}-${{ github.run_id }}-${{ github.run_attempt }}", step)
        self.assertIn("if-no-files-found: warn", step)
        self.assertNotIn("run:", step)
        masters = workflow.split("      - name: Preserve original Kiln diagnostics (not visual acceptance)\n", 1)[1].split("      - name:", 1)[0]
        masters_paths = masters.split("          path: |\n", 1)[1].split("          if-no-files-found:", 1)[0]
        self.assertEqual([line.strip() for line in masters_paths.splitlines()], pairs + [
            "artifacts/review/masters-native-manifest.json"])
        self.assertNotIn("name: ember-kiln-native-diagnostic-", masters)

    def test_schema_types_unknowns_commands_paths_seeds_and_duplicates_rejected(self):
        invalid = [[], None, {}, self.request(schemaVersion=True), self.request(schemaVersion=2),
                   self.request(case=[]), self.request(case=""), self.request(case="masters"),
                   self.request(case="../../wetland"), self.request(case="wetland; touch /tmp/injected"),
                   self.request(case="dev.wildercord.wildlife.WetlandTerrainTest"),
                   self.request(case="diagnostic-wall-turn"), self.request(case="wall-turn#relay"),
                   self.request(case="dev.wildercord.aura.WallTurnLessonTest"),
                   self.request(case="wall-turn; touch /tmp/injected"),
                   self.request(case="diagnostic-kiln-ring"), self.request(case="kiln-ring#inward"),
                   self.request(case="dev.wildercord.aura.world.EmberKilnTest"),
                   self.request(case="kiln-ring; touch /tmp/injected"),
                   self.request(case="diagnostic-progression-feasibility"),
                   self.request(case="progression-feasibility#reweave"),
                   self.request(case="dev.wildercord.cast.ReweaveFeasibilityTest"),
                   self.request(case="dev.wildercord.aura.StoneHingeFeasibilityTest"),
                   self.request(case="progression-feasibility; touch /tmp/injected"),
                   self.request(sourceSha="a" * 39), self.request(sourceSha="A" * 40),
                   self.request(sourceSha="HEAD"), self.request(sourceSha=True),
                   self.request(case=None), self.request(sourceSha=None)]
        for key in ("command", "path", "class", "seed", "env", "retry", "timeout", "shard"):
            invalid.append(self.request(**{key: "anything"}))
        for value in invalid:
            with self.subTest(value=value), self.assertRaises(ValueError):
                diagnostic.parse_request(json.dumps(value))
        for raw in ('{"schemaVersion":1,"case":null,"sourceSha":null,"case":"wetland"}', ' ' * 513, '{'):
            with self.subTest(raw=raw[:80]), self.assertRaises(ValueError):
                diagnostic.parse_request(raw)

    def test_request_only_commit_binds_one_source_revision_and_explicit_reruns(self):
        active, identity = self.request(), {"runAttempt": "1", "headSha": "b" * 40, "observedLiveHeadSha": "b" * 40}
        self.assertEqual(diagnostic.request_state(active, identity, ["a" * 40], [diagnostic.REQUEST]), "ready")
        self.assertEqual(diagnostic.request_state(active, {**identity, "runAttempt": "2"}, ["a" * 40], [diagnostic.REQUEST]), "ready")
        self.assertEqual(diagnostic.request_state(active, {**identity, "runAttempt": "2", "observedLiveHeadSha": "c" * 40}, ["a" * 40], [diagnostic.REQUEST]), "inactive-stale-pr-head")
        for parents, files in (([], [diagnostic.REQUEST]), (["b" * 40], [diagnostic.REQUEST]),
                               (["a" * 40, "b" * 40], [diagnostic.REQUEST]),
                               (["a" * 40], []), (["a" * 40], [diagnostic.REQUEST, "src/change.java"])):
            self.assertEqual(diagnostic.request_state(active, identity, parents, files), "inactive-stale-request")
        self.assertEqual(diagnostic.request_state(self.request(case=None, sourceSha=None), identity, [], []), "disabled")

    def test_environment_rejects_silent_skip_and_injected_options(self):
        diagnostic.check_environment(diagnostic.FIXED_ENV)
        for key in ("WILDERCORD_TOUR_ONLY", "WILDERCORD_CORDS_ONLY", "WILDERCORD_SHOWCASE", "WILDERCORD_UNKNOWN", *diagnostic.DISALLOWED_ENV):
            with self.subTest(key=key), self.assertRaises(ValueError):
                diagnostic.check_environment({**diagnostic.FIXED_ENV, key: "injected"})
        with self.assertRaises(ValueError):
            diagnostic.check_environment({})

    def test_live_public_pr_ref_is_observed_without_shell_or_interactive_login(self):
        with patch.object(diagnostic.subprocess, "check_output", return_value="b" * 40 + "\trefs/pull/25/head\n") as read:
            self.assertEqual(diagnostic.live_head("25"), "b" * 40)
            self.assertEqual(read.call_args.args[0], ["git", "ls-remote", "--exit-code", "origin", "refs/pull/25/head"])
            self.assertEqual(read.call_args.kwargs["env"]["GIT_TERMINAL_PROMPT"], "0")
            self.assertEqual(read.call_args.kwargs["timeout"], 30)
        for response in ("", "b" * 40 + "\trefs/pull/26/head", "bad\trefs/pull/25/head"):
            with patch.object(diagnostic.subprocess, "check_output", return_value=response), self.assertRaises(ValueError):
                diagnostic.live_head("25")

    def test_real_git_request_commit_and_later_source_revision(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root, text=True, stderr=subprocess.DEVNULL).strip()
            git("init"); git("config", "user.name", "Fixture"); git("config", "user.email", "fixture@example.invalid")
            (root / ".github").mkdir(); (root / "code.txt").write_text("source")
            (root / diagnostic.REQUEST).write_text(json.dumps(self.request(case=None, sourceSha=None)))
            git("add", "."); git("commit", "-m", "source")
            source = git("rev-parse", "HEAD")
            request = self.request(sourceSha=source)
            (root / diagnostic.REQUEST).write_text(json.dumps(request))
            git("add", "."); git("commit", "-m", "request")
            head = git("rev-parse", "HEAD")
            env = {"NATIVE_DIAGNOSTIC_HEAD_SHA": head, "GITHUB_EVENT_NAME": "pull_request",
                   "GITHUB_JOB": "native-diagnostic", "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "1", "NATIVE_DIAGNOSTIC_PR": "25"}
            with patch.object(diagnostic, "ROOT", root), patch.object(diagnostic, "live_head", return_value=head):
                identity = diagnostic.identity(env)
                self.assertEqual(identity["headSha"], head)
                self.assertEqual(identity["observedLiveHeadSha"], head)
                self.assertEqual(diagnostic.request_state(request, identity, git("rev-list", "--parents", "-n", "1", "HEAD").split()[1:], git("diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD").splitlines()), "ready")
                with self.assertRaises(ValueError):
                    diagnostic.identity({**env, "NATIVE_DIAGNOSTIC_HEAD_SHA": source})
                (root / "code.txt").write_text("changed")
                with self.assertRaises(ValueError):
                    diagnostic.identity(env)
                git("add", "."); git("commit", "-m", "later source")
                self.assertEqual(diagnostic.request_state(request, identity, git("rev-list", "--parents", "-n", "1", "HEAD").split()[1:], git("diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD").splitlines()), "inactive-stale-request")


class EvidenceTests(unittest.TestCase):
    def fixture(self, case="aura-fx"):
        return {"state": "ready", "request": {"schemaVersion": 1, "case": case, "sourceSha": "a" * 40},
                "selection": suites.select_entries(suite=diagnostic.CASES[case]),
                "provenance": {"headSha": "b" * 40, "runId": "123", "runAttempt": "1"},
                "sourceFilesSha256": {"fixture.java": "c" * 64}, "configuration": {},
                "fullClientGate": "unverified", "focusedClientGate": "unverified"}

    def log(self, data):
        selection = data["selection"]
        completion = ""
        if data["request"]["case"] in ("progression-feasibility", "reweave-player", "stone-hinge-owner-negative", "movement-foundations", "excise", "stone-fault-march"):
            entries = {"progression-feasibility": diagnostic.PROGRESSION_ENTRIES,
                       "reweave-player": diagnostic.REWEAVE_PLAYER_ENTRIES,
                       "stone-hinge-owner-negative": diagnostic.STONE_OWNER_NEGATIVE_ENTRIES,
                       "movement-foundations": diagnostic.MOVEMENT_FOUNDATION_ENTRIES,
                       "excise": diagnostic.LIFE_EXCISE_ENTRIES,
                       "stone-fault-march": diagnostic.STONE_MARCH_ENTRIES}[data["request"]["case"]]
            completion = "".join(diagnostic.SCENE_PREFIX + json.dumps({
                "suite": entry, "event": event, "phase": phase,
                "elapsedSeconds": 1.0, "sceneElapsedSeconds": 0.5}) + "\n"
                for entry in entries
                for event, phase in (("start", "setup"), ("phase", "run"),
                                     ("phase", "cleanup"), ("end", "returned")))
        if data["request"]["case"] == "stone-hinge-owner-negative":
            completion += suites.STONE_OWNER_NEGATIVE_PREFIX + suites.STONE_OWNER_NEGATIVE_RESULT + "\n"
        if data["request"]["case"] == "movement-foundations":
            completion += suites.STONE_VELOCITY_PREFIX + suites.STONE_VELOCITY_RESULT + "\n"
        return (suites.SELECTION_PREFIX + json.dumps(selection) + "\n" + suites.DESCRIPTOR_PREFIX + json.dumps(selection) + "\n"
                + suites.REQUEST_PREFIX + json.dumps(diagnostic.receipt(data)) + "\n"
                + "\n".join(diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": seed or "1"}) for entry, seed in diagnostic.SEEDS[data["request"]["case"]].items())
                + "\n" + completion + "BUILD SUCCESSFUL\n" + suites.EXIT_PREFIX + "0\n")

    def collect(self, data, log, *, launch=True, budget=diagnostic.MAX_LOG_BYTES):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / diagnostic.OUTPUT).mkdir(parents=True)
            if log is not None:
                (root / diagnostic.LOG).write_text(log)
            if launch:
                (root / diagnostic.OUTPUT / "launch.json").write_text("{}")
            with patch.object(diagnostic, "ROOT", root), patch.object(diagnostic, "prepared", return_value=copy.deepcopy(data)), patch.object(diagnostic, "MAX_LOG_BYTES", budget), redirect_stdout(io.StringIO()):
                status = diagnostic.collect({})
            result = json.loads((root / diagnostic.OUTPUT / "diagnostic-result.json").read_text())
            self.assertEqual(result["fullClientGate"], "unverified")
            self.assertEqual(result["focusedClientGate"], "unverified")
            self.assertEqual(status, 0 if result["diagnosticOutcome"] == "passed" else 1)
            return result

    def test_exact_success_records_seed_and_never_accepts_release(self):
        for case in diagnostic.CASES:
            data = self.fixture(case)
            result = self.collect(data, self.log(data))
            self.assertEqual(result["diagnosticOutcome"], "passed")
            self.assertEqual(result["verificationIssues"], [])
            self.assertEqual(set(result["observedWorldSeeds"]), set(diagnostic.SEEDS[case]))

    def test_failed_missing_mixed_truncated_and_replayed_evidence_never_pass(self):
        for case in ("aura-fx", "wall-turn", "kiln-ring", "ecology-return", "stasis-gallery", "progression-feasibility", "reweave-player", "stone-hinge-owner-negative", "movement-foundations", "excise", "stone-fault-march"):
            self.assert_invalid_evidence_never_passes(case)

    def assert_invalid_evidence_never_passes(self, case):
        data = self.fixture(case); good = self.log(data)
        stale = copy.deepcopy(data); stale["provenance"]["runAttempt"] = "2"
        wrong_selection = copy.deepcopy(data); wrong_selection["selection"]["entries"] = []
        wrong_source = copy.deepcopy(data); wrong_source["sourceFilesSha256"]["fixture.java"] = "d" * 64
        logs = [None, "", "BUILD SUCCESSFUL\n", good.replace("BUILD SUCCESSFUL", "BUILD FAILED"),
                good.replace(suites.EXIT_PREFIX + "0", suites.EXIT_PREFIX + "1"),
                good.replace(suites.DESCRIPTOR_PREFIX, "missing "), good + suites.EXIT_PREFIX + "0\n",
                self.log(stale), self.log(wrong_selection), self.log(wrong_source),
                good.replace(diagnostic.SEED_PREFIX, "missing "),
                good + diagnostic.SEED_PREFIX + '{"suite":[],"seed":"1"}\n']
        for log in logs:
            with self.subTest(log=log):
                self.assertEqual(self.collect(data, log)["diagnosticOutcome"], "unverified")
        self.assertEqual(self.collect(data, good, launch=False)["diagnosticOutcome"], "unverified")
        self.assertEqual(self.collect(data, good, budget=30)["diagnosticOutcome"], "unverified")

    def test_wetland_fixed_seed_cannot_be_changed_to_find_pass(self):
        data = self.fixture("wetland")
        result = self.collect(data, self.log(data).replace('-7620530482425397421', '42'))
        self.assertEqual(result["diagnosticOutcome"], "unverified")

    def test_siltcrest_requires_both_quality_world_seeds(self):
        data = self.fixture("siltcrest-presentation")
        good = self.log(data)
        key = "dev.wildercord.wildlife.SiltcrestPresentationTest#minimal"
        distinct = good.replace(json.dumps({"suite": key, "seed": "1"}),
                                json.dumps({"suite": key, "seed": "2"}))
        self.assertEqual(self.collect(data, distinct)["diagnosticOutcome"], "passed")
        missing = "\n".join(line for line in distinct.splitlines()
                            if not (diagnostic.SEED_PREFIX in line and key in line))
        self.assertEqual(self.collect(data, missing)["diagnosticOutcome"], "unverified")
        conflicting = distinct + diagnostic.SEED_PREFIX + json.dumps({"suite": key, "seed": "3"}) + "\n"
        self.assertEqual(self.collect(data, conflicting)["diagnosticOutcome"], "unverified")

    def test_wall_turn_requires_exactly_one_receipt_for_each_original_world(self):
        data = self.fixture("wall-turn")
        expected = {entry: None for entry in (
            "dev.wildercord.aura.WallTurnLessonTest",
            "dev.wildercord.aura.WallTurnSafetyTest",
            "dev.wildercord.aura.WallTurnCommitmentTest",
            "dev.wildercord.aura.WallTurnCommitmentTest#relay",
        )}
        self.assertEqual(diagnostic.SEEDS["wall-turn"], expected)
        good = self.log(data)
        for index, entry in enumerate(expected):
            good = good.replace(json.dumps({"suite": entry, "seed": "1"}),
                                json.dumps({"suite": entry, "seed": str(index + 10)}))
        self.assertEqual(self.collect(data, good)["diagnosticOutcome"], "passed")
        for index, entry in enumerate(expected):
            marker = diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": str(index + 10)})
            invalid = (good.replace(marker, ""), good + marker + "\n",
                       good + diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": "99"}) + "\n")
            for log in invalid:
                with self.subTest(entry=entry, log=log):
                    self.assertEqual(self.collect(data, log)["diagnosticOutcome"], "unverified")
        foreign = good + diagnostic.SEED_PREFIX + json.dumps({"suite": "dev.wildercord.aura.WallRelayChecks", "seed": "1"}) + "\n"
        self.assertEqual(self.collect(data, foreign)["diagnosticOutcome"], "unverified")

    def test_launcher_is_called_once_and_only_for_prepared_fixed_suite(self):
        data = self.fixture()
        with tempfile.TemporaryDirectory() as temp, patch.object(diagnostic, "ROOT", Path(temp)), patch.object(diagnostic, "prepared", return_value=data), patch.object(run_client_ci, "main", return_value=7) as launch:
            (Path(temp) / diagnostic.OUTPUT).mkdir(parents=True)
            self.assertEqual(diagnostic.run(diagnostic.FIXED_ENV), 7)
            launch.assert_called_once()
            self.assertEqual(launch.call_args.args[0][:2], ["--suite", "diagnostic-aura-fx"])
            self.assertEqual(launch.call_args.kwargs["diagnostic_provenance"], diagnostic.receipt(data))
            with self.assertRaises(FileExistsError):
                diagnostic.run(diagnostic.FIXED_ENV)
            self.assertEqual(launch.call_count, 1)

    def test_kiln_requires_exactly_one_receipt_for_each_original_world(self):
        data = self.fixture("kiln-ring")
        expected = {entry: None for entry in (
            "dev.wildercord.aura.world.EmberKilnTest",
            "dev.wildercord.aura.world.EmberKilnPresentationTest",
            "dev.wildercord.aura.world.EmberKilnOpponentViewTest",
        )}
        self.assertEqual(diagnostic.SEEDS["kiln-ring"], expected)
        good = self.log(data)
        for index, entry in enumerate(expected):
            good = good.replace(json.dumps({"suite": entry, "seed": "1"}),
                                json.dumps({"suite": entry, "seed": str(index + 10)}))
        self.assertEqual(self.collect(data, good)["diagnosticOutcome"], "passed")
        for index, entry in enumerate(expected):
            marker = diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": str(index + 10)})
            invalid = (good.replace(marker, ""), good + marker + "\n",
                       good + diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": "99"}) + "\n")
            for log in invalid:
                with self.subTest(entry=entry, log=log):
                    self.assertEqual(self.collect(data, log)["diagnosticOutcome"], "unverified")
        for foreign in ("dev.wildercord.aura.WallTurnLessonTest",
                        "dev.wildercord.aura.world.EmberKilnOpponentViewTest#inward"):
            log = good + diagnostic.SEED_PREFIX + json.dumps({"suite": foreign, "seed": "1"}) + "\n"
            self.assertEqual(self.collect(data, log)["diagnosticOutcome"], "unverified")

    def test_kiln_rejects_replayed_request_head_run_and_attempt_evidence(self):
        data = self.fixture("kiln-ring")
        for section, key, value in (("request", "sourceSha", "d" * 40),
                                    ("provenance", "headSha", "d" * 40),
                                    ("provenance", "runId", "456"),
                                    ("provenance", "runAttempt", "2")):
            stale = copy.deepcopy(data)
            stale[section][key] = value
            with self.subTest(section=section, key=key):
                self.assertEqual(self.collect(data, self.log(stale))["diagnosticOutcome"], "unverified")
        duplicate = self.log(data) + suites.REQUEST_PREFIX + json.dumps(diagnostic.receipt(data)) + "\n"
        self.assertEqual(self.collect(data, duplicate)["diagnosticOutcome"], "unverified")

    def test_stasis_requires_exactly_one_matching_seed_and_current_provenance(self):
        data = self.fixture("stasis-gallery")
        duplicate = self.log(data) + diagnostic.SEED_PREFIX + json.dumps({
            "suite": "dev.wildercord.gametest.WildercordScreenshots", "seed": "1"}) + "\n"
        self.assertEqual(self.collect(data, duplicate)["diagnosticOutcome"], "unverified")
        for key, value in (("headSha", "e" * 40), ("runId", "999"), ("runAttempt", "2")):
            changed = copy.deepcopy(data)
            changed["provenance"][key] = value
            self.assertEqual(self.collect(data, self.log(changed))["diagnosticOutcome"], "unverified")

    def test_unique_seed_receipt_rule_does_not_change_other_cases(self):
        for case in set(diagnostic.CASES) - {"wall-turn", "kiln-ring", "ecology-return", "stasis-gallery", "progression-feasibility", "reweave-player", "stone-hinge-owner-negative", "movement-foundations", "excise", "stone-fault-march"}:
            data = self.fixture(case)
            entry, seed = next(iter(diagnostic.SEEDS[case].items()))
            repeated = self.log(data) + diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": seed or "1"}) + "\n"
            with self.subTest(case=case):
                self.assertEqual(self.collect(data, repeated)["diagnosticOutcome"], "passed")

    def test_catalog_and_launcher_reuse_whole_classes_with_diagnostic_scope(self):
        for case, group in diagnostic.CASES.items():
            selection = suites.select_entries(suite=group)
            self.assertEqual(selection["kind"], "diagnostic")
            self.assertEqual(run_client_ci.launch_command(selection)[-1], "-PciSuite=" + group)
            self.assertEqual(native_ci_diagnostics.snapshot_thresholds(selection), (1800, 2700))
        self.assertEqual(suites.select_entries(suite="diagnostic-aura-fx")["entries"], ["dev.wildercord.gametest.WildercordAuraFxTest"])
        self.assertEqual(suites.select_entries(suite="diagnostic-siltcrest-presentation")["entries"], ["dev.wildercord.wildlife.SiltcrestPresentationTest"])
        self.assertEqual(suites.select_entries(suite="diagnostic-fungal-nursery")["entries"], ["dev.wildercord.wildlife.FungalNurseryTest"])
        self.assertEqual(suites.select_entries(suite="diagnostic-wetland")["entries"][-3:], ["dev.wildercord.wildlife.WetlandGardenTest", "dev.wildercord.wildlife.WetlandTerrainAbsenceTest", "dev.wildercord.wildlife.WetlandTerrainTest"])

    def test_wall_turn_selects_only_three_whole_classes_in_original_order(self):
        self.assertEqual(diagnostic.CASES["wall-turn"], "diagnostic-wall-turn")
        entries = ["dev.wildercord.aura.WallTurnLessonTest",
                   "dev.wildercord.aura.WallTurnSafetyTest",
                   "dev.wildercord.aura.WallTurnCommitmentTest"]
        selection = suites.select_entries(suite="diagnostic-wall-turn")
        self.assertEqual(selection, {"kind": "diagnostic", "name": "diagnostic-wall-turn",
                                     "count": 3, "entries": entries})
        for source in (suites.select_entries(), suites.select_entries(suite="masters")):
            self.assertEqual([entry for entry in source["entries"] if entry in entries], entries)

    def test_general_manifest_cannot_relabel_diagnostic_as_focused_or_full(self):
        for case in ("aura-fx", "wall-turn", "kiln-ring", "ecology-return", "stasis-gallery", "progression-feasibility"):
            self.assert_manifest_cannot_relabel_diagnostic(case)

    def test_ecology_return_selects_only_three_whole_classes_and_observes_reopen(self):
        self.assertEqual(diagnostic.CASE_FILES["ecology-return"], (
            "src/main/java/dev/wildercord/wildlife/LanternNewt.java",
            "src/main/java/dev/wildercord/wildlife/NewtPathNavigation.java"))
        entries = ["dev.wildercord.wildlife.RootmoltCounterTest",
                   "dev.wildercord.wildlife.ReedRefugeTest",
                   "dev.wildercord.wildlife.SiltcrestBankReturnTest"]
        self.assertEqual(diagnostic.CASES["ecology-return"], "diagnostic-ecology-return")
        self.assertEqual(suites.select_entries(suite="diagnostic-ecology-return"),
                         {"kind": "diagnostic", "name": "diagnostic-ecology-return",
                          "count": 3, "entries": entries})
        expected = {entry: None for entry in [*entries, entries[1] + "#reopen"]}
        self.assertEqual(diagnostic.SEEDS["ecology-return"], expected)
        data = self.fixture("ecology-return")
        good = self.log(data)
        for entry in expected:
            marker = diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": "1"})
            for bad in (good.replace(marker, ""), good + marker + "\n"):
                with self.subTest(entry=entry):
                    self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")

    def test_kiln_selects_only_three_whole_classes_in_original_order(self):
        self.assertEqual(diagnostic.CASES["kiln-ring"], "diagnostic-kiln-ring")
        entries = ["dev.wildercord.aura.world.EmberKilnTest",
                   "dev.wildercord.aura.world.EmberKilnPresentationTest",
                   "dev.wildercord.aura.world.EmberKilnOpponentViewTest"]
        selection = suites.select_entries(suite="diagnostic-kiln-ring")
        self.assertEqual(selection, {"kind": "diagnostic", "name": "diagnostic-kiln-ring",
                                     "count": 3, "entries": entries})
        masters = suites.select_entries(suite="masters")
        self.assertEqual(masters["count"], 43)
        for source in (suites.select_entries(), masters):
            self.assertEqual([entry for entry in source["entries"] if entry in entries], entries)

    def test_excise_cut_boundary_instrumentation_is_bound_to_source(self):
        for path in ("src/gametest/java/dev/wildercord/gametest/mixin/ExciseCutProbeMixin.java",
                     "src/gametest/resources/excise-cut-gametest.mixins.json"):
            self.assertIn(path, diagnostic.CASE_FILES["excise"])
            self.assertTrue((suites.ROOT / path).is_file())

    def test_stone_march_requires_three_whole_classes_and_exact_seeds(self):
        entries = ["dev.wildercord.aura.world.StoneMarchTest",
                   "dev.wildercord.aura.world.StoneMarchPresentationTest",
                   "dev.wildercord.aura.world.StoneMarchOpponentViewTest"]
        self.assertEqual(diagnostic.CASES["stone-fault-march"], "stone-fault-march")
        self.assertEqual(diagnostic.STONE_MARCH_ENTRIES, tuple(entries))
        self.assertEqual(diagnostic.SEEDS["stone-fault-march"], dict.fromkeys(entries))
        for selection in (suites.select_entries(), suites.select_entries(suite="masters")):
            self.assertEqual([e for e in selection["entries"] if e in entries], entries)
        data = self.fixture("stone-fault-march")
        good = self.log(data)
        self.assertEqual(self.collect(data, good)["completedEntries"], entries)
        for entry in entries:
            seed = diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": "1"})
            returned = diagnostic.SCENE_PREFIX + json.dumps({
                "suite": entry, "event": "end", "phase": "returned",
                "elapsedSeconds": 1.0, "sceneElapsedSeconds": 0.5})
            cleanup = returned.replace('"event": "end", "phase": "returned"',
                                       '"event": "phase", "phase": "cleanup"')
            for bad in (good.replace(seed, ""), good + seed + "\n",
                        good.replace(returned, ""), good + returned + "\n",
                        good.replace(cleanup, "")):
                with self.subTest(entry=entry):
                    self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")
        reordered = good.replace(entries[0], "TEMP_ENTRY").replace(entries[1], entries[0]).replace("TEMP_ENTRY", entries[1])
        self.assertEqual(self.collect(data, reordered)["diagnosticOutcome"], "unverified")
        self.assert_manifest_cannot_relabel_diagnostic("stone-fault-march")

    def assert_manifest_cannot_relabel_diagnostic(self, case):
        data = self.fixture(case)
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); log=root/"native.log"; out=root/"manifest.json"; log.write_text(self.log(data))
            for selector in ([], ["--suite", "masters"], ["--suite", "articulated"], ["--shard", "4/4"], ["--suite", diagnostic.CASES[case]]):
                with patch.object(test_manifest, "ROOT", root), redirect_stdout(io.StringIO()):
                    test_manifest.main(["--log", str(log), "--output", str(out), *selector])
                manifest=json.loads(out.read_text())
                self.assertEqual(manifest["fullClientGate"], "unverified")
                self.assertNotEqual(manifest.get("focusedClientGate"), "passed")

    def test_collect_preserves_evidence_after_head_advances_or_origin_fails(self):
        for case in ("aura-fx", "wall-turn", "kiln-ring", "ecology-return", "stasis-gallery", "progression-feasibility"):
            self.assert_collect_preserves_evidence(case)

    def assert_collect_preserves_evidence(self, case):
        original_root = diagnostic.ROOT
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            selection = suites.select_entries(suite=diagnostic.CASES[case])
            paths = [*diagnostic.CONFIG_FILES, *diagnostic.CASE_FILES.get(case, ()), ".gitignore", *["src/gametest/java/" + name.replace(".", "/") + ".java" for name in selection["entries"]]]
            if case == "wall-turn":
                paths.append("src/gametest/java/dev/wildercord/aura/WallRelayChecks.java")
            for name in paths:
                target = root / name; target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(original_root / name, target)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root, text=True, stderr=subprocess.DEVNULL).strip()
            git("init"); git("config", "user.name", "Fixture"); git("config", "user.email", "fixture@example.invalid")
            git("add", "."); git("commit", "-m", "source")
            source = git("rev-parse", "HEAD")
            (root / diagnostic.REQUEST).write_text(json.dumps({"schemaVersion": 1, "case": case, "sourceSha": source}))
            git("add", "."); git("commit", "-m", "request")
            head = git("rev-parse", "HEAD")
            env = {"NATIVE_DIAGNOSTIC_HEAD_SHA": head, "GITHUB_EVENT_NAME": "pull_request", "GITHUB_JOB": "native-diagnostic",
                   "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "1", "NATIVE_DIAGNOSTIC_PR": "25"}
            with patch.object(diagnostic, "ROOT", root), patch.object(diagnostic, "live_head", return_value=head), redirect_stdout(io.StringIO()):
                diagnostic.prepare(env)
                data = diagnostic.prepared(env)
            self.assertEqual(data["selection"], selection)
            self.assertEqual(data["configuration"]["declaredWorldSeeds"], diagnostic.SEEDS[case])
            self.assertEqual(data["sourceFilesSha256"], {path: diagnostic.digest(root / path)
                                                       for path in paths if path != ".gitignore"})
            (root / diagnostic.LOG).write_text(self.log(data))
            (root / diagnostic.OUTPUT / "launch.json").write_text("{}")
            with patch.object(diagnostic, "ROOT", root), patch.object(diagnostic, "live_head", return_value="d" * 40), redirect_stdout(io.StringIO()):
                with self.assertRaises(ValueError):
                    diagnostic.prepared(env)
                self.assertEqual(diagnostic.collect(env), 0)
            result = json.loads((root / diagnostic.OUTPUT / "diagnostic-result.json").read_text())
            self.assertEqual(result["headAtCollection"]["status"], "superseded")
            with patch.object(diagnostic, "ROOT", root), patch.object(diagnostic, "live_head", side_effect=subprocess.TimeoutExpired("git", 30)), redirect_stdout(io.StringIO()):
                self.assertEqual(diagnostic.collect(env), 0)
            result = json.loads((root / diagnostic.OUTPUT / "diagnostic-result.json").read_text())
            self.assertEqual(result["headAtCollection"]["status"], "unavailable")
            self.assertEqual(result["provenance"]["headSha"], head)
            self.assertEqual((root / diagnostic.OUTPUT / "native.log").read_text(), self.log(data))

    def test_excise_requires_all_five_whole_classes_and_four_observed_worlds(self):
        entry = diagnostic.EXCISE_ENTRIES[0]; group = "diagnostic-life-excise"; entries = list(diagnostic.LIFE_EXCISE_ENTRIES)
        self.assertEqual(suites.select_entries(suite=group), {"kind": "diagnostic", "name": group, "count": 5, "entries": entries})
        self.assertEqual(diagnostic.SEEDS["excise"], {
            "dev.wildercord.client.fx.LifeFormationTest": None,
            "dev.wildercord.client.fx.LifeFlightTest": None,
            "dev.wildercord.cast.ExcisePlayableTest#lesson": None,
            "dev.wildercord.cast.ExcisePlayableTest": None})
        self.assertEqual(suites.select_entries(suite="masters")["entries"].count(entry), 1)
        self.assertNotIn(entry, suites.select_entries(suite="articulated")["entries"])
        data = self.fixture("excise"); good = self.log(data)
        result = self.collect(data, good)
        self.assertEqual(result["completedEntries"], entries)
        self.assert_invalid_evidence_never_passes("excise")
        old_single = copy.deepcopy(data); old_single["selection"] = {"kind": "diagnostic", "name": "diagnostic-excise", "count": 1, "entries": [entry]}
        self.assertEqual(self.collect(data, self.log(old_single))["diagnosticOutcome"], "unverified")
        for invalid in (good.replace('"phase": "cleanup"', '"phase": "missing"'),
                        good.replace('"phase": "returned"', '"phase": "threw"'),
                        good + diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": "1"}) + "\n",
                        good.replace('"seed": "1"', '"seed": "9223372036854775808"')):
            with self.subTest(log=invalid):
                self.assertEqual(self.collect(data, invalid)["diagnosticOutcome"], "unverified")
        for update in ({"purpose": "release"}, {"entries": [diagnostic.REWEAVE_PLAYER_ENTRIES[0]]},
                       {"entries": [entry], "expectedCount": 1}, {"entries": entries[::-1]}):
            with tempfile.TemporaryDirectory() as temp:
                catalog = json.loads(suites.CATALOG.read_text()); catalog[group].update(update)
                path = Path(temp) / "catalog.json"; path.write_text(json.dumps(catalog))
                with self.subTest(update=update), self.assertRaises(ValueError):
                    suites.select_entries(suite=group, catalog=path)

    def test_movement_foundations_requires_both_whole_classes_and_fifteen_case_owner_result(self):
        group = "diagnostic-movement-foundations"; entries = list(diagnostic.MOVEMENT_FOUNDATION_ENTRIES)
        self.assertEqual(suites.select_entries(suite=group), {"kind": "diagnostic", "name": group, "count": 2, "entries": entries})
        self.assertEqual(set(diagnostic.SEEDS["movement-foundations"]), {diagnostic.STONE_VELOCITY_ENTRY, diagnostic.REED_REFUGE_ENTRY, diagnostic.REED_REFUGE_ENTRY + "#reopen"})
        for release in ("masters", "articulated"):
            self.assertNotIn(diagnostic.STONE_VELOCITY_ENTRY, suites.select_entries(suite=release)["entries"])
        data = self.fixture("movement-foundations"); good = self.log(data)
        result = self.collect(data, good)
        self.assertEqual(result["completedEntries"], entries)
        for key in ("movementGate", "peerGate", "latencyGate", "admissionGate"):
            self.assertEqual(result[key], "NOT_PROVEN")
        self.assertIs(result["gameplayEnabled"], False)
        self.assert_invalid_evidence_never_passes("movement-foundations")
        old_order = copy.deepcopy(data); old_order["selection"]["entries"].reverse()
        self.assertEqual(self.collect(data, self.log(old_order))["diagnosticOutcome"], "unverified")
        for invalid in (good.replace("owner_cases=15", "owner_cases=14"),
                        good.replace("movement_gate=NOT_PROVEN", "movement_gate=PROVEN"),
                        good.replace("peer_gate=NOT_PROVEN", "peer_gate=observed"),
                        good.replace("gameplay_enabled=false", "gameplay_enabled=true"),
                        good.replace(suites.STONE_VELOCITY_PREFIX, "missing "),
                        good + suites.STONE_VELOCITY_PREFIX + suites.STONE_VELOCITY_RESULT + "\n",
                        good.replace('"phase": "cleanup"', '"phase": "missing"'),
                        good.replace('"phase": "returned"', '"phase": "threw"'),
                        good + diagnostic.SEED_PREFIX + json.dumps({"suite": entries[1], "seed": "1"}) + "\n"):
            with self.subTest(log=invalid):
                self.assertEqual(self.collect(data, invalid)["diagnosticOutcome"], "unverified")
        for update in ({"purpose": "release"}, {"entries": entries[::-1]},
                       {"entries": entries[:1], "expectedCount": 1},
                       {"entries": [entries[0], entries[0]]}):
            with tempfile.TemporaryDirectory() as temp:
                catalog = json.loads(suites.CATALOG.read_text()); catalog[group].update(update)
                path = Path(temp) / "catalog.json"; path.write_text(json.dumps(catalog))
                with self.subTest(update=update), self.assertRaises(ValueError):
                    suites.select_entries(suite=group, catalog=path)

    def test_stone_owner_negative_retains_scope_completion_and_two_worlds(self):
        entry = diagnostic.STONE_OWNER_NEGATIVE_ENTRIES[0]
        group = "diagnostic-stone-hinge-owner-negative"
        self.assertEqual(suites.select_entries(suite=group), {
            "kind": "diagnostic", "name": group, "count": 1, "entries": [entry]})
        self.assertEqual(set(diagnostic.SEEDS["stone-hinge-owner-negative"]), {
            entry + "#ordinary-native-hit-control", entry + "#server-step-plus-motion-negative"})
        for release in ("masters", "articulated"):
            self.assertNotIn(entry, suites.select_entries(suite=release)["entries"])
        self.assertEqual(suites.select_entries()["entries"].count(entry), 1)
        data = self.fixture("stone-hinge-owner-negative"); good = self.log(data)
        result = self.collect(data, good)
        self.assertEqual(result["completedEntries"], [entry])
        self.assertEqual(result["movementGate"], "NOT_PROVEN")
        self.assertIs(result["gameplayEnabled"], False)
        self.assert_invalid_evidence_never_passes("stone-hinge-owner-negative")
        for invalid in (good.replace("movement_gate=NOT_PROVEN", "movement_gate=PROVEN"),
                        good.replace("gameplay_enabled=false", "gameplay_enabled=true"),
                        good.replace("conditional_shared_rest_ticks=120", "conditional_shared_rest_ticks=0"),
                        good.replace(suites.STONE_OWNER_NEGATIVE_PREFIX, "missing "),
                        good + suites.STONE_OWNER_NEGATIVE_PREFIX + suites.STONE_OWNER_NEGATIVE_RESULT + "\n",
                        good.replace('"phase": "cleanup"', '"phase": "missing"'),
                        good.replace('"phase": "returned"', '"phase": "threw"'),
                        good + diagnostic.SEED_PREFIX + json.dumps({"suite": entry + "#ordinary-native-hit-control", "seed": "1"}) + "\n"):
            with self.subTest(log=invalid):
                self.assertEqual(self.collect(data, invalid)["diagnosticOutcome"], "unverified")

    def test_stone_owner_negative_rejects_relabelled_or_foreign_selection(self):
        group = "diagnostic-stone-hinge-owner-negative"
        for update in ({"purpose": "release"}, {"entries": [diagnostic.PROGRESSION_ENTRIES[1]]},
                       {"entries": list(diagnostic.STONE_OWNER_NEGATIVE_ENTRIES) * 2, "expectedCount": 2}):
            with tempfile.TemporaryDirectory() as temp:
                catalog = json.loads(suites.CATALOG.read_text()); catalog[group].update(update)
                path = Path(temp) / "catalog.json"; path.write_text(json.dumps(catalog))
                with self.subTest(update=update), self.assertRaises(ValueError):
                    suites.select_entries(suite=group, catalog=path)

    def test_reweave_player_selects_one_whole_class_and_two_unique_worlds(self):
        entry = "dev.wildercord.cast.ReweavePlayableTest"
        self.assertEqual(suites.select_entries(suite="diagnostic-reweave-player"), {
            "kind": "diagnostic", "name": "diagnostic-reweave-player", "count": 1, "entries": [entry]})
        self.assertEqual(set(diagnostic.SEEDS["reweave-player"]), {entry + "#lesson", entry + "#input"})
        data = self.fixture("reweave-player")
        self.assertEqual(self.collect(data, self.log(data))["diagnosticOutcome"], "passed")
        duplicate = self.log(data) + diagnostic.SEED_PREFIX + json.dumps({"suite": entry + "#input", "seed": "1"}) + "\n"
        self.assertEqual(self.collect(data, duplicate)["diagnosticOutcome"], "unverified")
        self.assertEqual(suites.select_entries(suite="masters")["entries"].count(entry), 1)
        self.assertNotIn(entry, suites.select_entries(suite="articulated")["entries"])

    def test_reweave_player_requires_exact_cleanup_and_return(self):
        data = self.fixture("reweave-player"); good = self.log(data)
        result = self.collect(data, good)
        self.assertEqual(result["completedEntries"], list(diagnostic.REWEAVE_PLAYER_ENTRIES))
        for invalid in ("\n".join(line for line in good.splitlines() if diagnostic.SCENE_PREFIX not in line),
                        good.replace('"phase": "cleanup"', '"phase": "missing"'),
                        good.replace('"phase": "returned"', '"phase": "threw"'),
                        good + next(line for line in good.splitlines() if diagnostic.SCENE_PREFIX in line) + "\n"):
            self.assertEqual(self.collect(data, invalid)["diagnosticOutcome"], "unverified")

    def test_progression_selects_exactly_two_registered_whole_classes_outside_release_rosters(self):
        entries = ["dev.wildercord.cast.ReweaveFeasibilityTest", "dev.wildercord.aura.StoneHingeFeasibilityTest"]
        self.assertEqual(list(diagnostic.PROGRESSION_ENTRIES), entries)
        self.assertEqual(diagnostic.CASES["progression-feasibility"], "diagnostic-progression-feasibility")
        self.assertEqual(suites.select_entries(suite="diagnostic-progression-feasibility"), {
            "kind": "diagnostic", "name": "diagnostic-progression-feasibility", "count": 2, "entries": entries})
        self.assertEqual(suites.select_entries()["entries"][-6:], entries + ["dev.wildercord.cast.ReweavePlayableTest", *diagnostic.STONE_OWNER_NEGATIVE_ENTRIES, diagnostic.STONE_VELOCITY_ENTRY, *diagnostic.EXCISE_ENTRIES])
        for name, count in (("masters", 43), ("articulated", 6)):
            selection = suites.select_entries(suite=name)
            self.assertEqual(selection["count"], count)
            self.assertTrue(set(entries).isdisjoint(selection["entries"]))
        contract = json.loads((suites.ROOT / "src/gametest/resources/cast-receipt-native-contract.json").read_text())
        self.assertEqual(contract["expectedCount"], 46)
        self.assertTrue(set(entries).isdisjoint(contract["cases"]))
        self.assertEqual(diagnostic.SEEDS["progression-feasibility"], dict.fromkeys(entries))
        descriptor = json.loads(suites.DESCRIPTOR.read_text())
        self.assertEqual(descriptor["mixins"].count("stone-hinge-proof-gametest.mixins.json"), 1)
        for entry in entries:
            source = (suites.ROOT / ("src/gametest/java/" + entry.replace(".", "/") + ".java")).read_text()
            self.assertEqual(source.count("WILDERCORD_NATIVE_WORLD "), 1)
            self.assertIn("server.overworld().getSeed()", source)

    def test_progression_requires_each_seed_and_complete_ordered_cleanup_return(self):
        data = self.fixture("progression-feasibility"); good = self.log(data)
        result = self.collect(data, good)
        self.assertEqual(result["diagnosticOutcome"], "passed")
        self.assertEqual(result["completedEntries"], list(diagnostic.PROGRESSION_ENTRIES))
        lines = good.splitlines()
        for index, line in enumerate(lines):
            if diagnostic.SEED_PREFIX in line or diagnostic.SCENE_PREFIX in line:
                for bad in ("\n".join(lines[:index] + lines[index + 1:]), good + line + "\n"):
                    with self.subTest(marker=line):
                        self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")
        for old, new in (("returned", "threw"), ("cleanup", "run"),
                         (diagnostic.PROGRESSION_ENTRIES[0], "foreign.Class")):
            self.assertEqual(self.collect(data, good.replace(old, new))["diagnosticOutcome"], "unverified")
        scene_lines = [line for line in lines if diagnostic.SCENE_PREFIX in line]
        remaining = [line for line in lines if diagnostic.SCENE_PREFIX not in line]
        self.assertEqual(self.collect(data, "\n".join(remaining + list(reversed(scene_lines))))["diagnosticOutcome"], "unverified")
        for malformed in ("{", "[]", "null", '{"suite":[]}',
                          '{"suite":"x","suite":"y"}',
                          scene_lines[0].split(diagnostic.SCENE_PREFIX, 1)[1].replace('1.0', 'true'),
                          scene_lines[0].split(diagnostic.SCENE_PREFIX, 1)[1].replace('1.0', 'NaN')):
            self.assertEqual(self.collect(data, good + diagnostic.SCENE_PREFIX + malformed + "\n")["diagnosticOutcome"], "unverified")

    def test_progression_catalog_rejects_partial_reordered_duplicate_foreign_or_release_selections(self):
        for update in ({"entries": [diagnostic.PROGRESSION_ENTRIES[0]], "expectedCount": 1},
                       {"entries": list(reversed(diagnostic.PROGRESSION_ENTRIES))},
                       {"entries": [diagnostic.PROGRESSION_ENTRIES[0]] * 2},
                       {"entries": [diagnostic.PROGRESSION_ENTRIES[0], "foreign.Class"]},
                       {"purpose": "release"}, {"expectedCount": True}):
            with tempfile.TemporaryDirectory() as temp:
                catalog = json.loads(suites.CATALOG.read_text())
                catalog["diagnostic-progression-feasibility"].update(update)
                path = Path(temp) / "catalog.json"; path.write_text(json.dumps(catalog))
                with self.subTest(update=update), self.assertRaises(ValueError):
                    suites.select_entries(suite="diagnostic-progression-feasibility", catalog=path)

    def test_progression_malformed_seed_and_generic_manifest_without_completion_cannot_pass(self):
        data = self.fixture("progression-feasibility"); good = self.log(data)
        entry = diagnostic.PROGRESSION_ENTRIES[0]
        for bad in (json.dumps({"suite": entry, "seed": str(2 ** 63)}),
                    json.dumps({"suite": entry, "seed": "1", "override": True}),
                    '{"suite": "' + entry + '", "seed":"1", "seed":"1"}'):
            marker = diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": "1"})
            self.assertEqual(self.collect(data, good.replace(marker, diagnostic.SEED_PREFIX + bad))["diagnosticOutcome"], "unverified")
        incomplete = "\n".join(line for line in good.splitlines() if diagnostic.SCENE_PREFIX not in line)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); log = root / "native.log"; out = root / "manifest.json"; log.write_text(incomplete)
            with patch.object(test_manifest, "ROOT", root), redirect_stdout(io.StringIO()):
                test_manifest.main(["--log", str(log), "--output", str(out), "--suite", "diagnostic-progression-feasibility"])
            self.assertEqual(json.loads(out.read_text())["diagnosticOutcome"], "unverified")

    def test_progression_rejects_stale_head_source_run_and_attempt_receipts(self):
        data = self.fixture("progression-feasibility")
        for key, value in (("headSha", "e" * 40), ("runId", "999"), ("runAttempt", "2")):
            changed = copy.deepcopy(data); changed["provenance"][key] = value
            self.assertEqual(self.collect(data, self.log(changed))["diagnosticOutcome"], "unverified")
        changed = copy.deepcopy(data); changed["request"]["sourceSha"] = "e" * 40
        self.assertEqual(self.collect(data, self.log(changed))["diagnosticOutcome"], "unverified")
        request = data["request"]
        identity = {"headSha": "b" * 40, "observedLiveHeadSha": "b" * 40}
        self.assertEqual(diagnostic.request_state(request, identity, ["c" * 40], [diagnostic.REQUEST]), "inactive-stale-request")
        self.assertEqual(diagnostic.request_state(request, {**identity, "observedLiveHeadSha": "c" * 40}, ["a" * 40], [diagnostic.REQUEST]), "inactive-stale-pr-head")

    def test_progression_current_rejects_incomplete_reordered_or_relabelled_catalog(self):
        data = self.fixture("progression-feasibility")
        for entries, count, kind in ((list(diagnostic.PROGRESSION_ENTRIES[:1]), 1, "diagnostic"),
                                     (list(reversed(diagnostic.PROGRESSION_ENTRIES)), 2, "diagnostic"),
                                     (list(diagnostic.PROGRESSION_ENTRIES), 2, "suite")):
            selection = {**data["selection"], "entries": entries, "count": count, "kind": kind}
            with tempfile.TemporaryDirectory() as temp, patch.object(diagnostic, "ROOT", Path(temp)):
                path = Path(temp) / diagnostic.REQUEST; path.parent.mkdir(); path.write_text(json.dumps(data["request"]))
                with patch.object(diagnostic, "identity", return_value={"headSha": "b" * 40, "observedLiveHeadSha": "b" * 40}), \
                        patch.object(diagnostic, "git", side_effect=["b" * 40 + " " + "a" * 40, diagnostic.REQUEST]), \
                        patch.object(diagnostic, "select_entries", return_value=selection), self.assertRaises(ValueError):
                    diagnostic.current({})

    def test_unknown_cli_arguments_fail_without_native_launch(self):
        with patch.object(run_client_ci, "main") as launch:
            for args in (["--run", "--case", "aura-fx"], ["--run", "--prepare"], ["--run", "--command", "sh"]):
                with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                    diagnostic.main(args)
            launch.assert_not_called()

    def test_both_watchdogs_preserve_bounded_stacks_and_reject_symlinks(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(diagnostic, "ROOT", Path(temp)):
            root = Path(temp); source = root / diagnostic.SNAPSHOTS
            external, jvm = source / "run-fixture", source / "jvm-fixture"
            external.mkdir(parents=True); jvm.mkdir()
            (external / "after-1800s-pid-123.txt").write_text("external stack")
            (jvm / "snapshot-2700.txt").write_text("self JVM stack")
            (jvm / "snapshot-1800.txt").write_bytes(b"x" * (diagnostic.MAX_SNAPSHOT_BYTES + 1))
            (jvm / "anything.txt").write_text("unrelated")
            (root / "secret.txt").write_text("outside")
            (external / "after-2700s-pid-123.txt").symlink_to(root / "secret.txt")
            result = diagnostic.preserve_snapshots()
            self.assertEqual(len(result["files"]), 3)
            paths = {item["path"] for item in result["files"]}
            self.assertIn("snapshots/run-fixture/after-1800s-pid-123.txt", paths)
            self.assertIn("snapshots/jvm-fixture/snapshot-2700.txt", paths)
            self.assertEqual(sum(item["truncated"] for item in result["files"]), 1)
            self.assertTrue(all((root / diagnostic.OUTPUT / item["path"]).stat().st_size <= diagnostic.MAX_SNAPSHOT_BYTES for item in result["files"]))
            with patch.object(diagnostic, "MAX_SNAPSHOTS", 1):
                result = diagnostic.preserve_snapshots()
                self.assertEqual(len(result["files"]), 1)
                self.assertEqual(len(result["omitted"]), 1)

    def test_symlink_snapshot_parent_cannot_escape_fixed_directory(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(diagnostic, "ROOT", Path(temp)):
            root = Path(temp)
            (root / "outside/run-fixture").mkdir(parents=True)
            (root / "outside/run-fixture/after-1800s-pid-1.txt").write_text("outside")
            (root / "build/run/clientGameTest").mkdir(parents=True)
            (root / "build/run/clientGameTest/logs").symlink_to(root / "outside", target_is_directory=True)
            self.assertEqual(diagnostic.preserve_snapshots()["files"], [])


if __name__ == "__main__":
    unittest.main()
