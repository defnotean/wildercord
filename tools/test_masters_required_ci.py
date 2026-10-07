"""Adversarial required-part evidence: exact scope, completion, seeds and identity."""
import copy
import hashlib
from contextlib import redirect_stdout, redirect_stderr
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import Mock, patch

import client_suites as suites
import masters_required_ci as gate
import native_ci_diagnostics as diagnostics
import run_client_ci
import test_manifest
import run_native_diagnostic_ci as diagnostic
import test_native_diagnostic_ci as existing


COUNTER_ADDITIONS = ('dev.wildercord.aura.UnmovedNullAcceptanceTest', 'dev.wildercord.aura.arts.ArtWardsHardeningTest')

IDENTITY = {"headSha": "a" * 40, "checkoutSha": "b" * 40, "workflowSha": "b" * 40,
            "runId": "123", "runAttempt": "1", "job": "masters-native"}


def log_for(selection):
    lines = [suites.SELECTION_PREFIX + json.dumps(selection),
             suites.DESCRIPTOR_PREFIX + json.dumps(selection),
             gate.PART_PREFIX + json.dumps(gate.provenance(selection, IDENTITY))]
    if selection["name"] != "masters-core":
        lines += ["WILDERCORD_NATIVE_WORLD " + json.dumps({"suite": entry, "seed": "-123"})
                  for entry in selection["entries"]]
    lines += [diagnostics.SCENE_PREFIX + json.dumps({"suite": entry, "event": event,
              "phase": phase, "elapsedSeconds": 1.0, "sceneElapsedSeconds": 0.5})
              for entry in selection["entries"] for event, phase in
              (("start", "setup"), ("phase", "run"), ("phase", "cleanup"), ("end", "returned"))]
    return "\n".join(lines + ["BUILD SUCCESSFUL", suites.EXIT_PREFIX + "0"]) + "\n"


class RequiredPartTests(unittest.TestCase):
    def manifest(self, part, log=None, *, limit=gate.MAX_LOG_BYTES):
        selection = suites.select_entries(suite=part)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            source, output = root / "run.log", root / "manifest.json"
            source.write_text(log_for(selection) if log is None else log)
            with patch.object(gate, "launch_provenance", return_value=gate.provenance(selection, IDENTITY)), \
                    patch.object(gate, "MAX_LOG_BYTES", limit), patch.object(test_manifest, "ROOT", root), \
                    redirect_stdout(io.StringIO()):
                status = test_manifest.main(["--suite", part, "--log", str(source), "--output", str(output)])
            result = json.loads(output.read_text())
            self.assertEqual(result["fullClientGate"], "unverified")
            self.assertEqual(result["focusedClientGate"], "unverified")
            self.assertEqual(status, 0 if result["requiredPartOutcome"] == "passed" else 1)
            return result

    def aggregate(self, manifests, **kwargs):
        return gate.validate_aggregate(manifests, **{"head_sha": IDENTITY["headSha"], "workflow_sha": IDENTITY["workflowSha"],
            "run_id": IDENTITY["runId"], "run_attempt": IDENTITY["runAttempt"], **kwargs})

    def test_exact_partition_preserves_original_order_and_all_other_rosters(self):
        full = suites.select_entries(suite="masters")["entries"]
        parts = [suites.select_entries(suite=name) for name in suites.MASTERS_PART_NAMES]
        self.assertEqual([p["count"] for p in parts], [44, 1, 2])
        self.assertEqual(parts[0]["entries"], [entry for entry in full if entry not in suites.STONE_MARCH_ENTRIES])
        self.assertEqual(parts[0]["entries"][-1], full[-1])
        self.assertEqual(parts[1]["entries"] + parts[2]["entries"], list(suites.STONE_MARCH_ENTRIES))
        combined = [entry for part in parts for entry in part["entries"]]
        self.assertEqual(len(set(combined)), 47)
        self.assertEqual(set(combined), set(full))
        for selected in parts:
            self.assertEqual(selected["kind"], "required-part")
            self.assertEqual(run_client_ci.launch_command(selected)[-1], "-PciSuite=" + selected["name"])
            self.assertEqual(diagnostics.snapshot_thresholds(selected), (1800, 2700, 4500))
        self.assertEqual(suites.select_entries(suite="articulated")["count"], 6)
        self.assertCountEqual([e for i in range(1, 5) for e in suites.select_entries(shard=f"{i}/4")["entries"]],
                         suites.select_entries()["entries"])

    def test_catalog_rejects_missing_duplicate_reordered_foreign_or_relabelled_parts(self):
        for name in suites.MASTERS_PART_NAMES:
            catalog = json.loads(suites.CATALOG.read_text())
            entries = catalog[name]["entries"]
            edits = ({"entries": entries[:-1]}, {"entries": entries + entries[:1]},
                     {"entries": list(suites.STONE_MARCH_ENTRIES), "expectedCount": 3},
                     {"purpose": "release"}, {"purpose": "diagnostic"}, {"expectedCount": True},
                     {"entries": ["dev.wildercord.cast.RelayCircleTest"] * len(entries)})
            if len(entries) > 1:
                edits += ({"entries": entries[::-1]},)
            for edit in edits:
                with self.subTest(name=name, edit=edit), tempfile.TemporaryDirectory() as temp:
                    changed = copy.deepcopy(catalog); changed[name].update(edit)
                    path = Path(temp) / "catalog.json"; path.write_text(json.dumps(changed))
                    with self.assertRaises(ValueError):
                        suites.select_entries(suite=name, catalog=path)

    def test_counter_addition_preserves_every_prior_class_and_rejects_old_core_evidence(self):
        counter = "dev.wildercord.aura.EarnedCounterAcceptanceTest"
        full = suites.select_entries(suite="masters")
        previous_entries = [entry for entry in full["entries"] if entry not in (counter, "dev.wildercord.aura.MirrorRiposteReleasedOwnerTest", *COUNTER_ADDITIONS)]
        self.assertEqual(len(previous_entries), 43)
        # Golden of the independently reviewed 43-class partition at aeb32089.
        self.assertEqual(hashlib.sha256(json.dumps(previous_entries, separators=(",", ":")).encode()).hexdigest(),
                         "6b2cf7aae202e3839e4e6542fdad1ca184c0b0360b0fdbfa666d46d9c440eb05")
        self.assertEqual(full["entries"].count(counter), 1)
        self.assertEqual(full["entries"].index(counter), full["entries"].index("dev.wildercord.aura.MastersStyleTimelineTest") + 1)
        current = suites.select_entries(suite="masters-core")
        previous = {**current, "entries": [entry for entry in current["entries"] if entry not in (counter, "dev.wildercord.aura.MirrorRiposteReleasedOwnerTest", *COUNTER_ADDITIONS)], "count": 40}
        old_provenance = gate.provenance(current, IDENTITY)
        old_provenance["selectionSha256"] = gate.selection_hash(previous)
        old_provenance["mastersSelectionSha256"] = gate.selection_hash({**full, "entries": previous_entries, "count": 43})
        with patch.object(gate, "provenance", return_value=old_provenance):
            old_log = log_for(previous)
        old_core = self.manifest("masters-core", old_log)
        self.assertEqual(old_core["requiredPartOutcome"], "unverified")
        self.assertEqual(old_core["selection"]["count"], 44)
        self.assertEqual(self.aggregate([old_core, *(self.manifest(name) for name in suites.MASTERS_PART_NAMES[1:])])["focusedClientGate"], "unverified")

    def test_mirror_riposte_preserves_prior_forty_four_and_rejects_old_core_evidence(self):
        added = "dev.wildercord.aura.MirrorRiposteReleasedOwnerTest"
        full = suites.select_entries(suite="masters")
        previous_entries = [entry for entry in full["entries"] if entry != added and entry not in COUNTER_ADDITIONS]
        self.assertEqual(len(previous_entries), 44)
        self.assertEqual(hashlib.sha256(json.dumps(previous_entries, separators=(",", ":")).encode()).hexdigest(),
                         "f70b63005dc0ad64c6c5068d5adc5950c4c0617041bcff595552f31aae0cfccc")
        self.assertEqual(full["entries"].count(added), 1)
        self.assertEqual(full["entries"].index(added), full["entries"].index("dev.wildercord.aura.EarnedCounterAcceptanceTest") + 1)
        current = suites.select_entries(suite="masters-core")
        previous = {**current, "entries": [entry for entry in current["entries"] if entry != added and entry not in COUNTER_ADDITIONS], "count": 41}
        old_provenance = gate.provenance(current, IDENTITY)
        old_provenance["selectionSha256"] = gate.selection_hash(previous)
        old_provenance["mastersSelectionSha256"] = gate.selection_hash({**full, "entries": previous_entries, "count": 44})
        with patch.object(gate, "provenance", return_value=old_provenance):
            old_log = log_for(previous)
        old_core = self.manifest("masters-core", old_log)
        self.assertEqual(old_core["requiredPartOutcome"], "unverified")
        self.assertEqual(old_core["selection"]["count"], 44)
        self.assertEqual(self.aggregate([old_core, *(self.manifest(name) for name in suites.MASTERS_PART_NAMES[1:])])["focusedClientGate"], "unverified")
        descriptor = json.loads(suites.DESCRIPTOR.read_text())
        observer = "counter-hit-capture-gametest.mixins.json"
        self.assertEqual(descriptor["mixins"].count(observer), 1)
        self.assertEqual(descriptor["entrypoints"]["fabric-client-gametest"].count(added), 1)
        config = json.loads((suites.ROOT / "src/gametest/resources" / observer).read_text())
        self.assertEqual(config["mixins"], ["CounterHitCaptureMixin", "CounterRuneCaptureMixin",
                                          "CounterResonanceCaptureMixin", "CounterSpellDamageCaptureMixin", "CounterEffectCaptureMixin",
                                          "CounterAddonCaptureMixin"])
        self.assertNotIn(observer, (suites.ROOT / "src/main/resources/fabric.mod.json").read_text())

    def test_unmoved_null_preserves_frozen_forty_five_and_rejects_stale_core(self):
        full = suites.select_entries(suite="masters")
        prior = [entry for entry in full["entries"] if entry not in COUNTER_ADDITIONS]
        self.assertEqual(len(prior), 45)
        # Exact ordered roster from the separately frozen dcf242d9 checkpoint.
        self.assertEqual(hashlib.sha256(json.dumps(prior, separators=(",", ":")).encode()).hexdigest(),
                         "f32e8f75454d4a9ab57f6da093758f4c48a6ea43205dab20a4c403ec4e1c48da")
        anchor = full["entries"].index("dev.wildercord.aura.MirrorRiposteReleasedOwnerTest") + 1
        self.assertEqual(full["entries"][anchor:anchor + 2], list(COUNTER_ADDITIONS))
        descriptor = json.loads(suites.DESCRIPTOR.read_text())
        previous_full = [entry for entry in descriptor["entrypoints"]["fabric-client-gametest"] if entry not in COUNTER_ADDITIONS]
        self.assertEqual(len(previous_full), 310)
        self.assertEqual(hashlib.sha256(json.dumps(previous_full, separators=(",", ":")).encode()).hexdigest(),
                         "4cbbad85c756e671b0ef2907034ac81288fffa94ceb7990b91eba1f208a26941")
        for entry in COUNTER_ADDITIONS:
            self.assertEqual(full["entries"].count(entry), 1)
            self.assertEqual(descriptor["entrypoints"]["fabric-client-gametest"].count(entry), 1)
        observers = "masters-lifecycle-gametest.mixins.json"
        self.assertEqual(descriptor["mixins"].count(observers), 1)
        config = json.loads((suites.ROOT / "src/gametest/resources" / observers).read_text())
        self.assertEqual(config["mixins"].count("ReleasedOwnerSetterProbeMixin"), 1)
        self.assertNotIn(observers, (suites.ROOT / "src/main/resources/fabric.mod.json").read_text())
        current = suites.select_entries(suite="masters-core")
        for omitted in (COUNTER_ADDITIONS, COUNTER_ADDITIONS[:1], COUNTER_ADDITIONS[1:]):
            with self.subTest(omitted=omitted):
                entries = [entry for entry in current["entries"] if entry not in omitted]
                stale = {**current, "entries": entries, "count": len(entries)}
                old = gate.provenance(current, IDENTITY)
                old["selectionSha256"] = gate.selection_hash(stale)
                old["mastersSelectionSha256"] = gate.selection_hash({**full, "entries": prior, "count": 45})
                with patch.object(gate, "provenance", return_value=old):
                    old_log = log_for(stale)
                old_core = self.manifest("masters-core", old_log)
                self.assertEqual(old_core["requiredPartOutcome"], "unverified")
                self.assertEqual(self.aggregate([old_core, *(self.manifest(name) for name in suites.MASTERS_PART_NAMES[1:])])["focusedClientGate"], "unverified")

    def test_all_three_complete_parts_are_required_to_pass_the_focused_aggregate(self):
        manifests = [self.manifest(name) for name in suites.MASTERS_PART_NAMES]
        result = self.aggregate(manifests[::-1])
        self.assertEqual(result["focusedClientGate"], "passed")
        self.assertEqual(result["fullClientGate"], "unverified")
        self.assertEqual(result["selection"]["count"], 47)
        for missing in range(3):
            for inputs in (manifests[:missing] + manifests[missing + 1:],
                           manifests + [manifests[missing]], [manifests[missing]] * 3):
                self.assertEqual(self.aggregate(inputs)["focusedClientGate"], "unverified")
        for kw in ({"head_sha": "c" * 40}, {"workflow_sha": "d" * 40}, {"run_id": "999"}, {"run_attempt": "2"},
                   {"head_sha": "short"}, {"run_id": 123}, {"run_attempt": "0"}):
            self.assertEqual(self.aggregate(manifests, **kw)["focusedClientGate"], "unverified")

    def test_aggregate_rejects_forged_fields_seeds_incomplete_classes_and_wrong_identities(self):
        base = [self.manifest(name) for name in suites.MASTERS_PART_NAMES]
        for index in range(3):
            for key, value in (("scope", "diagnostic"), ("schemaVersion", True), ("part", "masters"),
                               ("requiredPartOutcome", "unverified"), ("fullClientGate", "passed"),
                               ("focusedClientGate", "passed"), ("verificationIssues", ["failed"]),
                               ("completedEntries", []), ("clientSuites", []),
                               ("counts", {"passed": True, "skipped": 0, "unverified": 0}),
                               ("selection", suites.select_entries(suite="masters"))):
                changed = copy.deepcopy(base); changed[index][key] = value
                with self.subTest(index=index, key=key):
                    self.assertEqual(self.aggregate(changed)["focusedClientGate"], "unverified")
            for key, value in (("headSha", "d" * 40), ("checkoutSha", "d" * 40),
                               ("workflowSha", "d" * 40), ("runId", "124"), ("runAttempt", "2"),
                               ("selectionSha256", "0" * 64), ("mastersSelectionSha256", "0" * 64),
                               ("schemaVersion", 1.0), ("job", "native-diagnostic")):
                changed = copy.deepcopy(base); changed[index]["provenance"][key] = value
                self.assertEqual(self.aggregate(changed)["focusedClientGate"], "unverified")
        for index in (1, 2):
            entry = base[index]["selection"]["entries"][0]
            for value in ({}, {entry: ["1", "2"]}, {entry: [str(2 ** 63)]}, {entry: [True]},
                          {"foreign.Class": ["1"]}):
                changed = copy.deepcopy(base); changed[index]["observedWorldSeeds"] = value
                self.assertEqual(self.aggregate(changed)["focusedClientGate"], "unverified")

    def test_part_rejects_each_missing_duplicate_malformed_or_wrong_scope_marker(self):
        for name in suites.MASTERS_PART_NAMES:
            selection = suites.select_entries(suite=name)
            good = log_for(selection)
            for line in good.splitlines():
                if line.startswith((suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX, gate.PART_PREFIX,
                                    diagnostics.SCENE_PREFIX, "WILDERCORD_NATIVE_WORLD ", suites.EXIT_PREFIX)):
                    for bad in (good.replace(line + "\n", "", 1), good + line + "\n"):
                        with self.subTest(name=name, line=line):
                            self.assertEqual(self.manifest(name, bad)["requiredPartOutcome"], "unverified")
            for prefix in (suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX, gate.PART_PREFIX, diagnostics.SCENE_PREFIX):
                line = next(line for line in good.splitlines() if line.startswith(prefix))
                payload = json.loads(line[len(prefix):]); key = next(iter(payload))
                malformed = line[:-1] + "," + json.dumps(key) + ":" + json.dumps(payload[key]) + "}"
                self.assertEqual(self.manifest(name, good.replace(line, malformed))["requiredPartOutcome"], "unverified")
            for other in (suites.select_entries(suite="masters"), suites.select_entries(suite="stone-fault-march-visuals")):
                stale = good.replace(json.dumps(selection), json.dumps(other))
                self.assertEqual(self.manifest(name, stale)["requiredPartOutcome"], "unverified")
            self.assertEqual(self.manifest(name, good + "BUILD FAILED\n")["requiredPartOutcome"], "unverified")
            self.assertEqual(self.manifest(name, good, limit=20)["requiredPartOutcome"], "unverified")

    def test_part_rejects_wrong_sha_run_attempt_seed_and_completion_types(self):
        part = "masters-march-visuals"
        good = log_for(suites.select_entries(suite=part))
        changes = ((IDENTITY["headSha"], "f" * 40), ('"runId": "123"', '"runId": "999"'),
                   ('"runAttempt": "1"', '"runAttempt": "2"'), ('"count": 2', '"count": 2.0'),
                   ('"seed": "-123"', '"seed": "9223372036854775808"'),
                   ('"seed": "-123"', '"seed": true'), ('"returned"', '"threw"'),
                   ('"elapsedSeconds": 1.0', '"elapsedSeconds": true'),
                   ('"sceneElapsedSeconds": 0.5', '"sceneElapsedSeconds": NaN'))
        for old, new in changes:
            self.assertEqual(self.manifest(part, good.replace(old, new))["requiredPartOutcome"], "unverified")
        other = log_for(suites.select_entries(suite="masters-march-mechanics"))
        self.assertEqual(self.manifest(part, other)["requiredPartOutcome"], "unverified")

    def test_identity_verifies_real_clean_checkout_and_exact_candidate(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root, text=True).strip()
            git("init", "-q"); git("config", "user.email", "test@example.com"); git("config", "user.name", "Test")
            (root / "source.txt").write_text("source"); git("add", "."); git("commit", "-qm", "source")
            sha = git("rev-parse", "HEAD")
            env = {"MASTERS_HEAD_SHA": "a" * 40, "GITHUB_SHA": sha, "GITHUB_RUN_ID": "123",
                   "GITHUB_RUN_ATTEMPT": "1", "GITHUB_JOB": "masters-native"}
            selected = suites.select_entries(suite="masters-march-mechanics")
            with patch.object(gate, "ROOT", root):
                self.assertEqual(gate.launch_provenance(selected, env)["checkoutSha"], sha)
                for key in env:
                    with self.subTest(key=key), self.assertRaises(ValueError):
                        gate.launch_provenance(selected, {**env, key: "wrong"})
                (root / "source.txt").write_text("dirty")
                with self.assertRaises(ValueError): gate.launch_provenance(selected, env)

    def test_bounded_manifest_reader_and_cli_never_accept_duplicate_json_or_paths(self):
        manifests = [self.manifest(name) for name in suites.MASTERS_PART_NAMES]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); paths = []
            for index, manifest in enumerate(manifests):
                path = root / f"part-{index}.json"; path.write_text(json.dumps(manifest)); paths.append(path)
            args = [arg for path in paths for arg in ("--manifest", str(path))]
            args += ["--head-sha", IDENTITY["headSha"], "--workflow-sha", IDENTITY["workflowSha"], "--run-id", "123", "--run-attempt", "1", "--output", str(root / "aggregate.json")]
            with redirect_stdout(io.StringIO()): self.assertEqual(gate.main(args), 0)
            bad = root / "bad.json"
            for raw in ('{"part":"masters-core","part":"masters-core"}', '[]', 'null', '{'):
                bad.write_text(raw)
                with self.assertRaises(ValueError): gate.read_manifest(bad)
            bad.write_text(" " * (gate.MAX_MANIFEST_BYTES + 1))
            with self.assertRaises(ValueError): gate.read_manifest(bad)
            bad.unlink(); bad.symlink_to(paths[0])
            with self.assertRaises(ValueError): gate.read_manifest(bad)
            with redirect_stderr(io.StringIO()), self.assertRaises(SystemExit): gate.main(args[:-4])

    def test_launcher_records_required_provenance_before_native_start_and_refuses_invalid_identity(self):
        selected = suites.select_entries(suite="masters-march-mechanics")
        receipt = gate.provenance(selected, IDENTITY)
        child_lines = [line for line in log_for(selected).splitlines()
                       if not line.startswith((suites.SELECTION_PREFIX, gate.PART_PREFIX, suites.EXIT_PREFIX))]
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "native.log"
            process = Mock(stdout=io.StringIO("\n".join(child_lines) + "\n"))
            process.wait.return_value = 0
            process.poll.return_value = 0
            with patch.object(run_client_ci.sys, "platform", "linux"), \
                    patch.object(gate, "launch_provenance", return_value=receipt), \
                    patch.object(run_client_ci.subprocess, "Popen", return_value=process) as popen, \
                    redirect_stdout(io.StringIO()):
                self.assertEqual(run_client_ci.main(["--suite", selected["name"], "--log", str(log)]), 0)
            lines = log.read_text().splitlines()
            self.assertTrue(lines[0].startswith(suites.SELECTION_PREFIX))
            self.assertEqual(json.loads(lines[1][len(gate.PART_PREFIX):]), receipt)
            self.assertEqual(suites.selection_issues(log.read_text(), selected), [])
            self.assertEqual(popen.call_args.args[0][-1], "-PciSuite=" + selected["name"])
            with patch.object(run_client_ci.sys, "platform", "linux"), \
                    patch.object(gate, "launch_provenance", side_effect=ValueError("wrong candidate")), \
                    patch.object(run_client_ci.subprocess, "Popen") as popen, \
                    redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                run_client_ci.main(["--suite", selected["name"], "--log", str(log)])
            popen.assert_not_called()

    def test_required_part_cannot_be_relabelled_as_whole_focused_full_or_sharded_pass(self):
        for name in suites.MASTERS_PART_NAMES:
            with tempfile.TemporaryDirectory() as temp:
                root = Path(temp); log = root / "run.log"; output = root / "manifest.json"
                log.write_text(log_for(suites.select_entries(suite=name)))
                for selector in ([], ["--suite", "masters"], ["--suite", "articulated"], ["--shard", "4/4"]):
                    with patch.object(test_manifest, "ROOT", root), redirect_stdout(io.StringIO()):
                        test_manifest.main(["--log", str(log), "--output", str(output), *selector])
                    result = json.loads(output.read_text())
                    self.assertEqual(result["fullClientGate"], "unverified")
                    self.assertNotEqual(result.get("focusedClientGate"), "passed")
                    self.assertEqual(result["counts"]["passed"], 0)

    def test_matrix_required_steps_and_all_artifacts_remain_unique_and_bounded(self):
        workflow = (suites.ROOT / ".github/workflows/build.yml").read_text()
        section = workflow.split("\n  masters-native:\n", 1)[1].split("\n  connected-combat-native:\n", 1)[0]
        self.assertIn("      max-parallel: 2\n", section)
        self.assertIn("      fail-fast: false\n", section)
        self.assertNotIn("          ref:", section)
        self.assertIn("      MASTERS_HEAD_SHA: ${{ github.event.pull_request.head.sha || github.sha }}", section)
        run = section.split("      - name: Run mandatory Masters part", 1)[1].split("      - name: Preserve original March", 1)[0]
        self.assertNotIn("continue-on-error", run)
        self.assertIn("        if: always()", run)
        for line in section.splitlines():
            if line.startswith("          name: "):
                self.assertIn("${{ matrix.part }}", line)
        step = section.split("      - name: Preserve original March diagnostics (not visual acceptance)", 1)[1].split("      - name:", 1)[0]
        paths = [line.strip() for line in step.split("          path: |\n")[1].split("          if-no-files-found:")[0].splitlines()]
        self.assertEqual(sum(path.endswith(".png") for path in paths), 9)
        self.assertEqual(sum(path.endswith(".json") for path in paths), 10)
        self.assertIn("        if: always() && hashFiles(", step)
        self.assertIn("          retention-days: 7", step)


class SingleMarchDiagnosticTests(unittest.TestCase):
    fixture = existing.EvidenceTests.fixture
    log = existing.EvidenceTests.log
    collect = existing.EvidenceTests.collect
    assert_collect_preserves_evidence = existing.EvidenceTests.assert_collect_preserves_evidence
    assert_manifest_cannot_relabel_diagnostic = existing.EvidenceTests.assert_manifest_cannot_relabel_diagnostic

    def test_one_whole_class_strict_completion_seeds_provenance_and_old_scope_rejection(self):
        for case in ("stone-fault-march-presentation", "stone-fault-march-opponent"):
            data = self.fixture(case); good = self.log(data)
            self.assertEqual(data["selection"]["count"], 1)
            self.assertEqual(self.collect(data, good)["diagnosticOutcome"], "passed")
            for line in good.splitlines():
                if line.startswith((suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX, suites.REQUEST_PREFIX,
                                    diagnostics.SCENE_PREFIX, diagnostic.SEED_PREFIX)):
                    for bad in (good.replace(line + "\n", "", 1), good + line + "\n"):
                        self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")
                    payload = json.loads(line.split(" ", 1)[1]); key = next(iter(payload))
                    duplicate = line[:-1] + "," + json.dumps(key) + ":" + json.dumps(payload[key]) + "}"
                    self.assertEqual(self.collect(data, good.replace(line, duplicate))["diagnosticOutcome"], "unverified")
            for other in ("stone-fault-march", "stone-fault-march-visuals",
                          "stone-fault-march-opponent" if case.endswith("presentation") else "stone-fault-march-presentation"):
                old = self.fixture(other); old_log = self.log(old)
                forged = old_log.replace(json.dumps(old["selection"]), json.dumps(data["selection"]))
                forged = forged.replace(json.dumps(diagnostic.receipt(old)), json.dumps(diagnostic.receipt(data)))
                for bad in (old_log, forged):
                    self.assertEqual(self.collect(data, bad)["diagnosticOutcome"], "unverified")
            for section, key, value in (("provenance", "headSha", "f" * 40), ("provenance", "runId", "999"),
                                        ("provenance", "runAttempt", "2"), ("sourceFilesSha256", "fixture.java", "d" * 64),
                                        ("request", "schemaVersion", True)):
                changed = copy.deepcopy(data); changed[section][key] = value
                self.assertEqual(self.collect(data, self.log(changed))["diagnosticOutcome"], "unverified")
            for old, new in (('"seed": "1"', '"seed": "9223372036854775808"'),
                             ('"seed": "1"', '"seed": true'), ('"returned"', '"threw"'),
                             ('"count": 1', '"count": true')):
                self.assertEqual(self.collect(data, good.replace(old, new))["diagnosticOutcome"], "unverified")
            self.assert_collect_preserves_evidence(case)
            self.assert_manifest_cannot_relabel_diagnostic(case)

    def test_single_class_preparation_refuses_forged_scope_before_hashing_sources(self):
        for case in ("stone-fault-march-presentation", "stone-fault-march-opponent"):
            data = self.fixture(case)
            for change in ({"kind": "suite"}, {"name": "stone-fault-march-visuals"},
                           {"count": True}, {"entries": list(suites.STONE_MARCH_VISUAL_ENTRIES), "count": 2}):
                with tempfile.TemporaryDirectory() as temp:
                    root = Path(temp); request = root / diagnostic.REQUEST
                    request.parent.mkdir(); request.write_text(json.dumps(data["request"]))
                    with patch.object(diagnostic, "ROOT", root), \
                            patch.object(diagnostic, "identity", return_value={"headSha": "b" * 40, "observedLiveHeadSha": "b" * 40}), \
                            patch.object(diagnostic, "git", side_effect=["b" * 40 + " " + "a" * 40, diagnostic.REQUEST]), \
                            patch.object(diagnostic, "select_entries", return_value={**data["selection"], **change}), \
                            patch.object(diagnostic, "digest") as digest, self.assertRaises(ValueError):
                        diagnostic.current({})
                    digest.assert_not_called()

    def test_single_class_catalog_cannot_be_expanded_relabelled_or_reordered(self):
        for case in ("stone-fault-march-presentation", "stone-fault-march-opponent"):
            catalog = json.loads(suites.CATALOG.read_text())
            for edit in ({"purpose": "release"}, {"purpose": "required-part"},
                         {"entries": list(suites.STONE_MARCH_VISUAL_ENTRIES), "expectedCount": 2},
                         {"entries": list(suites.STONE_MARCH_ENTRIES), "expectedCount": 3},
                         {"entries": ["dev.wildercord.cast.RelayCircleTest"]}, {"expectedCount": True}):
                with tempfile.TemporaryDirectory() as temp:
                    changed = copy.deepcopy(catalog); changed[case].update(edit)
                    path = Path(temp) / "catalog.json"; path.write_text(json.dumps(changed))
                    with self.assertRaises(ValueError): suites.select_entries(suite=case, catalog=path)


if __name__ == "__main__":
    unittest.main()
