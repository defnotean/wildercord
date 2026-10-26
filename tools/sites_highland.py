"""Worldgen, loot, advancements, books, and resident dialogue for the Windscar Highlands site pack."""

SITES = [
    ("highland_cairn_lodge", 32, 11, 2026101601, ["wildercord:windscar_highlands", "minecraft:windswept_hills", "minecraft:meadow"],
     "wildercord:windreed", "Keeper of the Cairns",
     "Find a Highland Cairn Lodge and its leeward Windreed terrace in the Windscar Highlands",
     [("minecraft:bread", 3, 6, 12), ("minecraft:cooked_mutton", 2, 4, 10), ("minecraft:coal", 3, 6, 10),
      ("minecraft:string", 3, 6, 10), ("minecraft:leather", 2, 4, 8), ("minecraft:flint_and_steel", 1, 1, 6),
      ("wildercord:windreed", 2, 4, 12)],
     [("wind_steps", 3), ("feather_fall", 3), ("surefoot", 3), ("waymark", 2), ("land_reading", 2)]),
    ("highland_survey_ring", 36, 12, 2026101602, ["wildercord:windscar_highlands"],
     "wildercord:survey_astrolabe", "The High Meridian",
     "Find a Highland Survey Ring on the Windscar ridges",
     [("minecraft:spyglass", 1, 1, 8), ("minecraft:compass", 1, 1, 8), ("minecraft:amethyst_shard", 2, 5, 10),
      ("minecraft:copper_ingot", 3, 6, 10), ("wildercord:windreed", 2, 4, 10)],
     [("sky_reading", 3), ("sun_reading", 3), ("moon_reading", 3), ("starchart", 2), ("lodestar", 2)]),
]

LANG = {
    "advancements.wildercord.world.highland_cairn_lodge.title": "Keeper of the Cairns",
    "advancements.wildercord.world.highland_cairn_lodge.description": "Find a Highland Cairn Lodge and its leeward Windreed terrace in the Windscar Highlands",
    "advancements.wildercord.world.highland_survey_ring.title": "The High Meridian",
    "advancements.wildercord.world.highland_survey_ring.description": "Find a Highland Survey Ring on the Windscar ridges",
    "journal.wildercord.entry.place.highland_cairn_lodge": "Highland Cairn Lodge: a dry-stone shelter, leeward Windreed plot and cairn lamp.",
    "journal.wildercord.entry.place.highland_survey_ring": "Highland Survey Ring: a stone meridian court where the Glass Cartographer waits.",
    "entity.wildercord.cairn_keeper": "Highland Cairn Keeper",
    "dialogue.wildercord.speaker.cairn_keeper": "Highland Cairn Keeper",
    "journal.wildercord.entry.person.cairn_keeper": "Highland Cairn Keeper: tends the Windscar cairn lodges, leeward Windreed terraces and Marchkeeper route notes.",
    "journal.wildercord.entry.learned.scout_wind_mark": "Tended a Highland Cairn Lodge lamp so the ridge path stays readable through dusk and wind.",
    "dialogue.wildercord.cairn_keeper.greet": "Come in out of the ridge wind. Warm your hands by the hearth before you try the next shelf.",
    "dialogue.wildercord.cairn_keeper.four_lights": "A green glimmer on the horizon, answering ours. Below the roots, the Listener says the shutter hums to the same count.",
    "dialogue.wildercord.cairn_keeper.windreed": "We plant Windreed in the lee of the north wall. Take the ripe tassels and leave the root for the next walker.",
    "dialogue.wildercord.cairn_keeper.cairns": "The Marchkeeper scout raised paired cairns along these heights: one stone for the road ahead, one for those carried home.",
    "dialogue.wildercord.cairn_keeper.cartographer": "Higher on the dry shelves stands a stone meridian ring. Its Glass Cartographer still sights the peaks if you wake its astrolabe.",
    "dialogue.wildercord.cairn_keeper.watch_orders": "Check the watchtowers and standing stone rings along the old hill roads. Their books still record how the cairns were laid.",
    "dialogue.wildercord.cairn_keeper.cairn_light": "Our eastern cairn carries a shuttered lamp. Flip its lever and stand beside the light so late walkers can read the ridge.",
    "dialogue.wildercord.cairn_keeper.light_return": "The ridge lamp is lit again. Thank you. If you test the Highland Survey Ring, read the book beside the astrolabe before you wake it.",
    "dialogue.wildercord.cairn_keeper.cartographer_return": "You carry the High Meridian's compass. Even that old brass surveyor yielded to steady footwork on the stone ring.",
    "book.wildercord.cairn_keeper.1": "When the Marchkeepers ended as an army, we stayed on the heights as guides. A road in the Windscar survives only while someone tends its stones.",
    "book.wildercord.cairn_keeper.2": "Build your wind-break on the north shelf and plant Windreed in its lee. Gather only the mature tassels; the root belongs to whoever climbs after you.",
    "book.wildercord.cairn_keeper.3": "The scout's paired cairns point between the watchtowers and the older standing stones. One cairn marks the turn; its partner marks the safe descent.",
    "book.wildercord.cairn_keeper.4": "Before you leave at dusk, throw the lever on the eastern cairn lamp. A lit cairn tells the next traveller that the shelf holds firm.",
    "book.wildercord.survey_ring.1": "Here stands the Marchkeepers' high meridian ring. Its Glass Cartographer was built to measure sightlines across the ridges with brass transit and prism glass.",
    "book.wildercord.survey_ring.2": "Use the Survey Astrolabe only if you wish to test your footing. Step clear of its transit arc and plumb circle, and cross inside the unlit meridian lane.",
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
