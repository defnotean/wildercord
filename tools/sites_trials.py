"""Worldgen, treasury loot table, advancement, and lang text for Multiplayer Co-Op Trials."""

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

SANCTUM_ID = "trial_leyline_sanctum"

LANG = {
    f"journal.wildercord.entry.place.{SANCTUM_ID}": "Leyline Sanctum: a warded Co-Op Trial arena where Vaelkhor, the Leyline Sovereign tests coordinated parties.",
    f"advancements.wildercord.world.{SANCTUM_ID}.title": "Sanctum of the Sovereign",
    f"advancements.wildercord.world.{SANCTUM_ID}.description": "Enter a Leyline Sanctum Co-Op Trial arena",
}


def write(gen):
    known = {r["path"] for r in gen.read_runes()}
    banned = gen.found_only() | set(gen.INNATE) | set(gen.FUSED)
    runes = [("domain", 2), ("ring", 3), ("lance", 3), ("nova", 3), ("totem", 2)]
    for path, _w in runes:
        assert path in known, f"{SANCTUM_ID}: unknown rune {path}"
        assert path not in banned, f"{SANCTUM_ID}: rune {path} is banned"

    gen.write_json(gen.DATA / f"tags/worldgen/biome/has_structure/{SANCTUM_ID}.json", {"replace": False, "values": TRIAL_BIOMES})
    gen.write_json(gen.DATA / f"worldgen/structure/{SANCTUM_ID}.json", {
        "type": "wildercord:site",
        "biomes": f"#wildercord:has_structure/{SANCTUM_ID}",
        "step": "surface_structures",
        "spawn_overrides": {},
        "terrain_adaptation": "beard_thin",
        "site": SANCTUM_ID,
    })
    gen.write_json(gen.DATA / f"worldgen/structure_set/{SANCTUM_ID}.json", {
        "structures": [{"structure": f"wildercord:{SANCTUM_ID}", "weight": 1}],
        "placement": {
            "type": "minecraft:random_spread",
            "spacing": 52,
            "separation": 18,
            "salt": 2026112501,
        },
    })
    gen.write_json(gen.DATA / "loot_table/chests/trial_leyline_treasury.json", {
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
        f"world/{SANCTUM_ID}",
        "world/ley_line",
        gen.item("wildercord:trial_gate"),
        LANG[f"advancements.wildercord.world.{SANCTUM_ID}.title"],
        LANG[f"advancements.wildercord.world.{SANCTUM_ID}.description"],
        gen.in_structure(f"wildercord:{SANCTUM_ID}"),
        xp=25,
    )
    return [SANCTUM_ID]
