"""The Life catalogue aggregate cannot replace a custom held lesson with a generic gallery pass."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
import client_suites


class LifeExcisePartitionTests(unittest.TestCase):
    def test_mandatory_aggregate_keeps_generic_suites_and_ordinary_excise(self):
        selected = client_suites.select_entries(suite="diagnostic-life-excise")
        self.assertEqual("diagnostic", selected["kind"])
        self.assertEqual(list(client_suites.LIFE_EXCISE_ENTRIES), selected["entries"])
        self.assertEqual(5, selected["count"])

    def test_direct_gradle_preserves_the_same_complete_diagnostic_contract(self):
        source = (client_suites.ROOT / "build.gradle").read_text()
        roster = source.split("def lifeExciseEntries = [", 1)[1].split("]", 1)[0]
        self.assertEqual(list(client_suites.LIFE_EXCISE_ENTRIES), __import__("re").findall(r"'([^']+)'", roster))
        self.assertIn("group.purpose != 'diagnostic' || group.expectedCount != 5 || group.entries != lifeExciseEntries", source)
        self.assertIn("validateLifeExciseSelection(name, group)", source)
        self.assertIn("validateLifeExciseSelection(name, catalog[name])", source)
        self.assertIn("Life32 full descriptor must retain the separate Excise ordinary class", source)

    def test_removing_any_class_cannot_relabel_an_incomplete_aggregate(self):
        original = json.loads(client_suites.CATALOG.read_text())
        for removed in client_suites.LIFE_EXCISE_ENTRIES:
            with self.subTest(removed=removed), tempfile.TemporaryDirectory() as tmp:
                catalog = copy.deepcopy(original)
                group = catalog["diagnostic-life-excise"]
                group["entries"].remove(removed)
                group["expectedCount"] = 4
                path = Path(tmp) / "catalog.json"
                path.write_text(json.dumps(catalog))
                with self.assertRaisesRegex(ValueError, "Life32 requires"):
                    client_suites.select_entries(suite="diagnostic-life-excise", catalog=path)

    def test_the_full_roster_cannot_silently_drop_its_custom_life_entry(self):
        with tempfile.TemporaryDirectory() as tmp:
            descriptor = json.loads(client_suites.DESCRIPTOR.read_text())
            descriptor["entrypoints"]["fabric-client-gametest"].remove(client_suites.LIFE_EXCISE_ENTRIES[-1])
            path = Path(tmp) / "fabric.mod.json"
            path.write_text(json.dumps(descriptor))
            with self.assertRaisesRegex(ValueError, "Life32 requires"):
                client_suites.select_entries(descriptor=path)

    def test_the_aggregate_stays_diagnostic_until_native_visual_gates_exist(self):
        with tempfile.TemporaryDirectory() as tmp:
            catalog = json.loads(client_suites.CATALOG.read_text())
            catalog["diagnostic-life-excise"]["purpose"] = "release"
            path = Path(tmp) / "catalog.json"
            path.write_text(json.dumps(catalog))
            with self.assertRaisesRegex(ValueError, "Life32 requires"):
                client_suites.select_entries(suite="diagnostic-life-excise", catalog=path)


if __name__ == "__main__":
    unittest.main()
