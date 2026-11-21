"""Worldgen, loot, advancements and English text for Oathhold, the 2,048x2,048 Sovereign Megacity."""

CENTER_BIOMES = [
    "minecraft:plains",
    "minecraft:sunflower_plains",
    "minecraft:meadow",
    "minecraft:forest",
    "minecraft:flower_forest",
    "minecraft:birch_forest",
    "minecraft:old_growth_birch_forest",
    "minecraft:dark_forest",
    "minecraft:taiga",
    "minecraft:savanna",
    "minecraft:savanna_plateau",
    "minecraft:cherry_grove",
    "minecraft:windswept_hills",
    "minecraft:windswept_forest",
]

LANG = {
    "advancements.wildercord.world.oathhold.title": "Oathhold, Sovereign Citadel",
    "advancements.wildercord.world.oathhold.description": "Cross the great walls of Oathhold, a walled metropolis spanning two thousand blocks",
    "journal.wildercord.entry.place.oathhold": "Oathhold: the two-thousand-block Sovereign Citadel of the Oathbound High Knights.",
}

LOOT_TABLES = {
    "oathhold_treasury": (
        ["shield", "halo", "bloom", "keenkeep"],
        [("minecraft:gold_ingot", 4, 3, 8), ("minecraft:diamond", 2, 1, 3),
         ("minecraft:emerald", 3, 4, 10), ("wildercord:aura_shard", 3, 2, 5),
         ("wildercord:blank_rune", 3, 2, 5), ("wildercord:mana_crystal", 2, 1, 3)],
    ),
    "oathhold_armory": (
        ["shield", "keenkeep", "magnetize", "ember"],
        [("minecraft:iron_ingot", 4, 4, 10), ("minecraft:shield", 2, 1, 1),
         ("minecraft:iron_sword", 2, 1, 1), ("minecraft:arrow", 3, 8, 20),
         ("wildercord:aura_shard", 2, 1, 3), ("minecraft:emerald", 2, 2, 5)],
    ),
    "oathhold_arcanum": (
        ["halo", "bloom", "glidewind", "lumenpath"],
        [("wildercord:blank_rune", 4, 2, 5), ("wildercord:torn_page", 3, 1, 3),
         ("wildercord:mana_crystal", 3, 1, 3), ("wildercord:spell_scroll", 2, 1, 1),
         ("minecraft:lapis_lazuli", 3, 4, 10), ("minecraft:book", 3, 2, 5)],
    ),
    "oathhold_market": (
        ["fair_wind", "bloom", "suzu_chime"],
        [("minecraft:emerald", 4, 3, 8), ("minecraft:bread", 4, 4, 10),
         ("minecraft:cooked_salmon", 3, 3, 7), ("minecraft:golden_carrot", 2, 2, 5),
         ("minecraft:lantern", 2, 2, 4)],
    ),
    "oathhold_barracks": (
        ["shield", "ember", "fireward", "keenkeep"],
        [("minecraft:iron_ingot", 3, 3, 7), ("minecraft:bread", 4, 3, 8),
         ("minecraft:iron_axe", 2, 1, 1), ("wildercord:aura_shard", 2, 1, 2),
         ("minecraft:emerald", 2, 1, 4)],
    ),
}


def write(gen):
    known = {r["path"] for r in gen.read_runes()}
    banned = gen.found_only() | set(gen.INNATE) | set(gen.FUSED)
    allowed = known - banned

    # 1. Center anchor structure (wildercord:oathhold) for /locate discovery
    gen.write_json(gen.DATA / "tags/worldgen/biome/has_structure/oathhold.json", {
        "replace": False,
        "values": CENTER_BIOMES,
    })
    gen.write_json(gen.DATA / "worldgen/structure/oathhold.json", {
        "type": "wildercord:site",
        "site": "oathhold",
        "biomes": "#wildercord:has_structure/oathhold",
        "spawn_overrides": {},
        "step": "surface_structures",
        "terrain_adaptation": "none",
    })
    gen.write_json(gen.DATA / "worldgen/structure_set/oathhold.json", {
        "placement": {
            "type": "minecraft:random_spread",
            "salt": 20261130,
            "separation": 160,
            "spacing": 256,
        },
        "structures": [{"structure": "wildercord:oathhold", "weight": 1}],
    })

    # 2. Seamless 128x128 urban sector structure (wildercord:oathhold_sector)
    gen.write_json(gen.DATA / "tags/worldgen/biome/has_structure/oathhold_sector.json", {
        "replace": False,
        "values": ["#minecraft:is_overworld"],
    })
    gen.write_json(gen.DATA / "worldgen/structure/oathhold_sector.json", {
        "type": "wildercord:site",
        "site": "oathhold_sector",
        "biomes": "#wildercord:has_structure/oathhold_sector",
        "spawn_overrides": {},
        "step": "surface_structures",
        "terrain_adaptation": "none",
    })
    gen.write_json(gen.DATA / "worldgen/structure_set/oathhold_sector.json", {
        "placement": {
            "type": "minecraft:random_spread",
            "salt": 20261131,
            "separation": 7,
            "spacing": 8,
        },
        "structures": [{"structure": "wildercord:oathhold_sector", "weight": 1}],
    })

    # 3. City District Loot Tables
    for table_id, (runes, extras) in LOOT_TABLES.items():
        rune_entries = [gen.rune_entry(r, 2) for r in runes if r in allowed]
        pools = []
        if rune_entries:
            pools.append({"rolls": 1, "entries": rune_entries})
        pools.append({
            "rolls": {"type": "minecraft:uniform", "min": 3, "max": 5},
            "entries": [gen.item_entry(*e) for e in extras],
        })
        gen.write_json(gen.DATA / f"loot_table/chests/{table_id}.json", {
            "type": "minecraft:chest",
            "pools": pools,
        })

    # 4. Discovery Advancement triggers on any city sector or center anchor
    gen.adv(
        "world/oathhold",
        "world/archive",
        gen.item("minecraft:chiseled_tuff_bricks"),
        LANG["advancements.wildercord.world.oathhold.title"],
        LANG["advancements.wildercord.world.oathhold.description"],
        gen.in_structure("wildercord:oathhold_sector"),
        xp=100,
    )

    return ["oathhold", "oathhold_sector"]
