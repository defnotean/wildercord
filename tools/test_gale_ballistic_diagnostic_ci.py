"""Fixed whole-class Gale physics evidence must never become ordinary-attack acceptance."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import client_suites as suites
import run_native_diagnostic_ci as diagnostic
import test_native_diagnostic_ci as existing


CASE = "gale-vault-ballistic"
SUITE = "diagnostic-gale-vault-ballistic"
ENTRY = "dev.wildercord.aura.world.GaleVaultBallisticTest"


class GaleBallisticDiagnosticTests(unittest.TestCase):
    fixture = existing.EvidenceTests.fixture
    log = existing.EvidenceTests.log
    collect = existing.EvidenceTests.collect
    assert_invalid_evidence_never_passes = existing.EvidenceTests.assert_invalid_evidence_never_passes
    assert_manifest_cannot_relabel_diagnostic = existing.EvidenceTests.assert_manifest_cannot_relabel_diagnostic
    assert_collect_preserves_evidence = existing.EvidenceTests.assert_collect_preserves_evidence

    def test_exact_registered_whole_class_preserves_required_rosters(self):
        self.assertEqual(diagnostic.CASES[CASE], SUITE)
        self.assertEqual(suites.select_entries(suite=SUITE), {
            "kind": "diagnostic", "name": SUITE, "count": 1, "entries": [ENTRY]})
        self.assertEqual(diagnostic.SEEDS[CASE], {ENTRY: None})
        self.assertEqual(suites.select_entries()["entries"].count(ENTRY), 1)
        for name, count in (("masters", 44), ("masters-core", 41), ("masters-march-mechanics", 1),
                            ("masters-march-visuals", 2), ("articulated", 6)):
            selected = suites.select_entries(suite=name)
            self.assertEqual(selected["count"], count)
            self.assertNotIn(ENTRY, selected["entries"])
        for filename, count in (("cast-receipt-native-contract.json", 46), ("crimson-moon-native-contract.json", 1)):
            contract = json.loads((suites.ROOT / "src/gametest/resources" / filename).read_text())
            self.assertEqual(contract["expectedCount"], count)
        self.assertEqual(len(contract["profiles"]) * len(contract["observerAngles"]), 4)
        parts = [suites.select_entries(shard=f"{i}/4")["entries"] for i in range(1, 5)]
        self.assertEqual([entry for part in parts for entry in part], suites.select_entries()["entries"])

    def test_catalog_cannot_choose_a_subset_another_class_or_release_scope(self):
        for update in ({"entries": []}, {"entries": [ENTRY + "#flat"]}, {"entries": [ENTRY, ENTRY], "expectedCount": 2},
                       {"entries": [suites.STONE_OWNER_NEGATIVE_ENTRIES[0]]},
                       {"purpose": "release"}, {"expectedCount": True}, {"expectedCount": 1.0}):
            with self.subTest(update=update), tempfile.TemporaryDirectory() as temp:
                catalog = json.loads(suites.CATALOG.read_text())
                catalog[SUITE].update(update)
                path = Path(temp) / "catalog.json"
                path.write_text(json.dumps(catalog))
                with self.assertRaises(ValueError):
                    suites.select_entries(suite=SUITE, catalog=path)
        with tempfile.TemporaryDirectory() as temp:
            descriptor = json.loads(suites.DESCRIPTOR.read_text())
            descriptor["entrypoints"]["fabric-client-gametest"].remove(ENTRY)
            path = Path(temp) / "descriptor.json"
            path.write_text(json.dumps(descriptor))
            with self.assertRaisesRegex(ValueError, "unregistered entrypoints"):
                suites.select_entries(suite=SUITE, descriptor=path)

    def test_request_cannot_supply_paths_commands_seeds_or_subcases(self):
        request = self.fixture(CASE)["request"]
        for case in (SUITE, ENTRY, CASE + "#flat", "../../" + CASE, CASE + "; true"):
            with self.subTest(case=case), self.assertRaises(ValueError):
                diagnostic.parse_request(json.dumps({**request, "case": case}))
        for key in ("path", "class", "suite", "command", "seed", "env", "retry", "timeout", "shard"):
            with self.subTest(key=key), self.assertRaises(ValueError):
                diagnostic.parse_request(json.dumps({**request, key: "anything"}))

    def test_forged_selection_is_rejected_before_hashing_or_launch(self):
        data = self.fixture(CASE)
        for changes in ({"entries": []}, {"entries": [ENTRY + "#flat"]}, {"kind": "suite"},
                        {"name": "masters"}, {"count": True}, {"count": 1.0}):
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

    def test_all_completion_boundaries_and_terminal_are_required_in_order(self):
        data = self.fixture(CASE)
        good = self.log(data)
        passed = self.collect(data, good)
        self.assertEqual(passed["completedEntries"], [ENTRY])
        self.assertFalse(passed["ordinaryAttack"])
        self.assertFalse(passed["physicsNativePending"])
        self.assertEqual(passed["observedWorldSeeds"], {ENTRY: ["1"]})
        self.assert_invalid_evidence_never_passes(CASE)
        self.assert_manifest_cannot_relabel_diagnostic(CASE)
        lines = good.splitlines()
        for index, line in enumerate(lines):
            if any(prefix in line for prefix in (diagnostic.SCENE_PREFIX, diagnostic.SEED_PREFIX, suites.GALE_BALLISTIC_PREFIX)):
                for bad in ("\n".join(lines[:index] + lines[index + 1:]), good + line + "\n",
                            line + "\n" + "\n".join(lines[:index] + lines[index + 1:])):
                    with self.subTest(marker=line):
                        result = self.collect(data, bad)
                        self.assertEqual(result["diagnosticOutcome"], "unverified")
                        self.assertTrue(result["physicsNativePending"])
                        self.assertFalse(result["ordinaryAttack"])
        for old, new in (("returned", "threw"), ("cleanup", "run"), (ENTRY, "foreign.Class"),
                         ("ordinary_attack=false", "ordinary_attack=true"), ("cleanup=complete", "cleanup=pending"),
                         (suites.GALE_BALLISTIC_PREFIX, "GALE_VAULT_FEASIBILITY ")):
            self.assertEqual(self.collect(data, good.replace(old, new))["diagnosticOutcome"], "unverified")

    def test_malformed_duplicate_fields_and_forged_types_never_pass(self):
        data = self.fixture(CASE)
        good = self.log(data)
        seed = diagnostic.SEED_PREFIX + json.dumps({"suite": ENTRY, "seed": "1"})
        for payload in ("{", "[]", "null", json.dumps({"suite": ENTRY, "seed": True}),
                        json.dumps({"suite": ENTRY, "seed": "1", "override": True}),
                        json.dumps({"suite": ENTRY, "seed": str(2 ** 63)}),
                        json.dumps({"suite": ENTRY, "seed": str(-2 ** 63 - 1)})):
            self.assertEqual(self.collect(data, good.replace(seed, diagnostic.SEED_PREFIX + payload))["diagnosticOutcome"], "unverified")
        for prefix in (suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX, suites.REQUEST_PREFIX,
                       diagnostic.SCENE_PREFIX, diagnostic.SEED_PREFIX):
            line = next(line for line in good.splitlines() if line.startswith(prefix))
            payload = json.loads(line[len(prefix):])
            key = next(iter(payload))
            duplicate = line[:-1] + ", " + json.dumps(key) + ": " + json.dumps(payload[key]) + "}"
            self.assertEqual(self.collect(data, good.replace(line, duplicate))["diagnosticOutcome"], "unverified")
        scene = next(line for line in good.splitlines() if line.startswith(diagnostic.SCENE_PREFIX))
        for value in ("true", "NaN", "Infinity", "-1"):
            bad = good.replace(scene, scene.replace('"elapsedSeconds": 1.0', '"elapsedSeconds": ' + value))
            self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")
        for prefix in (suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX):
            line = prefix + json.dumps(data["selection"])
            for count in ("true", "1.0"):
                self.assertEqual(self.collect(data, good.replace(line, line.replace('"count": 1', '"count": ' + count)))["diagnosticOutcome"], "unverified")

    def test_stale_source_hash_run_config_or_request_never_passes(self):
        data = self.fixture(CASE)
        for section, key, value in (("request", "sourceSha", "d" * 40), ("request", "schemaVersion", True),
                                    ("provenance", "headSha", "d" * 40), ("provenance", "runId", "999"),
                                    ("provenance", "runAttempt", "2"), ("configuration", "ordinaryAttack", True),
                                    ("sourceFilesSha256", "fixture.java", "d" * 64)):
            forged = copy.deepcopy(data)
            forged[section][key] = value
            with self.subTest(key=key):
                self.assertEqual(self.collect(data, self.log(forged))["diagnosticOutcome"], "unverified")
        # Real source/request commits verify current inventory hashes and prepared
        # provenance; later source or modified files cannot launch the old request.
        self.assert_collect_preserves_evidence(CASE)

    def test_launch_receipt_must_match_fixed_command_and_current_invocation(self):
        data = self.fixture(CASE)
        launch = json.loads(existing.fixture_launch_receipt(data))
        wrong_run = copy.deepcopy(launch)
        wrong_run["provenance"]["runAttempt"] = "2"
        for payload in ({}, wrong_run, {**launch, "command": ["sh", "arbitrary.sh"]},
                        {**launch, "extra": True}, {**launch, "startedAt": "2026-10-07"},
                        {**launch, "startedAt": True}):
            with self.subTest(payload=payload), patch.object(existing, "fixture_launch_receipt", return_value=json.dumps(payload)):
                result = self.collect(data, self.log(data))
                self.assertEqual(result["diagnosticOutcome"], "unverified")
                self.assertTrue(result["physicsNativePending"])
        for raw in ("{", "[]", "null", " " * 65537, existing.fixture_launch_receipt(data)[:-1] + ', "command": []}'):
            with patch.object(existing, "fixture_launch_receipt", return_value=raw):
                self.assertEqual(self.collect(data, self.log(data))["diagnosticOutcome"], "unverified")

    def test_source_inventory_and_final_marker_include_cleanup_without_enabling_gameplay(self):
        files = diagnostic.CASE_FILES[CASE]
        for path in files:
            self.assertTrue((suites.ROOT / path).is_file(), path)
        self.assertIn("src/gametest/java/dev/wildercord/gametest/galevault/GaleVaultProbe.java", files)
        config = "src/gametest/resources/gale-vault-ballistic-gametest.mixins.json"
        self.assertIn(config, files)
        mixins = json.loads((suites.ROOT / config).read_text())
        self.assertEqual(len(mixins["mixins"]), 4)
        for name in mixins["mixins"]:
            self.assertIn("src/gametest/java/dev/wildercord/gametest/galevault/mixin/" + name + ".java", files)
        source = (suites.ROOT / "src/gametest/java/dev/wildercord/aura/world/GaleVaultBallisticTest.java").read_text()
        self.assertIn("server.overworld().getSeed()", source)
        self.assertEqual(source.count(suites.GALE_BALLISTIC_PREFIX), 1)
        terminal = source.index(suites.GALE_BALLISTIC_PREFIX + suites.GALE_BALLISTIC_RESULT)
        self.assertGreater(terminal, source.index("} finally { origin = null; GaleVaultProbe.assertIdle(); }"))
        self.assertGreater(terminal, source.index("world.getServer().runOnServer(server -> clean());"))
        with patch.object(diagnostic, "identity", return_value={"headSha": "b" * 40, "observedLiveHeadSha": "b" * 40}), \
                patch.object(diagnostic, "parse_request", return_value=self.fixture(CASE)["request"]), \
                patch.object(diagnostic, "git", side_effect=["b" * 40 + " " + "a" * 40, diagnostic.REQUEST]):
            prepared = diagnostic.current({})
        self.assertTrue(prepared["physicsNativePending"])
        self.assertFalse(prepared["ordinaryAttack"])
        self.assertIn("src/gametest/java/dev/wildercord/aura/world/GaleVaultBallisticTest.java", prepared["sourceFilesSha256"])


if __name__ == "__main__":
    unittest.main()
