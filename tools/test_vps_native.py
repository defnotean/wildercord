"""VPS native protocol fixtures; these do not establish Minecraft coverage."""
import os
from pathlib import Path
import sys
import tempfile
import time
import unittest
import uuid
import json

import client_suites as suites
import full_client_ci as full
import run_vps_checks as runner
import vps_native as native
from native_ci_diagnostics import SCENE_PREFIX
from vps_identity import make_identity

HEAD, COMPUTER, BATCH = "a" * 40, "WIN-I2GBVRRD872", "1791916800"


def plan(**kwargs):
    return native.prepare(head=HEAD, computer=COMPUTER, batch=BATCH, attempt="1",
                          launch=str(uuid.uuid4()), environment={}, **kwargs)


def log_for(prepared):
    selected, launch = prepared["selection"], prepared["launch"]
    lines = [*prepared["preamble"], suites.DESCRIPTOR_PREFIX + json.dumps(selected)]
    for entry in selected["entries"]:
        for event, phase in (("start", "setup"), ("phase", "run"), ("phase", "cleanup"), ("end", "returned")):
            lines.append(SCENE_PREFIX + json.dumps({"suite": entry, "event": event, "phase": phase,
                         "launchId": launch, "elapsedSeconds": 1, "sceneElapsedSeconds": 0.5}))
    return "\n".join([*lines, "BUILD SUCCESSFUL in 4m", *native.exit_markers(prepared, 0)]) + "\n"


class VpsNativeTests(unittest.TestCase):
    def test_release_smoke_group_is_small_and_completes(self):
        p = plan(suite="release-smoke")
        self.assertLessEqual(p["selection"]["count"], 16)
        self.assertIsNone(p["moonReceipt"])
        result = native.manifest(log_for(p), p)
        self.assertEqual(result["focusedClientGate"], "passed", result["verificationIssues"])
        self.assertEqual(result["fullClientGate"], "unverified")
        self.assertEqual(result["counts"]["passed"], p["selection"]["count"])

    def test_partial_reordered_duplicate_and_foreign_logs_fail_closed(self):
        p = plan(suite="release-smoke")
        good = log_for(p)
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
            with self.subTest(sample=sample[-80:]):
                result = native.manifest(sample, p)
                self.assertTrue(result["verificationIssues"])
                self.assertEqual(result["counts"]["passed"], 0)

    def test_only_the_smoke_selector_and_positive_batch_identity(self):
        for selectors in ({}, {"suite": "masters"}, {"shard": "1/4"}, {"suite": "release-smoke", "shard": "1/4"}):
            with self.assertRaises((ValueError, KeyError)):
                plan(**selectors)
        for computer, batch, attempt in (("", BATCH, "1"), ("bad/name", BATCH, "1"),
                                        (COMPUTER, "0", "1"), (COMPUTER, BATCH, 1),
                                        (COMPUTER, BATCH, "0")):
            with self.assertRaises(ValueError):
                make_identity(HEAD, computer, batch, attempt, "smoke-native")

    @unittest.skipUnless(os.name == "nt", "Actual Windows child output and exit")
    def test_runner_records_actual_child_exit_after_output(self):
        p = plan(suite="release-smoke")
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
                self.assertEqual(native.manifest(path.read_text(), p)["focusedClientGate"], "unverified")


if __name__ == "__main__":
    unittest.main()
