"""Strict full-client proof fixtures. These tests do not run Minecraft."""
import copy
from contextlib import redirect_stdout
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import uuid
from unittest.mock import Mock, patch

import client_suites as suites
import full_client_ci as gate
from native_ci_diagnostics import SCENE_PREFIX
import run_client_ci
import test_manifest

IDENTITY = {"headSha": "a" * 40, "checkoutSha": "b" * 40, "workflowSha": "b" * 40,
            "runId": "123", "runAttempt": "1", "job": "game-tests"}
PROFILE = gate.REQUIRED_PROFILE


def log_for(selection, *, profile=None, identity=None, launch=None):
    profile = PROFILE if profile is None else profile
    launch = str(uuid.uuid4()) if launch is None else launch
    proof = gate.provenance(selection, IDENTITY if identity is None else identity, profile, launch)
    lines = [suites.SELECTION_PREFIX + json.dumps(selection), gate.PROVENANCE_PREFIX + json.dumps(proof),
             suites.DESCRIPTOR_PREFIX + json.dumps(selection),
             gate.PROFILE_PREFIX + json.dumps({"launchId": launch, "profile": profile})]
    for entry in selection["entries"]:
        for event, phase in (("start", "setup"), ("phase", "run"), ("phase", "cleanup"), ("end", "returned")):
            lines.append(SCENE_PREFIX + json.dumps({"suite": entry, "event": event, "phase": phase, "launchId": launch,
                         "elapsedSeconds": 1, "sceneElapsedSeconds": 0.5}))
            if phase == "run" and entry in gate.OPTIONAL and not profile[gate.OPTIONAL[entry][0]]:
                lines.append("[00:00:00] [INFO] [TEST SKIP] " + entry.rsplit(".", 1)[-1] + ": " + gate.OPTIONAL[entry][1])
    return "\n".join(lines + ["BUILD SUCCESSFUL in 1h 2m", suites.EXIT_PREFIX + "0",
        gate.OWNED_EXIT_PREFIX + json.dumps({"launchId": launch, "exitCode": 0})]) + "\n"


class FullClientTests(unittest.TestCase):
    def manifest(self, selection, log=None, *, profile=None):
        profile = PROFILE if profile is None else profile
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); source = root / "native.log"; output = root / "manifest.json"
            source.write_text(log_for(selection, profile=profile) if log is None else log)
            args = ["--log", str(source), "--output", str(output)]
            if selection["kind"] == "shard":
                args += ["--shard", selection["shard"]]
            for flag, key in (("--gallery", "animationGallery"), ("--shaders", "shaders"),
                              ("--showcase", "showcase"), ("--fireblood-shots", "firebloodShots")):
                if profile[key]:
                    args.append(flag)
            with patch.object(gate, "current_identity", return_value=IDENTITY), \
                    patch.object(test_manifest, "ROOT", root), redirect_stdout(io.StringIO()):
                status = test_manifest.main(args)
            result = json.loads(output.read_text())
            outcome = result.get("shardOutcome", result.get("fullRunOutcome"))
            self.assertEqual(status, 0 if outcome == "passed" else 1)
            self.assertEqual(result["fullClientGate"], "unverified")
            self.assertNotIn("focusedClientGate", result)
            return result

    def aggregate(self, manifests, **changes):
        return gate.validate_aggregate(manifests, **{"head_sha": IDENTITY["headSha"],
            "workflow_sha": IDENTITY["workflowSha"], "run_id": IDENTITY["runId"],
            "run_attempt": IDENTITY["runAttempt"], **changes})

    def fixtures(self):
        return [self.manifest(suites.select_entries(shard=shard)) for shard in gate.SHARDS]

    def test_exact_four_shards_alone_accept_full_gate_and_preserve_optional_skips(self):
        manifests = self.fixtures()
        for manifest in manifests:
            self.assertEqual(manifest["shardOutcome"], "passed")
            self.assertEqual(manifest["verificationIssues"], [])
        result = self.aggregate(manifests)
        self.assertEqual(result["fullClientGate"], "passed", result["verificationIssues"])
        self.assertEqual(sum(x["status"] == "skipped" for x in result["clientSuites"]), 3)
        self.assertEqual([suites.select_entries(shard=s)["entries"] for s in gate.SHARDS],
                         [m["completedEntries"] for m in manifests])
        self.assertEqual(self.aggregate(manifests[::-1])["fullClientGate"], "passed")
        full = self.manifest(suites.select_entries())
        self.assertEqual(full["fullRunOutcome"], "passed")
        self.assertEqual(self.aggregate([full])["fullClientGate"], "unverified")

    def test_legacy_success_and_partial_stale_logs_fail_closed(self):
        selection = suites.select_entries(shard="1/4")
        for log in ("", "BUILD SUCCESSFUL\n", "BUILD SUCCESSFUL\n" + suites.EXIT_PREFIX + "0\n",
                    log_for(suites.select_entries(shard="2/4"))):
            with self.subTest(log=log[:60]):
                result = self.manifest(selection, log)
                self.assertEqual(result["shardOutcome"], "unverified")
                self.assertTrue(result["verificationIssues"])
        good = log_for(selection)
        stale = copy.deepcopy(selection); stale["entries"] = stale["entries"][:-1]; stale["count"] -= 1
        self.assertEqual(self.manifest(selection, good.replace(json.dumps(selection), json.dumps(stale)))["shardOutcome"], "unverified")

    def test_missing_duplicate_malformed_reordered_or_thrown_lifecycle_rejected(self):
        selection = suites.select_entries(shard="1/4"); good = log_for(selection)
        lines = good.splitlines()
        for prefix in (suites.SELECTION_PREFIX, gate.PROVENANCE_PREFIX, suites.DESCRIPTOR_PREFIX,
                       gate.PROFILE_PREFIX, SCENE_PREFIX, suites.EXIT_PREFIX, gate.OWNED_EXIT_PREFIX, "BUILD SUCCESSFUL"):
            line = next(line for line in lines if line.startswith(prefix))
            edits = (good.replace(line + "\n", "", 1), good + line + "\n",
                     good.replace(line, prefix + "invalid-json", 1))
            for log in edits:
                with self.subTest(prefix=prefix, log=log[-50:]):
                    self.assertEqual(self.manifest(selection, log)["shardOutcome"], "unverified")
        for old, new in (("\"returned\"", "\"thrown\""), ("\"cleanup\"", "\"cancelled\""),
                         ("\"setup\"", "\"skipped\""), ('"elapsedSeconds": 1', '"elapsedSeconds": true'),
                         ('"elapsedSeconds": 1', '"elapsedSeconds": NaN'),
                         ('"phase": "run"', '"phase": "run", "phase": "run"')):
            self.assertEqual(self.manifest(selection, good.replace(old, new, 1))["shardOutcome"], "unverified")
        # A complete lifecycle copied after process exit is not a completed owned launch.
        scenes = [line for line in lines if line.startswith(SCENE_PREFIX)]
        moved = [line for line in lines if not line.startswith(SCENE_PREFIX)] + scenes
        self.assertEqual(self.manifest(selection, "\n".join(moved))["shardOutcome"], "unverified")
        self.assertEqual(self.manifest(selection, good + "BUILD FAILED\n")["shardOutcome"], "unverified")

    def test_mixed_identity_profiles_roster_and_borrowed_scope_fail(self):
        selection = suites.select_entries(shard="1/4"); good = log_for(selection)
        for key in ("headSha", "checkoutSha", "workflowSha", "runId", "runAttempt", "job", "selectionSha256", "fullSelectionSha256", "launchId"):
            lines = good.splitlines()
            i = next(i for i, line in enumerate(lines) if line.startswith(gate.PROVENANCE_PREFIX))
            proof = json.loads(lines[i][len(gate.PROVENANCE_PREFIX):]); proof[key] = "foreign"
            lines[i] = gate.PROVENANCE_PREFIX + json.dumps(proof)
            self.assertEqual(self.manifest(selection, "\n".join(lines))["shardOutcome"], "unverified", key)
        for prefix in ("WILDERCORD_NATIVE_REQUEST ", "WILDERCORD_MASTERS_PART "):
            self.assertEqual(self.manifest(selection, good + prefix + "{}\n")["shardOutcome"], "unverified")
        other_profile = {**PROFILE, "animationGallery": False}
        self.assertEqual(self.manifest(selection, log_for(selection, profile=other_profile))["shardOutcome"], "unverified")
        profile = {**PROFILE, "animationGallery": False}
        full = self.manifest(suites.select_entries(), profile=profile)
        self.assertEqual(full["counts"]["skipped"], 4)

    def test_only_documented_owned_optional_skips_are_accepted(self):
        for shard in gate.SHARDS:
            selection = suites.select_entries(shard=shard); good = log_for(selection)
            for log in (good + "[TEST SKIP] arbitrary\n", good.replace('"phase": "run"', '"phase": "cancelled"', 1)):
                self.assertEqual(self.manifest(selection, log)["shardOutcome"], "unverified")
            skips = [line for line in good.splitlines() if "[TEST SKIP]" in line]
            for skip in skips:
                for log in (good.replace(skip, ""), good.replace(skip, skip + "\n" + skip),
                            good.replace(skip, skip.replace(": ", ": wrong ", 1)),
                            good.replace(skip, "") + skip + "\n"):
                    self.assertEqual(self.manifest(selection, log)["shardOutcome"], "unverified")

    def test_malformed_marker_envelopes_and_scene_values_fail_without_crashing(self):
        selection = suites.select_entries(shard="1/4"); good = log_for(selection)
        for prefix in (suites.SELECTION_PREFIX, gate.PROVENANCE_PREFIX, suites.DESCRIPTOR_PREFIX,
                       gate.PROFILE_PREFIX, suites.EXIT_PREFIX, gate.OWNED_EXIT_PREFIX):
            for extra in (prefix.rstrip() + "{}", "embedded " + prefix + "{}"):
                self.assertEqual(self.manifest(selection, good + extra + "\n")["shardOutcome"], "unverified")
        scene = next(line for line in good.splitlines() if line.startswith(SCENE_PREFIX))
        for malformed in ("null", "[]", "{}", '{"suite":[],"phase":"run"}', '{"suite":{},"phase":"run"}'):
            self.assertEqual(self.manifest(selection, good.replace(scene, SCENE_PREFIX + malformed, 1))["shardOutcome"], "unverified")

    def test_strict_native_lifecycle_envelopes_and_nonce_bindings(self):
        manifests = self.fixtures()
        for shard in gate.SHARDS:
            selection = suites.select_entries(shard=shard); good = log_for(selection)
            native = "\n".join("[07:01:02] [Test thread/INFO] (Minecraft) [STDOUT]: " + line
                if line.startswith(SCENE_PREFIX) else line for line in good.splitlines())
            self.assertEqual(self.manifest(selection, native)["shardOutcome"], "passed")
            edits = [good + SCENE_PREFIX.rstrip() + "{}\n", good + SCENE_PREFIX.rstrip() + "\n"]
            for wrapper in ("HISTORICAL LOG COPY: ", "[07:01:02] [Other thread/INFO] (Minecraft) [STDOUT]: ",
                            "[07:01:02] [Test thread/ERROR] (Minecraft) [STDOUT]: ",
                            "2026-10-07T07:01:02Z ", " "):
                edits.append("\n".join(wrapper + line if line.startswith(SCENE_PREFIX) else line
                                       for line in good.splitlines()))
            lines = good.splitlines()
            for key, value in (("launchId", None), ("launchId", str(uuid.uuid4()))):
                changed = []
                for line in lines:
                    if line.startswith(SCENE_PREFIX):
                        marker = json.loads(line[len(SCENE_PREFIX):])
                        if value is None: marker.pop(key)
                        else: marker[key] = value
                        line = SCENE_PREFIX + json.dumps(marker)
                    changed.append(line)
                edits.append("\n".join(changed))
            for log in edits:
                with self.subTest(shard=shard, suffix=log[-60:]):
                    bad = self.manifest(selection, log)
                    self.assertEqual(bad["shardOutcome"], "unverified")
                    combined = [bad if m["shard"] == shard else m for m in manifests]
                    self.assertEqual(self.aggregate(combined)["fullClientGate"], "unverified")

    def test_foreign_launch_head_run_lifecycle_replay_cannot_pass_aggregate(self):
        fixtures = self.fixtures()
        # Includes the shard containing optional skips, which must own nonce-matched run scopes.
        for shard in gate.SHARDS:
            selection = suites.select_entries(shard=shard); current = log_for(selection)
            for change in ({}, {"headSha": "c" * 40}, {"runId": "456"},
                           {"checkoutSha": "c" * 40, "workflowSha": "c" * 40}):
                foreign = log_for(selection, identity={**IDENTITY, **change})
                foreign_lines = iter(line for line in foreign.splitlines() if SCENE_PREFIX in line or "[TEST SKIP]" in line)
                replayed = "\n".join(next(foreign_lines) if SCENE_PREFIX in line or "[TEST SKIP]" in line else line
                                       for line in current.splitlines())
                bad = self.manifest(selection, replayed)
                self.assertEqual(bad["shardOutcome"], "unverified")
                self.assertEqual(self.aggregate([bad if m["shard"] == shard else m for m in fixtures])["fullClientGate"], "unverified")
        trace = (suites.ROOT / "src/gametest/java/dev/wildercord/gametest/mixin/NativeSceneTraceMixin.java").read_text()
        self.assertIn('String launchId = System.getenv("WILDERCORD_FULL_CLIENT_LAUNCH_ID");', trace)
        self.assertIn('if (launchId != null) marker.addProperty("launchId", launchId);', trace)

    def test_focused_completion_keeps_plain_and_native_envelopes_but_rejects_quoted_or_truncated_markers(self):
        entry = suites.STONE_MARCH_VISUAL_ENTRIES[0]
        plain = "\n".join(SCENE_PREFIX + json.dumps({"suite": entry, "event": event, "phase": phase,
            "elapsedSeconds": 1, "sceneElapsedSeconds": 0.5}) for event, phase in
            (("start", "setup"), ("phase", "run"), ("phase", "cleanup"), ("end", "returned")))
        for prefix in ("", "[07:01:02] [Test thread/INFO] (Minecraft) [STDOUT]: "):
            log = "\n".join(prefix + line for line in plain.splitlines())
            self.assertEqual(suites._whole_class_completion(log, [entry], "Diagnostic")[1], [])
        for log in (plain + "\n" + SCENE_PREFIX.rstrip(), plain + "\n" + SCENE_PREFIX.rstrip() + "{}",
                    "\n".join("HISTORICAL LOG COPY: " + line for line in plain.splitlines())):
            self.assertTrue(suites._whole_class_completion(log, [entry], "Diagnostic")[1])

    def test_complete_profile_rejects_every_narrowing_selector(self):
        for name in ("SHOWCASE", "TOUR_ONLY", "CORDS_ONLY", "MECHANICS_ONLY", "ARTS", "TECHNIQUES", "WAYS",
                     "ANIMATION_GALLERY_BATCHES", "FIREBLOOD_ONLY", "SHOWCASE_ONLY", "FLIGHT_VISUALS_ONLY", "UNKNOWN"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                gate.execution_profile({"WILDERCORD_" + name: ""})
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "GRADLE_OPTS", "JAVA_OPTS"):
            with self.subTest(key=key), self.assertRaises(ValueError):
                gate.execution_profile({key: "-Dunsafe=true"})
        self.assertEqual(gate.execution_profile({"WILDERCORD_ANIMATION_GALLERY": "1"}), PROFILE)
        self.assertFalse(gate.execution_profile({"WILDERCORD_ANIMATION_GALLERY": "true"})["animationGallery"])

    def test_all_aggregate_adversaries_remain_unverified(self):
        good = self.fixtures()
        for manifests in ([], good[:1], good[:-1], good + good[:1], [good[0]] * 4):
            self.assertEqual(self.aggregate(manifests)["fullClientGate"], "unverified")
        for key, value in (("scope", "focused"), ("shard", "1/3"), ("shardOutcome", "cancelled"),
                           ("fullClientGate", "passed"), ("completedEntries", []), ("clientSuites", []),
                           ("counts", {"passed": 1, "skipped": 0, "unverified": 0}), ("verificationIssues", ["failed"]),
                           ("schemaVersion", True), ("schemaVersion", 1), ("lifecycleLaunchId", None),
                           ("lifecycleLaunchId", str(uuid.uuid4())), ("diagnosticOutcome", "passed")):
            bad = copy.deepcopy(good); bad[0][key] = value
            self.assertEqual(self.aggregate(bad)["fullClientGate"], "unverified", key)
        for key in ("headSha", "checkoutSha", "workflowSha", "runId", "runAttempt", "job", "fullSelectionSha256", "selectionSha256"):
            bad = copy.deepcopy(good); bad[1]["provenance"][key] = "foreign"
            self.assertEqual(self.aggregate(bad)["fullClientGate"], "unverified", key)
        for key, value in (("entries", good[0]["selection"]["entries"]), ("count", 0)):
            bad = copy.deepcopy(good); bad[1]["selection"][key] = value
            self.assertEqual(self.aggregate(bad)["fullClientGate"], "unverified")
        for result in ("cancelled", "skipped", "failure", ""):
            self.assertEqual(self.aggregate(good, shards_result=result)["fullClientGate"], "unverified")
            self.assertEqual(self.aggregate(good, download_result=result)["fullClientGate"], "unverified")
        bad = copy.deepcopy(good); bad[1]["provenance"]["launchId"] = good[0]["provenance"]["launchId"]
        self.assertEqual(self.aggregate(bad)["fullClientGate"], "unverified")
        for field in ("profile",):
            bad = copy.deepcopy(good); bad[1][field]["animationGallery"] = False
            self.assertEqual(self.aggregate(bad)["fullClientGate"], "unverified")

    def test_bounded_regular_readers_reject_duplicates_symlinks_fifos_and_invalid_json(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); path = root / "manifest.json"; path.write_text('{"x":1,"x":2}')
            with self.assertRaises(ValueError): gate.read_manifest(path)
            path.write_text("{}")
            link = root / "link"; link.symlink_to(path)
            fifo = root / "fifo"; os.mkfifo(fifo)
            for source in (link, fifo, root):
                with self.assertRaises((ValueError, OSError)): gate.read_evidence(source, 1024)
            with self.assertRaises(ValueError): gate.read_evidence(path, 1)
            path.write_bytes(b'\xff')
            with self.assertRaises(ValueError): gate.read_evidence(path, 1024)

    def test_aggregate_cli_writes_failure_for_missing_files_and_download_failures(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); out = root / "out.json"
            args = ["--artifact-root", str(root / "missing"), "--head-sha", IDENTITY["headSha"],
                "--workflow-sha", IDENTITY["workflowSha"], "--run-id", "123", "--run-attempt", "1",
                "--shards-result", "failure", "--download-result", "failure", "--output", str(out)]
            with redirect_stdout(io.StringIO()): self.assertEqual(gate.main(args), 1)
            self.assertEqual(json.loads(out.read_text())["fullClientGate"], "unverified")

    def test_launcher_binds_owned_exit_and_propagates_actual_profile(self):
        selection = suites.select_entries(shard="1/4")
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "native.log"
            process = Mock(stdout=io.StringIO("BUILD SUCCESSFUL\n")); process.wait.return_value = 0; process.poll.return_value = 0
            with patch.dict(os.environ, {"WILDERCORD_ANIMATION_GALLERY": "1"}, clear=True), \
                    patch.object(gate, "current_identity", return_value=IDENTITY), \
                    patch.object(run_client_ci.subprocess, "Popen", return_value=process) as popen, \
                    patch.object(run_client_ci, "NativeDiagnostics") as diag, redirect_stdout(io.StringIO()):
                self.assertEqual(run_client_ci.main(["--shard", "1/4", "--log", str(path)]), 0)
            log = path.read_text()
            proof = json.loads(next(l[len(gate.PROVENANCE_PREFIX):] for l in log.splitlines() if l.startswith(gate.PROVENANCE_PREFIX)))
            self.assertEqual(popen.call_args.kwargs["env"][gate.LAUNCH_ENV], proof["launchId"])
            self.assertEqual(proof["profile"], PROFILE)
            self.assertTrue(popen.call_args.kwargs["start_new_session"])
            self.assertIn(gate.OWNED_EXIT_PREFIX + json.dumps({"launchId": proof["launchId"], "exitCode": 0}, sort_keys=True), log)
            # Launcher exit success alone remains insufficient without native lifecycle proof.
            self.assertTrue(gate.run_evidence(log, selection, PROFILE, identity=IDENTITY)[1])

    def test_python_gradle_contract_parity_and_stale_processed_descriptor(self):
        home = Path(os.environ.get("GRADLE_HOME", ""))
        java_home = Path(os.environ.get("JAVA_HOME", ""))
        candidates = [home, *java_home.parent.glob("gradle-*")]
        candidates += list((Path.home() / ".gradle/wrapper/dists").glob("gradle-*/*/gradle-*"))
        groovy = next((path / "lib" for path in candidates if list((path / "lib").glob("groovy-*.jar"))), None)
        if groovy is None:
            self.skipTest("A Gradle distribution's Groovy runtime is required for executable parity checks")
        full = suites.select_entries(); fixtures, expected = [], []
        for shard in [None, *gate.SHARDS]:
            selection = suites.select_entries(shard=shard)
            props = {} if shard is None else dict(zip(("ciShard", "ciShards"), shard.split("/")))
            fixture = {"entries": full["entries"], "properties": props,
                       "environment": {"WILDERCORD_ANIMATION_GALLERY": "1"}, "shaders": False,
                       "processed": selection["entries"], "processedPlan": selection["plan"]}
            fixtures.append(fixture); expected.append({"selection": selection, "profile": PROFILE})
            wrong = copy.deepcopy(fixture); wrong["processed"] = full["entries"][::-1]
            fixtures.append(wrong); expected.append({"error": True})
        for properties in ({"ciShard": "1"}, {"ciShards": "4"}, {"ciShard": "0", "ciShards": "4"},
                           {"ciShard": "1", "ciShards": "0"}, {"ciShard": "x", "ciShards": "4"},
                           {"ciShard": "1", "ciShards": "4", "ciSuite": "masters"}, {"tailFrom": "x"},
                           {"ciShard": "1", "ciShards": "1"}, {"ciShard": "2", "ciShards": "3"},
                           {"ciShard": "07", "ciShards": "10"}, {"ciShard": "1", "ciShards": "311"},
                           {"ciShard": "01", "ciShards": "4"}):
            fixtures.append({"entries": full["entries"], "properties": properties, "environment": {}, "shaders": False})
            expected.append({"error": True})
        for env in ({"WILDERCORD_SHOWCASE": "1"}, {"WILDERCORD_ANIMATION_GALLERY_BATCHES": "1"},
                    {"WILDERCORD_TOUR_ONLY": ""}, {"WILDERCORD_UNKNOWN": "1"}, {"JAVA_TOOL_OPTIONS": "-Dunsafe=true"}):
            fixtures.append({"entries": full["entries"], "properties": {}, "environment": env, "shaders": False})
            expected.append({"error": True})
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            base = copy.deepcopy(fixtures[0])
            for identity in (None, {}, {**full["plan"], "planSha256": "stale"},
                             {**full["plan"], "schemaVersion": True}):
                wrong = copy.deepcopy(base); wrong["processedPlan"] = identity
                fixtures.append(wrong); expected.append({"error": True})
            # Exercise actual raw-file paths in the same Groovy bridge used by Gradle.
            plan_text = (suites.ROOT / "tools/client_shard_plan.json").read_text()
            source_text = suites.DESCRIPTOR.read_text()
            bad_files = [
                ("planPath", (suites.ROOT / "tools/tests/full-client-plan-v1-310.json").read_text()),
                ("planPath", "{}"),
                ("planPath", plan_text.replace('"schemaVersion": 1', '"schemaVersion": true')),
                ("planPath", plan_text.replace('"schemaVersion": 1', '"schemaVersion": 1, "schemaVersion": 1')),
                ("sourcePath", source_text.replace('"entrypoints":', '"entrypoints": {}, "entrypoints":')),
                ("sourcePath", source_text.replace('"fabric-client-gametest":', '"fabric-client-gametest": [], "fabric-client-gametest":')),
                ("sourcePath", "// Groovy accepts comments; the shared selector must not.\n" + source_text),
                ("processedPath", '{"entrypoints":{},"entrypoints":{}}'),
            ]
            for index, (field, raw) in enumerate(bad_files):
                bad_path = root / f"bad-{index}.json"; bad_path.write_text(raw)
                wrong = copy.deepcopy(base); wrong[field] = str(bad_path)
                fixtures.append(wrong); expected.append({"error": True})
            path = root / "fixtures.json"; path.write_text(json.dumps(fixtures))
            java = str(java_home / "bin/java") if os.environ.get("JAVA_HOME") else "java"
            result = subprocess.run([java, "-cp", str(groovy / "*"), "groovy.ui.GroovyMain",
                str(suites.ROOT / "tools/tests/ClientCiContractTest.groovy"),
                str(suites.ROOT / "tools/ClientCiContract.groovy"), str(path),
                str(suites.ROOT / "tools/client_shard_plan.py"), str(suites.ROOT / "tools/client_shard_plan.json"),
                str(suites.DESCRIPTOR)],
                capture_output=True, text=True, timeout=60)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(json.loads(result.stdout), expected)
        build = (suites.ROOT / "build.gradle").read_text()
        before_native = build.split("// Check the processed descriptor immediately", 1)[1].split("tasks.withType(JavaCompile)", 1)[0]
        self.assertIn("doFirst", before_native)
        self.assertIn("file('tools/client_shard_plan.py'), file('tools/client_shard_plan.json'),", before_native)
        self.assertIn('file("${resources.destinationDir}/fabric.mod.json"))', before_native)
        self.assertIn("clientCiContract.profile(environment", before_native)
        self.assertIn("WILDERCORD_CLIENT_PROFILE ", before_native)
        self.assertIn("file('src/gametest/resources/fabric.mod.json'),", before_native)
        for dependency in ("tools/client_shard_plan.py", "tools/client_shard_plan.json"):
            self.assertIn(f"inputs.file('{dependency}')", build)
        self.assertIn("originalDescriptor.custom['wildercord:clientShardPlan'] = fullSelection.plan", build)
        self.assertEqual(full["plan"]["orderedRosterSha256"], "af359daf9aa00cf6ca2ceca38c6b7eb6239b37b10e521c4e43ae2ce43be38e9c")
        self.assertEqual([suites.select_entries(shard=s)["count"] for s in gate.SHARDS], [86, 86, 85, 84])
        # The two counter-lifetime classes and the later reviewed packs only extend the frozen 310 roster.
        additions = ("dev.wildercord.aura.UnmovedNullAcceptanceTest",
                     "dev.wildercord.aura.arts.ArtWardsHardeningTest")
        legacy = json.loads((suites.ROOT / "tools/tests/full-client-plan-v1-310.json").read_text(encoding="utf-8"))
        frozen = {entry for group in legacy["groups"] for entry in group["entries"]}
        prior = [entry for entry in full["entries"] if entry not in additions and entry in frozen]
        self.assertEqual(len(prior), 310)
        self.assertEqual(gate.selection_hash({"kind": "full", "entries": prior, "count": 310}),
                         "53db541e284da70c78a256a21b420566b83dfd1f30b8a6ea55daaf80d9569974")
        self.assertEqual(gate.selection_hash({"kind": "full", "entries": full["entries"], "count": full["count"]}), "ef87f84afd3c3239bcd32911af3fafd91d3be06671da6c1a842e384cdb1f02d8")

    def test_workflow_requires_all_four_same_run_uploads_without_scheduling_or_permission_expansion(self):
        text = (suites.ROOT / ".github/workflows/build.yml").read_text()
        job = text.split("\n  game-tests:\n", 1)[1].split("\n  full-client-gate:\n", 1)[0]
        self.assertIn("timeout-minutes: 180", job); self.assertIn("shard: [1, 2, 3, 4]", job)
        self.assertIn('WILDERCORD_ANIMATION_GALLERY: "1"', job)
        self.assertIn("FULL_CLIENT_HEAD_SHA:", job)
        self.assertIn("name: full-client-shard-${{ matrix.shard }}-${{ github.run_id }}-${{ github.run_attempt }}", job)
        aggregate = text.split("\n  full-client-gate:\n", 1)[1].split("\n  masters-native:\n", 1)[0]
        self.assertIn("needs: [game-tests]\n    if: always()", aggregate)
        self.assertIn("merge-multiple: false", aggregate); self.assertIn("digest-mismatch: error", aggregate)
        self.assertIn("--shards-result", aggregate); self.assertIn("--download-result", aggregate)
        self.assertNotIn("github-token:", aggregate); self.assertNotIn("permissions:", aggregate)
        self.assertNotIn("continue-on-error", aggregate)
        self.assertEqual(text.count("permissions:"), 1)


if __name__ == "__main__":
    unittest.main()
