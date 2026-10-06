"""Stdlib regression checks for native CI selection, launch arguments and scoped evidence.

Run with: python -m unittest discover -s tools -p 'test_client_ci.py' -v
These tests do not launch Minecraft or establish a native gameplay pass.
"""
from contextlib import redirect_stderr, redirect_stdout
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch

import client_suites
import run_client_ci
import test_manifest


def launcher_log(selection, *, result=0, build="BUILD SUCCESSFUL"):
    descriptor = (client_suites.DESCRIPTOR_PREFIX + json.dumps(selection) + "\n"
                  if selection["kind"] == "suite" else "")
    return (client_suites.SELECTION_PREFIX + json.dumps(selection) + "\n" + descriptor + build + "\n"
            + client_suites.EXIT_PREFIX + str(result) + "\n")


class SelectionTests(unittest.TestCase):
    def test_masters_update_is_twenty_one_registered_unique_entries(self):
        selected = client_suites.select_entries(suite="masters")
        full = client_suites.select_entries()
        self.assertEqual(selected["count"], 21)
        self.assertEqual(len(set(selected["entries"])), 21)
        self.assertTrue(set(selected["entries"]).issubset(full["entries"]))

    def test_articulated_is_five_registered_acceptance_slices(self):
        selected = client_suites.select_entries(suite="articulated")
        self.assertEqual(selected["count"], 5)
        self.assertEqual(selected["entries"], [
            "dev.wildercord.client.CombatPresentationSettingsTest",
            "dev.wildercord.client.combat.ArticulatedCombatPresentationTest",
            "dev.wildercord.client.combat.ArticulatedArmorPresentationTest",
            "dev.wildercord.client.combat.ArticulatedFirstPersonCompositionTest",
            "dev.wildercord.client.combat.ArticulatedAuraShellPresentationTest",
        ])
        self.assertTrue(set(selected["entries"]).issubset(client_suites.select_entries()["entries"]))

    def test_four_shards_preserve_full_order_without_gaps_or_duplicates(self):
        full = client_suites.select_entries()["entries"]
        parts = [client_suites.select_entries(shard=f"{index}/4")["entries"]
                 for index in range(1, 5)]
        self.assertEqual([entry for part in parts for entry in part], full)
        self.assertEqual([len(part) for part in parts],
                         [len(full) * index // 4 - len(full) * (index - 1) // 4
                          for index in range(1, 5)])

    def test_invalid_shards_are_rejected(self):
        for value in ("0/4", "5/4", "1/0", "1/-4", "-1/4", "1", "1/4/5",
                      "1.0/4", "one/four", "", "1/4x", " 1/4"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                client_suites.parse_shard(value)

    def test_unknown_and_mixed_selectors_are_rejected(self):
        for options in ({"suite": "missing"}, {"suite": ""},
                        {"suite": "masters", "shard": "1/4"}):
            with self.subTest(options=options), self.assertRaises(ValueError):
                client_suites.select_entries(**options)

    def assert_bad_catalog(self, edit, message):
        with tempfile.TemporaryDirectory() as temp:
            catalog = json.loads(client_suites.CATALOG.read_text())
            edit(catalog["masters"])
            path = Path(temp) / "catalog.json"
            path.write_text(json.dumps(catalog))
            with self.assertRaisesRegex(ValueError, message):
                client_suites.select_entries(suite="masters", catalog=path)

    def test_duplicate_catalog_entries_are_rejected(self):
        self.assert_bad_catalog(lambda group: group["entries"].__setitem__(
            -1, group["entries"][0]), "duplicate")

    def test_missing_catalog_entry_is_rejected(self):
        self.assert_bad_catalog(lambda group: group["entries"].pop(), "exactly 21")

    def test_unregistered_catalog_entry_is_rejected(self):
        self.assert_bad_catalog(lambda group: group["entries"].__setitem__(
            -1, "dev.wildercord.MissingTest"), "unregistered")

    def test_empty_or_invalid_catalog_entries_are_rejected(self):
        for entries in ([], "not-a-list", [None], [""], ["  "]):
            with self.subTest(entries=entries):
                self.assert_bad_catalog(lambda group: group.update(entries=entries), "nonempty")

    def test_invalid_expected_count_is_rejected(self):
        for count in (20, 22, 21.0, True, "21"):
            with self.subTest(count=count):
                self.assert_bad_catalog(lambda group: group.update(expectedCount=count), "exactly")

    def test_missing_and_duplicate_descriptor_entries_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            data = json.loads(client_suites.DESCRIPTOR.read_text())
            entries = data["entrypoints"]["fabric-client-gametest"]
            target = client_suites.select_entries(suite="masters")["entries"][0]
            path = Path(temp) / "descriptor.json"
            entries.remove(target)
            path.write_text(json.dumps(data))
            with self.assertRaisesRegex(ValueError, "unregistered"):
                client_suites.select_entries(suite="masters", descriptor=path)
            entries.extend([target, target])
            path.write_text(json.dumps(data))
            with self.assertRaisesRegex(ValueError, "duplicate"):
                client_suites.select_entries(suite="masters", descriptor=path)


class LauncherTests(unittest.TestCase):
    def test_commands_preserve_harness_and_choose_only_one_selector(self):
        base = ["xvfb-run", "-a", "-s",
                "-screen 0 1280x720x24 +extension GLX +render -noreset",
                "./gradlew", "runClientGameTest", "--no-daemon", "--stacktrace", "--console=plain"]
        self.assertEqual(run_client_ci.launch_command(client_suites.select_entries()), base)
        self.assertEqual(run_client_ci.launch_command(client_suites.select_entries(shard="2/4")),
                         base + ["-PciShard=2", "-PciShards=4"])
        self.assertEqual(run_client_ci.launch_command(client_suites.select_entries(suite="masters")),
                         base + ["-PciSuite=masters"])

    def test_invalid_arguments_fail_before_launch(self):
        for args in (["--suite", "unknown"], ["--suite", "masters", "--shard", "1/4"],
                     ["--suite", "masters", "--check-log", "client.log"],
                     ["--shard", "0/4"], ["--suite", "masters", "--focusedSuite", "anything"]):
            with self.subTest(args=args), patch.object(run_client_ci.subprocess, "Popen") as popen:
                with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as exc:
                    run_client_ci.main(args)
                self.assertEqual(exc.exception.code, 2)
                popen.assert_not_called()

    def test_launcher_records_exact_selection_and_exit(self):
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "masters.log"
            selection = client_suites.select_entries(suite="masters")
            process = Mock(stdout=io.StringIO(client_suites.DESCRIPTOR_PREFIX
                                             + json.dumps(selection) + "\nBUILD SUCCESSFUL\n"))
            process.wait.return_value = 0
            process.poll.return_value = 0
            with patch.object(run_client_ci.sys, "platform", "linux"), \
                    patch.object(run_client_ci.subprocess, "Popen", return_value=process) as popen, \
                    redirect_stdout(io.StringIO()):
                result = run_client_ci.main(["--suite", "masters", "--log", str(log)])
            self.assertEqual(result, 0)
            self.assertEqual(client_suites.selection_issues(log.read_text(), selection), [])
            self.assertEqual(popen.call_args.args[0], run_client_ci.launch_command(selection))
            self.assertTrue(popen.call_args.kwargs["start_new_session"])

    def test_dual_backend_failure_still_stops_only_owned_process(self):
        failures = run_client_ci.BackendFailures()
        self.assertFalse(failures.observe("Failed to create backend OpenGL"))
        self.assertFalse(failures.observe("Failed to create backend OpenGL"))
        self.assertTrue(failures.observe("Failed to create backend Vulkan"))
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "masters.log"
            process = Mock(stdout=io.StringIO(
                "Failed to create backend OpenGL\nFailed to create backend Vulkan\n"))
            process.poll.return_value = 2
            with patch.object(run_client_ci.sys, "platform", "linux"), \
                    patch.object(run_client_ci.subprocess, "Popen", return_value=process), \
                    patch.object(run_client_ci, "stop_owned_group") as stop, \
                    redirect_stdout(io.StringIO()):
                result = run_client_ci.main(["--suite", "masters", "--log", str(log)])
            self.assertEqual(result, 2)
            stop.assert_called_once_with(process)
            self.assertIn(client_suites.EXIT_PREFIX + "2", log.read_text())


class ManifestTests(unittest.TestCase):
    def manifest(self, log_text, *args):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            log, output = root / "client.log", root / "manifest.json"
            if log_text is not None:
                log.write_text(log_text)
            with patch.object(test_manifest, "ROOT", root), redirect_stdout(io.StringIO()):
                status = test_manifest.main(["--log", str(log), "--output", str(output), *args])
            manifest = json.loads(output.read_text())
            self.assertEqual(status, 1 if manifest.get("focusedClientGate") == "unverified" else 0)
            return manifest

    def test_focused_success_is_exactly_twenty_one_and_never_a_full_pass(self):
        selection = client_suites.select_entries(suite="masters")
        manifest = self.manifest(launcher_log(selection), "--suite", "masters")
        self.assertEqual(manifest["selection"], selection)
        self.assertEqual([s["suite"] for s in manifest["clientSuites"]], selection["entries"])
        self.assertEqual(manifest["focusedClientGate"], "passed")
        self.assertEqual(manifest["fullClientGate"], "unverified")
        self.assertEqual(manifest["counts"], {"passed": 21, "skipped": 0, "unverified": 0})
        self.assertEqual(manifest["verificationIssues"], [])
        self.assertIn("full client gate and animation gallery are not established", manifest["basis"])

    def test_missing_or_failed_focused_evidence_cannot_pass(self):
        selection = client_suites.select_entries(suite="masters")
        good = launcher_log(selection)
        for log in (None, "", "BUILD SUCCESSFUL\n", launcher_log(selection, result=1),
                    launcher_log(selection, build="BUILD SUCCESSFUL\nBUILD FAILED"),
                    good.replace(client_suites.DESCRIPTOR_PREFIX + json.dumps(selection) + "\n", ""),
                    good.replace(client_suites.EXIT_PREFIX + "0\n", ""),
                    good + client_suites.EXIT_PREFIX + "0\n"):
            with self.subTest(log=log):
                manifest = self.manifest(log, "--suite", "masters")
                self.assertEqual(manifest["fullClientGate"], "unverified")
                self.assertEqual(manifest["focusedClientGate"], "unverified")
                self.assertEqual(manifest["counts"]["unverified"], 21)

    def test_mismatched_stale_duplicate_or_invalid_selection_cannot_pass(self):
        selection = client_suites.select_entries(suite="masters")
        stale = {**selection, "entries": selection["entries"][:-1]}
        logs = (launcher_log(client_suites.select_entries()), launcher_log(stale),
                launcher_log(selection) + client_suites.SELECTION_PREFIX + json.dumps(selection),
                client_suites.SELECTION_PREFIX + "invalid-json\nBUILD SUCCESSFUL\n"
                + client_suites.EXIT_PREFIX + "0\n")
        for log in logs:
            with self.subTest(log=log):
                manifest = self.manifest(log, "--suite", "masters")
                self.assertEqual(manifest["focusedClientGate"], "unverified")
                self.assertTrue(manifest["verificationIssues"])

    def test_focused_log_cannot_be_relabelled_as_full_or_shard(self):
        log = launcher_log(client_suites.select_entries(suite="masters"))
        for args in ([], ["--shard", "4/4"]):
            with self.subTest(args=args):
                manifest = self.manifest(log, *args)
                self.assertEqual(manifest["fullClientGate"], "unverified")
                self.assertEqual(manifest["counts"]["passed"], 0)

    def test_direct_gradle_focused_log_cannot_be_relabelled_as_full(self):
        selection = client_suites.select_entries(suite="masters")
        log = client_suites.DESCRIPTOR_PREFIX + json.dumps(selection) + "\nBUILD SUCCESSFUL\n"
        manifest = self.manifest(log)
        self.assertEqual(manifest["fullClientGate"], "unverified")
        self.assertEqual(manifest["counts"]["passed"], 0)

    def test_legacy_full_logs_preserve_optional_skip_and_gallery_behavior(self):
        full = self.manifest("BUILD SUCCESSFUL\n")
        self.assertEqual(full["fullClientGate"], "passed")
        self.assertEqual(full["counts"]["skipped"], 4)
        self.assertNotIn("focusedClientGate", full)
        gallery = self.manifest("BUILD SUCCESSFUL\n", "--gallery")
        self.assertEqual(gallery["counts"]["skipped"], 3)
        self.assertEqual(gallery["counts"]["passed"], full["counts"]["passed"] + 1)
        all_optional = self.manifest("BUILD SUCCESSFUL\n", "--gallery", "--shaders",
                                     "--showcase", "--fireblood-shots")
        self.assertEqual(all_optional["counts"]["skipped"], 0)

    def test_all_shard_manifests_keep_existing_scope_and_gallery_behavior(self):
        for index in range(1, 5):
            label = f"{index}/4"
            selection = client_suites.select_entries(shard=label)
            manifest = self.manifest(launcher_log(selection), "--shard", label, "--gallery")
            self.assertEqual(manifest["shard"], label)
            self.assertEqual(manifest["fullClientGate"], "passed")
            self.assertEqual(manifest["selection"], selection)
            self.assertEqual([s["suite"] for s in manifest["clientSuites"]], selection["entries"])
            self.assertIn("only its own contiguous part", manifest["basis"])

    def test_manifest_rejects_unknown_mixed_and_invalid_selectors(self):
        for args in (["--suite", "unknown"], ["--suite", "masters", "--shard", "1/4"],
                     ["--shard", "0/4"], ["--shard", "1/0"], ["--shard", "bad"]):
            with self.subTest(args=args), redirect_stderr(io.StringIO()), \
                    self.assertRaises(SystemExit) as exc:
                self.manifest("BUILD SUCCESSFUL\n", *args)
            self.assertEqual(exc.exception.code, 2)


if __name__ == "__main__":
    unittest.main()
