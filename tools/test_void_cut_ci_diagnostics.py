"""Tempfile-only diagnostic preservation checks; never native animation acceptance."""
from contextlib import redirect_stdout
import hashlib
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import collect_void_cut_diagnostics as diagnostics


class DiagnosticFixture(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.marker = "review/void-cut-start.json"
        self.output = "review/void-cut-diagnostics"
        self.identity = {"repository": "example/wildercord", "sourceCommit": "a" * 40,
                         "headCommit": "b" * 40, "runId": "287", "runAttempt": "1",
                         "job": "game-tests", "jobGroup": "game-tests-4-of-4"}

    def prepare(self):
        diagnostics.prepare(self.root, self.marker, self.identity)
        return json.loads((self.root / self.marker).read_text())

    def source(self, name, data=b"opaque native bytes"):
        path = self.root / diagnostics.SOURCE / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        return path

    def collect(self, **kwargs):
        return diagnostics.collect(self.root, self.marker, self.output, self.identity, **kwargs)


class CollectionTests(DiagnosticFixture):
    def test_no_captures_returns_honest_empty_manifest(self):
        self.prepare()
        result = self.collect()
        self.assertEqual(result["status"], "diagnostics-unavailable")
        self.assertEqual(result["missingFiles"], list(diagnostics.FILES))
        self.assertEqual(result["selectedBytes"], 0)
        self.assertFalse(result["testVerdictEstablished"])
        self.assertFalse(result["acceptedAnimationCoverageEstablished"])
        self.assertEqual(list((self.root / self.output).iterdir()),
                         [self.root / self.output / "manifest.json"])

    def test_exact_names_preserve_rejected_or_incomplete_bytes_and_hashes(self):
        self.prepare()
        sources = {}
        for name in diagnostics.FILES:
            data = (b'{"passed": false, "spanGui": 14}\n' if name.endswith(".json")
                    else b"\x89PNG\r\n\x1a\nopaque native framebuffer\x00\xff")
            sources[name] = self.source(name, data).read_bytes()
        for name in ("unrelated.png", "masters_style_void_cut_first_settled.png",
                     "masters_style_void_cut_third_active.png",
                     "masters_style_void_cut_first_active.png.bak",
                     "nested/masters_style_void_cut_first_active.png"):
            self.source(name)
        result = self.collect()
        self.assertEqual(result["provenance"], self.identity)
        self.assertEqual(result["status"], "diagnostics-available")
        self.assertEqual(result["missingFiles"], [])
        self.assertEqual({path.name for path in (self.root / self.output).iterdir()},
                         {*diagnostics.FILES, "manifest.json"})
        self.assertIn("may include rejected", result["basis"])
        self.assertFalse(result["acceptedAnimationCoverageEstablished"])
        for row in result["files"]:
            self.assertEqual(row["status"], "included")
            self.assertEqual(row["bytes"], len(sources[row["name"]]))
            self.assertEqual(row["sha256"], hashlib.sha256(sources[row["name"]]).hexdigest())
            self.assertEqual((self.root / self.output / row["artifactPath"]).read_bytes(), sources[row["name"]])
            self.assertEqual((self.root / row["sourcePath"]).read_bytes(), sources[row["name"]])

    def test_missing_counterpart_is_not_fabricated(self):
        self.prepare()
        name = "masters_style_void_cut_first_active.png"
        self.source(name)
        result = self.collect()
        self.assertEqual(result["missingFiles"], [entry for entry in diagnostics.FILES if entry != name])
        self.assertEqual(sum(row["status"] == "included" for row in result["files"]), 1)

    def test_preexisting_name_is_excluded_even_when_overwritten_after_marker(self):
        name = diagnostics.FILES[0]
        path = self.source(name, b"old run")
        self.prepare()
        path.write_bytes(b"replacement with fresh timestamps")
        result = self.collect()
        self.assertEqual(result["files"][0]["status"], "preexisting")
        self.assertEqual(result["status"], "diagnostics-unavailable")
        self.assertNotIn(name, result["missingFiles"])

    def test_missing_marker_cannot_adopt_workspace_files(self):
        self.source(diagnostics.FILES[0])
        result = self.collect()
        self.assertEqual(result["freshness"]["status"], "missing-preparation-marker")
        self.assertEqual(result["files"][0]["status"], "freshness-unavailable")
        self.assertEqual(result["status"], "diagnostics-unavailable")

    def test_other_commit_run_attempt_or_job_marker_cannot_be_reused(self):
        for key in ("sourceCommit", "headCommit", "runId", "runAttempt", "job", "jobGroup"):
            with self.subTest(key=key):
                self.prepare()
                wrong = {**self.identity, key: "different"}
                with self.assertRaisesRegex(diagnostics.DiagnosticError, "job/run/attempt"):
                    diagnostics.collect(self.root, self.marker, self.output, wrong)
                (self.root / self.marker).unlink()
        self.assertFalse((self.root / self.output).exists())

    def test_old_or_future_mtime_cannot_claim_current_capture(self):
        stamp = self.prepare()
        past = self.source(diagnostics.FILES[0])
        future = self.source(diagnostics.FILES[1])
        os.utime(past, ns=(stamp["startedNs"] - 1, stamp["startedNs"] - 1))
        os.utime(future, ns=(stamp["startedNs"] + 10**12, stamp["startedNs"] + 10**12))
        result = self.collect()
        self.assertEqual([row["status"] for row in result["files"][:2]],
                         ["outside-current-run-window"] * 2)

    def test_existing_marker_and_output_are_never_reused(self):
        self.prepare()
        with self.assertRaises(FileExistsError):
            self.prepare()
        self.collect()
        with self.assertRaisesRegex(diagnostics.DiagnosticError, "Never reuse"):
            self.collect()

    def test_payload_manifest_and_uncompressed_zip_stay_below_fourteen_mb(self):
        self.prepare()
        # Fill the payload allowance exactly, leaving the small manifest reserve.
        self.source(diagnostics.FILES[0], b"x" * (diagnostics.BYTE_LIMIT - diagnostics.REPORT_RESERVE))
        self.source(diagnostics.FILES[1], b"too large for remaining budget")
        result = self.collect()
        self.assertEqual(result["files"][0]["status"], "included")
        self.assertEqual(result["files"][1]["status"], "omitted-for-byte-limit")
        output = self.root / self.output
        self.assertLess(sum(path.stat().st_size for path in output.iterdir()), 14_000_000)
        archive = self.root / "diagnostics.zip"
        with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_STORED) as zipped:
            for path in output.iterdir():
                zipped.write(path, path.name)
        self.assertLess(archive.stat().st_size, 14_000_000)

    def test_oversized_file_does_not_hide_later_small_diagnostics(self):
        self.prepare()
        self.source(diagnostics.FILES[0], b"x" * 3000)
        self.source(diagnostics.FILES[1], b"native failure buffer")
        result = self.collect(budget=diagnostics.REPORT_RESERVE + 100)
        self.assertEqual(result["files"][0]["status"], "omitted-for-byte-limit")
        self.assertEqual(result["files"][1]["status"], "included")

    def test_invalid_budget_is_rejected(self):
        self.prepare()
        for budget in (0, diagnostics.REPORT_RESERVE - 1, diagnostics.BYTE_LIMIT + 1):
            with self.subTest(budget=budget), self.assertRaises(diagnostics.DiagnosticError):
                self.collect(budget=budget)

    def test_source_change_during_copy_cannot_publish_partial_artifact(self):
        self.prepare()
        name = diagnostics.FILES[0]
        source = self.source(name)
        original_safe_path, visits = diagnostics.safe_path, []

        def change_on_recheck(root, relative):
            result = original_safe_path(root, relative)
            if result == source:
                visits.append(result)
                if len(visits) == 2:
                    source.write_bytes(b"changed while copying")
            return result

        with patch.object(diagnostics, "safe_path", side_effect=change_on_recheck):
            with self.assertRaisesRegex(diagnostics.DiagnosticError, "changed while being copied"):
                self.collect()
        self.assertFalse((self.root / self.output).exists())

    def test_missing_captures_do_not_make_cli_fail(self):
        self.prepare()
        with patch.object(diagnostics, "ROOT", self.root), \
                patch.object(diagnostics, "provenance", return_value=self.identity), redirect_stdout(io.StringIO()):
            result = diagnostics.main(["--job-group", "game-tests-4-of-4", "--marker", self.marker,
                                       "--output", self.output])
        self.assertEqual(result, 0)


class PathSafetyTests(DiagnosticFixture):
    def test_absolute_traversal_and_overlapping_paths_are_rejected(self):
        for path in ("../outside", "review/../../outside", str(self.root / "absolute"), "."):
            with self.subTest(path=path), self.assertRaises(diagnostics.DiagnosticError):
                diagnostics.safe_path(self.root, path)
        for marker in (diagnostics.SOURCE / "marker.json", "build"):
            with self.subTest(marker=marker), self.assertRaises(diagnostics.DiagnosticError):
                diagnostics.prepare(self.root, marker, self.identity)
        self.prepare()
        for output in (diagnostics.SOURCE, diagnostics.SOURCE / "out", self.marker, "review"):
            with self.subTest(output=output), self.assertRaises(diagnostics.DiagnosticError):
                diagnostics.collect(self.root, self.marker, output, self.identity)

    def test_source_symlink_is_rejected_without_reading_target(self):
        self.prepare()
        path = self.root / diagnostics.SOURCE / diagnostics.FILES[0]
        path.parent.mkdir(parents=True)
        path.symlink_to(self.root / "missing-external-target")
        with self.assertRaisesRegex(diagnostics.DiagnosticError, "Symlinks"):
            self.collect()
        self.assertFalse((self.root / self.output).exists())

    def test_ancestor_marker_and_output_symlinks_are_rejected(self):
        self.prepare()
        outside = self.root / "outside"
        outside.mkdir()
        (self.root / "build").symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(diagnostics.DiagnosticError, "Symlinks"):
            self.collect()
        (self.root / "build").unlink()
        (self.root / self.marker).unlink()
        (self.root / self.marker).symlink_to(outside / "marker.json")
        with self.assertRaisesRegex(diagnostics.DiagnosticError, "Symlinks"):
            self.collect()
        (self.root / self.marker).unlink()
        self.prepare()
        (self.root / self.output).symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(diagnostics.DiagnosticError, "Symlinks"):
            self.collect()

    def test_nonregular_source_is_rejected_without_blocking(self):
        self.prepare()
        path = self.root / diagnostics.SOURCE / diagnostics.FILES[0]
        path.parent.mkdir(parents=True)
        os.mkfifo(path)
        with self.assertRaisesRegex(diagnostics.DiagnosticError, "regular"):
            self.collect()


class ProvenanceTests(unittest.TestCase):
    def setUp(self):
        self.environ = {"GITHUB_SHA": "a" * 40, "GITHUB_REPOSITORY": "example/wildercord",
                        "GITHUB_RUN_ID": "287", "GITHUB_RUN_ATTEMPT": "2",
                        "GITHUB_JOB": "game-tests", "GITHUB_EVENT_NAME": "push"}

    def provenance(self, **changes):
        with patch.object(diagnostics.subprocess, "check_output", return_value="a" * 40 + "\n"):
            return diagnostics.provenance(Path("."), {**self.environ, **changes}, "game-tests-4-of-4")

    def test_commit_run_attempt_and_matrix_group_are_explicit(self):
        result = self.provenance()
        self.assertEqual(result["sourceCommit"], "a" * 40)
        self.assertEqual(result["headCommit"], "a" * 40)
        self.assertEqual(result["runId"], "287")
        self.assertEqual(result["runAttempt"], "2")
        self.assertEqual(result["jobGroup"], "game-tests-4-of-4")

    def test_missing_or_inconsistent_current_run_identity_is_rejected(self):
        for changes in ({"GITHUB_SHA": "b" * 40}, {"GITHUB_SHA": "short"},
                        {"GITHUB_JOB": "masters-native"}, {"GITHUB_RUN_ID": ""},
                        {"GITHUB_RUN_ATTEMPT": "0"}, {"GITHUB_REPOSITORY": "../elsewhere"}):
            with self.subTest(changes=changes), self.assertRaises(diagnostics.DiagnosticError):
                self.provenance(**changes)

    def test_pr_head_is_distinct_from_checked_out_merge_commit(self):
        with tempfile.TemporaryDirectory() as temporary:
            event = Path(temporary) / "event.json"
            event.write_text(json.dumps({"pull_request": {"head": {"sha": "b" * 40}}}))
            result = self.provenance(GITHUB_EVENT_NAME="pull_request", GITHUB_EVENT_PATH=str(event))
        self.assertEqual(result["headCommit"], "b" * 40)
        self.assertEqual(result["sourceCommit"], "a" * 40)


if __name__ == "__main__":
    unittest.main()
