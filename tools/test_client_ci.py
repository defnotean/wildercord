"""Stdlib regression checks for native CI selection, launch arguments and scoped evidence.

Run with: python -m unittest discover -s tools -p 'test_client_ci.py' -v
These tests do not launch Minecraft or establish a native gameplay pass.
"""
from contextlib import redirect_stderr, redirect_stdout
import io
import json
from pathlib import Path
import re
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
    def test_masters_update_preserves_prior_forty_five_and_keeps_presentation_last(self):
        selected = client_suites.select_entries(suite="masters")
        full = client_suites.select_entries()
        self.assertEqual(selected["count"], 47)
        self.assertEqual(len(set(selected["entries"])), 47)
        self.assertTrue(set(selected["entries"]).issubset(full["entries"]))
        self.assertEqual(selected["entries"][:35], [
            "dev.wildercord.cast.RelayCircleTest",
            "dev.wildercord.cast.RelayLifetimeTest",
            "dev.wildercord.cast.RelayDefenceTest",
            "dev.wildercord.party.RelayImpactTest",
            "dev.wildercord.cast.ReweavePlayableTest",
            "dev.wildercord.cast.ExcisePlayableTest",
            "dev.wildercord.party.PartyMutationSafetyTest",
            "dev.wildercord.party.ArtFieldMutationSafetyTest",
            "dev.wildercord.gametest.WildercordHeartCirclesTest",
            "dev.wildercord.aura.world.ManaSkinSustainChecks",
            "dev.wildercord.gametest.WildercordMastersArtsTest",
            "dev.wildercord.aura.CrimsonMoonReleasedOwnerTest",
            "dev.wildercord.aura.CrimsonMoonTimelineTest",
            "dev.wildercord.aura.world.MasterOrdinaryPlannerTest",
            "dev.wildercord.aura.world.SwordMasterTrialTest",
            "dev.wildercord.world.upgrade.UpgradeRecoveryTest",
            "dev.wildercord.party.PartyOfflineProjectileTest",
            "dev.wildercord.aura.MastersStyleTimelineTest",
            "dev.wildercord.aura.EarnedCounterAcceptanceTest",
            "dev.wildercord.aura.MirrorRiposteReleasedOwnerTest",
            "dev.wildercord.aura.UnmovedNullAcceptanceTest",
            "dev.wildercord.aura.arts.ArtWardsHardeningTest",
            "dev.wildercord.party.MastersSecondFormTimelineTest",
            "dev.wildercord.aura.HailfallReleasedOwnerTest",
            "dev.wildercord.aura.HailSkyAcceptanceTest",
            "dev.wildercord.party.MastersGroundFieldTimelineTest",
            "dev.wildercord.aura.GroundFieldReleasedOwnerTest",
            "dev.wildercord.aura.world.DuelistMasterAccessTest",
            "dev.wildercord.monster.WildMonsterPressureTest",
            "dev.wildercord.aura.world.MasterVictoriesTest",
            "dev.wildercord.aura.world.MasterAntiAirTest",
            "dev.wildercord.client.auraworld.MasterModelPresentationTest",
            "dev.wildercord.aura.SwordStringAuthorityTest",
            "dev.wildercord.aura.CrescentCoverTest",
            "dev.wildercord.aura.CrescentAudienceTest",
        ])
        self.assertEqual(selected["entries"][35:], [
            "dev.wildercord.aura.WallTurnLessonTest",
            "dev.wildercord.aura.WallTurnSafetyTest",
            "dev.wildercord.aura.WallTurnCommitmentTest",
            "dev.wildercord.aura.world.EmberKilnTest",
            "dev.wildercord.aura.world.EmberKilnPresentationTest",
            "dev.wildercord.aura.world.EmberKilnOpponentViewTest",
            "dev.wildercord.aura.world.StoneMarchTest",
            "dev.wildercord.aura.world.StoneMarchPresentationTest",
            "dev.wildercord.aura.world.StoneMarchOpponentViewTest",
            "dev.wildercord.cast.LifeOwnerCameraTest",
            "dev.wildercord.cast.LifeOutcomeTransportTest",
            "dev.wildercord.gametest.WildercordMastersArtsPresentationTest",
        ])
        self.assertEqual(selected["entries"][-1], "dev.wildercord.gametest.WildercordMastersArtsPresentationTest")

    def test_existing_masters_job_labels_match_expanded_catalog(self):
        workflow = (client_suites.ROOT / ".github/workflows/build.yml").read_text()
        self.assertEqual(workflow.count("\n  masters-native:\n"), 1)
        section = workflow.split("\n  masters-native:\n", 1)[1]
        section = re.split(r"\n  [a-z][a-z-]*:\n", section, maxsplit=1)[0]
        self.assertIn("name: Masters update / ${{ matrix.part }} (required part)", section)
        self.assertIn("part: [masters-core, masters-march-mechanics, masters-march-visuals]", section)
        self.assertIn("      max-parallel: 2", section)
        self.assertIn("      fail-fast: false", section)
        self.assertIn("    timeout-minutes: 90\n", section)
        self.assertIn("      - name: Run mandatory Masters part\n        timeout-minutes: 85\n", section)
        self.assertIn("python tools/run_client_ci.py --suite ${{ matrix.part }} --log masters-native.log", section)
        self.assertIn("python tools/test_manifest.py --suite ${{ matrix.part }} --log masters-native.log", section)
        self.assertNotIn("--suite masters --log", section)

    def test_articulated_is_six_registered_acceptance_slices(self):
        selected = client_suites.select_entries(suite="articulated")
        self.assertEqual(selected["count"], 6)
        self.assertEqual(selected["entries"], [
            "dev.wildercord.client.CombatPresentationSettingsTest",
            "dev.wildercord.client.combat.ArticulatedCombatPresentationTest",
            "dev.wildercord.client.combat.ArticulatedArmorPresentationTest",
            "dev.wildercord.client.combat.ArticulatedFirstPersonCompositionTest",
            "dev.wildercord.client.combat.ArticulatedAuraShellPresentationTest",
            "dev.wildercord.client.combat.CrimsonMoonCaptureTest",
        ])
        self.assertTrue(set(selected["entries"]).issubset(client_suites.select_entries()["entries"]))

    def test_spectator_delivery_coverage_is_mandatory_in_the_genuine_pair(self):
        import hashlib
        from native import launch_two_clients, run_paired_matrix
        self.assertNotIn("dev.wildercord.cast.SpectatorDeliveryTest", client_suites.select_entries()["entries"])
        contract = json.loads((client_suites.ROOT / "src/gametest/resources/cast-receipt-native-contract.json").read_text())
        self.assertEqual(contract["expectedCount"], 54)
        # Frozen presentation50f91d7: every original ordered case, not only the spectator subset.
        self.assertEqual(hashlib.sha256(json.dumps(contract["cases"][:46], separators=(",", ":")).encode()).hexdigest(),
                         "a82c794e514d23cb26c34dddebc0e51390b4931787ca7d8a996967d853ad0ba4")
        self.assertEqual(contract["cases"][38:42], ["SPECTATOR_BOLT_VENOM", "SPECTATOR_SPARK_VENOM",
                                                "SPECTATOR_RAY_VENOM", "SPECTATOR_TOUCH_VENOM"])
        self.assertEqual(contract["cases"][46:], ["COUNTER_" + art + "_" + mode + "_" + hand
                         for art in ("UNMOVED", "NULL_PARRY") for mode in ("CLASSIC", "ARTICULATED") for hand in ("RIGHT", "LEFT")])
        workflow = (client_suites.ROOT / ".github/workflows/build.yml").read_text().split("  connected-combat-native:", 1)[1].split("  articulated-native:", 1)[0]
        self.assertIn("Connected cast receipts (54 cases, 2 widths)", workflow)
        self.assertIn("    timeout-minutes: 60", workflow)
        mandatory = workflow.split("      - name: Preserve both clients", 1)[0]
        self.assertNotIn("continue-on-error", mandatory)
        self.assertIn("run_paired_matrix.py", mandatory)
        self.assertIn("        timeout-minutes: 51", mandatory)
        self.assertIn("--total-timeout 3000", mandatory)
        self.assertEqual(launch_two_clients.MAX_TIMEOUT, 900)
        self.assertEqual(run_paired_matrix.MAX_TOTAL_SECONDS, 3000)
        groups = run_paired_matrix.GROUPS
        self.assertEqual([g["id"] for g in groups], ["cast-receipt-wide", "cast-receipt-slim",
                         "moon-wide-front_oblique", "moon-wide-reverse_oblique", "moon-slim-front_oblique", "moon-slim-reverse_oblique"])
        for group, skin in zip(groups[:2], ("wide", "slim")):
            selected = run_paired_matrix.selected(group)
            self.assertEqual(selected["cases"], contract["cases"])
            self.assertEqual(selected["profile"], "aura-" + skin)
            self.assertEqual(selected["expectedSkin"], skin)
            self.assertEqual(selected["maxTimeoutSeconds"], 900)
        self.assertTrue(all(run_paired_matrix.selected(group)["maxTimeoutSeconds"] == 180 for group in groups[2:]))
        source = (client_suites.ROOT / "tools/native/run_paired_matrix.py").read_text()
        self.assertIn("s.validate_counter_profile_union(counter_paths)", source)
        self.assertIn('all(g["status"] == "passed" for g in report["groups"])', source)
        self.assertIn("failed_group_cleanup(child_options.output, group)", source)
        self.assertNotIn('min(selected(group)["maxTimeoutSeconds"]', source)
        # Actual contract/union validators reject omissions, duplicated cases and a missing required width.
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); path = root / launch_two_clients.CONTRACT; path.parent.mkdir(parents=True)
            for index in (0, 38, 42, 46, 53):
                broken = json.loads(json.dumps(contract)); broken["cases"].pop(index); broken["expectedCount"] -= 1
                path.write_text(json.dumps(broken))
                with self.subTest(missing_case=index), self.assertRaises(ValueError): launch_two_clients.load_contract(root)
            broken = json.loads(json.dumps(contract)); broken["cases"][-1] = broken["cases"][-2]; path.write_text(json.dumps(broken))
            with self.assertRaises(ValueError): launch_two_clients.load_contract(root)
            for profile in ("aura-wide", "aura-slim"):
                broken = json.loads(json.dumps(contract)); del broken["profiles"][profile]; path.write_text(json.dumps(broken))
                with self.subTest(missing_profile=profile), self.assertRaises(ValueError): launch_two_clients.load_contract(root)
        for missing in ([], [Path("missing-wide")], [Path("missing-slim")]):
            with self.assertRaises(ValueError): launch_two_clients.validate_counter_profile_union(missing)

    def test_four_shards_preserve_relative_order_without_gaps_or_duplicates(self):
        full = client_suites.select_entries()["entries"]
        parts = [client_suites.select_entries(shard=f"{index}/4")["entries"]
                 for index in range(1, 5)]
        flattened = [entry for part in parts for entry in part]
        self.assertEqual(len(set(flattened)), len(full))
        self.assertEqual(set(flattened), set(full))
        for part in parts:
            self.assertEqual(part, [entry for entry in full if entry in part])
        self.assertEqual([len(part) for part in parts], [80, 84, 77, 71])

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
        self.assert_bad_catalog(lambda group: group["entries"].pop(), "exactly 47")

    def test_unregistered_catalog_entry_is_rejected(self):
        self.assert_bad_catalog(lambda group: group["entries"].__setitem__(
            -1, "dev.wildercord.MissingTest"), "unregistered")

    def test_empty_or_invalid_catalog_entries_are_rejected(self):
        for entries in ([], "not-a-list", [None], [""], ["  "]):
            with self.subTest(entries=entries):
                self.assert_bad_catalog(lambda group: group.update(entries=entries), "nonempty")

    def test_invalid_expected_count_is_rejected(self):
        for count in (27, 30, 33, 34, 35, 36, 37, 38, 39, 40.0, True, "40"):
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
    def test_moon_launch_nonce_is_fresh_scoped_and_never_changes_parent_environment(self):
        selection = client_suites.select_entries(suite="articulated")
        env = {run_client_ci.MOON_NONCE: "old", "GITHUB_SHA": "a" * 40,
               "GITHUB_RUN_ID": "42", "GITHUB_RUN_ATTEMPT": "1", "UNRELATED": "kept"}
        child, receipt = run_client_ci.moon_launch_environment(selection, env)
        second, _ = run_client_ci.moon_launch_environment(selection, env)
        self.assertNotEqual(child[run_client_ci.MOON_NONCE], second[run_client_ci.MOON_NONCE])
        self.assertEqual(env[run_client_ci.MOON_NONCE], "old")
        self.assertEqual(child["UNRELATED"], "kept")
        self.assertEqual(receipt["nonce"], child[run_client_ci.MOON_NONCE])
        self.assertEqual(receipt["selection"], selection)
        self.assertEqual(receipt["checkoutSha"], "a" * 40)
        self.assertFalse(receipt["remoteObserverCoverage"])
        self.assertFalse(receipt["serverReleaseFrameCorrespondenceVerified"])
        ordinary = client_suites.select_entries(suite="masters")
        self.assertEqual(run_client_ci.moon_launch_environment(ordinary, env), (None, None))
        for shard in range(1, 5):
            part = client_suites.select_entries(shard=f"{shard}/4")
            _, found = run_client_ci.moon_launch_environment(part, env)
            self.assertEqual(found is not None, run_client_ci.MOON_ENTRY in part["entries"])
        with self.assertRaises(ValueError):
            run_client_ci.moon_launch_environment(selection, env, lambda: "not-a-uuid")

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
            self.assertEqual(status, 1 if manifest["counts"]["unverified"] else 0)
            return manifest

    def test_focused_success_is_exactly_forty_four_and_never_a_full_pass(self):
        selection = client_suites.select_entries(suite="masters")
        manifest = self.manifest(launcher_log(selection), "--suite", "masters")
        self.assertEqual(manifest["selection"], selection)
        self.assertEqual([s["suite"] for s in manifest["clientSuites"]], selection["entries"])
        self.assertEqual(manifest["focusedClientGate"], "passed")
        self.assertEqual(manifest["fullClientGate"], "unverified")
        self.assertEqual(manifest["counts"], {"passed": 47, "skipped": 0, "unverified": 0})
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
                self.assertEqual(manifest["counts"]["unverified"], 47)

    def test_prior_twenty_seven_through_forty_four_evidence_cannot_pass_expanded_gate(self):
        selection = client_suites.select_entries(suite="masters")
        # Preserve the actual old order: presentation preceded the three appended Wall suites.
        moon = {"dev.wildercord.aura.CrimsonMoonReleasedOwnerTest", "dev.wildercord.aura.CrimsonMoonTimelineTest"}
        life = {"dev.wildercord.cast.LifeOwnerCameraTest", "dev.wildercord.cast.LifeOutcomeTransportTest"}
        prior_forty_five = [entry for entry in selection["entries"] if entry not in ('dev.wildercord.aura.UnmovedNullAcceptanceTest', 'dev.wildercord.aura.arts.ArtWardsHardeningTest')]
        self.assertEqual(len(prior_forty_five), 45)
        prior_forty_four = [entry for entry in prior_forty_five if entry != "dev.wildercord.aura.MirrorRiposteReleasedOwnerTest"]
        self.assertEqual(len(prior_forty_four), 44)
        prior_forty_three = [entry for entry in prior_forty_four if entry != "dev.wildercord.aura.EarnedCounterAcceptanceTest"]
        self.assertEqual(len(prior_forty_three), 43)
        prior_forty = [entry for entry in prior_forty_three if entry not in client_suites.STONE_MARCH_ENTRIES]
        self.assertEqual(len(prior_forty), 40)
        prior_thirty_nine = [entry for entry in prior_forty if entry != "dev.wildercord.cast.ExcisePlayableTest"]
        self.assertEqual(len(prior_thirty_nine), 39)
        prior_thirty_eight = [entry for entry in prior_thirty_nine if entry != "dev.wildercord.cast.ReweavePlayableTest"]
        self.assertEqual(len(prior_thirty_eight), 38)
        prior_thirty_six = [entry for entry in prior_thirty_eight if entry not in life]
        self.assertEqual(len(prior_thirty_six), 36)
        prior_thirty_five = [entry for entry in prior_thirty_six if entry != "dev.wildercord.aura.world.MasterOrdinaryPlannerTest"]
        prior_thirty_three = [entry for entry in prior_thirty_five if entry not in moon]
        prior_twenty_seven = prior_thirty_three[:26] + [prior_thirty_three[-1]]
        prior_thirty = prior_twenty_seven + prior_thirty_three[26:29]
        partial_life = [[entry for entry in selection["entries"] if entry != omitted] for omitted in life]
        for entries in (prior_forty_five, prior_twenty_seven, prior_thirty, prior_thirty_three, prior_thirty_five, prior_thirty_six, prior_thirty_eight, prior_thirty_nine, prior_forty, prior_forty_three, prior_forty_four, *partial_life):
            with self.subTest(previous_count=len(entries)):
                previous = {**selection, "count": len(entries), "entries": entries}
                manifest = self.manifest(launcher_log(previous), "--suite", "masters")
                self.assertEqual(manifest["selection"], selection)
                self.assertEqual(manifest["focusedClientGate"], "unverified")
                self.assertEqual(manifest["fullClientGate"], "unverified")
                self.assertEqual(manifest["counts"], {"passed": 0, "skipped": 0, "unverified": 47})
                self.assertEqual(len(manifest["verificationIssues"]), 2)

    def test_prior_five_articulated_suites_cannot_pass_the_six_suite_gate(self):
        selection = client_suites.select_entries(suite="articulated")
        previous = {**selection, "count": 5, "entries": selection["entries"][:-1]}
        manifest = self.manifest(launcher_log(previous), "--suite", "articulated")
        self.assertEqual(manifest["focusedClientGate"], "unverified")
        self.assertEqual(manifest["fullClientGate"], "unverified")
        self.assertEqual(manifest["counts"], {"passed": 0, "skipped": 0, "unverified": 6})
        self.assertTrue(manifest["verificationIssues"])

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

    def test_legacy_full_logs_never_establish_whole_client_acceptance(self):
        for flags in ([], ["--gallery"], ["--gallery", "--shaders", "--showcase", "--fireblood-shots"]):
            manifest = self.manifest("BUILD SUCCESSFUL\n", *flags)
            self.assertEqual(manifest["fullClientGate"], "unverified")
            self.assertEqual(manifest["fullRunOutcome"], "unverified")
            self.assertEqual(manifest["counts"]["passed"], 0)
            self.assertNotIn("focusedClientGate", manifest)

    def test_single_shard_never_claims_full_client_acceptance(self):
        for index in range(1, 5):
            label = f"{index}/4"
            selection = client_suites.select_entries(shard=label)
            manifest = self.manifest(launcher_log(selection), "--shard", label, "--gallery")
            self.assertEqual(manifest["shard"], label)
            self.assertEqual(manifest["fullClientGate"], "unverified")
            self.assertEqual(manifest["shardOutcome"], "unverified")
            self.assertEqual(manifest["selection"], selection)
            self.assertIn("only its own explicit group", manifest["basis"])

    def test_manifest_rejects_unknown_mixed_and_invalid_selectors(self):
        for args in (["--suite", "unknown"], ["--suite", "masters", "--shard", "1/4"],
                     ["--shard", "0/4"], ["--shard", "1/0"], ["--shard", "bad"]):
            with self.subTest(args=args), redirect_stderr(io.StringIO()), \
                    self.assertRaises(SystemExit) as exc:
                self.manifest("BUILD SUCCESSFUL\n", *args)
            self.assertEqual(exc.exception.code, 2)


if __name__ == "__main__":
    unittest.main()
