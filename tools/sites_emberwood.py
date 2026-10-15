"""Worldgen, loot, advancements, books, and resident dialogue for the Emberwood Reach site pack."""

SITES = [
    ("emberwood_charcoal_yard", 32, 11, 2026101605, ["wildercord:emberwood_reach", "minecraft:wooded_badlands", "minecraft:badlands"],
     "wildercord:cinder_fern", "Keeper of the Kilns",
     "Find an Emberwood Charcoal Yard and its Cinder Fern ash bed in the Emberwood Reach",
     [("minecraft:bread", 3, 6, 12), ("minecraft:charcoal", 3, 6, 12), ("minecraft:coal", 3, 6, 10),
      ("minecraft:iron_ingot", 2, 4, 8), ("minecraft:brick", 3, 6, 10), ("minecraft:flint_and_steel", 1, 1, 6),
      ("wildercord:cinder_fern", 2, 4, 12)],
     [("ember", 3), ("fireward", 3), ("kilnbake", 3), ("kilned", 2), ("firebreak", 2)]),
    ("emberwood_kiln_court", 36, 12, 2026101606, ["wildercord:emberwood_reach"],
     "wildercord:kiln_heartstone", "The Cooled Kiln",
     "Find an Emberwood Kiln Court in the Emberwood Reach",
     [("minecraft:iron_ingot", 2, 5, 10), ("minecraft:copper_ingot", 3, 6, 10), ("minecraft:compass", 1, 1, 8),
      ("minecraft:charcoal", 3, 6, 10), ("wildercord:cinder_fern", 2, 4, 10), ("wildercord:ember_tuft", 1, 2, 8)],
     [("smelt", 3), ("toolmend", 3), ("emberguard", 3), ("hearthglow", 2), ("fire", 2)]),
]

LANG = {
    "advancements.wildercord.world.emberwood_charcoal_yard.title": "Keeper of the Kilns",
    "advancements.wildercord.world.emberwood_charcoal_yard.description": "Find an Emberwood Charcoal Yard and its Cinder Fern ash bed in the Emberwood Reach",
    "advancements.wildercord.world.emberwood_kiln_court.title": "The Cooled Kiln",
    "advancements.wildercord.world.emberwood_kiln_court.description": "Find an Emberwood Kiln Court in the Emberwood Reach",
    "journal.wildercord.entry.place.emberwood_charcoal_yard": "Emberwood Charcoal Yard: a brick-and-timber burner's yard, Cinder Fern ash bed and kiln draft lamp.",
    "journal.wildercord.entry.place.emberwood_kiln_court": "Emberwood Kiln Court: a ceramic kiln court where The Kiln Widow waits.",
    "entity.wildercord.kiln_keeper": "Emberwood Kiln Keeper",
    "dialogue.wildercord.speaker.kiln_keeper": "Emberwood Kiln Keeper",
    "journal.wildercord.entry.person.kiln_keeper": "Emberwood Kiln Keeper: tends the charcoal kilns, Cinder Fern ash beds, and burner tallies in the Emberwood Reach.",
    "journal.wildercord.entry.learned.emberwood_draft": "Tended an Emberwood Charcoal Yard draft lamp so the firebreak road stays readable through smoke and dusk.",
    "dialogue.wildercord.kiln_keeper.greet": "Mind the ash bed by the west wall. A banked kiln looks cold long before its heart gives up the heat.",
    "dialogue.wildercord.kiln_keeper.cinder_fern": "We plant Cinder Fern in the podzol and coarse dirt beside the firebreak. Wait for the fronds to cool before you cut them.",
    "dialogue.wildercord.kiln_keeper.firebreaks": "Hessa held the kiln gate eleven winters by clearing the firebreaks first and letting the draft do the work.",
    "dialogue.wildercord.kiln_keeper.kiln_widow": "Out in the scorched woods stands our old ceramic Kiln Court. Its Kiln Widow still wakes if you stoke the Kiln Heartstone.",
    "dialogue.wildercord.kiln_keeper.forge_ledger": "Check the Mountain Forge Halls, Prospector's Camps, Basalt Foundries, or the Kiln Court itself. Their tallies record every firing.",
    "dialogue.wildercord.kiln_keeper.kiln_draft": "Our eastern brick stack carries a shuttered draft lamp. Flip its lever and stand beside the light so burners on the ash road know the kiln is banked.",
    "dialogue.wildercord.kiln_keeper.draft_return": "The draft lamp is lit. Anyone coming back along the firebreak at dusk will know the embers are settled.",
    "dialogue.wildercord.kiln_keeper.widow_return": "You carry the Cooled Kiln's compass. You held the firebreak lane while the old ceramic mantle vented its heat.",
    "book.wildercord.emberwood_yard.1": "Hessa kept the southern kiln gate eleven winters. She taught us that a charcoal burner's first tool is not the axe, but the coarse-dirt firebreak.",
    "book.wildercord.emberwood_yard.2": "Set Cinder Fern in the ash bed along the west wall. Harvest only mature cooled fronds for new Cinder Fern roots, and leave the root for the next firing.",
    "book.wildercord.emberwood_yard.3": "Our charcoal tallies match the ledgers in the Mountain Forge Halls, Prospector's Camps, and the ceramic Kiln Court deeper in the Reach.",
    "book.wildercord.emberwood_yard.4": "Before nightfall, throw the lever on the eastern draft lamp. A steady light tells the next burner that the kiln is banked and the firebreak holds.",
    "book.wildercord.kiln_court.1": "Here stands the Emberwood Kiln Court, where The Kiln Widow was cast from brick and iron slag to test the draft of the great firing rings.",
    "book.wildercord.kiln_court.2": "Stoke the Kiln Heartstone only when ready. Step clear of its annular kiln draft and molten glaze drop, and hold the unlit firebreak lane during its damper sweep.",
}


def item_entry(item, low, high, weight):
    entry = {"type": "minecraft:item", "name": item, "weight": weight}
    if low != 1 or high != 1:
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


def write(gen):
    known = {r["path"] for r in gen.read_runes()}
    banned = gen.found_only() | set(gen.INNATE) | set(gen.FUSED)
    ids = []
    for site_id, spacing, separation, salt, biomes, icon, title, desc, supplies, runes in SITES:
        ids.append(site_id)
        for path, _weight in runes:
            assert path in known, f"{site_id}: unknown rune {path}"
            assert path not in banned, f"{site_id}: rune {path} is found-only, innate or fused"
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
                "spacing": spacing,
                "separation": separation,
                "salt": salt,
            },
        })
        gen.write_json(gen.DATA / f"loot_table/chests/{site_id}.json", {
            "type": "minecraft:chest",
            "pools": [
                {
                    "rolls": {"type": "minecraft:uniform", "min": 1, "max": 2},
                    "entries": [gen.rune_entry(path, weight) for path, weight in runes],
                },
                {
                    "rolls": {"type": "minecraft:uniform", "min": 3, "max": 5},
                    "entries": [item_entry(item, lo, hi, w) for item, lo, hi, w in supplies],
                },
            ],
        })
        gen.adv(f"world/{site_id}", "world/ley_line", gen.item(icon), title, desc, gen.in_structure(f"wildercord:{site_id}"), xp=20)
    return ids
