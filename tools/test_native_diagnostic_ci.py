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

    def test_request_accepts_only_fixed_cases_or_disabled(self):
        for data in (self.request(), self.request(case="aura-fx"), self.request(case=None, sourceSha=None)):
            self.assertEqual(diagnostic.parse_request(json.dumps(data)), data)

    def test_schema_types_unknowns_commands_paths_seeds_and_duplicates_rejected(self):
        invalid = [[], None, {}, self.request(schemaVersion=True), self.request(schemaVersion=2),
                   self.request(case=[]), self.request(case=""), self.request(case="masters"),
                   self.request(case="../../wetland"), self.request(case="wetland; touch /tmp/injected"),
                   self.request(case="dev.wildercord.wildlife.WetlandTerrainTest"),
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
        return (suites.SELECTION_PREFIX + json.dumps(selection) + "\n" + suites.DESCRIPTOR_PREFIX + json.dumps(selection) + "\n"
                + suites.REQUEST_PREFIX + json.dumps(diagnostic.receipt(data)) + "\n"
                + "\n".join(diagnostic.SEED_PREFIX + json.dumps({"suite": entry, "seed": seed or "1"}) for entry, seed in diagnostic.SEEDS[data["request"]["case"]].items())
                + "\nBUILD SUCCESSFUL\n" + suites.EXIT_PREFIX + "0\n")

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
        data = self.fixture(); good = self.log(data)
        stale = copy.deepcopy(data); stale["provenance"]["runAttempt"] = "2"
        logs = [None, "", "BUILD SUCCESSFUL\n", good.replace("BUILD SUCCESSFUL", "BUILD FAILED"),
                good.replace(suites.EXIT_PREFIX + "0", suites.EXIT_PREFIX + "1"),
                good.replace(suites.DESCRIPTOR_PREFIX, "missing "), good + suites.EXIT_PREFIX + "0\n",
                self.log(stale), good.replace(diagnostic.SEED_PREFIX, "missing "),
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

    def test_catalog_and_launcher_reuse_whole_classes_with_diagnostic_scope(self):
        for case, group in diagnostic.CASES.items():
            selection = suites.select_entries(suite=group)
            self.assertEqual(selection["kind"], "diagnostic")
            self.assertEqual(run_client_ci.launch_command(selection)[-1], "-PciSuite=" + group)
            self.assertEqual(native_ci_diagnostics.snapshot_thresholds(selection), (1800, 2700))
        self.assertEqual(suites.select_entries(suite="diagnostic-aura-fx")["entries"], ["dev.wildercord.gametest.WildercordAuraFxTest"])
        self.assertEqual(suites.select_entries(suite="diagnostic-wetland")["entries"][-3:], ["dev.wildercord.wildlife.WetlandGardenTest", "dev.wildercord.wildlife.WetlandTerrainAbsenceTest", "dev.wildercord.wildlife.WetlandTerrainTest"])

    def test_general_manifest_cannot_relabel_diagnostic_as_focused_or_full(self):
        data = self.fixture()
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); log=root/"native.log"; out=root/"manifest.json"; log.write_text(self.log(data))
            for selector in ([], ["--suite", "masters"], ["--suite", "articulated"], ["--shard", "4/4"], ["--suite", "diagnostic-aura-fx"]):
                with patch.object(test_manifest, "ROOT", root), redirect_stdout(io.StringIO()):
                    test_manifest.main(["--log", str(log), "--output", str(out), *selector])
                manifest=json.loads(out.read_text())
                self.assertEqual(manifest["fullClientGate"], "unverified")
                self.assertNotEqual(manifest.get("focusedClientGate"), "passed")

    def test_collect_preserves_evidence_after_head_advances_or_origin_fails(self):
        original_root = diagnostic.ROOT
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            selection = suites.select_entries(suite="diagnostic-aura-fx")
            paths = [*diagnostic.CONFIG_FILES, ".gitignore", *["src/gametest/java/" + name.replace(".", "/") + ".java" for name in selection["entries"]]]
            for name in paths:
                target = root / name; target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(original_root / name, target)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root, text=True, stderr=subprocess.DEVNULL).strip()
            git("init"); git("config", "user.name", "Fixture"); git("config", "user.email", "fixture@example.invalid")
            git("add", "."); git("commit", "-m", "source")
            source = git("rev-parse", "HEAD")
            (root / diagnostic.REQUEST).write_text(json.dumps({"schemaVersion": 1, "case": "aura-fx", "sourceSha": source}))
            git("add", "."); git("commit", "-m", "request")
            head = git("rev-parse", "HEAD")
            env = {"NATIVE_DIAGNOSTIC_HEAD_SHA": head, "GITHUB_EVENT_NAME": "pull_request", "GITHUB_JOB": "native-diagnostic",
                   "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "1", "NATIVE_DIAGNOSTIC_PR": "25"}
            with patch.object(diagnostic, "ROOT", root), patch.object(diagnostic, "live_head", return_value=head), redirect_stdout(io.StringIO()):
                diagnostic.prepare(env)
                data = diagnostic.prepared(env)
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
