"""Adversarial explicit-plan checks; synthetic evidence is not native acceptance."""
import copy
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import client_shard_plan as plan
import client_suites as suites
import full_client_ci as gate
from test_full_client_ci import IDENTITY, log_for


class ExplicitPlanTests(unittest.TestCase):
    def setUp(self):
        self.entries = plan.read_json(suites.DESCRIPTOR)["entrypoints"]["fabric-client-gametest"]
        self.plan = plan.read_json(plan.PLAN)

    def rejected(self, edit, message=None):
        changed = copy.deepcopy(self.plan)
        edit(changed)
        with self.assertRaisesRegex(ValueError, message or "."):
            plan.validate_plan(self.entries, changed)

    def test_exact_reviewed_groups_and_independently_pinned_identity(self):
        self.assertEqual(plan.digest(self.plan), "d81692710775b8ce351c63ea357e6e56a643b138879962f7d97e6aa1b7fbe460")
        self.assertEqual(plan.digest(self.entries), "af359daf9aa00cf6ca2ceca38c6b7eb6239b37b10e521c4e43ae2ce43be38e9c")
        self.assertEqual([plan.digest(group["entries"]) for group in self.plan["groups"]], [
            "2271c4d3db8124db7897651d1742ad1cf0c581ef79e76c07df28584121c7b226",
            "43a7468679c26e787c54c1c8158d263cdb5bd78265547bc75bc30d6823a79a29",
            "5367df0a9a4eb7a7719878c42e3c457ae749fcad80a65d7e561e7cc5ddca3c48",
            "f5f487f50798fee8cdf3a0c3a5c625a7f766b76fd57f8e4b23654c63d60fc1a2"])
        full = suites.select_entries()
        for shard, group in zip(gate.SHARDS, self.plan["groups"]):
            selected = suites.select_entries(shard=shard)
            self.assertEqual(selected["entries"], group["entries"])
            self.assertEqual(selected["plan"], full["plan"])
        # The reviewed 317-roster groups are kept whole; later classes were only added to them.
        reviewed = plan.read_json(suites.ROOT / "tools/tests/full-client-plan-v2-317.json")
        self.assertEqual(plan.digest(reviewed), "cc43cf2f6b4b0a5f7fb99e15ae44b2f39502022e6cdc448d16ba17c45340171c")
        for current, previous in zip(self.plan["groups"], reviewed["groups"]):
            kept = set(previous["entries"])
            self.assertEqual([entry for entry in current["entries"] if entry in kept], previous["entries"])

    def test_counter_extension_preserves_every_reviewed_group_and_rejects_v1(self):
        legacy_path = suites.ROOT / "tools/tests/full-client-plan-v1-310.json"
        legacy = plan.read_json(legacy_path)
        self.assertEqual(plan.digest(legacy), "483153f41a4082651ee9e2210a742d73e201cd228ce836f45baf5184ccaa2082")
        additions = ("dev.wildercord.aura.UnmovedNullAcceptanceTest",
                     "dev.wildercord.aura.arts.ArtWardsHardeningTest")
        reviewed = plan.read_json(suites.ROOT / "tools/tests/full-client-plan-v2-317.json")
        later = set(self.entries) - set(additions) - {entry for group in legacy["groups"] for entry in group["entries"]}
        previous_entries = [entry for entry in self.entries if entry not in additions and entry not in later]
        self.assertEqual(len(previous_entries), 310)
        self.assertEqual(plan.digest(previous_entries), "4cbbad85c756e671b0ef2907034ac81288fffa94ceb7990b91eba1f208a26941")
        self.assertEqual(self.plan["requiredBlocks"][:-1], legacy["requiredBlocks"])
        for index, (current, previous) in enumerate(zip(self.plan["groups"], legacy["groups"])):
            self.assertEqual(current["id"], previous["id"])
            self.assertEqual([entry for entry in current["entries"] if entry not in additions and entry not in later],
                             previous["entries"])
            added_later = sum(entry in later for entry in current["entries"])
            self.assertEqual(current["expectedCount"], previous["expectedCount"] + (2 if index == 3 else 0) + added_later)
        for entry in additions:
            self.assertEqual(self.plan["groups"][3]["entries"].count(entry), 1)
            self.assertFalse(any(entry in group["entries"] for group in self.plan["groups"][:3]))
        with self.assertRaises(ValueError):
            plan.validate_plan(self.entries, legacy)
        for shard in (None, *gate.SHARDS):
            with self.subTest(shard=shard), self.assertRaises(ValueError):
                plan.selection(self.entries, shard=shard, plan_path=legacy_path)
        # Even a structurally complete old plan cannot borrow the new descriptor's identity.
        with self.assertRaises(ValueError):
            plan.validate_plan(previous_entries, self.plan)
        with self.assertRaises(ValueError):
            plan.validate_plan(self.entries, reviewed)

    def test_stale_roster_add_remove_duplicate_foreign_and_reorder_fail(self):
        variants = [self.entries + ["dev.wildercord.NewTest"], self.entries[:-1],
                    self.entries + self.entries[:1], ["dev.wildercord.ForeignTest", *self.entries[1:]],
                    [self.entries[1], self.entries[0], *self.entries[2:]], None, {}, [1], [" "]]
        for entries in variants:
            with self.subTest(entries=str(entries)[:60]), self.assertRaises(ValueError):
                plan.validate_plan(entries, self.plan)

    def test_malformed_schema_identity_and_types_fail(self):
        for root in (None, [], 1, "plan"):
            with self.subTest(root=root), self.assertRaises(ValueError):
                plan.validate_plan(self.entries, root)
        for key in self.plan:
            self.rejected(lambda value: value.pop(key))
        self.rejected(lambda value: value.update(foreign=True))
        for key in ("schemaVersion", "shardCount", "expectedCount"):
            for invalid in (True, None, "1", 1.0, -1, 0):
                self.rejected(lambda value: value.__setitem__(key, invalid))
        for key in ("planId", "orderedRosterSha256"):
            for invalid in (None, {}, [], 1, "stale"):
                self.rejected(lambda value: value.__setitem__(key, invalid))
        for profile in (None, [], {}, {**plan.REQUIRED_PROFILE, "animationGallery": 1},
                        {**plan.REQUIRED_PROFILE, "showcase": True}, {**plan.REQUIRED_PROFILE, "extra": False}):
            self.rejected(lambda value: value.update(executionProfile=profile))

    def test_malformed_group_types_ids_counts_and_entries_fail(self):
        for groups in (None, {}, [], self.plan["groups"][:-1], self.plan["groups"] + self.plan["groups"][:1],
                       self.plan["groups"][::-1], [None] * 4):
            self.rejected(lambda value: value.update(groups=groups))
        for key in ("id", "expectedCount", "entries"):
            self.rejected(lambda value: value["groups"][0].pop(key))
        self.rejected(lambda value: value["groups"][0].update(extra=1))
        for key in ("id", "expectedCount"):
            for invalid in (True, 1.0, "1", None, [], {}, 0, -1, 100):
                self.rejected(lambda value: value["groups"][0].__setitem__(key, invalid))
        for invalid in (None, [], {}, [True], [1], [""], ["dev.wildercord.cast.RelayCircleTest "],
                        ["dev.wildercord.cast.RelayCircleTest"] * 80):
            self.rejected(lambda value: value["groups"][0].update(entries=invalid))
        self.rejected(lambda value: value["groups"][0]["entries"].__setitem__(0, "dev.wildercord.ForeignTest"), "foreign")
        self.rejected(lambda value: value["groups"][0]["entries"].reverse(), "relative order")
        self.rejected(lambda value: value["groups"][0]["entries"].pop(), "count")
        # A duplicate across groups can preserve each group's own uniqueness, count and order.
        def overlap(value):
            value["groups"][0]["entries"][-1] = self.entries[-1]
        self.rejected(overlap, "disjoint")

    def swap(self, value, first_group, first_entry, second_group, second_entry):
        first, second = value["groups"][first_group]["entries"], value["groups"][second_group]["entries"]
        first[first.index(first_entry)], second[second.index(second_entry)] = second_entry, first_entry
        first.sort(key=self.entries.index)
        second.sort(key=self.entries.index)

    def test_required_blocks_are_exact_contiguous_colocated_class_lists(self):
        for block in self.plan["requiredBlocks"]:
            classes = block["entries"]
            owner = next(i for i, group in enumerate(self.plan["groups"]) if classes[0] in group["entries"])
            other = (owner + 1) % 4
            replacement = next(entry for entry in self.plan["groups"][other]["entries"]
                               if not any(entry in values for values in plan.REQUIRED_BLOCKS.values()))
            self.rejected(lambda value: self.swap(value, owner, classes[-1], other, replacement), "block was split")
        for key in ("id", "entries"):
            self.rejected(lambda value: value["requiredBlocks"][0].pop(key), "exact required")
        self.rejected(lambda value: value["requiredBlocks"].pop(), "exact required")
        self.rejected(lambda value: value["requiredBlocks"][0]["entries"].reverse(), "exact required")
        # UpgradeRecovery is part of the contiguous settings span, although it is not articulated.
        settings = plan.REQUIRED_BLOCKS["settings-articulated"]
        self.assertEqual(settings[3], "dev.wildercord.world.upgrade.UpgradeRecoveryTest")

    def test_reassignment_with_valid_counts_union_order_and_blocks_still_fails_hash(self):
        self.rejected(lambda value: self.swap(value, 0, "dev.wildercord.cast.CampConcordCraftTest",
            1, "dev.wildercord.client.fx.VoidWardDeliveryTest"), "plan hash changed")

    def test_unsupported_totals_and_noncanonical_shards_never_fall_back(self):
        for shard in ("1/1", "2/3", "1/311", "01/4", "1/04", "07/10", "0/4", "5/4", " 1/4", "1/4 ", "", True):
            with self.subTest(shard=shard), self.assertRaises(ValueError):
                plan.selection(self.entries, shard=shard)

    def test_plan_file_readers_fail_closed_for_malformed_and_nonregular_inputs(self):
        raw = json.dumps(self.plan)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); path = root / "plan.json"
            for bad in ("{", "null", "[]", raw.replace('"schemaVersion": 1', '"schemaVersion": 1, "schemaVersion": 1'),
                        raw.replace('"id": 1', '"id": 1, "id": 1', 1),
                        raw.replace('"schemaVersion": 1', '"schemaVersion": NaN'),
                        raw.replace('"schemaVersion": 1', '"schemaVersion": Infinity'), " " * (plan.MAX_CONFIG_BYTES + 1)):
                path.write_text(bad)
                with self.assertRaises(ValueError): plan.selection(self.entries, plan_path=path)
            path.write_bytes(b"\xff")
            with self.assertRaises(ValueError): plan.selection(self.entries, plan_path=path)
            path.write_text(raw)
            link = root / "link"; link.symlink_to(path)
            fifo = root / "fifo"; os.mkfifo(fifo)
            for bad in (link, fifo, root, root / "missing"):
                with self.assertRaises((ValueError, OSError)): plan.selection(self.entries, plan_path=bad)

    def test_processed_descriptor_rejects_old_arithmetic_and_missing_or_stale_identity(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "fabric.mod.json"
            for index, shard in enumerate(gate.SHARDS):
                expected = suites.select_entries(shard=shard)
                good = {"entrypoints": {"fabric-client-gametest": expected["entries"]},
                        "custom": {"wildercord:clientShardPlan": expected["plan"]}}
                path.write_text(json.dumps(good)); plan.verify_processed(expected, path)
                variants = [None, {}, {"entrypoints": good["entrypoints"]},
                    {"entrypoints": {"fabric-client-gametest": self.entries[len(self.entries) * index // 4:len(self.entries) * (index + 1) // 4]}}]
                for key in expected["plan"]:
                    missing = copy.deepcopy(good); missing["custom"]["wildercord:clientShardPlan"].pop(key)
                    changed = copy.deepcopy(good); changed["custom"]["wildercord:clientShardPlan"][key] = "stale"
                    variants.extend([missing, changed])
                for bad in variants:
                    path.write_text(json.dumps(bad))
                    with self.assertRaises(ValueError): plan.verify_processed(expected, path)
                # Duplicate metadata is rejected from RAW resource bytes, not after a lenient parse.
                path.write_text(json.dumps(good).replace('"planId":', '"planId": "stale", "planId":'))
                with self.assertRaises(ValueError): plan.verify_processed(expected, path)

    def test_plan_identity_is_required_at_every_log_and_aggregate_boundary(self):
        manifests = []
        for shard in gate.SHARDS:
            selection = suites.select_entries(shard=shard)
            evidence, issues = gate.run_evidence(log_for(selection), selection, gate.REQUIRED_PROFILE, identity=IDENTITY)
            self.assertEqual(issues, [])
            results = gate.suite_results(selection, gate.REQUIRED_PROFILE)
            manifests.append({**evidence, "shard": shard, "selection": selection,
                "shardOutcome": "passed", "fullClientGate": "unverified", "verificationIssues": [],
                "clientSuites": results, "counts": {status: sum(item["status"] == status for item in results)
                                                     for status in ("passed", "skipped", "unverified")}})
        kwargs = dict(head_sha=IDENTITY["headSha"], workflow_sha=IDENTITY["workflowSha"], run_id="123", run_attempt="1")
        accepted = gate.validate_aggregate(manifests, **kwargs)
        self.assertEqual(accepted["fullClientGate"], "passed")
        self.assertEqual(accepted["plan"], suites.select_entries()["plan"])
        selection = suites.select_entries(shard="3/4")  # Same class set as the historical 309-roster group 3.
        good = log_for(selection)
        for prefix in (suites.SELECTION_PREFIX, suites.DESCRIPTOR_PREFIX, gate.PROVENANCE_PREFIX):
            for key in (None, *selection["plan"]):
                lines = good.splitlines()
                index = next(i for i, line in enumerate(lines) if line.startswith(prefix))
                marker = json.loads(lines[index][len(prefix):])
                if key is None: marker.pop("plan")
                else: marker["plan"][key] = "stale"
                lines[index] = prefix + json.dumps(marker)
                _, issues = gate.run_evidence("\n".join(lines), selection, gate.REQUIRED_PROFILE, identity=IDENTITY)
                self.assertTrue(issues, (prefix, key))
        for boundary in ("selection", "provenance"):
            for key in (None, *selection["plan"]):
                bad = copy.deepcopy(manifests)
                if key is None: bad[2][boundary].pop("plan")
                else: bad[2][boundary]["plan"][key] = "stale"
                self.assertEqual(gate.validate_aggregate(bad, **kwargs)["fullClientGate"], "unverified", (boundary, key))
        # Old schema/hash with the identical third roster cannot borrow a fresh owned lifecycle.
        old = copy.deepcopy(manifests)
        old[2]["schemaVersion"] = 2
        old[2]["provenance"]["schemaVersion"] = 2
        old[2]["selection"].pop("plan")
        old[2]["provenance"].pop("plan")
        self.assertEqual(gate.validate_aggregate(old, **kwargs)["fullClientGate"], "unverified")

    def test_focused_masters_parts_are_independent_of_full_plan(self):
        with patch("client_suites.full_selection", side_effect=AssertionError("Focused suites must not consult full plan")), \
                patch("client_suites.read_json", side_effect=AssertionError("Focused suites retain their existing portable reader")):
            masters = suites.select_entries(suite="masters")
            parts = [suites.select_entries(suite=name) for name in suites.MASTERS_PART_NAMES]
        self.assertEqual(masters["count"], 47)
        self.assertEqual([part["count"] for part in parts], [44, 1, 2])
        self.assertEqual(set(masters["entries"]), {entry for part in parts for entry in part["entries"]})

    def test_portable_reader_without_posix_flags_retains_bounded_identity_checks(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); path = root / "plan.json"
            path.write_text(json.dumps(self.plan))
            link = root / "link"; link.symlink_to(path)
            fifo = root / "fifo"; os.mkfifo(fifo)
            with patch.object(plan.os, "O_NOFOLLOW", None), patch.object(plan.os, "O_NONBLOCK", None):
                self.assertEqual(plan.selection(self.entries, shard="1/4", plan_path=path)["count"], self.plan["groups"][0]["expectedCount"])
                for bad in (link, fifo, root):
                    with self.assertRaises(ValueError): plan.selection(self.entries, plan_path=bad)
                path.write_text(" " * (plan.MAX_CONFIG_BYTES + 1))
                with self.assertRaises(ValueError): plan.selection(self.entries, plan_path=path)
                self.assertEqual(suites.select_entries(suite="masters-core")["count"], 44)

    def test_portable_reader_rejects_replacement_before_and_after_open(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); path = root / "plan.json"; other = root / "other.json"
            path.write_text(json.dumps(self.plan)); other.write_text(json.dumps(self.plan))
            original_open = os.open
            def replace_before_open(name, flags):
                path.unlink(); path.symlink_to(other)
                return original_open(name, flags)
            with patch.object(plan.os, "O_NOFOLLOW", None), patch.object(plan.os, "O_NONBLOCK", None), \
                    patch.object(plan.os, "open", side_effect=replace_before_open):
                with self.assertRaisesRegex(ValueError, "path changed before read"):
                    plan.read_json(path)
            path.unlink(); path.write_text(json.dumps(self.plan))
            original_lstat = os.lstat
            calls = 0
            def replace_after_read(name):
                nonlocal calls
                calls += 1
                if calls == 2:
                    path.unlink(); path.symlink_to(other)
                return original_lstat(name)
            with patch.object(plan.os, "O_NOFOLLOW", None), patch.object(plan.os, "O_NONBLOCK", None), \
                    patch.object(plan.os, "lstat", side_effect=replace_after_read):
                with self.assertRaisesRegex(ValueError, "changed during read"):
                    plan.read_json(path)

    def test_standalone_selector_cli_and_fail_closed_output(self):
        script = str(suites.ROOT / "tools/client_shard_plan.py")
        raw = json.dumps(self.entries)
        for shard in gate.SHARDS:
            result = subprocess.run([sys.executable, "-I", script, "--shard", shard],
                                    input=raw, capture_output=True, text=True, timeout=10)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(json.loads(result.stdout), suites.select_entries(shard=shard))
        for data, args in (("null", []), (raw, ["--shard", "1/3"]), ("[", []), (" " * (plan.MAX_CONFIG_BYTES + 1), [])):
            result = subprocess.run([sys.executable, "-I", script, *args], input=data,
                                    capture_output=True, text=True, timeout=10)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(result.stdout, "")


if __name__ == "__main__":
    unittest.main()
