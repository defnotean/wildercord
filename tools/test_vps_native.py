"""VPS native protocol fixtures; these do not establish Minecraft coverage."""
import copy
import json
import os
from pathlib import Path
import sys
import tempfile
import time
import unittest
import uuid

import client_suites as suites
import full_client_ci as full
import masters_required_ci as masters
import run_vps_checks as runner
import vps_native as native
from native_ci_diagnostics import SCENE_PREFIX
from test_full_client_ci import log_for as full_log
from vps_identity import make_identity

HEAD, COMPUTER, BATCH = "a" * 40, "WIN-I2GBVRRD872", "1791916800"


def plan(**kwargs):
    return native.prepare(head=HEAD, computer=COMPUTER, batch=BATCH, attempt="1",
                          launch=str(uuid.uuid4()), environment={}, **kwargs)


def log_for(prepared):
    selected, launch = prepared["selection"], prepared["launch"]
    if selected["kind"] == "shard":
        return full_log(selected, identity=prepared["identity"], launch=launch)
    lines = [*prepared["preamble"], suites.DESCRIPTOR_PREFIX + json.dumps(selected)]
    for entry in selected["entries"]:
        if selected["name"].startswith("masters-march-"):
            lines.append("WILDERCORD_NATIVE_WORLD " + json.dumps({"suite": entry, "seed": "-123"}))
        for event, phase in (("start", "setup"), ("phase", "run"), ("phase", "cleanup"), ("end", "returned")):
            lines.append(SCENE_PREFIX + json.dumps({"suite": entry, "event": event, "phase": phase,
                         "launchId": launch, "elapsedSeconds": 1, "sceneElapsedSeconds": 0.5}))
    return "\n".join([*lines, "BUILD SUCCESSFUL in 4m", *native.exit_markers(prepared, 0)]) + "\n"


def aggregate(manifests, *, required=False, **changes):
    return (masters if required else full).validate_aggregate(manifests,
        **{"head_sha": HEAD, "workflow_sha": HEAD, "run_id": BATCH, "run_attempt": "1",
           "executor": "windows-vps", "computer": COMPUTER, **changes})


class VpsNativeTests(unittest.TestCase):
    def test_full_shards_require_four_owned_matching_vps_launches(self):
        prepared = [plan(shard=shard) for shard in full.SHARDS]
        manifests = [native.manifest(log_for(p), p) for p in prepared]
        for m in manifests:
            self.assertEqual(m["schemaVersion"], 4)
            self.assertEqual(m["shardOutcome"], "passed", m["verificationIssues"])
            self.assertEqual(m["fullClientGate"], "unverified")
        result = aggregate(manifests)
        self.assertEqual(result["fullClientGate"], "passed", result["verificationIssues"])
        self.assertEqual(sum(item["status"] == "skipped" for item in result["clientSuites"]), 3)
        for bad in (manifests[:-1], manifests + manifests[:1], [manifests[0]] * 4):
            self.assertEqual(aggregate(bad)["fullClientGate"], "unverified")
        bad = copy.deepcopy(manifests)
        bad[1]["provenance"]["launchId"] = bad[0]["provenance"]["launchId"]
        self.assertEqual(aggregate(bad)["fullClientGate"], "unverified")

    def test_all_required_parts_preserve_exact_forty_seven_class_gate(self):
        prepared = [plan(suite=name) for name in suites.MASTERS_PART_NAMES]
        manifests = [native.manifest(log_for(p), p) for p in prepared]
        for m in manifests:
            self.assertEqual(m["schemaVersion"], 2)
            self.assertEqual(m["requiredPartOutcome"], "passed", m["verificationIssues"])
            self.assertEqual(m["focusedClientGate"], "unverified")
        result = aggregate(manifests, required=True)
        self.assertEqual(result["focusedClientGate"], "passed", result["verificationIssues"])
        self.assertEqual(result["fullClientGate"], "unverified")
        for bad in (manifests[:-1], manifests + manifests[:1], [manifests[0]] * 3):
            self.assertEqual(aggregate(bad, required=True)["focusedClientGate"], "unverified")
        bad = copy.deepcopy(manifests)
        bad[1]["provenance"]["launchId"] = bad[0]["provenance"]["launchId"]
        self.assertEqual(aggregate(bad, required=True)["focusedClientGate"], "unverified")

    def test_vps_and_legacy_or_foreign_sources_cannot_be_combined(self):
        for required in (False, True):
            prepared = ([plan(suite=name) for name in suites.MASTERS_PART_NAMES] if required else
                        [plan(shard=shard) for shard in full.SHARDS])
            manifests = [native.manifest(log_for(p), p) for p in prepared]
            gate = "focusedClientGate" if required else "fullClientGate"
            for changes in ({"head_sha": "b" * 40}, {"workflow_sha": "b" * 40},
                            {"computer": "OTHER"}, {"run_id": "1"}, {"run_attempt": "2"},
                            {"executor": None, "computer": None}, {"computer": None}):
                with self.subTest(required=required, changes=changes):
                    self.assertEqual(aggregate(manifests, required=required, **changes)[gate], "unverified")
            for key, value in (("executor", "github"), ("computer", "OTHER"),
                               ("checkoutSha", "b" * 40), ("job", "diagnostic"),
                               ("runAttempt", 1), ("launchId", "invalid")):
                bad = copy.deepcopy(manifests); bad[0]["provenance"][key] = value
                self.assertEqual(aggregate(bad, required=required)[gate], "unverified")
            bad = copy.deepcopy(manifests)
            for key in ("executor", "computer"):
                del bad[0]["provenance"][key]
            self.assertEqual(aggregate(bad, required=required)[gate], "unverified")

    def test_complete_articulated_group_with_fresh_moon_nonce(self):
        p, other = plan(suite="articulated"), plan(suite="articulated")
        result = native.manifest(log_for(p), p)
        self.assertEqual(result["focusedClientGate"], "passed", result["verificationIssues"])
        self.assertEqual(result["fullClientGate"], "unverified")
        self.assertEqual(result["counts"], {"passed": 6, "skipped": 0, "unverified": 0})
        self.assertNotEqual(p["moonReceipt"]["nonce"], other["moonReceipt"]["nonce"])
        self.assertEqual(p["environment"]["WILDERCORD_MOON_RECEIPT_NONCE"], p["moonReceipt"]["nonce"])
        self.assertEqual(p["moonReceipt"]["executor"], "windows-vps")
        self.assertFalse(p["moonReceipt"]["remoteObserverCoverage"])

    def test_release_smoke_group_is_small_and_completes_without_moon_receipt(self):
        p = plan(suite="release-smoke")
        self.assertLessEqual(p["selection"]["count"], 16)
        self.assertIsNone(p["moonReceipt"])
        result = native.manifest(log_for(p), p)
        self.assertEqual(result["focusedClientGate"], "passed", result["verificationIssues"])
        self.assertEqual(result["fullClientGate"], "unverified")
        self.assertEqual(result["counts"]["passed"], p["selection"]["count"])

    def test_partial_reordered_duplicate_and_foreign_group_logs_fail_closed(self):
        for name in ("masters-core", "masters-march-visuals", "articulated"):
            p = plan(suite=name); good = log_for(p)
            lines = good.splitlines()
            required = [suites.SELECTION_PREFIX, p["prefix"], suites.DESCRIPTOR_PREFIX,
                        SCENE_PREFIX, "BUILD SUCCESSFUL", suites.EXIT_PREFIX, full.OWNED_EXIT_PREFIX]
            samples = [good + "[TEST SKIP] required class\n", good + "BUILD FAILED\n",
                       good.replace(p["launch"], str(uuid.uuid4())), "BUILD SUCCESSFUL\n",
                       good.replace('"phase": "cleanup"', '"phase": "thrown"', 1),
                       good.replace(suites.EXIT_PREFIX + "0", suites.EXIT_PREFIX + "7")]
            for prefix in required:
                line = next(line for line in lines if line.startswith(prefix))
                samples += [good.replace(line + "\n", "", 1), good + line + "\n",
                            good.replace(line, "embedded " + line, 1)]
            samples.append("\n".join([lines[-2], *lines[:-2], lines[-1]]))
            for sample in samples:
                with self.subTest(name=name, sample=sample[-80:]):
                    result = native.manifest(sample, p)
                    self.assertTrue(result["verificationIssues"])
                    self.assertEqual(result["counts"]["passed"], 0)

    def test_full_log_cannot_borrow_even_a_complete_other_launch(self):
        p, other = plan(shard="1/4"), plan(shard="1/4")
        result = native.manifest(log_for(other), p)
        self.assertEqual(result["shardOutcome"], "unverified")
        self.assertTrue(result["verificationIssues"])

    def test_required_animation_gallery_and_explicit_profile_are_preserved(self):
        p = plan(shard="1/4")
        self.assertEqual(p["profile"], full.REQUIRED_PROFILE)
        self.assertEqual(p["environment"]["WILDERCORD_ANIMATION_GALLERY"], "1")
        self.assertEqual(p["flags"], ["-PciShard=1", "-PciShards=4"])
        bad = log_for(p).replace('"animationGallery": true', '"animationGallery": false')
        self.assertEqual(native.manifest(bad, p)["shardOutcome"], "unverified")

    def test_only_explicit_complete_selectors_and_positive_batch_identity(self):
        for selectors in ({}, {"suite": "masters"}, {"suite": "diagnostic-excise"},
                          {"shard": "1/3"}, {"suite": "articulated", "shard": "1/4"}):
            with self.assertRaises((ValueError, KeyError)):
                plan(**selectors)
        for computer, batch, attempt in (("", BATCH, "1"), ("bad/name", BATCH, "1"),
                                        (COMPUTER, "0", "1"), (COMPUTER, BATCH, 1),
                                        (COMPUTER, BATCH, "0")):
            with self.assertRaises(ValueError):
                make_identity(HEAD, computer, batch, attempt, "game-tests")

    @unittest.skipUnless(os.name == "nt", "Actual Windows child output and exit")
    def test_runner_records_actual_child_exit_after_output(self):
        p = plan(suite="masters-march-mechanics")
        with tempfile.TemporaryDirectory() as directory:
            for code in (0, 7):
                path = Path(directory) / (str(code) + ".log")
                row = runner.invoke([sys.executable, "-c", f"print('actual child output', flush=True); raise SystemExit({code})"],
                    path, os.environ.copy(), time.monotonic() + 10, plan=p)
                self.assertEqual(row["exit"], code)
                lines = path.read_text().splitlines()
                self.assertEqual(lines[:2], p["preamble"][:2])
                self.assertEqual(lines[-2:], native.exit_markers(p, code))
                self.assertIn("actual child output", lines)
                self.assertEqual(native.manifest(path.read_text(), p)["requiredPartOutcome"], "unverified")


if __name__ == "__main__":
    unittest.main()
