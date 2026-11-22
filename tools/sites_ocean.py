"""Worldgen, chest loot tables, advancements, and journal/lang text for the 5 Deep Ocean structures."""

OCEAN_BIOMES = [
    "minecraft:ocean",
    "minecraft:deep_ocean",
    "minecraft:lukewarm_ocean",
    "minecraft:deep_lukewarm_ocean",
    "minecraft:cold_ocean",
    "minecraft:deep_cold_ocean",
]

DEEP_OCEAN_BIOMES = [
    "minecraft:deep_ocean",
    "minecraft:deep_lukewarm_ocean",
    "minecraft:deep_cold_ocean",
]

# id: (spacing, separation, salt, biomes, icon, title, description, supplies, runes)
SITES = [
    ("ocean_coral_dome_sanctuary", 36, 12, 2026112201, OCEAN_BIOMES,
     "wildercord:siren_pearl_lantern", "Tidefolk Haven",
     "Find a Coral-Dome Tidefolk Sanctuary and meet its underwater merchants",
     [("wildercord:abyssal_pearl", 1, 3, 12), ("wildercord:kelp_wrapped_sashimi", 2, 4, 12),
      ("wildercord:sea_salt_ramen", 1, 2, 10), ("minecraft:prismarine_crystals", 3, 7, 10),
      ("minecraft:emerald", 2, 6, 10), ("minecraft:heart_of_the_sea", 1, 1, 2)],
     [("wave", 3), ("ring", 3), ("burst", 2), ("beam", 2)]),
    ("ocean_sunken_galleon", 32, 10, 2026112202, OCEAN_BIOMES,
     "wildercord:tide_cleaver_harpoon", "Sunken Flagship",
     "Board a Sunken Sovereign Galleon resting on the ocean floor",
     [("wildercord:abyssal_iron_plate", 1, 3, 10), ("wildercord:coral_edge_fang", 1, 3, 10),
      ("minecraft:gold_ingot", 3, 8, 12), ("minecraft:nautilus_shell", 1, 2, 8),
      ("minecraft:emerald", 3, 7, 10), ("minecraft:tnt", 2, 4, 6)],
     [("bolt", 3), ("lance", 3), ("wave", 2), ("cone", 2)]),
    ("ocean_kelp_spire_watchtower", 30, 10, 2026112203, OCEAN_BIOMES,
     "wildercord:bathysphere_chestplate", "Watch Over the Deep",
     "Reach a Kelp-Spire Watchtower linking the seabed to the surface",
     [("wildercord:kelp_wrapped_sashimi", 2, 4, 12), ("minecraft:dried_kelp", 6, 14, 14),
      ("minecraft:copper_ingot", 4, 9, 12), ("wildercord:abyssal_pearl", 1, 2, 8),
      ("minecraft:spyglass", 1, 1, 6), ("minecraft:soul_sand", 2, 4, 8)],
     [("ray", 3), ("spark", 3), ("wave", 2), ("burst", 2)]),
    ("ocean_hydrothermal_forge", 34, 11, 2026112204, DEEP_OCEAN_BIOMES,
     "wildercord:trench_anvil", "Volcanic Deep",
     "Discover a Hydrothermal Vent Forge and its Trench Anvil",
     [("wildercord:abyssal_iron_plate", 2, 4, 12), ("wildercord:voltaic_jelly_sac", 1, 3, 10),
      ("wildercord:abyssal_pearl", 1, 2, 10), ("minecraft:raw_copper", 5, 10, 12),
      ("minecraft:diamond", 1, 3, 5), ("minecraft:magma_cream", 2, 4, 8)],
     [("pillar", 3), ("nova", 3), ("comet", 2), ("beam", 2)]),
    ("ocean_abyssal_citadel", 48, 16, 2026112205, DEEP_OCEAN_BIOMES,
     "wildercord:sovereign_altar", "Throne of Thalassor",
     "Enter an Abyssal Trench Citadel and its sunken Boss Colosseum",
     [("wildercord:abyssal_pearl", 2, 4, 12), ("wildercord:abyssal_iron_plate", 2, 5, 10),
      ("wildercord:star_barnacle_ambergris", 1, 2, 8), ("wildercord:sea_salt_ramen", 1, 2, 10),
      ("minecraft:gold_block", 1, 2, 6), ("minecraft:diamond", 2, 4, 8)],
     [("domain", 2), ("totem", 3), ("lance", 3), ("ring", 3)]),
]

LANG = {
    "journal.wildercord.entry.place.ocean_coral_dome_sanctuary": "Coral-Dome Tidefolk Sanctuary: four dry cyan-glass domes and a Conduit Plaza housing Tidefolk merchants.",
    "journal.wildercord.entry.place.ocean_sunken_galleon": "Sunken Sovereign Galleon: a three-deck warship lying on the seabed, guarded by Barnacle-Hulk Dredgers.",
    "journal.wildercord.entry.place.ocean_kelp_spire_watchtower": "Kelp-Spire Watchtower: a seabed diving bell wired to a surface dock with dual bubble-column elevators.",
    "journal.wildercord.entry.place.ocean_hydrothermal_forge": "Hydrothermal Vent Forge: a basalt-and-copper trench foundry housing the Trench Anvil in a dry glass dome.",
    "journal.wildercord.entry.place.ocean_abyssal_citadel": "Abyssal Trench Citadel: a stepped dark-prismarine ziggurat opening onto the eight-obelisk colosseum of Thalassor.",
}
for _sid, _sp, _sep, _salt, _bio, _icon, _title, _desc, _sup, _runes in SITES:
    LANG[f"advancements.wildercord.world.{_sid}.title"] = _title
    LANG[f"advancements.wildercord.world.{_sid}.description"] = _desc


def item_entry(item, low, high, weight):
    entry = {"type": "minecraft:item", "name": item, "weight": weight}
    if low != 1 or high != 1:
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


def write(gen):
    known = {r["path"] for r in gen.read_runes()}
    banned = gen.found_only() | set(gen.INNATE) | set(gen.FUSED)
    ids = []
    for sid, spacing, separation, salt, biomes, icon, title, desc, supplies, runes in SITES:
        ids.append(sid)
        for path, _w in runes:
            assert path in known, f"{sid}: unknown rune {path}"
            assert path not in banned, f"{sid}: rune {path} is banned"
        gen.write_json(gen.DATA / f"tags/worldgen/biome/has_structure/{sid}.json", {"replace": False, "values": biomes})
        gen.write_json(gen.DATA / f"worldgen/structure/{sid}.json", {
            "type": "wildercord:site",
            "biomes": f"#wildercord:has_structure/{sid}",
            "step": "surface_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "none",
            "site": sid,
        })
        gen.write_json(gen.DATA / f"worldgen/structure_set/{sid}.json", {
            "structures": [{"structure": f"wildercord:{sid}", "weight": 1}],
            "placement": {
                "type": "minecraft:random_spread",
                "spacing": spacing,
                "separation": separation,
                "salt": salt,
            },
        })
        gen.write_json(gen.DATA / f"loot_table/chests/{sid}.json", {
            "type": "minecraft:chest",
            "pools": [
                {
                    "rolls": {"type": "minecraft:uniform", "min": 1, "max": 2},
                    "entries": [gen.rune_entry(path, weight) for path, weight in runes],
                },
                {
                    "rolls": {"type": "minecraft:uniform", "min": 3, "max": 5},
                    "entries": [item_entry(it, lo, hi, w) for it, lo, hi, w in supplies],
                },
            ],
        })
        gen.adv(f"world/{sid}", "world/ley_line", gen.item(icon), title, desc, gen.in_structure(f"wildercord:{sid}"), xp=20)
    return ids
