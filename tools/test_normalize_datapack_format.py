"""Unit tests for tools/normalize_datapack_format.py across the 26.2 and 26.3 schemas."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import normalize_datapack_format as ndf
from datapack_profile import DataPackProfile

P262 = DataPackProfile.for_version("26.2")
P263 = DataPackProfile.for_version("26.3")

CHEST_263 = {
    "type": "minecraft:chest",
    "pools": [
        {
            "rolls": {"type": "minecraft:uniform", "min": 1, "max": 3},
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "wildercord:blank_rune",
                    "modifier": {"type": "minecraft:set_count", "count": 2},
                }
            ],
            "condition": {"type": "minecraft:match_tool", "predicate": {"items": "wildercord:pick"}},
        }
    ],
}

ADVANCEMENT_263 = {
    "parent": "minecraft:recipes/root",
    "criteria": {
        "has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "wildercord:cinnamon_bow"}]}},
        "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": "wildercord:cinnamon_grand_bow"}},
    },
    "requirements": [["has_the_recipe", "has_item"]],
    "rewards": {"recipes": ["wildercord:cinnamon_grand_bow"]},
}


class NormalizeLootTests(unittest.TestCase):
    def test_262_uses_functions_and_function(self):
        out = ndf.normalize_loot(P262, CHEST_263)
        pool = out["pools"][0]
        self.assertIn("conditions", pool)
        self.assertNotIn("condition", pool)
        entry = pool["entries"][0]
        self.assertIn("functions", entry)
        self.assertNotIn("modifier", entry)
        self.assertEqual(entry["functions"][0]["function"], "minecraft:set_count")
        self.assertNotIn("type", entry["functions"][0])

    def test_262_keeps_number_provider_type_untouched(self):
        out = ndf.normalize_loot(P262, CHEST_263)
        self.assertEqual(out["pools"][0]["rolls"]["type"], "minecraft:uniform")

    def test_263_round_trips_back(self):
        out = ndf.normalize_loot(P263, ndf.normalize_loot(P262, CHEST_263))
        self.assertIn("modifier", out["pools"][0]["entries"][0])
        self.assertIn("condition", out["pools"][0])

    def test_normalizing_is_idempotent(self):
        once = ndf.normalize_loot(P262, CHEST_263)
        self.assertEqual(once, ndf.normalize_loot(P262, once))


class NormalizeAdvancementTests(unittest.TestCase):
    LOCATION_263 = {
        "criteria": {
            "oathhold": {
                "trigger": "minecraft:location",
                "conditions": {
                    "player": {
                        "type": "minecraft:entity_properties",
                        "entity": "this",
                        "predicate": {"minecraft:location": {"structures": "wildercord:oathhold_sector"}},
                    }
                },
            }
        }
    }

    def test_262_singularizes_the_trigger_condition(self):
        out = ndf.normalize_advancement(P262, ADVANCEMENT_263)
        conditions = out["criteria"]["has_the_recipe"]["conditions"]
        self.assertEqual(conditions, {"recipe": "wildercord:cinnamon_grand_bow"})
        self.assertEqual(out["rewards"]["recipes"], ["wildercord:cinnamon_grand_bow"])

    def test_263_pluralizes_it_again(self):
        out = ndf.normalize_advancement(P263, ndf.normalize_advancement(P262, ADVANCEMENT_263))
        self.assertEqual(out["criteria"]["has_the_recipe"]["conditions"], {"recipes": "wildercord:cinnamon_grand_bow"})

    def test_262_wraps_the_player_condition_in_a_list(self):
        out = ndf.normalize_advancement(P262, self.LOCATION_263)
        player = out["criteria"]["oathhold"]["conditions"]["player"]
        self.assertIsInstance(player, list)
        self.assertEqual(player[0]["condition"], "minecraft:entity_properties")
        self.assertNotIn("type", player[0])
        self.assertEqual(player[0]["predicate"], {"minecraft:location": {"structures": "wildercord:oathhold_sector"}})

    def test_263_collapses_the_player_list_back_to_one_object(self):
        out = ndf.normalize_advancement(P263, ndf.normalize_advancement(P262, self.LOCATION_263))
        player = out["criteria"]["oathhold"]["conditions"]["player"]
        self.assertIsInstance(player, dict)
        self.assertEqual(player["type"], "minecraft:entity_properties")

    def test_advancement_normalizing_is_idempotent(self):
        once = ndf.normalize_advancement(P262, self.LOCATION_263)
        self.assertEqual(once, ndf.normalize_advancement(P262, once))


class RewriteTreeTests(unittest.TestCase):
    def _repo(self, tmp, relative, payload):
        root = Path(tmp)
        target = root / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        return root, target

    def test_check_reports_without_writing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, target = self._repo(tmp, "src/main/resources/data/wildercord/loot_table/chests/x.json", CHEST_263)
            changed = ndf.rewrite_tree(root, P262, "src/main/resources/data/wildercord/loot_table", ndf.normalize_loot, True)
            self.assertEqual(len(changed), 1)
            self.assertIn('"modifier"', target.read_text(encoding="utf-8"))

    def test_rewrite_lands_the_262_schema(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, target = self._repo(tmp, "src/main/resources/data/wildercord/advancement/recipes/misc/x.json", ADVANCEMENT_263)
            changed = ndf.rewrite_tree(
                root, P262, "src/main/resources/data/wildercord/advancement", ndf.normalize_advancement, False
            )
            self.assertEqual(len(changed), 1)
            self.assertIn('"recipe": "wildercord:cinnamon_grand_bow"', target.read_text(encoding="utf-8"))

    def test_already_correct_files_are_left_alone(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, target = self._repo(tmp, "src/main/resources/data/wildercord/loot_table/chests/x.json", CHEST_263)
            ndf.rewrite_tree(root, P262, "src/main/resources/data/wildercord/loot_table", ndf.normalize_loot, False)
            before = target.read_bytes()
            changed = ndf.rewrite_tree(root, P262, "src/main/resources/data/wildercord/loot_table", ndf.normalize_loot, False)
            self.assertEqual(changed, [])
            self.assertEqual(before, target.read_bytes())


if __name__ == "__main__":
    unittest.main()
