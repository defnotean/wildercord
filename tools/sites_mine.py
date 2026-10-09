"""The mining sites pack (sites-mine): eight places of mines, caves, forges and crystals.

Their pieces are src/main/java/dev/wildercord/world/sites/mine. This file writes each site's worldgen structure,
structure set, biome tag, chest loot, advancement and words. tools/generate_assets.py finds it by its name.
"""

PACK_SALT = 2026100900 + 4 * 100

# id: (name, find text, salt n, spacing, separation, step, terrain adaptation, biomes, icon)
HILLS = ["minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills", "minecraft:meadow",
         "minecraft:grove", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:taiga"]
LAND = ["#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_savanna", "minecraft:plains", "minecraft:sunflower_plains",
        "minecraft:meadow", "minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:snowy_plains"]
SITES = {
    "mine_hillside_mine": ("Hillside Mine", "Find a Hillside Mine", 1, 36, 12, "surface_structures", "none", HILLS,
                           "minecraft:rail"),
    "mine_forge_hall": ("Mountain Forge Hall", "Find a Mountain Forge Hall", 2, 52, 18, "surface_structures", "beard_thin",
                        ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:meadow", "minecraft:grove",
                         "minecraft:snowy_slopes"], "minecraft:anvil"),
    "mine_crystal_lab": ("Crystal Survey Lab", "Find a Crystal Survey Lab in a buried geode", 3, 40, 14, "underground_structures",
                         "none", LAND, "minecraft:amethyst_cluster"),
    "mine_collapsed_delve": ("Collapsed Delve", "Find a Collapsed Delve", 4, 40, 14, "surface_structures", "none", LAND + HILLS[2:3],
                             "minecraft:gravel"),
    "mine_basalt_foundry": ("Basalt Foundry", "Find a Basalt Foundry in the Nether", 5, 30, 10, "underground_decoration", "none",
                            ["minecraft:basalt_deltas"], "minecraft:blast_furnace"),
    "mine_deep_vault": ("Deepslate Vault", "Find a Deepslate Vault far below the ground", 6, 56, 20, "underground_structures", "none",
                        LAND, "minecraft:chiseled_deepslate"),
    "mine_prospector_camp": ("Prospector's Camp", "Find a Prospector's Camp in the badlands", 7, 34, 12, "surface_structures",
                             "beard_thin", ["#minecraft:is_badlands"], "minecraft:gold_nugget"),
    "mine_miners_rest": ("Miner's Rest", "Find a Miner's Rest on a cliff edge", 8, 30, 10, "surface_structures", "none",
                         ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
                          "minecraft:windswept_savanna", "minecraft:savanna_plateau", "minecraft:meadow", "minecraft:grove",
                          "minecraft:stony_shore"], "minecraft:lantern"),
}

# Monsters a site's own cave keeps drawing in (only for the guarded ones).
SPAWNS = {
    "mine_basalt_foundry": [("minecraft:magma_cube", 2), ("minecraft:blaze", 1)],
    "mine_deep_vault": [("minecraft:skeleton", 2), ("minecraft:zombie", 1)],
}

JOURNAL = {
    "mine_hillside_mine": "A Hillside Mine. The crew left their carts on the rails and their tally in the shed.",
    "mine_forge_hall": "A Mountain Forge Hall. The Iron breath was taught at its anvils, one blow per breath.",
    "mine_crystal_lab": "A Crystal Survey Lab, built inside a geode. Someone measured how the crystals hum.",
    "mine_collapsed_delve": "A Collapsed Delve. The beams held the roof long enough for one miner to wait.",
    "mine_basalt_foundry": "A Basalt Foundry. Ore from the deltas was poured here before the fires went wild.",
    "mine_deep_vault": "A Deepslate Vault. Its door opens to earth and fire together, never one alone.",
    "mine_prospector_camp": "A Prospector's Camp. Gold in the creek, and a map of where the seams run.",
    "mine_miners_rest": "A Miner's Rest. A lamp is kept lit here for those still below.",
}

LANG = {}
for _id, (_name, _find, *_rest) in SITES.items():
    LANG[f"advancements.wildercord.sites.{_id}.title"] = _name
    LANG[f"advancements.wildercord.sites.{_id}.description"] = _find
    LANG[f"journal.wildercord.entry.place.{_id}"] = JOURNAL[_id]
    LANG[f"structure.wildercord.{_id}"] = _name


def _item(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def _rune(rune, weight):
    return {"type": "minecraft:item", "name": "wildercord:rune", "weight": weight,
            "functions": [{"function": "minecraft:set_components", "components": {"wildercord:rune": f"wildercord:{rune}"}}]}


def _page(method, weight):
    return {"type": "minecraft:item", "name": "wildercord:manual_page", "weight": weight,
            "functions": [{"function": "minecraft:set_components", "components": {"wildercord:breathing_method": method}}]}


def _table(*pools):
    return {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi} if lo != hi else lo,
                                                  "entries": entries} for lo, hi, entries in pools]}


# The mining and crafting runes each site points at (all of them craftable, Tier I to III; none from the Archives' Tier IV).
LOOT = {
    "mine_hillside_mine": _table(
        (3, 5, [_item("minecraft:torch", 6, 4, 12), _item("minecraft:rail", 5, 4, 10), _item("minecraft:coal", 5, 2, 8),
                _item("minecraft:raw_iron", 4, 1, 4), _item("minecraft:raw_copper", 4, 2, 6), _item("minecraft:bread", 3, 1, 3),
                _item("minecraft:iron_pickaxe", 1), _item("minecraft:stone_pickaxe", 2)]),
        (1, 1, [_rune("stairdelve", 3), _rune("riser", 2), _rune("plumbline", 3), _rune("deepsound", 3), _rune("oretally", 2),
                _rune("torchfall", 2)])),
    "mine_hillside_mine_tally": _table(
        (2, 4, [_item("minecraft:raw_iron", 4, 2, 5), _item("minecraft:raw_copper", 4, 3, 8), _item("minecraft:raw_gold", 2, 1, 3),
                _item("minecraft:lapis_lazuli", 2, 2, 6), _item("minecraft:redstone", 2, 2, 6), _item("minecraft:emerald", 1)])),
    "mine_forge_hall": _table(
        (3, 5, [_item("minecraft:iron_ingot", 5, 2, 6), _item("minecraft:gold_ingot", 2, 1, 3), _item("minecraft:coal", 4, 3, 8),
                _item("minecraft:iron_nugget", 4, 4, 12), _item("minecraft:iron_axe", 1), _item("minecraft:iron_sword", 1),
                _item("minecraft:chainmail_chestplate", 1)]),
        (1, 1, [_rune("toolmend", 3), _rune("kilnbake", 3), _rune("smelt", 2), _rune("kilned", 2), _rune("blockpack", 2),
                _rune("millstone", 2)])),
    "mine_forge_hall_masterwork": _table(
        (1, 1, [_page("iron", 1)]),
        (1, 2, [_item("minecraft:iron_block", 2), _item("minecraft:diamond", 1), _item("minecraft:smithing_table", 2),
                _item("minecraft:anvil", 1), _item("wildercord:blank_rune", 3, 2, 4)])),
    "mine_crystal_lab": _table(
        (3, 5, [_item("minecraft:amethyst_shard", 6, 3, 9), _item("minecraft:tinted_glass", 3, 1, 3), _item("minecraft:spyglass", 1),
                _item("minecraft:glow_ink_sac", 3, 1, 4), _item("minecraft:paper", 3, 2, 6), _item("minecraft:calcite", 2, 2, 6),
                _item("wildercord:mana_crystal", 1)]),
        (1, 1, [_rune("hollowsense", 3), _rune("gloomsight", 3), _rune("lumenpath", 2), _rune("headlamp", 2), _rune("silklift", 2)])),
    "mine_collapsed_delve": _table(
        (2, 4, [_item("minecraft:torch", 5, 3, 8), _item("minecraft:bread", 4, 1, 3), _item("minecraft:oak_log", 4, 2, 5),
                _item("minecraft:iron_shovel", 1), _item("minecraft:coal", 4, 2, 6)]),
        (1, 1, [_rune("shoreup", 4), _rune("caveward", 2), _rune("siftfall", 3)])),
    "mine_collapsed_delve_cache": _table(
        (2, 4, [_item("minecraft:raw_iron", 4, 2, 6), _item("minecraft:raw_gold", 3, 1, 4), _item("minecraft:diamond", 1),
                _item("minecraft:emerald", 2, 1, 3), _item("minecraft:iron_pickaxe", 1)]),
        (1, 1, [_rune("luckstrike", 2), _rune("orepluck", 2), _rune("gangue", 1), _rune("lodepull", 2)]),
        (1, 1, [_page("stone", 1)])),
    "mine_basalt_foundry": _table(
        (3, 5, [_item("minecraft:gold_ingot", 4, 1, 4), _item("minecraft:iron_ingot", 3, 1, 4), _item("minecraft:magma_cream", 3, 1, 3),
                _item("minecraft:blackstone", 3, 4, 10), _item("minecraft:quartz", 4, 3, 9), _item("minecraft:netherite_scrap", 1)]),
        (1, 1, [_rune("lavaseal", 3), _rune("kilnbake", 2), _rune("smelt", 2), _rune("kilned", 2), _rune("gold_parley", 2)]),
        (0, 1, [_page("iron", 1)])),
    "mine_deep_vault": _table(
        (3, 5, [_item("minecraft:diamond", 2, 1, 2), _item("minecraft:gold_ingot", 4, 2, 5), _item("minecraft:emerald", 3, 1, 4),
                _item("minecraft:iron_ingot", 4, 3, 7), _item("minecraft:lapis_lazuli", 3, 4, 9), _item("wildercord:mana_crystal", 1)]),
        (1, 2, [_rune("deepway", 2), _rune("gangue", 2), _rune("luckstrike", 2), _rune("orepluck", 2), _rune("caveward", 2),
                _rune("lavaseal", 2)]),
        (1, 1, [_page("stone", 1), _page("iron", 1)])),
    "mine_deep_vault_antechamber": _table(
        (2, 4, [_item("minecraft:torch", 4, 4, 10), _item("minecraft:cobbled_deepslate", 3, 8, 16), _item("minecraft:bread", 3, 1, 3),
                _item("minecraft:raw_iron", 3, 2, 5)]),
        (1, 1, [_rune("headlamp", 2), _rune("torchfall", 2), _rune("lumenpath", 1)])),
    "mine_prospector_camp": _table(
        (3, 5, [_item("minecraft:gold_nugget", 6, 4, 14), _item("minecraft:raw_gold", 3, 1, 3), _item("minecraft:raw_copper", 3, 2, 6),
                _item("minecraft:map", 1), _item("minecraft:bread", 3, 1, 3), _item("minecraft:iron_shovel", 1),
                _item("minecraft:bucket", 2)]),
        (1, 1, [_rune("deepsound", 3), _rune("oretally", 3), _rune("siftfall", 2), _rune("luckstrike", 1), _rune("lodepull", 2)]),
        (0, 1, [_page("dune", 1)])),
    "mine_miners_rest": _table(
        (2, 4, [_item("minecraft:bread", 4, 1, 4), _item("minecraft:torch", 4, 4, 10), _item("minecraft:candle", 3, 1, 3),
                _item("minecraft:cooked_beef", 2, 1, 3), _item("minecraft:golden_carrot", 1, 1, 2)]),
        (1, 1, [_rune("caveward", 2), _rune("headlamp", 3), _rune("torchfall", 3), _rune("shoreup", 2)]),
        (1, 1, [_page("stone", 2), _page("iron", 1)])),
}


def write(g):
    for sid, (name, find, n, spacing, separation, step, adaptation, biomes, icon) in SITES.items():
        overrides = {}
        if sid in SPAWNS:
            overrides = {"monster": {"bounding_box": "piece", "spawns": [{"type": t, "weight": w, "count": 1} for t, w in SPAWNS[sid]]}}
        g.write_json(g.DATA / f"worldgen/structure/{sid}.json", {"type": "wildercord:site", "site": sid,
                     "biomes": f"#wildercord:has_structure/{sid}", "spawn_overrides": overrides, "step": step,
                     "terrain_adaptation": adaptation})
        placement = {"type": "minecraft:random_spread", "salt": PACK_SALT + n, "spacing": spacing, "separation": separation}
        if sid == "mine_basalt_foundry":
            placement["exclusion_zone"] = {"other_set": "minecraft:nether_complexes", "chunk_count": 4}
        g.write_json(g.DATA / f"worldgen/structure_set/{sid}.json", {"placement": placement,
                     "structures": [{"structure": f"wildercord:{sid}", "weight": 1}]})
        g.write_json(g.DATA / f"tags/worldgen/biome/has_structure/{sid}.json", {"replace": False, "values": biomes})
        # Added to the generator's tree (written, and old ones cleared, after every pack has run); the text is in LANG.
        g.adv(f"sites/{sid}", "world/ley_line" if sid == "mine_hillside_mine" else "sites/mine_hillside_mine", g.item(icon), name, find,
              g.in_structure(f"wildercord:{sid}"), xp=20 if spacing < 50 else 35)
    for path, table in LOOT.items():
        g.write_json(g.DATA / f"loot_table/chests/{path}.json", table)
    return list(SITES)
