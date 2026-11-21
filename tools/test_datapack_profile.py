"""Unit tests for tools/datapack_profile.py across Minecraft 26.2 and 26.3 schemas."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import datapack_profile as dp


class DataPackProfileTests(unittest.TestCase):
    def test_version_selection_and_rejection(self):
        self.assertEqual(dp.DataPackProfile.for_version("26.2").version, "26.2")
        self.assertEqual(dp.DataPackProfile.for_version("26.2.1").version, "26.2")
        self.assertEqual(dp.DataPackProfile.for_version("26.3").version, "26.3")
        p261 = dp.DataPackProfile.for_version("26.1")
        p2612 = dp.DataPackProfile.for_version("26.1.2")
        p121 = dp.DataPackProfile.for_version("1.21.11")
        self.assertEqual(p261.version, "26.1")
        self.assertEqual(p2612.version, "26.1")
        self.assertEqual(p121.version, "1.21.11")
        self.assertTrue(p2612.before_26_3)
        self.assertTrue(p2612.is_26_2)
        self.assertTrue(p2612.before_26_2)
        self.assertFalse(p2612.is_obfuscated)
        self.assertTrue(p121.before_26_3)
        self.assertTrue(p121.is_26_2)
        self.assertTrue(p121.before_26_2)
        self.assertTrue(p121.is_obfuscated)
        self.assertFalse(dp.DataPackProfile.for_version("26.3").before_26_3)
        with self.assertRaises(ValueError):
            dp.DataPackProfile.for_version("25.1")

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "gradle.properties").write_text("org.gradle.parallel=true\nminecraft_version=26.3\n", encoding="utf-8")
            self.assertEqual(dp.read_mc_version(root), "26.3")
            self.assertEqual(dp.DataPackProfile.from_gradle(root).version, "26.3")

    def test_loot_table_bidirectional_and_idempotent(self):
        legacy_loot = {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{
                    "type": "minecraft:item",
                    "name": "minecraft:arrow",
                    "functions": [
                        {"count": {"type": "minecraft:uniform", "min": 6, "max": 16}, "function": "minecraft:set_count"},
                    ],
                }],
                "conditions": [
                    {"condition": "minecraft:survives_explosion"},
                    {"condition": "minecraft:killed_by_player"},
                ],
            }],
        }
        p262 = dp.DataPackProfile.for_version("26.2")
        p263 = dp.DataPackProfile.for_version("26.3")

        as_263 = p263.loot_table(legacy_loot)
        pool_263 = as_263["pools"][0]
        self.assertEqual(pool_263["entries"][0]["modifier"], {
            "count": {"type": "minecraft:uniform", "min": 6, "max": 16},
            "type": "minecraft:set_count",
        })
        self.assertNotIn("functions", pool_263["entries"][0])
        self.assertEqual(pool_263["condition"], {
            "type": "minecraft:all_of",
            "terms": [{"type": "minecraft:survives_explosion"}, {"type": "minecraft:killed_by_player"}],
        })
        self.assertEqual(p263.loot_table(as_263), as_263)

        roundtrip_262 = p262.loot_table(as_263)
        self.assertEqual(roundtrip_262, legacy_loot)
        self.assertEqual(p262.loot_table(legacy_loot), legacy_loot)

    def test_exploration_map_and_block_property_condition(self):
        p262 = dp.DataPackProfile.for_version("26.2")
        p263 = dp.DataPackProfile.for_version("26.3")

        m262 = p262.exploration_map("#wildercord:travel_map_targets", "minecraft:target_x", zoom=2, search_radius=50)
        m263 = p263.exploration_map("wildercord:travel_map_targets", "minecraft:target_x", zoom=2, search_radius=50)
        self.assertEqual(m262["destination"], "wildercord:travel_map_targets")
        self.assertEqual(m263["destination"], "#wildercord:travel_map_targets")

        c262 = p262.block_property_condition("wildercord:moonreed", {"age": "2"})
        c263 = p263.block_property_condition("wildercord:moonreed", {"age": "2"})
        self.assertEqual(c262, {"condition": "minecraft:block_state_property", "block": "wildercord:moonreed", "properties": {"age": "2"}})
        converted_263 = p263.loot_table({"pools": [{"conditions": [c263]}]})["pools"][0]["condition"]
        self.assertEqual(converted_263, {"blocks": "wildercord:moonreed", "state": {"age": "2"}, "type": "minecraft:match_block"})

    def test_advancements(self):
        p262 = dp.DataPackProfile.for_version("26.2")
        p263 = dp.DataPackProfile.for_version("26.3")

        u262 = p262.recipe_unlock_advancement("wildercord:foo", "minecraft:stick")
        u263 = p263.recipe_unlock_advancement("wildercord:foo", "minecraft:stick")
        self.assertEqual(u262["criteria"]["has_the_recipe"]["conditions"], {"recipe": "wildercord:foo"})
        self.assertEqual(u263["criteria"]["has_the_recipe"]["conditions"], {"recipes": "wildercord:foo"})

        s262 = p262.in_structure_criterion("#wildercord:archive")
        s263 = p263.in_structure_criterion("#wildercord:archive")
        self.assertIsInstance(s262["conditions"]["player"], list)
        self.assertIsInstance(s263["conditions"]["player"], dict)
        self.assertIn("condition", s262["conditions"]["player"][0])
        self.assertIn("type", s263["conditions"]["player"])

    def test_worldgen_features_and_cleanup(self):
        p262 = dp.DataPackProfile.for_version("26.2")
        p263 = dp.DataPackProfile.for_version("26.3")

        self.assertEqual(p262.feature_subdir, "worldgen/configured_feature")
        self.assertEqual(p263.feature_subdir, "worldgen/feature")
        self.assertEqual(p262.unit_feature_json("wildercord:moonreed_patch"), {"type": "wildercord:moonreed_patch", "config": {}})
        self.assertEqual(p263.unit_feature_json("wildercord:moonreed_patch"), {"type": "wildercord:moonreed_patch"})

        sb262 = p262.simple_block_feature_json("wildercord:windreed", {"age": "2"})
        sb263 = p263.simple_block_feature_json("wildercord:windreed", {"age": "2"})
        self.assertEqual(sb262["config"]["to_place"]["type"], "minecraft:simple_state_provider")
        self.assertEqual(sb263["to_place"], {"id": "wildercord:windreed", "properties": {"age": "2"}})

        off = {"type": "minecraft:trapezoid", "min": -5, "max": 5, "plateau": 0}
        y_off = {"type": "minecraft:trapezoid", "min": -2, "max": 2, "plateau": 0}
        self.assertEqual(p262.placed_offset_modifier(off, y_off)["type"], "minecraft:random_offset")
        self.assertEqual(p263.placed_offset_modifier(off, y_off)["type"], "minecraft:offset")

        with tempfile.TemporaryDirectory() as tmp:
            data = Path(tmp)
            (data / "worldgen/feature").mkdir(parents=True)
            (data / "worldgen/feature/stale.json").write_text("{}")
            (data / "recipe/brewing").mkdir(parents=True)
            (data / "recipe/brewing/stale.json").write_text("{}")
            p262.clean_stale_feature_dir(data)
            p262.clean_stale_brewing_dir(data)
            self.assertFalse((data / "worldgen/feature").exists())
            self.assertFalse((data / "recipe/brewing").exists())

    def test_spawners_biomes_trades_glaive_and_brewing(self):
        p262 = dp.DataPackProfile.for_version("26.2")
        p263 = dp.DataPackProfile.for_version("26.3")

        self.assertEqual(p262.spawner_entry("minecraft:bat", 10, 8), {"type": "minecraft:bat", "weight": 10, "minCount": 8, "maxCount": 8})
        self.assertEqual(p263.spawner_entry("minecraft:bat", 10, 8), {"type": "minecraft:bat", "weight": 10, "count": 8})
        self.assertEqual(p262.spawner_entry("minecraft:frog", 10, 2, 5), {"type": "minecraft:frog", "weight": 10, "minCount": 2, "maxCount": 5})
        self.assertEqual(
            p263.spawner_entry("minecraft:frog", 10, 2, 5),
            {"type": "minecraft:frog", "weight": 10, "count": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 5}},
        )

        template_biome = {
            "attributes": {
                "minecraft:audio/background_music": {"default": {"sound": "minecraft:music.overworld.swamp"}},
                "minecraft:visual/sky_color": "#84acc3",
            },
            "carvers": ["minecraft:cave"],
            "downfall": 0.95,
        }
        spawns = {"monster": [p262.spawner_entry("minecraft:slime", 1, 1)], "creature": []}
        b262 = p262.apply_biome_spawns(template_biome, spawns)
        self.assertEqual(list(b262.keys()), ["attributes", "spawn_costs", "spawners", "carvers", "downfall"])
        self.assertEqual(tuple(b262["spawners"].keys()), dp.SPAWNER_CATEGORIES)
        self.assertNotIn("minecraft:gameplay/natural_mob_spawns", b262["attributes"])

        b263 = p263.apply_biome_spawns(template_biome, spawns)
        self.assertEqual(list(b263.keys()), ["attributes", "carvers", "downfall"])
        self.assertEqual(
            list(b263["attributes"].keys()),
            ["minecraft:audio/background_music", "minecraft:gameplay/natural_mob_spawns", "minecraft:visual/sky_color"],
        )

        p12110 = dp.DataPackProfile.for_version("1.21.10")
        legacy_biome = {
            "attributes": {
                "minecraft:audio/background_music": {"default": {"sound": "minecraft:music.overworld.swamp"}},
                "minecraft:visual/sky_color": "#84acc3",
                "minecraft:visual/water_fog_color": "#245257",
                "minecraft:gameplay/increased_fire_burnout": True,
            },
            "carvers": ["minecraft:cave"],
            "downfall": 0.95,
            "effects": {"grass_color": "#78a994", "water_color": "#3c8b91"},
        }
        b12110 = p12110.apply_biome_spawns(legacy_biome, spawns)
        self.assertNotIn("attributes", b12110)
        self.assertEqual(tuple(b12110["spawners"].keys()), dp.SPAWNER_CATEGORIES)
        self.assertEqual(b12110["effects"]["sky_color"], 0x84ACC3)
        self.assertEqual(b12110["effects"]["water_fog_color"], 0x245257)
        self.assertEqual(b12110["effects"]["grass_color"], 0x78A994)
        self.assertEqual(b12110["effects"]["fog_color"], 0x245257)
        self.assertEqual(b12110["effects"]["music"],
                         [{"data": {"sound": "minecraft:music.overworld.swamp", "replace_current_music": False}, "weight": 1}])

        mod = {"type": "wildercord:random_rune", "min_tier": 1, "max_tier": 2}
        self.assertEqual(p262.villager_item_modifier_field(mod), {"given_item_modifiers": [{"function": "wildercord:random_rune", "min_tier": 1, "max_tier": 2}]})
        self.assertEqual(p263.villager_item_modifier_field(mod), {"given_item_modifier": mod})

        self.assertEqual(p262.glaive_swing_component()[0], "minecraft:swing_animation")
        self.assertEqual(p263.glaive_swing_component()[0], "minecraft:attack_animation")

        self.assertEqual(p262.brewing_subdir, "brewing")
        self.assertEqual(p263.brewing_subdir, "recipe/brewing")
        json.dumps(b262)
        json.dumps(b263)


if __name__ == "__main__":
    unittest.main()
