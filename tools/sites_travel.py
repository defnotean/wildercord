"""The travel sites (pack 5): roadside places along the overworld's roads, all peaceful. Each writes its structure,
structure set, biome tag, chest loot, a "find it" advancement and its texts. The Java lives in
dev.wildercord.world.sites.travel; the books' pages and the lore journal's Places entries are translated here."""

SALT = 2026100900 + 5 * 100

# id: (name, spacing, separation, biomes, advancement description, icon)
SITES = {
    "travel_wayfarer_inn": ("Wayfarer Inn", 34, 12, ["plains", "sunflower_plains", "meadow", "forest", "birch_forest", "taiga", "savanna", "snowy_plains", "cherry_grove"],
                            "Find a wayfarer inn at a crossroads", "minecraft:campfire"),
    "travel_watchtower": ("Watchtower", 40, 14, ["windswept_hills", "windswept_forest", "windswept_gravelly_hills", "meadow", "savanna_plateau", "plains", "taiga", "snowy_taiga"],
                          "Find a road watchtower", "minecraft:spyglass"),
    "travel_broken_bridge": ("Broken Bridge", 40, 14, ["plains", "forest", "birch_forest", "taiga", "meadow", "savanna", "sparse_jungle", "flower_forest"],
                             "Find a broken bridge over a creek", "minecraft:stone_bricks"),
    "travel_rune_library": ("Rune Library", 44, 16, ["forest", "birch_forest", "dark_forest", "old_growth_birch_forest", "taiga", "plains", "cherry_grove", "meadow"],
                            "Find a roadside rune library", "minecraft:bookshelf"),
    "travel_standing_stones": ("Standing Stones", 36, 12, ["plains", "meadow", "windswept_hills", "snowy_plains", "savanna", "sunflower_plains", "taiga", "old_growth_pine_taiga"],
                               "Find a ring of standing stones", "minecraft:mossy_cobblestone"),
    "travel_caravan_camp": ("Caravan Camp", 34, 12, ["savanna", "plains", "desert", "sunflower_plains", "meadow", "taiga", "forest", "snowy_plains"],
                            "Find a caravan camp on the road", "minecraft:lead"),
    "travel_cartographer_hut": ("Cartographer's Hut", 36, 12, ["plains", "forest", "birch_forest", "taiga", "meadow", "snowy_plains", "savanna", "flower_forest"],
                                "Find a cartographer's hut", "minecraft:cartography_table"),
    "travel_vow_circle": ("Vow Circle", 44, 16, ["meadow", "cherry_grove", "plains", "flower_forest", "birch_forest", "sunflower_plains", "snowy_plains", "taiga"],
                          "Find a vow circle shrine", "minecraft:quartz_pillar"),
}

# Travel and Exploring runes (the Rune Catalog's uses), per chest: (rune, weight). The JUnit test checks each use.
RUNES = {
    "travel_wayfarer_inn": [("wayfarer_hymn", 2), ("swift", 3), ("softsole", 2), ("waymark", 3), ("lodestar", 2), ("homeward", 1), ("skyread", 2)],
    "travel_watchtower": [("sky_reading", 3), ("sun_reading", 3), ("moon_reading", 3), ("land_reading", 3), ("starchart", 2), ("waymark", 2), ("rally_light", 1), ("lodestar", 1)],
    "travel_broken_bridge": [("icepath", 3), ("wind_steps", 2), ("leap", 3), ("feather_fall", 3), ("surefoot", 3), ("dash", 2), ("porpoise", 1)],
    "travel_rune_library": [("lore_reading", 3), ("chalk_line", 2), ("chalkline", 2), ("relic_sense", 2), ("appraise", 2), ("landread", 2), ("lostfind", 2)],
    "travel_standing_stones": [("ruin_sense", 3), ("relic_sense", 3), ("grave_bearing", 2), ("portal_reckoning", 2), ("lux_reading", 3), ("stronghold_compass", 1), ("spire_sense", 1)],
    "travel_caravan_camp": [("steedsong", 2), ("steedmend", 3), ("trot", 3), ("swift", 3), ("tarry", 2), ("village_sense", 3), ("folk_census", 2), ("appraise", 2)],
    "travel_cartographer_hut": [("village_sense", 3), ("shipwreck_sense", 2), ("portal_sense", 2), ("home_bearing", 3), ("spawn_bearing", 3), ("starchart", 3), ("landread", 2)],
    "travel_vow_circle": [("glidewind", 1), ("wind_steps", 2), ("lostfind", 3), ("tarry", 2), ("soar", 1), ("homeward", 1), ("blink", 1)],
}

# Plain goods, per chest: (item, weight, low, high).
GOODS = {
    "travel_wayfarer_inn": [("minecraft:bread", 5, 2, 4), ("minecraft:cooked_mutton", 4, 1, 3), ("minecraft:baked_potato", 4, 2, 4), ("minecraft:map", 3, 1, 1),
                            ("minecraft:torch", 3, 4, 8), ("minecraft:white_bed", 1, 1, 1), ("minecraft:emerald", 2, 1, 3)],
    "travel_watchtower": [("minecraft:spyglass", 3, 1, 1), ("minecraft:flint_and_steel", 3, 1, 1), ("minecraft:map", 3, 1, 1), ("minecraft:arrow", 3, 4, 10),
                          ("minecraft:torch", 3, 4, 8), ("minecraft:compass", 2, 1, 1)],
    "travel_broken_bridge": [("minecraft:emerald", 5, 1, 4), ("minecraft:gold_nugget", 4, 2, 7), ("minecraft:iron_ingot", 2, 1, 2), ("minecraft:lead", 2, 1, 1),
                             ("minecraft:oak_boat", 1, 1, 1), ("minecraft:paper", 3, 1, 4)],
    "travel_rune_library": [("wildercord:torn_page", 4, 1, 1), ("wildercord:blank_rune", 4, 1, 3), ("minecraft:book", 4, 1, 3), ("minecraft:paper", 3, 2, 6),
                            ("minecraft:ink_sac", 2, 1, 3), ("minecraft:candle", 2, 1, 3)],
    "travel_standing_stones": [("wildercord:torn_page", 4, 1, 1), ("wildercord:mana_crystal", 2, 1, 1), ("minecraft:amethyst_shard", 3, 2, 5), ("minecraft:gold_ingot", 2, 1, 3),
                               ("minecraft:lapis_lazuli", 3, 2, 6)],
    "travel_caravan_camp": [("minecraft:emerald", 5, 2, 5), ("minecraft:saddle", 1, 1, 1), ("minecraft:lead", 3, 1, 2), ("minecraft:hay_block", 2, 1, 2),
                            ("minecraft:leather", 3, 2, 5), ("minecraft:dried_kelp", 3, 3, 8), ("minecraft:map", 2, 1, 1)],
    "travel_cartographer_hut": [("minecraft:paper", 5, 3, 8), ("minecraft:compass", 3, 1, 1), ("minecraft:glass_pane", 2, 1, 3), ("minecraft:spyglass", 1, 1, 1),
                                ("minecraft:book", 2, 1, 2), ("minecraft:ink_sac", 2, 1, 3)],
    "travel_vow_circle": [("wildercord:blank_rune", 4, 2, 4), ("wildercord:mana_crystal", 3, 1, 1), ("minecraft:amethyst_shard", 3, 2, 5), ("minecraft:experience_bottle", 3, 2, 5),
                          ("minecraft:lapis_lazuli", 2, 2, 6)],
}

LANG = {
    "journal.wildercord.entry.place.travel_wayfarer_inn": "Rested at a wayfarer inn.",
    "journal.wildercord.entry.place.travel_watchtower": "Climbed a road watchtower.",
    "journal.wildercord.entry.place.travel_broken_bridge": "Crossed a broken bridge.",
    "journal.wildercord.entry.place.travel_rune_library": "Read in a rune library.",
    "journal.wildercord.entry.place.travel_standing_stones": "Stood in a ring of standing stones.",
    "journal.wildercord.entry.place.travel_caravan_camp": "Shared a fire at a caravan camp.",
    "journal.wildercord.entry.place.travel_cartographer_hut": "Visited a cartographer's hut.",
    "journal.wildercord.entry.place.travel_vow_circle": "Walked a vow circle.",
    "book.wildercord.travel_ledger.1": "Guest Ledger\n\nRooms are free to those who walk. Take a bed, mind the fire, leave the pantry fuller than you found it if you can.",
    "book.wildercord.travel_ledger.2": "Heard on the road:\n\nA tower with a cold beacon on the hills. A bridge with its middle gone. Stones in a ring that tell a story. A hut full of maps.",
    "book.wildercord.travel_ledger.3": "Lost your way? Open the Rune Catalog (Ctrl+B) and pick Use: Travel or Use: Exploring. Those runes find roads, homes and the places between.",
    "book.wildercord.travel_watch.1": "Watch Orders\n\nKeep the beacon cold until there is need. Light it with flint and steel and the smoke climbs high enough for any road to see.",
    "book.wildercord.travel_watch.2": "From up here you can read the land. Runes that read the sky, sun and moon are kept in the watch chest. Take what you need.",
    "book.wildercord.travel_toll.1": "Toll Notice\n\nThe middle fell in the spring flood. Jump the gap if you dare. If you fall, the water is deep and there are ladders on both banks.",
    "book.wildercord.travel_toll.2": "Tolls are suspended until the bridge is mended. The takings are in the chest. Bridge runes would help, if anyone still carved them.",
    "book.wildercord.travel_catalog.1": "Reading the Catalog\n\nThere are hundreds of runes. Open the Rune Catalog with Ctrl+B, or the Catalog link on your Cord screen.",
    "book.wildercord.travel_catalog.2": "Set Use to Travel for runes that move you: dashes, glides, safe falls, mounts, paths over water.\n\nSet Use to Exploring for runes that tell you where things are.",
    "book.wildercord.travel_catalog.3": "Pick a rune and the Catalog shows what goes well with it. A sense rune and a bearing rune make a fine pair for the road.",
    "book.wildercord.travel_riddle.0": "A Reader's Riddle\n\nWalk the blue carpet from the door. Count the shelf columns as you pass. At the third, the lowest shelf is not a shelf at all.",
    "book.wildercord.travel_riddle.1": "A Reader's Riddle\n\nWalk the red carpet from the door. Count the shelf columns as you pass. At the fifth, the lowest shelf is not a shelf at all.",
    "book.wildercord.travel_riddle.2": "A Reader's Riddle\n\nWalk the blue carpet from the door. Count the shelf columns as you pass. At the eighth, the lowest shelf is not a shelf at all.",
    "book.wildercord.travel_riddle.end": "Break that one book and the reading nook is yours. The other shelves are only shelves, so spare them.",
    "book.wildercord.travel_first_roads.1": "The First Roads\n\nBefore the roads, travellers walked from stone to stone. Each ring was a day's walk from the next.",
    "book.wildercord.travel_first_roads.2": "The road-makers carved runes that told the way: which way home, which way a ruin lay, which way the next ring stood.",
    "book.wildercord.travel_first_roads.3": "When the roads were done the rings were left to the grass. The tallest stone of each still points inward, toward the old road.",
    "book.wildercord.travel_first_roads.4": "At every ring the road-makers left a gift for the next walker. Kneel at the flat stone before the tallest and dig beside it.",
    "book.wildercord.travel_survey.1": "Survey Notes\n\nThe roads between inns, towers and libraries were never mapped in full. The desk chest keeps my last map. It marks a place I never reached.",
    "book.wildercord.travel_survey.2": "A cartography table copies and extends a map. Bring paper. Bring a compass too: it locks the map to you.",
    "book.wildercord.travel_survey.3": "Sense runes tell which way a village, ruin or portal lies. Bearing runes point home or to where you first woke.",
    "book.wildercord.travel_vows.1": "Vows of the Circle\n\nSome Heart Circles ask for one vow when they form. Each vow has two sides. You may hold only one side at a time.",
    "book.wildercord.travel_vows.vow": "Circle %1$s\n\n%2$s: %3$s\n\nor\n\n%4$s: %5$s\n\n/vow take %6$s\n/vow take %7$s",
    "book.wildercord.travel_vows.2": "Type /vow to see your vows.\n\nA vow is silent while its circle is unformed or cracked.\n\nTo choose again, /vow release and the circle's number. It costs %s levels.",
}
for _id, (_name, _sp, _sep, _biomes, _desc, _icon) in SITES.items():
    LANG[f"advancements.wildercord.world.{_id}.title"] = _name
    LANG[f"advancements.wildercord.world.{_id}.description"] = _desc


def _runes(g, site):
    known = {r["path"] for r in g.read_runes()}
    banned = g.found_only() | set(g.INNATE) | set(g.FUSED)
    entries = []
    for path, weight in RUNES[site]:
        assert path in known, f"{site}: unknown rune {path}"
        assert path not in banned, f"{site}: rune {path} is found-only, innate or fused"
        entries.append(g.rune_entry(path, weight))
    return entries


def write(g):
    ids = list(SITES)
    targets = [f"wildercord:{s}" for s in ids if s != "travel_cartographer_hut"]
    g.write_json(g.DATA / "tags/worldgen/structure/travel_map_targets.json", {"replace": False, "values": targets})
    for n, (site, (name, spacing, separation, biomes, desc, icon)) in enumerate(SITES.items()):
        g.write_json(g.DATA / f"worldgen/structure/{site}.json", {
            "type": "wildercord:site", "site": site, "biomes": f"#wildercord:has_structure/{site}", "spawn_overrides": {},
            "step": "surface_structures", "terrain_adaptation": "beard_thin"})
        g.write_json(g.DATA / f"worldgen/structure_set/{site}.json", {
            "placement": {"type": "minecraft:random_spread", "salt": SALT + n, "spacing": spacing, "separation": separation},
            "structures": [{"structure": f"wildercord:{site}", "weight": 1}]})
        g.write_json(g.DATA / f"tags/worldgen/biome/has_structure/{site}.json", {"replace": False, "values": [f"minecraft:{b}" for b in biomes]})
        pools = [{"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": _runes(g, site)},
                 {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [g.item_entry(*goods) for goods in GOODS[site]]}]
        if site == "travel_cartographer_hut":
            pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:map", "functions": [
                {"function": "minecraft:exploration_map", "destination": "#wildercord:travel_map_targets", "decoration": "minecraft:target_x",
                 "zoom": 2, "search_radius": 50, "skip_existing_chunks": True}]}]})
        g.write_json(g.DATA / f"loot_table/chests/{site}.json", {"type": "minecraft:chest", "pools": pools})
        # Through g.adv: write_advancements runs after this and clears any advancement it did not list.
        g.adv(f"world/{site}", "world/ley_line" if site == "travel_wayfarer_inn" else "world/travel_wayfarer_inn", g.item(icon),
              name, desc, g.in_structure(f"wildercord:{site}"), xp=20)
    return ids
