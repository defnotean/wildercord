"""Worldgen, treasury loot tables, advancements, and lang text for Multiplayer Co-Op Trials."""

TRIAL_BIOMES = [
    "minecraft:plains",
    "minecraft:meadow",
    "minecraft:taiga",
    "minecraft:snowy_taiga",
    "minecraft:savanna",
    "minecraft:windswept_hills",
    "minecraft:stony_peaks",
    "minecraft:grove",
]

CRUCIBLE_BIOMES = [
    "wildercord:cinder_caldera",
    "wildercord:emberwood_reach",
    "minecraft:badlands",
    "minecraft:eroded_badlands",
    "minecraft:wooded_badlands",
]

SANCTUM_ID = "trial_leyline_sanctum"
CRUCIBLE_ID = "trial_ashen_crucible"

LANG = {
    f"journal.wildercord.entry.place.{SANCTUM_ID}": "Leyline Sanctum: a warded Co-Op Trial arena where Vaelkhor, the Leyline Sovereign tests coordinated parties.",
    f"advancements.wildercord.world.{SANCTUM_ID}.title": "Sanctum of the Sovereign",
    f"advancements.wildercord.world.{SANCTUM_ID}.description": "Enter a Leyline Sanctum Co-Op Trial arena",
    f"journal.wildercord.entry.place.{CRUCIBLE_ID}": "Ashen Crucible: a warded Co-Op foundry colosseum where Kharvok, the Crucible Sovereign challenges parties amid quenchable vents.",
    f"advancements.wildercord.world.{CRUCIBLE_ID}.title": "Crucible of Ashen Flame",
    f"advancements.wildercord.world.{CRUCIBLE_ID}.description": "Enter an Ashen Crucible Co-Op Trial foundry",
}


def write(gen):
    known = {r["path"] for r in gen.read_runes()}
    banned = gen.found_only() | set(gen.INNATE) | set(gen.FUSED)
    runes = [("domain", 2), ("ring", 3), ("lance", 3), ("nova", 3), ("totem", 2)]
    for path, _w in runes:
        assert path in known, f"{SANCTUM_ID}: unknown rune {path}"
        assert path not in banned, f"{SANCTUM_ID}: rune {path} is banned"

    for site_id, biomes, salt, treasury_path, icon, parent, adv_title, adv_desc in (
        (
            SANCTUM_ID, TRIAL_BIOMES, 2026112501,
            "loot_table/chests/trial_leyline_treasury.json",
            "wildercord:trial_gate", "world/ley_line",
            LANG[f"advancements.wildercord.world.{SANCTUM_ID}.title"],
            LANG[f"advancements.wildercord.world.{SANCTUM_ID}.description"],
        ),
        (
            CRUCIBLE_ID, CRUCIBLE_BIOMES, 2026112502,
            "loot_table/chests/trial_crucible_treasury.json",
            "wildercord:crucible_vent", f"world/{SANCTUM_ID}",
            LANG[f"advancements.wildercord.world.{CRUCIBLE_ID}.title"],
            LANG[f"advancements.wildercord.world.{CRUCIBLE_ID}.description"],
        ),
    ):
        gen.write_json(gen.DATA / f"tags/worldgen/biome/has_structure/{site_id}.json", {"replace": False, "values": biomes})
        gen.write_json(gen.DATA / f"worldgen/structure/{site_id}.json", {
            "type": "wildercord:site",
            "biomes": f"#wildercord:has_structure/{site_id}",
            "step": "surface_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "beard_thin",
            "site": site_id,
        })
        gen.write_json(gen.DATA / f"worldgen/structure_set/{site_id}.json", {
            "structures": [{"structure": f"wildercord:{site_id}", "weight": 1}],
            "placement": {
                "type": "minecraft:random_spread",
                "spacing": 52,
                "separation": 18,
                "salt": salt,
            },
        })
        gen.write_json(gen.DATA / treasury_path, {
            "type": "minecraft:chest",
            "pools": [
                {
                    "rolls": {"type": "minecraft:uniform", "min": 2, "max": 3},
                    "entries": [gen.rune_entry(path, weight) for path, weight in runes],
                },
                {
                    "rolls": {"type": "minecraft:uniform", "min": 4, "max": 6},
                    "entries": [
                        {"type": "minecraft:item", "name": "wildercord:mana_crystal", "weight": 8,
                         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]},
                        {"type": "minecraft:item", "name": "wildercord:blank_rune", "weight": 12,
                         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 6}}]},
                        {"type": "minecraft:item", "name": "minecraft:amethyst_shard", "weight": 10,
                         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 4, "max": 8}}]},
                        {"type": "minecraft:item", "name": "minecraft:gold_ingot", "weight": 10,
                         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 7}}]},
                        {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 6,
                         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]},
                    ],
                },
            ],
        })
        gen.adv(
            f"world/{site_id}",
            parent,
            gen.item(icon),
            adv_title,
            adv_desc,
            gen.in_structure(f"wildercord:{site_id}"),
            xp=25,
        )

    return [SANCTUM_ID, CRUCIBLE_ID]
